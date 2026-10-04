package com.ruralitos.app.ui.screens

import com.ruralitos.app.ui.components.EncabezadoPantallaRuralitos
import com.ruralitos.app.ui.components.VentanaConfirmarRuralitos
import com.ruralitos.app.ui.components.FlechaDesplegable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.EaisSalaEntity
import com.ruralitos.app.data.local.entity.SalaEntity
import com.ruralitos.app.data.local.entity.TerritorioSalaEntity
import com.ruralitos.app.data.remote.SupabaseApi
import com.ruralitos.app.data.remote.AccesoRecibido
import com.ruralitos.app.data.remote.FichaCompartidaCon
import com.ruralitos.app.data.sync.SincronizadorSupabase
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.ruralitos.app.ui.components.VentanaRuralitos
import com.ruralitos.app.data.remote.AccesoOtorgado
import com.ruralitos.app.data.sync.SincronizadorSalas
import com.ruralitos.app.data.sync.ProgramadorSincronizacion
import com.ruralitos.app.ui.components.BotonVolverRuralitos
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.SubmenuRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.components.SelectorAlcanceRuralitos
import com.ruralitos.app.ui.components.TarjetaCodigoRuralitos
import com.ruralitos.app.ui.components.TextoAjustado
import com.ruralitos.app.ui.components.rememberEstadoAlcance
import com.ruralitos.app.domain.AlcanceFichas
import com.ruralitos.app.domain.CatalogoAlcance
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.ruralitos.app.R
import kotlinx.coroutines.launch

private enum class SeccionSala(val etiqueta: String) {
    CENTROS("Centros"),
    BARRIOS("EAIS y barrios"),
    ACCESOS("Compartir acceso")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SalaScreen(
    database: RuralitosDatabase,
    supabase: SupabaseApi,
    onAgregarCentro: () -> Unit,
    onRegresar: () -> Unit,
    /** Quien usa la app: solo puede compartir las fichas que él mismo creó. */
    usuarioId: Long = -1L
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var seccion by remember { mutableStateOf(SeccionSala.CENTROS) }
    var recarga by remember { mutableIntStateOf(0) }
    var salas by remember { mutableStateOf<List<SalaEntity>>(emptyList()) }
    var eais by remember { mutableStateOf<List<EaisSalaEntity>>(emptyList()) }
    var territorios by remember { mutableStateOf<List<TerritorioSalaEntity>>(emptyList()) }
    var salaId by remember { mutableStateOf(supabase.organizacionGuardada().orEmpty()) }
    var eaisId by remember { mutableStateOf("") }
    var territorioId by remember { mutableStateOf("") }
    var numeroEais by remember { mutableStateOf("") }
    var eaisEditandoId by remember { mutableStateOf<String?>(null) }
    var eaisAEliminar by remember { mutableStateOf<EaisSalaEntity?>(null) }
    var nombreTerritorio by remember { mutableStateOf("") }
    var territorioEditandoId by remember { mutableStateOf<String?>(null) }
    var territorioAEliminar by remember { mutableStateOf<TerritorioSalaEntity?>(null) }
    val estadoAlcance = rememberEstadoAlcance()
    var permiso by remember { mutableStateOf("EDITOR") }
    var correo by remember { mutableStateOf("") }
    var codigoIngreso by remember { mutableStateOf("") }
    var codigoGenerado by remember { mutableStateOf("") }
    var procesando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (supabase.hayInternet()) {
            runCatching { SincronizadorSalas.actualizar(database, supabase) }
                .onFailure { mensaje = it.message ?: "No se pudo actualizar la Sala." }
        }
        salas = database.salaDao().listarSalas()
        if (salas.none { it.organizacionId == salaId }) {
            salaId = salas.firstOrNull()?.organizacionId.orEmpty()
        }
        recarga++
    }
    LaunchedEffect(salaId, recarga) {
        if (salaId.isNotBlank()) {
            supabase.guardarOrganizacionActiva(salaId)
            eais = database.salaDao().listarEais(salaId)
        } else eais = emptyList()
        if (eais.none { it.id == eaisId }) eaisId = eais.firstOrNull()?.id.orEmpty()
    }
    LaunchedEffect(eaisId, recarga) {
        territorios = if (eaisId.isBlank()) emptyList() else database.salaDao().listarTerritorios(eaisId)
        if (territorios.none { it.id == territorioId }) {
            territorioId = territorios.firstOrNull()?.id.orEmpty()
        }
    }

    val sala = salas.firstOrNull { it.organizacionId == salaId }
    val eaisElegido = eais.firstOrNull { it.id == eaisId }
    val territorio = territorios.firstOrNull { it.id == territorioId }
    val puedeAdministrar = sala?.permiso == "ADMINISTRADOR" || sala?.rol == "ADMINISTRADOR"
    val todasLasFichas by remember(database) { database.fichaFamiliarDao().listarFichas() }
        .collectAsState(initial = emptyList())
    val todosLosEais by remember(database) { database.salaDao().observarTodosEais() }
        .collectAsState(initial = emptyList())
    val todosLosBarrios by remember(database) { database.salaDao().observarTodosTerritorios() }
        .collectAsState(initial = emptyList())
    // Todos los usuarios comparten por igual, pero solo lo suyo: sus propias fichas.
    val catalogoCompartir = CatalogoAlcance(
        salas = salas,
        eais = todosLosEais,
        territorios = todosLosBarrios,
        fichas = todasLasFichas.filter { it.creadoPorUsuarioId == usuarioId },
        salaActivaId = salaId
    )

    fun ejecutar(
        mensajeExito: String = "Información actualizada correctamente.",
        sincronizarSalas: Boolean = false,
        accion: suspend () -> Unit
    ) {
        if (procesando) return
        procesando = true
        mensaje = null
        scope.launch {
            runCatching {
                accion()
                if (sincronizarSalas && supabase.hayInternet()) {
                    SincronizadorSalas.actualizar(database, supabase)
                    salas = database.salaDao().listarSalas()
                }
            }.onSuccess {
                recarga++
                mensaje = mensajeExito
                if (supabase.hayInternet()) ProgramadorSincronizacion.ejecutarAhora(context)
            }.onFailure {
                mensaje = it.message ?: "No se pudo completar la operación."
            }
            procesando = false
        }
    }

    territorioAEliminar?.let { item ->
        VentanaConfirmarRuralitos(
            titulo = "Eliminar ${item.etiqueta.lowercase()}",
            mensaje = "${item.nombre} dejará de aparecer para nuevas fichas. " +
                                "Las fichas históricas conservarán su información.",
            textoConfirmar = "Sí, eliminar",
            confirmarHabilitado = !procesando,
            onConfirmar = {
                                territorioAEliminar = null
                                ejecutar("Barrio eliminado correctamente.") {
                                    val remoto = supabase.desactivarTerritorio(salaId, item.id)
                                    database.salaDao().guardarTerritorio(
                                        item.copy(
                                            tipo = remoto.tipo,
                                            nombre = remoto.nombre,
                                            activo = remoto.activo,
                                            actualizadoEn = System.currentTimeMillis()
                                        )
                                    )
                                    if (territorioId == item.id) territorioId = ""
                                    if (territorioEditandoId == item.id) {
                                        territorioEditandoId = null
                                        nombreTerritorio = ""
                                    }
                                }
                            },
            textoCancelar = "Cancelar",
            peligro = true,
            onCancelar = { territorioAEliminar = null }
        )
    }

    eaisAEliminar?.let { item ->
        VentanaConfirmarRuralitos(
            titulo = "Eliminar ${item.nombre}",
            mensaje = "El EAIS dejará de estar disponible para nuevas fichas. Las fichas existentes no se borran y podrán seguir editándose.",
            textoConfirmar = "Sí, desactivar",
            confirmarHabilitado = !procesando,
            onConfirmar = {
                            eaisAEliminar = null
                            ejecutar("EAIS desactivado correctamente. Las fichas históricas se conservaron.") {
                                val remoto = supabase.desactivarEais(salaId, item.id)
                                database.salaDao().guardarEais(item.copy(activo = remoto.activo, actualizadoEn = System.currentTimeMillis()))
                                database.salaDao().desactivarTerritoriosDeEais(item.id)
                                eaisId = ""
                                territorioId = ""
                                eaisEditandoId = null
                                numeroEais = ""
                            }
                        },
            textoCancelar = "Cancelar",
            peligro = true,
            onCancelar = { eaisAEliminar = null }
        )
    }
// Estados exclusivamente visuales de paneles desplegables.
    // No modifican la lógica de negocio ni las operaciones existentes.
    var centrosAbierto by remember { mutableStateOf(true) }
    var eaisAbierto by remember { mutableStateOf(true) }
    var barriosAbierto by remember { mutableStateOf(false) }
    // Dentro de «Compartir acceso»: 0 = compartir (elegir, permiso y código), 1 = ingresar un código recibido.
    var subAcceso by remember { mutableStateOf(0) }
    var otorgados by remember { mutableStateOf<List<AccesoOtorgado>>(emptyList()) }
    var cargandoOtorgados by remember { mutableStateOf(false) }
    var errorOtorgados by remember { mutableStateOf<String?>(null) }
    var recargaOtorgados by remember { mutableIntStateOf(0) }
    var porQuitar by remember { mutableStateOf<AccesoOtorgado?>(null) }
    LaunchedEffect(seccion, subAcceso, recargaOtorgados) {
        if (seccion != SeccionSala.ACCESOS || subAcceso != 2) return@LaunchedEffect
        if (!supabase.hayInternet()) {
            errorOtorgados = "Necesitas internet para ver y quitar accesos."
            return@LaunchedEffect
        }
        cargandoOtorgados = true
        errorOtorgados = null
        runCatching { supabase.listarAccesosOtorgados() }
            .onSuccess { otorgados = it }
            .onFailure { errorOtorgados = it.message ?: "No se pudo cargar la lista." }
        cargandoOtorgados = false
    }
    // Detalle de «Compartido con»: las fichas sueltas que le di a una persona, para quitárselas de una en una.
    var fichasAbiertasDe by remember { mutableStateOf<String?>(null) }
    var fichasSueltas by remember { mutableStateOf<List<FichaCompartidaCon>>(emptyList()) }
    var cargandoSueltas by remember { mutableStateOf(false) }
    var fichaPorQuitar by remember { mutableStateOf<Pair<AccesoOtorgado, FichaCompartidaCon>?>(null) }
    LaunchedEffect(fichasAbiertasDe, recargaOtorgados) {
        val usuario = fichasAbiertasDe ?: return@LaunchedEffect
        cargandoSueltas = true
        fichasSueltas = emptyList()
        runCatching { supabase.fichasCompartidasCon(usuario) }.onSuccess { fichasSueltas = it }
        cargandoSueltas = false
    }
    fichaPorQuitar?.let { (persona, ficha) ->
        VentanaConfirmarRuralitos(
            titulo = "Quitar acceso a esta ficha",
            mensaje = "${persona.nombre} dejará de ver la ficha de ${ficha.jefe.ifBlank { "esta familia" }}. " +
                "Las demás fichas que le compartiste no cambian.",
            textoConfirmar = "Quitar acceso",
            confirmarHabilitado = !procesando,
            onConfirmar = {
                fichaPorQuitar = null
                ejecutar("Se quitó el acceso a la ficha correctamente.") {
                    supabase.quitarAccesoFicha(ficha.fichaId, persona.usuarioId)
                    recargaOtorgados++
                }
            },
            textoCancelar = "Cancelar",
            peligro = true,
            onCancelar = { fichaPorQuitar = null }
        )
    }

    // Botón redondo de «Compartido con»: yo mismo me quito el acceso a lo que otras personas me compartieron.
    var recibidosAbierto by remember { mutableStateOf(false) }
    var recibidos by remember { mutableStateOf<List<AccesoRecibido>>(emptyList()) }
    var cargandoRecibidos by remember { mutableStateOf(false) }
    var errorRecibidos by remember { mutableStateOf<String?>(null) }
    var recargaRecibidos by remember { mutableIntStateOf(0) }
    var porQuitarme by remember { mutableStateOf<AccesoRecibido?>(null) }
    LaunchedEffect(recibidosAbierto, recargaRecibidos) {
        if (!recibidosAbierto) return@LaunchedEffect
        if (!supabase.hayInternet()) {
            errorRecibidos = "Necesitas internet para ver y quitarte accesos."
            return@LaunchedEffect
        }
        cargandoRecibidos = true
        errorRecibidos = null
        runCatching { supabase.listarAccesosRecibidos() }
            .onSuccess { recibidos = it }
            .onFailure { errorRecibidos = it.message ?: "No se pudo cargar la lista." }
        cargandoRecibidos = false
    }
    if (recibidosAbierto) {
        VentanaRuralitos(
            titulo = "Lo que me compartieron",
            subtitulo = "Tú también puedes quitarte el acceso, sin pedirle permiso a quien te compartió.",
            simbolo = "↩",
            color = RojoClinico,
            onCerrar = { recibidosAbierto = false },
            contenido = {
                when {
                    cargandoRecibidos -> Text("Cargando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    errorRecibidos != null -> MensajeEstadoRuralitos(
                        titulo = "No se pudo cargar la lista",
                        descripcion = errorRecibidos.orEmpty(),
                        color = NaranjaClinico,
                        simbolo = "!"
                    )
                    recibidos.isEmpty() -> Text(
                        "Nadie te ha compartido fichas.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("sin_accesos_recibidos")
                    )
                    else -> recibidos.forEach { persona ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(persona.nombre, fontWeight = FontWeight.SemiBold)
                                listOf(persona.cargo, persona.correo).filter { it.isNotBlank() }.joinToString(" · ")
                                    .takeIf { it.isNotBlank() }?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                Text(
                                    persona.resumen + " · " + if (persona.permiso == "LECTOR") "Solo lectura" else "Puede editar",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MoradoClinico,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            TextButton(
                                onClick = { porQuitarme = persona },
                                enabled = !procesando,
                                modifier = Modifier.testTag("quitarme_${persona.autorId}")
                            ) { Text("Quitarme", color = RojoClinico, fontWeight = FontWeight.SemiBold) }
                        }
                        HorizontalDivider()
                    }
                }
            },
            acciones = {
                BotonSecundarioRuralitos(texto = "Cerrar", onClick = { recibidosAbierto = false })
            }
        )
    }
    porQuitarme?.let { persona ->
        VentanaConfirmarRuralitos(
            titulo = "Quitarme el acceso",
            mensaje = "Dejarás de ver las fichas que ${persona.nombre} te compartió y se borrarán de este teléfono. " +
                "Tus propias fichas no cambian. Si ${persona.nombre} te vuelve a compartir con un código nuevo, recuperarás el acceso.",
            textoConfirmar = "Quitarme el acceso",
            confirmarHabilitado = !procesando,
            onConfirmar = {
                porQuitarme = null
                ejecutar("Te quitaste el acceso correctamente.") {
                    supabase.quitarMiAcceso(persona.organizacionId, persona.autorId)
                    SincronizadorSupabase(context).retirarFichasDeAutorSinAcceso(persona.autorId)
                    recargaRecibidos++
                }
            },
            textoCancelar = "Cancelar",
            peligro = true,
            onCancelar = { porQuitarme = null }
        )
    }

    porQuitar?.let { persona ->
        VentanaConfirmarRuralitos(
            titulo = "Quitar acceso",
            mensaje = "${persona.nombre} dejará de ver las fichas que le compartiste. Tus fichas siguen siendo tuyas y, " +
                "si luego le generas otro código, recuperará el acceso.",
            textoConfirmar = "Quitar acceso",
            confirmarHabilitado = !procesando,
            onConfirmar = {
                porQuitar = null
                ejecutar("Se quitó el acceso correctamente.") {
                    supabase.quitarAccesoCompartido(persona.organizacionId, persona.usuarioId)
                    recargaOtorgados++
                }
            },
            textoCancelar = "Cancelar",
            peligro = true,
            onCancelar = { porQuitar = null }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F9FB))
    ) {
        Column(
            // Con el teclado abierto la pantalla se acorta y el desplazamiento sigue llegando a todos los campos.
            modifier = Modifier.fillMaxSize().formularioSeguro()
        ) {
            EncabezadoPantallaRuralitos(
                titulo = "Mis Salas",
                subtitulo = null,
                paso = null,
                totalPasos = null,
                etiquetaPaso = "",
                onVolver = onRegresar,
                descripcion = "Centros, barrios y accesos organizados por sección."
            )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

            // Contenedor blanco superpuesto al paisaje, con la forma escogida por el usuario:
            // sin onda, solo esquinas superiores grandes y redondeadas.
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                color = Color(0xFFF6F9FB),
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                ),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 22.dp,
                            bottom = 22.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TabsSalaModernas(
                        seleccionada = seccion,
                        onSeleccionar = { seccion = it }
                    )

                    when (seccion) {
                        SeccionSala.CENTROS -> {
                            PanelDesplegableSala(
                                titulo = "Centros de salud vinculados",
                                descripcion = "Puedes trabajar en más de un centro con la misma cuenta.",
                                simbolo = "S",
                                color = AzulClinico,
                                abierto = centrosAbierto,
                                onCambiar = { centrosAbierto = !centrosAbierto }
                            ) {
                                if (salas.isEmpty()) {
                                    Text(
                                        "Todavía no hay centros vinculados.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    salas.forEach { item ->
                                        FilterChip(
                                            selected = item.organizacionId == salaId,
                                            onClick = { salaId = item.organizacionId },
                                            label = {
                                                Text(
                                                    item.nombreCentroSalud.ifBlank { item.nombreSala },
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AzulClinico.copy(alpha = 0.13f),
                                                selectedLabelColor = AzulClinico
                                            )
                                        )
                                    }
                                }

                                BotonPrincipalRuralitos(
                                    texto = "Agregar otro centro de salud",
                                    descripcion = "Buscar la unidad y crear una Sala nueva",
                                    onClick = onAgregarCentro,
                                    color = AzulClinico,
                                    modifier = Modifier.padding(top = 8.dp)
                                )

                                sala?.let {
                                    MensajeEstadoRuralitos(
                                        titulo = it.nombreCentroSalud.ifBlank { it.nombreSala },
                                        descripcion = listOf(
                                            "Código UO: ${it.codigoUo.ifBlank { "sin código" }}",
                                            it.canton,
                                            "Permiso: ${it.permiso.lowercase()}"
                                        ).filter(String::isNotBlank).joinToString(" · "),
                                        color = CianRuralitos,
                                        simbolo = "S",
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }

                        SeccionSala.BARRIOS -> {
                            PanelDesplegableSala(
                                titulo = "EAIS del centro",
                                descripcion = "Selecciona un equipo para ver y administrar sus barrios.",
                                simbolo = "E",
                                color = CianRuralitos,
                                abierto = eaisAbierto,
                                onCambiar = { eaisAbierto = !eaisAbierto }
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    eais.forEach { item ->
                                        FilterChip(
                                            selected = item.id == eaisId,
                                            onClick = { eaisId = item.id },
                                            label = {
                                                Text(
                                                    item.nombre,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CianRuralitos.copy(alpha = 0.13f),
                                                selectedLabelColor = CianRuralitos
                                            )
                                        )
                                    }
                                }

                                if (puedeAdministrar) {
                                    if (eaisElegido != null && eaisEditandoId == null) {
                                        BotonSecundarioRuralitos(
                                            texto = "Modificar ${eaisElegido.nombre}",
                                            onClick = {
                                                eaisEditandoId = eaisElegido.id
                                                numeroEais = eaisElegido.numero.toString()
                                            },
                                            modifier = Modifier.padding(top = 8.dp)
                                        )

                                        BotonPrincipalRuralitos(
                                            texto = "Eliminar ${eaisElegido.nombre}",
                                            color = RojoClinico,
                                            onClick = { eaisAEliminar = eaisElegido },
                                            enabled = !procesando,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = numeroEais,
                                        onValueChange = { numeroEais = it.filter(Char::isDigit).take(3) },
                                        label = {
                                            Text(
                                                if (eaisEditandoId == null) {
                                                    "Número del nuevo EAIS"
                                                } else {
                                                    "Nuevo número del EAIS"
                                                }
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    )

                                    BotonPrincipalRuralitos(
                                        texto = if (eaisEditandoId == null) {
                                            "Agregar EAIS"
                                        } else {
                                            "Guardar número del EAIS"
                                        },
                                        onClick = {
                                            val numero = numeroEais.toIntOrNull()
                                                ?: return@BotonPrincipalRuralitos
                                            val idEditado = eaisEditandoId
                                            ejecutar(
                                                if (idEditado == null) {
                                                    "EAIS agregado correctamente."
                                                } else {
                                                    "EAIS modificado correctamente."
                                                }
                                            ) {
                                                val remoto = if (idEditado == null) {
                                                    supabase.crearEais(salaId, numero)
                                                } else {
                                                    supabase.actualizarEais(
                                                        salaId,
                                                        idEditado,
                                                        numero
                                                    )
                                                }

                                                database.salaDao().guardarEais(
                                                    EaisSalaEntity(
                                                        id = remoto.id,
                                                        salaId = remoto.organizacionId,
                                                        numero = remoto.numero,
                                                        activo = remoto.activo,
                                                        actualizadoEn = System.currentTimeMillis()
                                                    )
                                                )

                                                eaisId = remoto.id
                                                numeroEais = ""
                                                eaisEditandoId = null
                                            }
                                        },
                                        enabled = !procesando && numeroEais.toIntOrNull() != null,
                                        color = CianRuralitos,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )

                                    if (eaisEditandoId != null) {
                                        BotonSecundarioRuralitos(
                                            texto = "Cancelar modificación",
                                            onClick = {
                                                eaisEditandoId = null
                                                numeroEais = ""
                                            },
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }

                            PanelDesplegableSala(
                                titulo = "Barrios",
                                descripcion = eaisElegido?.let {
                                    "Barrios asignados al ${it.nombre}."
                                } ?: "Selecciona primero un EAIS.",
                                simbolo = "B",
                                color = NaranjaClinico,
                                abierto = barriosAbierto,
                                onCambiar = { barriosAbierto = !barriosAbierto }
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    territorios.forEach { item ->
                                        FilterChip(
                                            selected = item.id == territorioId,
                                            onClick = { territorioId = item.id },
                                            label = {
                                                Text(
                                                    "${item.etiqueta}: ${item.nombre}",
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AzulClinico.copy(alpha = 0.10f),
                                                selectedLabelColor = AzulClinico
                                            )
                                        )
                                    }
                                }

                                if (
                                    puedeAdministrar &&
                                    territorio != null &&
                                    territorioEditandoId == null
                                ) {
                                    MensajeEstadoRuralitos(
                                        titulo = "Barrio: ${territorio.nombre}",
                                        descripcion = "Puedes corregir su nombre o desactivarlo.",
                                        color = NaranjaClinico,
                                        simbolo = "T",
                                        modifier = Modifier.padding(top = 8.dp)
                                    )

                                    BotonSecundarioRuralitos(
                                        texto = "Editar barrio seleccionado",
                                        descripcion = "Corregir el nombre del barrio",
                                        enabled = !procesando,
                                        onClick = {
                                            territorioEditandoId = territorio.id
                                            nombreTerritorio = territorio.nombre
                                        },
                                        modifier = Modifier.padding(top = 8.dp)
                                    )

                                    BotonPrincipalRuralitos(
                                        texto = "Eliminar barrio seleccionado",
                                        descripcion = "Ocultarlo sin afectar las fichas históricas",
                                        enabled = !procesando,
                                        color = RojoClinico,
                                        onClick = {
                                            territorioAEliminar = territorio
                                        },
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }

                                if (puedeAdministrar && eaisElegido != null) {
                                    OutlinedTextField(
                                        value = nombreTerritorio,
                                        onValueChange = {
                                            nombreTerritorio = it.take(80)
                                        },
                                        label = {
                                            Text(
                                                if (territorioEditandoId == null) {
                                                    "Nombre del barrio"
                                                } else {
                                                    "Corregir nombre"
                                                }
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    )

                                    BotonPrincipalRuralitos(
                                        texto = if (territorioEditandoId == null) {
                                            "Agregar barrio"
                                        } else {
                                            "Guardar cambios del barrio"
                                        },
                                        descripcion = if (territorioEditandoId == null) {
                                            "Añadirlo al ${eaisElegido.nombre}"
                                        } else {
                                            "Conservar las fichas asociadas y corregir este registro"
                                        },
                                        onClick = {
                                            val idEditado = territorioEditandoId
                                            ejecutar(
                                                if (idEditado == null) {
                                                    "Barrio agregado correctamente."
                                                } else {
                                                    "Barrio editado correctamente."
                                                }
                                            ) {
                                                val remoto = if (idEditado == null) {
                                                    supabase.crearTerritorio(
                                                        salaId,
                                                        eaisId,
                                                        TerritorioSalaEntity.TIPO_BARRIO,
                                                        nombreTerritorio
                                                    )
                                                } else {
                                                    supabase.actualizarTerritorio(
                                                        salaId,
                                                        idEditado,
                                                        TerritorioSalaEntity.TIPO_BARRIO,
                                                        nombreTerritorio
                                                    )
                                                }

                                                database.salaDao().guardarTerritorio(
                                                    TerritorioSalaEntity(
                                                        id = remoto.id,
                                                        salaId = remoto.organizacionId,
                                                        eaisId = remoto.eaisId,
                                                        tipo = remoto.tipo,
                                                        nombre = remoto.nombre,
                                                        activo = remoto.activo,
                                                        actualizadoEn = System.currentTimeMillis()
                                                    )
                                                )

                                                territorioId = remoto.id
                                                territorioEditandoId = null
                                                nombreTerritorio = ""
                                            }
                                        },
                                        enabled = !procesando &&
                                            nombreTerritorio.trim().length >= 2,
                                        color = if (territorioEditandoId == null) {
                                            NaranjaClinico
                                        } else {
                                            CianRuralitos
                                        },
                                        modifier = Modifier.padding(top = 8.dp)
                                    )

                                    if (territorioEditandoId != null) {
                                        BotonSecundarioRuralitos(
                                            texto = "Cancelar edición",
                                            descripcion = "No aplicar cambios al barrio",
                                            enabled = !procesando,
                                            onClick = {
                                                territorioEditandoId = null
                                                nombreTerritorio = ""
                                            },
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        SeccionSala.ACCESOS -> {
                            SubTabsAcceso(
                                opciones = listOf(
                                    "Compartir" to "subtab_compartir",
                                    "Ingresar un código" to "subtab_ingresar",
                                    "Compartido con" to "subtab_compartido"
                                ),
                                seleccionada = subAcceso,
                                onSeleccionar = { subAcceso = it; mensaje = null }
                            )
                            if (subAcceso == 0) {
                            SelectorAlcanceRuralitos(
                                estado = estadoAlcance,
                                catalogo = catalogoCompartir,
                                titulo = "1. Qué quieres compartir",
                                color = MoradoClinico,
                                soloSincronizadas = true,
                                prefijoPrueba = "compartir"
                            )

                            SeccionFormularioRuralitos(
                                titulo = "2. Qué puede hacer la otra persona",
                                descripcion = "Se comparten solo las fichas que tú creaste. El código se usa una sola vez y caduca en 12 horas."
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "LECTOR" to "Solo lectura",
                                        "EDITOR" to "Puede editar"
                                    ).forEach { (valor, etiqueta) ->
                                        FilterChip(
                                            selected = permiso == valor,
                                            onClick = { permiso = valor },
                                            modifier = Modifier.testTag("permiso_$valor"),
                                            label = {
                                                Text(
                                                    etiqueta,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AzulClinico.copy(alpha = 0.12f),
                                                selectedLabelColor = AzulClinico
                                            )
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = correo,
                                    onValueChange = { correo = it.take(120) },
                                    label = {
                                        Text("Correo autorizado (opcional)")
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp)
                                        .testTag("compartir_correo")
                                )

                                BotonPrincipalRuralitos(
                                    texto = if (procesando) {
                                        "Generando código…"
                                    } else {
                                        "Generar código de verificación"
                                    },
                                    onClick = {
                                        val concesiones = AlcanceFichas.concesiones(
                                            estadoAlcance.nivel, estadoAlcance.elegidos, catalogoCompartir
                                        )
                                        if (concesiones.isEmpty()) {
                                            mensaje = "Elige al menos una opción de la lista."
                                            return@BotonPrincipalRuralitos
                                        }
                                        ejecutar("Código creado correctamente.") {
                                            val creado = supabase.crearCodigoAccesoVarios(
                                                concesiones = concesiones,
                                                permiso = permiso,
                                                correo = correo
                                            )
                                            codigoGenerado = creado.codigo
                                        }
                                    },
                                    enabled = !procesando &&
                                        AlcanceFichas.hayElegidos(estadoAlcance.nivel, estadoAlcance.elegidos),
                                    color = MoradoClinico,
                                    modifier = Modifier.padding(top = 8.dp).testTag("generar_codigo")
                                )

                                if (codigoGenerado.isNotBlank()) {
                                    TarjetaCodigoRuralitos(
                                        codigo = codigoGenerado,
                                        color = MoradoClinico,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }

                            } else if (subAcceso == 1) {
                            SeccionFormularioRuralitos(
                                titulo = "Ingresar con un código",
                                descripcion = "Únete a una Sala o recibe acceso a un centro, EAIS, barrio o ficha. Pega aquí el código que te enviaron."
                            ) {
                                OutlinedTextField(
                                    value = codigoIngreso,
                                    onValueChange = {
                                        // Si se pega el mensaje completo del chat, se toma solo el código de 16 letras y números.
                                        val encontrado = if (it.length > 20) {
                                            Regex("(?<![0-9A-Za-z])[0-9A-Fa-f]{16}(?![0-9A-Za-z])")
                                                .find(it)?.value
                                        } else null
                                        codigoIngreso = (encontrado ?: it)
                                            .uppercase()
                                            .filter(Char::isLetterOrDigit)
                                            .take(32)
                                    },
                                    label = {
                                        Text(
                                            "Código de asignación o verificación"
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("codigo_ingreso")
                                )

                                BotonPrincipalRuralitos(
                                    texto = "Verificar y agregar acceso",
                                    onClick = {
                                        ejecutar(
                                            mensajeExito =
                                                "Acceso agregado correctamente.",
                                            sincronizarSalas = true
                                        ) {
                                            supabase.aceptarInvitacion(
                                                codigoIngreso
                                            )
                                            codigoIngreso = ""
                                        }
                                    },
                                    enabled = !procesando &&
                                        codigoIngreso.length >= 8,
                                    color = AzulClinico,
                                    modifier = Modifier.padding(top = 8.dp).testTag("verificar_codigo")
                                )
                            }
                            } else {
                            SeccionFormularioRuralitos(
                                titulo = "Personas con acceso a tus fichas",
                                descripcion = "Ellas pueden ver (o editar) las fichas que tú compartiste. Tú decides cuándo quitarles el acceso. " +
                                    "Con el botón rojo redondo de abajo puedes quitarte tú el acceso a lo que otras personas te compartieron."
                            ) {
                                when {
                                    cargandoOtorgados -> Text("Cargando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    errorOtorgados != null -> MensajeEstadoRuralitos(
                                        titulo = "No se pudo cargar la lista",
                                        descripcion = errorOtorgados.orEmpty(),
                                        color = NaranjaClinico,
                                        simbolo = "!"
                                    )
                                    otorgados.isEmpty() -> Text(
                                        "Todavía no compartiste tus fichas con nadie.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag("sin_accesos_otorgados")
                                    )
                                    else -> otorgados.forEach { persona ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(persona.nombre, fontWeight = FontWeight.SemiBold)
                                                listOf(persona.cargo, persona.correo).filter { it.isNotBlank() }.joinToString(" · ")
                                                    .takeIf { it.isNotBlank() }?.let {
                                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                Text(
                                                    persona.resumen + " · " + if (persona.permiso == "LECTOR") "Solo lectura" else "Puede editar",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MoradoClinico,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            TextButton(
                                                onClick = { porQuitar = persona },
                                                enabled = !procesando,
                                                modifier = Modifier.testTag("quitar_${persona.usuarioId}")
                                            ) { Text("Quitar", color = RojoClinico, fontWeight = FontWeight.SemiBold) }
                                        }
                                        if (persona.fichas > 0) {
                                            val abierta = fichasAbiertasDe == persona.usuarioId
                                            TextButton(
                                                onClick = { fichasAbiertasDe = if (abierta) null else persona.usuarioId },
                                                modifier = Modifier.testTag("ver_fichas_${persona.usuarioId}")
                                            ) {
                                                Text(
                                                    if (abierta) "Ocultar fichas sueltas" else "Ver fichas sueltas (${persona.fichas})",
                                                    color = AzulClinico,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            if (abierta) {
                                                when {
                                                    cargandoSueltas -> Text("Cargando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    fichasSueltas.isEmpty() -> Text(
                                                        "No hay fichas sueltas.",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    else -> fichasSueltas.forEach { ficha ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(Modifier.weight(1f)) {
                                                                Text(ficha.jefe.ifBlank { "Familia sin nombre" }, fontWeight = FontWeight.SemiBold)
                                                                Text(
                                                                    "Ficha ${ficha.numero} · " + if (ficha.permiso == "LECTOR") "Solo lectura" else "Puede editar",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            TextButton(
                                                                onClick = { fichaPorQuitar = persona to ficha },
                                                                enabled = !procesando,
                                                                modifier = Modifier.testTag("quitar_ficha_${ficha.fichaId}")
                                                            ) { Text("Quitar", color = RojoClinico, fontWeight = FontWeight.SemiBold) }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                            }
                        }
                    }

                    mensaje?.let {
                        MensajeEstadoRuralitos(
                            titulo = if (
                                it.contains("correctamente", true)
                            ) {
                                "Listo"
                            } else {
                                "Aviso"
                            },
                            descripcion = it,
                            color = if (
                                it.contains("correctamente", true)
                            ) {
                                CianRuralitos
                            } else {
                                NaranjaClinico
                            },
                            simbolo = if (
                                it.contains("correctamente", true)
                            ) {
                                "✓"
                            } else {
                                "!"
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        }
        // Botón redondo flotante: abre la lista de quienes me compartieron para quitarme el acceso.
        if (seccion == SeccionSala.ACCESOS && subAcceso == 2) {
            Surface(
                onClick = { recibidosAbierto = true },
                shape = CircleShape,
                color = RojoClinico,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp)
                    .size(58.dp)
                    .testTag("fab_quitarme_acceso")
                    .semantics { contentDescription = "Quitarme el acceso a lo que me compartieron" }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("↩", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TabsSalaModernas(
    seleccionada: SeccionSala,
    onSeleccionar: (SeccionSala) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            MoradoClinico.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SeccionSala.entries.forEach { opcion ->
                val activa = seleccionada == opcion

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .clickable {
                            onSeleccionar(opcion)
                        },
                    color = if (activa) {
                        MoradoClinico
                    } else {
                        Color.Transparent
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        TextoAjustado(
                            texto = opcion.etiqueta,
                            color = if (activa) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.SemiBold,
                            tamano = 13.sp,
                            tamanoMinimo = 9.sp,
                            maxLineas = 2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelDesplegableSala(
    titulo: String,
    descripcion: String,
    simbolo: String,
    color: Color,
    abierto: Boolean,
    onCambiar: () -> Unit,
    contenido: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCambiar)
                    .padding(
                        horizontal = 14.dp,
                        vertical = 14.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    color = color.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = simbolo,
                            color = color,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Surface(
                    modifier = Modifier.size(36.dp),
                    color = color.copy(alpha = 0.07f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        FlechaDesplegable(color = color, arriba = abierto)
                    }
                }
            }

            if (abierto) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 14.dp,
                            end = 14.dp,
                            bottom = 14.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    contenido()
                }
            }
        }
    }
}

/** Dos secciones dentro de «Compartir acceso»: [opciones] son (texto, etiqueta de prueba). */
@Composable
private fun SubTabsAcceso(
    opciones: List<Pair<String, String>>,
    seleccionada: Int,
    onSeleccionar: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AzulClinico.copy(alpha = 0.18f))
    ) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            opciones.forEachIndexed { indice, (texto, etiqueta) ->
                val activa = indice == seleccionada
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .testTag(etiqueta)
                        .clickable { onSeleccionar(indice) },
                    color = if (activa) AzulClinico else Color.Transparent,
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TextoAjustado(
                            texto = texto,
                            color = if (activa) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            tamano = 14.sp,
                            tamanoMinimo = 10.sp,
                            maxLineas = 2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
