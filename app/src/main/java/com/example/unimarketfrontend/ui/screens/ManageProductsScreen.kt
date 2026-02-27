package com.example.unimarketfrontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unimarketfrontend.ui.components.RecentlyAddedCard
import com.example.unimarketfrontend.viewmodel.ManageProductsState
import com.example.unimarketfrontend.viewmodel.ManageProductsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProductsScreen(
    navController: NavController,
    viewModel: ManageProductsViewModel = viewModel()
) {

    val state by viewModel.state.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("createListing") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Listing")
            }
        }
    ) { innerPadding ->

        when (state) {

            is ManageProductsState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ManageProductsState.Error -> {
                val error = state as ManageProductsState.Error

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(error.message)
                }
            }

            is ManageProductsState.Success -> {

                val successState = state as ManageProductsState.Success
                val activeListings = successState.active
                val soldListings = successState.sold

                var selectedTab by remember { mutableStateOf("active") }

                val currentList =
                    if (selectedTab == "active") activeListings else soldListings

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {

                    Text(
                        text = "Manage Products",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {

                        TabButton(
                            title = "Active (${activeListings.size})",
                            isSelected = selectedTab == "active",
                            onClick = { selectedTab = "active" }
                        )

                        TabButton(
                            title = "Sold (${soldListings.size})",
                            isSelected = selectedTab == "sold",
                            onClick = { selectedTab = "sold" }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (currentList.isEmpty()) {

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedTab == "active")
                                    "No active listings"
                                else
                                    "No sold listings",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                    } else {

                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 100.dp
                            )
                        ) {
                            items(currentList) { listing ->
                                RecentlyAddedCard(listing)
                                Spacer(modifier = Modifier.height(16.dp))
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
        modifier = Modifier
            .weight(1f)
            .padding(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = title,
            color = if (isSelected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}