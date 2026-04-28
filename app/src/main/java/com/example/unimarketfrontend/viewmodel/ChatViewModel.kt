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

    // Guardamos el rol para usarlo en sendMessage sin tener que pasarlo de nuevo
    private var currentIAmBuyer: Boolean = true

    val isOffline: StateFlow<Boolean> = ConnectivityMonitor.isOnline
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    // Observa Room directamente — cuando refreshThread inserta mensajes,
    // el Flow emite automáticamente y la UI se actualiza
    val messages: StateFlow<List<MessageEntity>> = _otherId
        .filterNotNull()
        .flatMapLatest { id -> repository.observeMessages(id) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // iAmBuyer se pasa desde ChatScreen y ya no depende de la consulta a Room
    fun loadThread(otherId: Int, iAmBuyer: Boolean) {
        currentIAmBuyer = iAmBuyer
        _otherId.value = otherId
        viewModelScope.launch {
            // refreshThread ya sabe qué endpoint usar gracias a iAmBuyer
            repository.refreshThread(otherId, iAmBuyer)
        }
    }

    fun sendMessage(otherId: Int, content: String, iAmBuyer: Boolean) {
        if (content.isBlank()) return
        currentIAmBuyer = iAmBuyer

        viewModelScope.launch {
            _isSending.value = true

            if (ConnectivityMonitor.isOnline.value) {
                try {
                    repository.sendMessage(otherId, content, iAmBuyer)
                    // Refresh inmediato para que el mensaje aparezca en la UI
                    repository.refreshThread(otherId, iAmBuyer)
                    AnalyticsLogger.log(
                        "message_sent",
                        mapOf("seller_id" to otherId.toString())
                    )
                } catch (e: Exception) {
                    // Si falla, guarda como pendiente
                    repository.savePendingMessage(otherId, content)
                }
            } else {
                repository.savePendingMessage(otherId, content)
            }

            _isSending.value = false
        }
    }
}