package com.example.unimarketfrontend.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_courses")
data class CachedCourseEntity(
    @PrimaryKey val id: Int,
    val code: String,
    val name: String,
    val faculty: String,
    val departmentCode: String,
    val cachedAt: Long
)
