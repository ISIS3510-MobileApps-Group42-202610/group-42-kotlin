

package com.example.unimarketfrontend.analytics

import com.example.unimarketfrontend.analytics.model.BusinessEventRequest
import com.example.unimarketfrontend.analytics.model.PerformanceEventRequest
import com.example.unimarketfrontend.analytics.model.PerformanceTelemetryRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AnalyticsApiService {
    //BQ4 EN EL CHAT VIEW MODEL COMPARA TIMESTAMP DE TIEMPO RTA DEL VENDEDOR
    //ANALYTICS APPY SERVICE, ENVÍA EVENTOS Y LA RTA A LOS DIF ENDPOINTS DEL BACK
    //BACKEND PYTHON/DJANGO

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
