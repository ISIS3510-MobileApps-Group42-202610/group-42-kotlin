package com.example.unimarketfrontend.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.ui.screens.*

@Composable
fun AppNavigation() {
    var isLoggedIn by remember { mutableStateOf(RetrofitInstance.isLoggedIn()) }

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
                ForgotPasswordScreen(onNavigateToLogin = { authNavController.navigate("login") })
            }
        }
    } else {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "home") {
            composable("home") { HomeScreen(navController) }
            composable("profile") { ProfileScreen(navController = navController, onLogout = { isLoggedIn = false }) }
            composable("manage") { ManageProductsScreen(navController) }
            composable("messages") { MessagesScreen(navController) }
            composable("search") { SearchScreen(navController) }
            composable("createListing") { CreateListingScreen(navController) }

            composable(
                route = ListingRoutesFactory.DETAIL_ROUTE,
                arguments = listOf(navArgument("listingId") { type = NavType.IntType })
            ) { backStackEntry ->
                val listingId = backStackEntry.arguments?.getInt("listingId") ?: -1
                ListingDetailScreen(navController = navController, listingId = listingId)
            }

            composable(
                route = ChatRoutesFactory.CHAT_ROUTE,
                arguments = listOf(
                    navArgument("otherId") { type = NavType.IntType },
                    navArgument("otherName") { type = NavType.StringType },
                    navArgument("iAmBuyer") { type = NavType.IntType; defaultValue = 1 }
                )
            ) { entry ->
                val otherId = entry.arguments?.getInt("otherId") ?: 0
                val otherName = java.net.URLDecoder.decode(entry.arguments?.getString("otherName") ?: "User", "UTF-8")
                val iAmBuyer = (entry.arguments?.getInt("iAmBuyer") ?: 1) == 1
                ChatScreen(navController, otherId, otherName, iAmBuyer)
            }
        }
    }
}
