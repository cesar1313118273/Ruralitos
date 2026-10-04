package com.ruralitos.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeCampo
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.TextoSecundario
import androidx.compose.foundation.border
import androidx.compose.ui.platform.testTag

/**
 * Ventana emergente con el diseño general de la app: tarjeta blanca de bordes finos, encabezado con símbolo de color,
 * contenido que se desplaza si no cabe y botones al pie con el mismo estilo que los de las pantallas.
 */
@Composable
fun VentanaRuralitos(
    titulo: String,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    simbolo: String? = null,
    color: Color = AzulClinico,
    /** Las ventanas importantes (eliminar, archivos descargados) solo se cierran con sus botones. */
    cerrarAlTocarFuera: Boolean = true,
    contenido: @Composable ColumnScope.() -> Unit,
    acciones: @Composable ColumnScope.() -> Unit = {}
) {
    Dialog(
        onDismissRequest = { if (cerrarAlTocarFuera) onCerrar() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = cerrarAlTocarFuera,
            dismissOnClickOutside = cerrarAlTocarFuera
        )
    ) {
        Surface(
            modifier = modifier.padding(horizontal = 20.dp, vertical = 24.dp).widthIn(max = 420.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BordeClinico),
            shadowElevation = 8.dp
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (simbolo != null) {
                        Box(
                            Modifier.size(40.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(simbolo, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = if (simbolo != null) 12.dp else 0.dp, end = 8.dp)) {
                        Text(
                            titulo,
                            style = MaterialTheme.typography.titleMedium,
                            color = AzulClinicoOscuro,
                            fontWeight = FontWeight.SemiBold
                        )
                        subtitulo?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Box(
                        Modifier
                            .size(36.dp)
                            .semantics { contentDescription = "Cerrar"; role = Role.Button }
                            .clickable(onClick = onCerrar),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
                    }
                }
                HorizontalDivider(color = BordeClinico)
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = contenido
                )
                Column(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 18.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = acciones
                )
            }
        }
    }
}

/** Línea de datos de una ventana: etiqueta pequeña arriba y el valor debajo. */
@Composable
fun DatoVentanaRuralitos(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge, color = AzulClinicoOscuro)
    }
}


/**
 * Ventana de confirmación con el diseño de la app: ícono de color, mensaje y dos botones (el principal de color y
 * «cancelar» con borde). Con [peligro] es roja y no se cierra al tocar fuera.
 */
@Composable
fun VentanaConfirmarRuralitos(
    titulo: String,
    mensaje: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    textoCancelar: String = "Cancelar",
    peligro: Boolean = true,
    subtitulo: String? = null,
    simbolo: String = if (peligro) "!" else "i",
    color: Color = if (peligro) RojoClinico else CianRuralitos,
    cerrarAlTocarFuera: Boolean = !peligro,
    confirmarHabilitado: Boolean = true,
    etiquetaPruebaConfirmar: String? = null,
    contenidoExtra: @Composable ColumnScope.() -> Unit = {}
) {
    VentanaRuralitos(
        titulo = titulo,
        onCerrar = onCancelar,
        subtitulo = subtitulo,
        simbolo = simbolo,
        color = color,
        cerrarAlTocarFuera = cerrarAlTocarFuera,
        contenido = {
            if (mensaje.isNotBlank()) {
                Text(mensaje, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
            }
            contenidoExtra()
        },
        acciones = {
            BotonPrincipalRuralitos(
                texto = textoConfirmar,
                color = color,
                enabled = confirmarHabilitado,
                modifier = if (etiquetaPruebaConfirmar != null) Modifier.testTag(etiquetaPruebaConfirmar) else Modifier,
                onClick = onConfirmar
            )
            BotonSecundarioRuralitos(texto = textoCancelar, onClick = onCancelar)
        }
    )
}

/** Fila de una lista dentro de una ventana: círculo con inicial, dos líneas de texto y una flecha. */
@Composable
fun FilaListaVentanaRuralitos(
    titulo: String,
    detalle: String? = null,
    inicial: String = titulo.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
    color: Color = AzulClinico,
    marcada: Boolean? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(32.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) { Text(inicial, color = color, fontWeight = FontWeight.SemiBold) }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(titulo, color = AzulClinicoOscuro, style = MaterialTheme.typography.bodyLarge)
            if (!detalle.isNullOrBlank()) {
                Text(detalle, color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (marcada != null) {
            CasillaRuralitos(marcada)
        } else {
            Text("›", color = TextoSecundario, style = MaterialTheme.typography.titleLarge)
        }
    }
    HorizontalDivider(color = BordeClinico)
}

/** Casilla cuadrada con borde fino que se llena de color al marcarla. */
@Composable
fun CasillaRuralitos(marcada: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(20.dp)
            .background(if (marcada) CianRuralitos else Color.White, RoundedCornerShape(6.dp))
            .border(1.5.dp, if (marcada) CianRuralitos else BordeCampo, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (marcada) Text("✓", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
