package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import com.example.unimarketfrontend.analytics.BusinessAnalyticsProvider

class MessagesViewModel(
    private val listingId: Int,
    private val sellerId: Int
) : ViewModel() {
    private var firstMessageTracked = false

    fun onMessageSentSuccessfully(messageBody: String) {
        if (messageBody.isBlank()) return
        if (firstMessageTracked) return

        firstMessageTracked = true
        BusinessAnalyticsProvider.tracker.trackFirstMessageSent(
            listingId = listingId,
            sellerId = sellerId,
            metadata = mapOf(
                "source" to "messages_screen",
                "message_length" to messageBody.length
            )
        )
    }
}

