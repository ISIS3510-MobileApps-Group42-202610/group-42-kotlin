package com.example.unimarketfrontend.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.example.unimarketfrontend.utils.analytics.PerformanceTrackerProvider

fun NavController.navigateTracked(
    targetRoute: String,
    destinationKey: String = targetRoute
) {
    PerformanceTrackerProvider.tracker.markNavigationStart(
        fromRoute = currentDestination?.route,
        toRoute = destinationKey
    )
    navigate(targetRoute)
}

fun NavController.navigateTracked(
    targetRoute: String,
    destinationKey: String = targetRoute,
    builder: NavOptionsBuilder.() -> Unit
) {
    PerformanceTrackerProvider.tracker.markNavigationStart(
        fromRoute = currentDestination?.route,
        toRoute = destinationKey
    )
    navigate(targetRoute, builder)
}

