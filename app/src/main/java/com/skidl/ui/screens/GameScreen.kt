package com.skidl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.skidl.model.GuessMessage
import com.skidl.model.StrokePointMessage
import com.skidl.model.StrokeStartMessage
import com.skidl.model.StrokeEndMessage
import com.skidl.ui.SkidlUiState
import com.skidl.ui.components.DrawingCanvas

@Composable
fun GameScreen(
    state: SkidlUiState,
    onSendGuess: (String) -> Unit,
    onStrokeStart: (StrokeStartMessage) -> Unit,
    onStrokePoint: (StrokePointMessage) -> Unit,
    onStrokeEnd: (StrokeEndMessage) -> Unit
) {
    val round = state.roundState
    var guess by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF000000)) }
    var thickness by remember { mutableStateOf(12f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Drawer: ${round?.drawerId ?: ""}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Time: ${round?.secondsRemaining ?: 0}s", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        ScoreboardRow(players = round?.scoreboard ?: state.lobbyState?.players.orEmpty())
        Spacer(modifier = Modifier.height(8.dp))
        DrawingCanvas(
            strokes = round?.strokes ?: emptyList(),
            selectedColor = selectedColor,
            thickness = thickness,
            playerId = state.playerId,
            onStrokeStart = onStrokeStart,
            onStrokePoint = onStrokePoint,
            onStrokeEnd = onStrokeEnd
        )
        Spacer(modifier = Modifier.height(12.dp))
        BrushControls(
            selectedColor = selectedColor,
            onColorChange = { selectedColor = it },
            thickness = thickness,
            onThicknessChange = { thickness = it }
        )
        Spacer(modifier = Modifier.height(12.dp))
        GuessList(guesses = round?.guesses ?: emptyList())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = guess,
            onValueChange = { guess = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Guess the word") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            if (guess.isNotBlank()) {
                onSendGuess(guess)
                guess = ""
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Send Guess")
        }
    }
}

@Composable
private fun BrushControls(
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    thickness: Float,
    onThicknessChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Colors", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            colorPalette.forEach { color ->
                ColorSwatch(color = color, selected = color == selectedColor, onClick = { onColorChange(color) })
            }
        }
        Text(text = "Thickness", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = thickness,
            onValueChange = onThicknessChange,
            valueRange = 4f..32f
        )
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .width(40.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.Black else Color.LightGray,
                shape = MaterialTheme.shapes.small
            )
            .clip(MaterialTheme.shapes.small)
            .background(color)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun GuessList(guesses: List<GuessMessage>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        items(guesses) { guess ->
            GuessRow(guess)
        }
    }
}

@Composable
private fun GuessRow(guess: GuessMessage) {
    val background = if (guess.isCorrect) Color(0xFF81C784) else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(8.dp)
    ) {
        Text(text = guess.playerId, style = MaterialTheme.typography.bodySmall)
        Text(text = guess.text, style = MaterialTheme.typography.bodyMedium)
    }
}

private val colorPalette = listOf(
    Color(0xFF000000),
    Color(0xFFFF5722),
    Color(0xFFFFC107),
    Color(0xFF4CAF50),
    Color(0xFF2196F3),
    Color(0xFF9C27B0)
)

@Composable
private fun ScoreboardRow(players: List<com.skidl.model.Player>) {
    if (players.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEDE7F6))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        players.sortedByDescending { it.score }.take(5).forEach { player ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = player.name, style = MaterialTheme.typography.bodySmall)
                Text(text = "${player.score}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
