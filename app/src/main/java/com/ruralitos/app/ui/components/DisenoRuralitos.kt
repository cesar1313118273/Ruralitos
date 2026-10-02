package com.ruralitos.app.ui.components

import com.ruralitos.app.ui.theme.VerdeSalud
import com.ruralitos.app.ui.theme.VerdeSuaveRuralitos
import com.ruralitos.app.ui.theme.CianSuave
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.BorderStroke
import androidx.annotation.DrawableRes
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.R
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.FondoRuralitosWeb
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.BordeCampo
import com.ruralitos.app.ui.theme.RuralitosElevation
import com.ruralitos.app.ui.theme.RuralitosRadius
import com.ruralitos.app.ui.theme.RuralitosSpacing

@Composable
fun FondoRuralitos(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoRuralitosWeb),
        content = content
    )
}

@Composable
fun ContenidoAdaptable(
    modifier: Modifier = Modifier,
    anchoMaximo: Dp = 1180.dp,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val margen = when {
            maxWidth < 420.dp -> 0.dp
            maxWidth < 700.dp -> 8.dp
            else -> 18.dp
        }
        val ancho = (maxWidth - margen * 2).coerceAtMost(anchoMaximo)
        Box(
            modifier = Modifier
                .width(ancho)
                .fillMaxHeight()
                .align(Alignment.TopCenter)
                .padding(horizontal = margen),
            content = content
        )
    }
}

/** Mantiene visible la última casilla cuando aparece el teclado. */
fun Modifier.formularioSeguro(): Modifier =
    imePadding().navigationBarsPadding()

@Composable
fun LogoRuralitos(
    modifier: Modifier = Modifier,
    descripcion: String = "Ruralitos"
) {
    Image(
        painter = painterResource(R.drawable.ruralitos_logo),
        contentDescription = descripcion,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
fun EncabezadoRuralitos(
    titulo: String,
    descripcion: String,
    paso: String? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RuralitosRadius.card))
            .background(Color.White)
            .border(1.dp, BordeClinico, RoundedCornerShape(RuralitosRadius.card))
            .padding(RuralitosSpacing.base),
        horizontalArrangement = Arrangement.spacedBy(RuralitosSpacing.md)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .heightIn(min = 36.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Column(Modifier.weight(1f)) {
            paso?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = AzulClinicoOscuro,
                modifier = Modifier.padding(top = if (paso == null) 0.dp else 2.dp)
            )
            if (descripcion.isNotBlank()) {
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/** Barra de avance segmentada: un bloque por paso. */
@Composable
fun BarraAvanceRuralitos(
    paso: Int,
    total: Int,
    etiqueta: String,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 1..total) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                i < paso -> MaterialTheme.colorScheme.primary
                                i == paso -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else -> BordeClinico
                            }
                        )
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$paso de $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Botón de regreso a la ventana anterior, siempre arriba a la izquierda. */
@Composable
fun BotonVolverRuralitos(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(RuralitosRadius.input))
            .background(Color.White)
            .border(1.5.dp, AzulClinicoOscuro.copy(alpha = 0.55f), RoundedCornerShape(RuralitosRadius.input))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "‹",
            color = AzulClinicoOscuro,
            fontSize = 30.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun EncabezadoPantallaRuralitos(
    titulo: String,
    subtitulo: String?,
    paso: Int?,
    totalPasos: Int?,
    etiquetaPaso: String,
    onVolver: (() -> Unit)? = null,
    descripcion: String? = null
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = RuralitosSpacing.base)
            .padding(top = RuralitosSpacing.md, bottom = RuralitosSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            onVolver?.let {
                BotonVolverRuralitos(it, Modifier.padding(end = RuralitosSpacing.md))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleLarge,
                    color = AzulClinicoOscuro,
                )
                subtitulo?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                descripcion?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        limpiarTextoInterfaz(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
        if (paso != null && totalPasos != null) {
            BarraAvanceRuralitos(
                paso, totalPasos, etiquetaPaso,
                Modifier.padding(top = RuralitosSpacing.md)
            )
        }
    }
    HorizontalDivider(color = BordeClinico)
}

@Composable
fun BarraAccionPantallaRuralitos(barraAccion: @Composable ColumnScope.() -> Unit) {
    HorizontalDivider(color = BordeClinico)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = RuralitosSpacing.base, vertical = RuralitosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RuralitosSpacing.sm),
        content = barraAccion
    )
}

@Composable
private fun DescripcionPantallaRuralitos(descripcion: String?) {
    descripcion?.takeIf { it.isNotBlank() }?.let {
        Text(
            limpiarTextoInterfaz(it),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Estructura común de una pantalla: encabezado blanco con título y avance,
 * contenido con desplazamiento y barra de acciones fija abajo.
 * Solo organiza el diseño; las acciones las define cada pantalla.
 */
@Composable
fun PantallaRuralitos(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    descripcion: String? = null,
    paso: Int? = null,
    totalPasos: Int? = null,
    etiquetaPaso: String = "",
    onVolver: (() -> Unit)? = null,
    scrollHabilitado: Boolean = true,
    barraAccion: (@Composable ColumnScope.() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .background(FondoRuralitosWeb)
            .formularioSeguro()
    ) {
        EncabezadoPantallaRuralitos(titulo, subtitulo, paso, totalPasos, etiquetaPaso, onVolver, descripcion)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState(), enabled = scrollHabilitado)
                .padding(RuralitosSpacing.base),
            verticalArrangement = Arrangement.spacedBy(RuralitosSpacing.md)
        ) {
            contenido()
        }
        if (barraAccion != null) BarraAccionPantallaRuralitos(barraAccion)
    }
}

/** Igual que [PantallaRuralitos] pero para listas con muchos registros. */
@Composable
fun PantallaListaRuralitos(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    descripcion: String? = null,
    paso: Int? = null,
    totalPasos: Int? = null,
    etiquetaPaso: String = "",
    onVolver: (() -> Unit)? = null,
    barraAccion: (@Composable ColumnScope.() -> Unit)? = null,
    contenido: LazyListScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .background(FondoRuralitosWeb)
            .formularioSeguro()
    ) {
        EncabezadoPantallaRuralitos(titulo, subtitulo, paso, totalPasos, etiquetaPaso, onVolver, descripcion)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(RuralitosSpacing.base),
            verticalArrangement = Arrangement.spacedBy(RuralitosSpacing.md)
        ) {
            contenido()
        }
        if (barraAccion != null) BarraAccionPantallaRuralitos(barraAccion)
    }
}

/** Flecha de desplegable dibujada: centrada, ancha y de poca altura. */
@Composable
fun FlechaDesplegable(
    color: Color,
    modifier: Modifier = Modifier,
    arriba: Boolean = false,
    tamano: Dp = 16.dp
) {
    androidx.compose.foundation.Canvas(modifier.size(tamano)) {
        val w = size.width
        val h = size.height
        val yAlta = if (arriba) h * 0.62f else h * 0.38f
        val yBaja = if (arriba) h * 0.38f else h * 0.62f
        val grosor = 2.2.dp.toPx()
        val trazo = androidx.compose.ui.graphics.drawscope.Stroke(
            width = grosor,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
        val camino = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.18f, yAlta)
            lineTo(w * 0.50f, yBaja)
            lineTo(w * 0.82f, yAlta)
        }
        drawPath(camino, color, style = trazo)
    }
}

/** Campo tipo "spinner": muestra el valor elegido y una flecha para desplegar opciones. */
@Composable
fun BotonSelectorRuralitos(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val forma = RoundedCornerShape(RuralitosRadius.input)
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .alpha(if (enabled) 1f else 0.55f)
            .clip(forma)
            .background(Color.White)
            .border(1.dp, BordeCampo, forma)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { content() }
        Box(
            Modifier
                .padding(start = 8.dp)
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            FlechaDesplegable(color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Ventana de opciones del spinner, con el mismo diseño en toda la app. */
@Composable
fun MenuDesplegableRuralitos(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .background(Color.White)
            .widthIn(min = 220.dp),
        shape = RoundedCornerShape(RuralitosRadius.card),
        containerColor = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, BordeClinico),
        content = content
    )
}

/** Opción del spinner: texto grande y fácil de tocar, separada por una línea fina. */
@Composable
fun ItemMenuRuralitos(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    DropdownMenuItem(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        colors = MenuDefaults.itemColors(textColor = AzulClinicoOscuro)
    )
}

/** Tarjeta blanca de bordes finos para agrupar campos dentro de una pantalla. */
@Composable
fun TarjetaFormularioRuralitos(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RuralitosRadius.card))
            .background(Color.White)
            .border(1.dp, BordeClinico, RoundedCornerShape(RuralitosRadius.card))
            .padding(RuralitosSpacing.base),
        verticalArrangement = Arrangement.spacedBy(RuralitosSpacing.md),
        content = content
    )
}

@Composable
fun SeccionFormularioRuralitos(
    titulo: String,
    descripcion: String? = null,
    modifier: Modifier = Modifier,
    desplegable: Boolean = false,
    abiertaInicial: Boolean = true,
    progreso: Float? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var abierta by rememberSaveable(titulo) { mutableStateOf(abiertaInicial || !desplegable) }
    val avance by animateFloatAsState(
        targetValue = (progreso ?: 0f).coerceIn(0f, 1f),
        label = "avance_seccion"
    )
    val completo = avance >= 0.999f
    val colorRelleno = if (completo) VerdeSuaveRuralitos else CianSuave
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RuralitosRadius.card))
            .background(Color.White)
            .border(1.dp, BordeClinico, RoundedCornerShape(RuralitosRadius.card))
    ) {
        // Abierta: barra fina de llenado en la parte superior de la sección.
        if (progreso != null && desplegable && abierta) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(BordeClinico)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(avance)
                        .fillMaxHeight()
                        .background(if (completo) VerdeSalud else MaterialTheme.colorScheme.primary)
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    // Cerrada: el propio botón desplegable se va llenando.
                    if (progreso != null && desplegable && !abierta) {
                        Modifier.drawBehind {
                            drawRect(
                                color = colorRelleno,
                                size = androidx.compose.ui.geometry.Size(size.width * avance, size.height)
                            )
                        }
                    } else Modifier
                )
                .then(if (desplegable) Modifier.clickable { abierta = !abierta } else Modifier)
                .padding(RuralitosSpacing.base),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                if (abierta || !desplegable) {
                    descripcion?.let {
                        Text(
                            limpiarTextoInterfaz(it),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            if (desplegable) {
                Box(
                    Modifier
                        .padding(start = RuralitosSpacing.md)
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    FlechaDesplegable(color = MaterialTheme.colorScheme.primary, arriba = abierta)
                }
            }
        }
        if (abierta || !desplegable) {
            Column(
                Modifier.padding(
                    start = RuralitosSpacing.base,
                    end = RuralitosSpacing.base,
                    bottom = RuralitosSpacing.base
                )
            ) {
                content()
            }
        }
    }
}

/** Navegación interna compacta para pantallas con varios bloques de información. */
@Composable
fun SubmenuRuralitos(
    opciones: List<String>,
    seleccionada: String,
    onSeleccionar: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEach { opcion ->
            FilterChip(
                selected = opcion == seleccionada,
                onClick = { onSeleccionar(opcion) },
                label = {
                    Text(
                        opcion,
                        fontWeight = if (opcion == seleccionada) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                modifier = Modifier.heightIn(min = 44.dp),
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    selectedContainerColor = color,
                    selectedLabelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = opcion == seleccionada,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = color
                )
            )
        }
    }
}

@Composable
fun BotonPrincipalRuralitos(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    descripcion: String? = null,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val forma = RoundedCornerShape(RuralitosRadius.button)
    val interacciones = remember { MutableInteractionSource() }
    val presionado by interacciones.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (enabled && presionado) 0.965f else 1f,
        label = "escala_boton_principal"
    )
    val colorFondo by animateColorAsState(
        targetValue = if (enabled && presionado) lerp(color, Color.Black, 0.14f) else color,
        label = "color_boton_principal"
    )
    val indicacion = LocalIndication.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.52f)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .clip(forma)
            .background(colorFondo)
            .clickable(
                interactionSource = interacciones,
                indication = indicacion,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            limpiarTextoInterfaz(texto),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BotonSecundarioRuralitos(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    descripcion: String? = null,
    enabled: Boolean = true
) {
    val forma = RoundedCornerShape(RuralitosRadius.button)
    val interacciones = remember { MutableInteractionSource() }
    val presionado by interacciones.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (enabled && presionado) 0.965f else 1f,
        label = "escala_boton_secundario"
    )
    val colorFondo by animateColorAsState(
        targetValue = if (enabled && presionado) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            Color.White
        },
        label = "color_boton_secundario"
    )
    val indicacion = LocalIndication.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.52f)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .clip(forma)
            .background(colorFondo)
            .border(1.dp, BordeCampo, forma)
            .clickable(
                interactionSource = interacciones,
                indication = indicacion,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            limpiarTextoInterfaz(texto),
            style = MaterialTheme.typography.labelLarge,
            color = AzulClinicoOscuro,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MensajeEstadoRuralitos(
    titulo: String,
    descripcion: String,
    color: Color = AzulClinico,
    simbolo: String = "i",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RuralitosRadius.input),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.07f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.20f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White, RoundedCornerShape(11.dp))
                    .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    simbolo,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

/** Estado vacío de la misma familia visual para listas y registros. */
@Composable
fun RuralitosEmptyState(
    titulo: String,
    descripcion: String,
    @DrawableRes ilustracion: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RuralitosRadius.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        border = BorderStroke(1.dp, BordeClinico)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(RuralitosSpacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RuralitosSpacing.md)
        ) {
            Image(
                painter = painterResource(ilustracion),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Fit
            )
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, color = AzulClinicoOscuro)
                Text(
                    descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BotonAccionRuralitos(
    texto: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val forma = RoundedCornerShape(RuralitosRadius.button)
    Box(
        modifier = modifier
            .heightIn(min = 43.dp)
            .alpha(if (enabled) 1f else 0.48f)
            .clip(forma)
            .background(Color.White)
            .border(1.dp, color.copy(alpha = 0.45f), forma)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            limpiarTextoInterfaz(texto),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TarjetaRegistroRuralitos(
    titulo: String,
    descripcion: String,
    simbolo: String,
    color: Color,
    modifier: Modifier = Modifier,
    onEditar: (() -> Unit)? = null,
    onEliminar: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RuralitosRadius.card),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, BordeClinico)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        simbolo,
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        descripcion,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
            Column(Modifier.padding(top = 7.dp)) {
                content()
            }
            if (onEditar != null || onEliminar != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    onEditar?.let {
                        BotonAccionRuralitos(
                            texto = "Editar",
                            color = AzulClinico,
                            onClick = it,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    onEliminar?.let {
                        BotonAccionRuralitos(
                            texto = "Eliminar",
                            color = RojoClinico,
                            onClick = it,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private fun limpiarTextoInterfaz(valor: String): String = valor
    .replace("\\n", " ")
    .replace('\n', ' ')
    .replace("'n", " ", ignoreCase = true)
    .replace("’n", " ", ignoreCase = true)
    .replace(Regex("\\s{2,}"), " ")
    .trim()
