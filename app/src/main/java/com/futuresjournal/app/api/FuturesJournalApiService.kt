package com.futuresjournal.app.api

import com.futuresjournal.app.api.models.CancelEmergencyRequest
import com.futuresjournal.app.api.models.DeviceRegistrationRequest
import com.futuresjournal.app.api.models.ProceedEmergencyRequest
import com.futuresjournal.app.api.models.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface FuturesJournalApiService {

    @GET("/api/users/me")
    suspend fun getMe(
        @Header("Authorization") token: String
    ): Response<UserResponse>

    @POST("/api/devices/register")
    suspend fun registerDevice(
        @Header("Authorization") token: String,
        @Body body: DeviceRegistrationRequest
    ): Response<Unit>

    @DELETE("/api/devices/{id}")
    suspend fun unregisterDevice(
        @Header("Authorization") token: String,
        @Path("id") deviceId: String
    ): Response<Unit>

    @POST("/api/emergency/cancel")
    suspend fun cancelEmergency(
        @Header("Authorization") token: String,
        @Body body: CancelEmergencyRequest
    ): Response<Unit>

    @POST("/api/emergency/proceed")
    suspend fun proceedEmergency(
        @Header("Authorization") token: String,
        @Body body: ProceedEmergencyRequest
    ): Response<Unit>
}
