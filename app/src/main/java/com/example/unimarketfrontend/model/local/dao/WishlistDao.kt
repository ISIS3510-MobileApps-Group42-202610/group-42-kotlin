package com.example.unimarketfrontend.model.local.dao

import androidx.room.*
import com.example.unimarketfrontend.model.local.WishlistEntity
import kotlinx.coroutines.flow.Flow

/**
 * SPRINT 4 — DAO for wishlist local storage.
 * Uses Flow for reactive updates (Observer pattern via Room).
 */
@Dao
interface WishlistDao {

    /** Observe all wishlist items reactively — UI updates automatically when data changes */
    @Query("SELECT * FROM wishlist ORDER BY cachedAt DESC")
    fun observeWishlist(): Flow<List<WishlistEntity>>

    /** Get all wishlist items as a one-shot query */
    @Query("SELECT * FROM wishlist ORDER BY cachedAt DESC")
    suspend fun getAll(): List<WishlistEntity>

    /** Check if a specific listing is in the wishlist */
    @Query("SELECT COUNT(*) FROM wishlist WHERE listingId = :listingId")
    suspend fun isInWishlist(listingId: Int): Int

    /** Insert or replace a wishlist item */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WishlistEntity)

    /** Insert multiple items (used when refreshing from network) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WishlistEntity>)

    /** Remove a specific item from wishlist */
    @Query("DELETE FROM wishlist WHERE listingId = :listingId")
    suspend fun delete(listingId: Int)

    /** Clear all wishlist items */
    @Query("DELETE FROM wishlist")
    suspend fun clearAll()

    /** Get oldest cached timestamp for TTL check */
    @Query("SELECT MIN(cachedAt) FROM wishlist")
    suspend fun getOldestCachedAt(): Long?
}
