package com.futuresjournal.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.futuresjournal.app.api.ApiClient
import com.futuresjournal.app.auth.TokenStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var tokenInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tokenInput = findViewById(R.id.token_input)
        loginBtn = findViewById(R.id.login_btn)
        progress = findViewById(R.id.login_progress)

        loginBtn.setOnClickListener {
            val token = tokenInput.text.toString().trim()
            if (token.isEmpty()) return@setOnClickListener
            attemptLogin(token)
        }
    }

    private fun attemptLogin(token: String) {
        loginBtn.isEnabled = false
        progress.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = ApiClient.verifyToken(token)
                if (user != null) {
                    TokenStore.saveToken(this@LoginActivity, token, user.id)
                    try {
                        val fcmToken = FirebaseMessaging.getInstance().token.await()
                        ApiClient.registerDevice(fcmToken)
                    } catch (e: Exception) {
                        // FCM 등록 실패해도 로그인은 진행
                    }
                    withContext(Dispatchers.Main) {
                        startActivity(Intent(this@LoginActivity, OnboardingActivity::class.java))
                        finish()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, getString(R.string.token_error), Toast.LENGTH_SHORT).show()
                        loginBtn.isEnabled = true
                        progress.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, getString(R.string.token_error), Toast.LENGTH_SHORT).show()
                    loginBtn.isEnabled = true
                    progress.visibility = View.GONE
                }
            }
        }
    }
}
