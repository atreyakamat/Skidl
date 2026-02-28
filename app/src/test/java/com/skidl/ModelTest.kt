package com.skidl

import com.skidl.model.GameSettings
import com.skidl.model.GuessMessage
import com.skidl.model.LobbyState
import com.skidl.model.Player
import com.skidl.model.RoundState
import com.skidl.model.Stroke
import com.skidl.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {

    @Test
    fun `Player default values`() {
        val player = Player(playerId = "p1", name = "Alice")
        assertFalse(player.isHost)
        assertFalse(player.isReady)
        assertEquals(0, player.score)
    }

    @Test
    fun `Player copy with score`() {
        val player = Player(playerId = "p1", name = "Alice", score = 100)
        val updated = player.copy(score = 200)
        assertEquals(200, updated.score)
        assertEquals("p1", updated.playerId)
    }

    @Test
    fun `GameSettings default values`() {
        val settings = GameSettings()
        assertEquals(90, settings.roundTimeSeconds)
        assertNull(settings.roomCode)
        assertEquals("Skidl Room", settings.roomName)
    }

    @Test
    fun `LobbyState defaults`() {
        val lobby = LobbyState(hostPlayerId = "host-1")
        assertTrue(lobby.players.isEmpty())
        assertFalse(lobby.isHostReady)
    }

    @Test
    fun `RoundState defaults`() {
        val round = RoundState()
        assertNull(round.drawerId)
        assertNull(round.roundId)
        assertEquals(0, round.secondsRemaining)
        assertTrue(round.strokes.isEmpty())
        assertTrue(round.guesses.isEmpty())
    }

    @Test
    fun `Stroke with points`() {
        val stroke = Stroke(
            strokeId = "s1",
            playerId = "p1",
            color = "#FF0000",
            thickness = 5f,
            points = listOf(
                StrokePoint(0f, 0f, 100L),
                StrokePoint(1f, 1f, 200L)
            )
        )
        assertEquals(2, stroke.points.size)
        assertFalse(stroke.isErasing)
    }

    @Test
    fun `GuessMessage defaults`() {
        val guess = GuessMessage(playerId = "p1", text = "hello")
        assertFalse(guess.isCorrect)
        assertTrue(guess.timestamp > 0)
    }

    @Test
    fun `GuessMessage correct flag`() {
        val guess = GuessMessage(playerId = "p1", text = "apple", isCorrect = true)
        assertTrue(guess.isCorrect)
    }

    @Test
    fun `StrokePoint data class`() {
        val point = StrokePoint(10.5f, 20.3f, 999L)
        assertEquals(10.5f, point.x)
        assertEquals(20.3f, point.y)
        assertEquals(999L, point.timestamp)
    }
}
