package com.ruralitos.app.data.location

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/** Elevación Terrarium de Mapzen/USGS; las teselas se conservan para el trabajo sin red. */
object GestorTerrenoOffline {
    private const val ZOOM = 13
    private const val MAX_MEMORIA = 12
    private val memoria = object : LinkedHashMap<String, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean =
            size > MAX_MEMORIA
    }

    internal data class PuntoTesela(val x: Int, val y: Int, val fraccionX: Double, val fraccionY: Double)

    internal fun puntoTesela(latitud: Double, longitud: Double): PuntoTesela {
        val lat = latitud.coerceIn(-85.0511, 85.0511)
        val escala = (1 shl ZOOM).toDouble()
        val x = ((longitud + 180.0) / 360.0 * escala).coerceIn(0.0, escala - 0.000001)
        val rad = Math.toRadians(lat)
        val y = ((1.0 - ln(tan(rad) + 1.0 / kotlin.math.cos(rad)) / PI) / 2.0 * escala)
            .coerceIn(0.0, escala - 0.000001)
        return PuntoTesela(floor(x).toInt(), floor(y).toInt(), x - floor(x), y - floor(y))
    }

    internal fun decodificarTerrarium(color: Int): Double =
        ((color shr 16) and 0xff) * 256.0 + ((color shr 8) and 0xff) +
            (color and 0xff) / 256.0 - 32768.0

    fun alturaLocal(latitud: Double, longitud: Double): Double? {
        val punto = puntoTesela(latitud, longitud)
        val imagen = synchronized(memoria) { memoria[clave(punto.x, punto.y)] } ?: return null
        val px = (punto.fraccionX * (imagen.width - 1)).coerceIn(0.0, (imagen.width - 1).toDouble())
        val py = (punto.fraccionY * (imagen.height - 1)).coerceIn(0.0, (imagen.height - 1).toDouble())
        val x0 = floor(px).toInt()
        val y0 = floor(py).toInt()
        val x1 = (x0 + 1).coerceAtMost(imagen.width - 1)
        val y1 = (y0 + 1).coerceAtMost(imagen.height - 1)
        val fx = px - x0
        val fy = py - y0
        val superior = decodificarTerrarium(imagen.getPixel(x0, y0)) * (1 - fx) +
            decodificarTerrarium(imagen.getPixel(x1, y0)) * fx
        val inferior = decodificarTerrarium(imagen.getPixel(x0, y1)) * (1 - fx) +
            decodificarTerrarium(imagen.getPixel(x1, y1)) * fx
        return superior * (1 - fy) + inferior * fy
    }

    suspend fun obtener(context: Context, latitud: Double, longitud: Double): Double? {
        val punto = puntoTesela(latitud, longitud)
        cargar(context, punto.x, punto.y, descargar = true)
        return alturaLocal(latitud, longitud)
    }

    suspend fun precargarLocal(context: Context, latitud: Double, longitud: Double): Boolean {
        val punto = puntoTesela(latitud, longitud)
        return cargar(context, punto.x, punto.y, descargar = false)
    }

    suspend fun prepararZona(
        context: Context,
        latitud: Double,
        longitud: Double,
        onProgreso: suspend (Int, Int) -> Unit
    ): Boolean {
        val centro = puntoTesela(latitud, longitud)
        val objetivos = (-1..1).flatMap { dy -> (-1..1).map { dx -> centro.x + dx to centro.y + dy } }
        var completos = 0
        objetivos.forEach { (x, y) ->
            if (cargar(context, x, y, descargar = true)) completos++
            onProgreso(completos, objetivos.size)
        }
        return completos == objetivos.size
    }

    private suspend fun cargar(context: Context, x: Int, y: Int, descargar: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val id = clave(x, y)
            if (synchronized(memoria) { memoria.containsKey(id) }) return@withContext true
            val directorio = File(context.filesDir, "terreno_offline/$ZOOM").apply { mkdirs() }
            val archivo = File(directorio, "$x-$y.png")
            if (!archivo.exists() && descargar) {
                val url = URL(
                    "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/$ZOOM/$x/$y.png"
                )
                val temporal = File(directorio, "$x-$y.tmp")
                runCatching {
                    val conexion = (url.openConnection() as HttpsURLConnection).apply {
                        connectTimeout = 5_000
                        readTimeout = 5_000
                    }
                    try {
                        if (conexion.responseCode != 200) error("Tesela no disponible")
                        conexion.inputStream.use { entrada ->
                            temporal.outputStream().use { salida -> entrada.copyTo(salida) }
                        }
                        if (!temporal.renameTo(archivo)) error("No se pudo guardar la elevación")
                    } finally {
                        conexion.disconnect()
                    }
                }.onFailure { temporal.delete() }
            }
            val imagen = if (archivo.exists()) BitmapFactory.decodeFile(archivo.absolutePath) else null
            if (imagen != null) synchronized(memoria) { memoria[id] = imagen }
            imagen != null
        }

    private fun clave(x: Int, y: Int): String = "$ZOOM/$x/$y"
}
