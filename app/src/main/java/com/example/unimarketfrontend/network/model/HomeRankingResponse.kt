package com.example.unimarketfrontend.network.model


data class HomeRankingResponse(
    val recent: List<Listing>,
    val trending: List<Listing>,
    val categories: Map<String, Int>
)