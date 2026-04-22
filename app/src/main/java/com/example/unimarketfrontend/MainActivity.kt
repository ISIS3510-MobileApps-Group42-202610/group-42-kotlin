package com.example.unimarketfrontend

import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.unimarketfrontend.network.ConnectivityMonitor
import com.example.unimarketfrontend.ui.UniMarketApp
import com.example.unimarketfrontend.ui.theme.UniMarketFrontendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val startupStartElapsedMs = runCatching {
            Process.getStartElapsedRealtime()
        }.getOrDefault(SystemClock.elapsedRealtime())

        super.onCreate(savedInstanceState)
        
        // --- SPRINT 3: CONECTIVIDAD EVENTUAL ---
        // Encendemos el monitor de red apenas abre la app. 
        // Esto permite que toda la app sepa si hay internet desde el segundo 1.
        ConnectivityMonitor.start(applicationContext)

        enableEdgeToEdge()
        setContent {
            UniMarketFrontendTheme {
                UniMarketApp(
                    startupStartElapsedMs = startupStartElapsedMs,
                    context = applicationContext
                )
            }
        }
    }
}