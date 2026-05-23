package com.example.unimarketfrontend.model.network.api

import com.example.unimarketfrontend.model.analytics.AnalyticsEventRequest
import com.example.unimarketfrontend.model.analytics.BusinessEventRequest
import com.example.unimarketfrontend.model.analytics.CampusLocationEventRequest
import com.example.unimarketfrontend.model.analytics.MessagingResponseEventRequest
import com.example.unimarketfrontend.model.analytics.PerformanceEventRequest
import com.example.unimarketfrontend.model.analytics.PerformanceTelemetryRequest
import com.example.unimarketfrontend.network.model.AnalyticsEvent
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AnalyticsApiService {

    // BQ: Demand vs Supply (New endpoint)
    @POST("analytics/events")
    suspend fun sendEvent(
        @Body request: AnalyticsEventRequest
    ): Response<Unit>

    // BQ4 — Seller response time (endpoint correcto del backend)
    // El endpoint /events no existe. BQ4 usa MessagingResponseEvent.
    @POST("api/bq6/events/")
    suspend fun sendMessagingEvent(
        @Body request: MessagingResponseEventRequest
    ): Response<Unit>

    // BQ9/BQ8 — Business events canónicos
    @POST("api/business-events/")
    suspend fun sendBusinessEvent(
        @Body request: BusinessEventRequest
    ): Response<Unit>

    // BQ2 — Performance (startup, navegación)
    @POST("api/performance")
    suspend fun sendPerformanceTelemetry(
        @Body request: PerformanceTelemetryRequest
    ): Response<Unit>

    @POST("api/performance")
    suspend fun sendPerformanceEvent(
        @Body request: PerformanceEventRequest
    ): Response<Unit>

    // BQ10 — Campus location (banner de edificio)
    @POST("api/bq10/events/")
    suspend fun sendCampusEvent(
        @Body request: CampusLocationEventRequest
    ): Response<Unit>
}