package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.analytics.AnalyticsLogger
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.ConversationPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class MessagesUiState {
    object Loading : MessagesUiState()
    data class Success(val conversations: List<ConversationPreview>) : MessagesUiState()
    data class Error(val message: String) : MessagesUiState()
}

class MessagesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<MessagesUiState>(MessagesUiState.Loading)
    val uiState: StateFlow<MessagesUiState> = _uiState

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = MessagesUiState.Loading
            try {
                val messages = RetrofitInstance.api.getMessagesAsBuyer()

                val conversations = messages
                    .groupBy { it.seller?.id }
                    .mapNotNull { (sellerId, msgs) ->
                        if (sellerId == null) return@mapNotNull null

                        val last = msgs.maxByOrNull { it.created_at ?: "" }
                            ?: return@mapNotNull null


                        val user = last.seller?.user

                        ConversationPreview(
                            otherPersonId = sellerId,
                            otherPersonName = buildName(user?.name, user?.last_name),
                            lastMessage = last.content,
                            lastMessageTime = formatTime(last.created_at),
                            isRead = last.is_read
                        )
                    }

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

    private fun buildName(first: String?, last: String?): String {
        val full = listOfNotNull(first, last).joinToString(" ").trim()
        return full.ifBlank { "Vendedor Desconocido" }
    }

    private fun formatTime(isoDate: String?): String {
        if (isoDate == null) return ""
        return try {
            isoDate.substring(11, 16)
        } catch (e: Exception) {
            ""
        }
    }
}