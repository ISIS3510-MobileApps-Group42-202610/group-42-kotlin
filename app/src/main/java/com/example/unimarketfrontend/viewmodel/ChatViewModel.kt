package com.example.unimarketfrontend.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.message.Message
import com.example.unimarketfrontend.model.repository.MessagesRepository
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import com.example.unimarketfrontend.model.utils.analytics.AnalyticsLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/*
 * ViewModel del Chat - SPRINT 3 (Versión Final)
 * Implementa persistencia total con Room y reactividad mediante flujos.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessagesRepository(application)

    // ID de la persona con la que estamos chateando
    private val _otherId = MutableStateFlow<Int?>(null)

    // Observamos si estamos offline para avisar al usuario (Evita NIM)
    val isOffline = ConnectivityMonitor.isOnline
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // Indica si el sistema esta procesando un envio
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    //  Observamos los mensajes directamente desde Room
    // flatMapLatest hace que si cambiamos de chat, se deje de observar el anterior
    // y se empiece a observar el nuevo automaticamente.
    val messages: StateFlow<List<com.example.unimarketfrontend.model.local.MessageEntity>> = _otherId
        .filterNotNull()
        .flatMapLatest { id -> repository.observeMessages(id) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Carga el hilo y refresca desde la red
    fun loadThread(sellerId: Int) {
        if (_otherId.value == sellerId) return // Evitamos recargas innecesarias
        _otherId.value = sellerId

        viewModelScope.launch {
            // Intentamos bajar mensajes frescos del servidor NestJS
            repository.refreshThread(sellerId)
        }
    }

    // Envio de mensaje con soporte de Conectividad Eventual
    fun sendMessage(sellerId: Int, content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            _isSending.value = true

            val isOnline = ConnectivityMonitor.isOnline.value

            if (isOnline) {
                try {
                    // Intento de envio real
                    repository.sendMessage(sellerId, content)

                    // Analytics BQ4
                    runCatching {
                        AnalyticsLogger.log("message_sent", mapOf("seller_id" to sellerId.toString()))
                    }

                    // Refrescamos Room para que el nuevo mensaje aparezca
                    repository.refreshThread(sellerId)

                } catch (e: Exception) {
                    // Fallo el servidor, guardamos en cola local (Evita UC loss)
                    repository.savePendingMessage(sellerId, content)
                }
            } else {
                // SPRINT 3: Sin red, directo a Room
                repository.savePendingMessage(sellerId, content)
            }

            _isSending.value = false
        }
    }
}