package com.ruralitos.app.data.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestorTerrenoOfflineTest {
    @Test
    fun coordenadaCeroCaeEnElCentroDelMundo() {
        val punto = GestorTerrenoOffline.puntoTesela(0.0, 0.0)
        assertEquals(4096, punto.x)
        assertEquals(4096, punto.y)
        assertEquals(0.0, punto.fraccionX, 0.000001)
        assertEquals(0.0, punto.fraccionY, 0.000001)
    }

    @Test
    fun coordenadasDeEcuadorSonValidas() {
        val punto = GestorTerrenoOffline.puntoTesela(-1.8312, -78.1834)
        assertTrue(punto.x in 0 until 8192)
        assertTrue(punto.y in 0 until 8192)
        assertTrue(punto.fraccionX in 0.0..1.0)
        assertTrue(punto.fraccionY in 0.0..1.0)
    }

    @Test
    fun decodificaAltitudTerrarium() {
        assertEquals(0.0, GestorTerrenoOffline.decodificarTerrarium(0x00800000), 0.00001)
        assertEquals(100.5, GestorTerrenoOffline.decodificarTerrarium(0x00806480), 0.00001)
    }
}
