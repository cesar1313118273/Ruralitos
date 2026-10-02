package com.ruralitos.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ruralitos.app.data.local.entity.NotaDiariaConPersona
import com.ruralitos.app.data.local.entity.NotaDiariaEntity
import com.ruralitos.app.data.local.entity.PersonaParaNota
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaDiariaDao {
    @Insert
    suspend fun crear(nota: NotaDiariaEntity): Long

    @Query("UPDATE notas_diarias SET contenido = :contenido, syncEstado = CASE WHEN usuarioId = 0 THEN 'LOCAL' ELSE 'PENDIENTE' END, actualizadoEn = :ahora WHERE id = :id AND eliminadoEn IS NULL")
    suspend fun actualizarContenidoConFecha(id: Long, contenido: String, ahora: Long)
    suspend fun actualizarContenido(id: Long, contenido: String) =
        actualizarContenidoConFecha(id, contenido, System.currentTimeMillis())

    @Query("UPDATE notas_diarias SET eliminadoEn = :ahora, syncEstado = CASE WHEN usuarioId = 0 THEN 'LOCAL' ELSE 'PENDIENTE' END, actualizadoEn = :ahora WHERE id = :id AND eliminadoEn IS NULL")
    suspend fun eliminarConFecha(id: Long, ahora: Long)
    suspend fun eliminar(id: Long) = eliminarConFecha(id, System.currentTimeMillis())

    @Query("SELECT * FROM notas_diarias WHERE usuarioId = :usuarioId AND syncEstado = 'PENDIENTE'")
    suspend fun pendientesSync(usuarioId: Long): List<NotaDiariaEntity>
    @Query("SELECT COUNT(*) FROM notas_diarias WHERE usuarioId = :usuarioId AND syncEstado = 'PENDIENTE'")
    fun observarPendientesSync(usuarioId: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM notas_diarias WHERE usuarioId = :usuarioId AND syncEstado = 'CONFLICTO'")
    fun observarConflictosSync(usuarioId: Long): Flow<Int>
    @Query("SELECT COALESCE(MAX(actualizadoEn), 0) FROM notas_diarias WHERE usuarioId = :usuarioId AND syncEstado = 'PENDIENTE'")
    fun observarUltimoCambioPendiente(usuarioId: Long): Flow<Long>
    @Query("UPDATE notas_diarias SET syncEstado = 'SINCRONIZADO', syncVersion = :version WHERE id = :id AND actualizadoEn = :esperado")
    suspend fun marcarSincronizada(id: Long, esperado: Long, version: Long): Int
    @Query("UPDATE notas_diarias SET syncEstado = 'CONFLICTO' WHERE id = :id AND actualizadoEn = :esperado")
    suspend fun marcarConflicto(id: Long, esperado: Long): Int
    @Query("SELECT syncId FROM miembros_familia WHERE id = :id LIMIT 1")
    suspend fun miembroSyncId(id: Long): String?
    @Query("SELECT f.organizacionId FROM miembros_familia m JOIN fichas_familiares f ON f.id = m.fichaId WHERE m.id = :id LIMIT 1")
    suspend fun miembroOrganizacion(id: Long): String?
    @Query("SELECT id FROM miembros_familia WHERE syncId = :syncId LIMIT 1")
    suspend fun miembroLocalId(syncId: String): Long?
    @Query("SELECT * FROM notas_diarias WHERE syncId = :syncId LIMIT 1")
    suspend fun porSyncId(syncId: String): NotaDiariaEntity?
    @Insert suspend fun guardarRemota(nota: NotaDiariaEntity): Long
    @androidx.room.Update suspend fun actualizarRemota(nota: NotaDiariaEntity)

    @Query("SELECT * FROM notas_diarias WHERE id = :id LIMIT 1")
    suspend fun buscar(id: Long): NotaDiariaEntity?

    @Query("UPDATE notas_diarias SET realizada = :realizada, syncEstado = CASE WHEN usuarioId = 0 THEN 'LOCAL' ELSE 'PENDIENTE' END, actualizadoEn = :ahora WHERE id = :id AND eliminadoEn IS NULL")
    suspend fun marcarRealizadaConFecha(id: Long, realizada: Boolean, ahora: Long)
    suspend fun marcarRealizada(id: Long, realizada: Boolean) =
        marcarRealizadaConFecha(id, realizada, System.currentTimeMillis())

    @Query("SELECT * FROM notas_diarias WHERE realizada = 0 AND eliminadoEn IS NULL AND creadaEn >= :desde")
    suspend fun pendientesRecientes(desde: Long): List<NotaDiariaEntity>

    @Query("""
        SELECT n.id, n.miembroId, n.fechaLocal, n.contenido, n.creadaEn, n.realizada,
               m.apellidosNombres, m.cedula
        FROM notas_diarias AS n
        INNER JOIN miembros_familia AS m ON m.id = n.miembroId
        INNER JOIN fichas_familiares AS f ON f.id = m.fichaId
        WHERE (:organizacionId = '' OR f.organizacionId = :organizacionId)
          AND (n.usuarioId = :usuarioId OR n.usuarioId = 0) AND n.eliminadoEn IS NULL
        ORDER BY n.creadaEn DESC, n.id DESC
    """)
    fun observarTodas(organizacionId: String, usuarioId: Long): Flow<List<NotaDiariaConPersona>>

    @Query("""
        SELECT m.id AS miembroId, m.apellidosNombres, m.cedula,
               m.riesgoEnfermedadDiscapacidad AS diagnosticos
        FROM miembros_familia AS m
        INNER JOIN fichas_familiares AS f ON f.id = m.fichaId
        WHERE (:organizacionId = '' OR f.organizacionId = :organizacionId)
        ORDER BY m.apellidosNombres COLLATE NOCASE, m.id
    """)
    fun observarPersonas(organizacionId: String): Flow<List<PersonaParaNota>>
}
