package com.ruralitos.app.domain

import com.ruralitos.app.data.local.dao.ViviendaMapaFila
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuntoDeDestinoTest {
    @Test
    fun elDestinoNoTieneVisitasPeroSiSuFicha() {
        val fila = ViviendaMapaFila(
            fichaId = 7, jefe = "Ana", cedula = "1", numero = "12", barrio = "Centro", casa = "3",
            latitud = -1.6, longitud = -78.6, estado = "COMPLETA", syncEstado = "SINCRONIZADO", nivelRiesgo = "",
            integrantes = 4, visitasAtrasadas = 0, gestantes = 0, menoresCinco = 0, adultosMayores = 0
        )
        val punto = MapaSeguimiento.puntoDeDestino(fila, 1_000L)
        assertTrue(punto.visitas.isEmpty())
        assertEquals(7L, punto.vivienda.fichaId)
        assertEquals(7L, punto.principal.fichaId)
    }
}
