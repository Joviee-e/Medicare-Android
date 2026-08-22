package com.example.medicare.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Call

interface GeoapifyService {
    @GET("v2/places")
    fun getNearbyPlaces(
        @Query("categories") categories: String,
        @Query("filter") filter: String,
        @Query("bias") bias: String,
        @Query("limit") limit: Int,
        @Query("name") name: String?,
        @Query("apiKey") apiKey: String
    ): Call<GeoapifyPlacesResponse>
}

object GeoapifyClient {
    private var service: GeoapifyService? = null

    fun getService(): GeoapifyService {
        if (service == null) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.geoapify.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            service = retrofit.create(GeoapifyService::class.java)
        }
        return service!!
    }
}
