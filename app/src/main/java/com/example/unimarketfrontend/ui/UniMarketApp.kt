package com.example.unimarketfrontend.ui

import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.example.unimarketfrontend.ui.screens.*

@Composable
fun UniMarketApp() {

    var isLoggedIn by remember { mutableStateOf(false) }

    if (!isLoggedIn) {

        LoginScreen(
            onLoginSuccess = {
                isLoggedIn = true
            }
        )

    } else {

        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "home"
        ) {

            composable("home") {
                HomeScreen(navController)
            }

            composable("profile") {
                ProfileScreen(navController)
            }
            composable("manage") {
                ManageProductsScreen(navController)
            }
            composable("createListing") {
                HomeScreen(navController)
            }

            composable("search") { }

            composable("messages") { }
        }
    }
}