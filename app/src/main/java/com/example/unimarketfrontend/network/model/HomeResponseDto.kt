package com.example.unimarketfrontend.network.model


data class CategoryRankDto(
    val category: String,
    val count: Int
)

data class HomeResponseDto(
    val recent: List<Listing>,
    val trending: List<Listing>,
    val categories: List<CategoryRankDto>
)