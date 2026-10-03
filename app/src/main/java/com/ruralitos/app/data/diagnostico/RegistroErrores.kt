package com.ruralitos.app.data.diagnostico

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Guarda en el propio teléfono los cierres inesperados de la app, para que el usuario pueda enviarlos al soporte.
 * Nunca guarda el mensaje de la excepción ni valores de variables (podrían contener nombres o cédulas): solo el tipo
 * de error y la ruta del código donde ocurrió.
 */
object RegistroErrores {
    private const val ARCHIVO = "diagnostico/errores.log"
    private const val MAXIMO_BYTES = 120_000L

    fun instalar(context: Context) {
        val app = context.applicationContext
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { hilo, error ->
            runCatching { registrar(app, hilo.name, error) }
            anterior?.uncaughtException(hilo, error)
        }
    }

    internal fun registrar(context: Context, hilo: String, error: Throwable) {
        val archivo = File(context.filesDir, ARCHIVO).apply { parentFile?.mkdirs() }
        if (archivo.length() > MAXIMO_BYTES) {
            // Se conserva la mitad más reciente.
            val texto = archivo.readText()
            archivo.writeText(texto.takeLast((MAXIMO_BYTES / 2).toInt()))
        }
        archivo.appendText(encabezado(context) + cuerpo(hilo, error))
    }

    private fun encabezado(context: Context): String {
        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
        return "=== $fecha · Ruralitos $version · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · " +
            "${Build.MANUFACTURER} ${Build.MODEL}\n"
    }

    /** Solo tipos de error y rutas del código; nunca el mensaje de la excepción. */
    internal fun cuerpo(hilo: String, error: Throwable): String = buildString {
        appendLine("Hilo: $hilo")
        var actual: Throwable? = error
        var nivel = 0
        while (actual != null && nivel < 4) {
            appendLine(if (nivel == 0) "Error: ${actual.javaClass.name}" else "Causa: ${actual.javaClass.name}")
            actual.stackTrace.take(25).forEach { appendLine("    en $it") }
            actual = actual.cause?.takeIf { it !== actual }
            nivel++
        }
        appendLine()
    }

    fun leer(context: Context): String =
        File(context.filesDir, ARCHIVO).takeIf { it.isFile }?.readText().orEmpty()

    fun hayRegistros(context: Context): Boolean = File(context.filesDir, ARCHIVO).length() > 0

    fun borrar(context: Context) {
        File(context.filesDir, ARCHIVO).delete()
    }
}
