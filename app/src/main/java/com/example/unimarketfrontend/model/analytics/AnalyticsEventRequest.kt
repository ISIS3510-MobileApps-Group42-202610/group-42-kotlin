package com.example.unimarketfrontend.model.analytics

data class AnalyticsEventRequest(
    val event_name: String,
    val user_id: String? = null,
    val listing_id: Int? = null,
    val course_id: Int? = null,
    val course_code: String? = null,
    val course_name: String? = null,
    val faculty: String? = null,
    val department_code: String? = null,
    val category: String? = null,
    val condition: String? = null,
    val result_count: Int? = null,
    val source_screen: String,
    val timestamp: String,
    val metadata: Map<String, String?> = emptyMap()
)
