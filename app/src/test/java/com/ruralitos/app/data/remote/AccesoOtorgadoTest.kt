package com.ruralitos.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class AccesoOtorgadoTest {
    private fun acceso(centros: Int = 0, eais: Int = 0, barrios: Int = 0, fichas: Int = 0) =
        AccesoOtorgado("o", "u", "Ana", "", "", "EDITOR", centros, eais, barrios, fichas)

    @Test
    fun resumeLoCompartidoEnUnaFrase() {
        assertEquals("Todas tus fichas del centro", acceso(centros = 1, barrios = 2).resumen)
        assertEquals("1 barrio", acceso(barrios = 1).resumen)
        assertEquals("2 barrios y 3 fichas", acceso(barrios = 2, fichas = 3).resumen)
        assertEquals("1 EAIS, 2 barrios y 1 ficha", acceso(eais = 1, barrios = 2, fichas = 1).resumen)
        assertEquals("Sin fichas", acceso().resumen)
    }
}
