package com.example.unimarketfrontend.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: Int,
    val content: String,
    val sentBy: String, // "buyer" o "seller"
    val conversationWithId: Int, // ID de la otra persona para agrupar
    val createdAt: String?,
    val isMine: Boolean // Ya calculado para no pelear con IDs luego
)