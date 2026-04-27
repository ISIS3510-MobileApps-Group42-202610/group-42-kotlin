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

data class CampusPoint(val name: String, val lat: Double, val lng: Double)

object LocationHelper {
    // SPRINT 3: Lista extendida con el edificio Civico
    private val universityBuildings = listOf(
        CampusPoint("Mario Laserna (ML)", 4.6013, -74.0657),
        CampusPoint("Edificio SD", 4.6025, -74.0650),
        CampusPoint("Edificio W", 4.6010, -74.0665),
        CampusPoint("Edificio Cívico", 4.6008, -74.0668), // Nuevo: Civico
        CampusPoint("Edificio Au", 4.6020, -74.0645),
        CampusPoint("Biblioteca General", 4.6005, -74.0660)
    )

    // SMART FEATURE: Ahora busca el edificio MAS CERCANO, no el primero
    suspend fun getNearbyBuilding(context: Context): String? = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext null

        val location = withTimeoutOrNull(5000) {
            getCurrentLocation(context)
        } ?: getLastLocation(context)

        location?.let { loc ->
            universityBuildings
                .map { it to haversine(loc.latitude, loc.longitude, it.lat, it.lng) }
                .filter { it.second <= 120.0 } // Radio de 120 metros para mayor precision
                .minByOrNull { it.second } // Tomamos el mas cercano
                ?.first?.name
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

    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}