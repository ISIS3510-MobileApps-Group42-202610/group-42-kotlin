package com.example.unimarketfrontend.network.model

data class AddImageRequest(
    val url: String,
    val is_primary: Boolean? = false
)
