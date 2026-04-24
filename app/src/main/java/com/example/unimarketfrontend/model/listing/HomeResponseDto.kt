package com.example.unimarketfrontend.model.listing


data class CategoryRankDto(
    val category: String,
    val count: Int
)

data class HomeResponseDto(
    val recent: List<Listing>,
    val trending: List<Listing>,
    val categories: List<CategoryRankDto>
)