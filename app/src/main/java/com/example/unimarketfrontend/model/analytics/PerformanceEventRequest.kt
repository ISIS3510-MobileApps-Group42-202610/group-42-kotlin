package com.example.unimarketfrontend.model.analytics

enum class PerformanceEventType(val wireValue: String) {
    APP_STARTUP("app_startup"),
    SCREEN_NAVIGATION("screen_navigation")
}

data class PerformanceTelemetryRequest(
    val event_type: String,
    val device_model: String,
    val platform: String = "android",
    val duration_ms: Long,
    val os_version: String,
    val app_version: String
)