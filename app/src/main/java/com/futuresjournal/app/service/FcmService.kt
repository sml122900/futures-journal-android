package com.futuresjournal.app.service

import android.content.Intent
import android.os.Build
import com.futuresjournal.app.api.ApiClient
import com.futuresjournal.app.api.models.EmergencyPayload
import com.futuresjournal.app.util.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.registerDevice(token)
            } catch (e: Exception) {
                Logger.error("token register failed", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val type = data["type"]

        when (type) {
            "emergency" -> {
                val level = data["level"]?.toIntOrNull() ?: 0
                val sessionId = data["sessionId"] ?: return
                val payload = EmergencyPayload(
                    sessionId = sessionId,
                    level = level,
                    symbol = data["symbol"] ?: "",
                    side = data["side"] ?: "",
                    size = data["size"]?.toDoubleOrNull() ?: 0.0,
                    sizeMultiplier = data["sizeMultiplier"]?.toDoubleOrNull() ?: 0.0,
                    triggers = data["triggers"]?.split(",") ?: emptyList(),
                    forceSentence = data["forceSentence"] ?: "원칙을 지켜야 살아남는다",
                    countdownSeconds = data["countdown"]?.toIntOrNull() ?: 60
                )

                val intent = Intent(this, OverlayService::class.java).apply {
                    putExtra("payload", payload)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
    }
}
