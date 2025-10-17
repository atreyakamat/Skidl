package com.skidl

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.skidl.game.GameController
import com.skidl.model.Stroke
import com.skidl.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawingPersistenceInstrumentedTest {

    @Test
    fun strokesPersistAcrossUpdates() {
        val controller = GameController(hostPlayerId = "host-1", initialRoomName = "TestRoom")
        val strokeId = "stroke-1"
        val stroke = Stroke(
            strokeId = strokeId,
            playerId = "host-1",
            color = "#000000",
            thickness = 6f,
            points = listOf(StrokePoint(0f, 0f, 1L))
        )
        controller.appendStroke(stroke)
        controller.updateStroke(
            stroke.copy(points = stroke.points + StrokePoint(10f, 12f, 2L))
        )
        val strokes = controller.round.value.strokes
        assertEquals(1, strokes.size)
        assertEquals(2, strokes.first().points.size)
    }
}
