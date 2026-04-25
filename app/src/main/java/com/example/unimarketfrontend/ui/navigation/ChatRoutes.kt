package com.example.unimarketfrontend.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ChatRoutesFactory {
    const val CHAT_ROUTE = "chat/{sellerId}/{sellerName}"

    fun chatPath(personId: Int, personName: String): String {
        val encodedName = URLEncoder.encode(personName, StandardCharsets.UTF_8.name())
        return "chat/$personId/$encodedName"
    }
}

