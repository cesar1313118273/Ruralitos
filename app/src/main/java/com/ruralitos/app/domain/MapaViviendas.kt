package com.ruralitos.app.domain

import com.ruralitos.app.data.local.dao.ViviendaMapaFila
import java.text.Normalizer
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Qué muestra el mapa general de viviendas y de qué color. Todo sale de datos que ya existen en la ficha:
 * la calificación de riesgo, el borrador, las visitas de seguimiento atrasadas y el estado de sincronización.
 */
enum class EstadoVivienda(val etiqueta: String, val colorHex: String) {
    RIESGO_ALTO("Riesgo alto", "#D32F2F"),
    PENDIENTE("Pendiente", "#F7941D"),
    SIN_SINCRONIZAR("Sin sincronizar", "#9E9E9E"),
    AL_DIA("Al día", "#0889A0")
}

enum class FiltroVivienda(val etiqueta: String) {
    TODAS("Todas"),
    PENDIENTES("Pendientes"),
    RIESGO_ALTO("Riesgo alto"),
    SIN_SINCRONIZAR("Sin sincronizar")
}

object MapaViviendas {
    /** Un punto muestra lo más urgente de la familia: riesgo alto, luego pendiente, luego sin sincronizar. */
    fun estado(fila: ViviendaMapaFila): EstadoVivienda = when {
        fila.nivelRiesgo.equals("ALTO", ignoreCase = true) -> EstadoVivienda.RIESGO_ALTO
        fila.estado == "BORRADOR" || fila.visitasAtrasadas > 0 -> EstadoVivienda.PENDIENTE
        fila.syncEstado != "SINCRONIZADO" -> EstadoVivienda.SIN_SINCRONIZAR
        else -> EstadoVivienda.AL_DIA
    }

    fun cumpleFiltro(fila: ViviendaMapaFila, filtro: FiltroVivienda): Boolean = when (filtro) {
        FiltroVivienda.TODAS -> true
        FiltroVivienda.PENDIENTES -> estado(fila) == EstadoVivienda.PENDIENTE
        FiltroVivienda.RIESGO_ALTO -> estado(fila) == EstadoVivienda.RIESGO_ALTO
        FiltroVivienda.SIN_SINCRONIZAR -> fila.syncEstado != "SINCRONIZADO"
    }

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD).filterNot { it in '̀'..'ͯ' }.lowercase().trim()

    /** Busca por nombre del jefe o jefa, cédula, número de ficha, barrio o casa; sin tildes ni mayúsculas. */
    fun coincide(fila: ViviendaMapaFila, consulta: String): Boolean {
        val q = normalizar(consulta)
        if (q.isEmpty()) return true
        return q.split(' ').filter { it.isNotEmpty() }.all { palabra ->
            listOf(fila.jefe, fila.cedula, fila.numero, fila.barrio, fila.casa).any { normalizar(it).contains(palabra) }
        }
    }

    fun filtrar(filas: List<ViviendaMapaFila>, filtro: FiltroVivienda, consulta: String): List<ViviendaMapaFila> =
        filas.filter { cumpleFiltro(it, filtro) && coincide(it, consulta) }

    fun conteos(filas: List<ViviendaMapaFila>): Map<EstadoVivienda, Int> =
        EstadoVivienda.entries.associateWith { e -> filas.count { estado(it) == e } }

    /** GeoJSON para MapLibre: un punto por vivienda con su color; MapLibre las agrupa solo al alejar el mapa. */
    fun geoJson(filas: List<ViviendaMapaFila>): String {
        val sb = StringBuilder("""{"type":"FeatureCollection","features":[""")
        filas.forEachIndexed { i, f ->
            if (i > 0) sb.append(',')
            val e = estado(f)
            sb.append("""{"type":"Feature","geometry":{"type":"Point","coordinates":[${f.longitud},${f.latitud}]},""")
            sb.append(""""properties":{"id":${f.fichaId},"color":"${e.colorHex}","estado":"${e.name}"}}""")
        }
        return sb.append("]}").toString()
    }

    /** Distancia en línea recta (metros) entre dos puntos. */
    fun distanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = (lat2 - lat1) * PI / 180
        val dLon = (lon2 - lon1) * PI / 180
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** «850 m» o «1,2 km», con coma decimal. */
    fun textoDistancia(metros: Double): String =
        if (metros < 1000) "${(metros / 10).roundToInt() * 10} m"
        else "%.1f km".format(java.util.Locale("es"), metros / 1000.0)

    /** Tiempo a pie en línea recta a 4,5 km/h, redondeado a 5 minutos. */
    fun minutosAPie(metros: Double): Int = (((metros / 1000.0) / 4.5 * 60) / 5).roundToInt().coerceAtLeast(1) * 5
}
