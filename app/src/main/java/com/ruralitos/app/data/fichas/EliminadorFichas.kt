package com.ruralitos.app.data.fichas

import android.content.Context
import androidx.room.withTransaction
import com.ruralitos.app.data.agenda.RecordatorioAgenda
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.EliminacionSyncEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Elimina fichas de forma definitiva: la ficha y todo lo suyo (integrantes, embarazos, salud, riesgos, croquis, fotos y
 * firma), sus visitas de la agenda y las notas de la cuenta sobre sus integrantes. Lo que ya estaba en la nube queda
 * anotado para borrarse en la siguiente sincronización. La cuenta, la Sala y los barrios no se tocan.
 */
object EliminadorFichas {
    private class Eliminada(val idsAgenda: List<Long>, val archivos: List<String>)

    /** Elimina una ficha. El historial de la ficha se conserva como constancia de que fue eliminada. */
    suspend fun eliminarFicha(context: Context, database: RuralitosDatabase, ficha: FichaFamiliarEntity, usuarioId: Long) {
        withContext(Dispatchers.IO) {
            val eliminada = eliminarRegistros(database, ficha, usuarioId, borrarHistorial = false)
            limpiar(context, listOf(eliminada))
        }
    }

    /**
     * Elimina las fichas que creó esta persona, con su historial. Las que otras personas le compartieron no se tocan:
     * ni en el teléfono ni en la nube. Devuelve cuántas eran.
     */
    suspend fun eliminarTodas(context: Context, database: RuralitosDatabase, usuarioId: Long): Int =
        withContext(Dispatchers.IO) {
            val fichas = database.fichaFamiliarDao().todas().filterNot { com.ruralitos.app.domain.EtiquetasFicha.esRecibida(it) }
            val eliminadas = fichas.map { eliminarRegistros(database, it, usuarioId, borrarHistorial = true) }
            limpiar(context, eliminadas)
            fichas.size
        }

    private suspend fun eliminarRegistros(
        database: RuralitosDatabase,
        ficha: FichaFamiliarEntity,
        usuarioId: Long,
        borrarHistorial: Boolean
    ): Eliminada {
        val idsAgenda = database.agendaDao().idsDeFicha(ficha.id)
        val archivos = database.withTransaction {
            val syncDao = database.sincronizacionDao()
            val adjuntos = syncDao.adjuntos(ficha.id)
            val calificaciones = syncDao.calificaciones(ficha.id)
            val registrosRemotos = mutableListOf<Pair<String, String>>()
            registrosRemotos += "fichas_familiares" to ficha.syncId
            registrosRemotos += syncDao.miembros(ficha.id).map { "miembros_familia" to it.syncId }
            registrosRemotos += syncDao.embarazadas(ficha.id).map { "embarazadas" to it.syncId }
            registrosRemotos += syncDao.mortalidad(ficha.id).map { "mortalidad_familiar" to it.syncId }
            registrosRemotos += calificaciones.map { "calificaciones_riesgo" to it.syncId }
            calificaciones.forEach { calificacion ->
                registrosRemotos += syncDao.valores(calificacion.id).map { "valores_riesgo" to it.syncId }
            }
            registrosRemotos += syncDao.gestiones(ficha.id).map { "gestion_riesgo" to it.syncId }
            registrosRemotos += syncDao.contaminaciones(ficha.id).map { "contaminacion_ambiental" to it.syncId }
            registrosRemotos += syncDao.lugares(ficha.id).map { "lugares_tratamiento" to it.syncId }
            registrosRemotos += adjuntos.map { "adjuntos_ficha" to it.syncId }
            // Las notas de esta cuenta sobre los integrantes se borran con ellos; las de otras cuentas no son nuestras.
            registrosRemotos += database.notaDiariaDao().syncIdsDeFicha(ficha.id, usuarioId).map { "notas_privadas" to it }

            // Una ficha que otra persona me compartió solo se quita de este teléfono: eliminarla de la nube es de su autora.
            if (ficha.organizacionId.isNotBlank() && !com.ruralitos.app.domain.EtiquetasFicha.esRecibida(ficha)) {
                val ahora = System.currentTimeMillis()
                syncDao.guardarEliminaciones(
                    registrosRemotos
                        .filter { (_, syncId) -> syncId.isNotBlank() }
                        .distinct()
                        .map { (tabla, syncId) ->
                            EliminacionSyncEntity(
                                tabla = tabla,
                                registroSyncId = syncId,
                                organizacionId = ficha.organizacionId,
                                creadoEn = ahora
                            )
                        }
                )
            }
            database.agendaDao().eliminarDeFicha(ficha.id)
            database.fichaFamiliarDao().eliminarFicha(ficha)
            if (borrarHistorial) database.historialFichaDao().eliminarDeFicha(ficha.id)
            adjuntos.map { it.uri } + listOfNotNull(ficha.firmaUri)
        }
        return Eliminada(idsAgenda, archivos)
    }

    /** Cancela los avisos de las visitas y borra del teléfono las fotos y firmas, solo si están en la carpeta de la app. */
    private fun limpiar(context: Context, eliminadas: List<Eliminada>) {
        eliminadas.forEach { e -> e.idsAgenda.forEach { RecordatorioAgenda.cancelar(context, it) } }
        val raiz = context.filesDir.canonicalFile
        eliminadas.flatMap { it.archivos }.forEach { uriTexto ->
            val uri = android.net.Uri.parse(uriTexto)
            val archivo = uri.path?.let(::File)
            if (uri.scheme == "file" && archivo != null) {
                val seguro = archivo.canonicalFile
                if (seguro.path.startsWith(raiz.path + File.separator)) seguro.delete()
            }
        }
    }
}
