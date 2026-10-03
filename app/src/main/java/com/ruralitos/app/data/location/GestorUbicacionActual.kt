package com.ruralitos.app.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object GestorUbicacionActual {
    fun tienePermiso(context: Context): Boolean = tienePermisoPreciso(context) ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    fun tienePermisoPreciso(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** No devuelve un dato antiguo ni una ubicación de red de kilómetros como si fuera GPS preciso. */
    // El permiso se comprueba en la primera línea; las llamadas están dentro de runCatching por si se retira a mitad.
    @android.annotation.SuppressLint("MissingPermission")
    suspend fun obtener(context: Context): Location? = withTimeoutOrNull(25_000L) {
        if (!tienePermiso(context)) return@withTimeoutOrNull null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val proveedores = buildList {
            if (tienePermisoPreciso(context) &&
                runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (runCatching { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }
        if (proveedores.isEmpty()) return@withTimeoutOrNull null

        fun edad(location: Location): Long =
            (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
        fun util(location: Location, antiguedadMaxima: Long): Boolean =
            location.hasAccuracy() && location.accuracy in 0f..80f &&
                edad(location) in 0L..antiguedadMaxima &&
                location.latitude.isFinite() && location.longitude.isFinite()

        val gpsHabilitado = LocationManager.GPS_PROVIDER in proveedores
        val cacheGps = if (gpsHabilitado) {
            runCatching { manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }
                .getOrNull()?.takeIf { util(it, 15_000L) }
        } else null
        val cacheRed = if (LocationManager.NETWORK_PROVIDER in proveedores) {
            runCatching { manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }
                .getOrNull()?.takeIf { util(it, 15_000L) }
        } else null
        if (cacheGps != null && cacheGps.accuracy <= 30f) return@withTimeoutOrNull cacheGps
        if (!gpsHabilitado && cacheRed != null && cacheRed.accuracy <= 30f) return@withTimeoutOrNull cacheRed

        suspendCancellableCoroutine<Location?> { continuacion ->
            val handler = Handler(Looper.getMainLooper())
            val listeners = mutableListOf<LocationListener>()
            var mejorGps: Location? = cacheGps
            var mejorRed: Location? = cacheRed
            var terminado = false
            fun terminar(resultado: Location?) {
                if (terminado) return
                terminado = true
                listeners.forEach { runCatching { manager.removeUpdates(it) } }
                if (continuacion.isActive) continuacion.resume(resultado)
            }
            val espera = Runnable {
                terminar(mejorGps?.takeIf { util(it, 30_000L) }
                    ?: mejorRed?.takeIf { util(it, 30_000L) && it.accuracy <= 50f })
            }
            continuacion.invokeOnCancellation {
                handler.removeCallbacks(espera)
                listeners.forEach { runCatching { manager.removeUpdates(it) } }
            }
            proveedores.forEach { proveedor ->
                val listener = LocationListener { nueva ->
                    if (!util(nueva, 10_000L)) return@LocationListener
                    if (proveedor == LocationManager.GPS_PROVIDER) {
                        if (mejorGps == null || nueva.accuracy < checkNotNull(mejorGps).accuracy) mejorGps = nueva
                    } else if (mejorRed == null || nueva.accuracy < checkNotNull(mejorRed).accuracy) {
                        mejorRed = nueva
                    }
                    if (nueva.accuracy <= 30f && (proveedor == LocationManager.GPS_PROVIDER || !gpsHabilitado)) {
                        handler.removeCallbacks(espera)
                        terminar(nueva)
                    }
                }
                runCatching {
                    manager.requestLocationUpdates(proveedor, 0L, 0f, listener, Looper.getMainLooper())
                    listeners += listener
                }
            }
            if (listeners.isEmpty()) terminar(cacheGps ?: cacheRed?.takeIf { it.accuracy <= 50f })
            else handler.postDelayed(espera, 20_000L)
        }
    }
}
