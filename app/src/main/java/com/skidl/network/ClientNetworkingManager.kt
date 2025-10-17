package com.skidl.network

import android.util.Log
import com.skidl.model.SkidlMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class ClientNetworkingManager(
    private val scope: CoroutineScope,
    private val json: Json,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _incoming = MutableSharedFlow<SkidlMessage>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incoming: SharedFlow<SkidlMessage> = _incoming

    private var webSocket: WebSocket? = null

    fun connect(url: String) {
        if (webSocket != null) return
        _connectionState.value = ConnectionState.Connecting
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, SkidlClientListener())
    }

    fun disconnect() {
        webSocket?.close(1000, "client closed")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun send(message: SkidlMessage) {
        val ws = webSocket ?: return
        val jsonString = json.encodeToString(message) + "\n"
        ws.send(jsonString)
    }

    private inner class SkidlClientListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _connectionState.value = ConnectionState.Connected
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            _connectionState.value = ConnectionState.Disconnected
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                if (text.isBlank()) return
                val trimmed = text.trim()
                val message = json.decodeFromString(SkidlMessageSerializer, trimmed)
                scope.launch { _incoming.emit(message) }
            } catch (ex: SerializationException) {
                Log.e("ClientNetworking", "Failed to parse message", ex)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            onMessage(webSocket, bytes.utf8())
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connectionState.value = ConnectionState.Disconnected
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e("ClientNetworking", "WebSocket failure", t)
            _connectionState.value = ConnectionState.Disconnected
        }
    }
}

enum class ConnectionState {
    Connecting,
    Connected,
    Disconnected
}
