package com.ruralitos.app.data.location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Posición del GPS mientras la pantalla está a la vista y [activo]. Descarta lecturas viejas o muy imprecisas, y se
 * detiene sola al salir de la pantalla para no gastar batería.
 */
// El permiso se comprueba antes de pedir actualizaciones; cada llamada va dentro de runCatching por si se retira a mitad.
@android.annotation.SuppressLint("MissingPermission")
@Composable
fun rememberUbicacionEnVivo(activo: Boolean): State<Location?> {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val ubicacion = remember { mutableStateOf<Location?>(null) }
    DisposableEffect(lifecycle, activo) {
        val servicio = context.getSystemService(LocationManager::class.java)
        val oyentes = mutableListOf<LocationListener>()
        fun detener() {
            oyentes.forEach { runCatching { servicio.removeUpdates(it) } }
            oyentes.clear()
        }
        fun iniciar() {
            if (!activo || !GestorUbicacionActual.tienePermiso(context) || oyentes.isNotEmpty()) return
            val proveedores = buildList {
                if (GestorUbicacionActual.tienePermisoPreciso(context) &&
                    runCatching { servicio.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
                    add(LocationManager.GPS_PROVIDER)
                }
                if (runCatching { servicio.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
                    add(LocationManager.NETWORK_PROVIDER)
                }
            }
            proveedores.forEach { proveedor ->
                val oyente = LocationListener { nueva ->
                    val edadMs = (SystemClock.elapsedRealtimeNanos() - nueva.elapsedRealtimeNanos) / 1_000_000
                    if (edadMs !in 0..10_000 || !nueva.hasAccuracy() || nueva.accuracy > 100f ||
                        !nueva.latitude.isFinite() || !nueva.longitude.isFinite()) return@LocationListener
                    val anterior = ubicacion.value
                    if (anterior == null || nueva.elapsedRealtimeNanos >= anterior.elapsedRealtimeNanos ||
                        nueva.accuracy < anterior.accuracy / 2f) ubicacion.value = nueva
                }
                runCatching {
                    servicio.requestLocationUpdates(proveedor, 2_000L, 3f, oyente, Looper.getMainLooper())
                    oyentes += oyente
                }
            }
        }
        val observador = LifecycleEventObserver { _, _ ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) iniciar() else detener()
        }
        lifecycle.addObserver(observador)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) iniciar()
        onDispose { lifecycle.removeObserver(observador); detener() }
    }
    return ubicacion
}

/** Vibración corta de aviso; no hace nada si el teléfono no vibra. */
fun vibrarAviso(context: Context) {
    runCatching {
        val vibrador = if (android.os.Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        val patron = longArrayOf(0, 180, 120, 180)
        if (android.os.Build.VERSION.SDK_INT >= 26) vibrador?.vibrate(VibrationEffect.createWaveform(patron, -1))
        else @Suppress("DEPRECATION") vibrador?.vibrate(patron, -1)
    }
}
