package com.example.medicare

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.api.ProfileResponse
import com.example.medicare.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val sessionManager = SessionManager(this)
        
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin()
            return
        }
        
        // Session exists! Let's validate/refresh onboarding status with backend
        RetrofitClient.getApiService(this).getProfile()
            .enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val profile = response.body()?.profile
                        if (profile != null) {
                            val status = profile.onboardingStatus ?: "NOT_STARTED"
                            sessionManager.saveOnboardingStatus(status)
                            sessionManager.saveUserName(profile.name)
                            routeBasedOnStatus(status)
                        } else {
                            fallbackToCached(sessionManager)
                        }
                    } else {
                        // If it is 401, RetrofitClient's interceptor redirects to LoginActivity automatically
                        if (response.code() != 401) {
                            fallbackToCached(sessionManager)
                        }
                    }
                }

                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    // Offline or network error: fallback to cached onboarding status
                    fallbackToCached(sessionManager)
                }
            })
    }
    
    private fun fallbackToCached(sessionManager: SessionManager) {
        val cachedStatus = sessionManager.getOnboardingStatus()
        routeBasedOnStatus(cachedStatus)
    }
    
    private fun routeBasedOnStatus(status: String) {
        val intent = if (status == "NOT_STARTED" || status == "IN_PROGRESS") {
            Intent(this, OnboardingActivity::class.java)
        } else {
            Intent(this, HomeActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}