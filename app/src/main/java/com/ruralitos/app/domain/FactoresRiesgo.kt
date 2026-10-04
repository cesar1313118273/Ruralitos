package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity

/** Lista de códigos guardada como texto: `["A","B"]`. Solo letras mayúsculas, dígitos y guion bajo. */
internal object ListaDeCodigos {
    private val patron = Regex("\"([A-Z0-9_]+)\"")
    fun codificar(codigos: Collection<String>): String =
        codigos.filter { it.isNotBlank() }.distinct().sorted().joinToString(",", "[", "]") { "\"$it\"" }
    fun decodificar(texto: String): Set<String> = patron.findAll(texto).map { it.groupValues[1] }.toSet()
}

data class FactorRiesgoEdad(val codigo: String, val etiqueta: String)

/**
 * Factores de riesgo (Grupo II) por grupo de edad, tomados del manual MAIS-FCI del MSP y de la lista de la unidad.
 * La lista que se muestra depende de la edad actual: si la persona cambia de grupo, lo marcado antes se conserva pero
 * ya no cuenta.
 */
object FactoresRiesgoEdad {
    const val VIOLENCIA = "VIOLENCIA"
    const val CONSUMO = "CONSUMO"

    private fun f(codigo: String, etiqueta: String) = FactorRiesgoEdad(codigo, etiqueta)

    private val violencia = f(VIOLENCIA, "Violencia intrafamiliar, maltrato o negligencia")
    private val accidentes = f("ACCIDENTES", "Riesgo de accidentes")
    private val sedentarismo = f("SEDENTARISMO", "Sedentarismo")
    private val acoso = f("ACOSO_ESCOLAR", "Acoso escolar")
    private val intento = f("INTENTO_SUICIDA", "Intento autolítico")
    private val conductaSexual = f("CONDUCTA_SEXUAL_RIESGO", "Conducta sexual de riesgo")
    private val ansiedad = f("ANSIEDAD_DEPRESION", "Ansiedad o depresión moderada")
    private val consumo = f(CONSUMO, "Consumo problemático de alcohol o drogas")
    private val tabaquismo = f("TABAQUISMO", "Tabaquismo")

    private val porBanda: Map<GrupoEdadRiesgo, List<FactorRiesgoEdad>> = mapOf(
        GrupoEdadRiesgo.MENOR_DOS to listOf(
            f("CONTROLES_PRENATALES", "Controles prenatales insuficientes de la madre"),
            f("PESO_BAJO", "Bajo peso al nacer (menos de 2.500 g)"),
            f("PESO_LIMITE", "Peso al nacer de 2.500 a 3.000 g"),
            f("PRETERMINO", "Parto pretérmino (menos de 37 semanas)"),
            f("GEMELAR", "Gemelar o más"),
            f("MADRE_ADOLESCENTE", "Madre adolescente"),
            f("MADRE_ENFERMA", "Enfermedad grave o crónica de la madre"),
            f("LACTANCIA", "Dificultad o rechazo a la lactancia materna"),
            f("DIARREAS_RESPIRATORIAS", "Diarreas o infecciones respiratorias recurrentes"),
            f("DESARROLLO", "Problemas del desarrollo (lenguaje, motricidad, social)"),
            f("HOGAR_SUSTANCIAS", "Alcohol, drogas o fumador en el hogar"),
            violencia,
            accidentes
        ),
        GrupoEdadRiesgo.DOS_NUEVE to listOf(
            f("DIARREAS_RESPIRATORIAS", "Diarreas o infecciones respiratorias recurrentes"),
            sedentarismo,
            f("CONTROLES_SALUD", "Controles de salud insuficientes"),
            acoso,
            f("PANTALLAS", "Más de 2 horas al día en dispositivos electrónicos"),
            f("DESARROLLO", "Problemas del desarrollo o del aprendizaje"),
            f("HOGAR_SUSTANCIAS", "Alcohol, drogas o fumador en el hogar"),
            f("TRABAJO_INFANTIL", "Trabajo infantil"),
            f("ANEMIA_PARASITOSIS", "Anemia o parasitosis recurrente"),
            violencia,
            accidentes
        ),
        GrupoEdadRiesgo.ADOLESCENTE to listOf(
            sedentarismo,
            f("INICIO_SEXUAL_PRECOZ", "Inicio precoz de actividad sexual"),
            conductaSexual,
            consumo,
            tabaquismo,
            intento,
            ansiedad,
            acoso,
            f("DESERCION_ESCOLAR", "Deserción escolar"),
            violencia,
            accidentes
        ),
        GrupoEdadRiesgo.ADULTO to listOf(
            f("ANTECEDENTES_FAMILIARES", "Antecedentes familiares de enfermedad crónica"),
            sedentarismo,
            tabaquismo,
            consumo,
            f("MEDICAMENTOS_INADECUADOS", "Uso inadecuado de medicamentos"),
            conductaSexual,
            intento,
            ansiedad,
            f("RIESGO_LABORAL", "Riesgos laborales (esfuerzo, agroquímicos)"),
            violencia
        ),
        GrupoEdadRiesgo.ADULTO_MAYOR to listOf(
            f("RIESGO_CAIDA", "Riesgo de caída"),
            f("FRAGILIDAD", "Fragilidad"),
            f("DETERIORO_FUNCIONAL", "Deterioro funcional o cognitivo, dependencia"),
            f("ABANDONO", "Condición de abandono o mendicidad"),
            f("SIN_REDES_APOYO", "Sin redes de apoyo"),
            f("VIVE_SOLO", "Vive solo"),
            f("POLIFARMACIA", "Usa varios medicamentos a la vez"),
            f("DEFICIT_SENSORIAL", "Problemas de visión u oído"),
            ansiedad,
            f(CONSUMO, "Consumo de alcohol o tabaco"),
            f(VIOLENCIA, "Maltrato o violencia")
        )
    )

    /** Grupo de edad de la persona para esta lista (las embarazadas tienen además su propia lista). */
    fun banda(miembro: MiembroFamiliaEntity): GrupoEdadRiesgo? =
        DispensarizacionAutomatica.categoriaGrupoEdad(miembro, null)

    /** Grupo de edad a partir de la fecha de nacimiento escrita en el formulario (dd/MM/yyyy). */
    fun bandaPorFecha(fecha: String): GrupoEdadRiesgo? = DispensarizacionAutomatica.categoriaGrupoEdad(
        MiembroFamiliaEntity(
            fichaId = 0, grupoEdad = "", apellidosNombres = "", parentesco = "", fechaNacimiento = fecha,
            ocupacion = "", sexo = "", escolaridad = ""
        ),
        null
    )

    fun disponibles(banda: GrupoEdadRiesgo?): List<FactorRiesgoEdad> = banda?.let(porBanda::get).orEmpty()

    fun codificar(codigos: Collection<String>): String = ListaDeCodigos.codificar(codigos)
    fun decodificar(texto: String): Set<String> = ListaDeCodigos.decodificar(texto)

    /** Lo marcado que corresponde a la edad actual. */
    fun vigentes(miembro: MiembroFamiliaEntity): List<FactorRiesgoEdad> {
        val marcados = decodificar(miembro.factoresRiesgoEdadJson)
        return disponibles(banda(miembro)).filter { it.codigo in marcados }
    }

    fun hayConsumo(miembro: MiembroFamiliaEntity) = vigentes(miembro).any { it.codigo == CONSUMO }
    fun hayViolencia(miembro: MiembroFamiliaEntity) = vigentes(miembro).any { it.codigo == VIOLENCIA }
}

/** Enfermedades y alertas que se calculan solas a partir de los diagnósticos CIE-10 de la persona. */
object EstrategiasDesdeCie10 {
    private val patronCodigo = Regex("\"codigo\"\\s*:\\s*\"([^\"]+)\"")

    /** Códigos de los diagnósticos guardados en la ficha de la persona (comorbilidadesCie10Json). */
    fun codigos(json: String): List<String> = patronCodigo.findAll(json).map { it.groupValues[1] }.toList()

    private fun cualquiera(codigos: Collection<String>, vararg patrones: String): Boolean {
        val regex = patrones.map { Regex(it) }
        return codigos.map { it.uppercase().replace(".", "") }.any { c -> regex.any { it.matches(c) } }
    }

    fun hipertension(codigos: Collection<String>) = cualquiera(codigos, "I1[0-5].*")
    fun diabetes(codigos: Collection<String>) = cualquiera(codigos, "E1[0-4].*")
    fun tuberculosis(codigos: Collection<String>) = cualquiera(codigos, "A1[5-9].*")
    fun vih(codigos: Collection<String>) = cualquiera(codigos, "B2[0-4].*", "Z21.*")
    fun saludMental(codigos: Collection<String>) = cualquiera(codigos, "F\\d\\d.*")
    fun cuidadosPaliativos(codigos: Collection<String>) = cualquiera(codigos, "Z515.*")

    /** Las alertas epidemiológicas solo se piden con tuberculosis o VIH. */
    fun pideAlertasEpidemiologicas(codigos: Collection<String>) = tuberculosis(codigos) || vih(codigos)
}
