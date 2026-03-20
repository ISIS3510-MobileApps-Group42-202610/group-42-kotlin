package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.analytics.AnalyticsLogger
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.Message
import com.example.unimarketfrontend.network.model.SendMessageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    // Loads the conversation thread with a specific seller
    fun loadThread(sellerId: Int) {
        viewModelScope.launch {
            try {
                val thread = RetrofitInstance.api.getThread(sellerId)
                _messages.value = thread
                computeAndLogResponseTime(thread, sellerId)
            } catch (e: Exception) {
                // Keep existing messages on failure
            }
        }
    }

    // Sends a message and appends it locally on success
    fun sendMessage(sellerId: Int, content: String) {
        viewModelScope.launch {
            _isSending.value = true
            try {
                val sent = RetrofitInstance.api.sendMessageAsBuyer(
                    SendMessageRequest(seller_id = sellerId, content = content)
                )
                _messages.value = _messages.value + sent
                AnalyticsLogger.log(
                    "message_sent",
                    mapOf("seller_id" to sellerId.toString())
                )
            } catch (e: Exception) {
                // Handle send error
            } finally {
                _isSending.value = false
            }
        }
    }

    // BQ4: for each buyer message, finds the next seller reply and measures the gap in minutes
    private fun computeAndLogResponseTime(messages: List<Message>, sellerId: Int) {
        val buyerMessages = messages.filter { it.sent_by == "buyer" }
        val sellerReplies = messages.filter { it.sent_by == "seller" }

        if (buyerMessages.isEmpty() || sellerReplies.isEmpty()) return

        val responseTimes = mutableListOf<Long>()
        for (buyerMsg in buyerMessages) {
            val sentAt = parseMillis(buyerMsg.created_at) ?: continue
            val firstReply = sellerReplies
                .mapNotNull { parseMillis(it.created_at) }
                .filter { it > sentAt }
                .minOrNull() ?: continue
            responseTimes.add((firstReply - sentAt) / 60_000)
        }

        if (responseTimes.isNotEmpty()) {
            val avg = responseTimes.average().toLong()
            AnalyticsLogger.log(
                "seller_avg_response_time",
                mapOf(
                    "seller_id" to sellerId.toString(),
                    "avg_minutes" to avg.toString()
                )
            )
        }
    }

    private fun parseMillis(iso: String?): Long? = try {
        Instant.parse(iso).toEpochMilli()
    } catch (e: Exception) {
        null
    }
}