package com.ruralitos.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RutaAViviendaTest {
    @Test
    fun sinCoordenadasOConElPuntoDeEjemploNoHayUbicacion() {
        assertFalse(RutaAVivienda.tieneUbicacion(null, -78.0))
        assertFalse(RutaAVivienda.tieneUbicacion(-1.8, null))
        assertFalse(RutaAVivienda.tieneUbicacion(-1.8312, -78.1834))
        assertFalse(RutaAVivienda.tieneUbicacion(Double.NaN, -78.0))
        assertFalse(RutaAVivienda.tieneUbicacion(120.0, -78.0))
    }

    @Test
    fun unaUbicacionRealSeAcepta() {
        assertTrue(RutaAVivienda.tieneUbicacion(-1.6635, -78.6547))
    }

    @Test
    fun losEnlacesUsanPuntoDecimalSinImportarElIdioma() {
        assertEquals("google.navigation:q=-1.663500,-78.654700", RutaAVivienda.enlaceNavegacion(-1.6635, -78.6547))
        assertEquals("geo:-1.663500,-78.654700?q=-1.663500,-78.654700(Ana%20P%C3%A9rez)", RutaAVivienda.enlaceMapa(-1.6635, -78.6547, "Ana Pérez"))
        assertEquals("geo:-1.663500,-78.654700?q=-1.663500,-78.654700", RutaAVivienda.enlaceMapa(-1.6635, -78.6547, " "))
    }
}
