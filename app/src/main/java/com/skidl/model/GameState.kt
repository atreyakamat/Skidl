package com.skidl.model

import kotlinx.serialization.Serializable

@Serializable
data class GameSettings(
    val roundTimeSeconds: Int = 90,
    val roomCode: String? = null,
    val roomName: String = "Skidl Room"
)

@Serializable
data class LobbyState(
    val hostPlayerId: String,
    val players: List<Player> = emptyList(),
    val settings: GameSettings = GameSettings(),
    val isHostReady: Boolean = false
)

@Serializable
data class RoundState(
    val drawerId: String? = null,
    val roundId: String? = null,
    val secretWordHash: String? = null,
    val secondsRemaining: Int = 0,
    val strokes: List<Stroke> = emptyList(),
    val guesses: List<GuessMessage> = emptyList(),
    val scoreboard: List<Player> = emptyList()
)

@Serializable
data class GuessMessage(
    val playerId: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isCorrect: Boolean = false
)
