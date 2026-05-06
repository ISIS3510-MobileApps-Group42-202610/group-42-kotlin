package com.example.unimarketfrontend.model.message

// Cuerpo del POST cuando el comprador envía un mensaje
// seller_id: a qué vendedor le mandamos el mensaje
// content: el texto del mensaje
data class SendMessageRequest(
    val seller_id: Int,
    val content: String
)

// Cuerpo del POST cuando el vendedor responde a un comprador
// buyer_id: a qué comprador (user ID) le mandamos el mensaje
data class SendMessageAsSellerRequest(
    val buyer_id: Int,
    val content: String
)
