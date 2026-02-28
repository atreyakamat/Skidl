package com.skidl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.skidl.model.Stroke as StrokeModel
import com.skidl.model.StrokePointMessage
import com.skidl.model.StrokeStartMessage
import com.skidl.model.StrokeEndMessage
import java.util.UUID

/**
 * Color cache to avoid re-parsing the same color strings every recomposition.
 * Keys are the hex color strings (e.g. "#FF0000"), values are Compose Color objects.
 */
private val colorCache = HashMap<String, Color>(32)

private fun parseStrokeColor(hex: String): Color {
    return colorCache.getOrPut(hex) {
        try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {
            Color.Black
        }
    }
}

private val canvasShape = RoundedCornerShape(12.dp)

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
            .shadow(4.dp, canvasShape)
            .clip(canvasShape)
            .border(1.dp, Color(0xFFE0E0E0), canvasShape)
            .background(Color.White)
            .graphicsLayer {
                // Enable hardware-accelerated rendering for smooth drawing
                compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
            }
            .pointerInput(selectedColor, thickness) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val strokeId = UUID.randomUUID().toString()
                        currentStrokeId = strokeId
                        onStrokeStart(
                            StrokeStartMessage(
                                strokeId = strokeId,
                                playerId = playerId,
                                color = "#%06X".format(0xFFFFFF and selectedColor.toArgb()),
                                thickness = thickness,
                                x = offset.x,
                                y = offset.y,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val strokeId = currentStrokeId ?: return@detectDragGestures
                        onStrokePoint(
                            StrokePointMessage(
                                strokeId = strokeId,
                                x = change.position.x,
                                y = change.position.y,
                                timestamp = System.currentTimeMillis()
                            )
                        )
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
            val points = stroke.points
            if (points.isEmpty()) return@forEach

            val path = Path()
            val first = points[0]
            path.moveTo(first.x, first.y)

            if (points.size == 1) {
                // Single-point stroke: draw a dot
                path.lineTo(first.x + 0.1f, first.y + 0.1f)
            } else if (points.size == 2) {
                path.lineTo(points[1].x, points[1].y)
            } else {
                // Use quadratic bezier curves for smoother lines
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val midX = (prev.x + curr.x) / 2f
                    val midY = (prev.y + curr.y) / 2f
                    path.quadraticBezierTo(prev.x, prev.y, midX, midY)
                }
                // Connect to the last point
                val last = points.last()
                path.lineTo(last.x, last.y)
            }

            drawPath(
                path = path,
                color = parseStrokeColor(stroke.color),
                style = Stroke(
                    width = stroke.thickness,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

