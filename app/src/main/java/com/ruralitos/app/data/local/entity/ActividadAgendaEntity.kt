package com.ruralitos.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.UUID

@Entity(
    tableName = "actividades_agenda",
    indices = [Index("usuarioId"), Index("fechaHora"), Index("fichaId"), Index(value = ["syncId"], unique = true)]
)
data class ActividadAgendaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val fichaId: Long? = null,
    val miembroId: Long? = null,
    val persona: String = "",
    val cedula: String = "",
    val barrio: String = "",
    val fechaHora: Long,
    val tipo: String,
    val nota: String = "",
    val estado: String = "PENDIENTE",
    val recordar: Boolean = false,
    val creadoEn: Long = System.currentTimeMillis(),
    /** MANUAL o SEGUIMIENTO. Solo las citas SEGUIMIENTO se recalculan automáticamente. */
    @ColumnInfo(defaultValue = "'MANUAL'") val origen: String = "MANUAL",
    @ColumnInfo(defaultValue = "''") val grupoRiesgo: String = "",
    /** Fecha de la visita que dio origen al próximo seguimiento. */
    val fechaBase: Long? = null,
    /** Una fecha acordada manualmente no se sustituye por una simple edición clínica. */
    @ColumnInfo(defaultValue = "0") val fechaEditada: Boolean = false,
    @ColumnInfo(defaultValue = "''") val organizacionId: String = "",
    @ColumnInfo(defaultValue = "''") val syncId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "'PENDIENTE'") val syncEstado: String = "PENDIENTE",
    @ColumnInfo(defaultValue = "0") val syncVersion: Long = 0,
    @ColumnInfo(defaultValue = "0") val actualizadoEn: Long = System.currentTimeMillis(),
    val eliminadoEn: Long? = null
)

data class PersonaAgenda(
    val miembroId: Long,
    val fichaId: Long,
    val apellidosNombres: String,
    val cedula: String,
    val barrio: String
)
