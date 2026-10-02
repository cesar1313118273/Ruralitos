package com.ruralitos.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.NotaDiariaEntity
import com.ruralitos.app.data.agenda.RecordatorioNota
import com.ruralitos.app.domain.DispensarizacionAutomatica
import com.ruralitos.app.domain.CatalogoCie10
import com.ruralitos.app.domain.DiagnosticoCie10
import com.ruralitos.app.domain.GrupoEdadFamiliar
import com.ruralitos.app.domain.ReglasGrupoEdadFamiliar
import com.ruralitos.app.domain.ValidadorIdentidadEcuador
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private class SesionNota(val miembroId: Long) {
    var notaId: Long? = null
    val mutex = Mutex()
}

@Composable
fun MiembrosFamiliaScreen(
    fichaId: Long,
    usuarioId: Long,
    onContinuar: () -> Unit,
    onSalir: () -> Unit,
    textoRegresar: String = "Volver al panel de la ficha",
    descripcionRegresar: String = "Conservar los datos y salir de esta sección"
) {
    val context = LocalContext.current
    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val scope = rememberCoroutineScope()
    val permisoAvisosNotas = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val miembros by database.fichaContenidoDao()
        .listarMiembros(fichaId)
        .collectAsState(initial = emptyList())
    var mostrandoFormulario by remember { mutableStateOf(false) }
    var miembroEditando by remember { mutableStateOf<MiembroFamiliaEntity?>(null) }
    var miembroEliminar by remember { mutableStateOf<MiembroFamiliaEntity?>(null) }
    var miembroNota by remember { mutableStateOf<MiembroFamiliaEntity?>(null) }
    var textoNota by remember { mutableStateOf("") }
    var sesionNota by remember { mutableStateOf<SesionNota?>(null) }
    var errorNota by remember { mutableStateOf(false) }

    suspend fun guardarNota(contenido: String, sesion: SesionNota) {
        withContext(NonCancellable) {
            sesion.mutex.withLock {
                val idAnterior = sesion.notaId
                runCatching {
                    withContext(Dispatchers.IO) {
                        val id = sesion.notaId
                        if (contenido.isBlank()) {
                            if (id != null) database.notaDiariaDao().eliminar(id)
                            null
                        } else if (id == null) {
                            val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            database.notaDiariaDao().crear(
                                NotaDiariaEntity(
                                    miembroId = sesion.miembroId,
                                    fechaLocal = fecha,
                                    contenido = contenido.trim(),
                                    usuarioId = usuarioId
                                )
                            )
                        } else {
                            database.notaDiariaDao().actualizarContenido(id, contenido.trim())
                            id
                        }
                    }
                }.onSuccess {
                    sesion.notaId = it
                    if (it == null) {
                        idAnterior?.let { id -> RecordatorioNota.cancelar(context, id) }
                    } else if (idAnterior == null) {
                        database.notaDiariaDao().buscar(it)?.let { nota ->
                            RecordatorioNota.actualizar(context, nota)
                        }
                    }
                    if (sesionNota === sesion) errorNota = false
                }.onFailure {
                    if (sesionNota === sesion) errorNota = true
                }
            }
        }
    }

    LaunchedEffect(miembroNota?.id, textoNota) {
        val sesion = sesionNota ?: return@LaunchedEffect
        delay(450)
        guardarNota(textoNota, sesion)
    }

    LaunchedEffect(miembroNota?.id) {
        if (miembroNota != null && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            permisoAvisosNotas.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    miembroNota?.let { integrante ->
        AlertDialog(
            onDismissRequest = {
                val contenido = textoNota
                val sesion = sesionNota
                if (sesion != null) scope.launch { guardarNota(contenido, sesion) }
                miembroNota = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        val contenido = textoNota
                        val sesion = sesionNota
                        if (sesion != null) scope.launch { guardarNota(contenido, sesion) }
                        miembroNota = null
                    }) { Text("✕", color = RojoClinico) }
                    Text("Nota diaria", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column {
                    Text(integrante.apellidosNombres, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = textoNota,
                        onValueChange = { textoNota = it },
                        label = { Text("Nota importante") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Text(
                        if (errorNota) "No se pudo guardar. Comprueba el almacenamiento."
                        else "Se guarda automáticamente mientras escribes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (errorNota) RojoClinico else AzulClinico,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val contenido = textoNota
                    val sesion = sesionNota
                    if (sesion != null) scope.launch { guardarNota(contenido, sesion) }
                    miembroNota = null
                }) { Text("Guardar y cerrar") }
            }
        )
    }

    if (mostrandoFormulario) {
        FormularioMiembroScreen(
            fichaId = fichaId,
            miembro = miembroEditando,
            onGuardar = { miembro ->
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            if (miembro.id == 0L) {
                                database.fichaContenidoDao().guardarMiembro(miembro)
                            } else {
                                database.fichaContenidoDao().actualizarMiembro(miembro)
                            }
                        }
                    }.onSuccess {
                        mostrandoFormulario = false
                        miembroEditando = null
                    }.onFailure {
                        Toast.makeText(context, "No se pudo guardar el integrante.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCancelar = {
                mostrandoFormulario = false
                miembroEditando = null
            }
        )
        return
    }

    miembroEliminar?.let { seleccionado ->
        AlertDialog(
            onDismissRequest = { miembroEliminar = null },
            title = { Text("Eliminar integrante") },
            text = {
                Text("¿Deseas eliminar a ${seleccionado.apellidosNombres}? Esta acción quitará sus datos de la ficha.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        miembroEliminar = null
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                database.fichaContenidoDao().eliminarMiembro(seleccionado)
                            }
                        }
                    }
                ) { Text("Sí, eliminar", color = RojoClinico, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { miembroEliminar = null }) { Text("Conservar integrante") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .formularioSeguro()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            EncabezadoRuralitos(
                titulo = "Integrantes de la familia",
                descripcion = "Registra a cada persona del hogar. La edad determina automáticamente qué campos aplican en la ficha.",
                paso = "Información del hogar",
                color = MoradoClinico
            )
        }
        item {
            MensajeEstadoRuralitos(
                titulo = "${miembros.size} integrante(s) registrado(s)",
                descripcion = "La cédula también se utilizará como número de historia clínica.",
                color = AzulClinico,
                simbolo = miembros.size.toString()
            )
        }
        item {
            BotonPrincipalRuralitos(
                texto = "Agregar un integrante familiar",
                descripcion = "Abrir formulario de identificación, edad y seguimiento preventivo",
                color = CianRuralitos,
                onClick = {
                    miembroEditando = null
                    mostrandoFormulario = true
                }
            )
        }

        if (miembros.isEmpty()) {
            item {
                MensajeEstadoRuralitos(
                    titulo = "Aún no hay integrantes",
                    descripcion = "Usa el botón verde para registrar la primera persona de la familia.",
                    color = MoradoClinico,
                    simbolo = "+"
                )
            }
        }

        items(miembros, key = { it.id }) { miembro ->
            TarjetaRegistroRuralitos(
                titulo = miembro.apellidosNombres.ifBlank { "Integrante sin nombre" },
                descripcion = listOf(miembro.parentesco, miembro.grupoEdad)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                simbolo = miembro.apellidosNombres.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P",
                color = MoradoClinico,
                onEditar = {
                    miembroEditando = miembro
                    mostrandoFormulario = true
                },
                onEliminar = { miembroEliminar = miembro }
            ) {
                Text(
                    "Cédula e historia clínica: ${miembro.cedula.ifBlank { "Sin registrar" }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (miembro.ocupacion.isNotBlank()) {
                    Text(
                        "Ocupación: ${miembro.ocupacion}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    TextButton(onClick = {
                        sesionNota = SesionNota(miembro.id)
                        textoNota = ""
                        errorNota = false
                        miembroNota = miembro
                    }) {
                        Text("+", style = MaterialTheme.typography.headlineSmall, color = CianRuralitos)
                        Text(" Nota diaria", color = CianRuralitos)
                    }
                }
            }
        }

        item {
            BotonPrincipalRuralitos(
                texto = "Guardar información de esta sección",
                descripcion = "Los integrantes registrados ya están guardados",
                color = AzulClinico,
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
private fun FormularioMiembroScreen(
    fichaId: Long,
    miembro: MiembroFamiliaEntity?,
    onGuardar: (MiembroFamiliaEntity) -> Unit,
    onCancelar: () -> Unit
) {
    var nombres by remember { mutableStateOf(miembro?.apellidosNombres.orEmpty()) }
    var parentesco by remember { mutableStateOf(miembro?.parentesco.orEmpty().ifBlank { "HIJO/A" }) }
    var fechaNacimiento by remember { mutableStateOf(miembro?.fechaNacimiento.orEmpty()) }
    var ocupacion by remember { mutableStateOf(miembro?.ocupacion.orEmpty()) }
    var sexo by remember { mutableStateOf(miembro?.sexo.orEmpty().ifBlank { "H" }) }
    var escolaridad by remember { mutableStateOf(miembro?.escolaridad.orEmpty().ifBlank { "SIN" }) }
    var vacunas by remember { mutableStateOf(miembro?.vacunasCompletas ?: false) }
    var saludBucal by remember { mutableStateOf(miembro?.saludBucalAdecuada ?: false) }
    var estadoNutricional by remember { mutableStateOf(miembro?.estadoNutricional.orEmpty().ifBlank { DispensarizacionAutomatica.NUTRICION_SIN_ALTERACION }) }
    var hipertension by remember { mutableStateOf(miembro?.hipertensionArterial ?: false) }
    var diabetes by remember { mutableStateOf(miembro?.diabetesMellitus ?: false) }
    var tuberculosis by remember { mutableStateOf(miembro?.tuberculosis ?: false) }
    var saludMental by remember { mutableStateOf(miembro?.problemaSaludMental ?: false) }
    var consumo by remember { mutableStateOf(miembro?.consumoAlcoholDrogas ?: false) }
    var cuidadosPaliativos by remember { mutableStateOf(miembro?.cuidadosPaliativos ?: false) }
    var vih by remember { mutableStateOf(miembro?.vih ?: false) }
    var eventoSalud by remember { mutableStateOf(miembro?.eventoSalud ?: false) }
    var casoConfirmado by remember { mutableStateOf(miembro?.casoConfirmado ?: false) }
    var casoSospechosoUno by remember { mutableStateOf(miembro?.casoSospechosoUno ?: false) }
    var casoSospechosoDos by remember { mutableStateOf(miembro?.casoSospechosoDos ?: false) }
    var prestadorComunitario by remember { mutableStateOf(miembro?.prestadorComunitario ?: false) }
    var parteroAncestral by remember { mutableStateOf(miembro?.parteroAncestral ?: false) }
    var sabiduriaAncestral by remember { mutableStateOf(miembro?.sabiduriaAncestral ?: false) }
    var diagnosticos by remember { mutableStateOf(CatalogoCie10.decodificar(miembro?.comorbilidadesCie10Json ?: "[]")) }
    var consultaCie10 by remember { mutableStateOf("") }
    var catalogoCie10 by remember { mutableStateOf<List<DiagnosticoCie10>>(emptyList()) }
    val teniaDiscapacidad = miembro?.let {
        listOf(
            it.discapacidadVisual,
            it.discapacidadAuditiva,
            it.discapacidadLenguaje,
            it.discapacidadFisica,
            it.discapacidadIntelectual,
            it.discapacidadPsicosocial
        ).any { valor -> valor == true }
    } == true
    var porcentajeDiscapacidad by remember {
        mutableStateOf(miembro?.porcentajeDiscapacidad?.toString().orEmpty().takeIf { teniaDiscapacidad }.orEmpty())
    }
    var necesitaAyudaTecnica by remember {
        mutableStateOf((miembro?.necesitaAyudaTecnica ?: false) && teniaDiscapacidad)
    }
    var tipoDiscapacidad by remember {
        mutableStateOf(
            when {
                miembro?.discapacidadVisual == true -> "VISUAL"
                miembro?.discapacidadAuditiva == true -> "AUDITIVA"
                miembro?.discapacidadLenguaje == true -> "LENGUAJE"
                miembro?.discapacidadFisica == true -> "FISICA"
                miembro?.discapacidadIntelectual == true -> "INTELECTUAL"
                miembro?.discapacidadPsicosocial == true -> "PSICOSOCIAL"
                else -> "NINGUNA"
            }
        )
    }
    var riesgoPrioritario by remember {
        mutableStateOf(
            when {
                miembro?.riesgoGenetico == true -> "GENETICO"
                miembro?.victimaViolencia == true -> "VIOLENCIA"
                miembro?.privadoLibertad == true -> "PRIVADO_LIBERTAD"
                else -> "NINGUNO"
            }
        )
    }
    var cedula by remember { mutableStateOf(miembro?.cedula.orEmpty()) }
    var mostrarCalendario by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val grupoEdad = GrupoEdadFamiliar.calcular(fechaNacimiento)
    val camposPermitidos = ReglasGrupoEdadFamiliar.camposPermitidos(grupoEdad)
    val context = LocalContext.current
    LaunchedEffect(Unit) { catalogoCie10 = withContext(Dispatchers.IO) { CatalogoCie10.cargar(context) } }
    var resultadosCie10 by remember { mutableStateOf<List<DiagnosticoCie10>>(emptyList()) }
    LaunchedEffect(consultaCie10, catalogoCie10, diagnosticos) {
        resultadosCie10 = withContext(Dispatchers.Default) {
            CatalogoCie10.buscar(catalogoCie10, consultaCie10)
                .filterNot { candidato -> diagnosticos.any { it.codigo == candidato.codigo } }
                .take(8)
        }
    }
    LaunchedEffect(grupoEdad) {
        val permitidas = ReglasGrupoEdadFamiliar.camposPermitidos(grupoEdad).escolaridades
        if (permitidas.isNotEmpty() && escolaridad !in permitidas) {
            escolaridad = permitidas.first()
        }
    }

    PantallaRuralitos(
        titulo = if (miembro == null) "Agregar integrante" else "Editar integrante",
        descripcion = "Completa los datos por bloques. Los campos que no corresponden a la edad se bloquearán automáticamente.",
        subtitulo = "Integrantes de la familia",
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = if (miembro == null) "Guardar nuevo integrante" else "Guardar cambios del integrante",
                descripcion = "Validar datos y regresar al listado familiar",
                color = CianRuralitos,
                onClick = {
                    if (nombres.isBlank() || parentesco.isBlank() || grupoEdad == null || sexo.isBlank()) {
                        error = "Completa apellidos y nombres, parentesco, fecha de nacimiento y sexo."
                    } else if (cedula.isNotBlank() && !ValidadorIdentidadEcuador.esDocumentoFamiliarAceptable(cedula)) {
                        error = "La identificación, si se registra, debe contener exactamente 10 o 13 números."
                    } else if (
                        tipoDiscapacidad != "NINGUNA" &&
                        porcentajeDiscapacidad.toIntOrNull() !in 1..100
                    ) {
                        error = "Registra un porcentaje de discapacidad entre 1 y 100."
                    } else if (consultaCie10.isNotBlank()) {
                        error = "Selecciona un diagnóstico de la lista CIE-10 o borra la búsqueda pendiente."
                    } else if (diagnosticos.any { seleccionado ->
                            catalogoCie10.none {
                                it.codigo == seleccionado.codigo &&
                                    it.descripcion == seleccionado.descripcion
                            }
                        }) {
                        error = "Hay diagnósticos que no pertenecen al catálogo CIE-10. Elimínalos y selecciónalos nuevamente."
                    } else {
                        onGuardar(
                            MiembroFamiliaEntity(
                                id = miembro?.id ?: 0,
                                fichaId = fichaId,
                                grupoEdad = grupoEdad,
                                apellidosNombres = nombres.trim(),
                                parentesco = parentesco.trim(),
                                fechaNacimiento = fechaNacimiento,
                                ocupacion = if (camposPermitidos.ocupacion) ocupacion.trim() else "",
                                sexo = sexo,
                                escolaridad = escolaridad
                                    .takeIf { it in camposPermitidos.escolaridades }
                                    .orEmpty(),
                                vacunasCompletas = vacunas,
                                saludBucalAdecuada = saludBucal,
                                riesgoEnfermedadDiscapacidad = diagnosticos.joinToString("; ") { it.etiqueta },
                                estadoNutricional = estadoNutricional,
                                hipertensionArterial = hipertension,
                                diabetesMellitus = diabetes,
                                tuberculosis = tuberculosis,
                                problemaSaludMental = saludMental,
                                consumoAlcoholDrogas = consumo,
                                enfermedadCronica = diagnosticos.isNotEmpty(),
                                discapacidadVisual = tipoDiscapacidad == "VISUAL",
                                discapacidadAuditiva = tipoDiscapacidad == "AUDITIVA",
                                discapacidadLenguaje = tipoDiscapacidad == "LENGUAJE",
                                discapacidadFisica = tipoDiscapacidad == "FISICA",
                                discapacidadIntelectual = tipoDiscapacidad == "INTELECTUAL",
                                discapacidadPsicosocial = tipoDiscapacidad == "PSICOSOCIAL",
                                cuidadosPaliativos = cuidadosPaliativos,
                                vih = vih,
                                eventoSalud = eventoSalud,
                                casoConfirmado = casoConfirmado,
                                casoSospechosoUno = casoSospechosoUno,
                                casoSospechosoDos = casoSospechosoDos,
                                prestadorComunitario = prestadorComunitario,
                                parteroAncestral = parteroAncestral,
                                sabiduriaAncestral = sabiduriaAncestral,
                                comorbilidadesCie10Json = CatalogoCie10.codificar(diagnosticos),
                                porcentajeDiscapacidad = porcentajeDiscapacidad.toIntOrNull()
                                    ?.coerceIn(0, 100)
                                    ?.takeIf { tipoDiscapacidad != "NINGUNA" },
                                necesitaAyudaTecnica = necesitaAyudaTecnica && tipoDiscapacidad != "NINGUNA",
                                enfermedadCronicaDescompensada = diagnosticos.any { it.descompensada },
                                riesgoGenetico = riesgoPrioritario == "GENETICO",
                                victimaViolencia = riesgoPrioritario == "VIOLENCIA",
                                privadoLibertad = riesgoPrioritario == "PRIVADO_LIBERTAD",
                                numeroHistoriaClinica = cedula.trim(),
                                cedula = cedula.trim(),
                                syncId = miembro?.syncId ?: UUID.randomUUID().toString()
                            )
                        )
                    }
                }
            )
            BotonSecundarioRuralitos(
                texto = "Cancelar y regresar",
                descripcion = "No guardar los cambios de este formulario",
                onClick = onCancelar
            )
        }
    ) {
        SeccionFormularioRuralitos(
            titulo = "1. Identificación personal",
            descripcion = "Apellidos y nombres, parentesco, cédula y fecha de nacimiento."
        ) {
            CampoTexto(nombres, { nombres = it }, "Apellidos y nombres")
            SeleccionTextoMiembro(
                "Parentesco con el jefe de familia",
                listOf("JEFE/A DE FAMILIA", "CÓNYUGE/PAREJA", "HIJO/A", "PADRE/MADRE", "ABUELO/A", "NIETO/A", "HERMANO/A", "OTRO FAMILIAR", "NO FAMILIAR").map { it to it },
                parentesco,
                MoradoClinico
            ) { parentesco = it }
            OutlinedTextField(
                value = cedula,
                onValueChange = { cedula = it.filter(Char::isDigit).take(13) },
                label = { Text("Número de cédula o identificación") },
                supportingText = { Text("Se copiará automáticamente como historia clínica") },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            CampoFecha(fechaNacimiento, "Fecha de nacimiento") { mostrarCalendario = true }
            MensajeEstadoRuralitos(
                titulo = "Grupo de edad",
                descripcion = grupoEdad ?: "Selecciona una fecha válida para calcularlo",
                color = if (grupoEdad == null) AzulClinico else CianRuralitos,
                simbolo = "E",
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        SeccionFormularioRuralitos(
            titulo = "2. Características personales",
            descripcion = "Sexo, escolaridad y ocupación según el grupo de edad."
        ) {
            SeleccionTextoMiembro("Sexo", listOf("H" to "Hombre", "M" to "Mujer"), sexo, MoradoClinico) { sexo = it }
            if (camposPermitidos.escolaridades.isNotEmpty()) {
                SeleccionTextoMiembro(
                    "Escolaridad",
                    listOf("SIN" to "Sin escolaridad", "BAS" to "Básica", "BACH" to "Bachillerato", "SUP" to "Superior", "ESP" to "Especialidad")
                        .filter { it.first in camposPermitidos.escolaridades },
                    escolaridad,
                    AzulClinico
                ) { escolaridad = it }
            } else {
                MensajeEstadoRuralitos(
                    titulo = "Escolaridad bloqueada",
                    descripcion = "No aplica para este grupo de edad; corresponde a celdas grises en la ficha.",
                    color = AzulClinico,
                    simbolo = "—",
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            if (camposPermitidos.ocupacion) {
                CampoTexto(ocupacion, { ocupacion = it }, "Ocupación")
            } else {
                MensajeEstadoRuralitos(
                    titulo = "Ocupación bloqueada",
                    descripcion = "No aplica para este grupo de edad; no se exportará ningún valor.",
                    color = AzulClinico,
                    simbolo = "—",
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
        SeccionFormularioRuralitos(
            titulo = "3. Seguimiento preventivo",
            descripcion = "Vacunas, nutrición, salud bucal y consumo. Estos datos alimentan la dispensarización automática."
        ) {
            SeleccionBooleanMiembro(
                "Esquema completo de vacunas",
                vacunas
            ) { vacunas = it }
            SeleccionBooleanMiembro(
                "Salud bucal adecuada",
                saludBucal
            ) { saludBucal = it }
            SeleccionOpcionMiembro(
                "Estado nutricional evaluado",
                DispensarizacionAutomatica.opcionesNutricion,
                estadoNutricional,
                NaranjaClinico
            ) { estadoNutricional = it }
            SeleccionBooleanMiembro(
                "Consumo de alcohol u otras drogas",
                consumo
            ) { consumo = it }
            OutlinedTextField(
                value = consultaCie10,
                onValueChange = { consultaCie10 = it },
                label = { Text("Otros riesgos, enfermedad o discapacidad · CIE-10") },
                supportingText = {
                    Text(
                        if (diagnosticos.isEmpty()) "Busca por código o descripción y selecciona un resultado; no se guarda texto libre"
                        else "${diagnosticos.size} diagnóstico(s) seleccionado(s); el texto sin seleccionar no se guarda"
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                singleLine = true
            )
            resultadosCie10.forEach { resultado ->
                TextButton(onClick = {
                    diagnosticos = diagnosticos + resultado
                    when {
                        resultado.codigo.uppercase().matches(Regex("I1[0-5].*")) -> hipertension = true
                        resultado.codigo.uppercase().matches(Regex("E1[0-4].*")) -> diabetes = true
                        resultado.codigo.uppercase().matches(Regex("A1[5-9].*")) -> tuberculosis = true
                    }
                    consultaCie10 = ""
                }, modifier = Modifier.fillMaxWidth()) { Text("+ ${resultado.etiqueta}") }
            }
            diagnosticos.forEach { diagnostico ->
                FilterChip(
                    selected = true,
                    onClick = { diagnosticos = diagnosticos.filterNot { it.codigo == diagnostico.codigo } },
                    label = { Text("${diagnostico.codigo} · ${diagnostico.descripcion}  ×") },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
            if (diagnosticos.isNotEmpty()) {
                Text(
                    "¿Qué enfermedad crónica está descompensada?",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                )
                diagnosticos.forEach { diagnostico ->
                    OpcionDescompensacionDiagnostico(
                        diagnostico = diagnostico,
                        onCambio = { valor ->
                            diagnosticos = diagnosticos.map {
                                if (it.codigo == diagnostico.codigo) it.copy(descompensada = valor) else it
                            }
                        }
                    )
                }
            }
        }
        SeccionFormularioRuralitos(
            titulo = "4. Estrategias Nacionales",
            descripcion = "¿Esta persona pertenece a uno o varios de los siguientes grupos?"
        ) {
            SelectorMultipleMiembro(
                titulo = "Grupos de Estrategias Nacionales",
                opciones = listOf(
                    "DESNUTRICION" to "Desnutrición crónica",
                    "SALUD_MENTAL" to "Salud mental",
                    "PALIATIVOS" to "Cuidados paliativos",
                    "TUBERCULOSIS" to "Tuberculosis",
                    "DIABETES" to "Diabetes",
                    "HIPERTENSION" to "Hipertensión",
                    "VIH" to "VIH"
                ),
                seleccion = buildSet {
                    if (estadoNutricional == DispensarizacionAutomatica.NUTRICION_DESNUTRICION_CRONICA) add("DESNUTRICION")
                    if (saludMental) add("SALUD_MENTAL")
                    if (cuidadosPaliativos) add("PALIATIVOS")
                    if (tuberculosis) add("TUBERCULOSIS")
                    if (diabetes) add("DIABETES")
                    if (hipertension) add("HIPERTENSION")
                    if (vih) add("VIH")
                },
                onSeleccion = { seleccion ->
                    estadoNutricional = if ("DESNUTRICION" in seleccion) {
                        DispensarizacionAutomatica.NUTRICION_DESNUTRICION_CRONICA
                    } else if (estadoNutricional == DispensarizacionAutomatica.NUTRICION_DESNUTRICION_CRONICA) {
                        DispensarizacionAutomatica.NUTRICION_SIN_ALTERACION
                    } else estadoNutricional
                    saludMental = "SALUD_MENTAL" in seleccion
                    cuidadosPaliativos = "PALIATIVOS" in seleccion
                    tuberculosis = "TUBERCULOSIS" in seleccion
                    diabetes = "DIABETES" in seleccion
                    hipertension = "HIPERTENSION" in seleccion
                    vih = "VIH" in seleccion
                }
            )
            Text(
                "Hipertensión, diabetes y tuberculosis se marcan automáticamente al seleccionar un CIE‑10 relacionado; puedes cambiarlas aquí.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        SeccionFormularioRuralitos(
            titulo = "5. Discapacidad",
            descripcion = "Selecciona un tipo. Los campos relacionados se habilitan solo cuando corresponda."
        ) {
            SeleccionOpcionMiembro(
                titulo = "Tipo de discapacidad",
                opciones = listOf(
                    "NINGUNA" to "Ninguna",
                    "VISUAL" to "Visual",
                    "AUDITIVA" to "Auditiva",
                    "LENGUAJE" to "Del lenguaje",
                    "FISICA" to "Física",
                    "INTELECTUAL" to "Intelectual",
                    "PSICOSOCIAL" to "Psicosocial"
                ),
                seleccion = tipoDiscapacidad,
                color = MoradoClinico
            ) { seleccion ->
                tipoDiscapacidad = seleccion
                if (seleccion == "NINGUNA") {
                    porcentajeDiscapacidad = ""
                    necesitaAyudaTecnica = false
                }
            }
            OutlinedTextField(
                value = porcentajeDiscapacidad,
                onValueChange = { porcentajeDiscapacidad = it.filter(Char::isDigit).take(3) },
                label = { Text("Porcentaje de discapacidad") },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = tipoDiscapacidad != "NINGUNA"
            )
            SeleccionBooleanMiembro(
                titulo = "Necesita ayuda técnica",
                seleccion = necesitaAyudaTecnica,
                enabled = tipoDiscapacidad != "NINGUNA"
            ) { necesitaAyudaTecnica = it }
        }
        SeccionFormularioRuralitos(
            titulo = "6. Alertas Epidemiológicas",
            descripcion = "Registra eventos y casos epidemiológicos."
        ) {
            SelectorMultipleMiembro(
                titulo = "Grupos de alerta",
                opciones = listOf(
                    "EVENTO" to "Evento de salud",
                    "SOSPECHOSO" to "Caso sospechoso",
                    "CONFIRMADO" to "Caso confirmado"
                ),
                seleccion = buildSet {
                    if (eventoSalud) add("EVENTO")
                    if (casoSospechosoUno || casoSospechosoDos) add("SOSPECHOSO")
                    if (casoConfirmado) add("CONFIRMADO")
                },
                onSeleccion = { seleccion ->
                    eventoSalud = "EVENTO" in seleccion
                    casoSospechosoUno = "SOSPECHOSO" in seleccion
                    if (!casoSospechosoUno) casoSospechosoDos = false
                    casoConfirmado = "CONFIRMADO" in seleccion
                }
            )
        }
        SeccionFormularioRuralitos(
            titulo = "7. Actores comunitarios",
            descripcion = "Selecciona únicamente las funciones comunitarias que correspondan."
        ) {
            SelectorMultipleMiembro(
                titulo = "Actores comunitarios",
                opciones = listOf(
                    "PRESTADOR" to "Prestador comunitario de salud",
                    "PARTERO" to "Partero/a ancestral tradicional",
                    "SABIDURIA" to "Hombre/mujer de sabiduría ancestral"
                ),
                seleccion = buildSet {
                    if (prestadorComunitario) add("PRESTADOR")
                    if (parteroAncestral) add("PARTERO")
                    if (sabiduriaAncestral) add("SABIDURIA")
                },
                onSeleccion = { seleccion ->
                    prestadorComunitario = "PRESTADOR" in seleccion
                    parteroAncestral = "PARTERO" in seleccion
                    sabiduriaAncestral = "SABIDURIA" in seleccion
                }
            )
        }
        SeccionFormularioRuralitos(
            titulo = "8. Otros riesgos prioritarios",
            descripcion = "Selecciona la condición registrada o Ninguno."
        ) {
            SeleccionOpcionMiembro(
                titulo = "Riesgo prioritario",
                opciones = listOf(
                    "NINGUNO" to "Ninguno",
                    "GENETICO" to "Riesgo genético",
                    "VIOLENCIA" to "Víctima de violencia",
                    "PRIVADO_LIBERTAD" to "Persona privada de la libertad"
                ),
                seleccion = riesgoPrioritario,
                color = NaranjaClinico
            ) { riesgoPrioritario = it }
        }
        error?.let {
            MensajeEstadoRuralitos(
                titulo = "Revisa la información",
                descripcion = it,
                color = RojoClinico,
                simbolo = "!"
            )
        }
    }

    if (mostrarCalendario) {
        SelectorFechaDialog(
            onFechaSeleccionada = {
                fechaNacimiento = it
                mostrarCalendario = false
            },
            onCerrar = { mostrarCalendario = false }
        )
    }
}

@Composable
private fun SeleccionTextoMiembro(
    titulo: String,
    opciones: List<Pair<String, String>>,
    seleccion: String,
    color: Color,
    onSeleccion: (String) -> Unit
) {
    SelectorDesplegableMiembro(
        titulo = titulo,
        opciones = opciones,
        seleccion = seleccion,
        color = color,
        onSeleccion = onSeleccion
    )
}

@Composable
private fun SeleccionOpcionMiembro(
    titulo: String,
    opciones: List<Pair<String, String>>,
    seleccion: String,
    color: Color,
    onSeleccion: (String) -> Unit
) {
    SelectorDesplegableMiembro(
        titulo = titulo,
        opciones = opciones,
        seleccion = seleccion,
        color = color,
        onSeleccion = onSeleccion
    )
}

@Composable
private fun SeleccionBooleanMiembro(
    titulo: String,
    seleccion: Boolean,
    enabled: Boolean = true,
    onSeleccion: (Boolean) -> Unit
) {
    SelectorDesplegableMiembro(
        titulo = titulo,
        opciones = listOf("SI" to "Sí", "NO" to "No"),
        seleccion = if (seleccion) "SI" else "NO",
        color = if (seleccion) CianRuralitos else RojoClinico,
        enabled = enabled,
        onSeleccion = { onSeleccion(it == "SI") }
    )
}

@Composable
private fun SelectorDesplegableMiembro(
    titulo: String,
    opciones: List<Pair<String, String>>,
    seleccion: String,
    color: Color,
    enabled: Boolean = true,
    onSeleccion: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    Text(
        titulo,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 14.dp, bottom = 5.dp)
    )
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expandido = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text(
                opciones.firstOrNull { it.first == seleccion }?.second ?: "Seleccionar",
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        DropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            opciones.forEach { (valor, etiqueta) ->
                DropdownMenuItem(
                    text = { Text("${if (valor == seleccion) "✓ " else ""}$etiqueta") },
                    onClick = {
                        onSeleccion(valor)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
private fun OpcionDescompensacionDiagnostico(
    diagnostico: DiagnosticoCie10,
    onCambio: (Boolean) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(diagnostico.etiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = diagnostico.descompensada,
                onClick = { onCambio(true) },
                label = { Text("Sí") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RojoClinico,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = !diagnostico.descompensada,
                onClick = { onCambio(false) },
                label = { Text("No") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CianRuralitos,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun SelectorMultipleMiembro(
    titulo: String,
    opciones: List<Pair<String, String>>,
    seleccion: Set<String>,
    onSeleccion: (Set<String>) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    Text(titulo, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 5.dp))
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expandido = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text(
                when (seleccion.size) {
                    0 -> "Ninguno"
                    1 -> opciones.firstOrNull { it.first in seleccion }?.second.orEmpty()
                    else -> "${seleccion.size} grupos seleccionados"
                },
                fontWeight = FontWeight.Bold
            )
        }
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            DropdownMenuItem(
                text = { Text("${if (seleccion.isEmpty()) "✓ " else ""}Ninguno") },
                onClick = { onSeleccion(emptySet()); expandido = false }
            )
            opciones.forEach { (valor, etiqueta) ->
                DropdownMenuItem(
                    text = { Text("${if (valor in seleccion) "✓ " else ""}$etiqueta") },
                    onClick = {
                        onSeleccion(if (valor in seleccion) seleccion - valor else seleccion + valor)
                    }
                )
            }
        }
    }
}
