package com.skidl

import com.skidl.game.ScoreManager
import com.skidl.model.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreManagerTest {

    @Test
    fun `records scores with time bonus`() {
        val manager = ScoreManager()
        val points = manager.recordCorrectGuess("player1", timeRemaining = 30)
        assertEquals(130, points)
        val updated = manager.updatePlayer(Player(playerId = "player1", name = "Test"))
        assertEquals(130, updated.score)
    }
}
