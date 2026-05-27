package com.example.unimarketfrontend.model.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarketfrontend.model.local.CachedCourseEntity

@Dao
interface CourseDao {
    @Query("SELECT * FROM cached_courses ORDER BY code ASC")
    suspend fun getCourses(): List<CachedCourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCourses(courses: List<CachedCourseEntity>)

    @Query("DELETE FROM cached_courses")
    suspend fun clearCourses()

    @Query("SELECT MAX(cachedAt) FROM cached_courses")
    suspend fun getLastUpdated(): Long?
}
