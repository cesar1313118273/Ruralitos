package com.ruralitos.app

import com.ruralitos.app.domain.LlegadaVivienda
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlegadaViviendaTest {
    @Test
    fun conBuenGpsSeLlegaA40Metros() {
        assertTrue(LlegadaVivienda.haLlegado(35.0, 8f))
        assertTrue(LlegadaVivienda.haLlegado(40.0, 8f))
        assertFalse(LlegadaVivienda.haLlegado(41.0, 8f))
    }

    @Test
    fun conGpsImpreciso_elRadioCreceHastaCienMetros() {
        assertEquals(70.0, LlegadaVivienda.radio(70f), 1e-9)
        assertEquals(100.0, LlegadaVivienda.radio(250f), 1e-9)
        assertTrue(LlegadaVivienda.haLlegado(65.0, 70f))
        assertFalse(LlegadaVivienda.haLlegado(120.0, 250f))
    }

    @Test
    fun sinPrecisionUsaElMinimoYUnaDistanciaInvalidaNoAvisa() {
        assertEquals(40.0, LlegadaVivienda.radio(null), 1e-9)
        assertFalse(LlegadaVivienda.haLlegado(Double.NaN, 5f))
        assertFalse(LlegadaVivienda.haLlegado(Double.POSITIVE_INFINITY, 5f))
    }
}
