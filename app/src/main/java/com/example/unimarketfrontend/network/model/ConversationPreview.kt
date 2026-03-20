package com.example.unimarketfrontend.network.model

data class ConversationPreview(
    val otherPersonId: Int,
    val otherPersonName: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val isRead: Boolean
)
