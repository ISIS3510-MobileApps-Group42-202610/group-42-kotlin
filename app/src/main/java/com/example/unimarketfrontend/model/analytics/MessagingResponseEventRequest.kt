package com.example.unimarketfrontend.model.analytics

// Schema que espera el backend Django para BQ4 (MessagingResponseEvent)
data class MessagingResponseEventRequest(
    val event_name: String,
    val user_id: Int,
    val seller_id: Int,
    val avg_response_minutes: Double,
    val unread_conversations: Int,
    val timestamp: String,
    val properties: Map<String, String> = emptyMap()
)