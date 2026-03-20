package com.example.unimarketfrontend.network

import com.example.unimarketfrontend.network.model.AnalyticsEvent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// Separate API interface for the analytics engine
interface AnalyticsApiService {
    @POST("events")
    suspend fun logEvent(@Body event: AnalyticsEvent)
}

// Singleton HTTP client for the analytics engine backend
object AnalyticsRetrofitInstance {

    // Replace with the deployed analytics engine URL
    private const val ANALYTICS_BASE_URL = "https://group-42-analytic-engine-back.vercel.app/"

    val api: AnalyticsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ANALYTICS_BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnalyticsApiService::class.java)
    }
}