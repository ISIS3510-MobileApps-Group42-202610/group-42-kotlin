package com.example.unimarketfrontend.model.utils.analytics

import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.unimarketfrontend.model.analytics.PerformanceEventType
import com.example.unimarketfrontend.model.analytics.PerformanceTelemetryRequest
import com.example.unimarketfrontend.model.network.api.AnalyticsApiService
import com.example.unimarketfrontend.model.network.client.AnalyticsRetrofitInstance
import com.example.unimarketfrontend.model.utils.NavigationTimingStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/*
 * Este es el PerformanceTracker.
 * Solo se encarga de medir que tan rapido abre la app y que tan rapido navega.
 */
class PerformanceTracker(
    private val api: AnalyticsApiService,
    private val navigationTimingStore: NavigationTimingStore = NavigationTimingStore()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Mide cuanto tarda en arrancar la app desde el MainActivity
    fun trackAppStartup(startElapsedRealtimeMs: Long) {
        val durationMs = (SystemClock.elapsedRealtime() - startElapsedRealtimeMs)
            .coerceAtLeast(0L)
        sendPerformanceEvent(
            eventType = PerformanceEventType.APP_STARTUP,
            durationMs = durationMs
        )
    }

    // Registra cuando picas un boton para ir a otra pantalla
    fun markNavigationStart(fromRoute: String?, toRoute: String) {
        navigationTimingStore.start(fromRoute = fromRoute, toRoute = toRoute)
    }

    // Registra cuando la pantalla ya termino de cargar y se ve
    fun onDestinationDisplayed(route: String?) {
        if (route.isNullOrBlank()) return
        val sample = navigationTimingStore.finish(route) ?: return
        sendPerformanceEvent(
            eventType = PerformanceEventType.SCREEN_NAVIGATION,
            durationMs = sample.durationMs
        )
    }

    private fun sendPerformanceEvent(eventType: PerformanceEventType, durationMs: Long) {
        val request = PerformanceTelemetryRequest(
            event_type = eventType.wireValue,
            device_model = "${Build.MANUFACTURER} ${Build.MODEL}",
            platform = "android",
            duration_ms = durationMs,
            os_version = Build.VERSION.RELEASE ?: "unknown",
            app_version = "1.0"
        )

        scope.launch {
            try {
                api.sendPerformanceTelemetry(request)
            } catch (e: Exception) {
                Log.w("PerformanceTracker", "Fallo el envio de performance")
            }
        }
    }
}

object PerformanceTrackerProvider {
    val tracker: PerformanceTracker by lazy {
        PerformanceTracker(api = AnalyticsRetrofitInstance.api)
    }
}