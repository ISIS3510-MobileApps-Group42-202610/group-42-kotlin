package com.example.unimarketfrontend.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val otherPersonId: Int,
    val otherPersonName: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val isRead: Boolean,
    val cachedAt: Long = System.currentTimeMillis(),
    // SPRINT 3 FIX: Guardamos si el usuario logueado es el comprador en esta conversación.
    // Necesario para saber qué endpoint de thread usar al abrir el chat.
    val iAmBuyer: Boolean = true
)