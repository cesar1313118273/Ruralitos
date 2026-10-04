package com.ruralitos.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ruralitos.app.data.backup.GestorRespaldoRuralitos
import com.ruralitos.app.data.backup.ResultadoRestauracion
import com.ruralitos.app.data.fichas.EliminadorFichas
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.sync.ProgramadorSincronizacion
import com.ruralitos.app.data.sync.SincronizadorSupabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ruralitos.app.data.local.entity.SalaEntity
import com.ruralitos.app.data.local.entity.UsuarioEntity
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun SeguridadRespaldoScreen(
    usuario: UsuarioEntity,
    salaActiva: SalaEntity?,
    onRestaurado: (ResultadoRestauracion) -> Unit,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var modo by remember { mutableStateOf<String?>(null) }
    var clave by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var esError by remember { mutableStateOf(false) }
    var procesando by remember { mutableStateOf(false) }
    var restauracionPendiente by remember { mutableStateOf<Uri?>(null) }
    var respaldoCreado by remember { mutableStateOf<Uri?>(null) }
    val fichasGuardadas by remember(context) { RuralitosDatabase.obtenerBaseDatos(context).fichaFamiliarDao().listarFichas() }
        .collectAsState(initial = emptyList())
    var confirmarBorradoTotal by remember { mutableStateOf(false) }
    var textoBorrado by remember { mutableStateOf("") }
    var errorBorrado by remember { mutableStateOf(false) }

    fun compartir(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Respaldo cifrado de Ruralitos")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir respaldo de Ruralitos"))
    }

    val crearArchivo = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            procesando = true
            scope.launch {
                runCatching { GestorRespaldoRuralitos.crear(context, uri, clave, usuario) }
                    .onSuccess { resultado ->
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
                    }
                    .onFailure {
                        mensaje = "No se pudo crear el respaldo. Verifica el espacio y los archivos adjuntos."
                        esError = true
                    }
                procesando = false
            }
        }
    }
    val abrirArchivo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) restauracionPendiente = uri
    }

    if (confirmarBorradoTotal) {
        val cantidad = fichasGuardadas.size
        AlertDialog(
            onDismissRequest = { if (!procesando) confirmarBorradoTotal = false },
            title = { Text("Eliminar todas las fichas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Se eliminarán las $cantidad ficha(s) con todo su contenido: integrantes, salud, riesgos, croquis, " +
                            "fotos y firmas, además de sus visitas en la agenda y tus notas sobre sus integrantes. " +
                            "Se borran de este teléfono y de la nube, y tus compañeros dejarán de verlas."
                    )
                    Text(
                        "Tu cuenta, tu Sala y los barrios se conservan. Esta acción no se puede deshacer; " +
                            "si quieres guardar una copia, crea antes un respaldo.",
                        color = RojoClinico
                    )
                    OutlinedTextField(
                        value = textoBorrado,
                        onValueChange = { textoBorrado = it; errorBorrado = false },
                        label = { Text("Escribe ELIMINAR para confirmar") },
                        singleLine = true,
                        isError = errorBorrado,
                        enabled = !procesando,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("texto_confirmar_borrado")
                    )
                    if (errorBorrado) Text("Escribe la palabra ELIMINAR tal como se ve para continuar.", color = RojoClinico, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("confirmar_borrado_total"),
                    onClick = {
                        if (textoBorrado.trim() != "ELIMINAR") { errorBorrado = true; return@TextButton }
                        procesando = true
                        scope.launch {
                            runCatching {
                                EliminadorFichas.eliminarTodas(context, RuralitosDatabase.obtenerBaseDatos(context), usuario.id)
                            }.onSuccess { total ->
                                // Se borra en la nube ahora mismo; si falta algo (sin internet, sin permiso) sigue anotado.
                                val pendientes = withContext(Dispatchers.IO) {
                                    runCatching { SincronizadorSupabase(context).ejecutar() }
                                    val dao = RuralitosDatabase.obtenerBaseDatos(context).sincronizacionDao()
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
                            }.onFailure {
                                mensaje = "No se pudieron eliminar todas las fichas. Inténtalo de nuevo."
                                esError = true
                            }
                            procesando = false
                            confirmarBorradoTotal = false
                            textoBorrado = ""
                        }
                    }
                ) { Text(if (procesando) "Eliminando…" else "Eliminar todo", color = RojoClinico) }
            },
            dismissButton = {
                TextButton(onClick = { if (!procesando) confirmarBorradoTotal = false }) { Text("Cancelar") }
            }
        )
    }
    respaldoCreado?.let { uri ->
        AlertDialog(
            onDismissRequest = { respaldoCreado = null },
            title = { Text("Respaldo creado correctamente") },
            text = {
                Text(
                    "El archivo está cifrado y no contiene tu contraseña, sesión ni credenciales. " +
                        "¿Quieres compartirlo ahora?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    respaldoCreado = null
                    compartir(uri)
                }) { Text("Compartir archivo") }
            },
            dismissButton = {
                TextButton(onClick = { respaldoCreado = null }) { Text("Ahora no") }
            }
        )
    }
    restauracionPendiente?.let { uri ->
        AlertDialog(
            onDismissRequest = { restauracionPendiente = null },
            title = { Text("Importar fichas del respaldo") },
            text = {
                Text(
                    "Las fichas se agregarán a la Sala activa y quedarán pendientes de sincronización. " +
                        "Las fichas que ya existan no se duplicarán."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    restauracionPendiente = null
                    procesando = true
                    scope.launch {
                        runCatching {
                            GestorRespaldoRuralitos.restaurar(
                                context, uri, clave, usuario, salaActiva
                            )
                        }.onSuccess { resultado ->
                            mensaje = "${resultado.fichasImportadas} ficha(s) importadas; " +
                                "${resultado.fichasOmitidas} duplicada(s) omitidas."
                            esError = false
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
                }) { Text("Importar fichas") }
            },
            dismissButton = {
                TextButton(onClick = { restauracionPendiente = null }) { Text("Cancelar") }
            }
        )
    }

    PantallaRuralitos(
        titulo = when (modo) {
                "crear" -> "Crear respaldo portable"
                "restaurar" -> "Importar respaldo"
                else -> "Seguridad y respaldo"
            },
        descripcion = when (modo) {
                null -> "Protege y traslada tus fichas con archivos cifrados bajo tu control."
                "crear" -> "El respaldo incluirá datos clínicos, historial, firmas e imágenes disponibles."
                else -> "Las fichas se reasignarán a tu cuenta y Sala activa sin copiar otra sesión."
            },
        subtitulo = "Protección de datos",
        onVolver = {
            if (modo == null) {
                onRegresar()
            } else {
                modo = null
                clave = ""
                confirmacion = ""
                mensaje = null
            }
        }
    ) {
        if (modo == null) {
            MensajeEstadoRuralitos(
                titulo = "Respaldo portable y cifrado",
                descripcion = "Puede abrirse en otro teléfono o cuenta de Ruralitos con la contraseña que tú definas.",
                color = CianRuralitos,
                simbolo = "✓"
            )
            mensaje?.let {
                MensajeEstadoRuralitos(
                    titulo = if (esError) "No se pudo completar" else "Operación completada",
                    descripcion = it,
                    color = if (esError) RojoClinico else CianRuralitos,
                    simbolo = if (esError) "!" else "✓"
                )
            }
            SeccionFormularioRuralitos(
                titulo = "Exportar mis fichas",
                descripcion = "No incluye usuario, PIN, sesión de Supabase ni claves internas del dispositivo."
            ) {
                BotonPrincipalRuralitos(
                    texto = "Crear respaldo para guardar o compartir",
                    descripcion = "Generar archivo .ruralitos protegido con contraseña",
                    onClick = { modo = "crear"; mensaje = null },
                    color = CianRuralitos
                )
            }
            SeccionFormularioRuralitos(
                titulo = "Importar en esta cuenta",
                descripcion = "Destino: ${salaActiva?.nombreCentroSalud?.ifBlank { salaActiva.nombreSala } ?: "Sala principal"}."
            ) {
                MensajeEstadoRuralitos(
                    titulo = "Importación sin duplicados",
                    descripcion = "Las fichas existentes se conservan y las nuevas se agregan como pendientes de sincronización.",
                    color = NaranjaClinico,
                    simbolo = "+"
                )
                BotonPrincipalRuralitos(
                    texto = "Cargar respaldo de fichas",
                    descripcion = "Seleccionar un archivo .ruralitos",
                    onClick = { modo = "restaurar"; mensaje = null },
                    color = NaranjaClinico,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            SeccionFormularioRuralitos(
                titulo = "Empezar desde cero",
                descripcion = "Elimina todas las fichas, con su contenido, de este teléfono y de la nube. Tu cuenta se conserva."
            ) {
                MensajeEstadoRuralitos(
                    titulo = "${fichasGuardadas.size} ficha(s) guardadas",
                    descripcion = "Esta acción no se puede deshacer. Antes puedes crear un respaldo cifrado.",
                    color = RojoClinico,
                    simbolo = "!"
                )
                BotonPrincipalRuralitos(
                    texto = "Eliminar todas las fichas",
                    descripcion = "Borrar todos los registros de fichas",
                    onClick = { textoBorrado = ""; errorBorrado = false; mensaje = null; confirmarBorradoTotal = true },
                    color = RojoClinico,
                    modifier = Modifier.padding(top = 10.dp).testTag("eliminar_todas_fichas")
                )
            }
            SeccionFormularioRuralitos(
                titulo = "Protecciones activas",
                descripcion = "Seguridad local y remota de la cuenta."
            ) {
                MensajeEstadoRuralitos(
                    titulo = "Base y sesión protegidas",
                    descripcion = "SQLite permanece cifrado y la sesión se bloquea por inactividad.",
                    color = MoradoClinico,
                    simbolo = "15"
                )
            }
        } else {
            SeccionFormularioRuralitos(
                titulo = if (modo == "crear") "Contraseña del respaldo" else "Abrir respaldo cifrado",
                descripcion = if (modo == "crear") {
                    "Usa al menos 10 caracteres y guarda la contraseña en un lugar seguro."
                } else {
                    "Escribe la contraseña utilizada al crear el archivo."
                }
            ) {
                OutlinedTextField(
                    value = clave,
                    onValueChange = { clave = it; mensaje = null },
                    label = { Text("Contraseña del respaldo") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !procesando,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (modo == "crear") {
                    OutlinedTextField(
                        value = confirmacion,
                        onValueChange = { confirmacion = it; mensaje = null },
                        label = { Text("Confirmar contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !procesando,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                }
                Text(
                    if (modo == "crear") {
                        "Ruralitos no guarda esta contraseña y no podrá recuperarla."
                    } else {
                        "La importación no reemplaza tu perfil ni tu sesión actual."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            mensaje?.let {
                MensajeEstadoRuralitos(
                    titulo = "Revisa la información",
                    descripcion = it,
                    color = if (esError) RojoClinico else CianRuralitos,
                    simbolo = if (esError) "!" else "✓"
                )
            }
            BotonPrincipalRuralitos(
                texto = when {
                    procesando -> "Procesando respaldo…"
                    modo == "crear" -> "Elegir ubicación y crear"
                    else -> "Seleccionar archivo para importar"
                },
                descripcion = if (modo == "crear") "Guardar archivo cifrado" else "Buscar en el dispositivo",
                onClick = {
                    mensaje = when {
                        clave.length < 10 -> "La contraseña debe tener al menos 10 caracteres."
                        modo == "crear" && clave != confirmacion -> "Las contraseñas no coinciden."
                        else -> null
                    }
                    esError = mensaje != null
                    if (mensaje == null) {
                        if (modo == "crear") {
                            val fecha = SimpleDateFormat(
                                "yyyyMMdd_HHmm", Locale.getDefault()
                            ).format(Date())
                            crearArchivo.launch("ruralitos_fichas_$fecha.ruralitos")
                        } else {
                            abrirArchivo.launch(arrayOf("application/octet-stream", "*/*"))
                        }
                    }
                },
                enabled = !procesando,
                color = if (modo == "crear") CianRuralitos else NaranjaClinico
            )
        }
        Spacer(Modifier.padding(bottom = 12.dp))
    }
}
