package com.ruralitos.app

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.core.content.ContextCompat
import com.ruralitos.app.data.local.seed.EstablecimientosSaludSeeder
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.SalaEntity
import com.ruralitos.app.data.local.entity.EaisSalaEntity
import com.ruralitos.app.data.local.entity.TerritorioSalaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.ruralitos.app.ui.screens.BuscarUnidadOperativaScreen
import com.ruralitos.app.data.local.entity.EstablecimientoSaludEntity
import com.ruralitos.app.ui.screens.UbicacionFamiliaScreen
import com.ruralitos.app.ui.screens.MiembrosFamiliaScreen
import com.ruralitos.app.ui.screens.SaludFamiliarScreen
import com.ruralitos.app.ui.screens.RiesgoFamiliarScreen
import com.ruralitos.app.ui.screens.GestionRiesgoScreen
import com.ruralitos.app.ui.screens.ContaminacionAmbientalScreen
import com.ruralitos.app.ui.screens.CroquisMapaScreen
import com.ruralitos.app.ui.screens.FamiliogramaScreen
import com.ruralitos.app.ui.screens.LugaresTratamientoScreen
import com.ruralitos.app.ui.screens.BuscarFichasScreen
import com.ruralitos.app.ui.screens.FichaSeccionesScreen
import com.ruralitos.app.ui.screens.UbicacionFamiliaForm
import com.ruralitos.app.ui.screens.CargandoAccesoScreen
import com.ruralitos.app.ui.screens.AccesoConCargaMinima
import com.ruralitos.app.ui.screens.CrearAdministradorScreen
import com.ruralitos.app.ui.screens.GestionUsuariosScreen
import com.ruralitos.app.ui.screens.LoginClinicoScreen
import com.ruralitos.app.ui.screens.AccesoSupabaseScreen
import com.ruralitos.app.ui.screens.CambiarClaveCuentaScreen
import com.ruralitos.app.ui.screens.ConfigurarPinScreen
import com.ruralitos.app.ui.screens.DesbloqueoOfflineScreen
import com.ruralitos.app.ui.screens.GestionEquipoSupabaseScreen
import com.ruralitos.app.ui.screens.NuevaClaveSupabaseScreen
import com.ruralitos.app.ui.screens.OrganizacionInicialScreen
import com.ruralitos.app.ui.screens.RecuperarCuentaScreen
import com.ruralitos.app.ui.screens.RegistroSupabaseScreen
import com.ruralitos.app.ui.screens.RevisionFinalFichaScreen
import com.ruralitos.app.ui.screens.SeguridadRespaldoScreen
import com.ruralitos.app.ui.screens.EliminarCuentaScreen
import com.ruralitos.app.ui.screens.CambiarClaveScreen
import com.ruralitos.app.ui.screens.HistorialFichaScreen
import com.ruralitos.app.ui.screens.PerfilScreen
import com.ruralitos.app.ui.screens.EstadisticasScreen
import com.ruralitos.app.ui.screens.InicioRuralitosScreen
import com.ruralitos.app.ui.screens.NotasDiariasScreen
import com.ruralitos.app.ui.screens.AgendaScreen
import com.ruralitos.app.ui.screens.RutaSeguimientoScreen
import com.ruralitos.app.data.agenda.PlanificadorSeguimiento
import com.ruralitos.app.data.agenda.RecordatorioAgenda
import com.ruralitos.app.ui.screens.CredencialesProfesionalesScreen
import com.ruralitos.app.ui.screens.SalaScreen
import com.ruralitos.app.ui.screens.SeleccionTerritorioFichaScreen
import com.ruralitos.app.ui.screens.DispensarizacionScreen
import com.ruralitos.app.ui.screens.guardarFirmaProfesional
import com.ruralitos.app.data.local.entity.UsuarioEntity
import com.ruralitos.app.data.local.entity.HistorialFichaEntity
import com.ruralitos.app.data.local.entity.EliminacionSyncEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.session.SesionLocal
import com.ruralitos.app.domain.SeguridadClave
import com.ruralitos.app.domain.ValidadorIdentidadEcuador
import com.ruralitos.app.domain.NavegacionFicha
import com.ruralitos.app.ui.theme.RuralitosTheme
import com.ruralitos.app.ui.components.FondoRuralitos
import com.ruralitos.app.ui.components.ContenidoAdaptable
import com.ruralitos.app.data.remote.PerfilRemoto
import com.ruralitos.app.data.remote.SupabaseApi
import com.ruralitos.app.data.sync.ProgramadorSincronizacion
import com.ruralitos.app.data.sync.EstadoSincronizacion
import com.ruralitos.app.data.sync.SincronizadorSalas
import androidx.room.withTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


val VerdeRuralitos = Color(0xFF2ECC71)
val VerdeOscuro = Color(0xFF1E8449)
val VerdeFondo = Color(0xFFEAFBF1)
val AmarilloVivo = Color(0xFFF9D65C)
val AzulVivo = Color(0xFF45B7D1)
val NaranjaVivo = Color(0xFFFF9F43)
val TextoOscuro = Color(0xFF1F2933)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runCatching { SupabaseApi(this).procesarCallback(intent?.data) }

        setContent {
            RuralitosTheme {
                RuralitosApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (runCatching { SupabaseApi(this).procesarCallback(intent.data) }.getOrDefault(false)) {
            recreate()
        }
    }
}

@Composable
fun RuralitosApp() {
    // El usuario pidió navegar exclusivamente con los botones visibles de Ruralitos.
    BackHandler(enabled = true) { }
    val context = LocalContext.current
    var databaseCargada by remember { mutableStateOf<RuralitosDatabase?>(null) }
    var errorBaseDatos by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(context.applicationContext) {
        val resultado = withContext(Dispatchers.IO) {
            runCatching { RuralitosDatabase.obtenerBaseDatos(context) }
        }
        databaseCargada = resultado.getOrNull()
        errorBaseDatos = resultado.exceptionOrNull()?.let {
            android.util.Log.e("Ruralitos", "No se pudo preparar la base de datos", it)
            "No se pudo preparar la información local. Cierra y vuelve a abrir Ruralitos."
        }
    }
    LaunchedEffect(databaseCargada) {
        if (databaseCargada != null) {
            withContext(Dispatchers.IO) {
                runCatching { EstablecimientosSaludSeeder.cargarSiEstaVacio(context) }
                    .onFailure { android.util.Log.e("Ruralitos", "No se pudo cargar el catálogo local", it) }
            }
        }
    }
    val database = databaseCargada
    if (database == null) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = VerdeFondo) {
                if (errorBaseDatos == null) {
                    CargandoAccesoScreen()
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Ruralitos no pudo iniciar", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            errorBaseDatos.orEmpty(),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Button(
                            onClick = { (context as? Activity)?.recreate() },
                            modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
                        ) { Text("Intentar nuevamente") }
                    }
                }
            }
        }
        return
    }
    val scope = rememberCoroutineScope()
    val sesion = remember(context) { SesionLocal(context) }
    val resultadoSupabase = remember(context) { runCatching { SupabaseApi(context) } }
    val supabase = resultadoSupabase.getOrNull()
    if (supabase == null) {
        val detalle = resultadoSupabase.exceptionOrNull()?.message
            ?: "No se pudo cargar la configuración de conexión."
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = VerdeFondo) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Ruralitos no pudo iniciar", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        detalle,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
        return
    }
    val fichasPendientesSync by database.sincronizacionDao().observarPendientes().collectAsState(initial = 0)
    val ultimoCambioPendiente by database.sincronizacionDao()
        .observarUltimoCambioPendiente().collectAsState(initial = 0L)
    val salasLocales by database.salaDao().observarSalas().collectAsState(initial = emptyList())
    var estadoAcceso by remember { mutableStateOf("cargando") }
    val preferenciasAvisos = remember(context) {
        context.getSharedPreferences("preferencias_avisos_ruralitos", Context.MODE_PRIVATE)
    }
    var mostrarSolicitudAvisos by remember { mutableStateOf(false) }
    val permisoAvisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        preferenciasAvisos.edit().putBoolean("decision_tomada", true)
            .putBoolean("permitidos", concedido).apply()
    }
    var usuarioActual by remember { mutableStateOf<UsuarioEntity?>(null) }
    var mensajeAcceso by remember { mutableStateOf<String?>(null) }
    var procesandoAcceso by remember { mutableStateOf(false) }
    var codigoInvitacion by remember { mutableStateOf<String?>(null) }
    var pantallaActual by remember { mutableStateOf("inicio") }
    var fichaIdActual by remember { mutableStateOf<Long?>(null) }
    var fichaIdRuta by remember { mutableStateOf<Long?>(null) }
    var fichaAbiertaDesdeAgenda by remember { mutableStateOf(false) }
    var fichaSeleccionada by remember { mutableStateOf<FichaFamiliarEntity?>(null) }
    var modoEdicion by remember { mutableStateOf(false) }
    var desdeRevision by remember { mutableStateOf(false) }
    var establecimientoSeleccionado by remember {
        mutableStateOf<EstablecimientoSaludEntity?>(null)
    }
    var salaSeleccionada by remember { mutableStateOf<SalaEntity?>(null) }
    var eaisSeleccionado by remember { mutableStateOf<EaisSalaEntity?>(null) }
    var territorioSeleccionado by remember { mutableStateOf<TerritorioSalaEntity?>(null) }

    LaunchedEffect(estadoAcceso) {
        if (estadoAcceso == "autenticado" &&
            !preferenciasAvisos.getBoolean("decision_tomada", false)
        ) {
            mostrarSolicitudAvisos = true
        }
    }
    if (mostrarSolicitudAvisos && estadoAcceso == "autenticado") {
        AlertDialog(
            onDismissRequest = {
                preferenciasAvisos.edit().putBoolean("decision_tomada", true).apply()
                mostrarSolicitudAvisos = false
            },
            title = { Text("Recordatorios de Ruralitos") },
            text = { Text("¿Quieres recibir avisos de visitas y notas pendientes? Funcionan sin internet y no muestran datos clínicos en la notificación. Puedes cambiar el permiso después en Ajustes del celular.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarSolicitudAvisos = false
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permisoAvisos.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        preferenciasAvisos.edit().putBoolean("decision_tomada", true)
                            .putBoolean("permitidos", true).apply()
                    }
                }) { Text("Activar avisos") }
            },
            dismissButton = {
                TextButton(onClick = {
                    preferenciasAvisos.edit().putBoolean("decision_tomada", true)
                        .putBoolean("permitidos", false).apply()
                    mostrarSolicitudAvisos = false
                }) { Text("Ahora no") }
            }
        )
    }

    fun avanzarFicha(desde: String) {
        pantallaActual = when {
            desdeRevision -> NavegacionFicha.avanzar(desde, true)
            modoEdicion -> "menuFicha"
            else -> NavegacionFicha.avanzar(desde, false)
        }
        desdeRevision = false
    }

    fun regresarFicha(desde: String) {
        val destino = NavegacionFicha.regresar(desde, modoEdicion, desdeRevision)
        // El panel de la ficha siempre funciona como modo de edición.
        if (destino == "menuFicha") modoEdicion = true
        pantallaActual = destino
        desdeRevision = false
    }

    LaunchedEffect(database) {
        if (supabase.recuperacionPendiente()) {
            estadoAcceso = "nuevaClave"
            return@LaunchedEffect
        }
        if (sesion.estaVencida()) sesion.cerrar()
        val local = withContext(Dispatchers.IO) {
            sesion.usuarioId()?.let { database.usuarioDao().buscarPorId(it) }
                ?.takeIf { it.activo && it.pinConfigurado && it.supabaseId.isNotBlank() }
        }
        if (local != null) {
            usuarioActual = local
            estadoAcceso = "autenticado"
            local.organizacionId.takeIf(String::isNotBlank)?.let {
                ProgramadorSincronizacion.configurar(context)
            }
            if (supabase.sesionGuardada() != null && supabase.hayInternet()) {
                runCatching { prepararUsuarioDesdeSupabase(database, supabase) }
                    .onSuccess { remoto ->
                        if (remoto != null) {
                            usuarioActual = remoto
                            sesion.guardar(remoto.id)
                        }
                    }
            }
            return@LaunchedEffect
        }
        sesion.cerrar()
        if (supabase.sesionGuardada() != null && supabase.hayInternet()) {
            runCatching { prepararUsuarioDesdeSupabase(database, supabase) }
                .onSuccess { usuario ->
                    usuarioActual = usuario
                    estadoAcceso = when {
                        usuario == null -> "organizacion"
                        usuario.firmaUri.isNullOrBlank() -> "configurarProfesional"
                        !usuario.pinConfigurado -> "configurarPin"
                        else -> {
                            sesion.guardar(usuario.id)
                            ProgramadorSincronizacion.configurar(context)
                            "autenticado"
                        }
                    }
                }
                .onFailure {
                    mensajeAcceso = it.message ?: "No se pudo comprobar la sesión remota."
                    estadoAcceso = "login"
                }
        } else {
            estadoAcceso = "login"
        }
    }

    val cicloVida = (context as? ComponentActivity)?.lifecycle
    DisposableEffect(cicloVida, estadoAcceso) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_STOP -> {
                    if (estadoAcceso == "autenticado") sesion.registrarSalida()
                }
                Lifecycle.Event.ON_START -> {
                    if (estadoAcceso == "autenticado" && sesion.estaVencida()) {
                        sesion.cerrar()
                        usuarioActual = null
                        fichaIdActual = null
                        fichaSeleccionada = null
                        pantallaActual = "inicio"
                        mensajeAcceso = "La sesión se bloqueó por inactividad."
                        estadoAcceso = "login"
                    } else if (estadoAcceso == "autenticado" && supabase.sesionGuardada() != null) {
                        val preferencias = context.getSharedPreferences("ruralitos_sync", Context.MODE_PRIVATE)
                        val ahora = System.currentTimeMillis()
                        if (ahora - preferencias.getLong("ultima_consulta_primer_plano", 0L) >= 60_000L) {
                            preferencias.edit().putLong("ultima_consulta_primer_plano", ahora).apply()
                            ProgramadorSincronizacion.ejecutarAhora(context)
                        }
                    }
                }
                else -> Unit
            }
        }
        cicloVida?.addObserver(observador)
        onDispose { cicloVida?.removeObserver(observador) }
    }

    val usuarioSyncLocalId = usuarioActual?.id ?: 0L
    val agendaPendientesSync by remember(database, usuarioSyncLocalId) {
        database.agendaDao().observarPendientesSync(usuarioSyncLocalId)
    }.collectAsState(initial = 0)
    val notasPendientesSync by remember(database, usuarioSyncLocalId) {
        database.notaDiariaDao().observarPendientesSync(usuarioSyncLocalId)
    }.collectAsState(initial = 0)
    val agendaConflictosSync by remember(database, usuarioSyncLocalId) {
        database.agendaDao().observarConflictosSync(usuarioSyncLocalId)
    }.collectAsState(initial = 0)
    val notasConflictosSync by remember(database, usuarioSyncLocalId) {
        database.notaDiariaDao().observarConflictosSync(usuarioSyncLocalId)
    }.collectAsState(initial = 0)
    val agendaUltimoCambio by remember(database, usuarioSyncLocalId) {
        database.agendaDao().observarUltimoCambioPendiente(usuarioSyncLocalId)
    }.collectAsState(initial = 0L)
    val notasUltimoCambio by remember(database, usuarioSyncLocalId) {
        database.notaDiariaDao().observarUltimoCambioPendiente(usuarioSyncLocalId)
    }.collectAsState(initial = 0L)
    val revisionSincronizacion by EstadoSincronizacion.revision.collectAsState()
    val totalPendiente = fichasPendientesSync + agendaPendientesSync + notasPendientesSync
    val estadoSincronizacion = when {
        agendaConflictosSync + notasConflictosSync > 0 ->
            "${agendaConflictosSync + notasConflictosSync} cambios por revisar"
        totalPendiente > 0 -> "$totalPendiente pendiente${if (totalPendiente == 1) "" else "s"} de sincronizar"
        !supabase.hayInternet() -> "Sin conexión · datos guardados en el teléfono"
        EstadoSincronizacion.ultimoIntentoCorrecto(context, usuarioActual?.supabaseId.orEmpty()) == false ->
            "No se pudo sincronizar · datos guardados"
        EstadoSincronizacion.ultimoIntentoCorrecto(context, usuarioActual?.supabaseId.orEmpty()) == true ->
            "Sincronizado"
        else -> "Guardado local"
    }

    LaunchedEffect(estadoAcceso, fichasPendientesSync, ultimoCambioPendiente,
        agendaPendientesSync, notasPendientesSync, agendaUltimoCambio, notasUltimoCambio) {
        if (estadoAcceso == "autenticado" &&
            fichasPendientesSync + agendaPendientesSync + notasPendientesSync > 0 &&
            supabase.hayInternet()) {
            // Al completar un formulario se producen varias escrituras consecutivas.
            // Agruparlas evita lanzar una sincronización completa por cada cambio.
            delay(8_000)
            ProgramadorSincronizacion.ejecutarCambiosLocales(context)
        }
    }

    AccesoConCargaMinima(cargandoReal = estadoAcceso == "cargando") {
        FondoRuralitos {
            ContenidoAdaptable {
            when (estadoAcceso) {
                "cargando" -> CargandoAccesoScreen()

                "crearAdministrador" -> CrearAdministradorScreen(
                    procesando = procesandoAcceso,
                    mensajeError = mensajeAcceso,
                    onCrear = { cedula, nombres, cargo, clave ->
                        procesandoAcceso = true
                        mensajeAcceso = null
                        scope.launch {
                            val resultado = runCatching {
                                val protegida = withContext(Dispatchers.Default) {
                                    SeguridadClave.proteger(clave)
                                }
                                val usuarioBase = UsuarioEntity(
                                    cedula = cedula,
                                    nombres = nombres,
                                    cargo = cargo,
                                    rol = UsuarioEntity.ROL_ADMIN,
                                    claveHash = protegida.hash,
                                    claveSalt = protegida.salt
                                )
                                val id = withContext(Dispatchers.IO) {
                                    check(database.usuarioDao().contar() == 0)
                                    database.usuarioDao().guardar(usuarioBase)
                                }
                                usuarioBase.copy(id = id)
                            }
                            procesandoAcceso = false
                            resultado.onSuccess { usuario ->
                                sesion.guardar(usuario.id)
                                usuarioActual = usuario
                                estadoAcceso = "autenticado"
                            }.onFailure {
                                mensajeAcceso = "No se pudo crear el administrador. Inténtalo nuevamente."
                            }
                        }
                    }
                )

                "login" -> AccesoSupabaseScreen(
                    procesando = procesandoAcceso,
                    mensaje = mensajeAcceso,
                    onIngresar = { correo, clave ->
                        procesandoAcceso = true
                        mensajeAcceso = null
                        scope.launch {
                            runCatching {
                                supabase.iniciarSesion(correo, clave)

                                prepararUsuarioDesdeSupabase(database, supabase)
                            }.onSuccess { usuario ->
                                usuarioActual = usuario
                                estadoAcceso = when {
                                    usuario == null -> "organizacion"
                                    usuario.firmaUri.isNullOrBlank() -> "configurarProfesional"
                                    !usuario.pinConfigurado -> "configurarPin"
                                    else -> {
                                        sesion.guardar(usuario.id)
                                        ProgramadorSincronizacion.configurar(context)
                                        "autenticado"
                                    }
                                }
                            }.onFailure {
                                mensajeAcceso = it.message ?: "No se pudo iniciar sesión."
                            }
                            procesandoAcceso = false
                        }
                    },
                    onCrearCuenta = {
                        mensajeAcceso = null
                        estadoAcceso = "registro"
                    },
                    onRecuperar = {
                        mensajeAcceso = null
                        estadoAcceso = "recuperar"
                    },
                    onSinInternet = {
                        mensajeAcceso = null
                        estadoAcceso = "desbloqueo"
                    }
                )

                "registro" -> RegistroSupabaseScreen(
                    procesando = procesandoAcceso,
                    mensaje = mensajeAcceso,
                    onRegistrar = { correo, clave, cedula, nombres, cargo, telefono, codigoSenescyt ->
                        procesandoAcceso = true
                        mensajeAcceso = null
                        scope.launch {
                            runCatching {
                                supabase.registrar(correo, clave, cedula, nombres, cargo, telefono, codigoSenescyt)
                            }.onSuccess { resultado ->
                                if (resultado.requiereConfirmarCorreo) {
                                    estadoAcceso = "login"
                                    mensajeAcceso = "Cuenta creada. Revisa tu correo y confirma el enlace antes de ingresar."
                                } else {
                                    usuarioActual = prepararUsuarioDesdeSupabase(database, supabase)
                                    estadoAcceso = if (usuarioActual == null) "organizacion" else "configurarProfesional"
                                }
                            }.onFailure {
                                mensajeAcceso = it.message ?: "No se pudo crear la cuenta."
                            }
                            procesandoAcceso = false
                        }
                    },
                    onVolver = {
                        mensajeAcceso = null
                        estadoAcceso = "login"
                    }
                )

                "recuperar" -> RecuperarCuentaScreen(
                    procesando = procesandoAcceso,
                    mensaje = mensajeAcceso,
                    onEnviar = { correo ->
                        procesandoAcceso = true
                        mensajeAcceso = null
                        scope.launch {
                            runCatching { supabase.enviarRecuperacion(correo) }
                                .onSuccess {
                                    mensajeAcceso = "Enlace enviado. Revisa el correo y vuelve a Ruralitos desde ese enlace."
                                }
                                .onFailure {
                                    mensajeAcceso = it.message ?: "No se pudo enviar el enlace."
                                }
                            procesandoAcceso = false
                        }
                    },
                    onVolver = {
                        mensajeAcceso = null
                        estadoAcceso = "login"
                    }
                )

                "organizacion" -> OrganizacionInicialScreen(
                    procesando = procesandoAcceso,
                    mensaje = mensajeAcceso,
                    onAceptarCodigo = { codigo ->
                        procesandoAcceso = true
                        mensajeAcceso = null
                        scope.launch {
                            runCatching {
                                supabase.aceptarInvitacion(codigo)
                                checkNotNull(prepararUsuarioDesdeSupabase(database, supabase))
                            }.onSuccess { usuario ->
                                usuarioActual = usuario
                                estadoAcceso = if (usuario.firmaUri.isNullOrBlank()) "configurarProfesional" else "configurarPin"
                            }.onFailure {
                                mensajeAcceso = it.message ?: "El código no es válido o ya venció."
                            }
                            procesandoAcceso = false
                        }
                    },
                    onCrearSala = {
                        mensajeAcceso = null
                        estadoAcceso = "crearSalaUnidad"
                    },
                    onCerrarSesion = {
                        ProgramadorSincronizacion.cancelar(context)
                        scope.launch { supabase.cerrarSesion() }
                        usuarioActual = null
                        mensajeAcceso = null
                        estadoAcceso = "login"
                    }
                )

                "crearSalaUnidad" -> BuscarUnidadOperativaScreen(
                    onEstablecimientoSeleccionado = { establecimiento ->
                        procesandoAcceso = true
                        mensajeAcceso = null
                        scope.launch {
                            runCatching {
                                supabase.crearSala(establecimiento.codigoUo)
                                checkNotNull(prepararUsuarioDesdeSupabase(database, supabase))
                            }.onSuccess { usuario ->
                                usuarioActual = usuario
                                estadoAcceso = if (usuario.firmaUri.isNullOrBlank()) {
                                    "configurarProfesional"
                                } else {
                                    "configurarPin"
                                }
                            }.onFailure {
                                mensajeAcceso = it.message ?: "No se pudo crear la Sala."
                                estadoAcceso = "organizacion"
                            }
                            procesandoAcceso = false
                        }
                    },
                    onRegresar = { estadoAcceso = "organizacion" }
                )
                "configurarPin" -> ConfigurarPinScreen(
                    procesando = procesandoAcceso,
                    mensaje = mensajeAcceso,
                    onGuardar = { pin ->
                        val usuario = usuarioActual
                        if (usuario == null) {
                            estadoAcceso = "login"
                        } else {
                            procesandoAcceso = true
                            mensajeAcceso = null
                            scope.launch {
                                runCatching {
                                    val protegida = withContext(Dispatchers.Default) {
                                        SeguridadClave.proteger(pin)
                                    }
                                    withContext(Dispatchers.IO) {
                                        database.usuarioDao().guardarPin(usuario.id, protegida.hash, protegida.salt)
                                        database.usuarioDao().registrarAcceso(usuario.id)
                                        database.usuarioDao().buscarPorId(usuario.id)
                                    }
                                }.onSuccess { actualizado ->
                                    val autenticado = checkNotNull(actualizado)
                                    usuarioActual = autenticado
                                    sesion.guardar(autenticado.id)
                                    ProgramadorSincronizacion.configurar(context)
                                    estadoAcceso = "autenticado"
                                }.onFailure {
                                    mensajeAcceso = "No se pudo guardar el PIN. Inténtalo nuevamente."
                                }
                                procesandoAcceso = false
                            }
                        }
                    }
                )

                "configurarProfesional" -> {
                    val usuario = usuarioActual
                    if (usuario == null) {
                        estadoAcceso = "login"
                    } else {
                        CredencialesProfesionalesScreen(
                            usuario = usuario,
                            procesando = procesandoAcceso,
                            mensaje = mensajeAcceso,
                            obligatorio = true,
                            onGuardar = { codigo, firma ->
                                procesandoAcceso = true
                                mensajeAcceso = null
                                scope.launch {
                                    runCatching {
                                        guardarIdentidadProfesional(
                                            context = context,
                                            database = database,
                                            supabase = supabase,
                                            usuario = usuario,
                                            codigo = codigo,
                                            firma = firma
                                        )
                                    }.onSuccess {
                                        usuarioActual = it
                                        estadoAcceso = if (it.pinConfigurado) "autenticado" else "configurarPin"
                                    }.onFailure {
                                        mensajeAcceso = it.message ?: "No se pudo guardar la identidad profesional."
                                    }
                                    procesandoAcceso = false
                                }
                            },
                            onRegresar = {}
                        )
                    }
                }

                "desbloqueo" -> DesbloqueoOfflineScreen(
                    procesando = procesandoAcceso,
                    mensaje = mensajeAcceso,
                    onEntrar = { cedula, pin ->
                        procesandoAcceso = true
                        mensajeAcceso = null
                        scope.launch {
                            val usuario = withContext(Dispatchers.IO) {
                                database.usuarioDao().buscarPorCedula(cedula)
                            }?.takeIf { it.activo && it.pinConfigurado }
                            val correcto = usuario?.let {
                                withContext(Dispatchers.Default) {
                                    SeguridadClave.verificar(pin, it.claveHash, it.claveSalt)
                                }
                            } == true
                            if (correcto) {
                                val autenticado = checkNotNull(usuario)
                                withContext(Dispatchers.IO) {
                                    database.usuarioDao().registrarAcceso(autenticado.id)
                                }
                                sesion.guardar(autenticado.id)
                                usuarioActual = autenticado
                                estadoAcceso = "autenticado"
                            } else {
                                mensajeAcceso = "Cédula o PIN incorrectos. Primero debes haber iniciado sesión con internet en este teléfono."
                            }
                            procesandoAcceso = false
                        }
                    },
                    onVolver = {
                        mensajeAcceso = null
                        estadoAcceso = "login"
                    }
                )

                "nuevaClave" -> NuevaClaveSupabaseScreen(
                    procesando = procesandoAcceso,
                    mensaje = mensajeAcceso,
                    onGuardar = { nuevaClave ->
                        procesandoAcceso = true
                        mensajeAcceso = null
                        scope.launch {
                            runCatching {
                                supabase.actualizarClave(nuevaClave)
                                prepararUsuarioDesdeSupabase(database, supabase)
                            }.onSuccess { usuario ->
                                supabase.finalizarRecuperacion()
                                usuarioActual = usuario
                                estadoAcceso = when {
                                    usuario == null -> "organizacion"
                                    !usuario.pinConfigurado -> "configurarPin"
                                    else -> {
                                        sesion.guardar(usuario.id)
                                        ProgramadorSincronizacion.configurar(context)
                                        "autenticado"
                                    }
                                }
                            }.onFailure {
                                mensajeAcceso = it.message ?: "No se pudo actualizar la contraseña."
                            }
                            procesandoAcceso = false
                        }
                    }
                )

                "loginLocalLegacy" -> LoginClinicoScreen(
                    procesando = procesandoAcceso,
                    mensajeError = mensajeAcceso,
                    onIngresar = { cedula, clave ->
                        procesandoAcceso = true
                        mensajeAcceso = null
                        scope.launch {
                            val usuario = withContext(Dispatchers.IO) {
                                database.usuarioDao().buscarPorCedula(cedula)
                            }
                            val usuarioValido = usuario?.takeIf { it.activo }
                            val claveCorrecta = usuarioValido?.let {
                                withContext(Dispatchers.Default) {
                                    SeguridadClave.verificar(clave, it.claveHash, it.claveSalt)
                                }
                            } == true
                            procesandoAcceso = false
                            if (claveCorrecta) {
                                val autenticado = checkNotNull(usuarioValido)
                                sesion.guardar(autenticado.id)
                                usuarioActual = autenticado
                                estadoAcceso = "autenticado"
                                mensajeAcceso = null
                            } else {
                                mensajeAcceso = "Cédula o contraseña incorrecta, o cuenta desactivada."
                            }
                        }
                    }
                )

                else -> when (pantallaActual) {
                "inicio" -> {
                    PantallaPrincipal(
                        usuarioId = usuarioActual?.id ?: 0L,
                        usuarioNombre = usuarioActual?.nombres.orEmpty(),
                        usuarioCargo = usuarioActual?.cargo.orEmpty(),
                        codigoSenescyt = usuarioActual?.codigoSenescyt.orEmpty(),
                        fichasPendientesSync = fichasPendientesSync,
                        estadoSincronizacion = estadoSincronizacion,
                        onNuevaFicha = {
                            modoEdicion = false
                            fichaSeleccionada = null
                            fichaIdActual = null
                            pantallaActual = "seleccionarTerritorio"
                        },
                        onBuscarFicha = {
                            pantallaActual = "buscarFichas"
                        },
                        onSala = {
                            pantallaActual = "sala"
                        },
                        onDispensarizacion = {
                            pantallaActual = "dispensarizacion"
                        },
                        onNotasDiarias = {
                            pantallaActual = "notasDiarias"
                        },
                        onAgenda = {
                            pantallaActual = "agenda"
                        },
                        onEstadisticas = {
                            pantallaActual = "estadisticas"
                        },
                        onPerfil = {
                            pantallaActual = "perfil"
                        },
                        onCredenciales = {
                            pantallaActual = "credenciales"
                        },
                        onSeguridad = {
                            pantallaActual = "seguridad"
                        },
                        onEliminarCuenta = {
                            pantallaActual = "eliminarCuenta"
                        },
                        onCambiarClave = {
                            pantallaActual = "miClave"
                        },
                        onCerrarSesion = {
                            sesion.cerrar()
                            ProgramadorSincronizacion.cancelar(context)
                            scope.launch { supabase.cerrarSesion() }
                            usuarioActual = null
                            fichaIdActual = null
                            fichaSeleccionada = null
                            pantallaActual = "inicio"
                            estadoAcceso = "login"
                        }
                    )
                }

                "usuarios" -> {
                    val usuario = usuarioActual
                    if (usuario?.esAdministrador == true) {
                        GestionEquipoSupabaseScreen(
                            procesando = procesandoAcceso,
                            mensaje = mensajeAcceso,
                            codigoGenerado = codigoInvitacion,
                            onCrearInvitacion = { correo, rol ->
                                procesandoAcceso = true
                                mensajeAcceso = null
                                codigoInvitacion = null
                                scope.launch {
                                    runCatching {
                                        supabase.crearInvitacion(usuario.organizacionId, correo, rol)
                                    }.onSuccess {
                                        codigoInvitacion = it.codigo
                                        mensajeAcceso = "Invitación creada correctamente."
                                    }.onFailure {
                                        mensajeAcceso = it.message ?: "No se pudo crear la invitación."
                                    }
                                    procesandoAcceso = false
                                }
                            },
                            onRegresar = { pantallaActual = "inicio" }
                        )
                    } else {
                        pantallaActual = "inicio"
                    }
                }

                "seguridad" -> {
                    val usuario = usuarioActual
                    if (usuario != null) {
                        val salaActiva = salaSeleccionada
                            ?: salasLocales.firstOrNull { it.organizacionId == usuario.organizacionId }
                            ?: salasLocales.firstOrNull()
                        SeguridadRespaldoScreen(
                            usuario = usuario,
                            salaActiva = salaActiva,
                            onRestaurado = { resultado ->
                                if (supabase.hayInternet()) {
                                    ProgramadorSincronizacion.ejecutarAhora(context)
                                }
                                Toast.makeText(
                                    context,
                                    "${resultado.fichasImportadas} ficha(s) importadas.",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onRegresar = { pantallaActual = "inicio" }
                        )
                    } else {
                        pantallaActual = "inicio"
                    }
                }

                "eliminarCuenta" -> {
                    val usuario = usuarioActual
                    if (usuario != null) {
                        EliminarCuentaScreen(
                            correo = usuario.correo,
                            onCrearRespaldo = { pantallaActual = "seguridad" },
                            onEnviarCodigo = { supabase.enviarCodigoEliminacion() },
                            onEliminar = { codigo -> supabase.eliminarCuenta(codigo) },
                            onEliminada = {
                                ProgramadorSincronizacion.cancelar(context)
                                supabase.limpiarSesionLocal()
                                sesion.cerrar()
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        database.clearAllTables()
                                        EstablecimientosSaludSeeder.cargarSiEstaVacio(context)
                                    }
                                    usuarioActual = null
                                    fichaIdActual = null
                                    fichaSeleccionada = null
                                    salaSeleccionada = null
                                    eaisSeleccionado = null
                                    territorioSeleccionado = null
                                    pantallaActual = "inicio"
                                    estadoAcceso = "login"
                                    Toast.makeText(
                                        context,
                                        "La cuenta y los datos locales fueron eliminados.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onRegresar = { pantallaActual = "inicio" }
                        )
                    } else {
                        estadoAcceso = "login"
                    }
                }

                "miClave" -> {
                    val usuario = usuarioActual
                    if (usuario != null) {
                        CambiarClaveCuentaScreen(
                            procesando = procesandoAcceso,
                            mensaje = mensajeAcceso,
                            onGuardar = { actual, nueva ->
                                procesandoAcceso = true
                                mensajeAcceso = null
                                scope.launch {
                                    runCatching {
                                        supabase.cambiarClave(usuario.correo, actual, nueva)
                                    }.onSuccess {
                                        Toast.makeText(context, "Contraseña actualizada.", Toast.LENGTH_SHORT).show()
                                        pantallaActual = "inicio"
                                    }.onFailure {
                                        mensajeAcceso = it.message ?: "No se pudo cambiar la contraseña."
                                    }
                                    procesandoAcceso = false
                                }
                            },
                            onVolver = { pantallaActual = "inicio" }
                        )
                    } else {
                        estadoAcceso = "login"
                    }
                }

                "perfil" -> {
                    val usuario = usuarioActual
                    if (usuario != null) {
                        PerfilScreen(
                            usuario = usuario,
                            usuarioDao = database.usuarioDao(),
                            onGuardarRemoto = { actualizado ->
                                supabase.actualizarPerfil(
                                    PerfilRemoto(
                                        id = actualizado.supabaseId,
                                        cedula = actualizado.cedula,
                                        nombres = actualizado.nombres,
                                        cargo = actualizado.cargo,
                                        correo = actualizado.correo,
                                        telefono = actualizado.telefono,
                                        codigoSenescyt = actualizado.codigoSenescyt
                                    )
                                )
                            },
                            onActualizado = { usuarioActual = it },
                            onCambiarClave = { pantallaActual = "miClave" },
                            onRegresar = { pantallaActual = "inicio" }
                        )
                    } else {
                        estadoAcceso = "login"
                    }
                }

                "estadisticas" -> {
                    EstadisticasScreen(
                        onFichaSeleccionada = { ficha ->
                            fichaSeleccionada = ficha
                            fichaIdActual = ficha.id
                            modoEdicion = true
                            pantallaActual = "menuFicha"
                        },
                        onRegresar = { pantallaActual = "inicio" }
                    )
                }

                "credenciales" -> {
                    val usuario = usuarioActual
                    if (usuario != null) {
                        CredencialesProfesionalesScreen(
                            usuario = usuario,
                            procesando = procesandoAcceso,
                            mensaje = mensajeAcceso,
                            onGuardar = { codigo, firma ->
                                procesandoAcceso = true
                                mensajeAcceso = null
                                scope.launch {
                                    runCatching {
                                        guardarIdentidadProfesional(
                                            context = context,
                                            database = database,
                                            supabase = supabase,
                                            usuario = usuario,
                                            codigo = codigo,
                                            firma = firma
                                        )
                                    }.onSuccess {
                                        usuarioActual = it
                                        Toast.makeText(context, "Identidad profesional actualizada.", Toast.LENGTH_SHORT).show()
                                        pantallaActual = "inicio"
                                    }.onFailure { mensajeAcceso = it.message ?: "No se pudo guardar." }
                                    procesandoAcceso = false
                                }
                            },
                            onRegresar = { pantallaActual = "inicio" }
                        )
                    } else estadoAcceso = "login"
                }

                "notasDiarias" -> {
                    NotasDiariasScreen(
                        organizacionId = supabase.organizacionGuardada().orEmpty(),
                        usuarioId = usuarioActual?.id ?: 0L,
                        onRegresar = { pantallaActual = "inicio" }
                    )
                }

                "agenda" -> {
                    AgendaScreen(
                        usuarioId = usuarioActual?.id ?: 0L,
                        organizacionId = supabase.organizacionGuardada().orEmpty(),
                        onRegresar = { pantallaActual = "inicio" },
                        onAbrirFicha = { id ->
                            scope.launch {
                                fichaSeleccionada = withContext(Dispatchers.IO) {
                                    database.fichaFamiliarDao().buscarPorId(id)
                                }
                                if (fichaSeleccionada != null) {
                                    fichaIdActual = id
                                    modoEdicion = true
                                    fichaAbiertaDesdeAgenda = true
                                    pantallaActual = "menuFicha"
                                }
                            }
                        },
                        onAbrirRuta = { id ->
                            fichaIdRuta = id
                            pantallaActual = "rutaSeguimiento"
                        }
                    )
                }

                "rutaSeguimiento" -> {
                    val id = fichaIdRuta
                    if (id != null) RutaSeguimientoScreen(
                        fichaId = id,
                        usuarioId = usuarioActual?.id,
                        onRegresar = { pantallaActual = "agenda" },
                        onAbrirFicha = {
                            scope.launch {
                                fichaSeleccionada = withContext(Dispatchers.IO) {
                                    database.fichaFamiliarDao().buscarPorId(id)
                                }
                                if (fichaSeleccionada != null) {
                                    fichaIdActual = id
                                    modoEdicion = true
                                    fichaAbiertaDesdeAgenda = true
                                    pantallaActual = "menuFicha"
                                }
                            }
                        }
                    ) else pantallaActual = "agenda"
                }

                "sala" -> {
                    SalaScreen(
                        database = database,
                        supabase = supabase,
                        onAgregarCentro = { pantallaActual = "agregarSalaUnidad" },
                        onRegresar = { pantallaActual = "inicio" }
                    )
                }

                "agregarSalaUnidad" -> {
                    BuscarUnidadOperativaScreen(
                        onEstablecimientoSeleccionado = { establecimiento ->
                            procesandoAcceso = true
                            scope.launch {
                                runCatching {
                                    supabase.crearSala(establecimiento.codigoUo)
                                    prepararUsuarioDesdeSupabase(database, supabase)
                                }.onSuccess { actualizado ->
                                    if (actualizado != null) usuarioActual = actualizado
                                    pantallaActual = "sala"
                                }.onFailure {
                                    mensajeAcceso = it.message ?: "No se pudo agregar el centro."
                                    Toast.makeText(context, mensajeAcceso, Toast.LENGTH_LONG).show()
                                    pantallaActual = "sala"
                                }
                                procesandoAcceso = false
                            }
                        },
                        onRegresar = { pantallaActual = "sala" }
                    )
                }

                "seleccionarTerritorio" -> {
                    SeleccionTerritorioFichaScreen(
                        database = database,
                        organizacionActiva = supabase.organizacionGuardada(),
                        onContinuar = { sala, eais, territorio, fechaLlenado ->
                            salaSeleccionada = sala
                            eaisSeleccionado = eais
                            territorioSeleccionado = territorio
                            supabase.guardarOrganizacionActiva(sala.organizacionId)
                            val establecimiento = sala.comoEstablecimientoLocal()
                            establecimientoSeleccionado = establecimiento
                            scope.launch {
                                try {
                                    val ahora = System.currentTimeMillis()
                                    val fichaGuardada = withContext(Dispatchers.IO) {
                                        database.withTransaction {
                                            val usuario = checkNotNull(usuarioActual)
                                            val numeroAsignado = database.fichaFamiliarDao()
                                                .siguienteNumeroFichaBarrio(territorio.id).toString()
                                            val id = database.fichaFamiliarDao().guardarFicha(
                                                FichaFamiliarEntity(
                                                    // El jefe o jefa se registra en Integrantes de la familia.
                                                    cedulaJefeHogar = "",
                                                    institucionSistema = establecimiento.institucionSistema,
                                                    unidadOperativa = establecimiento.nombreCentroSalud,
                                                    codigoUo = establecimiento.codigoUo,
                                                    areaNumero = establecimiento.areaNumero,
                                                    codigoLocalizacion = listOf(
                                                        establecimiento.parroquiaCodigoLocalizacion,
                                                        establecimiento.cantonCodigoLocalizacion,
                                                        establecimiento.provinciaCodigoLocalizacion
                                                    ).joinToString(""),
                                                    parroquiaCodigoLocalizacion = establecimiento.parroquiaCodigoLocalizacion,
                                                    cantonCodigoLocalizacion = establecimiento.cantonCodigoLocalizacion,
                                                    provinciaCodigoLocalizacion = establecimiento.provinciaCodigoLocalizacion,
                                                    numeroFichaFamiliar = numeroAsignado,
                                                    provincia = establecimiento.provincia,
                                                    canton = establecimiento.canton,
                                                    parroquia = establecimiento.parroquia,
                                                    sector = establecimiento.sector,
                                                    manzana = "",
                                                    numeroFamilia = "",
                                                    direccionHabitualFamilia = "",
                                                    barrio = territorio.nombre,
                                                    numeroCasa = "",
                                                    comunidad = "",
                                                    grupoCultural = "",
                                                    nombreApellidoJefeFamilia = "",
                                                    numeroTelefono = "",
                                                    fechaLlenado = fechaLlenado,
                                                    numeroCarpeta = eais.numero.toString(),
                                                    responsableNombre = usuario.nombres,
                                                    responsableCodigo = usuario.codigoSenescyt,
                                                    firmaUri = usuario.firmaUri,
                                                    creadoEn = ahora,
                                                    actualizadoEn = ahora,
                                                    creadoPorUsuarioId = usuario.id,
                                                    actualizadoPorUsuarioId = usuario.id,
                                                    organizacionId = sala.organizacionId
                                                        .ifBlank { usuario.organizacionId },
                                                    establecimientoRemotoId = sala.establecimientoId,
                                                    eaisId = eais.id,
                                                    territorioId = territorio.id
                                                )
                                            )
                                            registrarEvento(
                                                database, id, numeroAsignado, usuario,
                                                "FICHA_CREADA"
                                            )
                                            checkNotNull(
                                                database.fichaFamiliarDao().buscarPorId(id)
                                            ) { "La ficha no quedó disponible después de guardarla." }
                                        }
                                    }
                                    modoEdicion = false
                                    fichaIdActual = fichaGuardada.id
                                    fichaSeleccionada = fichaGuardada
                                    pantallaActual = "ubicacion"
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        context,
                                        "No se pudo guardar el borrador.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onConfigurarSala = { pantallaActual = "sala" },
                        onRegresar = { pantallaActual = "inicio" }
                    )
                }
                "consolidado" -> {
                    DispensarizacionScreen(
                        onAbrirFicha = { ficha ->
                            fichaSeleccionada = ficha
                            fichaIdActual = ficha.id
                            modoEdicion = true
                            pantallaActual = "menuFicha"
                        },
                        onRegresar = { pantallaActual = "inicio" }
                    )
                }

                "dispensarizacion" -> {
                    DispensarizacionScreen(
                        onAbrirFicha = { ficha ->
                            fichaSeleccionada = ficha
                            fichaIdActual = ficha.id
                            modoEdicion = true
                            pantallaActual = "menuFicha"
                        },
                        onRegresar = { pantallaActual = "inicio" }
                    )
                }

                "dispensarizacionFicha" -> {
                    val ficha = fichaSeleccionada
                    if (ficha != null) {
                        DispensarizacionScreen(
                            fichaInicialId = ficha.id,
                            onAbrirFicha = { seleccionada ->
                                fichaSeleccionada = seleccionada
                                fichaIdActual = seleccionada.id
                                modoEdicion = true
                                pantallaActual = "menuFicha"
                            },
                            onRegresar = {
                                pantallaActual = if (desdeRevision) "revisionFicha" else "menuFicha"
                                desdeRevision = false
                            }
                        )
                    } else {
                        pantallaActual = "buscarFichas"
                    }
                }

                "buscarFichas" -> {
                    BuscarFichasScreen(
                        onFichaSeleccionada = { ficha ->
                            fichaAbiertaDesdeAgenda = false
                            fichaSeleccionada = ficha
                            fichaIdActual = ficha.id
                            modoEdicion = true
                            pantallaActual = "menuFicha"
                        },
                        onRegresar = { pantallaActual = "inicio" }
                    )
                }

                "menuFicha" -> {
                    val ficha = fichaSeleccionada
                    if (ficha != null) {
                        FichaSeccionesScreen(
                            ficha = ficha,
                            onAbrirSeccion = { seccion ->
                                pantallaActual = when (seccion) {
                                    "datos" -> "miembros"
                                    "ubicacion" -> "editarUbicacion"
                                    "miembros" -> "miembros"
                                    "salud" -> "saludFamiliar"
                                    "dispensarizacion" -> "dispensarizacionFicha"
                                    "riesgos" -> "riesgos"
                                    "gestion" -> "gestionRiesgo"
                                    "familiograma" -> "familiograma"
                                    "croquis" -> "croquisMapa"
                                    "contaminacion" -> "contaminacion"
                                    "tratamiento" -> "tratamiento"
                                    "revision" -> "revisionFicha"
                                    else -> "historialFicha"
                                }
                            },
                            onCambiarArchivado = {
                                scope.launch {
                                    val nuevoEstado = if (ficha.estado == "ARCHIVADA") {
                                        if (ficha.completadoPorUsuarioId != null) "COMPLETA" else "BORRADOR"
                                    } else {
                                        "ARCHIVADA"
                                    }
                                    withContext(Dispatchers.IO) {
                                        database.fichaFamiliarDao().actualizarEstado(
                                            fichaId = ficha.id,
                                            estado = nuevoEstado,
                                            usuarioId = usuarioActual?.id
                                        )
                                        registrarEvento(
                                            database,
                                            ficha.id,
                                            ficha.numeroFichaFamiliar,
                                            usuarioActual,
                                            if (nuevoEstado == "ARCHIVADA") "FICHA_ARCHIVADA" else "FICHA_REACTIVADA"
                                        )
                                    }
                                    if (nuevoEstado == "ARCHIVADA") {
                                        PlanificadorSeguimiento.retirarFicha(context, ficha.id)
                                    } else if (nuevoEstado == "COMPLETA") {
                                        usuarioActual?.id?.let { id ->
                                            PlanificadorSeguimiento.prepararFicha(context, ficha.id, id)
                                        }
                                    }
                                    fichaSeleccionada = ficha.copy(
                                        estado = nuevoEstado,
                                        actualizadoPorUsuarioId = usuarioActual?.id,
                                        actualizadoEn = System.currentTimeMillis()
                                    )
                                    Toast.makeText(
                                        context,
                                        if (nuevoEstado == "ARCHIVADA") "Ficha archivada." else "Ficha reactivada.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onEliminar = {
                                scope.launch {
                                    val idsAgenda = withContext(Dispatchers.IO) {
                                        database.agendaDao().idsDeFicha(ficha.id)
                                    }
                                    val archivosAEliminar = withContext(Dispatchers.IO) {
                                        registrarEvento(
                                            database, ficha.id, ficha.numeroFichaFamiliar,
                                            usuarioActual, "FICHA_ELIMINADA"
                                        )
                                        database.withTransaction {
                                            val syncDao = database.sincronizacionDao()
                                            val adjuntos = syncDao.adjuntos(ficha.id)
                                            val calificaciones = syncDao.calificaciones(ficha.id)
                                            val registrosRemotos = mutableListOf<Pair<String, String>>()
                                            registrosRemotos += "fichas_familiares" to ficha.syncId
                                            registrosRemotos += syncDao.miembros(ficha.id)
                                                .map { "miembros_familia" to it.syncId }
                                            registrosRemotos += syncDao.embarazadas(ficha.id)
                                                .map { "embarazadas" to it.syncId }
                                            registrosRemotos += syncDao.mortalidad(ficha.id)
                                                .map { "mortalidad_familiar" to it.syncId }
                                            registrosRemotos += calificaciones
                                                .map { "calificaciones_riesgo" to it.syncId }
                                            calificaciones.forEach { calificacion ->
                                                registrosRemotos += syncDao.valores(calificacion.id)
                                                    .map { "valores_riesgo" to it.syncId }
                                            }
                                            registrosRemotos += syncDao.gestiones(ficha.id)
                                                .map { "gestion_riesgo" to it.syncId }
                                            registrosRemotos += syncDao.contaminaciones(ficha.id)
                                                .map { "contaminacion_ambiental" to it.syncId }
                                            registrosRemotos += syncDao.lugares(ficha.id)
                                                .map { "lugares_tratamiento" to it.syncId }
                                            registrosRemotos += adjuntos.map { "adjuntos_ficha" to it.syncId }

                                            if (ficha.organizacionId.isNotBlank()) {
                                                val ahora = System.currentTimeMillis()
                                                syncDao.guardarEliminaciones(
                                                    registrosRemotos
                                                        .filter { (_, syncId) -> syncId.isNotBlank() }
                                                        .distinct()
                                                        .map { (tabla, syncId) ->
                                                            EliminacionSyncEntity(
                                                                tabla = tabla,
                                                                registroSyncId = syncId,
                                                                organizacionId = ficha.organizacionId,
                                                                creadoEn = ahora
                                                            )
                                                        }
                                                )
                                            }
                                            database.agendaDao().eliminarDeFicha(ficha.id)
                                            database.fichaFamiliarDao().eliminarFicha(ficha)
                                            adjuntos.map { it.uri } + listOfNotNull(ficha.firmaUri)
                                        }
                                    }
                                    idsAgenda.forEach { RecordatorioAgenda.cancelar(context, it) }
                                    archivosAEliminar.forEach { uriTexto ->
                                        val uri = android.net.Uri.parse(uriTexto)
                                        val archivo = uri.path?.let(::File)
                                        val raiz = context.filesDir.canonicalFile
                                        if (uri.scheme == "file" && archivo != null) {
                                            val seguro = archivo.canonicalFile
                                            if (seguro.path.startsWith(raiz.path + File.separator)) seguro.delete()
                                        }
                                    }
                                    ProgramadorSincronizacion.ejecutarAhora(context)
                                    Toast.makeText(context, "Ficha eliminada definitivamente.", Toast.LENGTH_SHORT).show()
                                    fichaSeleccionada = null
                                    fichaIdActual = null
                                    pantallaActual = if (fichaAbiertaDesdeAgenda) "agenda" else "buscarFichas"
                                    fichaAbiertaDesdeAgenda = false
                                }
                            },
                            onRegresar = {
                                pantallaActual = if (fichaAbiertaDesdeAgenda) "agenda" else "buscarFichas"
                                fichaAbiertaDesdeAgenda = false
                            }
                        )
                    } else {
                        pantallaActual = "buscarFichas"
                    }
                }

                "editarUbicacion" -> {
                    val ficha = fichaSeleccionada
                    if (ficha != null) {
                        UbicacionFamiliaScreen(
                            sectorInicial = ficha.sector,
                            datosIniciales = UbicacionFamiliaForm(
                                sector = ficha.sector,
                                manzana = ficha.manzana,
                                numeroFamilia = ficha.numeroFamilia,
                                direccion = ficha.direccionHabitualFamilia,
                                barrio = ficha.barrio,
                                numeroCasa = ficha.numeroCasa,
                                comunidad = ficha.comunidad,
                                grupoCultural = ficha.grupoCultural
                            ),
                            onGuardar = { ubicacion ->
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        database.fichaFamiliarDao().actualizarUbicacion(
                                            fichaId = ficha.id,
                                            sector = ubicacion.sector,
                                            manzana = ubicacion.manzana,
                                            numeroFamilia = ubicacion.numeroFamilia,
                                            direccion = ubicacion.direccion,
                                            barrio = ubicacion.barrio,
                                            numeroCasa = ubicacion.numeroCasa,
                                            comunidad = ubicacion.comunidad,
                                            grupoCultural = ubicacion.grupoCultural,
                                            latitud = ficha.latitud,
                                            longitud = ficha.longitud,
                                            altitud = ficha.altitud,
                                            usuarioId = usuarioActual?.id
                                        )
                                        registrarEvento(
                                            database, ficha.id, ficha.numeroFichaFamiliar,
                                            usuarioActual, "UBICACION_MODIFICADA"
                                        )
                                    }
                                    fichaSeleccionada = ficha.copy(
                                        sector = ubicacion.sector,
                                        manzana = ubicacion.manzana,
                                        numeroFamilia = ubicacion.numeroFamilia,
                                        direccionHabitualFamilia = ubicacion.direccion,
                                        barrio = ubicacion.barrio,
                                        numeroCasa = ubicacion.numeroCasa,
                                        comunidad = ubicacion.comunidad,
                                        grupoCultural = ubicacion.grupoCultural,
                                        actualizadoPorUsuarioId = usuarioActual?.id,
                                        actualizadoEn = System.currentTimeMillis()
                                    )
                                    avanzarFicha("ubicacion")
                                }
                            },
                            onRegresar = {
                                pantallaActual = if (desdeRevision) "revisionFicha" else "menuFicha"
                                desdeRevision = false
                            }
                        )
                    } else {
                        pantallaActual = "buscarFichas"
                    }
                }

                "ubicacion" -> {
                    val fichaId = fichaIdActual
                    val establecimiento = establecimientoSeleccionado
                    if (fichaId != null && establecimiento != null) {
                        UbicacionFamiliaScreen(
                            sectorInicial = establecimiento.sector,
                            datosIniciales = fichaSeleccionada?.takeIf { it.id == fichaId }?.let {
                                UbicacionFamiliaForm(
                                    sector = it.sector,
                                    manzana = it.manzana,
                                    numeroFamilia = it.numeroFamilia,
                                    direccion = it.direccionHabitualFamilia,
                                    barrio = it.barrio,
                                    numeroCasa = it.numeroCasa,
                                    comunidad = it.comunidad,
                                    grupoCultural = it.grupoCultural
                                )
                            },
                            onGuardar = { ubicacion ->
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            database.fichaFamiliarDao().actualizarUbicacion(
                                                fichaId = fichaId,
                                                sector = ubicacion.sector,
                                                manzana = ubicacion.manzana,
                                                numeroFamilia = ubicacion.numeroFamilia,
                                                direccion = ubicacion.direccion,
                                                barrio = ubicacion.barrio,
                                                numeroCasa = ubicacion.numeroCasa,
                                                comunidad = ubicacion.comunidad,
                                                grupoCultural = ubicacion.grupoCultural,
                                                latitud = null,
                                                longitud = null,
                                                altitud = null,
                                                usuarioId = usuarioActual?.id
                                            )
                                            val ficha = database.fichaFamiliarDao().buscarPorId(fichaId)
                                            registrarEvento(
                                                database, fichaId, ficha?.numeroFichaFamiliar.orEmpty(),
                                                usuarioActual, "UBICACION_REGISTRADA"
                                            )
                                        }
                                        fichaSeleccionada = fichaSeleccionada?.copy(
                                            sector = ubicacion.sector,
                                            manzana = ubicacion.manzana,
                                            numeroFamilia = ubicacion.numeroFamilia,
                                            direccionHabitualFamilia = ubicacion.direccion,
                                            barrio = ubicacion.barrio,
                                            numeroCasa = ubicacion.numeroCasa,
                                            comunidad = ubicacion.comunidad,
                                            grupoCultural = ubicacion.grupoCultural,
                                            actualizadoPorUsuarioId = usuarioActual?.id,
                                            actualizadoEn = System.currentTimeMillis()
                                        )
                                        Toast.makeText(
                                            context,
                                            "Borrador guardado correctamente.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        avanzarFicha("ubicacion")
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            "No se pudo guardar la ubicación.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onRegresar = {
                                regresarFicha("ubicacion")
                            }
                        )
                    } else {
                        pantallaActual = "inicio"
                    }
                }

                "miembros" -> {
                    val fichaId = fichaIdActual
                    if (fichaId != null) {
                        MiembrosFamiliaScreen(
                            fichaId = fichaId,
                            usuarioId = usuarioActual?.id ?: 0L,
                            onContinuar = {
                                scope.launch {
                                    fichaSeleccionada = withContext(Dispatchers.IO) {
                                        database.fichaFamiliarDao().buscarPorId(fichaId)
                                    } ?: fichaSeleccionada
                                    avanzarFicha("miembros")
                                }
                            },
                            onSalir = {
                                scope.launch {
                                    fichaSeleccionada = withContext(Dispatchers.IO) {
                                        database.fichaFamiliarDao().buscarPorId(fichaId)
                                    } ?: fichaSeleccionada
                                    regresarFicha("miembros")
                                }
                            },
                            textoRegresar = NavegacionFicha.textoRegresar(modoEdicion, desdeRevision),
                            descripcionRegresar = NavegacionFicha.descripcionRegresar(modoEdicion, desdeRevision)
                        )
                    } else {
                        pantallaActual = "inicio"
                    }
                }

                "saludFamiliar" -> {
                    val fichaId = fichaIdActual
                    if (fichaId != null) {
                        SaludFamiliarScreen(
                            fichaId = fichaId,
                            onContinuar = { avanzarFicha("saludFamiliar") },
                            onSalir = { regresarFicha("saludFamiliar") },
                            textoRegresar = NavegacionFicha.textoRegresar(modoEdicion, desdeRevision),
                            descripcionRegresar = NavegacionFicha.descripcionRegresar(modoEdicion, desdeRevision)
                        )
                    } else {
                        pantallaActual = "inicio"
                    }
                }

                "riesgos" -> {
                    val fichaId = fichaIdActual
                    if (fichaId != null) {
                        RiesgoFamiliarScreen(
                            fichaId = fichaId,
                            responsableActual = usuarioActual?.nombres.orEmpty(),
                            onContinuar = { avanzarFicha("riesgos") },
                            onSalir = { regresarFicha("riesgos") },
                            textoRegresar = NavegacionFicha.textoRegresar(modoEdicion, desdeRevision),
                            descripcionRegresar = NavegacionFicha.descripcionRegresar(modoEdicion, desdeRevision)
                        )
                    } else {
                        pantallaActual = "inicio"
                    }
                }

                "gestionRiesgo" -> {
                    val fichaId = fichaIdActual
                    if (fichaId != null) {
                        GestionRiesgoScreen(
                            fichaId = fichaId,
                            responsableActual = usuarioActual?.nombres.orEmpty(),
                            onContinuar = { avanzarFicha("gestionRiesgo") },
                            onSalir = { regresarFicha("gestionRiesgo") },
                            textoRegresar = NavegacionFicha.textoRegresar(modoEdicion, desdeRevision),
                            descripcionRegresar = NavegacionFicha.descripcionRegresar(modoEdicion, desdeRevision)
                        )
                    } else {
                        pantallaActual = "inicio"
                    }
                }

                "familiograma" -> {
                    val fichaId = fichaIdActual
                    if (fichaId != null) {
                        FamiliogramaScreen(
                            fichaId = fichaId,
                            onContinuar = { avanzarFicha("familiograma") },
                            onSalir = { regresarFicha("familiograma") },
                            textoRegresar = NavegacionFicha.textoRegresar(modoEdicion, desdeRevision),
                            descripcionRegresar = NavegacionFicha.descripcionRegresar(modoEdicion, desdeRevision)
                        )
                    } else pantallaActual = "inicio"
                }

                "croquisMapa" -> {
                    val fichaId = fichaIdActual
                    if (fichaId != null) {
                        CroquisMapaScreen(
                            fichaId = fichaId,
                            usuarioId = usuarioActual?.id,
                            modoEdicion = modoEdicion,
                            fichaInicial = fichaSeleccionada?.takeIf { modoEdicion && it.id == fichaId },
                            onContinuar = { avanzarFicha("croquisMapa") },
                            onRegresar = { regresarFicha("croquisMapa") },
                            textoRegresar = NavegacionFicha.textoRegresar(modoEdicion, desdeRevision),
                            descripcionRegresar = NavegacionFicha.descripcionRegresar(modoEdicion, desdeRevision)
                        )
                    } else pantallaActual = "inicio"
                }

                "contaminacion" -> {
                    val fichaId = fichaIdActual
                    if (fichaId != null) {
                        ContaminacionAmbientalScreen(
                            fichaId = fichaId,
                            onContinuar = { avanzarFicha("contaminacion") },
                            onSalir = { regresarFicha("contaminacion") },
                            textoRegresar = NavegacionFicha.textoRegresar(modoEdicion, desdeRevision),
                            descripcionRegresar = NavegacionFicha.descripcionRegresar(modoEdicion, desdeRevision)
                        )
                    } else pantallaActual = "inicio"
                }

                "tratamiento" -> {
                    val fichaId = fichaIdActual
                    if (fichaId != null) {
                        LugaresTratamientoScreen(
                            fichaId = fichaId,
                            onContinuar = { avanzarFicha("tratamiento") },
                            onSalir = { regresarFicha("tratamiento") },
                            textoRegresar = NavegacionFicha.textoRegresar(modoEdicion, desdeRevision),
                            descripcionRegresar = NavegacionFicha.descripcionRegresar(modoEdicion, desdeRevision)
                        )
                    } else pantallaActual = "inicio"
                }

                "revisionFicha" -> {
                    val fichaId = fichaIdActual
                    if (fichaId != null) {
                        RevisionFinalFichaScreen(
                            fichaId = fichaId,
                            usuarioId = usuarioActual?.id,
                            usuarioNombre = usuarioActual?.nombres.orEmpty(),
                            onAbrirSeccion = { seccion ->
                                scope.launch {
                                    fichaSeleccionada = withContext(Dispatchers.IO) {
                                        database.fichaFamiliarDao().buscarPorId(fichaId)
                                    }
                                    desdeRevision = true
                                    pantallaActual = when (seccion) {
                                        "datos" -> "miembros"
                                        "ubicacion" -> "editarUbicacion"
                                        "miembros" -> "miembros"
                                        "salud" -> "saludFamiliar"
                                        "dispensarizacion" -> "dispensarizacionFicha"
                                        "riesgos" -> "riesgos"
                                        "gestion" -> "gestionRiesgo"
                                        "familiograma" -> "familiograma"
                                        "croquis" -> "croquisMapa"
                                        "contaminacion" -> "contaminacion"
                                        "tratamiento" -> "tratamiento"
                                        else -> "menuFicha"
                                    }
                                }
                            },
                            onFinalizada = { nuevoEstado ->
                                Toast.makeText(
                                    context,
                                    if (nuevoEstado == "COMPLETA") {
                                        "Ficha finalizada correctamente."
                                    } else {
                                        "Ficha guardada como pendiente."
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                                fichaSeleccionada = fichaSeleccionada?.copy(
                                    estado = nuevoEstado,
                                    completadoPorUsuarioId = if (nuevoEstado == "COMPLETA") {
                                        usuarioActual?.id
                                    } else {
                                        fichaSeleccionada?.completadoPorUsuarioId
                                    },
                                    actualizadoPorUsuarioId = usuarioActual?.id,
                                    actualizadoEn = System.currentTimeMillis()
                                )
                                pantallaActual = if (fichaAbiertaDesdeAgenda) "agenda" else "inicio"
                                fichaAbiertaDesdeAgenda = false
                                fichaIdActual = null
                                fichaSeleccionada = null
                                modoEdicion = false
                                desdeRevision = false
                            },
                            onRegresar = {
                                regresarFicha("revisionFicha")
                            }
                        )
                    } else {
                        pantallaActual = "inicio"
                    }
                }

                "historialFicha" -> {
                    val fichaId = fichaIdActual
                    if (fichaId != null) {
                        HistorialFichaScreen(
                            fichaId = fichaId,
                            onRegresar = { pantallaActual = "menuFicha" }
                        )
                    } else {
                        pantallaActual = "buscarFichas"
                    }
                }
                }
            }
            }
        }
    }
}

private fun SalaEntity.comoEstablecimientoLocal(): EstablecimientoSaludEntity =
    EstablecimientoSaludEntity(
        codigoUo = codigoUo,
        nombreCentroSalud = nombreCentroSalud.ifBlank { nombreSala },
        institucionSistema = institucionSistema,
        provinciaCodigoLocalizacion = provinciaCodigoLocalizacion,
        provincia = provincia,
        cantonCodigoLocalizacion = cantonCodigoLocalizacion,
        canton = canton,
        parroquiaCodigoLocalizacion = parroquiaCodigoLocalizacion,
        parroquia = parroquia,
        sector = sector,
        areaNumero = areaNumero
    )
private suspend fun guardarIdentidadProfesional(
    context: Context,
    database: RuralitosDatabase,
    supabase: SupabaseApi,
    usuario: UsuarioEntity,
    codigo: String,
    firma: Bitmap?
): UsuarioEntity {
    val firmaAnterior = usuario.firmaUri
    val firmaUri = withContext(Dispatchers.IO) {
        firma?.let { guardarFirmaProfesional(context, usuario.id, it) } ?: firmaAnterior
    }
    val actualizado = usuario.copy(codigoSenescyt = codigo, firmaUri = firmaUri)
    supabase.actualizarPerfil(
        PerfilRemoto(
            id = actualizado.supabaseId,
            cedula = actualizado.cedula,
            nombres = actualizado.nombres,
            cargo = actualizado.cargo,
            correo = actualizado.correo,
            telefono = actualizado.telefono,
            codigoSenescyt = actualizado.codigoSenescyt
        )
    )
    withContext(Dispatchers.IO) {
        database.withTransaction {
            database.usuarioDao().actualizarCredencialesProfesionales(
                actualizado.id,
                actualizado.codigoSenescyt,
                actualizado.firmaUri
            )
            database.fichaFamiliarDao().aplicarFirmaProfesional(
                usuarioId = actualizado.id,
                codigoAnterior = usuario.codigoSenescyt,
                firmaUri = actualizado.firmaUri
            )
        }
    }
    ProgramadorSincronizacion.ejecutarAhora(context)
    return actualizado
}

private suspend fun prepararUsuarioDesdeSupabase(
    database: RuralitosDatabase,
    supabase: SupabaseApi
): UsuarioEntity? {
    val perfil: PerfilRemoto = supabase.obtenerPerfil()
        ?: throw IllegalStateException("No se encontró el perfil de la cuenta.")
    val salas = SincronizadorSalas.actualizar(database, supabase)
    if (salas.isEmpty()) return null
    val activa = supabase.organizacionGuardada()
        ?.let { id -> salas.firstOrNull { it.organizacionId == id } }
        ?: salas.first()
    supabase.guardarOrganizacionActiva(activa.organizacionId)

    val dao = database.usuarioDao()
    val existente = dao.buscarPorSupabaseId(perfil.id) ?: dao.buscarPorCedula(perfil.cedula)
    val id = if (existente == null) {
        dao.guardar(
            UsuarioEntity(
                cedula = perfil.cedula,
                nombres = perfil.nombres,
                cargo = perfil.cargo,
                rol = activa.rol,
                claveHash = "",
                claveSalt = "",
                correo = perfil.correo,
                telefono = perfil.telefono,
                codigoSenescyt = perfil.codigoSenescyt,
                activo = activa.activa,
                supabaseId = perfil.id,
                organizacionId = activa.organizacionId,
                establecimientoRemotoId = activa.establecimientoId
            )
        )
    } else {
        dao.actualizarDesdeSupabase(
            id = existente.id,
            cedula = perfil.cedula,
            nombres = perfil.nombres,
            cargo = perfil.cargo,
            rol = activa.rol,
            correo = perfil.correo,
            telefono = perfil.telefono,
            codigoSenescyt = perfil.codigoSenescyt,
            activo = activa.activa,
            supabaseId = perfil.id,
            organizacionId = activa.organizacionId,
            establecimientoRemotoId = activa.establecimientoId
        )
        existente.id
    }
    database.sincronizacionDao().asignarOrganizacionPendiente(activa.organizacionId)
    return dao.buscarPorId(id)
}
private suspend fun registrarEvento(
    database: RuralitosDatabase,
    fichaId: Long,
    numeroFicha: String,
    usuario: UsuarioEntity?,
    accion: String,
    detalle: String = ""
) {
    database.historialFichaDao().registrar(
        HistorialFichaEntity(
            fichaId = fichaId,
            numeroFicha = numeroFicha,
            usuarioId = usuario?.id,
            usuarioNombre = usuario?.nombres.orEmpty(),
            accion = accion,
            detalle = detalle
        )
    )
}

@Composable
fun PantallaPrincipal(
    usuarioNombre: String,
    usuarioCargo: String,
    codigoSenescyt: String,
    fichasPendientesSync: Int,
    estadoSincronizacion: String,
    onNuevaFicha: () -> Unit,
    onBuscarFicha: () -> Unit,
    onSala: () -> Unit,
    onDispensarizacion: () -> Unit,
    onNotasDiarias: () -> Unit,
    onAgenda: () -> Unit,
    onEstadisticas: () -> Unit,
    onPerfil: () -> Unit,
    onCredenciales: () -> Unit,
    onSeguridad: () -> Unit,
    onEliminarCuenta: () -> Unit,
    onCambiarClave: () -> Unit,
    onCerrarSesion: () -> Unit,
    usuarioId: Long = 0L
)  {
    InicioRuralitosScreen(
        usuarioNombre = usuarioNombre,
        usuarioCargo = usuarioCargo,
        codigoSenescyt = codigoSenescyt,
        fichasPendientesSync = fichasPendientesSync,
        estadoSincronizacion = estadoSincronizacion,
        onNuevaFicha = onNuevaFicha,
        onBuscarFicha = onBuscarFicha,
        onSala = onSala,
        onDispensarizacion = onDispensarizacion,
        onNotasDiarias = onNotasDiarias,
        onAgenda = onAgenda,
        onEstadisticas = onEstadisticas,
        onPerfil = onPerfil,
        onCredenciales = onCredenciales,
        onSeguridad = onSeguridad,
        onEliminarCuenta = onEliminarCuenta,
        onCambiarClave = onCambiarClave,
        onCerrarSesion = onCerrarSesion,
        usuarioId = usuarioId
    )
}

@Composable
fun AnimacionInicio() {
    var iniciarAnimacion by remember { mutableStateOf(false) }

    val escala by animateFloatAsState(
        targetValue = if (iniciarAnimacion) 1.1f else 0.7f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "escalaLogo"
    )

    val transparencia by animateFloatAsState(
        targetValue = if (iniciarAnimacion) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000
        ),
        label = "transparenciaLogo"
    )

    val colorFondo by animateColorAsState(
        targetValue = if (iniciarAnimacion) VerdeFondo else VerdeRuralitos,
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "colorFondoInicio"
    )

    LaunchedEffect(Unit) {
        iniciarAnimacion = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorFondo)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ruralitos",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = VerdeOscuro,
            modifier = Modifier
                .fillMaxWidth()
                .scale(escala)
                .alpha(transparencia),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

    }
}

@Composable
fun MenuPrincipal(
    usuarioNombre: String,
    esAdministrador: Boolean,
    onNuevaFicha: () -> Unit,
    onBuscarFicha: () -> Unit,
    onSala: () -> Unit,
    onConsolidado: () -> Unit,
    onUsuarios: () -> Unit,
    onSeguridad: () -> Unit,
    onCambiarClave: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var seccionActual by remember { mutableStateOf("menu") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ruralitos",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = VerdeOscuro,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Text(
            text = if (seccionActual == "menu") "Menú principal" else "Fichas familiares",
            fontSize = 18.sp,
            color = TextoOscuro,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 24.dp),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Sesión: $usuarioNombre",
            style = MaterialTheme.typography.bodySmall,
            color = TextoOscuro,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            textAlign = TextAlign.Center
        )

        if (seccionActual == "menu") {
            TarjetaMenu(
                titulo = "Fichas familiares",
                descripcion = "Crear, buscar y revisar fichas familiares",
                color = VerdeRuralitos,
                onClick = {
                    seccionActual = "fichas"
                }
            )

            if (esAdministrador) {
                TarjetaMenu(
                    titulo = "Usuarios locales",
                    descripcion = "Crear o desactivar cuentas del personal de salud",
                    color = AzulVivo,
                    onClick = onUsuarios
                )

                TarjetaMenu(
                    titulo = "Seguridad y respaldos",
                    descripcion = "Crear o restaurar una copia cifrada de toda la información",
                    color = NaranjaVivo,
                    onClick = onSeguridad
                )
            }

            TarjetaMenu(
                titulo = "Cambiar mi contraseña",
                descripcion = "Actualizar la contraseña de la sesión actual",
                color = VerdeOscuro,
                onClick = onCambiarClave
            )

            Button(
                onClick = onCerrarSesion,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Cerrar sesión")
            }
        } else if (seccionActual == "fichas") {
            TarjetaMenu(
                titulo = "Nueva ficha familiar",
                descripcion = "Crear una nueva ficha familiar desde cero",
                color = VerdeRuralitos,
                onClick = {
                    onNuevaFicha()
                }
            )

            TarjetaMenu(
                titulo = "Buscar o modificar ficha",
                descripcion = "Consultar una ficha familiar guardada y editarla",
                color = AzulVivo,
                onClick = onBuscarFicha
            )

            TarjetaMenu(
                titulo = "Registro general",
                descripcion = "Dispensarización y análisis",
                color = NaranjaVivo,
                onClick = onConsolidado
            )

            Button(
                onClick = {
                    seccionActual = "menu"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(text = "Regresar")
            }
        }
    }
}

@Composable
fun TarjetaMenu(
    titulo: String,
    descripcion: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = titulo,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = descripcion,
                fontSize = 15.sp,
                color = TextoOscuro,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
