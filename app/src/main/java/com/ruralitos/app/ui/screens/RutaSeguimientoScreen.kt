package com.ruralitos.app.ui.screens

import android.Manifest
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.location.GestorUbicacionActual
import com.ruralitos.app.R
import com.ruralitos.app.data.mapa.GestorMapaCampo
import com.ruralitos.app.data.mapa.GestorMapaDetalle
import com.ruralitos.app.data.mapa.GestorRutasOffline
import com.ruralitos.app.data.mapa.RutaCalculada
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import org.json.JSONObject
import org.json.JSONArray
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.PI

/** Navegación local desde el GPS actual hasta la vivienda guardada, sin alterar la ficha. */
@Composable
fun RutaSeguimientoScreen(fichaId: Long, usuarioId: Long?, onRegresar: () -> Unit, onAbrirFicha: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val db = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val ficha by db.fichaFamiliarDao().observarPorId(fichaId).collectAsState(initial = null)
    var mapaLocal by remember { mutableStateOf<String?>(null) }
    var mapaDetalle by remember { mutableStateOf<String?>(null) }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var estiloListo by remember { mutableStateOf(false) }
    var mapaEnUso by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("Preparando mapa de calles sin conexión…") }
    var ubicacionActual by remember { mutableStateOf<Location?>(null) }
    var permisoUbicacion by remember { mutableStateOf(GestorUbicacionActual.tienePermiso(context)) }
    var siguiendoGps by remember { mutableStateOf(true) }
    var marcadorGps by remember { mutableStateOf<Marker?>(null) }
    var calculandoRuta by remember { mutableStateOf(false) }
    var ultimoCalculo by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()
    val solicitarPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisoUbicacion = GestorUbicacionActual.tienePermiso(context) }
    val destino = remember(ficha?.latitud, ficha?.longitud) {
        ficha?.latitud?.let { lat -> ficha?.longitud?.let { lon -> LatLng(lat, lon) } }
    }
    var ruta by remember { mutableStateOf<RutaCalculada?>(null) }
    var modo by remember { mutableStateOf("auto") }
    val gps = ubicacionActual?.let { LatLng(it.latitude, it.longitude) }
    var avance by remember { mutableStateOf<AvanceRuta?>(null) }

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
        mensaje = if (mapaLocal == null) "No se pudo preparar el mapa local. La ubicación sigue disponible."
            else "Mapa de calles disponible sin internet."
    }
    LaunchedEffect(Unit) {
        if (!permisoUbicacion) solicitarPermiso.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }
    DisposableEffect(lifecycle, permisoUbicacion) {
        val servicio = context.getSystemService(LocationManager::class.java)
        val oyentes = mutableListOf<LocationListener>()
        fun detener() {
            oyentes.forEach { runCatching { servicio.removeUpdates(it) } }
            oyentes.clear()
        }
        fun iniciar() {
            if (!permisoUbicacion || oyentes.isNotEmpty()) return
            val proveedores = buildList {
                if (GestorUbicacionActual.tienePermisoPreciso(context) &&
                    runCatching { servicio.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
                    add(LocationManager.GPS_PROVIDER)
                }
                if (runCatching { servicio.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
                    add(LocationManager.NETWORK_PROVIDER)
                }
            }
            proveedores.forEach { proveedor ->
                val oyente = LocationListener { nueva ->
                    val edadMs = (SystemClock.elapsedRealtimeNanos() - nueva.elapsedRealtimeNanos) / 1_000_000
                    if (edadMs !in 0..10_000 || !nueva.hasAccuracy() || nueva.accuracy > 100f ||
                        !nueva.latitude.isFinite() || !nueva.longitude.isFinite()) return@LocationListener
                    val anterior = ubicacionActual
                    if (anterior == null || nueva.elapsedRealtimeNanos >= anterior.elapsedRealtimeNanos ||
                        nueva.accuracy < anterior.accuracy / 2f) ubicacionActual = nueva
                }
                runCatching {
                    servicio.requestLocationUpdates(proveedor, 1_000L, 1f, oyente, Looper.getMainLooper())
                    oyentes += oyente
                }
            }
            if (oyentes.isEmpty()) mensaje = "Activa la ubicación del teléfono para iniciar la navegación."
        }
        val observador = LifecycleEventObserver { _, _ ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) iniciar() else detener()
        }
        lifecycle.addObserver(observador)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) iniciar()
        onDispose { lifecycle.removeObserver(observador); detener() }
    }
    LaunchedEffect(permisoUbicacion) {
        if (permisoUbicacion && ubicacionActual == null) {
            GestorUbicacionActual.obtener(context)?.let { ubicacionActual = it }
        }
    }
    LaunchedEffect(destino, modo) {
        ruta = null
        avance = null
        ultimoCalculo = 0L
    }
    LaunchedEffect(gps, ruta) {
        val posicion = gps
        val recorrido = ruta
        avance = if (posicion != null && recorrido != null) {
            withContext(Dispatchers.Default) { recortarRuta(posicion, recorrido.puntos) }
        } else null
    }
    LaunchedEffect(gps, destino, modo, avance, calculandoRuta) {
        val desde = gps ?: return@LaunchedEffect
        val hasta = destino ?: return@LaunchedEffect
        if (ruta != null && avance == null) return@LaunchedEffect
        val ahora = SystemClock.elapsedRealtime()
        val umbralDesvio = max(45.0, (ubicacionActual?.accuracy ?: 0f) * 1.5)
        val cambiarRuta = ruta == null || avance == null ||
            (avance?.desvioMetros ?: 0.0) > umbralDesvio
        val esperaMinima = if (ruta == null) 8_000L else 12_000L
        if (!cambiarRuta || calculandoRuta || ahora - ultimoCalculo < esperaMinima) return@LaunchedEffect
        calculandoRuta = true
        ultimoCalculo = ahora
        mensaje = if (ruta == null) "Calculando ruta desde tu ubicación…" else "Actualizando ruta…"
        val modoSolicitado = modo
        scope.launch {
            val resultado = GestorRutasOffline.calcular(context, desde, hasta, modoSolicitado)
            if (destino == hasta && modo == modoSolicitado) {
                resultado.onSuccess {
                    ruta = it
                    mensaje = "Ruta activa sin internet. Sigue el marcador azul."
                }.onFailure {
                    mensaje = if (ruta != null) "GPS activo. Se conserva la última ruta mientras se recalcula."
                    else "No se pudo calcular la ruta. Comprueba que hay vías registradas cerca de ambos puntos."
                }
            }
            calculandoRuta = false
        }
    }
    LaunchedEffect(mapa, mapaLocal, mapaDetalle, destino) {
        val vista = mapa ?: return@LaunchedEffect
        val destino = destino ?: return@LaunchedEffect
        estiloListo = false
        val estilo = mapaLocal?.let { Style.Builder().fromJson(GestorMapaCampo.estilo(context, it, mapaDetalle)) }
            ?: Style.Builder().fromUri("asset://mapa_emergencia.json")
        vista.setStyle(estilo) { actual ->
            vista.clear()
            vista.addMarker(MarkerOptions().position(destino).title("Vivienda familiar")
                .icon(IconFactory.getInstance(context).fromBitmap(crearIconoDestinoRuta())))
            marcadorGps = null
            actual.addSource(GeoJsonSource("croquis-seguimiento"))
            actual.addLayer(LineLayer("linea-seguimiento", "croquis-seguimiento")
                .withProperties(lineColor("#087BEE"), lineWidth(5f)))
            vista.cameraPosition = CameraPosition.Builder().target(gps ?: destino).zoom(15.5).build()
            estiloListo = true
        }
    }
    LaunchedEffect(mapa, estiloListo, gps, siguiendoGps) {
        val vista = mapa ?: return@LaunchedEffect
        val actualGps = gps ?: return@LaunchedEffect
        if (!estiloListo) return@LaunchedEffect
        if (siguiendoGps && !mapaEnUso) {
            vista.animateCamera(CameraUpdateFactory.newLatLng(actualGps), 850)
        }
        val marcador = marcadorGps
        if (marcador == null) {
            marcadorGps = vista.addMarker(MarkerOptions().position(actualGps).title("Mi ubicación actual")
                .icon(IconFactory.getInstance(context).fromBitmap(crearIconoInicioRuta())))
        } else {
            val anterior = marcador.position
            val inicio = withFrameNanos { it }
            while (true) {
                val instante = withFrameNanos { it }
                val fraccion = ((instante - inicio) / 750_000_000.0).coerceIn(0.0, 1.0)
                marcador.position = LatLng(
                    anterior.latitude + (actualGps.latitude - anterior.latitude) * fraccion,
                    anterior.longitude + (actualGps.longitude - anterior.longitude) * fraccion
                )
                if (fraccion >= 1.0) break
            }
        }
    }
    LaunchedEffect(mapa, estiloListo, ruta, destino, gps, avance) {
        if (!estiloListo) return@LaunchedEffect
        val fuente = mapa?.style?.getSourceAs<GeoJsonSource>("croquis-seguimiento") ?: return@LaunchedEffect
        fuente.setGeoJson(avance?.geoJson ?: """{"type":"FeatureCollection","features":[]}""")
    }
    DisposableEffect(lifecycle, mapView) {
        var iniciado = false
        var reanudado = false
        fun sincronizar() {
            val estado = lifecycle.currentState
            if (estado.isAtLeast(Lifecycle.State.STARTED) && !iniciado) {
                mapView.onStart(); iniciado = true
            }
            if (estado.isAtLeast(Lifecycle.State.RESUMED) && !reanudado) {
                mapView.onResume(); reanudado = true
            }
            if (!estado.isAtLeast(Lifecycle.State.RESUMED) && reanudado) {
                mapView.onPause(); reanudado = false
            }
            if (!estado.isAtLeast(Lifecycle.State.STARTED) && iniciado) {
                mapView.onStop(); iniciado = false
            }
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

    val actual = ficha
    Column(Modifier.fillMaxSize().formularioSeguro()
        .verticalScroll(rememberScrollState(), enabled = !mapaEnUso)
        .background(Color(0xFFF6F9FB))) {
        CabeceraAgenda(onRegresar)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("SEGUIMIENTO EXTRAMURAL", color = Color(0xFF1565C0),
                fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text("Ruta de seguimiento", color = Color(0xFF0A2A5E),
                fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
            Text(actual?.let { "${it.nombreApellidoJefeFamilia} · ${it.barrio}" }
                ?: "Cargando ficha…", color = Color(0xFF5B7083), fontSize = 16.sp)

            if (actual?.latitud != null && actual.longitud != null) {
                SeccionFormularioRuralitos(
                    titulo = "1. Recorre la ruta",
                    descripcion = "El punto azul sigue tu GPS. La ruta se actualiza si cambias de camino."
                ) {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val alturaMapa = if (maxWidth >= 700.dp) 600.dp else 520.dp
                        Card(Modifier.fillMaxWidth().height(alturaMapa),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                            Box(Modifier.fillMaxSize()) {
                                AndroidView(factory = {
                                    mapView.apply {
                                        setOnTouchListener { vista, evento ->
                                            when (evento.actionMasked) {
                                                MotionEvent.ACTION_DOWN -> {
                                                    mapaEnUso = true
                                                    vista.parent?.requestDisallowInterceptTouchEvent(true)
                                                }
                                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                                    mapaEnUso = false
                                                    siguiendoGps = false
                                                    vista.parent?.requestDisallowInterceptTouchEvent(false)
                                                }
                                            }
                                            false
                                        }
                                    }
                                }, modifier = Modifier.fillMaxSize())
                                Surface(modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                                    shape = RoundedCornerShape(16.dp), color = Color.White,
                                    shadowElevation = 1.dp) {
                                    Text("GPS en vivo · Solo consulta", Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        color = AzulClinico, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Column(Modifier.align(Alignment.CenterEnd).padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(onClick = {
                                        siguiendoGps = true
                                        gps?.let { mapa?.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 16.0)) }
                                    }, shape = RoundedCornerShape(10.dp),
                                        color = Color(0xEEFFFFFF), shadowElevation = 1.dp) {
                                        Text("◎", Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = AzulClinico)
                                    }
                                    listOf("+" to true, "−" to false).forEach { (etiqueta, acercar) ->
                                        Surface(onClick = {
                                            mapa?.animateCamera(if (acercar) CameraUpdateFactory.zoomIn()
                                                else CameraUpdateFactory.zoomOut())
                                        }, shape = RoundedCornerShape(10.dp),
                                            color = Color(0xEEFFFFFF), shadowElevation = 1.dp) {
                                            Text(etiqueta, Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.SemiBold, color = AzulClinico)
                                        }
                                    }
                                }
                                Text("© OpenStreetMap contributors · Protomaps",
                                    Modifier.align(Alignment.BottomStart)
                                        .background(Color(0xEFFFFFFF)).padding(4.dp),
                                    color = AzulClinico, fontSize = 10.sp)
                            }
                        }
                    }
                }
                Text(when {
                    !permisoUbicacion -> "Permite la ubicación para navegar desde donde estás."
                    gps == null -> "Buscando tu ubicación actual…"
                    avance != null -> {
                        val distancia = (avance?.restanteMetros ?: 0.0) / 1000.0
                        val precision = ubicacionActual?.accuracy?.toInt() ?: 0
                        "${"%.1f".format(Locale.US, distancia)} km restantes · GPS ±$precision m" +
                            if (calculandoRuta) " · Actualizando ruta…" else ""
                    }
                    else -> mensaje
                }, color = if (!permisoUbicacion || mensaje.startsWith("No")) NaranjaClinico
                    else Color(0xFF5B7083), fontSize = 12.sp)
                if (!permisoUbicacion) {
                    BotonSecundarioRuralitos(texto = "Permitir ubicación", onClick = {
                        solicitarPermiso.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    })
                }
                SeccionFormularioRuralitos(
                    titulo = "2. Datos de la visita",
                    descripcion = "La ubicación de la vivienda guardada en la ficha."
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        DatoCoordenada("Latitud", "%.7f".format(Locale.US, actual.latitud),
                            AzulClinico, Modifier.weight(1f))
                        DatoCoordenada("Longitud", "%.7f".format(Locale.US, actual.longitud),
                            CianRuralitos, Modifier.weight(1f))
                        DatoCoordenada("Altitud", actual.altitud?.let { "%.0f m".format(Locale.US, it) }
                            ?: "Sin dato", NaranjaClinico, Modifier.weight(1f))
                    }
                    Text(gps?.let {
                        "Inicio · Mi ubicación: ${"%.6f".format(Locale.US, it.latitude)}, ${"%.6f".format(Locale.US, it.longitude)}"
                    } ?: "Inicio · Esperando GPS", Modifier.padding(top = 12.dp),
                        color = Color(0xFF5B7083), fontSize = 12.sp)
                }
                SeccionFormularioRuralitos(
                    titulo = "3. Indicaciones",
                    descripcion = "Cambia el modo de viaje solo para consultar el recorrido."
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("auto" to "Auto", "pedestrian" to "A pie", "bicycle" to "Bici")
                            .forEach { (valor, etiqueta) ->
                                Surface(onClick = { modo = valor },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (modo == valor) Color(0xFF1565C0) else Color.White,
                                    shadowElevation = 1.dp) {
                                    Text(etiqueta, Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                        color = if (modo == valor) Color.White else AzulClinico,
                                        fontWeight = FontWeight.SemiBold)
                                }
                            }
                    }
                    ruta?.instrucciones?.take(8)?.forEachIndexed { indice, instruccion ->
                        Text("${indice + 1}. $instruccion", Modifier.padding(top = 9.dp),
                            color = Color(0xFF0A2A5E))
                    }
                    Text("Comprueba el estado real de los caminos antes de viajar.",
                        Modifier.padding(top = 10.dp), color = Color(0xFF5B7083), fontSize = 12.sp)
                }
                BotonPrincipalRuralitos(texto = "Abrir navegación externa", color = CianRuralitos,
                    onClick = {
                        val coordenadas = "${actual.latitud},${actual.longitud}"
                        val navegacion = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$coordenadas"))
                            .setPackage("com.google.android.apps.maps")
                        val ubicacion = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$coordenadas"))
                        runCatching { context.startActivity(navegacion) }
                            .recoverCatching { context.startActivity(ubicacion) }
                            .onFailure { mensaje = "No hay una aplicación de navegación disponible en este dispositivo." }
                    })
            } else {
                Text("Esta ficha todavía no tiene coordenadas. Abre la ficha para guardar la vivienda en el mapa.",
                    color = Color(0xFF5B7083))
            }
            BotonSecundarioRuralitos(texto = "Abrir ficha familiar", onClick = onAbrirFicha,
                modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

private data class AvanceRuta(
    val geoJson: String,
    val desvioMetros: Double,
    val restanteMetros: Double
)

/** Proyecta el GPS sobre el tramo más cercano y oculta el recorrido ya transitado. */
private fun recortarRuta(actual: LatLng, ruta: List<LatLng>): AvanceRuta? {
    if (ruta.size < 2) return null
    val metrosLatitud = 111_195.0
    val metrosLongitud = metrosLatitud * cos(actual.latitude * PI / 180.0)
    var menorDistancia = Double.POSITIVE_INFINITY
    var mejorTramo = 0
    var mejorProyeccion = ruta.first()
    for (indice in 0 until ruta.lastIndex) {
        val a = ruta[indice]
        val b = ruta[indice + 1]
        val ax = (a.longitude - actual.longitude) * metrosLongitud
        val ay = (a.latitude - actual.latitude) * metrosLatitud
        val bx = (b.longitude - actual.longitude) * metrosLongitud
        val by = (b.latitude - actual.latitude) * metrosLatitud
        val dx = bx - ax
        val dy = by - ay
        val longitudCuadrada = dx * dx + dy * dy
        val fraccion = if (longitudCuadrada == 0.0) 0.0 else
            (-(ax * dx + ay * dy) / longitudCuadrada).coerceIn(0.0, 1.0)
        val px = ax + fraccion * dx
        val py = ay + fraccion * dy
        val distancia = sqrt(px * px + py * py)
        if (distancia < menorDistancia) {
            menorDistancia = distancia
            mejorTramo = indice
            mejorProyeccion = LatLng(
                a.latitude + (b.latitude - a.latitude) * fraccion,
                a.longitude + (b.longitude - a.longitude) * fraccion
            )
        }
    }
    val restantes = buildList {
        add(actual)
        add(mejorProyeccion)
        addAll(ruta.drop(mejorTramo + 1))
    }
    var distanciaRestante = 0.0
    for (indice in 0 until restantes.lastIndex) {
        val a = restantes[indice]
        val b = restantes[indice + 1]
        val lat = (b.latitude - a.latitude) * metrosLatitud
        val lon = (b.longitude - a.longitude) * metrosLongitud
        distanciaRestante += sqrt(lat * lat + lon * lon)
    }
    val coordenadas = JSONArray()
    restantes.forEach { coordenadas.put(JSONArray().put(it.longitude).put(it.latitude)) }
    val geometria = JSONObject().put("type", "LineString").put("coordinates", coordenadas)
    val geoJson = JSONObject().put("type", "Feature")
        .put("geometry", geometria).put("properties", JSONObject()).toString()
    return AvanceRuta(geoJson, menorDistancia, distanciaRestante)
}
