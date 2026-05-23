package com.example.unimarketfrontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.unimarketfrontend.model.course.Course
import com.example.unimarketfrontend.model.explore.ExploreSubcategory
import com.example.unimarketfrontend.model.explore.ExploreSubcategoryType
import com.example.unimarketfrontend.model.listing.ListingCategory
import com.example.unimarketfrontend.model.listing.ListingCondition
import com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider
import com.example.unimarketfrontend.model.repository.DataSource
import com.example.unimarketfrontend.ui.components.BottomNavigationBar
import com.example.unimarketfrontend.ui.navigation.ListingRoutesFactory
import com.example.unimarketfrontend.ui.navigation.navigateTracked
import com.example.unimarketfrontend.ui.theme.BackgroundLight
import com.example.unimarketfrontend.ui.theme.TextPrimary
import com.example.unimarketfrontend.viewmodel.ExploreFilterSection
import com.example.unimarketfrontend.viewmodel.ExploreMode
import com.example.unimarketfrontend.viewmodel.ExploreSort
import com.example.unimarketfrontend.viewmodel.ExploreUiState
import com.example.unimarketfrontend.viewmodel.ExploreViewModel
import java.util.Locale

import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.unimarketfrontend.ui.utils.ImageUtils
import com.example.unimarketfrontend.viewmodel.ExploreListingUiItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    navController: NavController,
    viewModel: ExploreViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val analyticsTracker = remember { BusinessAnalyticsProvider.tracker }

    LaunchedEffect(Unit) {
        viewModel.loadExploreData()
        try {
            analyticsTracker.trackCustomEvent("explore_screen_opened")
        } catch (_: Exception) {
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Explore") }) },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "explore",
                onRouteChange = { route ->
                    navController.navigateTracked(route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
                .padding(12.dp)
        ) {
            if (state.mode == ExploreMode.CATEGORIES) {
                CategoryBrowser(
                    state = state,
                    onCategorySelected = viewModel::onCategorySelected,
                    onSubcategorySelected = viewModel::onSubcategorySelected,
                    onFacultySelected = viewModel::onFacultySelected,
                    onDepartmentSelected = viewModel::onDepartmentCodeSelected,
                    onCourseSelected = { course ->
                        viewModel.onCourseSelected(course)
                        viewModel.openResultsForSubcategory(state.selectedSubcategory)
                    }
                )
            } else {
                ExploreResultsPanel(
                    state = state,
                    onListingClick = { item ->
                        try {
                            analyticsTracker.trackCustomEvent(
                                "explore_listing_opened",
                                listingId = item.id,
                                metadata = mapOf(
                                    "category" to item.category,
                                    "course_id" to item.courseId,
                                    "course_code" to item.courseCode
                                )
                            )

                            analyticsTracker.trackEvent(
                                "explore_listing_opened",
                                mapOf(
                                    "listing_id" to item.id.toString(),
                                    "course_id" to item.courseId?.toString(),
                                    "course_code" to item.courseCode,
                                    "category" to item.category,
                                    "condition" to item.condition,
                                    "source_screen" to "Explore"
                                )
                            )
                        } catch (_: Exception) {
                        }
                        navController.navigateTracked(
                            targetRoute = ListingRoutesFactory.detailPath(item.id),
                            destinationKey = ListingRoutesFactory.DETAIL_ROUTE
                        )
                    },
                    onBack = viewModel::backToCategories,
                    onOpenFilters = { viewModel.openFilterOverlay() },
                    onOpenCourseFilters = { viewModel.openFilterOverlay(ExploreFilterSection.COURSE) },
                    onRetry = viewModel::loadExploreData,
                    onWishlistToggle = viewModel::toggleWishlist
                )
            }

            if (state.isFilterOverlayOpen) {
                ExploreFilterOverlay(
                    state = state,
                    selectedFilterSection = state.selectedFilterSection,
                    onFilterSectionSelected = viewModel::onFilterSectionSelected,
                    onCategorySelected = viewModel::onDraftCategorySelected,
                    onFacultySelected = viewModel::onDraftFacultySelected,
                    onDepartmentCodeSelected = viewModel::onDraftDepartmentCodeSelected,
                    onCourseSelected = viewModel::onDraftCourseSelected,
                    onCourseSearchQueryChanged = viewModel::onCourseSearchQueryChanged,
                    onMinPriceChanged = viewModel::onMinPriceChanged,
                    onMaxPriceChanged = viewModel::onMaxPriceChanged,
                    onConditionSelected = viewModel::onConditionSelected,
                    onSortSelected = viewModel::onSortSelected,
                    onClearFilters = viewModel::clearFilters,
                    onApplyFilters = {
                        viewModel.applyDraftFilters()
                        viewModel.closeFilterOverlay()
                        viewModel.openResultsForSubcategory(state.selectedSubcategory)
                    },
                    onDismiss = viewModel::closeFilterOverlay
                )
            }
        }
    }
}

@Composable
private fun CategoryBrowser(
    state: ExploreUiState,
    onCategorySelected: (ListingCategory?) -> Unit,
    onSubcategorySelected: (ExploreSubcategory) -> Unit,
    onFacultySelected: (String?) -> Unit,
    onDepartmentSelected: (String?) -> Unit,
    onCourseSelected: (Course?) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        CategorySidebar(
            selectedCategory = state.appliedFilters.category,
            onCategorySelected = onCategorySelected,
            modifier = Modifier.weight(0.36f).fillMaxSize()
        )

        Spacer(Modifier.width(12.dp))

        SubcategoryPanel(
            state = state,
            onSubcategorySelected = onSubcategorySelected,
            onFacultySelected = onFacultySelected,
            onDepartmentSelected = onDepartmentSelected,
            onCourseSelected = onCourseSelected,
            modifier = Modifier.weight(0.64f).fillMaxSize()
        )
    }
}

@Composable
private fun CategorySidebar(
    selectedCategory: ListingCategory?,
    onCategorySelected: (ListingCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember {
        listOf(
            ListingCategory.TEXTBOOK,
            ListingCategory.NOTES,
            ListingCategory.SUPPLIES,
            ListingCategory.ELECTRONICS,
            ListingCategory.OTHER
        )
    }

    Surface(modifier = modifier, color = Color.White, tonalElevation = 1.dp, shadowElevation = 0.dp) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = categories,
                key = { it.value },
                contentType = { "category_sidebar_item" }
            ) { category ->
                val selected = selectedCategory == category
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { onCategorySelected(category) }
                        .padding(vertical = 10.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(22.dp)
                            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = categoryLabel(category),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) MaterialTheme.colorScheme.primary else TextPrimary,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun SubcategoryPanel(
    state: ExploreUiState,
    onSubcategorySelected: (ExploreSubcategory) -> Unit,
    onFacultySelected: (String?) -> Unit,
    onDepartmentSelected: (String?) -> Unit,
    onCourseSelected: (Course?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = Color.White, tonalElevation = 1.dp, shadowElevation = 0.dp) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = state.appliedFilters.category?.let { "${categoryLabel(it)}" } ?: "Select a category",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )

            if (state.courseDataSource == DataSource.CACHE) {
                Text(
                    text = "Using saved courses",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            when {
                state.appliedFilters.category == null -> {
                    Text("Choose a category to browse", style = MaterialTheme.typography.bodySmall)
                }
                state.appliedFilters.category == ListingCategory.TEXTBOOK || state.appliedFilters.category == ListingCategory.NOTES -> {
                    AcademicSubcategoryPanel(
                        state = state,
                        onSubcategorySelected = onSubcategorySelected,
                        onFacultySelected = onFacultySelected,
                        onDepartmentSelected = onDepartmentSelected,
                        onCourseSelected = onCourseSelected
                    )
                }
                else -> {
                    val virtualSubcategories = remember(state.appliedFilters.category) { virtualSubcategoriesFor(state.appliedFilters.category) }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = virtualSubcategories,
                            key = { it.id },
                            contentType = { "virtual_subcategory_option" }
                        ) { subcategory ->
                            SubcategoryRow(
                                label = subcategory.label,
                                onClick = { onSubcategorySelected(subcategory) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcademicSubcategoryPanel(
    state: ExploreUiState,
    onSubcategorySelected: (ExploreSubcategory) -> Unit,
    onFacultySelected: (String?) -> Unit,
    onDepartmentSelected: (String?) -> Unit,
    onCourseSelected: (Course?) -> Unit
) {
    when {
        state.appliedFilters.faculty == null -> {
            Text("Faculties", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = state.availableFaculties,
                    key = { it },
                    contentType = { "faculty_option" }
                ) { faculty ->
                    SubcategoryRow(label = faculty, onClick = {
                        onSubcategorySelected(
                            ExploreSubcategory(
                                id = "faculty_${faculty}",
                                label = faculty,
                                parentCategory = state.appliedFilters.category ?: ListingCategory.TEXTBOOK,
                                type = ExploreSubcategoryType.FACULTY,
                                faculty = faculty
                            )
                        )
                    })
                }
            }
        }
        state.appliedFilters.departmentCode == null -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Academic areas", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                TextButton(onClick = { onFacultySelected(null) }) { Text("Change faculty") }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = state.availableDepartmentCodes,
                    key = { it },
                    contentType = { "department_option" }
                ) { department ->
                    SubcategoryRow(label = department, onClick = {
                        onSubcategorySelected(
                            ExploreSubcategory(
                                id = "academic_${department}",
                                label = department,
                                parentCategory = state.appliedFilters.category ?: ListingCategory.TEXTBOOK,
                                type = ExploreSubcategoryType.ACADEMIC_AREA,
                                faculty = state.appliedFilters.faculty,
                                departmentCode = department
                            )
                        )
                    })
                }
            }
        }
        else -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Courses", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                TextButton(onClick = { onDepartmentSelected(null) }) { Text("Change area") }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = state.availableCourses,
                    key = { it.id },
                    contentType = { "course_option" }
                ) { course ->
                    CourseRow(course = course, onClick = { onCourseSelected(course) })
                }
            }
        }
    }
}

@Composable
private fun ExploreResultsPanel(
    state: ExploreUiState,
    onListingClick: (ExploreListingUiItem) -> Unit,
    onBack: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenCourseFilters: () -> Unit,
    onRetry: () -> Unit,
    onWishlistToggle: (Int) -> Unit
) {
    Surface(color = Color.Transparent, shadowElevation = 0.dp) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = buildBreadcrumb(state),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "${state.resultCount} results",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onBack) { Text("Categories") }
                TextButton(onClick = onOpenCourseFilters) { Text("Course") }
                TextButton(onClick = onOpenFilters) { Text("Filters") }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    state.errorMessage != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(state.errorMessage)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onRetry) { Text("Retry") }
                        }
                    }
                    state.filteredListings.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No listings found", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Try changing filters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(
                                items = state.filteredListings,
                                key = { it.id },
                                contentType = { "explore_listing_card" }
                            ) { item ->
                                CompactExploreListingCard(
                                    item = item,
                                    isWishlisted = state.wishlistListingIds.contains(item.id),
                                    onWishlistClick = { onWishlistToggle(item.id) },
                                    onClick = { onListingClick(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactExploreListingCard(
    item: ExploreListingUiItem,
    isWishlisted: Boolean,
    onWishlistClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ExploreThumbnailImage(
                imageUrl = item.imageUrl,
                modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$${item.price}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                val subtitle = listOfNotNull(
                    item.courseCode,
                    item.condition?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }
                ).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            WishlistHeartButton(isWishlisted = isWishlisted, onClick = onWishlistClick)
        }
    }
}

@Composable
fun ExploreThumbnailImage(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val thumbnailUrl = remember(imageUrl) { ImageUtils.getThumbnailUrl(imageUrl) }
    val thumbnailPx = remember(density) { with(density) { 64.dp.roundToPx() } }
    val request = remember(thumbnailUrl, context, thumbnailPx) {
        ImageRequest.Builder(context)
            .data(thumbnailUrl)
            .size(thumbnailPx, thumbnailPx)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
private fun WishlistHeartButton(isWishlisted: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = "Wishlist",
            tint = if (isWishlisted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreFilterOverlay(
    state: ExploreUiState,
    selectedFilterSection: ExploreFilterSection,
    onFilterSectionSelected: (ExploreFilterSection) -> Unit,
    onCategorySelected: (ListingCategory?) -> Unit,
    onFacultySelected: (String?) -> Unit,
    onDepartmentCodeSelected: (String?) -> Unit,
    onCourseSelected: (Course?) -> Unit,
    onCourseSearchQueryChanged: (String) -> Unit,
    onMinPriceChanged: (String) -> Unit,
    onMaxPriceChanged: (String) -> Unit,
    onConditionSelected: (ListingCondition?) -> Unit,
    onSortSelected: (ExploreSort) -> Unit,
    onClearFilters: () -> Unit,
    onApplyFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 420.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filters", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                FilterSectionMenu(
                    state = state,
                    selected = selectedFilterSection,
                    onSectionSelected = onFilterSectionSelected,
                    modifier = Modifier.weight(0.36f)
                )

                FilterSectionContent(
                    state = state,
                    selected = selectedFilterSection,
                    onCategorySelected = onCategorySelected,
                    onFacultySelected = onFacultySelected,
                    onDepartmentCodeSelected = onDepartmentCodeSelected,
                    onCourseSelected = onCourseSelected,
                    onCourseSearchQueryChanged = onCourseSearchQueryChanged,
                    onMinPriceChanged = onMinPriceChanged,
                    onMaxPriceChanged = onMaxPriceChanged,
                    onConditionSelected = onConditionSelected,
                    onSortSelected = onSortSelected,
                    modifier = Modifier.weight(0.64f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClearFilters) { Text("Clear filters") }
                Button(onClick = onApplyFilters) { Text("See ${state.resultCount} results") }
            }
        }
    }
}

@Composable
private fun FilterSectionMenu(
    state: ExploreUiState,
    selected: ExploreFilterSection,
    onSectionSelected: (ExploreFilterSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = remember {
        listOf(
            ExploreFilterSection.FEATURED,
            ExploreFilterSection.CATEGORIES,
            ExploreFilterSection.FACULTY,
            ExploreFilterSection.ACADEMIC_AREA,
            ExploreFilterSection.COURSE,
            ExploreFilterSection.CONDITION,
            ExploreFilterSection.PRICE,
            ExploreFilterSection.SORT_BY
        )
    }

    Surface(modifier = modifier, color = Color(0xFFF9F9F9)) {
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(
                items = sections,
                key = { it.name },
                contentType = { "filter_section_option" }
            ) { section ->
                val isSelected = selected == section
                val hasActive = sectionHasSelection(section, state)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSectionSelected(section) }
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(22.dp)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = sectionLabel(section),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else TextPrimary
                        )
                        if (hasActive) {
                            Text("Applied", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSectionContent(
    state: ExploreUiState,
    selected: ExploreFilterSection,
    onCategorySelected: (ListingCategory?) -> Unit,
    onFacultySelected: (String?) -> Unit,
    onDepartmentCodeSelected: (String?) -> Unit,
    onCourseSelected: (Course?) -> Unit,
    onCourseSearchQueryChanged: (String) -> Unit,
    onMinPriceChanged: (String) -> Unit,
    onMaxPriceChanged: (String) -> Unit,
    onConditionSelected: (ListingCondition?) -> Unit,
    onSortSelected: (ExploreSort) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (selected) {
            ExploreFilterSection.FEATURED -> {
                Text("Adjust filters to refine results", style = MaterialTheme.typography.bodySmall)
            }
            ExploreFilterSection.CATEGORIES -> {
                listOf(
                    ListingCategory.TEXTBOOK,
                    ListingCategory.NOTES,
                    ListingCategory.SUPPLIES,
                    ListingCategory.ELECTRONICS,
                    ListingCategory.OTHER
                ).forEach { category ->
                    FilterOptionRow(
                        label = categoryLabel(category),
                        selected = state.draftFilters.category == category,
                        onClick = { onCategorySelected(if (state.draftFilters.category == category) null else category) }
                    )
                }
            }
            ExploreFilterSection.FACULTY -> {
                state.availableFaculties.forEach { faculty ->
                    FilterOptionRow(
                        label = faculty,
                        selected = state.draftFilters.faculty == faculty,
                        onClick = { onFacultySelected(if (state.draftFilters.faculty == faculty) null else faculty) }
                    )
                }
            }
            ExploreFilterSection.ACADEMIC_AREA -> {
                if (state.draftFilters.faculty == null) {
                    Text("Select a faculty to narrow academic areas", style = MaterialTheme.typography.bodySmall)
                }
                state.availableDepartmentCodes.forEach { department ->
                    FilterOptionRow(
                        label = department,
                        selected = state.draftFilters.departmentCode == department,
                        onClick = { onDepartmentCodeSelected(if (state.draftFilters.departmentCode == department) null else department) }
                    )
                }
            }
            ExploreFilterSection.COURSE -> {
                OutlinedTextField(
                    value = state.draftFilters.courseSearchQuery,
                    onValueChange = onCourseSearchQueryChanged,
                    placeholder = { Text("Search course") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (state.draftFilters.faculty == null && state.draftFilters.departmentCode == null && state.draftFilters.courseSearchQuery.isBlank()) {
                    Text(
                        "Select a faculty or academic area to narrow courses",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                    items(
                        items = state.availableCourses,
                        key = { it.id },
                        contentType = { "course_option" }
                    ) { course ->
                        CourseRow(course = course, onClick = { onCourseSelected(course) })
                    }
                }
            }
            ExploreFilterSection.CONDITION -> {
                val conditions = remember { ListingCondition.values() }
                conditions.forEach { condition ->
                    FilterOptionRow(
                        label = conditionLabel(condition),
                        selected = state.draftFilters.condition == condition,
                        onClick = { onConditionSelected(if (state.draftFilters.condition == condition) null else condition) }
                    )
                }
            }
            ExploreFilterSection.PRICE -> {
                OutlinedTextField(
                    value = state.draftFilters.minPrice,
                    onValueChange = onMinPriceChanged,
                    placeholder = { Text("Min price") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                )
                OutlinedTextField(
                    value = state.draftFilters.maxPrice,
                    onValueChange = onMaxPriceChanged,
                    placeholder = { Text("Max price") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = false, onClick = {
                        onMinPriceChanged("")
                        onMaxPriceChanged("45000")
                    }, label = { Text("Up to $45.000") })
                    FilterChip(selected = false, onClick = {
                        onMinPriceChanged("45000")
                        onMaxPriceChanged("100000")
                    }, label = { Text("$45.000 to $100.000") })
                }
                FilterChip(selected = false, onClick = {
                    onMinPriceChanged("100000")
                    onMaxPriceChanged("")
                }, label = { Text("More than $100.000") })
            }
            ExploreFilterSection.SORT_BY -> {
                val sorts = remember { ExploreSort.values() }
                sorts.forEach { sort ->
                    FilterOptionRow(
                        label = sortLabel(sort),
                        selected = state.draftFilters.sort == sort,
                        onClick = { onSortSelected(sort) }
                    )
                }
            }
        }

        if (state.courseDataSource == DataSource.CACHE) {
            Text(
                text = "Using saved courses",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun FilterOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable { onClick() },
        style = MaterialTheme.typography.bodySmall,
        color = if (selected) MaterialTheme.colorScheme.primary else TextPrimary
    )
}

@Composable
private fun SubcategoryRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = TextPrimary
    )
}

@Composable
private fun CourseRow(course: Course, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(course.code, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        Text(course.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

private fun buildBreadcrumb(state: ExploreUiState): String {
    val parts = mutableListOf<String>()
    state.appliedFilters.category?.let { parts.add(categoryLabel(it)) }
    state.appliedFilters.faculty?.let { parts.add(it) }
    state.appliedFilters.departmentCode?.let { parts.add(it) }
    state.appliedFilters.course?.let { parts.add(it.code) }
    return parts.joinToString(" > ").ifBlank { "Results" }
}

private fun sectionLabel(section: ExploreFilterSection): String {
    return when (section) {
        ExploreFilterSection.FEATURED -> "Featured"
        ExploreFilterSection.CATEGORIES -> "Categories"
        ExploreFilterSection.FACULTY -> "Faculty"
        ExploreFilterSection.ACADEMIC_AREA -> "Academic area"
        ExploreFilterSection.COURSE -> "Course"
        ExploreFilterSection.CONDITION -> "Condition"
        ExploreFilterSection.PRICE -> "Price"
        ExploreFilterSection.SORT_BY -> "Sort by"
    }
}

private fun categoryLabel(category: ListingCategory): String {
    return category.name.lowercase(Locale.US).replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun sortLabel(sort: ExploreSort): String {
    return sort.name.lowercase(Locale.US).replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun conditionLabel(condition: ListingCondition): String {
    return condition.name.lowercase(Locale.US).replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun sectionHasSelection(section: ExploreFilterSection, state: ExploreUiState): Boolean {
    return when (section) {
        ExploreFilterSection.FEATURED -> false
        ExploreFilterSection.CATEGORIES -> state.draftFilters.category != null
        ExploreFilterSection.FACULTY -> state.draftFilters.faculty != null
        ExploreFilterSection.ACADEMIC_AREA -> state.draftFilters.departmentCode != null
        ExploreFilterSection.COURSE -> state.draftFilters.course != null || state.draftFilters.courseSearchQuery.isNotBlank()
        ExploreFilterSection.CONDITION -> state.draftFilters.condition != null
        ExploreFilterSection.PRICE -> state.draftFilters.minPrice.isNotBlank() || state.draftFilters.maxPrice.isNotBlank()
        ExploreFilterSection.SORT_BY -> state.draftFilters.sort != ExploreSort.RECENT
    }
}

private fun virtualSubcategoriesFor(category: ListingCategory): List<ExploreSubcategory> {
    return when (category) {
        ListingCategory.ELECTRONICS -> listOf(
            ExploreSubcategory(
                id = "electronics_calculators",
                label = "Calculators",
                parentCategory = category,
                type = ExploreSubcategoryType.VIRTUAL_KEYWORD,
                keywords = listOf("calculator", "calculadora", "casio", "ti")
            ),
            ExploreSubcategory(
                id = "electronics_laptops",
                label = "Laptops",
                parentCategory = category,
                type = ExploreSubcategoryType.VIRTUAL_KEYWORD,
                keywords = listOf("laptop", "notebook", "macbook")
            ),
            ExploreSubcategory(
                id = "electronics_tablets",
                label = "Tablets",
                parentCategory = category,
                type = ExploreSubcategoryType.VIRTUAL_KEYWORD,
                keywords = listOf("tablet", "ipad", "galaxy tab")
            ),
            ExploreSubcategory(
                id = "electronics_accessories",
                label = "Accessories",
                parentCategory = category,
                type = ExploreSubcategoryType.VIRTUAL_KEYWORD,
                keywords = listOf("charger", "cargador", "mouse", "keyboard", "teclado")
            )
        )
        ListingCategory.SUPPLIES -> listOf(
            ExploreSubcategory(
                id = "supplies_stationery",
                label = "Stationery",
                parentCategory = category,
                type = ExploreSubcategoryType.VIRTUAL_KEYWORD,
                keywords = listOf("pen", "pencil", "notebook", "cuaderno", "stationery")
            ),
            ExploreSubcategory(
                id = "supplies_lab",
                label = "Lab materials",
                parentCategory = category,
                type = ExploreSubcategoryType.VIRTUAL_KEYWORD,
                keywords = listOf("lab", "goggles", "gloves", "pipette")
            ),
            ExploreSubcategory(
                id = "supplies_art",
                label = "Art supplies",
                parentCategory = category,
                type = ExploreSubcategoryType.VIRTUAL_KEYWORD,
                keywords = listOf("art", "paint", "brush", "marker")
            ),
            ExploreSubcategory(
                id = "supplies_backpacks",
                label = "Backpacks",
                parentCategory = category,
                type = ExploreSubcategoryType.VIRTUAL_KEYWORD,
                keywords = listOf("backpack", "mochila", "bag")
            )
        )
        ListingCategory.OTHER -> listOf(
            ExploreSubcategory(
                id = "other_services",
                label = "Services",
                parentCategory = category,
                type = ExploreSubcategoryType.VIRTUAL_KEYWORD,
                keywords = listOf("service", "tutoring", "tutor", "printing")
            ),
            ExploreSubcategory(
                id = "other_misc",
                label = "Miscellaneous",
                parentCategory = category,
                type = ExploreSubcategoryType.VIRTUAL_KEYWORD,
                keywords = listOf("misc", "other", "various")
            )
        )
        else -> emptyList()
    }
}
