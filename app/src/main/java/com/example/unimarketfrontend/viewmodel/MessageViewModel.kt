package com.example.unimarketfrontend.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.analytics.AnalyticsLogger
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.ConversationPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Define los tres posibles estados de la pantalla de mensajes
sealed class MessagesUiState {
    object Loading : MessagesUiState()
    data class Success(val conversations: List<ConversationPreview>) : MessagesUiState()
    data class Error(val message: String) : MessagesUiState()
}

@RequiresApi(Build.VERSION_CODES.O)
class MessagesViewModel : ViewModel() {

    // Estado interno mutable — solo el ViewModel lo puede cambiar
    private val _uiState = MutableStateFlow<MessagesUiState>(MessagesUiState.Loading)

    // Estado de solo lectura que la pantalla observa
    val uiState: StateFlow<MessagesUiState> = _uiState
    // Se ejecuta automáticamente cuando se crea el ViewModel
    init {
        loadConversations()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = MessagesUiState.Loading
            try {
                // Llama al backend: GET /api/v1/messages/as-buyer
                val messages = RetrofitInstance.api.getMessagesAsBuyer()

                // Agrupa todos los mensajes por el ID del vendedor
                // Resultado: un Map donde cada clave es un seller_id
                // y el valor es la lista de mensajes con ese vendedor
                val conversations = messages
                    .groupBy { it.seller?.id }
                    .mapNotNull { (_, msgs) ->
                        // De cada grupo, toma el mensaje más reciente
                        val last = msgs.maxByOrNull { it.created_at ?: "" }
                            ?: return@mapNotNull null
                        // Construye el resumen de la conversación
                        ConversationPreview(
                            otherPersonId = last.seller?.id ?: 0,
                            otherPersonName = buildName(last.seller?.name, last.seller?.last_name),
                            lastMessage = last.content,
                            lastMessageTime = formatTime(last.created_at),
                            isRead = last.is_read
                        )
                    }

                // Cuenta conversaciones no leídas para el evento de analytics
                val unreadCount = conversations.count { !it.isRead }

                // Envía el evento al analytics engine — BQ4 proxy
                // CORRECTO: el segundo parámetro es directamente el Map, sin "properties ="
                AnalyticsLogger.log("messages_screen_opened", mapOf("unread_conversations" to unreadCount.toString()))

                _uiState.value = MessagesUiState.Success(conversations)
            } catch (e: Exception) {
                _uiState.value = MessagesUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Une nombre y apellido, descartando los que sean null
    // listOfNotNull("Juan", null) → ["Juan"]
    // joinToString(" ") → "Juan"
    private fun buildName(first: String?, last: String?): String =
        listOfNotNull(first, last).joinToString(" ").ifBlank { "Unknown" }

    // Extrae HH:mm de una fecha ISO como "2026-03-19T14:30:00Z"
    // substring(11, 16) toma los caracteres en posición 11, 12, 13, 14, 15 → "14:30"
    private fun formatTime(isoDate: String?): String {
        if (isoDate == null) return ""
        return try {
            isoDate.substring(11, 16)
        } catch (e: Exception) {
            ""
        }
    }
}