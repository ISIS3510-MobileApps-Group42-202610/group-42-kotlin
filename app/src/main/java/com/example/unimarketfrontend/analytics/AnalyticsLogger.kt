package com.example.unimarketfrontend.analytics

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.unimarketfrontend.network.AnalyticsRetrofitInstance
import com.example.unimarketfrontend.network.model.AnalyticsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

// Global singleton for sending analytics events
object AnalyticsLogger {

    private var currentUserId: Int = 0

    // Call this after a successful login
    fun setUserId(userId: Int) {
        currentUserId = userId
    }

    // Sends an event asynchronously without blocking the UI
    @RequiresApi(Build.VERSION_CODES.O)
    fun log(eventName: String, properties: Map<String, String> = emptyMap()) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val event = AnalyticsEvent(
                    event_name = eventName,
                    user_id = currentUserId,
                    properties = properties + mapOf("timestamp" to Instant.now().toString())
                )
                AnalyticsRetrofitInstance.api.logEvent(event)
            } catch (e: Exception) {
                // Analytics failures never affect the user experience
            }
        }
    }
}