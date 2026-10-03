package com.ruralitos.app

import com.ruralitos.app.data.local.dao.ViviendaMapaFila
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import com.ruralitos.app.domain.EstadoVisita
import com.ruralitos.app.domain.FiltroEstadoVisita
import com.ruralitos.app.domain.MapaSeguimiento
import com.ruralitos.app.domain.PeriodoVisita
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class MapaSeguimientoTest {
    private val dia = 24L * 3600_000
    private val ahora: Long = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 14, 10, 0, 0); set(Calendar.MILLISECOND, 0) // un miércoles
    }.timeInMillis

    private fun vivienda(id: Long) = ViviendaMapaFila(id, "JEFE $id", "0$id", "F$id", "Cerezal", "1", -4.0 + id / 1000.0, -79.2, "COMPLETA", "SINCRONIZADO", "", 3, 0)

    private fun visita(
        id: Long, ficha: Long?, fecha: Long, estado: String = "PENDIENTE", origen: String = "MANUAL", editada: Boolean = false, eliminada: Long? = null
    ) = ActividadAgendaEntity(
        id = id, usuarioId = 1, fichaId = ficha, fechaHora = fecha, tipo = "Visita domiciliaria", estado = estado,
        origen = origen, fechaEditada = editada, eliminadoEn = eliminada
    )

    @Test
    fun elEstadoSigueLasReglasDeLaAgenda() {
        assertEquals(EstadoVisita.REALIZADA, MapaSeguimiento.estado(visita(1, 1, ahora - dia, estado = "COMPLETADA"), ahora))
        assertEquals(EstadoVisita.POR_CONFIRMAR, MapaSeguimiento.estado(visita(2, 1, ahora - dia, origen = "SEGUIMIENTO"), ahora))
        assertEquals(EstadoVisita.CONFIRMADA, MapaSeguimiento.estado(visita(3, 1, ahora + dia, origen = "SEGUIMIENTO", editada = true), ahora))
        assertEquals(EstadoVisita.ATRASADA, MapaSeguimiento.estado(visita(4, 1, ahora - 3600_000), ahora))
        assertEquals(EstadoVisita.CONFIRMADA, MapaSeguimiento.estado(visita(5, 1, ahora + 3600_000), ahora))
        assertNull(MapaSeguimiento.estado(visita(6, 1, ahora, estado = "CANCELADA"), ahora))
        assertNull(MapaSeguimiento.estado(visita(7, 1, ahora, eliminada = 5L), ahora))
    }

    @Test
    fun cadaFiltroDeEstadoDejaSoloSusViviendas() {
        val v = (1L..4L).map(::vivienda)
        val a = listOf(
            visita(1, 1, ahora + dia),                                    // confirmada
            visita(2, 2, ahora - dia, origen = "SEGUIMIENTO"),            // por confirmar
            visita(3, 3, ahora - dia),                                    // atrasada
            visita(4, 4, ahora - 2 * dia, estado = "COMPLETADA")          // realizada
        )
        fun ids(f: FiltroEstadoVisita) = MapaSeguimiento.puntos(v, a, f, PeriodoVisita.TODAS, ahora).map { it.vivienda.fichaId }
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(FiltroEstadoVisita.TODAS))
        assertEquals(listOf(1L), ids(FiltroEstadoVisita.CONFIRMADAS))
        assertEquals(listOf(2L), ids(FiltroEstadoVisita.POR_CONFIRMAR))
        assertEquals(listOf(3L), ids(FiltroEstadoVisita.ATRASADAS))
        assertEquals("los atendidos son las fichas con visita realizada", listOf(4L), ids(FiltroEstadoVisita.REALIZADAS))
    }

    @Test
    fun unaViviendaSinVisitasNoApareceYLasSinFichaNoSePintan() {
        val v = listOf(vivienda(1), vivienda(2))
        val a = listOf(visita(1, 1, ahora + dia), visita(2, null, ahora + dia))
        assertEquals(listOf(1L), MapaSeguimiento.puntos(v, a, FiltroEstadoVisita.TODAS, PeriodoVisita.TODAS, ahora).map { it.vivienda.fichaId })
    }

    @Test
    fun elPuntoMuestraLoMasUrgenteDeLaFamilia() {
        val v = listOf(vivienda(1))
        val a = listOf(
            visita(1, 1, ahora + dia),                    // confirmada
            visita(2, 1, ahora - dia),                    // atrasada
            visita(3, 1, ahora - 5 * dia, estado = "COMPLETADA")
        )
        val p = MapaSeguimiento.puntos(v, a, FiltroEstadoVisita.TODAS, PeriodoVisita.TODAS, ahora).single()
        assertEquals(EstadoVisita.ATRASADA, p.estado)
        assertEquals(2L, p.principal.id)
        assertEquals(listOf(3L, 2L, 1L), p.visitas.map { it.id })
        // solo con las realizadas, la principal es la más reciente
        val soloRealizadas = listOf(visita(1, 1, ahora - 9 * dia, estado = "COMPLETADA"), visita(2, 1, ahora - 2 * dia, estado = "COMPLETADA"))
        assertEquals(2L, MapaSeguimiento.puntos(v, soloRealizadas, FiltroEstadoVisita.TODAS, PeriodoVisita.TODAS, ahora).single().principal.id)
    }

    @Test
    fun elPeriodoHoyIncluyeLoAtrasado() {
        val v = (1L..4L).map(::vivienda)
        val a = listOf(
            visita(1, 1, ahora + 2 * 3600_000),           // hoy
            visita(2, 2, ahora + 2 * dia),                // pasado mañana
            visita(3, 3, ahora - 2 * dia),                // atrasada
            visita(4, 4, ahora - 2 * dia, estado = "COMPLETADA") // realizada hace dos días: no es de hoy
        )
        fun ids(p: PeriodoVisita) = MapaSeguimiento.puntos(v, a, FiltroEstadoVisita.TODAS, p, ahora).map { it.vivienda.fichaId }
        assertEquals(listOf(1L, 3L), ids(PeriodoVisita.HOY))
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(PeriodoVisita.MES))
        assertEquals(listOf(1L, 2L, 3L, 4L), ids(PeriodoVisita.TODAS))
    }

    @Test
    fun laSemanaVaDeLunesADomingo() {
        val v = listOf(vivienda(1), vivienda(2))
        // ahora es miércoles 14: el lunes 12 está dentro; el domingo 11 no; el domingo 18 sí; el lunes 19 no
        val lunes = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 12, 9, 0, 0) }.timeInMillis
        val domingoAnterior = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 11, 9, 0, 0) }.timeInMillis
        val domingo = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 18, 20, 0, 0) }.timeInMillis
        val lunesSiguiente = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 19, 9, 0, 0) }.timeInMillis
        fun ids(fecha: Long) = MapaSeguimiento.puntos(
            listOf(vivienda(1)), listOf(visita(1, 1, fecha, estado = "COMPLETADA")), FiltroEstadoVisita.TODAS, PeriodoVisita.SEMANA, ahora
        ).size
        assertEquals(1, ids(lunes)); assertEquals(0, ids(domingoAnterior)); assertEquals(1, ids(domingo)); assertEquals(0, ids(lunesSiguiente))
        assertEquals(2, v.size)
    }

    @Test
    fun losConteosRespetanElPeriodo() {
        val v = (1L..3L).map(::vivienda)
        val a = listOf(visita(1, 1, ahora + 3600_000), visita(2, 2, ahora + 3 * dia), visita(3, 3, ahora + 3600_000, estado = "COMPLETADA"))
        val c = MapaSeguimiento.conteos(v, a, PeriodoVisita.HOY, ahora)
        assertEquals(2, c.getValue(FiltroEstadoVisita.TODAS))
        assertEquals(1, c.getValue(FiltroEstadoVisita.CONFIRMADAS))
        assertEquals(1, c.getValue(FiltroEstadoVisita.REALIZADAS))
    }

    @Test
    fun laVisitaFamiliarDeSeguimientoSeAbreComoUnSoloGrupo() {
        val f = ahora + dia
        val a = listOf(
            visita(1, 7, f, origen = "SEGUIMIENTO"), visita(2, 7, f, origen = "SEGUIMIENTO"),
            visita(3, 7, f + 3600_000, origen = "SEGUIMIENTO"), visita(4, 8, f, origen = "SEGUIMIENTO")
        )
        assertEquals(listOf(1L, 2L), MapaSeguimiento.grupoDe(a[0], a).map { it.id })
        assertEquals(listOf(5L), MapaSeguimiento.grupoDe(visita(5, 7, f), a).map { it.id })
    }

    @Test
    fun elGeoJsonLlevaElColorYLaLetraDelEstado() {
        val v = listOf(vivienda(1))
        val a = listOf(visita(1, 1, ahora - dia))
        val p = MapaSeguimiento.puntos(v, a, FiltroEstadoVisita.TODAS, PeriodoVisita.TODAS, ahora)
        val props = JSONObject(MapaSeguimiento.geoJson(p)).getJSONArray("features").getJSONObject(0).getJSONObject("properties")
        assertEquals("#D32F2F", props.getString("color"))
        assertEquals("A", props.getString("letra"))
        assertEquals(1, props.getInt("id"))
    }
}
