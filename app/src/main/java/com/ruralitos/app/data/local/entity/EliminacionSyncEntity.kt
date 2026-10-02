package com.ruralitos.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "eliminaciones_sync",
    indices = [
        Index(value = ["tabla", "registroSyncId"], unique = true),
        Index(value = ["organizacionId"])
    ]
)
data class EliminacionSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tabla: String,
    val registroSyncId: String,
    val organizacionId: String,
    val creadoEn: Long = System.currentTimeMillis()
)
