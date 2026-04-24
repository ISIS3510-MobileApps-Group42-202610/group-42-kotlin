package com.example.unimarketfrontend.model.uploads

import com.google.gson.annotations.SerializedName

data class CloudinarySignatureResponse(
    @SerializedName("api_key")
    val apiKey: String,
    val timestamp: Long,
    val signature: String,
    val folder: String?,
    @SerializedName("cloud_name")
    val cloudName: String
)
