package com.example.unimarketfrontend.network.model

data class Message(
    val id: Int,
    val seller_id: Int,
    val buyer_id: Int,
    val content: String,
    val sent_by: String,
    val sent_at: String,
    val is_read: Boolean
)

data class ConversationPreview(
    val otherPersonName: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val isRead: Boolean
)



