package com.ruralitos.app.ui.screens

import com.ruralitos.app.ui.components.ItemMenuRuralitos
import com.ruralitos.app.ui.components.MenuDesplegableRuralitos
import com.ruralitos.app.ui.components.BotonSelectorRuralitos
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.MortalidadFamiliarEntity
import com.ruralitos.app.domain.DispensarizacionAutomatica
import com.ruralitos.app.domain.GrupoEdadFamiliar
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
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private data class DatosPersonaSalud(
    val nombres: String,
    val cedula: String,
    val fechaNacimiento: String,
    val parentesco: String,
    val sexo: String,
    val escolaridad: String,
    val ocupacion: String
)

@Composable
fun SaludFamiliarScreen(
    fichaId: Long,
    onContinuar: () -> Unit,
    onSalir: () -> Unit,
    textoRegresar: String = "Volver al panel de la ficha",
    descripcionRegresar: String = "Conservar los datos y salir de esta sección"
) {
    val context = LocalContext.current
    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val scope = rememberCoroutineScope()
    val embarazadas by database.fichaContenidoDao()
        .listarEmbarazadas(fichaId)
        .collectAsState(initial = emptyList())
    val mortalidad by database.fichaContenidoDao()
        .listarMortalidad(fichaId)
        .collectAsState(initial = emptyList())
    val miembros by database.fichaContenidoDao()
        .listarMiembros(fichaId)
        .collectAsState(initial = emptyList())
    var modo by remember { mutableStateOf("lista") }
    var embarazadaEditando by remember { mutableStateOf<EmbarazadaEntity?>(null) }
    var mortalidadEditando by remember { mutableStateOf<MortalidadFamiliarEntity?>(null) }
    var embarazadaEliminar by remember { mutableStateOf<EmbarazadaEntity?>(null) }
    var mortalidadEliminar by remember { mutableStateOf<MortalidadFamiliarEntity?>(null) }

    if (modo == "embarazada") {
        FormularioEmbarazadaScreen(
            fichaId = fichaId,
            item = embarazadaEditando,
            miembros = miembros,
            onGuardar = { item, persona ->
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            guardarPersonaDesdeSalud(
                                database = database,
                                fichaId = fichaId,
                                datos = persona,
                                miembros = miembros,
                                nombreAnterior = embarazadaEditando?.apellidosNombres
                            )
                            if (item.id == 0L) database.fichaContenidoDao().guardarEmbarazada(item)
                            else database.fichaContenidoDao().actualizarEmbarazada(item)
                        }
                    }.onSuccess {
                        modo = "lista"
                        embarazadaEditando = null
                    }.onFailure {
                        Toast.makeText(context, "No se pudo guardar el embarazo.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCancelar = {
                modo = "lista"
                embarazadaEditando = null
            }
        )
        return
    }

    if (modo == "mortalidad") {
        FormularioMortalidadScreen(
            fichaId = fichaId,
            item = mortalidadEditando,
            onGuardar = { item ->
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            if (item.id == 0L) database.fichaContenidoDao().guardarMortalidad(item)
                            else database.fichaContenidoDao().actualizarMortalidad(item)
                        }
                    }.onSuccess {
                        modo = "lista"
                        mortalidadEditando = null
                    }.onFailure {
                        Toast.makeText(context, "No se pudo guardar el registro de mortalidad.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCancelar = {
                modo = "lista"
                mortalidadEditando = null
            }
        )
        return
    }

    embarazadaEliminar?.let { seleccionado ->
        ConfirmarEliminacionSalud(
            titulo = "Eliminar registro de embarazo",
            descripcion = "Se eliminará el registro de ${seleccionado.apellidosNombres}.",
            onConfirmar = {
                embarazadaEliminar = null
                scope.launch(Dispatchers.IO) {
                    database.fichaContenidoDao().eliminarEmbarazada(seleccionado)
                }
            },
            onCancelar = { embarazadaEliminar = null }
        )
    }
    mortalidadEliminar?.let { seleccionado ->
        ConfirmarEliminacionSalud(
            titulo = "Eliminar registro de mortalidad",
            descripcion = "Se eliminará el registro de ${seleccionado.nombre}.",
            onConfirmar = {
                mortalidadEliminar = null
                scope.launch(Dispatchers.IO) {
                    database.fichaContenidoDao().eliminarMortalidad(seleccionado)
                }
            },
            onCancelar = { mortalidadEliminar = null }
        )
    }

    PantallaListaRuralitos(
        titulo = "Embarazo y mortalidad",
        descripcion = "Registra por separado los embarazos actuales y los fallecimientos familiares de los últimos cinco años.",
        paso = 3,
        totalPasos = 9,
        etiquetaPaso = "Salud y evaluación",
        onVolver = onSalir,
        barraAccion = {
                BotonPrincipalRuralitos(
                    texto = "Guardar información de esta sección",
                    descripcion = "Los registros de salud ya están guardados",
                    color = AzulClinico,
                    onClick = onContinuar
                )

        }
    ) {
        item {
            SeccionFormularioRuralitos(
                titulo = "Embarazos registrados",
                descripcion = "${embarazadas.size} registro(s). Incluye fechas, semanas, vacunas y antecedentes obstétricos."
            ) {
                BotonPrincipalRuralitos(
                    texto = "Agregar un embarazo",
                    descripcion = "Abrir formulario obstétrico completo",
                    color = CianRuralitos,
                    onClick = {
                        embarazadaEditando = null
                        modo = "embarazada"
                    }
                )
                if (embarazadas.isEmpty()) {
                    MensajeEstadoRuralitos(
                        titulo = "Sin embarazos registrados",
                        descripcion = "Esta sección puede quedar vacía cuando no corresponda.",
                        color = NaranjaClinico,
                        simbolo = "0",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
        items(embarazadas, key = { "e${it.id}" }) { item ->
            TarjetaRegistroRuralitos(
                titulo = item.apellidosNombres.ifBlank { "Persona sin nombre" },
                descripcion = "Fecha probable de parto: ${item.fechaProbableParto.ifBlank { "Sin registrar" }}",
                simbolo = "E",
                color = NaranjaClinico,
                onEditar = {
                    embarazadaEditando = item
                    modo = "embarazada"
                },
                onEliminar = { embarazadaEliminar = item }
            ) {
                Text(
                    "Semanas de gestación: ${item.semanasGestacion?.toString() ?: "Sin registrar"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SeccionFormularioRuralitos(
                titulo = "Mortalidad familiar",
                descripcion = "${mortalidad.size} registro(s). Solo se incluyen fallecimientos ocurridos durante los últimos cinco años."
            ) {
                BotonPrincipalRuralitos(
                    texto = "Agregar un fallecimiento",
                    descripcion = "Registrar persona, parentesco, edad y causa",
                    color = RojoClinico,
                    onClick = {
                        mortalidadEditando = null
                        modo = "mortalidad"
                    }
                )
                if (mortalidad.isEmpty()) {
                    MensajeEstadoRuralitos(
                        titulo = "Sin mortalidad registrada",
                        descripcion = "Esta sección puede quedar vacía cuando no existan antecedentes recientes.",
                        color = AzulClinico,
                        simbolo = "0",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
        items(mortalidad, key = { "m${it.id}" }) { item ->
            TarjetaRegistroRuralitos(
                titulo = item.nombre.ifBlank { "Persona sin nombre" },
                descripcion = listOf(item.parentesco, item.causa)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                simbolo = "M",
                color = RojoClinico,
                onEditar = {
                    mortalidadEditando = item
                    modo = "mortalidad"
                },
                onEliminar = { mortalidadEliminar = item }
            ) {
                Text(
                    "Edad al fallecer: ${item.edadAlFallecer?.toString() ?: "Sin registrar"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private suspend fun guardarPersonaDesdeSalud(
    database: RuralitosDatabase,
    fichaId: Long,
    datos: DatosPersonaSalud,
    miembros: List<MiembroFamiliaEntity>,
    nombreAnterior: String?
) {
    val grupo = requireNotNull(GrupoEdadFamiliar.calcular(datos.fechaNacimiento))
    val existente = miembros.firstOrNull {
        datos.cedula.isNotBlank() && it.cedula == datos.cedula
    } ?: miembros.firstOrNull {
        it.apellidosNombres.equals(nombreAnterior ?: datos.nombres, ignoreCase = true)
    }
    val actualizado = existente?.copy(
        grupoEdad = grupo,
        apellidosNombres = datos.nombres,
        parentesco = datos.parentesco,
        fechaNacimiento = datos.fechaNacimiento,
        ocupacion = datos.ocupacion,
        sexo = datos.sexo,
        escolaridad = datos.escolaridad,
        numeroHistoriaClinica = datos.cedula,
        cedula = datos.cedula
    ) ?: MiembroFamiliaEntity(
        fichaId = fichaId,
        grupoEdad = grupo,
        apellidosNombres = datos.nombres,
        parentesco = datos.parentesco,
        fechaNacimiento = datos.fechaNacimiento,
        ocupacion = datos.ocupacion,
        sexo = datos.sexo,
        escolaridad = datos.escolaridad,
        vacunasCompletas = false,
        saludBucalAdecuada = false,
        estadoNutricional = DispensarizacionAutomatica.NUTRICION_SIN_ALTERACION,
        hipertensionArterial = false,
        diabetesMellitus = false,
        tuberculosis = false,
        problemaSaludMental = false,
        consumoAlcoholDrogas = false,
        enfermedadCronica = false,
        discapacidadVisual = false,
        discapacidadAuditiva = false,
        discapacidadLenguaje = false,
        discapacidadFisica = false,
        discapacidadIntelectual = false,
        discapacidadPsicosocial = false,
        cuidadosPaliativos = false,
        vih = false,
        eventoSalud = false,
        casoConfirmado = false,
        casoSospechosoUno = false,
        casoSospechosoDos = false,
        prestadorComunitario = false,
        parteroAncestral = false,
        sabiduriaAncestral = false,
        necesitaAyudaTecnica = false,
        enfermedadCronicaDescompensada = false,
        riesgoGenetico = false,
        victimaViolencia = false,
        privadoLibertad = false,
        numeroHistoriaClinica = datos.cedula,
        cedula = datos.cedula,
        syncId = UUID.randomUUID().toString()
    )
    if (actualizado.id == 0L) database.fichaContenidoDao().guardarMiembro(actualizado)
    else database.fichaContenidoDao().actualizarMiembro(actualizado)
}

@Composable
private fun ConfirmarEliminacionSalud(
    titulo: String,
    descripcion: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text("$descripcion Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text("Sí, eliminar", color = RojoClinico, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Conservar registro") }
        }
    )
}

@Composable
private fun FormularioEmbarazadaScreen(
    fichaId: Long,
    item: EmbarazadaEntity?,
    miembros: List<MiembroFamiliaEntity>,
    onGuardar: (EmbarazadaEntity, DatosPersonaSalud) -> Unit,
    onCancelar: () -> Unit
) {
    val personaInicial = remember(item, miembros) {
        miembros.firstOrNull { it.apellidosNombres.equals(item?.apellidosNombres, ignoreCase = true) }
    }
    var nombres by remember { mutableStateOf(personaInicial?.apellidosNombres ?: item?.apellidosNombres.orEmpty()) }
    var cedula by remember { mutableStateOf(personaInicial?.cedula.orEmpty()) }
    var fechaNacimiento by remember { mutableStateOf(personaInicial?.fechaNacimiento.orEmpty()) }
    var parentesco by remember { mutableStateOf(personaInicial?.parentesco.orEmpty().ifBlank { "CÓNYUGE/PAREJA" }) }
    var escolaridad by remember { mutableStateOf(personaInicial?.escolaridad.orEmpty().ifBlank { "SIN" }) }
    var ocupacion by remember { mutableStateOf(personaInicial?.ocupacion.orEmpty()) }
    var fum by remember { mutableStateOf(item?.fechaUltimaMenstruacion.orEmpty()) }
    var fpp by remember { mutableStateOf(item?.fechaProbableParto.orEmpty()) }
    var semanas by remember { mutableStateOf(item?.semanasGestacion?.toString().orEmpty()) }
    var primera by remember { mutableStateOf(item?.dosisDtPrimera ?: false) }
    var segunda by remember { mutableStateOf(item?.dosisDtSegunda ?: false) }
    var refuerzo by remember { mutableStateOf(item?.dosisDtRefuerzo ?: false) }
    var gestas by remember { mutableStateOf(item?.gestas?.toString().orEmpty()) }
    var partos by remember { mutableStateOf(item?.partos?.toString().orEmpty()) }
    var abortos by remember { mutableStateOf(item?.abortos?.toString().orEmpty()) }
    var cesareas by remember { mutableStateOf(item?.cesareas?.toString().orEmpty()) }
    var antecedentes by remember { mutableStateOf(item?.antecedentesPatologicosObstetricos.orEmpty()) }
    var riesgoObstetrico by remember { mutableStateOf(item?.riesgoObstetrico.orEmpty()) }
    var calendario by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    PantallaRuralitos(
        titulo = if (item == null) "Agregar embarazo" else "Editar embarazo",
        descripcion = "Completa las fechas, vacunación y antecedentes en bloques separados.",
        subtitulo = "Registro obstétrico",
        onVolver = onCancelar,
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = if (item == null) "Guardar nuevo embarazo" else "Guardar cambios del embarazo",
                descripcion = "Validar y regresar al listado de salud familiar",
                color = CianRuralitos,
                onClick = {
                    if (nombres.isBlank() || GrupoEdadFamiliar.calcular(fechaNacimiento) == null) {
                        error = "Completa los apellidos y nombres y una fecha de nacimiento válida."
                    } else if (cedula.isNotBlank() && !ValidadorIdentidadEcuador.esDocumentoFamiliarAceptable(cedula)) {
                        error = "La identificación, si se registra, debe contener 10 o 13 números."
                    } else {
                        onGuardar(
                            EmbarazadaEntity(
                                id = item?.id ?: 0,
                                fichaId = fichaId,
                                apellidosNombres = nombres.trim(),
                                fechaUltimaMenstruacion = fum,
                                fechaProbableParto = fpp,
                                semanasGestacion = semanas.toIntOrNull(),
                                dosisDtPrimera = primera,
                                dosisDtSegunda = segunda,
                                dosisDtRefuerzo = refuerzo,
                                gestas = gestas.toIntOrNull(),
                                partos = partos.toIntOrNull(),
                                abortos = abortos.toIntOrNull(),
                                cesareas = cesareas.toIntOrNull(),
                                antecedentesPatologicosObstetricos = antecedentes.trim(),
                                riesgoObstetrico = riesgoObstetrico,
                                syncId = item?.syncId ?: UUID.randomUUID().toString()
                            ),
                            DatosPersonaSalud(
                                nombres = nombres.trim(),
                                cedula = cedula.trim(),
                                fechaNacimiento = fechaNacimiento,
                                parentesco = parentesco,
                                sexo = "M",
                                escolaridad = escolaridad,
                                ocupacion = ocupacion.trim()
                            )
                        )
                    }
                }
            )

        }
    ) {
        SeccionFormularioRuralitos(
            titulo = "1. Datos personales",
            descripcion = "Selecciona una integrante o completa sus datos. También aparecerá en los consolidados."
        ) {
            SelectorPersonaExistenteSalud(miembros, personaInicial?.id) { persona ->
                nombres = persona.apellidosNombres
                cedula = persona.cedula
                fechaNacimiento = persona.fechaNacimiento
                parentesco = persona.parentesco.ifBlank { parentesco }
                escolaridad = persona.escolaridad.ifBlank { escolaridad }
                ocupacion = persona.ocupacion
            }
            CampoTexto(nombres, { nombres = it }, "Apellidos y nombres")
            OutlinedTextField(
                value = cedula,
                onValueChange = { cedula = it.filter(Char::isDigit).take(13) },
                label = { Text("Cédula o identificación") },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            CampoFecha(fechaNacimiento, "Fecha de nacimiento") { calendario = "nacimiento" }
            SelectorTextoSalud(
                "Parentesco",
                listOf("JEFE/A DE FAMILIA", "CÓNYUGE/PAREJA", "HIJO/A", "MADRE", "OTRO FAMILIAR", "NO FAMILIAR"),
                parentesco
            ) { parentesco = it }
            SelectorTextoSalud("Escolaridad", listOf("SIN", "BAS", "BACH", "SUP", "ESP"), escolaridad) { escolaridad = it }
            CampoTexto(ocupacion, { ocupacion = it }, "Ocupación")
        }
        SeccionFormularioRuralitos(
            titulo = "2. Gestación",
            descripcion = "Fechas principales y semanas de gestación."
        ) {
            CampoFecha(fum, "Fecha de última menstruación") { calendario = "fum" }
            CampoFecha(fpp, "Fecha probable del parto") { calendario = "fpp" }
            CampoEnteroSalud(semanas, { semanas = it }, "Semanas de gestación")
        }
        SeccionFormularioRuralitos(
            titulo = "3. Vacunación dT",
            descripcion = "Selecciona Sí o No para cada dosis."
        ) {
            OpcionSiNoSalud("Primera dosis", primera) { primera = it }
            OpcionSiNoSalud("Segunda dosis", segunda) { segunda = it }
            OpcionSiNoSalud("Dosis de refuerzo", refuerzo) { refuerzo = it }
        }
        SeccionFormularioRuralitos(
            titulo = "4. Antecedentes obstétricos",
            descripcion = "Número de gestas, partos, abortos, cesáreas y antecedentes clínicos."
        ) {
            CampoEnteroSalud(gestas, { gestas = it }, "Gestas")
            CampoEnteroSalud(partos, { partos = it }, "Partos")
            CampoEnteroSalud(abortos, { abortos = it }, "Abortos")
            CampoEnteroSalud(cesareas, { cesareas = it }, "Cesáreas")
            SeleccionRiesgoObstetrico(riesgoObstetrico) { riesgoObstetrico = it }
            OutlinedTextField(
                value = antecedentes,
                onValueChange = { antecedentes = it },
                label = { Text("Antecedentes patológicos obstétricos") },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                minLines = 3,
            )
        }
        error?.let {
            MensajeEstadoRuralitos(
                titulo = "Revisa el registro",
                descripcion = it,
                color = RojoClinico,
                simbolo = "!"
            )
        }
    }

    if (calendario != null) {
        SelectorFechaDialog(
            onFechaSeleccionada = { fecha ->
                when (calendario) {
                    "nacimiento" -> fechaNacimiento = fecha
                    "fum" -> fum = fecha
                    else -> fpp = fecha
                }
                calendario = null
            },
            onCerrar = { calendario = null }
        )
    }
}

@Composable
private fun SeleccionRiesgoObstetrico(valor: String, onCambio: (String) -> Unit) {
    Text("Riesgo obstétrico", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf("SIN_RIESGO" to "Sin riesgo", "BAJO" to "Bajo", "ALTO" to "Alto", "MUY_ALTO" to "Muy alto").forEach { (codigo, etiqueta) ->
            FilterChip(selected = valor == codigo, onClick = { onCambio(codigo) }, label = { Text(etiqueta) })
        }
    }
}

@Composable
private fun FormularioMortalidadScreen(
    fichaId: Long,
    item: MortalidadFamiliarEntity?,
    onGuardar: (MortalidadFamiliarEntity) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf(item?.nombre.orEmpty()) }
    var parentesco by remember { mutableStateOf(item?.parentesco.orEmpty()) }
    var edad by remember { mutableStateOf(item?.edadAlFallecer?.toString().orEmpty()) }
    var causa by remember { mutableStateOf(item?.causa.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    PantallaRuralitos(
        titulo = if (item == null) "Agregar fallecimiento" else "Editar fallecimiento",
        descripcion = "Registra únicamente antecedentes de mortalidad familiar de los últimos cinco años.",
        subtitulo = "Mortalidad familiar",
        onVolver = onCancelar,
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = if (item == null) "Guardar fallecimiento" else "Guardar cambios",
                descripcion = "Validar y regresar al listado de salud familiar",
                color = RojoClinico,
                onClick = {
                    if (nombre.isBlank() || parentesco.isBlank() || edad.toIntOrNull() == null || causa.isBlank()) {
                        error = "Completa apellidos y nombres, parentesco, edad al fallecer y causa."
                    } else {
                        onGuardar(
                            MortalidadFamiliarEntity(
                                id = item?.id ?: 0,
                                fichaId = fichaId,
                                nombre = nombre.trim(),
                                parentesco = parentesco.trim(),
                                edadAlFallecer = edad.toIntOrNull(),
                                causa = causa.trim(),
                                syncId = item?.syncId ?: UUID.randomUUID().toString()
                            )
                        )
                    }
                }
            )

        }
    ) {
        SeccionFormularioRuralitos(
            titulo = "Datos de la persona",
            descripcion = "Solo se requieren los cuatro datos de mortalidad de la ficha familiar."
        ) {
            CampoTexto(nombre, { nombre = it }, "Apellidos y nombres")
            CampoTexto(parentesco, { parentesco = it }, "Parentesco")
            CampoEnteroSalud(edad, { edad = it }, "Edad al fallecer")
            OutlinedTextField(
                value = causa,
                onValueChange = { causa = it },
                label = { Text("Causa del fallecimiento") },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                minLines = 3,
            )
        }
        error?.let {
            MensajeEstadoRuralitos(
                titulo = "Revisa el registro",
                descripcion = it,
                color = RojoClinico,
                simbolo = "!"
            )
        }
    }
}

@Composable
private fun SelectorPersonaExistenteSalud(
    miembros: List<MiembroFamiliaEntity>,
    idInicial: Long?,
    onSeleccion: (MiembroFamiliaEntity) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    var idSeleccionado by remember { mutableStateOf(idInicial) }
    Text("Integrante de la ficha", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 5.dp))
    Box(Modifier.fillMaxWidth()) {
        BotonSelectorRuralitos(onClick = { expandido = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(miembros.firstOrNull { it.id == idSeleccionado }?.apellidosNombres ?: "Completar una persona nueva")
        }
        MenuDesplegableRuralitos(expanded = expandido, onDismissRequest = { expandido = false }) {
            ItemMenuRuralitos(
                text = { Text("Completar una persona nueva") },
                onClick = { idSeleccionado = null; expandido = false }
            )
            miembros.forEach { persona ->
                ItemMenuRuralitos(
                    text = { Text("${if (persona.id == idSeleccionado) "✓ " else ""}${persona.apellidosNombres}") },
                    onClick = {
                        idSeleccionado = persona.id
                        onSeleccion(persona)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SelectorTextoSalud(
    titulo: String,
    opciones: List<String>,
    seleccion: String,
    onSeleccion: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    Text(titulo, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 5.dp))
    Box(Modifier.fillMaxWidth()) {
        BotonSelectorRuralitos(onClick = { expandido = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(
                when (seleccion) {
                    "H" -> "Hombre"
                    "M" -> "Mujer"
                    "SIN" -> "Sin escolaridad"
                    "BAS" -> "Básica"
                    "BACH" -> "Bachillerato"
                    "SUP" -> "Superior"
                    "ESP" -> "Especialidad"
                    else -> seleccion
                }
            )
        }
        MenuDesplegableRuralitos(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { opcion ->
                ItemMenuRuralitos(
                    text = { Text("${if (opcion == seleccion) "✓ " else ""}${when (opcion) { "H" -> "Hombre"; "M" -> "Mujer"; else -> opcion }}") },
                    onClick = { onSeleccion(opcion); expandido = false }
                )
            }
        }
    }
}

@Composable
private fun CampoEnteroSalud(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { onCambio(it.filter(Char::isDigit)) },
        label = { Text(etiqueta) },
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun OpcionSiNoSalud(
    titulo: String,
    valor: Boolean,
    onCambio: (Boolean) -> Unit
) {
    Text(
        titulo,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp, bottom = 5.dp)
    )
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = valor,
            onClick = { onCambio(true) },
            label = { Text("Sí", fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.heightIn(min = 48.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CianRuralitos,
                selectedLabelColor = Color.White
            )
        )
        FilterChip(
            selected = !valor,
            onClick = { onCambio(false) },
            label = { Text("No", fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.heightIn(min = 48.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = RojoClinico,
                selectedLabelColor = Color.White
            )
        )
    }
}
