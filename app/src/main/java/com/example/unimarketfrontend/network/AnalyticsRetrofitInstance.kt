// C:/Users/buu/StudioProjects/group-42-kotlin/app/src/main/java/com/example/unimarketfrontend/network/AnalyticsRetrofitInstance.kt

package com.example.unimarketfrontend.network

import com.example.unimarketfrontend.analytics.model.BusinessEventRequest
import com.example.unimarketfrontend.analytics.model.PerformanceEventRequest
import com.example.unimarketfrontend.analytics.model.PerformanceTelemetryRequest
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
        .addInterceptor(com.example.unimarketfrontend.analytics.AnalyticsAuthInterceptor())
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
