package com.example.unimarketfrontend.model.utils.analytics

import com.example.unimarketfrontend.model.network.client.AnalyticsRetrofitInstance
import com.example.unimarketfrontend.network.model.AnalyticsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AnalyticsLogger {
    private var currentUserId: Int = 0
    fun setUserId(userId: Int) { currentUserId = userId }

    fun log(eventName: String, properties: Map<String, String> = emptyMap()) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // SPRINT 3: Blindaje para que Django no explote
                val safeProps = mutableMapOf<String, String>()
                safeProps["seller_id"] = properties["seller_id"] ?: "0"
                safeProps["avg_minutes"] = properties["avg_minutes"] ?: "0.0"
                safeProps["unread_conversations"] = properties["unread_conversations"] ?: "0"
                safeProps["timestamp"] = System.currentTimeMillis().toString()

                // Agregamos el resto de propiedades que vengan
                properties.forEach { (k, v) -> if (!safeProps.containsKey(k)) safeProps[k] = v }

                val event = AnalyticsEvent(
                    event_name = eventName,
                    user_id = currentUserId,
                    properties = safeProps
                )
                AnalyticsRetrofitInstance.api.logEvent(event)
            } catch (e: Exception) { }
        }
    }
}