// 오버레이 서비스 — SYSTEM_ALERT_WINDOW로 풀스크린 경고 표시
package com.futuresjournal.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.futuresjournal.app.R
import com.futuresjournal.app.api.ApiClient
import com.futuresjournal.app.api.models.EmergencyPayload
import com.futuresjournal.app.util.Logger
import com.futuresjournal.app.util.OverlayPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var countdownJob: Job? = null
    private var overlayStartTime: Long = 0

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob)

    companion object {
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        @Suppress("DEPRECATION")
        val payload = intent?.getSerializableExtra("payload") as? EmergencyPayload
        if (payload == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            Logger.error("SYSTEM_ALERT_WINDOW permission denied")
            showFullScreenNotification(payload)
            stopSelf()
            return START_NOT_STICKY
        }

        showOverlay(payload)
        return START_NOT_STICKY
    }

    private fun showOverlay(payload: EmergencyPayload) {
        if (overlayView != null) return
        overlayStartTime = System.currentTimeMillis()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_warning, null)
        val view = overlayView!!

        // 컨텍스트 정보 표시
        view.findViewById<TextView>(R.id.context_text).text =
            "${payload.symbol} ${payload.side} × ${String.format("%.1f", payload.sizeMultiplier)}배"

        // 로컬 설정에서 멘트/카운트다운 로드
        val sentence = OverlayPrefs.getSelectedSentence(this)
        val countdownSecs = OverlayPrefs.getCountdownSeconds(this)

        view.findViewById<TextView>(R.id.target_sentence).text = sentence
        applyLevelStyle(view, payload.level)
        setupTypeChallenge(view, sentence, payload.sessionId)
        startCountdown(view, countdownSecs)

        if (payload.level == 3) {
            vibratePattern()
            playWarningSound()
        }

        windowManager.addView(view, params)
    }

    private fun applyLevelStyle(view: View, level: Int) {
        val card = view.findViewById<LinearLayout>(R.id.warning_card)
        when (level) {
            1 -> card.setBackgroundResource(R.drawable.warning_yellow)
            2 -> card.setBackgroundResource(R.drawable.warning_orange)
            3 -> card.setBackgroundResource(R.drawable.warning_red)
            else -> card.setBackgroundResource(R.drawable.warning_card_bg)
        }
    }

    private fun setupTypeChallenge(view: View, targetSentence: String, sessionId: String) {
        val input = view.findViewById<EditText>(R.id.typing_input)
        val feedback = view.findViewById<TextView>(R.id.typing_feedback)
        val confirmSection = view.findViewById<View>(R.id.confirm_section)
        val confirmBtn = view.findViewById<Button>(R.id.btn_confirm)

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val typed = s?.toString() ?: ""
                val match = typed == targetSentence
                if (match) {
                    feedback.text = "✓ 정확합니다"
                    feedback.setTextColor(resources.getColor(R.color.warning_yellow, null))
                    confirmSection.visibility = View.VISIBLE
                } else {
                    feedback.text = if (typed.isNotEmpty()) "일치하지 않습니다" else ""
                    feedback.setTextColor(resources.getColor(R.color.text_secondary, null))
                    confirmSection.visibility = View.GONE
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        confirmBtn.setOnClickListener {
            val durationSeconds = (System.currentTimeMillis() - overlayStartTime) / 1000
            serviceScope.launch(Dispatchers.IO) {
                try {
                    ApiClient.proceedEmergency(sessionId, input.text.toString(), durationSeconds)
                } catch (e: Exception) {
                    Logger.error("proceedEmergency failed", e)
                }
            }
            removeOverlay()
            stopSelf()
        }
    }

    private fun startCountdown(view: View, seconds: Int) {
        val display = view.findViewById<TextView>(R.id.countdown)
        val countdownPhase = view.findViewById<View>(R.id.countdown_phase)
        val typingPhase = view.findViewById<View>(R.id.typing_phase)
        val input = view.findViewById<EditText>(R.id.typing_input)
        var remaining = seconds

        countdownJob = serviceScope.launch(Dispatchers.Main) {
            while (remaining > 0) {
                display.text = remaining.toString()

                // 남은 시간에 따라 색상 변화
                val color = when {
                    remaining > seconds * 0.6 -> resources.getColor(R.color.warning_yellow, null)
                    remaining > seconds * 0.3 -> resources.getColor(R.color.warning_orange, null)
                    else -> resources.getColor(R.color.warning_red, null)
                }
                display.setTextColor(color)

                // 펄스 애니메이션 — 숫자가 튀었다가 줄어듦
                display.scaleX = 1.3f
                display.scaleY = 1.3f
                display.animate().scaleX(1f).scaleY(1f).setDuration(700).start()

                delay(1000)
                remaining--
            }

            // 카운트다운 종료 → 타이핑 페이즈로 전환
            countdownPhase.visibility = View.GONE
            typingPhase.visibility = View.VISIBLE
            input.isEnabled = true
            input.requestFocus()
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { Logger.error("removeView failed", e) }
        }
        overlayView = null
        countdownJob?.cancel()
    }

    private fun vibratePattern() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                val pattern = longArrayOf(0, 500, 250, 500, 250, 500)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                val pattern = longArrayOf(0, 500, 250, 500, 250, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Logger.error("vibrate failed", e)
        }
    }

    private fun playWarningSound() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 3000)
        } catch (e: Exception) {
            Logger.error("playWarningSound failed", e)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val channelId = "emergency_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.emergency_active_title),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(getString(R.string.emergency_active_title))
            .setContentText(getString(R.string.emergency_active_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showFullScreenNotification(payload: EmergencyPayload) {
        val channelId = "emergency_fullscreen"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Emergency Brake",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("🚨 매매 중단 권고")
            .setContentText("${payload.symbol} ${payload.side} — 비정상 사이즈 감지")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID + 1, notification)
    }

    override fun onDestroy() {
        serviceJob.cancel()
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
