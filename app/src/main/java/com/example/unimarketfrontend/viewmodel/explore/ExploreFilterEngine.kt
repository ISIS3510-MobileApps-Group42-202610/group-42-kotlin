package com.example.unimarketfrontend.viewmodel.explore

import com.example.unimarketfrontend.model.course.Course
import com.example.unimarketfrontend.model.explore.ExploreFilters
import com.example.unimarketfrontend.model.explore.ExploreSort
import com.example.unimarketfrontend.model.explore.ExploreSubcategory
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.listing.matchesAcademicDepartment
import com.example.unimarketfrontend.model.listing.matchesCategory
import com.example.unimarketfrontend.model.listing.matchesCondition
import com.example.unimarketfrontend.model.listing.matchesCourse
import com.example.unimarketfrontend.model.listing.matchesFaculty
import com.example.unimarketfrontend.model.listing.matchesPriceRange
import com.example.unimarketfrontend.model.listing.matchesSearchQuery
import com.example.unimarketfrontend.model.listing.matchesVirtualSubcategory
import com.example.unimarketfrontend.viewmodel.ExploreListingUiItem

object ExploreFilterEngine {
    fun buildResults(
        listings: List<Listing>,
        courses: List<Course>,
        coursesById: Map<Int, Course>,
        filters: ExploreFilters,
        subcategory: ExploreSubcategory?
    ): List<ExploreListingUiItem> {
        val filtered = listings
            .asSequence()
            .filter { it.matchesCategory(filters.category) }
            .filter { it.matchesFaculty(filters.faculty, courses) }
            .filter { it.matchesAcademicDepartment(filters.departmentCode, courses) }
            .filter { it.matchesCourse(filters.course, courses) }
            .filter { it.matchesVirtualSubcategory(subcategory, courses) }
            .filter { it.matchesPriceRange(filters.minPrice, filters.maxPrice) }
            .filter { it.matchesCondition(filters.condition) }
            .filter { it.matchesSearchQuery(filters.searchQuery, courses) }
            .toList()

        val sorted = when (filters.sort) {
            ExploreSort.RECENT -> filtered.sortedByDescending { it.created_at }
            ExploreSort.LOWEST_PRICE -> filtered.sortedBy { it.selling_price }
            ExploreSort.HIGHEST_PRICE -> filtered.sortedByDescending { it.selling_price }
        }

        return sorted.map { listing ->
            val course = listing.course_id?.let { coursesById[it] }
            ExploreListingUiItem(
                id = listing.id,
                title = listing.title,
                price = listing.selling_price,
                imageUrl = listing.images?.firstOrNull()?.url,
                category = listing.category,
                condition = listing.condition,
                courseId = listing.course_id,
                courseCode = course?.code,
                sellerName = null
            )
        }
    }
}
