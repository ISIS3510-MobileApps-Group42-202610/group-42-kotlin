package com.example.unimarketfrontend.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 * Esta clase es el molde para guardar los chats en la memoria del celular.
 * Usamos el ID del vendedor como llave primaria para que no se dupliquen.
 * Sirve para implementar el Temporal Cache (Ch. 10 del libro).
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val otherPersonId: Int,
    val otherPersonName: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val isRead: Boolean,
    val cachedAt: Long = System.currentTimeMillis() // Para saber que tan viejos son los datos y aplicar política de expiración
)