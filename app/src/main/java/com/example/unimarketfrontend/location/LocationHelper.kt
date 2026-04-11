package com.example.unimarketfrontend.location

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

private const val CAMPUS_LAT = 4.6013
private const val CAMPUS_LNG = -74.0657
private const val CAMPUS_RADIUS_METERS = 600.0

object LocationHelper {

    suspend fun isOnCampus(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext false

        val location = withTimeoutOrNull(5000) {
            getCurrentLocation(context)
        } ?: getLastLocation(context)

        location?.let {
            val distance = haversine(it.latitude, it.longitude, CAMPUS_LAT, CAMPUS_LNG)
            distance <= CAMPUS_RADIUS_METERS
        } ?: false
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

            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (cont.isActive) cont.resume(location)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }

            cont.invokeOnCancellation { cts.cancel() }
        }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(context: Context): Location? =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (cont.isActive) cont.resume(location)
                }
                .addOnFailureListener {
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