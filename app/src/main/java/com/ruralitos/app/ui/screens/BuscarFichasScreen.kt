package com.ruralitos.app.ui.screens

import com.ruralitos.app.ui.theme.FondoClinico
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.ruralitos.app.R
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.BarraAccionPantallaRuralitos
import com.ruralitos.app.ui.components.EncabezadoPantallaRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.VerdeSalud
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val FICHAS_POR_PAGINA = 5

private enum class CampoFechaBusqueda { INICIO, FIN }

private data class FiltrosFichasAplicados(
    val texto: String,
    val unidad: String,
    val sector: String,
    val fechaInicio: String,
    val fechaFin: String,
    val estado: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuscarFichasScreen(
    onFichaSeleccionada: (FichaFamiliarEntity) -> Unit,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember(context) { RuralitosDatabase.obtenerBaseDatos(context).fichaFamiliarDao() }
    val fechaHoy = remember { fechaVisible(Date()) }
    var texto by remember { mutableStateOf("") }
    var unidad by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("") }
    var fechaInicio by remember { mutableStateOf("") }
    var fechaFin by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("ACTIVAS") }
    var filtrosAplicados by remember {
        mutableStateOf(
            FiltrosFichasAplicados(
                texto = "",
                unidad = "",
                sector = "",
                fechaInicio = "",
                fechaFin = "",
                estado = "ACTIVAS"
            )
        )
    }
    var pagina by remember { mutableIntStateOf(1) }
    var selectorFecha by remember { mutableStateOf<CampoFechaBusqueda?>(null) }
    var errorFiltros by remember { mutableStateOf<String?>(null) }
    var filtrosAvanzados by remember { mutableStateOf(false) }

    fun buscar() {
        val inicioMillis = fechaParaSelectorMillis(fechaInicio)
        val finMillis = fechaParaSelectorMillis(fechaFin)
        errorFiltros = when {
            fechaInicio.isNotBlank() && inicioMillis == null -> "Selecciona una fecha inicial válida."
            fechaFin.isNotBlank() && finMillis == null -> "Selecciona una fecha final válida."
            inicioMillis != null && finMillis != null && inicioMillis > finMillis -> "La fecha inicial no puede ser posterior a la fecha final."
            else -> null
        }
        if (errorFiltros == null) {
            filtrosAplicados = FiltrosFichasAplicados(
                texto = texto.trim(),
                unidad = unidad.trim(),
                sector = sector.trim(),
                fechaInicio = fechaInicio,
                fechaFin = fechaFin,
                estado = estado
            )
            pagina = 1
        }
    }

    val flujo = remember(filtrosAplicados, pagina) {
        dao.buscarFichasFiltradasPaginadas(
            texto = filtrosAplicados.texto,
            unidad = filtrosAplicados.unidad,
            sector = filtrosAplicados.sector,
            fechaInicioClave = fechaAClaveOrdenable(filtrosAplicados.fechaInicio),
            fechaFinClave = fechaAClaveOrdenable(filtrosAplicados.fechaFin),
            estado = filtrosAplicados.estado,
            limite = FICHAS_POR_PAGINA,
            desplazamiento = (pagina - 1) * FICHAS_POR_PAGINA
        )
    }
    val totalFlujo = remember(filtrosAplicados) {
        dao.contarFichasFiltradas(
            texto = filtrosAplicados.texto,
            unidad = filtrosAplicados.unidad,
            sector = filtrosAplicados.sector,
            fechaInicioClave = fechaAClaveOrdenable(filtrosAplicados.fechaInicio),
            fechaFinClave = fechaAClaveOrdenable(filtrosAplicados.fechaFin),
            estado = filtrosAplicados.estado
        )
    }
    val fichas by flujo.collectAsState(initial = emptyList())
    val total by totalFlujo.collectAsState(initial = 0)
    val totalPaginas = maxOf(1, (total + FICHAS_POR_PAGINA - 1) / FICHAS_POR_PAGINA)

    LaunchedEffect(totalPaginas) {
        if (pagina > totalPaginas) pagina = totalPaginas
    }

    selectorFecha?.let { campo ->
        val fechaSeleccionada = if (campo == CampoFechaBusqueda.INICIO) fechaInicio else fechaFin
        val selector = rememberDatePickerState(
            initialSelectedDateMillis = fechaParaSelectorMillis(fechaSeleccionada)
                ?: fechaParaSelectorMillis(fechaHoy)
        )
        DatePickerDialog(
            onDismissRequest = { selectorFecha = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        selector.selectedDateMillis?.let { millis ->
                            if (campo == CampoFechaBusqueda.INICIO) {
                                fechaInicio = millisAFechaVisible(millis)
                            } else {
                                fechaFin = millisAFechaVisible(millis)
                            }
                        }
                        errorFiltros = null
                        selectorFecha = null
                    }
                ) { Text("Usar esta fecha") }
            },
            dismissButton = {
                TextButton(onClick = { selectorFecha = null }) { Text("Cancelar") }
            }
        ) {
            DatePicker(
                state = selector,
                title = {
                    Text(
                        if (campo == CampoFechaBusqueda.INICIO) "Selecciona la fecha inicial"
                        else "Selecciona la fecha final",
                        modifier = Modifier.padding(24.dp)
                    )
                },
                showModeToggle = true
            )
        }
    }

Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoClinico)
            .formularioSeguro()
    ) {
        EncabezadoPantallaRuralitos(
            titulo = "Buscar y modificar fichas",
            subtitulo = "Encuentra una familia por cualquiera de sus integrantes.",
            paso = null,
            totalPasos = null,
            etiquetaPaso = "",
            onVolver = onRegresar
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "busqueda") {
                TarjetaBusquedaFichas(
                    texto = texto,
                    onTextoCambio = { texto = it },
                    filtrosAvanzados = filtrosAvanzados,
                    onAlternarFiltros = {
                        filtrosAvanzados = !filtrosAvanzados
                    },
                    fechaInicio = fechaInicio,
                    fechaFin = fechaFin,
                    onFechaInicio = {
                        selectorFecha = CampoFechaBusqueda.INICIO
                    },
                    onFechaFin = {
                        selectorFecha = CampoFechaBusqueda.FIN
                    },
                    unidad = unidad,
                    onUnidadCambio = { unidad = it },
                    sector = sector,
                    onSectorCambio = { sector = it },
                    estado = estado,
                    onCambiarEstado = {
                        estado = siguienteEstado(estado)
                    },
                    errorFiltros = errorFiltros,
                    onBuscar = ::buscar
                )
            }

            item(key = "titulo_resultados") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = Color.White.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp,
                    border = BorderStroke(
                        1.dp,
                        AzulClinico.copy(alpha = 0.10f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 17.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                color = AzulClinico.copy(alpha = 0.10f),
                                shape = CircleShape
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "●●",
                                        color = AzulClinico,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text(
                                text = "Resultados",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }

                        Text(
                            text = "$total ficha(s)",
                            color = AzulClinico,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            if (fichas.isEmpty()) {
                item(key = "sin_resultados") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = Color.White.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            AzulClinico.copy(alpha = 0.12f)
                        ),
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp)
                        ) {
                            Text(
                                text = "No encontramos fichas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "No hay fichas con los filtros aplicados.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                    }
                }
            }

            items(
                items = fichas,
                key = { it.id }
            ) { ficha ->
                TarjetaFichaElegante(
                    ficha = ficha,
                    onClick = {
                        onFichaSeleccionada(ficha)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (totalPaginas > 1) {
                item(key = "paginacion") {
                    MenuPaginacion(
                        paginaActual = pagina,
                        totalPaginas = totalPaginas,
                        onCambiarPagina = { pagina = it },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaEncabezadoBusqueda(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            AzulClinico.copy(alpha = 0.09f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "ARCHIVO FAMILIAR",
                    color = AzulClinico,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Buscar y modificar fichas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0A2A5E),
                    modifier = Modifier.padding(top = 5.dp)
                )

                Text(
                    text = "Encuentra una familia por cualquiera de sus integrantes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }

            Surface(
                modifier = Modifier
                    .size(58.dp)
                    .padding(start = 4.dp),
                color = AzulClinico.copy(alpha = 0.10f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    IconoCarpetaBusqueda(
                        color = AzulClinico,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaBusquedaFichas(
    texto: String,
    onTextoCambio: (String) -> Unit,
    filtrosAvanzados: Boolean,
    onAlternarFiltros: () -> Unit,
    fechaInicio: String,
    fechaFin: String,
    onFechaInicio: () -> Unit,
    onFechaFin: () -> Unit,
    unidad: String,
    onUnidadCambio: (String) -> Unit,
    sector: String,
    onSectorCambio: (String) -> Unit,
    estado: String,
    onCambiarEstado: () -> Unit,
    errorFiltros: String?,
    onBuscar: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Color.White.copy(alpha = 0.97f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            AzulClinico.copy(alpha = 0.10f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    color = AzulClinico.copy(alpha = 0.10f),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IconoLupa(
                            color = Color(0xFF315C95),
                            modifier = Modifier.size(29.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 13.dp)
                ) {
                    Text(
                        text = "Buscar fichas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0A2A5E)
                    )

                    Text(
                        text = "Por nombre o cédula de cualquier integrante.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            OutlinedTextField(
                value = texto,
                onValueChange = onTextoCambio,
                label = { Text("Nombre o cédula") },
                leadingIcon = {
                    IconoUsuarioBusqueda(
                        color = Color(0xFF5B7083),
                        modifier = Modifier.size(25.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Surface(
                onClick = onAlternarFiltros,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                color = CianRuralitos.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    CianRuralitos.copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        color = CianRuralitos.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "≡",
                                color = CianRuralitos,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 11.dp)
                    ) {
                        Text(
                            text = if (filtrosAvanzados) {
                                "Ocultar filtros"
                            } else {
                                "Más filtros"
                            },
                            color = CianRuralitos,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Fechas, barrio y estado",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = if (filtrosAvanzados) "⌃" else "›",
                        color = CianRuralitos,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp
                    )
                }
            }

            AnimatedVisibility(
                visible = filtrosAvanzados
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    BoxWithConstraints(
                        Modifier.fillMaxWidth()
                    ) {
                        if (maxWidth < 620.dp) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SelectorFechaBusqueda(
                                    etiqueta = "Fecha inicial",
                                    fecha = fechaInicio,
                                    onClick = onFechaInicio
                                )

                                SelectorFechaBusqueda(
                                    etiqueta = "Fecha final",
                                    fecha = fechaFin,
                                    onClick = onFechaFin
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SelectorFechaBusqueda(
                                    etiqueta = "Fecha inicial",
                                    fecha = fechaInicio,
                                    onClick = onFechaInicio,
                                    modifier = Modifier.weight(1f)
                                )

                                SelectorFechaBusqueda(
                                    etiqueta = "Fecha final",
                                    fecha = fechaFin,
                                    onClick = onFechaFin,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    BoxWithConstraints(
                        Modifier.fillMaxWidth()
                    ) {
                        if (maxWidth < 620.dp) {
                            Column {
                                CampoFiltro(
                                    unidad,
                                    onUnidadCambio,
                                    "Unidad operativa"
                                )

                                CampoFiltro(
                                    sector,
                                    onSectorCambio,
                                    "Sector o barrio"
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement =
                                    Arrangement.spacedBy(12.dp)
                            ) {
                                CampoFiltro(
                                    unidad,
                                    onUnidadCambio,
                                    "Unidad operativa",
                                    Modifier.weight(1f)
                                )

                                CampoFiltro(
                                    sector,
                                    onSectorCambio,
                                    "Sector o barrio",
                                    Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    val colorEstado = colorEstado(estado)

                    Surface(
                        onClick = onCambiarEstado,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = colorEstado.copy(alpha = 0.08f),
                        border = BorderStroke(
                            1.dp,
                            colorEstado.copy(alpha = 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 15.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Estado de las fichas",
                                    style = MaterialTheme.typography.labelLarge
                                )

                                Text(
                                    text = nombreEstado(estado),
                                    color = colorEstado,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Text(
                                text = "Cambiar",
                                color = colorEstado,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            errorFiltros?.let {
                MensajeEstadoRuralitos(
                    titulo = "Revisa el rango de fechas",
                    descripcion = it,
                    color = RojoClinico,
                    simbolo = "!",
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Surface(
                onClick = onBuscar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(62.dp),
                color = Color(0xFF1565C0),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconoLupa(
                        color = Color.White,
                        modifier = Modifier.size(27.dp)
                    )

                    Text(
                        text = "Buscar fichas",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuPaginacion(
    paginaActual: Int,
    totalPaginas: Int,
    onCambiarPagina: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val inicio = when {
        totalPaginas <= 5 -> 1
        paginaActual <= 3 -> 1
        paginaActual >= totalPaginas - 2 -> totalPaginas - 4
        else -> paginaActual - 2
    }

    val paginas = inicio..minOf(inicio + 4, totalPaginas)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.96f),
        border = BorderStroke(
            1.dp,
            AzulClinico.copy(alpha = 0.12f)
        ),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Página $paginaActual de $totalPaginas",
                color = AzulClinico,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.padding(top = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotonPagina(
                    "‹",
                    paginaActual > 1,
                    false
                ) {
                    onCambiarPagina(paginaActual - 1)
                }

                paginas.forEach { pagina ->
                    BotonPagina(
                        pagina.toString(),
                        true,
                        pagina == paginaActual
                    ) {
                        onCambiarPagina(pagina)
                    }
                }

                BotonPagina(
                    "›",
                    paginaActual < totalPaginas,
                    false
                ) {
                    onCambiarPagina(paginaActual + 1)
                }
            }
        }
    }
}

@Composable
private fun BotonPagina(
    texto: String,
    habilitado: Boolean,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(39.dp)
            .clickable(
                enabled = habilitado,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = when {
            seleccionado -> AzulClinico
            habilitado -> AzulClinico.copy(alpha = 0.10f)
            else -> Color(0xFFF1F3F5)
        },
        border = BorderStroke(
            1.dp,
            if (seleccionado) {
                AzulClinico
            } else {
                AzulClinico.copy(alpha = 0.20f)
            }
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = texto,
                color = when {
                    seleccionado -> Color.White
                    habilitado -> AzulClinico
                    else -> Color(0xFFADB5BD)
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TarjetaFichaElegante(
    ficha: FichaFamiliarEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = colorEstado(ficha.estado)

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.98f),
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            AzulClinico.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ficha.numeroFichaFamiliar
                                .takeLast(3)
                                .ifBlank { "F" },
                            color = color,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = ficha.nombreApellidoJefeFamilia
                            .ifBlank { "Familia sin nombre" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF0A2A5E),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Ficha ${ficha.numeroFichaFamiliar} · ${ficha.fechaLlenado}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = color.copy(alpha = 0.13f)
                ) {
                    Text(
                        text = nombreEstado(ficha.estado),
                        color = color,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 6.dp
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = AzulClinico.copy(alpha = 0.08f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "▣",
                            color = Color(0xFF5B7083),
                            fontSize = 15.sp
                        )
                    }
                }

                Text(
                    text = "${ficha.cedulaJefeHogar} · ${
                        ficha.barrio.ifBlank {
                            ficha.comunidad.ifBlank {
                                ficha.sector.ifBlank {
                                    ficha.parroquia
                                }
                            }
                        }
                    }",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 9.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "›",
                    color = AzulClinico,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SelectorFechaBusqueda(
    etiqueta: String,
    fecha: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(
            1.dp,
            AzulClinico.copy(alpha = 0.28f)
        ),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = fecha.ifBlank { "Todas las fechas" },
                    style = MaterialTheme.typography.titleMedium,
                    color = AzulClinico,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 3.dp)
                )

                Text(
                    text = "Toca para elegir año, mes y día",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Surface(
                color = AzulClinico.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = fecha.take(2).ifBlank { "--" },
                    color = AzulClinico,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(
                        horizontal = 13.dp,
                        vertical = 10.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun CampoFiltro(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        modifier = modifier.padding(top = 9.dp),
        singleLine = true,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun BotonRegresarBusqueda(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(62.dp),
        color = Color.White.copy(alpha = 0.96f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.5.dp,
            CianRuralitos.copy(alpha = 0.65f)
        ),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "←",
                color = CianRuralitos,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Regresar al menú principal",
                color = CianRuralitos,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun FondoDecorativoBusqueda(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val alto = size.height

        drawCircle(
            color = Color(0xFF7DE7E0).copy(alpha = 0.13f),
            radius = size.width * 0.55f,
            center = Offset(
                size.width * 0.87f,
                alto * 0.20f
            )
        )

        val baseY = alto * 0.83f

        val capa1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, baseY)

            cubicTo(
                size.width * 0.18f,
                alto * 0.76f,
                size.width * 0.33f,
                alto * 0.92f,
                size.width * 0.53f,
                alto * 0.87f
            )

            cubicTo(
                size.width * 0.70f,
                alto * 0.83f,
                size.width * 0.84f,
                alto * 0.75f,
                size.width,
                alto * 0.79f
            )

            lineTo(size.width, alto)
            lineTo(0f, alto)
            close()
        }

        drawPath(
            path = capa1,
            color = Color(0xFF77D9EF).copy(alpha = 0.25f)
        )

        val capa2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, alto * 0.91f)

            cubicTo(
                size.width * 0.20f,
                alto * 0.82f,
                size.width * 0.39f,
                alto * 0.98f,
                size.width * 0.59f,
                alto * 0.92f
            )

            cubicTo(
                size.width * 0.76f,
                alto * 0.87f,
                size.width * 0.89f,
                alto * 0.84f,
                size.width,
                alto * 0.89f
            )

            lineTo(size.width, alto)
            lineTo(0f, alto)
            close()
        }

        drawPath(
            path = capa2,
            color = Color(0xFF65D6BC).copy(alpha = 0.24f)
        )
    }
}

@Composable
private fun IconoLupa(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val radio = size.minDimension * 0.28f
        val centro = Offset(
            size.width * 0.43f,
            size.height * 0.42f
        )

        drawCircle(
            color = color,
            radius = radio,
            center = centro,
            style = Stroke(
                width = size.minDimension * 0.09f
            )
        )

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.63f,
                size.height * 0.63f
            ),
            end = Offset(
                size.width * 0.86f,
                size.height * 0.86f
            ),
            strokeWidth = size.minDimension * 0.09f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun IconoUsuarioBusqueda(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawCircle(
            color = color,
            radius = size.minDimension * 0.17f,
            center = Offset(
                size.width * 0.5f,
                size.height * 0.28f
            ),
            style = Stroke(
                width = size.minDimension * 0.07f
            )
        )

        drawArc(
            color = color,
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(
                size.width * 0.18f,
                size.height * 0.54f
            ),
            size = androidx.compose.ui.geometry.Size(
                size.width * 0.64f,
                size.height * 0.42f
            ),
            style = Stroke(
                width = size.minDimension * 0.07f,
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
private fun IconoCarpetaBusqueda(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawRoundRect(
            color = color,
            topLeft = Offset(
                size.width * 0.12f,
                size.height * 0.28f
            ),
            size = androidx.compose.ui.geometry.Size(
                size.width * 0.68f,
                size.height * 0.50f
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                size.minDimension * 0.08f
            ),
            style = Stroke(
                width = size.minDimension * 0.07f
            )
        )

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.17f,
                size.height * 0.28f
            ),
            end = Offset(
                size.width * 0.37f,
                size.height * 0.28f
            ),
            strokeWidth = size.minDimension * 0.07f,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = color,
            radius = size.minDimension * 0.12f,
            center = Offset(
                size.width * 0.73f,
                size.height * 0.69f
            ),
            style = Stroke(
                width = size.minDimension * 0.07f
            )
        )

        drawLine(
            color = color,
            start = Offset(
                size.width * 0.81f,
                size.height * 0.77f
            ),
            end = Offset(
                size.width * 0.92f,
                size.height * 0.88f
            ),
            strokeWidth = size.minDimension * 0.07f,
            cap = StrokeCap.Round
        )
    }
}

private fun fechaVisible(fecha: Date): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fecha)

private fun fechaAClaveOrdenable(fecha: String): String {
    val partes = fecha.split('/')
    if (partes.size != 3) return ""
    val dia = partes[0].toIntOrNull() ?: return ""
    val mes = partes[1].toIntOrNull() ?: return ""
    val anio = partes[2].toIntOrNull() ?: return ""
    return String.format(Locale.US, "%04d%02d%02d", anio, mes, dia)
}
private fun formatearFechaBusqueda(valor: String): String {
    val digitos = valor.filter(Char::isDigit).take(8)
    return buildString {
        digitos.forEachIndexed { indice, caracter ->
            if (indice == 2 || indice == 4) append('/')
            append(caracter)
        }
    }
}

private fun siguienteEstado(estado: String): String = when (estado) {
    "ACTIVAS" -> "BORRADOR"
    "BORRADOR" -> "COMPLETA"
    "COMPLETA" -> "ARCHIVADA"
    "ARCHIVADA" -> "TODAS"
    else -> "ACTIVAS"
}

private fun nombreEstado(estado: String): String = when (estado) {
    "ACTIVAS" -> "Activas"
    "BORRADOR" -> "Pendientes"
    "COMPLETA" -> "Completas"
    "ARCHIVADA" -> "Archivadas"
    "TODAS" -> "Todas"
    else -> estado.lowercase().replaceFirstChar { it.uppercase() }
}

private fun colorEstado(estado: String): Color = when (estado) {
    "COMPLETA" -> VerdeSalud
    "ARCHIVADA" -> MoradoClinico
    "BORRADOR" -> NaranjaClinico
    "ELIMINADA" -> RojoClinico
    else -> AzulClinico
}
private fun fechaParaSelectorMillis(fecha: String): Long? {
    val partes = fecha.split('/')
    if (partes.size != 3) return null
    val dia = partes[0].toIntOrNull() ?: return null
    val mes = partes[1].toIntOrNull() ?: return null
    val anio = partes[2].toIntOrNull() ?: return null
    return runCatching {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            isLenient = false
            clear()
            set(anio, mes - 1, dia)
        }.timeInMillis
    }.getOrNull()
}

private fun millisAFechaVisible(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(millis))
