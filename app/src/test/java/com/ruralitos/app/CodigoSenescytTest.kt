package com.ruralitos.app

import com.ruralitos.app.ui.screens.codigoSenescytValido
import com.ruralitos.app.ui.screens.formatearCodigoSenescyt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodigoSenescytTest {
    @Test
    fun agregaLosSeparadoresMientrasSeEscribenLosDigitos() {
        assertEquals("1016.", formatearCodigoSenescyt("1016"))
        assertEquals("1016.2025-", formatearCodigoSenescyt("10162025"))
        assertEquals("1016.2025-3143718", formatearCodigoSenescyt("101620253143718"))
    }

    @Test
    fun aceptaPegadoConFormatoYDescartaCaracteresAdicionales() {
        assertEquals(
            "1016.2025-3143718",
            formatearCodigoSenescyt("1016.2025-3143718ABC999")
        )
    }

    @Test
    fun soloEsValidoCuandoElFormatoEstaCompleto() {
        assertTrue(codigoSenescytValido("1016.2025-3143718"))
        assertFalse(codigoSenescytValido("1016.2025-314371"))
        assertFalse(codigoSenescytValido("10162025-3143718"))
    }
}