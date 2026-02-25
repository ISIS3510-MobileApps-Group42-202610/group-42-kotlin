package com.example.unimarketfrontend.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.example.unimarketfrontend.ui.screens.HomeScreen
import com.example.unimarketfrontend.ui.screens.ProfileScreen

@Composable
fun AppNavigation() {

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
            // Change with the new screen
            HomeScreen(navController)
        }

        composable("messages") {
            // Change with the new screen
            HomeScreen(navController)
        }
    }
}