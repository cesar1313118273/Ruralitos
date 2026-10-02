package com.ruralitos.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.UUID

@Entity(
    tableName = "historial_fichas",
    indices = [Index("fichaId"), Index("creadoEn"), Index(value = ["syncId"], unique = true)]
)
data class HistorialFichaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fichaId: Long,
    val numeroFicha: String,
    val usuarioId: Long?,
    val usuarioNombre: String,
    val accion: String,
    val detalle: String = "",
    val creadoEn: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''")
    val syncId: String = UUID.randomUUID().toString()
)
