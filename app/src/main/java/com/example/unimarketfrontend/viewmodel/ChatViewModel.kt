package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.utils.analytics.AnalyticsLogger
import com.example.unimarketfrontend.model.local.MessageEntity
import com.example.unimarketfrontend.model.repository.MessagesRepository
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/*
 * ViewModel del Chat - SPRINT 3
 * Corregido para sincronizacion en vivo y bilateralismo visual.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessagesRepository(application)
    private val _otherId = MutableStateFlow<Int?>(null)

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    val isOffline = ConnectivityMonitor.isOnline.map { !it }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // UI reactiva: observa Room directamente
    val messages: StateFlow<List<MessageEntity>> = _otherId
        .filterNotNull()
        .flatMapLatest { id -> repository.observeMessages(id) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadThread(sellerId: Int) {
        if (_otherId.value == sellerId) return
        _otherId.value = sellerId
        viewModelScope.launch {
            repository.refreshThread(sellerId)
        }
    }

    fun sendMessage(sellerId: Int, content: String) {
        val cleanContent = content.trim()
        if (cleanContent.isBlank()) return

        viewModelScope.launch {
            _isSending.value = true
            try {
                val isOnline = ConnectivityMonitor.isOnline.value
                if (isOnline) {
                    try {
                        repository.sendMessage(sellerId, cleanContent)
                        // Refrescamos Room inmediatamente para ver el mensaje enviado
                        repository.refreshThread(sellerId)

                        AnalyticsLogger.log("message_sent", mapOf("seller_id" to sellerId.toString()))
                    } catch (e: Exception) {
                        repository.savePendingMessage(sellerId, cleanContent)
                    }
                } else {
                    repository.savePendingMessage(sellerId, cleanContent)
                }
            } finally {
                _isSending.value = false
            }
        }
    }
}