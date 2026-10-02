package com.ruralitos.app.data.mapa

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtremosRutaTest {
    private val calculada = ExtremosRuta(-0.1800000, -78.5000000, -0.2200000, -78.5200000)

    @Test fun rutaSoloCoincideConSusDosPuntosOriginales() {
        assertTrue(calculada.coincide(-0.1800000, -78.5000000, -0.2200000, -78.5200000))
        assertFalse(calculada.coincide(-0.1800000, -78.5000000, -0.2300000, -78.5300000))
        assertFalse(calculada.coincide(-0.1900000, -78.5100000, -0.2200000, -78.5200000))
        assertFalse(calculada.coincide(null, -78.5000000, -0.2200000, -78.5200000))
    }
}
