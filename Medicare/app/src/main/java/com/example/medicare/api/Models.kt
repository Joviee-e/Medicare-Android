package com.example.medicare.api

import com.google.gson.annotations.SerializedName

// General Base Response
data class BaseResponse(
    val success: Boolean,
    val message: String?
)

// Auth DTOs
data class RegisterRequest(
    val email: String,
    val password: String,
    val role: String,
    val name: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    val role: String?,
    @SerializedName("user_id") val userId: String?,
    val name: String?
)

// Profile DTOs
data class AccessibilitySettings(
    @SerializedName("contrast_mode") val contrastMode: Boolean,
    @SerializedName("voice_input") val voiceInput: Boolean,
    @SerializedName("haptic_feedback") val hapticFeedback: Boolean,
    @SerializedName("font_size") val fontSize: Int
)

data class PatientProfile(
    @SerializedName("_id") val id: String,
    val name: String,
    @SerializedName("blood_group") val bloodGroup: String?,
    @SerializedName("emergency_contact_name") val emergencyContactName: String?,
    @SerializedName("emergency_contact_phone") val emergencyContactPhone: String?,
    @SerializedName("accessibility_settings") val accessibilitySettings: AccessibilitySettings?
)

data class ProfileResponse(
    val success: Boolean,
    val profile: PatientProfile?
)

data class UpdateProfileRequest(
    val name: String,
    @SerializedName("blood_group") val bloodGroup: String,
    @SerializedName("emergency_contact_name") val emergencyContactName: String,
    @SerializedName("emergency_contact_phone") val emergencyContactPhone: String,
    @SerializedName("accessibility_settings") val accessibilitySettings: AccessibilitySettings
)

// Medicine DTOs
data class MedicineRequest(
    val name: String,
    val type: String,
    val dosage: String,
    val frequency: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("reminder_times") val reminderTimes: List<String>
)

data class MedicineResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("medicine_id") val medicineId: String?
)

data class ApiLog(
    val date: String,
    val time: String,
    val status: String
)

data class ApiMedicine(
    @SerializedName("_id") val id: String,
    @SerializedName("patient_id") val patientId: String,
    val name: String,
    val type: String,
    val dosage: String,
    val frequency: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("reminder_times") val reminderTimes: List<String>,
    val logs: List<ApiLog>
)

data class GetMedicinesResponse(
    val success: Boolean,
    val medicines: List<ApiMedicine>
)

data class LogRequest(
    val date: String,
    val time: String,
    val status: String
)
