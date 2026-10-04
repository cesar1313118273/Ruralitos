package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import java.text.Normalizer
import java.util.Locale

/** Lista de códigos guardada como texto: `["A","B"]`. Solo letras mayúsculas, dígitos y guion bajo. */
internal object ListaDeCodigos {
    private val patron = Regex("\"([A-Z0-9_]+)\"")
    fun codificar(codigos: Collection<String>): String =
        codigos.filter { it.isNotBlank() }.distinct().sorted().joinToString(",", "[", "]") { "\"$it\"" }
    fun decodificar(texto: String): Set<String> = patron.findAll(texto).map { it.groupValues[1] }.toSet()
}

/** Texto sin tildes ni mayúsculas, para buscar mientras se escribe. */
internal fun textoParaBuscar(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()

/**
 * Un factor de riesgo que se puede elegir escribiendo. [grupoIII] marca los que por definición pertenecen al Grupo III
 * (consumo problemático de sustancias, riesgo suicida): esos no son un factor del Grupo II.
 */
data class FactorRiesgoEdad(val codigo: String, val etiqueta: String, val grupoIII: Boolean = false)

/**
 * Factores de riesgo por grupo de edad, tomados del manual MAIS-FCI del MSP y de la lista de la unidad. Los de Grupo II
 * solo cuentan si la persona no tiene nada de un grupo superior. La lista depende de la edad actual: si la persona
 * cambia de grupo, lo elegido antes se conserva pero ya no cuenta.
 */
object FactoresRiesgoEdad {
    const val VIOLENCIA = "VIOLENCIA"
    const val CONSUMO_ALCOHOL = "CONSUMO_ALCOHOL"
    const val CONSUMO_DROGAS = "CONSUMO_DROGAS"
    const val RIESGO_SUICIDA = "RIESGO_SUICIDA"
    const val INTENTO_AUTOLITICO = "INTENTO_AUTOLITICO"

    /** Códigos de la primera versión de la lista: siguen valiendo para quien ya los tenía marcados (son del Grupo III). */
    private val heredados = mapOf(
        "CONSUMO" to FactorRiesgoEdad("CONSUMO", "Consumo problemático de alcohol o drogas", grupoIII = true),
        "INTENTO_SUICIDA" to FactorRiesgoEdad("INTENTO_SUICIDA", "Intento autolítico", grupoIII = true)
    )
    private val bandasConSustancias = setOf(GrupoEdadRiesgo.ADOLESCENTE, GrupoEdadRiesgo.ADULTO, GrupoEdadRiesgo.ADULTO_MAYOR)

    private fun f(codigo: String, etiqueta: String, grupoIII: Boolean = false) = FactorRiesgoEdad(codigo, etiqueta, grupoIII)

    private val violencia = f(VIOLENCIA, "Violencia intrafamiliar, maltrato o negligencia")
    private val accidentes = f("ACCIDENTES", "Riesgo de accidentes")
    private val sedentarismo = f("SEDENTARISMO", "Sedentarismo")
    private val acoso = f("ACOSO_ESCOLAR", "Acoso escolar")
    private val conductaSexual = f("CONDUCTA_SEXUAL_RIESGO", "Conducta sexual de riesgo")
    private val ansiedad = f("ANSIEDAD_DEPRESION", "Ansiedad o depresión moderada")
    private val tabaquismo = f("TABAQUISMO", "Tabaquismo")
    private val alcohol = f(CONSUMO_ALCOHOL, "Consumo problemático de alcohol", grupoIII = true)
    private val drogas = f(CONSUMO_DROGAS, "Consumo problemático de drogas", grupoIII = true)
    private val suicida = f(RIESGO_SUICIDA, "Riesgo suicida", grupoIII = true)
    private val intento = f(INTENTO_AUTOLITICO, "Intento autolítico", grupoIII = true)

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
            tabaquismo,
            ansiedad,
            acoso,
            f("DESERCION_ESCOLAR", "Deserción escolar"),
            violencia,
            accidentes,
            alcohol, drogas, suicida, intento
        ),
        GrupoEdadRiesgo.ADULTO to listOf(
            f("ANTECEDENTES_FAMILIARES", "Antecedentes familiares de enfermedad crónica"),
            sedentarismo,
            tabaquismo,
            f("MEDICAMENTOS_INADECUADOS", "Uso inadecuado de medicamentos"),
            conductaSexual,
            ansiedad,
            f("RIESGO_LABORAL", "Riesgos laborales (esfuerzo, agroquímicos)"),
            violencia,
            alcohol, drogas, suicida, intento
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
            tabaquismo,
            violencia,
            alcohol, drogas, suicida, intento
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

    /** Los factores que coinciden con lo escrito (sin tildes ni mayúsculas) y que aún no se eligieron. */
    fun sugerencias(banda: GrupoEdadRiesgo?, escrito: String, elegidos: Set<String>, maximo: Int = 8): List<FactorRiesgoEdad> {
        val buscado = textoParaBuscar(escrito)
        if (buscado.isEmpty()) return emptyList()
        return disponibles(banda).filter { it.codigo !in elegidos && textoParaBuscar(it.etiqueta).contains(buscado) }.take(maximo)
    }

    fun codificar(codigos: Collection<String>): String = ListaDeCodigos.codificar(codigos)
    fun decodificar(texto: String): Set<String> = ListaDeCodigos.decodificar(texto)

    /** Lo elegido que corresponde a la edad actual (Grupo II y Grupo III). */
    fun vigentes(miembro: MiembroFamiliaEntity): List<FactorRiesgoEdad> = vigentes(miembro.factoresRiesgoEdadJson, banda(miembro))

    fun vigentes(json: String, banda: GrupoEdadRiesgo?): List<FactorRiesgoEdad> {
        val marcados = decodificar(json)
        val actuales = disponibles(banda).filter { it.codigo in marcados }
        val antiguos = if (banda in bandasConSustancias) marcados.mapNotNull { heredados[it] } else emptyList()
        return actuales + antiguos
    }

    fun vigentesGrupoIII(miembro: MiembroFamiliaEntity) = vigentes(miembro).filter { it.grupoIII }
    fun vigentesGrupoII(miembro: MiembroFamiliaEntity) = vigentes(miembro).filterNot { it.grupoIII }

    private val codigosConsumo = setOf(CONSUMO_ALCOHOL, CONSUMO_DROGAS, "CONSUMO")
    fun hayConsumo(miembro: MiembroFamiliaEntity) = vigentes(miembro).any { it.codigo in codigosConsumo }
    fun hayConsumoEn(json: String, banda: GrupoEdadRiesgo?) = vigentes(json, banda).any { it.codigo in codigosConsumo }
    fun hayViolencia(miembro: MiembroFamiliaEntity) = vigentes(miembro).any { it.codigo == VIOLENCIA }
    fun hayViolenciaEn(json: String, banda: GrupoEdadRiesgo?) = vigentes(json, banda).any { it.codigo == VIOLENCIA }
}

/** Enfermedades y alertas que se calculan solas a partir de los diagnósticos CIE-10 de la persona. */
object EstrategiasDesdeCie10 {
    private val patronCodigo = Regex("\"codigo\"\\s*:\\s*\"([^\"]+)\"")

    /** Códigos de los diagnósticos guardados en la ficha de la persona (comorbilidadesCie10Json). */
    fun codigos(json: String): List<String> = patronCodigo.findAll(json).map { it.groupValues[1] }.toList()

    private fun limpios(codigos: Collection<String>) = codigos.map { it.uppercase().replace(".", "") }
    private fun cualquiera(codigos: Collection<String>, vararg patrones: String): Boolean {
        val regex = patrones.map { Regex(it) }
        return limpios(codigos).any { c -> regex.any { it.matches(c) } }
    }

    fun hipertension(codigos: Collection<String>) = cualquiera(codigos, "I1[0-5].*")
    fun diabetes(codigos: Collection<String>) = cualquiera(codigos, "E1[0-4].*")
    fun tuberculosis(codigos: Collection<String>) = cualquiera(codigos, "A1[5-9].*")
    fun vih(codigos: Collection<String>) = cualquiera(codigos, "B2[0-4].*", "Z21.*")
    fun saludMental(codigos: Collection<String>) = cualquiera(codigos, "F\\d\\d.*")
    fun cuidadosPaliativos(codigos: Collection<String>) = cualquiera(codigos, "Z515.*")

    /**
     * Estado nutricional que dicen los diagnósticos: desnutrición aguda (kwashiorkor, marasmo, proteicocalórica),
     * desnutrición crónica (retardo del desarrollo por desnutrición) u obesidad. Sin ninguno, sin alteración.
     */
    fun estadoNutricional(codigos: Collection<String>): String = when {
        cualquiera(codigos, "E4[0-4].*", "E46.*") -> DispensarizacionAutomatica.NUTRICION_DESNUTRICION_AGUDA
        cualquiera(codigos, "E45.*") -> DispensarizacionAutomatica.NUTRICION_DESNUTRICION_CRONICA
        cualquiera(codigos, "E66.*") -> DispensarizacionAutomatica.NUTRICION_OBESIDAD
        else -> DispensarizacionAutomatica.NUTRICION_SIN_ALTERACION
    }

    /** Las alertas epidemiológicas solo se piden con tuberculosis o VIH. */
    fun pideAlertasEpidemiologicas(codigos: Collection<String>) = tuberculosis(codigos) || vih(codigos)

    /**
     * Diagnósticos que por definición son del Grupo III: enfermedades crónicas (transmisibles o no, compensadas o no),
     * trastornos neurológicos y psiquiátricos, consumo de sustancias y lesiones autoinfligidas. Un diagnóstico agudo
     * (una gripe, una diarrea, una fractura) no entra aquí. Lista orientativa por capítulos del CIE-10: se puede ajustar.
     */
    private val grupoIII = listOf(
        "A1[5-9].*", "A30.*", "B18.*", "B2[0-4].*", "B57.*", "Z21.*",              // TB, lepra, hepatitis crónica, VIH, Chagas
        "C\\d\\d.*", "D4[5-7].*",                                                  // tumores malignos y afines
        "D5[5-9].*", "D6[01].*", "D63.*", "D6[6-9].*", "D8\\d.*",                  // sangre crónica, inmunidad
        "E0[0-7].*", "E1[0-4].*", "E2\\d.*", "E3[0-5].*", "E7\\d.*", "E8[0-8].*",  // tiroides, diabetes, endocrino, metabólico
        "F\\d\\d.*",                                                               // trastornos mentales, incluye consumo de sustancias
        "G[1-9]\\d.*",                                                             // neurológicos (epilepsia, Parkinson, secuelas…)
        "H3[3-5].*", "H4[0-2].*",                                                  // retina, glaucoma
        "I0[5-9].*", "I1[0-5].*", "I2\\d.*", "I3\\d.*", "I4\\d.*", "I5\\d.*", "I6\\d.*", "I7\\d.*", // circulatorio
        "J4[0-7].*", "J6\\d.*", "J70.*", "J84.*",                                  // asma, EPOC, neumoconiosis
        "K50.*", "K51.*", "K7[0-7].*", "K86.*",                                    // Crohn, colitis, hígado, páncreas
        "L40.*", "L93.*",                                                          // psoriasis, lupus cutáneo
        "M0[5-9].*", "M1[05-9].*", "M3\\d.*", "M45.*", "M8[01].*",                 // artritis, gota, artrosis, lupus, osteoporosis
        "N0[3-8].*", "N1[89].*",                                                   // glomerulopatías, enfermedad renal crónica
        "Q\\d\\d.*",                                                               // malformaciones congénitas
        "X[67]\\d.*", "X8[0-4].*", "T1491", "Z915"                                 // lesiones autoinfligidas e historia de autolesión
    ).map { Regex(it) }

    fun esGrupoIII(codigo: String): Boolean {
        val limpio = codigo.uppercase().replace(".", "")
        return grupoIII.any { it.matches(limpio) }
    }

    /** Hay al menos un diagnóstico del Grupo III. */
    fun hayGrupoIII(codigos: Collection<String>) = codigos.any(::esGrupoIII)
}
