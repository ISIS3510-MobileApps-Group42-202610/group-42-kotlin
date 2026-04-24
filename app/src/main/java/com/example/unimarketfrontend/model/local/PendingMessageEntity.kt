package com.example.unimarketfrontend.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 * Esta tabla guarda mensajes que el usuario quiso enviar pero no habia red.
 * Implementa la estrategia de Eventual Connectivity para evitar el antipatron User Content (UC).
 * Los mensajes se quedan aca hasta que el ConnectivityMonitor detecte que volvio el internet.
 */
@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val sellerId: Int,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)