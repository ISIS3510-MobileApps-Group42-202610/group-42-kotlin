package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.analytics.AnalyticsLogger
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.Message
import com.example.unimarketfrontend.network.model.SendMessageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    // Lista de mensajes del hilo actual
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    // Indica si hay un mensaje siendo enviado en este momento
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    // Carga todos los mensajes del hilo con un vendedor específico
    fun loadThread(sellerId: Int) {
        viewModelScope.launch {
            try {
                val thread = RetrofitInstance.api.getThread(sellerId)
                _messages.value = thread
                // Calcula y registra el tiempo de respuesta del vendedor (BQ4)
                computeAndLogResponseTime(thread, sellerId)
            } catch (e: Exception) {
                // Si falla la carga, mantenemos los mensajes existentes
            }
        }
    }

    // Envía un mensaje y lo agrega localmente a la lista
    fun sendMessage(sellerId: Int, content: String) {
        viewModelScope.launch {
            _isSending.value = true
            try {
                val sent = RetrofitInstance.api.sendMessageAsBuyer(
                    SendMessageRequest(seller_id = sellerId, content = content)
                )
                // Agrega el mensaje enviado al final de la lista existente
                _messages.value = _messages.value + sent
                AnalyticsLogger.log(
                    "message_sent",
                    mapOf("seller_id" to sellerId.toString())
                )
            } catch (e: Exception) {
                // Si falla el envío, el usuario puede intentar de nuevo
            } finally {
                // finally se ejecuta siempre, haya error o no
                _isSending.value = false
            }
        }
    }

    // BQ4: para cada mensaje del comprador, busca la primera respuesta del vendedor
    // y calcula cuántos minutos tardó en responder
    private fun computeAndLogResponseTime(messages: List<Message>, sellerId: Int) {
        val buyerMessages = messages.filter { it.sent_by == "buyer" }
        val sellerReplies = messages.filter { it.sent_by == "seller" }

        if (buyerMessages.isEmpty() || sellerReplies.isEmpty()) return

        val responseTimes = mutableListOf<Long>()

        for (buyerMsg in buyerMessages) {
            val sentAt = parseMillis(buyerMsg.created_at) ?: continue
            // Busca la primera respuesta del vendedor que llegó DESPUÉS del mensaje del comprador
            val firstReply = sellerReplies
                .mapNotNull { parseMillis(it.created_at) }
                .filter { it > sentAt }
                .minOrNull() ?: continue
            // Convierte la diferencia de milisegundos a minutos
            responseTimes.add((firstReply - sentAt) / 60_000)
        }

        if (responseTimes.isNotEmpty()) {
            val avg = responseTimes.average().toLong()
            AnalyticsLogger.log(
                "seller_avg_response_time",
                mapOf(
                    "seller_id" to sellerId.toString(),
                    "avg_minutes" to avg.toString()
                )
            )
        }
    }

    // Convierte una fecha ISO 8601 a milisegundos desde epoch
    // Devuelve null si la fecha no se puede parsear
    private fun parseMillis(iso: String?): Long? {
        if (iso == null) return null
        return try {
            // ISO format: "2026-03-19T14:30:00.000Z"
            // Parseamos manualmente para no depender de API 26
            val clean = iso.replace("Z", "").replace("T", " ")
            val parts = clean.split(" ")
            val dateParts = parts[0].split("-")
            val timeParts = parts[1].split(":")
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt()
            val day = dateParts[2].toInt()
            val hour = timeParts[0].toInt()
            val min = timeParts[1].toInt()
            // Aproximación simple en milisegundos suficiente para calcular diferencias
            ((year - 1970).toLong() * 365 * 24 * 3600 * 1000) +
                    (month.toLong() * 30 * 24 * 3600 * 1000) +
                    (day.toLong() * 24 * 3600 * 1000) +
                    (hour.toLong() * 3600 * 1000) +
                    (min.toLong() * 60 * 1000)
        } catch (e: Exception) {
            null
        }
    }
}
