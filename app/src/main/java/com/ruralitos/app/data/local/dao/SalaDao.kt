package com.ruralitos.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruralitos.app.data.local.entity.EaisSalaEntity
import com.ruralitos.app.data.local.entity.SalaEntity
import com.ruralitos.app.data.local.entity.TerritorioSalaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarSalas(items: List<SalaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarSala(item: SalaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarEais(items: List<EaisSalaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarEais(item: EaisSalaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTerritorios(items: List<TerritorioSalaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarTerritorio(item: TerritorioSalaEntity)

    @Query("SELECT * FROM salas WHERE activa = 1 ORDER BY nombreCentroSalud COLLATE NOCASE, nombreSala COLLATE NOCASE")
    fun observarSalas(): Flow<List<SalaEntity>>

    @Query("SELECT * FROM salas WHERE activa = 1 ORDER BY nombreCentroSalud COLLATE NOCASE, nombreSala COLLATE NOCASE")
    suspend fun listarSalas(): List<SalaEntity>

    @Query("SELECT * FROM salas WHERE organizacionId = :id LIMIT 1")
    suspend fun buscarSala(id: String): SalaEntity?

    @Query("SELECT * FROM eais_sala WHERE activo = 1 ORDER BY salaId, numero")
    fun observarTodosEais(): Flow<List<EaisSalaEntity>>

    @Query("SELECT t.* FROM territorios_sala t JOIN eais_sala e ON e.id = t.eaisId WHERE t.activo = 1 AND e.activo = 1 ORDER BY t.salaId, t.eaisId, t.nombre COLLATE NOCASE")
    fun observarTodosTerritorios(): Flow<List<TerritorioSalaEntity>>

    @Query("SELECT * FROM eais_sala ORDER BY salaId, numero")
    suspend fun listarEaisHistoricos(): List<EaisSalaEntity>

    @Query("SELECT * FROM territorios_sala ORDER BY salaId, eaisId, nombre COLLATE NOCASE")
    suspend fun listarTerritoriosHistoricos(): List<TerritorioSalaEntity>
    @Query("SELECT * FROM eais_sala WHERE salaId = :salaId AND activo = 1 ORDER BY numero")
    fun observarEais(salaId: String): Flow<List<EaisSalaEntity>>


    @Query("SELECT * FROM eais_sala WHERE salaId = :salaId AND activo = 1 ORDER BY numero")
    suspend fun listarEais(salaId: String): List<EaisSalaEntity>

    @Query("SELECT * FROM eais_sala WHERE id = :id LIMIT 1")
    suspend fun buscarEais(id: String): EaisSalaEntity?

    @Query("SELECT * FROM territorios_sala WHERE eaisId = :eaisId AND activo = 1 ORDER BY tipo, nombre COLLATE NOCASE")
    fun observarTerritorios(eaisId: String): Flow<List<TerritorioSalaEntity>>

    @Query("SELECT * FROM territorios_sala WHERE eaisId = :eaisId AND activo = 1 ORDER BY tipo, nombre COLLATE NOCASE")
    suspend fun listarTerritorios(eaisId: String): List<TerritorioSalaEntity>

    @Query("SELECT * FROM territorios_sala WHERE id = :id LIMIT 1")
    suspend fun buscarTerritorio(id: String): TerritorioSalaEntity?

    @Query("UPDATE salas SET activa = 0")
    suspend fun desactivarSalas()

    @Query("UPDATE eais_sala SET activo = 0 WHERE salaId = :salaId")
    suspend fun desactivarEais(salaId: String)

    @Query("UPDATE territorios_sala SET activo = 0 WHERE salaId = :salaId")
    suspend fun desactivarTerritorios(salaId: String)

    @Query("UPDATE territorios_sala SET activo = 0 WHERE eaisId = :eaisId")
    suspend fun desactivarTerritoriosDeEais(eaisId: String)

}
