package com.example.unimarketfrontend.model.listing

import com.example.unimarketfrontend.model.course.Course
import com.example.unimarketfrontend.model.explore.ExploreSubcategory
import com.example.unimarketfrontend.model.explore.ExploreSubcategoryType

fun Listing.matchesCategory(category: ListingCategory?): Boolean {
    return category == null || category.value.equals(this.category, ignoreCase = true)
}

fun Listing.matchesFaculty(faculty: String?, courses: List<Course>): Boolean {
    if (faculty == null) return true

    val course = this.course_id?.let { id -> courses.firstOrNull { it.id == id } }
    if (course != null) return course.faculty.equals(faculty, ignoreCase = true)

    return matchesCourseText(courses.filter { it.faculty.equals(faculty, ignoreCase = true) })
}

fun Listing.matchesAcademicDepartment(departmentCode: String?, courses: List<Course>): Boolean {
    if (departmentCode == null) return true

    val course = this.course_id?.let { id -> courses.firstOrNull { it.id == id } }
    if (course != null) return course.departmentCode.equals(departmentCode, ignoreCase = true)

    return matchesCourseText(courses.filter { it.departmentCode.equals(departmentCode, ignoreCase = true) })
}

fun Listing.matchesCourse(course: Course?, courses: List<Course>): Boolean {
    if (course == null) return true

    if (this.course_id == course.id) return true

    val listingText = buildListingText()
    return listingText.contains(course.code, ignoreCase = true) ||
        listingText.contains(course.name, ignoreCase = true)
}

fun Listing.matchesVirtualSubcategory(subcategory: ExploreSubcategory?, courses: List<Course>): Boolean {
    if (subcategory == null) return true
    if (subcategory.type != ExploreSubcategoryType.VIRTUAL_KEYWORD) return true

    val listingText = buildListingText()
    val course = this.course_id?.let { id -> courses.firstOrNull { it.id == id } }
    val courseText = listOfNotNull(course?.code, course?.name)
        .joinToString(" ")
        .ifBlank { "" }

    return subcategory.keywords.any { keyword ->
        listingText.contains(keyword, ignoreCase = true) ||
            courseText.contains(keyword, ignoreCase = true)
    }
}

fun Listing.matchesPriceRange(minPrice: String, maxPrice: String): Boolean {
    val min = minPrice.toDoubleOrNull()
    val max = maxPrice.toDoubleOrNull()

    if (min == null && max == null) return true
    if (min != null && selling_price < min) return false
    if (max != null && selling_price > max) return false
    return true
}

fun Listing.matchesCondition(condition: ListingCondition?): Boolean {
    return condition == null || condition.value.equals(this.condition, ignoreCase = true)
}

fun Listing.matchesSearchQuery(query: String, courses: List<Course>): Boolean {
    if (query.isBlank()) return true

    val normalized = query.trim().lowercase()
    val listingText = buildListingText()
    val course = this.course_id?.let { id -> courses.firstOrNull { it.id == id } }
    val courseText = listOfNotNull(course?.code, course?.name, course?.faculty, course?.departmentCode)
        .joinToString(" ")
        .lowercase()

    return listingText.contains(normalized) || courseText.contains(normalized)
}

private fun Listing.matchesCourseText(courses: List<Course>): Boolean {
    if (courses.isEmpty()) return false

    val listingText = buildListingText()
    return courses.any { course ->
        listingText.contains(course.code, ignoreCase = true) ||
            listingText.contains(course.name, ignoreCase = true)
    }
}

private fun Listing.buildListingText(): String {
    return listOfNotNull(title, product, category, condition)
        .joinToString(" ")
        .lowercase()
}

