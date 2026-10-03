package com.ruralitos.app.domain

import java.text.Normalizer
import java.util.Locale

/**
 * Sexo, cargos y saludo del profesional.
 *
 * El cargo depende del sexo que se elige al crear la cuenta (o en el perfil), y el saludo de la
 * pantalla de inicio usa el título correspondiente: "Dra. Ana Pérez", "Dr. Luis Mora", etc.
 */
object SaludoProfesional {
    const val SEXO_HOMBRE = "H"
    const val SEXO_MUJER = "M"

    private val cargosMujer = listOf(
        "Doctora", "Enfermera", "TAPS", "Administradora", "Odontóloga", "Obstetra", "Psicóloga"
    )
    private val cargosHombre = listOf(
        "Doctor", "Enfermero", "TAPS", "Administrador", "Odontólogo", "Obstetra", "Psicólogo"
    )

    /** Cargos disponibles para el sexo elegido (vacío mientras no se elija). */
    fun cargosPara(sexo: String): List<String> = when (sexo) {
        SEXO_MUJER -> cargosMujer
        SEXO_HOMBRE -> cargosHombre
        else -> emptyList()
    }

    fun nombreSexo(sexo: String): String = when (sexo) {
        SEXO_MUJER -> "Mujer"
        SEXO_HOMBRE -> "Hombre"
        else -> ""
    }

    /** Posición del cargo dentro de las listas (para pasar de una versión a otra). */
    private fun categoria(cargo: String): String? {
        val texto = normalizar(cargo)
        return when {
            "doctor" in texto || "medic" in texto -> "doctor"
            "enfermer" in texto -> "enfermeria"
            "taps" in texto -> "taps"
            "administr" in texto -> "administrativo"
            "odontolog" in texto -> "odontologo"
            "obstetra" in texto -> "obstetra"
            "psicolog" in texto -> "psicologo"
            else -> null
        }
    }

    private val categorias = listOf(
        "doctor", "enfermeria", "taps", "administrativo", "odontologo", "obstetra", "psicologo"
    )

    /**
     * Devuelve el cargo de la lista del sexo indicado que equivale al cargo actual
     * (por ejemplo "Médico/a" o "Doctor" para una mujer pasan a "Doctora").
     * Si no hay equivalente conocido, se conserva el texto original.
     */
    fun cargoEquivalente(cargo: String, sexo: String): String {
        val opciones = cargosPara(sexo)
        if (opciones.isEmpty()) return cargo.trim()
        val indice = categorias.indexOf(categoria(cargo))
        return if (indice >= 0) opciones[indice] else cargo.trim()
    }

    /** Intenta deducir el sexo a partir de un cargo antiguo con género ("Doctora", "Psicólogo"). */
    fun inferirSexo(cargo: String): String {
        val texto = normalizar(cargo)
        if ("/" in texto) return ""
        return when (texto) {
            "doctora", "medica", "enfermera", "administradora", "odontologa", "psicologa" -> SEXO_MUJER
            "doctor", "medico", "enfermero", "administrador", "odontologo", "psicologo" -> SEXO_HOMBRE
            else -> ""
        }
    }

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

    /** Título abreviado: Dr./Dra. (doctor, odontólogo), Lcdo./Lcda. (enfermería, obstetra, psicología), Sr./Sra. */
    fun tratamiento(cargo: String, sexo: String = ""): String {
        val mujer = sexo == SEXO_MUJER
        val hombre = sexo == SEXO_HOMBRE
        return when (categoria(cargo)) {
            "doctor", "odontologo" -> if (mujer) "Dra." else if (hombre) "Dr." else "Dr./Dra."
            "enfermeria", "obstetra", "psicologo" ->
                if (mujer) "Lcda." else if (hombre) "Lcdo." else "Lic."
            "taps", "administrativo" -> if (mujer) "Sra." else if (hombre) "Sr." else "Sr./Sra."
            else -> cargoVisible(cargo)
        }
    }

    /**
     * "Primer nombre + primer apellido". [apellidos] es la parte de apellidos de [nombresCompletos]
     * (que se guarda como "Apellidos Nombres"). Para cuentas antiguas sin esa separación se supone
     * el formato habitual de Ecuador: dos apellidos y luego los nombres.
     */
    fun nombreCorto(apellidos: String, nombresCompletos: String): String {
        val partes = nombresCompletos.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (partes.isEmpty()) return ""
        if (partes.size == 1) return capitalizar(partes[0])
        val cantidadApellidos = apellidos.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        val primerApellido: String
        val primerNombre: String
        if (cantidadApellidos > 0 && cantidadApellidos < partes.size) {
            primerApellido = partes[0]
            primerNombre = partes[cantidadApellidos]
        } else {
            primerApellido = partes[0]
            primerNombre = if (partes.size >= 3) partes[2] else partes[1]
        }
        return "${capitalizar(primerNombre)} ${capitalizar(primerApellido)}"
    }

    /**
     * Separa "Apellidos Nombres" en (apellidos, nombres). Si no se conocen los apellidos se supone
     * el formato habitual de Ecuador: dos apellidos y luego los nombres.
     */
    fun separarNombre(nombresCompletos: String, apellidos: String): Pair<String, String> {
        val partes = nombresCompletos.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (partes.isEmpty()) return "" to ""
        val conocidos = apellidos.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        val cantidad = when {
            conocidos in 1 until partes.size -> conocidos
            partes.size >= 3 -> 2
            partes.size == 2 -> 1
            else -> 0
        }
        return partes.take(cantidad).joinToString(" ") to partes.drop(cantidad).joinToString(" ")
    }

    fun completo(
        hora: Int,
        cargo: String,
        sexo: String,
        apellidos: String,
        nombresCompletos: String
    ): String {
        val persona = nombreCorto(apellidos, nombresCompletos).ifBlank { "profesional" }
        return "${segunHora(hora)}, ${tratamiento(cargo, sexo)} $persona"
    }

    private fun capitalizar(texto: String): String =
        texto.lowercase(Locale("es", "EC")).replaceFirstChar { it.titlecase(Locale("es", "EC")) }

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale("es", "EC"))
            .trim()
}
