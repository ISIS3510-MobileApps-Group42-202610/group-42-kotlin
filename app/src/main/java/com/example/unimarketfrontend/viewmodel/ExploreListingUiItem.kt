package com.example.unimarketfrontend.viewmodel

data class ExploreListingUiItem(
    val id: Int,
    val title: String,
    val price: Double,
    val imageUrl: String?,
    val category: String?,
    val condition: String?,
    val courseId: Int?,
    val courseCode: String?
)
