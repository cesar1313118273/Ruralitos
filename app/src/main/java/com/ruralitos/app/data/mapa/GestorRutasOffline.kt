package com.ruralitos.app.data.mapa

import android.content.Context
import com.valhalla.valhalla.Valhalla
import com.valhalla.valhalla.config.ValhallaConfigFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng

data class RutaCalculada(
    val puntos: List<LatLng>,
    val kilometros: Double,
    val minutos: Int,
    val instrucciones: List<String>,
    val modo: String
)

/** Red de OSM procesada en el dispositivo: nunca envía las ubicaciones del paciente. */
object GestorRutasOffline {
    private const val ARCHIVO = "ecuador_valhalla_tiles.tar"
    private const val LONGITUD_ESPERADA = 224_225_280L
    private val mutex = Mutex()
    private var motor: Valhalla? = null

    suspend fun calcular(
        context: Context,
        inicio: LatLng,
        destino: LatLng,
        modo: String = "auto"
    ): Result<RutaCalculada> = withContext(Dispatchers.IO) {
        runCatching {
            require(modo in setOf("auto", "pedestrian", "bicycle"))
            val app = context.applicationContext
            mutex.withLock {
                val valhalla = motor ?: run {
                    val archivo = prepararDatos(app)
                    val configuracion = ValhallaConfigFactory.usingTileExtract(archivo.absolutePath)
                    Valhalla(app, configuracion).also { motor = it }
                }
                val solicitud = JSONObject()
                    .put("locations", JSONArray()
                        .put(JSONObject().put("lat", inicio.latitude).put("lon", inicio.longitude))
                        .put(JSONObject().put("lat", destino.latitude).put("lon", destino.longitude)))
                    .put("costing", modo)
                    .put("language", "es-ES")
                    .put("units", "kilometers")
                val viaje = JSONObject(valhalla.routeRaw(solicitud.toString())).getJSONObject("trip")
                val tramos = viaje.getJSONArray("legs")
                val puntos = buildList {
                    repeat(tramos.length()) { indice ->
                        addAll(decodificarPolilinea6(tramos.getJSONObject(indice).getString("shape")))
                    }
                }
                check(puntos.size >= 2) { "El motor no devolvió un recorrido" }
                val resumen = viaje.getJSONObject("summary")
                val instrucciones = buildList {
                    repeat(tramos.length()) { tramo ->
                        val maniobras = tramos.getJSONObject(tramo).optJSONArray("maneuvers")
                        if (maniobras != null) repeat(maniobras.length()) { indice ->
                            maniobras.getJSONObject(indice).optString("instruction")
                                .takeIf(String::isNotBlank)?.let(::add)
                        }
                    }
                }
                RutaCalculada(
                    puntos = puntos,
                    kilometros = resumen.getDouble("length"),
                    minutos = kotlin.math.ceil(resumen.getDouble("time") / 60.0).toInt(),
                    instrucciones = instrucciones,
                    modo = modo
                )
            }
        }
    }

    private fun prepararDatos(context: Context): File {
        val destino = File(context.filesDir, ARCHIVO)
        if (destino.isFile && destino.length() == LONGITUD_ESPERADA) return destino
        val parcial = File(context.filesDir, "$ARCHIVO.parcial")
        parcial.delete()
        try {
            context.assets.open(ARCHIVO).use { entrada ->
                parcial.outputStream().buffered().use { salida ->
                    entrada.copyTo(salida, bufferSize = 256 * 1024)
                }
            }
            check(parcial.length() == LONGITUD_ESPERADA) { "La red vial quedó incompleta" }
            if (destino.exists()) check(destino.delete())
            check(parcial.renameTo(destino)) { "No se pudo activar la red vial" }
        } finally {
            parcial.delete()
        }
        return destino
    }

    /** Valhalla codifica latitud/longitud a seis decimales. */
    internal fun decodificarPolilinea6(valor: String): List<LatLng> {
        var indice = 0
        var latitud = 0L
        var longitud = 0L
        fun siguiente(): Long {
            var resultado = 0L
            var desplazamiento = 0
            while (true) {
                require(indice < valor.length) { "Geometría de ruta incompleta" }
                val codigo = valor[indice++].code - 63
                resultado = resultado or ((codigo and 0x1F).toLong() shl desplazamiento)
                if (codigo < 0x20) break
                desplazamiento += 5
                require(desplazamiento <= 60) { "Geometría de ruta inválida" }
            }
            return if ((resultado and 1L) != 0L) (resultado shr 1).inv() else resultado shr 1
        }
        return buildList {
            while (indice < valor.length) {
                latitud += siguiente()
                longitud += siguiente()
                add(LatLng(latitud / 1_000_000.0, longitud / 1_000_000.0))
            }
        }
    }
}
