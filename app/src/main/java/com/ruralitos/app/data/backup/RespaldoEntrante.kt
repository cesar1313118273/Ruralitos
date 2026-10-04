package com.ruralitos.app.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ruralitos.app.ui.components.AvisosRuralitos
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Un respaldo que llegó desde otra aplicación (WhatsApp, correo, el administrador de archivos…) al tocarlo con
 * «Abrir con Ruralitos» o al elegir Ruralitos en «Compartir». Se copia a la carpeta temporal de la app porque el permiso
 * que da Android sobre el archivo original dura poco; la pantalla de importar lo recoge de aquí.
 */
object RespaldoEntrante {
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val CARPETA = "respaldos_entrantes"
    private const val MAXIMO_BYTES = 1024L * 1024L * 1024L

    /** Archivo ya copiado y revisado, listo para importar; `null` si no hay ninguno esperando. */
    var pendiente by mutableStateOf<Uri?>(null)
        private set

    /** Nombre con el que llegó el archivo, para mostrarlo. */
    var nombre by mutableStateOf("")
        private set

    /** ¿Esta intención trae un archivo de respaldo (y no, por ejemplo, un enlace de acceso de Ruralitos)? */
    fun traeArchivo(intent: Intent?): Boolean = origen(intent) != null

    private fun origen(intent: Intent?): Uri? {
        intent ?: return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.takeIf { it.scheme == "content" || it.scheme == "file" }
            Intent.ACTION_SEND -> @Suppress("DEPRECATION") (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
                ?.takeIf { it.scheme == "content" || it.scheme == "file" }
            else -> null
        }
    }

    /** Copia y revisa el archivo en segundo plano. Devuelve `false` si la intención no traía ninguno. */
    fun recibir(context: Context, intent: Intent?): Boolean {
        val uri = origen(intent) ?: return false
        val app = context.applicationContext
        alcance.launch {
            runCatching { copiarYRevisar(app, uri) }
                .onSuccess { (archivo, nombreOriginal) ->
                    nombre = nombreOriginal
                    pendiente = Uri.fromFile(archivo)
                }
                .onFailure {
                    AvisosRuralitos.mostrar(
                        it.message?.takeIf { mensaje -> mensaje.startsWith("No ") || mensaje.startsWith("El ") }
                            ?: "No se pudo abrir el archivo recibido."
                    )
                }
        }
        return true
    }

    /** El usuario no quiere importarlo ahora o ya terminó: se olvida y se borra la copia temporal. */
    fun descartar(context: Context) {
        descartarArchivo(context.applicationContext)
        pendiente = null
        nombre = ""
    }

    private fun carpeta(context: Context) = File(context.cacheDir, CARPETA)

    private fun descartarArchivo(context: Context) {
        runCatching { carpeta(context).deleteRecursively() }
    }

    private fun copiarYRevisar(context: Context, uri: Uri): Pair<File, String> {
        val resolver = context.contentResolver
        val nombreOriginal = nombreDe(context, uri)
        val abierto = { if (uri.scheme == "file") uri.path?.let(::FileInputStream) else resolver.openInputStream(uri) }
        val esRespaldo = abierto()?.use(CifradoRespaldo::pareceRespaldo)
            ?: error("No se pudo abrir el archivo recibido.")
        if (!esRespaldo) error("El archivo recibido no es un respaldo de Ruralitos.")
        // Un respaldo nuevo reemplaza al que estuviera esperando.
        descartarArchivo(context)
        val destino = File(carpeta(context).apply { mkdirs() }, "respaldo_${System.currentTimeMillis()}.ruralitos")
        var total = 0L
        try {
            abierto()?.use { entrada ->
                destino.outputStream().use { salida ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val leidos = entrada.read(buffer)
                        if (leidos < 0) break
                        total += leidos
                        if (total > MAXIMO_BYTES) error("El respaldo recibido es demasiado grande.")
                        salida.write(buffer, 0, leidos)
                    }
                }
            } ?: error("No se pudo abrir el archivo recibido.")
        } catch (error: Exception) {
            destino.delete()
            throw error
        }
        return destino to nombreOriginal
    }

    private fun nombreDe(context: Context, uri: Uri): String {
        if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "respaldo.ruralitos"
    }
}
