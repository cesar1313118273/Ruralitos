package com.ruralitos.app.ui.screens

import android.content.Context
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.components.VentanaRuralitos
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
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.VerdeSuaveRuralitos
import com.ruralitos.app.ui.theme.NaranjaSuaveRuralitos
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.domain.EtiquetasFicha
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

    // Una ficha que me compartieron solo para ver: aquí solo se descargan los archivos, sin tocar la ficha.
    val soloLectura = EtiquetasFicha.soloLectura(fichaActual)
    val terminar: (String) -> Unit = { estado -> if (soloLectura) onRegresar() else onFinalizada(estado) }

    val finalizarFicha: () -> Unit = {
        procesando = true
        scope.launch {
            val nuevoEstado = if (soloLectura) fichaActual.estado
                else if (pendientes.isEmpty()) "COMPLETA" else "BORRADOR"
            runCatching {
                if (!soloLectura) withContext(Dispatchers.IO) {
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
                    soloLectura && descarga.errores.isNotEmpty() ->
                        "Hubo errores al descargar: " + descarga.errores.joinToString()
                    soloLectura -> "Archivos descargados."
                    descarga.errores.isNotEmpty() ->
                        "La ficha se guardó, pero hubo errores: " + descarga.errores.joinToString()
                    nuevoEstado == "COMPLETA" -> "Ficha finalizada correctamente."
                    else -> "Ficha guardada como pendiente."
                }
                com.ruralitos.app.ui.components.AvisosRuralitos.mostrar(mensaje)
                if (descarga.archivos.isNotEmpty()) mostrarCompartir = true
                else terminar(nuevoEstado)
            }.onFailure {
                procesando = false
                com.ruralitos.app.ui.components.AvisosRuralitos.mostrar("No se pudo finalizar la ficha: " + (it.message ?: "error desconocido"))
            }
        }
    }

    if (mostrarCompartir) {
        VentanaRuralitos(
            titulo = "Archivos descargados",
            subtitulo = "Se guardaron en Descargas/Ruralitos",
            simbolo = "✓",
            color = CianRuralitos,
            // La decisión es importante: tocar fuera o pulsar Atrás no cierra la ventana ni abandona la ficha.
            cerrarAlTocarFuera = false,
            onCerrar = {
                mostrarCompartir = false
                terminar(estadoGuardado)
            },
            contenido = {
                Text(
                    archivosGenerados.joinToString("\n") { "• " + it.nombre },
                    fontWeight = FontWeight.SemiBold,
                    color = AzulClinicoOscuro
                )
                Text(
                    "¿Deseas compartirlos por WhatsApp, Telegram, correo u otra aplicación?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            acciones = {
                BotonPrincipalRuralitos(
                    texto = "Compartir ahora",
                    color = CianRuralitos,
                    onClick = {
                        if (!GestorDescargasFicha.compartir(context, archivosGenerados)) {
                            com.ruralitos.app.ui.components.AvisosRuralitos.mostrar("No se encontró una aplicación para compartir.")
                        }
                        mostrarCompartir = false
                        terminar(estadoGuardado)
                    }
                )
                BotonSecundarioRuralitos(
                    texto = "Conservar en el celular",
                    onClick = {
                        mostrarCompartir = false
                        terminar(estadoGuardado)
                    }
                )
            }
        )
    }

    PantallaRuralitos(
        titulo = if (soloLectura) "Descargar la ficha" else "Revisión y finalización",
        descripcion = if (soloLectura) "Elige qué archivos deseas descargar. La ficha no se modifica."
            else "Comprueba los datos, elige qué archivos deseas descargar y finaliza la ficha.",
        subtitulo = if (soloLectura) "Solo lectura" else "Último paso",
        onVolver = onRegresar,
        barraAccion = {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val textoBoton = if (soloLectura) "Descargar archivos" else "Finalizar ficha"
                val descripcionFinal = if (soloLectura) {
                    "Guardar en Descargas/Ruralitos"
                } else if (pendientes.isEmpty()) {
                    "Guardar como completa y procesar la selección"
                } else {
                    "Guardar como pendiente y procesar la selección"
                }
                if (maxWidth >= 650.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BotonPrincipalRuralitos(
                            textoBoton, finalizarFicha, Modifier.weight(1f),
                            descripcionFinal, !procesando, CianRuralitos
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BotonPrincipalRuralitos(
                            textoBoton, finalizarFicha,
                            descripcion = descripcionFinal, enabled = !procesando, color = CianRuralitos
                        )
                    }
                }
            }
        }
    ) {
        if (!soloLectura) SeccionFormularioRuralitos(
            titulo = "Comprobación de información",
            descripcion = "Toca cualquier apartado para revisarlo o completar lo que falta antes de finalizar."
        ) {
            requisitos.forEach { requisito ->
                val color = if (requisito.cumplido) CianRuralitos else NaranjaClinico
                FilaRevisionSeccion(
                    color = color,
                    simbolo = if (requisito.cumplido) "✓" else "!",
                    titulo = requisito.titulo,
                    detalle = when {
                        requisito.cumplido -> "Información verificada · toca para revisarla"
                        requisito.seccion == "firma" ->
                            "Pendiente · configúrala en Menú > Identidad profesional"
                        else -> "Pendiente · toca para completar"
                    },
                    habilitada = requisito.seccion != "firma",
                    onClick = { onAbrirSeccion(requisito.seccion) }
                )
            }
            // Apartados que no bloquean la finalización pero se pueden revisar aquí.
            listOf(
                "gestion" to "Plan y seguimiento del riesgo",
                "contaminacion" to "Contaminación ambiental",
                "tratamiento" to "Lugar de atención o persona"
            ).forEach { (seccion, titulo) ->
                FilaRevisionSeccion(
                    color = AzulClinico,
                    simbolo = "›",
                    titulo = titulo,
                    detalle = "Opcional · toca para revisar o corregir",
                    habilitada = true,
                    onClick = { onAbrirSeccion(seccion) }
                )
            }
        }

        if (!soloLectura) Surface(
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
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (procesando) {
            Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = AzulClinico)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("Finalizando ficha…", fontWeight = FontWeight.SemiBold)
                        Text("Guardando y preparando los archivos seleccionados.")
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaRevisionSeccion(
    color: Color,
    simbolo: String,
    titulo: String,
    detalle: String,
    habilitada: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clickable(enabled = habilitada, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.16f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = color, shape = RoundedCornerShape(9.dp)) {
                Text(
                    simbolo,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(titulo, fontWeight = FontWeight.SemiBold)
                Text(
                    detalle,
                    color = color,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
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
