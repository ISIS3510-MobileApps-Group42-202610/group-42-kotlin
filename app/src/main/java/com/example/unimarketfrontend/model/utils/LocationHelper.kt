package com.example.unimarketfrontend.model.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.*

data class CampusPoint(val name: String, val lat: Double, val lng: Double)

object LocationHelper {

    // Coordenadas más precisas basadas en el centro de cada edificio
    // El radio del campus completo es ~300m, los edificios individuales ~30-50m
    private val universityBuildings = listOf(
        CampusPoint("Mario Laserna (ML)", 4.60127, -74.06575),
        CampusPoint("Edificio W", 4.60087, -74.06665),
        CampusPoint("Edificio SD", 4.60188, -74.06485),
        CampusPoint("Edificio Cívico", 4.60065, -74.06688),
        CampusPoint("Edificio Au", 4.60228, -74.06453),
        CampusPoint("Biblioteca General", 4.60005, -74.06620)
    )

    // Radio máximo para considerar que estás EN un edificio específico
    private const val BUILDING_RADIUS_M = 50.0

    // Diferencia mínima para declarar un ganador sin ambigüedad
    // Si dos edificios están a distancias muy similares, no reportamos ninguno
    // para evitar el bug de "estaba en W pero dice ML"
    private const val AMBIGUITY_THRESHOLD_M = 15.0

    // Devuelve el nombre del edificio más cercano si está dentro del radio
    // Devuelve null si no hay edificio cercano o si la detección es ambigua
    suspend fun getNearbyBuilding(context: Context): String? = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext null

        val location = withTimeoutOrNull(5_000) {
            getCurrentLocation(context)
        } ?: getLastLocation(context)

        location?.let { loc ->
            val candidates = universityBuildings
                .map { building ->
                    val dist = haversine(loc.latitude, loc.longitude, building.lat, building.lng)
                    building to dist
                }
                .filter { (_, dist) -> dist <= BUILDING_RADIUS_M }
                .sortedBy { (_, dist) -> dist }

            when {
                candidates.isEmpty() -> null

                candidates.size == 1 -> candidates.first().first.name

                else -> {
                    // Hay más de un edificio en el radio — verificamos ambigüedad
                    val closest = candidates[0]
                    val secondClosest = candidates[1]
                    val diff = secondClosest.second - closest.second

                    if (diff >= AMBIGUITY_THRESHOLD_M) {
                        // El más cercano está claramente más cerca → reportamos ese
                        closest.first.name
                    } else {
                        // Demasiado ambiguo → no reportamos edificio específico
                        // pero sí informamos que el usuario está en el campus
                        "Uniandes Campus"
                    }
                }
            }
        }
    }

    // Para el banner del ChatScreen: ¿estoy en el campus aunque sea?
    // Usa radio más grande (150m desde el centro del campus)
    suspend fun isOnCampus(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext false
        val location = withTimeoutOrNull(5_000) {
            getCurrentLocation(context)
        } ?: getLastLocation(context) ?: return@withContext false

        val campusCenter = CampusPoint("Centro campus", 4.6013, -74.0659)
        haversine(location.latitude, location.longitude, campusCenter.lat, campusCenter.lng) <= 150.0
    }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(context: Context): Location? =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(context: Context): Location? =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        }

    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}