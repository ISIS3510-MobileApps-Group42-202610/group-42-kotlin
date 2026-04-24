package com.example.unimarketfrontend.model.auth

data class RegisterRequest(
    val name: String,
    val last_name: String,
    val email: String,
    val password: String,
    val semester: Int? = null,
    val is_seller: Boolean = false
)