package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.local.MessageEntity
import com.example.unimarketfrontend.model.repository.MessagesRepository
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import com.example.unimarketfrontend.model.utils.analytics.AnalyticsLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.lang.Exception

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessagesRepository(application)

    private val _otherId = MutableStateFlow<Int?>(null)

    private var currentIAmBuyer: Boolean = true

    val isOffline: StateFlow<Boolean> = ConnectivityMonitor.isOnline
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    val messages: StateFlow<List<MessageEntity>> = _otherId
        .filterNotNull()
        .flatMapLatest { id -> repository.observeMessages(id) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadThread(otherId: Int, iAmBuyer: Boolean) {
        currentIAmBuyer = iAmBuyer
        _otherId.value = otherId
        viewModelScope.launch {
            repository.refreshThread(otherId, iAmBuyer)
        }
    }

    fun sendMessage(otherId: Int, content: String, iAmBuyer: Boolean) {
        if (content.isBlank()) return
        currentIAmBuyer = iAmBuyer

        viewModelScope.launch {
            _isSending.value = true
            val isOnline = ConnectivityMonitor.isOnline.value
            android.util.Log.d("ChatViewModel", "sendMessage: otherId=$otherId, iAmBuyer=$iAmBuyer, isOnline=$isOnline")

            if (!isOnline) {
                // Offline: guardar como pendiente y mostrar en UI
                android.util.Log.d("ChatViewModel", "OFFLINE - saving as pending")
                repository.savePendingMessage(otherId, content)
                _isSending.value = false
                return@launch
            }

            try {
                repository.sendMessage(otherId, content, iAmBuyer)
                repository.refreshThread(otherId, iAmBuyer)

                AnalyticsLogger.log(
                    "message_sent",
                    mapOf(
                        "other_id" to otherId.toString(),
                        "role" to if (iAmBuyer) "buyer" else "seller"
                    )
                )
            } catch (e: Exception) {
                // Network error: guardar como pendiente
                android.util.Log.e("ChatViewModel", "Send failed, saving as pending: ${e.message}")
                repository.savePendingMessage(otherId, content)
            } finally {
                _isSending.value = false
            }
        }
    }
}
