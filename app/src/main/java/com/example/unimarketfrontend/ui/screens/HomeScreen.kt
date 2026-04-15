package com.example.unimarketfrontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.unimarketfrontend.network.model.Listing
import com.example.unimarketfrontend.ui.components.BottomNavigationBar
import com.example.unimarketfrontend.ui.navigation.createListingNavigationHandler
import com.example.unimarketfrontend.ui.navigation.navigateTracked
import com.example.unimarketfrontend.ui.theme.AccentOrange
import com.example.unimarketfrontend.ui.theme.BackgroundLight
import com.example.unimarketfrontend.ui.theme.PrimaryIndigo
import com.example.unimarketfrontend.ui.theme.SecondaryEmerald
import com.example.unimarketfrontend.ui.theme.TextPrimary
import com.example.unimarketfrontend.ui.theme.TextSecondary
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    when (state) {
        is HomeUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is HomeUiState.Error -> {
            val error = state as HomeUiState.Error
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error.message)
            }
        }

        is HomeUiState.Success -> {
            val data = state as HomeUiState.Success
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

            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        currentRoute = "home",
                        onRouteChange = { route -> navController.navigateTracked(route) }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { navController.navigateTracked("createListing") },
                        containerColor = PrimaryIndigo,
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Anadir producto"
                        )
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Hello ${data.userName}",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "What are you looking for?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    ambientColor = Color.Black.copy(alpha = 0.08f)
                                )
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .clickable { },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                androidx.compose.material3.OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search books, notes...", color = TextSecondary) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp),
                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent
                                    ),
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
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

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredListings) { listing ->
                            HomeProductCard(
                                listing = listing,
                                onClick = { navigation.navigateToListing(listing.id) }
                            )
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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f)
            )
            .background(
                if (isSelected) PrimaryIndigo else Color.White,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else TextPrimary
        )
    }
}

@Composable
private fun HomeProductCard(
    listing: Listing,
    onClick: () -> Unit
) {
    var isFavorite by remember(listing.id) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = when (toDisplayCategory(listing.category)) {
                            "Books" -> PrimaryIndigo.copy(alpha = 0.12f)
                            "Electronics" -> AccentOrange.copy(alpha = 0.12f)
                            else -> SecondaryEmerald.copy(alpha = 0.12f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = listing.images?.firstOrNull()?.url
                if (imageUrl.isNullOrBlank()) {
                    Text(text = "UM", fontSize = 26.sp, color = TextSecondary)
                } else {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = listing.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = listing.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = toDisplayCategory(listing.category),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$${listing.selling_price}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )

                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) AccentOrange else TextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { isFavorite = !isFavorite }
                    )
                }
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