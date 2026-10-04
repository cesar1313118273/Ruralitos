package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import java.util.Date

/** Criterio de la escala de riesgo obstétrico: nivel 1 (bajo), 2 (alto) o 3 (inminente). */
data class FactorObstetrico(
    val codigo: String,
    val etiqueta: String,
    val nivel: Int,
    /** Pertenece al Grupo III por definición (patología crónica, consumo problemático). El resto es un factor de riesgo (Grupo II). */
    val cronico: Boolean = false
)

enum class OrigenRazonObstetrica { MARCADA, DATOS_DE_LA_EMBARAZADA, DIAGNOSTICO_CIE10, FICHA_DE_LA_PERSONA }

data class RazonObstetrica(
    val codigo: String,
    val etiqueta: String,
    val nivel: Int,
    val cronica: Boolean,
    val origen: OrigenRazonObstetrica
) {
    val automatica get() = origen != OrigenRazonObstetrica.MARCADA
}

data class EvaluacionObstetrica(val razones: List<RazonObstetrica>) {
    val nivel: Int get() = razones.maxOfOrNull { it.nivel } ?: 0
    val hayCronica: Boolean get() = razones.any { it.cronica }
    /** Riesgo 3: requiere atención inmediata, aunque el grupo de dispensarización sea el II. */
    val inminente: Boolean get() = nivel == 3
    /** Valor que se guarda en la ficha y viaja a la nube (SIN_RIESGO, BAJO, ALTO, MUY_ALTO). */
    val riesgoObstetrico: String get() = when (nivel) {
        0 -> "SIN_RIESGO"
        1 -> "BAJO"
        2 -> "ALTO"
        else -> "MUY_ALTO"
    }
}

/**
 * Escala de riesgo obstétrico de la unidad. La embarazada marca los criterios que tenga; algunos se detectan solos con
 * lo que ya está en la ficha (edad, gestas, abortos, escolaridad, consumo, nutrición, enfermedades y diagnósticos CIE-10).
 * El nivel (1, 2, 3) es informativo; el grupo de dispensarización lo deciden las definiciones de cada grupo:
 * una patología crónica es Grupo III y cualquier otro criterio es un factor de riesgo, Grupo II.
 */
object FactoresObstetricos {
    private fun f(codigo: String, etiqueta: String, nivel: Int, cronico: Boolean = false) =
        FactorObstetrico(codigo, etiqueta, nivel, cronico)

    val riesgo1 = listOf(
        f("POBREZA_EXTREMA", "Pobreza extrema", 1),
        f("DESEMPLEO", "Desempleo", 1),
        f("ANALFABETISMO", "Analfabetismo", 1),
        f("CONTROL_INSUFICIENTE", "Control insuficiente: menos de 3 visitas prenatales", 1),
        f("EDAD_EXTREMA", "Menor de 19 años, o mayor de 35 en el primer embarazo", 1),
        f("MENOR_15", "Adolescente menor de 15 años", 1),
        f("CONSUMO_DROGAS", "Consumo problemático de drogas", 1, cronico = true),
        f("CONSUMO_ALCOHOL", "Consumo problemático de alcohol", 1, cronico = true),
        f("TABAQUISMO", "Tabaquismo", 1),
        f("GRAN_MULTIPARIDAD", "Gran multiparidad: más de 3 gestas", 1),
        f("INCOMPATIBILIDAD_RH", "Incompatibilidad Rh", 1),
        f("ITU_RECURRENTE", "Infección de vías urinarias recurrente (más de 2 veces en 3 controles)", 1),
        f("FLUJO_RECURRENTE", "Flujo vaginal recurrente (más de 2 veces en 3 controles)", 1),
        f("INTERVALO_CESAREA", "Menos de 12 meses desde una cesárea previa", 1)
    )

    val riesgo2 = listOf(
        f("ANEMIA", "Anemia: hemoglobina menor de 10 g/dL o hematocrito menor de 25 %", 2),
        f("EMBARAZO_MULTIPLE", "Embarazo múltiple", 2),
        f("ENDOCRINOPATIA", "Endocrinopatía", 2, cronico = true),
        f("NEFROPATIA", "Nefropatía", 2, cronico = true),
        f("INMUNOLOGICA", "Enfermedad inmunológica", 2, cronico = true),
        f("HIPERTENSION_CRONICA", "Hipertensión", 2, cronico = true),
        f("POLIHIDRAMNIOS", "Polihidramnios", 2),
        f("OLIGOAMNIOS", "Oligoamnios", 2),
        f("EPILEPSIA", "Epilepsia", 2, cronico = true),
        f("ABORTOS_ESPONTANEOS", "Abortos espontáneos", 2),
        f("PARTOS_PREMATUROS", "Partos prematuros", 2),
        f("PARTOS_DISTOCICOS", "Partos distócicos", 2),
        f("RCIU_PREVIO", "Antecedente de retardo del crecimiento intrauterino", 2),
        f("NEUROPSIQUIATRICA", "Enfermedad neuropsiquiátrica", 2, cronico = true),
        f("VIH", "Infección por VIH", 2, cronico = true),
        f("HEPATITIS_B", "Hepatitis B", 2, cronico = true),
        f("HEPATITIS_C", "Hepatitis C", 2, cronico = true),
        f("TOXOPLASMOSIS", "Toxoplasmosis", 2),
        f("PIELONEFRITIS", "Pielonefritis", 2),
        f("RUBEOLA", "Rubéola", 2),
        f("SIFILIS", "Sífilis", 2),
        f("ESTREPTOCOCO_B", "Estreptococo B", 2),
        f("CMV", "Citomegalovirus", 2),
        f("HERPES_2", "Herpes tipo 2", 2),
        f("VPH", "Virus del papiloma humano (HPV)", 2),
        f("PRESENTACION_ANOMALA", "Presentación fetal anómala después de la semana 36", 2),
        f("DESNUTRICION", "Desnutrición", 2),
        f("OBESIDAD", "Obesidad", 2),
        f("ASMA", "Asma", 2, cronico = true)
    )

    val riesgo3 = listOf(
        f("AMENAZA_PARTO_PRETERMINO", "Amenaza de parto pretérmino", 3),
        f("CARDIOPATIA", "Cardiopatía", 3, cronico = true),
        f("DIABETES_GESTACIONAL", "Diabetes gestacional descompensada o en tratamiento", 3),
        f("HEMORRAGIA_VAGINAL", "Hemorragia vaginal", 3),
        f("ROTURA_MEMBRANAS", "Rotura prematura de membranas de más de 12 horas", 3),
        f("SIDA_CLINICO", "SIDA clínico", 3, cronico = true),
        f("TRASTORNO_HIPERTENSIVO", "Trastorno hipertensivo durante el embarazo", 3),
        f("FCF_ALTERADA", "Frecuencia cardíaca fetal menor de 110 o mayor de 160 por minuto", 3),
        f("SIN_MOVIMIENTOS_FETALES", "Falta o ausencia de movimientos fetales", 3)
    )

    val todos: List<FactorObstetrico> = riesgo1 + riesgo2 + riesgo3
    private val porCodigo = todos.associateBy { it.codigo }

    fun codificar(codigos: Collection<String>): String = ListaDeCodigos.codificar(codigos)
    fun decodificar(texto: String): Set<String> = ListaDeCodigos.decodificar(texto)

    /** Los criterios que coinciden con lo escrito (sin tildes ni mayúsculas) y que aún no se eligieron. */
    fun sugerencias(escrito: String, elegidos: Set<String>, maximo: Int = 8): List<FactorObstetrico> {
        val buscado = textoParaBuscar(escrito)
        if (buscado.isEmpty()) return emptyList()
        return todos.filter { it.codigo !in elegidos && textoParaBuscar(it.etiqueta).contains(buscado) }.take(maximo)
    }

    /** Diagnósticos CIE-10 (sin punto, ej. O600) que equivalen a un criterio de la escala. */
    private val porDiagnostico: List<Pair<Regex, String>> = listOf(
        "O30.*" to "EMBARAZO_MULTIPLE",
        "O40.*" to "POLIHIDRAMNIOS",
        "O410" to "OLIGOAMNIOS",
        "O32.*" to "PRESENTACION_ANOMALA",
        "O60.*" to "AMENAZA_PARTO_PRETERMINO",
        "O20.*" to "HEMORRAGIA_VAGINAL",
        "O46.*" to "HEMORRAGIA_VAGINAL",
        "O42.*" to "ROTURA_MEMBRANAS",
        "O1[1-6].*" to "TRASTORNO_HIPERTENSIVO",
        "O244.*" to "DIABETES_GESTACIONAL",
        "D5\\d.*" to "ANEMIA",
        "D6[0-4].*" to "ANEMIA",
        "O990.*" to "ANEMIA",
        "G40.*" to "EPILEPSIA",
        "J4[56].*" to "ASMA",
        "N1[0-2].*" to "PIELONEFRITIS",
        "O230.*" to "PIELONEFRITIS",
        "A5[0-3].*" to "SIFILIS",
        "B58.*" to "TOXOPLASMOSIS",
        "B06.*" to "RUBEOLA",
        "B25.*" to "CMV",
        "A60.*" to "HERPES_2",
        "B16.*" to "HEPATITIS_B",
        "B181.*" to "HEPATITIS_B",
        "B171.*" to "HEPATITIS_C",
        "B182.*" to "HEPATITIS_C",
        "N18.*" to "NEFROPATIA",
        "I0[5-9].*" to "CARDIOPATIA",
        "I2[0-5].*" to "CARDIOPATIA",
        "B2[0-4].*" to "VIH",
        "Z21.*" to "VIH"
    ).map { (patron, codigo) -> Regex(patron) to codigo }

    fun codigosPorDiagnostico(diagnosticos: Collection<String>): Set<String> {
        val limpios = diagnosticos.map { it.uppercase().replace(".", "") }
        return porDiagnostico.filter { (regex, _) -> limpios.any { regex.matches(it) } }.mapTo(mutableSetOf()) { it.second }
    }

    /**
     * Junta lo marcado por la embarazada con lo que se detecta solo.
     * [diagnosticos] son los códigos CIE-10 de la persona; [miembro] puede ser nulo si no se encuentra su ficha.
     */
    fun evaluar(
        marcados: Set<String>,
        miembro: MiembroFamiliaEntity?,
        gestas: Int?,
        abortos: Int?,
        diagnosticos: Collection<String> = emptyList(),
        hoy: Date = Date()
    ): EvaluacionObstetrica {
        val razones = mutableListOf<RazonObstetrica>()
        val vistos = mutableSetOf<String>()
        fun agregar(codigo: String, origen: OrigenRazonObstetrica) {
            val factor = porCodigo[codigo] ?: return
            if (vistos.add(codigo)) razones += RazonObstetrica(factor.codigo, factor.etiqueta, factor.nivel, factor.cronico, origen)
        }
        marcados.forEach { agregar(it, OrigenRazonObstetrica.MARCADA) }

        val edad = miembro?.let { CumplimientoPoblacion.edadEnAnios(it.fechaNacimiento, hoy) }
        if (edad != null) {
            if (edad < 15) agregar("MENOR_15", OrigenRazonObstetrica.DATOS_DE_LA_EMBARAZADA)
            if (edad < 19 || (edad > 35 && gestas == 1)) agregar("EDAD_EXTREMA", OrigenRazonObstetrica.DATOS_DE_LA_EMBARAZADA)
        }
        if ((gestas ?: 0) > 3) agregar("GRAN_MULTIPARIDAD", OrigenRazonObstetrica.DATOS_DE_LA_EMBARAZADA)
        if ((abortos ?: 0) > 0) agregar("ABORTOS_ESPONTANEOS", OrigenRazonObstetrica.DATOS_DE_LA_EMBARAZADA)
        codigosPorDiagnostico(diagnosticos).forEach { agregar(it, OrigenRazonObstetrica.DIAGNOSTICO_CIE10) }

        if (miembro != null) {
            if (miembro.escolaridad.trim().uppercase() == "SIN") agregar("ANALFABETISMO", OrigenRazonObstetrica.FICHA_DE_LA_PERSONA)
            val factoresPersona = FactoresRiesgoEdad.vigentes(miembro).map { it.codigo }.toSet()
            if (FactoresRiesgoEdad.CONSUMO_ALCOHOL in factoresPersona) agregar("CONSUMO_ALCOHOL", OrigenRazonObstetrica.FICHA_DE_LA_PERSONA)
            if (FactoresRiesgoEdad.CONSUMO_DROGAS in factoresPersona) agregar("CONSUMO_DROGAS", OrigenRazonObstetrica.FICHA_DE_LA_PERSONA)
            if (miembro.consumoAlcoholDrogas == true && vistos.none { it.startsWith("CONSUMO_") }) {
                agregar("CONSUMO_ALCOHOL", OrigenRazonObstetrica.FICHA_DE_LA_PERSONA)
            }
            when (miembro.estadoNutricional) {
                DispensarizacionAutomatica.NUTRICION_OBESIDAD -> agregar("OBESIDAD", OrigenRazonObstetrica.FICHA_DE_LA_PERSONA)
                DispensarizacionAutomatica.NUTRICION_DESNUTRICION_AGUDA,
                DispensarizacionAutomatica.NUTRICION_DESNUTRICION_CRONICA -> agregar("DESNUTRICION", OrigenRazonObstetrica.FICHA_DE_LA_PERSONA)
            }
            if (miembro.hipertensionArterial == true) agregar("HIPERTENSION_CRONICA", OrigenRazonObstetrica.FICHA_DE_LA_PERSONA)
            if (miembro.diabetesMellitus == true) agregar("ENDOCRINOPATIA", OrigenRazonObstetrica.FICHA_DE_LA_PERSONA)
            if (miembro.problemaSaludMental == true) agregar("NEUROPSIQUIATRICA", OrigenRazonObstetrica.FICHA_DE_LA_PERSONA)
            if (miembro.vih == true) agregar("VIH", OrigenRazonObstetrica.FICHA_DE_LA_PERSONA)
        }
        return EvaluacionObstetrica(razones)
    }
}
