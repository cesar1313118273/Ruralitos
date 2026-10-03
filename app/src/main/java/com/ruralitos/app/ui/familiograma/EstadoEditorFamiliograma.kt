package com.ruralitos.app.ui.familiograma

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.ruralitos.app.domain.familiograma.Aborto
import com.ruralitos.app.domain.familiograma.AbreviaturasPatologia
import com.ruralitos.app.domain.familiograma.Ancla
import com.ruralitos.app.domain.familiograma.COLOR_TINTA
import com.ruralitos.app.domain.familiograma.Entorno
import com.ruralitos.app.domain.familiograma.Familiograma
import com.ruralitos.app.domain.familiograma.Filiacion
import com.ruralitos.app.domain.familiograma.GeometriaFamiliograma
import com.ruralitos.app.domain.familiograma.HojaFamiliograma
import com.ruralitos.app.domain.familiograma.Lado
import com.ruralitos.app.domain.familiograma.MEDIA_ENTORNO
import com.ruralitos.app.domain.familiograma.MEDIA_PERSONA
import com.ruralitos.app.domain.familiograma.Persona
import com.ruralitos.app.domain.familiograma.Punto
import com.ruralitos.app.domain.familiograma.SexoPersona
import com.ruralitos.app.domain.familiograma.Texto
import com.ruralitos.app.domain.familiograma.TipoEntorno
import com.ruralitos.app.domain.familiograma.TipoTrazo
import com.ruralitos.app.domain.familiograma.Trazo
import com.ruralitos.app.domain.familiograma.Union
import com.ruralitos.app.domain.familiograma.Vinculo
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class Herramienta { MOVER, UNIR, TEXTO, LAPIZ, LINEA, FLECHA, ENCIERRO, BORRAR }

enum class PestanaBiblioteca { SIMBOLOS, ENTORNO, LIBRE }

/** Ventanas que puede mostrar el editor. */
sealed interface DialogoEditor {
    data class Persona(val id: String) : DialogoEditor
    data class Entorno(val id: String) : DialogoEditor
    data class TextoLibre(val id: String?, val x: Float, val y: Float) : DialogoEditor
    data class TipoUnion(val id: String) : DialogoEditor
    data class TipoHijo(val id: String) : DialogoEditor
    data class Biblioteca(val pestana: PestanaBiblioteca) : DialogoEditor
    data class Abreviatura(val nombre: String) : DialogoEditor
    data object Salir : DialogoEditor
}

/** Elemento tocado en el lienzo. */
sealed interface Golpe {
    data class PuntoAncla(val ancla: Ancla) : Golpe
    data class Elemento(val id: String) : Golpe
    data class LineaUnion(val id: String) : Golpe
    data class LineaHijo(val id: String) : Golpe
    data class LineaVinculo(val id: String) : Golpe
    data class DibujoLibre(val id: String) : Golpe
    data object Nada : Golpe
}

/** Todo lo que cambia mientras se edita: dibujo, selección, vista, herramienta y ventanas abiertas. */
class EstadoEditorFamiliograma(inicial: Familiograma) {
    var doc by mutableStateOf(inicial)
        private set
    private var guardado by mutableStateOf(inicial)
    val sinGuardar: Boolean get() = doc != guardado

    private val pasado = ArrayDeque<Familiograma>()
    private val futuro = ArrayDeque<Familiograma>()
    var puedeDeshacer by mutableStateOf(false)
        private set
    var puedeRehacer by mutableStateOf(false)
        private set

    var seleccionId by mutableStateOf<String?>(null)
    var herramienta by mutableStateOf(Herramienta.MOVER)
    var dialogo by mutableStateOf<DialogoEditor?>(null)
    var mensaje by mutableStateOf<String?>(null)

    // vista
    var escala by mutableFloatStateOf(1f)
    var desplazamiento by mutableStateOf(Offset.Zero)
    var tamanoLienzo by mutableStateOf(IntSize.Zero)
    var vistaManual = false
    /** `true` muestra la hoja entera (con sus franjas); `false` se acerca al dibujo. */
    var verHojaCompleta by mutableStateOf(false)
    /** Píxeles por dp de la pantalla: los puntos y márgenes de toque se miden en dp. */
    var densidad = 1f

    // estilo del dibujo libre
    var colorTrazo by mutableIntStateOf(COLOR_TINTA)
    var grosorTrazo by mutableFloatStateOf(2.4f)
    var trazoPunteado by mutableStateOf(false)

    // interacción en curso
    var origenEnlace by mutableStateOf<Ancla?>(null)
    var puntoEnlace by mutableStateOf<Punto?>(null)
    var anclaDestino by mutableStateOf<Ancla?>(null)
    var trazoEnCurso by mutableStateOf<Trazo?>(null)

    // ---- historial -------------------------------------------------------------------

    private fun actualizarBanderas() {
        puedeDeshacer = pasado.isNotEmpty()
        puedeRehacer = futuro.isNotEmpty()
    }

    /** Anota el estado actual antes de un cambio para poder deshacerlo. */
    fun registrarHistorial() {
        pasado.addLast(doc)
        if (pasado.size > 80) pasado.removeFirst()
        futuro.clear()
        actualizarBanderas()
    }

    /** Aplica un cambio guardando antes el estado actual en el historial. */
    fun cambiar(transformar: (Familiograma) -> Familiograma) {
        val nuevo = transformar(doc)
        if (nuevo == doc) return
        registrarHistorial()
        doc = nuevo
    }

    /** Cambia el dibujo sin tocar el historial (por ejemplo, mientras se arrastra una figura). */
    fun cambiarEnVivo(transformar: (Familiograma) -> Familiograma) {
        doc = transformar(doc)
    }

    fun deshacer() {
        val anterior = pasado.removeLastOrNull() ?: return
        futuro.addLast(doc)
        doc = anterior
        seleccionId = seleccionId?.takeIf { existe(it) }
        actualizarBanderas()
    }

    fun rehacer() {
        val siguiente = futuro.removeLastOrNull() ?: return
        pasado.addLast(doc)
        doc = siguiente
        seleccionId = seleccionId?.takeIf { existe(it) }
        actualizarBanderas()
    }

    fun marcarGuardado() {
        guardado = doc
    }

    private fun existe(id: String): Boolean =
        doc.persona(id) != null || doc.entorno(id) != null || doc.textos.any { it.id == id } ||
            doc.abortos.any { it.id == id } || doc.trazos.any { it.id == id }

    // ---- vista -----------------------------------------------------------------------

    fun aMundo(pantalla: Offset): Punto =
        Punto((pantalla.x - desplazamiento.x) / escala, (pantalla.y - desplazamiento.y) / escala)

    fun aPantalla(p: Punto): Offset = Offset(p.x * escala + desplazamiento.x, p.y * escala + desplazamiento.y)

    fun centroVisible(): Punto =
        aMundo(Offset(tamanoLienzo.width / 2f, tamanoLienzo.height / 2f))

    fun transformar(centro: Offset, zoom: Float, desplazar: Offset) {
        val nueva = (escala * zoom).coerceIn(0.25f, 4f)
        val factor = nueva / escala
        desplazamiento = Offset(
            centro.x - (centro.x - desplazamiento.x) * factor + desplazar.x,
            centro.y - (centro.y - desplazamiento.y) * factor + desplazar.y
        )
        escala = nueva
        vistaManual = true
    }

    fun ajustarVista() {
        val ancho = tamanoLienzo.width.toFloat()
        // abajo queda libre un espacio para la indicación de la herramienta
        val alto = tamanoLienzo.height.toFloat() - 34f * densidad
        if (ancho <= 0f || alto <= 0f) return
        val contenido = GeometriaFamiliograma.limites(doc)
        val caja = when {
            verHojaCompleta -> contenido?.unir(HojaFamiliograma.caja) ?: HojaFamiliograma.caja
            contenido != null -> contenido.expandir(14f)
            else -> HojaFamiliograma.caja
        }
        run {
            val margen = 24f
            val s = min((ancho - margen * 2) / caja.ancho, (alto - margen * 2) / caja.alto).coerceIn(0.25f, 2.2f)
            escala = s
            desplazamiento = Offset(
                (ancho - caja.ancho * s) / 2f - caja.izquierda * s,
                (alto - caja.alto * s) / 2f - caja.arriba * s
            )
        }
        vistaManual = false
    }

    // ---- puntería --------------------------------------------------------------------

    /** Elementos cuyos cuatro puntos de conexión se muestran ahora. */
    fun idsConAnclas(): Set<String> = when {
        herramienta == Herramienta.UNIR -> doc.personas.map { it.id }.toSet() + doc.entornos.map { it.id }
        else -> seleccionId?.takeIf { doc.persona(it) != null || doc.entorno(it) != null }?.let(::setOf).orEmpty()
    }

    fun golpear(p: Punto): Golpe {
        val z = densidad / escala
        // 1) puntos de conexión visibles (gana el más cercano al dedo)
        var mejor: Ancla? = null
        var mejorDistancia = 18f * z
        for (id in idsConAnclas()) {
            // el centro de la figura es para moverla o editarla; los puntos solo valen hacia los bordes
            val centro = GeometriaFamiliograma.centro(doc, id)
            val cuerpo = GeometriaFamiliograma.mitad(doc, id) * 0.55f
            if (centro != null && abs(p.x - centro.x) <= cuerpo && abs(p.y - centro.y) <= cuerpo) continue
            for (lado in Lado.entries) {
                val pos = GeometriaFamiliograma.posicion(doc, Ancla(id, lado)) ?: continue
                val d = hypot(p.x - pos.x, p.y - pos.y)
                if (d <= mejorDistancia) {
                    mejorDistancia = d
                    mejor = Ancla(id, lado)
                }
            }
        }
        mejor?.let { return Golpe.PuntoAncla(it) }
        // 2) elementos (el último dibujado queda encima)
        doc.textos.asReversed().firstOrNull { t ->
            val lineas = t.texto.split('\n')
            val ancho = (lineas.maxOfOrNull { it.length } ?: 1) * 8.4f
            p.x in (t.x - 6f)..(t.x + ancho + 6f) && p.y in (t.y - 18f)..(t.y + (lineas.size - 1) * 18f + 6f)
        }?.let { return Golpe.Elemento(it.id) }
        doc.personas.asReversed().firstOrNull {
            abs(p.x - it.x) <= MEDIA_PERSONA + 6f * z && abs(p.y - it.y) <= MEDIA_PERSONA + 6f * z
        }?.let { return Golpe.Elemento(it.id) }
        doc.entornos.asReversed().firstOrNull {
            abs(p.x - it.x) <= MEDIA_ENTORNO + 5f * z && abs(p.y - it.y) <= MEDIA_ENTORNO + 5f * z
        }?.let { return Golpe.Elemento(it.id) }
        doc.abortos.firstOrNull { hypot(p.x - it.x, p.y - it.y) <= maxOf(13f, 12f * z) }?.let { return Golpe.Elemento(it.id) }
        // 3) líneas
        val tol = 10f * z
        doc.uniones.firstOrNull { GeometriaFamiliograma.distanciaARuta(p, GeometriaFamiliograma.rutaUnion(doc, it)) <= tol }
            ?.let { return Golpe.LineaUnion(it.id) }
        doc.filiaciones.firstOrNull { GeometriaFamiliograma.distanciaARuta(p, GeometriaFamiliograma.rutaFiliacion(doc, it)) <= tol }
            ?.let { return Golpe.LineaHijo(it.id) }
        doc.vinculos.firstOrNull { GeometriaFamiliograma.distanciaARuta(p, GeometriaFamiliograma.rutaVinculo(doc, it)) <= tol }
            ?.let { return Golpe.LineaVinculo(it.id) }
        // 4) dibujo libre
        doc.trazos.asReversed().firstOrNull { distanciaATrazo(p, it) <= tol + it.grosor / 2 }
            ?.let { return Golpe.DibujoLibre(it.id) }
        return Golpe.Nada
    }

    private fun distanciaATrazo(p: Punto, t: Trazo): Float {
        if (t.puntos.size < 2) return Float.MAX_VALUE
        return when (t.tipo) {
            TipoTrazo.LAPIZ -> GeometriaFamiliograma.distanciaARuta(p, t.puntos)
            TipoTrazo.LINEA, TipoTrazo.FLECHA -> GeometriaFamiliograma.distanciaASegmento(p, t.puntos.first(), t.puntos.last())
            TipoTrazo.ENCIERRO -> {
                val a = t.puntos.first(); val b = t.puntos.last()
                val cx = (a.x + b.x) / 2; val cy = (a.y + b.y) / 2
                val rx = max(abs(b.x - a.x) / 2, 1f); val ry = max(abs(b.y - a.y) / 2, 1f)
                val k = hypot((p.x - cx) / rx, (p.y - cy) / ry)
                abs(k - 1f) * min(rx, ry)
            }
        }
    }

    // ---- edición ---------------------------------------------------------------------

    fun agregarPersona(sexo: SexoPersona): String {
        val libre = GeometriaFamiliograma.posicionLibre(doc, centroVisible())
        val id = doc.nuevoId("p")
        cambiar { it.copy(personas = it.personas + Persona(id, sexo, libre.x, libre.y, enHogar = false)) }
        seleccionId = id
        herramienta = Herramienta.MOVER
        return id
    }

    fun agregarEntorno(tipo: TipoEntorno): String {
        val libre = GeometriaFamiliograma.posicionLibre(doc, centroVisible(), separacion = 56f)
        val id = doc.nuevoId("e")
        cambiar { it.copy(entornos = it.entornos + Entorno(id, tipo, libre.x, libre.y)) }
        seleccionId = id
        herramienta = Herramienta.MOVER
        return id
    }

    fun agregarTexto(x: Float, y: Float, texto: String) {
        val id = doc.nuevoId("x")
        cambiar { it.copy(textos = it.textos + Texto(id, x, y, texto)) }
        seleccionId = id
    }

    /** Cuelga un símbolo de aborto de la unión más cercana al centro de la vista. */
    fun agregarAborto(): Boolean {
        if (doc.uniones.isEmpty()) {
            mensaje = "Primero une a dos personas para poder agregar un aborto."
            return false
        }
        val centro = centroVisible()
        val union = doc.uniones.minBy { u ->
            val m = GeometriaFamiliograma.puntoMedioUnion(doc, u)?.punto ?: Punto(0f, 0f)
            hypot(m.x - centro.x, m.y - centro.y)
        }
        val medio = GeometriaFamiliograma.puntoMedioUnion(doc, union)?.punto ?: return false
        val id = doc.nuevoId("a")
        var x = medio.x + 45f
        val y = medio.y + 48f
        while (doc.abortos.any { it.unionId == union.id && abs(it.x - x) < 24f }) x += 30f
        cambiar { it.copy(abortos = it.abortos + Aborto(id, union.id, x, y)) }
        seleccionId = id
        herramienta = Herramienta.MOVER
        return true
    }

    fun quitarElemento(id: String) {
        cambiar { it.quitar(id) }
        if (seleccionId == id) seleccionId = null
    }

    fun quitarLinea(id: String) {
        cambiar {
            it.copy(
                uniones = it.uniones.filterNot { u -> u.id == id },
                filiaciones = it.filiaciones.filterNot { f -> f.id == id || f.unionId == id },
                abortos = it.abortos.filterNot { a -> a.unionId == id },
                vinculos = it.vinculos.filterNot { v -> v.id == id }
            )
        }
    }

    /** Crea la línea correcta según los puntos que se unieron. */
    fun enlazar(origen: Ancla, destino: Ancla) {
        if (origen.elementoId == destino.elementoId) return
        val d = doc
        val ambasPersonas = d.persona(origen.elementoId) != null && d.persona(destino.elementoId) != null
        if (!ambasPersonas) {
            val existe = d.vinculos.any {
                setOf(it.a.elementoId, it.b.elementoId) == setOf(origen.elementoId, destino.elementoId)
            }
            if (existe) { mensaje = "Esos dos elementos ya están unidos."; return }
            cambiar { it.copy(vinculos = it.vinculos + Vinculo(it.nuevoId("v"), origen, destino)) }
            return
        }
        // arriba de uno con abajo de otro: es la línea de un hijo con un solo progenitor
        val hijoPadre = when {
            origen.lado == Lado.ARRIBA && destino.lado == Lado.ABAJO -> origen to destino
            origen.lado == Lado.ABAJO && destino.lado == Lado.ARRIBA -> destino to origen
            else -> null
        }
        if (hijoPadre != null) {
            val (hijo, padre) = hijoPadre
            cambiar { it.copy(filiaciones = it.filiaciones.filterNot { f -> f.hijo.elementoId == hijo.elementoId } +
                Filiacion(it.nuevoId("f"), hijo, progenitor = padre)) }
            return
        }
        val existe = d.uniones.any {
            setOf(it.a.elementoId, it.b.elementoId) == setOf(origen.elementoId, destino.elementoId)
        }
        if (existe) { mensaje = "Esas dos personas ya están unidas."; return }
        cambiar { it.copy(uniones = it.uniones + Union(it.nuevoId("u"), origen, destino)) }
    }

    /** Cuelga a [hijo] de la línea de unión [unionId]. */
    fun colgarHijo(hijo: Ancla, unionId: String) {
        cambiar {
            it.copy(
                filiaciones = it.filiaciones.filterNot { f -> f.hijo.elementoId == hijo.elementoId } +
                    Filiacion(it.nuevoId("f"), hijo, unionId = unionId)
            )
        }
    }

    fun nombreDe(id: String): String = doc.persona(id)?.let { it.nombre.ifBlank { null } } ?: ""

    fun cambiarAbreviatura(nombre: String, codigo: String): Boolean {
        val limpio = AbreviaturasPatologia.limpiarCodigo(codigo)
        val clave = AbreviaturasPatologia.normalizar(nombre)
        if (!AbreviaturasPatologia.esCodigoLibre(limpio, doc.patologiasNuevas, clave)) return false
        cambiar { AbreviaturasPatologia.registrar(it, nombre, limpio) }
        return true
    }
}
