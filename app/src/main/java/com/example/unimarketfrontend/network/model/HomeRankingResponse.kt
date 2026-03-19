package com.example.unimarketfrontend.network.model

// Response from GET /api/v1/listings/home/ranking
data class HomeRankingResponse(
    val recent: List<Listing>,
    val trending: List<Listing>,
    val categories: Map<String, Int>
)

