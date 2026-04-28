package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.utils.analytics.AnalyticsLogger
import com.example.unimarketfrontend.model.local.ConversationEntity
import com.example.unimarketfrontend.model.repository.MessagesRepository
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/*
 * ViewModel para la lista de conversaciones - SPRINT 3
 * Corregido para manejar reintentos automaticos y sincronizacion bilateral.
 * Sigue el patron Observer para reaccionar a cambios en Room.
 */
class MessagesViewModel(application: Application) : AndroidViewModel(application) {

    // Usamos el repositorio que ya sabe como sacar el DAO por si solo
    private val repository = MessagesRepository(application)

    private val _conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val conversations: StateFlow<List<ConversationEntity>> = _conversations

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    init {
        // 1. Observamos Room: si la DB cambia (ej: llega un mensaje), la UI se actualiza sola
        observeLocalConversations()

        // 2. Monitoreamos el internet para avisar al usuario y reintentar envios
        observeConnectivity()

        // 3. Carga inicial: intenta refrescar si el cache es viejo
        loadConversations()
    }

    private fun observeLocalConversations() {
        viewModelScope.launch {
            repository.observeConversations().collectLatest { cached ->
                _conversations.value = cached
            }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            ConnectivityMonitor.isOnline.collectLatest { online ->
                _isOffline.value = !online
                android.util.Log.d("MessagesViewModel", "Connectivity changed: online=$online")

                if (online) {
                    //  SPRINT 3: Si volvió el internet, vaciamos la cola de mensajes
                    launch {
                        try {
                            val retriedCount = repository.retryPendingMessages()
                            android.util.Log.d("MessagesViewModel", "Retried $retriedCount pending messages")

                            // Refrescamos para ver chats nuevos bilaterales
                            repository.refreshFromNetwork()
                        } catch (e: Exception) {
                            android.util.Log.e("MessagesViewModel", "Error retrying pending messages: ${e.message}", e)
                        }
                    }
                }
            }
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            _isRefreshing.value = true

            //  Táctica de Temporal Cache (Ch. 10):
            // Si pasaron 5 min o no hay nada, vamos a la red
            if (repository.isCacheStale() || _conversations.value.isEmpty()) {
                val success = repository.refreshFromNetwork()
                if (!success && _conversations.value.isEmpty()) {
                    // Solo marcamos offline critico si no hay ni siquiera cache viejo
                    _isOffline.value = true
                }
            }

            _isRefreshing.value = false

            //  Analytics BQ4: Registramos actividad y chats pendientes
            val unreadCount = _conversations.value.count { !it.isRead }
            AnalyticsLogger.log("messages_screen_opened", mapOf(
                "unread_conversations" to unreadCount.toString()
            ))
        }
    }
}
