package com.example.unimarketfrontend.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishlist")
data class WishlistEntity(
    @PrimaryKey val listingId: Int,
    val title: String,
    val sellingPrice: Double,
    val condition: String?,
    val category: String?,
    val imageUrl: String?,
    val active: Boolean,
    val sellerId: Int,
    val cachedAt: Long = System.currentTimeMillis()
)
