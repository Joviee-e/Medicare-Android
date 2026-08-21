package com.example.medicare

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("medicare_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ROLE = "role"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "name"
        private const val KEY_ONBOARDING_STATUS = "onboarding_status"
    }

    fun saveSession(accessToken: String, refreshToken: String, userId: String, role: String, email: String, name: String, onboardingStatus: String) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_ID, userId)
            putString(KEY_ROLE, role)
            putString(KEY_EMAIL, email)
            putString(KEY_NAME, name)
            putString(KEY_ONBOARDING_STATUS, onboardingStatus)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserRole(): String? = prefs.getString(KEY_ROLE, null)
    fun getUserEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getUserName(): String? = prefs.getString(KEY_NAME, null)
    fun getOnboardingStatus(): String = prefs.getString(KEY_ONBOARDING_STATUS, "NOT_STARTED") ?: "NOT_STARTED"

    fun saveUserName(name: String) {
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    fun saveOnboardingStatus(status: String) {
        prefs.edit().putString(KEY_ONBOARDING_STATUS, status).apply()
    }

    fun isLoggedIn(): Boolean = getAccessToken() != null

    fun logout() {
        prefs.edit().clear().apply()
    }
}
