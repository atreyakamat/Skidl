package com.skidl.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skidl.game.GameController
import com.skidl.game.ScoreManager
import com.skidl.game.WordBank
import com.skidl.model.GameSettings
import com.skidl.model.GuessMessage
import com.skidl.model.HostAdvertisement
import com.skidl.model.JoinMessage
import com.skidl.model.LobbyState
import com.skidl.model.Player
import com.skidl.model.PlayerJoinedMessage
import com.skidl.model.ReadyMessage
import com.skidl.model.RoundStartMessage
import com.skidl.model.SecretWordAssignedMessage
import com.skidl.model.SkidlMessage
import com.skidl.model.Stroke
import com.skidl.model.StrokeEndMessage
import com.skidl.model.StrokePoint
import com.skidl.model.StrokePointMessage
import com.skidl.model.StrokeStartMessage
import com.skidl.model.HeartbeatMessage
import com.skidl.network.ClientNetworkingManager
import com.skidl.network.ConnectionState
import com.skidl.network.DiscoveryBroadcaster
import com.skidl.network.DiscoveryListener
import com.skidl.network.HostNetworkingManager
import com.skidl.util.NetworkDefaults
import java.net.NetworkInterface
import java.util.Collections
import java.util.UUID
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class SkidlViewModel(
    private val applicationScope: CoroutineScope,
    private val context: Context
) : ViewModel() {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
        explicitNulls = false
        classDiscriminator = "#class"
    }

    private val wordBank = WordBank(context)
    private val scoreManager = ScoreManager()
    private var currentSecretWord: String? = null
    private var totalRounds: Int = 3
    private var currentRoundNumber: Int = 0
    private val guessTimestamps = java.util.concurrent.ConcurrentHashMap<String, MutableList<Long>>()
    private val correctGuessers = mutableSetOf<String>() // track who already guessed correctly this round
    private var errorDismissJob: Job? = null
    companion object {
        private const val MAX_PLAYERS = 12
        private const val MAX_NAME_LENGTH = 24
        private const val MAX_GUESS_LENGTH = 100
        private const val ERROR_DISMISS_MS = 5000L
    }

    private val discoveryListener = DiscoveryListener(applicationScope, json)
    private val hostNetworking = HostNetworkingManager(applicationScope, json)
    private val discoveryBroadcaster = DiscoveryBroadcaster(
        scope = applicationScope,
        roomId = UUID.randomUUID().toString(),
        json = json,
        ipProvider = { resolveLocalIp() }
    )
    private val clientNetworking = ClientNetworkingManager(applicationScope, json, OkHttpClient())

    private var gameController: GameController? = null
    private var clientConnectionMonitor: Job? = null
    private var roundTimerJob: Job? = null
    private var heartbeatJob: Job? = null

    private val _state = MutableStateFlow(SkidlUiState())
    val state: StateFlow<SkidlUiState> = _state

    init {
        observeDiscovery()
        observeHostMessages()
        observeClientMessages()
        observeDisconnects()
    }

    override fun onCleared() {
        super.onCleared()
        clientConnectionMonitor?.cancel()
        roundTimerJob?.cancel()
        heartbeatJob?.cancel()
        discoveryBroadcaster.stop()
        discoveryListener.stop()
        hostNetworking.stop()
        clientNetworking.disconnect()
    }

    fun navigate(screen: SkidlScreen) {
        _state.update { it.copy(screen = screen, error = null) }
    }

    fun setPlayerName(name: String) {
        _state.update { it.copy(displayName = name.take(MAX_NAME_LENGTH)) }
    }

    fun setRoomCode(code: String) {
        _state.update { it.copy(roomCode = code.takeIf { it.isNotBlank() }) }
    }

    fun setRoomName(roomName: String) {
        _state.update { it.copy(roomName = roomName) }
    }

    fun setManualIp(ip: String) {
        _state.update { it.copy(manualIp = ip) }
    }

    fun startHosting() {
        val playerId = "host-${UUID.randomUUID()}"
        val roomName = state.value.roomName.ifBlank { "Skidl Room" }
        val hostIp = resolveLocalIp()
        gameController = GameController(playerId, roomName)
        val hostPlayer = Player(playerId = playerId, name = state.value.displayName.ifBlank { "Host" }.take(MAX_NAME_LENGTH), isHost = true)
        gameController?.updatePlayer(hostPlayer)
        val serverStarted = hostNetworking.start()
        if (!serverStarted) {
            showError("Failed to start server. Port may be in use.")
            gameController = null
            return
        }
        discoveryBroadcaster.start(roomName, 1, NetworkDefaults.WEBSOCKET_PORT)
        discoveryBroadcaster.update(roomName, 1)
        heartbeatJob?.cancel()
        heartbeatJob = applicationScope.launch {
            while (isActive) {
                hostNetworking.broadcast(HeartbeatMessage(time = System.currentTimeMillis()))
                delay(NetworkDefaults.HEARTBEAT_INTERVAL_MS)
            }
        }
        _state.update {
            it.copy(
                isHosting = true,
                playerId = playerId,
                screen = SkidlScreen.Lobby,
                hostAddress = if (hostIp.isNotBlank()) "ws://$hostIp:${NetworkDefaults.WEBSOCKET_PORT}" else "",
                lobbyState = LobbyState(
                    hostPlayerId = playerId,
                    players = listOf(hostPlayer),
                    settings = GameSettings(roomName = roomName, roomCode = it.roomCode),
                    isHostReady = false
                )
            )
        }
    }

    fun stopHosting() {
        discoveryBroadcaster.stop()
        hostNetworking.stop()
        gameController = null
        scoreManager.reset()
        clientConnectionMonitor?.cancel()
        roundTimerJob?.cancel()
        heartbeatJob?.cancel()
        guessTimestamps.clear()
        currentRoundNumber = 0
        currentSecretWord = null
        _state.value = SkidlUiState()
    }

    fun startDiscovery() {
        discoveryListener.start()
        _state.update { it.copy(isDiscovering = true, availableHosts = emptyList(), screen = SkidlScreen.Join) }
    }

    fun stopDiscovery() {
        discoveryListener.stop()
        _state.update { it.copy(isDiscovering = false) }
    }

    fun joinHost(url: String) {
        val playerId = "client-${UUID.randomUUID()}"
        _state.update { it.copy(playerId = playerId) }
        clientNetworking.connect(url)
        clientConnectionMonitor?.cancel()
        clientConnectionMonitor = viewModelScope.launch {
            clientNetworking.connectionState.collect { state ->
                when (state) {
                    ConnectionState.Connected -> {
                        sendClientMessage(
                            JoinMessage(
                                playerId = playerId,
                                name = _state.value.displayName.ifBlank { "Player" },
                                roomCode = _state.value.roomCode
                            )
                        )
                        _state.update { it.copy(screen = SkidlScreen.Lobby) }
                    }

                    ConnectionState.Disconnected -> {
                        _state.update { it.copy(error = "Connection lost", screen = SkidlScreen.Join) }
                    }

                    ConnectionState.Connecting -> {}
                }
            }
        }
    }

    fun readyUp(ready: Boolean) {
        val playerId = _state.value.playerId
        val message = ReadyMessage(playerId = playerId, ready = ready)
        if (_state.value.isHosting) {
            handleReadyMessage(playerId, message)
            hostNetworking.broadcast(message)
        } else {
            sendClientMessage(message)
        }
    }

    fun sendGuess(text: String) {
        val trimmed = text.trim().take(MAX_GUESS_LENGTH)
        if (trimmed.isBlank()) return
        // Prevent drawer from guessing
        val round = _state.value.roundState
        if (round?.drawerId == _state.value.playerId) return
        val playerId = _state.value.playerId
        val guess = com.skidl.model.GuessNetworkMessage(playerId = playerId, text = trimmed)
        if (_state.value.isHosting) {
            handleGuess(playerId, guess)
        } else {
            sendClientMessage(guess)
        }
    }

    fun undoLastStroke() {
        val lastStroke = _state.value.roundState?.strokes?.lastOrNull() ?: return
        val message = com.skidl.model.StrokeRemoveMessage(strokeId = lastStroke.strokeId)
        if (_state.value.isHosting) {
            handleStrokeRemove(message)
        } else {
            sendClientMessage(message)
        }
    }

    fun clearCanvas() {
        val message = com.skidl.model.CanvasClearMessage()
        if (_state.value.isHosting) {
            handleCanvasClear(message)
        } else {
            sendClientMessage(message)
        }
    }

    fun onStrokeStart(stroke: StrokeStartMessage) {
        if (_state.value.isHosting) {
            val modelStroke = Stroke(
                strokeId = stroke.strokeId,
                playerId = stroke.playerId,
                color = stroke.color,
                thickness = stroke.thickness,
                points = listOf(StrokePoint(stroke.x, stroke.y, stroke.timestamp))
            )
            gameController?.appendStroke(modelStroke)
            hostNetworking.broadcast(stroke)
        } else {
            sendClientMessage(stroke)
        }
    }

    fun onStrokePoint(message: SkidlMessage) {
        if (_state.value.isHosting) {
            when (message) {
                is StrokePointMessage -> {
                    val existing = gameController?.round?.value?.strokes?.find { it.strokeId == message.strokeId }
                    if (existing != null) {
                        val updated = existing.copy(
                            points = existing.points + StrokePoint(message.x, message.y, message.timestamp)
                        )
                        gameController?.updateStroke(updated)
                    }
                }
                is com.skidl.model.StrokeBatchMessage -> {
                    val existing = gameController?.round?.value?.strokes?.find { it.strokeId == message.strokeId }
                    if (existing != null) {
                        val newPoints = existing.points + message.pts.map {
                            StrokePoint(it[0], it[1], it.getOrNull(2)?.toLong() ?: System.currentTimeMillis())
                        }
                        gameController?.updateStroke(existing.copy(points = newPoints))
                    }
                }
                else -> {}
            }
            hostNetworking.broadcast(message)
        } else {
            sendClientMessage(message)
        }
    }

    fun onStrokeEnd(message: StrokeEndMessage) {
        if (_state.value.isHosting) {
            hostNetworking.broadcast(message)
        } else {
            sendClientMessage(message)
        }
    }

    private fun observeDiscovery() {
        applicationScope.launch {
            discoveryListener.hosts.collect { ad ->
                _state.update { state ->
                    val current = state.availableHosts.toMutableList()
                    val index = current.indexOfFirst { it.roomId == ad.roomId }
                    if (index >= 0) {
                        current[index] = ad
                    } else {
                        current.add(ad)
                    }
                    state.copy(availableHosts = current)
                }
            }
        }
    }

    private fun observeHostMessages() {
        applicationScope.launch {
            hostNetworking.incomingMessages.collect { (playerId, message) ->
                when (message) {
                    is JoinMessage -> handleJoin(playerId ?: message.playerId, message)
                    is ReadyMessage -> handleReadyMessage(playerId ?: message.playerId, message)
                    is com.skidl.model.GuessNetworkMessage -> handleGuess(playerId ?: message.playerId, message)
                    is StrokeStartMessage -> onStrokeStart(message)
                    is StrokePointMessage, is com.skidl.model.StrokeBatchMessage -> onStrokePoint(message)
                    is StrokeEndMessage -> onStrokeEnd(message)
                    is com.skidl.model.StrokeRemoveMessage -> handleStrokeRemove(message)
                    is com.skidl.model.CanvasClearMessage -> handleCanvasClear(message)
                    else -> {}
                }
            }
        }
    }

    private fun observeClientMessages() {
        applicationScope.launch {
            clientNetworking.incoming.collect { message ->
                when (message) {
                    is PlayerJoinedMessage -> handlePlayerJoined(message)
                    is com.skidl.model.LobbyUpdateMessage -> handleLobbyUpdate(message)
                    is RoundStartMessage -> handleRoundStart(message)
                    is SecretWordAssignedMessage -> handleSecretWord(message)
                    is StrokeStartMessage -> mirrorStrokeStart(message)
                    is StrokePointMessage -> mirrorStrokePoint(message)
                    is com.skidl.model.StrokeBatchMessage -> mirrorStrokeBatch(message)
                    is StrokeEndMessage -> mirrorStrokeEnd(message)
                    is com.skidl.model.CorrectGuessMessage -> handleCorrectGuess(message)
                    is com.skidl.model.TimerUpdateMessage -> handleTimerUpdate(message)
                    is com.skidl.model.GuessNetworkMessage -> mirrorGuess(message)
                    is com.skidl.model.StrokeRemoveMessage -> mirrorStrokeRemove(message)
                    is com.skidl.model.CanvasClearMessage -> mirrorCanvasClear(message)
                    is com.skidl.model.RoundEndMessage -> handleRoundEnd(message)
                    is com.skidl.model.GameEndMessage -> handleGameEnd(message)
                    is com.skidl.model.PlayerLeftMessage -> handlePlayerLeft(message)
                    else -> {}
                }
            }
        }
    }

    private fun observeDisconnects() {
        applicationScope.launch {
            hostNetworking.disconnects.collect { playerId ->
                handlePlayerDisconnect(playerId)
            }
        }
    }

    private fun handleJoin(playerId: String, message: JoinMessage) {
        val controller = gameController ?: return
        val lobby = controller.lobby.value
        // Max player limit
        if (lobby.players.size >= MAX_PLAYERS) {
            Log.w("SkidlViewModel", "Max player limit reached ($MAX_PLAYERS), rejecting $playerId")
            hostNetworking.kick(playerId)
            return
        }
        // Prevent duplicate player IDs
        if (lobby.players.any { it.playerId == playerId }) {
            Log.w("SkidlViewModel", "Duplicate playerId: $playerId")
            return
        }
        // Sanitize name
        val safeName = message.name.trim().take(MAX_NAME_LENGTH).ifBlank { "Player" }
        val newPlayer = Player(
            playerId = playerId,
            name = safeName,
            isHost = false,
            isReady = false
        )
        controller.updatePlayer(newPlayer)
        val updatedLobby = controller.lobby.value
        hostNetworking.broadcast(PlayerJoinedMessage(player = newPlayer))
        hostNetworking.broadcast(com.skidl.model.LobbyUpdateMessage(players = updatedLobby.players))
        discoveryBroadcaster.update(updatedLobby.settings.roomName, updatedLobby.players.size)
        _state.update { it.copy(lobbyState = controller.lobby.value) }
    }

    private fun handleReadyMessage(playerId: String, message: ReadyMessage) {
        gameController?.setReady(playerId, message.ready)
        gameController?.lobby?.value?.let { lobby ->
            hostNetworking.broadcast(com.skidl.model.LobbyUpdateMessage(players = lobby.players))
            _state.update { it.copy(lobbyState = lobby) }
        }
    }

    private fun handleGuess(playerId: String, guess: com.skidl.model.GuessNetworkMessage) {
        val round = gameController?.round?.value ?: return
        val lobby = gameController?.lobby?.value ?: return

        // Prevent drawer from guessing
        if (playerId == round.drawerId) return
        // Prevent guessing if player already guessed correctly
        if (playerId in correctGuessers) return

        // Rate-limit: max 3 guesses per 2 seconds per player
        val now = System.currentTimeMillis()
        val timestamps = guessTimestamps.getOrPut(playerId) { mutableListOf() }
        timestamps.removeAll { now - it > 2000 }
        if (timestamps.size >= 3) return
        timestamps.add(now)

        // Sanitize guess text
        val safeText = guess.text.trim().take(MAX_GUESS_LENGTH)
        if (safeText.isBlank()) return

        val isCorrect = currentSecretWord?.equals(safeText, ignoreCase = true) == true
        val guessMessage = GuessMessage(playerId, safeText, now, isCorrect)
        gameController?.appendGuess(guessMessage)
        hostNetworking.broadcast(com.skidl.model.GuessNetworkMessage(playerId = guess.playerId, text = safeText))

        if (isCorrect) {
            correctGuessers.add(playerId)
            val points = scoreManager.recordCorrectGuess(playerId, round.secondsRemaining)
            val player = lobby.players.firstOrNull { it.playerId == playerId }
            if (player != null) {
                val updated = scoreManager.updatePlayer(player)
                val updatedPlayers = lobby.players.map {
                    if (it.playerId == updated.playerId) updated else it
                }
                gameController?.setPlayers(updatedPlayers)
                hostNetworking.broadcast(com.skidl.model.CorrectGuessMessage(playerId = playerId, word = guess.text, points = points))
                val latestLobby = gameController?.lobby?.value
                if (latestLobby != null) {
                    discoveryBroadcaster.update(latestLobby.settings.roomName, latestLobby.players.size)
                    hostNetworking.broadcast(com.skidl.model.LobbyUpdateMessage(players = latestLobby.players))
                    _state.update { it.copy(lobbyState = latestLobby) }
                }
            }
        }

        val latestRound = gameController?.round?.value
        if (latestRound != null) {
            _state.update { it.copy(roundState = latestRound) }
        }
    }

    private fun handlePlayerJoined(message: PlayerJoinedMessage) {
        val lobby = state.value.lobbyState
        val updatedPlayers = (lobby?.players ?: emptyList()) + message.player
        _state.update {
            it.copy(lobbyState = lobby?.copy(players = updatedPlayers) ?: LobbyState(
                hostPlayerId = message.player.playerId,
                players = updatedPlayers
            ))
        }
    }

    private fun handleLobbyUpdate(message: com.skidl.model.LobbyUpdateMessage) {
        _state.update {
            val lobby = (it.lobbyState ?: LobbyState(hostPlayerId = message.players.firstOrNull()?.playerId ?: ""))
                .copy(players = message.players)
            it.copy(
                lobbyState = lobby,
                roundState = it.roundState?.copy(scoreboard = message.players)
            )
        }
    }

    private fun handleRoundStart(message: RoundStartMessage) {
        _state.update {
            it.copy(
                roundState = it.roundState?.copy(
                    drawerId = message.drawerId,
                    roundId = message.roundId,
                    secondsRemaining = message.timeLimit,
                    strokes = emptyList(),
                    guesses = emptyList(),
                    scoreboard = it.lobbyState?.players ?: emptyList()
                ) ?: com.skidl.model.RoundState(
                    drawerId = message.drawerId,
                    roundId = message.roundId,
                    secondsRemaining = message.timeLimit,
                    scoreboard = it.lobbyState?.players ?: emptyList()
                ),
                screen = SkidlScreen.Game,
                secretWordForDrawer = null,
                lastRevealedWord = null,
                gameOver = false
            )
        }
    }

    private fun mirrorStrokeStart(message: StrokeStartMessage) {
        val stroke = Stroke(
            strokeId = message.strokeId,
            playerId = message.playerId,
            color = message.color,
            thickness = message.thickness,
            points = listOf(StrokePoint(message.x, message.y, message.timestamp))
        )
        _state.update { it.copy(roundState = it.roundState?.copy(strokes = (it.roundState?.strokes ?: emptyList()) + stroke)) }
    }

    private fun mirrorStrokePoint(message: StrokePointMessage) {
        val round = _state.value.roundState ?: return
        val strokes = round.strokes.toMutableList()
        val index = strokes.indexOfFirst { it.strokeId == message.strokeId }
        if (index >= 0) {
            val stroke = strokes[index]
            strokes[index] = stroke.copy(points = stroke.points + StrokePoint(message.x, message.y, message.timestamp))
            _state.update { it.copy(roundState = round.copy(strokes = strokes)) }
        }
    }

    private fun mirrorStrokeBatch(message: com.skidl.model.StrokeBatchMessage) {
        val round = _state.value.roundState ?: return
        val strokes = round.strokes.toMutableList()
        val index = strokes.indexOfFirst { it.strokeId == message.strokeId }
        if (index >= 0) {
            val stroke = strokes[index]
            val newPoints = stroke.points + message.pts.map { StrokePoint(it[0], it[1], it.getOrNull(2)?.toLong() ?: System.currentTimeMillis()) }
            strokes[index] = stroke.copy(points = newPoints)
            _state.update { it.copy(roundState = round.copy(strokes = strokes)) }
        }
    }

    private fun mirrorStrokeEnd(message: StrokeEndMessage) {
        // Currently end message does not change state; reserved for undo logic.
    }

    private fun handleStrokeRemove(message: com.skidl.model.StrokeRemoveMessage) {
        val controller = gameController ?: return
        controller.removeStroke(message.strokeId)
        hostNetworking.broadcast(message)
        val updatedRound = controller.round.value
        _state.update { it.copy(roundState = updatedRound) }
    }

    private fun handleCanvasClear(message: com.skidl.model.CanvasClearMessage) {
        val controller = gameController ?: return
        controller.clearStrokes()
        hostNetworking.broadcast(message)
        val updatedRound = controller.round.value
        _state.update { it.copy(roundState = updatedRound) }
    }

    private fun handleCorrectGuess(message: com.skidl.model.CorrectGuessMessage) {
        val lobby = _state.value.lobbyState ?: return
        val updated = lobby.players.map {
            if (it.playerId == message.playerId) it.copy(score = message.points) else it
        }
        val updatedRound = _state.value.roundState?.copy(
            guesses = _state.value.roundState?.guesses?.map {
                if (it.playerId == message.playerId && it.text.equals(message.word, ignoreCase = true)) {
                    it.copy(isCorrect = true)
                } else {
                    it
                }
            } ?: emptyList()
        )
        _state.update {
            it.copy(
                lobbyState = lobby.copy(players = updated),
                roundState = updatedRound ?: it.roundState
            )
        }
    }

    private fun mirrorGuess(message: com.skidl.model.GuessNetworkMessage) {
        val round = _state.value.roundState ?: return
        val guess = GuessMessage(playerId = message.playerId, text = message.text, timestamp = System.currentTimeMillis(), isCorrect = false)
        _state.update {
            it.copy(roundState = round.copy(guesses = round.guesses + guess))
        }
    }

    private fun handleTimerUpdate(message: com.skidl.model.TimerUpdateMessage) {
        val round = _state.value.roundState ?: return
        _state.update { it.copy(roundState = round.copy(secondsRemaining = message.secondsRemaining)) }
    }

    private fun mirrorStrokeRemove(message: com.skidl.model.StrokeRemoveMessage) {
        val round = _state.value.roundState ?: return
        val updatedStrokes = round.strokes.filterNot { it.strokeId == message.strokeId }
        _state.update { it.copy(roundState = round.copy(strokes = updatedStrokes)) }
    }

    private fun mirrorCanvasClear(message: com.skidl.model.CanvasClearMessage) {
        val round = _state.value.roundState ?: return
        _state.update { it.copy(roundState = round.copy(strokes = emptyList())) }
    }

    private fun handlePlayerDisconnect(playerId: String) {
        val controller = gameController ?: return
        val player = controller.lobby.value.players.firstOrNull { it.playerId == playerId } ?: return
        val updatedPlayers = controller.lobby.value.players.filterNot { it.playerId == playerId }
        controller.setPlayers(updatedPlayers)
        val lobby = controller.lobby.value
        hostNetworking.broadcast(com.skidl.model.PlayerLeftMessage(playerId = playerId, name = player.name))
        hostNetworking.broadcast(com.skidl.model.LobbyUpdateMessage(players = lobby.players))
        discoveryBroadcaster.update(lobby.settings.roomName, lobby.players.size)
        _state.update {
            it.copy(lobbyState = lobby)
        }
        showError("${player.name} disconnected")
    }

    private fun handlePlayerLeft(message: com.skidl.model.PlayerLeftMessage) {
        _state.update {
            val lobby = it.lobbyState?.copy(
                players = it.lobbyState.players.filterNot { p -> p.playerId == message.playerId }
            )
            it.copy(lobbyState = lobby)
        }
        showError("${message.name} disconnected")
    }

    private fun handleRoundEnd(message: com.skidl.model.RoundEndMessage) {
        _state.update {
            val updatedRound = it.roundState?.copy(scoreboard = message.scores)
            it.copy(
                screen = SkidlScreen.Scores,
                roundState = updatedRound,
                lastRevealedWord = message.word,
                secretWordForDrawer = null,
                lobbyState = it.lobbyState?.copy(players = message.scores)
            )
        }
    }

    private fun handleGameEnd(message: com.skidl.model.GameEndMessage) {
        _state.update {
            it.copy(
                screen = SkidlScreen.Scores,
                lobbyState = it.lobbyState?.copy(players = message.scores),
                roundState = it.roundState?.copy(scoreboard = message.scores),
                gameOver = true,
                secretWordForDrawer = null,
                lastRevealedWord = null
            )
        }
    }

    private fun handleSecretWord(message: SecretWordAssignedMessage) {
        // For the drawer, the "hash" field now contains the actual word
        _state.update {
            it.copy(
                roundState = (it.roundState ?: com.skidl.model.RoundState()).copy(secretWordHash = message.hash),
                secretWordForDrawer = if (it.playerId == message.drawerId) message.hash else null
            )
        }
    }

    fun hostStartRound() {
        viewModelScope.launch {
            val controller = gameController ?: return@launch
            currentRoundNumber++
            val drawerId = pickNextDrawer()
            val word = wordBank.nextWord()
            currentSecretWord = word
            guessTimestamps.clear()
            correctGuessers.clear()
            controller.startRound(drawerId, word, controller.lobby.value.settings.roundTimeSeconds)
            scoreManager.getScores(controller.lobby.value.players)
            val round = controller.round.value
            hostNetworking.broadcast(RoundStartMessage(drawerId = drawerId, roundId = round.roundId ?: "", timeLimit = round.secondsRemaining))
            hostNetworking.sendTo(drawerId, SecretWordAssignedMessage(drawerId = drawerId, hash = word))
            _state.update {
                it.copy(
                    roundState = round,
                    screen = SkidlScreen.Game,
                    secretWordForDrawer = if (it.playerId == drawerId) word else null,
                    currentRound = currentRoundNumber,
                    totalRounds = totalRounds
                )
            }
            roundTimerJob?.cancel()
            roundTimerJob = viewModelScope.launch {
                var remaining = round.secondsRemaining
                while (remaining > 0) {
                    kotlinx.coroutines.delay(1000)
                    remaining -= 1
                    controller.updateTimer(remaining)
                    hostNetworking.broadcast(com.skidl.model.TimerUpdateMessage(secondsRemaining = remaining))
                    val updatedRound = controller.round.value
                    _state.update { it.copy(roundState = updatedRound) }
                }
                // Round ended: broadcast round-end to all clients
                val finalScores = scoreManager.getScores(controller.lobby.value.players)
                controller.updateScores(finalScores)
                val revealedWord = currentSecretWord ?: "???"
                hostNetworking.broadcast(com.skidl.model.RoundEndMessage(word = revealedWord, scores = finalScores))
                currentSecretWord = null
                _state.update {
                    it.copy(
                        screen = SkidlScreen.Scores,
                        roundState = controller.round.value,
                        secretWordForDrawer = null,
                        lastRevealedWord = revealedWord
                    )
                }
            }
        }
    }

    fun hostNextRound() {
        if (currentRoundNumber >= totalRounds) {
            endGame()
        } else {
            hostStartRound()
        }
    }

    fun backToLobby() {
        roundTimerJob?.cancel()
        currentSecretWord = null
        currentRoundNumber = 0
        scoreManager.reset()
        guessTimestamps.clear()
        correctGuessers.clear()
        // Broadcast lobby update so clients navigate back
        if (_state.value.isHosting) {
            gameController?.lobby?.value?.let { lobby ->
                hostNetworking.broadcast(com.skidl.model.LobbyUpdateMessage(players = lobby.players))
            }
        }
        _state.update {
            it.copy(
                screen = SkidlScreen.Lobby,
                roundState = null,
                secretWordForDrawer = null,
                lastRevealedWord = null,
                currentRound = 0,
                totalRounds = totalRounds
            )
        }
    }

    private fun endGame() {
        val controller = gameController ?: return
        val finalScores = scoreManager.getScores(controller.lobby.value.players)
        controller.updateScores(finalScores)
        hostNetworking.broadcast(com.skidl.model.GameEndMessage(scores = finalScores))
        currentSecretWord = null
        _state.update {
            it.copy(
                screen = SkidlScreen.Scores,
                roundState = controller.round.value,
                secretWordForDrawer = null,
                lastRevealedWord = null,
                gameOver = true
            )
        }
    }

    private fun pickNextDrawer(): String {
        val lobby = gameController?.lobby?.value ?: return state.value.playerId
        val ordered = lobby.players
        val currentDrawer = gameController?.round?.value?.drawerId
        if (currentDrawer == null) return ordered.firstOrNull()?.playerId ?: state.value.playerId
        val idx = ordered.indexOfFirst { it.playerId == currentDrawer }
        return if (idx == -1 || idx + 1 >= ordered.size) ordered.first().playerId else ordered[idx + 1].playerId
    }

    private fun sendClientMessage(message: SkidlMessage) {
        clientNetworking.send(message)
    }

    private fun showError(message: String) {
        _state.update { it.copy(error = message) }
        errorDismissJob?.cancel()
        errorDismissJob = viewModelScope.launch {
            delay(ERROR_DISMISS_MS)
            _state.update { it.copy(error = null) }
        }
    }

    fun clearError() {
        errorDismissJob?.cancel()
        _state.update { it.copy(error = null) }
    }

    private fun resolveLocalIp(): String {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (networkInterface in interfaces) {
            val addresses = Collections.list(networkInterface.inetAddresses)
            for (address in addresses) {
                if (!address.isLoopbackAddress && address.hostAddress.indexOf(':') < 0) {
                    return address.hostAddress
                }
            }
        }
        return "0.0.0.0"
    }
}

data class SkidlUiState(
    val screen: SkidlScreen = SkidlScreen.Welcome,
    val isHosting: Boolean = false,
    val isDiscovering: Boolean = false,
    val availableHosts: List<HostAdvertisement> = emptyList(),
    val lobbyState: LobbyState? = null,
    val roundState: com.skidl.model.RoundState? = null,
    val playerId: String = "",
    val displayName: String = "",
    val roomCode: String? = null,
    val roomName: String = "Hotspot Skribble",
    val manualIp: String = "",
    val hostAddress: String = "",
    val error: String? = null,
    val secretWordForDrawer: String? = null,
    val lastRevealedWord: String? = null,
    val currentRound: Int = 0,
    val totalRounds: Int = 3,
    val gameOver: Boolean = false
)

enum class SkidlScreen {
    Welcome,
    HostSetup,
    Join,
    Lobby,
    Game,
    Scores
}

