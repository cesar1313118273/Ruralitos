package com.ruralitos.app.ui.screens

import android.Manifest
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.VentanaRuralitos
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ruralitos.app.data.local.dao.ViviendaMapaFila
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import com.ruralitos.app.data.location.GestorUbicacionActual
import com.ruralitos.app.data.location.rememberUbicacionEnVivo
import com.ruralitos.app.data.location.vibrarAviso
import com.ruralitos.app.data.mapa.GestorMapaCampo
import com.ruralitos.app.data.mapa.GestorMapaDetalle
import com.ruralitos.app.data.mapa.GestorRutasOffline
import com.ruralitos.app.data.mapa.RutaCalculada
import com.ruralitos.app.domain.EstadoVisita
import com.ruralitos.app.domain.LlegadaVivienda
import com.ruralitos.app.domain.MapaSeguimiento
import com.ruralitos.app.domain.MapaViviendas
import com.ruralitos.app.domain.PuntoSeguimiento
import com.ruralitos.app.domain.RecorridoVisitas
import com.ruralitos.app.ui.components.ClaseAncho
import com.ruralitos.app.ui.components.LocalClaseAncho
import com.ruralitos.app.ui.components.TextoAjustado
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.CianRuralitos
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
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AzulTexto = Color(0xFF0A2A5E)
private val GrisTexto = Color(0xFF5B7083)
private val VerdeAgenda = Color(0xFF0889A0)
private const val VACIO = """{"type":"FeatureCollection","features":[]}"""

/** Modos de viaje que ofrece el mapa; el valor es el nombre que entiende el motor de rutas. */
private val MODOS_VIAJE = listOf("pedestrian" to "A pie", "motor_scooter" to "Moto", "auto" to "Auto")

private fun textoModo(modo: String) = when (modo) {
    "pedestrian" -> "a pie"
    "motor_scooter" -> "en moto"
    "bicycle" -> "en bici"
    else -> "en auto"
}

private fun fecha(t: Long, patron: String = "EEE d MMM · HH:mm"): String =
    SimpleDateFormat(patron, Locale("es", "EC")).format(Date(t)).replaceFirstChar { it.uppercase() }

private fun colorDe(e: EstadoVisita) = Color(android.graphics.Color.parseColor(e.colorHex))

/** Recorrido ya ordenado: viviendas en orden de visita y la ruta que las une (si hay vías) o la línea recta. */
data class PlanRecorrido(
    val paradas: List<PuntoSeguimiento>,
    val ruta: RutaCalculada?,
    val kilometros: Double,
    val minutos: Int?,
    val modo: String,
    val desdeMiUbicacion: Boolean
)

/**
 * Todo lo que el mapa de seguimiento recuerda al salir a otra pantalla (abrir una ficha, ir a una ruta) y volver:
 * filtros, viviendas elegidas, recorrido ordenado y por cuál parada va.
 */
class EstadoMapaSeguimiento {
    /** Estados de visita que se ven en el mapa; al entrar están los cuatro. */
    var estados by mutableStateOf(EstadoVisita.entries.toSet())
    /** Rango de fechas (inicio y fin, de día completo). Al entrar son las dos la fecha de hoy. */
    var desde by mutableStateOf(MapaSeguimiento.inicioDia(System.currentTimeMillis()))
    var hasta by mutableStateOf(desde)
    /** Mientras nadie cambie las fechas se mantienen en «hoy», aunque la aplicación lleve días abierta. */
    var fechasElegidas by mutableStateOf(false)
    var modoViaje by mutableStateOf("pedestrian")
    var seleccionadaId by mutableStateOf<Long?>(null)
    /** Vivienda a la que se quiere llegar desde el botón «Cómo llegar» de la ficha (aunque no tenga visita). */
    var destinoId by mutableStateOf<Long?>(null)
    /** Pendiente: al abrir el mapa, trazar la ruta hasta [destinoId] desde la ubicación actual. */
    var iniciarRutaAlDestino by mutableStateOf(false)
    var modoRecorrido by mutableStateOf(false)
    val paradasIds = mutableStateListOf<Long>()
    var plan by mutableStateOf<PlanRecorrido?>(null)
    var paradaActual by mutableStateOf(0)
    /** Viviendas del recorrido a las que ya se llegó, para avisar una sola vez por cada una. */
    val llegadas = mutableStateListOf<Long>()
    var avisoLlegadaId by mutableStateOf<Long?>(null)

    fun irAVivienda(fichaId: Long) {
        reiniciarRecorrido()
        destinoId = fichaId
        seleccionadaId = fichaId
        iniciarRutaAlDestino = true
    }

    fun reiniciarRecorrido() {
        modoRecorrido = false
        paradasIds.clear()
        plan = null
        paradaActual = 0
        llegadas.clear()
        avisoLlegadaId = null
    }
}

/**
 * Mapa de seguimiento, dentro de la agenda: las viviendas con visitas como puntos de colores según el estado de la
 * visita (pendiente, programada, atrasada, realizada), con filtros por estado, fecha, barrio y riesgo; una tarjeta
 * para abrir la ficha y ver o cambiar la visita; y el recorrido ordenado con avisos de
 * llegada. Funciona sin internet con los mapas incluidos en la app.
 */
@Composable
fun MapaSeguimientoVista(
    usuarioId: Long,
    actividades: List<ActividadAgendaEntity>,
    ahora: Long,
    estado: EstadoMapaSeguimiento,
    onAbrirVisita: (List<ActividadAgendaEntity>) -> Unit,
    onAbrirFicha: (Long) -> Unit,
    onUbicarFicha: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val db = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val viviendas by remember(db, usuarioId) { db.fichaFamiliarDao().observarViviendasMapa(usuarioId, System.currentTimeMillis()) }
        .collectAsState(initial = emptyList())
    val sinUbicacion by remember(db) { db.fichaFamiliarDao().observarSinUbicacion() }
        .collectAsState(initial = emptyList())

    var verSinUbicacion by remember { mutableStateOf(false) }
    var ventanaFechas by remember { mutableStateOf(false) }
    var ubicacion by remember { mutableStateOf<Location?>(null) }
    var permiso by remember { mutableStateOf(GestorUbicacionActual.tienePermiso(context)) }
    var mapaLocal by remember { mutableStateOf<String?>(null) }
    var mapaDetalle by remember { mutableStateOf<String?>(null) }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var estiloListo by remember { mutableStateOf(false) }
    var ajusteInicial by remember { mutableStateOf(false) }
    var buscandoGps by remember { mutableStateOf(false) }
    var planificando by remember { mutableStateOf(false) }
    var avisoRecorrido by remember { mutableStateOf<String?>(null) }
    var rutaSel by remember { mutableStateOf<RutaCalculada?>(null) }
    var calculandoRutaSel by remember { mutableStateOf(false) }

    // El GPS en vivo solo se enciende mientras hay un recorrido que avisar; si no, basta una lectura puntual.
    val gpsVivo by rememberUbicacionEnVivo(activo = estado.plan != null && permiso)
    val posicion = gpsVivo ?: ubicacion

    // Sin las fechas elegidas por el usuario, el rango siempre es el de hoy.
    LaunchedEffect(ahora) {
        if (!estado.fechasElegidas) {
            val hoy = MapaSeguimiento.inicioDia(ahora)
            if (estado.desde != hoy) estado.desde = hoy
            if (estado.hasta != hoy) estado.hasta = hoy
        }
    }
    val hastaFinDeDia = MapaSeguimiento.finDia(estado.hasta)
    val puntosConVisita = remember(viviendas, actividades, estado.estados, estado.desde, estado.hasta, ahora) {
        MapaSeguimiento.puntos(viviendas, actividades, estado.estados, estado.desde, hastaFinDeDia, ahora)
    }
    // La vivienda de «Cómo llegar» se muestra aunque no tenga visita agendada.
    val puntos = remember(puntosConVisita, viviendas, estado.destinoId, ahora) {
        val destino = viviendas.firstOrNull { it.fichaId == estado.destinoId }
        if (destino == null || puntosConVisita.any { it.vivienda.fichaId == destino.fichaId }) puntosConVisita
        else puntosConVisita + MapaSeguimiento.puntoDeDestino(destino, ahora)
    }
    val conteos = remember(viviendas, actividades, estado.desde, estado.hasta, ahora) {
        MapaSeguimiento.conteos(viviendas, actividades, estado.desde, hastaFinDeDia, ahora)
    }
    val seleccionado = remember(puntos, estado.seleccionadaId) { puntos.firstOrNull { it.vivienda.fichaId == estado.seleccionadaId } }
    val sinUbicacionConVisita = remember(sinUbicacion, actividades, estado.estados, estado.desde, estado.hasta, ahora) {
        val conVisita = MapaSeguimiento.fichasConVisita(actividades, estado.estados, estado.desde, hastaFinDeDia, ahora)
        sinUbicacion.filter { it.fichaId in conVisita }
    }
    val ancho = LocalClaseAncho.current != ClaseAncho.COMPACTA

    fun alternarParada(id: Long) {
        avisoRecorrido = null
        estado.plan = null
        if (!estado.paradasIds.remove(id)) {
            if (estado.paradasIds.size >= RecorridoVisitas.MAXIMO_PARADAS) {
                avisoRecorrido = "Máximo ${RecorridoVisitas.MAXIMO_PARADAS} viviendas por recorrido."
            } else estado.paradasIds.add(id)
        }
    }

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
            actual.addSource(GeoJsonSource("recorrido-linea", VACIO))
            actual.addSource(GeoJsonSource("recorrido-paradas", VACIO))
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
                    circleColor(Expression.get("color")), circleRadius(10f),
                    circleStrokeColor("#FFFFFF"), circleStrokeWidth(2.5f)
                )
            )
            actual.addLayer(
                SymbolLayer("viv-letra", "viviendas").withFilter(Expression.not(Expression.has("point_count"))).withProperties(
                    textField(Expression.get("letra")),
                    textFont(arrayOf("Noto Sans Regular")), textSize(11f), textColor("#FFFFFF"),
                    textAllowOverlap(true), textIgnorePlacement(true)
                )
            )
            actual.addLayerBelow(
                LineLayer("rec-linea", "recorrido-linea").withProperties(
                    lineColor("#2B7CF0"), lineWidth(5f), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND)
                ),
                "viv-cluster"
            )
            actual.addLayer(
                CircleLayer("viv-seleccion", "vivienda-seleccionada").withProperties(
                    circleColor("#FFFFFF"), circleOpacity(0f), circleRadius(17f),
                    circleStrokeColor("#0A2A5E"), circleStrokeWidth(3.5f)
                )
            )
            actual.addLayer(
                CircleLayer("rec-numero-circulo", "recorrido-paradas").withProperties(
                    circleColor("#0A2A5E"), circleRadius(13f), circleStrokeColor("#FFFFFF"), circleStrokeWidth(2.5f)
                )
            )
            actual.addLayer(
                SymbolLayer("rec-numero-texto", "recorrido-paradas").withProperties(
                    textField(Expression.get("n")),
                    textFont(arrayOf("Noto Sans Regular")), textSize(13f), textColor("#FFFFFF"),
                    textAllowOverlap(true), textIgnorePlacement(true)
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
                tocada == null -> { estado.seleccionadaId = null; false }
                tocada.hasProperty("point_count") -> {
                    vista.animateCamera(CameraUpdateFactory.newLatLngZoom(punto, vista.cameraPosition.zoom + 2.0), 450)
                    true
                }
                estado.modoRecorrido -> { alternarParada(tocada.getNumberProperty("id").toLong()); true }
                else -> { estado.seleccionadaId = tocada.getNumberProperty("id").toLong(); true }
            }
        }
    }

    // Datos de los puntos
    LaunchedEffect(estiloListo, puntos) {
        if (!estiloListo) return@LaunchedEffect
        mapa?.style?.getSourceAs<GeoJsonSource>("viviendas")?.setGeoJson(MapaSeguimiento.geoJson(puntos))
    }
    // Cuadrar la cámara en lo que se ve: la primera vez y cada vez que cambia la búsqueda o un filtro
    LaunchedEffect(estiloListo, puntos.map { it.vivienda.fichaId }) {
        val vista = mapa ?: return@LaunchedEffect
        if (!estiloListo || puntos.isEmpty() || estado.plan != null) return@LaunchedEffect
        if (puntos.size == 1) {
            vista.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(puntos[0].vivienda.latitud, puntos[0].vivienda.longitud), 16.0), 500)
        } else {
            val limites = LatLngBounds.Builder().apply { puntos.forEach { include(LatLng(it.vivienda.latitud, it.vivienda.longitud)) } }.build()
            val animar = ajusteInicial
            vista.moveCamera(CameraUpdateFactory.newLatLngBounds(limites, 90))
            if (animar && vista.cameraPosition.zoom > 17.5) vista.moveCamera(CameraUpdateFactory.zoomTo(17.0))
        }
        ajusteInicial = true
    }
    LaunchedEffect(estiloListo, seleccionado, estado.modoRecorrido, estado.paradasIds.toList(), estado.plan) {
        if (!estiloListo) return@LaunchedEffect
        val fuente = mapa?.style?.getSourceAs<GeoJsonSource>("vivienda-seleccionada") ?: return@LaunchedEffect
        val resaltadas = when {
            estado.modoRecorrido && estado.plan == null -> viviendas.filter { it.fichaId in estado.paradasIds }
            estado.modoRecorrido -> emptyList()
            else -> listOfNotNull(seleccionado?.vivienda)
        }
        fuente.setGeoJson(
            if (resaltadas.isEmpty()) VACIO
            else resaltadas.joinToString(",", """{"type":"FeatureCollection","features":[""", "]}") {
                """{"type":"Feature","geometry":{"type":"Point","coordinates":[${it.longitud},${it.latitud}]},"properties":{}}"""
            }
        )
    }
    // Recorrido ordenado: línea y números sobre el mapa, y la cámara ajustada a todo el trayecto
    LaunchedEffect(estiloListo, estado.plan) {
        val vista = mapa ?: return@LaunchedEffect
        if (!estiloListo) return@LaunchedEffect
        val estilo = vista.style ?: return@LaunchedEffect
        val actual = estado.plan
        val linea = estilo.getSourceAs<GeoJsonSource>("recorrido-linea") ?: return@LaunchedEffect
        val numeros = estilo.getSourceAs<GeoJsonSource>("recorrido-paradas") ?: return@LaunchedEffect
        if (actual == null) { linea.setGeoJson(VACIO); numeros.setGeoJson(VACIO); return@LaunchedEffect }
        val trazo = actual.ruta?.puntos?.map { it.longitude to it.latitude } ?: buildList {
            posicion?.takeIf { actual.desdeMiUbicacion }?.let { add(it.longitude to it.latitude) }
            actual.paradas.forEach { add(it.vivienda.longitud to it.vivienda.latitud) }
        }
        linea.setGeoJson(
            """{"type":"Feature","geometry":{"type":"LineString","coordinates":[${trazo.joinToString(",") { "[${it.first},${it.second}]" }}]},"properties":{}}"""
        )
        numeros.setGeoJson(
            actual.paradas.mapIndexed { i, p ->
                """{"type":"Feature","geometry":{"type":"Point","coordinates":[${p.vivienda.longitud},${p.vivienda.latitud}]},"properties":{"n":"${i + 1}"}}"""
            }.joinToString(",", """{"type":"FeatureCollection","features":[""", "]}")
        )
        // Un encuadre necesita al menos dos puntos; con uno solo se centra en él.
        val extremos = actual.paradas.map { LatLng(it.vivienda.latitud, it.vivienda.longitud) } +
            listOfNotNull(posicion?.takeIf { actual.desdeMiUbicacion }?.let { LatLng(it.latitude, it.longitude) })
        if (extremos.size == 1) {
            vista.animateCamera(CameraUpdateFactory.newLatLngZoom(extremos.single(), 16.0), 600)
        } else if (extremos.size > 1) {
            val limites = LatLngBounds.Builder().apply { extremos.forEach { include(it) } }.build()
            runCatching { vista.animateCamera(CameraUpdateFactory.newLatLngBounds(limites, 110), 600) }
        }
    }
    // Distancia por camino hasta la vivienda elegida, con la red vial de la app (sin internet)
    val claveUbicacion = posicion?.let { (it.latitude * 2000).toInt() to (it.longitude * 2000).toInt() }
    LaunchedEffect(estado.seleccionadaId, estado.modoViaje, claveUbicacion, estado.modoRecorrido) {
        rutaSel = null
        val u = posicion
        val destino = seleccionado
        if (estado.modoRecorrido || u == null || destino == null) { calculandoRutaSel = false; return@LaunchedEffect }
        calculandoRutaSel = true
        rutaSel = GestorRutasOffline.calcular(
            context, LatLng(u.latitude, u.longitude), LatLng(destino.vivienda.latitud, destino.vivienda.longitud), estado.modoViaje
        ).getOrNull()
        calculandoRutaSel = false
    }
    LaunchedEffect(estiloListo, posicion) {
        if (!estiloListo) return@LaunchedEffect
        val fuente = mapa?.style?.getSourceAs<GeoJsonSource>("mi-ubicacion") ?: return@LaunchedEffect
        val u = posicion
        fuente.setGeoJson(
            if (u == null) VACIO
            else """{"type":"Feature","geometry":{"type":"Point","coordinates":[${u.longitude},${u.latitude}]},"properties":{}}"""
        )
    }
    // Aviso de llegada: al acercarse a la parada que toca, una sola vez por vivienda
    LaunchedEffect(gpsVivo, estado.plan, estado.paradaActual) {
        val u = gpsVivo ?: return@LaunchedEffect
        val parada = estado.plan?.paradas?.getOrNull(estado.paradaActual) ?: return@LaunchedEffect
        val id = parada.vivienda.fichaId
        if (id in estado.llegadas) return@LaunchedEffect
        val metros = MapaViviendas.distanciaMetros(u.latitude, u.longitude, parada.vivienda.latitud, parada.vivienda.longitud)
        if (LlegadaVivienda.haLlegado(metros, if (u.hasAccuracy()) u.accuracy else null)) {
            estado.llegadas.add(id)
            estado.avisoLlegadaId = id
            vibrarAviso(context)
        }
    }

    DisposableEffect(lifecycle, mapView) {
        var iniciado = false
        var reanudado = false
        fun sincronizar() {
            val e = lifecycle.currentState
            if (e.isAtLeast(Lifecycle.State.STARTED) && !iniciado) { mapView.onStart(); iniciado = true }
            if (e.isAtLeast(Lifecycle.State.RESUMED) && !reanudado) { mapView.onResume(); reanudado = true }
            if (!e.isAtLeast(Lifecycle.State.RESUMED) && reanudado) { mapView.onPause(); reanudado = false }
            if (!e.isAtLeast(Lifecycle.State.STARTED) && iniciado) { mapView.onStop(); iniciado = false }
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

    val distanciaSeleccion = remember(seleccionado, posicion) {
        val u = posicion
        if (seleccionado == null || u == null) null
        else MapaViviendas.distanciaMetros(u.latitude, u.longitude, seleccionado.vivienda.latitud, seleccionado.vivienda.longitud)
    }

    fun planificar() {
        val elegidas = estado.paradasIds.mapNotNull { id -> puntos.firstOrNull { it.vivienda.fichaId == id } }
        if (elegidas.size < 2 && !(elegidas.size == 1 && posicion != null)) {
            avisoRecorrido = "Elige al menos 2 viviendas."
            return
        }
        val modo = estado.modoViaje
        scope.launch {
            planificando = true
            avisoRecorrido = null
            val origen = posicion?.let { LatLng(it.latitude, it.longitude) }
            val coordenadas = listOfNotNull(origen) + elegidas.map { LatLng(it.vivienda.latitud, it.vivienda.longitud) }
            val costos = GestorRutasOffline.matriz(context, coordenadas, modo).getOrNull()
                ?: RecorridoVisitas.matrizEnLinea(coordenadas.map { it.latitude to it.longitude })
            val orden = RecorridoVisitas.ordenar(costos, if (origen != null) 0 else null)
            val desplazamiento = if (origen != null) 1 else 0
            val paradas = orden.filter { it >= desplazamiento }.map { elegidas[it - desplazamiento] }
            val trazo = listOfNotNull(origen) + paradas.map { LatLng(it.vivienda.latitud, it.vivienda.longitud) }
            val ruta = GestorRutasOffline.calcularVarios(context, trazo, modo).getOrNull()
            val enLinea = (0 until trazo.size - 1).sumOf {
                MapaViviendas.distanciaMetros(trazo[it].latitude, trazo[it].longitude, trazo[it + 1].latitude, trazo[it + 1].longitude)
            } / 1000.0
            estado.llegadas.clear()
            estado.avisoLlegadaId = null
            estado.paradaActual = 0
            estado.plan = PlanRecorrido(paradas, ruta, ruta?.kilometros ?: enLinea, ruta?.minutos, modo, origen != null)
            planificando = false
        }
    }

    // «Cómo llegar» desde la ficha: con la vivienda ya en el mapa se arma el recorrido de una sola parada, que dibuja la
    // ruta por camino desde donde estás y avisa al llegar. Si falta el permiso de ubicación se pide.
    LaunchedEffect(estado.iniciarRutaAlDestino, estado.destinoId, puntos, permiso, posicion) {
        if (!estado.iniciarRutaAlDestino) return@LaunchedEffect
        val destino = estado.destinoId ?: run { estado.iniciarRutaAlDestino = false; return@LaunchedEffect }
        if (puntos.none { it.vivienda.fichaId == destino }) return@LaunchedEffect
        if (!permiso) {
            avisoRecorrido = "Permite la ubicación para trazar la ruta hasta la vivienda."
            centrarEnMiUbicacion()
            return@LaunchedEffect
        }
        if (posicion == null) return@LaunchedEffect
        estado.iniciarRutaAlDestino = false
        estado.modoRecorrido = true
        estado.paradasIds.clear()
        estado.paradasIds.add(destino)
        planificar()
    }

    fun seleccionar(punto: PuntoSeguimiento) {
        val v = punto.vivienda
        if (estado.modoRecorrido) {
            if (estado.plan == null) alternarParada(v.fichaId)
            mapa?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(v.latitud, v.longitud), 16.0), 450)
            return
        }
        estado.seleccionadaId = v.fichaId
        mapa?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(v.latitud, v.longitud), 17.0), 450)
    }

    // Un destino sin visita agendada (botón «Cómo llegar») abre su ficha en lugar de una visita.
    fun abrirVisita(p: PuntoSeguimiento) =
        if (p.visitas.isEmpty()) onAbrirFicha(p.vivienda.fichaId)
        else onAbrirVisita(MapaSeguimiento.grupoDe(p.principal, actividades))

    val avisoLlegada = estado.avisoLlegadaId?.let { id -> estado.plan?.paradas?.firstOrNull { it.vivienda.fichaId == id } }

    Column(modifier.fillMaxSize().background(Color(0xFFF6F9FB))) {
        val panelBusqueda: @Composable () -> Unit = {
            Row(
                Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var abierto by remember { mutableStateOf(false) }
                Box(Modifier.weight(1f)) {
                    BotonControl(
                        texto = if (estado.estados.size == EstadoVisita.entries.size) "Estado · todos" else "Estado · ${estado.estados.size} de ${EstadoVisita.entries.size}",
                        activo = estado.estados.size != EstadoVisita.entries.size, modifier = Modifier.testTag("boton_estado")
                    ) { abierto = true }
                    com.ruralitos.app.ui.components.MenuDesplegableRuralitos(expanded = abierto, onDismissRequest = { abierto = false }) {
                        EstadoVisita.entries.forEach { e ->
                            val marcado = e in estado.estados
                            com.ruralitos.app.ui.components.ItemMenuRuralitos(
                                modifier = Modifier.testTag("estado_${e.name}"),
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        com.ruralitos.app.ui.components.CasillaRuralitos(marcado)
                                        Spacer(Modifier.width(10.dp))
                                        Box(Modifier.size(11.dp).clip(CircleShape).background(colorDe(e)))
                                        Text("  ${e.etiqueta}", color = AzulTexto)
                                        Text("  · ${conteos[e] ?: 0}", color = GrisTexto, fontSize = 12.sp)
                                    }
                                },
                                // el menú queda abierto para marcar varios; siempre debe quedar al menos un estado
                                onClick = {
                                    if (marcado && estado.estados.size == 1) return@ItemMenuRuralitos
                                    estado.estados = if (marcado) estado.estados - e else estado.estados + e
                                    estado.plan = null
                                }
                            )
                        }
                        com.ruralitos.app.ui.components.ItemMenuRuralitos(
                            text = { Text("Listo", color = CianRuralitos, fontWeight = FontWeight.SemiBold) },
                            onClick = { abierto = false }
                        )
                    }
                }
                Box(Modifier.weight(1.5f)) {
                    BotonControl(
                        texto = textoRango(estado.desde, estado.hasta),
                        activo = estado.fechasElegidas, modifier = Modifier.testTag("boton_fecha")
                    ) { ventanaFechas = true }
                }
            }
        }
        val mapaConControles: @Composable (Modifier) -> Unit = { modificador ->
            Box(modificador) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize().testTag("mapa_viviendas"))
                if (estado.modoRecorrido && estado.plan == null) {
                    BarraRecorrido(
                        elegidas = estado.paradasIds.size, modo = estado.modoViaje, aviso = avisoRecorrido,
                        planificando = planificando, hayPlan = estado.plan != null, hayGps = posicion != null,
                        onModo = { estado.modoViaje = it; estado.plan = null },
                        onElegirVisibles = {
                            estado.plan = null; avisoRecorrido = null
                            val porHacer = puntos.filter { it.estado != EstadoVisita.REALIZADA }.map { it.vivienda.fichaId }
                            estado.paradasIds.clear()
                            estado.paradasIds.addAll(porHacer.take(RecorridoVisitas.MAXIMO_PARADAS))
                            if (porHacer.isEmpty()) avisoRecorrido = "No hay visitas por hacer en lo que se ve."
                            else if (porHacer.size > RecorridoVisitas.MAXIMO_PARADAS)
                                avisoRecorrido = "Se eligieron las primeras ${RecorridoVisitas.MAXIMO_PARADAS} de ${porHacer.size}."
                        },
                        onLimpiar = { estado.paradasIds.clear(); estado.plan = null; avisoRecorrido = null },
                        onOrdenar = { planificar() },
                        onSalir = { estado.reiniciarRecorrido(); avisoRecorrido = null },
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp, start = 8.dp, end = 56.dp)
                    )
                } else if (!estado.modoRecorrido) {
                    Row(
                        Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = if (!ancho && seleccionado != null) 250.dp else 30.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (sinUbicacionConVisita.isNotEmpty()) {
                            Surface(
                                onClick = { verSinUbicacion = true },
                                shape = RoundedCornerShape(16.dp), color = Color(0xFFFFF8EF),
                                border = BorderStroke(1.dp, Color(0xFFFFD7A3)), shadowElevation = 1.dp
                            ) {
                                Text(
                                    "${sinUbicacionConVisita.size} sin ubicación · ver",
                                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color(0xFF8A4B00), fontSize = 12.sp, fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                Column(Modifier.align(Alignment.CenterEnd).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonMapa(if (buscandoGps) "…" else "◎", "Ir a mi ubicación", Modifier.testTag("boton_mi_ubicacion")) { centrarEnMiUbicacion() }
                    BotonMapa("+", "Acercar") { mapa?.animateCamera(CameraUpdateFactory.zoomIn()) }
                    BotonMapa("−", "Alejar") { mapa?.animateCamera(CameraUpdateFactory.zoomOut()) }
                }
                if (estado.plan == null) Row(
                    Modifier.align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = if (!ancho && seleccionado != null) 250.dp else 30.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            if (estado.modoRecorrido) estado.reiniciarRecorrido()
                            else { estado.modoRecorrido = true; estado.seleccionadaId = null }
                        },
                        modifier = Modifier.testTag("chip_recorrido"),
                        shape = RoundedCornerShape(22.dp), shadowElevation = 3.dp,
                        color = if (estado.modoRecorrido) AzulTexto else Color.White,
                        border = BorderStroke(1.dp, if (estado.modoRecorrido) AzulTexto else VerdeAgenda)
                    ) {
                        Text(
                            if (estado.modoRecorrido) "Recorrido · ${estado.paradasIds.size}" else "Recorrido",
                            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            color = if (estado.modoRecorrido) Color.White else VerdeAgenda, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                        )
                    }
                }
                if (!ancho && estado.plan == null && !estado.modoRecorrido) Leyenda(conteos, Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 8.dp))
                if (viviendas.isEmpty()) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 2.dp
                    ) {
                        Text(
                            "Aún no hay viviendas con ubicación. Abre una ficha, entra a Ubicación de vivienda y guarda el punto de la vivienda.",
                            Modifier.padding(16.dp), color = AzulTexto, fontSize = 14.sp
                        )
                    }
                } else if (puntos.isEmpty()) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp).testTag("sin_visitas_mapa"),
                        shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 2.dp
                    ) {
                        Text(
                            "No hay visitas en estas fechas con los estados marcados. Cambia las fechas o agenda una visita.",
                            Modifier.padding(16.dp), color = AzulTexto, fontSize = 14.sp
                        )
                    }
                }
                Text(
                    "© OpenStreetMap contributors · Protomaps",
                    Modifier.align(Alignment.BottomStart).background(Color(0xEFFFFFFF)).padding(horizontal = 6.dp, vertical = 3.dp),
                    color = AzulClinico, fontSize = 10.sp
                )
                if (!ancho && estado.modoRecorrido && estado.plan != null) {
                    TarjetaRecorrido(
                        estado.plan!!, estado.paradaActual, onSeleccionar = { seleccionar(it) },
                        onCerrar = { estado.plan = null },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                    )
                } else if (!ancho && !estado.modoRecorrido && seleccionado != null) {
                    TarjetaVisita(
                        seleccionado, distanciaSeleccion, rutaSel, calculandoRutaSel, estado.modoViaje, { estado.modoViaje = it },
                        onCerrar = { if (estado.seleccionadaId == estado.destinoId) estado.destinoId = null; estado.seleccionadaId = null },
                        onVerVisita = { abrirVisita(seleccionado) },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                    )
                }
                if (avisoLlegada != null) {
                    AvisoLlegada(
                        avisoLlegada, esUltima = estado.paradaActual >= (estado.plan?.paradas?.lastIndex ?: 0),
                        onRegistrar = { abrirVisita(avisoLlegada) },
                        onSiguiente = {
                            estado.avisoLlegadaId = null
                            estado.paradaActual = (estado.paradaActual + 1).coerceAtMost(estado.plan?.paradas?.lastIndex ?: 0)
                        },
                        onCerrar = { estado.avisoLlegadaId = null },
                        modifier = Modifier.align(Alignment.TopCenter).padding(8.dp)
                    )
                }
            }
        }

        if (ancho) {
            val estadoLista = rememberLazyListState()
            // La lista conserva su posición al aparecer una tarjeta arriba: se sube para que se vea.
            LaunchedEffect(estado.plan, estado.seleccionadaId) {
                if (estado.plan != null || estado.seleccionadaId != null) estadoLista.animateScrollToItem(0)
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                // Todo el panel se desplaza junto: en una pantalla ancha pero baja (teléfono horizontal) la tarjeta
                // no debe empujar fuera de la pantalla sus botones.
                LazyColumn(Modifier.width(360.dp).fillMaxHeight().background(Color.White), state = estadoLista) {
                    if (estado.modoRecorrido && estado.plan != null) {
                        item(key = "recorrido") {
                            TarjetaRecorrido(
                                estado.plan!!, estado.paradaActual, onSeleccionar = { seleccionar(it) },
                                onCerrar = { estado.plan = null },
                                modifier = Modifier.padding(8.dp), conDesplazamiento = false
                            )
                        }
                    } else if (!estado.modoRecorrido && seleccionado != null) {
                        item(key = "tarjeta") {
                            TarjetaVisita(
                                seleccionado, distanciaSeleccion, rutaSel, calculandoRutaSel, estado.modoViaje, { estado.modoViaje = it },
                                onCerrar = { if (estado.seleccionadaId == estado.destinoId) estado.destinoId = null; estado.seleccionadaId = null },
                                onVerVisita = { abrirVisita(seleccionado) },
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    item(key = "filtros") { panelBusqueda() }
                    item(key = "leyenda") { Leyenda(conteos, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontal = true) }
                    items(puntos, key = { it.vivienda.fichaId }) { p ->
                        val numero = estado.plan?.paradas?.indexOfFirst { it.vivienda.fichaId == p.vivienda.fichaId }?.takeIf { it >= 0 }?.plus(1)
                        FilaPunto(
                            p, if (estado.modoRecorrido) p.vivienda.fichaId in estado.paradasIds else p.vivienda.fichaId == estado.seleccionadaId,
                            marca = if (estado.modoRecorrido) (numero?.toString() ?: if (p.vivienda.fichaId in estado.paradasIds) "✓" else null) else null
                        ) { seleccionar(p) }
                    }
                }
                mapaConControles(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            panelBusqueda()
            mapaConControles(Modifier.weight(1f).fillMaxWidth())
        }
    }

    if (ventanaFechas) {
        VentanaFechas(
            desde = estado.desde, hasta = estado.hasta,
            onAplicar = { d, h ->
                estado.desde = d; estado.hasta = h; estado.fechasElegidas = true; estado.plan = null
                ventanaFechas = false
            },
            onHoy = {
                val hoy = MapaSeguimiento.inicioDia(System.currentTimeMillis())
                estado.desde = hoy; estado.hasta = hoy; estado.fechasElegidas = false; estado.plan = null
                ventanaFechas = false
            },
            onCerrar = { ventanaFechas = false }
        )
    }

    if (verSinUbicacion) {
        VentanaRuralitos(
            titulo = "Viviendas sin ubicación",
            simbolo = "⌖",
            color = AzulClinico,
            onCerrar = { verSinUbicacion = false },
            contenido = {
        Column {
                            Text(
                                "Pulsa «Ubicar ahora» para abrir el croquis de la ficha y guardar el punto de la vivienda.",
                                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyColumn(Modifier.height(280.dp)) {
                                items(sinUbicacionConVisita, key = { it.fichaId }) { f ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f).clickable { verSinUbicacion = false; onAbrirFicha(f.fichaId) }) {
                                            Text(f.jefe.ifBlank { "Ficha sin nombre" }, fontWeight = FontWeight.Medium, color = AzulTexto)
                                            Text(
                                                listOf(f.barrio, "Ficha ${f.numero}").filter { it.isNotBlank() }.joinToString(" · "),
                                                color = GrisTexto, fontSize = 12.sp
                                            )
                                        }
                                        Surface(
                                            onClick = { verSinUbicacion = false; onUbicarFicha(f.fichaId) },
                                            modifier = Modifier.testTag("ubicar_ahora"),
                                            shape = RoundedCornerShape(10.dp), color = CianRuralitos
                                        ) {
                                            Text("Ubicar ahora", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
            },
            acciones = {
                BotonSecundarioRuralitos(texto = "Cerrar", onClick = { verSinUbicacion = false })
            }
        )
    }
}

private val MESES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)

private fun textoRango(desde: Long, hasta: Long): String {
    fun f(t: Long) = "%02d/%02d/%d".format(MapaSeguimiento.dia(t), MapaSeguimiento.mes(t), MapaSeguimiento.anio(t))
    return "${f(desde)} – ${f(hasta)}"
}

@Composable
private fun BotonControl(texto: String, activo: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        color = if (activo) Color(0xFFE3F4F7) else Color.White,
        border = BorderStroke(1.dp, if (activo) VerdeAgenda else Color(0xFFCFDDE5))
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            TextoAjustado(texto, Modifier.weight(1f), tamano = 13.sp, tamanoMinimo = 7.sp, color = AzulTexto, fontWeight = FontWeight.Medium)
            Text(" ▾", color = VerdeAgenda, fontSize = 14.sp)
        }
    }
}

/** Fecha de inicio y fecha de fin, cada una con el calendario de la app. Ninguna puede quedar antes de la otra. */
@Composable
private fun VentanaFechas(
    desde: Long,
    hasta: Long,
    onAplicar: (Long, Long) -> Unit,
    onHoy: () -> Unit,
    onCerrar: () -> Unit
) {
    var inicio by remember { mutableStateOf(MapaSeguimiento.inicioDia(desde)) }
    var fin by remember { mutableStateOf(MapaSeguimiento.inicioDia(hasta)) }
    var error by remember { mutableStateOf<String?>(null) }
    var editando by remember { mutableStateOf<String?>(null) }

    editando?.let { campo ->
        com.ruralitos.app.ui.components.CalendarioRuralitos(
            titulo = if (campo == "ini") "Fecha inicio" else "Fecha fin",
            fechaInicialMillis = if (campo == "ini") inicio else fin,
            onElegida = { dia ->
                if (campo == "ini") inicio = dia else fin = dia
                error = null
                editando = null
            },
            onCerrar = { editando = null }
        )
    }

    com.ruralitos.app.ui.components.VentanaRuralitos(
        titulo = "Fechas del mapa",
        subtitulo = "Visitas entre dos fechas",
        simbolo = "▦",
        color = CianRuralitos,
        onCerrar = onCerrar,
        contenido = {
            CampoFechaMapa("Fecha inicio", inicio, "campo_fecha_ini") { editando = "ini" }
            CampoFechaMapa("Fecha fin", fin, "campo_fecha_fin") { editando = "fin" }
            if (error != null) {
                Text(error!!, color = Color(0xFFC83E4D), fontSize = 12.sp, modifier = Modifier.testTag("error_fechas"))
            }
        },
        acciones = {
            com.ruralitos.app.ui.components.BotonPrincipalRuralitos(
                texto = "Aplicar",
                color = CianRuralitos,
                modifier = Modifier.testTag("aplicar_fechas"),
                onClick = {
                    if (fin < inicio) error = "La fecha fin no puede ser anterior a la fecha inicio."
                    else onAplicar(inicio, fin)
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.ruralitos.app.ui.components.BotonSecundarioRuralitos(
                    texto = "Hoy", onClick = onHoy, modifier = Modifier.weight(1f).testTag("fechas_hoy")
                )
                com.ruralitos.app.ui.components.BotonSecundarioRuralitos(
                    texto = "Cancelar", onClick = onCerrar, modifier = Modifier.weight(1f)
                )
            }
        }
    )
}

@Composable
private fun CampoFechaMapa(etiqueta: String, valor: Long, etiquetaPrueba: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(etiquetaPrueba),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFCFDDE5))
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(etiqueta, color = GrisTexto, fontSize = 10.sp)
            Text(
                "%02d/%02d/%d".format(MapaSeguimiento.dia(valor), MapaSeguimiento.mes(valor), MapaSeguimiento.anio(valor)),
                color = AzulTexto, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ChipFiltro(texto: String, activo: Boolean, modificador: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modificador, shape = RoundedCornerShape(16.dp),
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
private fun Leyenda(conteos: Map<EstadoVisita, Int>, modifier: Modifier = Modifier, horizontal: Boolean = false) {
    val contenido: @Composable () -> Unit = {
        EstadoVisita.entries.forEach { e ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = if (horizontal) 10.dp else 0.dp)) {
                Box(Modifier.size(15.dp).clip(CircleShape).background(colorDe(e)), contentAlignment = Alignment.Center) {
                    Text(e.letra, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
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
private fun FilaPunto(p: PuntoSeguimiento, activa: Boolean, marca: String? = null, onClick: () -> Unit) {
    val v = p.vivienda
    Row(
        Modifier.fillMaxWidth().background(if (activa) Color(0xFFE3F4F7) else Color.White)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(colorDe(p.estado)), contentAlignment = Alignment.Center) {
            Text(p.estado.letra, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text(v.jefe.ifBlank { "Ficha sin nombre" }, color = AzulTexto, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(v.barrio, if (p.visitas.isEmpty()) "Destino" else fecha(p.principal.fechaHora)).filter { it.isNotBlank() }.joinToString(" · "),
                color = GrisTexto, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (marca != null) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(AzulTexto), contentAlignment = Alignment.Center) {
                Text(marca, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else Text(p.estado.etiqueta, color = colorDe(p.estado), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(BordeClinico))
}

@Composable
private fun TarjetaVisita(
    p: PuntoSeguimiento,
    distanciaMetros: Double?,
    ruta: RutaCalculada?,
    calculandoRuta: Boolean,
    modo: String,
    onModo: (String) -> Unit,
    onCerrar: () -> Unit,
    onVerVisita: () -> Unit,
    modifier: Modifier = Modifier
) {
    val v = p.vivienda
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
            if (v.gestantes + v.menoresCinco + v.adultosMayores > 0 || v.nivelRiesgo.equals("ALTO", true)) {
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (v.nivelRiesgo.equals("ALTO", true)) Etiqueta("Riesgo alto", Color(0xFFD32F2F))
                    if (v.gestantes > 0) Etiqueta("Gestante${if (v.gestantes > 1) "s ${v.gestantes}" else ""}", Color(0xFF7B1FA2))
                    if (v.menoresCinco > 0) Etiqueta("Menor de 5 años", Color(0xFF1565C0))
                    if (v.adultosMayores > 0) Etiqueta("Mayor de 65", Color(0xFF5D4037))
                }
            }
            if (distanciaMetros != null) {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MODOS_VIAJE.forEach { (valor, etiqueta) -> ChipFiltro(etiqueta, valor == modo) { onModo(valor) } }
                }
                Text(
                    when {
                        ruta != null -> "Por camino: ${MapaViviendas.textoDistancia(ruta.kilometros * 1000)} · unos ${ruta.minutos} min ${textoModo(modo)}"
                        calculandoRuta -> "Calculando la ruta por camino… · en línea recta ${MapaViviendas.textoDistancia(distanciaMetros)}"
                        else -> "A ${MapaViviendas.textoDistancia(distanciaMetros)} en línea recta (sin ruta por camino)"
                    },
                    color = AzulTexto, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp).testTag("distancia_ruta")
                )
            }
            BotonTarjeta(if (p.visitas.isEmpty()) "Abrir la ficha" else "Ver visita", relleno = true, Modifier.fillMaxWidth().padding(top = 10.dp).testTag("ver_visita_mapa"), onVerVisita)
        }
    }
}

@Composable
private fun BotonTarjeta(texto: String, relleno: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp),
        color = if (relleno) CianRuralitos else Color.White,
        border = if (relleno) null else BorderStroke(1.dp, CianRuralitos)
    ) {
        Text(
            texto, Modifier.padding(vertical = 10.dp).fillMaxWidth(),
            color = if (relleno) Color.White else CianRuralitos, fontWeight = FontWeight.Medium, fontSize = 14.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Etiqueta(texto: String, color: Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.14f)) {
        Text(texto, Modifier.padding(horizontal = 9.dp, vertical = 3.dp), color = color.copy(alpha = 1f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BarraRecorrido(
    elegidas: Int,
    modo: String,
    aviso: String?,
    planificando: Boolean,
    hayPlan: Boolean,
    hayGps: Boolean,
    onModo: (String) -> Unit,
    onElegirVisibles: () -> Unit,
    onLimpiar: () -> Unit,
    onOrdenar: () -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag("barra_recorrido"),
        shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 3.dp, border = BorderStroke(1.dp, BordeClinico)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (elegidas == 0) "Toca las viviendas que vas a visitar" else "$elegidas elegida${if (elegidas == 1) "" else "s"}",
                    Modifier.weight(1f), color = AzulTexto, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                )
                Text("✕", color = GrisTexto, fontSize = 18.sp, modifier = Modifier.testTag("salir_recorrido").clickable(onClick = onSalir).padding(4.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                ChipFiltro("Elegir visibles", false, Modifier.testTag("recorrido_visibles")) { onElegirVisibles() }
                MODOS_VIAJE.forEach { (valor, etiqueta) -> ChipFiltro(etiqueta, valor == modo) { onModo(valor) } }
                ChipFiltro("Limpiar", false) { onLimpiar() }
            }
            val puede = !planificando && (elegidas >= 2 || (elegidas == 1 && hayGps))
            Surface(
                onClick = { if (puede) onOrdenar() }, modifier = Modifier.fillMaxWidth().testTag("recorrido_ordenar"),
                shape = RoundedCornerShape(12.dp), color = if (puede) CianRuralitos else Color(0xFFB8C7D1)
            ) {
                Text(
                    when {
                        planificando -> "Calculando el mejor orden…"
                        hayPlan -> "Volver a ordenar"
                        else -> "Ordenar recorrido"
                    },
                    Modifier.padding(vertical = 9.dp).fillMaxWidth(), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                aviso ?: if (hayGps) "Empieza desde tu ubicación actual." else "Sin GPS: empieza por la vivienda más conveniente.",
                color = if (aviso != null) Color(0xFF8A4B00) else GrisTexto, fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TarjetaRecorrido(
    plan: PlanRecorrido,
    paradaActual: Int,
    onSeleccionar: (PuntoSeguimiento) -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
    conDesplazamiento: Boolean = true
) {
    val actual = paradaActual.coerceIn(0, plan.paradas.lastIndex)
    Surface(
        modifier = modifier.fillMaxWidth().testTag("tarjeta_recorrido"),
        shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 4.dp, border = BorderStroke(1.dp, BordeClinico)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Recorrido de ${plan.paradas.size} viviendas", color = AzulTexto, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        if (plan.ruta != null) "${MapaViviendas.textoDistancia(plan.kilometros * 1000)} · unos ${plan.minutos} min ${textoModo(plan.modo)}"
                        else "${MapaViviendas.textoDistancia(plan.kilometros * 1000)} en línea recta (sin ruta por camino)",
                        color = GrisTexto, fontSize = 12.sp, modifier = Modifier.testTag("resumen_recorrido")
                    )
                }
                Text("✕", color = GrisTexto, fontSize = 18.sp, modifier = Modifier.testTag("cerrar_recorrido").clickable(onClick = onCerrar).padding(4.dp))
            }
            val columna: @Composable () -> Unit = {
                plan.paradas.forEachIndexed { i, p ->
                    val tramo = plan.ruta?.tramos?.getOrNull(if (plan.desdeMiUbicacion) i else i - 1)
                    val hecha = i < actual
                    Row(
                        Modifier.fillMaxWidth().clickable { onSeleccionar(p) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(24.dp).clip(CircleShape).background(if (hecha) Color(0xFF9AB0BD) else AzulTexto),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (hecha) "✓" else "${i + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.padding(start = 10.dp).weight(1f)) {
                            Text(
                                p.vivienda.jefe.ifBlank { "Ficha sin nombre" }, color = AzulTexto,
                                fontWeight = if (i == actual) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                listOf(p.vivienda.barrio, if (p.visitas.isEmpty()) "Destino" else p.estado.etiqueta, if (p.visitas.isEmpty()) "" else fecha(p.principal.fechaHora, "d MMM HH:mm")).filter { it.isNotBlank() }.joinToString(" · "),
                                color = GrisTexto, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (tramo != null) Text("${MapaViviendas.textoDistancia(tramo.first * 1000)} · ${tramo.second} min", color = GrisTexto, fontSize = 11.sp)
                    }
                }
            }
            if (conDesplazamiento) {
                Column(Modifier.padding(top = 6.dp).height(150.dp).verticalScroll(rememberScrollState())) { columna() }
            } else {
                Column(Modifier.padding(top = 6.dp)) { columna() }
            }
        }
    }
}

/** Aparece al acercarse a la vivienda que toca visitar; vibra una sola vez. */
@Composable
private fun AvisoLlegada(
    p: PuntoSeguimiento,
    esUltima: Boolean,
    onRegistrar: () -> Unit,
    onSiguiente: () -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag("aviso_llegada"),
        shape = RoundedCornerShape(16.dp), color = Color(0xFFE6F6EC), shadowElevation = 6.dp,
        border = BorderStroke(1.5.dp, Color(0xFF2E9E5B))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Llegaste a la vivienda", color = Color(0xFF1B6B3A), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        listOf(p.vivienda.jefe.ifBlank { "Ficha sin nombre" }, p.vivienda.barrio).filter { it.isNotBlank() }.joinToString(" · "),
                        color = AzulTexto, fontSize = 13.sp
                    )
                }
                Text("✕", color = GrisTexto, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onCerrar).padding(4.dp))
            }
            BotonTarjeta("Registrar visita", relleno = true, Modifier.fillMaxWidth().testTag("registrar_llegada"), onRegistrar)
            if (!esUltima) BotonTarjeta("Siguiente parada", relleno = false, Modifier.fillMaxWidth().testTag("siguiente_parada"), onSiguiente)
        }
    }
}
