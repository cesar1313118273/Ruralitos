package com.ruralitos.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "salas",
    indices = [
        Index(value = ["establecimientoId"]),
        Index(value = ["activa"]),
        Index(value = ["nombreSala"])
    ]
)
data class SalaEntity(
    @PrimaryKey
    val organizacionId: String,
    val establecimientoId: Long?,
    val nombreSala: String,
    val codigoSala: String,
    val rol: String,
    val permiso: String,
    val activa: Boolean = true,
    val codigoUo: String = "",
    val nombreCentroSalud: String = "",
    val institucionSistema: String = "",
    val provinciaCodigoLocalizacion: String = "",
    val provincia: String = "",
    val cantonCodigoLocalizacion: String = "",
    val canton: String = "",
    val parroquiaCodigoLocalizacion: String = "",
    val parroquia: String = "",
    val sector: String = "",
    val areaNumero: String = "",
    val actualizadoEn: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "eais_sala",
    indices = [
        Index(value = ["salaId", "numero"], unique = true),
        Index(value = ["salaId", "activo"])
    ]
)
data class EaisSalaEntity(
    @PrimaryKey
    val id: String,
    val salaId: String,
    val numero: Int,
    val activo: Boolean = true,
    val actualizadoEn: Long = System.currentTimeMillis()
) {
    val nombre: String
        get() = "EAIS $numero"
}

@Entity(
    tableName = "territorios_sala",
    indices = [
        Index(value = ["salaId"]),
        Index(value = ["eaisId", "activo"]),
        Index(value = ["eaisId", "tipo", "nombre"], unique = true)
    ]
)
data class TerritorioSalaEntity(
    @PrimaryKey
    val id: String,
    val salaId: String,
    val eaisId: String,
    val tipo: String,
    val nombre: String,
    val activo: Boolean = true,
    val actualizadoEn: Long = System.currentTimeMillis()
) {
    val etiqueta: String
        get() = "Barrio"

    companion object {
        const val TIPO_BARRIO = "BARRIO"
        const val TIPO_COMUNIDAD = "COMUNIDAD"
    }
}
