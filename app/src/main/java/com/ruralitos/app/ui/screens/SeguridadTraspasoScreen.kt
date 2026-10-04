package com.ruralitos.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ruralitos.app.data.fichas.EliminadorFichas
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.SalaEntity
import com.ruralitos.app.data.local.entity.UsuarioEntity
import com.ruralitos.app.data.remote.CodigoTraspasoRemoto
import com.ruralitos.app.data.remote.SupabaseApi
import com.ruralitos.app.data.sync.ProgramadorSincronizacion
import com.ruralitos.app.data.sync.SincronizadorSupabase
import com.ruralitos.app.domain.AlcanceFichas
import com.ruralitos.app.domain.CatalogoAlcance
import com.ruralitos.app.domain.EtiquetasFicha
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.SelectorAlcanceRuralitos
import com.ruralitos.app.ui.components.TarjetaCodigoRuralitos
import com.ruralitos.app.ui.components.VentanaConfirmarRuralitos
import com.ruralitos.app.ui.components.mensajeDeTraspaso
import com.ruralitos.app.ui.components.rememberEstadoAlcance
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.TextoSecundario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MODO_TRASPASAR = "traspasar"
private const val MODO_ELIMINAR = "eliminar"

/**
 * Seguridad y traspaso: un menú con dos espacios separados. «Traspasar» entrega fichas a otra persona (por centro, EAIS,
 * barrio o ficha) con un código: la otra persona pasa a ser la dueña de las MISMAS fichas y quien las entrega pierde el
 * acceso. «Eliminar fichas» borra las fichas propias.
 */
@Composable
fun SeguridadTraspasoScreen(
    usuario: UsuarioEntity,
    salaActiva: SalaEntity?,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var modo by remember { mutableStateOf<String?>(null) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var esError by remember { mutableStateOf(false) }
    var procesando by remember { mutableStateOf(false) }
    var textoBorrado by remember { mutableStateOf("") }
    var errorBorrado by remember { mutableStateOf(false) }
    // Dentro de Traspasar: 0 = entregar fichas, 1 = recibir fichas con un código.
    var subTraspaso by remember { mutableStateOf(0) }
    var confirmarEntrega by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<CodigoTraspasoRemoto?>(null) }
    var codigoRecibido by remember { mutableStateOf("") }
    val estadoAlcance = rememberEstadoAlcance()
    val api = remember(context) { SupabaseApi(context) }
    val miCuentaRemota = remember(api) { api.sesionGuardada()?.usuarioId.orEmpty() }

    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val fichasGuardadas by remember(database) { database.fichaFamiliarDao().listarFichas() }
        .collectAsState(initial = emptyList())
    val salas by remember(database) { database.salaDao().observarSalas() }.collectAsState(initial = emptyList())
    val todosLosEais by remember(database) { database.salaDao().observarTodosEais() }
        .collectAsState(initial = emptyList())
    val todosLosBarrios by remember(database) { database.salaDao().observarTodosTerritorios() }
        .collectAsState(initial = emptyList())

    // Solo se traspasan las fichas propias (las que creó esta persona).
    val traspasables = fichasGuardadas.filter { EtiquetasFicha.esMia(it, usuario.id, miCuentaRemota) }
    val catalogo = CatalogoAlcance(
        salas = salas,
        eais = todosLosEais,
        territorios = todosLosBarrios,
        fichas = traspasables,
        salaActivaId = salaActiva?.organizacionId ?: usuario.organizacionId
    )

    fun volverAlMenu() {
        modo = null
        mensaje = null
        textoBorrado = ""
        errorBorrado = false
        codigoGenerado = null
        codigoRecibido = ""
    }

    if (confirmarEntrega) {
        val cuantas = AlcanceFichas.fichasDe(estadoAlcance.nivel, estadoAlcance.elegidos, catalogo).size
        VentanaConfirmarRuralitos(
            titulo = "Traspasar fichas",
            mensaje = "Vas a generar un código para entregar ${if (cuantas == 1) "1 ficha" else "$cuantas fichas"} a otra persona. " +
                "Cuando ella lo acepte, pasarán a ser suyas: ella decidirá con quién se comparten y tú dejarás de tener acceso, " +
                "a menos que ella te las comparta. Las demás personas que las veían por ti también dejarán de verlas.",
            textoConfirmar = "Generar código",
            confirmarHabilitado = !procesando,
            onConfirmar = {
                confirmarEntrega = false
                val concesiones = AlcanceFichas.concesiones(estadoAlcance.nivel, estadoAlcance.elegidos, catalogo)
                procesando = true
                mensaje = null
                scope.launch {
                    runCatching { api.crearCodigoTraspaso(concesiones) }
                        .onSuccess { codigoGenerado = it }
                        .onFailure { mensaje = it.message ?: "No se pudo generar el código."; esError = true }
                    procesando = false
                }
            },
            textoCancelar = "Cancelar",
            peligro = true,
            onCancelar = { confirmarEntrega = false }
        )
    }

    PantallaRuralitos(
        titulo = when (modo) {
            MODO_TRASPASAR -> "Traspasar fichas"
            MODO_ELIMINAR -> "Eliminar fichas"
            else -> "Seguridad y traspaso"
        },
        descripcion = when (modo) {
            MODO_TRASPASAR -> "Entrega tus fichas a otra persona: ella pasa a ser la dueña."
            MODO_ELIMINAR -> "Borra solo tus fichas. Las compartidas contigo y tu cuenta se conservan."
            else -> "Entrega tus fichas a otra persona o elimina las tuyas."
        },
        subtitulo = "Protección de datos",
        onVolver = { if (modo == null) onRegresar() else volverAlMenu() }
    ) {
        mensaje?.let {
            MensajeEstadoRuralitos(
                titulo = if (esError) "No se pudo completar" else "Operación completada",
                descripcion = it,
                color = if (esError) RojoClinico else CianRuralitos,
                simbolo = if (esError) "!" else "✓"
            )
        }
        when (modo) {
            null -> {
                MensajeEstadoRuralitos(
                    titulo = "Tus fichas siempre en la nube",
                    descripcion = "Tus fichas se guardan en tu cuenta: para usarlas en otro teléfono basta iniciar sesión. " +
                        "Aquí entregas fichas a otra persona sin hacer copias.",
                    color = CianRuralitos,
                    simbolo = "✓"
                )
                TarjetaOpcionSeguridad(
                    simbolo = "⇄", color = AzulClinico,
                    titulo = "Traspasar fichas",
                    descripcion = "Entregarlas a otra persona o recibirlas",
                    estado = "Por centro, EAIS, barrio o ficha",
                    etiquetaPrueba = "abrir_traspasar"
                ) { modo = MODO_TRASPASAR; mensaje = null }
                TarjetaOpcionSeguridad(
                    simbolo = "!", color = RojoClinico,
                    titulo = "Eliminar fichas",
                    descripcion = "Empezar desde cero",
                    estado = fichasGuardadas.count { !EtiquetasFicha.esRecibida(it) }.let { if (it == 1) "1 ficha tuya" else "$it fichas tuyas" },
                    etiquetaPrueba = "abrir_eliminar"
                ) { modo = MODO_ELIMINAR; mensaje = null; textoBorrado = ""; errorBorrado = false }
            }

            MODO_TRASPASAR -> {
                SubOpcionesTraspaso(
                    opciones = listOf("Entregar fichas" to "subtab_entregar", "Recibir fichas" to "subtab_recibir"),
                    seleccionada = subTraspaso,
                    onSeleccionar = { subTraspaso = it; mensaje = null }
                )
                if (subTraspaso == 0) {
                    SelectorAlcanceRuralitos(
                        estado = estadoAlcance,
                        catalogo = catalogo,
                        titulo = "1. Qué fichas entregas",
                        color = AzulClinico,
                        soloSincronizadas = true,
                        prefijoPrueba = "traspasar"
                    )
                    val hayAlgo = AlcanceFichas.hayElegidos(estadoAlcance.nivel, estadoAlcance.elegidos)
                    SeccionFormularioRuralitos(
                        titulo = "2. Generar el código",
                        descripcion = "Pasan a ser de la otra persona; tú dejas de tener acceso. El código se usa una sola vez y caduca en 12 horas."
                    ) {
                        BotonPrincipalRuralitos(
                            texto = if (procesando) "Generando código…" else "Generar código de traspaso",
                            onClick = { confirmarEntrega = true },
                            enabled = !procesando && hayAlgo,
                            color = AzulClinico,
                            modifier = Modifier.testTag("generar_traspaso")
                        )
                        codigoGenerado?.let { codigo ->
                            TarjetaCodigoRuralitos(
                                codigo = codigo.codigo,
                                color = AzulClinico,
                                titulo = "Código de traspaso · ${if (codigo.total == 1) "1 ficha" else "${codigo.total} fichas"}",
                                mensajeParaEnviar = mensajeDeTraspaso(codigo.codigo, codigo.total),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                } else {
                    SeccionFormularioRuralitos(
                        titulo = "Recibir fichas con un código",
                        descripcion = "Pega el código que te enviaron. Las fichas pasarán a ser tuyas y aparecerán en tu lista."
                    ) {
                        OutlinedTextField(
                            value = codigoRecibido,
                            onValueChange = {
                                // Si se pega el mensaje completo del chat, se toma solo el código de 16 letras y números.
                                val encontrado = if (it.length > 20) {
                                    Regex("(?<![0-9A-Za-z])[0-9A-Fa-f]{16}(?![0-9A-Za-z])").find(it)?.value
                                } else null
                                codigoRecibido = (encontrado ?: it).uppercase().filter(Char::isLetterOrDigit).take(32)
                            },
                            label = { Text("Código de traspaso") },
                            singleLine = true,
                            enabled = !procesando,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("codigo_traspaso")
                        )
                        BotonPrincipalRuralitos(
                            texto = if (procesando) "Recibiendo…" else "Aceptar traspaso",
                            enabled = !procesando && codigoRecibido.length >= 8,
                            color = AzulClinico,
                            modifier = Modifier.padding(top = 8.dp).testTag("aceptar_traspaso"),
                            onClick = {
                                procesando = true
                                mensaje = null
                                scope.launch {
                                    runCatching { api.aceptarTraspaso(codigoRecibido) }
                                        .onSuccess { total ->
                                            esError = false
                                            mensaje = if (total == 1) "Recibiste 1 ficha. Ya es tuya y aparece en tu lista."
                                            else "Recibiste $total fichas. Ya son tuyas y aparecen en tu lista."
                                            codigoRecibido = ""
                                            // Se bajan ahora mismo para que aparezcan sin esperar.
                                            withContext(Dispatchers.IO) { runCatching { SincronizadorSupabase(context).ejecutar() } }
                                            ProgramadorSincronizacion.ejecutarAhora(context)
                                        }
                                        .onFailure { mensaje = it.message ?: "No se pudo recibir el traspaso."; esError = true }
                                    procesando = false
                                }
                            }
                        )
                    }
                }
            }

            MODO_ELIMINAR -> {
                val cantidad = fichasGuardadas.count { !EtiquetasFicha.esRecibida(it) }
                val recibidas = fichasGuardadas.size - cantidad
                MensajeEstadoRuralitos(
                    titulo = if (cantidad == 1) "1 ficha tuya" else "$cantidad fichas tuyas",
                    descripcion = "Se borran solo las fichas que tú creaste, con todo su contenido (integrantes, salud, riesgos, croquis, fotos y firmas), " +
                        "además de sus visitas en la agenda y tus notas sobre sus integrantes. Se borran de este teléfono " +
                        "y de la nube, y tus compañeros dejarán de verlas. Tu cuenta, tu Sala y los barrios se conservan. " +
                        (if (recibidas > 0) "Las $recibidas ficha(s) que otras personas te compartieron no se tocan. " else "") +
                        "No se puede deshacer.",
                    color = RojoClinico,
                    simbolo = "!"
                )
                SeccionFormularioRuralitos(titulo = "Para confirmar, escribe ELIMINAR") {
                    OutlinedTextField(
                        value = textoBorrado,
                        onValueChange = { textoBorrado = it; errorBorrado = false },
                        label = { Text("Escribe ELIMINAR") },
                        singleLine = true,
                        isError = errorBorrado,
                        enabled = !procesando,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("texto_confirmar_borrado")
                    )
                    if (errorBorrado) {
                        Text(
                            "Escribe la palabra ELIMINAR tal como se ve para continuar.",
                            color = RojoClinico,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                BotonPrincipalRuralitos(
                    texto = if (procesando) "Eliminando…" else "Eliminar todas las fichas",
                    color = RojoClinico,
                    enabled = !procesando,
                    modifier = Modifier.testTag("eliminar_todas_fichas"),
                    onClick = {
                        if (textoBorrado.trim() != "ELIMINAR") {
                            errorBorrado = true
                            return@BotonPrincipalRuralitos
                        }
                        procesando = true
                        scope.launch {
                            runCatching {
                                EliminadorFichas.eliminarTodas(context, database, usuario.id)
                            }.onSuccess { total ->
                                // Se borra en la nube ahora mismo; si falta algo (sin internet, sin permiso) sigue anotado.
                                val pendientes = withContext(Dispatchers.IO) {
                                    runCatching { SincronizadorSupabase(context).ejecutar() }
                                    val dao = database.sincronizacionDao()
                                    dao.contarFichasEnBaja() to dao.contarEliminacionesPendientes()
                                }
                                if (pendientes.second > 0) ProgramadorSincronizacion.ejecutarAhora(context)
                                mensaje = when {
                                    pendientes.second == 0 ->
                                        "Se eliminaron $total ficha(s) de este teléfono y de la nube. Tu cuenta sigue igual y ya puedes empezar desde cero."
                                    pendientes.first == 0 ->
                                        "Se eliminaron $total ficha(s). En la nube ya no aparecen; aún se están limpiando algunos de sus datos y terminará solo."
                                    else ->
                                        "Se eliminaron $total ficha(s) de este teléfono, pero ${pendientes.first} todavía no se pudieron borrar de la nube " +
                                            "(sin internet o sin permiso). Mientras tanto no volverán a aparecer aquí y se reintentará solo."
                                }
                                esError = pendientes.first > 0
                                modo = null
                            }.onFailure {
                                mensaje = "No se pudieron eliminar todas las fichas. Inténtalo de nuevo."
                                esError = true
                            }
                            procesando = false
                            textoBorrado = ""
                        }
                    }
                )
            }
        }
        Spacer(Modifier.padding(bottom = 12.dp))
    }
}

/** Dos opciones lado a lado (entregar o recibir), con el mismo aspecto que las de Compartir acceso. */
@Composable
private fun SubOpcionesTraspaso(
    opciones: List<Pair<String, String>>,
    seleccionada: Int,
    onSeleccionar: (Int) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEachIndexed { indice, (texto, etiqueta) ->
            val activa = indice == seleccionada
            Surface(
                onClick = { onSeleccionar(indice) },
                modifier = Modifier.weight(1f).testTag(etiqueta),
                shape = RoundedCornerShape(12.dp),
                color = if (activa) AzulClinico else Color.White,
                border = BorderStroke(1.dp, if (activa) AzulClinico else BordeClinico)
            ) {
                Text(
                    texto,
                    modifier = Modifier.padding(vertical = 11.dp).fillMaxWidth(),
                    color = if (activa) Color.White else AzulClinicoOscuro,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/** Una opción del menú de Seguridad y traspaso: símbolo de color, qué hace y una línea de estado. */
@Composable
private fun TarjetaOpcionSeguridad(
    simbolo: String,
    color: Color,
    titulo: String,
    descripcion: String,
    estado: String,
    etiquetaPrueba: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(etiquetaPrueba),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BordeClinico)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(simbolo, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, color = AzulClinicoOscuro, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(descripcion, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
                Text(estado, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
            Text("›", color = TextoSecundario, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
