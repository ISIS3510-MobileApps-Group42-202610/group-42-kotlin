package com.example.unimarketfrontend.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.local.MessageEntity
import com.example.unimarketfrontend.model.repository.MessagesRepository
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import com.example.unimarketfrontend.model.utils.analytics.AnalyticsLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessagesRepository(application)

    private val _otherId = MutableStateFlow<Int?>(null)

    val isOffline: StateFlow<Boolean> = ConnectivityMonitor.isOnline
        .map { isOnline -> !isOnline }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _sellerDetails = MutableStateFlow<String?>(null)
    val sellerDetails: StateFlow<String?> = _sellerDetails

    val messages: StateFlow<List<MessageEntity>> = _otherId
        .flatMapLatest { id ->
            if (id == null) {
                emptyFlow()
            } else {
                repository.observeMessages(id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadThread(sellerId: Int) {
        if (_otherId.value == sellerId) return

        _otherId.value = sellerId
        _sellerDetails.value = "Usuario #$sellerId"

        viewModelScope.launch {
            val refreshed = repository.refreshThread(sellerId)

            if (!refreshed) {
                Log.e("CHAT_VM", "No se pudo refrescar el hilo con id=$sellerId")
            }
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

                        runCatching {
                            AnalyticsLogger.log(
                                "message_sent",
                                mapOf("seller_id" to sellerId.toString())
                            )
                        }

                        repository.refreshThread(sellerId)
                    } catch (e: Exception) {
                        Log.e("CHAT_VM", "Error enviando mensaje. Se guarda como pendiente: ${e.message}")
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