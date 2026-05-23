package com.example.unimarketfrontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.unimarketfrontend.model.local.WishlistEntity
import com.example.unimarketfrontend.ui.navigation.ListingRoutesFactory
import com.example.unimarketfrontend.viewmodel.WishlistViewModel

/**
 * SPRINT 4 — WishlistScreen
 * Shows the user's saved listings with offline support (Tank Architecture).
 *
 * New view for Sprint 4 (Esteban Hernandez).
 * Shows the user's saved listings with offline support.
 *
 * Eventual Connectivity:
 * - Shows cached wishlist when offline (no blank screen)
 * - Displays offline banner when no connectivity
 * - Refreshes from network when online
 *
 * Caching: Cache-then-network via WishlistViewModel + WishlistRepository
 * Local Storage: Room (WishlistEntity)
 * Multi-threading: StateFlow + coroutines
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    navController: NavController,
    viewModel: WishlistViewModel = viewModel()
) {
    val wishlist by viewModel.wishlist.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWishlist()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Wishlist", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Offline banner — avoids Non-Informative Message antipattern
            if (isOffline) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "You are offline. Showing saved wishlist.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Error snackbar
            errorMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(msg)
                }
            }

            if (wishlist.isEmpty() && !isRefreshing) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No saved listings yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Tap the heart icon on any listing to save it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(wishlist, key = { it.listingId }) { item ->
                        WishlistCard(
                            item = item,
                            onTap = {
                                navController.navigate(
                                    ListingRoutesFactory.detailPath(item.listingId)
                                )
                            },
                            onRemove = {
                                viewModel.removeFromWishlist(item.listingId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WishlistCard(
    item: WishlistEntity,
    onTap: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Listing image
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "UM", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = item.title.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$${item.sellingPrice}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.category ?: "Others"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Remove from wishlist",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
