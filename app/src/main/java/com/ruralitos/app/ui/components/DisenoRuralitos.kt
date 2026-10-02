package com.ruralitos.app.ui.components

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
import com.ruralitos.app.R
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.FondoRuralitosWeb
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.VerdeClinico
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeClinico
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
    Column(modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.ruralitos_paisaje_cabecera),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .clip(RoundedCornerShape(RuralitosRadius.card)),
            contentScale = ContentScale.Crop
        )
        Column(
            Modifier.fillMaxWidth()
                .padding(horizontal = RuralitosSpacing.xs, vertical = RuralitosSpacing.sm)
        ) {
            paso?.let {
                Text(
                    text = it.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = AzulClinicoOscuro,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = if (paso == null) 0.dp else 2.dp)
            )
            if (descripcion.isNotBlank()) {
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SeccionFormularioRuralitos(
    titulo: String,
    descripcion: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth().padding(vertical = RuralitosSpacing.xs)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = RuralitosSpacing.xs, vertical = 6.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
            descripcion?.let {
                Text(
                    limpiarTextoInterfaz(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
        Column(Modifier.padding(horizontal = RuralitosSpacing.xs, vertical = RuralitosSpacing.sm)) {
            content()
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
                        fontWeight = if (opcion == seleccionada) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                },
                modifier = Modifier.heightIn(min = 44.dp),
                shape = RoundedCornerShape(RuralitosRadius.input),
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
            .heightIn(min = 52.dp)
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
            .heightIn(min = 50.dp)
            .alpha(if (enabled) 1f else 0.52f)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .clip(forma)
            .background(colorFondo)
            .border(1.dp, MaterialTheme.colorScheme.outline, forma)
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
            color = MaterialTheme.colorScheme.primary,
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
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
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
            .background(color.copy(alpha = 0.07f))
            .border(1.dp, color.copy(alpha = 0.42f), forma)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            limpiarTextoInterfaz(texto),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraBold
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
        elevation = CardDefaults.cardElevation(defaultElevation = RuralitosElevation.card),
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
                        .size(38.dp)
                        .background(color.copy(alpha = 0.11f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        simbolo,
                        color = color,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
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
