package com.example.unimarketfrontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarketfrontend.ui.components.RecentlyAddedCard
import com.example.unimarketfrontend.viewmodel.ManageProductsState
import com.example.unimarketfrontend.viewmodel.ManageProductsViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.unimarketfrontend.ui.components.BottomNavigationBar
import com.example.unimarketfrontend.ui.navigation.navigateTracked
import com.example.unimarketfrontend.ui.theme.BackgroundLight
import com.example.unimarketfrontend.ui.theme.PrimaryIndigo
import com.example.unimarketfrontend.ui.theme.TextPrimary
import com.example.unimarketfrontend.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProductsScreen(
    navController: NavController,
    viewModel: ManageProductsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isManageVisible = backStackEntry?.destination?.route == "manage"

    LaunchedEffect(isManageVisible) {
        if (isManageVisible) {
            viewModel.refresh()
        }
    }

    Scaffold(
        containerColor = BackgroundLight,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("createListing") },
                containerColor = PrimaryIndigo,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Listing")
            }
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "manage",
                onRouteChange = { route -> navController.navigateTracked(route) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(innerPadding)
        ) {
            Text(
                text = "Manage Products",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(16.dp)
            )

            when (state) {
                is ManageProductsState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                    }
                }
                is ManageProductsState.Error -> {
                    val error = state as ManageProductsState.Error
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error.message, color = TextSecondary)
                    }
                }
                is ManageProductsState.Success -> {
                    val successState = state as ManageProductsState.Success
                    var selectedTab by remember { mutableStateOf("active") }
                    val currentList = if (selectedTab == "active") successState.active else successState.sold

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(4.dp)
                        ) {
                            TabButton(
                                title = "Active (${successState.active.size})",
                                isSelected = selectedTab == "active",
                                onClick = { selectedTab = "active" }
                            )
                            TabButton(
                                title = "Sold (${successState.sold.size})",
                                isSelected = selectedTab == "sold",
                                onClick = { selectedTab = "sold" }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (currentList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (selectedTab == "active") "No active listings" else "No sold listings",
                                    color = TextSecondary
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 340.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 100.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(
                                    items = currentList,
                                    key = { it.id },
                                    contentType = { "listing" }
                                ) { listing ->
                                    RecentlyAddedCard(
                                        listing = listing,
                                        onClick = { navController.navigate("listingDetail/${listing.id}") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryIndigo else Color.Transparent,
            contentColor = if (isSelected) Color.White else TextSecondary
        ),
        elevation = null
    ) {
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
