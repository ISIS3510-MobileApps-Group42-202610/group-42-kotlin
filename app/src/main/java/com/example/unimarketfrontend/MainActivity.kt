package com.example.unimarketfrontend

import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import com.example.unimarketfrontend.ui.UniMarketApp
import com.example.unimarketfrontend.ui.theme.UniMarketFrontendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- SPRINT 3: CONECTIVIDAD EVENTUAL ---
        // Encendemos el monitor de red una sola vez usando el contexto de la aplicación
        ConnectivityMonitor.start(applicationContext)

        val startupStartElapsedMs = runCatching {
            Process.getStartElapsedRealtime()
        }.getOrDefault(SystemClock.elapsedRealtime())

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

    override fun onDestroy() {
        super.onDestroy()
        ConnectivityMonitor.stop(applicationContext)
    }
}