package com.ruralitos.app.domain

import kotlin.math.abs

/**
 * Para el botón «Cómo llegar»: decide si una vivienda tiene ubicación real. La ruta se traza dentro de Ruralitos, en
 * el mapa de la Agenda (con los mapas sin conexión de la app); no se usa ninguna aplicación de mapas externa.
 */
object RutaAVivienda {
    // Punto de ejemplo que usaban las versiones antiguas cuando la vivienda no tenía GPS.
    private const val EJEMPLO_LAT = -1.8312
    private const val EJEMPLO_LNG = -78.1834

    fun tieneUbicacion(latitud: Double?, longitud: Double?): Boolean {
        if (latitud == null || longitud == null) return false
        if (!latitud.isFinite() || !longitud.isFinite()) return false
        if (latitud !in -90.0..90.0 || longitud !in -180.0..180.0) return false
        if (abs(latitud - EJEMPLO_LAT) < 0.000001 && abs(longitud - EJEMPLO_LNG) < 0.000001) return false
        return true
    }
}
