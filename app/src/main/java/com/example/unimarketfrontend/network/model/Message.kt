package com.example.unimarketfrontend.network.model

data class Message(
    val id: Int,
    val content: String,
    val sent_by: String,
    val is_read: Boolean,
    val created_at: String?,
    val seller_id: Int?,
    val buyer_id: Int?,
    val seller: SellerInfo?,
    val buyer: BuyerInfo?
)

data class SellerInfo(
    val id: Int,
    val user: User?
)

data class BuyerInfo(
    val id: Int,
    val user: User?
)