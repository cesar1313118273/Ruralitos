package com.ruralitos.app.domain

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Distancias en línea recta entre dos puntos, para mostrar «a cuánto está» una vivienda antes de calcular la ruta. */
object MapaViviendas {
    /** Distancia en línea recta (metros) entre dos puntos. */
    fun distanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = (lat2 - lat1) * PI / 180
        val dLon = (lon2 - lon1) * PI / 180
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** «850 m» o «1,2 km», con coma decimal. */
    fun textoDistancia(metros: Double): String =
        if (metros < 1000) "${(metros / 10).roundToInt() * 10} m"
        else "%.1f km".format(java.util.Locale("es"), metros / 1000.0)

    /** Tiempo a pie en línea recta a 4,5 km/h, redondeado a 5 minutos. */
    fun minutosAPie(metros: Double): Int = (((metros / 1000.0) / 4.5 * 60) / 5).roundToInt().coerceAtLeast(1) * 5
}
