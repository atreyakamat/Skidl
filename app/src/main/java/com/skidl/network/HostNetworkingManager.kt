package com.skidl.network

import android.util.Log
import com.skidl.model.JoinMessage
import com.skidl.model.SkidlMessage
import com.skidl.util.NetworkDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.util.UUID

class HostNetworkingManager(
    private val scope: CoroutineScope,
    private val json: Json
) {
    private val _incomingMessages = MutableSharedFlow<Pair<String?, SkidlMessage>>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingMessages: SharedFlow<Pair<String?, SkidlMessage>> = _incomingMessages

    private val connections = mutableMapOf<String, WebSocket>()
    private var server: SkidlHostServer? = null

    fun start() {
        if (server != null) return
        val socket = SkidlHostServer(NetworkDefaults.WEBSOCKET_PORT)
        server = socket
        socket.start()
    }

    fun stop() {
        server?.stop(1000)
        server = null
        connections.clear()
    }

    fun broadcast(message: SkidlMessage) {
        val jsonString = json.encodeToString(message) + "\n"
        server?.broadcast(jsonString)
    }

    fun sendTo(playerId: String, message: SkidlMessage) {
        val connection = connections[playerId] ?: return
        val jsonString = json.encodeToString(message) + "\n"
        connection.send(jsonString)
    }

    private inner class SkidlHostServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {
        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
            Log.d("HostNetworking", "Client connected: ${conn.remoteSocketAddress}")
        }

        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
            val playerId = connections.entries.firstOrNull { it.value == conn }?.key
            if (playerId != null) {
                connections.remove(playerId)
            }
            Log.d("HostNetworking", "Client disconnected: ${playerId ?: conn.remoteSocketAddress}")
        }

        override fun onMessage(conn: WebSocket, message: String) {
            try {
                val trimmed = message.trim()
                if (trimmed.isEmpty()) return
                var skidlMessage = json.decodeFromString(SkidlMessageSerializer, trimmed)
                if (skidlMessage is JoinMessage) {
                    val clientId = if (skidlMessage.playerId.isNotBlank()) skidlMessage.playerId else UUID.randomUUID().toString()
                    connections[clientId] = conn
                    if (clientId != skidlMessage.playerId) {
                        skidlMessage = skidlMessage.copy(playerId = clientId)
                    }
                }
                scope.launch {
                    _incomingMessages.emit(connections.entries.firstOrNull { it.value == conn }?.key to skidlMessage)
                }
            } catch (ex: SerializationException) {
                Log.e("HostNetworking", "Failed to parse message", ex)
            }
        }

        override fun onError(conn: WebSocket?, ex: Exception) {
            Log.e("HostNetworking", "Server error", ex)
        }

        override fun onStart() {
            Log.i("HostNetworking", "Host server started on port ${NetworkDefaults.WEBSOCKET_PORT}")
        }
    }
}
