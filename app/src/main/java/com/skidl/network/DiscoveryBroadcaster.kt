package com.skidl.network

import android.util.Log
import com.skidl.model.HostAdvertisement
import com.skidl.util.NetworkDefaults
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DiscoveryBroadcaster(
    private val scope: CoroutineScope,
    private val roomId: String,
    private val json: Json,
    private val ipProvider: () -> String
) {
    private var job: Job? = null
    @Volatile private var lastRoomName: String = ""
    @Volatile private var lastPlayerCount: Int = 0
    @Volatile private var lastPort: Int = NetworkDefaults.WEBSOCKET_PORT

    fun start(roomName: String, playerCount: Int, port: Int = NetworkDefaults.WEBSOCKET_PORT) {
        lastRoomName = roomName
        lastPlayerCount = playerCount
        lastPort = port
        if (job != null) return
        job = scope.launch(Dispatchers.IO) {
            val socket = DatagramSocket().apply { broadcast = true }
            val broadcastAddress = InetAddress.getByName("255.255.255.255")
            try {
                while (isActive) {
                    val payload = HostAdvertisement(
                        roomName = lastRoomName,
                        ip = ipProvider(),
                        port = lastPort,
                        roomId = roomId,
                        players = lastPlayerCount
                    )
                    val message = json.encodeToString(payload) + "\n"
                    val buffer = message.toByteArray()
                    val packet = DatagramPacket(buffer, buffer.size, broadcastAddress, NetworkDefaults.UDP_PORT)
                    socket.send(packet)
                    delay(NetworkDefaults.BROADCAST_INTERVAL_MS)
                }
            } catch (t: Throwable) {
                Log.e("DiscoveryBroadcaster", "Broadcast failed", t)
            } finally {
                socket.close()
            }
        }
    }

    fun update(roomName: String, playerCount: Int) {
        lastRoomName = roomName
        lastPlayerCount = playerCount
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
