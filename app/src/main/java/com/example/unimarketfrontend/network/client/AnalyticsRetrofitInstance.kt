package com.example.unimarketfrontend.network.client

import com.example.unimarketfrontend.model.analytics.BusinessEventRequest
import com.example.unimarketfrontend.model.analytics.PerformanceEventRequest
import com.example.unimarketfrontend.model.analytics.PerformanceTelemetryRequest
import com.example.unimarketfrontend.network.interceptor.AnalyticsAuthInterceptor
import com.example.unimarketfrontend.network.model.AnalyticsEvent
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

/*
 * Esta es la unica interfaz para hablar con el motor de analytics (Django).
 * Aca centralizamos todos los eventos: negocio, performance y legacy.
 */
interface AnalyticsApiService {
    @POST("events")
    suspend fun logEvent(@Body event: AnalyticsEvent)

    @POST("api/business-events/")
    suspend fun sendBusinessEvent(@Body request: BusinessEventRequest): Response<Unit>

    @POST("api/performance")
    suspend fun sendPerformanceEvent(@Body request: PerformanceEventRequest): Response<Unit>

    @POST("api/performance")
    suspend fun sendPerformanceTelemetry(@Body request: PerformanceTelemetryRequest): Response<Unit>
}

object AnalyticsRetrofitInstance {
    private const val ANALYTICS_BASE_URL = "https://group-42-analytic-engine-back.vercel.app/"

    private val client = OkHttpClient.Builder()
        .addInterceptor(AnalyticsAuthInterceptor())
        .build()

    val api: AnalyticsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ANALYTICS_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnalyticsApiService::class.java)
    }
}
