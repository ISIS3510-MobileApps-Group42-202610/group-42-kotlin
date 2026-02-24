package com.example.unimarketfrontend.network.model

data class Listing(
    val id: Int,
    val seller_id: Int,
    val buyer_id: Int?,
    val course_id: Int?,
    val title: String,
    val product: String,
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