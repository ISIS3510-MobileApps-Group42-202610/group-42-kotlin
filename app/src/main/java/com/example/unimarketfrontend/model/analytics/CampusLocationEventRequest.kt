package com.example.unimarketfrontend.model.analytics

// Schema que espera el backend Django para BQ10 (CampusLocationEvent)
data class CampusLocationEventRequest(
    val event_name: String,
    val user_id: Int,
    val listing_id: Int,
    val seller_id: Int,
    val building_name: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
    val metadata: Map<String, String> = emptyMap()
)

