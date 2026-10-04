package com.ruralitos.app.domain

import java.text.Normalizer
import java.util.Locale

/** Rol de cada integrante dentro de la familia. Cada rol es una opción separada (jefe y jefa, hijo e hija, etc.). */
object RolFamiliar {
    val opciones: List<Pair<String, String>> = listOf(
        "JEFE DE FAMILIA" to "Jefe de familia",
        "JEFA DE FAMILIA" to "Jefa de familia",
        "CÓNYUGE/PAREJA" to "Cónyuge o pareja",
        "HIJO" to "Hijo",
        "HIJA" to "Hija",
        "PADRE" to "Padre",
        "MADRE" to "Madre",
        "ABUELO" to "Abuelo",
        "ABUELA" to "Abuela",
        "NIETO" to "Nieto",
        "NIETA" to "Nieta",
        "HERMANO" to "Hermano",
        "HERMANA" to "Hermana",
        "OTRO FAMILIAR" to "Otro familiar",
        "NO FAMILIAR" to "No familiar"
    )

    /** Roles de una mujer, para la ficha de la embarazada. */
    val opcionesMujer: List<Pair<String, String>> = opciones.filter {
        it.first in setOf("JEFA DE FAMILIA", "CÓNYUGE/PAREJA", "HIJA", "MADRE", "ABUELA", "NIETA", "HERMANA", "OTRO FAMILIAR", "NO FAMILIAR")
    }

    private fun limpio(valor: String) = Normalizer.normalize(valor, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").uppercase(Locale.ROOT).trim()

    /** Jefe o jefa, también con el valor de versiones anteriores («JEFE/A DE FAMILIA»). */
    fun esJefe(valor: String) = limpio(valor).startsWith("JEF")

    /**
     * Lleva un rol guardado antes («HIJO/A», «JEFE/A DE FAMILIA»…) a las opciones separadas, según el sexo de la persona
     * (H hombre, M mujer). Los valores que ya son de la lista nueva no se tocan.
     */
    fun normalizar(valor: String, sexo: String): String {
        val v = valor.trim()
        if (v.isEmpty() || opciones.any { it.first == v }) return v
        val mujer = sexo.trim().uppercase(Locale.ROOT) in setOf("M", "F", "MUJER")
        return when (limpio(v)) {
            "JEFE/A DE FAMILIA" -> if (mujer) "JEFA DE FAMILIA" else "JEFE DE FAMILIA"
            "HIJO/A" -> if (mujer) "HIJA" else "HIJO"
            "PADRE/MADRE" -> if (mujer) "MADRE" else "PADRE"
            "ABUELO/A" -> if (mujer) "ABUELA" else "ABUELO"
            "NIETO/A" -> if (mujer) "NIETA" else "NIETO"
            "HERMANO/A" -> if (mujer) "HERMANA" else "HERMANO"
            "CONYUGE/PAREJA" -> "CÓNYUGE/PAREJA"
            else -> v
        }
    }

    /** Sexo que sugiere el rol (H o M), o nulo si el rol no lo define. */
    fun sexoDelRol(valor: String): String? = when (valor) {
        "JEFE DE FAMILIA", "HIJO", "PADRE", "ABUELO", "NIETO", "HERMANO" -> "H"
        "JEFA DE FAMILIA", "HIJA", "MADRE", "ABUELA", "NIETA", "HERMANA" -> "M"
        else -> null
    }
}
