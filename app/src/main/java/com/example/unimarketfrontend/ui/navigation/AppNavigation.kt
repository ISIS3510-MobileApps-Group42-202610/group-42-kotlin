package com.example.unimarketfrontend.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.example.unimarketfrontend.ui.screens.HomeScreen
import com.example.unimarketfrontend.ui.screens.ProfileScreen
import com.example.unimarketfrontend.ui.screens.MessagesScreen


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

        composable("messages") {
            MessagesScreen(navController)
        }
        composable("search") {
            HomeScreen(navController) // TODO: crear SearchScreen
        }
        composable("manage") {
            HomeScreen(navController) // TODO: crear ManageScreen
        }
    }
}
