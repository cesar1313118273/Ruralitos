package com.ruralitos.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NavegacionFichaTest {
    @Test
    fun `crear ficha avanza y regresa entre secciones sin salir al inicio`() {
        assertEquals("saludFamiliar", NavegacionFicha.avanzar("miembros", false))
        assertEquals("ubicacion", NavegacionFicha.regresar("miembros", false, false))
        assertEquals("miembros", NavegacionFicha.regresar("saludFamiliar", false, false))
        assertEquals("tratamiento", NavegacionFicha.regresar("revisionFicha", false, false))
    }

    @Test
    fun `editar ficha continua a siguiente seccion y regresar vuelve al panel`() {
        assertEquals("saludFamiliar", NavegacionFicha.avanzar("miembros", false))
        assertEquals("menuFicha", NavegacionFicha.regresar("miembros", true, false))
        assertEquals("revisionFicha", NavegacionFicha.avanzar("tratamiento", false))
    }

    @Test
    fun `una seccion abierta desde revision siempre vuelve a revision`() {
        assertEquals("revisionFicha", NavegacionFicha.avanzar("riesgos", true))
        assertEquals("revisionFicha", NavegacionFicha.regresar("riesgos", true, true))
    }
}