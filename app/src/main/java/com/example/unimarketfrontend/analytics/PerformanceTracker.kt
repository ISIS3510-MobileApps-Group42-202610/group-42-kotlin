package com.example.unimarketfrontend.analytics

import android.os.Build
import android.util.Log
import com.example.unimarketfrontend.analytics.model.PerformanceEventType
import com.example.unimarketfrontend.analytics.model.PerformanceTelemetryRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PerformanceTracker(
    private val api: AnalyticsApiService,
    private val navigationTimingStore: NavigationTimingStore = NavigationTimingStore()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun trackAppStartup(startElapsedRealtimeMs: Long) {
        val durationMs = (android.os.SystemClock.elapsedRealtime() - startElapsedRealtimeMs)
            .coerceAtLeast(0L)
        sendPerformanceEvent(
            eventType = PerformanceEventType.APP_STARTUP,
            durationMs = durationMs
        )
    }

    fun markNavigationStart(fromRoute: String?, toRoute: String) {
        navigationTimingStore.start(fromRoute = fromRoute, toRoute = toRoute)
    }

    fun onDestinationDisplayed(route: String?) {
        if (route.isNullOrBlank()) return
        val sample = navigationTimingStore.finish(route) ?: return
        sendPerformanceEvent(
            eventType = PerformanceEventType.SCREEN_NAVIGATION,
            durationMs = sample.durationMs
        )
    }

    private fun sendPerformanceEvent(
        eventType: PerformanceEventType,
        durationMs: Long
    ) {
        val request = PerformanceTelemetryRequest(
            event_type = eventType.wireValue,
            device_model = deviceModel(),
            platform = "android",
            duration_ms = durationMs,
            os_version = Build.VERSION.RELEASE ?: "unknown",
            app_version = appVersionName()
        )

        scope.launch {
            runCatching {
                api.sendPerformanceTelemetry(request)
            }.onSuccess { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Performance telemetry failed with HTTP ${response.code()}")
                }
            }.onFailure { error ->
                Log.w(TAG, "Performance telemetry failed", error)
            }
        }
    }

    // Avoid direct BuildConfig import: some AGP/Kotlin setups fail to expose it to this source set.
    private fun appVersionName(): String {
        return runCatching {
            val buildConfigClass = Class.forName("com.example.unimarketfrontend.BuildConfig")
            buildConfigClass.getField("VERSION_NAME").get(null) as? String
        }.getOrNull().orEmpty().ifBlank { "unknown" }
    }

    private fun deviceModel(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        return listOf(manufacturer, model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "unknown" }
    }

    companion object {
        private const val TAG = "PerformanceTracker"
    }
}

object PerformanceTrackerProvider {
    val tracker: PerformanceTracker by lazy {
        PerformanceTracker(api = AnalyticsRetrofitInstance.api)
    }
}
