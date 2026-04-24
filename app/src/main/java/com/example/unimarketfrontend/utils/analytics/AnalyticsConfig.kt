package com.example.unimarketfrontend.utils.analytics

// Modelo de configuración para el analytics engine
object AnalyticsConfig {
    const val ANALYTICS_BASE_URL = "https://group-42-analytic-engine-back.vercel.app/"

    var authMode: AnalyticsAuthMode = AnalyticsAuthMode.None
    var authToken: String? = null
    var apiKey: String? = null

    val businessEventFlags: MutableMap<String, Boolean> = mutableMapOf(
        "listing_viewed" to true,
        "chat_started" to true,
        "first_message_sent" to true,
        "transaction_completed" to true
    )
}

enum class AnalyticsAuthMode {
    None,
    Token,
    Bearer,
    ApiKey
}




