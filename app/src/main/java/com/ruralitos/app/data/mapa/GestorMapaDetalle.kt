package com.ruralitos.app.data.mapa

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** El detalle de Ecuador se entrega con la aplicación y se prepara una sola vez. */
object GestorMapaDetalle {
    const val ARCHIVO = "ecuador_zoom15.pmtiles"

    suspend fun preparar(context: Context): String? = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val destino = File(app.filesDir, ARCHIVO)
        val esperada = runCatching { ArchivosMapa.longitudAsset(app, ARCHIVO) }.getOrDefault(-1L)
        if (esperada <= 0L) return@withContext null
        if (!esArchivoValido(destino, esperada)) {
            val parcial = File(app.filesDir, "$ARCHIVO.parcial")
            runCatching {
                parcial.delete()
                ArchivosMapa.verificarEspacio(app, esperada)
                app.assets.open(ARCHIVO).use { entrada ->
                    parcial.outputStream().buffered().use { salida ->
                        entrada.copyTo(salida, bufferSize = 256 * 1024)
                    }
                }
                check(esArchivoValido(parcial, esperada)) { "El mapa detallado está incompleto" }
                if (destino.exists()) check(destino.delete())
                check(parcial.renameTo(destino)) { "No se pudo activar el mapa detallado" }
            }.onFailure {
                parcial.delete()
                return@withContext null
            }
        }
        "pmtiles://${Uri.fromFile(destino)}"
    }

    private fun esArchivoValido(archivo: File, esperada: Long): Boolean =
        archivo.isFile && archivo.length() == esperada &&
            runCatching {
                archivo.inputStream().use { entrada ->
                    ByteArray(7).also { entrada.read(it) }
                        .contentEquals("PMTiles".toByteArray(Charsets.US_ASCII))
                }
            }.getOrDefault(false)
}
