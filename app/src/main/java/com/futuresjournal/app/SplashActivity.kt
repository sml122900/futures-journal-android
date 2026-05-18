package com.futuresjournal.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.futuresjournal.app.auth.TokenStore

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val next = when {
            !TokenStore.isAuthenticated(this) -> LoginActivity::class.java
            !TokenStore.isOnboardingDone(this) -> OnboardingActivity::class.java
            else -> MainActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }
}
