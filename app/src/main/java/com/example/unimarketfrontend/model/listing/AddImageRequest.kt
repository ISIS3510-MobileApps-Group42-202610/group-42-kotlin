package com.example.unimarketfrontend.model.listing

data class AddImageRequest(
    val url: String,
    val is_primary: Boolean? = false
)