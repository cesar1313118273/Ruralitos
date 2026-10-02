package com.ruralitos.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MapaCampoAssetTest {
    private val assets = File("src/main/assets")

    @Test
    fun mapaBaseDeEcuadorEstaIncluidoYEsMbtilesValido() {
        val archivo = File(assets, "ecuador_base.mbtiles")
        assertTrue("Falta el mapa de campo", archivo.isFile)
        assertTrue("El mapa local parece incompleto", archivo.length() > 100_000_000L)
        assertEquals("SQLite format 3\u0000", archivo.inputStream().use { entrada ->
            ByteArray(16).also { assertEquals(16, entrada.read(it)) }.toString(Charsets.US_ASCII)
        })
    }

    @Test
    fun detalleZoom15EstaIncluidoYEsPmtilesValido() {
        val archivo = File(assets, "ecuador_zoom15.pmtiles")
        assertTrue("Falta el mapa detallado integrado", archivo.isFile)
        assertEquals("El mapa detallado parece incompleto", 140_147_517L, archivo.length())
        assertEquals("PMTiles", archivo.inputStream().use { entrada ->
            ByteArray(7).also { assertEquals(7, entrada.read(it)) }.toString(Charsets.US_ASCII)
        })
    }

    @Test
    fun estiloLocalIncluyeEtiquetasSinSolicitarFuentesRemotas() {
        val estilo = File(assets, "mapa_campo.json").readText()
        assertTrue(estilo.contains("__MAPA_LOCAL__"))
        assertTrue(estilo.contains("© OpenStreetMap contributors"))
        assertTrue(estilo.contains("asset://fonts/{fontstack}/{range}.pbf"))
        assertTrue(estilo.contains("\"id\":\"etiquetas-ciudades\""))
        assertTrue(estilo.contains("\"id\":\"etiquetas-calles\""))
        assertTrue(estilo.contains("\"id\":\"etiquetas-localidades\""))
        assertTrue(estilo.contains("\"id\":\"etiquetas-salud\""))
        assertFalse(estilo.contains("server.arcgisonline.com"))
        assertFalse(estilo.contains("https://"))
        val fuentes = File(assets, "fonts/Noto Sans Regular")
        listOf("0-255.pbf", "256-511.pbf", "512-767.pbf", "768-1023.pbf", "8192-8447.pbf")
            .forEach { rango -> assertTrue("Falta la tipografía $rango", File(fuentes, rango).length() > 1_000L) }
    }
}
