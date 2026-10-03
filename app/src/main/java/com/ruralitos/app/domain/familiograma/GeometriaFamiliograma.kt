package com.ruralitos.app.domain.familiograma

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Rectángulo en unidades de lienzo. */
data class Caja(val izquierda: Float, val arriba: Float, val derecha: Float, val abajo: Float) {
    val ancho: Float get() = derecha - izquierda
    val alto: Float get() = abajo - arriba

    fun unir(otra: Caja) = Caja(
        min(izquierda, otra.izquierda), min(arriba, otra.arriba),
        max(derecha, otra.derecha), max(abajo, otra.abajo)
    )

    fun expandir(margen: Float) = Caja(izquierda - margen, arriba - margen, derecha + margen, abajo + margen)
}

/**
 * La "hoja" del familiograma: el rectángulo que se inserta en el Excel y el PDF, con las tres franjas de
 * generaciones que ya trae la ficha (ABUELOS, PADRES, HIJOS). Las proporciones salen de las filas de la
 * hoja 4 de la plantilla (filas 3 a 7, 8 a 17 y 18 a 32), así lo que se dibuja cae en la franja correcta.
 */
object HojaFamiliograma {
    const val ANCHO = 1000f
    const val ALTO = 545.4f
    const val FIN_ABUELOS = ALTO * 0.1873f
    const val FIN_PADRES = ALTO * 0.5167f

    val caja = Caja(0f, 0f, ANCHO, ALTO)

    /** Altura (en el lienzo) donde se coloca una generación: 0 abuelos, 1 padres, 2 hijos. */
    fun alturaGeneracion(generacion: Int): Float = when (generacion) {
        0 -> FIN_ABUELOS * 0.5f + 2f
        1 -> (FIN_ABUELOS + FIN_PADRES) / 2f - 10f
        else -> (FIN_PADRES + ALTO) / 2f - 14f
    }

    /** ¿Algún elemento queda fuera de la hoja y no saldría en el Excel con las franjas bien puestas? */
    fun hayElementosFuera(doc: Familiograma): Boolean {
        val c = GeometriaFamiliograma.limites(doc) ?: return false
        return c.izquierda < -2f || c.arriba < -2f || c.derecha > ANCHO + 2f || c.abajo > ALTO + 2f
    }
}

/** Punto de una ruta con la dirección de su tramo (para dibujar marcas perpendiculares). */
data class PuntoEnRuta(val punto: Punto, val dx: Float, val dy: Float)

/**
 * Cálculos de posición de las líneas del familiograma. Todo se recalcula a partir de las
 * personas, por eso las líneas se adaptan solas cuando se mueve una figura.
 */
object GeometriaFamiliograma {
    const val MARGEN_HOGAR_LATERAL = 36f
    const val MARGEN_HOGAR_ARRIBA = 30f
    const val MARGEN_HOGAR_ABAJO = 46f
    const val DISTANCIA_ABORTO_MINIMA = 22f

    fun mitad(doc: Familiograma, id: String): Float = when {
        doc.persona(id) != null -> MEDIA_PERSONA
        else -> MEDIA_ENTORNO
    }

    fun centro(doc: Familiograma, id: String): Punto? =
        doc.persona(id)?.let { Punto(it.x, it.y) } ?: doc.entorno(id)?.let { Punto(it.x, it.y) }

    fun posicion(doc: Familiograma, ancla: Ancla): Punto? {
        val c = centro(doc, ancla.elementoId) ?: return null
        val m = mitad(doc, ancla.elementoId)
        return when (ancla.lado) {
            Lado.ARRIBA -> Punto(c.x, c.y - m)
            Lado.ABAJO -> Punto(c.x, c.y + m)
            Lado.IZQUIERDA -> Punto(c.x - m, c.y)
            Lado.DERECHA -> Punto(c.x + m, c.y)
        }
    }

    /** Ruta ortogonal de una unión entre dos anclas. */
    fun rutaUnion(doc: Familiograma, union: Union): List<Punto> {
        val a = posicion(doc, union.a) ?: return emptyList()
        val b = posicion(doc, union.b) ?: return emptyList()
        val ha = union.a.lado.horizontal
        val hb = union.b.lado.horizontal
        return when {
            ha && hb -> {
                val mx = (a.x + b.x) / 2
                listOf(a, Punto(mx, a.y), Punto(mx, b.y), b)
            }
            !ha && !hb -> {
                val my = (a.y + b.y) / 2
                listOf(a, Punto(a.x, my), Punto(b.x, my), b)
            }
            ha -> listOf(a, Punto(b.x, a.y), b)
            else -> listOf(a, Punto(a.x, b.y), b)
        }
    }

    fun puntoMedio(ruta: List<Punto>): PuntoEnRuta? {
        if (ruta.size < 2) return null
        val tramos = (1 until ruta.size).map { hypot(ruta[it].x - ruta[it - 1].x, ruta[it].y - ruta[it - 1].y) }
        var resto = tramos.sum() / 2
        for (i in tramos.indices) {
            val largo = tramos[i]
            if (resto <= largo || i == tramos.lastIndex) {
                val f = if (largo > 0f) resto / largo else 0f
                val a = ruta[i]
                val b = ruta[i + 1]
                val dx = if (largo > 0f) (b.x - a.x) / largo else 1f
                val dy = if (largo > 0f) (b.y - a.y) / largo else 0f
                return PuntoEnRuta(Punto(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f), dx, dy)
            }
            resto -= largo
        }
        return null
    }

    fun puntoMedioUnion(doc: Familiograma, union: Union): PuntoEnRuta? = puntoMedio(rutaUnion(doc, union))

    /** Punto de donde nace la línea de un hijo (mitad de la unión o ancla del progenitor). */
    fun origenFiliacion(doc: Familiograma, f: Filiacion): Punto? =
        f.unionId?.let { id -> doc.union(id)?.let { puntoMedioUnion(doc, it)?.punto } }
            ?: f.progenitor?.let { posicion(doc, it) }

    fun rutaFiliacion(doc: Familiograma, f: Filiacion): List<Punto> {
        val origen = origenFiliacion(doc, f) ?: return emptyList()
        val destino = posicion(doc, f.hijo) ?: return emptyList()
        return rutaVertical(origen, destino)
    }

    fun rutaAborto(doc: Familiograma, a: Aborto): List<Punto> {
        val origen = doc.union(a.unionId)?.let { puntoMedioUnion(doc, it)?.punto } ?: return emptyList()
        return rutaVertical(origen, Punto(a.x, a.y))
    }

    private fun rutaVertical(origen: Punto, destino: Punto): List<Punto> {
        val medio = (origen.y + destino.y) / 2
        return listOf(origen, Punto(origen.x, medio), Punto(destino.x, medio), destino)
    }

    fun rutaVinculo(doc: Familiograma, v: Vinculo): List<Punto> {
        val a = posicion(doc, v.a) ?: return emptyList()
        val b = posicion(doc, v.b) ?: return emptyList()
        return listOf(a, b)
    }

    // ---- puntería --------------------------------------------------------------------

    fun distanciaARuta(p: Punto, ruta: List<Punto>): Float {
        var mejor = Float.MAX_VALUE
        for (i in 1 until ruta.size) mejor = min(mejor, distanciaASegmento(p, ruta[i - 1], ruta[i]))
        return mejor
    }

    fun distanciaASegmento(p: Punto, a: Punto, b: Punto): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val largo2 = dx * dx + dy * dy
        val t = if (largo2 == 0f) 0f else (((p.x - a.x) * dx + (p.y - a.y) * dy) / largo2).coerceIn(0f, 1f)
        return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
    }

    /** Ancla (de otro elemento) más cercana a [p], si está dentro del [radio]. */
    fun anclaCercana(doc: Familiograma, p: Punto, excluirId: String?, radio: Float): Ancla? {
        var mejor: Ancla? = null
        var distancia = radio
        val ids = doc.personas.map { it.id } + doc.entornos.map { it.id }
        for (id in ids) {
            if (id == excluirId) continue
            for (lado in Lado.entries) {
                val ancla = Ancla(id, lado)
                val pos = posicion(doc, ancla) ?: continue
                val d = hypot(p.x - pos.x, p.y - pos.y)
                if (d < distancia) {
                    distancia = d
                    mejor = ancla
                }
            }
        }
        return mejor
    }

    /** Unión más cercana a [p] (para colgar un hijo o un aborto), si está dentro de la [tolerancia]. */
    fun unionCercana(doc: Familiograma, p: Punto, tolerancia: Float, excluirPersonaId: String? = null): Union? =
        doc.uniones
            .filter { excluirPersonaId == null || (it.a.elementoId != excluirPersonaId && it.b.elementoId != excluirPersonaId) }
            .map { it to distanciaARuta(p, rutaUnion(doc, it)) }
            .filter { it.second <= tolerancia }
            .minByOrNull { it.second }
            ?.first

    // ---- límites ---------------------------------------------------------------------

    /** Recuadro punteado que encierra a quienes viven en el hogar. */
    fun cajaHogar(doc: Familiograma): Caja? {
        val miembros = doc.personas.filter { it.enHogar }
        if (miembros.isEmpty()) return null
        return Caja(
            miembros.minOf { it.x } - MEDIA_PERSONA - MARGEN_HOGAR_LATERAL,
            miembros.minOf { it.y } - MEDIA_PERSONA - MARGEN_HOGAR_ARRIBA,
            miembros.maxOf { it.x } + MEDIA_PERSONA + MARGEN_HOGAR_LATERAL,
            miembros.maxOf { it.y } + MEDIA_PERSONA + MARGEN_HOGAR_ABAJO
        )
    }

    /** Caja que contiene todo el dibujo (para ajustar la vista o exportar). */
    fun limites(doc: Familiograma): Caja? {
        var caja: Caja? = null
        fun incluir(c: Caja) { caja = caja?.unir(c) ?: c }
        cajaHogar(doc)?.let(::incluir)
        doc.personas.forEach {
            incluir(Caja(it.x - MEDIA_PERSONA - 14, it.y - MEDIA_PERSONA - 4, it.x + MEDIA_PERSONA + 14, it.y + MEDIA_PERSONA + 40))
        }
        doc.entornos.forEach {
            incluir(Caja(it.x - MEDIA_ENTORNO - 26, it.y - MEDIA_ENTORNO - 4, it.x + MEDIA_ENTORNO + 26, it.y + MEDIA_ENTORNO + 26))
        }
        doc.textos.forEach {
            incluir(Caja(it.x - 4, it.y - 18, it.x + 15f * (it.texto.lines().maxOfOrNull { l -> l.length } ?: 1).coerceAtLeast(1) * 0.6f + 4, it.y + 18f * (it.texto.lines().size - 1) + 6))
        }
        doc.abortos.forEach { incluir(Caja(it.x - 8, it.y - 8, it.x + 8, it.y + 8)) }
        doc.trazos.forEach { t ->
            if (t.puntos.isEmpty()) return@forEach
            val x0 = t.puntos.minOf { it.x }; val x1 = t.puntos.maxOf { it.x }
            val y0 = t.puntos.minOf { it.y }; val y1 = t.puntos.maxOf { it.y }
            incluir(Caja(x0 - 4, y0 - 4, x1 + 4, y1 + 4))
        }
        return caja
    }

    /** Posición libre cerca de [deseada] para colocar un elemento nuevo sin taparlos. */
    fun posicionLibre(doc: Familiograma, deseada: Punto, separacion: Float = 54f): Punto {
        val ocupados = doc.personas.map { Punto(it.x, it.y) } + doc.entornos.map { Punto(it.x, it.y) }
        var p = deseada
        var intentos = 0
        while (ocupados.any { hypot(it.x - p.x, it.y - p.y) < separacion } && intentos < 40) {
            intentos++
            p = Punto(deseada.x + (intentos % 5) * separacion, deseada.y + (intentos / 5) * separacion)
        }
        return p
    }
}
