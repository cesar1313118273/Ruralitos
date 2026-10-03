package com.ruralitos.app.data.export

import android.content.Context
import android.net.Uri
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Permite que cada centro use su propia plantilla de Excel para la ficha familiar, en lugar de la que trae la app.
 * La plantilla propia debe conservar las cuatro hojas («1» a «4») y las mismas casillas, porque la app escribe los
 * datos por posición. Se valida al importarla y, si deja de abrirse, se vuelve a la de la app.
 */
object PlantillaPropia {
    private fun archivo(context: Context) = File(context.filesDir, "plantillas/ficha_familiar.xlsx")

    fun existe(context: Context): Boolean = archivo(context).isFile

    /** Bytes de la plantilla propia si existe y es válida; si no, nulo. */
    internal fun bytes(context: Context): ByteArray? = archivo(context).takeIf { it.isFile }
        ?.readBytes()?.takeIf { runCatching { validar(it) }.isSuccess }

    @Throws(IllegalArgumentException::class)
    internal fun validar(bytes: ByteArray) {
        val libro = try {
            XSSFWorkbook(ByteArrayInputStream(FichaExcelExporter.sanitizarPlantillaXlsx(bytes)))
        } catch (error: Exception) {
            throw IllegalArgumentException("El archivo no es un Excel (.xlsx) válido.")
        }
        libro.use {
            val faltantes = listOf("1", "2", "3", "4").filter { nombre -> it.getSheet(nombre) == null }
            require(faltantes.isEmpty()) {
                "La plantilla debe tener las hojas «1», «2», «3» y «4» como la original. Faltan: ${faltantes.joinToString()}."
            }
        }
    }

    /** Importa y valida un archivo elegido por el usuario. Lanza un mensaje claro si no sirve. */
    fun importar(context: Context, origen: Uri) {
        val bytes = context.contentResolver.openInputStream(origen)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("No se pudo leer el archivo.")
        require(bytes.size <= 15L * 1024 * 1024) { "El archivo es demasiado grande (máximo 15 MB)." }
        validar(bytes)
        archivo(context).apply { parentFile?.mkdirs() }.writeBytes(bytes)
    }

    fun quitar(context: Context) {
        archivo(context).delete()
    }
}
