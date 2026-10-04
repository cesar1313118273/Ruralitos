package com.ruralitos.app.ui.screens

import com.ruralitos.app.ui.components.TextoAjustado
import com.ruralitos.app.ui.components.EncabezadoPantallaRuralitos
import com.ruralitos.app.ui.components.FlechaDesplegable
import androidx.compose.ui.draw.clip
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.ItemMenuRuralitos
import com.ruralitos.app.ui.components.MenuDesplegableRuralitos
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.statusBarsPadding
import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.testTag
import com.ruralitos.app.ui.components.BotonAccionRuralitos
import com.ruralitos.app.ui.components.BotonFlotanteRedondo
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.DatoVentanaRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.VentanaRuralitos
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.withTransaction
import androidx.core.content.ContextCompat
import com.ruralitos.app.R
import com.ruralitos.app.ui.components.ListaDeColumnasAdaptable
import com.ruralitos.app.data.agenda.RecordatorioAgenda
import com.ruralitos.app.data.agenda.PlanificadorSeguimiento
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import com.ruralitos.app.data.local.entity.PersonaAgenda
import com.ruralitos.app.ui.components.BotonVolverRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val agendaAzul = Color(0xFF0A2A5E)
private val agendaVerde = Color(0xFF0889A0)
private val agendaBorde = Color(0xFFE2ECF1)
private val agendaSecundario = Color(0xFF5B7083)
private val agendaFondo = Color(0xFFF6F9FB)
private val agendaAzulPunto = Color(0xFF1565C0)
private val agendaNaranja = Color(0xFFEF790F)
private val agendaVerdeEstado = Color(0xFF0889A0)
private val agendaRojoAtraso = Color(0xFFC7474B)
private val agendaTipos = listOf(
    "Visita domiciliaria", "Control prenatal", "Seguimiento", "Vacunación", "Otra actividad"
)

@Composable
fun AgendaScreen(
    usuarioId: Long,
    organizacionId: String,
    onRegresar: () -> Unit,
    onAbrirFicha: (Long) -> Unit,
    estadoMapa: EstadoMapaSeguimiento = remember { EstadoMapaSeguimiento() },
    pestanaInicial: Int = 0,
    onPestanaCambiada: (Int) -> Unit = {},
    onUbicarFicha: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val scope = rememberCoroutineScope()
    val actividades by database.agendaDao().observar(usuarioId).collectAsState(initial = emptyList())
    val personas by database.agendaDao().observarPersonas(organizacionId).collectAsState(initial = emptyList())
    var fechaSeleccionada by remember { mutableStateOf(inicioDia(System.currentTimeMillis())) }
    var editando by remember { mutableStateOf<ActividadAgendaEntity?>(null) }
    var formulario by remember { mutableStateOf(false) }
    var eliminar by remember { mutableStateOf<List<ActividadAgendaEntity>?>(null) }
    var error by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var visitaPorConfirmar by remember { mutableStateOf<ActividadAgendaEntity?>(null) }
    var hoy by remember { mutableStateOf(inicioDia(System.currentTimeMillis())) }
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var pestana by remember { mutableIntStateOf(pestanaInicial.coerceIn(0, 2)) }
    var fichaParaAgendar by remember { mutableStateOf<Long?>(null) }
    var grupoDetalle by remember { mutableStateOf<List<ActividadAgendaEntity>?>(null) }
    var paginaSeguimiento by remember { mutableIntStateOf(0) }
    var paginaDia by remember { mutableIntStateOf(0) }
    val permisoAvisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            permisoAvisos.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        runCatching {
            database.agendaDao().actividadesActivas(usuarioId).forEach {
                RecordatorioAgenda.actualizar(context, it)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            ahora = System.currentTimeMillis()
            hoy = inicioDia(ahora)
        }
    }

    LaunchedEffect(usuarioId, organizacionId) {
        if (usuarioId > 0L) {
            runCatching { PlanificadorSeguimiento.prepararTodas(context, organizacionId, usuarioId) }
                .onFailure { error = "No se pudieron actualizar los seguimientos. Tus actividades guardadas siguen disponibles." }
        }
    }

    visitaPorConfirmar?.let { actividad ->
        VentanaRuralitos(
            titulo = "Registrar visita familiar",
            subtitulo = listOf(actividad.persona, actividad.barrio).filter { it.isNotBlank() }.joinToString(" · "),
            simbolo = "✓",
            color = agendaVerde,
            onCerrar = { visitaPorConfirmar = null },
            contenido = {
                Text(
                    "Confirma la atención del hogar. Las próximas visitas se calcularán nuevamente según el grupo de riesgo de cada integrante.",
                    color = agendaSecundario
                )
            },
            acciones = {
                BotonPrincipalRuralitos(
                    texto = "Registrar visita",
                    color = agendaVerde,
                    modifier = Modifier.testTag("confirmar_registro_visita"),
                    onClick = {
                        visitaPorConfirmar = null
                        scope.launch {
                            runCatching {
                                PlanificadorSeguimiento.registrarVisitaFamiliar(context, actividad.id, usuarioId)
                                // Se comprueba en la base que de verdad quedó realizada; si no, se avisa.
                                val guardada = withContext(Dispatchers.IO) { database.agendaDao().buscar(actividad.id) }
                                check(guardada?.estado == "COMPLETADA") { "La visita no quedó registrada." }
                            }.onSuccess {
                                error = ""
                                android.widget.Toast.makeText(
                                    context, "Visita registrada. Las próximas visitas se calcularon de nuevo.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }.onFailure {
                                error = "No se pudo registrar la visita. Inténtalo nuevamente."
                                android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
                BotonSecundarioRuralitos(
                    texto = "Editar ficha",
                    onClick = {
                        visitaPorConfirmar = null
                        actividad.fichaId?.let(onAbrirFicha)
                    }
                )
                BotonSecundarioRuralitos(texto = "Cancelar", onClick = { visitaPorConfirmar = null })
            }
        )
    }

    eliminar?.let { grupo ->
        VentanaRuralitos(
            titulo = "Eliminar actividad",
            simbolo = "!",
            color = agendaRojoAtraso,
            onCerrar = { eliminar = null },
            contenido = {
                Text(
                    "¿Eliminar esta actividad? Un seguimiento automático no volverá a aparecer hasta la siguiente ronda de visitas.",
                    color = agendaSecundario
                )
            },
            acciones = {
                BotonPrincipalRuralitos(
                    texto = "Eliminar",
                    color = agendaRojoAtraso,
                    onClick = {
                        eliminar = null
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    database.withTransaction {
                                        grupo.forEach { actividad ->
                                            if (actividad.origen == "SEGUIMIENTO") {
                                                database.agendaDao().actualizar(actividad.copy(estado = "CANCELADA"))
                                            } else database.agendaDao().eliminar(actividad.id)
                                        }
                                    }
                                }
                                grupo.forEach { RecordatorioAgenda.cancelar(context, it.id) }
                            }.onFailure { error = "No se pudo eliminar la actividad." }
                        }
                    }
                )
                BotonSecundarioRuralitos(texto = "Conservar", onClick = { eliminar = null })
            }
        )
    }

    if (formulario) {
        FormularioAgenda(
            usuarioId = usuarioId,
            organizacionId = organizacionId,
            personas = personas,
            personaInicial = fichaParaAgendar?.let { id -> personas.firstOrNull { it.fichaId == id } },
            original = editando,
            fechaInicial = fechaSeleccionada,
            error = error,
            guardando = guardando,
            onVolver = { formulario = false; error = ""; fichaParaAgendar = null },
            onGuardar = { item ->
                if (guardando) return@FormularioAgenda
                if (item.recordar && item.fechaHora <= System.currentTimeMillis()) {
                    error = "El recordatorio necesita una fecha y hora futuras."
                    return@FormularioAgenda
                }
                guardando = true
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            if (item.id == 0L) {
                                val id = database.agendaDao().crear(item)
                                item.copy(id = id)
                            } else {
                                database.agendaDao().actualizar(item)
                                item
                            }
                        }
                    }.onSuccess { guardada ->
                        RecordatorioAgenda.cancelar(context, guardada.id)
                        RecordatorioAgenda.actualizar(context, guardada)
                        fechaSeleccionada = inicioDia(guardada.fechaHora)
                        formulario = false
                        error = ""
                        fichaParaAgendar = null
                    }.onFailure { error = "No se pudo guardar la actividad. Inténtalo nuevamente." }
                    guardando = false
                }
            }
        )
        return
    }

    val delDia = remember(actividades, fechaSeleccionada) {
        agruparVisitas(actividades.filter {
            it.estado != "CANCELADA" && inicioDia(it.fechaHora) == fechaSeleccionada
        })
    }
    val proximas = remember(actividades, ahora) {
        agruparVisitas(actividades.filter {
            estadoVisible(it, ahora) == "Por confirmar" || estadoVisible(it, ahora) == "Atrasada"
        }.sortedBy { it.fechaHora })
    }
    val totalPaginasSeguimiento = maxOf(1, (proximas.size + 2) / 3)
    val paginaSeguimientoActual = paginaSeguimiento.coerceIn(0, totalPaginasSeguimiento - 1)
    val totalPaginasDia = maxOf(1, (delDia.size + 2) / 3)
    val paginaDiaActual = paginaDia.coerceIn(0, totalPaginasDia - 1)
    LaunchedEffect(fechaSeleccionada) { paginaDia = 0 }

    fun editarFechaGrupo(grupo: List<ActividadAgendaEntity>) {
        if (grupo.size == 1) {
            editando = grupo.first(); error = ""; formulario = true
            return
        }
        val actual = Calendar.getInstance().apply { timeInMillis = grupo.first().fechaHora }
        DatePickerDialog(context, { _, anio, mes, dia ->
            val nueva = Calendar.getInstance().apply {
                timeInMillis = grupo.first().fechaHora
                set(anio, mes, dia)
            }.timeInMillis
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        database.withTransaction {
                            grupo.forEach { database.agendaDao().actualizar(it.copy(fechaHora = nueva, fechaEditada = true)) }
                        }
                    }
                    grupo.forEach {
                        RecordatorioAgenda.cancelar(context, it.id)
                        RecordatorioAgenda.actualizar(context, it.copy(fechaHora = nueva, fechaEditada = true))
                    }
                }.onFailure { error = "No se pudo cambiar la fecha familiar." }
            }
        }, actual.get(Calendar.YEAR), actual.get(Calendar.MONTH), actual.get(Calendar.DAY_OF_MONTH)).show()
    }
    fun confirmarGrupo(grupo: List<ActividadAgendaEntity>) {
        scope.launch {
            runCatching {
                val confirmadas = grupo.filter {
                    it.origen == "SEGUIMIENTO" && it.estado == "PENDIENTE"
                }.map { it.copy(fechaEditada = true) }
                withContext(Dispatchers.IO) {
                    database.withTransaction {
                        confirmadas.forEach { database.agendaDao().actualizar(it) }
                    }
                }
                confirmadas.forEach {
                    RecordatorioAgenda.cancelar(context, it.id)
                    RecordatorioAgenda.actualizar(context, it)
                }
            }.onFailure { error = "No se pudo confirmar la fecha. Inténtalo nuevamente." }
        }
        grupoDetalle = null
    }

    fun completarGrupo(grupo: List<ActividadAgendaEntity>) {
        val actividad = grupo.first()
        grupoDetalle = null
        if (actividad.fichaId != null && actividad.origen == "SEGUIMIENTO") {
            visitaPorConfirmar = actividad
        } else {
            scope.launch {
                runCatching {
                    val actualizada = actividad.copy(estado = "COMPLETADA")
                    withContext(Dispatchers.IO) { database.agendaDao().actualizar(actualizada) }
                    RecordatorioAgenda.actualizar(context, actualizada)
                }.onFailure { error = "No se pudo registrar la actividad." }
            }
        }
    }

    grupoDetalle?.let { grupo ->
        val actividad = grupo.first()
        val estadoActual = estadoVisible(actividad, ahora)
        val colorActual = colorEstado(actividad, ahora)
        VentanaRuralitos(
            titulo = if (grupo.size > 1) "Visita familiar (${grupo.size})" else actividad.tipo,
            subtitulo = estadoActual,
            simbolo = when (estadoActual) { "Realizado" -> "✓"; "Atrasada" -> "!"; else -> "◷" },
            color = colorActual,
            onCerrar = { grupoDetalle = null },
            contenido = {
                DatoVentanaRuralitos("Persona", grupo.joinToString(", ") { it.persona }.ifBlank { "Actividad general" })
                DatoVentanaRuralitos(
                    "Fecha y lugar",
                    "${formato(actividad.fechaHora, "EEEE d 'de' MMMM · HH:mm")}${if (actividad.barrio.isNotBlank()) " · ${actividad.barrio}" else ""}"
                )
                if (actividad.nota.isNotBlank()) DatoVentanaRuralitos("Nota", actividad.nota)
                if (estadoActual == "Atrasada") {
                    Text(
                        "La fecha ya pasó. Si se hizo la visita, márcala como realizada; si no, reprográmala o elimínala.",
                        color = agendaRojoAtraso, fontSize = 12.sp
                    )
                }
            },
            acciones = {
                if (actividad.estado != "COMPLETADA") {
                    // Se puede registrar como realizada en cualquier estado: una visita hecha tarde también hay que guardarla.
                    BotonPrincipalRuralitos(
                        texto = "Marcar como realizada",
                        color = agendaVerde,
                        modifier = Modifier.testTag("marcar_realizada"),
                        onClick = { completarGrupo(grupo) }
                    )
                    if (actividad.origen == "SEGUIMIENTO" && !actividad.fechaEditada) {
                        BotonPrincipalRuralitos(
                            texto = "Confirmar fecha",
                            color = agendaAzulPunto,
                            modifier = Modifier.testTag("confirmar_fecha"),
                            onClick = { confirmarGrupo(grupo) }
                        )
                    }
                    BotonSecundarioRuralitos(
                        texto = if (estadoActual == "Atrasada") "Reprogramar" else "Cambiar fecha",
                        onClick = { grupoDetalle = null; editarFechaGrupo(grupo) }
                    )
                } else {
                    MensajeEstadoRuralitos(
                        titulo = "Visita realizada",
                        descripcion = "Esta actividad ya quedó registrada.",
                        color = agendaVerdeEstado,
                        simbolo = "✓"
                    )
                }
                if (actividad.fichaId != null) {
                    BotonSecundarioRuralitos(
                        texto = "Abrir ficha",
                        onClick = { grupoDetalle = null; actividad.fichaId.let(onAbrirFicha) }
                    )
                }
                if (actividad.estado != "COMPLETADA") {
                    BotonAccionRuralitos(
                        texto = "Eliminar actividad",
                        color = agendaRojoAtraso,
                        onClick = { grupoDetalle = null; eliminar = grupo },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(agendaFondo).formularioSeguro()) {
      EncabezadoPantallaRuralitos(
          titulo = when (pestana) { 0 -> "Seguimiento"; 1 -> "Agenda"; else -> "Mapa de visitas" },
          subtitulo = null,
          paso = null,
          totalPasos = null,
          etiquetaPaso = "",
          onVolver = onRegresar,
          descripcion = when (pestana) {
              0 -> "Organiza visitas, notas y controles pendientes"
              1 -> "Organiza y consulta tus actividades de salud"
              else -> "Ubica tus visitas y ordena el recorrido del día"
          }
      )
      if (pestana == 2) {
        Box(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
            PestanasAgenda(pestana, onSeleccionar = { pestana = it; onPestanaCambiada(it) })
        }
        MapaSeguimientoVista(
            usuarioId = usuarioId,
            actividades = actividades,
            ahora = ahora,
            estado = estadoMapa,
            onAbrirVisita = { grupoDetalle = it },
            onAbrirFicha = onAbrirFicha,
            onUbicarFicha = onUbicarFicha,
            modifier = Modifier.weight(1f)
        )
      } else
      Box(Modifier.weight(1f).fillMaxWidth()) {
        ListaDeColumnasAdaptable(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp),
            espacio = 15.dp
        ) {
            item { PestanasAgenda(pestana, onSeleccionar = { pestana = it; onPestanaCambiada(it) }) }
            if (pestana == 0) {
                item {
                    ResumenSeguimientos(
                        hoy = delDia.size,
                        pendientes = actividades.count { estadoVisible(it, ahora) == "Por confirmar" },
                        confirmadas = actividades.count {
                            it.origen == "SEGUIMIENTO" && estadoVisible(it, ahora) == "Programado"
                        }
                    )
                }
                item {
                    EncabezadoActividades(
                        titulo = "Por confirmar y atrasadas",
                        fecha = formato(hoy, "EEE d 'de' MMMM 'de' yyyy")
                    )
                }
                if (proximas.isEmpty()) {
                    item { MensajeAgendaVacia("No hay visitas por confirmar ni atrasadas.") }
                } else {
                    items(proximas.drop(paginaSeguimientoActual * 3).take(3),
                        key = { "proxima_" + it.first().id }) { grupo ->
                        TarjetaActividadAgenda(grupo, ahora, onAbrir = {
                            fechaSeleccionada = inicioDia(grupo.first().fechaHora)
                            grupoDetalle = grupo
                        })
                    }
                    if (totalPaginasSeguimiento > 1) item {
                        PaginacionAgenda(paginaSeguimientoActual, totalPaginasSeguimiento,
                            onAnterior = { paginaSeguimiento = (paginaSeguimientoActual - 1).coerceAtLeast(0) },
                            onSiguiente = { paginaSeguimiento = (paginaSeguimientoActual + 1).coerceAtMost(totalPaginasSeguimiento - 1) })
                    }
                }
            } else {
                item {
                    CalendarioAgenda(
                        fechaSeleccionada = fechaSeleccionada,
                        actividades = actividades,
                        ahora = ahora,
                        onSeleccionarDia = { fechaSeleccionada = it }
                    )
                }
                item {
                    EncabezadoActividades(
                        titulo = "Actividades del día",
                        fecha = formato(fechaSeleccionada, "EEE d 'de' MMMM 'de' yyyy")
                    )
                }
                if (delDia.isEmpty()) {
                    item { MensajeAgendaVacia("No hay actividades programadas para este día.") }
                } else {
                    items(delDia.drop(paginaDiaActual * 3).take(3),
                        key = { "dia_" + it.first().id }) { grupo ->
                        TarjetaActividadAgenda(grupo, ahora, onAbrir = { grupoDetalle = grupo })
                    }
                    if (totalPaginasDia > 1) item {
                        PaginacionAgenda(paginaDiaActual, totalPaginasDia,
                            onAnterior = { paginaDia = (paginaDiaActual - 1).coerceAtLeast(0) },
                            onSiguiente = { paginaDia = (paginaDiaActual + 1).coerceAtMost(totalPaginasDia - 1) })
                    }
                }
            }
            if (error.isNotBlank()) {
                item { Text(error, color = Color(0xFFC83E4D),
                    modifier = Modifier.padding(horizontal = 20.dp)) }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
      }
    }
    // El + para agendar está siempre en Seguimiento y en Agenda; en el mapa no.
    if (pestana != 2) {
        BotonFlotanteRedondo(
            descripcion = "Agendar una visita",
            color = agendaVerde,
            etiquetaPrueba = "boton_agendar",
            onClick = { fichaParaAgendar = null; editando = null; error = ""; formulario = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 24.dp)
        )
    }
    }
}

@Composable
internal fun CabeceraAgenda(onRegresar: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotonVolverRuralitos(onRegresar)
        }
        HorizontalDivider(color = Color(0xFFE2ECF1))
    }
}

@Composable
private fun PestanasAgenda(seleccionada: Int, onSeleccionar: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            .background(Color(0xFFE8EFFA), RoundedCornerShape(16.dp)),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        listOf("Seguimiento", "Agenda", "Mapa").forEachIndexed { indice, titulo ->
            Surface(
                onClick = { onSeleccionar(indice) },
                modifier = Modifier.weight(1f).heightIn(min = 49.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (seleccionada == indice) agendaVerde else Color.Transparent,
                shadowElevation = if (seleccionada == indice) 2.dp else 0.dp
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painterResource(when (indice) {
                            0 -> R.drawable.ruralitos_agenda_lista
                            1 -> R.drawable.ruralitos_agenda_calendario
                            else -> R.drawable.ruralitos_agenda_mapa
                        }),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(if (seleccionada == indice) Color.White else agendaSecundario)
                    )
                    Spacer(Modifier.width(5.dp))
                    // Con tres pestañas el espacio es poco: la letra se achica en vez de cortarse.
                    TextoAjustado(titulo, color = if (seleccionada == indice) Color.White else agendaSecundario,
                        tamano = 14.sp, tamanoMinimo = 7.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false))
                }
            }
        }
    }
}

@Composable
private fun ResumenSeguimientos(hoy: Int, pendientes: Int, confirmadas: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple("Hoy", hoy, agendaAzulPunto),
            Triple("Pendientes", pendientes, agendaNaranja),
            Triple("Confirmadas", confirmadas, agendaVerdeEstado)
        ).forEachIndexed { indice, dato ->
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 91.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, agendaBorde),
                shadowElevation = 1.dp
            ) {
                // Icono y número arriba; el nombre debajo, con todo el ancho de la tarjeta, para que no se corte ninguna letra.
                Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(painterResource(when (indice) {
                            0 -> R.drawable.ruralitos_agenda_calendario
                            1 -> R.drawable.ruralitos_agenda_reloj
                            else -> R.drawable.ruralitos_agenda_confirmado
                        }), null, Modifier.size(25.dp))
                        Text(dato.second.toString(), color = agendaAzul, fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                    TextoAjustado(dato.first, color = agendaSecundario, tamano = 12.sp, tamanoMinimo = 7.sp)
                }
            }
        }
    }
}

@Composable
private fun EncabezadoActividades(titulo: String, fecha: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 19.dp, vertical = 4.dp)) {
        Text(titulo, color = agendaAzul, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(fecha, color = agendaSecundario, fontSize = 12.sp)
    }
}

@Composable
private fun MensajeAgendaVacia(mensaje: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).heightIn(min = 74.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, agendaBorde)
    ) {
        Box(Modifier.padding(18.dp), contentAlignment = Alignment.CenterStart) {
            Text(mensaje, color = agendaSecundario, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PaginacionAgenda(
    pagina: Int,
    total: Int,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onAnterior, enabled = pagina > 0) {
            Text("Anterior", color = if (pagina > 0) agendaVerde else agendaSecundario)
        }
        Text("${pagina + 1} de $total", color = agendaAzul, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onSiguiente, enabled = pagina + 1 < total) {
            Text("Siguiente", color = if (pagina + 1 < total) agendaVerde else agendaSecundario)
        }
    }
}

@Composable
private fun CalendarioAgenda(
    fechaSeleccionada: Long,
    actividades: List<ActividadAgendaEntity>,
    ahora: Long,
    onSeleccionarDia: (Long) -> Unit
) {
    val mes = remember(fechaSeleccionada) {
        Calendar.getInstance().apply {
            timeInMillis = fechaSeleccionada
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val desplazamiento = (mes.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val diasMes = mes.getActualMaximum(Calendar.DAY_OF_MONTH)
    val filas = (desplazamiento + diasMes + 6) / 7
    val actividadesPorDia = remember(actividades) {
        actividades.filter { it.estado != "CANCELADA" }.groupBy { inicioDia(it.fechaHora) }
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, agendaBorde),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(formato(mes.timeInMillis, "MMMM 'de' yyyy").replaceFirstChar { it.uppercase() },
                    color = agendaAzul, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                listOf(-1, 1).forEach { movimiento ->
                    Surface(
                        onClick = {
                            val nueva = Calendar.getInstance().apply {
                                timeInMillis = mes.timeInMillis
                                add(Calendar.MONTH, movimiento)
                            }
                            onSeleccionarDia(inicioDia(nueva.timeInMillis))
                        },
                        modifier = Modifier.padding(start = 6.dp).size(35.dp),
                        shape = CircleShape,
                        color = Color(0xFFE8EFFA)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (movimiento < 0) "‹" else "›", color = agendaAzul,
                                fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom").forEach { nombre ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(nombre, color = agendaSecundario, fontSize = 11.sp)
                    }
                }
            }
            repeat(filas) { fila ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(7) { columna ->
                        val indice = fila * 7 + columna - desplazamiento
                        val fecha = Calendar.getInstance().apply {
                            timeInMillis = mes.timeInMillis
                            add(Calendar.DAY_OF_MONTH, indice)
                        }
                        val dia = inicioDia(fecha.timeInMillis)
                        val elegido = dia == fechaSeleccionada
                        val delMes = indice in 0 until diasMes
                        val puntos = actividadesPorDia[dia].orEmpty()
                            .map { colorEstado(it, ahora) }.distinct().take(3)
                        Surface(
                            onClick = { onSeleccionarDia(dia) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(7.dp),
                            color = if (elegido) agendaVerde else if (delMes) Color.White else Color(0xFFF6F9FB),
                            border = if (elegido) null else BorderStroke(1.dp, agendaBorde)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Text(fecha.get(Calendar.DAY_OF_MONTH).toString(),
                                    color = if (elegido) Color.White else if (delMes) agendaAzul else Color(0xFFABB8CA),
                                    fontSize = 13.sp,
                                    fontWeight = if (elegido) FontWeight.SemiBold else FontWeight.Normal)
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    puntos.forEach { punto ->
                                        Box(Modifier.size(5.dp).background(if (elegido) Color.White else punto, CircleShape))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun estadoVisible(actividad: ActividadAgendaEntity, ahora: Long = System.currentTimeMillis()): String = when {
    actividad.estado == "COMPLETADA" -> "Realizado"
    actividad.estado == "CANCELADA" -> "Cancelada"
    actividad.origen == "SEGUIMIENTO" && !actividad.fechaEditada -> "Por confirmar"
    actividad.fechaHora < ahora -> "Atrasada"
    else -> "Programado"
}

private fun colorEstado(actividad: ActividadAgendaEntity, ahora: Long = System.currentTimeMillis()): Color =
    when (estadoVisible(actividad, ahora)) {
    "Realizado" -> agendaVerdeEstado
    "Por confirmar" -> agendaNaranja
    "Atrasada" -> agendaRojoAtraso
    else -> agendaAzulPunto
}

@Composable
private fun TarjetaActividadAgenda(
    integrantes: List<ActividadAgendaEntity>,
    ahora: Long,
    onAbrir: () -> Unit
) {
    val actividad = integrantes.first()
    val familiar = integrantes.size > 1
    val acento = if (familiar || actividad.origen == "MANUAL") agendaAzulPunto else agendaVerdeEstado
    val icono = when {
        familiar -> R.drawable.ruralitos_agenda_calendario
        actividad.tipo == "Visita domiciliaria" -> R.drawable.ruralitos_agenda_casita
        actividad.origen == "SEGUIMIENTO" -> R.drawable.ruralitos_agenda_estetoscopio
        else -> iconoActividad(actividad.tipo)
    }
    val estado = estadoVisible(actividad, ahora)
    val fondoEstado = when (estado) {
        "Realizado" -> Color(0xFFE1F5E9)
        "Por confirmar" -> Color(0xFFFFEFDE)
        "Atrasada" -> Color(0xFFFFEBEC)
        else -> Color(0xFFE8EFFA)
    }
    Surface(
        onClick = onAbrir,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).testTag("tarjeta_actividad_${actividad.id}"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, agendaBorde),
        shadowElevation = 1.dp
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(formato(actividad.fechaHora, "HH:mm"), color = agendaAzul,
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.width(7.dp))
            Box(Modifier.width(3.dp).height(52.dp).background(acento, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(11.dp),
                color = if (acento == agendaAzulPunto) Color(0xFFE8EFFA) else Color(0xFFE3F4F7)) {
                Box(Modifier.size(39.dp), contentAlignment = Alignment.Center) {
                    Image(painterResource(icono), null, Modifier.size(27.dp))
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(if (familiar) "Visita familiar (" + integrantes.size + ")" else actividad.tipo,
                    color = agendaAzul, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(if (familiar) integrantes.joinToString(", ") { it.persona }
                    else actividad.persona.ifBlank { "Actividad general" },
                    color = agendaSecundario, fontSize = 11.sp, lineHeight = 13.sp)
                if (actividad.barrio.isNotBlank()) {
                    Text(actividad.barrio, color = agendaSecundario, fontSize = 11.sp)
                }
                if (actividad.grupoRiesgo.isNotBlank()) {
                    Text("Grupo " + integrantes.map { it.grupoRiesgo }.distinct().joinToString("/"),
                        color = agendaVerdeEstado, fontSize = 11.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(color = fondoEstado, shape = RoundedCornerShape(12.dp)) {
                    Text(estado, color = colorEstado(actividad, ahora), fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp))
                }
                Text("›", color = agendaSecundario, fontSize = 25.sp)
            }
        }
    }
}
private fun agruparVisitas(items: List<ActividadAgendaEntity>): List<List<ActividadAgendaEntity>> =
    items.groupBy { item ->
        if (item.origen == "SEGUIMIENTO" && item.estado == "PENDIENTE")
            "f_${item.fichaId}_${item.fechaHora}" else "a_${item.id}"
    }.values.toList()

private fun iconoActividad(tipo: String): Int = when (tipo) {
    "Visita domiciliaria" -> R.drawable.ruralitos_agenda_casita
    "Control prenatal" -> R.drawable.ruralitos_icono_red
    else -> R.drawable.ruralitos_icono_agenda
}

@Composable
private fun FormularioAgenda(
    usuarioId: Long,
    organizacionId: String,
    personas: List<PersonaAgenda>,
    personaInicial: PersonaAgenda?,
    original: ActividadAgendaEntity?,
    fechaInicial: Long,
    error: String,
    guardando: Boolean,
    onVolver: () -> Unit,
    onGuardar: (ActividadAgendaEntity) -> Unit
) {
    val context = LocalContext.current
    var fechaHora by remember(original?.id, fechaInicial) {
        mutableStateOf(original?.fechaHora ?: fechaSugerida(fechaInicial))
    }
    var tipo by remember(original?.id) { mutableStateOf(original?.tipo ?: agendaTipos.first()) }
    var nota by remember(original?.id) { mutableStateOf(original?.nota.orEmpty()) }
    var recordar by remember(original?.id) { mutableStateOf(original?.recordar ?: false) }
    var persona by remember(original?.id, personaInicial?.miembroId) {
        mutableStateOf(personas.firstOrNull { it.miembroId == original?.miembroId } ?: personaInicial.takeIf { original == null })
    }
    var personaModificada by remember(original?.id, personaInicial?.miembroId) { mutableStateOf(original == null && personaInicial != null) }
    var buscarPersona by remember { mutableStateOf("") }
    var elegirPersona by remember { mutableStateOf(false) }
    var elegirTipo by remember { mutableStateOf(false) }
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        recordar = granted
    }

    if (elegirPersona) {
        AlertDialog(
            onDismissRequest = { elegirPersona = false },
            title = { Text("Paciente o familia") },
            text = {
                Column {
                    OutlinedTextField(buscarPersona, { buscarPersona = it },
                        label = { Text("Buscar nombre o cédula") }, singleLine = true,
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    LazyColumn(Modifier.height(320.dp).padding(top = 8.dp)) {
                        item {
                            FilaBusquedaAgenda("Actividad general", "Sin paciente ni familia asociada") {
                                persona = null; personaModificada = true; elegirPersona = false
                            }
                        }
                        items(personas.filter {
                            buscarPersona.isBlank() || it.apellidosNombres.contains(buscarPersona, true) || it.cedula.contains(buscarPersona)
                        }.take(60), key = { it.miembroId }) { item ->
                            FilaBusquedaAgenda(item.apellidosNombres, "${item.cedula} · ${item.barrio}") {
                                persona = item; personaModificada = true; elegirPersona = false
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { elegirPersona = false }) { Text("Cerrar") } }
        )
    }

    PantallaRuralitos(
        titulo = if (original == null) "Nueva actividad" else "Editar actividad",
        subtitulo = "Disponible sin internet",
        onVolver = onVolver,
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = if (guardando) "Guardando…" else "Guardar actividad",
                enabled = !guardando,
                color = CianRuralitos,
                onClick = {
                    onGuardar(ActividadAgendaEntity(
                            id = original?.id ?: 0,
                            usuarioId = usuarioId,
                            fichaId = if (original?.origen == "SEGUIMIENTO") original.fichaId else if (personaModificada) persona?.fichaId else original?.fichaId,
                            miembroId = if (original?.origen == "SEGUIMIENTO") original.miembroId else if (personaModificada) persona?.miembroId else original?.miembroId,
                            persona = if (original?.origen == "SEGUIMIENTO") original.persona else if (personaModificada) persona?.apellidosNombres.orEmpty() else original?.persona.orEmpty(),
                            cedula = if (original?.origen == "SEGUIMIENTO") original.cedula else if (personaModificada) persona?.cedula.orEmpty() else original?.cedula.orEmpty(),
                            barrio = if (original?.origen == "SEGUIMIENTO") original.barrio else if (personaModificada) persona?.barrio.orEmpty() else original?.barrio.orEmpty(),
                            fechaHora = fechaHora,
                            tipo = tipo,
                            nota = nota.trim(),
                            estado = original?.estado ?: "PENDIENTE",
                            recordar = recordar,
                            creadoEn = original?.creadoEn ?: System.currentTimeMillis(),
                            origen = original?.origen ?: "MANUAL",
                            grupoRiesgo = original?.grupoRiesgo.orEmpty(),
                            fechaBase = original?.fechaBase,
                            fechaEditada = (original?.fechaEditada ?: false) ||
                                original?.origen == "SEGUIMIENTO",
                            organizacionId = original?.organizacionId?.ifBlank { organizacionId } ?: organizacionId,
                            syncId = original?.syncId ?: java.util.UUID.randomUUID().toString(),
                            syncVersion = original?.syncVersion ?: 0,
                            syncEstado = original?.syncEstado ?: "PENDIENTE",
                            actualizadoEn = original?.actualizadoEn ?: System.currentTimeMillis(),
                            eliminadoEn = original?.eliminadoEn
                        ))
                }
            )
        }
    ) {
        SeccionFormularioRuralitos(
            titulo = "Paciente o familia",
            descripcion = "Elige a quién corresponde la visita o deja una actividad general."
        ) {
            CampoAgenda(
                texto = if (personaModificada) persona?.apellidosNombres ?: "Actividad general"
                    else original?.persona?.takeIf { it.isNotBlank() } ?: "Actividad general",
                icono = R.drawable.ruralitos_icono_red,
                onClick = { elegirPersona = true }
            )
        }

        SeccionFormularioRuralitos(
            titulo = "Fecha y hora",
            descripcion = "Cuándo se realizará la actividad."
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Fecha", color = agendaSecundario, style = MaterialTheme.typography.labelLarge)
                    CampoAgenda(formato(fechaHora, "dd/MM/yyyy"), R.drawable.ruralitos_icono_agenda) {
                        val c = Calendar.getInstance().apply { timeInMillis = fechaHora }
                        DatePickerDialog(context, { _, y, m, d ->
                            fechaHora = Calendar.getInstance().apply {
                                timeInMillis = fechaHora
                                set(y, m, d)
                            }.timeInMillis
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Hora", color = agendaSecundario, style = MaterialTheme.typography.labelLarge)
                    CampoAgenda(formato(fechaHora, "HH:mm"), R.drawable.ruralitos_icono_agenda) {
                        val c = Calendar.getInstance().apply { timeInMillis = fechaHora }
                        TimePickerDialog(context, { _, h, m ->
                            fechaHora = Calendar.getInstance().apply {
                                timeInMillis = fechaHora
                                set(Calendar.HOUR_OF_DAY, h)
                                set(Calendar.MINUTE, m)
                            }.timeInMillis
                        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
                    }
                }
            }
        }

        SeccionFormularioRuralitos(
            titulo = "Tipo de actividad",
            descripcion = "Control, visita domiciliaria u otra actividad de salud."
        ) {
            Box {
                CampoAgenda(tipo, iconoActividad(tipo)) {
                    if (original?.origen != "SEGUIMIENTO") elegirTipo = true
                }
                MenuDesplegableRuralitos(expanded = elegirTipo && original?.origen != "SEGUIMIENTO", onDismissRequest = { elegirTipo = false }) {
                    agendaTipos.forEach { opcion ->
                        ItemMenuRuralitos(text = { Text(opcion) }, onClick = { tipo = opcion; elegirTipo = false })
                    }
                }
            }
        }

        SeccionFormularioRuralitos(
            titulo = "Notas",
            descripcion = "Opcional. Escribe el motivo de la visita u observaciones."
        ) {
            OutlinedTextField(
                nota, { nota = it },
                placeholder = { Text("Motivo de la visita, observaciones…") },
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        SeccionFormularioRuralitos(
            titulo = "Recordatorio"
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ruralitos_icono_agenda), null, Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Avisarme a la hora indicada", color = agendaAzul, fontWeight = FontWeight.SemiBold)
                    Text("El aviso aparece en este dispositivo", color = agendaSecundario, style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = recordar, onCheckedChange = { activo ->
                    if (activo && Build.VERSION.SDK_INT >= 33) permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else recordar = activo
                })
            }
        }

        if (error.isNotBlank()) {
            Text(error, color = Color(0xFFD93F4C), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FilaBusquedaAgenda(titulo: String, detalle: String?, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp)
    ) {
        Text(titulo, color = agendaAzul, fontWeight = FontWeight.SemiBold)
        detalle?.let {
            Text(it, color = agendaSecundario, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
    HorizontalDivider(color = Color(0xFFE2ECF1))
}

@Composable
private fun CampoAgenda(texto: String, icono: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFCFDDE5))
    ) {
        Row(Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(icono), null, Modifier.size(28.dp))
            Text(texto, modifier = Modifier.weight(1f).padding(start = 10.dp), color = agendaAzul,
                style = MaterialTheme.typography.bodyLarge)
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE3F4F7)),
                contentAlignment = Alignment.Center
            ) {
                FlechaDesplegable(color = agendaVerde)
            }
        }
    }
}

private fun inicioDia(tiempo: Long): Long = Calendar.getInstance().apply {
    timeInMillis = tiempo
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun moverDias(tiempo: Long, dias: Int): Long = Calendar.getInstance().apply {
    timeInMillis = tiempo
    add(Calendar.DAY_OF_MONTH, dias)
}.timeInMillis

private fun fechaSugerida(dia: Long): Long {
    val propuesta = Calendar.getInstance().apply {
        timeInMillis = dia
        set(Calendar.HOUR_OF_DAY, 8)
        set(Calendar.MINUTE, 30)
    }
    if (inicioDia(dia) == inicioDia(System.currentTimeMillis()) && propuesta.timeInMillis <= System.currentTimeMillis()) {
        propuesta.timeInMillis = System.currentTimeMillis()
        propuesta.add(Calendar.HOUR_OF_DAY, 1)
        propuesta.set(Calendar.MINUTE, 0)
    }
    propuesta.set(Calendar.SECOND, 0)
    propuesta.set(Calendar.MILLISECOND, 0)
    return propuesta.timeInMillis
}

private fun formato(tiempo: Long, patron: String): String =
    SimpleDateFormat(patron, Locale("es", "EC")).format(Date(tiempo))
