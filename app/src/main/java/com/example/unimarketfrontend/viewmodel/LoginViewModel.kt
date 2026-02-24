package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.LoginRequest
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.login(
                    LoginRequest(email, password)
                )
                RetrofitInstance.setToken(response.access_token)
                onSuccess()
            } catch (e: Exception) {
                onError()
            }
        }
    }
}