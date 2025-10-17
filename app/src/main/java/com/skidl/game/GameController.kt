package com.skidl.game

import com.skidl.model.GameSettings
import com.skidl.model.GuessMessage
import com.skidl.model.LobbyState
import com.skidl.model.Player
import com.skidl.model.RoundState
import com.skidl.model.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.max

class GameController(
    hostPlayerId: String,
    initialRoomName: String
) {
    private val _lobby = MutableStateFlow(
        LobbyState(
            hostPlayerId = hostPlayerId,
            settings = GameSettings(roomName = initialRoomName)
        )
    )
    val lobby: StateFlow<LobbyState> = _lobby

    private val _round = MutableStateFlow(RoundState())
    val round: StateFlow<RoundState> = _round

    fun setPlayers(players: List<Player>) {
        _lobby.value = _lobby.value.copy(players = players)
    }

    fun updatePlayer(player: Player) {
        val players = _lobby.value.players.toMutableList()
        val index = players.indexOfFirst { it.playerId == player.playerId }
        if (index >= 0) {
            players[index] = player
        } else {
            players.add(player)
        }
        _lobby.value = _lobby.value.copy(players = players)
    }

    fun setReady(playerId: String, ready: Boolean) {
        val updated = _lobby.value.players.map {
            if (it.playerId == playerId) it.copy(isReady = ready) else it
        }
        _lobby.value = _lobby.value.copy(players = updated)
    }

    fun startRound(drawerId: String, word: String, timeLimit: Int) {
        val roundId = UUID.randomUUID().toString()
        val hash = hashWord(word)
        _round.value = RoundState(
            drawerId = drawerId,
            roundId = roundId,
            secretWordHash = hash,
            secondsRemaining = timeLimit,
            strokes = emptyList(),
            guesses = emptyList(),
            scoreboard = _lobby.value.players
        )
    }

    fun updateTimer(secondsRemaining: Int) {
        _round.value = _round.value.copy(secondsRemaining = max(secondsRemaining, 0))
    }

    fun appendStroke(stroke: Stroke) {
        val strokes = _round.value.strokes.toMutableList().apply { add(stroke) }
        _round.value = _round.value.copy(strokes = strokes)
    }

    fun updateStroke(stroke: Stroke) {
        val strokes = _round.value.strokes.toMutableList()
        val index = strokes.indexOfFirst { it.strokeId == stroke.strokeId }
        if (index >= 0) {
            strokes[index] = stroke
            _round.value = _round.value.copy(strokes = strokes)
        }
    }

    fun removeStroke(strokeId: String) {
        val strokes = _round.value.strokes.filterNot { it.strokeId == strokeId }
        _round.value = _round.value.copy(strokes = strokes)
    }

    fun clearStrokes() {
        _round.value = _round.value.copy(strokes = emptyList())
    }

    fun appendGuess(guess: GuessMessage) {
        val guesses = _round.value.guesses.toMutableList().apply { add(guess) }
        _round.value = _round.value.copy(guesses = guesses)
    }

    fun updateScores(scores: List<Player>) {
        _round.value = _round.value.copy(scoreboard = scores)
        _lobby.value = _lobby.value.copy(players = scores)
    }

    fun updateSettings(settings: GameSettings) {
        _lobby.value = _lobby.value.copy(settings = settings)
    }

    private fun hashWord(word: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(word.encodeToByteArray())
        return hash.joinToString("") { String.format("%02x", it) }
    }
}
