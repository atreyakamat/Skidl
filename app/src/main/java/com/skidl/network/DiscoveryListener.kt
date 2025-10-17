package com.skidl.network

import android.util.Log
import com.skidl.model.HostAdvertisement
import com.skidl.util.NetworkDefaults
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecodingException

class DiscoveryListener(
    private val scope: CoroutineScope,
    private val json: Json
) {
    private val _hosts = MutableSharedFlow<HostAdvertisement>(
        replay = 1,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val hosts: SharedFlow<HostAdvertisement> = _hosts

    private var job: Job? = null
    private var socket: DatagramSocket? = null

    fun start() {
        if (job != null) return
        job = scope.launch(Dispatchers.IO) {
            DatagramSocket(NetworkDefaults.UDP_PORT).use { udpSocket ->
                socket = udpSocket
                udpSocket.broadcast = true
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                while (true) {
                    try {
                        udpSocket.receive(packet)
                        val message = String(packet.data, 0, packet.length)
                        val ad = json.decodeFromString(HostAdvertisement.serializer(), message)
                        _hosts.emit(ad)
                    } catch (ex: JsonDecodingException) {
                        Log.w("DiscoveryListener", "Invalid broadcast: ${ex.localizedMessage}")
                    } catch (t: Throwable) {
                        Log.e("DiscoveryListener", "Discovery failed", t)
                        break
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        socket?.close()
        socket = null
    }
}
