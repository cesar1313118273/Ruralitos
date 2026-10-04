package com.ruralitos.app.ui.screens

import androidx.compose.runtime.mutableStateListOf
import com.ruralitos.app.ui.components.VentanaRuralitos
import com.ruralitos.app.domain.EtiquetasFicha
import com.ruralitos.app.ui.components.VentanaConfirmarRuralitos
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.FloatingActionButton
import com.ruralitos.app.ui.components.OpcionBusqueda
import com.ruralitos.app.ui.components.BotonFlotanteRedondo
import com.ruralitos.app.ui.components.PilaBotonesFlotantes
import com.ruralitos.app.ui.components.TarjetaFormularioRuralitos
import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.MortalidadFamiliarEntity
import com.ruralitos.app.domain.FactoresObstetricos
import com.ruralitos.app.ui.components.BuscadorDeFactores
import com.ruralitos.app.domain.RolFamiliar
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
import com.ruralitos.app.domain.GrupoDispensarizacion
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
    alCrearFicha: (suspend (MiembroFamiliaEntity, String, EmbarazadaEntity?) -> Boolean)? = null
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
    var fichasRepetidas by remember { mutableStateOf<List<com.ruralitos.app.data.local.entity.FichaFamiliarEntity>>(emptyList()) }
    var creacionPendiente by remember {
        mutableStateOf<Triple<MiembroFamiliaEntity, String, EmbarazadaEntity?>?>(null)
    }
    var mostrandoFormulario by remember { mutableStateOf(false) }
    var miembroEditando by remember { mutableStateOf<MiembroFamiliaEntity?>(null) }
    var miembroEliminar by remember { mutableStateOf<MiembroFamiliaEntity?>(null) }
    var miembroNota by remember { mutableStateOf<MiembroFamiliaEntity?>(null) }
    var textoNota by remember { mutableStateOf("") }
    var sesionNota by remember { mutableStateOf<SesionNota?>(null) }
    var errorNota by remember { mutableStateOf(false) }
    var eligiendoActor by remember { mutableStateOf(false) }
    var actorEditando by remember { mutableStateOf<MiembroFamiliaEntity?>(null) }
    val embarazadas by database.fichaContenidoDao()
        .listarEmbarazadas(fichaId)
        .collectAsState(initial = emptyList())
    val mortalidad by database.fichaContenidoDao()
        .listarMortalidad(fichaId)
        .collectAsState(initial = emptyList())
    var mostrandoMortalidad by remember { mutableStateOf(false) }
    // El embarazo de una persona se reconoce por su nombre, como en las fichas anteriores.
    fun embarazoDe(persona: MiembroFamiliaEntity?) = persona?.let { p ->
        embarazadas.firstOrNull { it.apellidosNombres.trim().equals(p.apellidosNombres.trim(), ignoreCase = true) }
    }

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
        VentanaRuralitos(
            titulo = "Nota diaria",
            subtitulo = integrante.apellidosNombres,
            simbolo = "✎",
            color = CianRuralitos,
            onCerrar = {
                val contenido = textoNota
                val sesion = sesionNota
                if (sesion != null) scope.launch { guardarNota(contenido, sesion) }
                miembroNota = null
            },
            contenido = {
                OutlinedTextField(
                    value = textoNota,
                    onValueChange = { textoNota = it },
                    label = { Text("Nota importante") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    if (errorNota) "No se pudo guardar. Comprueba el almacenamiento."
                    else "Se guarda automáticamente mientras escribes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (errorNota) RojoClinico else AzulClinico
                )
            },
            acciones = {
                BotonPrincipalRuralitos(
                    texto = "Guardar y cerrar",
                    color = CianRuralitos,
                    onClick = {
                        val contenido = textoNota
                        val sesion = sesionNota
                        if (sesion != null) scope.launch { guardarNota(contenido, sesion) }
                        miembroNota = null
                    }
                )
            }
        )
    }

    if (fichasRepetidas.isNotEmpty()) {
        VentanaConfirmarRuralitos(
            titulo = "Ya existe una ficha con esta identificación",
            mensaje = fichasRepetidas.take(3).joinToString("\n") { f ->
                "• ${f.nombreApellidoJefeFamilia.ifBlank { "Familia sin nombre" }} · ficha ${f.numeroFichaFamiliar}" +
                    (EtiquetasFicha.texto(f)?.let { " · $it" } ?: " · tuya")
            } + "\n\nSi es la misma familia, ábrela desde «Buscar y modificar fichas» en lugar de crear otra.",
            textoConfirmar = "Crear otra de todos modos",
            onConfirmar = {
                val pendiente = creacionPendiente
                fichasRepetidas = emptyList()
                creacionPendiente = null
                if (pendiente != null && alCrearFicha != null) scope.launch {
                    val creada = runCatching { alCrearFicha(pendiente.first, pendiente.second, pendiente.third) }.getOrDefault(false)
                    if (creada) {
                        mostrandoFormulario = false
                        miembroEditando = null
                    } else {
                        com.ruralitos.app.ui.components.AvisosRuralitos.mostrar("No se pudo guardar la ficha.")
                    }
                }
            },
            textoCancelar = "Cancelar",
            peligro = false,
            onCancelar = { fichasRepetidas = emptyList(); creacionPendiente = null }
        )
    }

    if (mostrandoFormulario) {
        FormularioMiembroScreen(
            fichaId = fichaId,
            miembro = miembroEditando,
            telefonoJefeInicial = fichaActual?.numeroTelefono.orEmpty(),
            sugerirJefe = miembros.none { RolFamiliar.esJefe(it.parentesco) },
            hayOtroJefe = miembros.any { RolFamiliar.esJefe(it.parentesco) && it.id != (miembroEditando?.id ?: -1L) },
            embarazoInicial = embarazoDe(miembroEditando),
            mostrarAvance = mostrarAvance,
            onGuardar = { miembro, telefonoJefe, embarazo ->
              if (fichaId == 0L && alCrearFicha != null) {
                // Borrador: nada se guarda hasta registrar al jefe o jefa de familia.
                if (!RolFamiliar.esJefe(miembro.parentesco)) {
                    com.ruralitos.app.ui.components.AvisosRuralitos.mostrar("Primero registra al jefe o jefa de familia. Hasta entonces no se guarda nada.")
                } else {
                    scope.launch {
                        // Antes de crear, se avisa si este teléfono ya tiene una ficha con la misma identificación
                        // (propia o compartida por otra persona) para no duplicar a la familia.
                        val repetidas = if (miembro.cedula.isBlank()) emptyList() else withContext(Dispatchers.IO) {
                            database.fichaFamiliarDao().fichasConCedula(miembro.cedula.trim())
                        }
                        if (repetidas.isNotEmpty()) {
                            fichasRepetidas = repetidas
                            creacionPendiente = Triple(miembro, telefonoJefe, embarazo)
                            return@launch
                        }
                        val creada = runCatching { alCrearFicha(miembro, telefonoJefe, embarazo) }.getOrDefault(false)
                        if (creada) {
                            mostrandoFormulario = false
                            miembroEditando = null
                        } else {
                            com.ruralitos.app.ui.components.AvisosRuralitos.mostrar("No se pudo guardar la ficha.")
                        }
                    }
                }
              } else
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val previa = embarazoDe(miembroEditando)
                            if (miembro.id == 0L) {
                                database.fichaContenidoDao().guardarMiembro(miembro)
                            } else {
                                database.fichaContenidoDao().actualizarMiembro(miembro)
                            }
                            if (embarazo != null) {
                                val registro = embarazo.copy(
                                    id = previa?.id ?: 0L,
                                    fichaId = fichaId,
                                    syncId = previa?.syncId ?: embarazo.syncId
                                )
                                if (registro.id == 0L) database.fichaContenidoDao().guardarEmbarazada(registro)
                                else database.fichaContenidoDao().actualizarEmbarazada(registro)
                            } else if (previa != null) {
                                database.fichaContenidoDao().eliminarEmbarazada(previa)
                            }
                            // El jefe o jefa de familia define la cédula, el nombre y el
                            // teléfono con los que se identifica la ficha.
                            if (RolFamiliar.esJefe(miembro.parentesco)) {
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
                        com.ruralitos.app.ui.components.AvisosRuralitos.mostrar("No se pudo guardar el integrante.")
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
        VentanaRuralitos(
            titulo = "Actores comunitarios",
            subtitulo = "Elige a la persona",
            simbolo = "◉",
            color = VerdeSalud,
            onCerrar = { eligiendoActor = false },
            contenido = {
                Column(Modifier.testTag("lista_actores")) {
                    Text(
                        "Elige a la persona que sea prestador comunitario de salud, partero/a ancestral o de sabiduría ancestral.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 6.dp)
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
            acciones = { BotonSecundarioRuralitos(texto = "Cerrar", onClick = { eligiendoActor = false }) }
        )
    }

    actorEditando?.let { persona ->
        var prestador by remember(persona.id) { mutableStateOf(persona.prestadorComunitario == true) }
        var partero by remember(persona.id) { mutableStateOf(persona.parteroAncestral == true) }
        var sabiduria by remember(persona.id) { mutableStateOf(persona.sabiduriaAncestral == true) }
        VentanaRuralitos(
            titulo = persona.apellidosNombres.ifBlank { "Integrante sin nombre" },
            subtitulo = "Actores comunitarios",
            simbolo = "◉",
            color = VerdeSalud,
            onCerrar = { actorEditando = null },
            contenido = {
                FilaActor("Prestador comunitario de salud", R.drawable.prestador_comunitario, prestador, "actor_prestador") { prestador = it }
                FilaActor("Partero/a ancestral tradicional", R.drawable.partero_ancestral, partero, "actor_partero") { partero = it }
                FilaActor("Hombre o mujer de sabiduría ancestral", R.drawable.sabiduria_ancestral, sabiduria, "actor_sabiduria") { sabiduria = it }
            },
            acciones = {
                BotonPrincipalRuralitos(
                    texto = "Guardar",
                    color = CianRuralitos,
                    modifier = Modifier.testTag("guardar_actores"),
                    onClick = {
                        val cambiada = persona.copy(
                            prestadorComunitario = prestador, parteroAncestral = partero, sabiduriaAncestral = sabiduria
                        )
                        actorEditando = null
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) { database.fichaContenidoDao().actualizarMiembro(cambiada) } }
                                .onFailure { com.ruralitos.app.ui.components.AvisosRuralitos.mostrar("No se pudo guardar.") }
                        }
                    }
                )
                BotonSecundarioRuralitos(texto = "Cancelar", onClick = { actorEditando = null })
            }
        )
    }

    if (mostrandoMortalidad) {
        VentanaMortalidad(
            fichaId = fichaId,
            registros = mortalidad,
            onGuardar = { item ->
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            if (item.id == 0L) database.fichaContenidoDao().guardarMortalidad(item)
                            else database.fichaContenidoDao().actualizarMortalidad(item)
                        }
                    }.onFailure {
                        com.ruralitos.app.ui.components.AvisosRuralitos.mostrar("No se pudo guardar el registro de mortalidad.")
                    }
                }
            },
            onEliminar = { item ->
                scope.launch(Dispatchers.IO) { database.fichaContenidoDao().eliminarMortalidad(item) }
            },
            onCerrar = { mostrandoMortalidad = false }
        )
    }

    miembroEliminar?.let { seleccionado ->
        VentanaConfirmarRuralitos(
            titulo = "Eliminar integrante",
            mensaje = "¿Deseas eliminar a ${seleccionado.apellidosNombres}? Esta acción quitará sus datos de la ficha.",
            textoConfirmar = "Sí, eliminar",
            onConfirmar = {
                                miembroEliminar = null
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        embarazoDe(seleccionado)?.let { database.fichaContenidoDao().eliminarEmbarazada(it) }
                                        database.fichaContenidoDao().eliminarMiembro(seleccionado)
                                    }
                                }
                            },
            textoCancelar = "Conservar integrante",
            peligro = true,
            onCancelar = { miembroEliminar = null }
        )
    }

    Box(Modifier.fillMaxSize()) {
    PantallaListaRuralitos(
        titulo = "Integrantes de la familia",
        descripcion = "Registra a cada persona del hogar. La edad determina automáticamente qué campos aplican en la ficha.",
        paso = 2,
        totalPasos = 8,
        etiquetaPaso = "Información del hogar",
        onVolver = onSalir,
        barraAccion = {
                BotonPrincipalRuralitos(
                    texto = "Guardar información de esta sección",
                    descripcion = "Los integrantes registrados ya están guardados",
                    color = AzulClinico,
                    onClick = {
                    if (fichaId == 0L && alCrearFicha != null) {
                        com.ruralitos.app.ui.components.AvisosRuralitos.mostrar("Registra primero al jefe o jefa de familia para guardar la ficha.")
                    } else {
                        onContinuar()
                    }
                }
                )

        }
    ) {
        if (miembros.isEmpty()) {
            item {
                MensajeEstadoRuralitos(
                    titulo = "Aún no hay integrantes",
                    descripcion = "Toca el botón + para registrar al jefe o jefa de familia: su cédula y su teléfono identifican la ficha.",
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
    PilaBotonesFlotantes {
        BotonFlotanteRedondo(
            descripcion = "Agregar un integrante familiar",
            color = CianRuralitos,
            etiquetaPrueba = "boton_agregar_integrante",
            onClick = {
                miembroEditando = null
                mostrandoFormulario = true
            }
        )
        if (miembros.isNotEmpty() && !(fichaId == 0L && alCrearFicha != null)) {
            BotonFlotanteRedondo(
                descripcion = "Actores comunitarios",
                color = VerdeSalud,
                etiquetaPrueba = "boton_actores_comunitarios",
                onClick = { eligiendoActor = true }
            ) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.prestador_comunitario),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
        if (!(fichaId == 0L && alCrearFicha != null)) {
            BotonFlotanteRedondo(
                descripcion = "Mortalidad familiar",
                color = RojoClinico,
                etiquetaPrueba = "boton_mortalidad",
                onClick = { mostrandoMortalidad = true }
            ) { Text("✝", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
        }
    }
    }
}

/**
 * Ventana de mortalidad familiar (últimos cinco años): lista lo registrado y pide los cuatro datos de la ficha.
 * Vive en Integrantes; ya no existe una sección aparte de embarazo y mortalidad.
 */
@Composable
private fun VentanaMortalidad(
    fichaId: Long,
    registros: List<MortalidadFamiliarEntity>,
    onGuardar: (MortalidadFamiliarEntity) -> Unit,
    onEliminar: (MortalidadFamiliarEntity) -> Unit,
    onCerrar: () -> Unit
) {
    var editando by remember { mutableStateOf<MortalidadFamiliarEntity?>(null) }
    var nombre by remember { mutableStateOf("") }
    var parentesco by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var causa by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var porEliminar by remember { mutableStateOf<MortalidadFamiliarEntity?>(null) }

    fun limpiar() {
        editando = null; nombre = ""; parentesco = ""; edad = ""; causa = ""; error = null
    }

    porEliminar?.let { seleccionado ->
        VentanaConfirmarRuralitos(
            titulo = "Eliminar registro de mortalidad",
            mensaje = "Se eliminará el registro de ${seleccionado.nombre}. Esta acción no se puede deshacer.",
            textoConfirmar = "Sí, eliminar",
            onConfirmar = {
                            if (editando?.id == seleccionado.id) limpiar()
                            onEliminar(seleccionado)
                            porEliminar = null
                        },
            textoCancelar = "Conservar registro",
            peligro = true,
            onCancelar = { porEliminar = null }
        )
    }

    VentanaRuralitos(
        titulo = "Mortalidad familiar",
        subtitulo = "Últimos cinco años",
        simbolo = "✝",
        color = RojoClinico,
        onCerrar = onCerrar,
        contenido = {
            Column(Modifier.testTag("ventana_mortalidad")) {
                Text(
                    "Solo fallecimientos de los últimos cinco años. Registra apellidos y nombres, parentesco, edad al fallecer y causa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                registros.forEach { registro ->
                    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        Text(registro.nombre.ifBlank { "Persona sin nombre" }, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOf(registro.parentesco, registro.edadAlFallecer?.let { "$it años" }.orEmpty(), registro.causa)
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row {
                            TextButton(onClick = {
                                editando = registro
                                nombre = registro.nombre; parentesco = registro.parentesco
                                edad = registro.edadAlFallecer?.toString().orEmpty(); causa = registro.causa
                                error = null
                            }) { Text("Editar") }
                            TextButton(onClick = { porEliminar = registro }) { Text("Eliminar", color = RojoClinico) }
                        }
                    }
                    HorizontalDivider()
                }
                Text(
                    if (editando == null) "Agregar un fallecimiento" else "Editar fallecimiento",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 14.dp)
                )
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Apellidos y nombres") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("mortalidad_nombre")
                )
                OutlinedTextField(
                    value = parentesco, onValueChange = { parentesco = it },
                    label = { Text("Parentesco") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("mortalidad_parentesco")
                )
                OutlinedTextField(
                    value = edad, onValueChange = { edad = it.filter(Char::isDigit).take(3) },
                    label = { Text("Edad al fallecer") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("mortalidad_edad")
                )
                OutlinedTextField(
                    value = causa, onValueChange = { causa = it },
                    label = { Text("Causa del fallecimiento") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("mortalidad_causa")
                )
                error?.let {
                    Text(it, color = RojoClinico, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                }
            }
        },
        acciones = {
            BotonPrincipalRuralitos(
                texto = if (editando == null) "Guardar fallecimiento" else "Guardar cambios",
                color = RojoClinico,
                modifier = Modifier.testTag("guardar_fallecimiento"),
                onClick = {
                    if (nombre.isBlank() || parentesco.isBlank() || edad.toIntOrNull() == null || causa.isBlank()) {
                        error = "Completa apellidos y nombres, parentesco, edad al fallecer y causa."
                    } else {
                        onGuardar(
                            MortalidadFamiliarEntity(
                                id = editando?.id ?: 0,
                                fichaId = fichaId,
                                nombre = nombre.trim(),
                                parentesco = parentesco.trim(),
                                edadAlFallecer = edad.toIntOrNull(),
                                causa = causa.trim(),
                                syncId = editando?.syncId ?: UUID.randomUUID().toString()
                            )
                        )
                        limpiar()
                    }
                }
            )
            BotonSecundarioRuralitos(texto = "Cerrar", onClick = onCerrar)
        }
    )
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
    hayOtroJefe: Boolean,
    embarazoInicial: EmbarazadaEntity?,
    mostrarAvance: Boolean = false,
    onGuardar: (MiembroFamiliaEntity, String, EmbarazadaEntity?) -> Unit,
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
        mutableStateOf(
            RolFamiliar.normalizar(miembro?.parentesco.orEmpty(), miembro?.sexo.orEmpty())
                .ifBlank { if (sugerirJefe) "JEFE DE FAMILIA" else "HIJO" }
        )
    }
    var fechaNacimiento by remember { mutableStateOf(miembro?.fechaNacimiento.orEmpty()) }
    var ocupacion by remember { mutableStateOf(miembro?.ocupacion.orEmpty()) }
    var sexo by remember { mutableStateOf(miembro?.sexo.orEmpty().ifBlank { "H" }) }
    var escolaridad by remember { mutableStateOf(miembro?.escolaridad.orEmpty().ifBlank { "SIN" }) }
    var vacunas by remember { mutableStateOf(miembro?.vacunasCompletas ?: false) }
    var saludBucal by remember { mutableStateOf(miembro?.saludBucalAdecuada ?: false) }
    // Paciente embarazada: al marcarla aparecen los datos del embarazo (antes eran una sección aparte).
    var embarazada by remember { mutableStateOf(embarazoInicial != null) }
    var fum by remember { mutableStateOf(embarazoInicial?.fechaUltimaMenstruacion.orEmpty()) }
    var fpp by remember { mutableStateOf(embarazoInicial?.fechaProbableParto.orEmpty()) }
    var semanas by remember { mutableStateOf(embarazoInicial?.semanasGestacion?.toString().orEmpty()) }
    var dtPrimera by remember { mutableStateOf(embarazoInicial?.dosisDtPrimera ?: false) }
    var dtSegunda by remember { mutableStateOf(embarazoInicial?.dosisDtSegunda ?: false) }
    var dtRefuerzo by remember { mutableStateOf(embarazoInicial?.dosisDtRefuerzo ?: false) }
    var gestas by remember { mutableStateOf(embarazoInicial?.gestas?.toString().orEmpty()) }
    var partos by remember { mutableStateOf(embarazoInicial?.partos?.toString().orEmpty()) }
    var abortos by remember { mutableStateOf(embarazoInicial?.abortos?.toString().orEmpty()) }
    var cesareas by remember { mutableStateOf(embarazoInicial?.cesareas?.toString().orEmpty()) }
    var antecedentesObstetricos by remember { mutableStateOf(embarazoInicial?.antecedentesPatologicosObstetricos.orEmpty()) }
    var factoresObstetricos by remember { mutableStateOf(FactoresObstetricos.decodificar(embarazoInicial?.factoresObstetricosJson ?: "[]")) }
    var calendarioEmbarazo by remember { mutableStateOf<String?>(null) }
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

    fun cambiarEmbarazada(valor: Boolean) {
        embarazada = valor
        if (valor) {
            sexo = "M"
            if (RolFamiliar.sexoDelRol(parentesco) == "H") {
                parentesco = if (RolFamiliar.esJefe(parentesco) && !hayOtroJefe) "JEFA DE FAMILIA" else "CÓNYUGE/PAREJA"
            }
        }
    }

    // El integrante tal como quedaría con lo que hay en el formulario: se usa para guardarlo y para mostrar su grupo en vivo.
    fun armarMiembro(): MiembroFamiliaEntity {
        val codigosActuales = diagnosticos.map { it.codigo }
        // El estado nutricional sale de los diagnósticos CIE-10 (E40-E46 desnutrición, E66 obesidad); un dato manual
        // de versiones anteriores se conserva mientras ningún diagnóstico lo explique.
        val nutricionAhora = EstrategiasDesdeCie10.estadoNutricional(codigosActuales)
        val nutricionAntes = EstrategiasDesdeCie10.estadoNutricional(codigosIniciales)
        val nutricionGuardada = miembro?.estadoNutricional.orEmpty()
        val estadoNutricional = when {
            nutricionAhora != DispensarizacionAutomatica.NUTRICION_SIN_ALTERACION -> nutricionAhora
            nutricionAntes == DispensarizacionAutomatica.NUTRICION_SIN_ALTERACION && nutricionGuardada.isNotBlank() -> nutricionGuardada
            else -> DispensarizacionAutomatica.NUTRICION_SIN_ALTERACION
        }
        // Se calculan de los diagnósticos CIE-10; un dato manual de versiones anteriores se conserva
        // mientras no lo explique (o lo desmienta) un diagnóstico.
        fun calculada(ahora: Boolean, antes: Boolean, guardado: Boolean?) = ahora || (guardado == true && !antes)
        return MiembroFamiliaEntity(
            id = miembro?.id ?: 0,
            fichaId = fichaId,
            grupoEdad = grupoEdad.orEmpty(),
            apellidosNombres = nombres.trim(),
            parentesco = parentesco.trim(),
            fechaNacimiento = fechaNacimiento,
            ocupacion = if (camposPermitidos.ocupacion) ocupacion.trim() else "",
            sexo = if (embarazada) "M" else sexo,
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
                FactoresRiesgoEdad.hayConsumoEn(FactoresRiesgoEdad.codificar(factoresEdad), FactoresRiesgoEdad.bandaPorFecha(fechaNacimiento)),
                FactoresRiesgoEdad.hayConsumoEn(FactoresRiesgoEdad.codificar(factoresEdadIniciales), FactoresRiesgoEdad.bandaPorFecha(fechaNacimiento)),
                miembro?.consumoAlcoholDrogas
            ),
            enfermedadCronica = EstrategiasDesdeCie10.hayGrupoIII(codigosActuales),
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
                FactoresRiesgoEdad.hayViolenciaEn(FactoresRiesgoEdad.codificar(factoresEdad), FactoresRiesgoEdad.bandaPorFecha(fechaNacimiento)),
                FactoresRiesgoEdad.hayViolenciaEn(FactoresRiesgoEdad.codificar(factoresEdadIniciales), FactoresRiesgoEdad.bandaPorFecha(fechaNacimiento)),
                miembro?.victimaViolencia
            ),
            privadoLibertad = riesgoPrioritario == "PRIVADO_LIBERTAD",
            numeroHistoriaClinica = cedula.trim(),
            cedula = cedula.trim(),
            syncId = miembro?.syncId ?: UUID.randomUUID().toString()
        )
    }

    fun armarEmbarazo(): EmbarazadaEntity? {
        if (!embarazada) return null
        val persona = armarMiembro()
        val evaluacion = FactoresObstetricos.evaluar(
            factoresObstetricos, persona, gestas.toIntOrNull(), abortos.toIntOrNull(), diagnosticos.map { it.codigo }
        )
        return EmbarazadaEntity(
            id = embarazoInicial?.id ?: 0,
            fichaId = fichaId,
            apellidosNombres = nombres.trim(),
            fechaUltimaMenstruacion = fum,
            fechaProbableParto = fpp,
            semanasGestacion = semanas.toIntOrNull(),
            dosisDtPrimera = dtPrimera,
            dosisDtSegunda = dtSegunda,
            dosisDtRefuerzo = dtRefuerzo,
            gestas = gestas.toIntOrNull(),
            partos = partos.toIntOrNull(),
            abortos = abortos.toIntOrNull(),
            cesareas = cesareas.toIntOrNull(),
            antecedentesPatologicosObstetricos = antecedentesObstetricos.trim(),
            riesgoObstetrico = evaluacion.riesgoObstetrico,
            factoresObstetricosJson = FactoresObstetricos.codificar(factoresObstetricos),
            syncId = embarazoInicial?.syncId ?: UUID.randomUUID().toString()
        )
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
                    } else if (RolFamiliar.esJefe(parentesco) && hayOtroJefe) {
                        error = "Ya hay un jefe o jefa de familia. Elimínalo primero o elige otro rol."
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
                        onGuardar(armarMiembro(), telefonoJefe.trim(), armarEmbarazo())
                    }
                }
            )

        }
    ) {
        TarjetaFormularioRuralitos(
            Modifier.clickable { cambiarEmbarazada(!embarazada) }.testTag("casilla_embarazada")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = embarazada, onCheckedChange = null)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Paciente embarazada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Al marcarla aparecen los datos del embarazo y el riesgo obstétrico.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        SeccionFormularioRuralitos(
            titulo = "1. Identificación personal",
            desplegable = true,
            progreso = avance(*(listOf(nombres.isNotBlank(), true, cedula.isNotBlank(), fechaNacimiento.isNotBlank()) +
                (if (RolFamiliar.esJefe(parentesco)) listOf(telefonoJefe.isNotBlank()) else emptyList())).toBooleanArray()),
            abiertaInicial = true,
            descripcion = "Apellidos y nombres, parentesco, cédula y fecha de nacimiento."
        ) {
            CampoTexto(nombres, { nombres = it }, "Apellidos y nombres")
            SeleccionTextoMiembro(
                "Rol familiar",
                if (embarazada) RolFamiliar.opcionesMujer else RolFamiliar.opciones,
                parentesco,
                MoradoClinico,
                bloqueadas = if (hayOtroJefe) setOf("JEFE DE FAMILIA", "JEFA DE FAMILIA") else emptySet()
            ) {
                parentesco = it
                RolFamiliar.sexoDelRol(it)?.let { sexoDelRol -> sexo = sexoDelRol }
            }
            if (RolFamiliar.esJefe(parentesco)) {
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
            progreso = avance(*(listOf(true) +
                (if (camposPermitidos.escolaridades.isNotEmpty()) listOf(true) else emptyList()) +
                (if (camposPermitidos.ocupacion) listOf(ocupacion.isNotBlank()) else emptyList())).toBooleanArray()),
            abiertaInicial = false,
            descripcion = "Sexo, escolaridad y ocupación según el grupo de edad."
        ) {
            SeleccionTextoMiembro("Sexo", listOf("H" to "Hombre", "M" to "Mujer"), if (embarazada) "M" else sexo, MoradoClinico, enabled = !embarazada) { sexo = it; marcar("sexo") }
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
            progreso = avance(true, true),
            abiertaInicial = false,
            descripcion = "Vacunas, salud bucal y diagnósticos CIE-10. El estado nutricional se toma de los diagnósticos."
        ) {
            SeleccionBooleanMiembro(
                "Esquema completo de vacunas",
                vacunas
            ) { vacunas = it; marcar("vacunas") }
            SeleccionBooleanMiembro(
                "Salud bucal adecuada",
                saludBucal
            ) { saludBucal = it; marcar("saludBucal") }
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
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("cie_buscar"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            com.ruralitos.app.ui.components.ListaSugerenciasRuralitos(
                sugerencias = resultadosCie10.map {
                    com.ruralitos.app.ui.components.SugerenciaRuralitos(
                        clave = it.codigo, codigo = it.codigo, titulo = it.descripcion,
                        marca = if (EstrategiasDesdeCie10.esGrupoIII(it.codigo)) "Grupo III" else null
                    )
                },
                etiquetaPrueba = { "cie_resultado_$it" },
                onElegir = { codigo ->
                    resultadosCie10.firstOrNull { it.codigo == codigo }?.let { diagnosticos = diagnosticos + it }
                    consultaCie10 = ""
                }
            )
            diagnosticos.forEach { diagnostico ->
                com.ruralitos.app.ui.components.ChipElegidoRuralitos(
                    texto = "${diagnostico.codigo} · ${diagnostico.descripcion}",
                    onQuitar = { diagnosticos = diagnosticos.filterNot { it.codigo == diagnostico.codigo } },
                    etiquetaPrueba = "cie_elegido_${diagnostico.codigo}"
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
        val opcionesFactores = FactoresRiesgoEdad.disponibles(bandaEdad).map {
            OpcionBusqueda(it.codigo, it.etiqueta, grupoIII = it.grupoIII)
        }
        SeccionFormularioRuralitos(
            titulo = "4. Factores de riesgo según la edad",
            desplegable = true,
            abiertaInicial = false,
            descripcion = "Escribe y elige. Cuentan para el Grupo II solo si la persona no tiene nada de un grupo superior."
        ) {
            if (bandaEdad == null) {
                MensajeEstadoRuralitos(
                    titulo = "Falta la fecha de nacimiento",
                    descripcion = "Las opciones dependen de la edad. Selecciona primero la fecha de nacimiento.",
                    color = AzulClinico,
                    simbolo = "i"
                )
            } else {
                Text(
                    bandaEdad.etiqueta,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp).testTag("banda_factores")
                )
                BuscadorDeFactores(
                    titulo = "Escribe un factor de riesgo",
                    opciones = opcionesFactores,
                    elegidos = factoresEdad,
                    onCambio = { factoresEdad = it },
                    prefijoPrueba = "factor"
                )
            }
            // El grupo real de la persona, con todo lo que hay en la ficha: se revisa del IV al I y se queda en el primero que cumple.
            val clasificacion = DispensarizacionAutomatica.clasificar(armarMiembro(), armarEmbarazo())
            val grupoActual = clasificacion.grupo
            val hayFactoresElegidos = opcionesFactores.any { it.codigo in factoresEdad && !it.grupoIII }
            MensajeEstadoRuralitos(
                titulo = if (grupoActual == GrupoDispensarizacion.PENDIENTE) "Evaluación pendiente"
                else "Grupo ${grupoActual.codigo} · ${grupoActual.titulo}",
                descripcion = clasificacion.razones.joinToString("; ") +
                    if (hayFactoresElegidos && grupoActual.prioridad > GrupoDispensarizacion.II.prioridad)
                        ". Los factores elegidos no cambian el grupo porque ya cumple uno superior."
                    else "",
                color = when (grupoActual) {
                    GrupoDispensarizacion.I -> CianRuralitos
                    GrupoDispensarizacion.II -> NaranjaClinico
                    GrupoDispensarizacion.PENDIENTE -> AzulClinico
                    else -> RojoClinico
                },
                simbolo = grupoActual.codigo,
                modifier = Modifier.padding(top = 12.dp).testTag("resumen_factores")
            )
        }
        if (embarazada) {
            val persona = armarMiembro()
            val evaluacion = FactoresObstetricos.evaluar(
                factoresObstetricos, persona, gestas.toIntOrNull(), abortos.toIntOrNull(), diagnosticos.map { it.codigo }
            )
            val detectados = evaluacion.razones.filter { it.automatica }.map { it.codigo }.toSet()
            SeccionFormularioRuralitos(
                titulo = "4.1 Gestación",
                desplegable = true,
                progreso = avance(fum.isNotBlank(), fpp.isNotBlank(), semanas.isNotBlank()),
                abiertaInicial = false,
                descripcion = "Fechas principales y semanas de gestación."
            ) {
                CampoFecha(fum, "Fecha de última menstruación") { calendarioEmbarazo = "fum" }
                CampoFecha(fpp, "Fecha probable del parto") { calendarioEmbarazo = "fpp" }
                CampoEnteroSalud(semanas, { semanas = it }, "Semanas de gestación")
            }
            SeccionFormularioRuralitos(
                titulo = "4.2 Vacunación dT",
                desplegable = true,
                progreso = avance(true, true, true),
                abiertaInicial = false,
                descripcion = "Selecciona Sí o No para cada dosis."
            ) {
                SeleccionBooleanMiembro("Primera dosis", dtPrimera) { dtPrimera = it }
                SeleccionBooleanMiembro("Segunda dosis", dtSegunda) { dtSegunda = it }
                SeleccionBooleanMiembro("Dosis de refuerzo", dtRefuerzo) { dtRefuerzo = it }
            }
            SeccionFormularioRuralitos(
                titulo = "4.3 Antecedentes obstétricos",
                desplegable = true,
                progreso = avance(gestas.isNotBlank(), partos.isNotBlank(), abortos.isNotBlank(), cesareas.isNotBlank()),
                abiertaInicial = false,
                descripcion = "Número de gestas, partos, abortos, cesáreas y antecedentes clínicos."
            ) {
                CampoEnteroSalud(gestas, { gestas = it }, "Gestas")
                CampoEnteroSalud(partos, { partos = it }, "Partos")
                CampoEnteroSalud(abortos, { abortos = it }, "Abortos")
                CampoEnteroSalud(cesareas, { cesareas = it }, "Cesáreas")
                OutlinedTextField(
                    value = antecedentesObstetricos,
                    onValueChange = { antecedentesObstetricos = it },
                    label = { Text("Antecedentes patológicos obstétricos") },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    minLines = 3,
                )
            }
            SeccionFormularioRuralitos(
                titulo = "4.4 Riesgo obstétrico",
                desplegable = true,
                progreso = avance(true),
                abiertaInicial = false,
                descripcion = "Escribe y elige los criterios de la escala de riesgo obstétrico (Riesgo 1, 2 y 3). Algunos se detectan solos con los datos de la ficha."
            ) {
                BuscadorDeFactores(
                    titulo = "Escribe un criterio de riesgo obstétrico",
                    opciones = FactoresObstetricos.todos.map {
                        OpcionBusqueda(it.codigo, it.etiqueta, "Riesgo ${it.nivel}", grupoIII = it.cronico)
                    },
                    elegidos = factoresObstetricos,
                    onCambio = { factoresObstetricos = it },
                    prefijoPrueba = "criterio",
                    automaticos = detectados
                )
                armarEmbarazo()?.let { ResultadoRiesgoObstetrico(evaluacion, persona, it) }
            }
        }
        SeccionFormularioRuralitos(
            titulo = "5. Discapacidad",
            desplegable = true,
            progreso = avance(*(listOf(true) +
                (if (tipoDiscapacidad != "NINGUNA") listOf(porcentajeDiscapacidad.isNotBlank(), true) else emptyList())).toBooleanArray()),
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
            progreso = avance(true),
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
            progreso = avance(true),
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
    calendarioEmbarazo?.let { campo ->
        SelectorFechaDialog(
            onFechaSeleccionada = {
                if (campo == "fum") fum = it else fpp = it
                calendarioEmbarazo = null
            },
            onCerrar = { calendarioEmbarazo = null }
        )
    }
}

@Composable
private fun SeleccionTextoMiembro(
    titulo: String,
    opciones: List<Pair<String, String>>,
    seleccion: String,
    color: Color,
    enabled: Boolean = true,
    bloqueadas: Set<String> = emptySet(),
    onSeleccion: (String) -> Unit
) {
    SelectorDesplegableMiembro(
        titulo = titulo,
        opciones = opciones,
        seleccion = seleccion,
        color = color,
        enabled = enabled,
        bloqueadas = bloqueadas,
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
    bloqueadas: Set<String> = emptySet(),
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
                val bloqueada = valor in bloqueadas && valor != seleccion
                ItemMenuRuralitos(
                    text = { Text("$etiqueta${if (bloqueada) " (ya registrado)" else ""}") },
                    enabled = !bloqueada,
                    seleccionado = valor == seleccion,
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
                text = { Text("Ninguno") },
                casilla = seleccion.isEmpty(),
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
                            Text(etiqueta)
                        }
                    },
                    casilla = valor in seleccion,
                    onClick = {
                        onSeleccion(if (valor in seleccion) seleccion - valor else seleccion + valor)
                    }
                )
            }
            ItemMenuRuralitos(
                text = { Text("Listo", color = CianRuralitos, fontWeight = FontWeight.SemiBold) },
                onClick = { expandido = false }
            )
        }
    }
}
