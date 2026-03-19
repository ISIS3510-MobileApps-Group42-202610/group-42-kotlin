package com.example.unimarketfrontend.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.unimarketfrontend.ui.screens.CreateListingScreen
import com.example.unimarketfrontend.ui.screens.HomeScreen
import com.example.unimarketfrontend.ui.screens.ListingDetailScreen
import com.example.unimarketfrontend.ui.screens.ManageProductsScreen
import com.example.unimarketfrontend.ui.screens.ProfileScreen
import com.example.unimarketfrontend.ui.screens.MessagesScreen


@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") { HomeScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
        composable("manage") { ManageProductsScreen(navController) }
        composable("messages") { HomeScreen(navController) }
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
