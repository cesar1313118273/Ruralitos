package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.ValorRiesgoEntity
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class GrupoDispensarizacion(
    val codigo: String,
    val titulo: String,
    val prioridad: Int
) {
    PENDIENTE("?", "Evaluación pendiente", 0),
    I("I", "Aparentemente sano", 1),
    II("II", "Con factores de riesgo", 2),
    III("III", "Con enfermedad crónica", 3),
    IV("IV", "Con discapacidad", 4)
}

enum class GrupoEdadRiesgo(val etiqueta: String) {
    MENOR_DOS("0 a 23 meses"),
    DOS_NUEVE("2 a 9 años"),
    ADOLESCENTE("10 a 19 años"),
    ADULTO("20 a 64 años"),
    ADULTO_MAYOR("65 años o más"),
    EMBARAZADA("Embarazada"),
    EMBARAZADA_ADOLESCENTE("Embarazada adolescente")
}

/** Pictogramas MAIS históricos y las figuras nuevas de riesgo/edad. */
object IconosMais {
    const val DISCAPACIDAD_VISUAL = "disc_visual"
    const val DISCAPACIDAD_LENGUAJE = "disc_lenguaje"
    const val DISCAPACIDAD_AUDITIVA = "disc_auditiva"
    const val DISCAPACIDAD_FISICA = "disc_fisica"
    const val DISCAPACIDAD_INTELECTUAL = "disc_intelectual"
    const val SALUD_MENTAL = "salud_mental"
    const val EMBARAZO_BAJO_RIESGO = "embarazo_bajo"
    const val EMBARAZO_ALTO_RIESGO = "embarazo_alto"
    const val EMBARAZO_RIESGO = "embarazo_riesgo"
    const val DIABETES = "diabetes"
    const val HIPERTENSION_DIABETES = "hipertension_diabetes"
    const val MENOR_DOS = "menor_2"
    const val OBESIDAD_MENOR_CINCO = "obesidad_menor5"
    const val OBESIDAD_CINCO_ONCE = "obesidad_5_11"
    const val DESNUTRICION_AGUDA = "desnutricion_aguda"
    const val DESNUTRICION_CRONICA = "desnutricion_cronica"
    const val HIPERTENSION = "hipertension"
    const val TUBERCULOSIS = "tuberculosis"
    const val OBESIDAD_ADOLESCENTE = "obesidad_adolescente"
    const val OBESIDAD_ADULTO = "obesidad_adulto"
    const val OBESIDAD_ADULTO_MAYOR = "obesidad_adulto_mayor"
    const val VACUNACION_INCOMPLETA = "vacunacion"
    const val CONSUMO_ALCOHOL_DROGAS = "consumo"
    const val SANEAMIENTO_AMBIENTAL = "saneamiento"
    const val DISCAPACIDAD_FISICA_APOYO = "disc_fisica_apoyo"
    const val CUIDADOS_PALIATIVOS = "paliativos"
    const val VIH = "vih"
    const val RIESGO_I_MENOR_DOS = "riesgo_i_0_23"
    const val RIESGO_I_DOS_NUEVE = "riesgo_i_2_9"
    const val RIESGO_I_EMBARAZO = "riesgo_i_embarazo"
    const val RIESGO_II_MENOR_DOS = "riesgo_ii_0_23"
    const val RIESGO_II_DOS_NUEVE = "riesgo_ii_2_9"
    const val RIESGO_II_ADOLESCENTE = "riesgo_ii_10_19"
    const val RIESGO_II_ADULTO = "riesgo_ii_20_64"
    const val RIESGO_II_ADULTO_MAYOR = "riesgo_ii_65_mas"
    const val RIESGO_II_EMBARAZO = "riesgo_ii_embarazo"
    const val RIESGO_III_MENOR_DOS = "riesgo_iii_0_23"
    const val RIESGO_III_DOS_NUEVE = "riesgo_iii_2_9"
    const val RIESGO_III_ADOLESCENTE = "riesgo_iii_10_19"
    const val RIESGO_III_ADULTO = "riesgo_iii_20_64"
    const val RIESGO_III_ADULTO_MAYOR = "riesgo_iii_65_mas"
    const val RIESGO_III_EMBARAZO = "riesgo_iii_embarazo"
    const val RIESGO_III_EMBARAZO_ADOLESCENTE = "riesgo_iii_embarazo_adolescente"

    val iconosGrupoEdad = listOf(
        RIESGO_I_MENOR_DOS, RIESGO_I_DOS_NUEVE, RIESGO_I_EMBARAZO,
        RIESGO_II_MENOR_DOS, RIESGO_II_DOS_NUEVE, RIESGO_II_ADOLESCENTE,
        RIESGO_II_ADULTO, RIESGO_II_ADULTO_MAYOR, RIESGO_II_EMBARAZO,
        RIESGO_III_MENOR_DOS, RIESGO_III_DOS_NUEVE, RIESGO_III_ADOLESCENTE,
        RIESGO_III_ADULTO, RIESGO_III_ADULTO_MAYOR, RIESGO_III_EMBARAZO,
        RIESGO_III_EMBARAZO_ADOLESCENTE
    )

    val todos = setOf(
        DISCAPACIDAD_VISUAL,
        DISCAPACIDAD_LENGUAJE,
        DISCAPACIDAD_AUDITIVA,
        DISCAPACIDAD_FISICA,
        DISCAPACIDAD_INTELECTUAL,
        SALUD_MENTAL,
        EMBARAZO_BAJO_RIESGO,
        EMBARAZO_ALTO_RIESGO,
        EMBARAZO_RIESGO,
        DIABETES,
        HIPERTENSION_DIABETES,
        MENOR_DOS,
        OBESIDAD_MENOR_CINCO,
        OBESIDAD_CINCO_ONCE,
        DESNUTRICION_AGUDA,
        DESNUTRICION_CRONICA,
        HIPERTENSION,
        TUBERCULOSIS,
        OBESIDAD_ADOLESCENTE,
        OBESIDAD_ADULTO,
        OBESIDAD_ADULTO_MAYOR,
        VACUNACION_INCOMPLETA,
        CONSUMO_ALCOHOL_DROGAS,
        SANEAMIENTO_AMBIENTAL,
        DISCAPACIDAD_FISICA_APOYO,
        CUIDADOS_PALIATIVOS,
        VIH
    ) + iconosGrupoEdad
}

data class PictogramaDispensarizacion(
    val id: String,
    val etiqueta: String
) {
    init {
        require(id in IconosMais.todos) { "El pictograma $id no pertenece al catálogo de Ruralitos." }
    }
}

data class ResultadoDispensarizacion(
    val grupo: GrupoDispensarizacion,
    val razones: List<String>,
    val pictogramas: List<PictogramaDispensarizacion>,
    val camposPendientes: List<String>
) {
    val completa: Boolean get() = camposPendientes.isEmpty()
}

object DispensarizacionAutomatica {
    const val NUTRICION_SIN_ALTERACION = "SIN_ALTERACION"
    const val NUTRICION_OBESIDAD = "OBESIDAD"
    const val NUTRICION_DESNUTRICION_AGUDA = "DESNUTRICION_AGUDA"
    const val NUTRICION_DESNUTRICION_CRONICA = "DESNUTRICION_CRONICA"

    val opcionesNutricion = listOf(
        NUTRICION_SIN_ALTERACION to "Sin alteración",
        NUTRICION_OBESIDAD to "Obesidad",
        NUTRICION_DESNUTRICION_AGUDA to "Desnutrición aguda",
        NUTRICION_DESNUTRICION_CRONICA to "Desnutrición crónica"
    )

    fun clasificar(
        miembro: MiembroFamiliaEntity,
        embarazo: EmbarazadaEntity? = null
    ): ResultadoDispensarizacion {
        val pictogramas = pictogramas(miembro, embarazo)
        val pendientes = camposPendientes(miembro)
        val razonesIv = buildList {
            if (miembro.discapacidadVisual == true) add("Discapacidad visual registrada")
            if (miembro.discapacidadAuditiva == true) add("Discapacidad auditiva registrada")
            if (miembro.discapacidadLenguaje == true) add("Discapacidad del lenguaje registrada")
            if (miembro.discapacidadFisica == true) add("Discapacidad física registrada")
            if (miembro.discapacidadIntelectual == true) add("Discapacidad intelectual registrada")
            if (miembro.discapacidadPsicosocial == true) add("Discapacidad psicosocial registrada")
        }
        val razonesIii = buildList {
            if (miembro.hipertensionArterial == true) add("Hipertensión arterial registrada")
            if (miembro.diabetesMellitus == true) add("Diabetes mellitus registrada")
            if (miembro.tuberculosis == true) add("Tuberculosis registrada")
            if (miembro.problemaSaludMental == true) add("Problema de salud mental registrado")
            if (miembro.enfermedadCronica == true) add("Otra enfermedad crónica registrada")
            if (miembro.cuidadosPaliativos == true) add("Cuidados paliativos registrados")
            if (miembro.vih == true) add("VIH registrado")
        }
        val razonesIi = buildList {
            if (miembro.vacunasCompletas == false) add("Esquema de vacunación incompleto")
            if (miembro.saludBucalAdecuada == false) add("Salud bucal que requiere seguimiento")
            when (miembro.estadoNutricional) {
                NUTRICION_OBESIDAD -> add("Obesidad registrada")
                NUTRICION_DESNUTRICION_AGUDA -> add("Desnutrición aguda registrada")
                NUTRICION_DESNUTRICION_CRONICA -> add("Desnutrición crónica registrada")
            }
            if (miembro.consumoAlcoholDrogas == true) add("Consumo de alcohol u otras drogas registrado")
            if (embarazo != null) {
                if (embarazo.antecedentesPatologicosObstetricos.isNotBlank()) {
                    add("Embarazo con antecedente obstétrico registrado")
                }
                if (edadEnAnios(miembro.fechaNacimiento)?.let { it in 10..19 } == true) {
                    add("Embarazo adolescente registrado")
                }
            }
            if (miembro.riesgoEnfermedadDiscapacidad.isNotBlank()) {
                add("Antecedente de texto libre pendiente de validación clínica")
            }
        }

        val (grupo, razones) = when {
            razonesIv.isNotEmpty() -> GrupoDispensarizacion.IV to razonesIv
            razonesIii.isNotEmpty() -> GrupoDispensarizacion.III to razonesIii
            razonesIi.isNotEmpty() -> GrupoDispensarizacion.II to razonesIi
            pendientes.isEmpty() -> GrupoDispensarizacion.I to listOf("Evaluación registrada sin riesgos, enfermedad crónica ni discapacidad")
            else -> GrupoDispensarizacion.PENDIENTE to listOf("Faltan datos clínicos para asignar un grupo de forma segura")
        }
        val iconoGrupo = iconoGrupoEdad(grupo, categoriaGrupoEdad(miembro, embarazo))
        return ResultadoDispensarizacion(
            grupo, razones, (listOfNotNull(iconoGrupo) + pictogramas).distinctBy { it.id }, pendientes
        )
    }

    fun categoriaGrupoEdad(
        miembro: MiembroFamiliaEntity,
        embarazo: EmbarazadaEntity? = null,
        hoy: Date = Date()
    ): GrupoEdadRiesgo? {
        val meses = edadEnMeses(miembro.fechaNacimiento, hoy)
        if (embarazo != null) {
            return if (meses != null && meses in 120..239) {
                GrupoEdadRiesgo.EMBARAZADA_ADOLESCENTE
            } else {
                GrupoEdadRiesgo.EMBARAZADA
            }
        }
        return when (meses) {
            null -> null
            in 0..23 -> GrupoEdadRiesgo.MENOR_DOS
            in 24..119 -> GrupoEdadRiesgo.DOS_NUEVE
            in 120..239 -> GrupoEdadRiesgo.ADOLESCENTE
            in 240..779 -> GrupoEdadRiesgo.ADULTO
            else -> GrupoEdadRiesgo.ADULTO_MAYOR
        }
    }

    fun iconoGrupoEdad(
        grupo: GrupoDispensarizacion,
        categoria: GrupoEdadRiesgo?
    ): PictogramaDispensarizacion? {
        val id = when (grupo) {
            GrupoDispensarizacion.I -> when (categoria) {
                GrupoEdadRiesgo.MENOR_DOS -> IconosMais.RIESGO_I_MENOR_DOS
                GrupoEdadRiesgo.DOS_NUEVE -> IconosMais.RIESGO_I_DOS_NUEVE
                GrupoEdadRiesgo.EMBARAZADA, GrupoEdadRiesgo.EMBARAZADA_ADOLESCENTE -> IconosMais.RIESGO_I_EMBARAZO
                else -> null
            }
            GrupoDispensarizacion.II -> when (categoria) {
                GrupoEdadRiesgo.MENOR_DOS -> IconosMais.RIESGO_II_MENOR_DOS
                GrupoEdadRiesgo.DOS_NUEVE -> IconosMais.RIESGO_II_DOS_NUEVE
                GrupoEdadRiesgo.ADOLESCENTE -> IconosMais.RIESGO_II_ADOLESCENTE
                GrupoEdadRiesgo.ADULTO -> IconosMais.RIESGO_II_ADULTO
                GrupoEdadRiesgo.ADULTO_MAYOR -> IconosMais.RIESGO_II_ADULTO_MAYOR
                GrupoEdadRiesgo.EMBARAZADA, GrupoEdadRiesgo.EMBARAZADA_ADOLESCENTE -> IconosMais.RIESGO_II_EMBARAZO
                else -> null
            }
            GrupoDispensarizacion.III -> when (categoria) {
                GrupoEdadRiesgo.MENOR_DOS -> IconosMais.RIESGO_III_MENOR_DOS
                GrupoEdadRiesgo.DOS_NUEVE -> IconosMais.RIESGO_III_DOS_NUEVE
                GrupoEdadRiesgo.ADOLESCENTE -> IconosMais.RIESGO_III_ADOLESCENTE
                GrupoEdadRiesgo.ADULTO -> IconosMais.RIESGO_III_ADULTO
                GrupoEdadRiesgo.ADULTO_MAYOR -> IconosMais.RIESGO_III_ADULTO_MAYOR
                GrupoEdadRiesgo.EMBARAZADA -> IconosMais.RIESGO_III_EMBARAZO
                GrupoEdadRiesgo.EMBARAZADA_ADOLESCENTE -> IconosMais.RIESGO_III_EMBARAZO_ADOLESCENTE
                else -> null
            }
            else -> null
        } ?: return null
        return PictogramaDispensarizacion(id, "Grupo ${grupo.codigo}: ${categoria?.etiqueta.orEmpty()}")
    }

    fun buscarEmbarazo(
        miembro: MiembroFamiliaEntity,
        embarazadas: List<EmbarazadaEntity>
    ): EmbarazadaEntity? {
        val nombreMiembro = normalizar(miembro.apellidosNombres)
        if (nombreMiembro.isBlank()) return null
        return embarazadas.firstOrNull { embarazo ->
            val nombreEmbarazo = normalizar(embarazo.apellidosNombres)
            nombreEmbarazo == nombreMiembro ||
                (nombreEmbarazo.length >= 8 && nombreMiembro.contains(nombreEmbarazo)) ||
                (nombreMiembro.length >= 8 && nombreEmbarazo.contains(nombreMiembro))
        }
    }

    fun pictogramasRiesgoFamiliar(
        valores: List<ValorRiesgoEntity>
    ): List<PictogramaDispensarizacion> {
        val riesgo = valores.associate { it.componente to it.valor }
        return buildList {
            if ((riesgo[1] ?: 0) > 0) {
                add(PictogramaDispensarizacion(
                    IconosMais.VACUNACION_INCOMPLETA,
                    "Niños y niñas con esquema de vacunación incompleta"
                ))
            }
            when (riesgo[4] ?: 0) {
                1 -> add(PictogramaDispensarizacion(
                    IconosMais.EMBARAZO_BAJO_RIESGO,
                    "Mujer embarazada de bajo riesgo"
                ))
                2 -> add(PictogramaDispensarizacion(
                    IconosMais.EMBARAZO_ALTO_RIESGO,
                    "Mujer embarazada de alto riesgo"
                ))
                in 3..4 -> add(PictogramaDispensarizacion(
                    IconosMais.EMBARAZO_RIESGO,
                    "Mujer embarazada en riesgo"
                ))
            }
            if ((riesgo[6] ?: 0) > 0) {
                add(PictogramaDispensarizacion(
                    IconosMais.SALUD_MENTAL,
                    "Personas con problemas de salud mental"
                ))
            }
            if (listOf(7, 8, 9).any { (riesgo[it] ?: 0) > 0 }) {
                add(PictogramaDispensarizacion(
                    IconosMais.SANEAMIENTO_AMBIENTAL,
                    "Falta de servicios de saneamiento ambiental"
                ))
            }
            if ((riesgo[16] ?: 0) > 0) {
                add(PictogramaDispensarizacion(
                    IconosMais.CONSUMO_ALCOHOL_DROGAS,
                    "Personas con consumo de alcohol y otras drogas"
                ))
            }
        }
    }

    private fun camposPendientes(miembro: MiembroFamiliaEntity): List<String> = buildList {
        if (miembro.vacunasCompletas == null) add("vacunación")
        if (miembro.saludBucalAdecuada == null) add("salud bucal")
        if (miembro.estadoNutricional.isBlank()) add("estado nutricional")
        if (miembro.hipertensionArterial == null) add("hipertensión")
        if (miembro.diabetesMellitus == null) add("diabetes")
        if (miembro.tuberculosis == null) add("tuberculosis")
        if (miembro.problemaSaludMental == null) add("salud mental")
        if (miembro.consumoAlcoholDrogas == null) add("alcohol u otras drogas")
        if (miembro.enfermedadCronica == null) add("otras enfermedades crónicas")
        if (
            miembro.discapacidadVisual == null || miembro.discapacidadAuditiva == null ||
            miembro.discapacidadLenguaje == null || miembro.discapacidadFisica == null ||
            miembro.discapacidadIntelectual == null
        ) add("discapacidades")
    }

    private fun pictogramas(
        miembro: MiembroFamiliaEntity,
        embarazo: EmbarazadaEntity?
    ): List<PictogramaDispensarizacion> = buildList {
        val edad = edadEnAnios(miembro.fechaNacimiento)
        if (edad != null && edad < 2) {
            add(PictogramaDispensarizacion(
                IconosMais.MENOR_DOS,
                "Niños y niñas menores de 2 años"
            ))
        }
        if (miembro.discapacidadVisual == true) add(PictogramaDispensarizacion(
            IconosMais.DISCAPACIDAD_VISUAL,
            "Personas con discapacidad visual"
        ))
        if (miembro.discapacidadLenguaje == true) add(PictogramaDispensarizacion(
            IconosMais.DISCAPACIDAD_LENGUAJE,
            "Personas con discapacidad del lenguaje"
        ))
        if (miembro.discapacidadAuditiva == true) add(PictogramaDispensarizacion(
            IconosMais.DISCAPACIDAD_AUDITIVA,
            "Personas con discapacidad auditiva"
        ))
        if (miembro.discapacidadFisica == true) add(PictogramaDispensarizacion(
            if (miembro.necesitaAyudaTecnica == true) IconosMais.DISCAPACIDAD_FISICA_APOYO
            else IconosMais.DISCAPACIDAD_FISICA,
            if (miembro.necesitaAyudaTecnica == true) "Discapacidad física con apoyo"
            else "Personas con discapacidad física"
        ))
        if (miembro.discapacidadIntelectual == true) add(PictogramaDispensarizacion(
            IconosMais.DISCAPACIDAD_INTELECTUAL,
            "Personas con discapacidad intelectual"
        ))
        if (miembro.problemaSaludMental == true) add(PictogramaDispensarizacion(
            IconosMais.SALUD_MENTAL,
            "Personas con problemas de salud mental"
        ))
        embarazo?.let { add(pictogramaEmbarazo(it)) }
        when {
            miembro.hipertensionArterial == true && miembro.diabetesMellitus == true ->
                add(PictogramaDispensarizacion(
                    IconosMais.HIPERTENSION_DIABETES,
                    "Personas con hipertensión arterial y diabetes mellitus"
                ))
            miembro.diabetesMellitus == true -> add(PictogramaDispensarizacion(
                IconosMais.DIABETES,
                "Personas con diabetes mellitus"
            ))
            miembro.hipertensionArterial == true -> add(PictogramaDispensarizacion(
                IconosMais.HIPERTENSION,
                "Personas con hipertensión arterial"
            ))
        }
        if (miembro.tuberculosis == true) add(PictogramaDispensarizacion(
            IconosMais.TUBERCULOSIS,
            "Personas con tuberculosis"
        ))
        if (miembro.cuidadosPaliativos == true) add(PictogramaDispensarizacion(
            IconosMais.CUIDADOS_PALIATIVOS,
            "Personas que reciben cuidados paliativos"
        ))
        if (miembro.vih == true) add(PictogramaDispensarizacion(
            IconosMais.VIH,
            "Personas con VIH"
        ))
        if (miembro.estadoNutricional == NUTRICION_OBESIDAD) {
            pictogramaObesidad(edad)?.let(::add)
        }
        if (edad != null && edad < 5 && miembro.estadoNutricional == NUTRICION_DESNUTRICION_AGUDA) {
            add(PictogramaDispensarizacion(
                IconosMais.DESNUTRICION_AGUDA,
                "Desnutrición aguda de niños y niñas menores de 5 años"
            ))
        }
        if (miembro.estadoNutricional == NUTRICION_DESNUTRICION_CRONICA) {
            add(PictogramaDispensarizacion(
                IconosMais.DESNUTRICION_CRONICA,
                "Desnutrición crónica"
            ))
        }
        if (edad != null && edad < 18 && miembro.vacunasCompletas == false) {
            add(PictogramaDispensarizacion(
                IconosMais.VACUNACION_INCOMPLETA,
                "Niños y niñas con esquema de vacunación incompleta"
            ))
        }
        if (miembro.consumoAlcoholDrogas == true) add(PictogramaDispensarizacion(
            IconosMais.CONSUMO_ALCOHOL_DROGAS,
            "Personas con consumo de alcohol y otras drogas"
        ))
    }.distinctBy { it.id }

    private fun pictogramaEmbarazo(embarazo: EmbarazadaEntity): PictogramaDispensarizacion {
        val antecedentes = normalizar(embarazo.antecedentesPatologicosObstetricos)
        val riesgoEvidente = listOf(
            "alto riesgo", "preeclampsia", "eclampsia", "hemorragia", "amenaza",
            "placenta previa", "embarazo multiple", "diabetes gestacional", "hipertension"
        ).any(antecedentes::contains)
        return when {
            riesgoEvidente -> PictogramaDispensarizacion(
                IconosMais.EMBARAZO_RIESGO,
                "Mujer embarazada en riesgo"
            )
            antecedentes.isNotBlank() -> PictogramaDispensarizacion(
                IconosMais.EMBARAZO_ALTO_RIESGO,
                "Mujer embarazada de alto riesgo"
            )
            else -> PictogramaDispensarizacion(
                IconosMais.EMBARAZO_BAJO_RIESGO,
                "Mujer embarazada de bajo riesgo"
            )
        }
    }

    private fun pictogramaObesidad(edad: Int?): PictogramaDispensarizacion? = when (edad) {
        null -> null
        in 0..4 -> PictogramaDispensarizacion(
            IconosMais.OBESIDAD_MENOR_CINCO,
            "Obesidad en niños y niñas menores de 5 años"
        )
        in 5..11 -> PictogramaDispensarizacion(
            IconosMais.OBESIDAD_CINCO_ONCE,
            "Obesidad en niños y niñas en edad escolar de 5 a 11 años"
        )
        in 12..19 -> PictogramaDispensarizacion(
            IconosMais.OBESIDAD_ADOLESCENTE,
            "Obesidad en adolescentes de 10 a 19 años"
        )
        in 20..64 -> PictogramaDispensarizacion(
            IconosMais.OBESIDAD_ADULTO,
            "Obesidad en personas adultas jóvenes de 20 a 64 años"
        )
        else -> PictogramaDispensarizacion(
            IconosMais.OBESIDAD_ADULTO_MAYOR,
            "Obesidad en personas adultas mayores de 65 años y más"
        )
    }

    private fun normalizar(texto: String): String = Normalizer.normalize(
        texto.trim().lowercase(Locale.getDefault()),
        Normalizer.Form.NFD
    ).replace("\\p{Mn}+".toRegex(), "").replace("[^a-z0-9 ]".toRegex(), "")
        .replace("\\s+".toRegex(), " ")

    private fun edadEnAnios(fechaNacimiento: String, hoy: Date = Date()): Int? =
        edadEnMeses(fechaNacimiento, hoy)?.div(12)

    private fun edadEnMeses(fechaNacimiento: String, hoy: Date = Date()): Int? {
        val fecha = runCatching {
            SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply { isLenient = false }
                .parse(fechaNacimiento)
        }.getOrNull() ?: return null
        if (fecha.after(hoy)) return null
        val nacimiento = Calendar.getInstance().apply { time = fecha }
        val referencia = Calendar.getInstance().apply { time = hoy }
        var meses = (referencia.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR)) * 12 +
            referencia.get(Calendar.MONTH) - nacimiento.get(Calendar.MONTH)
        if (referencia.get(Calendar.DAY_OF_MONTH) < nacimiento.get(Calendar.DAY_OF_MONTH)) meses--
        return meses.coerceAtLeast(0)
    }
}
