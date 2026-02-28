package com.skidl

import com.skidl.game.ScoreManager
import com.skidl.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScoreManagerTest {

    private lateinit var manager: ScoreManager

    @Before
    fun setup() {
        manager = ScoreManager()
    }

    @Test
    fun `records scores with time bonus`() {
        val points = manager.recordCorrectGuess("player1", timeRemaining = 30)
        assertEquals(130, points) // 100 base + 30 bonus
        val updated = manager.updatePlayer(Player(playerId = "player1", name = "Test"))
        assertEquals(130, updated.score)
    }

    @Test
    fun `records zero time bonus when timer expired`() {
        val points = manager.recordCorrectGuess("player1", timeRemaining = 0)
        assertEquals(100, points)
    }

    @Test
    fun `negative time remaining treated as zero bonus`() {
        val points = manager.recordCorrectGuess("player1", timeRemaining = -5)
        assertEquals(100, points) // max(-5, 0) = 0 bonus
    }

    @Test
    fun `accumulates scores across multiple guesses`() {
        manager.recordCorrectGuess("player1", timeRemaining = 10) // 110
        val points = manager.recordCorrectGuess("player1", timeRemaining = 20) // +120 = 230
        assertEquals(230, points)
    }

    @Test
    fun `tracks multiple players independently`() {
        manager.recordCorrectGuess("p1", timeRemaining = 30) // 130
        manager.recordCorrectGuess("p2", timeRemaining = 10) // 110

        val p1 = manager.updatePlayer(Player(playerId = "p1", name = "Alice"))
        val p2 = manager.updatePlayer(Player(playerId = "p2", name = "Bob"))
        assertEquals(130, p1.score)
        assertEquals(110, p2.score)
    }

    @Test
    fun `getScores returns updated players`() {
        manager.recordCorrectGuess("p1", timeRemaining = 50) // 150
        manager.recordCorrectGuess("p2", timeRemaining = 20) // 120

        val players = listOf(
            Player(playerId = "p1", name = "Alice"),
            Player(playerId = "p2", name = "Bob")
        )
        val scored = manager.getScores(players)
        assertEquals(150, scored.first { it.playerId == "p1" }.score)
        assertEquals(120, scored.first { it.playerId == "p2" }.score)
    }

    @Test
    fun `reset clears all scores`() {
        manager.recordCorrectGuess("player1", timeRemaining = 30)
        manager.reset()
        val updated = manager.updatePlayer(Player(playerId = "player1", name = "Test"))
        assertEquals(0, updated.score)
    }

    @Test
    fun `custom base points`() {
        val points = manager.recordCorrectGuess("p1", timeRemaining = 10, basePoints = 200)
        assertEquals(210, points)
    }

    @Test
    fun `updatePlayer preserves existing score if not tracked`() {
        val player = Player(playerId = "unknown", name = "Ghost", score = 42)
        val result = manager.updatePlayer(player)
        assertEquals(42, result.score)
    }

    @Test
    fun `high time bonus rewards fast guessers`() {
        val fast = manager.recordCorrectGuess("fast", timeRemaining = 85)
        val managerSlow = ScoreManager()
        val slow = managerSlow.recordCorrectGuess("slow", timeRemaining = 5)
        assertTrue("Fast guesser should score higher", fast > slow)
    }
}
