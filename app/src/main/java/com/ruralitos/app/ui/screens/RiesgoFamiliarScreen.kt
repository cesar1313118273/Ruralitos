package com.ruralitos.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.CalificacionRiesgoEntity
import com.ruralitos.app.domain.InstrumentoRiesgoFamiliar
import com.ruralitos.app.domain.RiesgoFamiliar
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.PantallaListaRuralitos
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
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeClinico
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RiesgoFamiliarScreen(
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
    val calificaciones by database.fichaContenidoDao()
        .listarCalificaciones(fichaId)
        .collectAsState(initial = emptyList())
    var mostrandoFormulario by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<CalificacionRiesgoEntity?>(null) }
    var eliminar by remember { mutableStateOf<CalificacionRiesgoEntity?>(null) }
    var valoresIniciales by remember { mutableStateOf(List(18) { -1 }) }

    if (mostrandoFormulario) {
        FormularioRiesgoScreen(
            fichaId = fichaId,
            calificacion = editando,
            valoresIniciales = valoresIniciales,
            responsableActual = responsableActual,
            onGuardar = { calificacion, valores ->
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            if (calificacion.id == 0L) {
                                database.fichaContenidoDao()
                                    .guardarCalificacionCompleta(calificacion, valores)
                            } else {
                                database.fichaContenidoDao()
                                    .actualizarCalificacionCompleta(calificacion, valores)
                            }
                        }
                    }.onSuccess {
                        mostrandoFormulario = false
                        editando = null
                    }.onFailure {
                        Toast.makeText(context, "No se pudo guardar la calificación.", Toast.LENGTH_SHORT).show()
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
            title = { Text("Eliminar calificación") },
            text = {
                Text("Se eliminará la evaluación del ${seleccionado.fechaCalificacion} y sus 18 respuestas.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        eliminar = null
                        scope.launch(Dispatchers.IO) {
                            database.fichaContenidoDao().eliminarCalificacion(seleccionado)
                        }
                    }
                ) { Text("Sí, eliminar", color = RojoClinico, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { eliminar = null }) { Text("Conservar evaluación") }
            }
        )
    }

    PantallaListaRuralitos(
        titulo = "Calificación del riesgo familiar",
        descripcion = "Selecciona descripciones comprensibles. Ruralitos calcula internamente el puntaje y el nivel de riesgo.",
        paso = 5,
        totalPasos = 10,
        etiquetaPaso = "Salud y evaluación",
        onVolver = onSalir,
        barraAccion = {
                BotonPrincipalRuralitos(
                    texto = "Guardar información de esta sección",
                    descripcion = "Las calificaciones ya están guardadas",
                    color = AzulClinico,
                    onClick = onContinuar
                )

        }
    ) {
        item {
            MensajeEstadoRuralitos(
                titulo = "${calificaciones.size} evaluación(es) guardada(s)",
                descripcion = "El historial se conserva y las cuatro evaluaciones más recientes se exportan a la ficha.",
                color = CianRuralitos,
                simbolo = calificaciones.size.toString()
            )
        }
        item {
            BotonPrincipalRuralitos(
                texto = "Crear nueva calificación",
                descripcion = "Responder los 18 componentes del instrumento familiar",
                color = CianRuralitos,
                onClick = {
                    editando = null
                    valoresIniciales = List(18) { -1 }
                    mostrandoFormulario = true
                }
            )
        }
        if (calificaciones.isEmpty()) {
            item {
                MensajeEstadoRuralitos(
                    titulo = "No hay calificaciones",
                    descripcion = "Crea la primera evaluación para determinar el nivel de riesgo de la familia.",
                    color = AzulClinico,
                    simbolo = "!"
                )
            }
        }
        items(calificaciones, key = { it.id }) { item ->
            val color = colorNivelRiesgo(item.nivel)
            TarjetaRegistroRuralitos(
                titulo = "Evaluación del ${item.fechaCalificacion}",
                descripcion = "Nivel: ${nombreNivelRiesgo(item.nivel)}",
                simbolo = nombreNivelRiesgo(item.nivel).take(1),
                color = color,
                onEditar = {
                    scope.launch {
                        val valores = withContext(Dispatchers.IO) {
                            database.fichaContenidoDao()
                                .obtenerValoresRiesgo(item.id)
                                .map { it.valor }
                        }
                        valoresIniciales = if (valores.size == 18) valores else List(18) { -1 }
                        editando = item
                        mostrandoFormulario = true
                    }
                },
                onEliminar = { eliminar = item }
            ) {
                Text(
                    "Puntaje calculado: ${item.total}",
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Responsable: ${item.responsable.ifBlank { "Sin registrar" }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun FormularioRiesgoScreen(
    fichaId: Long,
    calificacion: CalificacionRiesgoEntity?,
    valoresIniciales: List<Int>,
    responsableActual: String,
    onGuardar: (CalificacionRiesgoEntity, List<Int>) -> Unit,
    onCancelar: () -> Unit
) {
    val fechaHoy = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }
    var fecha by remember { mutableStateOf(calificacion?.fechaCalificacion ?: fechaHoy) }
    val responsable = responsableActual.ifBlank { calificacion?.responsable.orEmpty() }
    var mostrarCalendario by remember { mutableStateOf(false) }
    val valores = remember(calificacion?.id, valoresIniciales) {
        mutableStateListOf<Int>().apply { addAll(valoresIniciales) }
    }
    var indiceActivo by remember(calificacion?.id) { mutableIntStateOf(0) }
    val desplazamiento = rememberScrollState()
    LaunchedEffect(indiceActivo) { desplazamiento.animateScrollTo(0) }
    val seleccionadas = valores.indices.count { indice ->
        InstrumentoRiesgoFamiliar.valorValido(indice, valores[indice])
    }
    val seleccionCompleta = seleccionadas == valores.size
    val valoresCalculables = valores.mapIndexed { indice, valor ->
        if (InstrumentoRiesgoFamiliar.valorValido(indice, valor)) valor else 0
    }
    val resultado = RiesgoFamiliar.calcular(valoresCalculables)

    PantallaRuralitos(
        titulo = if (calificacion == null) "Nueva calificación" else "Editar calificación",
        descripcion = "Lee cada descripción y marca una sola opción. Los valores numéricos se procesan internamente.",
        subtitulo = "Instrumento de riesgo familiar",
        onVolver = onCancelar,
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = if (seleccionCompleta) "Guardar calificación completa" else "Faltan ${18 - seleccionadas} componentes",
                descripcion = if (seleccionCompleta) {
                    "Registrar resultado y regresar al historial de evaluaciones"
                } else {
                    "Revisa los bloques y selecciona una opción en cada componente"
                },
                color = CianRuralitos,
                enabled = seleccionCompleta,
                onClick = {
                    onGuardar(
                        CalificacionRiesgoEntity(
                            id = calificacion?.id ?: 0,
                            fichaId = fichaId,
                            fechaCalificacion = fecha,
                            responsable = responsable.trim(),
                            total = resultado.total,
                            nivel = resultado.nivel
                        ),
                        valores.toList()
                    )
                }
            )

        }
    ) {
        Text(
            text = "${indiceActivo + 1} de ${valores.size}",
            style = MaterialTheme.typography.labelLarge,
            color = AzulClinico
        )
        LinearProgressIndicator(
            progress = { (indiceActivo + 1).toFloat() / valores.size },
            modifier = Modifier.fillMaxWidth(),
            color = AzulClinico,
            trackColor = BordeClinico
        )
        PreguntaRiesgo(
            index = indiceActivo,
            categoria = when (indiceActivo) {
                in 0..5 -> "A. Riesgos biológicos"
                in 6..10 -> "B. Riesgos sanitarios"
                else -> "C. Riesgos socioeconómicos"
            },
            valores = valores
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonSecundarioRuralitos(
                texto = "Anterior",
                onClick = { indiceActivo = (indiceActivo - 1).coerceAtLeast(0) },
                enabled = indiceActivo > 0,
                modifier = Modifier.weight(1f)
            )
            BotonPrincipalRuralitos(
                texto = if (indiceActivo == valores.lastIndex) "Primera" else "Siguiente",
                onClick = {
                    indiceActivo = if (indiceActivo == valores.lastIndex) 0 else indiceActivo + 1
                },
                modifier = Modifier.weight(1f),
                color = AzulClinico
            )
        }
        SeccionFormularioRuralitos(
            titulo = "Datos de la evaluación",
            descripcion = "Fecha y responsable de esta calificación."
        ) {
            CampoFecha(fecha, "Fecha de calificación") { mostrarCalendario = true }
            OutlinedTextField(
                value = responsable,
                onValueChange = {},
                readOnly = true,
                label = { Text("Responsable de la cuenta activa") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true
            )
            MensajeEstadoRuralitos(
                titulo = "$seleccionadas de 18 componentes respondidos",
                descripcion = if (seleccionCompleta) {
                    "Resultado: ${nombreNivelRiesgo(resultado.nivel)} · Puntaje calculado: ${resultado.total}"
                } else {
                    "Completa todos los componentes para obtener el resultado final."
                },
                color = if (seleccionCompleta) colorNivelRiesgo(resultado.nivel) else AzulClinico,
                simbolo = if (seleccionCompleta) "✓" else seleccionadas.toString(),
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }

    if (mostrarCalendario) {
        SelectorFechaDialog(
            onFechaSeleccionada = {
                fecha = it
                mostrarCalendario = false
            },
            onCerrar = { mostrarCalendario = false }
        )
    }
}

@Composable
private fun PreguntaRiesgo(
    index: Int,
    categoria: String,
    valores: MutableList<Int>
) {
    val componente = InstrumentoRiesgoFamiliar.definiciones[index]
    SeccionFormularioRuralitos(titulo = categoria) {
        Text(
            "${index + 1}. ${componente.nombre}",
            style = MaterialTheme.typography.titleLarge,
            color = AzulClinicoOscuro
        )
        Text(
            "Selecciona la situación que mejor describe a la familia.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        componente.opciones.forEach { opcion ->
            val seleccionada = valores[index] == opcion.valor
            Surface(
                onClick = { valores[index] = opcion.valor },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (seleccionada) CianRuralitos.copy(alpha = 0.08f) else Color.White,
                border = BorderStroke(1.dp, if (seleccionada) CianRuralitos else BordeClinico)
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp)) {
                    RadioButton(selected = seleccionada, onClick = null)
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(
                            InstrumentoRiesgoFamiliar.etiqueta(opcion.valor),
                            color = AzulClinicoOscuro,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            opcion.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun nombreNivelRiesgo(nivel: String): String = when (nivel) {
    "SIN_RIESGO" -> "Sin riesgo"
    "BAJO" -> "Riesgo bajo"
    "MEDIO" -> "Riesgo medio"
    "ALTO" -> "Riesgo alto"
    else -> nivel.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

private fun colorNivelRiesgo(nivel: String): Color = when (nivel) {
    "SIN_RIESGO" -> CianRuralitos
    "BAJO" -> AzulClinico
    "MEDIO" -> NaranjaClinico
    "ALTO" -> RojoClinico
    else -> MoradoClinico
}
