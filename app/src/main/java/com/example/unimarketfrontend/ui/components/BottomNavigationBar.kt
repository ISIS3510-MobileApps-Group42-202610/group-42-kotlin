package com.example.unimarketfrontend.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
/*TODO: Esta parte es Incompleta*/
@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onRouteChange: (String) -> Unit
) {
    NavigationBar {

        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = { onRouteChange("home") }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Search, null) },
            label = { Text("Search") },
            selected = currentRoute == "search",
            onClick = { onRouteChange("search") }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Message, null) },
            label = { Text("Messages") },
            selected = currentRoute == "messages",
            onClick = { onRouteChange("messages") }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBox, null) },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = { onRouteChange("profile") }
        )
    }
}