package com.skidl.model

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val playerId: String,
    val name: String,
    val isHost: Boolean = false,
    val isReady: Boolean = false,
    val score: Int = 0
)
