package com.ruralitos.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.UUID

@Entity(
    tableName = "fichas_familiares",
    indices = [
        Index(value = ["cedulaJefeHogar"]),
        Index(value = ["codigoUo"]),
        Index(value = ["fechaLlenado"]),
        Index(value = ["estado"]),
        Index(value = ["actualizadoEn"]),
        Index(value = ["syncId"], unique = true),
        Index(value = ["syncEstado"]),
        Index(value = ["organizacionId"]),
        Index(value = ["eaisId"]),
        Index(value = ["territorioId"])
    ]
)
data class FichaFamiliarEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val cedulaJefeHogar: String,

    val institucionSistema: String,
    val unidadOperativa: String,
    val codigoUo: String,
    val areaNumero: String,
    val codigoLocalizacion: String,

    val parroquiaCodigoLocalizacion: String,
    val cantonCodigoLocalizacion: String,
    val provinciaCodigoLocalizacion: String,

    val numeroFichaFamiliar: String,

    val provincia: String,
    val canton: String,
    val parroquia: String,
    val sector: String,
    val manzana: String,
    val numeroFamilia: String,

    val direccionHabitualFamilia: String,
    val barrio: String,
    val numeroCasa: String,
    val comunidad: String,
    val grupoCultural: String,

    val nombreApellidoJefeFamilia: String,
    val numeroTelefono: String,
    val fechaLlenado: String,
    val numeroCarpeta: String,

    val latitud: Double? = null,
    val longitud: Double? = null,
    val altitud: Double? = null,

    /** Símbolos y textos que se colocan sobre el mapa del croquis (ver ElementosCroquis). */
    @ColumnInfo(defaultValue = "'[]'")
    val croquisElementosJson: String = "[]",

    @ColumnInfo(defaultValue = "''")
    val responsableNombre: String = "",
    @ColumnInfo(defaultValue = "''")
    val responsableCodigo: String = "",
    val firmaUri: String? = null,

    @ColumnInfo(defaultValue = "'BORRADOR'")
    val estado: String = "BORRADOR",
    @ColumnInfo(defaultValue = "0")
    val creadoEn: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val actualizadoEn: Long = System.currentTimeMillis(),

    @ColumnInfo(defaultValue = "NULL")
    val creadoPorUsuarioId: Long? = null,
    @ColumnInfo(defaultValue = "NULL")
    val actualizadoPorUsuarioId: Long? = null,
    @ColumnInfo(defaultValue = "NULL")
    val completadoPorUsuarioId: Long? = null,

    @ColumnInfo(defaultValue = "''")
    val syncId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "'PENDIENTE'")
    val syncEstado: String = "PENDIENTE",
    @ColumnInfo(defaultValue = "0")
    val syncVersion: Long = 0L,
    @ColumnInfo(defaultValue = "''")
    val syncError: String = "",
    @ColumnInfo(defaultValue = "''")
    val organizacionId: String = "",
    @ColumnInfo(defaultValue = "NULL")
    val establecimientoRemotoId: Long? = null,
    @ColumnInfo(defaultValue = "''")
    val eaisId: String = "",
    @ColumnInfo(defaultValue = "''")
    val territorioId: String = "",

    /** Quién creó la ficha en la nube (id de su cuenta) y cómo se llama; vacíos si la creó este teléfono y aún no bajó. */
    @ColumnInfo(defaultValue = "''")
    val autorRemotoId: String = "",
    @ColumnInfo(defaultValue = "''")
    val autorNombre: String = "",
    /** Vacío si la ficha es propia; «LECTOR» o «EDITOR» si otra persona me la compartió. */
    @ColumnInfo(defaultValue = "''")
    val miPermiso: String = "",
    /** A cuántas personas les compartí esta ficha (solo las propias). */
    @ColumnInfo(defaultValue = "0")
    val compartidaConPersonas: Int = 0
)
