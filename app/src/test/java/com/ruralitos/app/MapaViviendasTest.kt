package com.ruralitos.app

import com.ruralitos.app.data.local.dao.ViviendaMapaFila
import com.ruralitos.app.domain.EstadoVivienda
import com.ruralitos.app.domain.FiltroVivienda
import com.ruralitos.app.domain.MapaViviendas
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapaViviendasTest {
    private fun fila(
        id: Long = 1, jefe: String = "PÉREZ LUIS", cedula: String = "0102030405", numero: String = "0142",
        barrio: String = "San José", casa: String = "12", estado: String = "COMPLETA", sync: String = "SINCRONIZADO",
        riesgo: String = "", atrasadas: Int = 0, lat: Double = -4.0, lon: Double = -79.2
    ) = ViviendaMapaFila(id, jefe, cedula, numero, barrio, casa, lat, lon, estado, sync, riesgo, 4, atrasadas)

    @Test
    fun elColorMuestraLoMasUrgenteDeLaFamilia() {
        assertEquals(EstadoVivienda.AL_DIA, MapaViviendas.estado(fila()))
        assertEquals(EstadoVivienda.RIESGO_ALTO, MapaViviendas.estado(fila(riesgo = "ALTO", estado = "BORRADOR", atrasadas = 3)))
        assertEquals(EstadoVivienda.PENDIENTE, MapaViviendas.estado(fila(estado = "BORRADOR")))
        assertEquals(EstadoVivienda.PENDIENTE, MapaViviendas.estado(fila(atrasadas = 1)))
        assertEquals(EstadoVivienda.SIN_SINCRONIZAR, MapaViviendas.estado(fila(sync = "PENDIENTE")))
        assertEquals("un riesgo medio no pinta de rojo", EstadoVivienda.AL_DIA, MapaViviendas.estado(fila(riesgo = "MEDIO")))
    }

    @Test
    fun losFiltrosSeparanLasViviendas() {
        val todas = listOf(
            fila(1), fila(2, riesgo = "ALTO"), fila(3, estado = "BORRADOR"), fila(4, sync = "ERROR")
        )
        assertEquals(4, MapaViviendas.filtrar(todas, FiltroVivienda.TODAS, "").size)
        assertEquals(listOf(3L), MapaViviendas.filtrar(todas, FiltroVivienda.PENDIENTES, "").map { it.fichaId })
        assertEquals(listOf(2L), MapaViviendas.filtrar(todas, FiltroVivienda.RIESGO_ALTO, "").map { it.fichaId })
        assertEquals(listOf(4L), MapaViviendas.filtrar(todas, FiltroVivienda.SIN_SINCRONIZAR, "").map { it.fichaId })
    }

    @Test
    fun laBusquedaIgnoraTildesYMayusculasYAceptaVariasPalabras() {
        val filas = listOf(fila(1, jefe = "PÉREZ LUIS", barrio = "San José"), fila(2, jefe = "GÓMEZ ANA", barrio = "Centro", cedula = "1111111111"))
        assertEquals(listOf(1L), MapaViviendas.filtrar(filas, FiltroVivienda.TODAS, "perez").map { it.fichaId })
        assertEquals(listOf(1L), MapaViviendas.filtrar(filas, FiltroVivienda.TODAS, "luis san jose").map { it.fichaId })
        assertEquals(listOf(2L), MapaViviendas.filtrar(filas, FiltroVivienda.TODAS, "1111").map { it.fichaId })
        assertEquals(2, MapaViviendas.filtrar(filas, FiltroVivienda.TODAS, "  ").size)
        assertTrue(MapaViviendas.filtrar(filas, FiltroVivienda.TODAS, "nadie").isEmpty())
    }

    @Test
    fun elGeoJsonTraeUnPuntoPorViviendaConSuColor() {
        val json = JSONObject(MapaViviendas.geoJson(listOf(fila(7, riesgo = "ALTO", lat = -3.99, lon = -79.2), fila(8))))
        val features = json.getJSONArray("features")
        assertEquals(2, features.length())
        val primero = features.getJSONObject(0)
        assertEquals(7, primero.getJSONObject("properties").getInt("id"))
        assertEquals("#D32F2F", primero.getJSONObject("properties").getString("color"))
        assertEquals(-79.2, primero.getJSONObject("geometry").getJSONArray("coordinates").getDouble(0), 1e-9)
        assertEquals(-3.99, primero.getJSONObject("geometry").getJSONArray("coordinates").getDouble(1), 1e-9)
        assertEquals(0, JSONObject(MapaViviendas.geoJson(emptyList())).getJSONArray("features").length())
    }

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
    fun losConteosCuentanCadaEstado() {
        val c = MapaViviendas.conteos(listOf(fila(1), fila(2, riesgo = "ALTO"), fila(3, riesgo = "ALTO"), fila(4, estado = "BORRADOR")))
        assertEquals(1, c[EstadoVivienda.AL_DIA])
        assertEquals(2, c[EstadoVivienda.RIESGO_ALTO])
        assertEquals(1, c[EstadoVivienda.PENDIENTE])
        assertEquals(0, c[EstadoVivienda.SIN_SINCRONIZAR])
    }
}
