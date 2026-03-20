package com.example.unimarketfrontend.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.unimarketfrontend.analytics.PerformanceTrackerProvider
import com.example.unimarketfrontend.ui.navigation.AppNavigation
import com.example.unimarketfrontend.ui.screens.ForgotPasswordScreen
import com.example.unimarketfrontend.ui.screens.LoginScreen
import com.example.unimarketfrontend.ui.screens.RegisterScreen

@Composable
fun UniMarketApp(startupStartElapsedMs: Long) {
    LaunchedEffect(startupStartElapsedMs) {
        withFrameNanos { }
        PerformanceTrackerProvider.tracker.trackAppStartup(startupStartElapsedMs)
    }

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
        }

    } else {
        AppNavigation()
    }
}