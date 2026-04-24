package com.example.unimarketfrontend.model.listing

data class ListingImage(
    val id: Int,
    val listing_id: Int,
    val is_primary: Boolean,
    val uploaded_at: String,
    val url: String
)