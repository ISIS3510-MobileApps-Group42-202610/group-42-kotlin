package com.example.unimarketfrontend.network.model

data class UserWithPurchases(
    val id: Int,
    val name: String,
    val last_name: String,
    val email: String,
    val semester: Int?,
    val profile_pic: String?,
    val is_seller: Boolean,
    val purchases: List<Purchase>?
)