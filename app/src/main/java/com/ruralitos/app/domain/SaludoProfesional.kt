package com.ruralitos.app.domain

import java.text.Normalizer
import java.util.Locale

object SaludoProfesional {
    val cargosPredeterminados = listOf(
        "Médico/a",
        "Enfermero/a",
        "TAPS",
        "Administrativo/a",
        "Odontólogo/a",
        "Obstetra",
        "Psicólogo/a"
    )

    fun segunHora(hora: Int): String = when (hora.coerceIn(0, 23)) {
        in 6..11 -> "Buenos días"
        in 12..17 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    fun cargoVisible(cargo: String): String {
        val limpio = cargo.trim().replace(Regex("\\s+"), " ")
        if (limpio.isBlank()) return "Profesional"
        return limpio.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale("es", "EC")) else it.toString()
        }
    }

    fun tratamiento(cargo: String): String {
        val normalizado = Normalizer.normalize(cargo, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale("es", "EC"))
            .trim()
        return when {
            "medic" in normalizado || "odontolog" in normalizado -> "Doctor/a"
            "enfermer" in normalizado || "obstetra" in normalizado ||
                "psicolog" in normalizado -> "Licenciado/a"
            "taps" in normalizado || "administrativ" in normalizado -> "Señor/a"
            else -> cargoVisible(cargo)
        }
    }

    fun completo(hora: Int, cargo: String, nombres: String): String {
        val persona = nombres.trim().replace(Regex("\\s+"), " ").ifBlank { "profesional" }
        return "${segunHora(hora)}, ${tratamiento(cargo)} $persona"
    }
}
