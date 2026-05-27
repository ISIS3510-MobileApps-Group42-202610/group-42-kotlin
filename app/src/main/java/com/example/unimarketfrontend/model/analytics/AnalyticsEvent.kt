package com.example.unimarketfrontend.model.analytics

data class AnalyticsEvent(
    val event_name: String,
    val user_id: Int,
    val properties: Map<String, String>
)