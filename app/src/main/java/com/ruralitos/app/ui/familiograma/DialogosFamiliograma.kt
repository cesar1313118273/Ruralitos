package com.ruralitos.app.ui.familiograma

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ruralitos.app.domain.familiograma.AbreviaturasPatologia
import com.ruralitos.app.domain.familiograma.Familiograma
import com.ruralitos.app.domain.familiograma.SexoPersona
import com.ruralitos.app.domain.familiograma.TipoEntorno
import com.ruralitos.app.domain.familiograma.TipoUnion
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeCampo
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.CianSuave
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.TextoSecundario
import com.ruralitos.app.ui.theme.VerdeSalud
import com.ruralitos.app.ui.theme.VerdeSuaveRuralitos

@Composable
internal fun DialogosFamiliograma(
    estado: EstadoEditorFamiliograma,
    onGuardarYSalir: () -> Unit,
    onSalirSinGuardar: () -> Unit
) {
    when (val d = estado.dialogo) {
        is DialogoEditor.Persona -> DialogoPersona(estado, d.id)
        is DialogoEditor.Entorno -> DialogoEntorno(estado, d.id)
        is DialogoEditor.TextoLibre -> DialogoTexto(estado, d)
        is DialogoEditor.TipoUnion -> DialogoTipoUnion(estado, d.id)
        is DialogoEditor.TipoHijo -> DialogoTipoHijo(estado, d.id)
        is DialogoEditor.Biblioteca -> DialogoBiblioteca(estado, d.pestana)
        is DialogoEditor.Abreviatura -> DialogoAbreviatura(estado, d.nombre)
        DialogoEditor.Salir -> DialogoSalir(estado, onGuardarYSalir, onSalirSinGuardar)
        null -> Unit
    }
}

// ---------------------------------------------------------------------------------------
// Piezas comunes
// ---------------------------------------------------------------------------------------

@Composable
private fun VentanaEditor(
    onCerrar: () -> Unit,
    ancho: Dp = 560.dp,
    contenido: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // la ventana de diálogo también ocupa toda la pantalla y mantiene ocultas las barras del sistema
        val ventana = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.DisposableEffect(ventana) {
            ventana?.let { PantallaCompleta.activar(it) }
            onDispose { }
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BordeClinico),
            modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = ancho)
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp)
            ) { contenido() }
        }
    }
}

@Composable
private fun EncabezadoVentana(titulo: String, subtitulo: String? = null, icono: @Composable (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
        icono?.let { it(); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AzulClinicoOscuro)
            if (subtitulo != null) Text(subtitulo, fontSize = 12.sp, color = TextoSecundario)
        }
    }
}

@Composable
private fun BotonVentana(
    texto: String,
    modifier: Modifier = Modifier,
    principal: Boolean = false,
    peligro: Boolean = false,
    habilitado: Boolean = true,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(12.dp)
    val base = modifier
        .heightIn(min = 44.dp)
        .clip(forma)
    val estilo = when {
        principal -> base.background(if (habilitado) CianRuralitos else CianRuralitos.copy(alpha = 0.5f))
        peligro -> base.background(Color.White).border(1.dp, RojoClinico.copy(alpha = 0.5f), forma)
        else -> base.background(Color.White).border(1.dp, BordeCampo, forma)
    }
    Box(
        estilo.clickable(role = Role.Button, enabled = habilitado, onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                principal -> Color.White
                peligro -> RojoClinico
                else -> AzulClinicoOscuro
            }
        )
    }
}

@Composable
private fun CampoVentana(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    teclado: KeyboardType = KeyboardType.Text,
    accion: ImeAction = ImeAction.Done,
    alAccion: () -> Unit = {},
    unaLinea: Boolean = true,
    error: Boolean = false,
    ayuda: String? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = unaLinea,
        isError = error,
        supportingText = ayuda?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = accion),
        keyboardActions = KeyboardActions(onAny = { alAccion() }),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CianRuralitos,
            unfocusedBorderColor = BordeCampo,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        modifier = modifier
    )
}

@Composable
private fun FilaInterruptor(texto: String, valor: Boolean, onCambio: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto, fontSize = 14.sp, color = AzulClinicoOscuro, modifier = Modifier.weight(1f))
        Switch(
            checked = valor,
            onCheckedChange = onCambio,
            colors = SwitchDefaults.colors(checkedTrackColor = CianRuralitos)
        )
    }
}

// ---------------------------------------------------------------------------------------
// Persona: edad y patologías
// ---------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoPersona(estado: EstadoEditorFamiliograma, id: String) {
    val persona = estado.doc.persona(id)
    if (persona == null) {
        estado.dialogo = null
        return
    }
    var edad by remember(id) { mutableStateOf(persona.edad) }
    var patologias by remember(id) { mutableStateOf(persona.patologias) }
    var docLocal by remember(id) { mutableStateOf(estado.doc) }
    var campo by remember(id) { mutableStateOf("") }
    var cambiando by remember(id) { mutableStateOf(false) }
    var codigoManual by remember(id) { mutableStateOf("") }
    var enHogar by remember(id) { mutableStateOf(persona.enHogar) }
    var informante by remember(id) { mutableStateOf(persona.informante) }
    var fallecido by remember(id) { mutableStateOf(persona.fallecido) }
    var confirmarQuitar by remember(id) { mutableStateOf(false) }

    val resolucion = if (campo.isBlank()) null else AbreviaturasPatologia.resolver(campo, docLocal.patologiasNuevas)
    val clave = AbreviaturasPatologia.normalizar(campo)
    val codigoLimpio = AbreviaturasPatologia.limpiarCodigo(codigoManual)
    val codigoValido = codigoLimpio.isEmpty() ||
        AbreviaturasPatologia.esCodigoLibre(codigoLimpio, docLocal.patologiasNuevas, clave)

    /** Agrega lo escrito a la lista y devuelve el nuevo estado local (sin cerrar la ventana). */
    fun agregarCampo(): Pair<List<String>, Familiograma> {
        val texto = campo.trim().replace(Regex("\\s+"), " ")
        if (texto.isEmpty() || !codigoValido) return patologias to docLocal
        if (patologias.any { AbreviaturasPatologia.normalizar(it) == AbreviaturasPatologia.normalizar(texto) }) {
            campo = ""
            return patologias to docLocal
        }
        val nuevaLista = patologias + AbreviaturasPatologia.nombreVisible(texto)
        val nuevoDoc = AbreviaturasPatologia.registrar(
            docLocal, texto, codigoLimpio.takeIf { cambiando && it.isNotEmpty() }
        )
        patologias = nuevaLista
        docLocal = nuevoDoc
        campo = ""
        cambiando = false
        codigoManual = ""
        return nuevaLista to nuevoDoc
    }

    fun guardar() {
        val (lista, docFinal) = agregarCampo()
        estado.cambiar { base ->
            val actual = base.persona(id) ?: return@cambiar base
            base.copy(patologiasNuevas = docFinal.patologiasNuevas)
                .conPersona(
                    actual.copy(
                        edad = edad.trim(), patologias = lista,
                        enHogar = enHogar, informante = informante, fallecido = fallecido
                    )
                )
                .limpiarPatologias()
        }
        estado.dialogo = null
    }

    VentanaEditor(onCerrar = { estado.dialogo = null }, ancho = 640.dp) {
        EncabezadoVentana(
            titulo = persona.nombre.ifBlank { if (persona.sexo == SexoPersona.HOMBRE) "Hombre" else "Mujer" },
            subtitulo = listOf(
                if (persona.sexo == SexoPersona.HOMBRE) "Hombre" else "Mujer",
                persona.parentesco.lowercase().replaceFirstChar { it.uppercase() }
            ).filter { it.isNotBlank() }.joinToString(" · ") + " · Edad y patologías",
            icono = { GlifoPersona(persona.sexo) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1.35f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    CampoVentana(
                        valor = edad,
                        onCambio = { edad = it.filter { c -> c.isDigit() || c == 'm' }.take(3) },
                        etiqueta = "Edad",
                        teclado = KeyboardType.Number,
                        accion = ImeAction.Next,
                        modifier = Modifier.width(96.dp).testTag("campo_edad")
                    )
                    CampoVentana(
                        valor = campo,
                        onCambio = { campo = it.take(40) },
                        etiqueta = "Patología",
                        accion = ImeAction.Done,
                        alAccion = { agregarCampo() },
                        modifier = Modifier.weight(1f).testTag("campo_patologia")
                    )
                }
                // lo que hará el sistema con la patología escrita
                when {
                    resolucion == null -> Text(
                        "Escribe una patología: si está en la ficha se usa su abreviatura; si no, se crea una nueva.",
                        fontSize = 12.sp, color = TextoSecundario, modifier = Modifier.padding(top = 4.dp)
                    )
                    resolucion.deFicha -> AvisoAbreviatura(
                        fondo = VerdeSuaveRuralitos, texto = Color(0xFF27500A),
                        mensaje = "Abreviatura de la ficha: ${resolucion.codigo} · ${resolucion.nombre}"
                    )
                    else -> Column {
                        AvisoAbreviatura(
                            fondo = CianSuave, texto = Color(0xFF055E70),
                            mensaje = "No está en la ficha. Abreviatura nueva: ${
                                codigoLimpio.takeIf { cambiando && it.isNotEmpty() && codigoValido } ?: resolucion.codigo
                            }"
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Text(
                                if (cambiando) "Usar la automática" else "Cambiar abreviatura",
                                fontSize = 12.sp, color = CianRuralitos, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { cambiando = !cambiando; codigoManual = "" }.padding(vertical = 6.dp)
                            )
                        }
                        if (cambiando) {
                            CampoVentana(
                                valor = codigoManual,
                                onCambio = { codigoManual = AbreviaturasPatologia.limpiarCodigo(it) },
                                etiqueta = "Abreviatura (2 a 5 letras)",
                                accion = ImeAction.Done,
                                alAccion = { agregarCampo() },
                                error = !codigoValido,
                                ayuda = if (!codigoValido) "Ya está en uso, elige otra." else null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                val sugeridas = if (resolucion?.deFicha == false) AbreviaturasPatologia.sugerencias(campo) else emptyList()
                if (sugeridas.isNotEmpty()) {
                    FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        sugeridas.forEach { s ->
                            FichaChip("${s.nombre} · ${s.codigo}", false) { campo = s.nombre }
                        }
                    }
                }
                BotonVentana(
                    "Agregar patología",
                    Modifier.fillMaxWidth().padding(top = 8.dp).testTag("agregar_patologia"),
                    habilitado = campo.isNotBlank() && codigoValido
                ) { agregarCampo() }
                if (patologias.isNotEmpty()) {
                    Text("Patologías de esta persona", fontSize = 12.sp, color = TextoSecundario, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        patologias.forEach { p ->
                            val r = AbreviaturasPatologia.resolver(p, docLocal.patologiasNuevas)
                            FichaChip("${r.codigo} · ${r.nombre} ×", !r.deFicha) {
                                patologias = patologias.filterNot { it == p }
                            }
                        }
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                FilaInterruptor("Vive en el hogar", enHogar) { enHogar = it }
                FilaInterruptor("Informante", informante) { informante = it }
                FilaInterruptor(if (persona.sexo == SexoPersona.HOMBRE) "Fallecido" else "Fallecida", fallecido) { fallecido = it }
                Spacer(Modifier.height(10.dp))
                BotonVentana("Guardar", Modifier.fillMaxWidth().testTag("guardar_persona"), principal = true) { guardar() }
                Spacer(Modifier.height(8.dp))
                BotonVentana("Cancelar", Modifier.fillMaxWidth()) { estado.dialogo = null }
                Spacer(Modifier.height(8.dp))
                if (confirmarQuitar) {
                    Text("¿Quitar a esta persona y sus líneas?", fontSize = 12.sp, color = RojoClinico)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        BotonVentana("Sí, quitar", Modifier.weight(1f), peligro = true) {
                            estado.quitarElemento(id)
                            estado.dialogo = null
                        }
                        BotonVentana("No", Modifier.weight(1f)) { confirmarQuitar = false }
                    }
                } else {
                    BotonVentana("Quitar persona", Modifier.fillMaxWidth(), peligro = true) { confirmarQuitar = true }
                }
            }
        }
    }
}

@Composable
private fun AvisoAbreviatura(fondo: Color, texto: Color, mensaje: String) {
    Text(
        mensaje,
        fontSize = 13.sp,
        color = texto,
        modifier = Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .background(fondo, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

@Composable
private fun FichaChip(texto: String, destacado: Boolean, onClick: () -> Unit) {
    Text(
        texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (destacado) Color.White else AzulClinicoOscuro,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (destacado) CianRuralitos else Color.White)
            .border(1.dp, if (destacado) CianRuralitos else BordeCampo, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun GlifoPersona(sexo: SexoPersona) {
    Canvas(Modifier.size(40.dp).background(CianSuave, RoundedCornerShape(12.dp))) {
        val l = size.minDimension * 0.46f
        val c = Offset(size.width / 2, size.height / 2)
        val trazo = Stroke(width = 2.dp.toPx())
        if (sexo == SexoPersona.HOMBRE) {
            drawRect(AzulClinicoOscuro, Offset(c.x - l / 2, c.y - l / 2), Size(l, l), style = trazo)
        } else drawCircle(AzulClinicoOscuro, l / 2, c, style = trazo)
    }
}

// ---------------------------------------------------------------------------------------
// Entorno, texto y tipos de línea
// ---------------------------------------------------------------------------------------

@Composable
private fun DialogoEntorno(estado: EstadoEditorFamiliograma, id: String) {
    val entorno = estado.doc.entorno(id)
    if (entorno == null) {
        estado.dialogo = null
        return
    }
    var etiqueta by remember(id) { mutableStateOf(entorno.etiqueta) }
    VentanaEditor(onCerrar = { estado.dialogo = null }, ancho = 440.dp) {
        EncabezadoVentana(entorno.tipo.etiqueta, "Nombre que se verá bajo el ícono")
        CampoVentana(
            valor = etiqueta, onCambio = { etiqueta = it.take(24) }, etiqueta = "Nombre",
            modifier = Modifier.fillMaxWidth(),
            alAccion = { estado.cambiar { d -> d.copy(entornos = d.entornos.map { if (it.id == id) it.copy(etiqueta = etiqueta.trim()) else it }) }; estado.dialogo = null }
        )
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonVentana("Quitar", Modifier.weight(1f), peligro = true) { estado.quitarElemento(id); estado.dialogo = null }
            BotonVentana("Cancelar", Modifier.weight(1f)) { estado.dialogo = null }
            BotonVentana("Guardar", Modifier.weight(1f), principal = true) {
                estado.cambiar { d -> d.copy(entornos = d.entornos.map { if (it.id == id) it.copy(etiqueta = etiqueta.trim()) else it }) }
                estado.dialogo = null
            }
        }
    }
}

@Composable
private fun DialogoTexto(estado: EstadoEditorFamiliograma, d: DialogoEditor.TextoLibre) {
    val existente = d.id?.let { id -> estado.doc.textos.firstOrNull { it.id == id } }
    var texto by remember(d) { mutableStateOf(existente?.texto.orEmpty()) }
    VentanaEditor(onCerrar = { estado.dialogo = null }, ancho = 440.dp) {
        EncabezadoVentana(if (existente == null) "Nueva nota" else "Editar nota", "Texto libre dentro del familiograma")
        CampoVentana(
            valor = texto, onCambio = { texto = it.take(120) }, etiqueta = "Texto",
            unaLinea = false, accion = ImeAction.Default, modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (existente != null) BotonVentana("Quitar", Modifier.weight(1f), peligro = true) {
                estado.quitarElemento(existente.id); estado.dialogo = null
            }
            BotonVentana("Cancelar", Modifier.weight(1f)) { estado.dialogo = null }
            BotonVentana("Guardar", Modifier.weight(1f), principal = true, habilitado = texto.isNotBlank()) {
                val limpio = texto.trim()
                if (existente == null) estado.agregarTexto(d.x, d.y, limpio)
                else estado.cambiar { doc -> doc.copy(textos = doc.textos.map { if (it.id == existente.id) it.copy(texto = limpio) else it }) }
                estado.dialogo = null
            }
        }
    }
}

@Composable
private fun GlifoUnion(tipo: TipoUnion) {
    Canvas(Modifier.size(width = 64.dp, height = 32.dp)) {
        val trazo = Stroke(width = 1.8.dp.toPx())
        val h = size.height
        val lado = h * 0.5f
        val y = h / 2
        drawRect(AzulClinicoOscuro, Offset(2.dp.toPx(), y - lado / 2), Size(lado, lado), style = trazo)
        val radio = lado / 2
        val cx = size.width - 2.dp.toPx() - radio
        drawCircle(AzulClinicoOscuro, radio, Offset(cx, y), style = trazo)
        val x1 = 2.dp.toPx() + lado
        val x2 = cx - radio
        val medio = (x1 + x2) / 2
        if (tipo == TipoUnion.CONSANGUINEA) {
            drawLine(AzulClinicoOscuro, Offset(x1, y - 2.5.dp.toPx()), Offset(x2, y - 2.5.dp.toPx()), trazo.width)
            drawLine(AzulClinicoOscuro, Offset(x1, y + 2.5.dp.toPx()), Offset(x2, y + 2.5.dp.toPx()), trazo.width)
        } else {
            drawLine(AzulClinicoOscuro, Offset(x1, y), Offset(x2, y), trazo.width)
        }
        val marcas = when (tipo) {
            TipoUnion.SEPARACION -> listOf(0f)
            TipoUnion.DIVORCIO -> listOf(-3.dp.toPx(), 3.dp.toPx())
            else -> emptyList()
        }
        marcas.forEach { o ->
            drawLine(
                AzulClinicoOscuro,
                Offset(medio + o - 3.dp.toPx(), y + 8.dp.toPx()),
                Offset(medio + o + 3.dp.toPx(), y - 8.dp.toPx()),
                trazo.width
            )
        }
    }
}

@Composable
private fun GlifoHijo(adoptado: Boolean) {
    Canvas(Modifier.size(width = 64.dp, height = 32.dp)) {
        val trazo = Stroke(width = 1.8.dp.toPx())
        val lado = size.height * 0.5f
        val cx = size.width / 2
        drawLine(AzulClinicoOscuro, Offset(cx, 0f), Offset(cx, size.height * 0.25f), trazo.width)
        drawRect(AzulClinicoOscuro, Offset(cx - lado / 2, size.height * 0.25f), Size(lado, lado), style = trazo)
        if (adoptado) {
            val y0 = size.height * 0.2f
            val y1 = size.height * 0.85f
            for (s in listOf(-1f, 1f)) {
                val x1 = cx + s * (lado / 2 + 4.dp.toPx())
                val x2 = cx + s * (lado / 2 + 8.dp.toPx())
                drawLine(AzulClinicoOscuro, Offset(x1, y0), Offset(x2, y0), trazo.width)
                drawLine(AzulClinicoOscuro, Offset(x2, y0), Offset(x2, y1), trazo.width)
                drawLine(AzulClinicoOscuro, Offset(x2, y1), Offset(x1, y1), trazo.width)
            }
        }
    }
}

@Composable
private fun FilaOpcion(seleccionada: Boolean, etiqueta: String, glifo: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (seleccionada) CianSuave else Color.White)
            .border(if (seleccionada) 1.5.dp else 1.dp, if (seleccionada) CianRuralitos else BordeCampo, RoundedCornerShape(12.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        glifo()
        Text(etiqueta, fontSize = 14.sp, color = AzulClinicoOscuro, fontWeight = if (seleccionada) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun DialogoTipoUnion(estado: EstadoEditorFamiliograma, id: String) {
    val union = estado.doc.union(id)
    if (union == null) {
        estado.dialogo = null
        return
    }
    VentanaEditor(onCerrar = { estado.dialogo = null }, ancho = 440.dp) {
        EncabezadoVentana("Tipo de unión", "Elige cómo se dibuja la línea entre las dos personas")
        TipoUnion.entries.forEach { tipo ->
            FilaOpcion(union.tipo == tipo, tipo.etiqueta, { GlifoUnion(tipo) }) {
                estado.cambiar { d -> d.copy(uniones = d.uniones.map { if (it.id == id) it.copy(tipo = tipo) else it }) }
                estado.dialogo = null
            }
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonVentana("Quitar línea", Modifier.weight(1f), peligro = true) { estado.quitarLinea(id); estado.dialogo = null }
            BotonVentana("Cerrar", Modifier.weight(1f)) { estado.dialogo = null }
        }
    }
}

@Composable
private fun DialogoTipoHijo(estado: EstadoEditorFamiliograma, id: String) {
    val filiacion = estado.doc.filiaciones.firstOrNull { it.id == id }
    if (filiacion == null) {
        estado.dialogo = null
        return
    }
    VentanaEditor(onCerrar = { estado.dialogo = null }, ancho = 440.dp) {
        EncabezadoVentana("Tipo de hijo", "Los hijos adoptados se dibujan entre corchetes")
        listOf(false to "Hijo biológico", true to "Hijo adoptado").forEach { (adoptado, etiqueta) ->
            FilaOpcion(filiacion.adoptado == adoptado, etiqueta, { GlifoHijo(adoptado) }) {
                estado.cambiar { d -> d.copy(filiaciones = d.filiaciones.map { if (it.id == id) it.copy(adoptado = adoptado) else it }) }
                estado.dialogo = null
            }
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonVentana("Quitar línea", Modifier.weight(1f), peligro = true) { estado.quitarLinea(id); estado.dialogo = null }
            BotonVentana("Cerrar", Modifier.weight(1f)) { estado.dialogo = null }
        }
    }
}

@Composable
private fun DialogoAbreviatura(estado: EstadoEditorFamiliograma, nombre: String) {
    val actual = estado.doc.patologiasNuevas.firstOrNull {
        AbreviaturasPatologia.normalizar(it.nombre) == AbreviaturasPatologia.normalizar(nombre)
    }
    if (actual == null) {
        estado.dialogo = null
        return
    }
    var codigo by remember(nombre) { mutableStateOf(actual.codigo) }
    val limpio = AbreviaturasPatologia.limpiarCodigo(codigo)
    val libre = limpio == actual.codigo || AbreviaturasPatologia.esCodigoLibre(
        limpio, estado.doc.patologiasNuevas, AbreviaturasPatologia.normalizar(nombre)
    )
    VentanaEditor(onCerrar = { estado.dialogo = null }, ancho = 440.dp) {
        EncabezadoVentana("Cambiar abreviatura", actual.nombre)
        CampoVentana(
            valor = codigo, onCambio = { codigo = AbreviaturasPatologia.limpiarCodigo(it) },
            etiqueta = "Abreviatura (2 a 5 letras)", modifier = Modifier.fillMaxWidth(),
            error = !libre || limpio.length < 2,
            ayuda = when {
                limpio.length < 2 -> "Escribe al menos 2 letras."
                !libre -> "Ya está en uso, elige otra."
                else -> null
            }
        )
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonVentana("Cancelar", Modifier.weight(1f)) { estado.dialogo = null }
            BotonVentana("Guardar", Modifier.weight(1f), principal = true, habilitado = libre && limpio.length >= 2) {
                estado.cambiarAbreviatura(nombre, limpio)
                estado.dialogo = null
            }
        }
    }
}

@Composable
private fun DialogoSalir(estado: EstadoEditorFamiliograma, onGuardarYSalir: () -> Unit, onSalirSinGuardar: () -> Unit) {
    VentanaEditor(onCerrar = { estado.dialogo = null }, ancho = 440.dp) {
        EncabezadoVentana("Hay cambios sin guardar", "Si sales ahora se perderán los cambios del familiograma.")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonVentana("Guardar y salir", Modifier.fillMaxWidth(), principal = true) {
                estado.dialogo = null
                onGuardarYSalir()
            }
            BotonVentana("Salir sin guardar", Modifier.fillMaxWidth(), peligro = true) {
                estado.dialogo = null
                onSalirSinGuardar()
            }
            BotonVentana("Seguir editando", Modifier.fillMaxWidth()) { estado.dialogo = null }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Biblioteca: símbolos, entorno y dibujo libre
// ---------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoBiblioteca(estado: EstadoEditorFamiliograma, inicial: PestanaBiblioteca) {
    var pestana by remember { mutableStateOf(inicial) }
    VentanaEditor(onCerrar = { estado.dialogo = null }, ancho = 640.dp) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                PestanaBiblioteca.SIMBOLOS to "Símbolos",
                PestanaBiblioteca.ENTORNO to "Entorno",
                PestanaBiblioteca.LIBRE to "Dibujo libre"
            ).forEach { (p, etiqueta) ->
                val activa = pestana == p
                Text(
                    etiqueta,
                    fontSize = 14.sp,
                    fontWeight = if (activa) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (activa) CianRuralitos else TextoSecundario,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activa) CianSuave else Color.Transparent)
                        .clickable { pestana = p }
                        .padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        when (pestana) {
            PestanaBiblioteca.SIMBOLOS -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BaldosaSimbolo("Hombre", { GlifoPersonaGrande(SexoPersona.HOMBRE) }) {
                        estado.agregarPersona(SexoPersona.HOMBRE); estado.dialogo = null
                    }
                    BaldosaSimbolo("Mujer", { GlifoPersonaGrande(SexoPersona.MUJER) }) {
                        estado.agregarPersona(SexoPersona.MUJER); estado.dialogo = null
                    }
                    BaldosaSimbolo("Aborto", { GlifoAborto() }) {
                        if (estado.agregarAborto()) estado.dialogo = null else estado.dialogo = null
                    }
                    BaldosaSimbolo("Nota de texto", { IconoEditor(IconoEditorTipo.TEXTO, AzulClinicoOscuro, tamano = 30.dp) }) {
                        estado.herramienta = Herramienta.TEXTO; estado.dialogo = null
                    }
                }
                Text(
                    "Une a las personas desde sus cuatro puntos de conexión. Toca una línea para elegir matrimonio, " +
                        "separación, divorcio o unión consanguínea; toca a una persona para poner la edad, las " +
                        "patologías, si es informante o si falleció.",
                    fontSize = 12.sp, color = TextoSecundario, lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            PestanaBiblioteca.ENTORNO -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TipoEntorno.entries.forEach { tipo ->
                        BaldosaSimbolo(tipo.etiqueta, { GlifoEntorno(tipo) }) {
                            estado.agregarEntorno(tipo); estado.dialogo = null
                        }
                    }
                }
                Text(
                    "Coloca el ícono y únelo con una línea punteada a la familia o a quien participa " +
                        "(arrastra de un punto del ícono a un punto de la persona).",
                    fontSize = 12.sp, color = TextoSecundario, lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            PestanaBiblioteca.LIBRE -> PanelDibujoLibre(estado)
        }
        BotonVentana("Cerrar", Modifier.fillMaxWidth().padding(top = 12.dp)) { estado.dialogo = null }
    }
}

@Composable
private fun BaldosaSimbolo(etiqueta: String, glifo: @Composable () -> Unit, onClick: () -> Unit) {
    Column(
        Modifier
            .width(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BordeCampo, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.height(36.dp), contentAlignment = Alignment.Center) { glifo() }
        Text(etiqueta, fontSize = 11.sp, color = AzulClinicoOscuro, lineHeight = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun GlifoPersonaGrande(sexo: SexoPersona) {
    Canvas(Modifier.size(34.dp)) {
        val trazo = Stroke(width = 2.dp.toPx())
        val l = size.minDimension * 0.78f
        if (sexo == SexoPersona.HOMBRE) {
            drawRect(AzulClinicoOscuro, Offset((size.width - l) / 2, (size.height - l) / 2), Size(l, l), style = trazo)
        } else drawCircle(AzulClinicoOscuro, l / 2, Offset(size.width / 2, size.height / 2), style = trazo)
    }
}

@Composable
private fun GlifoAborto() {
    Canvas(Modifier.size(34.dp)) {
        drawLine(AzulClinicoOscuro, Offset(size.width / 2, 2.dp.toPx()), Offset(size.width / 2, size.height * 0.62f), 3.dp.toPx())
        drawCircle(AzulClinicoOscuro, 5.dp.toPx(), Offset(size.width / 2, size.height * 0.78f))
    }
}

@Composable
private fun GlifoEntorno(tipo: TipoEntorno) {
    Canvas(Modifier.size(36.dp)) {
        drawIntoCanvas { canvas ->
            val c = canvas.nativeCanvas
            val linea = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                color = COLOR_CIAN_OSCURO
                strokeWidth = 1.6f
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }
            val suave = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.FILL
                color = COLOR_CIAN_SUAVE
            }
            c.save()
            c.translate(size.width / 2, size.height / 2)
            val k = size.minDimension / 30f
            c.scale(k, k)
            IconosEntorno.dibujar(c, tipo, linea, suave)
            c.restore()
        }
    }
}

private val coloresTrazo = listOf(
    0xFF0A2A5E.toInt(), 0xFF0889A0.toInt(), 0xFF3A7F1F.toInt(), 0xFF1565C0.toInt(), 0xFFC62828.toInt()
)

@Composable
private fun PanelDibujoLibre(estado: EstadoEditorFamiliograma) {
    val herramientas = listOf(
        Herramienta.LAPIZ to "Lápiz",
        Herramienta.LINEA to "Línea",
        Herramienta.FLECHA to "Flecha",
        Herramienta.ENCIERRO to "Encierro"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        herramientas.forEach { (h, etiqueta) ->
            val activa = estado.herramienta == h
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activa) CianSuave else Color.White)
                    .border(if (activa) 1.5.dp else 1.dp, if (activa) CianRuralitos else BordeCampo, RoundedCornerShape(12.dp))
                    .clickable(role = Role.RadioButton) {
                        estado.herramienta = h
                        estado.trazoPunteado = h == Herramienta.ENCIERRO
                    }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GlifoTrazo(h, if (activa) CianRuralitos else AzulClinicoOscuro)
                Text(etiqueta, fontSize = 12.sp, color = if (activa) CianRuralitos else AzulClinicoOscuro)
            }
        }
    }
    Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Color", fontSize = 13.sp, color = TextoSecundario, modifier = Modifier.width(56.dp))
        coloresTrazo.forEach { c ->
            Box(
                Modifier
                    .size(30.dp)
                    .background(Color(c), CircleShape)
                    .border(if (estado.colorTrazo == c) 3.dp else 0.dp, if (estado.colorTrazo == c) CianSuave else Color.Transparent, CircleShape)
                    .border(if (estado.colorTrazo == c) 1.5.dp else 0.dp, CianRuralitos, CircleShape)
                    .clickable { estado.colorTrazo = c }
            )
        }
    }
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Grosor", fontSize = 13.sp, color = TextoSecundario, modifier = Modifier.width(56.dp))
        Slider(
            value = estado.grosorTrazo,
            onValueChange = { estado.grosorTrazo = it },
            valueRange = 1f..8f,
            colors = SliderDefaults.colors(thumbColor = CianRuralitos, activeTrackColor = CianRuralitos),
            modifier = Modifier.weight(1f)
        )
    }
    FilaInterruptor("Línea punteada", estado.trazoPunteado) { estado.trazoPunteado = it }
    Text(
        "Usa «Encierro» para rodear solo a quienes viven en el hogar: arrastra para dibujar el óvalo.",
        fontSize = 12.sp, color = TextoSecundario, lineHeight = 16.sp, modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun GlifoTrazo(h: Herramienta, color: Color) {
    Canvas(Modifier.size(34.dp)) {
        val grosor = 2.2.dp.toPx()
        when (h) {
            Herramienta.LAPIZ -> {
                val ruta = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.1f, size.height * 0.7f)
                    cubicTo(size.width * 0.3f, size.height * 0.1f, size.width * 0.5f, size.height * 0.95f, size.width * 0.9f, size.height * 0.3f)
                }
                drawPath(ruta, color, style = Stroke(grosor, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
            Herramienta.LINEA -> drawLine(color, Offset(size.width * 0.1f, size.height * 0.8f), Offset(size.width * 0.9f, size.height * 0.2f), grosor, androidx.compose.ui.graphics.StrokeCap.Round)
            Herramienta.FLECHA -> {
                val a = Offset(size.width * 0.1f, size.height * 0.85f)
                val b = Offset(size.width * 0.88f, size.height * 0.18f)
                drawLine(color, a, b, grosor, androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color, b, Offset(b.x - size.width * 0.28f, b.y + size.height * 0.02f), grosor, androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color, b, Offset(b.x - size.width * 0.02f, b.y + size.height * 0.28f), grosor, androidx.compose.ui.graphics.StrokeCap.Round)
            }
            else -> drawOval(
                color, Offset(size.width * 0.06f, size.height * 0.18f), Size(size.width * 0.88f, size.height * 0.64f),
                style = Stroke(grosor, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(7f, 5f)))
            )
        }
    }
}
