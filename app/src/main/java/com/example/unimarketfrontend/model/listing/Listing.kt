package com.example.unimarketfrontend.model.listing

data class Listing(
    val id: Int,
    val seller_id: Int,
    val owner_user_id: Int? = null, // El ID de la tabla users, no de sellers
    val buyer_id: Int?,
    val course_id: Int?,
    val title: String,
    val product: String?,
    val category: String?,
    val condition: String?,
    val original_price: Double?,
    val selling_price: Double,
    val created_at: String,
    val updated_at: String,
    val active: Boolean,
    val images: List<ListingImage>?,
    val priceHistory: List<HistoricPrice>?
)