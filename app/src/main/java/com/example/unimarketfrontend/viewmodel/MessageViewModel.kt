package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        loadMessages()
    }

    fun loadMessages() {
        _uiState.value = MessagesUiState.Success(
            conversations = listOf(
                ConversationPreview(
                    otherPersonName = "Juan Pérez",
                    lastMessage = "Is the calculus book still available?",
                    lastMessageTime = "2026-02-24",
                    isRead = false
                ),
                ConversationPreview(
                    otherPersonName = "María García",
                    lastMessage = "Yes! I can meet tomorrow at ML",
                    lastMessageTime = "2026-02-23",
                    isRead = true
                ),
                ConversationPreview(
                    otherPersonName = "Carlos López",
                    lastMessage = "How much for the TI-84?",
                    lastMessageTime = "2026-02-22",
                    isRead = true
                )
            )
        )
    }
}