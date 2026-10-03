package com.ruralitos.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import com.ruralitos.app.R
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.AdjuntoFichaEntity
import com.ruralitos.app.data.local.entity.ContaminacionAmbientalEntity
import com.ruralitos.app.data.local.entity.LugarTratamientoEntity
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.RuralitosEmptyState
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.TarjetaRegistroRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Familiograma a partir de una foto: se quita el fondo blanco y se guarda un PNG transparente. */
@Composable
fun FamiliogramaFotoScreen(
    fichaId: Long,
    onContinuar: () -> Unit,
    onSalir: () -> Unit,
    textoRegresar: String = "Volver al panel de la ficha",
    descripcionRegresar: String = "Conservar los datos y salir de esta sección"
) {
    val context = LocalContext.current
    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val scope = rememberCoroutineScope()
    val adjuntos by database.fichaContenidoDao()
        .listarAdjuntos(fichaId)
        .collectAsState(initial = emptyList())
    val adjunto = adjuntos.lastOrNull { it.tipo.equals("FAMILIOGRAMA", ignoreCase = true) }
    var imagenNueva by remember { mutableStateOf<Bitmap?>(null) }
    var imagenGuardada by remember { mutableStateOf<Bitmap?>(null) }
    var intensidadFondo by remember { mutableStateOf(48f) }
    var intensidadAplicada by remember { mutableStateOf(48) }
    var resultadoImagen by remember { mutableStateOf<ResultadoImagenFondo?>(null) }
    var mensaje by remember { mutableStateOf("") }
    var procesando by remember { mutableStateOf(false) }
    var cargandoImagen by remember { mutableStateOf(false) }
    var procesandoFondo by remember { mutableStateOf(false) }
    var confirmarQuitar by remember { mutableStateOf(false) }

    LaunchedEffect(adjunto?.uri, adjunto?.actualizadoEn) {
        val uri = adjunto?.uri
        imagenGuardada = if (uri.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            leerBitmap(context, Uri.parse(uri))
        }
    }

    LaunchedEffect(imagenNueva, intensidadAplicada) {
        val origen = imagenNueva
        if (origen == null) {
            resultadoImagen = null
            procesandoFondo = false
        } else {
            procesandoFondo = true
            resultadoImagen = withContext(Dispatchers.Default) {
                procesarFondoClaro(origen, intensidadAplicada)
            }
            procesandoFondo = false
        }
    }
    val imagenProcesada = resultadoImagen?.bitmap

    fun eliminarArchivoAnterior(uriTexto: String?, excepto: String? = null) {
        if (uriTexto.isNullOrBlank() || uriTexto == excepto) return
        val uri = Uri.parse(uriTexto)
        if (uri.scheme != "file") return
        val archivo = uri.path?.let(::File)?.canonicalFile ?: return
        val directorio = File(context.filesDir, "familiogramas").canonicalFile
        if (archivo.parentFile == directorio && archivo.isFile) archivo.delete()
    }

    fun guardar(bitmap: Bitmap) {
        procesando = true
        mensaje = ""
        scope.launch {
            runCatching {
                val anterior = adjunto?.uri
                val uriInterna = withContext(Dispatchers.IO) {
                    val directorio = File(context.filesDir, "familiogramas").apply { mkdirs() }
                    val archivo = File(
                        directorio,
                        "familiograma_${fichaId}_${System.currentTimeMillis()}.png"
                    )
                    check(guardarPngConTransparenciaVerificada(bitmap, archivo)) {
                        "El archivo guardado no conserva un canal alfa transparente."
                    }
                    Uri.fromFile(archivo).toString()
                }
                withContext(Dispatchers.IO) {
                    database.withTransaction {
                        val actual = database.fichaContenidoDao().listarTodosLosAdjuntos()
                            .lastOrNull {
                                it.fichaId == fichaId &&
                                    it.tipo.equals("FAMILIOGRAMA", ignoreCase = true)
                            }
                        if (actual == null) {
                            database.fichaContenidoDao().guardarAdjunto(
                                AdjuntoFichaEntity(
                                    fichaId = fichaId,
                                    tipo = "FAMILIOGRAMA",
                                    uri = uriInterna
                                )
                            )
                        } else {
                            database.fichaContenidoDao().actualizarUriAdjunto(actual.id, uriInterna)
                        }
                        database.fichaFamiliarDao().marcarPendiente(fichaId)
                    }
                    eliminarArchivoAnterior(anterior, uriInterna)
                }
                imagenNueva = null
                mensaje = "Familiograma guardado con fondo transparente y listo para Excel y PDF."
            }.onFailure {
                mensaje = "No se pudo guardar la imagen seleccionada."
            }
            procesando = false
        }
    }

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            cargandoImagen = true
            mensaje = ""
            scope.launch {
                val imagen = withContext(Dispatchers.IO) {
                    cargarImagenReducida(context, uri, dimensionMaxima = 1600)
                }
                intensidadFondo = 48f
                intensidadAplicada = 48
                imagenNueva = imagen
                cargandoImagen = false
                mensaje = if (imagen == null) "No se pudo leer la imagen seleccionada." else ""
            }
        }
    }

    if (confirmarQuitar && adjunto != null) {
        AlertDialog(
            onDismissRequest = { confirmarQuitar = false },
            title = { Text("Quitar familiograma") },
            text = { Text("La imagen dejará de aparecer en Excel y PDF. Podrás subir otra posteriormente.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarQuitar = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                database.withTransaction {
                                    database.fichaContenidoDao().eliminarAdjunto(adjunto)
                                    database.fichaFamiliarDao().marcarPendiente(fichaId)
                                }
                                eliminarArchivoAnterior(adjunto.uri)
                            }
                            mensaje = "Familiograma eliminado."
                        }
                    }
                ) { Text("Sí, quitar imagen", color = RojoClinico, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarQuitar = false }) { Text("Conservar imagen") }
            }
        )
    }

    PantallaRuralitos(
        titulo = "Imagen del familiograma",
        descripcion = "Sube la fotografía, ajusta la eliminación del papel blanco y guarda un PNG transparente.",
        subtitulo = "Evidencias familiares",
        onVolver = onSalir,
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = "Guardar información de esta sección",
                descripcion = "Continuar con el familiograma actualmente guardado",
                color = MoradoClinico,
                enabled = !procesando && !cargandoImagen && !procesandoFondo,
                onClick = onContinuar
            )

        }
    ) {
        SeccionFormularioRuralitos(
            titulo = if (imagenNueva == null) "Vista previa guardada" else "Vista previa del fondo transparente",
            descripcion = if (imagenNueva == null) {
                if (imagenGuardada == null) "Aún no se ha seleccionado una imagen."
                else "Esta es la imagen que se insertará en Excel y PDF."
            } else {
                "Ajusta la barra hasta conservar las líneas y quitar únicamente el fondo claro."
            }
        ) {
            val vistaPrevia = imagenProcesada ?: imagenGuardada
            if (vistaPrevia != null) {
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 230.dp, max = 440.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Image(
                        bitmap = vistaPrevia.asImageBitmap(),
                        contentDescription = "Vista previa del familiograma",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 230.dp, max = 430.dp)
                            .fondoCuadriculaTransparente()
                            .padding(12.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                        .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .drawBehind {
                            drawRoundRect(
                                color = BordeClinico,
                                cornerRadius = CornerRadius(16.dp.toPx()),
                                style = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("▧", color = AzulClinico, style = MaterialTheme.typography.headlineLarge)
                        Text("Sin imagen", fontWeight = FontWeight.SemiBold)
                        Text("PNG, JPG o WebP", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (imagenNueva != null) {
                Text(
                    "Quitar fondo blanco · ${intensidadFondo.toInt()}%",
                    fontWeight = FontWeight.SemiBold,
                    color = CianRuralitos,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Slider(
                    value = intensidadFondo,
                    onValueChange = { intensidadFondo = it },
                    onValueChangeFinished = { intensidadAplicada = intensidadFondo.toInt() },
                    valueRange = 0f..100f
                )
                Text(
                    "Mueve la barra y suéltala para actualizar. Ruralitos conserva el color original de las líneas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Transparencia detectada: ${resultadoImagen?.porcentajeTransparente ?: 0}% · " +
                        "la cuadrícula confirma las zonas que quedarán sin fondo en el PNG.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CianRuralitos,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (cargandoImagen || procesandoFondo) {
                MensajeEstadoRuralitos(
                    titulo = if (cargandoImagen) "Cargando familiograma…" else "Procesando transparencia…",
                    descripcion = "Puedes seguir desplazándote mientras Ruralitos prepara la imagen.",
                    color = AzulClinico,
                    simbolo = "…",
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
        SeccionFormularioRuralitos(
            titulo = "Gestionar imagen",
            descripcion = "Selecciona una fotografía, revisa el resultado y confirma antes de reemplazar la imagen guardada."
        ) {
            BotonPrincipalRuralitos(
                texto = if (imagenNueva == null) {
                    if (adjunto == null) "Seleccionar foto del familiograma"
                    else "Seleccionar otra foto del familiograma"
                } else {
                    "Elegir una foto diferente"
                },
                descripcion = "Abrir archivos PNG, JPG o WebP",
                color = AzulClinico,
                enabled = !procesando && !cargandoImagen && !procesandoFondo,
                onClick = { selector.launch(arrayOf("image/png", "image/jpeg", "image/webp")) }
            )
            if (imagenProcesada != null) {
                BotonPrincipalRuralitos(
                    texto = if (procesando) "Guardando PNG transparente…" else "Guardar familiograma transparente",
                    descripcion = "Aplicar este ajuste a Excel, PDF y sincronización",
                    color = CianRuralitos,
                    enabled = !procesando && !procesandoFondo && (resultadoImagen?.porcentajeTransparente ?: 0) > 0,
                    onClick = { guardar(imagenProcesada) },
                    modifier = Modifier.padding(top = 10.dp)
                )
                BotonSecundarioRuralitos(
                    texto = "Cancelar este ajuste",
                    descripcion = "Conservar la imagen que ya estaba guardada",
                    enabled = !procesando,
                    onClick = { imagenNueva = null; resultadoImagen = null },
                    modifier = Modifier.padding(top = 9.dp)
                )
            } else if (adjunto != null) {
                BotonPrincipalRuralitos(
                    texto = "Quitar imagen guardada",
                    descripcion = "Eliminarla de esta ficha y de las exportaciones",
                    color = RojoClinico,
                    onClick = { confirmarQuitar = true },
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
        if (mensaje.isNotBlank()) {
            MensajeEstadoRuralitos(
                titulo = if (mensaje.startsWith("No")) "No se completó la acción" else "Imagen actualizada",
                descripcion = mensaje,
                color = if (mensaje.startsWith("No")) RojoClinico else CianRuralitos,
                simbolo = if (mensaje.startsWith("No")) "!" else "✓"
            )
        }
    }
}
@Composable
fun ContaminacionAmbientalScreen(
    fichaId: Long,
    onContinuar: () -> Unit,
    onSalir: () -> Unit,
    textoRegresar: String = "Volver al panel de la ficha",
    descripcionRegresar: String = "Conservar los datos y salir de esta sección"
) {
    val context = LocalContext.current
    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val scope = rememberCoroutineScope()
    val items by database.fichaContenidoDao()
        .listarContaminacion(fichaId)
        .collectAsState(initial = emptyList())
    var editando by remember { mutableStateOf<ContaminacionAmbientalEntity?>(null) }
    var mostrarFormulario by remember { mutableStateOf(false) }

    if (mostrarFormulario) {
        FormularioContaminacionScreen(
            fichaId = fichaId,
            item = editando,
            onGuardar = { item ->
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            if (item.id == 0L) {
                                database.fichaContenidoDao().guardarContaminacion(item)
                            } else {
                                database.fichaContenidoDao().actualizarContaminacion(item)
                            }
                        }
                    }.onSuccess {
                        mostrarFormulario = false
                        editando = null
                    }.onFailure {
                        Toast.makeText(context, "No se pudo guardar el informe.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCancelar = {
                mostrarFormulario = false
                editando = null
            }
        )
        return
    }

    PantallaRuralitos(
        titulo = "Contaminación ambiental",
        descripcion = "Registra la fecha, el tipo de contaminación y su posible causante.",
        subtitulo = "Entorno familiar",
        onVolver = onSalir,
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = "Guardar información de esta sección",
                descripcion = "Los informes registrados ya están guardados",
                color = CianRuralitos,
                onClick = onContinuar
            )

        }
    ) {
        SeccionFormularioRuralitos(
            titulo = "Informes identificados",
            descripcion = "${items.size} registro(s). Cada dato se exporta por separado en la hoja 4."
        ) {
            BotonPrincipalRuralitos(
                texto = "Registrar contaminación",
                descripcion = "Agregar fecha, descripción y causante",
                color = NaranjaClinico,
                onClick = {
                    editando = null
                    mostrarFormulario = true
                }
            )
        }
        items.forEach { item ->
            TarjetaRegistroRuralitos(
                titulo = item.fechaInforme,
                descripcion = item.tipoContaminanteDescripcion,
                simbolo = "A",
                color = NaranjaClinico,
                onEditar = {
                    editando = item
                    mostrarFormulario = true
                },
                onEliminar = {
                    scope.launch(Dispatchers.IO) {
                        database.fichaContenidoDao().eliminarContaminacion(item)
                    }
                }
            ) {
                Text(
                    "Causante: ${item.causanteContaminacion.ifBlank { "Sin registrar" }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (items.isEmpty()) {
            RuralitosEmptyState(
                titulo = "Sin contaminación registrada",
                descripcion = "Puedes continuar cuando no se haya identificado contaminación ambiental.",
                ilustracion = R.drawable.ruralitos_icono_ambiente
            )
        }
    }
}

@Composable
fun LugaresTratamientoScreen(
    fichaId: Long,
    onContinuar: () -> Unit,
    onSalir: () -> Unit,
    textoRegresar: String = "Volver al panel de la ficha",
    descripcionRegresar: String = "Conservar los datos y salir de esta sección"
) {
    val context = LocalContext.current
    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val scope = rememberCoroutineScope()
    val lugares by database.fichaContenidoDao()
        .listarLugaresTratamiento(fichaId)
        .collectAsState(initial = emptyList())
    var texto by remember { mutableStateOf("") }
    var editando by remember { mutableStateOf<LugarTratamientoEntity?>(null) }
    var eliminar by remember { mutableStateOf<LugarTratamientoEntity?>(null) }

    eliminar?.let { seleccionado ->
        AlertDialog(
            onDismissRequest = { eliminar = null },
            title = { Text("Eliminar lugar o persona") },
            text = { Text("Se eliminará “${seleccionado.descripcion}” de la ficha.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        eliminar = null
                        scope.launch(Dispatchers.IO) {
                            database.withTransaction {
                                database.fichaContenidoDao().eliminarLugarTratamiento(seleccionado)
                                database.fichaFamiliarDao().marcarPendiente(fichaId)
                            }
                        }
                    }
                ) { Text("Sí, eliminar", color = RojoClinico, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { eliminar = null }) { Text("Conservar registro") }
            }
        )
    }

    PantallaRuralitos(
        titulo = "Lugar o persona para la atención",
        descripcion = "Registra centros de salud, lugares alternativos o personas de confianza a quienes acude la familia.",
        subtitulo = "Red de atención",
        onVolver = onSalir,
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = "Guardar información de esta sección",
                descripcion = "Los lugares registrados ya están guardados",
                color = MoradoClinico,
                onClick = onContinuar
            )

        }
    ) {
        SeccionFormularioRuralitos(
            titulo = if (editando == null) "Agregar lugar o persona" else "Editar registro seleccionado",
            descripcion = "Anota dónde o con quién se atiende la familia cuando alguien se enferma: un centro de salud, un hospital, un médico particular, un curandero o una persona de confianza. Puedes registrar hasta 4."
        ) {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Centro, lugar o persona") },
                supportingText = { Text("Escribe una descripción clara y reconocible") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            BotonPrincipalRuralitos(
                texto = when {
                    editando != null -> "Guardar cambios del registro"
                    lugares.size >= 4 -> "Límite de 4 registros alcanzado"
                    else -> "Guardar nuevo lugar o persona"
                },
                descripcion = if (lugares.size >= 4 && editando == null) {
                    "Edita o elimina un registro para poder agregar otro"
                } else {
                    "Guardar en el siguiente renglón disponible de la hoja 4"
                },
                color = if (editando == null) CianRuralitos else AzulClinico,
                enabled = texto.isNotBlank() && (editando != null || lugares.size < 4),
                onClick = {
                    if (texto.isNotBlank()) {
                        val actual = editando
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                database.withTransaction {
                                    if (actual == null) {
                                        check(lugares.size < 4) { "La ficha ya tiene cuatro lugares de atención." }
                                        database.fichaContenidoDao().guardarLugarTratamiento(
                                            LugarTratamientoEntity(
                                                fichaId = fichaId,
                                                descripcion = texto.trim()
                                            )
                                        )
                                    } else {
                                        database.fichaContenidoDao().actualizarLugarTratamiento(
                                            actual.copy(descripcion = texto.trim())
                                        )
                                    }
                                    database.fichaFamiliarDao().marcarPendiente(fichaId)
                                }
                            }
                            texto = ""
                            editando = null
                        }
                    }
                },
                modifier = Modifier.padding(top = 11.dp)
            )
            if (editando != null) {
                BotonSecundarioRuralitos(
                    texto = "Cancelar edición",
                    descripcion = "Mantener el registro como estaba",
                    onClick = {
                        editando = null
                        texto = ""
                    },
                    modifier = Modifier.padding(top = 9.dp)
                )
            }
        }
        if (lugares.isEmpty()) {
            RuralitosEmptyState(
                titulo = "Sin red de atención registrada",
                descripcion = "Agrega un centro, lugar o persona de confianza.",
                ilustracion = R.drawable.ruralitos_icono_red
            )
        } else {
            MensajeEstadoRuralitos(
                titulo = "${lugares.size} lugar(es) o persona(s)",
                descripcion = if (lugares.size >= 4) {
                    "Ya registraste los 4 lugares permitidos. Puedes editar o eliminar uno."
                } else {
                    "Puedes editar cada registro o agregar ${4 - lugares.size} más."
                },
                color = AzulClinico,
                simbolo = lugares.size.toString()
            )
        }
        lugares.forEachIndexed { index, item ->
            TarjetaRegistroRuralitos(
                titulo = "Opción de atención ${index + 1}",
                descripcion = item.descripcion,
                simbolo = (index + 1).toString(),
                color = CianRuralitos,
                onEditar = {
                    editando = item
                    texto = item.descripcion
                },
                onEliminar = { eliminar = item }
            )
        }
    }
}

private fun leerBitmap(context: android.content.Context, uri: Uri): Bitmap? =
    if (uri.scheme == "file") {
        uri.path?.let(BitmapFactory::decodeFile)
    } else {
        context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
    }

private fun reducirImagen(bitmap: Bitmap, maximo: Int): Bitmap {
    val mayor = maxOf(bitmap.width, bitmap.height)
    if (mayor <= maximo) return bitmap
    val escala = maximo.toFloat() / mayor
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * escala).toInt(),
        (bitmap.height * escala).toInt(),
        true
    )
}
