package com.skidl.network

import com.skidl.model.CorrectGuessMessage
import com.skidl.model.CanvasClearMessage
import com.skidl.model.GuessNetworkMessage
import com.skidl.model.HeartbeatMessage
import com.skidl.model.HostAdvertisement
import com.skidl.model.JoinMessage
import com.skidl.model.LobbyUpdateMessage
import com.skidl.model.PlayerJoinedMessage
import com.skidl.model.ReadyMessage
import com.skidl.model.RoundStartMessage
import com.skidl.model.SecretWordAssignedMessage
import com.skidl.model.SkidlMessage
import com.skidl.model.StrokeBatchMessage
import com.skidl.model.StrokeEndMessage
import com.skidl.model.StrokePointMessage
import com.skidl.model.StrokeStartMessage
import com.skidl.model.StrokeRemoveMessage
import com.skidl.model.TimerUpdateMessage
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object SkidlMessageSerializer : JsonContentPolymorphicSerializer<SkidlMessage>(SkidlMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out SkidlMessage> {
        val type = element.jsonObject["type"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Message missing type")
        return when (type) {
            "host_ad" -> HostAdvertisement.serializer()
            "join" -> JoinMessage.serializer()
            "ready" -> ReadyMessage.serializer()
            "stroke_start" -> StrokeStartMessage.serializer()
            "stroke_point" -> StrokePointMessage.serializer()
            "stroke_points" -> StrokeBatchMessage.serializer()
            "stroke_end" -> StrokeEndMessage.serializer()
            "stroke_remove" -> StrokeRemoveMessage.serializer()
            "guess" -> GuessNetworkMessage.serializer()
            "player_joined" -> PlayerJoinedMessage.serializer()
            "lobby_update" -> LobbyUpdateMessage.serializer()
            "round_start" -> RoundStartMessage.serializer()
            "secret_word_assigned" -> SecretWordAssignedMessage.serializer()
            "correct_guess" -> CorrectGuessMessage.serializer()
            "heartbeat" -> HeartbeatMessage.serializer()
            "timer_update" -> TimerUpdateMessage.serializer()
            "canvas_clear" -> CanvasClearMessage.serializer()
            else -> throw IllegalArgumentException("Unsupported message type $type")
        }
    }
}
