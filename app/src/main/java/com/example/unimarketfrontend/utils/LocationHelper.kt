package com.example.unimarketfrontend.utils

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


// Coordenadas del centro de la Universidad y el radio de deteccion (600 metros)
private const val CAMPUS_LAT = 4.6013
private const val CAMPUS_LNG = -74.0657
private const val CAMPUS_RADIUS_METERS = 600.0


object LocationHelper {

    // Funcion principal que nos dice si el usuario esta en la U o no
    // Es suspend porque el GPS puede tardar en responder y no queremos trabar la app
    suspend fun isOnCampus(context: Context): Boolean = withContext(Dispatchers.IO) {
        // Primero chequeamos si tenemos permiso, si no, ni lo intentamos
        if (!hasPermission(context)) return@withContext false

        // Intentamos sacar la ubicacion actual, pero solo esperamos 5 segundos
        // Si en 5 segundos el GPS no responde, saltamos al plan B (ultima ubicacion conocida)
        val location = withTimeoutOrNull(5000) {
            getCurrentLocation(context)
        } ?: getLastLocation(context)

        // Si logramos sacar la posicion, calculamos la distancia usando Haversine
        location?.let {
            val distance = haversine(it.latitude, it.longitude, CAMPUS_LAT, CAMPUS_LNG)
            distance <= CAMPUS_RADIUS_METERS
        } ?: false
    }

    // Funcion basica para ver si el usuario ya nos dio permiso de usar el GPS
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    // Esta funcion pide la posicion exacta al sensor GPS en este preciso momento
    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(context: Context): Location? =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            // Pedimos ubicacion de alta precision
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (cont.isActive) cont.resume(location)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }

            // Si el usuario cierra la pantalla mientras buscamos, cancelamos la peticion
            cont.invokeOnCancellation { cts.cancel() }
        }

    // Plan B: saca la ultima posicion que el celular guardo en cache (es instantaneo)
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

    /**
     * Haversine formula. Giving great-circle distances between two points on a sphere from their longitudes and latitudes.
     * It is a special case of a more general formula in spherical trigonometry, the law of haversines, relating the
     * sides and angles of spherical "triangles".
     *
     * https://rosettacode.org/wiki/Haversine_formula#Java
     *
     * @return Distance in kilometers
     */


    // use también el repo https://gist.github.com/jferrao/cb44d09da234698a7feee68ca895f491 para calcular esta función
    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}