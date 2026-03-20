package com.example.unimarketfrontend.ui.components.bridge

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.unimarketfrontend.network.model.Listing

interface ListingCardBridge {
    @Composable
    fun render(listing: Listing, onClick: (Listing) -> Unit)
}

class TrendingCardBridge : ListingCardBridge {
    @Composable
    override fun render(listing: Listing, onClick: (Listing) -> Unit) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .width(180.dp)
                .padding(end = 12.dp)
                .clickable { onClick(listing) }
        ) {
            Column {
                AsyncImage(
                    model = listing.images?.firstOrNull()?.url ?: "",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = listing.condition ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$${listing.selling_price}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

class RecentCardBridge : ListingCardBridge {
    @Composable
    override fun render(listing: Listing, onClick: (Listing) -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onClick(listing) }
        ) {
            Row {
                AsyncImage(
                    model = listing.images?.firstOrNull()?.url ?: "",
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = listing.condition ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$${listing.selling_price}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

object ListingCardBridgeFactory {
    fun createTrendingCard(): ListingCardBridge = TrendingCardBridge()
    fun createRecentCard(): ListingCardBridge = RecentCardBridge()
}

