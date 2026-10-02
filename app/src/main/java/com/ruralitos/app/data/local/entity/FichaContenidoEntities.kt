package com.ruralitos.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.UUID

@Entity(
    tableName = "miembros_familia",
    foreignKeys = [ForeignKey(
        entity = FichaFamiliarEntity::class,
        parentColumns = ["id"],
        childColumns = ["fichaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("fichaId"), Index("cedula"), Index(value = ["syncId"], unique = true)]
)
data class MiembroFamiliaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fichaId: Long,
    val grupoEdad: String,
    val apellidosNombres: String,
    val parentesco: String,
    val fechaNacimiento: String,
    val ocupacion: String,
    val sexo: String,
    val escolaridad: String,
    val vacunasCompletas: Boolean? = null,
    val saludBucalAdecuada: Boolean? = null,
    val riesgoEnfermedadDiscapacidad: String = "",
    @ColumnInfo(defaultValue = "''") val estadoNutricional: String = "",
    @ColumnInfo(defaultValue = "NULL") val hipertensionArterial: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val diabetesMellitus: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val tuberculosis: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val problemaSaludMental: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val consumoAlcoholDrogas: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val enfermedadCronica: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val discapacidadVisual: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val discapacidadAuditiva: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val discapacidadLenguaje: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val discapacidadFisica: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val discapacidadIntelectual: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val discapacidadPsicosocial: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val cuidadosPaliativos: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val vih: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val eventoSalud: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val casoConfirmado: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val casoSospechosoUno: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val casoSospechosoDos: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val prestadorComunitario: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val parteroAncestral: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val sabiduriaAncestral: Boolean? = null,
    /** JSON compacto de diagnósticos seleccionados del catálogo CIE-10 incluido en la app. */
    @ColumnInfo(defaultValue = "'[]'") val comorbilidadesCie10Json: String = "[]",
    @ColumnInfo(defaultValue = "NULL") val porcentajeDiscapacidad: Int? = null,
    @ColumnInfo(defaultValue = "NULL") val necesitaAyudaTecnica: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val enfermedadCronicaDescompensada: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val riesgoGenetico: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val victimaViolencia: Boolean? = null,
    @ColumnInfo(defaultValue = "NULL") val privadoLibertad: Boolean? = null,
    val numeroHistoriaClinica: String = "",
    val cedula: String = "",
    @ColumnInfo(defaultValue = "''") val syncId: String = UUID.randomUUID().toString()
)

@Entity(
    tableName = "embarazadas",
    foreignKeys = [ForeignKey(
        entity = FichaFamiliarEntity::class,
        parentColumns = ["id"],
        childColumns = ["fichaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("fichaId"), Index(value = ["syncId"], unique = true)]
)
data class EmbarazadaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fichaId: Long,
    val apellidosNombres: String,
    val fechaUltimaMenstruacion: String,
    val fechaProbableParto: String,
    val semanasGestacion: Int? = null,
    val dosisDtPrimera: Boolean = false,
    val dosisDtSegunda: Boolean = false,
    val dosisDtRefuerzo: Boolean = false,
    val gestas: Int? = null,
    val partos: Int? = null,
    val abortos: Int? = null,
    val cesareas: Int? = null,
    val antecedentesPatologicosObstetricos: String = "",
    @ColumnInfo(defaultValue = "''") val riesgoObstetrico: String = "",
    @ColumnInfo(defaultValue = "''") val syncId: String = UUID.randomUUID().toString()
)

@Entity(
    tableName = "mortalidad_familiar",
    foreignKeys = [ForeignKey(
        entity = FichaFamiliarEntity::class,
        parentColumns = ["id"],
        childColumns = ["fichaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("fichaId"), Index(value = ["syncId"], unique = true)]
)
data class MortalidadFamiliarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fichaId: Long,
    val nombre: String,
    val parentesco: String,
    val edadAlFallecer: Int? = null,
    val causa: String,
    @ColumnInfo(defaultValue = "''") val syncId: String = UUID.randomUUID().toString()
)

@Entity(
    tableName = "calificaciones_riesgo",
    foreignKeys = [ForeignKey(
        entity = FichaFamiliarEntity::class,
        parentColumns = ["id"],
        childColumns = ["fichaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("fichaId"), Index(value = ["syncId"], unique = true)]
)
data class CalificacionRiesgoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fichaId: Long,
    val fechaCalificacion: String,
    val responsable: String,
    val total: Int = 0,
    val nivel: String = "SIN_RIESGO",
    @ColumnInfo(defaultValue = "''") val syncId: String = UUID.randomUUID().toString()
)

@Entity(
    tableName = "valores_riesgo",
    foreignKeys = [ForeignKey(
        entity = CalificacionRiesgoEntity::class,
        parentColumns = ["id"],
        childColumns = ["calificacionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("calificacionId"), Index(value = ["calificacionId", "componente"], unique = true), Index(value = ["syncId"], unique = true)]
)
data class ValorRiesgoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val calificacionId: Long,
    val componente: Int,
    val valor: Int,
    @ColumnInfo(defaultValue = "''") val syncId: String = UUID.randomUUID().toString()
)

@Entity(
    tableName = "gestion_riesgo",
    foreignKeys = [ForeignKey(
        entity = FichaFamiliarEntity::class,
        parentColumns = ["id"],
        childColumns = ["fichaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("fichaId"), Index(value = ["syncId"], unique = true)]
)
data class GestionRiesgoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fichaId: Long,
    val fechaAnalisis: String,
    val numero: Int? = null,
    val compromisoFamilia: String,
    val compromisoEquipoSalud: String,
    val fechaEvaluacion: String = "",
    val cumplimiento: String = "PENDIENTE",
    val causasIncumplimientoObservaciones: String = "",
    val responsable: String = "",
    @ColumnInfo(defaultValue = "''") val syncId: String = UUID.randomUUID().toString()
)

@Entity(
    tableName = "contaminacion_ambiental",
    foreignKeys = [ForeignKey(
        entity = FichaFamiliarEntity::class,
        parentColumns = ["id"],
        childColumns = ["fichaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("fichaId"), Index(value = ["syncId"], unique = true)]
)
data class ContaminacionAmbientalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fichaId: Long,
    val fechaInforme: String,
    val tipoContaminanteDescripcion: String,
    val causanteContaminacion: String,
    @ColumnInfo(defaultValue = "''") val syncId: String = UUID.randomUUID().toString()
)

@Entity(
    tableName = "lugares_tratamiento",
    foreignKeys = [ForeignKey(
        entity = FichaFamiliarEntity::class,
        parentColumns = ["id"],
        childColumns = ["fichaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("fichaId"), Index(value = ["syncId"], unique = true)]
)
data class LugarTratamientoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fichaId: Long,
    val descripcion: String,
    @ColumnInfo(defaultValue = "''") val syncId: String = UUID.randomUUID().toString()
)

@Entity(
    tableName = "adjuntos_ficha",
    foreignKeys = [ForeignKey(
        entity = FichaFamiliarEntity::class,
        parentColumns = ["id"],
        childColumns = ["fichaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("fichaId"), Index(value = ["fichaId", "tipo"], unique = true), Index(value = ["syncId"], unique = true)]
)
data class AdjuntoFichaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fichaId: Long,
    val tipo: String,
    val uri: String,
    val actualizadoEn: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val syncId: String = UUID.randomUUID().toString()
)
