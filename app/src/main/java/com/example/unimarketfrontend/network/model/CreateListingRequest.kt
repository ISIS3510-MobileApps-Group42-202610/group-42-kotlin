package com.example.unimarketfrontend.network.model

data class CreateListingRequest(
    val title: String,
    val product: String?,
    val category: String?,
    val condition: String?,
    val original_price: Double?,
    val selling_price: Double,
    val course_id: Int?
)