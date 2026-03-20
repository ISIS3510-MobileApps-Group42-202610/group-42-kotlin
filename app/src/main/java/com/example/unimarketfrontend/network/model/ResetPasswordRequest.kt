package com.example.unimarketfrontend.network.model

data class ResetPasswordRequest(
    val token: String,
    val new_password: String
)