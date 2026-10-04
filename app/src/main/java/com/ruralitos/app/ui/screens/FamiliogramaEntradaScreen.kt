package com.ruralitos.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ruralitos.app.data.familiograma.AlmacenFamiliograma
import com.ruralitos.app.data.familiograma.aIntegranteFamiliograma
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.domain.RolFamiliar
import com.ruralitos.app.domain.SaludoProfesional
import com.ruralitos.app.domain.familiograma.ArmadoFamiliograma
import com.ruralitos.app.domain.familiograma.Familiograma
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.TarjetaFormularioRuralitos
import com.ruralitos.app.ui.familiograma.EditorFamiliogramaScreen
import com.ruralitos.app.ui.familiograma.IconoEditor
import com.ruralitos.app.ui.familiograma.IconoEditorTipo
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.AzulSuaveRuralitos
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.CianSuave
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.TextoSecundario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ModoFamiliograma { ENTRADA, FOTO, EDITOR }

/**
 * Sección «Familiograma»: dos caminos, dibujarlo dentro de la app (con los integrantes de la ficha)
 * o subir una foto del que se hizo en papel.
 */
@Composable
fun FamiliogramaScreen(
    fichaId: Long,
    onContinuar: () -> Unit,
    onSalir: () -> Unit,
    textoRegresar: String = "Volver al panel de la ficha",
    descripcionRegresar: String = "Conservar los datos y salir de esta sección"
) {
    val context = LocalContext.current
    val database = remember(context) { RuralitosDatabase.obtenerBaseDatos(context) }
    val scope = rememberCoroutineScope()
    val adjuntos by database.fichaContenidoDao().listarAdjuntos(fichaId).collectAsState(initial = emptyList())
    val miembros by database.fichaContenidoDao().listarMiembros(fichaId).collectAsState(initial = emptyList())
    val adjunto = adjuntos.lastOrNull { it.tipo.equals(AlmacenFamiliograma.TIPO, ignoreCase = true) }

    var modo by remember { mutableStateOf(ModoFamiliograma.ENTRADA) }
    var docEditor by remember { mutableStateOf<Familiograma?>(null) }
    var imagenGuardada by remember { mutableStateOf<Bitmap?>(null) }
    var dibujoGuardado by remember { mutableStateOf<Familiograma?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var confirmarRearmar by remember { mutableStateOf(false) }
    var confirmarQuitar by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }

    LaunchedEffect(adjunto?.uri, adjunto?.actualizadoEn) {
        val uri = adjunto?.uri
        if (uri.isNullOrBlank()) {
            imagenGuardada = null
            dibujoGuardado = null
            cargando = false
        } else {
            cargando = true
            imagenGuardada = withContext(Dispatchers.IO) {
                runCatching {
                    val u = Uri.parse(uri)
                    if (u.scheme == "file") u.path?.let(BitmapFactory::decodeFile)
                    else context.contentResolver.openInputStream(u)?.use(BitmapFactory::decodeStream)
                }.getOrNull()
            }
            dibujoGuardado = AlmacenFamiliograma.leerDocumento(context, uri)
            cargando = false
        }
    }

    val integrantes = miembros.map { it.aIntegranteFamiliograma() }
    val jefe = miembros.firstOrNull { RolFamiliar.esJefe(it.parentesco) }
    val subtituloEditor = jefe?.let {
        val apellido = it.apellidosNombres.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        if (apellido.isBlank()) null else "Familia ${apellido.lowercase().replaceFirstChar { c -> c.uppercase() }}"
    } ?: "Ficha familiar"

    fun abrirConIntegrantes() {
        docEditor = ArmadoFamiliograma.desdeIntegrantes(integrantes)
        modo = ModoFamiliograma.EDITOR
    }

    when (modo) {
        ModoFamiliograma.EDITOR -> {
            val inicial = docEditor
            if (inicial == null) modo = ModoFamiliograma.ENTRADA
            else EditorFamiliogramaScreen(
                inicial = inicial,
                subtitulo = subtituloEditor,
                onGuardar = { doc -> AlmacenFamiliograma.guardar(context, database, fichaId, doc) },
                onSalir = { modo = ModoFamiliograma.ENTRADA }
            )
            return
        }
        ModoFamiliograma.FOTO -> {
            BackHandler { modo = ModoFamiliograma.ENTRADA }
            FamiliogramaFotoScreen(
                fichaId = fichaId,
                onContinuar = onContinuar,
                onSalir = { modo = ModoFamiliograma.ENTRADA },
                textoRegresar = textoRegresar,
                descripcionRegresar = descripcionRegresar
            )
            return
        }
        ModoFamiliograma.ENTRADA -> Unit
    }

    if (confirmarRearmar) {
        AlertDialog(
            onDismissRequest = { confirmarRearmar = false },
            title = { Text("Volver a armar el familiograma") },
            text = {
                Text(
                    "Se dibujará de nuevo con los integrantes de la ficha. El dibujo guardado se reemplazará " +
                        "solo cuando vuelvas a guardar."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmarRearmar = false; abrirConIntegrantes() }) {
                    Text("Sí, armar de nuevo", color = CianRuralitos, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarRearmar = false }) { Text("Conservar el dibujo") } }
        )
    }
    if (confirmarQuitar && adjunto != null) {
        AlertDialog(
            onDismissRequest = { confirmarQuitar = false },
            title = { Text("Quitar familiograma") },
            text = { Text("La imagen dejará de aparecer en Excel y PDF. Podrás dibujar o subir otra después.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarQuitar = false
                    scope.launch {
                        AlmacenFamiliograma.quitar(context, database, adjunto)
                        mensaje = "Familiograma eliminado."
                    }
                }) { Text("Sí, quitar", color = RojoClinico, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmarQuitar = false }) { Text("Conservar") } }
        )
    }

    val hayDibujo = dibujoGuardado != null
    PantallaRuralitos(
        titulo = "Familiograma",
        descripcion = "Dibújalo en la app con los integrantes de la ficha o sube una foto del que hiciste en papel.",
        subtitulo = "Evidencias familiares",
        onVolver = onSalir,
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = "Guardar información de esta sección",
                descripcion = "Continuar con el familiograma actualmente guardado",
                color = com.ruralitos.app.ui.theme.MoradoClinico,
                onClick = onContinuar
            )
        }
    ) {
        // --- dibujar en la app ---
        TarjetaFormularioRuralitos(Modifier.border(2.dp, CianRuralitos, RoundedCornerShape(16.dp))) {
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(44.dp).background(CianSuave, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { IconoEditor(IconoEditorTipo.LAPIZ, CianRuralitos, tamano = 24.dp) }
                Column(Modifier.weight(1f)) {
                    Text("Dibujar en la app", fontWeight = FontWeight.SemiBold, color = AzulClinicoOscuro, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Con símbolos, abreviaturas de las patologías e íconos de la comunidad. Funciona sin internet.",
                        style = MaterialTheme.typography.bodySmall, color = TextoSecundario
                    )
                }
            }
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                BotonPrincipalRuralitos(
                    texto = when {
                        hayDibujo -> "Seguir editando el dibujo"
                        integrantes.isEmpty() -> "Registra primero a los integrantes"
                        else -> "Ingresar con los ${integrantes.size} integrantes de la ficha"
                    },
                    color = CianRuralitos,
                    enabled = !cargando && (hayDibujo || integrantes.isNotEmpty()),
                    onClick = {
                        if (hayDibujo) {
                            docEditor = dibujoGuardado
                            modo = ModoFamiliograma.EDITOR
                        } else abrirConIntegrantes()
                    }
                )
                if (hayDibujo && integrantes.isNotEmpty()) {
                    BotonSecundarioRuralitos(
                        texto = "Volver a armar con los integrantes",
                        onClick = { confirmarRearmar = true },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Text(
                    "Al entrar, la pantalla gira a horizontal para dibujar mejor.",
                    style = MaterialTheme.typography.bodySmall, color = TextoSecundario,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        // --- subir foto ---
        TarjetaFormularioRuralitos {
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(44.dp).background(AzulSuaveRuralitos, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("▧", color = AzulClinico, style = MaterialTheme.typography.titleLarge) }
                Column(Modifier.weight(1f)) {
                    Text("Subir una foto", fontWeight = FontWeight.SemiBold, color = AzulClinicoOscuro, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Elige la foto del familiograma hecho en papel. Ruralitos quita el fondo blanco." +
                            if (hayDibujo) " Esto reemplazará el dibujo guardado." else "",
                        style = MaterialTheme.typography.bodySmall, color = TextoSecundario
                    )
                }
            }
            Box(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                BotonSecundarioRuralitos(texto = "Subir una foto", onClick = { modo = ModoFamiliograma.FOTO })
            }
        }

        // --- lo que está guardado ---
        TarjetaFormularioRuralitos {
            Column(Modifier.padding(14.dp)) {
                Text("Familiograma guardado", fontWeight = FontWeight.SemiBold, color = AzulClinicoOscuro, style = MaterialTheme.typography.titleSmall)
                val imagen = imagenGuardada
                if (cargando) {
                    Text("Cargando…", style = MaterialTheme.typography.bodySmall, color = TextoSecundario, modifier = Modifier.padding(top = 6.dp))
                } else if (imagen != null) {
                    Text(
                        if (hayDibujo) "Dibujado en la app. Esta es la imagen que va al Excel y al PDF."
                        else "Foto subida. Esta es la imagen que va al Excel y al PDF.",
                        style = MaterialTheme.typography.bodySmall, color = TextoSecundario,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )
                    Image(
                        bitmap = imagen.asImageBitmap(),
                        contentDescription = "Vista previa del familiograma guardado",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 260.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, BordeClinico, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    )
                    BotonSecundarioRuralitos(
                        texto = "Quitar familiograma",
                        onClick = { confirmarQuitar = true },
                        modifier = Modifier.padding(top = 10.dp)
                    )
                } else {
                    Text(
                        "Aún no hay familiograma guardado en esta ficha.",
                        style = MaterialTheme.typography.bodySmall, color = TextoSecundario,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
        if (mensaje.isNotBlank()) {
            MensajeEstadoRuralitos(
                titulo = "Listo",
                descripcion = mensaje,
                color = CianRuralitos,
                simbolo = "✓"
            )
        }
    }
}
