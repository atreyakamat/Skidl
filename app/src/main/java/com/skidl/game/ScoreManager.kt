package com.skidl.game

import com.skidl.model.Player
import kotlin.math.max

class ScoreManager {
    private val scores = mutableMapOf<String, Int>()

    fun recordCorrectGuess(playerId: String, timeRemaining: Int, basePoints: Int = 100): Int {
        val bonus = max(timeRemaining, 0)
        val total = scores.getOrDefault(playerId, 0) + basePoints + bonus
        scores[playerId] = total
        return total
    }

    fun updatePlayer(player: Player): Player {
        val score = scores.getOrDefault(player.playerId, player.score)
        scores[player.playerId] = score
        return player.copy(score = score)
    }

    fun getScores(players: List<Player>): List<Player> {
        return players.map { updatePlayer(it) }
    }

    fun reset() {
        scores.clear()
    }
}
