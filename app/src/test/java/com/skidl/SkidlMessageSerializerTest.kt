package com.skidl

import com.skidl.model.JoinMessage
import com.skidl.model.ReadyMessage
import com.skidl.model.StrokePointMessage
import com.skidl.model.StrokeStartMessage
import com.skidl.network.SkidlMessageSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkidlMessageSerializerTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `decode join message`() {
        val original = JoinMessage(playerId = "p1", name = "Test")
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is JoinMessage)
        decoded as JoinMessage
        assertEquals("p1", decoded.playerId)
        assertEquals("Test", decoded.name)
    }

    @Test
    fun `decode stroke point message`() {
        val original = StrokePointMessage(strokeId = "s1", x = 10f, y = 20f, timestamp = 1L)
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is StrokePointMessage)
    }

    @Test
    fun `decode ready message`() {
        val original = ReadyMessage(playerId = "p1", ready = true)
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is ReadyMessage)
    }
}
