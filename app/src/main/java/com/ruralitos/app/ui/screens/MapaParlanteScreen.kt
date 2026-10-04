package com.ruralitos.app.ui.screens

import android.graphics.RectF
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.VentanaRuralitos
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.mapa.GestorMapaCampo
import com.ruralitos.app.data.mapa.GestorMapaDetalle
import com.ruralitos.app.domain.BarrioParlante
import com.ruralitos.app.domain.MapaParlante
import com.ruralitos.app.domain.PictogramaDispensarizacion
import com.ruralitos.app.domain.StickerBarrio
import com.ruralitos.app.ui.components.ClaseAncho
import com.ruralitos.app.ui.components.EncabezadoPantallaRuralitos
import com.ruralitos.app.ui.components.IconoMais
import com.ruralitos.app.ui.components.LocalClaseAncho
import com.ruralitos.app.ui.components.TextoAjustado
import com.ruralitos.app.ui.components.FiltroOrigenFichas
import com.ruralitos.app.domain.EtiquetasFicha
import com.ruralitos.app.domain.OrigenFicha
import com.ruralitos.app.domain.admite
import com.ruralitos.app.domain.textoDatosDe
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.FondoClinico
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconOffset
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

private val AzulTexto = Color(0xFF0A2A5E)
private val GrisTexto = Color(0xFF5B7083)
private const val VACIO_PARLANTE = """{"type":"FeatureCollection","features":[]}"""

/**
 * Mapa parlante: elige un barrio y el mapa pone, en el punto medio de sus viviendas, una figura por cada situación que
 * existe en él (hipertensos, embarazadas, menores de 2 años…) con el número de personas encima. Sin barrio elegido
 * muestra cada barrio con su total de personas. Todo sale de las fichas guardadas y funciona sin internet.
 */
@Composable
fun MapaParlanteScreen(onRegresar: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val db = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val fichas by remember(db) { db.fichaFamiliarDao().listarFichas() }.collectAsState(initial = null)
    val miembros by remember(db) { db.fichaContenidoDao().listarTodosMiembros() }.collectAsState(initial = null)
    val embarazadas by remember(db) { db.fichaContenidoDao().listarTodasEmbarazadas() }.collectAsState(initial = null)

    // Por defecto se ven mis fichas; las que otra persona me compartió se piden con el filtro.
    var origen by remember { mutableStateOf(OrigenFicha.MIAS) }
    val fichasVistas = remember(fichas, origen) { fichas?.filter { origen.admite(it) } }
    val idsVistos = remember(fichasVistas) { fichasVistas?.mapTo(HashSet()) { it.id } }
    val miembrosVistos = remember(miembros, idsVistos) { if (miembros != null && idsVistos != null) miembros!!.filter { it.fichaId in idsVistos } else null }
    val embarazadasVistas = remember(embarazadas, idsVistos) { if (embarazadas != null && idsVistos != null) embarazadas!!.filter { it.fichaId in idsVistos } else null }
    val totalPropias = remember(fichas) { fichas.orEmpty().count { !EtiquetasFicha.esRecibida(it) } }
    val totalRecibidas = remember(fichas) { fichas.orEmpty().count { EtiquetasFicha.esRecibida(it) } }

    // Clasificar a cada persona es trabajo de CPU: se hace fuera del hilo de la pantalla.
    val barrios by produceState<List<BarrioParlante>?>(null, fichasVistas, miembrosVistos, embarazadasVistas) {
        val f = fichasVistas
        val m = miembrosVistos
        val e = embarazadasVistas
        if (f != null && m != null && e != null) {
            value = withContext(Dispatchers.Default) { MapaParlante.barrios(f, m, e) }
        }
    }

    var claveElegida by remember { mutableStateOf<String?>(null) }
    var detalleAbierto by remember { mutableStateOf(false) }
    var ventanaBarrios by remember { mutableStateOf(false) }
    var mapaLocal by remember { mutableStateOf<String?>(null) }
    var mapaDetalle by remember { mutableStateOf<String?>(null) }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var estiloListo by remember { mutableStateOf(false) }
    val imagenesAgregadas = remember { mutableSetOf<String>() }

    val lista = barrios.orEmpty()
    val elegido = remember(lista, claveElegida) { lista.firstOrNull { it.clave == claveElegida } }
    val ancho = LocalClaseAncho.current != ClaseAncho.COMPACTA

    MapLibre.getInstance(context)
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { mapa = it }
        }
    }
    LaunchedEffect(Unit) {
        mapaLocal = GestorMapaCampo.preparar(context)
        mapaDetalle = GestorMapaDetalle.preparar(context)
    }

    LaunchedEffect(mapa, mapaLocal, mapaDetalle) {
        val vista = mapa ?: return@LaunchedEffect
        estiloListo = false
        // La atribución obligatoria (© OpenStreetMap contributors · Protomaps) ya se escribe sobre el mapa.
        vista.uiSettings.isLogoEnabled = false
        vista.uiSettings.isAttributionEnabled = false
        val estilo = mapaLocal?.let { Style.Builder().fromJson(GestorMapaCampo.estilo(context, it, mapaDetalle)) }
            ?: Style.Builder().fromUri("asset://mapa_emergencia.json")
        vista.setStyle(estilo) { actual ->
            actual.addSource(GeoJsonSource("barrios", VACIO_PARLANTE))
            actual.addSource(GeoJsonSource("tablero", VACIO_PARLANTE))
            actual.addSource(GeoJsonSource("figuras", VACIO_PARLANTE))
            actual.addLayer(
                CircleLayer("barrio-circulo", "barrios").withProperties(
                    circleColor("#0A2A5E"), circleRadius(21f), circleStrokeColor("#FFFFFF"), circleStrokeWidth(3f)
                )
            )
            actual.addLayer(
                SymbolLayer("barrio-total", "barrios").withProperties(
                    textField(Expression.get("total")), textFont(arrayOf("Noto Sans Regular")), textSize(14f),
                    textColor("#FFFFFF"), textAllowOverlap(true), textIgnorePlacement(true)
                )
            )
            actual.addLayer(
                SymbolLayer("barrio-nombre", "barrios").withProperties(
                    textField(Expression.get("nombre")), textFont(arrayOf("Noto Sans Regular")), textSize(13f),
                    textColor("#0A2A5E"), textHaloColor("#FFFFFF"), textHaloWidth(2f),
                    textAnchor(Property.TEXT_ANCHOR_TOP), textOffset(arrayOf(0f, 1.9f))
                )
            )
            actual.addLayer(
                SymbolLayer("tablero", "tablero").withProperties(
                    iconImage(Expression.get("img")), iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                    iconAllowOverlap(true), iconIgnorePlacement(true)
                )
            )
            actual.addLayer(
                SymbolLayer("figuras", "figuras").withProperties(
                    iconImage(Expression.get("img")), iconAnchor(Property.ICON_ANCHOR_CENTER),
                    iconOffset(Expression.get("desplazamiento")),
                    iconAllowOverlap(true), iconIgnorePlacement(true)
                )
            )
            vista.cameraPosition = CameraPosition.Builder().target(LatLng(-1.8, -78.2)).zoom(6.0).build()
            estiloListo = true
        }
        vista.addOnMapClickListener { punto ->
            val p = vista.projection.toScreenLocation(punto)
            val zona = RectF(p.x - 30f, p.y - 30f, p.x + 30f, p.y + 30f)
            val tocado = vista.queryRenderedFeatures(zona, "barrio-circulo", "barrio-total", "barrio-nombre").firstOrNull()
            val clave = tocado?.getStringProperty("clave")
            if (clave != null) { claveElegida = clave; true } else false
        }
    }

    // Lo que se dibuja: los barrios con su total, o el tablero del barrio elegido.
    LaunchedEffect(estiloListo, lista, elegido) {
        val vista = mapa ?: return@LaunchedEffect
        if (!estiloListo) return@LaunchedEffect
        val estilo = vista.style ?: return@LaunchedEffect
        val fuenteBarrios = estilo.getSourceAs<GeoJsonSource>("barrios") ?: return@LaunchedEffect
        val fuenteTablero = estilo.getSourceAs<GeoJsonSource>("tablero") ?: return@LaunchedEffect
        val fuenteFiguras = estilo.getSourceAs<GeoJsonSource>("figuras") ?: return@LaunchedEffect
        val conUbicacion = lista.filter { it.centro != null }

        if (elegido == null) {
            fuenteTablero.setGeoJson(VACIO_PARLANTE)
            fuenteFiguras.setGeoJson(VACIO_PARLANTE)
            fuenteBarrios.setGeoJson(
                conUbicacion.joinToString(",", """{"type":"FeatureCollection","features":[""", "]}") { b ->
                    val (lat, lon) = b.centro!!
                    """{"type":"Feature","geometry":{"type":"Point","coordinates":[$lon,$lat]},"properties":{"clave":${comillas(b.clave)},"nombre":${comillas(b.nombre)},"total":"${b.personas}"}}"""
                }
            )
            // Un encuadre necesita al menos dos puntos; con un solo barrio se centra en él.
            if (conUbicacion.size == 1) {
                val (lat, lon) = conUbicacion.single().centro!!
                vista.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 14.5), 500)
            } else if (conUbicacion.size > 1) {
                val limites = LatLngBounds.Builder().apply { conUbicacion.forEach { include(LatLng(it.centro!!.first, it.centro.second)) } }.build()
                runCatching { vista.animateCamera(CameraUpdateFactory.newLatLngBounds(limites, 110), 500) }
            }
            return@LaunchedEffect
        }

        fuenteBarrios.setGeoJson(VACIO_PARLANTE)
        val centro = elegido.centro
        if (centro == null) {
            fuenteTablero.setGeoJson(VACIO_PARLANTE)
            fuenteFiguras.setGeoJson(VACIO_PARLANTE)
            return@LaunchedEffect
        }
        // Las imágenes se dibujan fuera del hilo de la pantalla y se agregan al estilo ya hechas.
        val medidas = TableroParlante.medidas(elegido)
        val firma = elegido.stickers.joinToString("|") { "${it.id}:${it.personas}" }
        val nombreTablero = "tablero-${elegido.clave}-${elegido.fichas}-${elegido.personas}-${firma.hashCode()}"
        val dibujos = withContext(Dispatchers.Default) {
            val tablero = TableroParlante.imagenTablero(context, elegido)
            val figuras = elegido.stickers.mapNotNull { s ->
                TableroParlante.imagenSticker(context, s.id, s.personas)?.let { "figura-${s.id}-${s.personas}" to it }
            }
            tablero to figuras
        }
        // MapLibre solo admite cambios desde el hilo de la pantalla.
        withContext(Dispatchers.Main) {
            val actual = vista.style ?: return@withContext
            if (imagenesAgregadas.add(nombreTablero)) actual.addImage(nombreTablero, dibujos.first)
            dibujos.second.forEach { (nombre, imagen) -> if (imagenesAgregadas.add(nombre)) actual.addImage(nombre, imagen) }
            fuenteTablero.setGeoJson(
                """{"type":"Feature","geometry":{"type":"Point","coordinates":[${centro.second},${centro.first}]},"properties":{"img":${comillas(nombreTablero)}}}"""
            )
            val conImagen = elegido.stickers.filter { "figura-${it.id}-${it.personas}" in imagenesAgregadas }
            fuenteFiguras.setGeoJson(
                conImagen.mapIndexed { i, s ->
                    val (dx, dy) = medidas.desplazamientos[elegido.stickers.indexOf(s)]
                    """{"type":"Feature","geometry":{"type":"Point","coordinates":[${centro.second},${centro.first}]},"properties":{"img":"figura-${s.id}-${s.personas}","desplazamiento":[$dx,$dy]}}"""
                }.joinToString(",", """{"type":"FeatureCollection","features":[""", "]}")
            )
            vista.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(centro.first, centro.second), 15.5), 500)
        }
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

    Column(Modifier.fillMaxSize().background(FondoClinico).formularioSeguro()) {
        EncabezadoPantallaRuralitos(
            titulo = "Mapa de viviendas",
            subtitulo = null,
            paso = null, totalPasos = null, etiquetaPaso = "",
            onVolver = onRegresar,
            descripcion = when {
                barrios == null -> "Calculando los totales por barrio…"
                lista.isEmpty() -> "Aún no hay barrios con fichas"
                elegido != null -> "Mapa parlante · ${elegido.nombre}"
                else -> "${lista.size} barrio${if (lista.size == 1) "" else "s"} · elige uno para ver sus figuras"
            }
        )
        val selector: @Composable () -> Unit = {
            // Un solo selector: al tocarlo se abre una ventana con todos los barrios para elegir uno.
            Column(Modifier.fillMaxWidth().background(Color.White)) {
            FiltroOrigenFichas(
                origen, textoDatosDe(origen, totalPropias, totalRecibidas), { origen = it; claveElegida = null },
                Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)
            )
            Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                Surface(
                    onClick = { ventanaBarrios = true }, modifier = Modifier.fillMaxWidth().testTag("selector_barrio"),
                    shape = RoundedCornerShape(12.dp), color = if (elegido != null) Color(0xFFE3F4F7) else Color.White,
                    border = BorderStroke(1.dp, if (elegido != null) CianRuralitos else Color(0xFFCFDDE5))
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Barrio", color = GrisTexto, fontSize = 11.sp)
                            TextoAjustado(
                                elegido?.nombre ?: "Todos los barrios", tamano = 16.sp, tamanoMinimo = 11.sp,
                                color = AzulTexto, fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text("▾", color = CianRuralitos, fontSize = 16.sp)
                    }
                }
            }
            }
        }
        val mapaConControles: @Composable (Modifier) -> Unit = { modificador ->
            Box(modificador) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize().testTag("mapa_parlante"))
                Text(
                    "© OpenStreetMap contributors · Protomaps",
                    Modifier.align(Alignment.BottomStart).background(Color(0xEFFFFFFF)).padding(horizontal = 6.dp, vertical = 3.dp),
                    color = AzulClinico, fontSize = 10.sp
                )
                if (lista.isEmpty() && barrios != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 2.dp
                    ) {
                        Text(
                            if (origen == OrigenFicha.RECIBIDAS) "Nadie te ha compartido fichas con barrio todavía."
                            else "Aún no hay fichas con barrio. Crea una ficha y vuelve aquí.",
                            Modifier.padding(16.dp), color = AzulTexto, fontSize = 14.sp
                        )
                    }
                }
                if (elegido != null && elegido.centro == null) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp).testTag("barrio_sin_ubicacion"),
                        shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 2.dp
                    ) {
                        Text(
                            "Ninguna ficha de ${elegido.nombre} tiene la vivienda ubicada, por eso no se puede colocar el punto medio. " +
                                "Los totales sí están abajo. Abre una ficha, entra a Ubicación de vivienda y guarda el punto de la vivienda.",
                            Modifier.padding(16.dp), color = AzulTexto, fontSize = 14.sp
                        )
                    }
                }
                if (!ancho && elegido != null) {
                    ResumenBarrio(
                        elegido, detalleAbierto, { detalleAbierto = !detalleAbierto },
                        Modifier.align(Alignment.BottomCenter).padding(8.dp)
                    )
                }
            }
        }

        if (ancho) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.width(340.dp).fillMaxHeight().background(Color.White)) {
                    selector()
                    if (elegido != null) {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            CabeceraBarrio(elegido)
                            elegido.stickers.forEach { FilaSticker(it) }
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f)) {
                            items(lista, key = { it.clave }) { b -> FilaBarrio(b) { claveElegida = b.clave } }
                        }
                    }
                }
                mapaConControles(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            selector()
            mapaConControles(Modifier.weight(1f).fillMaxWidth())
        }
    }

    if (ventanaBarrios) {
        VentanaBarrios(
            barrios = lista, elegida = claveElegida,
            onElegir = { claveElegida = it; ventanaBarrios = false },
            onCerrar = { ventanaBarrios = false }
        )
    }
}

/** Ventana con todos los barrios; la opción elegida queda marcada. */
@Composable
private fun VentanaBarrios(
    barrios: List<BarrioParlante>,
    elegida: String?,
    onElegir: (String?) -> Unit,
    onCerrar: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    val visibles = remember(barrios, busqueda) {
        val q = java.text.Normalizer.normalize(busqueda, java.text.Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "").lowercase().trim()
        if (q.isEmpty()) barrios
        else barrios.filter {
            java.text.Normalizer.normalize(it.nombre, java.text.Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "").lowercase().contains(q)
        }
    }
    VentanaRuralitos(
        titulo = "Elegir barrio",
        subtitulo = "Mapa de viviendas",
        simbolo = "⌖",
        color = AzulClinico,
        onCerrar = onCerrar,
        contenido = {
    Column {
                    if (barrios.size > 8) {
                        androidx.compose.material3.OutlinedTextField(
                            value = busqueda, onValueChange = { busqueda = it }, singleLine = true,
                            placeholder = { Text("Buscar barrio") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("buscar_barrio")
                        )
                    }
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        item(key = "todos") {
                            FilaOpcionBarrio(
                                "Todos los barrios", "${barrios.size} barrio${if (barrios.size == 1) "" else "s"} · ${barrios.sumOf { it.personas }} personas",
                                elegida == null, Modifier.testTag("barrio_todos")
                            ) { onElegir(null) }
                        }
                        items(visibles, key = { it.clave }) { b ->
                            FilaOpcionBarrio(
                                b.nombre, "${b.personas} persona${if (b.personas == 1) "" else "s"} · ${b.fichas} familia${if (b.fichas == 1) "" else "s"}" +
                                    if (b.centro == null) " · sin ubicación" else "",
                                b.clave == elegida, Modifier.testTag("barrio_${b.clave}")
                            ) { onElegir(b.clave) }
                        }
                        if (visibles.isEmpty()) item { Text("Ningún barrio coincide.", color = GrisTexto, modifier = Modifier.padding(12.dp)) }
                    }
                }
        },
        acciones = {
            BotonSecundarioRuralitos(texto = "Cerrar", onClick = onCerrar)
        }
    )
}

@Composable
private fun FilaOpcionBarrio(titulo: String, detalle: String, marcada: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).background(if (marcada) Color(0xFFE3F4F7) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            TextoAjustado(titulo, tamano = 15.sp, tamanoMinimo = 11.sp, maxLineas = 2, color = AzulTexto, fontWeight = FontWeight.Medium)
            Text(detalle, color = GrisTexto, fontSize = 12.sp)
        }
        if (marcada) Text("✓", color = CianRuralitos, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

private fun comillas(texto: String): String = org.json.JSONObject.quote(texto)

@Composable
private fun CabeceraBarrio(b: BarrioParlante) {
    Column(Modifier.fillMaxWidth().padding(14.dp)) {
        Text(b.nombre, color = AzulTexto, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Text(
            "${b.personas} persona${if (b.personas == 1) "" else "s"} · ${b.fichas} familia${if (b.fichas == 1) "" else "s"}",
            color = GrisTexto, fontSize = 12.sp
        )
        if (b.fichasSinUbicacion > 0 && b.centro != null) {
            Text(
                "${b.fichasSinUbicacion} ficha${if (b.fichasSinUbicacion == 1) "" else "s"} sin ubicación no cuenta${if (b.fichasSinUbicacion == 1) "" else "n"} para el punto medio, pero sus personas sí están en los totales.",
                color = Color(0xFF8A4B00), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (b.stickers.isEmpty()) {
            Text("Todavía no hay figuras para este barrio: ninguna persona tiene una condición registrada.", color = GrisTexto, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun FilaSticker(s: StickerBarrio) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp).testTag("fila_figura"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoMais(PictogramaDispensarizacion(s.id, s.etiqueta), Modifier.size(38.dp))
        Text(s.etiqueta, Modifier.weight(1f).padding(horizontal = 10.dp), color = AzulTexto, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Surface(shape = RoundedCornerShape(12.dp), color = AzulTexto) {
            Text(s.personas.toString(), Modifier.padding(horizontal = 10.dp, vertical = 3.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
    Box(Modifier.fillMaxWidth().padding(start = 14.dp).size(width = 1.dp, height = 1.dp).background(BordeClinico))
}

@Composable
private fun FilaBarrio(b: BarrioParlante, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(b.nombre, color = AzulTexto, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text("${b.fichas} familia${if (b.fichas == 1) "" else "s"}${if (b.centro == null) " · sin ubicación" else ""}", color = GrisTexto, fontSize = 12.sp)
        }
        Surface(shape = RoundedCornerShape(12.dp), color = AzulTexto) {
            Text(b.personas.toString(), Modifier.padding(horizontal = 10.dp, vertical = 3.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ResumenBarrio(b: BarrioParlante, abierto: Boolean, alternar: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag("resumen_barrio"),
        shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 4.dp, border = BorderStroke(1.dp, BordeClinico)
    ) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(onClick = alternar).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(b.nombre, color = AzulTexto, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "${b.personas} persona${if (b.personas == 1) "" else "s"} · ${b.fichas} familia${if (b.fichas == 1) "" else "s"} · ${b.stickers.size} figura${if (b.stickers.size == 1) "" else "s"}",
                        color = GrisTexto, fontSize = 12.sp
                    )
                }
                Text(if (abierto) "Ocultar ▾" else "Ver detalle ▴", color = CianRuralitos, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            if (abierto) {
                Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                    CabeceraBarrio(b)
                    b.stickers.forEach { FilaSticker(it) }
                }
            }
        }
    }
}
