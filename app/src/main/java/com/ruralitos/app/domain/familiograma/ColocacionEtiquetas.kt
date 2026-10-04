package com.ruralitos.app.domain.familiograma

import kotlin.math.max
import kotlin.math.min

/** Qué se escribe junto a una persona. */
enum class TipoEtiqueta { SIGLAS, NOMBRE }

/** Un texto que hay que colocar junto a una persona; [ancho] y [alto] son su tamaño ya medido, en unidades de lienzo. */
data class BloqueEtiqueta(val personaId: String, val tipo: TipoEtiqueta, val ancho: Float, val alto: Float)

/** Tramo recto de una línea del dibujo. [peso] dice cuánto estorba que un texto la cruce (el marco punteado del hogar, poco). */
data class Segmento(val a: Punto, val b: Punto, val peso: Int = 3)

/**
 * Todo lo que ya ocupa sitio en el dibujo: figuras (cajas) y líneas (segmentos). Una etiqueta o la leyenda solo
 * se colocan donde no tapen nada de esto.
 */
class Obstaculos(val cajas: List<Caja>, val segmentos: List<Segmento>) {
    /**
     * Cuánto estorba poner algo en [caja]; 0 significa que el sitio está libre. Taparse con una figura pesa mucho más que
     * cruzar una línea, y cruzar el marco punteado del hogar casi nada.
     */
    fun choques(caja: Caja, tolerancia: Float = 1.5f): Int {
        val c = caja.expandir(tolerancia)
        val cajasTocadas = cajas.count { solapan(c, it) }
        val lineasTocadas = segmentos.filter { cortaCaja(it, c) }.sumOf { it.peso }
        return cajasTocadas * 10 + lineasTocadas
    }

    fun mas(extra: Collection<Caja>) = Obstaculos(cajas + extra, segmentos)

    companion object {
        fun solapan(a: Caja, b: Caja) =
            a.izquierda < b.derecha && a.derecha > b.izquierda && a.arriba < b.abajo && a.abajo > b.arriba

        /** ¿El segmento pasa por dentro de la caja? (recorte de Liang–Barsky) */
        fun cortaCaja(s: Segmento, c: Caja): Boolean {
            var t0 = 0f
            var t1 = 1f
            val dx = s.b.x - s.a.x
            val dy = s.b.y - s.a.y
            fun recortar(p: Float, q: Float): Boolean {
                if (p == 0f) return q >= 0f
                val r = q / p
                if (p < 0f) {
                    if (r > t1) return false
                    t0 = max(t0, r)
                } else {
                    if (r < t0) return false
                    t1 = min(t1, r)
                }
                return true
            }
            return recortar(-dx, s.a.x - c.izquierda) && recortar(dx, c.derecha - s.a.x) &&
                recortar(-dy, s.a.y - c.arriba) && recortar(dy, c.abajo - s.a.y)
        }
    }
}

/**
 * Coloca las siglas de las patologías, los nombres y la leyenda de abreviaturas donde no tapen líneas ni figuras.
 * Antes las siglas iban siempre debajo de cada persona, que es justo por donde bajan las líneas de los hijos; ahora cada
 * texto prueba varios lados y se queda con el primero que está libre. Todo se recalcula al mover una figura.
 */
object ColocacionEtiquetas {
    private const val SEPARACION = 6f

    /** Figuras y líneas del familiograma que no se deben tapar. */
    fun obstaculos(doc: Familiograma): Obstaculos {
        val cajas = mutableListOf<Caja>()
        val segmentos = mutableListOf<Segmento>()

        fun ruta(puntos: List<Punto>, peso: Int = 3) {
            for (i in 1 until puntos.size) segmentos += Segmento(puntos[i - 1], puntos[i], peso)
        }

        doc.personas.forEach {
            val m = MEDIA_PERSONA + 3f // la cruz del fallecido sobresale del contorno
            cajas += Caja(it.x - m, it.y - m, it.x + m, it.y + m)
            if (it.informante) {
                val punta = MEDIA_PERSONA + 2f
                cajas += Caja(it.x + punta, it.y + punta, it.x + punta + 26f, it.y + punta + 26f)
            }
        }
        doc.entornos.forEach {
            cajas += Caja(it.x - MEDIA_ENTORNO, it.y - MEDIA_ENTORNO, it.x + MEDIA_ENTORNO, it.y + MEDIA_ENTORNO + 20f)
        }
        doc.textos.forEach {
            val lineas = it.texto.lines()
            val ancho = 15f * (lineas.maxOfOrNull { l -> l.length } ?: 1).coerceAtLeast(1) * 0.6f
            cajas += Caja(it.x - 4f, it.y - 16f, it.x + ancho + 4f, it.y + 18f * (lineas.size - 1) + 4f)
        }
        doc.abortos.forEach { cajas += Caja(it.x - 8f, it.y - 8f, it.x + 8f, it.y + 8f) }
        GeometriaFamiliograma.cajaHogar(doc)?.let { h ->
            // solo el marco punteado y su rótulo: dentro del hogar sí se puede escribir
            ruta(
                listOf(Punto(h.izquierda, h.arriba), Punto(h.derecha, h.arriba), Punto(h.derecha, h.abajo), Punto(h.izquierda, h.abajo), Punto(h.izquierda, h.arriba)),
                peso = 1
            )
            cajas += Caja(h.izquierda + 8f, h.arriba + 4f, h.izquierda + 64f, h.arriba + 24f)
        }
        doc.uniones.forEach { ruta(GeometriaFamiliograma.rutaUnion(doc, it)) }
        doc.filiaciones.forEach { ruta(GeometriaFamiliograma.rutaFiliacion(doc, it)) }
        doc.abortos.forEach { ruta(GeometriaFamiliograma.rutaAborto(doc, it)) }
        doc.vinculos.forEach { ruta(GeometriaFamiliograma.rutaVinculo(doc, it)) }
        doc.trazos.forEach { t ->
            when (t.tipo) {
                TipoTrazo.ENCIERRO -> if (t.puntos.size >= 2) {
                    val a = t.puntos.first(); val b = t.puntos.last()
                    val x0 = min(a.x, b.x); val x1 = max(a.x, b.x); val y0 = min(a.y, b.y); val y1 = max(a.y, b.y)
                    ruta(listOf(Punto(x0, y0), Punto(x1, y0), Punto(x1, y1), Punto(x0, y1), Punto(x0, y0)))
                }
                else -> ruta(t.puntos)
            }
        }
        return Obstaculos(cajas, segmentos)
    }

    /** Lados donde puede ir un texto de [ancho] x [alto] junto a una persona, del más natural al menos. */
    internal fun candidatos(p: Persona, ancho: Float, alto: Float): List<Caja> {
        val m = MEDIA_PERSONA
        val lado = m + SEPARACION + 5f // deja pasar la cruz del fallecido
        val abajo = p.y + m + SEPARACION
        val arriba = p.y - m - SEPARACION - alto
        fun caja(izq: Float, top: Float) = Caja(izq, top, izq + ancho, top + alto)
        return listOf(
            caja(p.x - ancho / 2f, abajo),                 // debajo, centrado (lo de siempre)
            caja(p.x + 3f, abajo),                         // debajo, hacia la derecha de la línea central
            caja(p.x - 3f - ancho, abajo),                 // debajo, hacia la izquierda
            caja(p.x + lado, p.y - alto / 2f),             // a la derecha
            caja(p.x - lado - ancho, p.y - alto / 2f),     // a la izquierda
            caja(p.x - ancho / 2f, arriba),                // arriba, centrado
            caja(p.x + 3f, arriba),
            caja(p.x - 3f - ancho, arriba),
            caja(p.x + lado, abajo),                       // esquina inferior derecha
            caja(p.x - lado - ancho, abajo),
            caja(p.x + lado, arriba),
            caja(p.x - lado - ancho, arriba)
        )
    }

    /**
     * Devuelve dónde va cada bloque. Las siglas se colocan primero; el nombre intenta ir justo debajo de ellas.
     * Un sitio ocupado por otra etiqueta ya colocada también cuenta como ocupado.
     */
    fun colocar(doc: Familiograma, bloques: List<BloqueEtiqueta>): Map<Pair<String, TipoEtiqueta>, Caja> {
        val resultado = linkedMapOf<Pair<String, TipoEtiqueta>, Caja>()
        var ocupado = obstaculos(doc)
        val orden = bloques.sortedWith(compareBy({ it.tipo.ordinal }, { b -> doc.persona(b.personaId)?.y ?: 0f }, { b -> doc.persona(b.personaId)?.x ?: 0f }))
        for (bloque in orden) {
            val persona = doc.persona(bloque.personaId) ?: continue
            val candidatos = buildList {
                if (bloque.tipo == TipoEtiqueta.NOMBRE) {
                    resultado[bloque.personaId to TipoEtiqueta.SIGLAS]?.let { siglas ->
                        add(Caja(persona.x - bloque.ancho / 2f, siglas.abajo + 3f, persona.x + bloque.ancho / 2f, siglas.abajo + 3f + bloque.alto))
                    }
                }
                addAll(candidatos(persona, bloque.ancho, bloque.alto))
            }
            val elegido = candidatos.firstOrNull { ocupado.choques(it) == 0 }
                ?: candidatos.minByOrNull { ocupado.choques(it) } // sin sitio libre: el que menos estorba
                ?: continue
            resultado[bloque.personaId to bloque.tipo] = elegido
            ocupado = ocupado.mas(listOf(elegido))
        }
        return resultado
    }

    /**
     * Sitio de la leyenda de abreviaturas nuevas ([ancho] x [alto]): al lado de todo el dibujo y sin tapar nada.
     * Primero a la derecha de todo lo dibujado; si no cabe dentro de la hoja, en cualquier hueco libre de sus bordes;
     * y si no hay, fuera de la hoja, a la derecha.
     */
    fun colocarLeyenda(
        doc: Familiograma,
        etiquetas: Collection<Caja>,
        ancho: Float,
        alto: Float,
        hoja: Caja = HojaFamiliograma.caja,
        margen: Float = HojaFamiliograma.ANCHO * 0.02f
    ): Caja {
        val ocupado = obstaculos(doc).mas(etiquetas)
        val contenido = (etiquetas.fold<Caja, Caja?>(GeometriaFamiliograma.limites(doc)) { acumulada, c -> acumulada?.unir(c) ?: c })
        fun caja(izq: Float, top: Float) = Caja(izq, top, izq + ancho, top + alto)
        val dentro = hoja.derecha - margen
        val candidatos = buildList {
            if (contenido != null) add(caja(contenido.derecha + margen, hoja.arriba + margen))
            // los bordes derecho e izquierdo de la hoja, de arriba hacia abajo
            var y = hoja.arriba + margen
            while (y + alto <= hoja.abajo - margen + 0.5f) {
                add(caja(dentro - ancho, y))
                y += 12f
            }
            y = hoja.arriba + margen
            while (y + alto <= hoja.abajo - margen + 0.5f) {
                add(caja(hoja.izquierda + margen, y))
                y += 12f
            }
        }.filter { it.derecha <= dentro + 0.5f && it.izquierda >= hoja.izquierda + margen - 0.5f }
        candidatos.firstOrNull { ocupado.choques(it, 2f) == 0 }?.let { return it }
        val afuera = max(hoja.derecha, contenido?.derecha ?: hoja.derecha) + margen
        return caja(afuera, hoja.arriba + margen)
    }
}
