package com.ruralitos.app.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "notas_diarias",
    foreignKeys = [ForeignKey(
        entity = MiembroFamiliaEntity::class,
        parentColumns = ["id"],
        childColumns = ["miembroId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("miembroId"), Index("fechaLocal"), Index("usuarioId"), Index(value = ["syncId"], unique = true)]
)
data class NotaDiariaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val miembroId: Long,
    /** Fecha civil del teléfono en el momento de crear la nota: yyyy-MM-dd. */
    val fechaLocal: String,
    val contenido: String,
    val creadaEn: Long = System.currentTimeMillis(),
    /** Una nota realizada deja de generar avisos. */
    @ColumnInfo(defaultValue = "0") val realizada: Boolean = false,
    /** Las notas antiguas sin propietario permanecen locales y no se envían. */
    @ColumnInfo(defaultValue = "0") val usuarioId: Long = 0,
    @ColumnInfo(defaultValue = "''") val syncId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "'PENDIENTE'") val syncEstado: String = "PENDIENTE",
    @ColumnInfo(defaultValue = "0") val syncVersion: Long = 0,
    @ColumnInfo(defaultValue = "0") val actualizadoEn: Long = System.currentTimeMillis(),
    val eliminadoEn: Long? = null
)

data class NotaDiariaConPersona(
    val id: Long,
    val miembroId: Long,
    val fechaLocal: String,
    val contenido: String,
    val creadaEn: Long,
    val realizada: Boolean,
    val apellidosNombres: String,
    val cedula: String
)

data class PersonaParaNota(
    val miembroId: Long,
    val apellidosNombres: String,
    val cedula: String,
    val diagnosticos: String
)
