package com.example.unimarketfrontend.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.unimarketfrontend.ui.screens.ChatScreen
import com.example.unimarketfrontend.analytics.PerformanceTrackerProvider
import com.example.unimarketfrontend.ui.screens.CreateListingScreen
import com.example.unimarketfrontend.ui.screens.ForgotPasswordScreen
import com.example.unimarketfrontend.ui.screens.HomeScreen
import com.example.unimarketfrontend.ui.screens.ListingDetailScreen
import com.example.unimarketfrontend.ui.screens.LoginScreen
import com.example.unimarketfrontend.ui.screens.ManageProductsScreen
import com.example.unimarketfrontend.ui.screens.MessagesScreen
import com.example.unimarketfrontend.ui.screens.ProfileScreen
import com.example.unimarketfrontend.ui.screens.RegisterScreen

@Composable
fun AppNavigation() {
    var isLoggedIn by remember { mutableStateOf(false) }

    if (!isLoggedIn) {
        val authNavController = rememberNavController()

        NavHost(navController = authNavController, startDestination = "login") {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { isLoggedIn = true },
                    onNavigateToRegister = { authNavController.navigate("register") },
                    onNavigateToForgotPassword = { authNavController.navigate("forgot_password") }
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = { authNavController.navigate("login") },
                    onNavigateToLogin = { authNavController.navigate("login") }
                )
            }
            composable("forgot_password") {
                ForgotPasswordScreen(
                    onNavigateToLogin = { authNavController.navigate("login") }
                )
            }
            
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

    } else {
        val navController = rememberNavController()
        TrackDestinationDisplay(navController = navController)

        NavHost(navController = navController, startDestination = "home") {
            composable("home") { HomeScreen(navController) }
            composable("profile") { ProfileScreen(navController) }
            composable("manage") { ManageProductsScreen(navController) }
            composable("messages") { MessagesScreen(navController) }
            composable("createListing") { CreateListingScreen(navController) }
            composable(
                route = ListingRoutesFactory.DETAIL_ROUTE,
                arguments = listOf(navArgument("listingId") { type = NavType.IntType })
            ) { backStackEntry ->
                val listingId = backStackEntry.arguments?.getInt("listingId") ?: -1
                ListingDetailScreen(navController = navController, listingId = listingId)
            }
        }
    }
}

@Composable
private fun TrackDestinationDisplay(navController: NavController) {
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            PerformanceTrackerProvider.tracker.onDestinationDisplayed(destination.route)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
}