package com.example.unimarketfrontend.ui
import androidx.compose.runtime.*
import com.example.unimarketfrontend.ui.screens.HomeScreen
import com.example.unimarketfrontend.ui.screens.LoginScreen

@Composable
fun UniMarketApp() {

    var isLoggedIn by remember { mutableStateOf(false) }

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = {
                isLoggedIn = true
            }
        )
    } else {
        HomeScreen()
    }
}