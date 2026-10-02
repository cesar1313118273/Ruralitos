package com.ruralitos.app.domain

object ValidadorIdentidadEcuador {
    /**
     * En la ficha familiar el documento funciona como identificador local e historia clínica.
     * Puede provenir de brigadas, registros provisionales o personas extranjeras, por lo que
     * aquí se valida el formato solicitado por el formulario, no el dígito verificador civil.
     */
    fun esDocumentoFamiliarAceptable(valor: String): Boolean {
        val documento = valor.trim()
        return documento.all(Char::isDigit) && documento.length in setOf(10, 13)
    }

    fun esIdentificacionAceptable(valor: String): Boolean {
        val documento = valor.trim()
        return when {
            documento.length == 10 && documento.all(Char::isDigit) -> esCedulaValida(documento)
            documento.length == 13 && documento.all(Char::isDigit) -> true
            else -> false
        }
    }

    fun esCedulaValida(cedula: String): Boolean {
        if (cedula.length != 10 || !cedula.all(Char::isDigit)) return false
        val provincia = cedula.substring(0, 2).toInt()
        if (provincia !in 1..24 && provincia != 30) return false
        if (cedula[2].digitToInt() >= 6) return false
        val factores = intArrayOf(2, 1, 2, 1, 2, 1, 2, 1, 2)
        val suma = (0..8).sumOf { indice ->
            val producto = cedula[indice].digitToInt() * factores[indice]
            if (producto >= 10) producto - 9 else producto
        }
        val verificador = (10 - suma % 10) % 10
        return verificador == cedula[9].digitToInt()
    }
}
