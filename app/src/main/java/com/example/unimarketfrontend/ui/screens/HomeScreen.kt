package com.example.unimarketfrontend.ui.screens

import android.app.Application
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import com.example.unimarketfrontend.ui.components.BottomNavigationBar
import com.example.unimarketfrontend.ui.components.OfflineBanner
import com.example.unimarketfrontend.ui.navigation.createListingNavigationHandler
import com.example.unimarketfrontend.ui.navigation.navigateTracked
import com.example.unimarketfrontend.ui.theme.*
import com.example.unimarketfrontend.viewmodel.HomeUiState
import com.example.unimarketfrontend.viewmodel.HomeViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val navigation = navController.createListingNavigationHandler()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isHomeVisible = backStackEntry?.destination?.route == "home"
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(isHomeVisible) {
        if (isHomeVisible) {
            viewModel.refreshHome()
        }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    when (state) {
        is HomeUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryIndigo)
            }
        }

        is HomeUiState.Error -> {
            val error = state as HomeUiState.Error
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error.message, color = TextSecondary)
            }
        }

        is HomeUiState.Success -> {
            val data = state as HomeUiState.Success
            val isOnline by ConnectivityMonitor.isOnline.collectAsState()
            
            val allListings = remember(data.trending, data.recent) {
                (data.trending + data.recent).distinctBy { it.id }
            }

            val filteredListings = remember(allListings, selectedCategory, searchQuery) {
                allListings.filter { listing ->
                    val categoryOk = selectedCategory == "All" || toDisplayCategory(listing.category) == selectedCategory
                    val queryOk = searchQuery.isBlank() ||
                        listing.title.contains(searchQuery, ignoreCase = true) ||
                        listing.product.orEmpty().contains(searchQuery, ignoreCase = true)
                    categoryOk && queryOk
                }
            }

            val interested = remember(allListings) {
                allListings.shuffled().take(10)
            }

            val trendingLimited = remember(data.trending) { data.trending.take(10) }
            val recentLimited = remember(data.recent) { data.recent.take(10) }

            val isSearching = searchQuery.isNotBlank() || selectedCategory != "All"

            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        currentRoute = "home",
                        onRouteChange = { route: String -> navController.navigateTracked(route) }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { navController.navigateTracked("createListing") },
                        containerColor = PrimaryIndigo,
                        contentColor = Color.White
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Anadir producto")
                    }
                },
                containerColor = BackgroundLight
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(innerPadding)
                ) {
                    if (!isOnline) {
                        OfflineBanner("Showing saved data.")
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = if (isLandscape) 6.dp else 12.dp)
                    ) {
                        if (!isLandscape) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Hello ${data.userName}", fontSize = 14.sp, color = TextSecondary)
                                    Text(text = "Unimarket", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                // SPRINT 4: Boton para ir a la Wishlist
                                IconButton(onClick = { navController.navigate("wishlist") }) {
                                    Icon(
                                        imageVector = Icons.Default.FavoriteBorder,
                                        contentDescription = "Wishlist",
                                        tint = PrimaryIndigo
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isLandscape) 40.dp else 52.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { if (it.length <= 100) searchQuery = it },
                                    placeholder = { Text("Search products...", color = TextSecondary, fontSize = 13.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    singleLine = true
                                )
                            }

                            if (isLandscape) {
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("All", "Books", "Electronics", "Notes").forEach { category ->
                                        HomeCategoryChip(
                                            label = category,
                                            isSelected = selectedCategory == category,
                                            onClick = { selectedCategory = category },
                                            compact = true
                                        )
                                    }
                                }
                            }
                        }

                        if (!isLandscape) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("All", "Books", "Electronics", "Notes", "Furniture").forEach { category ->
                                    HomeCategoryChip(
                                        label = category,
                                        isSelected = selectedCategory == category,
                                        onClick = { selectedCategory = category }
                                    )
                                }
                            }
                        }
                    }

                    if (isSearching) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = if (isLandscape) 280.dp else 340.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = filteredListings, 
                                key = { it.id },
                                contentType = { "full_card" }
                            ) { listing ->
                                HomeFullCard(
                                    listing = listing,
                                    isWishlisted = data.wishlistListingIds.contains(listing.id),
                                    onWishlistClick = { viewModel.toggleWishlist(listing.id) },
                                    onClick = { navigation.navigateToListing(listing.id) }
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            HomeSection("Trending", trendingLimited) { navigation.navigateToListing(it.id) }
                            HomeSection("Recently Added", recentLimited) { navigation.navigateToListing(it.id) }
                            HomeSection("Te puede interesar", interested) { navigation.navigateToListing(it.id) }
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeCategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    Box(
        modifier = Modifier
            .background(if (isSelected) PrimaryIndigo else Color.White, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = if (compact) 10.dp else 16.dp, vertical = if (compact) 4.dp else 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else TextPrimary
        )
    }
}

@Composable
private fun HomeSection(
    title: String,
    items: List<Listing>,
    onItemClick: (Listing) -> Unit
) {
    if (items.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = items, 
                key = { it.id },
                contentType = { "small_card" }
            ) { listing ->
                HomeSmallCard(listing) { onItemClick(listing) }
            }
        }
    }
}

@Composable
private fun HomeSmallCard(
    listing: Listing,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = listing.images?.firstOrNull()?.url
    val imageRequest = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(300)
            .crossfade(false)
            .build()
    }
    
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
        ) {
            if (imageUrl == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "UM", fontSize = 24.sp, color = TextSecondary)
                }
            } else {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = listing.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1)
        Text(text = "$${listing.selling_price}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
    }
}

@Composable
private fun HomeFullCard(
    listing: Listing,
    isWishlisted: Boolean,
    onWishlistClick: () -> Unit,
    onClick: () -> Unit
) {
    val displayCategory = remember(listing.category) { toDisplayCategory(listing.category) }
    val context = LocalContext.current
    val imageUrl = listing.images?.firstOrNull()?.url
    val imageRequest = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(300)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(
                        color = when (displayCategory) {
                            "Books" -> PrimaryIndigo.copy(alpha = 0.1f)
                            "Electronics" -> AccentOrange.copy(alpha = 0.1f)
                            else -> SecondaryEmerald.copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl == null) {
                    Text(text = "UM", fontSize = 18.sp, color = TextSecondary)
                } else {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = listing.title,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = listing.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                Text(text = displayCategory, fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "$${listing.selling_price}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
            }
            IconButton(
                onClick = onWishlistClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Wishlist",
                    tint = if (isWishlisted) PrimaryIndigo else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun toDisplayCategory(category: String?): String {
    return when (category?.lowercase()) {
        "textbook", "books", "book" -> "TextBook"
        "electronics" -> "Electronics"
        "notes" -> "Notes"
        "furniture" -> "Furniture"
        else -> "Books"
    }
}
