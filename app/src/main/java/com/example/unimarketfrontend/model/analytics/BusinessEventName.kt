package com.example.unimarketfrontend.model.analytics

enum class BusinessEventName(val value: String) {
    LISTING_VIEWED("listing_viewed"),
    CHAT_STARTED("chat_started"),
    FIRST_MESSAGE_SENT("first_message_sent"),
    TRANSACTION_COMPLETED("transaction_completed"),
    CAMPUS_BANNER_SHOWN("campus_banner_shown")
}