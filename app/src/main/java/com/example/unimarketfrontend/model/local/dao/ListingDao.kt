package com.example.unimarketfrontend.model.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.unimarketfrontend.model.local.CachedCourseEntity
import com.example.unimarketfrontend.model.local.ListingEntity

@Dao
interface ListingDao {

    @Query("SELECT * FROM listings ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ListingEntity>

    @Query("SELECT * FROM listings WHERE id = :listingId LIMIT 1")
    suspend fun getById(listingId: Int): ListingEntity?
    @Query("SELECT * FROM listings WHERE active = 1 ORDER BY updatedAt DESC")
    suspend fun getActive(): List<ListingEntity>
    @Upsert
    suspend fun upsertAll(listings: List<ListingEntity>)

    @Upsert
    suspend fun upsert(listing: ListingEntity)

    @Query("DELETE FROM listings")
    suspend fun clearAll()
}

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
