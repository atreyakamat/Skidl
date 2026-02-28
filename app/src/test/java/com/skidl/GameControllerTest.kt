package com.skidl

import com.skidl.game.GameController
import com.skidl.model.GuessMessage
import com.skidl.model.Player
import com.skidl.model.Stroke
import com.skidl.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameControllerTest {

    private lateinit var controller: GameController

    @Before
    fun setup() {
        controller = GameController("host-1", "Test Room")
    }

    @Test
    fun `initial lobby has host player id`() {
        assertEquals("host-1", controller.lobby.value.hostPlayerId)
    }

    @Test
    fun `initial lobby has correct room name`() {
        assertEquals("Test Room", controller.lobby.value.settings.roomName)
    }

    @Test
    fun `initial round state is empty`() {
        assertNull(controller.round.value.drawerId)
        assertEquals(0, controller.round.value.strokes.size)
        assertEquals(0, controller.round.value.guesses.size)
    }

    @Test
    fun `updatePlayer adds new player`() {
        val player = Player(playerId = "p1", name = "Alice")
        controller.updatePlayer(player)
        assertEquals(1, controller.lobby.value.players.size)
        assertEquals("Alice", controller.lobby.value.players.first().name)
    }

    @Test
    fun `updatePlayer updates existing player`() {
        controller.updatePlayer(Player(playerId = "p1", name = "Alice"))
        controller.updatePlayer(Player(playerId = "p1", name = "Alice Updated", score = 100))
        assertEquals(1, controller.lobby.value.players.size)
        assertEquals("Alice Updated", controller.lobby.value.players.first().name)
        assertEquals(100, controller.lobby.value.players.first().score)
    }

    @Test
    fun `setPlayers replaces all players`() {
        controller.updatePlayer(Player(playerId = "p1", name = "Alice"))
        val newPlayers = listOf(
            Player(playerId = "p2", name = "Bob"),
            Player(playerId = "p3", name = "Charlie")
        )
        controller.setPlayers(newPlayers)
        assertEquals(2, controller.lobby.value.players.size)
        assertTrue(controller.lobby.value.players.none { it.playerId == "p1" })
    }

    @Test
    fun `setReady updates player ready state`() {
        controller.updatePlayer(Player(playerId = "p1", name = "Alice", isReady = false))
        controller.setReady("p1", true)
        assertTrue(controller.lobby.value.players.first().isReady)
    }

    @Test
    fun `setReady ignores unknown player`() {
        controller.updatePlayer(Player(playerId = "p1", name = "Alice"))
        controller.setReady("unknown", true) // should not crash
        assertEquals(1, controller.lobby.value.players.size)
    }

    @Test
    fun `startRound initializes round state`() {
        controller.updatePlayer(Player(playerId = "p1", name = "Alice"))
        controller.startRound("p1", "apple", 90)
        val round = controller.round.value
        assertEquals("p1", round.drawerId)
        assertNotNull(round.roundId)
        assertEquals(90, round.secondsRemaining)
        assertEquals(0, round.strokes.size)
        assertEquals(0, round.guesses.size)
    }

    @Test
    fun `startRound hashes the secret word`() {
        controller.startRound("p1", "apple", 60)
        val hash = controller.round.value.secretWordHash
        assertNotNull(hash)
        assertTrue("Hash should be a hex string", hash!!.matches(Regex("[0-9a-f]+")))
    }

    @Test
    fun `updateTimer decrements timer`() {
        controller.startRound("p1", "apple", 90)
        controller.updateTimer(45)
        assertEquals(45, controller.round.value.secondsRemaining)
    }

    @Test
    fun `updateTimer clamps at zero`() {
        controller.startRound("p1", "apple", 10)
        controller.updateTimer(-5)
        assertEquals(0, controller.round.value.secondsRemaining)
    }

    @Test
    fun `appendStroke adds stroke`() {
        controller.startRound("p1", "apple", 60)
        val stroke = Stroke(
            strokeId = "s1", playerId = "p1", color = "#000000", thickness = 5f,
            points = listOf(StrokePoint(1f, 2f, 100L))
        )
        controller.appendStroke(stroke)
        assertEquals(1, controller.round.value.strokes.size)
        assertEquals("s1", controller.round.value.strokes.first().strokeId)
    }

    @Test
    fun `updateStroke modifies existing stroke`() {
        controller.startRound("p1", "apple", 60)
        val stroke = Stroke(strokeId = "s1", playerId = "p1", color = "#000000", thickness = 5f)
        controller.appendStroke(stroke)
        val updated = stroke.copy(points = listOf(StrokePoint(1f, 2f, 100L), StrokePoint(3f, 4f, 200L)))
        controller.updateStroke(updated)
        assertEquals(2, controller.round.value.strokes.first().points.size)
    }

    @Test
    fun `updateStroke ignores unknown stroke`() {
        controller.startRound("p1", "apple", 60)
        val unknown = Stroke(strokeId = "unknown", playerId = "p1", color = "#000000", thickness = 5f)
        controller.updateStroke(unknown) // should not crash or add
        assertEquals(0, controller.round.value.strokes.size)
    }

    @Test
    fun `removeStroke removes by id`() {
        controller.startRound("p1", "apple", 60)
        controller.appendStroke(Stroke(strokeId = "s1", playerId = "p1", color = "#000000", thickness = 5f))
        controller.appendStroke(Stroke(strokeId = "s2", playerId = "p1", color = "#FF0000", thickness = 3f))
        controller.removeStroke("s1")
        assertEquals(1, controller.round.value.strokes.size)
        assertEquals("s2", controller.round.value.strokes.first().strokeId)
    }

    @Test
    fun `clearStrokes removes all strokes`() {
        controller.startRound("p1", "apple", 60)
        controller.appendStroke(Stroke(strokeId = "s1", playerId = "p1", color = "#000000", thickness = 5f))
        controller.appendStroke(Stroke(strokeId = "s2", playerId = "p1", color = "#FF0000", thickness = 3f))
        controller.clearStrokes()
        assertEquals(0, controller.round.value.strokes.size)
    }

    @Test
    fun `appendGuess adds guess`() {
        controller.startRound("p1", "apple", 60)
        val guess = GuessMessage(playerId = "p2", text = "banana")
        controller.appendGuess(guess)
        assertEquals(1, controller.round.value.guesses.size)
        assertEquals("banana", controller.round.value.guesses.first().text)
    }

    @Test
    fun `updateScores updates both round and lobby`() {
        controller.updatePlayer(Player(playerId = "p1", name = "Alice"))
        controller.startRound("p1", "apple", 60)
        val scored = listOf(Player(playerId = "p1", name = "Alice", score = 200))
        controller.updateScores(scored)
        assertEquals(200, controller.round.value.scoreboard.first().score)
        assertEquals(200, controller.lobby.value.players.first().score)
    }

    @Test
    fun `updateSettings changes game settings`() {
        controller.updateSettings(controller.lobby.value.settings.copy(roundTimeSeconds = 120))
        assertEquals(120, controller.lobby.value.settings.roundTimeSeconds)
    }
}
