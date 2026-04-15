package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.analytics.AnalyticsLogger
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.ConversationPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Definimos los tres estados posibles de la pantalla de mensajes
sealed class MessagesUiState {
    object Loading : MessagesUiState() // Cuando esta descargando
    data class Success(val conversations: List<ConversationPreview>) : MessagesUiState() // Cuando ya estan los chats
    data class Error(val message: String) : MessagesUiState() // Si algo exploto
}

class MessagesViewModel : ViewModel() {

    // Flujo interno para manejar el estado de la UI
    private val _uiState = MutableStateFlow<MessagesUiState>(MessagesUiState.Loading)
    val uiState: StateFlow<MessagesUiState> = _uiState

    init {
        // Apenas nace el ViewModel, mandamos a traer los mensajes
        loadConversations()
    }

    // Logica pesada: El backend nos manda mensajes sueltos, nosotros armamos los hilos de chat
    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = MessagesUiState.Loading
            try {
                // Pedimos todos los mensajes donde somos compradores
                val messages = RetrofitInstance.api.getMessagesAsBuyer()

                // Agrupamos la lista plana de mensajes por el ID del vendedor
                val conversations = messages
                    .groupBy { it.seller?.id }
                    .mapNotNull { (sellerId, msgs) ->
                        if (sellerId == null) return@mapNotNull null

                        // Buscamos el mensaje mas reciente para mostrarlo como preview
                        val last = msgs.maxByOrNull { it.created_at ?: "" }
                            ?: return@mapNotNull null

                        val user = last.seller?.user

                        // Creamos el objeto de vista para la lista
                        ConversationPreview(
                            otherPersonId = sellerId,
                            otherPersonName = buildName(user?.name, user?.last_name),
                            lastMessage = last.content,
                            lastMessageTime = formatTime(last.created_at),
                            isRead = last.is_read
                        )
                    }

                // Analytics: mandamos cuantas conversaciones tiene el usuario sin leer
                val unreadCount = conversations.count { !it.isRead }
                AnalyticsLogger.log(
                    "messages_screen_opened",
                    mapOf("unread_conversations" to unreadCount.toString())
                )

                _uiState.value = MessagesUiState.Success(conversations)
            } catch (e: Exception) {
                _uiState.value = MessagesUiState.Error(e.message ?: "Error al cargar mensajes")
            }
        }
    }

    // Helper para juntar nombre y apellido sin que se vea feo si falta uno
    private fun buildName(first: String?, last: String?): String {
        val full = listOfNotNull(first, last).joinToString(" ").trim()
        return full.ifBlank { "Vendedor Desconocido" }
    }

    // Cortamos el string de fecha para mostrar solo la hora (HH:mm)
    private fun formatTime(isoDate: String?): String {
        if (isoDate == null) return ""
        return try {
            isoDate.substring(11, 16)
        } catch (e: Exception) {
            ""
        }
    }
}