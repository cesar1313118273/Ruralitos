package com.ruralitos.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.AzulSuaveRuralitos
import com.ruralitos.app.ui.theme.BordeCampo
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.CianSuave
import com.ruralitos.app.ui.theme.TextoSecundario
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ruralitos.app.domain.textoParaBuscar

/** Una opción que se puede elegir escribiendo. [detalle] es un texto corto que acompaña (p. ej. «Riesgo 2»). */
data class OpcionBusqueda(
    val codigo: String,
    val etiqueta: String,
    val detalle: String = "",
    val grupoIII: Boolean = false
) {
    val rotulo: String get() = buildString {
        append(etiqueta)
        if (detalle.isNotBlank()) append(" · ").append(detalle)
        if (grupoIII) append(" · Grupo III")
    }
}

/**
 * Cuadro de texto que va mostrando opciones mientras se escribe. Lo elegido queda como etiquetas que se pueden quitar;
 * las [automaticos] se detectaron solas con otros datos y no se quitan desde aquí.
 */
@Composable
fun BuscadorDeFactores(
    titulo: String,
    opciones: List<OpcionBusqueda>,
    elegidos: Set<String>,
    onCambio: (Set<String>) -> Unit,
    prefijoPrueba: String,
    modifier: Modifier = Modifier,
    automaticos: Set<String> = emptySet(),
    maximoSugerencias: Int = 8
) {
    var escrito by remember { mutableStateOf("") }
    val buscado = textoParaBuscar(escrito)
    val sugerencias = if (buscado.isEmpty()) emptyList() else opciones
        .filter { it.codigo !in elegidos && it.codigo !in automaticos && textoParaBuscar(it.etiqueta).contains(buscado) }
        .take(maximoSugerencias)
    val porCodigo = opciones.associateBy { it.codigo }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        OutlinedTextField(
            value = escrito,
            onValueChange = { escrito = it },
            label = { Text(titulo) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CianRuralitos,
                unfocusedBorderColor = BordeCampo,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().testTag("${prefijoPrueba}_buscar")
        )
        if (buscado.isNotEmpty() && sugerencias.isEmpty()) {
            Text(
                "No hay opciones que coincidan con lo que escribes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        ListaSugerenciasRuralitos(
            sugerencias = sugerencias.map {
                SugerenciaRuralitos(
                    clave = it.codigo, titulo = it.etiqueta, detalle = it.detalle,
                    marca = if (it.grupoIII) "Grupo III" else null
                )
            },
            etiquetaPrueba = { "${prefijoPrueba}_sugerencia_$it" },
            onElegir = { clave -> onCambio(elegidos + clave); escrito = "" }
        )
        (elegidos + automaticos).mapNotNull { porCodigo[it] }.forEach { opcion ->
            val automatico = opcion.codigo in automaticos
            ChipElegidoRuralitos(
                texto = opcion.rotulo + if (automatico) " · automático" else "",
                onQuitar = if (automatico) null else ({ onCambio(elegidos - opcion.codigo) }),
                etiquetaPrueba = "${prefijoPrueba}_${opcion.codigo}"
            )
        }
    }
}


/** Una sugerencia del buscador: [codigo] (si lo hay) va en una insignia; [marca] es una etiqueta corta, p. ej. «Grupo III». */
data class SugerenciaRuralitos(
    val clave: String,
    val titulo: String,
    val codigo: String? = null,
    val detalle: String = "",
    val marca: String? = null
)

/**
 * Resultados de un buscador en una tarjeta de bordes finos: insignia con el código, la descripción, una marca opcional y
 * un círculo «+» para agregar. Se usa en el CIE-10, los factores de riesgo y los criterios obstétricos.
 */
@Composable
fun ListaSugerenciasRuralitos(
    sugerencias: List<SugerenciaRuralitos>,
    etiquetaPrueba: (String) -> String,
    onElegir: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sugerencias.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BordeClinico),
        shadowElevation = 3.dp
    ) {
        Column {
            sugerencias.forEachIndexed { indice, s ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onElegir(s.clave) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag(etiquetaPrueba(s.clave)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (s.codigo != null) {
                        Text(
                            s.codigo,
                            color = AzulClinico,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(AzulSuaveRuralitos, RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Column(Modifier.weight(1f).padding(start = if (s.codigo != null) 10.dp else 0.dp)) {
                        Text(s.titulo, color = AzulClinicoOscuro, style = MaterialTheme.typography.bodyMedium)
                        if (s.detalle.isNotBlank()) {
                            Text(s.detalle, color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (s.marca != null) {
                        Text(
                            s.marca,
                            color = Color(0xFFA02834),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .background(Color(0xFFFCE8EA), RoundedCornerShape(7.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Box(
                        Modifier.size(26.dp).background(CianRuralitos, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("+", color = Color.White, fontWeight = FontWeight.Bold) }
                }
                if (indice < sugerencias.lastIndex) HorizontalDivider(color = BordeClinico)
            }
        }
    }
}

/** Lo ya elegido: etiqueta de fondo suave con borde de color y una ✕ para quitarla (si se puede). */
@Composable
fun ChipElegidoRuralitos(
    texto: String,
    onQuitar: (() -> Unit)?,
    etiquetaPrueba: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .padding(top = 6.dp)
            .background(CianSuave, RoundedCornerShape(10.dp))
            .border(1.dp, CianRuralitos.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .then(if (onQuitar != null) Modifier.clickable(onClick = onQuitar) else Modifier)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag(etiquetaPrueba),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto, color = Color(0xFF066B7E), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f, fill = false))
        if (onQuitar != null) {
            Text("  ✕", color = Color(0xFF066B7E), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}
