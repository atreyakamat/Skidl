package com.skidl.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skidl.model.GuessMessage
import com.skidl.model.StrokePointMessage
import com.skidl.model.StrokeStartMessage
import com.skidl.model.StrokeEndMessage
import com.skidl.ui.SkidlUiState
import com.skidl.ui.components.DrawingCanvas

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameScreen(
    state: SkidlUiState,
    onSendGuess: (String) -> Unit,
    onStrokeStart: (StrokeStartMessage) -> Unit,
    onStrokePoint: (StrokePointMessage) -> Unit,
    onStrokeEnd: (StrokeEndMessage) -> Unit,
    onUndo: () -> Unit = {},
    onClearCanvas: () -> Unit = {}
) {
    val round = state.roundState
    var guess by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF000000)) }
    var thickness by remember { mutableStateOf(12f) }
    val isDrawer = round?.drawerId == state.playerId
    val drawerName by remember(state.lobbyState?.players, round?.drawerId) {
        derivedStateOf {
            state.lobbyState?.players?.firstOrNull { it.playerId == round?.drawerId }?.name
                ?: round?.drawerId ?: ""
        }
    }
    val players = state.lobbyState?.players.orEmpty()
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // ─── Header: Drawer, Round, Timer ───
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "\u270F\uFE0F $drawerName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(140.dp)
                        )
                        if (state.currentRound > 0) {
                            Text(
                                text = "Round ${state.currentRound}/${state.totalRounds}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Timer with color urgency
                    val timeLeft = round?.secondsRemaining ?: 0
                    val timerColor = when {
                        timeLeft <= 10 -> Color(0xFFFF5722)
                        timeLeft <= 30 -> Color(0xFFFFC107)
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    Text(
                        text = "\u23F1 ${timeLeft}s",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = timerColor
                    )
                }
            }

            // ─── Secret word for drawer ───
            AnimatedVisibility(
                visible = isDrawer && state.secretWordForDrawer != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "\uD83D\uDD8A\uFE0F Draw: ${state.secretWordForDrawer ?: ""}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ─── Compact Scoreboard ───
            ScoreboardRow(players = round?.scoreboard ?: players)

            Spacer(modifier = Modifier.height(6.dp))

            // ─── Canvas ───
            DrawingCanvas(
                strokes = round?.strokes ?: emptyList(),
                selectedColor = selectedColor,
                thickness = thickness,
                playerId = state.playerId,
                onStrokeStart = if (isDrawer) onStrokeStart else { _ -> },
                onStrokePoint = if (isDrawer) onStrokePoint else { _ -> },
                onStrokeEnd = if (isDrawer) onStrokeEnd else { _ -> }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isDrawer) {
                // ─── Drawer Controls ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onUndo,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) { Text("↩ Undo") }
                    Button(
                        onClick = onClearCanvas,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) { Text("\uD83D\uDDD1 Clear") }
                }

                Spacer(modifier = Modifier.height(8.dp))

                BrushControls(
                    selectedColor = selectedColor,
                    onColorChange = { selectedColor = it },
                    thickness = thickness,
                    onThicknessChange = { thickness = it }
                )
            } else {
                // ─── Guesser Controls ───
                GuessList(guesses = round?.guesses ?: emptyList(), players = players)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = guess,
                        onValueChange = { guess = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Guess the word…") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                    Button(
                        onClick = {
                            if (guess.isNotBlank()) {
                                onSendGuess(guess)
                                guess = ""
                            }
                        },
                        modifier = Modifier.height(52.dp),
                        shape = MaterialTheme.shapes.small,
                        enabled = guess.isNotBlank()
                    ) {
                        Text("\u27A1\uFE0F")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrushControls(
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    thickness: Float,
    onThicknessChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "\uD83C\uDFA8 Colors",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                colorPalette.forEach { color ->
                    ColorSwatch(
                        color = color,
                        selected = color == selectedColor,
                        onClick = { onColorChange(color) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\u2712 Thickness: ${thickness.toInt()}px",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = thickness,
                onValueChange = onThicknessChange,
                valueRange = 2f..40f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent
    Box(
        modifier = Modifier
            .size(42.dp)
            .shadow(if (selected) 4.dp else 1.dp, CircleShape)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun GuessList(guesses: List<GuessMessage>, players: List<com.skidl.model.Player> = emptyList()) {
    val listState = rememberLazyListState()

    // Auto-scroll to latest guess
    LaunchedEffect(guesses.size) {
        if (guesses.isNotEmpty()) {
            listState.animateScrollToItem(guesses.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(guesses, key = { "${it.playerId}-${it.timestamp}" }) { guess ->
            GuessRow(guess, players)
        }
    }
}

@Composable
private fun GuessRow(guess: GuessMessage, players: List<com.skidl.model.Player>) {
    val background = if (guess.isCorrect)
        Color(0xFF6BCB77).copy(alpha = 0.25f)
    else
        Color.Transparent
    val displayName = players.firstOrNull { it.playerId == guess.playerId }?.name ?: guess.playerId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = guess.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (guess.isCorrect) FontWeight.Bold else FontWeight.Normal
            )
        }
        if (guess.isCorrect) {
            Text(text = "✅", fontSize = 16.sp)
        }
    }
}

private val colorPalette = listOf(
    Color(0xFF000000), // Black
    Color(0xFF808080), // Gray
    Color(0xFFFFFFFF), // White
    Color(0xFFFF5722), // Red-Orange
    Color(0xFFE91E63), // Pink
    Color(0xFFFFC107), // Yellow
    Color(0xFFFF9800), // Orange
    Color(0xFF4CAF50), // Green
    Color(0xFF2196F3), // Blue
    Color(0xFF9C27B0), // Purple
    Color(0xFF795548), // Brown
    Color(0xFF00BCD4), // Cyan
)

@Composable
private fun ScoreboardRow(players: List<com.skidl.model.Player>) {
    if (players.isEmpty()) return
    val sorted = remember(players) {
        players.sortedByDescending { it.score }.take(5)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sorted.forEach { player ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${player.score}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
