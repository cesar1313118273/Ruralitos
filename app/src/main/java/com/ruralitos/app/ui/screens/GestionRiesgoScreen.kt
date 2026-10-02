package com.ruralitos.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.GestionRiesgoEntity
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.TarjetaRegistroRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GestionRiesgoScreen(
    fichaId: Long,
    responsableActual: String,
    onContinuar: () -> Unit,
    onSalir: () -> Unit,
    textoRegresar: String = "Volver al panel de la ficha",
    descripcionRegresar: String = "Conservar los datos y salir de esta sección"
) {
    val context = LocalContext.current
    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val scope = rememberCoroutineScope()
    val seguimientos by database.fichaContenidoDao()
        .listarGestionRiesgo(fichaId)
        .collectAsState(initial = emptyList())
    var mostrandoFormulario by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<GestionRiesgoEntity?>(null) }
    var eliminar by remember { mutableStateOf<GestionRiesgoEntity?>(null) }

    if (mostrandoFormulario) {
        FormularioGestionRiesgoScreen(
            fichaId = fichaId,
            item = editando,
            numeroSugerido = seguimientos.size + 1,
            responsableActual = responsableActual,
            onGuardar = { item ->
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            if (item.id == 0L) database.fichaContenidoDao().guardarGestionRiesgo(item)
                            else database.fichaContenidoDao().actualizarGestionRiesgo(item)
                        }
                    }.onSuccess {
                        mostrandoFormulario = false
                        editando = null
                    }.onFailure {
                        Toast.makeText(context, "No se pudo guardar el seguimiento.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCancelar = {
                mostrandoFormulario = false
                editando = null
            }
        )
        return
    }

    eliminar?.let { seleccionado ->
        AlertDialog(
            onDismissRequest = { eliminar = null },
            title = { Text("Eliminar seguimiento") },
            text = {
                Text("Se eliminará el seguimiento número ${seleccionado.numero ?: "sin número"} del ${seleccionado.fechaAnalisis}.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        eliminar = null
                        scope.launch(Dispatchers.IO) {
                            database.fichaContenidoDao().eliminarGestionRiesgo(seleccionado)
                        }
                    }
                ) { Text("Sí, eliminar", color = RojoClinico, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { eliminar = null }) { Text("Conservar seguimiento") }
            }
        )
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .formularioSeguro()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            EncabezadoRuralitos(
                titulo = "Plan y seguimiento del riesgo",
                descripcion = "Registra los compromisos de la familia y del equipo de salud, luego evalúa su cumplimiento.",
                paso = "Gestión del riesgo",
                color = AzulClinico
            )
        }
        item {
            MensajeEstadoRuralitos(
                titulo = "${seguimientos.size} seguimiento(s) registrado(s)",
                descripcion = "Cada visita se conserva como un registro independiente para mostrar la evolución familiar.",
                color = MoradoClinico,
                simbolo = seguimientos.size.toString()
            )
        }
        item {
            BotonPrincipalRuralitos(
                texto = "Agregar nuevo seguimiento",
                descripcion = "Crear compromisos y programar su evaluación",
                color = CianRuralitos,
                onClick = {
                    editando = null
                    mostrandoFormulario = true
                }
            )
        }

        if (seguimientos.isEmpty()) {
            item {
                MensajeEstadoRuralitos(
                    titulo = "Aún no hay seguimientos",
                    descripcion = "Agrega el primer plan de acción para esta familia.",
                    color = AzulClinico,
                    simbolo = "+"
                )
            }
        }

        items(seguimientos.size) { index ->
            val item = seguimientos[index]
            val color = colorCumplimiento(item.cumplimiento)
            TarjetaRegistroRuralitos(
                titulo = "Seguimiento ${item.numero ?: index + 1}",
                descripcion = "Análisis: ${item.fechaAnalisis}",
                simbolo = (item.numero ?: index + 1).toString(),
                color = color,
                onEditar = {
                    editando = item
                    mostrandoFormulario = true
                },
                onEliminar = { eliminar = item }
            ) {
                Text(
                    "Estado: ${nombreCumplimiento(item.cumplimiento)}",
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
                if (item.compromisoFamilia.isNotBlank()) {
                    Text(
                        "Familia: ${item.compromisoFamilia}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (item.fechaEvaluacion.isNotBlank()) {
                    Text(
                        "Evaluación: ${item.fechaEvaluacion}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }

        item {
            BotonPrincipalRuralitos(
                texto = "Guardar información de esta sección",
                descripcion = "Los planes y evaluaciones ya están guardados",
                color = MoradoClinico,
                onClick = onContinuar
            )
            BotonSecundarioRuralitos(
                texto = textoRegresar,
                descripcion = descripcionRegresar,
                onClick = onSalir,
                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun FormularioGestionRiesgoScreen(
    fichaId: Long,
    item: GestionRiesgoEntity?,
    numeroSugerido: Int,
    responsableActual: String,
    onGuardar: (GestionRiesgoEntity) -> Unit,
    onCancelar: () -> Unit
) {
    val hoy = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    var fechaAnalisis by remember { mutableStateOf(item?.fechaAnalisis ?: hoy) }
    var numero by remember { mutableStateOf((item?.numero ?: numeroSugerido).toString()) }
    var compromisoFamilia by remember { mutableStateOf(item?.compromisoFamilia.orEmpty()) }
    var compromisoEquipo by remember { mutableStateOf(item?.compromisoEquipoSalud.orEmpty()) }
    var fechaEvaluacion by remember { mutableStateOf(item?.fechaEvaluacion.orEmpty()) }
    var cumplimiento by remember { mutableStateOf(item?.cumplimiento ?: "PENDIENTE") }
    var observaciones by remember { mutableStateOf(item?.causasIncumplimientoObservaciones.orEmpty()) }
    val responsable = item?.responsable?.ifBlank { responsableActual } ?: responsableActual
    var calendario by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    PantallaRuralitos(
        titulo = if (item == null) "Agregar seguimiento" else "Editar seguimiento",
        descripcion = "Completa el plan en cuatro bloques: control, compromisos, evaluación y observaciones.",
        subtitulo = "Plan de acción familiar",
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = if (item == null) "Guardar nuevo seguimiento" else "Guardar cambios del seguimiento",
                descripcion = "Validar el plan y regresar al historial",
                color = CianRuralitos,
                onClick = {
                    if (compromisoFamilia.isBlank() && compromisoEquipo.isBlank()) {
                        error = "Registra al menos un compromiso de la familia o del equipo de salud."
                    } else {
                        onGuardar(
                            GestionRiesgoEntity(
                                id = item?.id ?: 0,
                                fichaId = fichaId,
                                fechaAnalisis = fechaAnalisis,
                                numero = numero.toIntOrNull(),
                                compromisoFamilia = compromisoFamilia.trim(),
                                compromisoEquipoSalud = compromisoEquipo.trim(),
                                fechaEvaluacion = fechaEvaluacion,
                                cumplimiento = cumplimiento,
                                causasIncumplimientoObservaciones = observaciones.trim(),
                                responsable = responsable.trim()
                            )
                        )
                    }
                }
            )
            BotonSecundarioRuralitos(
                texto = "Cancelar y regresar",
                descripcion = "No guardar los cambios de este seguimiento",
                onClick = onCancelar
            )
        }
    ) {
        SeccionFormularioRuralitos(
            titulo = "1. Control del seguimiento",
            descripcion = "Fecha de análisis, número consecutivo y responsable automático."
        ) {
            CampoFecha(fechaAnalisis, "Fecha de análisis") { calendario = "analisis" }
            OutlinedTextField(
                value = numero,
                onValueChange = { numero = it.filter(Char::isDigit) },
                label = { Text("Número de seguimiento") },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                value = responsable,
                onValueChange = {},
                readOnly = true,
                label = { Text("Responsable automático") },
                supportingText = { Text("Usuario que mantiene abierta la sesión") },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
        }
        SeccionFormularioRuralitos(
            titulo = "2. Compromisos acordados",
            descripcion = "Describe por separado lo que realizará la familia y lo que realizará el equipo de salud."
        ) {
            CampoLargoGestion(
                compromisoFamilia,
                { compromisoFamilia = it },
                "Compromiso de la familia"
            )
            CampoLargoGestion(
                compromisoEquipo,
                { compromisoEquipo = it },
                "Compromiso del equipo de salud"
            )
        }
        SeccionFormularioRuralitos(
            titulo = "3. Evaluación del cumplimiento",
            descripcion = "Selecciona la fecha y el estado actual del acuerdo."
        ) {
            CampoFecha(fechaEvaluacion, "Fecha de evaluación") { calendario = "evaluacion" }
            Text(
                "Resultado del seguimiento",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                listOf("PENDIENTE", "SI_CUMPLE", "NO_CUMPLE", "PARCIAL").forEach { opcion ->
                    val color = colorCumplimiento(opcion)
                    FilterChip(
                        selected = cumplimiento == opcion,
                        onClick = { cumplimiento = opcion },
                        label = {
                            Text(
                                nombreCumplimiento(opcion),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.heightIn(min = 50.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
        SeccionFormularioRuralitos(
            titulo = "4. Observaciones",
            descripcion = "Explica causas de incumplimiento, avances parciales o información relevante."
        ) {
            CampoLargoGestion(
                observaciones,
                { observaciones = it },
                "Causas de incumplimiento y observaciones"
            )
        }
        error?.let {
            MensajeEstadoRuralitos(
                titulo = "Revisa el seguimiento",
                descripcion = it,
                color = RojoClinico,
                simbolo = "!"
            )
        }
    }

    if (calendario != null) {
        SelectorFechaDialog(
            onFechaSeleccionada = { fecha ->
                if (calendario == "analisis") fechaAnalisis = fecha else fechaEvaluacion = fecha
                calendario = null
            },
            onCerrar = { calendario = null }
        )
    }
}

@Composable
private fun CampoLargoGestion(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        minLines = 3,
        maxLines = 6
    )
}

private fun nombreCumplimiento(valor: String): String = when (valor) {
    "PENDIENTE" -> "Pendiente"
    "SI_CUMPLE" -> "Sí cumple"
    "NO_CUMPLE" -> "No cumple"
    "PARCIAL" -> "Cumplimiento parcial"
    else -> valor.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

private fun colorCumplimiento(valor: String): Color = when (valor) {
    "SI_CUMPLE" -> CianRuralitos
    "NO_CUMPLE" -> RojoClinico
    "PARCIAL" -> NaranjaClinico
    "PENDIENTE" -> AzulClinico
    else -> MoradoClinico
}
