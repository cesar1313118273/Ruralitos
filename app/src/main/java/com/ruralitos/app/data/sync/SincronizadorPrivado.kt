package com.ruralitos.app.data.sync

import android.content.Context
import com.ruralitos.app.data.agenda.RecordatorioAgenda
import com.ruralitos.app.data.agenda.RecordatorioNota
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import com.ruralitos.app.data.local.entity.NotaDiariaEntity
import com.ruralitos.app.data.remote.SupabaseApi
import org.json.JSONObject
import java.util.UUID

internal data class ResultadoPrivado(val subidas: Int, val errores: Int, val reintentables: Int)

/** Agenda y notas de una cuenta. Nunca sube las notas históricas sin propietario. */
internal class SincronizadorPrivado(context: Context, private val api: SupabaseApi) {
    private val appContext = context.applicationContext
    private val db = RuralitosDatabase.obtenerBaseDatos(appContext)
    private val agenda = db.agendaDao()
    private val notas = db.notaDiariaDao()

    suspend fun ejecutar(organizacionId: String, soloSubidas: Boolean): ResultadoPrivado {
        val ownerId = api.sesionGuardada()?.usuarioId?.takeIf(String::isNotBlank)
            ?: return ResultadoPrivado(0, 0, 0)
        val usuario = db.usuarioDao().buscarPorSupabaseId(ownerId)
            ?: return ResultadoPrivado(0, 0, 0)
        var subidas = 0
        var errores = 0
        var reintentables = 0

        agenda.pendientesSync(usuario.id).forEach { original ->
            if (original.organizacionId.isNotBlank() && original.organizacionId != organizacionId) return@forEach
            if (original.fichaId?.let { agenda.fichaOrganizacion(it) }?.let { it != organizacionId } == true) {
                return@forEach
            }
            runCatching {
                val item = asegurarIdSeguimiento(original, ownerId)
                val payload = agendaPayload(item)
                val remoto = guardar("agenda_privada", item.syncId, item.syncVersion, ownerId,
                    organizacionId, payload, item.actualizadoEn, item.eliminadoEn)
                if (remoto == null) {
                    if (item.syncVersion == 0L && item.origen == "SEGUIMIENTO" &&
                        !item.fechaEditada && item.estado == "PENDIENTE" && item.eliminadoEn == null
                    ) {
                        agenda.marcarSincronizada(item.id, item.actualizadoEn, 0)
                    } else {
                        agenda.marcarConflicto(item.id, item.actualizadoEn)
                    }
                } else if (agenda.marcarSincronizada(item.id, item.actualizadoEn,
                        remoto.optLong("version", item.syncVersion + 1)) > 0) subidas++
            }.onFailure { error ->
                errores++; EstadoSincronizacion.anotarError("subir agenda", error)
                if (errorSincronizacionReintentable(error)) reintentables++
            }
        }

        notas.pendientesSync(usuario.id).forEach { item ->
            if (notas.miembroOrganizacion(item.miembroId) != organizacionId) return@forEach
            runCatching {
                val remoto = guardar("notas_privadas", item.syncId, item.syncVersion, ownerId,
                    organizacionId, notaPayload(item), item.actualizadoEn, item.eliminadoEn)
                if (remoto == null) notas.marcarConflicto(item.id, item.actualizadoEn)
                else if (notas.marcarSincronizada(item.id, item.actualizadoEn,
                        remoto.optLong("version", item.syncVersion + 1)) > 0) subidas++
            }.onFailure { error ->
                errores++; EstadoSincronizacion.anotarError("subir nota", error)
                if (errorSincronizacionReintentable(error)) reintentables++
            }
        }

        if (!soloSubidas) {
            runCatching { descargarAgenda(ownerId, organizacionId, usuario.id) }
                .onFailure { error -> errores++; EstadoSincronizacion.anotarError("bajar agenda", error); if (errorSincronizacionReintentable(error)) reintentables++ }
            runCatching { descargarNotas(ownerId, organizacionId, usuario.id) }
                .onFailure { error -> errores++; EstadoSincronizacion.anotarError("bajar notas", error); if (errorSincronizacionReintentable(error)) reintentables++ }
        }
        return ResultadoPrivado(subidas, errores, reintentables)
    }

    private suspend fun asegurarIdSeguimiento(item: ActividadAgendaEntity, ownerId: String): ActividadAgendaEntity {
        if (item.syncVersion != 0L || item.origen != "SEGUIMIENTO") return item
        val fichaId = item.fichaId?.let { agenda.fichaSyncId(it) } ?: return item
        val miembroId = item.miembroId?.let { agenda.miembroSyncId(it) } ?: return item
        val syncId = UUID.nameUUIDFromBytes(
            "ruralitos-seguimiento:$ownerId:$fichaId:$miembroId:${item.fechaBase ?: 0L}"
                .toByteArray(Charsets.UTF_8)
        ).toString()
        if (syncId != item.syncId) agenda.fijarSyncId(item.id, syncId)
        return item.copy(syncId = syncId)
    }

    private suspend fun guardar(
        tabla: String, id: String, version: Long, ownerId: String, org: String,
        payload: JSONObject, actualizadoEn: Long, eliminadoEn: Long?
    ): JSONObject? {
        val cambios = JSONObject().put("payload", payload)
            .put("actualizado_en", actualizadoEn)
            .put("eliminado_en", eliminadoEn ?: JSONObject.NULL)
        return if (version == 0L) {
            api.insertarPrivadoNuevo(tabla, cambios.put("id", id)
                .put("owner_id", ownerId).put("organizacion_id", org).put("version", 1))
        } else {
            api.actualizarPrivadoSiVersion(tabla, id, version,
                cambios.put("version", version + 1))
        }
    }

    private suspend fun agendaPayload(item: ActividadAgendaEntity): JSONObject = JSONObject()
        .put("ficha_id", item.fichaId?.let { agenda.fichaSyncId(it) } ?: JSONObject.NULL)
        .put("miembro_id", item.miembroId?.let { agenda.miembroSyncId(it) } ?: JSONObject.NULL)
        .put("persona", item.persona).put("cedula", item.cedula).put("barrio", item.barrio)
        .put("fecha_hora", item.fechaHora).put("tipo", item.tipo).put("nota", item.nota)
        .put("estado", item.estado).put("recordar", item.recordar)
        .put("creado_en", item.creadoEn).put("origen", item.origen)
        .put("grupo_riesgo", item.grupoRiesgo)
        .put("fecha_base", item.fechaBase ?: JSONObject.NULL)
        .put("fecha_editada", item.fechaEditada)

    private suspend fun notaPayload(item: NotaDiariaEntity): JSONObject = JSONObject()
        .put("miembro_id", notas.miembroSyncId(item.miembroId) ?: JSONObject.NULL)
        .put("fecha_local", item.fechaLocal).put("contenido", item.contenido)
        .put("creada_en", item.creadaEn).put("realizada", item.realizada)

    private suspend fun descargarAgenda(ownerId: String, org: String, usuarioId: Long) {
        val filas = api.seleccionarPaginado(
            "agenda_privada?owner_id=eq.$ownerId&organizacion_id=eq.$org&select=*"
        )
        filas.forEach { remoto ->
            val id = remoto.getString("id")
            val version = remoto.getLong("version")
            val previo = agenda.porSyncId(id)
            if (previo?.syncEstado == "PENDIENTE" || previo?.syncEstado == "CONFLICTO" ||
                (previo != null && previo.syncVersion >= version)) return@forEach
            val datos = remoto.getJSONObject("payload")
            val fichaSyncId = datos.optTexto("ficha_id")
            val miembroSyncId = datos.optTexto("miembro_id")
            val fichaId = fichaSyncId.takeIf { it.isNotBlank() }?.let { agenda.fichaLocalId(it) }
            val miembroId = miembroSyncId.takeIf { it.isNotBlank() }?.let { agenda.miembroLocalId(it) }
            if (fichaSyncId.isNotBlank() && fichaId == null) return@forEach
            if (miembroSyncId.isNotBlank() && miembroId == null) return@forEach
            val item = ActividadAgendaEntity(
                id = previo?.id ?: 0,
                usuarioId = usuarioId, fichaId = fichaId, miembroId = miembroId,
                persona = datos.optString("persona"), cedula = datos.optString("cedula"),
                barrio = datos.optString("barrio"), fechaHora = datos.optLong("fecha_hora"),
                tipo = datos.optString("tipo"), nota = datos.optString("nota"),
                estado = datos.optString("estado", "PENDIENTE"),
                recordar = datos.optBoolean("recordar"), creadoEn = datos.optLong("creado_en"),
                origen = datos.optString("origen", "MANUAL"),
                grupoRiesgo = datos.optString("grupo_riesgo"),
                fechaBase = datos.optLongOrNull("fecha_base"),
                fechaEditada = datos.optBoolean("fecha_editada"),
                organizacionId = org,
                syncId = id, syncEstado = "SINCRONIZADO", syncVersion = version,
                actualizadoEn = remoto.optLong("actualizado_en"),
                eliminadoEn = remoto.optLongOrNull("eliminado_en")
            )
            val localId = if (previo == null) agenda.guardarRemota(item)
                else { agenda.actualizarRemota(item); previo.id }
            if (item.eliminadoEn != null) RecordatorioAgenda.cancelar(appContext, localId)
            else RecordatorioAgenda.actualizar(appContext, item.copy(id = localId))
        }
    }

    private suspend fun descargarNotas(ownerId: String, org: String, usuarioId: Long) {
        val filas = api.seleccionarPaginado(
            "notas_privadas?owner_id=eq.$ownerId&organizacion_id=eq.$org&select=*"
        )
        filas.forEach { remoto ->
            val id = remoto.getString("id")
            val version = remoto.getLong("version")
            val previo = notas.porSyncId(id)
            if (previo?.syncEstado == "PENDIENTE" || previo?.syncEstado == "CONFLICTO" ||
                (previo != null && previo.syncVersion >= version)) return@forEach
            val datos = remoto.getJSONObject("payload")
            val miembroSyncId = datos.optTexto("miembro_id")
            val miembroId = notas.miembroLocalId(miembroSyncId) ?: return@forEach
            val item = NotaDiariaEntity(
                id = previo?.id ?: 0, miembroId = miembroId,
                fechaLocal = datos.optString("fecha_local"),
                contenido = datos.optString("contenido"),
                creadaEn = datos.optLong("creada_en"), realizada = datos.optBoolean("realizada"),
                usuarioId = usuarioId, syncId = id, syncEstado = "SINCRONIZADO",
                syncVersion = version, actualizadoEn = remoto.optLong("actualizado_en"),
                eliminadoEn = remoto.optLongOrNull("eliminado_en")
            )
            val localId = if (previo == null) notas.guardarRemota(item)
                else { notas.actualizarRemota(item); previo.id }
            if (item.eliminadoEn != null || item.realizada) RecordatorioNota.cancelar(appContext, localId)
            else RecordatorioNota.actualizar(appContext, item.copy(id = localId))
        }
    }

    private fun JSONObject.optLongOrNull(nombre: String): Long? =
        if (isNull(nombre) || !has(nombre)) null else optLong(nombre)

    private fun JSONObject.optTexto(nombre: String): String =
        if (isNull(nombre) || !has(nombre)) "" else optString(nombre)
}
