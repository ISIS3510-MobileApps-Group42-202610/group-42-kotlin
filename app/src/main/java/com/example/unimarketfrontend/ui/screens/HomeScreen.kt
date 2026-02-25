package com.example.unimarketfrontend.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.unimarketfrontend.ui.components.CategoryChip
import com.example.unimarketfrontend.ui.components.TrendingCard
import com.example.unimarketfrontend.ui.components.HeaderSection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import com.example.unimarketfrontend.viewmodel.HomeViewModel
import com.example.unimarketfrontend.viewmodel.HomeUiState
import com.example.unimarketfrontend.ui.components.BottomNavigationBar
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    when (state) {
        is HomeUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is HomeUiState.Error -> {
            Text("Error loading data")
        }

        is HomeUiState.Success -> {

            val data = state as HomeUiState.Success
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {

                BottomNavigationBar(currentRoute = "home",
                    onRouteChange = { route ->
                        navController.navigate(route)
                    })
            }){innerPadding ->
            LazyColumn(
               modifier = Modifier
                   .fillMaxSize()
                   .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {

                item {
                    HeaderSection(userName = data.userName)
                }

                item {
                    Spacer(Modifier.height(25.dp))
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                    ) {
                        data.categories.forEach {
                            CategoryChip(it.name, "${it.count} items")
                        }
                    }
                }

                item {
                    Text(
                        text = "Trending Now",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 16.dp)
                    ) {
                        data.listings.forEach { listing ->
                            TrendingCard(
                                imageUrl = listing.images?.firstOrNull()?.url ?: "",
                                title = listing.title,
                                price = "$${listing.selling_price}",
                                condition = listing.condition ?: "Unknown"
                            )
                        }
                    }
                }

            }
        }
        }
    }
}