package com.example.unimarketfrontend.model.auth

data class ResetPasswordRequest(
    val token: String,
    val new_password: String
)