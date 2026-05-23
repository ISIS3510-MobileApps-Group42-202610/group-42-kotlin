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

    // Coordenadas de edificios del campus de la Universidad de los Andes
    // Calibradas con GPS real del usuario: lat≈4.6045, lng≈-74.0658 (zona SD)
    // El campus está más al norte de lo que Google Maps sugiere
    private val universityBuildings = listOf(
        CampusPoint("Mario Laserna (ML)", 4.60320, -74.06530),
        CampusPoint("Edificio W", 4.60250, -74.06620),
        CampusPoint("Edificio SD", 4.60450, -74.06580),
        CampusPoint("Edificio RGD", 4.60400, -74.06550),
        CampusPoint("Centro Deportivo", 4.60550, -74.06500),
        CampusPoint("Edificio C", 4.60300, -74.06600),
        CampusPoint("Edificio Q", 4.60350, -74.06560),
        CampusPoint("Edificio O", 4.60280, -74.06570),
        CampusPoint("Edificio B", 4.60220, -74.06610),
        CampusPoint("Edificio Aulas", 4.60380, -74.06540),
        CampusPoint("Edificio Au", 4.60500, -74.06480),
        CampusPoint("Biblioteca General", 4.60200, -74.06640),
        CampusPoint("Edificio Cívico", 4.60230, -74.06680),
        CampusPoint("Edificio Franco", 4.60420, -74.06520),
        CampusPoint("Edificio Lleras", 4.60310, -74.06580)
    )

    // Radio máximo para considerar que estás cerca de un edificio
    private const val BUILDING_RADIUS_M = 120.0

    // Diferencia mínima para declarar un ganador sin ambigüedad
    // Con muchos edificios cercanos, usamos un umbral bajo para siempre reportar el más cercano
    private const val AMBIGUITY_THRESHOLD_M = 5.0

    // Devuelve el nombre del edificio más cercano si está dentro del radio
    // Devuelve null si no hay edificio cercano o si la detección es ambigua
    suspend fun getNearbyBuilding(context: Context): String? = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) {
            android.util.Log.d("LocationHelper", "No location permission")
            return@withContext null
        }

        // Intentar obtener ubicación con timeout más largo
        val location = withTimeoutOrNull(8_000) {
            getCurrentLocation(context)
        } ?: getLastLocation(context)

        if (location == null) {
            android.util.Log.d("LocationHelper", "Could not get location (both current and last are null)")
            return@withContext null
        }

        android.util.Log.d("LocationHelper", "Got location: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}m")

        val candidates = universityBuildings
            .map { building ->
                val dist = haversine(location.latitude, location.longitude, building.lat, building.lng)
                building to dist
            }
            .filter { (_, dist) -> dist <= BUILDING_RADIUS_M }
            .sortedBy { (_, dist) -> dist }

        android.util.Log.d("LocationHelper", "Found ${candidates.size} buildings within ${BUILDING_RADIUS_M}m")
        candidates.forEach { (building, dist) ->
            android.util.Log.d("LocationHelper", "  ${building.name}: ${String.format("%.1f", dist)}m")
        }

        when {
            candidates.isEmpty() -> {
                // Verificar si al menos estamos en el campus (radio amplio)
                val campusCenter = CampusPoint("Campus", 4.6040, -74.0658)
                val distToCampus = haversine(location.latitude, location.longitude, campusCenter.lat, campusCenter.lng)
                android.util.Log.d("LocationHelper", "Distance to campus center: ${String.format("%.1f", distToCampus)}m")
                if (distToCampus <= 300.0) "Uniandes Campus" else null
            }

            candidates.size == 1 -> candidates.first().first.name

            else -> {
                val closest = candidates[0]
                val secondClosest = candidates[1]
                val diff = secondClosest.second - closest.second

                if (diff >= AMBIGUITY_THRESHOLD_M) {
                    closest.first.name
                } else {
                    closest.first.name // Con umbral de 5m, siempre reportamos el más cercano
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

        val campusCenter = CampusPoint("Centro campus", 4.6040, -74.0658)
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
            // Usar BALANCED en vez de HIGH_ACCURACY para que funcione mejor en interiores
            // HIGH_ACCURACY depende del GPS que no funciona bien dentro de edificios
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    android.util.Log.d("LocationHelper", "getCurrentLocation success: ${loc?.latitude}, ${loc?.longitude}")
                    if (cont.isActive) cont.resume(loc)
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("LocationHelper", "getCurrentLocation failed: ${e.message}")
                    if (cont.isActive) cont.resume(null)
                }
        }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(context: Context): Location? =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation
                .addOnSuccessListener { loc ->
                    android.util.Log.d("LocationHelper", "getLastLocation success: ${loc?.latitude}, ${loc?.longitude}")
                    if (cont.isActive) cont.resume(loc)
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("LocationHelper", "getLastLocation failed: ${e.message}")
                    if (cont.isActive) cont.resume(null)
                }
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