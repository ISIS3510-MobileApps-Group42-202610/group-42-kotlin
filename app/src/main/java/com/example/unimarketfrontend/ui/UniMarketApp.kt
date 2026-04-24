package com.example.unimarketfrontend.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import com.example.unimarketfrontend.utils.analytics.PerformanceTrackerProvider
import com.example.unimarketfrontend.network.client.RetrofitInstance
import com.example.unimarketfrontend.ui.navigation.AppNavigation

@Composable
fun UniMarketApp(startupStartElapsedMs: Long, context: Context) {
    LaunchedEffect(startupStartElapsedMs) {
        withFrameNanos { }
        PerformanceTrackerProvider.tracker.trackAppStartup(startupStartElapsedMs)
    }

    RetrofitInstance.init(context)
    AppNavigation()
}