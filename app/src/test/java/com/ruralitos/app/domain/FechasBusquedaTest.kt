package com.ruralitos.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FechasBusquedaTest {
    @Test
    fun sinFechasSoloSeVeLoDeHoy() {
        assertEquals("03/10/2026" to "03/10/2026", FechasBusqueda.efectivas("", "", false, "03/10/2026"))
        assertTrue(FechasBusqueda.esDelDia("", "", false))
    }

    @Test
    fun alPasarElDiaLaVistaSeMueveAlNuevoHoy() {
        assertEquals("04/10/2026" to "04/10/2026", FechasBusqueda.efectivas("", "", false, "04/10/2026"))
    }

    @Test
    fun lasFechasElegidasNoSeMuevenConElDia() {
        assertEquals("01/10/2026" to "02/10/2026", FechasBusqueda.efectivas("01/10/2026", "02/10/2026", false, "09/10/2026"))
        assertEquals("01/10/2026" to "", FechasBusqueda.efectivas("01/10/2026", "", false, "09/10/2026"))
        assertFalse(FechasBusqueda.esDelDia("01/10/2026", "", false))
    }

    @Test
    fun todasLasFechasQuitaElLimite() {
        assertEquals("" to "", FechasBusqueda.efectivas("", "", true, "03/10/2026"))
        assertEquals("" to "", FechasBusqueda.efectivas("01/10/2026", "02/10/2026", true, "03/10/2026"))
        assertFalse(FechasBusqueda.esDelDia("", "", true))
    }

    @Test
    fun laDescripcionDiceQueFechasSeMuestran() {
        assertEquals("Mostrando solo las fichas de hoy, 03/10/2026", FechasBusqueda.descripcion("", "", false, "03/10/2026"))
        assertEquals("Mostrando fichas de todas las fechas", FechasBusqueda.descripcion("", "", true, "03/10/2026"))
        assertEquals("Mostrando las fichas del 01/10/2026", FechasBusqueda.descripcion("01/10/2026", "01/10/2026", false, "03/10/2026"))
        assertEquals("Mostrando las fichas del 01/10/2026 al 02/10/2026", FechasBusqueda.descripcion("01/10/2026", "02/10/2026", false, "03/10/2026"))
        assertEquals("Mostrando las fichas desde el 01/10/2026", FechasBusqueda.descripcion("01/10/2026", "", false, "03/10/2026"))
        assertEquals("Mostrando las fichas hasta el 02/10/2026", FechasBusqueda.descripcion("", "02/10/2026", false, "03/10/2026"))
    }

    @Test
    fun laClaveOrdenaLasFechas() {
        assertEquals("20261003", FechasBusqueda.clave("03/10/2026"))
        assertEquals("20260105", FechasBusqueda.clave("5/1/2026"))
        assertEquals("", FechasBusqueda.clave(""))
        assertEquals("", FechasBusqueda.clave("2026-10-03"))
        assertTrue(FechasBusqueda.clave("31/12/2025") < FechasBusqueda.clave("01/01/2026"))
    }
}
