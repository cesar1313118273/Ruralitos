package com.ruralitos.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElementosCroquisTest {
    @Test
    fun codificarYDecodificarConservaSimbolosYTextos() {
        val lista = listOf(
            ElementoCroquis("a", TipoElementoCroquis.SIMBOLO, "IGLESIA", "Iglesia del barrio", -0.22, -78.51),
            ElementoCroquis("b", TipoElementoCroquis.TEXTO, "", "Camino al río", -0.23, -78.52)
        )
        val leida = ElementosCroquis.decodificar(ElementosCroquis.codificar(lista))
        assertEquals(lista, leida)
        assertEquals("Iglesia del barrio", leida[0].etiqueta)
    }

    @Test
    fun unSimboloSinNombreUsaElNombreDelSimbolo() {
        val e = ElementoCroquis("a", TipoElementoCroquis.SIMBOLO, "ESCUELA", "", 0.0, 0.0)
        assertEquals("Escuela", e.etiqueta)
    }

    @Test
    fun lasListasVaciasODanadasNoRompenNada() {
        assertTrue(ElementosCroquis.decodificar(null).isEmpty())
        assertTrue(ElementosCroquis.decodificar("").isEmpty())
        assertTrue(ElementosCroquis.decodificar("[]").isEmpty())
        assertTrue(ElementosCroquis.decodificar("esto no es json").isEmpty())
    }

    @Test
    fun seOmitenLosElementosInvalidos() {
        val json = """[
            {"id":"1","tipo":"SIMBOLO","codigo":"NO_EXISTE","texto":"x","lat":-0.2,"lon":-78.5},
            {"id":"2","tipo":"OTRO","codigo":"","texto":"x","lat":-0.2,"lon":-78.5},
            {"id":"3","tipo":"TEXTO","codigo":"","texto":"sin coordenadas"},
            {"id":"4","tipo":"TEXTO","codigo":"","texto":"bueno","lat":-0.2,"lon":-78.5}
        ]"""
        val leida = ElementosCroquis.decodificar(json)
        assertEquals(listOf("4"), leida.map { it.id })
    }

    @Test
    fun hayUnTopeDeElementosPorFicha() {
        val muchos = (1..50).map { ElementoCroquis("id$it", TipoElementoCroquis.TEXTO, "", "t$it", 0.0, 0.0) }
        assertEquals(ElementosCroquis.MAXIMO, ElementosCroquis.decodificar(ElementosCroquis.codificar(muchos)).size)
    }

    @Test
    fun elTextoSeLimpiaYSeAcorta() {
        assertEquals("Tienda de Rosa", ElementosCroquis.limpiarTexto("  Tienda   de  Rosa  "))
        assertEquals(ElementosCroquis.MAXIMO_TEXTO, ElementosCroquis.limpiarTexto("x".repeat(100)).length)
    }

    @Test
    fun cadaSimboloTieneCodigoUnicoYNombre() {
        assertEquals(SimboloCroquis.entries.size, SimboloCroquis.entries.map { it.codigo }.toSet().size)
        assertTrue(SimboloCroquis.entries.all { it.etiqueta.isNotBlank() })
        assertEquals(SimboloCroquis.IGLESIA, SimboloCroquis.deCodigo("IGLESIA"))
    }
}
