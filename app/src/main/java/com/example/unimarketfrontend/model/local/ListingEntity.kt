package com.example.unimarketfrontend.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey val id: Int,
    val sellerId: Int,
    val buyerId: Int?,
    val courseId: Int?,
    val title: String,
    val product: String?,
    val category: String?,
    val condition: String?,
    val originalPrice: Double?,
    val sellingPrice: Double,
    val createdAt: String,
    val updatedAt: String,
    val active: Boolean,
    val primaryImageUrl: String?
)