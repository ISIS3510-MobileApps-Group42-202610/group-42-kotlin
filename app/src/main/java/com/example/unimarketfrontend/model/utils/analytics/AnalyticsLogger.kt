package com.example.unimarketfrontend.model.utils.analytics

import com.example.unimarketfrontend.model.analytics.MessagingResponseEventRequest
import com.example.unimarketfrontend.model.network.client.AnalyticsRetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object AnalyticsLogger {
    private var currentUserId: Int = 0

    fun setUserId(userId: Int) {
        currentUserId = userId
    }

    // Método principal para eventos de mensajería (BQ4)
    // Ahora manda al endpoint correcto con el schema correcto
    fun logMessagingEvent(
        eventName: String,
        sellerId: Int = 0,
        avgResponseMinutes: Double = 0.0,
        unreadConversations: Int = 0
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = MessagingResponseEventRequest(
                    event_name = eventName,
                    user_id = currentUserId,
                    seller_id = sellerId,
                    avg_response_minutes = avgResponseMinutes,
                    unread_conversations = unreadConversations,
                    timestamp = nowIso()
                )
                AnalyticsRetrofitInstance.api.sendMessagingEvent(request)
            } catch (e: Exception) {
                // Analytics failures never affect the user experience
            }
        }
    }

    // Método legacy para compatibilidad con código existente
    // Se mantiene para no romper las llamadas que ya existen
    fun log(eventName: String, properties: Map<String, String> = emptyMap()) {
        val sellerId = properties["seller_id"]?.toIntOrNull() ?: 0
        val avgMinutes = properties["avg_minutes"]?.toDoubleOrNull() ?: 0.0
        val unread = properties["unread_conversations"]?.toIntOrNull() ?: 0
        logMessagingEvent(
            eventName = eventName,
            sellerId = sellerId,
            avgResponseMinutes = avgMinutes,
            unreadConversations = unread
        )
    }

    private fun nowIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}