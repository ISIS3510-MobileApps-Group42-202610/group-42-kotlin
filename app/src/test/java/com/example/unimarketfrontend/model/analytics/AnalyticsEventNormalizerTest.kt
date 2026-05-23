package com.example.unimarketfrontend.model.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalyticsEventNormalizerTest {

    @Test
    fun normalize_mapsCamelCaseAndDerivesDepartmentCode() {
        val params = mapOf(
            "courseId" to "20",
            "courseCode" to "ISIS-1221",
            "sourceScreen" to "Explore"
        )

        val normalized = AnalyticsEventNormalizer.normalize(params)

        assertEquals(20, normalized.courseId)
        assertEquals("ISIS-1221", normalized.courseCode)
        assertEquals("ISIS", normalized.departmentCode)
        assertEquals("Explore", normalized.sourceScreen)
    }

    @Test
    fun normalize_excludesKnownKeysFromMetadata() {
        val params = mapOf(
            "course_id" to "1",
            "listing_id" to "999",
            "custom_flag" to "true"
        )

        val normalized = AnalyticsEventNormalizer.normalize(params)

        assertEquals("true", normalized.metadata["custom_flag"])
        assertNull(normalized.metadata["course_id"])
        assertNull(normalized.metadata["listing_id"])
    }
}

