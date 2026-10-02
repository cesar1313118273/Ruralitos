package com.ruralitos.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ruralitos.app.data.local.entity.HistorialFichaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistorialFichaDao {
    @Insert
    suspend fun registrar(item: HistorialFichaEntity): Long

    @Query("SELECT * FROM historial_fichas WHERE fichaId = :fichaId ORDER BY creadoEn DESC, id DESC")
    fun listar(fichaId: Long): Flow<List<HistorialFichaEntity>>
}
