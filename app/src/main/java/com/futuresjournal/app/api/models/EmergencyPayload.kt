package com.futuresjournal.app.api.models

import java.io.Serializable

data class EmergencyPayload(
    val sessionId: String,
    val level: Int,
    val symbol: String,
    val side: String,
    val size: Double,
    val sizeMultiplier: Double,
    val triggers: List<String>,          // 트리거 type 배열
    val triggerMessages: List<String>?,  // 발동 메시지 배열 (새 시스템)
    val forceSentence: String,
    val countdownSeconds: Int
) : Serializable
