package com.example.unimarketfrontend.model.explore

import com.example.unimarketfrontend.model.course.Course
import com.example.unimarketfrontend.model.listing.ListingCategory
import com.example.unimarketfrontend.model.listing.ListingCondition
import com.example.unimarketfrontend.model.repository.DataSource
import com.example.unimarketfrontend.viewmodel.ExploreListingUiItem

enum class ExploreMode {
    CATEGORIES,
    RESULTS
}

enum class ExploreSort {
    RECENT,
    LOWEST_PRICE,
    HIGHEST_PRICE
}

enum class ExploreFilterSection {
    FEATURED,
    CATEGORIES,
    FACULTY,
    ACADEMIC_AREA,
    COURSE,
    CONDITION,
    PRICE,
    SORT_BY
}

data class ExploreFilters(
    val category: ListingCategory? = null,
    val faculty: String? = null,
    val departmentCode: String? = null,
    val course: Course? = null,
    val minPrice: String = "",
    val maxPrice: String = "",
    val condition: ListingCondition? = null,
    val sort: ExploreSort = ExploreSort.RECENT,
    val searchQuery: String = "",
    val courseSearchQuery: String = ""
)

data class ExploreUiState(
    val mode: ExploreMode = ExploreMode.CATEGORIES,
    val isLoading: Boolean = false,
    val filteredListings: List<ExploreListingUiItem> = emptyList(),
    val courses: List<Course> = emptyList(),
    val coursesById: Map<Int, Course> = emptyMap(),
    val availableFaculties: List<String> = emptyList(),
    val availableDepartmentCodes: List<String> = emptyList(),
    val availableCourses: List<Course> = emptyList(),
    val departmentsByFaculty: Map<String, List<String>> = emptyMap(),
    val coursesByDepartment: Map<String, List<Course>> = emptyMap(),
    val appliedFilters: ExploreFilters = ExploreFilters(),
    val draftFilters: ExploreFilters = ExploreFilters(),
    val selectedSubcategoryId: String? = null,
    val selectedSubcategoryLabel: String? = null,
    val selectedSubcategory: ExploreSubcategory? = null,
    val wishlistListingIds: Set<Int> = emptySet(),
    val isFilterOverlayOpen: Boolean = false,
    val selectedFilterSection: ExploreFilterSection = ExploreFilterSection.FEATURED,
    val resultCount: Int = 0,
    val courseDataSource: DataSource? = null,
    val coursesLastUpdated: Long? = null,
    val errorMessage: String? = null
)
