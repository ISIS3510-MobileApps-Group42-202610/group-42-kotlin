package com.example.unimarketfrontend.model.listing

data class MyListingsResponse(
    val active: List<Listing>,
    val sold: List<Listing>
)