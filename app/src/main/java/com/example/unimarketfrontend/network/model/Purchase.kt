package com.example.unimarketfrontend.network.model

data class Purchase(
    val id: Int,
    val listing: Listing?,
    val created_at: String?
)