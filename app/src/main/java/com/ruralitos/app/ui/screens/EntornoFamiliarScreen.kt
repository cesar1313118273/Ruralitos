package com.ruralitos.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.AdjuntoFichaEntity
import com.ruralitos.app.data.local.entity.ContaminacionAmbientalEntity
import com.ruralitos.app.data.local.entity.LugarTratamientoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EntornoFamiliarScreen(
    fichaId: Long,
    onFinalizar: () -> Unit,
    onSalir: () -> Unit
) {
    val context = LocalContext.current
    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val scope = rememberCoroutineScope()
    val contaminaciones by database.fichaContenidoDao()
        .listarContaminacion(fichaId)
        .collectAsState(initial = emptyList())
    val lugares by database.fichaContenidoDao()
        .listarLugaresTratamiento(fichaId)
        .collectAsState(initial = emptyList())
    val adjuntos by database.fichaContenidoDao()
        .listarAdjuntos(fichaId)
        .collectAsState(initial = emptyList())
    var mostrandoContaminacion by remember { mutableStateOf(false) }
    var contaminacionEditando by remember { mutableStateOf<ContaminacionAmbientalEntity?>(null) }
    var lugarTexto by remember { mutableStateOf("") }
    var lugarEditando by remember { mutableStateOf<LugarTratamientoEntity?>(null) }

    fun guardarAdjunto(tipo: String, uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                database.fichaContenidoDao().guardarAdjunto(
                    AdjuntoFichaEntity(
                        fichaId = fichaId,
                        tipo = tipo,
                        uri = uri.toString()
                    )
                )
            }
        }
    }

    val selectorFamiliograma = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { guardarAdjunto("FAMILIOGRAMA", it) } }
    val selectorCroquis = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { guardarAdjunto("CROQUIS", it) } }

    if (mostrandoContaminacion) {
        FormularioContaminacionScreen(
            fichaId = fichaId,
            item = contaminacionEditando,
            onGuardar = { item ->
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            if (item.id == 0L) database.fichaContenidoDao().guardarContaminacion(item)
                            else database.fichaContenidoDao().actualizarContaminacion(item)
                        }
                    }.onSuccess {
                        mostrandoContaminacion = false
                        contaminacionEditando = null
                    }.onFailure {
                        Toast.makeText(context, "No se pudo guardar el informe.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCancelar = { mostrandoContaminacion = false }
        )
        return
    }

    LazyColumn(Modifier.fillMaxSize().formularioSeguro().padding(24.dp)) {
        item {
            Text("Familiograma y entorno", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("Hoja 4 · Imágenes, contaminación y lugares de tratamiento.", modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

            Text("Familiograma", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(if (adjuntos.any { it.tipo == "FAMILIOGRAMA" }) "Imagen seleccionada" else "Sin imagen")
            Button(
                onClick = { selectorFamiliograma.launch(arrayOf("image/png", "image/jpeg")) },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            ) { Text("Seleccionar imagen del familiograma") }

            Text("Croquis de vivienda y contaminación", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
            Text(if (adjuntos.any { it.tipo == "CROQUIS" }) "Imagen seleccionada" else "Sin imagen")
            Button(
                onClick = { selectorCroquis.launch(arrayOf("image/png", "image/jpeg")) },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            ) { Text("Seleccionar imagen del croquis") }

            Text("Contaminación ambiental", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp))
            Button(
                onClick = {
                    contaminacionEditando = null
                    mostrandoContaminacion = true
                },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp)
            ) { Text("Agregar informe") }
        }

        items(contaminaciones, key = { "c${it.id}" }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(item.fechaInforme, fontWeight = FontWeight.SemiBold)
                    Text(item.tipoContaminanteDescripcion, maxLines = 2)
                    Text("Causante: ${item.causanteContaminacion.ifBlank { "Sin registrar" }}")
                    Row {
                        TextButton(onClick = {
                            contaminacionEditando = item
                            mostrandoContaminacion = true
                        }) { Text("Editar") }
                        TextButton(onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    database.fichaContenidoDao().eliminarContaminacion(item)
                                }
                            }
                        }) { Text("Eliminar") }
                    }
                }
            }
        }

        item {
            Text("Lugar o persona a la que acuden para tratamiento", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
            OutlinedTextField(
                value = lugarTexto,
                onValueChange = { lugarTexto = it },
                label = { Text("Lugar o persona") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Button(
                onClick = {
                    if (lugarTexto.isNotBlank()) {
                        val actual = lugarEditando
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                if (actual == null) {
                                    database.fichaContenidoDao().guardarLugarTratamiento(
                                        LugarTratamientoEntity(fichaId = fichaId, descripcion = lugarTexto.trim())
                                    )
                                } else {
                                    database.fichaContenidoDao().actualizarLugarTratamiento(
                                        actual.copy(descripcion = lugarTexto.trim())
                                    )
                                }
                            }
                            lugarTexto = ""
                            lugarEditando = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp)
            ) { Text(if (lugarEditando == null) "Agregar" else "Actualizar") }
        }

        items(lugares, key = { "l${it.id}" }) { item ->
            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(item.descripcion)
                    Row {
                        TextButton(onClick = {
                            lugarEditando = item
                            lugarTexto = item.descripcion
                        }) { Text("Editar") }
                        TextButton(onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    database.fichaContenidoDao().eliminarLugarTratamiento(item)
                                }
                            }
                        }) { Text("Eliminar") }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onFinalizar, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) { Text("Finalizar borrador") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onSalir, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) { Text("Continuar después") }
        }
    }
}

@Composable
fun FormularioContaminacionScreen(
    fichaId: Long,
    item: ContaminacionAmbientalEntity?,
    onGuardar: (ContaminacionAmbientalEntity) -> Unit,
    onCancelar: () -> Unit
) {
    val hoy = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    var fecha by remember { mutableStateOf(item?.fechaInforme ?: hoy) }
    var descripcion by remember { mutableStateOf(item?.tipoContaminanteDescripcion.orEmpty()) }
    var causante by remember { mutableStateOf(item?.causanteContaminacion.orEmpty()) }
    var mostrarCalendario by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    PantallaRuralitos(
        titulo = if (item == null) "Agregar contaminación" else "Editar contaminación",
        descripcion = "Completa por separado la fecha, el tipo de contaminación y su posible causante.",
        subtitulo = "Entorno familiar",
        onVolver = onCancelar,
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = if (item == null) "Guardar informe ambiental" else "Guardar cambios del informe",
                descripcion = "Validar y regresar al listado de contaminación",
                color = CianRuralitos,
                onClick = {
                    if (descripcion.isBlank()) {
                        error = "Describe el tipo de contaminación."
                    } else {
                        onGuardar(
                            ContaminacionAmbientalEntity(
                                id = item?.id ?: 0,
                                fichaId = fichaId,
                                fechaInforme = fecha,
                                tipoContaminanteDescripcion = descripcion.trim(),
                                causanteContaminacion = causante.trim()
                            )
                        )
                    }
                }
            )

        }
    ) {
        SeccionFormularioRuralitos(
            titulo = "1. Identificación",
            descripcion = "Fecha en la que se identificó la contaminación ambiental."
        ) {
            CampoFecha(fecha, "Fecha de identificación") {
                mostrarCalendario = true
            }
        }
        SeccionFormularioRuralitos(
            titulo = "2. Tipo y descripción",
            descripcion = "Explica qué contaminación existe y cómo afecta el entorno."
        ) {
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Tipo de contaminante y descripción") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 7
            )
        }
        SeccionFormularioRuralitos(
            titulo = "3. Posible causante",
            descripcion = "Persona, actividad, instalación o condición que origina la contaminación."
        ) {
            CampoTexto(
                causante,
                { causante = it },
                "Causante de la contaminación"
            )
        }
        error?.let {
            MensajeEstadoRuralitos(
                titulo = "Revisa el informe",
                descripcion = it,
                color = RojoClinico,
                simbolo = "!"
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
