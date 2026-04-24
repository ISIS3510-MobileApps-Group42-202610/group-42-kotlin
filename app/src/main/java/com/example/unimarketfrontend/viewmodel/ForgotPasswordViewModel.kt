package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.auth.ForgotPasswordRequest
import com.example.unimarketfrontend.model.auth.ResetPasswordRequest
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {

    fun forgotPassword(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                RetrofitInstance.api.forgotPassword(ForgotPasswordRequest(email))
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Request failed")
            }
        }
    }

    fun resetPassword(
        token: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                RetrofitInstance.api.resetPassword(
                    ResetPasswordRequest(
                        token        = token,
                        new_password = newPassword
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Reset failed")
            }
        }
    }
}
