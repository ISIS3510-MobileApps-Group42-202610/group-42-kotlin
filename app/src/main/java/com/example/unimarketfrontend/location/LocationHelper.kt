
package com.example.unimarketfrontend.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.*

private const val CAMPUS_LAT = 4.6013
private const val CAMPUS_LNG = -74.0657
private const val CAMPUS_RADIUS_METERS = 600.0

object LocationHelper {

    suspend fun isOnCampus(context: Context): Boolean {
        if (!hasPermission(context)) return false
        val location = getLastLocation(context) ?: return false
        val distance = haversine(location.latitude, location.longitude, CAMPUS_LAT, CAMPUS_LNG)
        return distance <= CAMPUS_RADIUS_METERS
    }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    // Tipo explícito Location? para que Kotlin pueda inferir el tipo del coroutine
    private suspend fun getLastLocation(context: Context): Location? =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            try {
                client.lastLocation
                    .addOnSuccessListener { location: Location? -> cont.resume(location) }
                    .addOnFailureListener { cont.resume(null) }
            } catch (e: SecurityException) {
                cont.resume(null)
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
