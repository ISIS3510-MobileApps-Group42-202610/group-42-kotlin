package com.example.unimarketfrontend.network.model

// Un mensaje individual del hilo entre comprador y vendedor
// seller y buyer son objetos User completos, no solo IDs
// El backend los retorna anidados en el JSON
data class Message(
    val id: Int,
    val content: String,
    val sent_by: String,
    val is_read: Boolean,
    val created_at: String?,
    val buyer: User?,
    val seller: User?
)


