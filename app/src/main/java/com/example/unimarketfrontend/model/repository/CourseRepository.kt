package com.example.unimarketfrontend.model.repository

import com.example.unimarketfrontend.model.course.Course
import com.example.unimarketfrontend.model.local.dao.CourseDao
import com.example.unimarketfrontend.model.mappers.toDomainCourses
import com.example.unimarketfrontend.model.mappers.toEntity
import com.example.unimarketfrontend.model.network.api.ApiService
import com.example.unimarketfrontend.model.network.client.RetrofitInstance

enum class DataSource {
    NETWORK,
    CACHE
}

sealed class CourseResult {
    data class Success(
        val courses: List<Course>,
        val source: DataSource,
        val lastUpdated: Long?
    ) : CourseResult()

    data class Error(
        val message: String
    ) : CourseResult()
}

class CourseRepository(
    private val courseDao: CourseDao,
    private val api: ApiService = RetrofitInstance.api
) {
    suspend fun getCourses(forceRefresh: Boolean = false): CourseResult {
        val cached = courseDao.getCourses()
        val lastUpdated = courseDao.getLastUpdated()

        if (cached.isNotEmpty() && !forceRefresh) {
            return CourseResult.Success(
                courses = cached.toDomainCourses(),
                source = DataSource.CACHE,
                lastUpdated = lastUpdated
            )
        }

        return try {
            val response = api.getCourses()
            if (!response.isSuccessful) {
                val errorBody = try {
                    response.errorBody()?.string()?.take(300)
                } catch (_: Exception) {
                    null
                }
                val message = buildString {
                    append("Could not load courses from network")
                    append(" (HTTP ${response.code()})")
                    if (!response.message().isNullOrBlank()) {
                        append(": ${response.message()}")
                    }
                    if (!errorBody.isNullOrBlank()) {
                        append(" - $errorBody")
                    }
                }
                return if (cached.isNotEmpty()) {
                    CourseResult.Success(
                        courses = cached.toDomainCourses(),
                        source = DataSource.CACHE,
                        lastUpdated = lastUpdated
                    )
                } else {
                    CourseResult.Error(message)
                }
            }

            val remote = response.body().orEmpty()
            val now = System.currentTimeMillis()
            val entities = remote.mapNotNull { it.toEntity(now) }
            if (entities.isNotEmpty()) {
                courseDao.clearCourses()
                courseDao.upsertCourses(entities)
            }
            CourseResult.Success(
                courses = entities.toDomainCourses(),
                source = DataSource.NETWORK,
                lastUpdated = now
            )
        } catch (e: Exception) {
            if (cached.isNotEmpty()) {
                CourseResult.Success(
                    courses = cached.toDomainCourses(),
                    source = DataSource.CACHE,
                    lastUpdated = lastUpdated
                )
            } else {
                CourseResult.Error("Could not load courses from network: ${e.message}")
            }
        }
    }
}
