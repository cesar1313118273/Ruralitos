package com.ruralitos.app

import com.ruralitos.app.domain.MapaViviendas
import org.junit.Assert.assertEquals
import org.junit.Test

class MapaViviendasTest {
    @Test
    fun laDistanciaYElTiempoAPieSonRazonables() {
        // un grado de latitud ≈ 111 km
        assertEquals(111_195.0, MapaViviendas.distanciaMetros(0.0, 0.0, 1.0, 0.0), 300.0)
        assertEquals("850 m", MapaViviendas.textoDistancia(852.0))
        assertEquals("1,2 km", MapaViviendas.textoDistancia(1_234.0))
        assertEquals(15, MapaViviendas.minutosAPie(1_150.0))
        assertEquals(5, MapaViviendas.minutosAPie(50.0))
    }

    @Test
    fun dosPuntosIgualesEstanADistanciaCero() {
        assertEquals(0.0, MapaViviendas.distanciaMetros(-4.0, -79.2, -4.0, -79.2), 1e-6)
    }
}
