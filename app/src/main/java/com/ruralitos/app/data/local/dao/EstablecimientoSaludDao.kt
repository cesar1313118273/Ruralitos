package com.ruralitos.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruralitos.app.data.local.entity.EstablecimientoSaludEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EstablecimientoSaludDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarEstablecimientos(establecimientos: List<EstablecimientoSaludEntity>)

    @Query("""
    SELECT * FROM establecimientos_salud
    WHERE codigoUo LIKE '%' || :texto || '%'
    OR nombreCentroSalud LIKE '%' || :texto || '%'
    OR provincia LIKE '%' || :texto || '%'
    OR canton LIKE '%' || :texto || '%'
    OR parroquia LIKE '%' || :texto || '%'
    ORDER BY nombreCentroSalud ASC
    LIMIT 30
""")
    fun buscarEstablecimientos(texto: String): Flow<List<EstablecimientoSaludEntity>>

    @Query("SELECT * FROM establecimientos_salud WHERE codigoUo = :codigoUo LIMIT 1")
    suspend fun buscarPorCodigoUo(codigoUo: String): EstablecimientoSaludEntity?

    @Query("SELECT COUNT(*) FROM establecimientos_salud")
    suspend fun contarEstablecimientos(): Int
}