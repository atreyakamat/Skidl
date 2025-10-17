package com.skidl.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.skidl.ui.SkidlUiState

@Composable
fun ScoresScreen(state: SkidlUiState) {
    val players = state.lobbyState?.players.orEmpty().sortedByDescending { it.score }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Final Scores", style = MaterialTheme.typography.headlineSmall)
        LazyColumn {
            items(players) { player ->
                Text(
                    text = "${player.name}: ${player.score}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
