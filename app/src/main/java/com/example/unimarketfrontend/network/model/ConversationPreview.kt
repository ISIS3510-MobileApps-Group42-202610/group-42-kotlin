package com.example.unimarketfrontend.network.model

// Resumen de una conversación que se muestra en la lista de mensajes
// Este modelo NO viene del backend — lo construye MessagesViewModel
// agrupando los mensajes por vendedor
data class ConversationPreview(
    val otherPersonId: Int,
    val otherPersonName: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val isRead: Boolean
)