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
import java.util.concurrent.ConcurrentHashMap

class HostNetworkingManager(
    private val scope: CoroutineScope,
    private val json: Json
) {
    companion object {
        private const val MAX_MESSAGE_SIZE = 16_384 // 16KB max message
        private const val MAX_CONNECTIONS = 16
    }

    private val _incomingMessages = MutableSharedFlow<Pair<String?, SkidlMessage>>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingMessages: SharedFlow<Pair<String?, SkidlMessage>> = _incomingMessages

    private val _disconnects = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val disconnects: SharedFlow<String> = _disconnects

    private val connections = ConcurrentHashMap<String, WebSocket>()
    private var server: SkidlHostServer? = null

    fun start(): Boolean {
        if (server != null) return true
        return try {
            val socket = SkidlHostServer(NetworkDefaults.WEBSOCKET_PORT)
            server = socket
            socket.start()
            true
        } catch (e: Exception) {
            Log.e("HostNetworking", "Failed to start server on port ${NetworkDefaults.WEBSOCKET_PORT}", e)
            server = null
            false
        }
    }

    fun stop() {
        server?.stop(1000)
        server = null
        connections.clear()
    }

    fun broadcast(message: SkidlMessage) {
        try {
            val jsonString = json.encodeToString(message) + "\n"
            server?.broadcast(jsonString)
        } catch (e: Exception) {
            Log.e("HostNetworking", "Broadcast failed", e)
        }
    }

    fun sendTo(playerId: String, message: SkidlMessage) {
        val connection = connections[playerId] ?: return
        try {
            val jsonString = json.encodeToString(message) + "\n"
            if (connection.isOpen) {
                connection.send(jsonString)
            }
        } catch (e: Exception) {
            Log.e("HostNetworking", "Send to $playerId failed", e)
        }
    }

    fun kick(playerId: String) {
        val conn = connections.remove(playerId)
        conn?.close(1000, "kicked")
    }

    val playerCount: Int get() = connections.size

    private inner class SkidlHostServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {
        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
            if (connections.size >= MAX_CONNECTIONS) {
                Log.w("HostNetworking", "Connection limit reached ($MAX_CONNECTIONS), rejecting ${conn.remoteSocketAddress}")
                conn.close(1008, "Server full")
                return
            }
            Log.d("HostNetworking", "Client connected: ${conn.remoteSocketAddress}")
        }

        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
            val playerId = connections.entries.firstOrNull { it.value == conn }?.key
            if (playerId != null) {
                connections.remove(playerId)
                scope.launch { _disconnects.emit(playerId) }
            }
            Log.d("HostNetworking", "Client disconnected: ${playerId ?: conn.remoteSocketAddress}")
        }

        override fun onMessage(conn: WebSocket, message: String) {
            try {
                val trimmed = message.trim()
                if (trimmed.isEmpty()) return
                if (trimmed.length > MAX_MESSAGE_SIZE) {
                    Log.w("HostNetworking", "Oversized message rejected (${trimmed.length} bytes)")
                    return
                }
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
            Log.e("HostNetworking", "Server error (conn=${conn?.remoteSocketAddress})", ex)
            // If it's a connection-level error, clean up that connection
            if (conn != null) {
                val playerId = connections.entries.firstOrNull { it.value == conn }?.key
                if (playerId != null) {
                    connections.remove(playerId)
                    scope.launch { _disconnects.emit(playerId) }
                }
            }
        }

        override fun onStart() {
            Log.i("HostNetworking", "Host server started on port ${NetworkDefaults.WEBSOCKET_PORT}")
        }
    }
}
