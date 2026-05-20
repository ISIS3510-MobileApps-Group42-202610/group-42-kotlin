package com.example.unimarketfrontend.model.local.dao

import androidx.room.*
import com.example.unimarketfrontend.model.local.WishlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist ORDER BY cachedAt DESC")
    fun observeWishlist(): Flow<List<WishlistEntity>>

    @Query("SELECT COUNT(*) FROM wishlist WHERE listingId = :listingId")
    suspend fun isInWishlist(listingId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WishlistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WishlistEntity>)

    @Query("DELETE FROM wishlist WHERE listingId = :listingId")
    suspend fun delete(listingId: Int)

    @Query("DELETE FROM wishlist")
    suspend fun clearAll()

    @Query("SELECT MIN(cachedAt) FROM wishlist")
    suspend fun getOldestCachedAt(): Long?
}
