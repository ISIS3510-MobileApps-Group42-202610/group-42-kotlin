package com.example.unimarketfrontend.model.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/*
 * Monitorea el estado de la conexion a internet en tiempo real.
 * Sigue el patron Singleton para que toda la app consulte el mismo estado.
 * El libro Ch. 10 recomienda informar al usuario sobre la conectividad para evitar NIM.
 */
object ConnectivityMonitor {

    private val _isOnline = MutableStateFlow(false) // CORRECCIÓN: Iniciar en false hasta verificar
    val isOnline: StateFlow<Boolean> = _isOnline

    private var isStarted = false
    private var callback: ConnectivityManager.NetworkCallback? = null

    // Se llama en el onCreate del MainActivity para prender los sensores de red
    fun start(context: Context) {
        if (isStarted) {
            android.util.Log.d("ConnectivityMonitor", "Already started, skipping")
            return // Evitar múltiples registros
        }
        isStarted = true

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Verificación inicial CRÍTICA
        _isOnline.value = isCurrentlyConnected(cm)
        android.util.Log.d("ConnectivityMonitor", "Initial connectivity state: ${_isOnline.value}")

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) // NUEVO: Verificar que realmente hay internet
            .build()

        callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                android.util.Log.d("ConnectivityMonitor", "Network available")
                // Verificar que realmente tiene internet antes de marcar como online
                val caps = cm.getNetworkCapabilities(network)
                if (caps != null) {
                    val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    _isOnline.value = hasInternet && isValidated
                    android.util.Log.d("ConnectivityMonitor", "Network available - hasInternet=$hasInternet, validated=$isValidated")
                }
            }

            override fun onLost(network: Network) {
                android.util.Log.d("ConnectivityMonitor", "Network lost")
                // Verificar si hay otra red disponible antes de marcar como offline
                _isOnline.value = isCurrentlyConnected(cm)
                android.util.Log.d("ConnectivityMonitor", "Network lost - current state: ${_isOnline.value}")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val isConnected = hasInternet && isValidated

                android.util.Log.d("ConnectivityMonitor", "Capabilities changed - Internet: $hasInternet, Validated: $isValidated, Connected: $isConnected")
                _isOnline.value = isConnected
            }
        }

        cm.registerNetworkCallback(request, callback!!)
    }

    // Meodo para reiniciar el monitor (útil al cambiar de sesión)
    fun restart(context: Context) {
        android.util.Log.d("ConnectivityMonitor", "Restarting connectivity monitor")
        stop(context)
        start(context)
    }

    // Metodo para detener el monitor
    fun stop(context: Context) {
        if (!isStarted) return

        android.util.Log.d("ConnectivityMonitor", "Stopping connectivity monitor")
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        callback?.let { cm.unregisterNetworkCallback(it) }
        callback = null
        isStarted = false
    }

    private fun isCurrentlyConnected(cm: ConnectivityManager): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return hasInternet && isValidated
    }
}