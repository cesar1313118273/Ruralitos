package com.ruralitos.app.data.location

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Obtiene la elevación del terreno del punto marcado. Las respuestas se guardan
 * por cuadrícula para que los ajustes repetidos del pin no requieran red y el
 * mapa siga siendo útil fuera de línea.
 */
object GestorAltitudTerreno {
    private val cache = mutableMapOf<String, Double>()
    private const val PREFERENCIAS = "altitudes_mapa"

    /** Aproximación local inmediata, solo dentro de un radio inferior a la resolución del DEM. */
    fun estimarCercana(latitud: Double, longitud: Double): Double? = synchronized(cache) {
        cache.entries.mapNotNull { (clave, altura) ->
            val partes = clave.split(',')
            val latMuestra = partes.getOrNull(0)?.toDoubleOrNull() ?: return@mapNotNull null
            val lonMuestra = partes.getOrNull(1)?.toDoubleOrNull() ?: return@mapNotNull null
            val distancia = hypot(
                (latitud - latMuestra) * 111_320.0,
                (longitud - lonMuestra) * 111_320.0 * cos(Math.toRadians(latitud))
            )
            if (distancia <= 45.0) distancia to altura else null
        }.minByOrNull { it.first }?.second
    }

    suspend fun precargar(context: Context) = withContext(Dispatchers.IO) {
        val guardadas = context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE).all
        synchronized(cache) {
            guardadas.forEach { (clave, valor) ->
                (valor as? Number)?.let { cache[clave] = it.toDouble() }
            }
        }
    }

    fun recordar(context: Context, latitud: Double, longitud: Double, altitud: Double) {
        val clave = "%.5f,%.5f".format(Locale.US, latitud, longitud)
        synchronized(cache) { cache[clave] = altitud }
        context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
            .edit().putFloat(clave, altitud.toFloat()).apply()
    }

    suspend fun obtener(context: Context, latitud: Double, longitud: Double): Double? =
        withContext(Dispatchers.IO) {
            val clave = "%.5f,%.5f".format(Locale.US, latitud, longitud)
            GestorTerrenoOffline.obtener(context, latitud, longitud)?.let { elevacion ->
                recordar(context, latitud, longitud, elevacion)
                return@withContext elevacion
            }
            val memoria = synchronized(cache) { cache[clave] }
            if (memoria != null) return@withContext memoria
            val preferencias = context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
            if (preferencias.contains(clave)) {
                return@withContext preferencias.getFloat(clave, 0f).toDouble().also { elevacion ->
                    synchronized(cache) { cache[clave] = elevacion }
                }
            }
            null
        }
}
