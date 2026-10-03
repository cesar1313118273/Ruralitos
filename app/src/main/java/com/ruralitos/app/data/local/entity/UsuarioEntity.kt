package com.ruralitos.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "usuarios",
    indices = [
        Index(value = ["cedula"], unique = true),
        Index(value = ["supabaseId"])
    ]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cedula: String,
    val nombres: String,
    val cargo: String,
    val rol: String,
    val claveHash: String,
    val claveSalt: String,
    @ColumnInfo(defaultValue = "''")
    val correo: String = "",
    @ColumnInfo(defaultValue = "''")
    val telefono: String = "",
    val activo: Boolean = true,
    val creadoEn: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''")
    val supabaseId: String = "",
    @ColumnInfo(defaultValue = "''")
    val organizacionId: String = "",
    @ColumnInfo(defaultValue = "NULL")
    val establecimientoRemotoId: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val pinConfigurado: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val ultimoAccesoEn: Long = 0L,
    @ColumnInfo(defaultValue = "''")
    val codigoSenescyt: String = "",
    @ColumnInfo(defaultValue = "NULL")
    val firmaUri: String? = null,
    @ColumnInfo(defaultValue = "''")
    val sexo: String = "",
    @ColumnInfo(defaultValue = "''")
    val apellidos: String = ""
) {
    val esAdministrador: Boolean
        get() = rol == ROL_ADMIN

    companion object {
        const val ROL_ADMIN = "ADMIN"
        const val ROL_MEDICO = "MEDICO"
    }
}
