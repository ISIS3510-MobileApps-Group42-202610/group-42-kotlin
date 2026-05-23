package com.example.unimarketfrontend.model.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.unimarketfrontend.model.local.PendingWishlistEntity

@Dao
interface PendingWishlistDao {
    @Insert
    suspend fun insert(action: PendingWishlistEntity)

    @Query("SELECT * FROM pending_wishlist_actions ORDER BY timestamp ASC")
    suspend fun getAllPending(): List<PendingWishlistEntity>

    @Query("DELETE FROM pending_wishlist_actions WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM pending_wishlist_actions WHERE listingId = :listingId AND action = :action")
    suspend fun deleteByListingAndAction(listingId: Int, action: String)
}
