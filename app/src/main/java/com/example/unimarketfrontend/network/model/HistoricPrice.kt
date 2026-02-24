package com.example.unimarketfrontend.network.model

data class HistoricPrice(
    val id: Int,
    val listing_id: Int,
    val start_date: String,
    val final_date: String?
)