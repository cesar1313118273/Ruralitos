package com.ruralitos.app

import com.ruralitos.app.domain.InstitucionVisible
import org.junit.Assert.assertEquals
import org.junit.Test

class InstitucionVisibleTest {
    @Test
    fun quitaElNombreDelMinisterioEnCualquierEscritura() {
        listOf("MSP", "msp", " M.S.P. ", "Ministerio de Salud Pública", "MINISTERIO DE SALUD PUBLICA DEL ECUADOR", "SNS-MSP")
            .forEach { assertEquals(it, "", InstitucionVisible.limpiar(it)) }
    }

    @Test
    fun respetaLaInstitucionQueEscribeElCentro() {
        assertEquals("Clínica San José", InstitucionVisible.limpiar("  Clínica San José "))
        assertEquals("", InstitucionVisible.limpiar(""))
    }
}
