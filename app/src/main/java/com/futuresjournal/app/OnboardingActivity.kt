package com.futuresjournal.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.futuresjournal.app.auth.TokenStore
import com.futuresjournal.app.util.PermissionHelper

class OnboardingActivity : AppCompatActivity() {

    private var currentStep = 0

    private data class OnboardingStep(
        val title: String,
        val desc: String,
        val actionLabel: String,
        val checkPermission: () -> Boolean,
        val requestPermission: () -> Unit
    )

    private lateinit var steps: List<OnboardingStep>
    private lateinit var stepIndicator: TextView
    private lateinit var stepTitle: TextView
    private lateinit var stepDesc: TextView
    private lateinit var stepStatus: TextView
    private lateinit var manufacturerGuide: TextView
    private lateinit var btnAction: Button
    private lateinit var btnNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        stepIndicator = findViewById(R.id.step_indicator)
        stepTitle = findViewById(R.id.step_title)
        stepDesc = findViewById(R.id.step_desc)
        stepStatus = findViewById(R.id.step_status)
        manufacturerGuide = findViewById(R.id.manufacturer_guide)
        btnAction = findViewById(R.id.btn_action)
        btnNext = findViewById(R.id.btn_next)

        steps = listOf(
            OnboardingStep(
                title = getString(R.string.permission_overlay_title),
                desc = getString(R.string.permission_overlay_desc),
                actionLabel = "권한 설정 열기",
                checkPermission = { PermissionHelper.hasOverlayPermission(this) },
                requestPermission = { PermissionHelper.requestOverlayPermission(this) }
            ),
            OnboardingStep(
                title = getString(R.string.permission_notification_title),
                desc = getString(R.string.permission_notification_desc),
                actionLabel = "권한 요청",
                checkPermission = { PermissionHelper.hasNotificationPermission(this) },
                requestPermission = { PermissionHelper.requestNotificationPermission(this) }
            ),
            OnboardingStep(
                title = getString(R.string.permission_battery_title),
                desc = getString(R.string.permission_battery_desc),
                actionLabel = "설정 열기",
                checkPermission = { PermissionHelper.isIgnoringBatteryOptimizations(this) },
                requestPermission = { PermissionHelper.requestIgnoreBatteryOptimizations(this) }
            )
        )

        showStep(currentStep)

        btnAction.setOnClickListener {
            if (currentStep < steps.size) {
                steps[currentStep].requestPermission()
            }
        }

        btnNext.setOnClickListener {
            if (currentStep < steps.size - 1) {
                currentStep++
                showStep(currentStep)
            } else {
                finishOnboarding()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showStep(currentStep)
    }

    private fun showStep(step: Int) {
        if (step >= steps.size) {
            finishOnboarding()
            return
        }

        val s = steps[step]
        stepIndicator.text = "${step + 1} / ${steps.size}"
        stepTitle.text = s.title
        stepDesc.text = s.desc
        btnAction.text = s.actionLabel

        val granted = s.checkPermission()
        stepStatus.text = if (granted) "✅ 완료" else "⚠️ 미설정"
        stepStatus.setTextColor(
            if (granted) getColor(android.R.color.holo_green_dark)
            else getColor(android.R.color.holo_orange_dark)
        )

        btnNext.text = if (step == steps.size - 1) getString(R.string.onboarding_complete) else "다음"

        val guide = PermissionHelper.getManufacturerGuideMessage()
        if (step == 0 && guide != null) {
            manufacturerGuide.text = guide
            manufacturerGuide.visibility = View.VISIBLE
        } else {
            manufacturerGuide.visibility = View.GONE
        }
    }

    private fun finishOnboarding() {
        TokenStore.setOnboardingDone(this)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        showStep(currentStep)
    }
}
