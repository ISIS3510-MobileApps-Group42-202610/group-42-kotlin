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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {

        item {
            HeaderSection(userName = "Sebastián")
        }

        item {
            Spacer(Modifier.height(25.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                CategoryChip("Textbooks", "124 items")
                CategoryChip("Electronics", "89 items")
                CategoryChip("Lab Equip", "45 items")
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

                TrendingCard(
                    imageUrl = "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f",
                    title = "Calculus - James Stewart",
                    price = "$85.000",
                    condition = "Good"
                )

                TrendingCard(
                    imageUrl = "https://images.unsplash.com/photo-1519389950473-47ba0277781c",
                    title = "TI-84 Plus",
                    price = "$180.000",
                    condition = "Like New"
                )
            }
        }
    }
}