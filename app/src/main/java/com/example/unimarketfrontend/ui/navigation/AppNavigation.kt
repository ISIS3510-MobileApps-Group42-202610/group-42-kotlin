package com.example.unimarketfrontend.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.example.unimarketfrontend.ui.screens.*

@Composable
fun AppNavigation() {
    var isLoggedIn by remember { mutableStateOf(false) }

    if (!isLoggedIn) {
        val authNavController = rememberNavController()

        NavHost(
            navController = authNavController,
            startDestination = "login"
        ) {
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
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") { HomeScreen(navController) }
            composable("profile") { ProfileScreen(navController) }
            composable("manage") { HomeScreen(navController) }
            composable("messages") { HomeScreen(navController) }
        }
    }
}