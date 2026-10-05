package com.ruralitos.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import com.ruralitos.app.data.local.entity.PersonaAgenda
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgendaDao {
    @Query("SELECT * FROM actividades_agenda WHERE usuarioId = :usuarioId AND eliminadoEn IS NULL ORDER BY fechaHora, id")
    fun observar(usuarioId: Long): Flow<List<ActividadAgendaEntity>>

    @Query("SELECT * FROM actividades_agenda WHERE id = :id LIMIT 1")
    suspend fun buscar(id: Long): ActividadAgendaEntity?

    @Insert suspend fun crear(item: ActividadAgendaEntity): Long
    @Update suspend fun actualizarLocal(item: ActividadAgendaEntity)
    suspend fun actualizar(item: ActividadAgendaEntity) {
        actualizarLocal(item.copy(syncEstado = "PENDIENTE", actualizadoEn = System.currentTimeMillis()))
    }
    @Query("UPDATE actividades_agenda SET eliminadoEn = :ahora, syncEstado = 'PENDIENTE', actualizadoEn = :ahora, estado = 'CANCELADA' WHERE id = :id AND eliminadoEn IS NULL")
    suspend fun eliminarConFecha(id: Long, ahora: Long)
    suspend fun eliminar(id: Long) = eliminarConFecha(id, System.currentTimeMillis())

    @Query("SELECT * FROM actividades_agenda WHERE usuarioId = :usuarioId AND syncEstado = 'PENDIENTE' ORDER BY actualizadoEn")
    suspend fun pendientesSync(usuarioId: Long): List<ActividadAgendaEntity>
    @Query("SELECT COUNT(*) FROM actividades_agenda WHERE usuarioId = :usuarioId AND syncEstado = 'PENDIENTE'")
    fun observarPendientesSync(usuarioId: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM actividades_agenda WHERE usuarioId = :usuarioId AND syncEstado = 'CONFLICTO'")
    fun observarConflictosSync(usuarioId: Long): Flow<Int>
    @Query("SELECT COALESCE(MAX(actualizadoEn), 0) FROM actividades_agenda WHERE usuarioId = :usuarioId AND syncEstado = 'PENDIENTE'")
    fun observarUltimoCambioPendiente(usuarioId: Long): Flow<Long>
    @Query("UPDATE actividades_agenda SET syncEstado = 'SINCRONIZADO', syncVersion = :version WHERE id = :id AND actualizadoEn = :esperado")
    suspend fun marcarSincronizada(id: Long, esperado: Long, version: Long): Int
    @Query("UPDATE actividades_agenda SET syncEstado = 'CONFLICTO' WHERE id = :id AND actualizadoEn = :esperado")
    suspend fun marcarConflicto(id: Long, esperado: Long): Int
    @Query("UPDATE actividades_agenda SET syncId = :syncId WHERE id = :id AND syncVersion = 0")
    suspend fun fijarSyncId(id: Long, syncId: String): Int
    @Query("SELECT syncId FROM fichas_familiares WHERE id = :id LIMIT 1")
    suspend fun fichaSyncId(id: Long): String?
    @Query("SELECT organizacionId FROM fichas_familiares WHERE id = :id LIMIT 1")
    suspend fun fichaOrganizacion(id: Long): String?
    @Query("SELECT syncId FROM miembros_familia WHERE id = :id LIMIT 1")
    suspend fun miembroSyncId(id: Long): String?
    @Query("SELECT id FROM fichas_familiares WHERE syncId = :syncId LIMIT 1")
    suspend fun fichaLocalId(syncId: String): Long?
    @Query("SELECT id FROM miembros_familia WHERE syncId = :syncId LIMIT 1")
    suspend fun miembroLocalId(syncId: String): Long?
    @Query("SELECT * FROM actividades_agenda WHERE syncId = :syncId LIMIT 1")
    suspend fun porSyncId(syncId: String): ActividadAgendaEntity?
    @Insert suspend fun guardarRemota(item: ActividadAgendaEntity): Long
    @Update suspend fun actualizarRemota(item: ActividadAgendaEntity)

    @Query("SELECT * FROM actividades_agenda WHERE fichaId = :fichaId AND usuarioId = :usuarioId AND origen = 'SEGUIMIENTO' AND estado = 'PENDIENTE' AND eliminadoEn IS NULL")
    suspend fun seguimientosPendientes(fichaId: Long, usuarioId: Long): List<ActividadAgendaEntity>

    @Query("SELECT * FROM actividades_agenda WHERE fichaId = :fichaId AND usuarioId = :usuarioId AND origen = 'SEGUIMIENTO' AND estado = 'CANCELADA'")
    suspend fun seguimientosCancelados(fichaId: Long, usuarioId: Long): List<ActividadAgendaEntity>

    @Query("SELECT * FROM actividades_agenda WHERE fichaId = :fichaId AND origen = 'SEGUIMIENTO' AND estado = 'PENDIENTE' AND eliminadoEn IS NULL")
    suspend fun seguimientosPendientesFicha(fichaId: Long): List<ActividadAgendaEntity>

    @Query("SELECT id FROM actividades_agenda WHERE fichaId = :fichaId")
    suspend fun idsDeFicha(fichaId: Long): List<Long>

    @Query("SELECT * FROM actividades_agenda WHERE usuarioId = :usuarioId AND estado != 'CANCELADA' AND eliminadoEn IS NULL")
    suspend fun actividadesActivas(usuarioId: Long): List<ActividadAgendaEntity>

    @Query("UPDATE actividades_agenda SET eliminadoEn = :ahora, syncEstado = 'PENDIENTE', actualizadoEn = :ahora, estado = 'CANCELADA' WHERE fichaId = :fichaId AND eliminadoEn IS NULL")
    suspend fun eliminarDeFichaConFecha(fichaId: Long, ahora: Long)
    suspend fun eliminarDeFicha(fichaId: Long) = eliminarDeFichaConFecha(fichaId, System.currentTimeMillis())

    /** Cancela las citas por hacer de una ficha que dejó de ser de esta persona; las visitas ya realizadas se conservan. */
    @Query("UPDATE actividades_agenda SET eliminadoEn = :ahora, syncEstado = 'PENDIENTE', actualizadoEn = :ahora, estado = 'CANCELADA' WHERE fichaId = :fichaId AND estado = 'PENDIENTE' AND eliminadoEn IS NULL")
    suspend fun cancelarPendientesDeFicha(fichaId: Long, ahora: Long)

    @Query("""
        SELECT DISTINCT a.fichaId FROM actividades_agenda a
        LEFT JOIN fichas_familiares f ON f.id = a.fichaId
        WHERE a.usuarioId = :usuarioId AND a.origen = 'SEGUIMIENTO'
          AND a.estado = 'PENDIENTE' AND a.eliminadoEn IS NULL AND a.fichaId IS NOT NULL
          AND (f.id IS NULL OR f.estado != 'COMPLETA' OR f.miPermiso != '')
    """)
    suspend fun fichasInactivasConSeguimiento(usuarioId: Long): List<Long>

    @Query("SELECT MAX(fechaBase) FROM actividades_agenda WHERE fichaId = :fichaId AND usuarioId = :usuarioId AND origen = 'SEGUIMIENTO'")
    suspend fun ultimaFechaBase(fichaId: Long, usuarioId: Long): Long?

    /** Solo las fichas propias (creadas o traspasadas): el seguimiento de las compartidas es de su dueña. */
    @Query("SELECT * FROM fichas_familiares WHERE estado = 'COMPLETA' AND miPermiso = '' AND (:organizacionId = '' OR organizacionId = :organizacionId)")
    suspend fun fichasCompletas(organizacionId: String): List<FichaFamiliarEntity>

    @Query("SELECT * FROM miembros_familia WHERE fichaId = :fichaId ORDER BY id")
    suspend fun miembros(fichaId: Long): List<MiembroFamiliaEntity>

    @Query("SELECT * FROM embarazadas WHERE fichaId = :fichaId")
    suspend fun embarazos(fichaId: Long): List<EmbarazadaEntity>

    @Query("""
        SELECT m.id AS miembroId, m.fichaId AS fichaId, m.apellidosNombres AS apellidosNombres,
               m.cedula AS cedula, f.barrio AS barrio
        FROM miembros_familia AS m
        INNER JOIN fichas_familiares AS f ON f.id = m.fichaId
        WHERE f.miPermiso = '' AND (:organizacionId = '' OR f.organizacionId = :organizacionId)
        ORDER BY m.apellidosNombres
    """)
    fun observarPersonas(organizacionId: String): Flow<List<PersonaAgenda>>
}
