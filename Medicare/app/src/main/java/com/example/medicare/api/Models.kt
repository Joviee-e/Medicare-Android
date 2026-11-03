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

data class ForgotPasswordResponse(
    val success: Boolean,
    val message: String?,
    @SerializedName("reset_code") val resetCode: String? = null
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
    val phone: String,
    @SerializedName("country_code") val countryCode: String? = null,
    @SerializedName("phone_national") val phoneNational: String? = null
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
    @SerializedName("accessibility_settings") val accessibilitySettings: AccessibilitySettings?,
    @SerializedName("phone_country_code") val phoneCountryCode: String? = null,
    @SerializedName("phone_national") val phoneNational: String? = null
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
    @SerializedName("accessibility_settings") val accessibilitySettings: AccessibilitySettings,
    @SerializedName("phone_country_code") val phoneCountryCode: String?,
    @SerializedName("phone_national") val phoneNational: String?
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
    @SerializedName("medicine_id") val medicineId: String?,
    val medication: MedicationConcept? = null,
    val alerts: List<MedicationAlert>? = null,
    val guidance: List<MedicationGuidance>? = null,
    val ml: MlAdherence? = null,
    @SerializedName("medication_info") val medicationInfo: MedicationInfo? = null
)

// AI Assistant DTOs
data class ChatRequest(
    val message: String,
    val context: Map<String, String>? = null
)

data class ChatResponse(
    val success: Boolean,
    val reply: String?,
    val model: String? = null,
    val disclaimer: String? = null,
    val message: String? = null
)

// Medication Intelligence & Safety DTOs
data class MedicationConcept(
    val name: String,
    @SerializedName("normalized_name") val normalizedName: String? = null,
    val rxcui: String? = null,
    val ingredients: List<String>? = null,
    @SerializedName("drug_class") val drugClass: String? = null
)

data class MedicationAlert(
    val type: String,
    val priority: String,
    val title: String,
    val message: String,
    @SerializedName("matched_allergen") val matchedAllergen: String? = null,
    @SerializedName("other_medication") val otherMedication: String? = null,
    val source: String? = null
)

data class MedicationGuidance(
    val type: String,
    val category: String? = null,
    val message: String,
    val source: String? = null
)

data class MlAdherence(
    @SerializedName("adherence_risk") val adherenceRisk: String,
    val confidence: Float? = null,
    val insight: String? = null,
    val priority: String? = null
)

data class MedicationInfo(
    val medicine: String? = null,
    val uses: List<String>? = null,
    @SerializedName("common_side_effects") val commonSideEffects: List<String>? = null,
    val precautions: List<String>? = null,
    @SerializedName("supportive_recommendations") val supportiveRecommendations: List<String>? = null,
    @SerializedName("dosage_evaluation") val dosageEvaluation: String? = null,
    val source: String? = null,
    @SerializedName("source_url") val sourceUrl: String? = null,
    @SerializedName("source_updated") val sourceUpdated: String? = null
)

data class AnalyzeMedicationRequest(
    val name: String,
    val dosage: String? = null
)

data class AnalyzeMedicationResponse(
    val success: Boolean,
    val medication: MedicationConcept? = null,
    val alerts: List<MedicationAlert>? = null,
    val guidance: List<MedicationGuidance>? = null,
    val ml: MlAdherence? = null,
    @SerializedName("medication_info") val medicationInfo: MedicationInfo? = null,
    val message: String? = null
)

// Notification DTOs
data class ApiNotification(
    @SerializedName("_id") val id: String,
    @SerializedName("patient_id") val patientId: String,
    @SerializedName("medicine_id") val medicineId: String? = null,
    @SerializedName("medicine_name") val medicineName: String? = null,
    val title: String,
    val message: String,
    val priority: String,
    val type: String,
    @SerializedName("context_for_ai") val contextForAi: String? = null,
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)

data class GetNotificationsResponse(
    val success: Boolean,
    val count: Int,
    val notifications: List<ApiNotification>
)

data class MlAdherenceResponse(
    val success: Boolean,
    val ml: MlAdherence
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
    @SerializedName("address_line1") val addressLine1: String? = null,
    val street: String? = null,
    val suburb: String? = null,
    val city: String? = null,
    val categories: List<String>? = null,
    val distance: Double? = null,
    val website: String? = null,
    val contact: GeoapifyContact? = null
)

data class GeoapifyContact(
    val phone: String? = null,
    val email: String? = null
)

// ==========================================
// Predictive Intelligence & ML Enhancements
// ==========================================

data class PredictiveInsightsResponse(
    val success: Boolean,
    val insights: PredictiveInsightsData? = null,
    val message: String? = null
)

data class PredictiveInsightsData(
    val status: String,
    @SerializedName("patient_id") val patientId: String? = null,
    @SerializedName("total_active_medications") val totalActiveMedications: Int = 0,
    @SerializedName("total_logged_doses") val totalLoggedDoses: Int = 0,
    val disclaimer: String? = null,
    val adherence: AdherenceInsightDto? = null,
    val routine: RoutineInsightDto? = null,
    @SerializedName("reminder_optimization") val reminderOptimization: ReminderOptimizationDto? = null
)

data class AdherenceInsightDto(
    val status: String,
    val message: String? = null,
    @SerializedName("minimum_required_doses") val minimumRequiredDoses: Int = 5,
    @SerializedName("current_logged_doses") val currentLoggedDoses: Int = 0,
    @SerializedName("historical_adherence_pct") val historicalAdherencePct: Float = 0f,
    @SerializedName("predicted_miss_probability") val predictedMissProbability: Float = 0f,
    @SerializedName("risk_level") val riskLevel: String = "Low",
    @SerializedName("confidence_score") val confidenceScore: Float = 0f,
    @SerializedName("total_logged_doses") val totalLoggedDoses: Int = 0,
    @SerializedName("missed_doses_total") val missedDosesTotal: Int = 0,
    @SerializedName("delayed_doses_total") val delayedDosesTotal: Int = 0,
    @SerializedName("contributing_factors") val contributingFactors: List<String> = emptyList()
)

data class RoutineBreakdownDto(
    val morning: Float = 0f,
    val afternoon: Float = 0f,
    val evening: Float = 0f,
    val weekend: Float = 0f
)

data class RoutineInsightDto(
    val status: String,
    val message: String? = null,
    val archetype: String = "Establishing Baseline",
    val description: String? = null,
    @SerializedName("cluster_id") val clusterId: Int = -1,
    @SerializedName("time_of_day_breakdown") val timeOfDayBreakdown: RoutineBreakdownDto? = null,
    val recommendations: List<String> = emptyList()
)

data class ReminderSuggestionDto(
    @SerializedName("suggestion_id") val suggestionId: String,
    @SerializedName("medicine_id") val medicineId: String,
    @SerializedName("medicine_name") val medicineName: String,
    @SerializedName("current_reminder_time") val currentReminderTime: String,
    @SerializedName("suggested_reminder_time") val suggestedReminderTime: String,
    @SerializedName("delay_minutes_observed") val delayMinutesObserved: Float = 0f,
    @SerializedName("predicted_delay_minutes") val predictedDelayMinutes: Float = 0f,
    @SerializedName("shift_minutes") val shiftMinutes: Int = 0,
    val rationale: String? = null,
    val status: String = "pending_user_acceptance",
    @SerializedName("safety_confirmation_required") val safetyConfirmationRequired: Boolean = true
)

data class ReminderOptimizationDto(
    val status: String,
    val message: String? = null,
    @SerializedName("most_responsive_period") val mostResponsivePeriod: String = "Not enough data",
    @SerializedName("least_responsive_period") val leastResponsivePeriod: String = "Not enough data",
    @SerializedName("suggestions_count") val suggestionsCount: Int = 0,
    val suggestions: List<ReminderSuggestionDto> = emptyList(),
    @SerializedName("safety_notice") val safetyNotice: String? = null
)

data class AcceptReminderSuggestionRequest(
    @SerializedName("medicine_id") val medicineId: String,
    @SerializedName("current_reminder_time") val currentReminderTime: String,
    @SerializedName("suggested_reminder_time") val suggestedReminderTime: String,
    @SerializedName("confirmed_by_user") val confirmedByUser: Boolean = true
)

