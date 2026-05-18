package com.futuresjournal.app.api

import android.content.Context
import android.os.Build
import com.futuresjournal.app.api.models.CancelEmergencyRequest
import com.futuresjournal.app.api.models.DeviceRegistrationRequest
import com.futuresjournal.app.api.models.ProceedEmergencyRequest
import com.futuresjournal.app.api.models.UserResponse
import com.futuresjournal.app.auth.TokenStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    const val BASE_URL = "https://futures-journal-virid.vercel.app"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(FuturesJournalApiService::class.java)

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun bearerToken(): String {
        val ctx = appContext ?: return ""
        return "Bearer ${TokenStore.getToken(ctx) ?: ""}"
    }

    suspend fun verifyToken(token: String): UserInfo? {
        return try {
            val response = api.getMe("Bearer $token")
            if (response.isSuccessful) {
                response.body()?.let { UserInfo(it.id, it.email) }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun registerDevice(fcmToken: String) {
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        api.registerDevice(
            token = bearerToken(),
            body = DeviceRegistrationRequest(fcmToken = fcmToken, deviceName = deviceName)
        )
    }

    suspend fun cancelEmergency(sessionId: String, reason: String = "user_cancelled") {
        api.cancelEmergency(
            token = bearerToken(),
            body = CancelEmergencyRequest(sessionId = sessionId, reason = reason)
        )
    }

    suspend fun proceedEmergency(sessionId: String, typedText: String, durationSeconds: Long = 0) {
        api.proceedEmergency(
            token = bearerToken(),
            body = ProceedEmergencyRequest(
                sessionId = sessionId,
                typedText = typedText,
                durationSeconds = durationSeconds
            )
        )
    }

    data class UserInfo(val id: String, val email: String)
}
