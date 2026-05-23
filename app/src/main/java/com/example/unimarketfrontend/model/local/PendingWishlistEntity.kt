package com.example.unimarketfrontend.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_wishlist_actions")
data class PendingWishlistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listingId: Int,
    val action: String, // "ADD" or "REMOVE"
    val timestamp: Long = System.currentTimeMillis()
)
