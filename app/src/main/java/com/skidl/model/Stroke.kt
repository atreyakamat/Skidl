package com.skidl.model

import kotlinx.serialization.Serializable

@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
    val timestamp: Long
)

@Serializable
data class Stroke(
    val strokeId: String,
    val playerId: String,
    val color: String,
    val thickness: Float,
    val points: List<StrokePoint> = emptyList(),
    val isErasing: Boolean = false
)
