package com.ruralitos.app.data.agenda

import android.content.Context
import androidx.room.withTransaction
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.domain.DispensarizacionAutomatica
import com.ruralitos.app.domain.GrupoDispensarizacion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/** Plan local de visitas extramurales. Nunca modifica las actividades manuales. */
object PlanificadorSeguimiento {
    private const val ORIGEN = "SEGUIMIENTO"
    private const val TIPO = "Seguimiento extramural"

    data class Resultado(val creadas: Int, val reemplazadas: Int)

    suspend fun prepararFicha(context: Context, fichaId: Long, usuarioId: Long): Resultado =
        withContext(Dispatchers.IO) {
            val db = RuralitosDatabase.obtenerBaseDatos(context)
            val ficha = db.fichaFamiliarDao().buscarPorId(fichaId) ?: return@withContext Resultado(0, 0)
            if (ficha.estado != "COMPLETA") {
                retirarFicha(context, fichaId)
                return@withContext Resultado(0, 0)
            }
            val (resultado, canceladas) = db.withTransaction {
                reconciliar(db, ficha, usuarioId)
            }
            canceladas.forEach { RecordatorioAgenda.cancelar(context, it) }
            db.agendaDao().seguimientosPendientes(fichaId, usuarioId).forEach {
                RecordatorioAgenda.actualizar(context, it)
            }
            resultado
        }

    suspend fun retirarFicha(context: Context, fichaId: Long) = withContext(Dispatchers.IO) {
        val db = RuralitosDatabase.obtenerBaseDatos(context)
        val canceladas = db.withTransaction {
            db.agendaDao().seguimientosPendientesFicha(fichaId).also { pendientes ->
                pendientes.forEach { db.agendaDao().eliminar(it.id) }
            }.map { it.id }
        }
        canceladas.forEach { RecordatorioAgenda.cancelar(context, it) }
    }

    suspend fun prepararTodas(context: Context, organizacionId: String, usuarioId: Long) =
        withContext(Dispatchers.IO) {
            val db = RuralitosDatabase.obtenerBaseDatos(context)
            db.agendaDao().fichasInactivasConSeguimiento(usuarioId).forEach { fichaId ->
                retirarFicha(context, fichaId)
            }
            db.agendaDao().fichasCompletas(organizacionId).forEach { ficha ->
                val (_, canceladas) = db.withTransaction { reconciliar(db, ficha, usuarioId) }
                canceladas.forEach { RecordatorioAgenda.cancelar(context, it) }
                db.agendaDao().seguimientosPendientes(ficha.id, usuarioId).forEach {
                    RecordatorioAgenda.actualizar(context, it)
                }
            }
        }

    /** La fecha real de esta visita es el nuevo punto de partida de TODOS los integrantes. */
    suspend fun registrarVisitaFamiliar(
        context: Context,
        actividadId: Long,
        usuarioId: Long,
        fechaVisita: Long = System.currentTimeMillis()
    ): Resultado = withContext(Dispatchers.IO) {
        val db = RuralitosDatabase.obtenerBaseDatos(context)
        val (resultado, canceladas) = db.withTransaction {
            val agenda = db.agendaDao()
            val realizada = agenda.buscar(actividadId)
                ?: return@withTransaction Resultado(0, 0) to emptyList<Long>()
            if (realizada.usuarioId != usuarioId || realizada.estado == "COMPLETADA") {
                return@withTransaction Resultado(0, 0) to emptyList<Long>()
            }
            val fichaId = realizada.fichaId
                ?: return@withTransaction Resultado(0, 0) to emptyList<Long>()
            val ficha = db.fichaFamiliarDao().buscarPorId(fichaId)
                ?: return@withTransaction Resultado(0, 0) to emptyList<Long>()

            val pendientes = agenda.seguimientosPendientes(fichaId, usuarioId)
            val canceladas = pendientes.map { it.id }.toMutableList()
            pendientes.forEach { if (it.id != actividadId) agenda.eliminar(it.id) }
            // Se conserva la visita efectuada como historial, no como cita futura.
            agenda.actualizar(realizada.copy(estado = "COMPLETADA", origen = ORIGEN, fechaBase = fechaVisita))
            canceladas += actividadId
            val nuevas = crearParaIntegrantes(db, ficha, usuarioId, fechaVisita)
            Resultado(nuevas, pendientes.size - if (pendientes.any { it.id == actividadId }) 1 else 0) to canceladas
        }
        canceladas.distinct().forEach { RecordatorioAgenda.cancelar(context, it) }
        db.agendaDao().buscar(actividadId)?.fichaId?.let { fichaId ->
            db.agendaDao().seguimientosPendientes(fichaId, usuarioId).forEach {
                RecordatorioAgenda.actualizar(context, it)
            }
        }
        resultado
    }

    private suspend fun reconciliar(
        db: RuralitosDatabase,
        ficha: FichaFamiliarEntity,
        usuarioId: Long
    ): Pair<Resultado, List<Long>> {
        val dao = db.agendaDao()
        val pendientes = dao.seguimientosPendientes(ficha.id, usuarioId)
        val porMiembro = pendientes.groupBy { it.miembroId }
        val miembros = dao.miembros(ficha.id)
        val usuarioSyncId = db.usuarioDao().buscarPorId(usuarioId)?.supabaseId.orEmpty()
        val embarazos = dao.embarazos(ficha.id)
        val base = dao.ultimaFechaBase(ficha.id, usuarioId) ?: ficha.actualizadoEn
        val canceladosEnEstaBase = dao.seguimientosCancelados(ficha.id, usuarioId)
            .filter { it.fechaBase == base }.mapNotNull { it.miembroId }.toSet()
        val vigentes = mutableSetOf<Long>()
        val canceladas = mutableListOf<Long>()
        var creadas = 0
        var reemplazadas = 0

        miembros.forEach { miembro ->
            if (miembro.id in canceladosEnEstaBase) return@forEach
            val embarazo = DispensarizacionAutomatica.buscarEmbarazo(miembro, embarazos)
            val grupo = DispensarizacionAutomatica.clasificar(miembro, embarazo).grupo
            if (grupo == GrupoDispensarizacion.PENDIENTE) return@forEach
            val actuales = porMiembro[miembro.id].orEmpty()
            val existente = actuales.firstOrNull()
            if (existente == null) {
                dao.crear(cita(ficha, usuarioId, usuarioSyncId, miembro.id, miembro.syncId, miembro.apellidosNombres,
                    miembro.cedula, grupo, base))
                creadas++
            } else {
                vigentes += existente.id
                if (existente.grupoRiesgo != grupo.codigo || existente.persona != miembro.apellidosNombres ||
                    existente.cedula != miembro.cedula || existente.barrio != ficha.barrio) {
                    dao.actualizar(existente.copy(
                        grupoRiesgo = grupo.codigo,
                        fechaHora = if (existente.fechaEditada) existente.fechaHora else proximaFecha(base, grupo),
                        persona = miembro.apellidosNombres,
                        cedula = miembro.cedula,
                        barrio = ficha.barrio
                    ))
                    if (!existente.fechaEditada) canceladas += existente.id
                    reemplazadas++
                }
            }
        }
        pendientes.filterNot { it.id in vigentes }.forEach {
            dao.eliminar(it.id)
            canceladas += it.id
            reemplazadas++
        }
        return Resultado(creadas, reemplazadas) to canceladas
    }

    private suspend fun crearParaIntegrantes(
        db: RuralitosDatabase,
        ficha: FichaFamiliarEntity,
        usuarioId: Long,
        fechaVisita: Long
    ): Int {
        val dao = db.agendaDao()
        val embarazos = dao.embarazos(ficha.id)
        val usuarioSyncId = db.usuarioDao().buscarPorId(usuarioId)?.supabaseId.orEmpty()
        var creadas = 0
        dao.miembros(ficha.id).forEach { miembro ->
            val embarazo = DispensarizacionAutomatica.buscarEmbarazo(miembro, embarazos)
            val grupo = DispensarizacionAutomatica.clasificar(miembro, embarazo).grupo
            if (grupo != GrupoDispensarizacion.PENDIENTE) {
                dao.crear(cita(ficha, usuarioId, usuarioSyncId, miembro.id, miembro.syncId, miembro.apellidosNombres,
                    miembro.cedula, grupo, fechaVisita))
                creadas++
            }
        }
        return creadas
    }

    private fun cita(
        ficha: FichaFamiliarEntity,
        usuarioId: Long,
        usuarioSyncId: String,
        miembroId: Long,
        miembroSyncId: String,
        persona: String,
        cedula: String,
        grupo: GrupoDispensarizacion,
        base: Long
    ) = ActividadAgendaEntity(
        usuarioId = usuarioId,
        fichaId = ficha.id,
        miembroId = miembroId,
        persona = persona,
        cedula = cedula,
        barrio = ficha.barrio,
        fechaHora = proximaFecha(base, grupo),
        tipo = TIPO,
        nota = "Seguimiento recomendado del grupo ${grupo.codigo}. Confirmar fecha con la familia.",
        origen = ORIGEN,
        grupoRiesgo = grupo.codigo,
        fechaBase = base,
        organizacionId = ficha.organizacionId,
        syncId = if (usuarioSyncId.isNotBlank() && ficha.syncId.isNotBlank() && miembroSyncId.isNotBlank()) {
            java.util.UUID.nameUUIDFromBytes(
                "ruralitos-seguimiento:$usuarioSyncId:${ficha.syncId}:$miembroSyncId:$base"
                    .toByteArray(Charsets.UTF_8)
            ).toString()
        } else java.util.UUID.randomUUID().toString()
    )

    fun proximaFecha(base: Long, grupo: GrupoDispensarizacion): Long =
        Calendar.getInstance().apply {
            timeInMillis = base
            add(Calendar.MONTH, when (grupo) {
                GrupoDispensarizacion.I -> 12
                GrupoDispensarizacion.II -> 6
                GrupoDispensarizacion.III -> 4
                GrupoDispensarizacion.IV -> 3
                GrupoDispensarizacion.PENDIENTE -> 12
            })
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
