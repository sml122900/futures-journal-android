package com.futuresjournal.app.api.models

import java.io.Serializable

data class EmergencyPayload(
    val sessionId: String,
    val level: Int,
    val symbol: String,
    val side: String,
    val size: Double,
    val sizeMultiplier: Double,
    val triggers: List<String>,
    val forceSentence: String,
    val countdownSeconds: Int
) : Serializable
