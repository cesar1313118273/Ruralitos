package com.ruralitos.app.domain

import com.ruralitos.app.data.local.dao.ViviendaMapaFila
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import java.util.Calendar

/** Estado de una visita tal como lo muestra la agenda, para pintar el mapa con los mismos colores y nombres. */
enum class EstadoVisita(val etiqueta: String, val colorHex: String, val letra: String) {
    PENDIENTE("Pendiente", "#F7941D", "P"),
    PROGRAMADA("Programada", "#1565C0", "V"),
    ATRASADA("Atrasada", "#D32F2F", "A"),
    REALIZADA("Realizada", "#0889A0", "R")
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
 * de la pestaña Seguimiento: una visita de seguimiento sin programar está «pendiente»; una programada o manual cuya
 * hora ya pasó está «atrasada»; las demás están «programadas» hasta que se marcan «realizadas».
 *
 * El mapa solo se filtra por estado y por un rango de fechas (inicio y fin incluidos, de día completo).
 */
object MapaSeguimiento {
    fun estado(a: ActividadAgendaEntity, ahora: Long): EstadoVisita? = when {
        a.eliminadoEn != null || a.estado == "CANCELADA" -> null
        a.estado == "COMPLETADA" -> EstadoVisita.REALIZADA
        a.origen == "SEGUIMIENTO" && !a.fechaEditada -> EstadoVisita.PENDIENTE
        a.fechaHora < ahora -> EstadoVisita.ATRASADA
        else -> EstadoVisita.PROGRAMADA
    }

    // ---- fechas -------------------------------------------------------------------------------------------------

    fun inicioDia(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** El último instante del día de [t]. */
    fun finDia(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = inicioDia(t)
        add(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MILLISECOND, -1)
    }.timeInMillis

    /** Cuántos días tiene el mes (1 = enero … 12 = diciembre) de ese año. */
    fun diasDelMes(anio: Int, mes: Int): Int = Calendar.getInstance().apply {
        clear(); set(anio, mes - 1, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    /** El inicio del día [dia]/[mes]/[anio]; si el día no existe en ese mes se usa el último. */
    fun fecha(anio: Int, mes: Int, dia: Int): Long = Calendar.getInstance().apply {
        clear(); set(anio, mes - 1, dia.coerceIn(1, diasDelMes(anio, mes)))
    }.timeInMillis

    fun anio(t: Long): Int = Calendar.getInstance().apply { timeInMillis = t }.get(Calendar.YEAR)
    fun mes(t: Long): Int = Calendar.getInstance().apply { timeInMillis = t }.get(Calendar.MONTH) + 1
    fun dia(t: Long): Int = Calendar.getInstance().apply { timeInMillis = t }.get(Calendar.DAY_OF_MONTH)

    // ---- puntos -------------------------------------------------------------------------------------------------

    private val prioridad = mapOf(
        EstadoVisita.ATRASADA to 0, EstadoVisita.PENDIENTE to 1, EstadoVisita.PROGRAMADA to 2, EstadoVisita.REALIZADA to 3
    )

    private fun cumple(a: ActividadAgendaEntity, estados: Set<EstadoVisita>, desde: Long, hasta: Long, ahora: Long): EstadoVisita? {
        val e = estado(a, ahora) ?: return null
        return e.takeIf { it in estados && a.fechaHora in desde..hasta }
    }

    /**
     * Un punto por vivienda con al menos una visita de un estado elegido dentro del rango [desde]..[hasta] (ambos
     * incluidos, en milisegundos).
     */
    fun puntos(
        viviendas: List<ViviendaMapaFila>,
        actividades: List<ActividadAgendaEntity>,
        estados: Set<EstadoVisita>,
        desde: Long,
        hasta: Long,
        ahora: Long
    ): List<PuntoSeguimiento> {
        val porFicha = actividades.filter { it.fichaId != null }.groupBy { it.fichaId!! }
        return viviendas.mapNotNull { v ->
            val coinciden = porFicha[v.fichaId].orEmpty().mapNotNull { a ->
                cumple(a, estados, desde, hasta, ahora)?.let { Triple(a, it, a.fechaHora) }
            }
            if (coinciden.isEmpty()) return@mapNotNull null
            val urgente = coinciden.minOf { prioridad.getValue(it.second) }
            val delEstado = coinciden.filter { prioridad.getValue(it.second) == urgente }
            val soloRealizadas = urgente == prioridad.getValue(EstadoVisita.REALIZADA)
            val principal = if (soloRealizadas) delEstado.maxBy { it.third } else delEstado.minBy { it.third }
            PuntoSeguimiento(v, coinciden.sortedBy { it.third }.map { it.first }, principal.second, principal.first)
        }
    }

    /**
     * Una vivienda a la que solo se quiere llegar (botón «Cómo llegar» de la ficha), sin visita agendada: aparece en el
     * mapa como cualquier otra y se puede trazar la ruta hasta ella. No tiene visitas (`visitas` vacía).
     */
    fun puntoDeDestino(v: ViviendaMapaFila, ahora: Long) = PuntoSeguimiento(
        vivienda = v,
        visitas = emptyList(),
        estado = EstadoVisita.PROGRAMADA,
        principal = ActividadAgendaEntity(
            usuarioId = 0L, fichaId = v.fichaId, persona = v.jefe, fechaHora = ahora, tipo = "Destino",
            estado = "PENDIENTE", origen = "MANUAL", fechaEditada = true
        )
    )

    /** Las fichas que tienen alguna visita así, tengan o no la vivienda ubicada. */
    fun fichasConVisita(
        actividades: List<ActividadAgendaEntity>,
        estados: Set<EstadoVisita>,
        desde: Long,
        hasta: Long,
        ahora: Long
    ): Set<Long> = actividades.filter { a -> a.fichaId != null && cumple(a, estados, desde, hasta, ahora) != null }
        .mapTo(mutableSetOf()) { it.fichaId!! }

    /** Cuántas viviendas tienen visitas de cada estado dentro del rango, sin importar qué estados estén marcados. */
    fun conteos(
        viviendas: List<ViviendaMapaFila>,
        actividades: List<ActividadAgendaEntity>,
        desde: Long,
        hasta: Long,
        ahora: Long
    ): Map<EstadoVisita, Int> = EstadoVisita.entries.associateWith { e ->
        puntos(viviendas, actividades, setOf(e), desde, hasta, ahora).size
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
