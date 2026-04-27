package com.example.unimarketfrontend.model.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.*

/*
 * Helper para el sensor de ubicacion (GPS).
 * SPRINT 3: Ahora detecta edificios especificos para la feature Context-Aware.
 * Incluye la formula de Haversine para calculo de distancia en esfera.
 */
data class CampusPoint(val name: String, val lat: Double, val lng: Double)

object LocationHelper {
    
    // Lista de puntos clave en la Universidad de los Andes
    private val universityBuildings = listOf(
        CampusPoint("Mario Laserna (ML)", 4.6013, -74.0657),
        CampusPoint("Edificio SD", 4.6025, -74.0650),
        CampusPoint("Edificio W", 4.6010, -74.0665),
        CampusPoint("Edificio Au", 4.6020, -74.0645),
        CampusPoint("Biblioteca General", 4.6005, -74.0660)
    )

    // Funcion original para compatibilidad con pantallas existentes
    suspend fun isOnCampus(context: Context): Boolean {
        return getNearbyBuilding(context) != null
    }

    // Funcion "Smart": te dice en que edificio estas exactamente
    suspend fun getNearbyBuilding(context: Context): String? = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext null

        val location = withTimeoutOrNull(5000) {
            getCurrentLocation(context)
        } ?: getLastLocation(context)

        location?.let { loc ->
            // Si estas a menos de 150 metros de algun edificio, lo detectamos
            universityBuildings.firstOrNull { building ->
                haversine(loc.latitude, loc.longitude, building.lat, building.lng) <= 150.0
            }?.name
        }
    }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(context: Context): Location? = suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        cont.invokeOnCancellation { cts.cancel() }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(context: Context): Location? = suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation
            .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
    }

    // Formula de Haversine para calcular distancia real en metros sobre la Tierra
    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0 
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
