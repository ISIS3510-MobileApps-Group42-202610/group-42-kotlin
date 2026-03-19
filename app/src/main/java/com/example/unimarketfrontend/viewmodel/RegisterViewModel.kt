package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.RegisterRequest
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    fun register(
        name: String,
        lastName: String,
        email: String,
        password: String,
        semester: Int?,
        isSeller: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                RetrofitInstance.api.register(
                    RegisterRequest(
                        name      = name,
                        last_name = lastName,
                        email     = email,
                        password  = password,
                        semester  = semester,
                        is_seller = isSeller
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Registration failed")
            }
        }
    }
}
