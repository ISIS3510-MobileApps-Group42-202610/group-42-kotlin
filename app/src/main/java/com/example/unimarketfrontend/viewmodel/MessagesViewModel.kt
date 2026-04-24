package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.local.ConversationEntity
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import com.example.unimarketfrontend.model.utils.analytics.AnalyticsLogger
import com.example.unimarketfrontend.repository.MessagesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/*
 * Este es el cerebro de la pantalla de mensajes para el Sprint 3.
 * Ahora usamos AndroidViewModel porque necesitamos el 'context' para inicializar Room.
 * Implementamos la estrategia Cache-then-Network: mostramos lo guardado y refrescamos de fondo.
 */
class MessagesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = MessagesRepository(messagesDao = db.messagesDao())

    // Estado de la lista de conversaciones (Viene de la DB local)
    private val _conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val conversations: StateFlow<List<ConversationEntity>> = _conversations

    // Controlamos si el sistema esta descargando datos nuevos
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    // Para avisarle al usuario que no tiene internet y esta viendo datos viejos
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    init {
        // 1. Nos suscribimos a Room: cualquier cambio en la DB actualiza la pantalla automaticamente (Observer)
        observeLocalConversations()
        
        // 2. Monitoreamos el internet para saber cuando mostrar el banner de error
        observeConnectivity()
        
        // 3. Cargamos los datos iniciales
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
                // Si vuelve el internet, aprovechamos para enviar lo que quedo pendiente en la cola
                if (online) {
                    launch {
                        val sent = repository.retryPendingMessages()
                        if (sent > 0) {
                            // Si se enviaron mensajes pendientes, refrescamos la lista para ver los cambios
                            repository.refreshFromNetwork()
                        }
                    }
                }
            }
        }
    }

    // Funcion principal de carga (Estrategia de Conectividad Eventual)
    fun loadConversations() {
        viewModelScope.launch {
            _isRefreshing.value = true
            
            // Usamos concurrencia (async) para verificar si el cache expiro sin bloquear el flujo
            val isStale = repository.isCacheStale()

            // Si los datos son viejos o la lista esta vacia, vamos a la red
            if (isStale || _conversations.value.isEmpty()) {
                val success = repository.refreshFromNetwork()
                if (!success && _conversations.value.isEmpty()) {
                    // Si falla la red y no hay nada en cache, marcamos offline critico
                    _isOffline.value = true
                }
            }

            _isRefreshing.value = false

            // Analytics: mandamos cuantas conversaciones tiene el usuario sin leer
            val unreadCount = _conversations.value.count { !it.isRead }
            AnalyticsLogger.log(
                "messages_screen_opened",
                mapOf("unread_conversations" to unreadCount.toString())
            )
        }
    }
}
