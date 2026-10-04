package com.ruralitos.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        sugerencias.forEach { opcion ->
            TextButton(
                onClick = { onCambio(elegidos + opcion.codigo); escrito = "" },
                modifier = Modifier.fillMaxWidth().testTag("${prefijoPrueba}_sugerencia_${opcion.codigo}")
            ) { Text("+ ${opcion.rotulo}", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
        }
        (elegidos + automaticos).mapNotNull { porCodigo[it] }.forEach { opcion ->
            val automatico = opcion.codigo in automaticos
            FilterChip(
                selected = true,
                onClick = { if (!automatico) onCambio(elegidos - opcion.codigo) },
                label = { Text(opcion.rotulo + if (automatico) " · automático" else "  ×") },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("${prefijoPrueba}_${opcion.codigo}")
            )
        }
    }
}
