package com.ruralitos.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "establecimientos_salud",
    indices = [
        Index(value = ["codigoUo"], unique = true),
        Index(value = ["nombreCentroSalud"])
    ]
)
data class EstablecimientoSaludEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val codigoUo: String,
    val nombreCentroSalud: String,
    val institucionSistema: String,

    val provinciaCodigoLocalizacion: String,
    val provincia: String,

    val cantonCodigoLocalizacion: String,
    val canton: String,

    val parroquiaCodigoLocalizacion: String,
    val parroquia: String,

    val sector: String,
    val areaNumero: String
)
