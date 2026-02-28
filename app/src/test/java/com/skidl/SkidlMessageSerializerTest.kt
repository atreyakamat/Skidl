package com.skidl

import com.skidl.model.*
import com.skidl.network.SkidlMessageSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkidlMessageSerializerTest {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "#class"
    }

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

    @Test
    fun `decode host advertisement`() {
        val original = HostAdvertisement(roomName = "Room1", ip = "192.168.1.1", port = 50000, roomId = "r1", players = 3)
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is HostAdvertisement)
        decoded as HostAdvertisement
        assertEquals("Room1", decoded.roomName)
        assertEquals(3, decoded.players)
    }

    @Test
    fun `decode stroke start message`() {
        val original = StrokeStartMessage(strokeId = "s1", playerId = "p1", color = "#FF0000", thickness = 5f, x = 1f, y = 2f, timestamp = 100L)
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is StrokeStartMessage)
        decoded as StrokeStartMessage
        assertEquals("#FF0000", decoded.color)
        assertEquals(5f, decoded.thickness)
    }

    @Test
    fun `decode stroke batch message`() {
        val original = StrokeBatchMessage(strokeId = "s1", pts = listOf(listOf(1f, 2f, 3f), listOf(4f, 5f, 6f)))
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is StrokeBatchMessage)
        decoded as StrokeBatchMessage
        assertEquals(2, decoded.pts.size)
    }

    @Test
    fun `decode stroke end message`() {
        val original = StrokeEndMessage(strokeId = "s1")
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is StrokeEndMessage)
    }

    @Test
    fun `decode stroke remove message`() {
        val original = StrokeRemoveMessage(strokeId = "s1")
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is StrokeRemoveMessage)
    }

    @Test
    fun `decode guess network message`() {
        val original = GuessNetworkMessage(playerId = "p1", text = "apple")
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is GuessNetworkMessage)
        decoded as GuessNetworkMessage
        assertEquals("apple", decoded.text)
    }

    @Test
    fun `decode player joined message`() {
        val player = Player(playerId = "p1", name = "Alice", isHost = false)
        val original = PlayerJoinedMessage(player = player)
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is PlayerJoinedMessage)
        decoded as PlayerJoinedMessage
        assertEquals("Alice", decoded.player.name)
    }

    @Test
    fun `decode lobby update message`() {
        val players = listOf(Player(playerId = "p1", name = "Alice"), Player(playerId = "p2", name = "Bob"))
        val original = LobbyUpdateMessage(players = players)
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is LobbyUpdateMessage)
        decoded as LobbyUpdateMessage
        assertEquals(2, decoded.players.size)
    }

    @Test
    fun `decode round start message`() {
        val original = RoundStartMessage(drawerId = "p1", roundId = "r1", timeLimit = 90)
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is RoundStartMessage)
        decoded as RoundStartMessage
        assertEquals(90, decoded.timeLimit)
    }

    @Test
    fun `decode secret word assigned message`() {
        val original = SecretWordAssignedMessage(drawerId = "p1", hash = "abc123")
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is SecretWordAssignedMessage)
    }

    @Test
    fun `decode correct guess message`() {
        val original = CorrectGuessMessage(playerId = "p1", word = "apple", points = 130)
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is CorrectGuessMessage)
        decoded as CorrectGuessMessage
        assertEquals(130, decoded.points)
    }

    @Test
    fun `decode heartbeat message`() {
        val original = HeartbeatMessage(time = 1234567890L)
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is HeartbeatMessage)
    }

    @Test
    fun `decode timer update message`() {
        val original = TimerUpdateMessage(secondsRemaining = 45)
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is TimerUpdateMessage)
        decoded as TimerUpdateMessage
        assertEquals(45, decoded.secondsRemaining)
    }

    @Test
    fun `decode canvas clear message`() {
        val original = CanvasClearMessage()
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is CanvasClearMessage)
    }

    @Test
    fun `decode player left message`() {
        val original = PlayerLeftMessage(playerId = "p1", name = "Alice")
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is PlayerLeftMessage)
        decoded as PlayerLeftMessage
        assertEquals("Alice", decoded.name)
    }

    @Test
    fun `decode round end message`() {
        val players = listOf(Player(playerId = "p1", name = "Alice", score = 130))
        val original = RoundEndMessage(word = "apple", scores = players)
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is RoundEndMessage)
        decoded as RoundEndMessage
        assertEquals("apple", decoded.word)
        assertEquals(130, decoded.scores.first().score)
    }

    @Test
    fun `decode game end message`() {
        val players = listOf(Player(playerId = "p1", name = "Alice", score = 500))
        val original = GameEndMessage(scores = players)
        val payload = json.encodeToString(original)
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is GameEndMessage)
        decoded as GameEndMessage
        assertEquals(500, decoded.scores.first().score)
    }

    @Test
    fun `polymorphic encode-decode round trip for SkidlMessage`() {
        val message: SkidlMessage = JoinMessage(playerId = "p1", name = "Poly")
        val payload = json.encodeToString<SkidlMessage>(message)
        assertTrue("JSON should contain type field", payload.contains("\"type\":\"join\""))
        val decoded = json.decodeFromString(SkidlMessageSerializer, payload)
        assertTrue(decoded is JoinMessage)
        assertEquals("Poly", (decoded as JoinMessage).name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown type throws exception`() {
        val badJson = """{"type":"unknown_message","data":"test"}"""
        json.decodeFromString(SkidlMessageSerializer, badJson)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing type throws exception`() {
        val badJson = """{"data":"test"}"""
        json.decodeFromString(SkidlMessageSerializer, badJson)
    }
}
