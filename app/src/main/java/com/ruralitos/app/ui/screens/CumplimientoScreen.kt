package com.ruralitos.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.data.cumplimiento.PoblacionAsignadaRepositorio
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.domain.CumplimientoGrupo
import com.ruralitos.app.domain.CumplimientoPoblacion
import com.ruralitos.app.domain.GrupoCumplimiento
import com.ruralitos.app.domain.NivelCumplimiento
import com.ruralitos.app.domain.PoblacionAsignada
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.RecursosIconografiaMais
import com.ruralitos.app.ui.components.TextoAjustado
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.VerdeSalud

private val GrisSinMeta = Color(0xFF8A97A3)

private fun colorNivel(nivel: NivelCumplimiento): Color = when (nivel) {
    NivelCumplimiento.BAJO -> RojoClinico
    NivelCumplimiento.MEDIO -> NaranjaClinico
    NivelCumplimiento.ALTO -> VerdeSalud
    NivelCumplimiento.SIN_META -> GrisSinMeta
}

/**
 * Cumplimiento: la población asignada que escribe el usuario frente a las personas que ya tienen ficha,
 * por grupo de edad y sexo.
 */
@Composable
fun CumplimientoScreen(organizacionId: String, onRegresar: () -> Unit) {
    val context = LocalContext.current
    val repositorio = remember { PoblacionAsignadaRepositorio(context) }
    var asignada by remember(organizacionId) { mutableStateOf(repositorio.cargar(organizacionId)) }
    var editando by remember { mutableStateOf(false) }
    val miembros by remember { RuralitosDatabase.obtenerBaseDatos(context).fichaContenidoDao().listarTodosMiembros() }
        .collectAsState(initial = emptyList())
    val resumen = remember(asignada, miembros) { CumplimientoPoblacion.calcular(asignada, miembros) }

    PantallaRuralitos(
        titulo = "Cumplimiento",
        descripcion = "Las personas con ficha frente a la población que tienes asignada.",
        onVolver = onRegresar
    ) {
        TarjetaGeneral(resumen.asignados, resumen.registrados, resumen.porcentaje, resumen.nivel,
            resumen.asignadosHombres, resumen.asignadosMujeres, resumen.registradosHombres, resumen.registradosMujeres,
            onEditar = { editando = true })

        if (asignada.vacia) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, AzulClinico.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth().testTag("cumplimiento_sin_poblacion")
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Escribe tu población asignada", fontWeight = FontWeight.SemiBold, color = AzulClinicoOscuro)
                    Text(
                        "Con la tabla de tu unidad, las barras mostrarán cuántas personas de cada grupo ya tienen ficha. " +
                            "Mientras tanto verás cuántas fichas llevas por grupo.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BotonPrincipalRuralitos(
                        texto = "Escribir población asignada",
                        onClick = { editando = true },
                        modifier = Modifier.fillMaxWidth().testTag("escribir_poblacion")
                    )
                }
            }
        }

        Text("Por grupo de edad", fontWeight = FontWeight.SemiBold, color = AzulClinicoOscuro,
            modifier = Modifier.padding(top = 4.dp))
        resumen.grupos.forEach { TarjetaGrupoCumplimiento(it) }

        if (resumen.sinClasificar > 0) {
            Text(
                "${resumen.sinClasificar} persona(s) con ficha no se cuentan porque les falta la fecha de nacimiento o el sexo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("sin_clasificar")
            )
        }
        Leyenda()
    }

    if (editando) {
        VentanaPoblacion(
            inicial = asignada,
            onCancelar = { editando = false },
            onGuardar = {
                repositorio.guardar(organizacionId, it)
                asignada = it
                editando = false
            }
        )
    }
}

@Composable
private fun TarjetaGeneral(
    asignados: Int, registrados: Int, porcentaje: Int, nivel: NivelCumplimiento,
    asignadosH: Int, asignadosM: Int, registradosH: Int, registradosM: Int,
    onEditar: () -> Unit
) {
    val color = colorNivel(nivel)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BordeClinico),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().testTag("cumplimiento_general")
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextoAjustado(
                    "Cumplimiento general",
                    modifier = Modifier.weight(1f),
                    tamano = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AzulClinicoOscuro
                )
                TextButton(onClick = onEditar, modifier = Modifier.testTag("boton_poblacion")) {
                    TextoAjustado("Población", tamano = 14.sp, color = AzulClinico, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (asignados == 0) "—" else "$porcentaje %",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.testTag("porcentaje_general")
                )
                Text(
                    if (asignados == 0) "$registrados persona(s) con ficha" else "$registrados de $asignados personas con ficha",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp).weight(1f)
                )
            }
            Barra(porcentaje, color, Modifier.padding(top = 8.dp), alto = 14.dp)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CasillaSexo("Hombres", registradosH, asignadosH, Modifier.weight(1f))
                CasillaSexo("Mujeres", registradosM, asignadosM, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CasillaSexo(titulo: String, registrados: Int, asignados: Int, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F6F9)).padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(titulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextoAjustado(
            if (asignados == 0) "$registrados con ficha"
            else "$registrados de $asignados · ${CumplimientoPoblacion.porcentaje(registrados, asignados)} %",
            tamano = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AzulClinicoOscuro
        )
    }
}

@Composable
private fun Barra(porcentaje: Int, color: Color, modifier: Modifier = Modifier, alto: androidx.compose.ui.unit.Dp = 10.dp) {
    Box(
        modifier.fillMaxWidth().height(alto).clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f))
    ) {
        Box(
            Modifier
                .fillMaxWidth((porcentaje.coerceIn(0, 100)) / 100f)
                .height(alto)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

@Composable
private fun TarjetaGrupoCumplimiento(g: CumplimientoGrupo) {
    val color = colorNivel(g.nivel)
    val bitmap = RecursosIconografiaMais.recurso(g.grupo.icono)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, color.copy(alpha = 0.30f)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().testTag("grupo_${g.grupo.name}")
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = color.copy(alpha = 0.09f), modifier = Modifier.size(46.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (bitmap != null) Image(
                            painter = painterResource(bitmap),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    TextoAjustado(g.grupo.titulo, tamano = 16.sp, fontWeight = FontWeight.SemiBold, color = AzulClinicoOscuro)
                    Text(
                        if (g.asignados == 0) "Sin población asignada" else "Asignados: ${g.asignados}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (g.asignados == 0) "${g.registrados}" else "${g.registrados} / ${g.asignados}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = AzulClinicoOscuro,
                        modifier = Modifier.testTag("cantidad_${g.grupo.name}")
                    )
                    if (g.asignados > 0) Text(
                        "${g.porcentaje} %",
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("porcentaje_${g.grupo.name}")
                    )
                }
            }
            Barra(g.porcentaje, color, Modifier.padding(top = 10.dp))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                DetalleSexo("Hombres", g.registradosHombres, g.asignadosHombres)
                DetalleSexo("Mujeres", g.registradosMujeres, g.asignadosMujeres)
                val resto = when {
                    g.asignados == 0 -> ""
                    g.sobran > 0 -> "Superan por ${g.sobran}"
                    else -> "Faltan ${g.faltan}"
                }
                if (resto.isNotEmpty()) Text(resto, color = color, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DetalleSexo(titulo: String, registrados: Int, asignados: Int) {
    Text(
        if (asignados == 0) "$titulo $registrados" else "$titulo $registrados de $asignados",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun Leyenda() {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            RojoClinico to "Menos de ${CumplimientoPoblacion.CORTE_MEDIO} %",
            NaranjaClinico to "${CumplimientoPoblacion.CORTE_MEDIO} a ${CumplimientoPoblacion.CORTE_ALTO - 1} %",
            VerdeSalud to "${CumplimientoPoblacion.CORTE_ALTO} % o más"
        ).forEach { (color, texto) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                TextoAjustado(texto, modifier = Modifier.padding(start = 5.dp), tamano = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Ventana con la tabla «Población asignada»: hombres y mujeres por grupo; el total se suma solo. */
@Composable
private fun VentanaPoblacion(inicial: PoblacionAsignada, onCancelar: () -> Unit, onGuardar: (PoblacionAsignada) -> Unit) {
    val textos = remember {
        mutableStateMapOf<String, String>().apply {
            GrupoCumplimiento.entries.forEach {
                put("${it.name}|H", inicial.hombres(it).takeIf { v -> v > 0 }?.toString().orEmpty())
                put("${it.name}|M", inicial.mujeres(it).takeIf { v -> v > 0 }?.toString().orEmpty())
            }
        }
    }
    fun valor(g: GrupoCumplimiento, sexo: String) = textos["${g.name}|$sexo"]?.toIntOrNull() ?: 0
    val totalH = GrupoCumplimiento.entries.sumOf { valor(it, "H") }
    val totalM = GrupoCumplimiento.entries.sumOf { valor(it, "M") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Población asignada") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).testTag("ventana_poblacion")) {
                Text(
                    "Escribe lo que dice la tabla de tu unidad. El total se suma solo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FilaTabla("Grupo", "Hombres", "Mujeres", "Total", encabezado = true)
                GrupoCumplimiento.entries.forEach { g ->
                    FilaTabla(
                        g.titulo,
                        textos["${g.name}|H"].orEmpty(),
                        textos["${g.name}|M"].orEmpty(),
                        (valor(g, "H") + valor(g, "M")).toString(),
                        onHombres = { textos["${g.name}|H"] = CumplimientoPoblacion.limpiarCasilla(it) },
                        onMujeres = { textos["${g.name}|M"] = CumplimientoPoblacion.limpiarCasilla(it) },
                        etiqueta = g.name
                    )
                }
                FilaTabla("Ciclos de vida", totalH.toString(), totalM.toString(), (totalH + totalM).toString(), esTotal = true)
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("guardar_poblacion"),
                onClick = {
                    onGuardar(PoblacionAsignada(GrupoCumplimiento.entries.associateWith { valor(it, "H") to valor(it, "M") }))
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun FilaTabla(
    grupo: String,
    hombres: String,
    mujeres: String,
    totalTexto: String,
    encabezado: Boolean = false,
    esTotal: Boolean = false,
    onHombres: ((String) -> Unit)? = null,
    onMujeres: ((String) -> Unit)? = null,
    etiqueta: String = ""
) {
    val peso = if (encabezado || esTotal) FontWeight.SemiBold else FontWeight.Normal
    val tamano = if (encabezado) 12.sp else 14.sp
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TextoAjustado(grupo, modifier = Modifier.weight(1.45f), tamano = tamano, fontWeight = peso, maxLineas = 2,
            color = if (encabezado) MaterialTheme.colorScheme.onSurfaceVariant else AzulClinicoOscuro)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (onHombres != null) CasillaNumero(hombres, onHombres, "Hombres, $grupo", "casilla_${etiqueta}_H")
            else TextoAjustado(hombres, tamano = tamano, fontWeight = peso, textAlign = TextAlign.Center)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (onMujeres != null) CasillaNumero(mujeres, onMujeres, "Mujeres, $grupo", "casilla_${etiqueta}_M")
            else TextoAjustado(mujeres, tamano = tamano, fontWeight = peso, textAlign = TextAlign.Center)
        }
        Box(Modifier.weight(0.8f), contentAlignment = Alignment.Center) {
            TextoAjustado(totalTexto, tamano = tamano, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                color = AzulClinicoOscuro)
        }
    }
}

@Composable
private fun CasillaNumero(texto: String, onCambio: (String) -> Unit, descripcion: String, etiqueta: String) {
    BasicTextField(
        value = texto,
        onValueChange = onCambio,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AzulClinicoOscuro, textAlign = TextAlign.Center),
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .testTag(etiqueta)
            .semantics { contentDescription = descripcion },
        decorationBox = { campo ->
            Box(
                Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF1F6F9))
                    .border(1.dp, BordeClinico, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (texto.isEmpty()) Text("0", color = GrisSinMeta, fontSize = 15.sp)
                campo()
            }
        }
    )
}
