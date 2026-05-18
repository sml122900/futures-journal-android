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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var countdownJob: Job? = null
    private var overlayStartTime: Long = 0

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
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_warning, null)

        setupWarningContent(overlayView!!, payload)
        setupTypeChallenge(overlayView!!, payload)
        startCountdown(overlayView!!, payload.countdownSeconds)

        if (payload.level == 3) {
            vibratePattern()
            playWarningSound()
        }

        windowManager.addView(overlayView, params)
    }

    private fun setupWarningContent(view: View, payload: EmergencyPayload) {
        view.findViewById<TextView>(R.id.target_sentence).text = payload.forceSentence
        view.findViewById<TextView>(R.id.context_text).text =
            "${payload.symbol} ${payload.side} × ${String.format("%.1f", payload.sizeMultiplier)}배 사이즈"

        val triggerList = view.findViewById<LinearLayout>(R.id.trigger_list)
        payload.triggers.forEach { trigger ->
            val tv = TextView(this).apply {
                text = "• $trigger"
                setTextColor(resources.getColor(R.color.text_secondary, null))
                setPadding(0, 4, 0, 4)
            }
            triggerList.addView(tv)
        }

        applyLevelStyle(view, payload.level)
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

    private fun setupTypeChallenge(view: View, payload: EmergencyPayload) {
        val targetSentence = payload.forceSentence
        val input = view.findViewById<EditText>(R.id.typing_input)
        val proceedBtn = view.findViewById<Button>(R.id.btn_proceed)
        val feedback = view.findViewById<TextView>(R.id.typing_feedback)

        input.isEnabled = false

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val typed = s?.toString() ?: ""
                val match = typed == targetSentence
                proceedBtn.isEnabled = match
                feedback.text = when {
                    match -> getString(R.string.type_correct)
                    typed.isNotEmpty() -> getString(R.string.type_wrong)
                    else -> ""
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        view.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try { ApiClient.cancelEmergency(payload.sessionId) } catch (e: Exception) {}
            }
            removeOverlay()
            stopSelf()
        }

        proceedBtn.setOnClickListener {
            val durationSeconds = (System.currentTimeMillis() - overlayStartTime) / 1000
            CoroutineScope(Dispatchers.IO).launch {
                try { ApiClient.proceedEmergency(payload.sessionId, input.text.toString(), durationSeconds) } catch (e: Exception) {}
            }
            removeOverlay()
            stopSelf()
        }

        view.findViewById<Button>(R.id.btn_open_bitget).setOnClickListener {
            val intent = packageManager.getLaunchIntentForPackage("com.bitget.exchange")
            if (intent != null) startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun startCountdown(view: View, seconds: Int) {
        val display = view.findViewById<TextView>(R.id.countdown)
        val input = view.findViewById<EditText>(R.id.typing_input)
        var remaining = seconds

        countdownJob = CoroutineScope(Dispatchers.Main).launch {
            while (remaining > 0) {
                val m = remaining / 60
                val s = remaining % 60
                display.text = String.format("%02d:%02d", m, s)
                delay(1000)
                remaining--
            }
            display.text = "⌨️ 타이핑 가능"
            input.isEnabled = true
            input.requestFocus()
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
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
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
