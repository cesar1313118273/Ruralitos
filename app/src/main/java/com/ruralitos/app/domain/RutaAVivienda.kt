package com.ruralitos.app.domain

import java.util.Locale
import kotlin.math.abs

/** Para el botón «Cómo llegar»: decide si una vivienda tiene ubicación real y arma los enlaces de navegación. */
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

    private fun coordenadas(latitud: Double, longitud: Double) =
        String.format(Locale.US, "%.6f,%.6f", latitud, longitud)

    /** Abre la navegación paso a paso (Google Maps y similares). */
    fun enlaceNavegacion(latitud: Double, longitud: Double): String =
        "google.navigation:q=" + coordenadas(latitud, longitud)

    /** Plan B: cualquier aplicación de mapas que entienda `geo:`; marca el punto con el nombre de la familia. */
    fun enlaceMapa(latitud: Double, longitud: Double, nombre: String): String {
        val c = coordenadas(latitud, longitud)
        val etiqueta = nombre.trim().replace("(", "").replace(")", "")
        return if (etiqueta.isBlank()) "geo:$c?q=$c" else "geo:$c?q=$c(" + java.net.URLEncoder.encode(etiqueta, "UTF-8").replace("+", "%20") + ")"
    }
}
