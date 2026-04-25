package com.example.unimarketfrontend.model.network.api

import com.example.unimarketfrontend.model.analytics.BusinessEventRequest
import com.example.unimarketfrontend.model.analytics.PerformanceEventRequest
import com.example.unimarketfrontend.model.analytics.PerformanceTelemetryRequest
import com.example.unimarketfrontend.network.model.AnalyticsEvent
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AnalyticsApiService {
    @POST("events")
    suspend fun logEvent(@Body event: AnalyticsEvent)

    @POST("api/business-events/")
    suspend fun sendBusinessEvent(
        @Body request: BusinessEventRequest
    ): Response<Unit>

    @POST("api/performance")
    suspend fun sendPerformanceEvent(
        @Body request: PerformanceEventRequest
    ): Response<Unit>

    @POST("api/performance")
    suspend fun sendPerformanceTelemetry(
        @Body request: PerformanceTelemetryRequest
    ): Response<Unit>
}