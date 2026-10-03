package com.ruralitos.app.ui.screens

import com.ruralitos.app.ui.components.EncabezadoPantallaRuralitos
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.statusBarsPadding
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.R
import com.ruralitos.app.ui.components.ListaDeColumnasAdaptable
import com.ruralitos.app.data.agenda.RecordatorioNota
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.NotaDiariaConPersona
import com.ruralitos.app.data.local.entity.NotaDiariaEntity
import com.ruralitos.app.data.local.entity.PersonaParaNota
import com.ruralitos.app.ui.components.BotonVolverRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val FondoNotasDiarias = Color(0xFFF6F9FB)
private val AzulTituloNotas = Color(0xFF0A2A5E)
private val AzulEtiquetaNotas = Color(0xFF1565C0)
private val GrisTextoNotas = Color(0xFF5B7083)
private val BordeCampoNotas = Color(0xFFE2ECF1)
private val FondoTarjetaNotas = Color(0xFFF6F9FB)
private val VerdeBotonNotas = Color(0xFF0889A0)

@Composable
fun NotasDiariasScreen(
    organizacionId: String,
    usuarioId: Long,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    val database = remember(context) {
        RuralitosDatabase.obtenerBaseDatos(context)
    }

    val dao = remember(database) { database.notaDiariaDao() }
    val notas by remember(dao, organizacionId, usuarioId) { dao.observarTodas(organizacionId, usuarioId) }
        .collectAsState(initial = emptyList())
    val personas by remember(dao, organizacionId) { dao.observarPersonas(organizacionId) }
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val permisoAvisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    var busqueda by remember { mutableStateOf("") }
    var fechaInicio by remember { mutableStateOf("") }
    var fechaFin by remember { mutableStateOf("") }
    var fechaEnEdicion by remember { mutableStateOf<String?>(null) }
    var hoy by remember { mutableStateOf(fechaLocal()) }
    var pagina by remember { mutableIntStateOf(0) }
    var personaAbierta by remember { mutableStateOf<PersonaParaNota?>(null) }
    var sesionNota by remember { mutableStateOf<SesionNotaEnPantalla?>(null) }
    var textoNota by remember { mutableStateOf("") }
    var errorGuardado by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            permisoAvisos.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        runCatching { RecordatorioNota.recuperarPendientes(context) }
        while (true) {
            delay(30_000)
            hoy = fechaLocal()
        }
    }

    val texto = busqueda.trim()
    val rangoValido = fechaInicio.isBlank() || fechaFin.isBlank() || fechaInicio <= fechaFin
    val notasPeriodo = remember(notas, fechaInicio, fechaFin, hoy) {
        notas.filter { nota ->
            if (fechaInicio.isBlank() && fechaFin.isBlank()) nota.fechaLocal == hoy
            else (fechaInicio.isBlank() || nota.fechaLocal >= fechaInicio) &&
                (fechaFin.isBlank() || nota.fechaLocal <= fechaFin)
        }
    }
    val notasPorPersona = remember(notasPeriodo) { notasPeriodo.groupBy { it.miembroId } }
    val visibles = remember(personas, texto, notasPorPersona, rangoValido) {
        if (!rangoValido) emptyList() else
        personas.filter {
            if (texto.isEmpty()) notasPorPersona.containsKey(it.miembroId)
            else it.apellidosNombres.contains(texto, ignoreCase = true) ||
                it.cedula.contains(texto, ignoreCase = true)
        }
    }
    LaunchedEffect(texto, fechaInicio, fechaFin) { pagina = 0 }
    val totalPaginas = maxOf(1, (visibles.size + 2) / 3)
    val paginaActual = pagina.coerceIn(0, totalPaginas - 1)
    val personasPagina = visibles.drop(paginaActual * 3).take(3)

    suspend fun guardarNota(sesion: SesionNotaEnPantalla?, contenido: String) {
        if (sesion == null) return
        withContext(NonCancellable) {
            sesion.mutex.withLock {
                withContext(Dispatchers.IO) {
                    val valor = contenido.trim()
                    if (valor.isBlank()) {
                        sesion.notaId?.let { dao.eliminar(it) }
                        sesion.notaId?.let { RecordatorioNota.cancelar(context, it) }
                        sesion.notaId = null
                    } else if (sesion.notaId == null) {
                        sesion.notaId = dao.crear(
                            NotaDiariaEntity(
                                miembroId = sesion.miembroId,
                                fechaLocal = fechaLocal(),
                                contenido = valor,
                                usuarioId = usuarioId
                            )
                        )
                        sesion.notaId?.let { dao.buscar(it)?.let { nota -> RecordatorioNota.actualizar(context, nota) } }
                    } else {
                        dao.actualizarContenido(sesion.notaId!!, valor)
                    }
                }
            }
        }
    }

    fun abrirNota(persona: PersonaParaNota, nota: NotaDiariaConPersona?) {
        personaAbierta = persona
        sesionNota = SesionNotaEnPantalla(persona.miembroId, nota?.id)
        textoNota = nota?.contenido.orEmpty()
        errorGuardado = false
    }

    fun cerrarNota() {
        val sesion = sesionNota
        val contenido = textoNota
        personaAbierta = null
        sesionNota = null
        scope.launch {
            try {
                guardarNota(sesion, contenido)
                if (sesion?.notaId != null && !sesion.fechaExistente) { fechaInicio = ""; fechaFin = "" }
            } catch (_: Exception) { errorGuardado = true }
        }
    }

    LaunchedEffect(sesionNota, textoNota) {
        val sesion = sesionNota ?: return@LaunchedEffect
        delay(450)
        try {
            guardarNota(sesion, textoNota)
            errorGuardado = false
            if (!sesion.fechaExistente && sesion.notaId != null) { fechaInicio = ""; fechaFin = "" }
        } catch (_: Exception) { errorGuardado = true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .formularioSeguro()
            .background(FondoNotasDiarias)
    ) {
        EncabezadoPantallaRuralitos(
            titulo = "Notas diarias",
            subtitulo = null,
            paso = null,
            totalPasos = null,
            etiquetaPaso = "",
            onVolver = onRegresar,
            descripcion = "Notas importantes de las personas registradas"
        )
        ListaDeColumnasAdaptable(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp),
            espacio = 14.dp
        ) {

        item {
            CampoBusquedaNotas(
                valor = busqueda,
                onCambio = { busqueda = it },
                texto = "Buscar por nombre o cédula",
                iconoRes = R.drawable.notasdiarias_icono_busqueda_nombre_cedula_2026
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CampoFechaNotas("Desde", fechaInicio, Modifier.weight(1f)) {
                    fechaEnEdicion = "inicio"
                }
                CampoFechaNotas("Hasta", fechaFin, Modifier.weight(1f)) {
                    fechaEnEdicion = "fin"
                }
            }
        }
        if (fechaInicio.isNotBlank() || fechaFin.isNotBlank()) item {
            TextButton(onClick = { fechaInicio = ""; fechaFin = "" },
                modifier = Modifier.padding(start = 18.dp)) {
                Text("Quitar fechas", color = VerdeBotonNotas)
            }
        }
        if (!rangoValido) item {
            Text("La fecha de inicio no puede ser posterior a la fecha de fin.",
                color = Color(0xFFB3261E), modifier = Modifier.padding(horizontal = 18.dp))
        }

        if (visibles.isEmpty()) {
            item {
                TarjetaSinNotas(
                    titulo = "Personas · 0",
                    mensaje = when {
                        !rangoValido -> "Revisa las fechas seleccionadas."
                        texto.isNotEmpty() -> "No hay personas que coincidan con la búsqueda."
                        fechaInicio.isNotBlank() || fechaFin.isNotBlank() -> "No hay notas en ese intervalo. Busca por nombre o cédula para crear una nueva."
                        else -> "No hay notas para hoy. Busca una persona para agregar una nota."
                    }
                )
            }
        } else {
            item {
                Text(
                    text = "Personas · ${visibles.size}",
                    color = AzulEtiquetaNotas,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(
                        start = 18.dp,
                        end = 18.dp,
                        top = 2.dp
                    )
                )
            }

            items(
                items = personasPagina,
                key = { it.miembroId }
            ) { persona ->
                val notasPersona = notasPorPersona[persona.miembroId].orEmpty()
                TarjetaNotaDiaria(
                    persona = persona,
                    cantidadNotas = notasPersona.size,
                    onClick = { abrirNota(persona, notasPersona.firstOrNull()) }
                )
            }
            if (totalPaginas > 1) {
                item {
                    PaginacionNotas(
                        pagina = paginaActual,
                        total = totalPaginas,
                        onAnterior = { pagina = (paginaActual - 1).coerceAtLeast(0) },
                        onSiguiente = { pagina = (paginaActual + 1).coerceAtMost(totalPaginas - 1) }
                    )
                }
            }
        }


        item {
            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }
    }
    }

    fechaEnEdicion?.let { tipo ->
        SelectorFechaNotasDialog(
            titulo = if (tipo == "inicio") "Fecha de inicio" else "Fecha de fin",
            valorActual = if (tipo == "inicio") fechaInicio else fechaFin,
            onFecha = { valor ->
                if (tipo == "inicio") fechaInicio = valor else fechaFin = valor
                fechaEnEdicion = null
            },
            onCerrar = { fechaEnEdicion = null }
        )
    }
    personaAbierta?.let { persona ->
        DialogoNotaDiaria(
            persona = persona,
            notas = notasPorPersona[persona.miembroId].orEmpty(),
            notaSeleccionadaId = sesionNota?.notaId,
            notaRealizada = notas.firstOrNull { it.id == sesionNota?.notaId }?.realizada ?: false,
            texto = textoNota,
            errorGuardado = errorGuardado,
            onTextoCambio = { textoNota = it },
            onRealizadaCambio = { realizada ->
                val id = sesionNota?.notaId ?: return@DialogoNotaDiaria
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) { dao.marcarRealizada(id, realizada) }
                        if (realizada) RecordatorioNota.cancelar(context, id)
                        else dao.buscar(id)?.let { RecordatorioNota.actualizar(context, it) }
                    }.onFailure { errorGuardado = true }
                }
            },
            onSeleccionar = { nota ->
                val anterior = sesionNota
                val contenido = textoNota
                scope.launch { guardarNota(anterior, contenido) }
                sesionNota = SesionNotaEnPantalla(persona.miembroId, nota.id)
                textoNota = nota.contenido
            },
            onNueva = {
                val anterior = sesionNota
                val contenido = textoNota
                scope.launch { guardarNota(anterior, contenido) }
                sesionNota = SesionNotaEnPantalla(persona.miembroId)
                textoNota = ""
            },
            onCerrar = ::cerrarNota
        )
    }
}

private class SesionNotaEnPantalla(
    val miembroId: Long,
    var notaId: Long? = null,
    val fechaExistente: Boolean = notaId != null
) { val mutex = Mutex() }

@Composable
private fun CabeceraNotasConOnda(onVolver: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White).statusBarsPadding()) {
        BotonVolverRuralitos(onVolver, Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp))
        HorizontalDivider(color = Color(0xFFE2ECF1))
    }
}

@Composable
private fun CampoBusquedaNotas(
    valor: String,
    onCambio: (String) -> Unit,
    texto: String,
    iconoRes: Int
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        placeholder = {
            Text(
                text = texto,
                color = GrisTextoNotas,
                fontSize = 16.sp
            )
        },
        leadingIcon = {
            Image(
                painter = painterResource(iconoRes),
                contentDescription = null,
                modifier = Modifier.size(31.dp),
                contentScale = ContentScale.Fit
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .heightIn(min = 66.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF9BCFF0),
            unfocusedBorderColor = BordeCampoNotas,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor = AzulEtiquetaNotas
        )
    )
}

@Composable
private fun CampoFechaNotas(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.heightIn(min = 66.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BordeCampoNotas)
    ) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.notasdiarias_icono_calendario_busqueda_mes_2026),
                contentDescription = null,
                modifier = Modifier.size(25.dp)
            )
            Column(Modifier.padding(start = 7.dp)) {
                Text(etiqueta, color = GrisTextoNotas, fontSize = 12.sp)
                Text(if (valor.isBlank()) "Elegir fecha" else
                    valor.substring(8, 10) + "/" + valor.substring(5, 7) + "/" + valor.substring(0, 4),
                    color = if (valor.isBlank()) GrisTextoNotas else AzulTituloNotas,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SelectorFechaNotasDialog(
    titulo: String,
    valorActual: String,
    onFecha: (String) -> Unit,
    onCerrar: () -> Unit
) {
    val inicial = remember(valorActual) {
        Calendar.getInstance().apply {
            if (valorActual.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                set(valorActual.substring(0, 4).toInt(), valorActual.substring(5, 7).toInt() - 1,
                    valorActual.substring(8, 10).toInt())
            }
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
    }
    var mesActual by remember(valorActual) { mutableLongStateOf(inicial) }
    val calendario = Calendar.getInstance().apply { timeInMillis = mesActual }
    val desplazamiento = (calendario.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val dias = calendario.getActualMaximum(Calendar.DAY_OF_MONTH)
    val filas = (desplazamiento + dias + 6) / 7
    AlertDialog(
        onDismissRequest = onCerrar,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        title = { Text(titulo, color = AzulTituloNotas, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        mesActual = Calendar.getInstance().apply {
                            timeInMillis = mesActual
                            add(Calendar.MONTH, -1)
                        }.timeInMillis
                    }) { Text("‹", color = AzulEtiquetaNotas, fontSize = 26.sp) }
                    Text(SimpleDateFormat("MMMM yyyy", Locale("es", "EC")).format(Date(mesActual))
                        .replaceFirstChar { it.uppercase() },
                        modifier = Modifier.weight(1f),
                        color = AzulTituloNotas, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center)
                    TextButton(onClick = {
                        mesActual = Calendar.getInstance().apply {
                            timeInMillis = mesActual
                            add(Calendar.MONTH, 1)
                        }.timeInMillis
                    }) { Text("›", color = AzulEtiquetaNotas, fontSize = 26.sp) }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("L", "M", "M", "J", "V", "S", "D").forEach { nombre ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(nombre, color = GrisTextoNotas, fontSize = 12.sp)
                        }
                    }
                }
                repeat(filas) { fila ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(7) { columna ->
                            val numero = fila * 7 + columna - desplazamiento + 1
                            if (numero !in 1..dias) {
                                Box(Modifier.weight(1f).height(35.dp))
                            } else {
                                val valor = String.format(Locale.US, "%04d-%02d-%02d",
                                    calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH) + 1, numero)
                                Surface(
                                    onClick = { onFecha(valor) },
                                    modifier = Modifier.weight(1f).height(35.dp),
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (valor == valorActual) VerdeBotonNotas else FondoTarjetaNotas
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(numero.toString(),
                                            color = if (valor == valorActual) Color.White else AzulTituloNotas,
                                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar", color = AzulEtiquetaNotas) } },
        dismissButton = {
            if (valorActual.isNotBlank()) {
                TextButton(onClick = { onFecha("") }) { Text("Quitar fecha", color = VerdeBotonNotas) }
            }
        }
    )
}

@Composable
private fun TarjetaSinNotas(
    titulo: String,
    mensaje: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FondoTarjetaNotas
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFE2ECF1)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp,
                    vertical = 20.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(
                    id = R.drawable.notasdiarias_ilustracion_sin_notas_hoy_2026
                ),
                contentDescription = null,
                modifier = Modifier.size(132.dp),
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = titulo,
                    color = AzulTituloNotas,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = mensaje,
                    color = GrisTextoNotas,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun TarjetaNotaDiaria(
    persona: PersonaParaNota,
    cantidadNotas: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFE2ECF1)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.5.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = persona.apellidosNombres.ifBlank { "Persona sin nombre" },
                color = AzulTituloNotas,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = "Cédula: ${persona.cedula.ifBlank { "Sin registrar" }}",
                color = GrisTextoNotas,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "Diagnóstico: ${persona.diagnosticos.ifBlank { "Sin diagnóstico registrado" }}",
                color = Color(0xFF334A67),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (cantidadNotas == 0) "Sin nota en este período"
                    else if (cantidadNotas == 1) "1 nota en este período"
                    else "$cantidadNotas notas en este período",
                    color = GrisTextoNotas,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeBotonNotas)
                ) {
                    Text(if (cantidadNotas == 0) "Agregar" else "Ver")
                }
            }
        }
    }
}

@Composable
private fun PaginacionNotas(
    pagina: Int,
    total: Int,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButton(onClick = onAnterior, enabled = pagina > 0) { Text("Anterior") }
        Text("${pagina + 1} de $total", color = AzulTituloNotas, fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = onSiguiente, enabled = pagina + 1 < total) { Text("Siguiente") }
    }
}

@Composable
private fun DialogoNotaDiaria(
    persona: PersonaParaNota,
    notas: List<NotaDiariaConPersona>,
    notaSeleccionadaId: Long?,
    notaRealizada: Boolean,
    texto: String,
    errorGuardado: Boolean,
    onTextoCambio: (String) -> Unit,
    onRealizadaCambio: (Boolean) -> Unit,
    onSeleccionar: (NotaDiariaConPersona) -> Unit,
    onNueva: () -> Unit,
    onCerrar: () -> Unit
) {
    var marcada by remember(notaSeleccionadaId, notaRealizada) { mutableStateOf(notaRealizada) }
    AlertDialog(
        onDismissRequest = onCerrar,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        title = {
            Column {
                Text(persona.apellidosNombres.ifBlank { "Persona sin nombre" },
                    color = AzulTituloNotas, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Cédula: ${persona.cedula.ifBlank { "Sin registrar" }}",
                    color = GrisTextoNotas, fontSize = 13.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (notas.size > 1) {
                    Text("Notas de este período", color = AzulEtiquetaNotas, fontWeight = FontWeight.SemiBold)
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 116.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(notas, key = { it.id }) { nota ->
                            Text(
                                text = "${nota.fechaLocal} · ${nota.contenido.take(35)}",
                                color = if (nota.id == notaSeleccionadaId) VerdeBotonNotas else AzulTituloNotas,
                                modifier = Modifier.fillMaxWidth().clickable { onSeleccionar(nota) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = texto,
                    onValueChange = onTextoCambio,
                    label = { Text("Nota importante") },
                    placeholder = { Text("Escribe la nota de esta persona") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulEtiquetaNotas,
                        unfocusedBorderColor = BordeCampoNotas
                    )
                )
                Text(
                    if (errorGuardado) "No se pudo guardar. Inténtalo de nuevo."
                    else "Se guarda automáticamente en este dispositivo.",
                    color = if (errorGuardado) Color(0xFFB3261E) else GrisTextoNotas,
                    fontSize = 12.sp
                )
                if (notaSeleccionadaId != null) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            marcada = !marcada
                            onRealizadaCambio(marcada)
                        }) {
                        Checkbox(
                            checked = marcada,
                            onCheckedChange = { marcada = it; onRealizadaCambio(it) },
                            colors = CheckboxDefaults.colors(checkedColor = VerdeBotonNotas)
                        )
                        Column {
                            Text("Realizada", color = AzulTituloNotas, fontWeight = FontWeight.SemiBold)
                            Text("Al marcarla se detienen los avisos.", color = GrisTextoNotas, fontSize = 12.sp)
                        }
                    }
                }
                if (notaSeleccionadaId != null) {
                    TextButton(onClick = onNueva) { Text("Nueva nota de hoy", color = VerdeBotonNotas) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar", color = AzulEtiquetaNotas) }
        }
    )
}

@Composable
private fun BotonVolverNotas(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .heightIn(min = 60.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = VerdeBotonNotas
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = VerdeBotonNotas
        )
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.notasdiarias_icono_regresar_inicio_casa_2026
            ),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "Volver al inicio",
            color = VerdeBotonNotas,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

private fun fechaLocal(): String =
    SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.US
    ).format(Date())
