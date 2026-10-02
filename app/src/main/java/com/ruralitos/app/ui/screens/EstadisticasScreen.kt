package com.ruralitos.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.PantallaListaRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.data.local.dao.ResumenFichas
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.VerdeSalud
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EstadisticasScreen(
    onFichaSeleccionada: (FichaFamiliarEntity) -> Unit,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember(context) { RuralitosDatabase.obtenerBaseDatos(context).fichaFamiliarDao() }
    val hoy = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    var fecha by remember { mutableStateOf(hoy) }
    var texto by remember { mutableStateOf("") }
    var mostrarCalendario by remember { mutableStateOf(false) }
    val resumen by dao.observarResumen().collectAsState(ResumenFichas(0, 0, 0, 0))
    val flujoFecha = remember(fecha) { dao.observarPorFecha(fecha) }
    val fichasFecha by flujoFecha.collectAsState(initial = emptyList())
    val fichasVisibles = remember(fichasFecha, texto) {
        val filtro = texto.trim()
        if (filtro.isBlank()) fichasFecha else fichasFecha.filter {
            it.nombreApellidoJefeFamilia.contains(filtro, true) ||
                it.cedulaJefeHogar.contains(filtro, true) ||
                it.numeroFichaFamiliar.contains(filtro, true) ||
                it.barrio.contains(filtro, true) ||
                it.comunidad.contains(filtro, true)
        }
    }
    val borradoresFecha = fichasFecha.count { it.estado == "BORRADOR" }
    val completasFecha = fichasFecha.count { it.estado == "COMPLETA" }
    val archivadasFecha = fichasFecha.count { it.estado == "ARCHIVADA" }

    PantallaListaRuralitos(
        titulo = "Estadísticas",
        descripcion = "Mide el avance diario y encuentra rápidamente las fichas que requieren seguimiento.",
        subtitulo = "Actividad de fichas",
    ) {
item {
            TarjetasResumen(resumen)
}
        item { GraficoEstadoFichas(resumen) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BordeClinico),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        if (fecha == hoy) "Actividad de hoy" else "Actividad del $fecha",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${fichasFecha.size} registradas · $borradoresFecha borradores · $completasFecha completas · $archivadasFecha archivadas",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                    val avance = if (fichasFecha.isEmpty()) 0f else completasFecha.toFloat() / fichasFecha.size
                    LinearProgressIndicator(
                        progress = { avance },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = CianRuralitos,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    CampoFecha(fecha, "Fecha de búsqueda") { mostrarCalendario = true }
                    if (fecha != hoy) {
                        OutlinedButton(
                            onClick = { fecha = hoy },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) { Text("Volver a la fecha actual") }
                    }
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        label = { Text("Buscar en la fecha seleccionada") },
                        supportingText = { Text("Nombre, identificación, ficha o barrio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }
        }
        if (fichasVisibles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        "No existen fichas que coincidan con la fecha y búsqueda seleccionadas.",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(fichasVisibles, key = { it.id }) { ficha ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onFichaSeleccionada(ficha) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BordeClinico),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(ficha.nombreApellidoJefeFamilia, fontWeight = FontWeight.Bold)
                        Text(
                            "${ficha.numeroFichaFamiliar} · ${ficha.barrio.ifBlank { ficha.comunidad.ifBlank { ficha.parroquia } }}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        ficha.estado,
                        color = when (ficha.estado) {
                            "COMPLETA" -> VerdeSalud
                            "ARCHIVADA" -> MoradoClinico
                            else -> NaranjaClinico
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            BotonSecundarioRuralitos(
                texto = "Regresar al inicio",
                descripcion = "Volver al panel principal",
                onClick = onRegresar
            )
            Spacer(Modifier.height(14.dp))
        }
    }

    if (mostrarCalendario) {
        SelectorFechaDialog(
            onFechaSeleccionada = { fecha = it; mostrarCalendario = false },
            onCerrar = { mostrarCalendario = false }
        )
    }
}

@Composable
private fun GraficoEstadoFichas(resumen: ResumenFichas) {
    val total = resumen.total.coerceAtLeast(1)
    val partes = listOf(
        Triple("Completas", resumen.completas, CianRuralitos),
        Triple("Borradores", resumen.borradores, NaranjaClinico),
        Triple("Archivadas", resumen.archivadas, MoradoClinico)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BordeClinico)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Text("Estado de fichas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 13.dp.toPx()
                        drawArc(BordeClinico, -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                        var inicio = -90f
                        partes.forEach { (_, cantidad, color) ->
                            val sweep = 360f * cantidad.coerceAtLeast(0) / total
                            if (sweep > 0f) drawArc(color, inicio, sweep, false, style = Stroke(stroke, cap = StrokeCap.Butt))
                            inicio += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(resumen.total.toString(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("fichas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    partes.forEach { (titulo, cantidad, color) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(Modifier.size(8.dp)) { drawCircle(color) }
                            Text(titulo, modifier = Modifier.weight(1f).padding(start = 7.dp), style = MaterialTheme.typography.bodySmall)
                            Text(cantidad.toString(), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetasResumen(resumen: ResumenFichas) {
    val tarjetas = listOf(
        Triple("Total", resumen.total, AzulClinico),
        Triple("Borradores", resumen.borradores, NaranjaClinico),
        Triple("Completas", resumen.completas, CianRuralitos),
        Triple("Archivadas", resumen.archivadas, MoradoClinico)
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BordeClinico),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("Resumen general", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                tarjetas.forEach { (titulo, valor, color) ->
                    Column(Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(valor.toString(), color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            titulo,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
