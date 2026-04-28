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

        android.util.Log.d("ChatViewModel", "sendMessage called - otherId=$otherId, iAmBuyer=$iAmBuyer, content='$content'")

        viewModelScope.launch {
            _isSending.value = true

            try {
                if (ConnectivityMonitor.isOnline.value) {
                    android.util.Log.d("ChatViewModel", "Online - sending message to server")

                    // Enviar mensaje al servidor
                    val sentMessage = repository.sendMessage(otherId, content, iAmBuyer)
                    android.util.Log.d("ChatViewModel", "Message sent successfully: ${sentMessage.id}")

                    // Refresh inmediato para que el mensaje aparezca en la UI
                    val refreshSuccess = repository.refreshThread(otherId, iAmBuyer)
                    android.util.Log.d("ChatViewModel", "Thread refresh result: $refreshSuccess")

                    AnalyticsLogger.log(
                        "message_sent",
                        mapOf(
                            "other_id" to otherId.toString(),
                            "role" to if (iAmBuyer) "buyer" else "seller"
                        )
                    )
                } else {
                    // Sin conexión: guardar como pendiente
                    repository.savePendingMessage(otherId, content)
                    android.util.Log.d("ChatViewModel", "Message saved as pending (offline)")
                }
            } catch (e: Exception) {
                // Si falla el envío, guardar como pendiente
                android.util.Log.e("ChatViewModel", "Error sending message: ${e.message}", e)
                repository.savePendingMessage(otherId, content)
            } finally {
                _isSending.value = false
            }
        }
    }
}