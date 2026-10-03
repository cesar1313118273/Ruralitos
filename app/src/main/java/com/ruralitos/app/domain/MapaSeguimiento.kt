package com.ruralitos.app.domain

import com.ruralitos.app.data.local.dao.ViviendaMapaFila
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import java.util.Calendar

/** Estado de una visita tal como lo muestra la agenda, para pintar el mapa con los mismos colores y nombres. */
enum class EstadoVisita(val etiqueta: String, val colorHex: String, val letra: String) {
    ATRASADA("Atrasada", "#D32F2F", "A"),
    POR_CONFIRMAR("Por confirmar", "#F7941D", "P"),
    CONFIRMADA("Confirmada", "#1565C0", "C"),
    REALIZADA("Realizada", "#0889A0", "R")
}

enum class FiltroEstadoVisita(val etiqueta: String) {
    TODAS("Todas"),
    POR_CONFIRMAR("Por confirmar"),
    CONFIRMADAS("Confirmadas"),
    ATRASADAS("Atrasadas"),
    REALIZADAS("Realizadas")
}

enum class PeriodoVisita(val etiqueta: String) {
    HOY("Hoy"),
    SEMANA("Semana"),
    MES("Mes"),
    TODAS("Todas las fechas")
}

/** Una vivienda en el mapa de seguimiento con las visitas que cumplen los filtros. */
data class PuntoSeguimiento(
    val vivienda: ViviendaMapaFila,
    val visitas: List<ActividadAgendaEntity>,
    val estado: EstadoVisita,
    /** La visita que más importa: la próxima por hacer, o la última realizada si todas lo están. */
    val principal: ActividadAgendaEntity
)

/**
 * Mapa de seguimiento: junta las visitas de la agenda con la ubicación de cada vivienda. Las reglas de estado son las
 * de la pestaña Seguimiento: una visita de seguimiento sin confirmar está «por confirmar»; una confirmada o manual cuya
 * hora ya pasó está «atrasada»; las demás están «confirmadas» hasta que se marcan «realizadas».
 */
object MapaSeguimiento {
    fun estado(a: ActividadAgendaEntity, ahora: Long): EstadoVisita? = when {
        a.eliminadoEn != null || a.estado == "CANCELADA" -> null
        a.estado == "COMPLETADA" -> EstadoVisita.REALIZADA
        a.origen == "SEGUIMIENTO" && !a.fechaEditada -> EstadoVisita.POR_CONFIRMAR
        a.fechaHora < ahora -> EstadoVisita.ATRASADA
        else -> EstadoVisita.CONFIRMADA
    }

    private fun inicioDia(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Rango [desde, hasta) de un periodo; nulo si no limita. «Hoy» además arrastra lo atrasado, que aún falta por hacer. */
    private fun rango(periodo: PeriodoVisita, ahora: Long): Pair<Long, Long>? {
        val hoy = inicioDia(ahora)
        val c = Calendar.getInstance().apply { timeInMillis = hoy }
        return when (periodo) {
            PeriodoVisita.TODAS -> null
            PeriodoVisita.HOY -> hoy to c.apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            PeriodoVisita.SEMANA -> {
                val dia = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 // lunes = 0
                c.add(Calendar.DAY_OF_MONTH, -dia)
                val desde = c.timeInMillis
                c.add(Calendar.DAY_OF_MONTH, 7)
                desde to c.timeInMillis
            }
            PeriodoVisita.MES -> {
                c.set(Calendar.DAY_OF_MONTH, 1)
                val desde = c.timeInMillis
                c.add(Calendar.MONTH, 1)
                desde to c.timeInMillis
            }
        }
    }

    fun cumpleFiltroEstado(e: EstadoVisita, filtro: FiltroEstadoVisita): Boolean = when (filtro) {
        FiltroEstadoVisita.TODAS -> true
        FiltroEstadoVisita.POR_CONFIRMAR -> e == EstadoVisita.POR_CONFIRMAR
        FiltroEstadoVisita.CONFIRMADAS -> e == EstadoVisita.CONFIRMADA
        FiltroEstadoVisita.ATRASADAS -> e == EstadoVisita.ATRASADA
        FiltroEstadoVisita.REALIZADAS -> e == EstadoVisita.REALIZADA
    }

    fun cumplePeriodo(a: ActividadAgendaEntity, e: EstadoVisita, periodo: PeriodoVisita, ahora: Long): Boolean {
        val rango = rango(periodo, ahora) ?: return true
        if (a.fechaHora in rango.first until rango.second) return true
        // Lo que quedó atrasado sigue siendo trabajo de hoy.
        return periodo == PeriodoVisita.HOY && (e == EstadoVisita.ATRASADA ||
            (e == EstadoVisita.POR_CONFIRMAR && a.fechaHora < rango.first))
    }

    private val prioridad = mapOf(
        EstadoVisita.ATRASADA to 0, EstadoVisita.POR_CONFIRMAR to 1, EstadoVisita.CONFIRMADA to 2, EstadoVisita.REALIZADA to 3
    )

    /**
     * Un punto por vivienda con al menos una visita que cumpla el estado y el periodo. [viviendas] ya viene filtrada por
     * barrio, riesgo o búsqueda.
     */
    fun puntos(
        viviendas: List<ViviendaMapaFila>,
        actividades: List<ActividadAgendaEntity>,
        filtro: FiltroEstadoVisita,
        periodo: PeriodoVisita,
        ahora: Long
    ): List<PuntoSeguimiento> {
        val porFicha = actividades.filter { it.fichaId != null }.groupBy { it.fichaId!! }
        return viviendas.mapNotNull { v ->
            val coinciden = porFicha[v.fichaId].orEmpty().mapNotNull { a ->
                val e = estado(a, ahora) ?: return@mapNotNull null
                if (cumpleFiltroEstado(e, filtro) && cumplePeriodo(a, e, periodo, ahora)) Triple(a, e, a.fechaHora) else null
            }
            if (coinciden.isEmpty()) return@mapNotNull null
            val urgente = coinciden.minOf { prioridad.getValue(it.second) }
            val delEstado = coinciden.filter { prioridad.getValue(it.second) == urgente }
            val principal = if (urgente == prioridad.getValue(EstadoVisita.REALIZADA)) delEstado.maxBy { it.third } else delEstado.minBy { it.third }
            PuntoSeguimiento(v, coinciden.sortedBy { it.third }.map { it.first }, principal.second, principal.first)
        }
    }

    /** Las fichas que tienen alguna visita con ese estado y periodo, tengan o no la vivienda ubicada. */
    fun fichasConVisita(
        actividades: List<ActividadAgendaEntity>,
        filtro: FiltroEstadoVisita,
        periodo: PeriodoVisita,
        ahora: Long
    ): Set<Long> = actividades.filter { a ->
        val e = estado(a, ahora)
        a.fichaId != null && e != null && cumpleFiltroEstado(e, filtro) && cumplePeriodo(a, e, periodo, ahora)
    }.mapTo(mutableSetOf()) { it.fichaId!! }

    /** Cuántas viviendas hay por estado, con el resto de filtros ya aplicados. */
    fun conteos(
        viviendas: List<ViviendaMapaFila>,
        actividades: List<ActividadAgendaEntity>,
        periodo: PeriodoVisita,
        ahora: Long
    ): Map<FiltroEstadoVisita, Int> = FiltroEstadoVisita.entries.associateWith { f ->
        puntos(viviendas, actividades, f, periodo, ahora).size
    }

    /** Las visitas que la agenda trata como una sola: la visita familiar de seguimiento comparte ficha y hora. */
    fun grupoDe(visita: ActividadAgendaEntity, actividades: List<ActividadAgendaEntity>): List<ActividadAgendaEntity> =
        if (visita.origen == "SEGUIMIENTO" && visita.estado == "PENDIENTE" && visita.fichaId != null) {
            actividades.filter {
                it.fichaId == visita.fichaId && it.fechaHora == visita.fechaHora &&
                    it.origen == "SEGUIMIENTO" && it.estado == "PENDIENTE" && it.eliminadoEn == null
            }.ifEmpty { listOf(visita) }
        } else listOf(visita)

    fun geoJson(puntos: List<PuntoSeguimiento>): String {
        val sb = StringBuilder("""{"type":"FeatureCollection","features":[""")
        puntos.forEachIndexed { i, p ->
            if (i > 0) sb.append(',')
            sb.append("""{"type":"Feature","geometry":{"type":"Point","coordinates":[${p.vivienda.longitud},${p.vivienda.latitud}]},""")
            sb.append(""""properties":{"id":${p.vivienda.fichaId},"color":"${p.estado.colorHex}","estado":"${p.estado.name}","letra":"${p.estado.letra}"}}""")
        }
        return sb.append("]}").toString()
    }
}
