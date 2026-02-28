package com.skidl.game

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.random.Random

class WordBank(private val context: Context) {
    private var words: List<String> = emptyList()
    private val rng = Random.Default
    private val usedWords = mutableSetOf<String>()

    suspend fun loadIfNeeded() {
        if (words.isNotEmpty()) return
        words = withContext(Dispatchers.IO) {
            val input = context.assets.open("words.json")
            val content = input.bufferedReader().use { it.readText() }
            val array = JSONArray(content)
            buildList {
                for (i in 0 until array.length()) {
                    add(array.getString(i))
                }
            }
        }
    }

    suspend fun nextWord(): String {
        loadIfNeeded()
        if (words.isEmpty()) return "apple"
        // Avoid repeats until all words used
        val available = words - usedWords
        val pool = available.ifEmpty {
            usedWords.clear()
            words
        }
        val word = pool.random(rng)
        usedWords.add(word)
        return word
    }
}
