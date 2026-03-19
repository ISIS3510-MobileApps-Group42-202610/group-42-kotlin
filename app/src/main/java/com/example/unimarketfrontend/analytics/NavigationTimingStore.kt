package com.example.unimarketfrontend.analytics

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

data class NavigationLatencySample(
    val fromRoute: String,
    val toRoute: String,
    val durationMs: Long
)

class NavigationTimingStore(
    private val nowMs: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private data class PendingNavigation(
        val fromRoute: String,
        val startedAtMs: Long
    )

    private val pendingByDestination = ConcurrentHashMap<String, PendingNavigation>()

    fun start(fromRoute: String?, toRoute: String) {
        pendingByDestination[toRoute] = PendingNavigation(
            fromRoute = fromRoute.orEmpty(),
            startedAtMs = nowMs()
        )
    }

    fun finish(toRoute: String): NavigationLatencySample? {
        val pending = pendingByDestination.remove(toRoute) ?: return null
        val durationMs = (nowMs() - pending.startedAtMs).coerceAtLeast(0L)
        return NavigationLatencySample(
            fromRoute = pending.fromRoute,
            toRoute = toRoute,
            durationMs = durationMs
        )
    }
}

