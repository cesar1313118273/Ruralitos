package com.ruralitos.app.data.familiograma

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.withTransaction
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.AdjuntoFichaEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.domain.familiograma.EdadFamiliograma
import com.ruralitos.app.domain.familiograma.Familiograma
import com.ruralitos.app.domain.familiograma.IntegranteFamiliograma
import com.ruralitos.app.domain.familiograma.PngConDatos
import com.ruralitos.app.domain.familiograma.SerializadorFamiliograma
import com.ruralitos.app.ui.familiograma.DibujanteFamiliograma
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Guarda el familiograma dibujado como el mismo adjunto `FAMILIOGRAMA` que ya usan la sincronización,
 * el respaldo, el Excel y el PDF: un PNG transparente. Dentro del PNG va el dibujo en JSON para
 * poder volver a editarlo.
 */
object AlmacenFamiliograma {
    const val TIPO = "FAMILIOGRAMA"

    /** Proporción del recuadro del Excel (hoja 4, columnas AB:CS, filas 3:32). */
    const val ANCHO_PNG = 1830
    const val ALTO_PNG = 1000

    private fun directorio(context: Context) = File(context.filesDir, "familiogramas")

    suspend fun guardar(
        context: Context,
        database: RuralitosDatabase,
        fichaId: Long,
        doc: Familiograma
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = DibujanteFamiliograma().renderizar(doc, ANCHO_PNG, ALTO_PNG)
            val bytes = try {
                val salida = ByteArrayOutputStream()
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida)) { "No se pudo codificar el PNG." }
                PngConDatos.insertar(salida.toByteArray(), SerializadorFamiliograma.aJson(doc))
            } finally {
                bitmap.recycle()
            }
            val dir = directorio(context).apply { mkdirs() }
            val archivo = File(dir, "familiograma_${fichaId}_${System.currentTimeMillis()}.png")
            archivo.writeBytes(bytes)
            val uriNueva = Uri.fromFile(archivo).toString()

            var anterior: String? = null
            database.withTransaction {
                val dao = database.fichaContenidoDao()
                val actual = dao.listarTodosLosAdjuntos()
                    .lastOrNull { it.fichaId == fichaId && it.tipo.equals(TIPO, ignoreCase = true) }
                anterior = actual?.uri
                if (actual == null) {
                    dao.guardarAdjunto(AdjuntoFichaEntity(fichaId = fichaId, tipo = TIPO, uri = uriNueva))
                } else {
                    dao.actualizarUriAdjunto(actual.id, uriNueva)
                }
                database.fichaFamiliarDao().marcarPendiente(fichaId)
            }
            eliminarArchivoPropio(context, anterior, uriNueva)
        }.onFailure { android.util.Log.e("RuralitosFamiliograma", "No se pudo guardar el familiograma", it) }
            .isSuccess
    }

    /** Quita el familiograma (dibujo o foto) de la ficha. */
    suspend fun quitar(context: Context, database: RuralitosDatabase, adjunto: AdjuntoFichaEntity) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
                database.fichaContenidoDao().eliminarAdjunto(adjunto)
                database.fichaFamiliarDao().marcarPendiente(adjunto.fichaId)
            }
            eliminarArchivoPropio(context, adjunto.uri, null)
        }

    private fun eliminarArchivoPropio(context: Context, uriTexto: String?, excepto: String?) {
        if (uriTexto.isNullOrBlank() || uriTexto == excepto) return
        val uri = Uri.parse(uriTexto)
        if (uri.scheme != "file") return
        runCatching {
            val archivo = uri.path?.let(::File)?.canonicalFile ?: return
            if (archivo.parentFile == directorio(context).canonicalFile && archivo.isFile) archivo.delete()
        }
    }

    /** Dibujo guardado dentro del PNG, o `null` si el adjunto es una foto o no se puede leer. */
    suspend fun leerDocumento(context: Context, uriTexto: String): Familiograma? = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(uriTexto)
            val bytes = if (uri.scheme == "file") {
                uri.path?.let { File(it).takeIf(File::isFile)?.readBytes() }
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return@runCatching null
            PngConDatos.leer(bytes)?.let(SerializadorFamiliograma::desdeJson)
        }.getOrNull()
    }
}

/** Datos de un integrante de la ficha en el formato que usa el dibujo. */
fun MiembroFamiliaEntity.aIntegranteFamiliograma(): IntegranteFamiliograma {
    val patologias = buildList {
        if (hipertensionArterial == true) add("Hipertensión arterial")
        if (diabetesMellitus == true) add("Diabetes mellitus")
        if (tuberculosis == true) add("Tuberculosis")
        if (listOf(
                discapacidadVisual, discapacidadAuditiva, discapacidadLenguaje,
                discapacidadFisica, discapacidadIntelectual, discapacidadPsicosocial
            ).any { it == true }
        ) add("Discapacidad")
    }
    return IntegranteFamiliograma(
        nombre = apellidosNombres,
        parentesco = parentesco,
        sexo = sexo,
        edad = EdadFamiliograma.desdeFechaNacimiento(fechaNacimiento).orEmpty(),
        patologias = patologias
    )
}
