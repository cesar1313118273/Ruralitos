package com.ruralitos.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.mapa.UbicacionesRuta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UbicacionesRutaInstrumentedTest {
    @Test
    fun centroEsPorUsuarioYElCambioDeInicioSoloAfectaUnaFicha() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val base = System.currentTimeMillis()
        val usuarioA = base
        val usuarioB = base + 1
        val fichaA = base + 2
        val fichaB = base + 3
        val centro = UbicacionesRuta.Punto(-0.220164, -78.512327, 2810.0)
        val inicioDistinto = UbicacionesRuta.Punto(-0.218000, -78.510000, 2805.0)

        assertFalse(UbicacionesRuta.introduccionVista(context, usuarioA))
        UbicacionesRuta.guardarCentro(context, usuarioA, centro)
        assertTrue(UbicacionesRuta.introduccionVista(context, usuarioA))
        assertEquals(centro, UbicacionesRuta.inicioFicha(context, fichaA, usuarioA))
        assertEquals(null, UbicacionesRuta.centro(context, usuarioB))

        UbicacionesRuta.guardarInicioFicha(context, fichaA, inicioDistinto)
        assertEquals(inicioDistinto, UbicacionesRuta.inicioFicha(context, fichaA, usuarioA))
        assertEquals(centro, UbicacionesRuta.inicioFicha(context, fichaB, usuarioA))
    }
}
