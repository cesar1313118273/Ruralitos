package com.ruralitos.app.ui.screens

import android.Manifest
import android.graphics.RectF
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ruralitos.app.data.local.dao.ViviendaMapaFila
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.location.GestorUbicacionActual
import com.ruralitos.app.data.mapa.GestorMapaCampo
import com.ruralitos.app.data.mapa.GestorMapaDetalle
import com.ruralitos.app.domain.EstadoVivienda
import com.ruralitos.app.domain.FiltroVivienda
import com.ruralitos.app.domain.MapaViviendas
import com.ruralitos.app.ui.components.ClaseAncho
import com.ruralitos.app.ui.components.EncabezadoPantallaRuralitos
import com.ruralitos.app.ui.components.LocalClaseAncho
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.FondoClinico
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource

private val AzulTexto = Color(0xFF0A2A5E)
private val GrisTexto = Color(0xFF5B7083)
private const val VACIO = """{"type":"FeatureCollection","features":[]}"""

private fun colorDe(e: EstadoVivienda) = Color(android.graphics.Color.parseColor(e.colorHex))

/**
 * Mapa general de viviendas: todas las fichas con ubicación como puntos de colores, con búsqueda, filtros,
 * agrupación al alejar el mapa y una tarjeta al tocar una vivienda. En pantallas anchas, la lista y la tarjeta van
 * en un panel a la izquierda. Funciona sin internet con los mapas incluidos en la app.
 */
@Composable
fun MapaViviendasScreen(
    usuarioId: Long,
    onRegresar: () -> Unit,
    onAbrirFicha: (Long) -> Unit,
    onAbrirRuta: (Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val db = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val ahora = remember { System.currentTimeMillis() }
    val viviendas by remember(db, usuarioId) { db.fichaFamiliarDao().observarViviendasMapa(usuarioId, ahora) }
        .collectAsState(initial = emptyList())
    val sinUbicacion by remember(db) { db.fichaFamiliarDao().observarSinUbicacion() }
        .collectAsState(initial = emptyList())

    var consulta by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf(FiltroVivienda.TODAS) }
    var seleccionadaId by remember { mutableStateOf<Long?>(null) }
    var verSinUbicacion by remember { mutableStateOf(false) }
    var ubicacion by remember { mutableStateOf<Location?>(null) }
    var permiso by remember { mutableStateOf(GestorUbicacionActual.tienePermiso(context)) }
    var mapaLocal by remember { mutableStateOf<String?>(null) }
    var mapaDetalle by remember { mutableStateOf<String?>(null) }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var estiloListo by remember { mutableStateOf(false) }
    var ajusteInicial by remember { mutableStateOf(false) }
    var buscandoGps by remember { mutableStateOf(false) }

    val visibles = remember(viviendas, filtro, consulta) { MapaViviendas.filtrar(viviendas, filtro, consulta) }
    val seleccionada = remember(viviendas, seleccionadaId) { viviendas.firstOrNull { it.fichaId == seleccionadaId } }
    val conteos = remember(viviendas) { MapaViviendas.conteos(viviendas) }
    val ancho = LocalClaseAncho.current != ClaseAncho.COMPACTA

    MapLibre.getInstance(context)
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { mapa = it }
        }
    }

    val solicitarPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permiso = GestorUbicacionActual.tienePermiso(context)
    }
    fun centrarEnMiUbicacion() {
        if (!permiso) {
            solicitarPermiso.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        buscandoGps = true
        scope.launch {
            val encontrada = GestorUbicacionActual.obtener(context)
            buscandoGps = false
            if (encontrada != null) {
                ubicacion = encontrada
                mapa?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(encontrada.latitude, encontrada.longitude), 16.0))
            }
        }
    }

    // La posición se dibuja sin mover el mapa, para que el primer vistazo sea el de las viviendas.
    LaunchedEffect(permiso) {
        if (permiso && ubicacion == null) ubicacion = GestorUbicacionActual.obtener(context)
    }
    LaunchedEffect(Unit) {
        mapaLocal = GestorMapaCampo.preparar(context)
        mapaDetalle = GestorMapaDetalle.preparar(context)
    }

    // Estilo, fuentes y capas: se crean una vez; los datos se actualizan aparte.
    LaunchedEffect(mapa, mapaLocal, mapaDetalle) {
        val vista = mapa ?: return@LaunchedEffect
        estiloListo = false
        // La atribución obligatoria (© OpenStreetMap contributors · Protomaps) ya se escribe sobre el mapa.
        vista.uiSettings.isLogoEnabled = false
        vista.uiSettings.isAttributionEnabled = false
        val estilo = mapaLocal?.let { Style.Builder().fromJson(GestorMapaCampo.estilo(context, it, mapaDetalle)) }
            ?: Style.Builder().fromUri("asset://mapa_emergencia.json")
        vista.setStyle(estilo) { actual ->
            actual.addSource(
                GeoJsonSource("viviendas", VACIO, GeoJsonOptions().withCluster(true).withClusterRadius(46).withClusterMaxZoom(15))
            )
            actual.addSource(GeoJsonSource("vivienda-seleccionada", VACIO))
            actual.addSource(GeoJsonSource("mi-ubicacion", VACIO))
            actual.addLayer(
                CircleLayer("viv-cluster", "viviendas").withFilter(Expression.has("point_count")).withProperties(
                    circleColor("#0A2A5E"),
                    circleRadius(
                        Expression.step(
                            Expression.get("point_count"), Expression.literal(16),
                            Expression.stop(10, 20), Expression.stop(50, 26)
                        )
                    ),
                    circleStrokeColor("#FFFFFF"), circleStrokeWidth(2.5f)
                )
            )
            actual.addLayer(
                SymbolLayer("viv-cluster-numero", "viviendas").withFilter(Expression.has("point_count")).withProperties(
                    textField(Expression.get("point_count_abbreviated")),
                    textFont(arrayOf("Noto Sans Regular")), textSize(13f), textColor("#FFFFFF"),
                    textAllowOverlap(true), textIgnorePlacement(true)
                )
            )
            actual.addLayer(
                CircleLayer("viv-punto", "viviendas").withFilter(Expression.not(Expression.has("point_count"))).withProperties(
                    circleColor(Expression.get("color")), circleRadius(9f),
                    circleStrokeColor("#FFFFFF"), circleStrokeWidth(2.5f)
                )
            )
            actual.addLayer(
                CircleLayer("viv-seleccion", "vivienda-seleccionada").withProperties(
                    circleColor("#FFFFFF"), circleOpacity(0f), circleRadius(17f),
                    circleStrokeColor("#0A2A5E"), circleStrokeWidth(3.5f)
                )
            )
            actual.addLayer(
                CircleLayer("mi-ubicacion-halo", "mi-ubicacion").withProperties(
                    circleColor("#2B7CF0"), circleOpacity(0.2f), circleRadius(16f)
                )
            )
            actual.addLayer(
                CircleLayer("mi-ubicacion-punto", "mi-ubicacion").withProperties(
                    circleColor("#2B7CF0"), circleRadius(6f), circleStrokeColor("#FFFFFF"), circleStrokeWidth(2.5f)
                )
            )
            vista.cameraPosition = CameraPosition.Builder().target(LatLng(-1.8, -78.2)).zoom(6.0).build()
            estiloListo = true
        }
        vista.addOnMapClickListener { punto ->
            val p = vista.projection.toScreenLocation(punto)
            val zona = RectF(p.x - 28f, p.y - 28f, p.x + 28f, p.y + 28f)
            val tocada = vista.queryRenderedFeatures(zona, "viv-punto", "viv-cluster", "viv-cluster-numero").firstOrNull()
            when {
                tocada == null -> { seleccionadaId = null; false }
                tocada.hasProperty("point_count") -> {
                    vista.animateCamera(CameraUpdateFactory.newLatLngZoom(punto, vista.cameraPosition.zoom + 2.0), 450)
                    true
                }
                else -> { seleccionadaId = tocada.getNumberProperty("id").toLong(); true }
            }
        }
    }

    // Datos de los puntos
    LaunchedEffect(estiloListo, visibles) {
        if (!estiloListo) return@LaunchedEffect
        mapa?.style?.getSourceAs<GeoJsonSource>("viviendas")?.setGeoJson(MapaViviendas.geoJson(visibles))
    }
    // Cuadrar la cámara en lo que se ve: la primera vez y cada vez que cambia la búsqueda o el filtro
    LaunchedEffect(estiloListo, visibles.map { it.fichaId }) {
        val vista = mapa ?: return@LaunchedEffect
        if (!estiloListo || visibles.isEmpty()) return@LaunchedEffect
        if (visibles.size == 1) {
            vista.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(visibles[0].latitud, visibles[0].longitud), 16.0), 500)
        } else {
            val limites = LatLngBounds.Builder().apply { visibles.forEach { include(LatLng(it.latitud, it.longitud)) } }.build()
            val animar = ajusteInicial
            vista.moveCamera(CameraUpdateFactory.newLatLngBounds(limites, 90))
            if (animar && vista.cameraPosition.zoom > 17.5) vista.moveCamera(CameraUpdateFactory.zoomTo(17.0))
        }
        ajusteInicial = true
    }
    LaunchedEffect(estiloListo, seleccionada) {
        if (!estiloListo) return@LaunchedEffect
        val fuente = mapa?.style?.getSourceAs<GeoJsonSource>("vivienda-seleccionada") ?: return@LaunchedEffect
        if (seleccionada == null) fuente.setGeoJson(VACIO)
        else fuente.setGeoJson(
            """{"type":"Feature","geometry":{"type":"Point","coordinates":[${seleccionada.longitud},${seleccionada.latitud}]},"properties":{}}"""
        )
    }
    LaunchedEffect(estiloListo, ubicacion) {
        if (!estiloListo) return@LaunchedEffect
        val fuente = mapa?.style?.getSourceAs<GeoJsonSource>("mi-ubicacion") ?: return@LaunchedEffect
        val u = ubicacion
        fuente.setGeoJson(
            if (u == null) VACIO
            else """{"type":"Feature","geometry":{"type":"Point","coordinates":[${u.longitude},${u.latitude}]},"properties":{}}"""
        )
    }

    DisposableEffect(lifecycle, mapView) {
        var iniciado = false
        var reanudado = false
        fun sincronizar() {
            val estado = lifecycle.currentState
            if (estado.isAtLeast(Lifecycle.State.STARTED) && !iniciado) { mapView.onStart(); iniciado = true }
            if (estado.isAtLeast(Lifecycle.State.RESUMED) && !reanudado) { mapView.onResume(); reanudado = true }
            if (!estado.isAtLeast(Lifecycle.State.RESUMED) && reanudado) { mapView.onPause(); reanudado = false }
            if (!estado.isAtLeast(Lifecycle.State.STARTED) && iniciado) { mapView.onStop(); iniciado = false }
        }
        val observer = LifecycleEventObserver { _, _ -> sincronizar() }
        lifecycle.addObserver(observer)
        sincronizar()
        onDispose {
            lifecycle.removeObserver(observer)
            if (reanudado) mapView.onPause()
            if (iniciado) mapView.onStop()
            mapView.onDestroy()
        }
    }

    val distanciaSeleccion = remember(seleccionada, ubicacion) {
        val u = ubicacion
        if (seleccionada == null || u == null) null
        else MapaViviendas.distanciaMetros(u.latitude, u.longitude, seleccionada.latitud, seleccionada.longitud)
    }

    fun seleccionar(vivienda: ViviendaMapaFila) {
        seleccionadaId = vivienda.fichaId
        mapa?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(vivienda.latitud, vivienda.longitud), 17.0), 450)
    }

    Column(Modifier.fillMaxSize().background(FondoClinico).formularioSeguro()) {
        EncabezadoPantallaRuralitos(
            titulo = "Mapa de viviendas",
            subtitulo = null,
            paso = null, totalPasos = null, etiquetaPaso = "",
            onVolver = onRegresar,
            descripcion = when {
                viviendas.isEmpty() -> "Aún no hay viviendas con ubicación"
                visibles.size == viviendas.size -> "${viviendas.size} viviendas con ubicación"
                else -> "${visibles.size} de ${viviendas.size} viviendas"
            }
        )
        val panelBusqueda: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp)) {
                CampoBusquedaMapa(consulta) { consulta = it }
                Row(
                    Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FiltroVivienda.entries.forEach { f ->
                        val cantidad = if (f == FiltroVivienda.TODAS) viviendas.size
                        else viviendas.count { MapaViviendas.cumpleFiltro(it, f) }
                        ChipFiltro("${f.etiqueta} · $cantidad", f == filtro) { filtro = f }
                    }
                }
            }
        }
        val mapaConControles: @Composable (Modifier) -> Unit = { modificador ->
            Box(modificador) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize().testTag("mapa_viviendas"))
                if (sinUbicacion.isNotEmpty()) {
                    Surface(
                        onClick = { verSinUbicacion = true },
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp), color = Color(0xFFFFF8EF),
                        border = BorderStroke(1.dp, Color(0xFFFFD7A3)), shadowElevation = 1.dp
                    ) {
                        Text(
                            "${sinUbicacion.size} sin ubicación · ver",
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color(0xFF8A4B00), fontSize = 12.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }
                Column(Modifier.align(Alignment.CenterEnd).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonMapa(if (buscandoGps) "…" else "◎", "Ir a mi ubicación", Modifier.testTag("boton_mi_ubicacion")) { centrarEnMiUbicacion() }
                    BotonMapa("+", "Acercar") { mapa?.animateCamera(CameraUpdateFactory.zoomIn()) }
                    BotonMapa("−", "Alejar") { mapa?.animateCamera(CameraUpdateFactory.zoomOut()) }
                }
                if (!ancho) Leyenda(conteos, Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = if (seleccionada == null) 30.dp else 188.dp))
                if (viviendas.isEmpty()) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 2.dp
                    ) {
                        Text(
                            "Aún no hay viviendas con ubicación. Abre una ficha, entra a Croquis y guarda el punto de la vivienda.",
                            Modifier.padding(16.dp), color = AzulTexto, fontSize = 14.sp
                        )
                    }
                }
                Text(
                    "© OpenStreetMap contributors · Protomaps",
                    Modifier.align(Alignment.BottomStart).background(Color(0xEFFFFFFF)).padding(horizontal = 6.dp, vertical = 3.dp),
                    color = AzulClinico, fontSize = 10.sp
                )
                if (!ancho && seleccionada != null) {
                    TarjetaVivienda(
                        seleccionada, distanciaSeleccion,
                        onCerrar = { seleccionadaId = null },
                        onAbrirFicha = { onAbrirFicha(seleccionada.fichaId) },
                        onAbrirRuta = { onAbrirRuta(seleccionada.fichaId) },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                    )
                }
            }
        }

        if (ancho) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                // Todo el panel se desplaza junto: en una pantalla ancha pero baja (teléfono horizontal) la tarjeta
                // no debe empujar fuera de la pantalla sus botones.
                LazyColumn(Modifier.width(360.dp).fillMaxHeight().background(Color.White)) {
                    if (seleccionada != null) {
                        item(key = "tarjeta") {
                            TarjetaVivienda(
                                seleccionada, distanciaSeleccion,
                                onCerrar = { seleccionadaId = null },
                                onAbrirFicha = { onAbrirFicha(seleccionada.fichaId) },
                                onAbrirRuta = { onAbrirRuta(seleccionada.fichaId) },
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    item(key = "busqueda") { panelBusqueda() }
                    item(key = "leyenda") { Leyenda(conteos, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontal = true) }
                    items(visibles, key = { it.fichaId }) { v -> FilaVivienda(v, v.fichaId == seleccionadaId) { seleccionar(v) } }
                }
                mapaConControles(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            panelBusqueda()
            mapaConControles(Modifier.weight(1f).fillMaxWidth())
        }
    }

    if (verSinUbicacion) {
        AlertDialog(
            onDismissRequest = { verSinUbicacion = false },
            title = { Text("Viviendas sin ubicación") },
            text = {
                Column {
                    Text(
                        "Abre la ficha, entra a Croquis y guarda el punto de la vivienda para verla en el mapa.",
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(Modifier.height(280.dp)) {
                        items(sinUbicacion, key = { it.fichaId }) { f ->
                            Column(
                                Modifier.fillMaxWidth().clickable { verSinUbicacion = false; onAbrirFicha(f.fichaId) }.padding(vertical = 8.dp)
                            ) {
                                Text(f.jefe.ifBlank { "Ficha sin nombre" }, fontWeight = FontWeight.Medium, color = AzulTexto)
                                Text(
                                    listOf(f.barrio, "Ficha ${f.numero}").filter { it.isNotBlank() }.joinToString(" · "),
                                    color = GrisTexto, fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { verSinUbicacion = false }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun CampoBusquedaMapa(valor: String, alCambiar: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2F6F9))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⌕", color = GrisTexto, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (valor.isEmpty()) Text("Buscar familia, cédula o barrio", color = Color(0xFF8A9BB0), fontSize = 14.sp)
            BasicTextField(
                value = valor, onValueChange = alCambiar, singleLine = true,
                textStyle = TextStyle(color = AzulTexto, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().testTag("busqueda_mapa")
            )
        }
        if (valor.isNotEmpty()) {
            Text("✕", color = GrisTexto, fontSize = 16.sp, modifier = Modifier.clickable { alCambiar("") }.padding(start = 8.dp))
        }
    }
}

@Composable
private fun ChipFiltro(texto: String, activo: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(16.dp),
        color = if (activo) CianRuralitos else Color(0xFFEAF1F5)
    ) {
        Text(
            texto, Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (activo) Color.White else AzulTexto, fontSize = 12.sp, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BotonMapa(texto: String, descripcion: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(10.dp),
        color = Color(0xEEFFFFFF), shadowElevation = 1.dp, border = BorderStroke(1.dp, Color(0xFFD5E0E8))
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Text(texto, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = AzulClinico)
        }
    }
}

@Composable
private fun Leyenda(conteos: Map<EstadoVivienda, Int>, modifier: Modifier = Modifier, horizontal: Boolean = false) {
    val contenido: @Composable () -> Unit = {
        EstadoVivienda.entries.forEach { e ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = if (horizontal) 10.dp else 0.dp)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(colorDe(e)))
                Text(" ${e.etiqueta} · ${conteos[e] ?: 0}", color = AzulTexto, fontSize = 11.sp)
            }
        }
    }
    if (horizontal) {
        Row(modifier.horizontalScroll(rememberScrollState())) { contenido() }
    } else {
        Surface(modifier, shape = RoundedCornerShape(10.dp), color = Color(0xF2FFFFFF), border = BorderStroke(1.dp, Color(0xFFD5E0E8))) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) { contenido() }
        }
    }
}

@Composable
private fun FilaVivienda(v: ViviendaMapaFila, activa: Boolean, onClick: () -> Unit) {
    val estado = MapaViviendas.estado(v)
    Row(
        Modifier.fillMaxWidth().background(if (activa) Color(0xFFE3F4F7) else Color.White)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(colorDe(estado)))
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text(v.jefe.ifBlank { "Ficha sin nombre" }, color = AzulTexto, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(v.barrio, if (v.casa.isNotBlank()) "casa ${v.casa}" else "").filter { it.isNotBlank() }.joinToString(" · "),
                color = GrisTexto, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Text(estado.etiqueta, color = colorDe(estado), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(BordeClinico))
}

@Composable
private fun TarjetaVivienda(
    v: ViviendaMapaFila,
    distanciaMetros: Double?,
    onCerrar: () -> Unit,
    onAbrirFicha: () -> Unit,
    onAbrirRuta: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado = MapaViviendas.estado(v)
    Surface(
        modifier = modifier.fillMaxWidth().testTag("tarjeta_vivienda"),
        shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 4.dp,
        border = BorderStroke(1.dp, BordeClinico)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(v.jefe.ifBlank { "Ficha sin nombre" }, color = AzulTexto, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Ficha ${v.numero} · ${v.integrantes} integrante${if (v.integrantes == 1) "" else "s"}", color = GrisTexto, fontSize = 12.sp)
                    val direccion = listOf(v.barrio, if (v.casa.isNotBlank()) "casa ${v.casa}" else "").filter { it.isNotBlank() }.joinToString(", ")
                    if (direccion.isNotBlank()) Text(direccion, color = GrisTexto, fontSize = 12.sp)
                }
                Text("✕", color = GrisTexto, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onCerrar).padding(4.dp))
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Etiqueta(estado.etiqueta, colorDe(estado))
                if (v.visitasAtrasadas > 0) Etiqueta("Visita atrasada", Color(0xFFF7941D))
            }
            if (distanciaMetros != null) {
                Text(
                    "A ${MapaViviendas.textoDistancia(distanciaMetros)} en línea recta · unos ${MapaViviendas.minutosAPie(distanciaMetros)} min a pie",
                    color = AzulTexto, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
                )
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = onAbrirFicha, modifier = Modifier.weight(1f).testTag("abrir_ficha_mapa"),
                    shape = RoundedCornerShape(12.dp), color = CianRuralitos
                ) {
                    Text("Abrir ficha", Modifier.padding(vertical = 10.dp).fillMaxWidth(), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Surface(
                    onClick = onAbrirRuta, modifier = Modifier.weight(1f).testTag("como_llegar_mapa"),
                    shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, CianRuralitos)
                ) {
                    Text("Cómo llegar", Modifier.padding(vertical = 10.dp).fillMaxWidth(), color = CianRuralitos, fontWeight = FontWeight.Medium, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, color: Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.14f)) {
        Text(texto, Modifier.padding(horizontal = 9.dp, vertical = 3.dp), color = color.copy(alpha = 1f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
