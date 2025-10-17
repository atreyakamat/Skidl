package com.skidl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.skidl.model.Stroke as StrokeModel
import com.skidl.model.StrokePointMessage
import com.skidl.model.StrokeStartMessage
import com.skidl.model.StrokeEndMessage
import java.util.UUID

@Composable
fun DrawingCanvas(
    strokes: List<StrokeModel>,
    modifier: Modifier = Modifier,
    selectedColor: Color,
    thickness: Float,
    playerId: String,
    onStrokeStart: (StrokeStartMessage) -> Unit,
    onStrokePoint: (StrokePointMessage) -> Unit,
    onStrokeEnd: (StrokeEndMessage) -> Unit
) {
    var currentStrokeId by remember { mutableStateOf<String?>(null) }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .background(Color.White)
            .pointerInput(selectedColor, thickness) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val strokeId = UUID.randomUUID().toString()
                        currentStrokeId = strokeId
                        onStrokeStart(
                            StrokeStartMessage(
                                strokeId = strokeId,
                                playerId = playerId,
                                color = "#%06X".format(selectedColor.toArgb() and 0xFFFFFF),
                                thickness = thickness,
                                x = offset.x,
                                y = offset.y,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    },
                    onDrag = { change, _ ->
                        val strokeId = currentStrokeId ?: return@detectDragGestures
                        val point = StrokePointMessage(
                            strokeId = strokeId,
                            x = change.position.x,
                            y = change.position.y,
                            timestamp = System.currentTimeMillis()
                        )
                        onStrokePoint(point)
                    },
                    onDragEnd = {
                        currentStrokeId?.let { strokeId ->
                            onStrokeEnd(StrokeEndMessage(strokeId))
                        }
                        currentStrokeId = null
                    },
                    onDragCancel = {
                        currentStrokeId?.let { strokeId ->
                            onStrokeEnd(StrokeEndMessage(strokeId))
                        }
                        currentStrokeId = null
                    }
                )
            }
    ) {
        strokes.forEach { stroke ->
            val path = Path()
            stroke.points.firstOrNull()?.let { first ->
                path.moveTo(first.x, first.y)
                stroke.points.drop(1).forEach { point ->
                    path.lineTo(point.x, point.y)
                }
                val color = Color(android.graphics.Color.parseColor(stroke.color))
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = stroke.thickness, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt()
)
