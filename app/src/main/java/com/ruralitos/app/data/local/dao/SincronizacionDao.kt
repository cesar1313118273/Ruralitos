package com.ruralitos.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ruralitos.app.data.local.entity.AdjuntoFichaEntity
import com.ruralitos.app.data.local.entity.CalificacionRiesgoEntity
import com.ruralitos.app.data.local.entity.ContaminacionAmbientalEntity
import com.ruralitos.app.data.local.entity.EliminacionSyncEntity
import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.GestionRiesgoEntity
import com.ruralitos.app.data.local.entity.HistorialFichaEntity
import com.ruralitos.app.data.local.entity.LugarTratamientoEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.MortalidadFamiliarEntity
import com.ruralitos.app.data.local.entity.ValorRiesgoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SincronizacionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun guardarEliminaciones(items: List<EliminacionSyncEntity>): List<Long>

    @Query("""
        UPDATE fichas_familiares SET organizacionId = :organizacionId
        WHERE organizacionId = ''
    """)
    suspend fun asignarOrganizacionPendiente(organizacionId: String)

    @Query("""
        SELECT * FROM fichas_familiares
        WHERE organizacionId = :organizacionId AND syncEstado NOT IN ('SINCRONIZADO', 'CONFLICTO')
        ORDER BY CASE WHEN syncEstado = 'ERROR' THEN 1 ELSE 0 END, actualizadoEn ASC
    """)
    suspend fun fichasPendientes(organizacionId: String): List<FichaFamiliarEntity>

    @Query("SELECT COUNT(*) FROM fichas_familiares WHERE syncEstado NOT IN ('SINCRONIZADO', 'CONFLICTO')")
    fun observarPendientes(): Flow<Int>

    @Query("SELECT COUNT(*) FROM fichas_familiares WHERE syncEstado = 'CONFLICTO'")
    fun observarConflictos(): Flow<Int>

    @Query("SELECT COALESCE(MAX(actualizadoEn), 0) FROM fichas_familiares WHERE syncEstado NOT IN ('SINCRONIZADO', 'CONFLICTO')")
    fun observarUltimoCambioPendiente(): Flow<Long>

    @Query("SELECT COUNT(*) FROM fichas_familiares")
    suspend fun contarFichasDeTodasLasOrganizaciones(): Int

    /** Fichas, agenda y notas con cambios que aún no llegaron al servidor (incluye los conflictos sin decidir). */
    @Query("""
        SELECT (SELECT COUNT(*) FROM fichas_familiares WHERE syncEstado <> 'SINCRONIZADO')
             + (SELECT COUNT(*) FROM actividades_agenda WHERE syncEstado IN ('PENDIENTE', 'CONFLICTO'))
             + (SELECT COUNT(*) FROM notas_diarias WHERE syncEstado IN ('PENDIENTE', 'CONFLICTO'))
    """)
    suspend fun contarCambiosSinSubir(): Int

    /**
     * Las etiquetas de «compartida» (autor, permiso, a cuántas personas) NO son cambios de la ficha: no deben dejarla
     * «pendiente de sincronizar». Un disparador de la base marca pendiente toda ficha sincronizada que se modifica, así que
     * al anotar etiquetas se las aparta un instante como «descargando» y se devuelven a «sincronizada» (siempre dentro de
     * una sola transacción, ver [ponerEtiquetas]).
     */
    @Query("UPDATE fichas_familiares SET syncEstado = 'DESCARGANDO' WHERE syncEstado = 'SINCRONIZADO'")
    suspend fun suspenderSincronizadas()

    @Query("UPDATE fichas_familiares SET syncEstado = 'SINCRONIZADO' WHERE syncEstado = 'DESCARGANDO'")
    suspend fun reanudarSincronizadas()

    /** Antes de poner las etiquetas de compartidas que dice el servidor se borran las anteriores. */
    @Query("""
        UPDATE fichas_familiares SET miPermiso = '', autorNombre = '', compartidaConPersonas = 0, editorNombre = '', editadaPorOtroEn = 0
        WHERE organizacionId = :organizacionId AND syncEstado IN ('SINCRONIZADO', 'DESCARGANDO')
    """)
    suspend fun limpiarEtiquetasCompartidas(organizacionId: String)

    /** Ficha que otra persona me compartió: quién la creó y qué puedo hacer con ella. */
    @Query("""
        UPDATE fichas_familiares SET autorRemotoId = :autorId, autorNombre = :autorNombre, miPermiso = :permiso
        WHERE syncId = :syncId
    """)
    suspend fun marcarFichaRecibida(syncId: String, autorId: String, autorNombre: String, permiso: String)

    @Query("SELECT * FROM fichas_familiares WHERE autorRemotoId = :autorId AND syncEstado = 'SINCRONIZADO'")
    suspend fun fichasSincronizadasDeAutor(autorId: String): List<FichaFamiliarEntity>

    /** Otra persona modificó una ficha mía: quién y cuándo. */
    @Query("UPDATE fichas_familiares SET editorNombre = :nombre, editadaPorOtroEn = :enMillis WHERE syncId = :syncId")
    suspend fun marcarFichaEditadaPorOtro(syncId: String, nombre: String, enMillis: Long)

    /** Ficha mía que ya compartí con :personas personas. */
    @Query("UPDATE fichas_familiares SET compartidaConPersonas = :personas WHERE syncId = :syncId")
    suspend fun marcarFichaCompartidaPorMi(syncId: String, personas: Int)

    @Query("SELECT COUNT(*) FROM fichas_familiares WHERE organizacionId = :organizacionId")
    suspend fun contarFichasDeOrganizacion(organizacionId: String): Int

    @Query("SELECT * FROM fichas_familiares WHERE organizacionId = :organizacionId")
    suspend fun fichasDeOrganizacion(organizacionId: String): List<FichaFamiliarEntity>

    /** Guarda la versión del servidor sin tocar el estado: se llama justo después de subir la cabecera. */
    @Query("UPDATE fichas_familiares SET syncVersion = :version WHERE id = :id")
    suspend fun fijarVersion(id: Long, version: Long)

    /** La subida vio que otra persona cambió la ficha antes: se conserva todo y se pide decidir. */
    @Query("UPDATE fichas_familiares SET syncEstado = 'CONFLICTO', syncError = :mensaje WHERE id = :id")
    suspend fun marcarConflicto(id: Long, mensaje: String)

    /** Las fichas que versiones anteriores dejaron en conflicto vuelven a la cola de subida. */
    @Query("UPDATE fichas_familiares SET syncEstado = 'PENDIENTE', syncError = '' WHERE syncEstado = 'CONFLICTO'")
    suspend fun convertirConflictosEnPendientes(): Int

    /** «Conservar mis cambios»: se toma como base la versión actual del servidor y se vuelve a subir. */
    @Query("""
        UPDATE fichas_familiares SET syncEstado = 'PENDIENTE', syncError = '', syncVersion = :version
        WHERE id = :id AND syncEstado = 'CONFLICTO'
    """)
    suspend fun conservarLocalTrasConflicto(id: Long, version: Long): Int

    /** Cierre de una descarga atómica: sin condiciones, porque ocurre dentro de la misma transacción. */
    @Query("""
        UPDATE fichas_familiares SET syncEstado = 'SINCRONIZADO', syncVersion = :version, syncError = ''
        WHERE id = :id
    """)
    suspend fun marcarSincronizadaDescarga(id: Long, version: Long): Int

    @Query("""
        SELECT f.syncId FROM calificaciones_riesgo c JOIN fichas_familiares f ON f.id = c.fichaId
        WHERE c.syncId = :calificacionSyncId LIMIT 1
    """)
    suspend fun fichaSyncIdDeCalificacion(calificacionSyncId: String): String?

    @Query("SELECT syncId FROM fichas_familiares WHERE id = :id")
    suspend fun fichaSyncId(id: Long): String?

    @Query("SELECT DISTINCT organizacionId FROM fichas_familiares WHERE organizacionId <> ''")
    suspend fun organizacionesLocales(): List<String>

    /** Devuelve a la ficha el estado y la fecha que tenía (tras un cambio interno que no es una edición del usuario). */
    @Query("UPDATE fichas_familiares SET syncEstado = :estado, actualizadoEn = :actualizadoEn WHERE id = :id")
    suspend fun restablecerEstado(id: Long, estado: String, actualizadoEn: Long)

    @Query("UPDATE adjuntos_ficha SET syncId = :nuevo WHERE syncId = :actual")
    suspend fun cambiarSyncIdAdjunto(actual: String, nuevo: String)

    @Query("""
        UPDATE fichas_familiares SET
            syncEstado = 'SINCRONIZADO', syncVersion = :version, syncError = ''
        WHERE id = :id AND actualizadoEn = :actualizadoEnEsperado
    """)
    suspend fun marcarSincronizada(
        id: Long,
        version: Long,
        actualizadoEnEsperado: Long
    ): Int

    @Query("""
        UPDATE fichas_familiares
        SET syncEstado = 'DESCARGANDO', syncError = ''
        WHERE id = :id
    """)
    suspend fun marcarDescargando(id: Long): Int

    @Query("""
        UPDATE fichas_familiares SET syncEstado = 'ERROR', syncError = :mensaje
        WHERE id = :id
    """)
    suspend fun marcarError(id: Long, mensaje: String)

    @Query("SELECT * FROM fichas_familiares WHERE syncId = :syncId LIMIT 1")
    suspend fun fichaPorSyncId(syncId: String): FichaFamiliarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarFichaRemota(item: FichaFamiliarEntity): Long

    @Update
    suspend fun actualizarFichaRemota(item: FichaFamiliarEntity)

    @Query("SELECT id FROM miembros_familia WHERE syncId = :syncId LIMIT 1")
    suspend fun idMiembro(syncId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarMiembroRemoto(item: MiembroFamiliaEntity): Long

    @Update suspend fun actualizarMiembroRemoto(item: MiembroFamiliaEntity)

    @Query("SELECT id FROM embarazadas WHERE syncId = :syncId LIMIT 1")
    suspend fun idEmbarazada(syncId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarEmbarazadaRemota(item: EmbarazadaEntity): Long

    @Update suspend fun actualizarEmbarazadaRemota(item: EmbarazadaEntity)

    @Query("SELECT id FROM mortalidad_familiar WHERE syncId = :syncId LIMIT 1")
    suspend fun idMortalidad(syncId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarMortalidadRemota(item: MortalidadFamiliarEntity): Long

    @Update suspend fun actualizarMortalidadRemota(item: MortalidadFamiliarEntity)

    @Query("SELECT id FROM calificaciones_riesgo WHERE syncId = :syncId LIMIT 1")
    suspend fun idCalificacion(syncId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarCalificacionRemota(item: CalificacionRiesgoEntity): Long

    @Update suspend fun actualizarCalificacionRemota(item: CalificacionRiesgoEntity)

    @Query("SELECT id FROM valores_riesgo WHERE syncId = :syncId LIMIT 1")
    suspend fun idValorRiesgo(syncId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarValorRiesgoRemoto(item: ValorRiesgoEntity): Long

    @Update suspend fun actualizarValorRiesgoRemoto(item: ValorRiesgoEntity)

    @Query("SELECT id FROM gestion_riesgo WHERE syncId = :syncId LIMIT 1")
    suspend fun idGestion(syncId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarGestionRemota(item: GestionRiesgoEntity): Long

    @Update suspend fun actualizarGestionRemota(item: GestionRiesgoEntity)

    @Query("SELECT id FROM contaminacion_ambiental WHERE syncId = :syncId LIMIT 1")
    suspend fun idContaminacion(syncId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarContaminacionRemota(item: ContaminacionAmbientalEntity): Long

    @Update suspend fun actualizarContaminacionRemota(item: ContaminacionAmbientalEntity)

    @Query("SELECT id FROM lugares_tratamiento WHERE syncId = :syncId LIMIT 1")
    suspend fun idLugar(syncId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarLugarRemoto(item: LugarTratamientoEntity): Long

    @Update suspend fun actualizarLugarRemoto(item: LugarTratamientoEntity)

    @Query("SELECT id FROM historial_fichas WHERE syncId = :syncId LIMIT 1")
    suspend fun idHistorial(syncId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarHistorialRemoto(item: HistorialFichaEntity): Long

    @Update suspend fun actualizarHistorialRemoto(item: HistorialFichaEntity)

    @Query("SELECT id FROM adjuntos_ficha WHERE syncId = :syncId LIMIT 1")
    suspend fun idAdjunto(syncId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarAdjuntoRemoto(item: AdjuntoFichaEntity): Long

    @Update suspend fun actualizarAdjuntoRemoto(item: AdjuntoFichaEntity)

    @Query("SELECT * FROM fichas_familiares WHERE organizacionId = :organizacionId AND syncEstado = 'SINCRONIZADO'")
    suspend fun fichasLocalesSincronizadas(organizacionId: String): List<FichaFamiliarEntity>

    @Query("DELETE FROM fichas_familiares WHERE syncId = :syncId AND syncEstado IN ('SINCRONIZADO', 'DESCARGANDO')")
    suspend fun eliminarFichaRemota(syncId: String)

    @Query("DELETE FROM miembros_familia WHERE syncId = :syncId")
    suspend fun eliminarMiembroRemoto(syncId: String)

    @Query("DELETE FROM embarazadas WHERE syncId = :syncId")
    suspend fun eliminarEmbarazadaRemota(syncId: String)

    @Query("DELETE FROM mortalidad_familiar WHERE syncId = :syncId")
    suspend fun eliminarMortalidadRemota(syncId: String)

    @Query("DELETE FROM calificaciones_riesgo WHERE syncId = :syncId")
    suspend fun eliminarCalificacionRemota(syncId: String)

    @Query("DELETE FROM valores_riesgo WHERE syncId = :syncId")
    suspend fun eliminarValorRiesgoRemoto(syncId: String)

    @Query("DELETE FROM gestion_riesgo WHERE syncId = :syncId")
    suspend fun eliminarGestionRemota(syncId: String)

    @Query("DELETE FROM contaminacion_ambiental WHERE syncId = :syncId")
    suspend fun eliminarContaminacionRemota(syncId: String)

    @Query("DELETE FROM lugares_tratamiento WHERE syncId = :syncId")
    suspend fun eliminarLugarRemoto(syncId: String)

    @Query("DELETE FROM adjuntos_ficha WHERE syncId = :syncId")
    suspend fun eliminarAdjuntoRemoto(syncId: String)

    @Query("DELETE FROM eliminaciones_sync WHERE registroSyncId = :syncId")
    suspend fun descartarTumbaRemota(syncId: String)

    @Query("SELECT * FROM miembros_familia WHERE fichaId = :fichaId")
    suspend fun miembros(fichaId: Long): List<MiembroFamiliaEntity>

    @Query("SELECT * FROM embarazadas WHERE fichaId = :fichaId")
    suspend fun embarazadas(fichaId: Long): List<EmbarazadaEntity>

    @Query("SELECT * FROM mortalidad_familiar WHERE fichaId = :fichaId")
    suspend fun mortalidad(fichaId: Long): List<MortalidadFamiliarEntity>

    @Query("SELECT * FROM calificaciones_riesgo WHERE fichaId = :fichaId")
    suspend fun calificaciones(fichaId: Long): List<CalificacionRiesgoEntity>

    @Query("SELECT * FROM valores_riesgo WHERE calificacionId = :calificacionId")
    suspend fun valores(calificacionId: Long): List<ValorRiesgoEntity>

    @Query("SELECT * FROM gestion_riesgo WHERE fichaId = :fichaId")
    suspend fun gestiones(fichaId: Long): List<GestionRiesgoEntity>

    @Query("SELECT * FROM contaminacion_ambiental WHERE fichaId = :fichaId")
    suspend fun contaminaciones(fichaId: Long): List<ContaminacionAmbientalEntity>

    @Query("SELECT * FROM lugares_tratamiento WHERE fichaId = :fichaId")
    suspend fun lugares(fichaId: Long): List<LugarTratamientoEntity>

    @Query("SELECT * FROM adjuntos_ficha WHERE fichaId = :fichaId")
    suspend fun adjuntos(fichaId: Long): List<AdjuntoFichaEntity>

    @Query("SELECT * FROM historial_fichas WHERE fichaId = :fichaId")
    suspend fun historial(fichaId: Long): List<HistorialFichaEntity>

    @Query("SELECT * FROM eliminaciones_sync WHERE organizacionId = :organizacionId ORDER BY creadoEn LIMIT :limite")
    suspend fun eliminaciones(organizacionId: String, limite: Int = 100): List<EliminacionSyncEntity>

    @Query("DELETE FROM eliminaciones_sync WHERE id = :id")
    suspend fun eliminarTumba(id: Long)

    @Query("DELETE FROM eliminaciones_sync WHERE id IN (:ids)")
    suspend fun eliminarTumbas(ids: List<Long>)

    @Query("SELECT DISTINCT tabla FROM eliminaciones_sync WHERE organizacionId = :organizacionId")
    suspend fun tablasConEliminaciones(organizacionId: String): List<String>

    @Query("SELECT * FROM eliminaciones_sync WHERE organizacionId = :organizacionId AND tabla = :tabla ORDER BY id LIMIT :limite")
    suspend fun eliminacionesDeTabla(organizacionId: String, tabla: String, limite: Int): List<EliminacionSyncEntity>

    /** Fichas que el usuario eliminó y que la nube todavía no confirmó: no deben volver a bajar. */
    @Query("SELECT registroSyncId FROM eliminaciones_sync WHERE organizacionId = :organizacionId AND tabla = 'fichas_familiares'")
    suspend fun fichasEnBaja(organizacionId: String): List<String>

    @Query("SELECT COUNT(*) FROM eliminaciones_sync WHERE tabla = 'fichas_familiares'")
    suspend fun contarFichasEnBaja(): Int

    @Query("SELECT COUNT(*) FROM eliminaciones_sync")
    suspend fun contarEliminacionesPendientes(): Int
}
