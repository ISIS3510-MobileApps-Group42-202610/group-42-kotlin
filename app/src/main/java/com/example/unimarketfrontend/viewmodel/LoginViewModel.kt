package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.analytics.AnalyticsAuthMode
import com.example.unimarketfrontend.analytics.AnalyticsConfig
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.LoginRequest
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.login(LoginRequest(email, password))
                // Store the JWT token in the Retrofit interceptor
                RetrofitInstance.setToken(response.access_token)

                AnalyticsConfig.authMode = AnalyticsAuthMode.Bearer
                AnalyticsConfig.authToken = response.access_token

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Login failed")
            }
        }
    }
}