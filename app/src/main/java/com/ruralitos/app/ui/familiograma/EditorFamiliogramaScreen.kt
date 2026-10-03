package com.ruralitos.app.ui.familiograma

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.domain.familiograma.AbreviaturasPatologia
import com.ruralitos.app.domain.familiograma.Ancla
import com.ruralitos.app.domain.familiograma.Familiograma
import com.ruralitos.app.domain.familiograma.GeometriaFamiliograma
import com.ruralitos.app.domain.familiograma.Punto
import com.ruralitos.app.domain.familiograma.TipoTrazo
import com.ruralitos.app.domain.familiograma.Trazo
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeCampo
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.CianSuave
import com.ruralitos.app.ui.theme.FondoClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.TextoSecundario
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun Context.actividad(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/**
 * Editor del familiograma: lienzo horizontal con figuras que se mueven, se unen por sus cuatro
 * puntos y se editan al tocarlas.
 */
@Composable
fun EditorFamiliogramaScreen(
    inicial: Familiograma,
    subtitulo: String,
    onGuardar: suspend (Familiograma) -> Boolean,
    onSalir: () -> Unit
) {
    val estado = remember { EstadoEditorFamiliograma(inicial) }
    EditorFamiliogramaContenido(estado, subtitulo, onGuardar, onSalir)
}

/** Contenido del editor sobre un estado ya creado (así se puede probar desde fuera). */
@Composable
internal fun EditorFamiliogramaContenido(
    estado: EstadoEditorFamiliograma,
    subtitulo: String,
    onGuardar: suspend (Familiograma) -> Boolean,
    onSalir: () -> Unit,
    forzarHorizontal: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var guardando by remember { mutableStateOf(false) }
    val dibujante = remember { DibujanteFamiliograma() }

    // El lienzo se abre siempre en horizontal y se devuelve la orientación al salir.
    DisposableEffect(forzarHorizontal) {
        if (!forzarHorizontal) return@DisposableEffect onDispose { }
        val actividad = context.actividad()
        val anterior = actividad?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        actividad?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        // ocupa toda la pantalla: bajo la cámara y sin barras de estado ni de navegación
        val pantallaCompleta = actividad?.let { PantallaCompleta.activar(it.window) }
        onDispose {
            pantallaCompleta?.restaurar()
            actividad?.requestedOrientation = anterior
        }
    }

    fun guardar(despues: (() -> Unit)? = null) {
        if (guardando) return
        if (estado.doc.personas.isEmpty()) {
            estado.mensaje = "Agrega al menos una persona antes de guardar."
            return
        }
        guardando = true
        scope.launch {
            val ok = onGuardar(estado.doc)
            guardando = false
            if (ok) {
                estado.marcarGuardado()
                estado.mensaje = "Familiograma guardado."
                despues?.invoke()
            } else {
                estado.mensaje = "No se pudo guardar el familiograma."
            }
        }
    }

    fun intentarSalir() {
        if (estado.sinGuardar) estado.dialogo = DialogoEditor.Salir else onSalir()
    }

    BackHandler { intentarSalir() }

    LaunchedEffect(estado.mensaje) {
        if (estado.mensaje != null) {
            delay(2600)
            estado.mensaje = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(FondoClinico)
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
    ) {
        BarraSuperiorEditor(
            subtitulo = subtitulo,
            estado = estado,
            guardando = guardando,
            onVolver = ::intentarSalir,
            onGuardar = { guardar() }
        )
        Row(Modifier.weight(1f).fillMaxWidth()) {
            RielHerramientas(estado)
            Box(Modifier.weight(1f).fillMaxHeight()) {
                LienzoFamiliograma(estado, dibujante)
                IndicacionHerramienta(estado, Modifier.align(Alignment.BottomStart).padding(8.dp))
                estado.mensaje?.let {
                    Text(
                        it,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .background(AzulClinicoOscuro.copy(alpha = 0.92f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
                BotonFlotante(
                    IconoEditorTipo.AJUSTAR, "Ver la hoja completa o acercar al dibujo",
                    Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    estado.verHojaCompleta = !estado.verHojaCompleta
                    estado.ajustarVista()
                }
            }
            PanelDerecho(estado)
        }
    }

    DialogosFamiliograma(estado, onGuardarYSalir = { guardar(despues = onSalir) }, onSalirSinGuardar = onSalir)
}

@Composable
private fun BarraSuperiorEditor(
    subtitulo: String,
    estado: EstadoEditorFamiliograma,
    guardando: Boolean,
    onVolver: () -> Unit,
    onGuardar: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(0.5.dp, BordeClinico)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, AzulClinicoOscuro.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onVolver),
            contentAlignment = Alignment.Center
        ) { IconoEditor(IconoEditorTipo.REGRESAR, AzulClinicoOscuro) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                "Dibujar familiograma",
                color = AzulClinicoOscuro,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitulo,
                color = TextoSecundario,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        BotonIcono(IconoEditorTipo.DESHACER, "Deshacer", estado.puedeDeshacer) { estado.deshacer() }
        BotonIcono(IconoEditorTipo.REHACER, "Rehacer", estado.puedeRehacer) { estado.rehacer() }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (guardando) CianRuralitos.copy(alpha = 0.6f) else CianRuralitos)
                .clickable(role = Role.Button, enabled = !guardando, onClick = onGuardar)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconoEditor(IconoEditorTipo.GUARDAR, Color.White, tamano = 18.dp)
            Spacer(Modifier.width(6.dp))
            Text(if (guardando) "Guardando…" else "Guardar", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun BotonIcono(tipo: IconoEditorTipo, descripcion: String, habilitado: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        IconoEditor(tipo, if (habilitado) AzulClinicoOscuro else AzulClinicoOscuro.copy(alpha = 0.3f))
    }
}

@Composable
private fun BotonFlotante(tipo: IconoEditorTipo, descripcion: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, BordeCampo, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { IconoEditor(tipo, AzulClinicoOscuro) }
}

@Composable
private fun RielHerramientas(estado: EstadoEditorFamiliograma) {
    Column(
        Modifier
            .width(62.dp)
            .fillMaxHeight()
            .background(Color.White)
            .border(0.5.dp, BordeClinico)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val libre = estado.herramienta in listOf(Herramienta.LAPIZ, Herramienta.LINEA, Herramienta.FLECHA, Herramienta.ENCIERRO)
        ItemRiel(IconoEditorTipo.MOVER, "Mover", estado.herramienta == Herramienta.MOVER) {
            estado.herramienta = Herramienta.MOVER
        }
        ItemRiel(IconoEditorTipo.UNIR, "Unir", estado.herramienta == Herramienta.UNIR) {
            estado.herramienta = if (estado.herramienta == Herramienta.UNIR) Herramienta.MOVER else Herramienta.UNIR
        }
        ItemRiel(IconoEditorTipo.TEXTO, "Texto", estado.herramienta == Herramienta.TEXTO) {
            estado.herramienta = if (estado.herramienta == Herramienta.TEXTO) Herramienta.MOVER else Herramienta.TEXTO
        }
        ItemRiel(IconoEditorTipo.LAPIZ, "Libre", libre) {
            estado.dialogo = DialogoEditor.Biblioteca(PestanaBiblioteca.LIBRE)
        }
        ItemRiel(IconoEditorTipo.BORRAR, "Borrar", estado.herramienta == Herramienta.BORRAR) {
            estado.herramienta = if (estado.herramienta == Herramienta.BORRAR) Herramienta.MOVER else Herramienta.BORRAR
        }
    }
}

@Composable
private fun ItemRiel(icono: IconoEditorTipo, etiqueta: String, activo: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(if (activo) CianSuave else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconoEditor(icono, if (activo) CianRuralitos else TextoSecundario)
        Text(etiqueta, fontSize = 11.sp, color = if (activo) CianRuralitos else TextoSecundario, maxLines = 1)
    }
}

@Composable
private fun IndicacionHerramienta(estado: EstadoEditorFamiliograma, modifier: Modifier) {
    val texto = when (estado.herramienta) {
        Herramienta.MOVER -> "Arrastra para mover · toca para editar · une desde los puntos · pellizca para acercar"
        Herramienta.UNIR -> "Arrastra de un punto a otro para unir. Para un hijo, suéltalo sobre la línea de la pareja"
        Herramienta.TEXTO -> "Toca el lienzo para escribir una nota"
        Herramienta.LAPIZ -> "Dibuja libremente con el dedo"
        Herramienta.LINEA -> "Arrastra para trazar una línea"
        Herramienta.FLECHA -> "Arrastra para trazar una flecha"
        Herramienta.ENCIERRO -> "Arrastra para encerrar a quienes viven en el hogar"
        Herramienta.BORRAR -> "Toca lo que quieras quitar"
    }
    Text(
        texto,
        fontSize = 11.sp,
        color = TextoSecundario,
        maxLines = 2,
        modifier = modifier
            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

// ---------------------------------------------------------------------------------------
// Lienzo
// ---------------------------------------------------------------------------------------

@Composable
private fun LienzoFamiliograma(estado: EstadoEditorFamiliograma, dibujante: DibujanteFamiliograma) {
    val densidad = LocalContext.current.resources.displayMetrics.density
    estado.densidad = densidad
    Canvas(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color.White)
            .testTag("lienzo")
            .onSizeChanged {
                val primera = estado.tamanoLienzo.width == 0
                estado.tamanoLienzo = it
                if (primera || !estado.vistaManual) estado.ajustarVista()
            }
            .pointerInput(estado) {
                awaitEachGesture { procesarGesto(estado, viewConfiguration.touchSlop) }
            }
    ) {
        // rejilla de puntos
        val paso = 24f * densidad * estado.escala
        if (paso > 10f) {
            val rejilla = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFD5E3EA.toInt()
                strokeWidth = 1.6f * densidad
                strokeCap = android.graphics.Paint.Cap.ROUND
            }
            val x0 = ((estado.desplazamiento.x % paso) + paso) % paso
            val y0 = ((estado.desplazamiento.y % paso) + paso) % paso
            val puntos = ArrayList<Float>()
            var x = x0
            while (x < size.width) {
                var y = y0
                while (y < size.height) { puntos.add(x); puntos.add(y); y += paso }
                x += paso
            }
            drawIntoCanvas { it.nativeCanvas.drawPoints(puntos.toFloatArray(), rejilla) }
        }
        drawIntoCanvas { canvas ->
            val c = canvas.nativeCanvas
            c.save()
            c.translate(estado.desplazamiento.x, estado.desplazamiento.y)
            c.scale(estado.escala * 1f, estado.escala * 1f)
            val doc = estado.doc
            dibujante.dibujarHoja(c)
            dibujante.dibujar(c, doc)
            dibujante.dibujarLeyenda(c, doc)
            val ids = if (estado.origenEnlace != null) {
                doc.personas.map { it.id }.toSet() + doc.entornos.map { it.id }
            } else estado.idsConAnclas()
            dibujante.dibujarSuperposicion(
                c, doc,
                SuperposicionEditor(
                    seleccionId = estado.seleccionId,
                    idsConAnclas = ids,
                    origenEnlace = estado.origenEnlace,
                    puntoEnlace = estado.puntoEnlace,
                    anclaDestino = estado.anclaDestino,
                    trazoEnCurso = estado.trazoEnCurso,
                    escala = estado.escala,
                    densidad = densidad
                )
            )
            c.restore()
        }
    }
}

private sealed interface Modo {
    data object Ninguno : Modo
    data object Panoramica : Modo
    data object Dibujar : Modo
    data class Mover(val id: String, val dx: Float, val dy: Float) : Modo
    data class Enlazar(val origen: Ancla) : Modo
    data class Tocar(val golpe: Golpe) : Modo
}

private suspend fun AwaitPointerEventScope.procesarGesto(estado: EstadoEditorFamiliograma, pendiente: Float) {
    val abajo = awaitFirstDown(requireUnconsumed = false)
    val inicio = abajo.position
    val mundoInicio = estado.aMundo(inicio)
    val herramienta = estado.herramienta
    val dibujando = herramienta in listOf(Herramienta.LAPIZ, Herramienta.LINEA, Herramienta.FLECHA, Herramienta.ENCIERRO)
    val golpe = if (dibujando) Golpe.Nada else estado.golpear(mundoInicio)

    var modo: Modo = when {
        dibujando -> Modo.Dibujar
        golpe is Golpe.PuntoAncla && herramienta != Herramienta.BORRAR -> Modo.Enlazar(golpe.ancla)
        golpe is Golpe.Elemento && herramienta != Herramienta.BORRAR -> {
            estado.seleccionId = golpe.id
            val centro = estado.doc.persona(golpe.id)?.let { Punto(it.x, it.y) }
                ?: estado.doc.entorno(golpe.id)?.let { Punto(it.x, it.y) }
                ?: estado.doc.textos.firstOrNull { it.id == golpe.id }?.let { Punto(it.x, it.y) }
                ?: estado.doc.abortos.firstOrNull { it.id == golpe.id }?.let { Punto(it.x, it.y) }
                ?: mundoInicio
            Modo.Mover(golpe.id, mundoInicio.x - centro.x, mundoInicio.y - centro.y)
        }
        golpe == Golpe.Nada -> Modo.Panoramica
        else -> Modo.Tocar(golpe)
    }

    var movido = false
    var multitoque = false
    var registrado = false
    val puntosTrazo = mutableListOf(mundoInicio)

    do {
        val evento = awaitPointerEvent()
        val activos = evento.changes.count { it.pressed }
        if (activos >= 2) {
            if (!multitoque) {
                multitoque = true
                estado.origenEnlace = null; estado.puntoEnlace = null; estado.anclaDestino = null
                estado.trazoEnCurso = null
                modo = Modo.Ninguno
            }
            val zoom = evento.calculateZoom()
            val pan = evento.calculatePan()
            val centro = evento.calculateCentroid()
            if (zoom != 1f || pan != Offset.Zero) estado.transformar(centro, zoom, pan)
            evento.changes.forEach { it.consume() }
        } else if (!multitoque) {
            val cambio = evento.changes.firstOrNull { it.id == abajo.id } ?: break
            if (!movido && (cambio.position - inicio).getDistance() > pendiente) movido = true
            if (movido) {
                val mundo = estado.aMundo(cambio.position)
                when (val m = modo) {
                    Modo.Panoramica -> {
                        estado.desplazamiento += cambio.positionChange()
                        estado.vistaManual = true
                    }
                    is Modo.Mover -> {
                        if (!registrado) { estado.registrarHistorial(); registrado = true }
                        estado.cambiarEnVivo { it.moverElemento(m.id, mundo.x - m.dx, mundo.y - m.dy) }
                    }
                    is Modo.Enlazar -> {
                        estado.origenEnlace = m.origen
                        estado.puntoEnlace = mundo
                        estado.anclaDestino = GeometriaFamiliograma.anclaCercana(
                            estado.doc, mundo, m.origen.elementoId, 28f * estado.densidad / estado.escala
                        )
                    }
                    Modo.Dibujar -> {
                        if (herramienta == Herramienta.LAPIZ) {
                            val ultimo = puntosTrazo.last()
                            if (kotlin.math.hypot(mundo.x - ultimo.x, mundo.y - ultimo.y) > 1.5f / estado.escala) puntosTrazo.add(mundo)
                        } else {
                            if (puntosTrazo.size == 1) puntosTrazo.add(mundo) else puntosTrazo[1] = mundo
                        }
                        estado.trazoEnCurso = Trazo(
                            "en-curso", tipoTrazo(herramienta), puntosTrazo.toList(),
                            estado.colorTrazo, estado.grosorTrazo, estado.trazoPunteado
                        )
                    }
                    else -> Unit
                }
                cambio.consume()
            }
        }
    } while (evento.changes.any { it.pressed })

    if (multitoque) return

    when (val m = modo) {
        is Modo.Enlazar -> {
            val destino = estado.anclaDestino
            val fin = estado.puntoEnlace
            estado.origenEnlace = null; estado.puntoEnlace = null; estado.anclaDestino = null
            if (movido && fin != null) {
                if (destino != null) estado.enlazar(m.origen, destino)
                else if (m.origen.lado == com.ruralitos.app.domain.familiograma.Lado.ARRIBA &&
                    estado.doc.persona(m.origen.elementoId) != null
                ) {
                    val union = GeometriaFamiliograma.unionCercana(
                        estado.doc, fin, 18f * estado.densidad / estado.escala, excluirPersonaId = m.origen.elementoId
                    )
                    if (union != null) estado.colgarHijo(m.origen, union.id)
                }
            }
        }
        Modo.Dibujar -> {
            val t = estado.trazoEnCurso
            estado.trazoEnCurso = null
            if (movido && t != null && t.puntos.size >= 2) {
                val a = t.puntos.first(); val b = t.puntos.last()
                val largo = kotlin.math.hypot(b.x - a.x, b.y - a.y)
                if (t.tipo == TipoTrazo.LAPIZ || largo > 6f) {
                    estado.cambiar { it.copy(trazos = it.trazos + t.copy(id = it.nuevoId("t"))) }
                }
            }
        }
        is Modo.Mover -> if (!movido) alTocarElemento(estado, m.id)
        is Modo.Tocar -> if (!movido) alTocar(estado, m.golpe)
        Modo.Panoramica -> if (!movido) {
            if (herramienta == Herramienta.TEXTO) {
                estado.dialogo = DialogoEditor.TextoLibre(null, mundoInicio.x, mundoInicio.y)
            } else {
                estado.seleccionId = null
            }
        }
        Modo.Ninguno -> Unit
    }
}

private fun tipoTrazo(h: Herramienta) = when (h) {
    Herramienta.LINEA -> TipoTrazo.LINEA
    Herramienta.FLECHA -> TipoTrazo.FLECHA
    Herramienta.ENCIERRO -> TipoTrazo.ENCIERRO
    else -> TipoTrazo.LAPIZ
}

private fun alTocarElemento(estado: EstadoEditorFamiliograma, id: String) {
    estado.seleccionId = id
    val doc = estado.doc
    estado.dialogo = when {
        doc.persona(id) != null -> DialogoEditor.Persona(id)
        doc.entorno(id) != null -> DialogoEditor.Entorno(id)
        else -> doc.textos.firstOrNull { it.id == id }?.let { DialogoEditor.TextoLibre(it.id, it.x, it.y) }
    }
}

private fun alTocar(estado: EstadoEditorFamiliograma, golpe: Golpe) {
    val borrar = estado.herramienta == Herramienta.BORRAR
    when (golpe) {
        is Golpe.Elemento -> if (borrar) estado.quitarElemento(golpe.id) else alTocarElemento(estado, golpe.id)
        is Golpe.LineaUnion -> if (borrar) estado.quitarLinea(golpe.id) else estado.dialogo = DialogoEditor.TipoUnion(golpe.id)
        is Golpe.LineaHijo -> if (borrar) estado.quitarLinea(golpe.id) else estado.dialogo = DialogoEditor.TipoHijo(golpe.id)
        is Golpe.LineaVinculo -> if (borrar) estado.quitarLinea(golpe.id) else estado.mensaje = "Usa «Borrar» para quitar esta línea punteada."
        is Golpe.DibujoLibre -> if (borrar) estado.quitarElemento(golpe.id) else estado.seleccionId = golpe.id
        else -> Unit
    }
}

// ---------------------------------------------------------------------------------------
// Panel derecho
// ---------------------------------------------------------------------------------------

@Composable
private fun PanelDerecho(estado: EstadoEditorFamiliograma) {
    val doc = estado.doc
    val nuevas = AbreviaturasPatologia.nuevasEnUso(doc)
    val deFicha = AbreviaturasPatologia.codigosDeFichaEnUso(doc)
    Column(
        Modifier
            .width(196.dp)
            .fillMaxHeight()
            .background(Color.White)
            .border(0.5.dp, BordeClinico)
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonBiblioteca(IconoEditorTipo.SIMBOLOS, "Símbolos", Modifier.weight(1f)) {
                estado.dialogo = DialogoEditor.Biblioteca(PestanaBiblioteca.SIMBOLOS)
            }
            BotonBiblioteca(IconoEditorTipo.ENTORNO, "Entorno", Modifier.weight(1f)) {
                estado.dialogo = DialogoEditor.Biblioteca(PestanaBiblioteca.ENTORNO)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mostrar nombres", fontSize = 12.sp, color = AzulClinicoOscuro, modifier = Modifier.weight(1f))
            Switch(
                checked = doc.mostrarNombres,
                onCheckedChange = { marcado -> estado.cambiar { it.copy(mostrarNombres = marcado) } },
                colors = SwitchDefaults.colors(checkedTrackColor = CianRuralitos)
            )
        }
        Column {
            Text("Abreviaturas nuevas", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AzulClinicoOscuro)
            Text(
                "No están en la ficha. Se crean solas al escribir la patología; toca una para cambiarla.",
                fontSize = 11.sp, color = TextoSecundario, lineHeight = 14.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )
            if (nuevas.isEmpty()) {
                Text("Ninguna por ahora.", fontSize = 12.sp, color = TextoSecundario)
            } else nuevas.forEach { n ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { estado.dialogo = DialogoEditor.Abreviatura(n.nombre) }
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        n.codigo,
                        color = Color(0xFF055E70),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(CianSuave, RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                    Text(n.nombre, fontSize = 12.sp, color = AzulClinicoOscuro, lineHeight = 14.sp)
                }
            }
        }
        Column {
            Text("De la ficha en uso", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AzulClinicoOscuro)
            Spacer(Modifier.height(6.dp))
            if (deFicha.isEmpty()) {
                Text("Ninguna por ahora.", fontSize = 12.sp, color = TextoSecundario)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    deFicha.forEach {
                        Text(
                            "${it.codigo} · ${it.nombre}",
                            fontSize = 12.sp, color = AzulClinicoOscuro, lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonBiblioteca(icono: IconoEditorTipo, etiqueta: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BordeCampo, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconoEditor(icono, CianRuralitos)
        Text(etiqueta, fontSize = 11.sp, color = AzulClinicoOscuro)
    }
}
