package com.ruralitos.app

import com.ruralitos.app.data.local.dao.ViviendaMapaFila
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import com.ruralitos.app.domain.EstadoVisita
import com.ruralitos.app.domain.MapaSeguimiento
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class MapaSeguimientoTest {
    private val hora = 3600_000L
    private val dia = 24L * hora
    private val ahora: Long = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 14, 10, 0, 0); set(Calendar.MILLISECOND, 0) // un miércoles
    }.timeInMillis
    private val todos = EstadoVisita.entries.toSet()
    private val hoyDesde = MapaSeguimiento.inicioDia(ahora)
    private val hoyHasta = MapaSeguimiento.finDia(ahora)

    private fun vivienda(id: Long) = ViviendaMapaFila(id, "JEFE $id", "0$id", "F$id", "Cerezal", "1", -4.0 + id / 1000.0, -79.2, "COMPLETA", "SINCRONIZADO", "", 3, 0)

    private fun visita(
        id: Long, ficha: Long?, fecha: Long, estado: String = "PENDIENTE", origen: String = "MANUAL", editada: Boolean = false, eliminada: Long? = null
    ) = ActividadAgendaEntity(
        id = id, usuarioId = 1, fichaId = ficha, fechaHora = fecha, tipo = "Visita domiciliaria", estado = estado,
        origen = origen, fechaEditada = editada, eliminadoEn = eliminada
    )

    private fun ids(
        v: List<ViviendaMapaFila>, a: List<ActividadAgendaEntity>, estados: Set<EstadoVisita> = todos,
        desde: Long = hoyDesde - 30 * dia, hasta: Long = hoyHasta + 30 * dia
    ) = MapaSeguimiento.puntos(v, a, estados, desde, hasta, ahora).map { it.vivienda.fichaId }

    @Test
    fun elEstadoSigueLasReglasDeLaAgenda() {
        assertEquals(EstadoVisita.REALIZADA, MapaSeguimiento.estado(visita(1, 1, ahora - dia, estado = "COMPLETADA"), ahora))
        assertEquals(EstadoVisita.POR_CONFIRMAR, MapaSeguimiento.estado(visita(2, 1, ahora - dia, origen = "SEGUIMIENTO"), ahora))
        assertEquals(EstadoVisita.CONFIRMADA, MapaSeguimiento.estado(visita(3, 1, ahora + dia, origen = "SEGUIMIENTO", editada = true), ahora))
        assertEquals(EstadoVisita.ATRASADA, MapaSeguimiento.estado(visita(4, 1, ahora - hora), ahora))
        assertEquals(EstadoVisita.CONFIRMADA, MapaSeguimiento.estado(visita(5, 1, ahora + hora), ahora))
        assertNull(MapaSeguimiento.estado(visita(6, 1, ahora, estado = "CANCELADA"), ahora))
        assertNull(MapaSeguimiento.estado(visita(7, 1, ahora, eliminada = 5L), ahora))
    }

    @Test
    fun cadaEstadoMarcadoDejaSoloSusViviendas() {
        val v = (1L..4L).map(::vivienda)
        val a = listOf(
            visita(1, 1, ahora + dia),                                    // confirmada
            visita(2, 2, ahora - dia, origen = "SEGUIMIENTO"),            // por confirmar
            visita(3, 3, ahora - dia),                                    // atrasada
            visita(4, 4, ahora - 2 * dia, estado = "COMPLETADA")          // realizada
        )
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(v, a))
        assertEquals(listOf(1L), ids(v, a, setOf(EstadoVisita.CONFIRMADA)))
        assertEquals(listOf(2L), ids(v, a, setOf(EstadoVisita.POR_CONFIRMAR)))
        assertEquals(listOf(3L), ids(v, a, setOf(EstadoVisita.ATRASADA)))
        assertEquals("los atendidos son las fichas con visita realizada", listOf(4L), ids(v, a, setOf(EstadoVisita.REALIZADA)))
        assertEquals("se pueden marcar varios", listOf(2L, 3L), ids(v, a, setOf(EstadoVisita.POR_CONFIRMAR, EstadoVisita.ATRASADA)))
        assertEquals("sin ninguno marcado no hay nada", emptyList<Long>(), ids(v, a, emptySet()))
    }

    @Test
    fun elRangoIncluyeElDiaDeInicioYElDiaDeFinCompletos() {
        val v = (1L..5L).map(::vivienda)
        val a = listOf(
            visita(1, 1, hoyDesde, estado = "COMPLETADA"),                      // a las 00:00 de hoy
            visita(2, 2, hoyHasta, estado = "COMPLETADA"),                      // a las 23:59:59.999 de hoy
            visita(3, 3, hoyDesde - 1, estado = "COMPLETADA"),                  // un instante antes de hoy
            visita(4, 4, hoyHasta + 1, estado = "COMPLETADA"),                  // un instante después de hoy
            visita(5, 5, hoyDesde + 12 * hora, estado = "COMPLETADA")           // a mediodía
        )
        assertEquals(listOf(1L, 2L, 5L), ids(v, a, desde = hoyDesde, hasta = hoyHasta))
    }

    @Test
    fun conLasFechasDeHoyAparecenSoloLasVisitasDeHoy() {
        val v = (1L..4L).map(::vivienda)
        val a = listOf(
            visita(1, 1, ahora + 2 * hora),               // hoy, más tarde
            visita(2, 2, ahora + 2 * dia),                // pasado mañana
            visita(3, 3, ahora - 2 * dia),                // atrasada de hace dos días: no es de hoy
            visita(4, 4, ahora - 3 * hora)                // atrasada de esta mañana: sí es de hoy
        )
        assertEquals(listOf(1L, 4L), ids(v, a, desde = hoyDesde, hasta = hoyHasta))
        // ampliando el rango hacia atrás se ve lo atrasado de antes
        assertEquals(listOf(1L, 3L, 4L), ids(v, a, desde = hoyDesde - 3 * dia, hasta = hoyHasta))
    }

    @Test
    fun unaViviendaSinVisitasNoApareceYLasSinFichaNoSePintan() {
        val v = listOf(vivienda(1), vivienda(2))
        val a = listOf(visita(1, 1, ahora + dia), visita(2, null, ahora + dia))
        assertEquals(listOf(1L), ids(v, a))
    }

    @Test
    fun elPuntoMuestraLoMasUrgenteDeLaFamilia() {
        val v = listOf(vivienda(1))
        val a = listOf(
            visita(1, 1, ahora + dia),                    // confirmada
            visita(2, 1, ahora - dia),                    // atrasada
            visita(3, 1, ahora - 5 * dia, estado = "COMPLETADA")
        )
        val p = MapaSeguimiento.puntos(v, a, todos, hoyDesde - 30 * dia, hoyHasta + 30 * dia, ahora).single()
        assertEquals(EstadoVisita.ATRASADA, p.estado)
        assertEquals(2L, p.principal.id)
        assertEquals(listOf(3L, 2L, 1L), p.visitas.map { it.id })
        // solo con las realizadas, la principal es la más reciente
        val soloRealizadas = listOf(visita(1, 1, ahora - 9 * dia, estado = "COMPLETADA"), visita(2, 1, ahora - 2 * dia, estado = "COMPLETADA"))
        assertEquals(2L, MapaSeguimiento.puntos(v, soloRealizadas, todos, hoyDesde - 30 * dia, hoyHasta, ahora).single().principal.id)
    }

    @Test
    fun losConteosRespetanElRangoYNoDependenDeLosEstadosMarcados() {
        val v = (1L..3L).map(::vivienda)
        val a = listOf(visita(1, 1, ahora + hora), visita(2, 2, ahora + 3 * dia), visita(3, 3, ahora + hora, estado = "COMPLETADA"))
        val c = MapaSeguimiento.conteos(v, a, hoyDesde, hoyHasta, ahora)
        assertEquals(1, c.getValue(EstadoVisita.CONFIRMADA))
        assertEquals(1, c.getValue(EstadoVisita.REALIZADA))
        assertEquals(0, c.getValue(EstadoVisita.ATRASADA))
        assertEquals(0, c.getValue(EstadoVisita.POR_CONFIRMAR))
    }

    @Test
    fun lasFichasSinUbicacionTambienSeCuentanParaElAviso() {
        val a = listOf(visita(1, 7, ahora + hora), visita(2, 8, ahora + 5 * dia), visita(3, 9, ahora + hora, estado = "COMPLETADA"))
        assertEquals(setOf(7L, 9L), MapaSeguimiento.fichasConVisita(a, todos, hoyDesde, hoyHasta, ahora))
        assertEquals(setOf(9L), MapaSeguimiento.fichasConVisita(a, setOf(EstadoVisita.REALIZADA), hoyDesde, hoyHasta, ahora))
    }

    @Test
    fun laVisitaFamiliarDeSeguimientoSeAbreComoUnSoloGrupo() {
        val f = ahora + dia
        val a = listOf(
            visita(1, 7, f, origen = "SEGUIMIENTO"), visita(2, 7, f, origen = "SEGUIMIENTO"),
            visita(3, 7, f + hora, origen = "SEGUIMIENTO"), visita(4, 8, f, origen = "SEGUIMIENTO")
        )
        assertEquals(listOf(1L, 2L), MapaSeguimiento.grupoDe(a[0], a).map { it.id })
        assertEquals(listOf(5L), MapaSeguimiento.grupoDe(visita(5, 7, f), a).map { it.id })
    }

    @Test
    fun elGeoJsonLlevaElColorYLaLetraDelEstado() {
        val p = MapaSeguimiento.puntos(listOf(vivienda(1)), listOf(visita(1, 1, ahora - hora)), todos, hoyDesde, hoyHasta, ahora)
        val props = JSONObject(MapaSeguimiento.geoJson(p)).getJSONArray("features").getJSONObject(0).getJSONObject("properties")
        assertEquals("#D32F2F", props.getString("color"))
        assertEquals("A", props.getString("letra"))
        assertEquals(1, props.getInt("id"))
    }

    @Test
    fun lasFechasSeArmanYSeDescomponenBien() {
        assertEquals(31, MapaSeguimiento.diasDelMes(2026, 10))
        assertEquals(30, MapaSeguimiento.diasDelMes(2026, 11))
        assertEquals(28, MapaSeguimiento.diasDelMes(2026, 2))
        assertEquals(29, MapaSeguimiento.diasDelMes(2028, 2))
        val t = MapaSeguimiento.fecha(2026, 10, 3)
        assertEquals(2026, MapaSeguimiento.anio(t)); assertEquals(10, MapaSeguimiento.mes(t)); assertEquals(3, MapaSeguimiento.dia(t))
        assertEquals("un 31 en un mes de 30 días pasa al último", 30, MapaSeguimiento.dia(MapaSeguimiento.fecha(2026, 11, 31)))
        assertEquals("el fin del día es un milisegundo antes del siguiente", MapaSeguimiento.fecha(2026, 10, 4) - 1, MapaSeguimiento.finDia(t))
    }
}
