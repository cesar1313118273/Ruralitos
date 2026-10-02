package com.ruralitos.app.ui.screens

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.view.MotionEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.ruralitos.app.data.location.GestorUbicacionActual
import com.ruralitos.app.R
import com.ruralitos.app.data.location.GestorAltitudTerreno
import com.ruralitos.app.data.location.GestorTerrenoOffline
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.AdjuntoFichaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.mapa.GestorMapaCampo
import com.ruralitos.app.data.mapa.GestorMapaDetalle
import com.ruralitos.app.domain.DispensarizacionAutomatica
import com.ruralitos.app.domain.IconosMais
import com.ruralitos.app.domain.PictogramaDispensarizacion
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.IconoMais
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.offline.OfflineManager
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.roundToInt

@SuppressLint("ClickableViewAccessibility")
@Composable
fun CroquisMapaScreen(
    fichaId: Long,
    usuarioId: Long?,
    modoEdicion: Boolean = false,
    fichaInicial: FichaFamiliarEntity? = null,
    onContinuar: () -> Unit,
    onRegresar: () -> Unit,
    textoRegresar: String = "Volver al panel de la ficha",
    descripcionRegresar: String = "Salir de esta sección sin cambiar las coordenadas"
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val ficha by database.fichaFamiliarDao().observarPorId(fichaId).collectAsState(initial = null)
    val fichaDisponible = if (modoEdicion) ficha ?: fichaInicial?.takeIf { it.id == fichaId } else ficha
    val adjuntos by database.fichaContenidoDao()
        .listarAdjuntos(fichaId)
        .collectAsState(initial = emptyList())
    val miembros by database.fichaContenidoDao()
        .listarMiembros(fichaId)
        .collectAsState(initial = emptyList())
    val embarazadas by database.fichaContenidoDao()
        .listarEmbarazadas(fichaId)
        .collectAsState(initial = emptyList())
    val calificaciones by database.fichaContenidoDao()
        .listarCalificaciones(fichaId)
        .collectAsState(initial = emptyList())
    val valoresRiesgo by database.fichaContenidoDao()
        .listarTodosValoresRiesgo()
        .collectAsState(initial = emptyList())
    val pictogramasMais = remember(miembros, embarazadas, calificaciones, valoresRiesgo) {
        val individuales = miembros.flatMap { miembro ->
            val embarazo = DispensarizacionAutomatica.buscarEmbarazo(miembro, embarazadas)
            DispensarizacionAutomatica.clasificar(miembro, embarazo).pictogramas
        }
        val evaluacionActual = calificaciones.maxByOrNull { it.id }
        val familiares = evaluacionActual?.let { evaluacion ->
            DispensarizacionAutomatica.pictogramasRiesgoFamiliar(
                valoresRiesgo.filter { it.calificacionId == evaluacion.id }
            )
        }.orEmpty()
        combinarPictogramasMais(individuales, familiares)
    }
    // Versiones anteriores usaban este punto de ejemplo; nunca debe pasar por una vivienda real.
    val fichaLatitud = fichaDisponible?.latitud
    val fichaLongitud = fichaDisponible?.longitud
    val ubicacionGuardadaValida = fichaLatitud != null && fichaLongitud != null &&
        !(kotlin.math.abs(fichaLatitud - (-1.8312)) < 0.000001 &&
            kotlin.math.abs(fichaLongitud - (-78.1834)) < 0.000001)
    var latitud by remember(fichaDisponible?.id) { mutableStateOf(if (ubicacionGuardadaValida) fichaLatitud!! else -1.8312) }
    var longitud by remember(fichaDisponible?.id) { mutableStateOf(if (ubicacionGuardadaValida) fichaLongitud!! else -78.1834) }
    var altitud by remember(fichaDisponible?.id) { mutableStateOf(if (ubicacionGuardadaValida) fichaDisponible?.altitud else null) }
    var ubicacionElegida by remember(fichaDisponible?.id) { mutableStateOf(ubicacionGuardadaValida) }
    val fueraDelMapaNacional = ubicacionElegida &&
        (latitud < -5.02 || latitud > 1.69 || longitud < -92.02 || longitud > -75.18)
    var mapaEnUso by remember { mutableStateOf(false) }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var marcador by remember { mutableStateOf<Marker?>(null) }
    var estiloMapaListo by remember { mutableStateOf(false) }
    var arrastrandoMarcador by remember { mutableStateOf(false) }
    var ubicacionAutomaticaIntentada by remember(fichaDisponible?.id) { mutableStateOf(false) }
    var ubicacionGpsConfirmada by remember(fichaDisponible?.id) { mutableStateOf(false) }
    var ubicacionModificadaManual by remember(fichaDisponible?.id) { mutableStateOf(false) }
    var gpsAutomaticoPendiente by remember(fichaId) { mutableStateOf(false) }
    var precisionGpsMetros by remember { mutableStateOf<Float?>(null) }
    var origenGps by remember { mutableStateOf("GPS") }
    var estadoOffline by remember { mutableStateOf("Mapa de calles de Ecuador disponible sin internet.") }
    var mensaje by remember {
        mutableStateOf("Arrastra el señalizador rojo o toca el mapa para ubicar la vivienda.")
    }
    LaunchedEffect(mensaje) {
        if (mensaje == "Captura guardada") {
            delay(2500)
            mensaje = ""
        }
    }
    var procesando by remember { mutableStateOf(false) }
    var capturandoMapa by remember { mutableStateOf(false) }
    var capturaVersion by remember { mutableIntStateOf(0) }
    var capturaAnterior by remember { mutableStateOf<Bitmap?>(null) }
    var altitudActualizando by remember { mutableStateOf(false) }
    var estadoTerreno by remember { mutableStateOf("La elevación se consulta al mover la ubicación; sin internet puede ser aproximada.") }
    var estiloDeRespaldo by remember { mutableStateOf(false) }
    var mapaLocalPreparado by remember { mutableStateOf(false) }
    var rutaMapaLocal by remember { mutableStateOf<String?>(null) }
    var rutaMapaDetalle by remember { mutableStateOf<String?>(null) }
    var internetDisponible by remember { mutableStateOf(tieneInternet(context)) }
    // La cartografía vectorial local es la única vista, con o sin conexión.
    LaunchedEffect(adjuntos, capturaVersion) {
        val ruta = adjuntos.firstOrNull { it.tipo == "CROQUIS" }?.uri?.let { Uri.parse(it).path }
        capturaAnterior = withContext(Dispatchers.IO) {
            ruta?.let { BitmapFactory.decodeFile(it, BitmapFactory.Options().apply { inSampleSize = 4 }) }
        }
    }

    fun agregarMarcadorUnico(mapLibre: MapLibreMap): Marker? {
        marcador?.let(mapLibre::removeMarker)
        if (!ubicacionElegida) {
            marcador = null
            return null
        }
        return mapLibre.addMarker(
            MarkerOptions()
                .position(LatLng(latitud, longitud))
                .title(
                    if (pictogramasMais.isEmpty()) {
                        "Vivienda familiar"
                    } else {
                        "Vivienda familiar · ${pictogramasMais.joinToString { it.etiqueta }}"
                    }
                )
                .icon(IconFactory.getInstance(context).fromBitmap(crearIconoDestinoRuta()))
        ).also { marcador = it }
    }

    fun actualizarUbicacionManual(punto: LatLng) {
        ubicacionModificadaManual = true
        ubicacionGpsConfirmada = false
        ubicacionElegida = true
        precisionGpsMetros = null
        latitud = punto.latitude
        longitud = punto.longitude
        altitud = GestorTerrenoOffline.alturaLocal(latitud, longitud)
            ?: GestorAltitudTerreno.estimarCercana(latitud, longitud)
        altitudActualizando = true
        mapa?.let { mapLibre ->
            val actual = marcador
            if (actual == null && estiloMapaListo) agregarMarcadorUnico(mapLibre)
            else actual?.let {
                it.position = punto
                mapLibre.updateMarker(it)
            }
        }
    }

    val localizar: (Boolean) -> Unit = { automatico ->
        procesando = true
        mensaje = "Buscando una ubicación precisa; esto puede tardar unos segundos…"
        scope.launch {
            val actual = GestorUbicacionActual.obtener(context)
            val guardadaDuranteGps = if (automatico && modoEdicion) withContext(Dispatchers.IO) {
                database.fichaFamiliarDao().buscarPorId(fichaId)
            } else null
            val latitudGuardada = guardadaDuranteGps?.latitud
            val longitudGuardada = guardadaDuranteGps?.longitud
            val guardadaValida = latitudGuardada != null && longitudGuardada != null &&
                !(kotlin.math.abs(latitudGuardada - (-1.8312)) < 0.000001 &&
                    kotlin.math.abs(longitudGuardada - (-78.1834)) < 0.000001)
            if (guardadaValida) {
                latitud = latitudGuardada!!
                longitud = longitudGuardada!!
                altitud = guardadaDuranteGps?.altitud
                ubicacionElegida = true
                ubicacionGpsConfirmada = false
                mensaje = "Se conservó la ubicación guardada de esta vivienda."
            } else if (actual == null) {
                mensaje = if (GestorUbicacionActual.tienePermisoPreciso(context))
                    "No se obtuvo una ubicación suficientemente precisa. Sal al exterior, activa el GPS y vuelve a intentarlo; también puedes marcar la vivienda manualmente."
                else "Activa la ubicación precisa para Ruralitos en los ajustes del teléfono, o marca la vivienda manualmente."
            } else if (!ubicacionModificadaManual || !automatico) {
                precisionGpsMetros = actual.accuracy
                origenGps = if (actual.provider == android.location.LocationManager.GPS_PROVIDER) "GPS" else "Red"
                ubicacionGpsConfirmada = true
                ubicacionElegida = true
                latitud = actual.latitude
                longitud = actual.longitude
                altitud = actual.altitude.takeIf { actual.hasAltitude() }
                mensaje = if (actual.hasAccuracy()) {
                    "$origenGps: precisión estimada de ${actual.accuracy.roundToInt()} m. " +
                        "Comprueba la vivienda y ajusta el señalizador si hace falta."
                } else {
                    "Ubicación automática obtenida. Puedes arrastrar el señalizador para ajustarla."
                }
            }
            procesando = false
        }
    }
    val permisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { it }) {
            localizar(gpsAutomaticoPendiente)
        } else {
            mensaje = "Concede permiso de ubicación para localizar la vivienda."
        }
    }

    LaunchedEffect(fichaDisponible?.id) {
        val fichaActual = fichaDisponible ?: return@LaunchedEffect
        if (ubicacionAutomaticaIntentada) return@LaunchedEffect
        ubicacionAutomaticaIntentada = true
        val latitudGuardada = fichaActual.latitud
        val longitudGuardada = fichaActual.longitud
        if (ubicacionGuardadaValida && latitudGuardada != null && longitudGuardada != null) {
            GestorTerrenoOffline.precargarLocal(context, latitudGuardada, longitudGuardada)
            mensaje = "Se cargó la ubicación guardada. Arrastra el señalizador si necesitas corregirla."
        } else if (GestorUbicacionActual.tienePermiso(context)) {
            localizar(true)
        } else {
            gpsAutomaticoPendiente = true
            permisos.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Solo al editar: usar la vivienda guardada incluso si el mapa termina de cargar
    // después de la ficha. Un toque manual o el botón GPS tienen prioridad posterior.
    LaunchedEffect(modoEdicion, fichaDisponible?.id, fichaDisponible?.latitud,
        fichaDisponible?.longitud, mapa, estiloMapaListo) {
        if (!modoEdicion || !ubicacionGuardadaValida || ubicacionModificadaManual ||
            ubicacionGpsConfirmada) return@LaunchedEffect
        val guardada = fichaDisponible ?: return@LaunchedEffect
        val lat = guardada.latitud ?: return@LaunchedEffect
        val lon = guardada.longitud ?: return@LaunchedEffect
        latitud = lat
        longitud = lon
        altitud = guardada.altitud
        ubicacionElegida = true
        val vista = mapa
        if (estiloMapaListo && vista != null) {
            if (marcador == null) agregarMarcadorUnico(vista)
            else marcador?.let { actual ->
                actual.position = LatLng(lat, lon)
                vista.updateMarker(actual)
            }
            vista.cameraPosition = CameraPosition.Builder()
                .target(LatLng(lat, lon)).zoom(15.0).build()
        }
    }

    // MapView conserva sus listeners entre recomposiciones; estos estados les entregan
    // siempre las funciones y coordenadas vigentes de la ficha actual.
    val moverViviendaActual = rememberUpdatedState<(LatLng) -> Unit> { punto ->
        actualizarUbicacionManual(punto)
    }

    MapLibre.getInstance(context)
    val mapView = remember(fichaId) {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { mapLibre ->
                mapa = mapLibre
                mapLibre.uiSettings.isCompassEnabled = true
                mapLibre.uiSettings.isAttributionEnabled = true
                addOnDidFailLoadingMapListener { _ ->
                    if (mapaLocalPreparado && !estiloMapaListo && !estiloDeRespaldo) {
                        estiloDeRespaldo = true
                        estadoOffline = "No se pudo cargar el mapa vectorial. El GPS y las coordenadas siguen disponibles."
                        mapLibre.setStyle(Style.Builder().fromUri(ESTILO_MAPA_EMERGENCIA)) {
                            agregarMarcadorUnico(mapLibre)
                            estiloMapaListo = true
                            mapLibre.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(latitud, longitud)).zoom(16.0).build()
                        }
                    }
                }
                mapLibre.addOnMapClickListener { punto ->
                    moverViviendaActual.value(punto)
                    mensaje = "Vivienda movida; sus coordenadas se actualizaron."
                    true
                }
            }
        }
    }

    fun capturarMapa() {
        val mapLibre = mapa ?: return
        if (procesando || !ubicacionElegida) return
        procesando = true
        capturandoMapa = true
        val puntoEnPantalla = mapLibre.projection.toScreenLocation(LatLng(latitud, longitud))
        marcador?.let(mapLibre::removeMarker)
        marcador = null
        mapView.postDelayed({
            mapLibre.snapshot { captura ->
                agregarMarcadorUnico(mapLibre)
                capturandoMapa = false
                scope.launch {
                    runCatching {
                        val uriCroquis = withContext(Dispatchers.IO) {
                            val bitmap = agregarMarcadorUnicoYAtribucion(
                                captura, puntoEnPantalla,
                                mapView.width, mapView.height
                            )
                            val directorio = File(context.filesDir, "croquis_mapas").apply { mkdirs() }
                            val archivo = File(directorio, "croquis_$fichaId.png")
                            FileOutputStream(archivo).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                            Uri.fromFile(archivo).toString()
                        }
                        withContext(Dispatchers.IO) {
                            val actual = adjuntos.firstOrNull { it.tipo == "CROQUIS" }
                            if (actual == null) {
                                database.fichaContenidoDao().guardarAdjunto(
                                    AdjuntoFichaEntity(fichaId = fichaId, tipo = "CROQUIS", uri = uriCroquis)
                                )
                            } else {
                                database.fichaContenidoDao().actualizarUriAdjunto(actual.id, uriCroquis)
                            }
                        }
                    }.onSuccess { mensaje = "Captura guardada"; capturaVersion++ }
                        .onFailure { mensaje = "No se pudo guardar la captura del mapa." }
                    procesando = false
                }
            }
        }, 100L)
    }

    LaunchedEffect(Unit) {
        rutaMapaLocal = GestorMapaCampo.preparar(context)
        rutaMapaDetalle = GestorMapaDetalle.preparar(context)
        mapaLocalPreparado = true
    }

    LaunchedEffect(mapa, mapaLocalPreparado, rutaMapaLocal, rutaMapaDetalle) {
        val mapLibre = mapa ?: return@LaunchedEffect
        if (!mapaLocalPreparado) return@LaunchedEffect
        val ruta = rutaMapaLocal
        if (ruta == null) {
            estiloDeRespaldo = true
            estadoOffline = "No se pudo preparar el mapa local. Comprueba el espacio disponible; GPS y coordenadas siguen funcionando."
        }
        val estilo = if (ruta == null) {
            Style.Builder().fromUri(ESTILO_MAPA_EMERGENCIA)
        } else {
            Style.Builder().fromJson(GestorMapaCampo.estilo(context, ruta, rutaMapaDetalle))
        }
        mapLibre.setStyle(estilo) {
            val punto = LatLng(latitud, longitud)
            agregarMarcadorUnico(mapLibre)
            estiloMapaListo = true
            mapLibre.cameraPosition = CameraPosition.Builder()
                .target(punto)
                .zoom(if (ubicacionElegida) 15.0 else 7.0)
                .build()
        }
    }

    DisposableEffect(lifecycle, mapView) {
        var started = false
        var resumed = false
        fun syncMapLifecycle() {
            val state = lifecycle.currentState
            if (state.isAtLeast(Lifecycle.State.STARTED) && !started) {
                mapView.onStart()
                started = true
            }
            if (state.isAtLeast(Lifecycle.State.RESUMED) && !resumed) {
                mapView.onResume()
                resumed = true
            }
            if (!state.isAtLeast(Lifecycle.State.RESUMED) && resumed) {
                mapView.onPause()
                resumed = false
            }
            if (!state.isAtLeast(Lifecycle.State.STARTED) && started) {
                mapView.onStop()
                started = false
            }
        }
        val observer = LifecycleEventObserver { _, _ -> syncMapLifecycle() }
        lifecycle.addObserver(observer)
        // Compose puede crear MapView después de ON_START y ON_RESUME.
        syncMapLifecycle()
        onDispose {
            lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(latitud, longitud, ubicacionElegida, mapa) {
        val mapLibre = mapa ?: return@LaunchedEffect
        if (!ubicacionElegida) return@LaunchedEffect
        val punto = LatLng(latitud, longitud)
        marcador?.let {
            it.position = punto
            mapLibre.updateMarker(it)
        }
        if (!arrastrandoMarcador && !ubicacionModificadaManual &&
            ((ficha?.latitud != null && ficha?.longitud != null) || ubicacionGpsConfirmada)) {
            val objetivo = if (ubicacionGpsConfirmada || mapLibre.cameraPosition.zoom < 14.0)
                CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(punto).zoom(15.0).build())
            else CameraUpdateFactory.newLatLng(punto)
            mapLibre.animateCamera(objetivo)
        }
    }

    LaunchedEffect(latitud, longitud, ubicacionElegida, arrastrandoMarcador) {
        if (!ubicacionElegida) return@LaunchedEffect
        val alturaGuardada = fichaDisponible?.takeIf {
            it.latitud == latitud && it.longitud == longitud
        }?.altitud
        if (modoEdicion && !ubicacionModificadaManual && !ubicacionGpsConfirmada &&
            alturaGuardada != null) {
            altitud = alturaGuardada
            altitudActualizando = false
            return@LaunchedEffect
        }
        val estimada = GestorTerrenoOffline.alturaLocal(latitud, longitud)
            ?: GestorAltitudTerreno.estimarCercana(latitud, longitud)
        altitud = estimada ?: alturaGuardada
        if (arrastrandoMarcador) {
            altitudActualizando = false
            return@LaunchedEffect
        }
        altitudActualizando = true
        delay(140)
        val latitudConsultada = latitud
        val longitudConsultada = longitud
        val elevacion = GestorAltitudTerreno.obtener(context, latitudConsultada, longitudConsultada)
        if (latitud == latitudConsultada && longitud == longitudConsultada) {
            altitud = elevacion ?: estimada ?: alturaGuardada
            altitudActualizando = false
        }
    }

    LaunchedEffect(Unit) { GestorAltitudTerreno.precargar(context) }
    DisposableEffect(context) {
        val gestor = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val observador = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                scope.launch {
                    internetDisponible = tieneInternet(context)
                }
            }

            override fun onLost(network: android.net.Network) {
                scope.launch {
                    internetDisponible = tieneInternet(context)
                }
            }

            override fun onCapabilitiesChanged(
                network: android.net.Network,
                networkCapabilities: NetworkCapabilities
            ) {
                scope.launch {
                    internetDisponible = tieneInternet(context)
                }
            }
        }
        gestor.registerDefaultNetworkCallback(observador)
        internetDisponible = tieneInternet(context)
        onDispose { gestor.unregisterNetworkCallback(observador) }
    }

    LaunchedEffect(pictogramasMais, ubicacionElegida, mapa, estiloMapaListo, capturandoMapa) {
        val mapLibre = mapa ?: return@LaunchedEffect
        if (!estiloMapaListo || capturandoMapa) return@LaunchedEffect
        agregarMarcadorUnico(mapLibre)
    }

    LaunchedEffect(Unit) {
        runCatching {
            OfflineManager.getInstance(context).setMaximumAmbientCacheSize(
                500L * 1024L * 1024L,
                object : OfflineManager.FileSourceCallback {
                    override fun onSuccess() = Unit
                    override fun onError(message: String) = Unit
                }
            )
        }
    }

    PantallaRuralitos(
        titulo = "Ubicación de vivienda",
        subtitulo = "Ubicación exacta",
        descripcion = "Localiza la vivienda en el mapa.",
        onVolver = onRegresar,
        scrollHabilitado = !mapaEnUso,
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = "Guardar información de esta sección",
                descripcion = "Guarda la posición actual aunque no tomes una captura",
                color = NaranjaClinico,
                enabled = !procesando && ubicacionElegida,
                onClick = {
                    procesando = true
                    val latitudGuardar = latitud
                    val longitudGuardar = longitud
                    val altitudGuardar = altitud
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                database.fichaFamiliarDao().actualizarCoordenadas(
                                    fichaId, latitudGuardar, longitudGuardar, altitudGuardar, usuarioId
                                )
                            }
                        }.onSuccess {
                            onContinuar()
                        }.onFailure {
                            mensaje = "No se pudieron guardar las coordenadas. Inténtalo de nuevo."
                            procesando = false
                        }
                    }
                }
            )
        }
    ) {
        if (estiloDeRespaldo) Text(estadoOffline, color = NaranjaClinico, fontSize = 12.sp)

        SeccionFormularioRuralitos(
            titulo = "1. Ubica la vivienda",
            descripcion = "Arrastra el punto rojo o toca el mapa para ubicar la vivienda."
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val alturaMapa = if (maxWidth >= 700.dp) 600.dp else 520.dp
                Card(
                    modifier = Modifier.fillMaxWidth().height(alturaMapa),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = {
                                mapView.apply {
                                setOnTouchListener { vista, evento ->
                                    val mapLibre = mapa
                                    val marcadorActual = marcador
                                    fun cerca(actual: Marker?): Boolean = mapLibre != null && actual != null &&
                                        mapLibre.projection.toScreenLocation(actual.position).let { posicion ->
                                            hypot(evento.x - posicion.x, evento.y - posicion.y) <=
                                                52f * resources.displayMetrics.density
                                        }
                                    val viviendaCerca = evento.actionMasked == MotionEvent.ACTION_DOWN && cerca(marcadorActual)
                                    if (viviendaCerca) {
                                        arrastrandoMarcador = true
                                        mapaEnUso = true
                                        mapLibre!!.uiSettings.setAllGesturesEnabled(false)
                                        vista.parent?.requestDisallowInterceptTouchEvent(true)
                                        return@setOnTouchListener true
                                    }

                                    if (arrastrandoMarcador && mapLibre != null) {
                                        when (evento.actionMasked) {
                                            MotionEvent.ACTION_MOVE -> {
                                                val punto = mapLibre.projection.fromScreenLocation(
                                                    PointF(evento.x, evento.y)
                                                )
                                                moverViviendaActual.value(punto)
                                            }
                                            MotionEvent.ACTION_UP,
                                            MotionEvent.ACTION_CANCEL -> {
                                                if (evento.actionMasked == MotionEvent.ACTION_UP) {
                                                    val punto = mapLibre.projection.fromScreenLocation(
                                                        PointF(evento.x, evento.y)
                                                    )
                                                    moverViviendaActual.value(punto)
                                                }
                                                arrastrandoMarcador = false
                                                mapaEnUso = false
                                                mapLibre.uiSettings.setAllGesturesEnabled(true)
                                                vista.parent?.requestDisallowInterceptTouchEvent(false)
                                                mensaje = "Vivienda ajustada; sus coordenadas se actualizaron."
                                            }
                                        }
                                        return@setOnTouchListener true
                                    }

                                    val interactuando =
                                        evento.actionMasked != MotionEvent.ACTION_UP &&
                                            evento.actionMasked != MotionEvent.ACTION_CANCEL
                                    mapaEnUso = interactuando
                                    vista.parent?.requestDisallowInterceptTouchEvent(interactuando)
                                    false
                                }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        Column(
                            modifier = Modifier.align(Alignment.CenterEnd).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(onClick = {
                                ubicacionModificadaManual = false
                                gpsAutomaticoPendiente = false
                                if (GestorUbicacionActual.tienePermiso(context)) localizar(false)
                                else permisos.launch(arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            }, shape = CircleShape, color = Color.White, shadowElevation = 1.dp) {
                                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                    Text("⌖", color = Color(0xFF1565C0), fontSize = 28.sp)
                                }
                            }
                            Surface(onClick = { capturarMapa() }, shape = CircleShape,
                                color = Color.White, shadowElevation = 1.dp) {
                                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                    Text("📷", fontSize = 22.sp)
                                }
                            }
                            listOf("+" to true, "−" to false).forEach { (etiqueta, acercar) ->
                                Surface(
                                    modifier = Modifier.clickable {
                                        mapa?.animateCamera(
                                            if (acercar) CameraUpdateFactory.zoomIn()
                                            else CameraUpdateFactory.zoomOut()
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xEEFFFFFF),
                                    shadowElevation = 1.dp
                                ) {
                                    Text(
                                        etiqueta,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AzulClinico
                                    )
                                }
                            }
                        }
                        if (arrastrandoMarcador) {
                            Surface(
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xF5FFFFFF),
                                shadowElevation = 1.dp
                            ) {
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                                    Text("Vivienda",
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("${"%.6f".format(Locale.US, latitud)}, " +
                                        "${"%.6f".format(Locale.US, longitud)} · " +
                                        (altitud?.let { "%.0f m".format(Locale.US, it) } ?: "Altitud sin dato"),
                                        color = AzulClinico, fontSize = 12.sp)
                                }
                            }
                        }
                        Text("© OpenStreetMap contributors · Protomaps",
                            modifier = Modifier.align(Alignment.BottomStart)
                                .background(Color(0xEFFFFFFF)).padding(4.dp),
                            color = AzulClinico, fontSize = 10.sp)
                        if (fueraDelMapaNacional) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomCenter)
                                    .padding(start = 14.dp, end = 14.dp, bottom = 62.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xF9FFF4E7),
                                shadowElevation = 1.dp
                            ) {
                                Text(
                                    "Fuera de Ecuador: el mapa local no cubre esta ubicación. Usa GPS o corrige el punto.",
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF8A4B00)
                                )
                            }
                        } else if (!internetDisponible) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xEEFFFFFF)
                            ) {
                                Text(
                                    "Mapa local sin internet; los caminos visibles dependen de la cartografía de la zona.",
                                    modifier = Modifier.padding(9.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AzulClinico
                                )
                            }
                        }
                    }
                }
            }
        }

        if (mensaje == "Captura guardada") {
            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFE3F4F7)) {
                Text("✓  Captura guardada", Modifier.fillMaxWidth().padding(12.dp),
                    color = CianRuralitos, fontWeight = FontWeight.SemiBold)
            }
        } else if (mensaje.startsWith("No") || mensaje.startsWith("Concede") ||
            mensaje.startsWith("Activa") || mensaje.startsWith("Buscando")) {
            Text(mensaje, color = if (mensaje.startsWith("Buscando")) AzulClinico else RojoClinico,
                fontSize = 12.sp)
        }
        capturaAnterior?.let { captura ->
            Text("Captura guardada de esta vivienda", color = AzulClinico,
                fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Image(bitmap = captura.asImageBitmap(), contentDescription = "Captura previa del mapa",
                modifier = Modifier.fillMaxWidth().height(260.dp), contentScale = ContentScale.Fit)
        }

        SeccionFormularioRuralitos(
            titulo = "2. Coordenadas seleccionadas",
            descripcion = "La latitud y longitud cambian al mover el punto; la altitud cercana puede ser aproximada."
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 620.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        DatoCoordenada("Latitud", if (ubicacionElegida) "%.7f".format(Locale.US, latitud) else "Sin seleccionar", AzulClinico, Modifier.weight(1f))
                        DatoCoordenada("Longitud", if (ubicacionElegida) "%.7f".format(Locale.US, longitud) else "Sin seleccionar", CianRuralitos, Modifier.weight(1f))
                        DatoCoordenada("Altitud", altitud?.let { "%.0f m".format(Locale.US, it) } ?: "Sin dato",
                            NaranjaClinico, Modifier.weight(1f))
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DatoCoordenada(
                            "Latitud",
                            if (ubicacionElegida) "%.7f".format(Locale.US, latitud) else "Sin seleccionar",
                            AzulClinico,
                            Modifier.weight(1f)
                        )
                        DatoCoordenada(
                            "Longitud",
                            if (ubicacionElegida) "%.7f".format(Locale.US, longitud) else "Sin seleccionar",
                            CianRuralitos,
                            Modifier.weight(1f)
                        )
                        DatoCoordenada(
                            "Altitud",
                            altitud?.let { "%.1f m".format(Locale.US, it) } ?: "Sin dato",
                            NaranjaClinico,
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        Text("La altitud puede ser aproximada. ${estadoTerreno}",
            color = Color(0xFF5B7083), fontSize = 11.sp)
        precisionGpsMetros?.let {
            Text("Precisión de $origenGps: ±${it.roundToInt()} m. Ajusta el punto sobre la vivienda si es necesario.",
                color = AzulClinico, fontSize = 12.sp)
        }
        if (fueraDelMapaNacional) {
            Text("Esta ubicación está fuera de la cobertura del mapa descargado de Ecuador. Pulsa GPS para volver a tu posición o corrige el punto manualmente.",
                color = NaranjaClinico, fontSize = 12.sp)
        }
        if (!ubicacionElegida) {
            Text("Aún no hay una ubicación seleccionada. Usa el botón GPS o toca la vivienda en el mapa.",
                color = NaranjaClinico, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun DatoCoordenada(
    etiqueta: String,
    valor: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.10f)
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                valor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

private fun agregarMarcadorUnicoYAtribucion(
    origen: Bitmap,
    punto: PointF,
    anchoVista: Int,
    altoVista: Int
): Bitmap {
    val bitmap = origen.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(bitmap)
    val icono = crearIconoDestinoRuta()
    val escalaX = bitmap.width.toFloat() / anchoVista.coerceAtLeast(1)
    val escalaY = bitmap.height.toFloat() / altoVista.coerceAtLeast(1)
    val x = punto.x * escalaX
    val y = punto.y * escalaY
    canvas.drawBitmap(
        icono,
        null,
        RectF(
            x - icono.width * escalaX / 2f,
            y - icono.height * escalaY,
            x + icono.width * escalaX / 2f,
            y
        ),
        Paint(Paint.ANTI_ALIAS_FLAG)
    )
    val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = (bitmap.width * 0.022f).coerceAtLeast(13f)
        setShadowLayer(4f, 0f, 0f, AndroidColor.BLACK)
    }
    canvas.drawText(
        "© OpenStreetMap contributors · Protomaps",
        12f,
        bitmap.height - 12f,
        texto
    )
    return bitmap
}

internal fun crearIconoInicioRuta(): Bitmap = crearIconoRuta(AndroidColor.rgb(0, 108, 221))

internal fun crearIconoDestinoRuta(): Bitmap = crearIconoRuta(AndroidColor.rgb(220, 38, 38))

private fun crearIconoRuta(colorMarcadorValor: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(88, 112, Bitmap.Config.ARGB_8888)
    val lienzo = Canvas(bitmap)
    val colorMarcador = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorMarcadorValor
        setShadowLayer(5f, 0f, 3f, AndroidColor.argb(120, 0, 0, 0))
    }
    val punta = Path().apply {
        moveTo(24f, 55f); lineTo(64f, 55f); lineTo(44f, 109f); close()
    }
    lienzo.drawPath(punta, colorMarcador)
    lienzo.drawCircle(44f, 44f, 37f, colorMarcador)
    lienzo.drawCircle(44f, 44f, 17f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE })
    return bitmap
}

private fun combinarPictogramasMais(
    individuales: List<PictogramaDispensarizacion>,
    familiares: List<PictogramaDispensarizacion>
): List<PictogramaDispensarizacion> {
    val iconosEmbarazo = setOf(
        IconosMais.EMBARAZO_BAJO_RIESGO,
        IconosMais.EMBARAZO_ALTO_RIESGO,
        IconosMais.EMBARAZO_RIESGO
    )
    val embarazoFamiliar = familiares.firstOrNull { it.id in iconosEmbarazo }
    val individualesAjustados = if (embarazoFamiliar == null) {
        individuales
    } else {
        individuales.filterNot { it.id in iconosEmbarazo }
    }
    val familiaresAjustados = familiares.filterNot { familiar ->
        familiar.id in individualesAjustados.map { it.id }
    }
    val orden = IconosMais.iconosGrupoEdad + listOf(
        IconosMais.MENOR_DOS,
        IconosMais.VACUNACION_INCOMPLETA,
        IconosMais.EMBARAZO_BAJO_RIESGO,
        IconosMais.EMBARAZO_ALTO_RIESGO,
        IconosMais.EMBARAZO_RIESGO,
        IconosMais.OBESIDAD_MENOR_CINCO,
        IconosMais.OBESIDAD_CINCO_ONCE,
        IconosMais.OBESIDAD_ADOLESCENTE,
        IconosMais.OBESIDAD_ADULTO,
        IconosMais.OBESIDAD_ADULTO_MAYOR,
        IconosMais.DESNUTRICION_AGUDA,
        IconosMais.DESNUTRICION_CRONICA,
        IconosMais.HIPERTENSION,
        IconosMais.DIABETES,
        IconosMais.HIPERTENSION_DIABETES,
        IconosMais.TUBERCULOSIS,
        IconosMais.SALUD_MENTAL,
        IconosMais.CUIDADOS_PALIATIVOS,
        IconosMais.VIH,
        IconosMais.DISCAPACIDAD_VISUAL,
        IconosMais.DISCAPACIDAD_LENGUAJE,
        IconosMais.DISCAPACIDAD_AUDITIVA,
        IconosMais.DISCAPACIDAD_FISICA,
        IconosMais.DISCAPACIDAD_FISICA_APOYO,
        IconosMais.DISCAPACIDAD_INTELECTUAL,
        IconosMais.CONSUMO_ALCOHOL_DROGAS,
        IconosMais.SANEAMIENTO_AMBIENTAL
    )
    return (individualesAjustados + familiaresAjustados)
        .distinctBy { it.id }
        .sortedBy { pictograma ->
            orden.indexOf(pictograma.id).takeIf { it >= 0 } ?: Int.MAX_VALUE
        }
}

private const val ESTILO_MAPA_EMERGENCIA = "asset://mapa_emergencia.json"

private fun tieneInternet(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val red = manager.activeNetwork ?: return false
    return manager.getNetworkCapabilities(red)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
}
