package com.example.unimarketfrontend.utils.analytics

import com.example.unimarketfrontend.network.client.AnalyticsRetrofitInstance
import com.example.unimarketfrontend.network.model.AnalyticsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// BQ4 EN CHAT VIEW MODEL
//BQ2 TMB EN CHAT VIEW MODEL
//SMART FEATURE EN LOCATION HELPER
// Singleton global para enviar eventos al analytics engine
object AnalyticsLogger {

    private var currentUserId: Int = 0

    // Llama esto después de un login exitoso
    fun setUserId(userId: Int) {
        currentUserId = userId
    }

    // Envía un evento de forma asíncrona sin bloquear la UI
    // eventName: nombre del evento (ej: "messages_screen_opened")
    // properties: datos adicionales del evento como mapa clave-valor
    // = emptyMap() significa que si no pasás properties, usa un mapa vacío
    fun log(eventName: String, properties: Map<String, String> = emptyMap()) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val event = AnalyticsEvent(
                    event_name = eventName,
                    user_id = currentUserId,
                    properties = properties + mapOf(
                        "timestamp" to System.currentTimeMillis().toString()
                    )
                )
                AnalyticsRetrofitInstance.api.logEvent(event)
            } catch (e: Exception) {
                // Si el analytics engine falla, ignoramos el error
                // Nunca queremos que analytics rompa la app
            }
        }
    }
}