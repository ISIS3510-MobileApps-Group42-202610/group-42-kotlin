package com.example.unimarketfrontend.network.model

data class Message(
    val id: Int,
    val content: String,
    val sent_by: String,
    val is_read: Boolean,
    val created_at: String?,
    val buyer: User?,
    val seller: User?
)