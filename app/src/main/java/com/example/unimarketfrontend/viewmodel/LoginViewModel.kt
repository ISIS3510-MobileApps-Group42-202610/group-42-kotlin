package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.utils.analytics.AnalyticsAuthMode
import com.example.unimarketfrontend.model.utils.analytics.AnalyticsConfig
import com.example.unimarketfrontend.model.utils.ErrorTranslator
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.repository.AuthRepository
import com.example.unimarketfrontend.model.user.LoginRequest
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = authRepository.login(LoginRequest(email, password))
                authRepository.saveToken(response.access_token)

                AnalyticsConfig.authMode = AnalyticsAuthMode.Bearer
                AnalyticsConfig.authToken = response.access_token

                onSuccess()

            } catch (e: Exception) {
                val userMessage = ErrorTranslator.getUserFriendlyMessage(e)
                onError(userMessage)
            }
        }
    }
}