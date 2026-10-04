package com.ruralitos.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ruralitos.app.data.backup.CifradoRespaldo
import com.ruralitos.app.data.backup.GestorRespaldoRuralitos
import com.ruralitos.app.data.backup.ResultadoRestauracion
import com.ruralitos.app.data.fichas.EliminadorFichas
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.SalaEntity
import com.ruralitos.app.data.local.entity.UsuarioEntity
import com.ruralitos.app.data.sync.ProgramadorSincronizacion
import com.ruralitos.app.data.sync.SincronizadorSupabase
import com.ruralitos.app.domain.AlcanceFichas
import com.ruralitos.app.domain.CatalogoAlcance
import com.ruralitos.app.domain.EtiquetasFicha
import com.ruralitos.app.ui.components.AvisosRuralitos
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.SelectorAlcanceRuralitos
import com.ruralitos.app.ui.components.VentanaConfirmarRuralitos
import com.ruralitos.app.ui.components.rememberEstadoAlcance
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.TextoSecundario
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MODO_EXPORTAR = "exportar"
private const val MODO_IMPORTAR = "importar"
private const val MODO_ELIMINAR = "eliminar"

/**
 * Seguridad y respaldo: un menú con tres espacios separados (exportar, importar y eliminar fichas). Cada uno tiene su
 * propia pantalla, con su color, para no mezclar lo que hace cada cosa.
 */
@Composable
fun SeguridadRespaldoScreen(
    usuario: UsuarioEntity,
    salaActiva: SalaEntity?,
    onRestaurado: (ResultadoRestauracion) -> Unit,
    onRegresar: () -> Unit,
    /** Un respaldo que llegó desde otra aplicación («Abrir con Ruralitos»): la pantalla abre directo en Importar. */
    archivoEntrante: Uri? = null,
    nombreArchivoEntrante: String = "",
    onTerminarEntrante: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var modo by remember { mutableStateOf<String?>(if (archivoEntrante != null) MODO_IMPORTAR else null) }
    var clave by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var esError by remember { mutableStateOf(false) }
    var procesando by remember { mutableStateOf(false) }
    var archivo by remember { mutableStateOf(archivoEntrante) }
    var nombreArchivo by remember { mutableStateOf(nombreArchivoEntrante) }
    var confirmarImportacion by remember { mutableStateOf(false) }
    var respaldoCreado by remember { mutableStateOf<Uri?>(null) }
    var idsParaExportar by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var textoBorrado by remember { mutableStateOf("") }
    var errorBorrado by remember { mutableStateOf(false) }
    val estadoAlcance = rememberEstadoAlcance()
    val miCuentaRemota = remember(context) { com.ruralitos.app.data.remote.SupabaseApi(context).sesionGuardada()?.usuarioId.orEmpty() }

    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val fichasGuardadas by remember(database) { database.fichaFamiliarDao().listarFichas() }
        .collectAsState(initial = emptyList())
    val salas by remember(database) { database.salaDao().observarSalas() }.collectAsState(initial = emptyList())
    val todosLosEais by remember(database) { database.salaDao().observarTodosEais() }
        .collectAsState(initial = emptyList())
    val todosLosBarrios by remember(database) { database.salaDao().observarTodosTerritorios() }
        .collectAsState(initial = emptyList())

    // Todos los usuarios por igual: cada uno exporta las fichas que él mismo creó.
    val exportables = fichasGuardadas.filter { EtiquetasFicha.esMia(it, usuario.id, miCuentaRemota) }
    val catalogo = CatalogoAlcance(
        salas = salas,
        eais = todosLosEais,
        territorios = todosLosBarrios,
        fichas = exportables,
        salaActivaId = salaActiva?.organizacionId ?: usuario.organizacionId
    )

    val entranteActual by rememberUpdatedState(archivoEntrante)
    val terminarEntrante by rememberUpdatedState(onTerminarEntrante)
    DisposableEffect(Unit) { onDispose { if (entranteActual != null) terminarEntrante() } }
    LaunchedEffect(archivoEntrante) {
        if (archivoEntrante != null && archivo != archivoEntrante) {
            archivo = archivoEntrante
            nombreArchivo = nombreArchivoEntrante
            modo = MODO_IMPORTAR
        }
    }

    fun compartir(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Respaldo cifrado de Ruralitos")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir respaldo de Ruralitos"))
    }

    fun volverAlMenu() {
        if (modo == MODO_IMPORTAR && archivoEntrante != null && archivo == archivoEntrante) {
            onTerminarEntrante()
            archivo = null
            nombreArchivo = ""
        }
        modo = null
        clave = ""
        confirmacion = ""
        mensaje = null
        textoBorrado = ""
        errorBorrado = false
    }

    val crearArchivo = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            procesando = true
            scope.launch {
                runCatching {
                    GestorRespaldoRuralitos.crear(
                        context, uri, clave, usuario,
                        fichaIds = idsParaExportar
                    )
                }.onSuccess { resultado ->
                    respaldoCreado = uri
                    mensaje = "Respaldo listo: ${resultado.fichasIncluidas} ficha(s) y " +
                        "${resultado.adjuntosIncluidos} archivo(s) incluidos."
                    if (resultado.adjuntosOmitidos > 0) {
                        mensaje += " ${resultado.adjuntosOmitidos} archivo(s) no estaban disponibles."
                    }
                    esError = false
                    modo = null
                    clave = ""
                    confirmacion = ""
                }.onFailure {
                    mensaje = "No se pudo crear el respaldo. Verifica el espacio y los archivos adjuntos."
                    esError = true
                }
                procesando = false
            }
        }
    }
    val abrirArchivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val parece = runCatching { context.contentResolver.openInputStream(uri)?.use(CifradoRespaldo::pareceRespaldo) }
                .getOrNull() == true
            if (parece) {
                archivo = uri
                nombreArchivo = nombreDeArchivo(context, uri)
                mensaje = null
            } else {
                mensaje = "El archivo elegido no es un respaldo de Ruralitos."
                esError = true
            }
        }
    }

    respaldoCreado?.let { uri ->
        VentanaConfirmarRuralitos(
            titulo = "Respaldo creado correctamente",
            mensaje = "El archivo está cifrado y no contiene tu contraseña, sesión ni credenciales. " +
                "¿Quieres compartirlo ahora?",
            textoConfirmar = "Compartir archivo",
            onConfirmar = {
                respaldoCreado = null
                compartir(uri)
            },
            textoCancelar = "Ahora no",
            peligro = false,
            onCancelar = { respaldoCreado = null }
        )
    }
    if (confirmarImportacion) {
        VentanaConfirmarRuralitos(
            titulo = "Importar fichas del respaldo",
            mensaje = "Las fichas se agregarán a la Sala activa y quedarán pendientes de sincronización. " +
                "Las fichas que ya existan no se duplicarán.",
            textoConfirmar = "Importar fichas",
            onConfirmar = {
                confirmarImportacion = false
                val origen = archivo ?: return@VentanaConfirmarRuralitos
                procesando = true
                scope.launch {
                    runCatching {
                        GestorRespaldoRuralitos.restaurar(context, origen, clave, usuario, salaActiva)
                    }.onSuccess { resultado ->
                        mensaje = "${resultado.fichasImportadas} ficha(s) importadas; " +
                            "${resultado.fichasOmitidas} duplicada(s) omitidas."
                        esError = false
                        if (archivoEntrante != null && archivo == archivoEntrante) onTerminarEntrante()
                        archivo = null
                        nombreArchivo = ""
                        modo = null
                        clave = ""
                        onRestaurado(resultado)
                    }.onFailure {
                        mensaje = it.message
                            ?: "No se pudo importar. La contraseña o el archivo pueden ser incorrectos."
                        esError = true
                    }
                    procesando = false
                }
            },
            textoCancelar = "Cancelar",
            peligro = false,
            onCancelar = { confirmarImportacion = false }
        )
    }

    PantallaRuralitos(
        titulo = when (modo) {
            MODO_EXPORTAR -> "Exportar fichas"
            MODO_IMPORTAR -> "Importar fichas"
            MODO_ELIMINAR -> "Eliminar fichas"
            else -> "Seguridad y respaldo"
        },
        descripcion = when (modo) {
            MODO_EXPORTAR -> "Crea un archivo cifrado con las fichas que elijas."
            MODO_IMPORTAR -> "Las fichas pasan a tu cuenta y a tu Sala activa."
            MODO_ELIMINAR -> "Borra solo tus fichas. Las compartidas contigo y tu cuenta se conservan."
            else -> "Exporta, importa o elimina tus fichas con archivos cifrados bajo tu control."
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
                    titulo = "Respaldo portable y cifrado",
                    descripcion = "Tus fichas viajan en archivos cifrados con una contraseña que solo tú defines.",
                    color = CianRuralitos,
                    simbolo = "✓"
                )
                TarjetaOpcionRespaldo(
                    simbolo = "↑", color = CianRuralitos,
                    titulo = "Exportar fichas",
                    descripcion = "Crear un respaldo cifrado",
                    estado = "Por centro, EAIS, barrio o ficha",
                    etiquetaPrueba = "abrir_exportar"
                ) { modo = MODO_EXPORTAR; mensaje = null }
                TarjetaOpcionRespaldo(
                    simbolo = "↓", color = NaranjaClinico,
                    titulo = "Importar fichas",
                    descripcion = "Cargar un respaldo .ruralitos",
                    estado = "Destino: ${salaActiva?.nombreCentroSalud?.ifBlank { salaActiva.nombreSala } ?: "Sala principal"}",
                    etiquetaPrueba = "abrir_importar"
                ) { modo = MODO_IMPORTAR; mensaje = null }
                TarjetaOpcionRespaldo(
                    simbolo = "!", color = RojoClinico,
                    titulo = "Eliminar fichas",
                    descripcion = "Empezar desde cero",
                    estado = fichasGuardadas.count { !EtiquetasFicha.esRecibida(it) }.let { if (it == 1) "1 ficha tuya" else "$it fichas tuyas" },
                    etiquetaPrueba = "abrir_eliminar"
                ) { modo = MODO_ELIMINAR; mensaje = null; textoBorrado = ""; errorBorrado = false }
            }

            MODO_EXPORTAR -> {
                SelectorAlcanceRuralitos(
                    estado = estadoAlcance,
                    catalogo = catalogo,
                    titulo = "1. Qué exportar",
                    color = CianRuralitos,
                    prefijoPrueba = "exportar"
                )
                val elegidas = AlcanceFichas.fichasDe(estadoAlcance.nivel, estadoAlcance.elegidos, catalogo)
                val hayAlgo = AlcanceFichas.hayElegidos(estadoAlcance.nivel, estadoAlcance.elegidos) && elegidas.isNotEmpty()
                SeccionFormularioRuralitos(
                    titulo = "2. Contraseña del respaldo",
                    descripcion = "Usa al menos 10 caracteres y guárdala en un lugar seguro. Ruralitos no la guarda y no podrá recuperarla."
                ) {
                    OutlinedTextField(
                        value = clave,
                        onValueChange = { clave = it; mensaje = null },
                        label = { Text("Contraseña del respaldo") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !procesando,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("exportar_clave")
                    )
                    OutlinedTextField(
                        value = confirmacion,
                        onValueChange = { confirmacion = it; mensaje = null },
                        label = { Text("Repite la contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !procesando,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("exportar_confirmacion")
                    )
                }
                BotonPrincipalRuralitos(
                    texto = when {
                        procesando -> "Procesando respaldo…"
                        hayAlgo -> "Crear respaldo (${if (elegidas.size == 1) "1 ficha" else "${elegidas.size} fichas"})"
                        else -> "Crear respaldo"
                    },
                    onClick = {
                        mensaje = when {
                            !hayAlgo -> "Elige al menos una ficha para exportar."
                            clave.length < 10 -> "La contraseña debe tener al menos 10 caracteres."
                            clave != confirmacion -> "Las contraseñas no coinciden."
                            else -> null
                        }
                        esError = mensaje != null
                        if (mensaje == null) {
                            idsParaExportar = elegidas.map { it.id }.toSet()
                            val fecha = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            crearArchivo.launch("ruralitos_fichas_$fecha.ruralitos")
                        }
                    },
                    enabled = !procesando,
                    color = CianRuralitos,
                    modifier = Modifier.testTag("crear_respaldo")
                )
            }

            MODO_IMPORTAR -> {
                SeccionFormularioRuralitos(
                    titulo = "Destino",
                    descripcion = "Las fichas se agregan a tu cuenta y a esta Sala. No se copia ninguna sesión."
                ) {
                    MensajeEstadoRuralitos(
                        titulo = salaActiva?.nombreCentroSalud?.ifBlank { salaActiva.nombreSala } ?: "Sala principal",
                        descripcion = "Las fichas que ya existan se conservan y las nuevas quedan pendientes de sincronizar.",
                        color = NaranjaClinico,
                        simbolo = "+"
                    )
                }
                SeccionFormularioRuralitos(titulo = "1. Elegir el archivo") {
                    if (archivo != null) {
                        MensajeEstadoRuralitos(
                            titulo = nombreArchivo.ifBlank { "respaldo.ruralitos" },
                            descripcion = "Archivo listo para importar.",
                            color = CianRuralitos,
                            simbolo = "✓",
                            modifier = Modifier.testTag("archivo_elegido")
                        )
                    }
                    BotonSecundarioRuralitos(
                        texto = if (archivo == null) "Seleccionar archivo .ruralitos" else "Elegir otro archivo",
                        onClick = { abrirArchivo.launch(arrayOf("*/*")) },
                        enabled = !procesando,
                        modifier = Modifier.testTag("elegir_archivo")
                    )
                }
                SeccionFormularioRuralitos(
                    titulo = "2. Contraseña del respaldo",
                    descripcion = "Escribe la contraseña que se usó al crear el archivo."
                ) {
                    OutlinedTextField(
                        value = clave,
                        onValueChange = { clave = it; mensaje = null },
                        label = { Text("Contraseña del respaldo") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !procesando,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("importar_clave")
                    )
                }
                BotonPrincipalRuralitos(
                    texto = if (procesando) "Importando…" else "Importar fichas",
                    onClick = {
                        when {
                            archivo == null -> { mensaje = "Primero elige el archivo del respaldo."; esError = true }
                            clave.length < 10 -> { mensaje = "La contraseña debe tener al menos 10 caracteres."; esError = true }
                            else -> { mensaje = null; confirmarImportacion = true }
                        }
                    },
                    enabled = !procesando,
                    color = NaranjaClinico,
                    modifier = Modifier.testTag("importar_fichas")
                )
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
                SeccionFormularioRuralitos(
                    titulo = "Antes de borrar",
                    descripcion = "Si quieres guardar una copia, crea primero un respaldo cifrado."
                ) {
                    BotonSecundarioRuralitos(
                        texto = "Crear un respaldo primero",
                        onClick = { modo = MODO_EXPORTAR; mensaje = null },
                        modifier = Modifier.testTag("respaldo_antes_de_borrar")
                    )
                }
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

/** Una opción del menú de Seguridad y respaldo: símbolo de color, qué hace y una línea de estado. */
@Composable
private fun TarjetaOpcionRespaldo(
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

private fun nombreDeArchivo(context: Context, uri: Uri): String {
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() }?.let { return it }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "respaldo.ruralitos"
}
