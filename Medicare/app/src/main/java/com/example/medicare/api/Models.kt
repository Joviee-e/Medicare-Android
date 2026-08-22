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

data class GoogleLoginRequest(
    @SerializedName("id_token") val idToken: String,
    val role: String = "patient"
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    @SerializedName("new_password") val newPassword: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    val role: String?,
    @SerializedName("user_id") val userId: String?,
    val name: String?,
    @SerializedName("onboarding_status") val onboardingStatus: String?
)

// Profile DTOs
data class AccessibilitySettings(
    @SerializedName("contrast_mode") val contrastMode: Boolean,
    @SerializedName("voice_input") val voiceInput: Boolean,
    @SerializedName("haptic_feedback") val hapticFeedback: Boolean,
    @SerializedName("font_size") val fontSize: Int
)

data class EmergencyContact(
    val name: String,
    val relationship: String,
    val phone: String
)

data class MedicalInformation(
    val allergies: String,
    val conditions: String,
    val medications: String
)

data class PatientProfile(
    @SerializedName("_id") val id: String,
    val name: String,
    @SerializedName("blood_group") val bloodGroup: String?,
    @SerializedName("emergency_contact_name") val emergencyContactName: String?,
    @SerializedName("emergency_contact_phone") val emergencyContactPhone: String?,
    @SerializedName("emergency_contacts") val emergencyContacts: List<EmergencyContact>?,
    @SerializedName("date_of_birth") val dateOfBirth: String?,
    val age: String?,
    val gender: String?,
    val phone: String?,
    val address: String?,
    @SerializedName("medical_information") val medicalInformation: MedicalInformation?,
    @SerializedName("onboarding_status") val onboardingStatus: String?,
    @SerializedName("completion_percentage") val completionPercentage: Int?,
    @SerializedName("accessibility_settings") val accessibilitySettings: AccessibilitySettings?
)

data class ProfileResponse(
    val success: Boolean,
    val profile: PatientProfile?
)

data class UpdateProfileRequest(
    val name: String,
    @SerializedName("blood_group") val bloodGroup: String,
    @SerializedName("emergency_contacts") val emergencyContacts: List<EmergencyContact>,
    @SerializedName("date_of_birth") val dateOfBirth: String,
    val age: String,
    val gender: String,
    val phone: String,
    val address: String,
    @SerializedName("medical_information") val medicalInformation: MedicalInformation,
    @SerializedName("onboarding_status") val onboardingStatus: String,
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

// Geoapify Places Response models
data class GeoapifyPlacesResponse(
    val features: List<GeoapifyFeature>
)

data class GeoapifyFeature(
    val properties: GeoapifyProperties,
    val geometry: GeoapifyGeometry
)

data class GeoapifyGeometry(
    val coordinates: List<Double> // [longitude, latitude]
)

data class GeoapifyProperties(
    @SerializedName("place_id") val placeId: String,
    val name: String? = null,
    val formatted: String? = null,
    val categories: List<String>? = null,
    val distance: Double? = null,
    val website: String? = null,
    val contact: GeoapifyContact? = null
)

data class GeoapifyContact(
    val phone: String? = null,
    val email: String? = null
)
