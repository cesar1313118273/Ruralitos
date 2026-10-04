package com.ruralitos.app.ui.familiograma

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.ruralitos.app.domain.SaludoProfesional
import com.ruralitos.app.domain.familiograma.Aborto
import com.ruralitos.app.domain.familiograma.AbreviaturasPatologia
import com.ruralitos.app.domain.familiograma.Ancla
import com.ruralitos.app.domain.familiograma.BloqueEtiqueta
import com.ruralitos.app.domain.familiograma.ColocacionEtiquetas
import com.ruralitos.app.domain.familiograma.TipoEtiqueta
import com.ruralitos.app.domain.familiograma.COLOR_TINTA
import com.ruralitos.app.domain.familiograma.Entorno
import com.ruralitos.app.domain.familiograma.Familiograma
import com.ruralitos.app.domain.familiograma.Filiacion
import com.ruralitos.app.domain.familiograma.Caja
import com.ruralitos.app.domain.familiograma.GeometriaFamiliograma
import com.ruralitos.app.domain.familiograma.HojaFamiliograma
import com.ruralitos.app.domain.familiograma.MEDIA_ENTORNO
import com.ruralitos.app.domain.familiograma.MEDIA_PERSONA
import com.ruralitos.app.domain.familiograma.Persona
import com.ruralitos.app.domain.familiograma.Punto
import com.ruralitos.app.domain.familiograma.SexoPersona
import com.ruralitos.app.domain.familiograma.TipoEntorno
import com.ruralitos.app.domain.familiograma.TipoTrazo
import com.ruralitos.app.domain.familiograma.TipoUnion
import com.ruralitos.app.domain.familiograma.Trazo
import com.ruralitos.app.domain.familiograma.Union
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val ALTO_LINEA_SIGLAS = 15f

internal const val COLOR_CIAN = 0xFF0889A0.toInt()
internal const val COLOR_CIAN_OSCURO = 0xFF066B7E.toInt()
internal const val COLOR_CIAN_SUAVE = 0xFFE3F4F7.toInt()
internal const val COLOR_VERDE = 0xFF3A7F1F.toInt()
internal const val COLOR_ROJO = 0xFFC62828.toInt()
internal const val COLOR_AZUL = 0xFF1565C0.toInt()
internal const val COLOR_GRIS = 0xFF5B7083.toInt()

/** Datos temporales que solo se dibujan en el editor (selección, puntos de conexión, línea en curso). */
data class SuperposicionEditor(
    val seleccionId: String? = null,
    val idsConAnclas: Set<String> = emptySet(),
    val origenEnlace: Ancla? = null,
    val puntoEnlace: Punto? = null,
    val anclaDestino: Ancla? = null,
    val trazoEnCurso: Trazo? = null,
    val escala: Float = 1f,
    val densidad: Float = 1f
)

/**
 * Dibuja el familiograma en un [Canvas] de Android. Se usa tanto en la pantalla de edición como
 * para generar el PNG que va al Excel y al PDF, así lo que se ve al dibujar es lo que se imprime.
 */
class DibujanteFamiliograma {
    private val tinta = pincel(Paint.Style.STROKE, COLOR_TINTA, 1.8f)
    private val relleno = pincel(Paint.Style.FILL, 0xFFFFFFFF.toInt())
    private val cianLinea = pincel(Paint.Style.STROKE, COLOR_CIAN, 1.6f)
    private val punteado = pincel(Paint.Style.STROKE, COLOR_CIAN, 2f).apply {
        pathEffect = DashPathEffect(floatArrayOf(2f, 5f), 0f)
    }
    private val hogar = pincel(Paint.Style.STROKE, COLOR_CIAN, 1.6f).apply {
        pathEffect = DashPathEffect(floatArrayOf(7f, 5f), 0f)
    }
    private val textoEdad = texto(14f, COLOR_TINTA, negrita = false)
    private val textoCodigo = texto(13f, COLOR_CIAN, negrita = true)
    private val textoNombre = texto(12f, COLOR_GRIS, negrita = false)
    private val textoEtiqueta = texto(13f, COLOR_CIAN_OSCURO, negrita = false)
    private val textoLibre = texto(15f, COLOR_TINTA, negrita = false).apply { textAlign = Paint.Align.LEFT }
    private val iconoLinea = pincel(Paint.Style.STROKE, COLOR_CIAN_OSCURO, 1.6f)
    private val iconoRelleno = pincel(Paint.Style.FILL, COLOR_CIAN_SUAVE)
    private val trazoLibre = pincel(Paint.Style.STROKE, COLOR_TINTA, 2.4f)
    private val relleno2 = pincel(Paint.Style.FILL, COLOR_TINTA)

    private fun pincel(estilo: Paint.Style, color: Int, grosor: Float = 1f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = estilo
        this.color = color
        strokeWidth = grosor
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun texto(tamano: Float, color: Int, negrita: Boolean) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = tamano
        textAlign = Paint.Align.CENTER
        isFakeBoldText = negrita
    }

    private fun Canvas.textoCentradoV(texto: String, x: Float, y: Float, p: Paint) {
        val fm = p.fontMetrics
        drawText(texto, x, y - (fm.ascent + fm.descent) / 2f, p)
    }

    // ------------------------------------------------------------------------------------
    // Contenido (editor y exportación)
    // ------------------------------------------------------------------------------------

    fun dibujar(c: Canvas, doc: Familiograma) {
        dibujarHogar(c, doc)
        doc.trazos.forEach { dibujarTrazo(c, it) }
        doc.vinculos.forEach { v ->
            val ruta = GeometriaFamiliograma.rutaVinculo(doc, v)
            if (ruta.size == 2) c.drawLine(ruta[0].x, ruta[0].y, ruta[1].x, ruta[1].y, punteado)
        }
        doc.uniones.forEach { dibujarUnion(c, doc, it) }
        doc.filiaciones.forEach { dibujarFiliacion(c, doc, it) }
        doc.abortos.forEach { dibujarAborto(c, doc, it) }
        doc.entornos.forEach { dibujarEntorno(c, it) }
        val etiquetas = etiquetasDe(doc)
        doc.personas.forEach { dibujarPersona(c, doc, it, etiquetas) }
        doc.textos.forEach { t ->
            val lineas = t.texto.split('\n')
            lineas.forEachIndexed { i, linea -> c.drawText(linea, t.x, t.y + i * 18f, textoLibre) }
        }
    }

    private fun dibujarHogar(c: Canvas, doc: Familiograma) {
        val caja = GeometriaFamiliograma.cajaHogar(doc) ?: return
        c.drawRoundRect(RectF(caja.izquierda, caja.arriba, caja.derecha, caja.abajo), 22f, 22f, hogar)
        val etiqueta = texto(13f, COLOR_CIAN, negrita = true).apply { textAlign = Paint.Align.LEFT }
        c.drawText("Hogar", caja.izquierda + 14f, caja.arriba + 18f, etiqueta)
    }

    private fun rutaAPath(ruta: List<Punto>): Path = Path().apply {
        ruta.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
    }

    private fun dibujarUnion(c: Canvas, doc: Familiograma, u: Union) {
        val ruta = GeometriaFamiliograma.rutaUnion(doc, u)
        if (ruta.size < 2) return
        if (u.tipo == TipoUnion.CONSANGUINEA) {
            for (signo in listOf(-1f, 1f)) {
                for (i in 1 until ruta.size) {
                    val a = ruta[i - 1]; val b = ruta[i]
                    val ang = atan2(b.y - a.y, b.x - a.x) + Math.PI.toFloat() / 2
                    val ox = cos(ang) * 3f * signo; val oy = sin(ang) * 3f * signo
                    c.drawLine(a.x + ox, a.y + oy, b.x + ox, b.y + oy, tinta)
                }
            }
        } else {
            c.drawPath(rutaAPath(ruta), tinta)
        }
        val marcas = when (u.tipo) {
            TipoUnion.SEPARACION -> listOf(0f)
            TipoUnion.DIVORCIO -> listOf(-5f, 5f)
            else -> emptyList()
        }
        if (marcas.isNotEmpty()) {
            val medio = GeometriaFamiliograma.puntoMedio(ruta) ?: return
            val nx = -medio.dy; val ny = medio.dx
            val vx = nx * 0.94f + medio.dx * 0.34f
            val vy = ny * 0.94f + medio.dy * 0.34f
            marcas.forEach { o ->
                val cx = medio.punto.x + medio.dx * o
                val cy = medio.punto.y + medio.dy * o
                c.drawLine(cx - vx * 11f, cy - vy * 11f, cx + vx * 11f, cy + vy * 11f, tinta)
            }
        }
    }

    private fun dibujarFiliacion(c: Canvas, doc: Familiograma, f: Filiacion) {
        val ruta = GeometriaFamiliograma.rutaFiliacion(doc, f)
        if (ruta.size < 2) return
        c.drawPath(rutaAPath(ruta), tinta)
        if (f.adoptado) {
            val hijo = doc.persona(f.hijo.elementoId) ?: return
            val m = MEDIA_PERSONA
            for (lado in listOf(-1f, 1f)) {
                val x1 = hijo.x + lado * (m + 7f)
                val x2 = hijo.x + lado * (m + 12f)
                val p = Path().apply {
                    moveTo(x1, hijo.y - m - 4f); lineTo(x2, hijo.y - m - 4f)
                    lineTo(x2, hijo.y + m + 4f); lineTo(x1, hijo.y + m + 4f)
                }
                c.drawPath(p, tinta)
            }
        }
    }

    private fun dibujarAborto(c: Canvas, doc: Familiograma, a: Aborto) {
        val ruta = GeometriaFamiliograma.rutaAborto(doc, a)
        if (ruta.size < 2) return
        val grueso = Paint(tinta).apply { strokeWidth = 3.2f }
        c.drawPath(rutaAPath(ruta), grueso)
        c.drawCircle(a.x, a.y, 6.5f, relleno2)
    }

    private fun dibujarTrazo(c: Canvas, t: Trazo) {
        if (t.puntos.size < 2) return
        val p = Paint(trazoLibre).apply {
            color = t.color
            strokeWidth = t.grosor
            if (t.punteado) pathEffect = DashPathEffect(floatArrayOf(t.grosor * 3f + 3f, t.grosor * 2f + 3f), 0f)
        }
        val a = t.puntos.first()
        val b = t.puntos.last()
        when (t.tipo) {
            TipoTrazo.LAPIZ -> {
                val ruta = Path()
                ruta.moveTo(a.x, a.y)
                for (i in 1 until t.puntos.size) {
                    val ant = t.puntos[i - 1]; val act = t.puntos[i]
                    ruta.quadTo(ant.x, ant.y, (ant.x + act.x) / 2f, (ant.y + act.y) / 2f)
                }
                ruta.lineTo(b.x, b.y)
                c.drawPath(ruta, p)
            }
            TipoTrazo.LINEA -> c.drawLine(a.x, a.y, b.x, b.y, p)
            TipoTrazo.FLECHA -> {
                c.drawLine(a.x, a.y, b.x, b.y, p)
                val ang = atan2(b.y - a.y, b.x - a.x)
                val largo = 9f + t.grosor * 1.5f
                for (delta in listOf(0.5f, -0.5f)) {
                    c.drawLine(
                        b.x, b.y,
                        b.x - largo * cos(ang + delta), b.y - largo * sin(ang + delta),
                        Paint(p).apply { pathEffect = null }
                    )
                }
            }
            TipoTrazo.ENCIERRO -> c.drawOval(
                RectF(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y)), p
            )
        }
    }

    private fun dibujarPersona(
        c: Canvas,
        doc: Familiograma,
        p: Persona,
        etiquetas: Map<Pair<String, TipoEtiqueta>, Caja>
    ) {
        val m = MEDIA_PERSONA
        if (p.sexo == SexoPersona.HOMBRE) {
            c.drawRect(p.x - m, p.y - m, p.x + m, p.y + m, relleno)
            c.drawRect(p.x - m, p.y - m, p.x + m, p.y + m, tinta)
        } else {
            c.drawCircle(p.x, p.y, m, relleno)
            c.drawCircle(p.x, p.y, m, tinta)
        }
        if (p.edad.isNotBlank()) c.textoCentradoV(p.edad, p.x, p.y, textoEdad)
        if (p.fallecido) {
            val e = m + 3f
            val grueso = Paint(tinta).apply { strokeWidth = 2.2f }
            c.drawLine(p.x - e, p.y - e, p.x + e, p.y + e, grueso)
            c.drawLine(p.x + e, p.y - e, p.x - e, p.y + e, grueso)
        }
        if (p.informante) {
            val punta = Punto(p.x + m + 2f, p.y + m + 2f)
            val inicio = Punto(punta.x + 23f, punta.y + 23f)
            val grueso = Paint(tinta).apply { strokeWidth = 2.6f }
            c.drawLine(inicio.x, inicio.y, punta.x + 5f, punta.y + 5f, grueso)
            val ang = atan2(punta.y - inicio.y, punta.x - inicio.x)
            val flecha = Path().apply {
                moveTo(punta.x, punta.y)
                lineTo(punta.x - 12f * cos(ang - 0.42f), punta.y - 12f * sin(ang - 0.42f))
                lineTo(punta.x - 12f * cos(ang + 0.42f), punta.y - 12f * sin(ang + 0.42f))
                close()
            }
            c.drawPath(flecha, relleno2)
        }
        etiquetas[p.id to TipoEtiqueta.SIGLAS]?.let { caja ->
            lineasDeSiglas(doc, p).forEachIndexed { i, linea ->
                c.drawText(linea, (caja.izquierda + caja.derecha) / 2f, caja.arriba + 12f + i * ALTO_LINEA_SIGLAS, textoCodigo)
            }
        }
        etiquetas[p.id to TipoEtiqueta.NOMBRE]?.let { caja ->
            c.drawText(nombreCorto(p), (caja.izquierda + caja.derecha) / 2f, caja.arriba + 11f, textoNombre)
        }
    }

    private fun lineasDeSiglas(doc: Familiograma, p: Persona): List<String> =
        p.patologias.map { AbreviaturasPatologia.codigoDe(doc, it) }.distinct().chunked(3).map { it.joinToString(" ") }

    private fun nombreCorto(p: Persona): String =
        SaludoProfesional.nombreCorto("", p.nombre).ifBlank { p.nombre }.take(22)

    /**
     * Dónde va el texto de cada persona (siglas de sus patologías y, si se pide, su nombre). Cada texto se coloca en un lado
     * libre de líneas y figuras, no siempre debajo; se vuelve a calcular cada vez que se mueve algo.
     */
    fun etiquetasDe(doc: Familiograma): Map<Pair<String, TipoEtiqueta>, Caja> {
        val bloques = buildList {
            doc.personas.forEach { p ->
                val lineas = lineasDeSiglas(doc, p)
                if (lineas.isNotEmpty()) {
                    add(
                        BloqueEtiqueta(
                            p.id, TipoEtiqueta.SIGLAS,
                            lineas.maxOf { textoCodigo.measureText(it) } + 4f,
                            lineas.size * ALTO_LINEA_SIGLAS
                        )
                    )
                }
                if (doc.mostrarNombres && p.nombre.isNotBlank()) {
                    add(BloqueEtiqueta(p.id, TipoEtiqueta.NOMBRE, textoNombre.measureText(nombreCorto(p)) + 4f, 14f))
                }
            }
        }
        return ColocacionEtiquetas.colocar(doc, bloques)
    }

    private fun dibujarEntorno(c: Canvas, e: Entorno) {
        val m = MEDIA_ENTORNO
        val caja = RectF(e.x - m, e.y - m, e.x + m, e.y + m)
        c.drawRoundRect(caja, 10f, 10f, relleno)
        c.drawRoundRect(caja, 10f, 10f, cianLinea)
        c.save()
        c.translate(e.x, e.y)
        c.scale(1.2f, 1.2f)
        IconosEntorno.dibujar(c, e.tipo, iconoLinea, iconoRelleno)
        c.restore()
        if (e.etiqueta.isNotBlank()) c.drawText(e.etiqueta.take(24), e.x, e.y + m + 15f, textoEtiqueta)
    }

    // ------------------------------------------------------------------------------------
    // Superposición del editor
    // ------------------------------------------------------------------------------------

    fun dibujarSuperposicion(c: Canvas, doc: Familiograma, s: SuperposicionEditor) {
        val z = s.densidad / s.escala.coerceAtLeast(0.2f)
        s.trazoEnCurso?.let { dibujarTrazo(c, it) }
        s.seleccionId?.let { id ->
            val anillo = pincel(Paint.Style.STROKE, COLOR_CIAN, 1.6f * z).apply {
                pathEffect = DashPathEffect(floatArrayOf(4f * z, 3f * z), 0f)
            }
            doc.persona(id)?.let { p ->
                val r = MEDIA_PERSONA + 7f
                if (p.sexo == SexoPersona.HOMBRE) c.drawRoundRect(RectF(p.x - r, p.y - r, p.x + r, p.y + r), 6f, 6f, anillo)
                else c.drawCircle(p.x, p.y, r, anillo)
            }
            doc.entorno(id)?.let { e ->
                val r = MEDIA_ENTORNO + 5f
                c.drawRoundRect(RectF(e.x - r, e.y - r, e.x + r, e.y + r), 10f, 10f, anillo)
            }
            doc.textos.firstOrNull { it.id == id }?.let { t ->
                val ancho = textoLibre.measureText(t.texto.lines().maxByOrNull { it.length }.orEmpty())
                val alto = 18f * t.texto.lines().size
                c.drawRect(t.x - 4f, t.y - 16f, t.x + ancho + 4f, t.y - 16f + alto + 4f, anillo)
            }
            doc.abortos.firstOrNull { it.id == id }?.let { a -> c.drawCircle(a.x, a.y, 11f, anillo) }
            doc.trazos.firstOrNull { it.id == id }?.let { t ->
                if (t.puntos.isNotEmpty()) {
                    val x0 = t.puntos.minOf { it.x } - 5f; val x1 = t.puntos.maxOf { it.x } + 5f
                    val y0 = t.puntos.minOf { it.y } - 5f; val y1 = t.puntos.maxOf { it.y } + 5f
                    c.drawRect(x0, y0, x1, y1, anillo)
                }
            }
        }
        val relleno = pincel(Paint.Style.FILL, 0xFFFFFFFF.toInt())
        val borde = pincel(Paint.Style.STROKE, COLOR_CIAN, 2f * z)
        val caliente = pincel(Paint.Style.FILL, COLOR_VERDE)
        s.idsConAnclas.forEach { id ->
            com.ruralitos.app.domain.familiograma.Lado.entries.forEach { lado ->
                val ancla = Ancla(id, lado)
                val pos = GeometriaFamiliograma.posicion(doc, ancla) ?: return@forEach
                val destino = s.anclaDestino == ancla
                c.drawCircle(pos.x, pos.y, (if (destino) 5.5f else 4.2f) * z, if (destino) caliente else relleno)
                c.drawCircle(pos.x, pos.y, (if (destino) 5.5f else 4.2f) * z, if (destino) pincel(Paint.Style.STROKE, 0xFFFFFFFF.toInt(), 2f * z) else borde)
            }
        }
        val origen = s.origenEnlace
        val fin = s.puntoEnlace
        if (origen != null && fin != null) {
            GeometriaFamiliograma.posicion(doc, origen)?.let { a ->
                val guia = pincel(Paint.Style.STROKE, COLOR_CIAN, 2f * z).apply {
                    pathEffect = DashPathEffect(floatArrayOf(6f * z, 4f * z), 0f)
                }
                c.drawLine(a.x, a.y, fin.x, fin.y, guia)
            }
        }
    }

    // ------------------------------------------------------------------------------------
    // Hoja y leyenda
    // ------------------------------------------------------------------------------------

    /** Guías de la hoja (solo en el editor): el marco y las franjas ABUELOS, PADRES e HIJOS de la ficha. */
    fun dibujarHoja(c: Canvas) {
        val hoja = HojaFamiliograma.caja
        val franja = pincel(Paint.Style.FILL, 0xFFF1F5F8.toInt())
        c.drawRect(hoja.izquierda, HojaFamiliograma.FIN_ABUELOS, hoja.derecha, HojaFamiliograma.FIN_PADRES, franja)
        val borde = pincel(Paint.Style.STROKE, 0xFFB9CBD6.toInt(), 1.4f)
        c.drawRect(hoja.izquierda, hoja.arriba, hoja.derecha, hoja.abajo, borde)
        val division = pincel(Paint.Style.STROKE, 0xFFC9D8E0.toInt(), 1f).apply {
            pathEffect = DashPathEffect(floatArrayOf(6f, 5f), 0f)
        }
        c.drawLine(hoja.izquierda, HojaFamiliograma.FIN_ABUELOS, hoja.derecha, HojaFamiliograma.FIN_ABUELOS, division)
        c.drawLine(hoja.izquierda, HojaFamiliograma.FIN_PADRES, hoja.derecha, HojaFamiliograma.FIN_PADRES, division)
        val rotulo = texto(11f, 0xFF8AA0B0.toInt(), negrita = true)
        listOf(
            "ABUELOS" to (HojaFamiliograma.FIN_ABUELOS / 2f),
            "PADRES" to ((HojaFamiliograma.FIN_ABUELOS + HojaFamiliograma.FIN_PADRES) / 2f),
            "HIJOS" to ((HojaFamiliograma.FIN_PADRES + HojaFamiliograma.ALTO) / 2f)
        ).forEach { (nombre, y) ->
            c.save()
            c.rotate(-90f, 13f, y)
            c.drawText(nombre, 13f, y + 4f, rotulo)
            c.restore()
        }
    }

    /** Una fila de la leyenda ya medida: su código y su nombre en varias líneas si hace falta. */
    private class ListaPreparada(
        val filas: List<Pair<String, android.text.StaticLayout>>,
        val ancho: Float,
        val alto: Float,
        val unidad: Float,
        val altoTitulo: Float,
        val espacio: Float,
        val anchoCodigo: Float,
        val codigo: Paint,
        val titulo: Paint,
        val marco: Paint,
        val fondo: Paint
    )

    private fun prepararLista(filas: List<Pair<String, String>>, anchoLista: Float, unidad: Float): ListaPreparada {
        val titulo = texto(unidad * 1.05f, COLOR_TINTA, negrita = true).apply { textAlign = Paint.Align.LEFT }
        val codigo = texto(unidad, COLOR_CIAN, negrita = true).apply { textAlign = Paint.Align.LEFT }
        val nombre = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TINTA
            textSize = unidad * 0.92f
        }
        val marco = pincel(Paint.Style.STROKE, COLOR_CIAN, unidad * 0.1f)
        val fondo = pincel(Paint.Style.FILL, 0xFFFFFFFF.toInt())
        val anchoCodigo = codigo.measureText("WWWW") + unidad * 0.4f
        val anchoNombre = (anchoLista - unidad * 1.6f - anchoCodigo).toInt().coerceAtLeast(40)
        val lineas = filas.take(12).map { (cod, nom) ->
            Pair(cod, android.text.StaticLayout.Builder.obtain(nom, 0, nom.length, nombre, anchoNombre).build())
        }
        val espacio = unidad * 0.5f
        val altoTitulo = unidad * 2.5f
        val altoFilas = lineas.sumOf { (_, l) -> (maxOf(l.height.toFloat(), unidad * 1.3f) + espacio).toDouble() }.toFloat()
        return ListaPreparada(lineas, anchoLista, altoTitulo + altoFilas + unidad * 0.3f, unidad, altoTitulo, espacio, anchoCodigo, codigo, titulo, marco, fondo)
    }

    private fun listaDeLeyenda(doc: Familiograma): ListaPreparada? {
        val nuevas = AbreviaturasPatologia.nuevasEnUso(doc)
        if (nuevas.isEmpty()) return null
        return prepararLista(nuevas.map { it.codigo to it.nombre }, HojaFamiliograma.ANCHO * 0.27f, HojaFamiliograma.ANCHO / 76f)
    }

    /**
     * Recuadro de la leyenda de abreviaturas nuevas (o `null` si no hay). Va al lado de todo el familiograma, en un sitio
     * libre: nunca encima de una figura, una línea o unas siglas. Si no cabe dentro de la hoja, queda fuera, a la derecha.
     */
    fun cajaLeyenda(doc: Familiograma): Caja? {
        val lista = listaDeLeyenda(doc) ?: return null
        return ColocacionEtiquetas.colocarLeyenda(doc, etiquetasDe(doc).values, lista.ancho, lista.alto)
    }

    /** Dibuja la leyenda en su sitio libre. */
    fun dibujarLeyenda(c: Canvas, doc: Familiograma) {
        val caja = cajaLeyenda(doc) ?: return
        dibujarLeyendaEn(c, doc, caja)
    }

    fun dibujarLeyendaEn(c: Canvas, doc: Familiograma, caja: Caja) {
        val lista = listaDeLeyenda(doc) ?: return
        val unidad = lista.unidad
        val izquierda = caja.izquierda
        val arriba = caja.arriba
        val marcoCaja = RectF(izquierda, arriba, izquierda + lista.ancho, arriba + lista.alto)
        c.drawRoundRect(marcoCaja, unidad * 0.8f, unidad * 0.8f, lista.fondo)
        c.drawRoundRect(marcoCaja, unidad * 0.8f, unidad * 0.8f, lista.marco)
        c.drawText("Abreviaturas nuevas", izquierda + unidad * 0.8f, arriba + unidad * 1.55f, lista.titulo)
        var y = arriba + lista.altoTitulo
        lista.filas.forEach { (cod, layout) ->
            c.drawText(cod, izquierda + unidad * 0.8f, y - lista.codigo.ascent() + unidad * 0.05f, lista.codigo)
            c.save()
            c.translate(izquierda + unidad * 0.8f + lista.anchoCodigo, y)
            layout.draw(c)
            c.restore()
            y += maxOf(layout.height.toFloat(), unidad * 1.3f) + lista.espacio
        }
    }

    // ------------------------------------------------------------------------------------
    // Exportación a imagen
    // ------------------------------------------------------------------------------------

    /**
     * Genera la imagen (fondo transparente) que se inserta en el Excel y el PDF. La imagen es la hoja
     * completa, así las franjas ABUELOS, PADRES e HIJOS de la ficha coinciden con lo que se dibujó.
     * Si algo quedó fuera de la hoja, se reduce el dibujo para que todo quepa.
     */
    fun renderizar(doc: Familiograma, ancho: Int, alto: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        bitmap.setHasAlpha(true)
        val c = Canvas(bitmap)
        var region = HojaFamiliograma.caja
        GeometriaFamiliograma.limites(doc)?.let { limites ->
            if (HojaFamiliograma.hayElementosFuera(doc)) region = region.unir(limites.expandir(6f))
        }
        val leyenda = cajaLeyenda(doc)
        if (leyenda != null && (leyenda.derecha > region.derecha || leyenda.izquierda < region.izquierda ||
                leyenda.abajo > region.abajo || leyenda.arriba < region.arriba)
        ) region = region.unir(leyenda.expandir(6f))
        val escala = min(ancho / region.ancho, alto / region.alto)
        c.save()
        c.translate(
            (ancho - region.ancho * escala) / 2f - region.izquierda * escala,
            (alto - region.alto * escala) / 2f - region.arriba * escala
        )
        c.scale(escala, escala)
        dibujar(c, doc)
        if (leyenda != null) dibujarLeyendaEn(c, doc, leyenda)
        c.restore()
        return bitmap
    }
}
