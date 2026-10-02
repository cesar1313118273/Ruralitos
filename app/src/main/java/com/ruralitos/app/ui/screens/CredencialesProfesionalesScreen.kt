package com.ruralitos.app.ui.screens

import com.ruralitos.app.ui.components.EncabezadoPantallaRuralitos
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.statusBarsPadding
import com.ruralitos.app.ui.theme.FondoClinico
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.ruralitos.app.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.ruralitos.app.ui.components.BotonVolverRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ruralitos.app.data.local.entity.UsuarioEntity
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CredencialesProfesionalesScreen(
    usuario: UsuarioEntity,
    procesando: Boolean,
    mensaje: String?,
    obligatorio: Boolean = false,
    onGuardar: (codigoSenescyt: String, firma: Bitmap?) -> Unit,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var codigo by remember(usuario.id) { mutableStateOf(usuario.codigoSenescyt) }
    var modo by remember { mutableStateOf("DIBUJAR") }
    var foto by remember { mutableStateOf<Bitmap?>(null) }
    var intensidadFondo by remember { mutableStateOf(55f) }
    var intensidadAplicada by remember { mutableStateOf(55) }
    val trazos = remember { mutableStateListOf<List<Offset>>() }
    val trazoActual = remember { mutableStateListOf<Offset>() }
    var tamanoLienzo by remember { mutableStateOf(IntSize.Zero) }
    var error by remember { mutableStateOf<String?>(null) }
    var cargandoFoto by remember { mutableStateOf(false) }
    var procesandoFondo by remember { mutableStateOf(false) }
    var resultadoFoto by remember { mutableStateOf<ResultadoImagenFondo?>(null) }

    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            modo = "FOTO"
            cargandoFoto = true
            error = null
            scope.launch {
                val imagen = withContext(Dispatchers.IO) {
                    cargarImagenReducida(context, uri, dimensionMaxima = 1400)
                }
                foto = imagen
                intensidadAplicada = intensidadFondo.toInt()
                cargandoFoto = false
                error = if (imagen == null) "No se pudo leer esa imagen." else null
            }
        }
    }

    LaunchedEffect(foto, intensidadAplicada) {
        val origen = foto
        if (origen == null) {
            resultadoFoto = null
            procesandoFondo = false
        } else {
            procesandoFondo = true
            resultadoFoto = withContext(Dispatchers.Default) {
                procesarFondoClaro(origen, intensidadAplicada)
            }
            procesandoFondo = false
        }
    }
    val fotoProcesada = resultadoFoto?.bitmap

Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoClinico)
            .formularioSeguro()
    ) {
        CabeceraIdentidadProfesional(if (!obligatorio) onRegresar else null)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TarjetaCodigoProfesional(
                    codigo = codigo,
                    onCodigoCambio = {
                        codigo = it.uppercase().filter { c ->
                            c.isLetterOrDigit() || c in ".-_/".toCharArray()
                        }.take(40)
                        error = null
                    }
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.97f),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp,
                    border = BorderStroke(
                        1.dp,
                        AzulMarca.copy(alpha = 0.10f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        EncabezadoTarjetaCredencial(
                            simbolo = "✎",
                            titulo = "Firma del responsable",
                            descripcion = "Dibújala con el dedo o sube una foto y elimina su fondo con la barra.",
                            color = AzulMarcaOscuro
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectorModoFirma(
                                texto = "Dibujar firma",
                                simbolo = "✎",
                                seleccionado = modo == "DIBUJAR",
                                onClick = {
                                    modo = "DIBUJAR"
                                },
                                modifier = Modifier.weight(1f)
                            )

                            SelectorModoFirma(
                                texto = "Subir imagen",
                                simbolo = "▧",
                                seleccionado = modo == "FOTO",
                                onClick = {
                                    selectorFoto.launch(
                                        arrayOf(
                                            "image/png",
                                            "image/jpeg"
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (modo == "DIBUJAR") {
                            Text(
                                text = "Firma dentro del recuadro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AzulMarca,
                                modifier = Modifier.padding(
                                    top = 18.dp,
                                    bottom = 9.dp
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f)
                                    .background(
                                        Color(0xFFF6F9FB),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = AzulMarca.copy(alpha = 0.30f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .onSizeChanged {
                                        tamanoLienzo = it
                                    }
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = {
                                                trazoActual.clear()
                                                trazoActual.add(it)
                                            },
                                            onDrag = { cambio, _ ->
                                                cambio.consume()

                                                val punto = cambio.position
                                                val ultimo =
                                                    trazoActual.lastOrNull()

                                                if (
                                                    punto.x in
                                                        0f..size.width.toFloat() &&
                                                    punto.y in
                                                        0f..size.height.toFloat() &&
                                                    (
                                                        ultimo == null ||
                                                            (punto - ultimo)
                                                                .getDistance() >= 2.5f
                                                    )
                                                ) {
                                                    trazoActual.add(punto)
                                                }
                                            },
                                            onDragEnd = {
                                                if (trazoActual.size > 1) {
                                                    trazos.add(
                                                        trazoActual.toList()
                                                    )
                                                }

                                                trazoActual.clear()
                                            },
                                            onDragCancel = {
                                                trazoActual.clear()
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (
                                    trazos.isEmpty() &&
                                    trazoActual.isEmpty()
                                ) {
                                    Text(
                                        text = "✎",
                                        color = AzulMarca.copy(alpha = 0.25f),
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Light
                                    )
                                }

                                Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    trazos.forEach { trazo ->
                                        trazo.zipWithNext()
                                            .forEach { (inicio, fin) ->
                                                drawLine(
                                                    color =
                                                        Color(0xFF172033),
                                                    start = inicio,
                                                    end = fin,
                                                    strokeWidth =
                                                        5.dp.toPx(),
                                                    cap = StrokeCap.Round
                                                )
                                            }
                                    }

                                    trazoActual.zipWithNext()
                                        .forEach { (inicio, fin) ->
                                            drawLine(
                                                color = Color(0xFF172033),
                                                start = inicio,
                                                end = fin,
                                                strokeWidth = 5.dp.toPx(),
                                                cap = StrokeCap.Round
                                            )
                                        }
                                }
                            }

                            BotonContornoAzul(
                                texto = "Borrar y volver a dibujar",
                                simbolo = "↻",
                                onClick = {
                                    trazos.clear()
                                    trazoActual.clear()
                                },
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        } else {
                            fotoProcesada?.let { bitmap ->
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription =
                                        "Vista previa de firma transparente",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                        .aspectRatio(3f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .fondoCuadriculaTransparente()
                                )

                                Text(
                                    text = "Control para quitar el fondo",
                                    style =
                                        MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AzulMarca,
                                    modifier = Modifier.padding(top = 16.dp)
                                )

                                Slider(
                                    value = intensidadFondo,
                                    onValueChange = {
                                        intensidadFondo = it
                                    },
                                    onValueChangeFinished = {
                                        intensidadAplicada =
                                            intensidadFondo.toInt()
                                    },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AzulMarca,
                                        activeTrackColor = AzulMarca,
                                        inactiveTrackColor =
                                            AzulMarca.copy(alpha = 0.16f)
                                    )
                                )

                                Text(
                                    text =
                                        "Intensidad ${intensidadFondo.toInt()}% · " +
                                            "suelta la barra para actualizar la vista previa.",
                                    style =
                                        MaterialTheme.typography.bodySmall,
                                    color =
                                        MaterialTheme.colorScheme
                                            .onSurfaceVariant
                                )

                                Text(
                                    text =
                                        "Transparencia detectada: " +
                                            "${resultadoFoto?.porcentajeTransparente ?: 0}% · " +
                                            "la cuadrícula visible detrás de la firma " +
                                            "confirma las zonas sin fondo.",
                                    style =
                                        MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CianRuralitos,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            } ?: MensajeEstadoRuralitos(
                                titulo = "Selecciona una foto de la firma",
                                descripcion =
                                    "Admite archivos PNG o JPG guardados en el dispositivo.",
                                color = AzulMarca,
                                simbolo = "+"
                            )
                        }

                        if (cargandoFoto || procesandoFondo) {
                            MensajeEstadoRuralitos(
                                titulo = if (cargandoFoto) {
                                    "Cargando imagen…"
                                } else {
                                    "Procesando transparencia…"
                                },
                                descripcion =
                                    "La interfaz permanece disponible mientras preparamos la vista previa.",
                                color = AzulMarca,
                                simbolo = "…",
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }

                        if (
                            !usuario.firmaUri.isNullOrBlank() &&
                            trazos.isEmpty() &&
                            foto == null
                        ) {
                            EstadoFirmaProtegida(
                                modifier = Modifier.padding(top = 14.dp)
                            )
                        }
                    }
                }

                (error ?: mensaje)?.let {
                    MensajeEstadoRuralitos(
                        titulo = "Revisa la identidad profesional",
                        descripcion = it,
                        color = MaterialTheme.colorScheme.error,
                        simbolo = "!"
                    )
                }




            }
        }
        HorizontalDivider(color = Color(0xFFE2ECF1))
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BotonPrincipalAzul(
                    texto = if (procesando) {
                        "Guardando identidad…"
                    } else {
                        "Guardar identidad profesional"
                    },
                    simbolo = "▣",
                    enabled =
                        !procesando &&
                            !cargandoFoto &&
                            !procesandoFondo,
                    onClick = {
                        val firmaNueva = when {
                            modo == "FOTO" ->
                                fotoProcesada

                            trazos.isNotEmpty() &&
                                tamanoLienzo != IntSize.Zero ->
                                crearFirmaDesdeTrazos(
                                    trazos.toList(),
                                    tamanoLienzo
                                )

                            else ->
                                null
                        }

                        error = when {
                            codigo.length < 3 ->
                                "Ingresa un código SENESCYT válido."

                            modo == "FOTO" &&
                                fotoProcesada != null &&
                                !tieneTransparenciaReal(fotoProcesada) ->
                                "Aumenta la barra hasta que la cuadrícula aparezca detrás de la firma."

                            obligatorio &&
                                firmaNueva == null &&
                                usuario.firmaUri.isNullOrBlank() ->
                                "Dibuja o sube tu firma profesional."

                            else ->
                                null
                        }

                        if (error == null) {
                            onGuardar(
                                codigo.trim(),
                                firmaNueva
                            )
                        }
                    }
                )
        }
    }
}

private val AzulMarca = Color(0xFF1565C0)
private val AzulMarcaOscuro = Color(0xFF0A2A5E)
private val AzulMarcaMuyClaro = Color(0xFFE8EFFA)

@Composable
private fun CabeceraIdentidadProfesional(onVolver: (() -> Unit)?) {
    EncabezadoPantallaRuralitos(
        titulo = "Identidad profesional",
        subtitulo = null,
        paso = null,
        totalPasos = null,
        etiquetaPaso = "",
        onVolver = onVolver,
        descripcion = "Configura una sola vez el código y la firma que se colocarán automáticamente en tus fichas."
    )
}

@Composable
private fun TarjetaCodigoProfesional(
    codigo: String,
    onCodigoCambio: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.97f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            AzulMarca.copy(alpha = 0.10f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            EncabezadoTarjetaCredencial(
                simbolo = "▣",
                titulo = "Código profesional SENESCYT",
                descripcion =
                    "Este código identifica al responsable que crea o actualiza la ficha familiar.",
                color = AzulMarca
            )

            OutlinedTextField(
                value = codigo,
                onValueChange = onCodigoCambio,
                label = {
                    Text("Código SENESCYT")
                },
                supportingText = {
                    Text(
                        "Se reutilizará como código del responsable."
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AzulMarca,
                    unfocusedBorderColor =
                        AzulMarca.copy(alpha = 0.40f),
                    focusedLabelColor = AzulMarca,
                    cursorColor = AzulMarca
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun EncabezadoTarjetaCredencial(
    simbolo: String,
    titulo: String,
    descripcion: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            color = AzulMarcaMuyClaro,
            shape = CircleShape
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = simbolo,
                    color = color,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 13.dp)
        ) {
            Text(
                text = titulo,
                color = AzulMarcaOscuro,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = descripcion,
                color = Color(0xFF5B7083),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SelectorModoFirma(
    texto: String,
    simbolo: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp),
        color = if (seleccionado) {
            AzulMarca
        } else {
            Color.White
        },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.3.dp,
            if (seleccionado) {
                AzulMarca
            } else {
                AzulMarca.copy(alpha = 0.28f)
            }
        ),
        shadowElevation = if (seleccionado) {
            4.dp
        } else {
            0.dp
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = simbolo,
                color = if (seleccionado) {
                    Color.White
                } else {
                    AzulMarca
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = texto,
                color = if (seleccionado) {
                    Color.White
                } else {
                    AzulMarca
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 7.dp)
            )
        }
    }
}

@Composable
private fun EstadoFirmaProtegida(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFE3F4F7),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            CianRuralitos.copy(alpha = 0.30f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                color = CianRuralitos,
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 13.dp)
            ) {
                Text(
                    text = "Firma actual protegida",
                    color = AzulMarcaOscuro,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text =
                        "Si no dibujas ni subes otra, se conservará la firma guardada.",
                    color = Color(0xFF5B7083),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun BotonPrincipalAzul(
    texto: String,
    simbolo: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp),
        color = if (enabled) {
            AzulMarca
        } else {
            AzulMarca.copy(alpha = 0.45f)
        },
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (enabled) {
            5.dp
        } else {
            0.dp
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = simbolo,
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = texto,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

@Composable
private fun BotonContornoAzul(
    texto: String,
    simbolo: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        color = Color.White.copy(alpha = 0.94f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.4.dp,
            if (enabled) {
                AzulMarca.copy(alpha = 0.72f)
            } else {
                AzulMarca.copy(alpha = 0.20f)
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = simbolo,
                color = if (enabled) {
                    AzulMarca
                } else {
                    AzulMarca.copy(alpha = 0.35f)
                },
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = texto,
                color = if (enabled) {
                    AzulMarca
                } else {
                    AzulMarca.copy(alpha = 0.35f)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

@Composable
private fun FondoIdentidadProfesional(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        drawCircle(
            color = Color(0xFF60CFF3).copy(alpha = 0.11f),
            radius = size.width * 0.50f,
            center = Offset(
                size.width * 0.95f,
                size.height * 0.20f
            )
        )

        val ondaIzquierda = Path().apply {
            moveTo(
                0f,
                size.height * 0.34f
            )

            cubicTo(
                size.width * 0.08f,
                size.height * 0.40f,
                size.width * 0.05f,
                size.height * 0.49f,
                0f,
                size.height * 0.53f
            )

            close()
        }

        drawPath(
            path = ondaIzquierda,
            color = Color(0xFF42C9E8).copy(alpha = 0.16f)
        )

        val ondaInferior = Path().apply {
            moveTo(
                0f,
                size.height * 0.86f
            )

            cubicTo(
                size.width * 0.18f,
                size.height * 0.80f,
                size.width * 0.34f,
                size.height * 0.94f,
                size.width * 0.52f,
                size.height * 0.90f
            )

            cubicTo(
                size.width * 0.70f,
                size.height * 0.86f,
                size.width * 0.84f,
                size.height * 0.79f,
                size.width,
                size.height * 0.84f
            )

            lineTo(
                size.width,
                size.height
            )

            lineTo(
                0f,
                size.height
            )

            close()
        }

        drawPath(
            path = ondaInferior,
            color = Color(0xFF64D6C8).copy(alpha = 0.14f)
        )
    }
}

fun guardarFirmaProfesional(context: Context, usuarioId: Long, bitmap: Bitmap): String {
    val directorio = File(context.filesDir, "firmas_profesionales").apply { mkdirs() }
    val version = System.currentTimeMillis()
    val archivo = File(directorio, "firma_${usuarioId}_$version.png")
    val temporal = File(directorio, "firma_${usuarioId}_$version.tmp")
    check(guardarPngConTransparenciaVerificada(bitmap, temporal)) {
        "La firma no contiene transparencia PNG verificable."
    }
    if (archivo.exists()) archivo.delete()
    if (!temporal.renameTo(archivo)) {
        temporal.copyTo(archivo, overwrite = true)
        temporal.delete()
    }
    return Uri.fromFile(archivo).toString()
}

private fun crearFirmaDesdeTrazos(trazos: List<List<Offset>>, tamano: IntSize): Bitmap {
    val ancho = 1200
    val alto = 400
    val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888).apply {
        setHasAlpha(true)
        eraseColor(AndroidColor.TRANSPARENT)
    }
    val canvas = AndroidCanvas(bitmap)
    val pintura = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(18, 24, 38)
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }
    val escalaX = ancho.toFloat() / tamano.width.coerceAtLeast(1)
    val escalaY = alto.toFloat() / tamano.height.coerceAtLeast(1)
    trazos.forEach { trazo ->
        trazo.zipWithNext().forEach { (inicio, fin) ->
            canvas.drawLine(inicio.x * escalaX, inicio.y * escalaY, fin.x * escalaX, fin.y * escalaY, pintura)
        }
    }
    return recortarBordesTransparentes(bitmap)
}
