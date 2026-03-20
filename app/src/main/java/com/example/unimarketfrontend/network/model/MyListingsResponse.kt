package com.example.unimarketfrontend.network.model
data class MyListingsResponse(
    val active: List<Listing>,
    val sold: List<Listing>
)