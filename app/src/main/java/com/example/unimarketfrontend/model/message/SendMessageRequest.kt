package com.example.unimarketfrontend.model.message

// Cuerpo del POST cuando el comprador envía un mensaje
// seller_id: a qué vendedor le mandamos el mensaje
// content: el texto del mensaje
data class SendMessageRequest(
    val seller_id: Int,
    val content: String
)