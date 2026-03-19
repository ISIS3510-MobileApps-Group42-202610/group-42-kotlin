package com.example.unimarketfrontend.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.example.unimarketfrontend.analytics.PerformanceTrackerProvider
import com.example.unimarketfrontend.ui.navigation.AppNavigation
import com.example.unimarketfrontend.ui.screens.LoginScreen

@Composable
fun UniMarketApp(startupStartElapsedMs: Long) {
    LaunchedEffect(startupStartElapsedMs) {
        // El arranque se considera completado cuando la aplicación ya es visible, no solo cuando el código interno terminó de inicializarse
        withFrameNanos { }
        PerformanceTrackerProvider.tracker.trackAppStartup(startupStartElapsedMs)
    }

    var isLoggedIn by remember { mutableStateOf(false) }

    if (!isLoggedIn) {

        LoginScreen(
            onLoginSuccess = {
                isLoggedIn = true
            }
        )

    } else {
        AppNavigation()
    }
}