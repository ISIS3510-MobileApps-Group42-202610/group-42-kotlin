package com.example.unimarketfrontend.network.model

import com.google.gson.JsonElement

data class Course(
    val id: Int,
    val code: String,
    val name: String,
    val faculty: JsonElement? = null,
    val faculty_name: String? = null
) {
    fun facultyLabel(): String {
        if (!faculty_name.isNullOrBlank()) return faculty_name
        val parsed = when {
            faculty == null || faculty.isJsonNull -> null
            faculty.isJsonPrimitive -> faculty.asString
            faculty.isJsonObject -> faculty.asJsonObject.get("name")?.asString
            else -> null
        }
        return parsed ?: "Unknown faculty"
    }

    fun selectorLabel(): String {
        return "$code - $name (${facultyLabel()})"
    }
}

