package com.example.medicare.api

import android.content.Context
import android.content.Intent
import com.example.medicare.LoginActivity
import com.example.medicare.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private var retrofit: Retrofit? = null
    
    // Default loopback connection pointing to Flask host running on developer workstation
    private var baseUrl = "http://10.0.2.2:5000/api/"

    fun setBaseUrl(url: String) {
        baseUrl = url
        retrofit = null // Force client rebuild on next request
    }

    fun getApiService(context: Context): ApiService {
        if (retrofit == null) {
            val sessionManager = SessionManager(context.applicationContext)
            
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor { chain ->
                    val originalRequest = chain.request()
                    val token = sessionManager.getAccessToken()
                    
                    val request = if (token != null) {
                        originalRequest.newBuilder()
                            .header("Authorization", "Bearer $token")
                            .build()
                    } else {
                        originalRequest
                    }
                    
                    val response = chain.proceed(request)
                    
                    // Handle 401 unauthorized errors globally, except during login/register
                    if (response.code == 401) {
                        val path = originalRequest.url.encodedPath
                        if (!path.contains("auth/login") && !path.contains("auth/register")) {
                            sessionManager.logout()
                            val intent = Intent(context.applicationContext, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            context.applicationContext.startActivity(intent)
                        }
                    }
                    
                    response
                }
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }

    /**
     * Parses the error body from a Retrofit Response and extracts the "message" field.
     */
    fun parseErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val jsonObject = JSONObject(errorBody)
                jsonObject.optString("message", "An error occurred")
            } else {
                "An error occurred"
            }
        } catch (e: Exception) {
            "An error occurred"
        }
    }
}
