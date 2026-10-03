package com.ruralitos.app.domain

import java.text.Normalizer

/**
 * Ruralitos es una herramienta independiente: no muestra ni escribe en los documentos el nombre del Ministerio de
 * Salud Pública. Si un dato antiguo o del servidor trae esa institución, se deja en blanco para que el centro
 * escriba la suya.
 */
object InstitucionVisible {
    private val PROHIBIDAS = setOf("MSP", "MINISTERIODESALUDPUBLICA", "MINISTERIODESALUD", "SNSMSP", "SNS")

    fun limpiar(valor: String): String {
        val clave = Normalizer.normalize(valor, Normalizer.Form.NFD)
            .filter { it.isLetter() }
            .uppercase()
        return if (clave in PROHIBIDAS || clave.startsWith("MINISTERIODESALUD")) "" else valor.trim()
    }
}
