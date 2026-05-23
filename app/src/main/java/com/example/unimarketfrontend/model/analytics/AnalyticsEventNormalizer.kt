package com.example.unimarketfrontend.model.analytics

object AnalyticsEventNormalizer {
    data class NormalizedPayload(
        val listingId: Int?,
        val courseId: Int?,
        val courseCode: String?,
        val courseName: String?,
        val faculty: String?,
        val departmentCode: String?,
        val category: String?,
        val condition: String?,
        val resultCount: Int?,
        val sourceScreen: String,
        val timestamp: String?,
        val metadata: Map<String, String?>
    )

    private val knownKeys = setOf(
        "event_name",
        "user_id",
        "listing_id",
        "listingId",
        "course_id",
        "courseId",
        "course_code",
        "courseCode",
        "course_name",
        "courseName",
        "faculty",
        "department_code",
        "departmentCode",
        "department",
        "category",
        "condition",
        "result_count",
        "resultCount",
        "source_screen",
        "sourceScreen",
        "timestamp"
    )

    fun normalize(
        params: Map<String, String?>,
        defaultSourceScreen: String = "Unknown"
    ): NormalizedPayload {
        val cleanParams = params.mapValues { (_, value) -> value?.trim()?.takeIf { it.isNotEmpty() } }

        val listingId = cleanParams["listing_id"]
            ?: cleanParams["listingId"]
        val courseId = cleanParams["course_id"]
            ?: cleanParams["courseId"]
        val courseCode = cleanParams["course_code"]
            ?: cleanParams["courseCode"]
        val courseName = cleanParams["course_name"]
            ?: cleanParams["courseName"]
        val faculty = cleanParams["faculty"]
        val departmentCode = cleanParams["department_code"]
            ?: cleanParams["departmentCode"]
            ?: cleanParams["department"]
            ?: courseCode?.substringBefore("-")?.takeIf { it.isNotBlank() }
        val category = cleanParams["category"]
        val condition = cleanParams["condition"]
        val resultCount = cleanParams["result_count"]
            ?: cleanParams["resultCount"]
        val sourceScreen = cleanParams["source_screen"]
            ?: cleanParams["sourceScreen"]
            ?: defaultSourceScreen
        val timestamp = cleanParams["timestamp"]

        val metadata = cleanParams
            .filterKeys { key -> key !in knownKeys }

        return NormalizedPayload(
            listingId = listingId?.toIntOrNull(),
            courseId = courseId?.toIntOrNull(),
            courseCode = courseCode,
            courseName = courseName,
            faculty = faculty,
            departmentCode = departmentCode,
            category = category,
            condition = condition,
            resultCount = resultCount?.toIntOrNull(),
            sourceScreen = sourceScreen,
            timestamp = timestamp,
            metadata = metadata
        )
    }
}

