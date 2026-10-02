package com.ruralitos.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.ruralitos.app.VerdeOscuro
import com.ruralitos.app.R
import com.ruralitos.app.data.export.ArchivoFichaDescargado
import com.ruralitos.app.data.export.GestorDescargasFicha
import com.ruralitos.app.data.agenda.PlanificadorSeguimiento
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.HistorialFichaEntity
import com.ruralitos.app.domain.ValidadorFicha
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.VerdeSuaveRuralitos
import com.ruralitos.app.ui.theme.NaranjaSuaveRuralitos
import com.ruralitos.app.ui.theme.BordeClinico
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun RevisionFinalFichaScreen(
    fichaId: Long,
    usuarioId: Long?,
    usuarioNombre: String,
    onAbrirSeccion: (String) -> Unit,
    onFinalizada: (String) -> Unit,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val ficha by database.fichaFamiliarDao().observarPorId(fichaId).collectAsState(initial = null)
    val miembros by database.fichaContenidoDao().listarMiembros(fichaId).collectAsState(initial = emptyList())
    val calificaciones by database.fichaContenidoDao().listarCalificaciones(fichaId).collectAsState(initial = emptyList())
    val adjuntos by database.fichaContenidoDao().listarAdjuntos(fichaId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var procesando by remember { mutableStateOf(false) }
    var descargarPdf by remember { mutableStateOf(false) }
    var descargarExcel by remember { mutableStateOf(false) }
    var archivosGenerados by remember { mutableStateOf<List<ArchivoFichaDescargado>>(emptyList()) }
    var estadoGuardado by remember { mutableStateOf("BORRADOR") }
    var mostrarCompartir by remember { mutableStateOf(false) }

    val fichaActual = ficha
    if (fichaActual == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val requisitos = ValidadorFicha.revisar(
        ficha = fichaActual,
        cantidadMiembros = miembros.size,
        cantidadCalificaciones = calificaciones.size,
        tiposAdjuntos = adjuntos.map { it.tipo.uppercase() }.toSet()
    )
    val pendientes = requisitos.filterNot { it.cumplido }

    val finalizarFicha: () -> Unit = {
        procesando = true
        scope.launch {
            val nuevoEstado = if (pendientes.isEmpty()) "COMPLETA" else "BORRADOR"
            runCatching {
                withContext(Dispatchers.IO) {
                    database.fichaFamiliarDao().actualizarEstado(fichaId, nuevoEstado, usuarioId)
                    database.historialFichaDao().registrar(
                        HistorialFichaEntity(
                            fichaId = fichaId,
                            numeroFicha = fichaActual.numeroFichaFamiliar,
                            usuarioId = usuarioId,
                            usuarioNombre = usuarioNombre,
                            accion = if (nuevoEstado == "COMPLETA") "FICHA_COMPLETADA"
                                else "FICHA_GUARDADA_PENDIENTE"
                        )
                    )
                    if (nuevoEstado == "COMPLETA" && usuarioId != null)
                        PlanificadorSeguimiento.prepararFicha(context, fichaId, usuarioId)
                    else if (nuevoEstado != "COMPLETA")
                        PlanificadorSeguimiento.retirarFicha(context, fichaId)
                }
                GestorDescargasFicha.generar(
                    context = context,
                    fichaId = fichaId,
                    nombreBase = "ficha_" + fichaActual.numeroFichaFamiliar +
                        "_" + fichaActual.cedulaJefeHogar,
                    descargarPdf = descargarPdf,
                    descargarExcel = descargarExcel
                )
            }.onSuccess { descarga ->
                procesando = false
                estadoGuardado = nuevoEstado
                archivosGenerados = descarga.archivos
                val mensaje = when {
                    descarga.errores.isNotEmpty() ->
                        "La ficha se guardó, pero hubo errores: " + descarga.errores.joinToString()
                    nuevoEstado == "COMPLETA" -> "Ficha finalizada correctamente."
                    else -> "Ficha guardada como pendiente."
                }
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                if (descarga.archivos.isNotEmpty()) mostrarCompartir = true
                else onFinalizada(nuevoEstado)
            }.onFailure {
                procesando = false
                Toast.makeText(
                    context,
                    "No se pudo finalizar la ficha: " + (it.message ?: "error desconocido"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    if (mostrarCompartir) {
        AlertDialog(
            // La decisión es importante: tocar fuera o pulsar Atrás ya no cierra
            // accidentalmente esta ventana ni abandona la ficha.
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text("Archivos descargados") },
            text = {
                Column {
                    Text("Se guardaron en Descargas/Ruralitos:")
                    Text(
                        archivosGenerados.joinToString("\n") { "• " + it.nombre },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 9.dp)
                    )
                    Text(
                        "¿Deseas compartirlos por WhatsApp, Telegram, correo u otra aplicación?",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!GestorDescargasFicha.compartir(context, archivosGenerados)) {
                        Toast.makeText(context, "No se encontró una aplicación para compartir.", Toast.LENGTH_LONG).show()
                    }
                    mostrarCompartir = false
                    onFinalizada(estadoGuardado)
                }) { Text("Compartir ahora") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarCompartir = false
                    onFinalizada(estadoGuardado)
                }) { Text("Conservar en el celular") }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .formularioSeguro()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EncabezadoRuralitos(
            titulo = "Revisión y finalización",
            descripcion = "Comprueba los datos, elige qué archivos deseas descargar y finaliza la ficha.",
            paso = "Último paso",
            color = MoradoClinico
        )

        SeccionFormularioRuralitos(
            titulo = "Comprobación de información",
            descripcion = "Toca un elemento pendiente para ir directamente a la sección que debes completar."
        ) {
            requisitos.forEach { requisito ->
                val color = if (requisito.cumplido) CianRuralitos else NaranjaClinico
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 5.dp)
                        .clickable(
                            enabled = !requisito.cumplido && requisito.seccion != "firma"
                        ) {
                            onAbrirSeccion(requisito.seccion)
                        },
                    shape = RoundedCornerShape(11.dp),
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.14f))
                ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = color, shape = RoundedCornerShape(9.dp)) {
                            Text(
                                if (requisito.cumplido) "✓" else "!",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(requisito.titulo, fontWeight = FontWeight.Bold)
                            Text(
                                when {
                                    requisito.cumplido -> "Información verificada"
                                    requisito.seccion == "firma" ->
                                        "Pendiente · configúrala en Menú > Identidad profesional"
                                    else -> "Pendiente · toca para completar"
                                },
                                color = color,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

        }

        SeccionFormularioRuralitos(
            titulo = "Todas las secciones de la ficha",
            descripcion = "Revisa o corrige cualquier apartado antes de finalizar."
        ) {
            listOf(
                "datos" to "1. Datos de la familia",
                "ubicacion" to "2. Dirección y vivienda",
                "miembros" to "3. Integrantes y diagnósticos",
                "salud" to "4. Embarazo y mortalidad",
                "dispensarizacion" to "Registro general",
                "riesgos" to "5. Calificación del riesgo familiar",
                "gestion" to "6. Plan y seguimiento del riesgo",
                "familiograma" to "7. Familiograma",
                "croquis" to "8. Croquis, GPS y mapa",
                "contaminacion" to "9. Contaminación ambiental",
                "tratamiento" to "10. Lugar de atención o persona"
            ).forEach { (seccion, titulo) ->
                Text(
                    text = "$titulo  ›",
                    modifier = Modifier.fillMaxWidth().clickable { onAbrirSeccion(seccion) }
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                    color = AzulClinico,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Surface(
            color = if (pendientes.isEmpty()) VerdeSuaveRuralitos else NaranjaSuaveRuralitos,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                (if (pendientes.isEmpty()) CianRuralitos else NaranjaClinico).copy(alpha = 0.45f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (pendientes.isEmpty()) "✓ La ficha quedará finalizada"
                    else "La ficha se guardará como pendiente",
                    color = if (pendientes.isEmpty()) CianRuralitos else NaranjaClinico,
                    fontWeight = FontWeight.SemiBold
                )
                if (pendientes.isNotEmpty()) {
                    Text(
                        "Faltan " + pendientes.size + " requisito(s). Podrás completarlos después sin perder información.",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        SeccionFormularioRuralitos(
            titulo = "Archivos para descargar",
            descripcion = "Marca ninguno, uno o los dos formatos."
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 650.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OpcionDescarga(
                            descargarPdf, { descargarPdf = it }, "Documento PDF",
                        "4 páginas A4 horizontales", R.drawable.ruralitos_icono_pdf, com.ruralitos.app.ui.theme.RojoClinico, Modifier.weight(1f)
                        )
                        OpcionDescarga(
                            descargarExcel, { descargarExcel = it }, "Archivo Excel",
                            "Plantilla oficial editable", R.drawable.ruralitos_icono_excel, CianRuralitos, Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OpcionDescarga(
                            descargarPdf, { descargarPdf = it }, "Documento PDF",
                            "4 páginas A4 horizontales", R.drawable.ruralitos_icono_pdf, com.ruralitos.app.ui.theme.RojoClinico
                        )
                        OpcionDescarga(
                            descargarExcel, { descargarExcel = it }, "Archivo Excel",
                            "Plantilla oficial editable", R.drawable.ruralitos_icono_excel, CianRuralitos
                        )
                    }
                }
            }
            Text(
                when {
                    descargarPdf && descargarExcel -> "Se descargarán PDF y Excel."
                    descargarPdf -> "Se descargará únicamente el PDF."
                    descargarExcel -> "Se descargará únicamente el Excel."
                    else -> "No se descargarán archivos."
                },
                color = AzulClinico,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (procesando) {
            Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = AzulClinico)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("Finalizando ficha…", fontWeight = FontWeight.Bold)
                        Text("Guardando y preparando los archivos seleccionados.")
                    }
                }
            }
        }

        BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            val descripcionFinal = if (pendientes.isEmpty()) {
                "Guardar como completa y procesar la selección"
            } else {
                "Guardar como pendiente y procesar la selección"
            }
            if (maxWidth >= 650.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotonSecundarioRuralitos(
                        "Regresar a la página anterior", onRegresar, Modifier.weight(1f),
                        "Volver sin finalizar", !procesando
                    )
                    BotonPrincipalRuralitos(
                        "Finalizar ficha", finalizarFicha, Modifier.weight(1f),
                        descripcionFinal, !procesando, CianRuralitos
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotonSecundarioRuralitos(
                        "Regresar a la página anterior", onRegresar,
                        descripcion = "Volver sin finalizar", enabled = !procesando
                    )
                    BotonPrincipalRuralitos(
                        "Finalizar ficha", finalizarFicha,
                        descripcion = descripcionFinal, enabled = !procesando, color = CianRuralitos
                    )
                }
            }
        }
    }
}

@Composable
private fun OpcionDescarga(
    seleccionada: Boolean,
    onCambio: (Boolean) -> Unit,
    titulo: String,
    descripcion: String,
    icono: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onCambio(!seleccionada) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionada) color.copy(alpha = 0.06f) else Color.White
        ),
        border = BorderStroke(
            if (seleccionada) 2.dp else 1.dp,
            if (seleccionada) color else BordeClinico
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(icono),
                contentDescription = null,
                modifier = Modifier.height(42.dp).padding(end = 2.dp)
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(titulo, fontWeight = FontWeight.SemiBold)
                Text(
                    descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(
                checked = seleccionada,
                onCheckedChange = onCambio,
                colors = CheckboxDefaults.colors(checkedColor = color)
            )
        }
    }
}
