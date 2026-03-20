package com.example.unimarketfrontend.network.model

// Event schema posted to the analytics engine
data class AnalyticsEvent(
    val event_name: String,
    val user_id: Int,
    val properties: Map<String, String>
)