package com.ruralitos.app.data.mapa

import kotlin.math.abs

/** La geometría de una ruta solo pertenece a estos dos puntos exactos. */
data class ExtremosRuta(
    val inicioLatitud: Double,
    val inicioLongitud: Double,
    val destinoLatitud: Double,
    val destinoLongitud: Double
) {
    fun coincide(
        inicioLatitudActual: Double?,
        inicioLongitudActual: Double?,
        destinoLatitudActual: Double?,
        destinoLongitudActual: Double?
    ): Boolean = inicioLatitudActual != null && inicioLongitudActual != null &&
        destinoLatitudActual != null && destinoLongitudActual != null &&
        abs(inicioLatitud - inicioLatitudActual) < 0.0000001 &&
        abs(inicioLongitud - inicioLongitudActual) < 0.0000001 &&
        abs(destinoLatitud - destinoLatitudActual) < 0.0000001 &&
        abs(destinoLongitud - destinoLongitudActual) < 0.0000001
}
