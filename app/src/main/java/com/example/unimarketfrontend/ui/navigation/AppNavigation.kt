package com.example.unimarketfrontend.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.unimarketfrontend.analytics.PerformanceTrackerProvider
import com.example.unimarketfrontend.ui.screens.CreateListingScreen
import com.example.unimarketfrontend.ui.screens.HomeScreen
import com.example.unimarketfrontend.ui.screens.ListingDetailScreen
import com.example.unimarketfrontend.ui.screens.ManageProductsScreen
import com.example.unimarketfrontend.ui.screens.MessagesScreen
import com.example.unimarketfrontend.ui.screens.ProfileScreen


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    TrackDestinationDisplay(navController = navController)

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
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
