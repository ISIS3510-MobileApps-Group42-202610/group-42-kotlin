package com.example.unimarketfrontend.network.model

data class Review(
    val id: Int,
    val transaction_id: Int?,
    val user_id: Int?,
    val reviewer_id: Int?,
    val content: String?,
    val rating: Int?,
    val created_at: String?,
    val user: User?
)

