package com.ruralitos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.EaisSalaEntity
import com.ruralitos.app.data.local.entity.SalaEntity
import com.ruralitos.app.data.local.entity.TerritorioSalaEntity
import com.ruralitos.app.data.remote.SupabaseApi
import com.ruralitos.app.data.sync.SincronizadorSalas
import com.ruralitos.app.data.sync.ProgramadorSincronizacion
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.SubmenuRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.ruralitos.app.R
import kotlinx.coroutines.launch

private enum class SeccionSala(val etiqueta: String) {
    CENTROS("Centros"),
    BARRIOS("EAIS y barrios"),
    ACCESOS("Compartir acceso")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SalaScreen(
    database: RuralitosDatabase,
    supabase: SupabaseApi,
    onAgregarCentro: () -> Unit,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var seccion by remember { mutableStateOf(SeccionSala.CENTROS) }
    var recarga by remember { mutableIntStateOf(0) }
    var salas by remember { mutableStateOf<List<SalaEntity>>(emptyList()) }
    var eais by remember { mutableStateOf<List<EaisSalaEntity>>(emptyList()) }
    var territorios by remember { mutableStateOf<List<TerritorioSalaEntity>>(emptyList()) }
    var salaId by remember { mutableStateOf(supabase.organizacionGuardada().orEmpty()) }
    var eaisId by remember { mutableStateOf("") }
    var territorioId by remember { mutableStateOf("") }
    var numeroEais by remember { mutableStateOf("") }
    var eaisEditandoId by remember { mutableStateOf<String?>(null) }
    var eaisAEliminar by remember { mutableStateOf<EaisSalaEntity?>(null) }
    var nombreTerritorio by remember { mutableStateOf("") }
    var territorioEditandoId by remember { mutableStateOf<String?>(null) }
    var territorioAEliminar by remember { mutableStateOf<TerritorioSalaEntity?>(null) }
    var alcance by remember { mutableStateOf("SALA") }
    var permiso by remember { mutableStateOf("EDITOR") }
    var correo by remember { mutableStateOf("") }
    var codigoIngreso by remember { mutableStateOf("") }
    var codigoGenerado by remember { mutableStateOf("") }
    var procesando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (supabase.hayInternet()) {
            runCatching { SincronizadorSalas.actualizar(database, supabase) }
                .onFailure { mensaje = it.message ?: "No se pudo actualizar la Sala." }
        }
        salas = database.salaDao().listarSalas()
        if (salas.none { it.organizacionId == salaId }) {
            salaId = salas.firstOrNull()?.organizacionId.orEmpty()
        }
        recarga++
    }
    LaunchedEffect(salaId, recarga) {
        if (salaId.isNotBlank()) {
            supabase.guardarOrganizacionActiva(salaId)
            eais = database.salaDao().listarEais(salaId)
        } else eais = emptyList()
        if (eais.none { it.id == eaisId }) eaisId = eais.firstOrNull()?.id.orEmpty()
    }
    LaunchedEffect(eaisId, recarga) {
        territorios = if (eaisId.isBlank()) emptyList() else database.salaDao().listarTerritorios(eaisId)
        if (territorios.none { it.id == territorioId }) {
            territorioId = territorios.firstOrNull()?.id.orEmpty()
        }
    }

    val sala = salas.firstOrNull { it.organizacionId == salaId }
    val eaisElegido = eais.firstOrNull { it.id == eaisId }
    val territorio = territorios.firstOrNull { it.id == territorioId }
    val puedeAdministrar = sala?.permiso == "ADMINISTRADOR" || sala?.rol == "ADMINISTRADOR"

    fun ejecutar(
        mensajeExito: String = "Información actualizada correctamente.",
        sincronizarSalas: Boolean = false,
        accion: suspend () -> Unit
    ) {
        if (procesando) return
        procesando = true
        mensaje = null
        scope.launch {
            runCatching {
                accion()
                if (sincronizarSalas && supabase.hayInternet()) {
                    SincronizadorSalas.actualizar(database, supabase)
                    salas = database.salaDao().listarSalas()
                }
            }.onSuccess {
                recarga++
                mensaje = mensajeExito
                if (supabase.hayInternet()) ProgramadorSincronizacion.ejecutarAhora(context)
            }.onFailure {
                mensaje = it.message ?: "No se pudo completar la operación."
            }
            procesando = false
        }
    }

    territorioAEliminar?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!procesando) territorioAEliminar = null },
            title = { Text("Eliminar ${item.etiqueta.lowercase()}") },
            text = {
                Text(
                    "${item.nombre} dejará de aparecer para nuevas fichas. " +
                        "Las fichas históricas conservarán su información."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !procesando,
                    onClick = {
                        territorioAEliminar = null
                        ejecutar("Barrio eliminado correctamente.") {
                            val remoto = supabase.desactivarTerritorio(salaId, item.id)
                            database.salaDao().guardarTerritorio(
                                item.copy(
                                    tipo = remoto.tipo,
                                    nombre = remoto.nombre,
                                    activo = remoto.activo,
                                    actualizadoEn = System.currentTimeMillis()
                                )
                            )
                            if (territorioId == item.id) territorioId = ""
                            if (territorioEditandoId == item.id) {
                                territorioEditandoId = null
                                nombreTerritorio = ""
                            }
                        }
                    }
                ) { Text("Sí, eliminar", color = RojoClinico, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    enabled = !procesando,
                    onClick = { territorioAEliminar = null }
                ) { Text("Cancelar") }
            }
        )
    }

    eaisAEliminar?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!procesando) eaisAEliminar = null },
            title = { Text("Eliminar ${item.nombre}") },
            text = { Text("El EAIS dejará de estar disponible para nuevas fichas. Las fichas existentes no se borran y podrán seguir editándose.") },
            confirmButton = {
                TextButton(enabled = !procesando, onClick = {
                    eaisAEliminar = null
                    ejecutar("EAIS desactivado correctamente. Las fichas históricas se conservaron.") {
                        val remoto = supabase.desactivarEais(salaId, item.id)
                        database.salaDao().guardarEais(item.copy(activo = remoto.activo, actualizadoEn = System.currentTimeMillis()))
                        database.salaDao().desactivarTerritoriosDeEais(item.id)
                        eaisId = ""
                        territorioId = ""
                        eaisEditandoId = null
                        numeroEais = ""
                    }
                }) { Text("Sí, desactivar", color = RojoClinico, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(enabled = !procesando, onClick = { eaisAEliminar = null }) { Text("Cancelar") } }
        )
    }
// Estados exclusivamente visuales de paneles desplegables.
    // No modifican la lógica de negocio ni las operaciones existentes.
    var centrosAbierto by remember { mutableStateOf(true) }
    var eaisAbierto by remember { mutableStateOf(true) }
    var barriosAbierto by remember { mutableStateOf(false) }
    var compartirAbierto by remember { mutableStateOf(true) }
    var ingresarCodigoAbierto by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FBFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Se conserva exactamente la misma imagen ya utilizada por Ruralitos.
            Image(
                painter = painterResource(R.drawable.ruralitos_paisaje_cabecera),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(178.dp),
                contentScale = ContentScale.Crop
            )

            // Contenedor blanco superpuesto al paisaje, con la forma escogida por el usuario:
            // sin onda, solo esquinas superiores grandes y redondeadas.
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp),
                color = Color(0xFFF9FCFF),
                shape = RoundedCornerShape(
                    topStart = 30.dp,
                    topEnd = 30.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                ),
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 22.dp,
                            bottom = 22.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = seccion.etiqueta.uppercase(),
                        color = MoradoClinico,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Mis Salas",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Centros, barrios y accesos organizados por sección.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TabsSalaModernas(
                        seleccionada = seccion,
                        onSeleccionar = { seccion = it }
                    )

                    when (seccion) {
                        SeccionSala.CENTROS -> {
                            PanelDesplegableSala(
                                titulo = "Centros de salud vinculados",
                                descripcion = "Puedes trabajar en más de un centro con la misma cuenta.",
                                simbolo = "S",
                                color = AzulClinico,
                                abierto = centrosAbierto,
                                onCambiar = { centrosAbierto = !centrosAbierto }
                            ) {
                                if (salas.isEmpty()) {
                                    Text(
                                        "Todavía no hay centros vinculados.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    salas.forEach { item ->
                                        FilterChip(
                                            selected = item.organizacionId == salaId,
                                            onClick = { salaId = item.organizacionId },
                                            label = {
                                                Text(
                                                    item.nombreCentroSalud.ifBlank { item.nombreSala },
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AzulClinico.copy(alpha = 0.13f),
                                                selectedLabelColor = AzulClinico
                                            )
                                        )
                                    }
                                }

                                BotonPrincipalRuralitos(
                                    texto = "Agregar otro centro de salud",
                                    descripcion = "Buscar la unidad y crear una Sala nueva",
                                    onClick = onAgregarCentro,
                                    color = AzulClinico,
                                    modifier = Modifier.padding(top = 8.dp)
                                )

                                sala?.let {
                                    MensajeEstadoRuralitos(
                                        titulo = it.nombreCentroSalud.ifBlank { it.nombreSala },
                                        descripcion = listOf(
                                            "Código UO: ${it.codigoUo.ifBlank { "sin código" }}",
                                            it.canton,
                                            "Permiso: ${it.permiso.lowercase()}"
                                        ).filter(String::isNotBlank).joinToString(" · "),
                                        color = CianRuralitos,
                                        simbolo = "S",
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }

                        SeccionSala.BARRIOS -> {
                            PanelDesplegableSala(
                                titulo = "EAIS del centro",
                                descripcion = "Selecciona un equipo para ver y administrar sus barrios.",
                                simbolo = "E",
                                color = CianRuralitos,
                                abierto = eaisAbierto,
                                onCambiar = { eaisAbierto = !eaisAbierto }
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    eais.forEach { item ->
                                        FilterChip(
                                            selected = item.id == eaisId,
                                            onClick = { eaisId = item.id },
                                            label = {
                                                Text(
                                                    item.nombre,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CianRuralitos.copy(alpha = 0.13f),
                                                selectedLabelColor = CianRuralitos
                                            )
                                        )
                                    }
                                }

                                if (puedeAdministrar) {
                                    if (eaisElegido != null && eaisEditandoId == null) {
                                        BotonSecundarioRuralitos(
                                            texto = "Modificar ${eaisElegido.nombre}",
                                            onClick = {
                                                eaisEditandoId = eaisElegido.id
                                                numeroEais = eaisElegido.numero.toString()
                                            },
                                            modifier = Modifier.padding(top = 8.dp)
                                        )

                                        BotonPrincipalRuralitos(
                                            texto = "Eliminar ${eaisElegido.nombre}",
                                            color = RojoClinico,
                                            onClick = { eaisAEliminar = eaisElegido },
                                            enabled = !procesando,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = numeroEais,
                                        onValueChange = { numeroEais = it.filter(Char::isDigit).take(3) },
                                        label = {
                                            Text(
                                                if (eaisEditandoId == null) {
                                                    "Número del nuevo EAIS"
                                                } else {
                                                    "Nuevo número del EAIS"
                                                }
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    )

                                    BotonPrincipalRuralitos(
                                        texto = if (eaisEditandoId == null) {
                                            "Agregar EAIS"
                                        } else {
                                            "Guardar número del EAIS"
                                        },
                                        onClick = {
                                            val numero = numeroEais.toIntOrNull()
                                                ?: return@BotonPrincipalRuralitos
                                            val idEditado = eaisEditandoId
                                            ejecutar(
                                                if (idEditado == null) {
                                                    "EAIS agregado correctamente."
                                                } else {
                                                    "EAIS modificado correctamente."
                                                }
                                            ) {
                                                val remoto = if (idEditado == null) {
                                                    supabase.crearEais(salaId, numero)
                                                } else {
                                                    supabase.actualizarEais(
                                                        salaId,
                                                        idEditado,
                                                        numero
                                                    )
                                                }

                                                database.salaDao().guardarEais(
                                                    EaisSalaEntity(
                                                        id = remoto.id,
                                                        salaId = remoto.organizacionId,
                                                        numero = remoto.numero,
                                                        activo = remoto.activo,
                                                        actualizadoEn = System.currentTimeMillis()
                                                    )
                                                )

                                                eaisId = remoto.id
                                                numeroEais = ""
                                                eaisEditandoId = null
                                            }
                                        },
                                        enabled = !procesando && numeroEais.toIntOrNull() != null,
                                        color = CianRuralitos,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )

                                    if (eaisEditandoId != null) {
                                        BotonSecundarioRuralitos(
                                            texto = "Cancelar modificación",
                                            onClick = {
                                                eaisEditandoId = null
                                                numeroEais = ""
                                            },
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }

                            PanelDesplegableSala(
                                titulo = "Barrios",
                                descripcion = eaisElegido?.let {
                                    "Barrios asignados al ${it.nombre}."
                                } ?: "Selecciona primero un EAIS.",
                                simbolo = "B",
                                color = NaranjaClinico,
                                abierto = barriosAbierto,
                                onCambiar = { barriosAbierto = !barriosAbierto }
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    territorios.forEach { item ->
                                        FilterChip(
                                            selected = item.id == territorioId,
                                            onClick = { territorioId = item.id },
                                            label = {
                                                Text(
                                                    "${item.etiqueta}: ${item.nombre}",
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AzulClinico.copy(alpha = 0.10f),
                                                selectedLabelColor = AzulClinico
                                            )
                                        )
                                    }
                                }

                                if (
                                    puedeAdministrar &&
                                    territorio != null &&
                                    territorioEditandoId == null
                                ) {
                                    MensajeEstadoRuralitos(
                                        titulo = "Barrio: ${territorio.nombre}",
                                        descripcion = "Puedes corregir su nombre o desactivarlo.",
                                        color = NaranjaClinico,
                                        simbolo = "T",
                                        modifier = Modifier.padding(top = 8.dp)
                                    )

                                    BotonSecundarioRuralitos(
                                        texto = "Editar barrio seleccionado",
                                        descripcion = "Corregir el nombre del barrio",
                                        enabled = !procesando,
                                        onClick = {
                                            territorioEditandoId = territorio.id
                                            nombreTerritorio = territorio.nombre
                                        },
                                        modifier = Modifier.padding(top = 8.dp)
                                    )

                                    BotonPrincipalRuralitos(
                                        texto = "Eliminar barrio seleccionado",
                                        descripcion = "Ocultarlo sin afectar las fichas históricas",
                                        enabled = !procesando,
                                        color = RojoClinico,
                                        onClick = {
                                            territorioAEliminar = territorio
                                        },
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }

                                if (puedeAdministrar && eaisElegido != null) {
                                    OutlinedTextField(
                                        value = nombreTerritorio,
                                        onValueChange = {
                                            nombreTerritorio = it.take(80)
                                        },
                                        label = {
                                            Text(
                                                if (territorioEditandoId == null) {
                                                    "Nombre del barrio"
                                                } else {
                                                    "Corregir nombre"
                                                }
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    )

                                    BotonPrincipalRuralitos(
                                        texto = if (territorioEditandoId == null) {
                                            "Agregar barrio"
                                        } else {
                                            "Guardar cambios del barrio"
                                        },
                                        descripcion = if (territorioEditandoId == null) {
                                            "Añadirlo al ${eaisElegido.nombre}"
                                        } else {
                                            "Conservar las fichas asociadas y corregir este registro"
                                        },
                                        onClick = {
                                            val idEditado = territorioEditandoId
                                            ejecutar(
                                                if (idEditado == null) {
                                                    "Barrio agregado correctamente."
                                                } else {
                                                    "Barrio editado correctamente."
                                                }
                                            ) {
                                                val remoto = if (idEditado == null) {
                                                    supabase.crearTerritorio(
                                                        salaId,
                                                        eaisId,
                                                        TerritorioSalaEntity.TIPO_BARRIO,
                                                        nombreTerritorio
                                                    )
                                                } else {
                                                    supabase.actualizarTerritorio(
                                                        salaId,
                                                        idEditado,
                                                        TerritorioSalaEntity.TIPO_BARRIO,
                                                        nombreTerritorio
                                                    )
                                                }

                                                database.salaDao().guardarTerritorio(
                                                    TerritorioSalaEntity(
                                                        id = remoto.id,
                                                        salaId = remoto.organizacionId,
                                                        eaisId = remoto.eaisId,
                                                        tipo = remoto.tipo,
                                                        nombre = remoto.nombre,
                                                        activo = remoto.activo,
                                                        actualizadoEn = System.currentTimeMillis()
                                                    )
                                                )

                                                territorioId = remoto.id
                                                territorioEditandoId = null
                                                nombreTerritorio = ""
                                            }
                                        },
                                        enabled = !procesando &&
                                            nombreTerritorio.trim().length >= 2,
                                        color = if (territorioEditandoId == null) {
                                            NaranjaClinico
                                        } else {
                                            CianRuralitos
                                        },
                                        modifier = Modifier.padding(top = 8.dp)
                                    )

                                    if (territorioEditandoId != null) {
                                        BotonSecundarioRuralitos(
                                            texto = "Cancelar edición",
                                            descripcion = "No aplicar cambios al barrio",
                                            enabled = !procesando,
                                            onClick = {
                                                territorioEditandoId = null
                                                nombreTerritorio = ""
                                            },
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        SeccionSala.ACCESOS -> {
                            PanelDesplegableSala(
                                titulo = "Compartir acceso con otro usuario",
                                descripcion = "El código permite consultar o editar solo el nivel que selecciones.",
                                simbolo = "↗",
                                color = MoradoClinico,
                                abierto = compartirAbierto,
                                onCambiar = {
                                    compartirAbierto = !compartirAbierto
                                }
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "SALA" to "Centro completo",
                                        "EAIS" to "Solo EAIS",
                                        "TERRITORIO" to "Solo barrio"
                                    ).forEach { (valor, etiqueta) ->
                                        FilterChip(
                                            selected = alcance == valor,
                                            onClick = { alcance = valor },
                                            enabled = valor == "SALA" ||
                                                eaisElegido != null &&
                                                (
                                                    valor != "TERRITORIO" ||
                                                    territorio != null
                                                ),
                                            label = {
                                                Text(
                                                    etiqueta,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MoradoClinico.copy(alpha = 0.14f),
                                                selectedLabelColor = MoradoClinico
                                            )
                                        )
                                    }
                                }

                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "LECTOR" to "Solo lectura",
                                        "EDITOR" to "Puede editar"
                                    ).forEach { (valor, etiqueta) ->
                                        FilterChip(
                                            selected = permiso == valor,
                                            onClick = { permiso = valor },
                                            label = {
                                                Text(
                                                    etiqueta,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AzulClinico.copy(alpha = 0.12f),
                                                selectedLabelColor = AzulClinico
                                            )
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = correo,
                                    onValueChange = { correo = it.take(120) },
                                    label = {
                                        Text("Correo autorizado (opcional)")
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp)
                                )

                                BotonPrincipalRuralitos(
                                    texto = if (procesando) {
                                        "Generando código…"
                                    } else {
                                        "Generar código de verificación"
                                    },
                                    onClick = {
                                        ejecutar {
                                            val creado =
                                                supabase.crearCodigoAcceso(
                                                    organizacionId = salaId,
                                                    alcance = alcance,
                                                    eaisId = eaisId.takeIf {
                                                        alcance != "SALA"
                                                    },
                                                    territorioId =
                                                        territorioId.takeIf {
                                                            alcance == "TERRITORIO"
                                                        },
                                                    permiso = permiso,
                                                    correo = correo
                                                )
                                            codigoGenerado = creado.codigo
                                        }
                                    },
                                    enabled = puedeAdministrar && !procesando,
                                    color = MoradoClinico,
                                    modifier = Modifier.padding(top = 8.dp)
                                )

                                if (codigoGenerado.isNotBlank()) {
                                    MensajeEstadoRuralitos(
                                        titulo = codigoGenerado,
                                        descripcion = "Comparte este código con el usuario autorizado. Caduca en 7 días.",
                                        color = MoradoClinico,
                                        simbolo = "#",
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }

                            PanelDesplegableSala(
                                titulo = "Ingresar con un código",
                                descripcion = "Únete a una Sala o recibe acceso a un EAIS o barrio.",
                                simbolo = "#",
                                color = AzulClinico,
                                abierto = ingresarCodigoAbierto,
                                onCambiar = {
                                    ingresarCodigoAbierto =
                                        !ingresarCodigoAbierto
                                }
                            ) {
                                OutlinedTextField(
                                    value = codigoIngreso,
                                    onValueChange = {
                                        codigoIngreso = it
                                            .uppercase()
                                            .filter(Char::isLetterOrDigit)
                                            .take(32)
                                    },
                                    label = {
                                        Text(
                                            "Código de asignación o verificación"
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                BotonPrincipalRuralitos(
                                    texto = "Verificar y agregar acceso",
                                    onClick = {
                                        ejecutar(
                                            mensajeExito =
                                                "Acceso agregado correctamente.",
                                            sincronizarSalas = true
                                        ) {
                                            supabase.aceptarInvitacion(
                                                codigoIngreso
                                            )
                                            codigoIngreso = ""
                                        }
                                    },
                                    enabled = !procesando &&
                                        codigoIngreso.length >= 8,
                                    color = AzulClinico,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    mensaje?.let {
                        MensajeEstadoRuralitos(
                            titulo = if (
                                it.contains("correctamente", true)
                            ) {
                                "Listo"
                            } else {
                                "Aviso"
                            },
                            descripcion = it,
                            color = if (
                                it.contains("correctamente", true)
                            ) {
                                CianRuralitos
                            } else {
                                NaranjaClinico
                            },
                            simbolo = if (
                                it.contains("correctamente", true)
                            ) {
                                "✓"
                            } else {
                                "!"
                            }
                        )
                    }

                    BotonSecundarioRuralitos(
                        texto = "Regresar al inicio",
                        onClick = onRegresar
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TabsSalaModernas(
    seleccionada: SeccionSala,
    onSeleccionar: (SeccionSala) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 3.dp,
        border = BorderStroke(
            1.dp,
            MoradoClinico.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SeccionSala.entries.forEach { opcion ->
                val activa = seleccionada == opcion

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .clickable {
                            onSeleccionar(opcion)
                        },
                    color = if (activa) {
                        MoradoClinico
                    } else {
                        Color.Transparent
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opcion.etiqueta,
                            color = if (activa) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelDesplegableSala(
    titulo: String,
    descripcion: String,
    simbolo: String,
    color: Color,
    abierto: Boolean,
    onCambiar: () -> Unit,
    contenido: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 3.dp,
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCambiar)
                    .padding(
                        horizontal = 14.dp,
                        vertical = 14.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    color = color.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = simbolo,
                            color = color,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Surface(
                    modifier = Modifier.size(36.dp),
                    color = color.copy(alpha = 0.07f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (abierto) "⌃" else "⌄",
                            color = color,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (abierto) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 14.dp,
                            end = 14.dp,
                            bottom = 14.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    contenido()
                }
            }
        }
    }
}
