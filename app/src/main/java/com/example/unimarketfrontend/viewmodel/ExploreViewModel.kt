package com.example.unimarketfrontend.viewmodel

import android.app.Application
import android.os.Trace
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.BuildConfig
import com.example.unimarketfrontend.model.course.Course
import com.example.unimarketfrontend.model.course.coursesFor
import com.example.unimarketfrontend.model.course.departmentCodesForFaculty
import com.example.unimarketfrontend.model.course.faculties
import com.example.unimarketfrontend.model.course.matchesCourseSearch
import com.example.unimarketfrontend.model.explore.ExploreSubcategory
import com.example.unimarketfrontend.model.explore.ExploreSubcategoryType
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.listing.ListingCategory
import com.example.unimarketfrontend.model.listing.ListingCondition
import com.example.unimarketfrontend.model.listing.matchesAcademicDepartment
import com.example.unimarketfrontend.model.listing.matchesCategory
import com.example.unimarketfrontend.model.listing.matchesCondition
import com.example.unimarketfrontend.model.listing.matchesCourse
import com.example.unimarketfrontend.model.listing.matchesFaculty
import com.example.unimarketfrontend.model.listing.matchesPriceRange
import com.example.unimarketfrontend.model.listing.matchesSearchQuery
import com.example.unimarketfrontend.model.listing.matchesVirtualSubcategory
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.repository.CourseRepository
import com.example.unimarketfrontend.model.repository.CourseResult
import com.example.unimarketfrontend.model.repository.DataSource
import com.example.unimarketfrontend.model.repository.ListingRepository
import com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider
import com.example.unimarketfrontend.model.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

class ExploreViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository = ListingRepository(
        listingDao = AppDatabase.getInstance(application).listingDao()
    )

    private val courseRepository = CourseRepository(
        courseDao = AppDatabase.getInstance(application).courseDao()
    )

    private val analyticsTracker = BusinessAnalyticsProvider.tracker
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(ExploreUiState(isLoading = true))
    val uiState: StateFlow<ExploreUiState> = _uiState

    private var allListingsCache: List<Listing> = emptyList()
    private var lastEmptySignature: String? = null

    private val searchQueryFlow = MutableStateFlow("")
    private val courseSearchQueryFlow = MutableStateFlow("")

    init {
        loadExploreData()
        observeSearchQueries()
    }

    private fun observeSearchQueries() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    val snapshot = _uiState.value
                    val updatedApplied = snapshot.appliedFilters.copy(searchQuery = query)
                    val updatedDraft = snapshot.draftFilters.copy(searchQuery = query)
                    _uiState.value = updateCourseDerivedState(
                        snapshot.copy(appliedFilters = updatedApplied, draftFilters = updatedDraft)
                    )
                    applyFilters()
                }
        }

        viewModelScope.launch {
            courseSearchQueryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    val snapshot = _uiState.value
                    val updatedDraft = snapshot.draftFilters.copy(courseSearchQuery = query)
                    _uiState.value = updateCourseDerivedState(snapshot.copy(draftFilters = updatedDraft))
                }
        }
    }

    fun loadExploreData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val startMs = if (BuildConfig.DEBUG) System.currentTimeMillis() else 0L
            if (BuildConfig.DEBUG) {
                Trace.beginSection("Explore_loadExploreData")
            }
            try {
                val listings = withContext(Dispatchers.IO) {
                    runCatching { listingRepository.syncSearchListings() }
                        .getOrElse { listingRepository.getCachedActiveListings() }
                }

                val coursesResult = withContext(Dispatchers.IO) { courseRepository.getCourses() }

                val wishlistIds = withContext(Dispatchers.IO) {
                    runCatching { authRepository.getWishlist().map { it.id }.toSet() }
                        .getOrDefault(emptySet())
                }

                val (courses, dataSource, lastUpdated) = when (coursesResult) {
                    is CourseResult.Success -> Triple(coursesResult.courses, coursesResult.source, coursesResult.lastUpdated)
                    is CourseResult.Error -> Triple(emptyList(), null, null)
                }

                allListingsCache = listings

                val baseState = _uiState.value.copy(
                    isLoading = false,
                    courses = courses,
                    wishlistListingIds = wishlistIds,
                    courseDataSource = dataSource,
                    coursesLastUpdated = lastUpdated,
                    errorMessage = if (listings.isEmpty() && courses.isEmpty() && coursesResult is CourseResult.Error) {
                        coursesResult.message
                    } else {
                        null
                    }
                )

                _uiState.value = updateCourseDerivedState(baseState)

                applyFilters()
            } finally {
                if (BuildConfig.DEBUG) {
                    Trace.endSection()
                    Log.d("ExplorePerf", "loadExploreData completed in ${System.currentTimeMillis() - startMs}ms")
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(
            draftFilters = _uiState.value.draftFilters.copy(searchQuery = query)
        )
        trackEvent("explore_search_used", mapOf("query_length" to query.length.toString()))
        searchQueryFlow.value = query
    }

    fun onCategorySelected(category: ListingCategory?) {
        val applied = _uiState.value.appliedFilters.copy(
            category = category,
            faculty = null,
            departmentCode = null,
            course = null,
            courseSearchQuery = ""
        )
        val draft = applied.copy(searchQuery = _uiState.value.draftFilters.searchQuery)
        val resetState = _uiState.value.copy(
            appliedFilters = applied,
            draftFilters = draft,
            selectedSubcategoryId = null,
            selectedSubcategoryLabel = null,
            selectedSubcategory = null,
            mode = ExploreMode.CATEGORIES
        )
        _uiState.value = updateCourseDerivedState(resetState)
        trackEvent("explore_category_selected", mapOf("category" to category?.value))
        applyFilters()
    }

    fun onFacultySelected(faculty: String?) {
        val applied = _uiState.value.appliedFilters.copy(
            faculty = faculty,
            departmentCode = null,
            course = null,
            courseSearchQuery = ""
        )
        val draft = applied.copy(searchQuery = _uiState.value.draftFilters.searchQuery)
        val updated = _uiState.value.copy(
            appliedFilters = applied,
            draftFilters = draft,
            selectedSubcategoryId = faculty?.let { "faculty_${it}" },
            selectedSubcategoryLabel = faculty,
            selectedSubcategory = faculty?.let {
                ExploreSubcategory(
                    id = "faculty_${it}",
                    label = it,
                    parentCategory = _uiState.value.appliedFilters.category ?: ListingCategory.TEXTBOOK,
                    type = ExploreSubcategoryType.FACULTY,
                    faculty = it
                )
            }
        )
        _uiState.value = updateCourseDerivedState(updated)
        trackEvent("explore_faculty_selected", mapOf("faculty" to faculty))
        applyFilters()
    }

    fun onDepartmentCodeSelected(departmentCode: String?) {
        val applied = _uiState.value.appliedFilters.copy(
            departmentCode = departmentCode,
            course = null,
            courseSearchQuery = ""
        )
        val draft = applied.copy(searchQuery = _uiState.value.draftFilters.searchQuery)
        val updated = _uiState.value.copy(
            appliedFilters = applied,
            draftFilters = draft,
            selectedSubcategoryId = departmentCode?.let { "academic_${it}" },
            selectedSubcategoryLabel = departmentCode,
            selectedSubcategory = departmentCode?.let {
                ExploreSubcategory(
                    id = "academic_${it}",
                    label = it,
                    parentCategory = _uiState.value.appliedFilters.category ?: ListingCategory.TEXTBOOK,
                    type = ExploreSubcategoryType.ACADEMIC_AREA,
                    faculty = _uiState.value.appliedFilters.faculty,
                    departmentCode = it
                )
            }
        )
        _uiState.value = updateCourseDerivedState(updated)
        trackEvent("explore_department_selected", mapOf("department_code" to departmentCode))
        if (departmentCode != null) {
            openResultsForSubcategory(_uiState.value.selectedSubcategory)
        }
        applyFilters()
    }

    fun onCourseSelected(course: Course?) {
        val applied = _uiState.value.appliedFilters.copy(course = course)
        val draft = _uiState.value.draftFilters.copy(course = course)
        _uiState.value = updateCourseDerivedState(
            _uiState.value.copy(appliedFilters = applied, draftFilters = draft)
        )
        trackEvent(
            "explore_course_selected",
            mapOf("course_id" to course?.id?.toString(), "course_code" to course?.code)
        )
        applyFilters()
    }

    fun onCourseSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(
            draftFilters = _uiState.value.draftFilters.copy(courseSearchQuery = query)
        )
        courseSearchQueryFlow.value = query
    }

    fun onSubcategorySelected(subcategory: ExploreSubcategory) {
        trackEvent(
            "explore_subcategory_selected",
            mapOf(
                "subcategory_id" to subcategory.id,
                "subcategory_label" to subcategory.label,
                "category" to subcategory.parentCategory.value
            )
        )

        when (subcategory.type) {
            ExploreSubcategoryType.FACULTY -> onFacultySelected(subcategory.faculty)
            ExploreSubcategoryType.ACADEMIC_AREA -> {
                onFacultySelected(subcategory.faculty)
                onDepartmentCodeSelected(subcategory.departmentCode)
            }
            ExploreSubcategoryType.VIRTUAL_KEYWORD -> {
                _uiState.value = _uiState.value.copy(
                    selectedSubcategory = subcategory,
                    selectedSubcategoryId = subcategory.id,
                    selectedSubcategoryLabel = subcategory.label
                )
                openResultsForSubcategory(subcategory)
            }
        }
    }

    fun onDraftCategorySelected(category: ListingCategory?) {
        val draft = _uiState.value.draftFilters.copy(
            category = category,
            faculty = null,
            departmentCode = null,
            course = null,
            courseSearchQuery = ""
        )
        _uiState.value = updateCourseDerivedState(
            _uiState.value.copy(
                draftFilters = draft,
                selectedSubcategoryId = null,
                selectedSubcategoryLabel = null,
                selectedSubcategory = null
            )
        )
    }

    fun onDraftFacultySelected(faculty: String?) {
        val draft = _uiState.value.draftFilters.copy(
            faculty = faculty,
            departmentCode = null,
            course = null,
            courseSearchQuery = ""
        )
        _uiState.value = updateCourseDerivedState(
            _uiState.value.copy(
                draftFilters = draft,
                selectedSubcategoryId = faculty?.let { "faculty_${it}" },
                selectedSubcategoryLabel = faculty,
                selectedSubcategory = faculty?.let {
                    ExploreSubcategory(
                        id = "faculty_${it}",
                        label = it,
                        parentCategory = _uiState.value.draftFilters.category ?: ListingCategory.TEXTBOOK,
                        type = ExploreSubcategoryType.FACULTY,
                        faculty = it
                    )
                }
            )
        )
    }

    fun onDraftDepartmentCodeSelected(departmentCode: String?) {
        val draft = _uiState.value.draftFilters.copy(
            departmentCode = departmentCode,
            course = null,
            courseSearchQuery = ""
        )
        _uiState.value = updateCourseDerivedState(
            _uiState.value.copy(
                draftFilters = draft,
                selectedSubcategoryId = departmentCode?.let { "academic_${it}" },
                selectedSubcategoryLabel = departmentCode,
                selectedSubcategory = departmentCode?.let {
                    ExploreSubcategory(
                        id = "academic_${it}",
                        label = it,
                        parentCategory = _uiState.value.draftFilters.category ?: ListingCategory.TEXTBOOK,
                        type = ExploreSubcategoryType.ACADEMIC_AREA,
                        faculty = _uiState.value.draftFilters.faculty,
                        departmentCode = it
                    )
                }
            )
        )
    }

    fun onDraftCourseSelected(course: Course?) {
        val draft = _uiState.value.draftFilters.copy(course = course)
        _uiState.value = updateCourseDerivedState(_uiState.value.copy(draftFilters = draft))
    }

    fun applyDraftFilters() {
        val updated = _uiState.value.copy(appliedFilters = _uiState.value.draftFilters)
        _uiState.value = updateCourseDerivedState(updated)
        applyFilters()
    }

    fun openResultsForSubcategory(subcategory: ExploreSubcategory?) {
        _uiState.value = _uiState.value.copy(mode = ExploreMode.RESULTS)
        trackEvent(
            "explore_results_opened",
            mapOf(
                "category" to _uiState.value.appliedFilters.category?.value,
                "subcategory_id" to subcategory?.id,
                "subcategory_label" to subcategory?.label
            )
        )
    }

    fun backToCategories() {
        _uiState.value = _uiState.value.copy(mode = ExploreMode.CATEGORIES)
    }

    fun openFilterOverlay(section: ExploreFilterSection = ExploreFilterSection.FEATURED) {
        _uiState.value = updateCourseDerivedState(
            _uiState.value.copy(
                isFilterOverlayOpen = true,
                selectedFilterSection = section,
                draftFilters = _uiState.value.appliedFilters
            )
        )
        trackEvent("explore_filter_overlay_opened", mapOf("section" to section.name.lowercase()))
    }

    fun closeFilterOverlay() {
        _uiState.value = _uiState.value.copy(isFilterOverlayOpen = false)
    }

    fun onMinPriceChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            draftFilters = _uiState.value.draftFilters.copy(minPrice = value)
        )
    }

    fun onMaxPriceChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            draftFilters = _uiState.value.draftFilters.copy(maxPrice = value)
        )
    }

    fun onConditionSelected(condition: ListingCondition?) {
        _uiState.value = _uiState.value.copy(
            draftFilters = _uiState.value.draftFilters.copy(condition = condition)
        )
    }

    fun onSortSelected(sort: ExploreSort) {
        _uiState.value = _uiState.value.copy(
            draftFilters = _uiState.value.draftFilters.copy(sort = sort)
        )
    }

    fun onFilterSectionSelected(section: ExploreFilterSection) {
        _uiState.value = _uiState.value.copy(selectedFilterSection = section)
    }

    fun clearFilters() {
        val cleared = ExploreFilters()
        val resetState = _uiState.value.copy(
            appliedFilters = cleared,
            draftFilters = cleared,
            selectedSubcategory = null,
            selectedSubcategoryId = null,
            selectedSubcategoryLabel = null
        )
        _uiState.value = updateCourseDerivedState(resetState)
        applyFilters()
    }

    fun applyFilters() {
        val snapshot = _uiState.value
        val listingsSnapshot = allListingsCache
        viewModelScope.launch {
            if (BuildConfig.DEBUG) {
                Trace.beginSection("Explore_applyFilters")
            }
            val startMs = if (BuildConfig.DEBUG) System.currentTimeMillis() else 0L
            try {
                val filteredItems = withContext(Dispatchers.Default) {
                    val filtered = filterListings(
                        listings = listingsSnapshot,
                        courses = snapshot.courses,
                        filters = snapshot.appliedFilters,
                        subcategory = snapshot.selectedSubcategory
                    )

                    val sortedListings = when (snapshot.appliedFilters.sort) {
                        ExploreSort.RECENT -> filtered.sortedByDescending { it.created_at }
                        ExploreSort.LOWEST_PRICE -> filtered.sortedBy { it.selling_price }
                        ExploreSort.HIGHEST_PRICE -> filtered.sortedByDescending { it.selling_price }
                    }

                    sortedListings.map { listing ->
                        val course = listing.course_id?.let { snapshot.coursesById[it] }
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

                val resultCount = filteredItems.size

                _uiState.update {
                    it.copy(
                        filteredListings = filteredItems,
                        resultCount = resultCount
                    )
                }

                if (snapshot.appliedFilters.minPrice.isNotBlank() || snapshot.appliedFilters.maxPrice.isNotBlank()) {
                    trackEvent(
                        "explore_price_filter_applied",
                        mapOf(
                            "min_price" to snapshot.appliedFilters.minPrice,
                            "max_price" to snapshot.appliedFilters.maxPrice,
                            "result_count" to resultCount.toString()
                        )
                    )
                }

                if (snapshot.appliedFilters.condition != null) {
                    trackEvent(
                        "explore_condition_filter_applied",
                        mapOf(
                            "condition" to snapshot.appliedFilters.condition.value,
                            "result_count" to resultCount.toString()
                        )
                    )
                }

                if (snapshot.appliedFilters.sort != ExploreSort.RECENT) {
                    trackEvent(
                        "explore_sort_selected",
                        mapOf(
                            "sort_type" to snapshot.appliedFilters.sort.name.lowercase(),
                            "result_count" to resultCount.toString()
                        )
                    )
                }

                trackExploreAnalytics(snapshot, resultCount)
                maybeTrackEmptyResults(snapshot, resultCount)
            } finally {
                if (BuildConfig.DEBUG) {
                    Trace.endSection()
                    Log.d("ExplorePerf", "applyFilters completed in ${System.currentTimeMillis() - startMs}ms")
                }
            }
        }
    }

    private fun filterListings(
        listings: List<Listing>,
        courses: List<Course>,
        filters: ExploreFilters,
        subcategory: ExploreSubcategory?
    ): List<Listing> {
        return listings
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
    }

    fun toggleWishlist(listingId: Int) {
        val snapshot = _uiState.value
        val wasWishlisted = snapshot.wishlistListingIds.contains(listingId)
        val updated = if (wasWishlisted) {
            snapshot.wishlistListingIds - listingId
        } else {
            snapshot.wishlistListingIds + listingId
        }
        
        if (BuildConfig.DEBUG) {
            Log.d("ExplorePerf", "toggleWishlist (optimistic) for $listingId")
        }
        
        _uiState.value = snapshot.copy(wishlistListingIds = updated)

        viewModelScope.launch(Dispatchers.IO) {
            val startMs = if (BuildConfig.DEBUG) System.currentTimeMillis() else 0L
            if (BuildConfig.DEBUG) {
                Trace.beginSection("Explore_toggleWishlist")
            }
            try {
                val result = runCatching {
                    if (wasWishlisted) {
                        authRepository.removeFromWishlist(listingId)
                    } else {
                        authRepository.addToWishlist(listingId)
                    }
                }

                if (result.isFailure) {
                    _uiState.update { current ->
                        current.copy(
                            wishlistListingIds = if (wasWishlisted) {
                                current.wishlistListingIds + listingId
                            } else {
                                current.wishlistListingIds - listingId
                            },
                            errorMessage = "Could not update wishlist"
                        )
                    }
                } else {
                    val eventName = if (wasWishlisted) "wishlist_removed" else "wishlist_added"
                    val listing = allListingsCache.firstOrNull { it.id == listingId }
                    val course = listing?.course_id?.let { id -> snapshot.coursesById[id] }
                    trackEvent(
                        eventName,
                        mapOf(
                            "listing_id" to listingId.toString(),
                            "course_id" to listing?.course_id?.toString(),
                            "course_code" to course?.code,
                            "course_name" to course?.name,
                            "faculty" to course?.faculty,
                            "department_code" to course?.departmentCode,
                            "category" to listing?.category,
                            "source_screen" to "Explore"
                        )
                    )
                }
            } finally {
                if (BuildConfig.DEBUG) {
                    Trace.endSection()
                    Log.d("ExplorePerf", "toggleWishlist (network) completed in ${System.currentTimeMillis() - startMs}ms")
                }
            }
        }
    }

    private fun updateCourseDerivedState(state: ExploreUiState): ExploreUiState {
        if (BuildConfig.DEBUG) {
            Trace.beginSection("Explore_deriveCourseHierarchy")
        }
        val startMs = if (BuildConfig.DEBUG) System.currentTimeMillis() else 0L

        val draft = state.draftFilters
        
        // 1. Ensure maps and base lists are ready
        val faculties = if (state.availableFaculties.isEmpty()) {
            state.courses.faculties()
        } else {
            state.availableFaculties
        }
        
        val deptsByFaculty = if (state.departmentsByFaculty.isEmpty() && state.courses.isNotEmpty()) {
            state.courses.groupBy({ it.faculty }, { it.departmentCode })
                .mapValues { it.value.distinct().sorted() }
        } else {
            state.departmentsByFaculty
        }

        val coursesByDept = if (state.coursesByDepartment.isEmpty() && state.courses.isNotEmpty()) {
            state.courses.groupBy { it.departmentCode }
        } else {
            state.coursesByDepartment
        }

        val coursesById = if (state.coursesById.isEmpty() && state.courses.isNotEmpty()) {
            state.courses.associateBy { it.id }
        } else {
            state.coursesById
        }

        // 2. Derive current selection state
        val facultyValid = draft.faculty == null || faculties.contains(draft.faculty)
        val selectedFaculty = if (facultyValid) draft.faculty else null

        val departmentCodes = if (selectedFaculty != null) {
            deptsByFaculty[selectedFaculty] ?: emptyList()
        } else {
            // If no faculty selected, show all department codes
            deptsByFaculty.values.flatten().distinct().sorted()
        }
        
        val deptValid = draft.departmentCode == null || departmentCodes.contains(draft.departmentCode)
        val selectedDepartment = if (deptValid) draft.departmentCode else null

        val availableCourses = if (selectedDepartment != null) {
            val baseList = coursesByDept[selectedDepartment] ?: emptyList()
            if (draft.courseSearchQuery.isNotBlank()) {
                baseList.filter { it.matchesCourseSearch(draft.courseSearchQuery) }
            } else {
                baseList
            }
        } else if (selectedFaculty != null) {
            // Courses for all departments in this faculty
            val deptCodes = deptsByFaculty[selectedFaculty] ?: emptyList()
            val baseList = deptCodes.flatMap { coursesByDept[it] ?: emptyList() }
            if (draft.courseSearchQuery.isNotBlank()) {
                baseList.filter { it.matchesCourseSearch(draft.courseSearchQuery) }
            } else {
                baseList
            }
        } else if (draft.courseSearchQuery.isNotBlank()) {
            state.courses.filter { it.matchesCourseSearch(draft.courseSearchQuery) }
        } else {
            emptyList()
        }

        val sortedAvailableCourses = availableCourses.sortedBy { it.code }

        val selectedCourse = if (draft.course != null && sortedAvailableCourses.none { it.id == draft.course.id }) {
            null
        } else {
            draft.course
        }

        val updatedDraft = draft.copy(
            faculty = selectedFaculty,
            departmentCode = selectedDepartment,
            course = selectedCourse
        )

        val updatedState = state.copy(
            draftFilters = updatedDraft,
            availableFaculties = faculties,
            availableDepartmentCodes = departmentCodes,
            availableCourses = sortedAvailableCourses,
            departmentsByFaculty = deptsByFaculty,
            coursesByDepartment = coursesByDept,
            coursesById = coursesById
        )

        if (BuildConfig.DEBUG) {
            Trace.endSection()
            Log.d("ExplorePerf", "deriveCourseHierarchy completed in ${System.currentTimeMillis() - startMs}ms")
        }

        return updatedState
    }

    private fun trackExploreAnalytics(state: ExploreUiState, resultCount: Int) {
        val filters = state.appliedFilters
        val params = mutableMapOf(
            "category" to filters.category?.value,
            "faculty" to filters.faculty,
            "department_code" to filters.departmentCode,
            "course_id" to filters.course?.id?.toString(),
            "course_code" to filters.course?.code,
            "course_name" to filters.course?.name,
            "result_count" to resultCount.toString(),
            "source_screen" to "Explore"
        )

        // Identify which event to send based on what changed
        val eventName = when {
            filters.course != null -> "explore_course_selected"
            filters.departmentCode != null -> "explore_department_selected"
            filters.faculty != null -> "explore_faculty_selected"
            filters.category != null -> "explore_category_selected"
            else -> "explore_results_opened"
        }

        trackEvent(eventName, params)
    }

    private fun maybeTrackEmptyResults(state: ExploreUiState, resultCount: Int) {
        if (state.mode != ExploreMode.RESULTS || resultCount != 0) return
        val filters = state.appliedFilters

        val signature = listOf(
            filters.category?.value,
            filters.faculty,
            filters.departmentCode,
            filters.course?.code,
            state.selectedSubcategoryId,
            filters.minPrice,
            filters.maxPrice,
            filters.condition?.value,
            filters.searchQuery
        ).joinToString("|")

        if (signature == lastEmptySignature) return
        lastEmptySignature = signature

        trackEvent(
            "explore_empty_results",
            mapOf(
                "category" to filters.category?.value,
                "subcategory_id" to state.selectedSubcategoryId,
                "subcategory_label" to state.selectedSubcategoryLabel,
                "faculty" to filters.faculty,
                "department_code" to filters.departmentCode,
                "course_id" to filters.course?.id?.toString(),
                "course_code" to filters.course?.code,
                "min_price" to filters.minPrice,
                "max_price" to filters.maxPrice,
                "condition" to filters.condition?.value,
                "result_count" to "0",
                "source_screen" to "Explore"
            )
        )
    }

    private fun buildExploreContextParams(state: ExploreUiState): MutableMap<String, String?> {
        val params = mutableMapOf<String, String?>()
        val filters = state.appliedFilters
        params["source_screen"] = "Explore"
        filters.category?.let { params["category"] = it.value }
        filters.faculty?.let { params["faculty"] = it }
        filters.departmentCode?.let { params["department_code"] = it }
        filters.course?.let { course ->
            params["course_id"] = course.id.toString()
            params["course_code"] = course.code
            params["course_name"] = course.name
        }
        return params
    }

    private fun trackEvent(eventName: String, metadata: Map<String, String?>) {
        val stateSnapshot = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            val baseParams = buildExploreContextParams(stateSnapshot)
            metadata.forEach { (key, value) ->
                if (value != null) {
                    baseParams[key] = value
                }
            }
            try {
                analyticsTracker.trackEvent(eventName, baseParams)
            } catch (_: Exception) {
            }
        }
    }
}
