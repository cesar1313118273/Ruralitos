package com.ruralitos.app.data.export

import android.annotation.TargetApi
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ruralitos.app.domain.AgrupacionRegistroComunitario
import java.io.File

data class ArchivoFichaDescargado(
    val uri: Uri,
    val nombre: String,
    val mimeType: String
)

data class ResultadoDescargaFicha(
    val archivos: List<ArchivoFichaDescargado>,
    val errores: List<String>
)

object GestorDescargasFicha {
    private const val CARPETA_RURALITOS = "Ruralitos"
    private const val MIME_PDF = "application/pdf"
    private const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    suspend fun generar(
        context: Context,
        fichaId: Long,
        nombreBase: String,
        descargarPdf: Boolean,
        descargarExcel: Boolean
    ): ResultadoDescargaFicha = withContext(Dispatchers.IO) {
        val archivos = mutableListOf<ArchivoFichaDescargado>()
        val errores = mutableListOf<String>()
        val baseSeguro = normalizarNombre(nombreBase)

        if (descargarPdf) {
            runCatching {
                crearArchivo(
                    context = context,
                    nombre = "$baseSeguro.pdf",
                    mimeType = MIME_PDF
                ) { uri ->
                    FichaPdfPlantillaExporter.exportar(context, fichaId, uri)
                }
            }.onSuccess(archivos::add)
                .onFailure { errores += "PDF: ${it.message ?: "no se pudo crear"}" }
        }

        if (descargarExcel) {
            runCatching {
                crearArchivo(
                    context = context,
                    nombre = "$baseSeguro.xlsx",
                    mimeType = MIME_XLSX
                ) { uri ->
                    FichaExcelExporter.exportar(context, fichaId, uri)
                }
            }.onSuccess(archivos::add)
                .onFailure { errores += "Excel: ${it.message ?: "no se pudo crear"}" }
        }

        ResultadoDescargaFicha(archivos = archivos, errores = errores)
    }

    suspend fun generarRegistroGeneral(
        context: Context,
        filtro: FiltroConsolidado,
        agrupaciones: List<AgrupacionRegistroComunitario>
    ): ResultadoDescargaFicha = withContext(Dispatchers.IO) {
        val base = normalizarNombre(
            "registro_general_${filtro.etiqueta.ifBlank { "ruralitos" }}"
        )
        runCatching {
            crearArchivo(
                context = context,
                nombre = "$base.xlsx",
                mimeType = MIME_XLSX
            ) { uri ->
                RegistroGeneralExcelExporter.exportar(context, filtro, agrupaciones, uri)
            }
        }.fold(
            onSuccess = { ResultadoDescargaFicha(listOf(it), emptyList()) },
            onFailure = { ResultadoDescargaFicha(emptyList(), listOf(it.message ?: "No se pudo crear el registro general.")) }
        )
    }

    fun compartir(context: Context, archivos: List<ArchivoFichaDescargado>): Boolean {
        if (archivos.isEmpty()) return false
        val uris = ArrayList(archivos.map { it.uri })
        val intent = if (archivos.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = archivos.first().mimeType
                putExtra(Intent.EXTRA_STREAM, archivos.first().uri)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData = ClipData.newUri(
            context.contentResolver,
            "Archivo Ruralitos",
            archivos.first().uri
        ).apply {
            archivos.drop(1).forEach { addItem(ClipData.Item(it.uri)) }
        }

        return runCatching {
            context.startActivity(Intent.createChooser(intent, "Compartir archivo Ruralitos"))
        }.isSuccess
    }

    private suspend fun crearArchivo(
        context: Context,
        nombre: String,
        mimeType: String,
        exportar: suspend (Uri) -> Unit
    ): ArchivoFichaDescargado {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            crearEnDescargas(context, nombre, mimeType, exportar)
        } else {
            crearEnDirectorioAplicacion(context, nombre, mimeType, exportar)
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private suspend fun crearEnDescargas(
        context: Context,
        nombre: String,
        mimeType: String,
        exportar: suspend (Uri) -> Unit
    ): ArchivoFichaDescargado {
        val resolver = context.contentResolver
        val valores = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + File.separator + CARPETA_RURALITOS
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
            ?: error("Android no permitió crear el archivo en Descargas.")

        try {
            exportar(uri)
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null
            )
            return ArchivoFichaDescargado(uri, nombre, mimeType)
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private suspend fun crearEnDirectorioAplicacion(
        context: Context,
        nombre: String,
        mimeType: String,
        exportar: suspend (Uri) -> Unit
    ): ArchivoFichaDescargado {
        val raiz = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val carpeta = File(raiz, CARPETA_RURALITOS).apply { mkdirs() }
        val archivo = archivoDisponible(carpeta, nombre)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )
        try {
            exportar(uri)
            return ArchivoFichaDescargado(uri, archivo.name, mimeType)
        } catch (error: Throwable) {
            archivo.delete()
            throw error
        }
    }

    private fun archivoDisponible(carpeta: File, nombre: String): File {
        val directo = File(carpeta, nombre)
        if (!directo.exists()) return directo

        val punto = nombre.lastIndexOf('.')
        val base = if (punto > 0) nombre.substring(0, punto) else nombre
        val extension = if (punto > 0) nombre.substring(punto + 1) else ""
        var indice = 2
        while (true) {
            val candidato = File(
                carpeta,
                if (extension.isBlank()) "$base ($indice)" else "$base ($indice).$extension"
            )
            if (!candidato.exists()) return candidato
            indice++
        }
    }

    internal fun normalizarNombre(nombre: String): String =
        nombre
            .trim()
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifBlank { "ficha_familiar" }
            .take(90)
}

