package com.example.unimarketfrontend.network.model

data class SendMessageRequest(
    val seller_id: Int,
    val content: String
)