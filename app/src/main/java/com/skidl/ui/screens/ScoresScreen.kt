package com.skidl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skidl.R
import com.skidl.ui.SkidlUiState

@Composable
fun ScoresScreen(
    state: SkidlUiState,
    onNextRound: () -> Unit = {},
    onBackToLobby: () -> Unit = {}
) {
    val players = state.lobbyState?.players.orEmpty().sortedByDescending { it.score }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (state.gameOver) "Game Over!" else "Round Scores",
            style = MaterialTheme.typography.headlineSmall
        )

        if (state.lastRevealedWord != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The word was: ${state.lastRevealedWord}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(players) { index, player ->
                val medal = when (index) {
                    0 -> "1st"
                    1 -> "2nd"
                    2 -> "3rd"
                    else -> "${index + 1}th"
                }
                Text(
                    text = "$medal  ${player.name}: ${player.score} pts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isHosting) {
            if (!state.gameOver) {
                Button(onClick = onNextRound, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.next_round))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(onClick = onBackToLobby, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.back_to_lobby))
            }
        }
    }
}
