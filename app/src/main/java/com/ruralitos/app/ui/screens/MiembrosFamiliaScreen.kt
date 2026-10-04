package com.ruralitos.app.ui.screens

import androidx.compose.runtime.mutableStateListOf
import com.ruralitos.app.ui.components.ItemMenuRuralitos
import com.ruralitos.app.ui.components.MenuDesplegableRuralitos
import com.ruralitos.app.ui.components.BotonSelectorRuralitos
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
import androidx.compose.foundation.Image
import com.ruralitos.app.ui.theme.VerdeSalud
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.testTag
import com.ruralitos.app.domain.EstrategiasDesdeCie10
import com.ruralitos.app.domain.FactoresRiesgoEdad
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.ruralitos.app.R
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
    descripcionRegresar: String = "Conservar los datos y salir de esta sección",
    mostrarAvance: Boolean = false,
    alCrearFicha: (suspend (MiembroFamiliaEntity, String) -> Boolean)? = null
) {
    val context = LocalContext.current
    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val scope = rememberCoroutineScope()
    val permisoAvisosNotas = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val miembros by database.fichaContenidoDao()
        .listarMiembros(fichaId)
        .collectAsState(initial = emptyList())
    val fichaActual by database.fichaFamiliarDao()
        .observarPorId(fichaId)
        .collectAsState(initial = null)
    var mostrandoFormulario by remember { mutableStateOf(false) }
    var miembroEditando by remember { mutableStateOf<MiembroFamiliaEntity?>(null) }
    var miembroEliminar by remember { mutableStateOf<MiembroFamiliaEntity?>(null) }
    var miembroNota by remember { mutableStateOf<MiembroFamiliaEntity?>(null) }
    var textoNota by remember { mutableStateOf("") }
    var sesionNota by remember { mutableStateOf<SesionNota?>(null) }
    var errorNota by remember { mutableStateOf(false) }
    var eligiendoActor by remember { mutableStateOf(false) }
    var actorEditando by remember { mutableStateOf<MiembroFamiliaEntity?>(null) }

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
                    Text(integrante.apellidosNombres, fontWeight = FontWeight.SemiBold)
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
            telefonoJefeInicial = fichaActual?.numeroTelefono.orEmpty(),
            sugerirJefe = miembros.none { it.parentesco == "JEFE/A DE FAMILIA" },
            mostrarAvance = mostrarAvance,
            onGuardar = { miembro, telefonoJefe ->
              if (fichaId == 0L && alCrearFicha != null) {
                // Borrador: nada se guarda hasta registrar al jefe o jefa de familia.
                if (miembro.parentesco != "JEFE/A DE FAMILIA") {
                    Toast.makeText(
                        context,
                        "Primero registra al jefe o jefa de familia. Hasta entonces no se guarda nada.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    scope.launch {
                        val creada = runCatching { alCrearFicha(miembro, telefonoJefe) }.getOrDefault(false)
                        if (creada) {
                            mostrandoFormulario = false
                            miembroEditando = null
                        } else {
                            Toast.makeText(context, "No se pudo guardar la ficha.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
              } else
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            if (miembro.id == 0L) {
                                database.fichaContenidoDao().guardarMiembro(miembro)
                            } else {
                                database.fichaContenidoDao().actualizarMiembro(miembro)
                            }
                            // El jefe o jefa de familia define la cédula, el nombre y el
                            // teléfono con los que se identifica la ficha.
                            if (miembro.parentesco == "JEFE/A DE FAMILIA") {
                                val ficha = database.fichaFamiliarDao().buscarPorId(fichaId)
                                if (ficha != null) {
                                    database.fichaFamiliarDao().actualizarDatosPrincipales(
                                        fichaId = fichaId,
                                        cedula = miembro.cedula.trim(),
                                        nombre = miembro.apellidosNombres.trim(),
                                        telefono = telefonoJefe,
                                        numeroFicha = ficha.numeroFichaFamiliar,
                                        fecha = ficha.fechaLlenado,
                                        carpeta = ficha.numeroCarpeta,
                                        responsableNombre = ficha.responsableNombre,
                                        responsableCodigo = ficha.responsableCodigo,
                                        usuarioId = usuarioId.takeIf { it != 0L }
                                    )
                                    database.fichaFamiliarDao().marcarPendiente(fichaId)
                                }
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

    if (eligiendoActor) {
        AlertDialog(
            onDismissRequest = { eligiendoActor = false },
            title = { Text("Actores comunitarios") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).testTag("lista_actores")) {
                    Text(
                        "Elige a la persona que sea prestador comunitario de salud, partero/a ancestral o de sabiduría ancestral.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    miembros.forEach { persona ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { actorEditando = persona; eligiendoActor = false }
                                .padding(vertical = 8.dp)
                                .testTag("actor_persona_${persona.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(persona.apellidosNombres.ifBlank { "Integrante sin nombre" }, modifier = Modifier.weight(1f))
                            IconosActores(persona)
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { eligiendoActor = false }) { Text("Cerrar") } }
        )
    }

    actorEditando?.let { persona ->
        var prestador by remember(persona.id) { mutableStateOf(persona.prestadorComunitario == true) }
        var partero by remember(persona.id) { mutableStateOf(persona.parteroAncestral == true) }
        var sabiduria by remember(persona.id) { mutableStateOf(persona.sabiduriaAncestral == true) }
        AlertDialog(
            onDismissRequest = { actorEditando = null },
            title = { Text(persona.apellidosNombres.ifBlank { "Integrante sin nombre" }) },
            text = {
                Column {
                    FilaActor("Prestador comunitario de salud", R.drawable.prestador_comunitario, prestador, "actor_prestador") { prestador = it }
                    FilaActor("Partero/a ancestral tradicional", R.drawable.partero_ancestral, partero, "actor_partero") { partero = it }
                    FilaActor("Hombre o mujer de sabiduría ancestral", R.drawable.sabiduria_ancestral, sabiduria, "actor_sabiduria") { sabiduria = it }
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("guardar_actores"),
                    onClick = {
                        val cambiada = persona.copy(
                            prestadorComunitario = prestador, parteroAncestral = partero, sabiduriaAncestral = sabiduria
                        )
                        actorEditando = null
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) { database.fichaContenidoDao().actualizarMiembro(cambiada) } }
                                .onFailure { Toast.makeText(context, "No se pudo guardar.", Toast.LENGTH_SHORT).show() }
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { actorEditando = null }) { Text("Cancelar") } }
        )
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
                ) { Text("Sí, eliminar", color = RojoClinico, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { miembroEliminar = null }) { Text("Conservar integrante") }
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
    PantallaListaRuralitos(
        titulo = "Integrantes de la familia",
        descripcion = "Registra a cada persona del hogar. La edad determina automáticamente qué campos aplican en la ficha.",
        paso = 2,
        totalPasos = 9,
        etiquetaPaso = "Información del hogar",
        onVolver = onSalir,
        barraAccion = {
                BotonPrincipalRuralitos(
                    texto = "Guardar información de esta sección",
                    descripcion = "Los integrantes registrados ya están guardados",
                    color = AzulClinico,
                    onClick = {
                    if (fichaId == 0L && alCrearFicha != null) {
                        Toast.makeText(
                            context,
                            "Registra primero al jefe o jefa de familia para guardar la ficha.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        onContinuar()
                    }
                }
                )

        }
    ) {
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
                    descripcion = "Empieza registrando al jefe o jefa de familia: su cédula y su teléfono identifican la ficha.",
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
                IconosActores(miembro, Modifier.padding(top = 6.dp))
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
    }
    if (miembros.isNotEmpty() && !(fichaId == 0L && alCrearFicha != null)) {
        ExtendedFloatingActionButton(
            onClick = { eligiendoActor = true },
            containerColor = VerdeSalud,
            contentColor = Color.White,
            icon = {
                Image(
                    painter = painterResource(R.drawable.prestador_comunitario),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    contentScale = ContentScale.Fit
                )
            },
            text = { Text("Actores comunitarios") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 104.dp)
                .testTag("boton_actores_comunitarios")
        )
    }
    }
}

/** Iconos de los roles comunitarios que tiene una persona. */
@Composable
private fun IconosActores(persona: MiembroFamiliaEntity, modifier: Modifier = Modifier) {
    val iconos = buildList {
        if (persona.prestadorComunitario == true) add(R.drawable.prestador_comunitario to "Prestador comunitario de salud")
        if (persona.parteroAncestral == true) add(R.drawable.partero_ancestral to "Partero/a ancestral tradicional")
        if (persona.sabiduriaAncestral == true) add(R.drawable.sabiduria_ancestral to "Sabiduría ancestral")
    }
    if (iconos.isEmpty()) return
    Row(modifier.testTag("iconos_actores_${persona.id}"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        iconos.forEach { (recurso, descripcion) ->
            Image(
                painter = painterResource(recurso),
                contentDescription = descripcion,
                modifier = Modifier.size(32.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun FilaActor(titulo: String, recurso: Int, marcado: Boolean, etiquetaPrueba: String, onCambio: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCambio(!marcado) }
            .padding(vertical = 6.dp)
            .testTag(etiquetaPrueba),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = marcado, onCheckedChange = null)
        Image(
            painter = painterResource(recurso),
            contentDescription = null,
            modifier = Modifier.padding(start = 10.dp).size(40.dp),
            contentScale = ContentScale.Fit
        )
        Text(titulo, modifier = Modifier.padding(start = 10.dp).weight(1f))
    }
}

@Composable
private fun FormularioMiembroScreen(
    fichaId: Long,
    miembro: MiembroFamiliaEntity?,
    telefonoJefeInicial: String,
    sugerirJefe: Boolean,
    mostrarAvance: Boolean = false,
    onGuardar: (MiembroFamiliaEntity, String) -> Unit,
    onCancelar: () -> Unit
) {
    // Preguntas respondidas por el usuario (solo para la barra de llenado al crear).
    val tocados = remember { mutableStateListOf<String>() }
    fun marcar(clave: String) { if (clave !in tocados) tocados.add(clave) }
    fun avance(vararg respondidas: Boolean): Float? =
        if (mostrarAvance && miembro == null && respondidas.isNotEmpty())
            respondidas.count { it }.toFloat() / respondidas.size
        else null
    var telefonoJefe by remember { mutableStateOf(telefonoJefeInicial) }
    var nombres by remember { mutableStateOf(miembro?.apellidosNombres.orEmpty()) }
    var parentesco by remember {
        mutableStateOf(miembro?.parentesco.orEmpty().ifBlank { if (sugerirJefe) "JEFE/A DE FAMILIA" else "HIJO/A" })
    }
    var fechaNacimiento by remember { mutableStateOf(miembro?.fechaNacimiento.orEmpty()) }
    var ocupacion by remember { mutableStateOf(miembro?.ocupacion.orEmpty()) }
    var sexo by remember { mutableStateOf(miembro?.sexo.orEmpty().ifBlank { "H" }) }
    var escolaridad by remember { mutableStateOf(miembro?.escolaridad.orEmpty().ifBlank { "SIN" }) }
    var vacunas by remember { mutableStateOf(miembro?.vacunasCompletas ?: false) }
    var saludBucal by remember { mutableStateOf(miembro?.saludBucalAdecuada ?: false) }
    var estadoNutricional by remember { mutableStateOf(miembro?.estadoNutricional.orEmpty().ifBlank { DispensarizacionAutomatica.NUTRICION_SIN_ALTERACION }) }
    var factoresEdad by remember { mutableStateOf(FactoresRiesgoEdad.decodificar(miembro?.factoresRiesgoEdadJson ?: "[]")) }
    val factoresEdadIniciales = remember { FactoresRiesgoEdad.decodificar(miembro?.factoresRiesgoEdadJson ?: "[]") }
    val codigosIniciales = remember { EstrategiasDesdeCie10.codigos(miembro?.comorbilidadesCie10Json ?: "[]") }
    var eventoSalud by remember { mutableStateOf(miembro?.eventoSalud ?: false) }
    var casoConfirmado by remember { mutableStateOf(miembro?.casoConfirmado ?: false) }
    var casoSospechosoUno by remember { mutableStateOf(miembro?.casoSospechosoUno ?: false) }
    var casoSospechosoDos by remember { mutableStateOf(miembro?.casoSospechosoDos ?: false) }
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
        onVolver = onCancelar,
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
                        val codigosActuales = diagnosticos.map { it.codigo }
                        // Se calculan de los diagnósticos CIE-10; un dato manual de versiones anteriores se conserva
                        // mientras no lo explique (o lo desmienta) un diagnóstico.
                        fun calculada(ahora: Boolean, antes: Boolean, guardado: Boolean?) = ahora || (guardado == true && !antes)
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
                                hipertensionArterial = calculada(EstrategiasDesdeCie10.hipertension(codigosActuales), EstrategiasDesdeCie10.hipertension(codigosIniciales), miembro?.hipertensionArterial),
                                diabetesMellitus = calculada(EstrategiasDesdeCie10.diabetes(codigosActuales), EstrategiasDesdeCie10.diabetes(codigosIniciales), miembro?.diabetesMellitus),
                                tuberculosis = calculada(EstrategiasDesdeCie10.tuberculosis(codigosActuales), EstrategiasDesdeCie10.tuberculosis(codigosIniciales), miembro?.tuberculosis),
                                problemaSaludMental = calculada(EstrategiasDesdeCie10.saludMental(codigosActuales), EstrategiasDesdeCie10.saludMental(codigosIniciales), miembro?.problemaSaludMental),
                                consumoAlcoholDrogas = calculada(
                                    FactoresRiesgoEdad.CONSUMO in factoresEdad &&
                                        FactoresRiesgoEdad.disponibles(FactoresRiesgoEdad.bandaPorFecha(fechaNacimiento)).any { it.codigo == FactoresRiesgoEdad.CONSUMO },
                                    FactoresRiesgoEdad.CONSUMO in factoresEdadIniciales,
                                    miembro?.consumoAlcoholDrogas
                                ),
                                enfermedadCronica = diagnosticos.isNotEmpty(),
                                discapacidadVisual = tipoDiscapacidad == "VISUAL",
                                discapacidadAuditiva = tipoDiscapacidad == "AUDITIVA",
                                discapacidadLenguaje = tipoDiscapacidad == "LENGUAJE",
                                discapacidadFisica = tipoDiscapacidad == "FISICA",
                                discapacidadIntelectual = tipoDiscapacidad == "INTELECTUAL",
                                discapacidadPsicosocial = tipoDiscapacidad == "PSICOSOCIAL",
                                cuidadosPaliativos = calculada(EstrategiasDesdeCie10.cuidadosPaliativos(codigosActuales), EstrategiasDesdeCie10.cuidadosPaliativos(codigosIniciales), miembro?.cuidadosPaliativos),
                                vih = calculada(EstrategiasDesdeCie10.vih(codigosActuales), EstrategiasDesdeCie10.vih(codigosIniciales), miembro?.vih),
                                eventoSalud = eventoSalud,
                                casoConfirmado = casoConfirmado,
                                casoSospechosoUno = casoSospechosoUno,
                                casoSospechosoDos = casoSospechosoDos,
                                // Los actores comunitarios se asignan desde el botón de la lista de integrantes.
                                prestadorComunitario = miembro?.prestadorComunitario ?: false,
                                parteroAncestral = miembro?.parteroAncestral ?: false,
                                sabiduriaAncestral = miembro?.sabiduriaAncestral ?: false,
                                factoresRiesgoEdadJson = FactoresRiesgoEdad.codificar(factoresEdad),
                                comorbilidadesCie10Json = CatalogoCie10.codificar(diagnosticos),
                                porcentajeDiscapacidad = porcentajeDiscapacidad.toIntOrNull()
                                    ?.coerceIn(0, 100)
                                    ?.takeIf { tipoDiscapacidad != "NINGUNA" },
                                necesitaAyudaTecnica = necesitaAyudaTecnica && tipoDiscapacidad != "NINGUNA",
                                enfermedadCronicaDescompensada = diagnosticos.any { it.descompensada },
                                riesgoGenetico = riesgoPrioritario == "GENETICO",
                                victimaViolencia = calculada(
                                    FactoresRiesgoEdad.VIOLENCIA in factoresEdad &&
                                        FactoresRiesgoEdad.disponibles(FactoresRiesgoEdad.bandaPorFecha(fechaNacimiento)).any { it.codigo == FactoresRiesgoEdad.VIOLENCIA },
                                    FactoresRiesgoEdad.VIOLENCIA in factoresEdadIniciales,
                                    miembro?.victimaViolencia
                                ),
                                privadoLibertad = riesgoPrioritario == "PRIVADO_LIBERTAD",
                                numeroHistoriaClinica = cedula.trim(),
                                cedula = cedula.trim(),
                                syncId = miembro?.syncId ?: UUID.randomUUID().toString()
                            ),
                            telefonoJefe.trim()
                        )
                    }
                }
            )

        }
    ) {
        SeccionFormularioRuralitos(
            titulo = "1. Identificación personal",
            desplegable = true,
            progreso = avance(*(listOf(nombres.isNotBlank(), true, cedula.isNotBlank(), fechaNacimiento.isNotBlank()) +
                (if (parentesco == "JEFE/A DE FAMILIA") listOf(telefonoJefe.isNotBlank()) else emptyList())).toBooleanArray()),
            abiertaInicial = true,
            descripcion = "Apellidos y nombres, parentesco, cédula y fecha de nacimiento."
        ) {
            CampoTexto(nombres, { nombres = it }, "Apellidos y nombres")
            SeleccionTextoMiembro(
                "Parentesco con el jefe de familia",
                listOf("JEFE/A DE FAMILIA", "CÓNYUGE/PAREJA", "HIJO/A", "PADRE/MADRE", "ABUELO/A", "NIETO/A", "HERMANO/A", "OTRO FAMILIAR", "NO FAMILIAR").map { it to it },
                parentesco,
                MoradoClinico
            ) { parentesco = it }
            if (parentesco == "JEFE/A DE FAMILIA") {
                OutlinedTextField(
                    value = telefonoJefe,
                    onValueChange = { telefonoJefe = it.filter { c -> c.isDigit() || c in "+ -" }.take(20) },
                    label = { Text("Número de teléfono") },
                    supportingText = { Text("Contacto de la familia. Solo se pide al jefe o jefa.") },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
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
            desplegable = true,
            progreso = avance(*(listOf("sexo" in tocados) +
                (if (camposPermitidos.escolaridades.isNotEmpty()) listOf("escolaridad" in tocados) else emptyList()) +
                (if (camposPermitidos.ocupacion) listOf(ocupacion.isNotBlank()) else emptyList())).toBooleanArray()),
            abiertaInicial = false,
            descripcion = "Sexo, escolaridad y ocupación según el grupo de edad."
        ) {
            SeleccionTextoMiembro("Sexo", listOf("H" to "Hombre", "M" to "Mujer"), sexo, MoradoClinico) { sexo = it; marcar("sexo") }
            if (camposPermitidos.escolaridades.isNotEmpty()) {
                SeleccionTextoMiembro(
                    "Escolaridad",
                    listOf("SIN" to "Sin escolaridad", "BAS" to "Básica", "BACH" to "Bachillerato", "SUP" to "Superior", "ESP" to "Especialidad")
                        .filter { it.first in camposPermitidos.escolaridades },
                    escolaridad,
                    AzulClinico
                ) { escolaridad = it; marcar("escolaridad") }
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
            desplegable = true,
            progreso = avance("vacunas" in tocados, "saludBucal" in tocados, "nutricion" in tocados),
            abiertaInicial = false,
            descripcion = "Vacunas, nutrición y salud bucal. Estos datos alimentan la dispensarización automática."
        ) {
            SeleccionBooleanMiembro(
                "Esquema completo de vacunas",
                vacunas
            ) { vacunas = it; marcar("vacunas") }
            SeleccionBooleanMiembro(
                "Salud bucal adecuada",
                saludBucal
            ) { saludBucal = it; marcar("saludBucal") }
            SeleccionOpcionMiembro(
                "Estado nutricional evaluado",
                DispensarizacionAutomatica.opcionesNutricion,
                estadoNutricional,
                NaranjaClinico
            ) { estadoNutricional = it; marcar("nutricion") }
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
        val bandaEdad = FactoresRiesgoEdad.bandaPorFecha(fechaNacimiento)
        val factoresDisponibles = FactoresRiesgoEdad.disponibles(bandaEdad)
        SeccionFormularioRuralitos(
            titulo = "4. Factores de riesgo según la edad",
            desplegable = true,
            abiertaInicial = false,
            descripcion = "Marca solo lo que corresponda. Cada factor marcado pasa a la persona al Grupo II."
        ) {
            if (bandaEdad == null) {
                MensajeEstadoRuralitos(
                    titulo = "Falta la fecha de nacimiento",
                    descripcion = "La lista de factores depende de la edad. Selecciona primero la fecha de nacimiento.",
                    color = AzulClinico,
                    simbolo = "i"
                )
            } else {
                Text(
                    bandaEdad.etiqueta,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp).testTag("banda_factores")
                )
                factoresDisponibles.forEach { factor ->
                    CasillaFactorRiesgo(
                        etiqueta = factor.etiqueta,
                        marcada = factor.codigo in factoresEdad,
                        etiquetaPrueba = "factor_${factor.codigo}"
                    ) { marcado ->
                        factoresEdad = if (marcado) factoresEdad + factor.codigo else factoresEdad - factor.codigo
                    }
                }
                val marcados = factoresDisponibles.count { it.codigo in factoresEdad }
                MensajeEstadoRuralitos(
                    titulo = if (marcados == 0) "Sin factores marcados" else "Grupo II · con factores de riesgo",
                    descripcion = if (marcados == 0) "El grupo lo deciden el resto de datos de la ficha."
                    else "$marcados factor(es) marcado(s).",
                    color = if (marcados == 0) CianRuralitos else NaranjaClinico,
                    simbolo = if (marcados == 0) "✓" else "II",
                    modifier = Modifier.padding(top = 10.dp).testTag("resumen_factores")
                )
            }
        }
        SeccionFormularioRuralitos(
            titulo = "5. Discapacidad",
            desplegable = true,
            progreso = avance(*(listOf("discTipo" in tocados) +
                (if (tipoDiscapacidad != "NINGUNA") listOf(porcentajeDiscapacidad.isNotBlank(), "ayudaTecnica" in tocados) else emptyList())).toBooleanArray()),
            abiertaInicial = false,
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
                marcar("discTipo")
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
            ) { necesitaAyudaTecnica = it; marcar("ayudaTecnica") }
        }
        if (EstrategiasDesdeCie10.pideAlertasEpidemiologicas(diagnosticos.map { it.codigo })) SeccionFormularioRuralitos(
            titulo = "6. Alertas Epidemiológicas",
            desplegable = true,
            progreso = avance("alertas" in tocados),
            abiertaInicial = false,
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
                    marcar("alertas")
                    eventoSalud = "EVENTO" in seleccion
                    casoSospechosoUno = "SOSPECHOSO" in seleccion
                    if (!casoSospechosoUno) casoSospechosoDos = false
                    casoConfirmado = "CONFIRMADO" in seleccion
                }
            )
        }
        SeccionFormularioRuralitos(
            titulo = "7. Otros riesgos prioritarios",
            desplegable = true,
            progreso = avance("riesgoPrioritario" in tocados),
            abiertaInicial = false,
            descripcion = "Selecciona la condición registrada o Ninguno."
        ) {
            SeleccionOpcionMiembro(
                titulo = "Riesgo prioritario",
                opciones = listOf(
                    "NINGUNO" to "Ninguno",
                    "GENETICO" to "Riesgo genético",
                    "PRIVADO_LIBERTAD" to "Persona privada de la libertad"
                ),
                seleccion = riesgoPrioritario,
                color = NaranjaClinico
            ) { riesgoPrioritario = it; marcar("riesgoPrioritario") }
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
private fun CasillaFactorRiesgo(
    etiqueta: String,
    marcada: Boolean,
    etiquetaPrueba: String,
    onCambio: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCambio(!marcada) }
            .padding(vertical = 2.dp)
            .testTag(etiquetaPrueba),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        androidx.compose.material3.Checkbox(checked = marcada, onCheckedChange = null)
        Text(etiqueta, modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 8.dp))
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
        BotonSelectorRuralitos(
            onClick = { expandido = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text(
                opciones.firstOrNull { it.first == seleccion }?.second ?: "Seleccionar",
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
        MenuDesplegableRuralitos(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            opciones.forEach { (valor, etiqueta) ->
                ItemMenuRuralitos(
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
    onSeleccion: (Set<String>) -> Unit,
    iconos: Map<String, Int> = emptyMap()
) {
    var expandido by remember { mutableStateOf(false) }
    Text(titulo, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 5.dp))
    Box(Modifier.fillMaxWidth()) {
        BotonSelectorRuralitos(
            onClick = { expandido = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text(
                when (seleccion.size) {
                    0 -> "Ninguno"
                    1 -> opciones.firstOrNull { it.first in seleccion }?.second.orEmpty()
                    else -> "${seleccion.size} grupos seleccionados"
                },
                fontWeight = FontWeight.SemiBold
            )
        }
        MenuDesplegableRuralitos(expanded = expandido, onDismissRequest = { expandido = false }) {
            ItemMenuRuralitos(
                text = { Text("${if (seleccion.isEmpty()) "✓ " else ""}Ninguno") },
                onClick = { onSeleccion(emptySet()); expandido = false }
            )
            opciones.forEach { (valor, etiqueta) ->
                ItemMenuRuralitos(
                    text = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            iconos[valor]?.let {
                                Image(
                                    painter = painterResource(it),
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 10.dp).size(36.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Text("${if (valor in seleccion) "✓ " else ""}$etiqueta")
                        }
                    },
                    onClick = {
                        onSeleccion(if (valor in seleccion) seleccion - valor else seleccion + valor)
                    }
                )
            }
        }
    }
}
