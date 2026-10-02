package com.ruralitos.app.data.mapa

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Prepara una copia privada del mapa vectorial para que SQLite lo lea sin red. */
object GestorMapaCampo {
    private const val ASSET = "ecuador_base.mbtiles"
    private const val LONGITUD_ESPERADA = 123_170_816L

    suspend fun preparar(context: Context): String? = withContext(Dispatchers.IO) {
        val destino = File(context.applicationContext.filesDir, ASSET)
        if (!esCopiaValida(destino)) {
            val parcial = File(context.applicationContext.filesDir, "$ASSET.parcial")
            runCatching {
                parcial.delete()
                context.applicationContext.assets.open(ASSET).use { entrada ->
                    parcial.outputStream().buffered().use { salida ->
                        entrada.copyTo(salida, bufferSize = 256 * 1024)
                    }
                }
                check(esCopiaValida(parcial)) { "La copia del mapa local quedó incompleta" }
                if (destino.exists()) check(destino.delete())
                check(parcial.renameTo(destino)) { "No se pudo activar el mapa local" }
            }.onFailure {
                parcial.delete()
                return@withContext null
            }
        }
        "mbtiles://${destino.absolutePath}"
    }

    fun estilo(context: Context, urlMapa: String, urlDetalle: String? = null): String {
        val base = context.applicationContext.assets.open("mapa_campo.json")
            .bufferedReader()
            .use { it.readText() }
            .replace("__MAPA_LOCAL__", urlMapa)
        if (urlDetalle == null) return base

        val estilo = JSONObject(base)
        estilo.getJSONObject("sources").put(
            "ecuador_detalle",
            JSONObject()
                .put("type", "vector")
                .put("url", urlDetalle)
                .put("attribution", "© OpenStreetMap contributors · Protomaps")
        )
        val originales = estilo.getJSONArray("layers")
        val capas = JSONArray()
        for (indice in 0 until originales.length()) {
            val original = originales.getJSONObject(indice)
            if (original.optString("source") != "ecuador") {
                capas.put(original)
                continue
            }
            val minimo = original.optDouble("minzoom", 0.0)
            if (minimo < 15.0) {
                capas.put(JSONObject(original.toString()).put("maxzoom", 15))
            }
            capas.put(
                JSONObject(original.toString())
                    .put("id", "${original.getString("id")}-z15")
                    .put("source", "ecuador_detalle")
                    .put("minzoom", maxOf(15.0, minimo))
            )
        }
        estilo.put("layers", capas)
        return estilo.toString()
    }

    private fun esCopiaValida(archivo: File): Boolean =
        archivo.isFile && archivo.length() == LONGITUD_ESPERADA &&
            archivo.inputStream().use { entrada ->
                ByteArray(16).also { entrada.read(it) }
                    .contentEquals("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))
            }
}
