package com.example.unimarketfrontend.analytics.model

data class BusinessEventRequest(
    val event_name: String,
    val listing_id: Int,
    val buyer_user_id: Int?,
    val seller_user_id: Int?,
    val timestamp: String,
    val metadata: Map<String, Any?>? = null,
    val client_event_id: String
)

data class PerformanceEventRequest(
    val metric_name: String,
    val value_ms: Long,
    val timestamp: String,
    val listing_id: Int? = null,
    val screen_name: String? = null,
    val metadata: Map<String, Any?>? = null,
    val client_event_id: String
)
