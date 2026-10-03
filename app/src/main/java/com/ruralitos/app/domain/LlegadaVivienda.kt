package com.ruralitos.app.domain

/** Cuándo se considera que se llegó a una vivienda: cerca de ella, descontando lo que se equivoca el GPS. */
object LlegadaVivienda {
    const val DISTANCIA_MINIMA_M = 40.0
    const val DISTANCIA_MAXIMA_M = 100.0

    /** Radio de llegada: 40 m, o la precisión del GPS si es peor, sin pasar de 100 m para no avisar de lejos. */
    fun radio(precisionMetros: Float?): Double =
        (precisionMetros?.toDouble() ?: 0.0).coerceIn(DISTANCIA_MINIMA_M, DISTANCIA_MAXIMA_M)

    fun haLlegado(distanciaMetros: Double, precisionMetros: Float?): Boolean =
        distanciaMetros.isFinite() && distanciaMetros <= radio(precisionMetros)
}
