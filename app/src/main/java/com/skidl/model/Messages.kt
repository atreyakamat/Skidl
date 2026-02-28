package com.skidl.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SkidlMessage {
    val type: String
}

@Serializable
@SerialName("host_ad")
data class HostAdvertisement(
    override val type: String = "host_ad",
    val roomName: String,
    val ip: String,
    val port: Int,
    val roomId: String,
    val players: Int = 0
) : SkidlMessage

@Serializable
@SerialName("join")
data class JoinMessage(
    override val type: String = "join",
    val playerId: String,
    val name: String,
    val roomCode: String? = null
) : SkidlMessage

@Serializable
@SerialName("ready")
data class ReadyMessage(
    override val type: String = "ready",
    val playerId: String,
    val ready: Boolean
) : SkidlMessage

@Serializable
@SerialName("stroke_start")
data class StrokeStartMessage(
    override val type: String = "stroke_start",
    val strokeId: String,
    val playerId: String,
    val color: String,
    val thickness: Float,
    val x: Float,
    val y: Float,
    val timestamp: Long
) : SkidlMessage

@Serializable
@SerialName("stroke_point")
data class StrokePointMessage(
    override val type: String = "stroke_point",
    val strokeId: String,
    val x: Float,
    val y: Float,
    val timestamp: Long
) : SkidlMessage

@Serializable
@SerialName("stroke_remove")
data class StrokeRemoveMessage(
    override val type: String = "stroke_remove",
    val strokeId: String
) : SkidlMessage

@Serializable
@SerialName("stroke_points")
data class StrokeBatchMessage(
    override val type: String = "stroke_points",
    val strokeId: String,
    val pts: List<List<Float>>
) : SkidlMessage

@Serializable
@SerialName("stroke_end")
data class StrokeEndMessage(
    override val type: String = "stroke_end",
    val strokeId: String
) : SkidlMessage

@Serializable
@SerialName("guess")
data class GuessNetworkMessage(
    override val type: String = "guess",
    val playerId: String,
    val text: String
) : SkidlMessage

@Serializable
@SerialName("player_joined")
data class PlayerJoinedMessage(
    override val type: String = "player_joined",
    val player: Player
) : SkidlMessage

@Serializable
@SerialName("lobby_update")
data class LobbyUpdateMessage(
    override val type: String = "lobby_update",
    val players: List<Player>
) : SkidlMessage

@Serializable
@SerialName("round_start")
data class RoundStartMessage(
    override val type: String = "round_start",
    val drawerId: String,
    val roundId: String,
    val timeLimit: Int
) : SkidlMessage

@Serializable
@SerialName("secret_word_assigned")
data class SecretWordAssignedMessage(
    override val type: String = "secret_word_assigned",
    val drawerId: String,
    val hash: String
) : SkidlMessage

@Serializable
@SerialName("correct_guess")
data class CorrectGuessMessage(
    override val type: String = "correct_guess",
    val playerId: String,
    val word: String,
    val points: Int
) : SkidlMessage

@Serializable
@SerialName("heartbeat")
data class HeartbeatMessage(
    override val type: String = "heartbeat",
    val time: Long
) : SkidlMessage

@Serializable
@SerialName("timer_update")
data class TimerUpdateMessage(
     override val type: String = "timer_update",
     val secondsRemaining: Int
) : SkidlMessage

@Serializable
@SerialName("canvas_clear")
data class CanvasClearMessage(
    override val type: String = "canvas_clear"
) : SkidlMessage

@Serializable
@SerialName("player_left")
data class PlayerLeftMessage(
    override val type: String = "player_left",
    val playerId: String,
    val name: String
) : SkidlMessage

@Serializable
@SerialName("round_end")
data class RoundEndMessage(
    override val type: String = "round_end",
    val word: String,
    val scores: List<Player>
) : SkidlMessage

@Serializable
@SerialName("game_end")
data class GameEndMessage(
    override val type: String = "game_end",
    val scores: List<Player>
) : SkidlMessage
