package com.ruralitos.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Delete
import com.ruralitos.app.data.local.entity.AdjuntoFichaEntity
import com.ruralitos.app.data.local.entity.CalificacionRiesgoEntity
import com.ruralitos.app.data.local.entity.ContaminacionAmbientalEntity
import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.GestionRiesgoEntity
import com.ruralitos.app.data.local.entity.LugarTratamientoEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.MortalidadFamiliarEntity
import com.ruralitos.app.data.local.entity.ValorRiesgoEntity
import com.ruralitos.app.domain.RiesgoFamiliar
import kotlinx.coroutines.flow.Flow

@Dao
interface FichaContenidoDao {
    @Insert suspend fun guardarMiembro(item: MiembroFamiliaEntity): Long
    @Insert suspend fun guardarEmbarazada(item: EmbarazadaEntity): Long
    @Insert suspend fun guardarMortalidad(item: MortalidadFamiliarEntity): Long
    @Insert suspend fun guardarGestionRiesgo(item: GestionRiesgoEntity): Long
    @Insert suspend fun guardarContaminacion(item: ContaminacionAmbientalEntity): Long
    @Insert suspend fun guardarLugarTratamiento(item: LugarTratamientoEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarAdjunto(item: AdjuntoFichaEntity): Long

    @Update suspend fun actualizarMiembro(item: MiembroFamiliaEntity)
    @Update suspend fun actualizarEmbarazada(item: EmbarazadaEntity)
    @Update suspend fun actualizarMortalidad(item: MortalidadFamiliarEntity)
    @Update suspend fun actualizarGestionRiesgo(item: GestionRiesgoEntity)
    @Update suspend fun actualizarContaminacion(item: ContaminacionAmbientalEntity)
    @Update suspend fun actualizarLugarTratamiento(item: LugarTratamientoEntity)

    @Delete suspend fun eliminarMiembro(item: MiembroFamiliaEntity)
    @Delete suspend fun eliminarEmbarazada(item: EmbarazadaEntity)
    @Delete suspend fun eliminarMortalidad(item: MortalidadFamiliarEntity)
    @Delete suspend fun eliminarGestionRiesgo(item: GestionRiesgoEntity)
    @Delete suspend fun eliminarContaminacion(item: ContaminacionAmbientalEntity)
    @Delete suspend fun eliminarLugarTratamiento(item: LugarTratamientoEntity)
    @Delete suspend fun eliminarAdjunto(item: AdjuntoFichaEntity)

    @Insert suspend fun guardarCalificacion(item: CalificacionRiesgoEntity): Long
    @Insert suspend fun guardarValoresRiesgo(items: List<ValorRiesgoEntity>)
    @Update suspend fun actualizarCalificacion(item: CalificacionRiesgoEntity)
    @Delete suspend fun eliminarCalificacion(item: CalificacionRiesgoEntity)

    @Query("SELECT * FROM valores_riesgo WHERE calificacionId = :calificacionId ORDER BY componente")
    suspend fun obtenerValoresRiesgo(calificacionId: Long): List<ValorRiesgoEntity>

    @Query("DELETE FROM valores_riesgo WHERE calificacionId = :calificacionId")
    suspend fun eliminarValoresRiesgo(calificacionId: Long)

    @Transaction
    suspend fun guardarCalificacionCompleta(
        calificacion: CalificacionRiesgoEntity,
        valores: List<Int>
    ): Long {
        val resultado = RiesgoFamiliar.calcular(valores)
        val calificacionId = guardarCalificacion(
            calificacion.copy(total = resultado.total, nivel = resultado.nivel)
        )
        guardarValoresRiesgo(
            valores.mapIndexed { index, valor ->
                ValorRiesgoEntity(
                    calificacionId = calificacionId,
                    componente = index + 1,
                    valor = valor
                )
            }
        )
        return calificacionId
    }

    @Transaction
    suspend fun actualizarCalificacionCompleta(
        calificacion: CalificacionRiesgoEntity,
        valores: List<Int>
    ) {
        val resultado = RiesgoFamiliar.calcular(valores)
        actualizarCalificacion(
            calificacion.copy(total = resultado.total, nivel = resultado.nivel)
        )
        eliminarValoresRiesgo(calificacion.id)
        guardarValoresRiesgo(
            valores.mapIndexed { index, valor ->
                ValorRiesgoEntity(
                    calificacionId = calificacion.id,
                    componente = index + 1,
                    valor = valor
                )
            }
        )
    }

    @Query("SELECT * FROM miembros_familia WHERE fichaId = :fichaId ORDER BY id")
    fun listarMiembros(fichaId: Long): Flow<List<MiembroFamiliaEntity>>

    /** Integrantes de MIS fichas (las que creé o me traspasaron); no cuenta las que otra persona me compartió. */
    @Query("""
        SELECT m.* FROM miembros_familia m
        JOIN fichas_familiares f ON f.id = m.fichaId
        WHERE f.miPermiso = ''
        ORDER BY m.fichaId, m.id
    """)
    fun listarMiembrosDeMisFichas(): Flow<List<MiembroFamiliaEntity>>

    @Query("SELECT * FROM miembros_familia ORDER BY fichaId, id")
    fun listarTodosMiembros(): Flow<List<MiembroFamiliaEntity>>

    @Query("SELECT * FROM miembros_familia WHERE fichaId = :fichaId AND parentesco LIKE 'JEF%' ORDER BY id LIMIT 1")
    suspend fun buscarJefeFamilia(fichaId: Long): MiembroFamiliaEntity?

    @Query("SELECT * FROM embarazadas WHERE fichaId = :fichaId ORDER BY id")
    fun listarEmbarazadas(fichaId: Long): Flow<List<EmbarazadaEntity>>

    @Query("SELECT * FROM embarazadas ORDER BY fichaId, id")
    fun listarTodasEmbarazadas(): Flow<List<EmbarazadaEntity>>

    @Query("SELECT * FROM mortalidad_familiar WHERE fichaId = :fichaId ORDER BY id")
    fun listarMortalidad(fichaId: Long): Flow<List<MortalidadFamiliarEntity>>

    @Query("SELECT * FROM calificaciones_riesgo WHERE fichaId = :fichaId ORDER BY fechaCalificacion")
    fun listarCalificaciones(fichaId: Long): Flow<List<CalificacionRiesgoEntity>>

    @Query("SELECT * FROM calificaciones_riesgo ORDER BY fichaId, id")
    fun listarTodasCalificaciones(): Flow<List<CalificacionRiesgoEntity>>

    @Query("SELECT * FROM valores_riesgo ORDER BY calificacionId, componente")
    fun listarTodosValoresRiesgo(): Flow<List<ValorRiesgoEntity>>

    @Query("SELECT * FROM gestion_riesgo WHERE fichaId = :fichaId ORDER BY fechaAnalisis")
    fun listarGestionRiesgo(fichaId: Long): Flow<List<GestionRiesgoEntity>>

    @Query("SELECT * FROM contaminacion_ambiental WHERE fichaId = :fichaId ORDER BY fechaInforme")
    fun listarContaminacion(fichaId: Long): Flow<List<ContaminacionAmbientalEntity>>

    @Query("SELECT * FROM lugares_tratamiento WHERE fichaId = :fichaId ORDER BY id")
    fun listarLugaresTratamiento(fichaId: Long): Flow<List<LugarTratamientoEntity>>

    @Query("SELECT * FROM adjuntos_ficha WHERE fichaId = :fichaId ORDER BY tipo")
    fun listarAdjuntos(fichaId: Long): Flow<List<AdjuntoFichaEntity>>

    @Query("SELECT * FROM adjuntos_ficha ORDER BY id")
    suspend fun listarTodosLosAdjuntos(): List<AdjuntoFichaEntity>

    @Query("UPDATE adjuntos_ficha SET uri = :uri, actualizadoEn = :actualizadoEn WHERE id = :id")
    suspend fun actualizarUriAdjunto(
        id: Long,
        uri: String,
        actualizadoEn: Long = System.currentTimeMillis()
    )
}
