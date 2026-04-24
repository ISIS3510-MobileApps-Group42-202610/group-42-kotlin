package com.example.unimarketfrontend.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.utils.analytics.AnalyticsLogger
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.message.Message
import com.example.unimarketfrontend.model.message.SendMessageRequest
import com.example.unimarketfrontend.model.repository.MessagesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/*
 * ViewModel para la pantalla de chat individual.
 * SPRINT 3: Ahora es un AndroidViewModel para acceder al contexto y usar Room.
 * Implementamos Conectividad Eventual: si el usuario manda un mensaje sin red, 
 * se guarda localmente y se marca como [Pending].
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = MessagesRepository(messagesDao = db.messagesDao())

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
                // SPRINT 3: Podríamos implementar cache de mensajes por hilo aquí también.
                val thread = RetrofitInstance.api.getThread(sellerId)
                _messages.value = thread
                
                // BQ4: Tiempo de respuesta (Smart Feature)
                computeAndLogResponseTime(thread, sellerId)
            } catch (e: Exception) {
                Log.e("CHAT_VM", "Error cargando hilo: ${e.message}")
            }
        }
    }

    // Envía un mensaje con soporte Offline (Eventual Connectivity)
    fun sendMessage(sellerId: Int, content: String) {
        viewModelScope.launch {
            _isSending.value = true
            
            // Verificamos si hay internet usando nuestro Monitor (Sensor de red)
            val isOnline = ConnectivityMonitor.isOnline.value

            if (isOnline) {
                try {
                    // Intento de envío real al servidor NestJS
                    val response = RetrofitInstance.api.sendMessageAsBuyer(
                        SendMessageRequest(seller_id = sellerId, content = content)
                    )
                    _messages.value = _messages.value + response
                    AnalyticsLogger.log("message_sent", mapOf("seller_id" to sellerId.toString()))
                } catch (e: Exception) {
                    // Si falla el servidor pero el monitor decía que había red, 
                    // lo mandamos a la cola de pendientes para no perder el dato (Evita UC)
                    Log.w("CHAT_VM", "Fallo envio con red, guardando pendiente")
                    saveLocally(sellerId, content)
                }
            } else {
                // SPRINT 3: Sin red, directo a la base de datos local (Room)
                saveLocally(sellerId, content)
            }
            
            _isSending.value = false
        }
    }

    // Guarda el mensaje en Room y lo muestra en la UI con tag de pendiente
    private suspend fun saveLocally(sellerId: Int, content: String) {
        repository.savePendingMessage(sellerId, content)
        
        // Creamos un mensaje "ficticio" para que el usuario lo vea de una vez (Feedback inmediato)
        val tempMessage = Message(
            id = -1,
            content = "[Pending] $content",
            sent_by = "buyer",
            is_read = false,
            created_at = null,
            seller_id = sellerId, // Lo pide el modelo
            buyer_id = null,      // Lo dejamos null porque es local
            seller = null,
            buyer = null
        )
        _messages.value += tempMessage
    }

    // BQ4: Algoritmo para calcular eficiencia del vendedor basado en timestamps
    private fun computeAndLogResponseTime(messages: List<Message>, sellerId: Int) {
        val buyerMessages = messages.filter { it.sent_by == "buyer" }
        val sellerReplies = messages.filter { it.sent_by == "seller" }

        if (buyerMessages.isEmpty() || sellerReplies.isEmpty()) return

        val responseTimes = mutableListOf<Long>()

        for (buyerMsg in buyerMessages) {
            val sentAt = parseMillis(buyerMsg.created_at) ?: continue
            val firstReply = sellerReplies
                .mapNotNull { parseMillis(it.created_at) }
                .filter { it > sentAt } // Buscamos la respuesta inmediata posterior
                .minOrNull() ?: continue
            
            responseTimes.add((firstReply - sentAt) / 60_000) // Diferencia en minutos
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

    // Helper para parsear fechas ISO 8601 a milisegundos (Basado en lógica de kotlinx-datetime)
    private fun parseMillis(iso: String?): Long? {
        if (iso == null) return null
        return try {
            val clean = iso.replace("Z", "").replace("T", " ")
            val parts = clean.split(" ")
            val dateParts = parts[0].split("-")
            val timeParts = parts[1].split(":")
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt()
            val day = dateParts[2].toInt()
            val hour = timeParts[0].toInt()
            val min = timeParts[1].toInt()
            
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