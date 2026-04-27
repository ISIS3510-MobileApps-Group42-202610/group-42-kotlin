package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.local.ConversationEntity
import com.example.unimarketfrontend.model.repository.MessagesRepository
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import com.example.unimarketfrontend.model.utils.analytics.AnalyticsLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MessagesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessagesRepository(application)

    private val _conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val conversations: StateFlow<List<ConversationEntity>> = _conversations

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    init {
        observeLocalConversations()
        observeConnectivity()
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
                if (online) {
                    launch {
                        repository.retryPendingMessages()
                        repository.refreshFromNetwork()
                    }
                }
            }
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            _isRefreshing.value = true
            if (repository.isCacheStale() || _conversations.value.isEmpty()) {
                val success = repository.refreshFromNetwork()
                if (!success && _conversations.value.isEmpty()) {
                    _isOffline.value = true
                }
            }
            _isRefreshing.value = false

            val unread = _conversations.value.count { !it.isRead }
            AnalyticsLogger.log("messages_screen_opened", mapOf("unread_conversations" to unread.toString()))
        }
    }
}