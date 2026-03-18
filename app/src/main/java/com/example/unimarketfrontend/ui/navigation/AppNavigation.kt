package com.example.unimarketfrontend.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.unimarketfrontend.ui.screens.ChatScreen
import com.example.unimarketfrontend.ui.screens.HomeScreen
import com.example.unimarketfrontend.ui.screens.MessagesScreen
import com.example.unimarketfrontend.ui.screens.ProfileScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        composable("home") { HomeScreen(navController) }

        composable("profile") { ProfileScreen(navController) }

        composable("messages") { MessagesScreen(navController) }

        composable("search") { HomeScreen(navController) } // TODO: create SearchScreen

        // Chat screen with typed path parameters
        composable(
            route = "chat/{sellerId}/{sellerName}",
            arguments = listOf(
                navArgument("sellerId") { type = NavType.IntType },
                navArgument("sellerName") { type = NavType.StringType }
            )
        ) { entry ->
            val sellerId = entry.arguments?.getInt("sellerId") ?: 0
            val sellerName = entry.arguments?.getString("sellerName") ?: "Seller"
            ChatScreen(navController, sellerId, sellerName)
        }
    }
}