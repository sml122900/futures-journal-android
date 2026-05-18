package com.futuresjournal.app.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class UserResponse(
    val id: String,
    val email: String,
    val name: String?
)

@JsonClass(generateAdapter = false)
data class DeviceRegistrationRequest(
    @Json(name = "fcmToken") val fcmToken: String,
    val platform: String = "android",
    val deviceName: String? = null
)

@JsonClass(generateAdapter = false)
data class CancelEmergencyRequest(
    @Json(name = "sessionId") val sessionId: String,
    val reason: String
)

@JsonClass(generateAdapter = false)
data class ProceedEmergencyRequest(
    @Json(name = "sessionId") val sessionId: String,
    @Json(name = "typedText") val typedText: String,
    @Json(name = "durationSeconds") val durationSeconds: Long
)
