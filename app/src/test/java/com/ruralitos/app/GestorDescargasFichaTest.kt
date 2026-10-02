package com.ruralitos.app

import com.ruralitos.app.data.export.GestorDescargasFicha
import org.junit.Assert.assertEquals
import org.junit.Test

class GestorDescargasFichaTest {
    @Test
    fun normalizaNombreParaPdfYExcelSinCaracteresInvalidos() {
        val nombre = GestorDescargasFicha.normalizarNombre(
            " ficha 001 / familia: Pérez  "
        )

        assertEquals("ficha_001_familia_P_rez", nombre)
    }

    @Test
    fun usaNombreSeguroCuandoLaEntradaEstaVacia() {
        assertEquals(
            "ficha_familiar",
            GestorDescargasFicha.normalizarNombre("   ")
        )
    }
}
