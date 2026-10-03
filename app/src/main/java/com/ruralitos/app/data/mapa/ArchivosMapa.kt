package com.ruralitos.app.data.mapa

import android.content.Context
import android.os.StatFs
import java.io.IOException

/** Utilidades comunes a los mapas que se entregan dentro de la aplicación y se copian al almacenamiento privado. */
internal object ArchivosMapa {
    /** Margen libre que debe quedar después de copiar, para que el teléfono no se quede sin espacio. */
    private const val MARGEN_LIBRE = 200L * 1024 * 1024

    /**
     * Tamaño real del archivo empaquetado en la app. Así, al actualizar un mapa, no hay que cambiar ningún número
     * a mano en el código para que la copia se acepte como completa.
     */
    fun longitudAsset(context: Context, nombre: String): Long =
        runCatching { context.assets.openFd(nombre).use { it.length } }
            .getOrElse { context.assets.open(nombre).use { it.available().toLong() } }

    /** Falla antes de empezar a copiar si no cabe, en vez de llenar el almacenamiento a medias. */
    fun verificarEspacio(context: Context, necesario: Long) {
        val libre = StatFs(context.filesDir.path).availableBytes
        if (libre < necesario + MARGEN_LIBRE) {
            throw IOException(
                "Espacio insuficiente para preparar el mapa: hacen falta ${(necesario + MARGEN_LIBRE) / 1_048_576} MB " +
                    "y hay ${libre / 1_048_576} MB libres."
            )
        }
    }
}
