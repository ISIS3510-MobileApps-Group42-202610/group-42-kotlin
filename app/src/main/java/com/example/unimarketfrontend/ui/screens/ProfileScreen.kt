package com.example.unimarketfrontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.listing.Purchase
import com.example.unimarketfrontend.model.user.User
import com.example.unimarketfrontend.ui.components.BottomNavigationBar
import com.example.unimarketfrontend.ui.navigation.ListingRoutesFactory
import com.example.unimarketfrontend.ui.navigation.navigateTracked
import com.example.unimarketfrontend.viewmodel.ProfileViewModel

private val Primary   = Color(0xFF7B8FE8)
private val BgColor   = Color(0xFFFFFFFF)
private val CardColor = Color(0xFFF5F5F5)
private val TextGray  = Color(0xFFC4C4C4)

private enum class ProfileTab { WISHLIST, PURCHASES, FOLLOWING }

@Composable
fun ProfileScreen(
    navController: NavController,
    onLogout: () -> Unit = {}
) {
    val viewModel: ProfileViewModel = viewModel()

    val user      by viewModel.user.collectAsState()
    val wishlist  by viewModel.wishlist.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val following by viewModel.following.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg  by viewModel.errorMsg.collectAsState()

    var selectedTab        by remember { mutableStateOf(ProfileTab.PURCHASES) }
    var showDeleteDialog   by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = CardColor,
            title = { Text("Delete account", color = Color.Black, fontWeight = FontWeight.Bold) },
            text  = { Text("Are you sure? This action cannot be undone.", color = TextGray) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteAccount(
                        onSuccess = { onLogout() },
                        onError   = {  }
                    )
                }) {
                    Text("Delete", color = Color(0xFFB00505), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }

    Scaffold(
        containerColor = BgColor,
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "profile",
                onRouteChange = { route ->
                    navController.navigateTracked(route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    ) { innerPadding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("My Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Person, contentDescription = "Settings", tint = TextGray)
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user?.name?.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (user != null) "${user!!.name} ${user!!.last_name}" else "...",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                                Text(user?.email ?: "", fontSize = 13.sp, color = TextGray)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE0E0E0))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(purchases.size.toString(), "Purchases")
                            StatItem(
                                value = wishlist.size.toString(), 
                                label = "Wishlist",
                                onClick = { navController.navigate("wishlist") }
                            )
                            StatItem(following.size.toString(), "Following")
                        }
                    }
                }
            }

            item {
                if (user?.is_seller == true) {
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Switch to Seller Mode", fontWeight = FontWeight.SemiBold, color = Color.Black)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabChip(
                        label = "Wishlist",
                        icon = { Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp)) },
                        selected = selectedTab == ProfileTab.WISHLIST,
                        onClick = { selectedTab = ProfileTab.WISHLIST },
                        modifier = Modifier.weight(1f)
                    )
                    TabChip(
                        label = "Purchases",
                        icon = { Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(16.dp)) },
                        selected = selectedTab == ProfileTab.PURCHASES,
                        onClick = { selectedTab = ProfileTab.PURCHASES },
                        modifier = Modifier.weight(1f)
                    )
                    TabChip(
                        label = "Following",
                        icon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp)) },
                        selected = selectedTab == ProfileTab.FOLLOWING,
                        onClick = { selectedTab = ProfileTab.FOLLOWING },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (errorMsg != null) {
                item { Text(errorMsg!!, color = Color.Red, fontSize = 12.sp) }
            }

            when (selectedTab) {
                ProfileTab.WISHLIST -> {
                    if (wishlist.isEmpty()) {
                        item { EmptyState("You don't have items in your wishlist :c") }
                    } else {
                        items(wishlist) { listing ->
                            WishlistItem(
                                listing = listing, 
                                onClick = {
                                    navController.navigate(ListingRoutesFactory.detailPath(listing.id))
                                },
                                onRemove = {
                                    viewModel.removeFromWishlist(listing.id)
                                }
                            )
                        }
                    }
                }
                ProfileTab.PURCHASES -> {
                    if (purchases.isEmpty()) {
                        item { EmptyState("You don't have any purchases") }
                    } else {
                        items(purchases) { purchase ->
                            PurchaseItem(purchase = purchase)
                        }
                    }
                }
                ProfileTab.FOLLOWING -> {
                    if (following.isEmpty()) {
                        item { EmptyState("You don't follow anyone") }
                    } else {
                        items(following) { followedUser ->
                            FollowingItem(user = followedUser)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                Button(
                    onClick = { showDeleteDialog = true },
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHovered) Color.Red else Color(0xFFF10000)
                    )
                ) {
                    Text("Delete account", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            item {
                Button(
                    onClick = { viewModel.logout(onSuccess = { onLogout() }) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                ) {
                    Text("Sign out", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(label, fontSize = 12.sp, color = TextGray)
    }
}

@Composable
private fun TabChip(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg      = if (selected) Primary else CardColor
    val content = if (selected) Color.White else Color.Black

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = bg,
        modifier = modifier.height(40.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides content) {
                icon()
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = content)
            }
        }
    }
}

@Composable
private fun WishlistItem(listing: Listing, onClick: () -> Unit, onRemove: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ShoppingBag, null, tint = Primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(listing.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                Text("$${listing.selling_price}", color = Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            TextButton(onClick = onRemove) {
                Text("Remove", color = Color.Red, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PurchaseItem(purchase: Purchase) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ShoppingBag, null, tint = Primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    purchase.listing?.title ?: "Item deleted!",
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black
                )
                purchase.listing?.let {
                    Text("$${it.selling_price}", color = Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                purchase.created_at?.let {
                    Text(it.take(10), fontSize = 12.sp, color = TextGray)
                }
            }
            Surface(shape = RoundedCornerShape(50), color = Color(0xFF1B3A2A)) {
                Text(
                    "completed",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun FollowingItem(user: User) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    user.name.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("${user.name} ${user.last_name}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Black)
                Text(user.email, fontSize = 12.sp, color = TextGray)
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = TextGray, fontSize = 14.sp)
    }
}