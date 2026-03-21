package com.example.unimarketfrontend.network.model

// Modelo del evento que se manda al analytics engine
// properties es un mapa flexible de clave-valor
data class AnalyticsEvent(
    val event_name: String,
    val user_id: Int,
    val properties: Map<String, String>
)
