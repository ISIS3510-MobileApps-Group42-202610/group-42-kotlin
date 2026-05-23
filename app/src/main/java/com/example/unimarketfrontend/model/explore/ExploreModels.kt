package com.example.unimarketfrontend.model.explore

import com.example.unimarketfrontend.model.listing.ListingCategory

data class ExploreSubcategory(
    val id: String,
    val label: String,
    val parentCategory: ListingCategory,
    val type: ExploreSubcategoryType,
    val faculty: String? = null,
    val departmentCode: String? = null,
    val keywords: List<String> = emptyList()
)

enum class ExploreSubcategoryType {
    FACULTY,
    ACADEMIC_AREA,
    VIRTUAL_KEYWORD
}

