package com.example.medicare.api

import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    // Auth endpoints
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    @POST("auth/google")
    fun googleLogin(@Body request: GoogleLoginRequest): Call<AuthResponse>

    @POST("auth/forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<BaseResponse>

    @POST("auth/reset-password")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<BaseResponse>

    @POST("auth/logout")
    fun logout(): Call<BaseResponse>

    // Patient profile endpoints
    @GET("patients/profile")
    fun getProfile(): Call<ProfileResponse>

    @PUT("patients/profile")
    fun updateProfile(@Body request: UpdateProfileRequest): Call<BaseResponse>

    // Medicine endpoints
    @GET("medicines")
    fun getMedicines(): Call<GetMedicinesResponse>

    @POST("medicines")
    fun createMedicine(@Body request: MedicineRequest): Call<MedicineResponse>

    @PUT("medicines/{id}")
    fun updateMedicine(@Path("id") id: String, @Body request: MedicineRequest): Call<BaseResponse>

    @DELETE("medicines/{id}")
    fun deleteMedicine(@Path("id") id: String): Call<BaseResponse>

    @POST("medicines/{id}/log")
    fun logCompliance(@Path("id") id: String, @Body request: LogRequest): Call<BaseResponse>

    // AI Assistant endpoint
    @POST("ai/chat")
    fun chatAi(@Body request: ChatRequest): Call<ChatResponse>

    // Notification endpoints
    @GET("notifications")
    fun getNotifications(@Query("unread_only") unreadOnly: Boolean = false): Call<GetNotificationsResponse>

    @POST("notifications/{id}/read")
    fun markNotificationRead(@Path("id") id: String): Call<BaseResponse>

    @POST("notifications/clear")
    fun clearNotifications(): Call<BaseResponse>

    // Medication Intelligence endpoint
    @POST("medications/analyze")
    fun analyzeMedication(@Body request: AnalyzeMedicationRequest): Call<AnalyzeMedicationResponse>

    // ML Adherence endpoint
    @GET("ml/adherence")
    fun getAdherence(): Call<MlAdherenceResponse>
}
