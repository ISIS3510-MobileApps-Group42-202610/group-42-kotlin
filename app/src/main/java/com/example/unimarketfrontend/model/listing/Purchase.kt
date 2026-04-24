package com.example.unimarketfrontend.model.listing

data class Purchase(
    val id: Int,
    val listing: Listing?,
    val created_at: String?
)