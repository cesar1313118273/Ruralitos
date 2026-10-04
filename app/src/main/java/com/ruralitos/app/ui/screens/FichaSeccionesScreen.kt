package com.ruralitos.app.ui.screens

import com.ruralitos.app.ui.components.FlechaDesplegable
import com.ruralitos.app.ui.components.VentanaConfirmarRuralitos
import com.ruralitos.app.ui.theme.TextoSecundario
import androidx.compose.material3.HorizontalDivider
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import androidx.compose.foundation.layout.statusBarsPadding
import com.ruralitos.app.ui.theme.FondoClinico
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.R
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.domain.EtiquetasFicha
import com.ruralitos.app.domain.RutaAVivienda
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.testTag
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.BotonVolverRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.VerdeSalud

private data class SeccionFicha(
    val id: String,
    val simbolo: String,
    val titulo: String,
    val descripcion: String,
    val color: Color
)

private data class GrupoFicha(
    val titulo: String,
    val descripcion: String,
    val secciones: List<SeccionFicha>
)

@Composable
fun FichaSeccionesScreen(
    ficha: FichaFamiliarEntity,
    onAbrirSeccion: (String) -> Unit,
    onCambiarArchivado: () -> Unit,
    onEliminar: () -> Unit,
    onComoLlegar: () -> Unit,
    onRegresar: () -> Unit
) {
    var confirmarEliminacion by remember { mutableStateOf(false) }
    var verAccesos by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    if (verAccesos) {
        com.ruralitos.app.ui.components.AccesosDeFichaRuralitos(
            ficha = ficha,
            onCerrar = { verAccesos = false },
            onTraspasada = { verAccesos = false; onRegresar() }
        )
    }
    val grupos = listOf(
        GrupoFicha(
            "1. Información del hogar",
            "Identificación, dirección y personas que integran la familia.",
            listOf(
                SeccionFicha("ubicacion", "1", "Dirección y vivienda", "Sector, barrio, referencias y número de casa. El GPS está en Ubicación de vivienda.", CianRuralitos),
                SeccionFicha("miembros", "2", "Integrantes de la familia", "Registra al jefe o jefa del hogar y a cada miembro; la cédula y el teléfono del jefe identifican la ficha.", MoradoClinico)
            )
        ),
        GrupoFicha(
            "2. Salud y evaluación familiar",
            "Registro clínico, calificación de riesgos y compromisos de seguimiento.",
            listOf(
                SeccionFicha("riesgos", "3", "Calificación del riesgo familiar", "Escoge descripciones claras; Ruralitos calcula la puntuación numérica.", RojoClinico),
                SeccionFicha("gestion", "4", "Plan y seguimiento del riesgo", "Compromisos, evaluación del cumplimiento y observaciones.", AzulClinico)
            )
        ),
        GrupoFicha(
            "3. Evidencias y entorno",
            "Imágenes, ubicación exacta y condiciones ambientales de la familia.",
            listOf(
                SeccionFicha("familiograma", "5", "Imagen del familiograma", "Dibújalo en la app con los integrantes de la ficha o sube una foto. Aparecerá en Excel y PDF.", CianRuralitos),
                SeccionFicha("croquis", "6", "Ubicación de vivienda", "Obtén latitud, longitud y altitud; mueve el punto y guarda el croquis.", AzulClinico),
                SeccionFicha("contaminacion", "7", "Contaminación ambiental", "Fecha, tipo, descripción y causante de la contaminación.", NaranjaClinico),
                SeccionFicha("tratamiento", "8", "Lugar de atención o persona", "Indica dónde o con quién recibe atención la familia.", CianRuralitos)
            )
        ),
        GrupoFicha(
            "4. Comprobación y trazabilidad",
            "Verifica la ficha antes de finalizar y consulta los cambios realizados.",
            listOf(
                SeccionFicha("revision", "✓", "Revisar y finalizar la ficha", "Comprueba datos pendientes y confirma la firma profesional.", MoradoClinico),
                SeccionFicha("historial", "H", "Historial de cambios", "Consulta las acciones y responsables registrados en esta ficha.", NaranjaClinico)
            )
        )
    )

    val expandido = remember {
        mutableStateListOf(true, false, false, false)
    }

    if (confirmarEliminacion) {
        VentanaConfirmarRuralitos(
            titulo = "Eliminar ficha definitivamente",
            mensaje = "Se eliminarán la ficha, sus integrantes, evaluaciones, firma y adjuntos internos. Esta acción no se puede deshacer.",
            textoConfirmar = "Sí, eliminar definitivamente",
            onConfirmar = {
                                confirmarEliminacion = false
                                onEliminar()
                            },
            textoCancelar = "No eliminar",
            peligro = true,
            onCancelar = { confirmarEliminacion = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoClinico)
            .formularioSeguro()
    ) {
        BannerFichaPanel(
            ficha = ficha,
            onVolver = onRegresar
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EtiquetasFicha.texto(ficha)?.let { AvisoFichaCompartida(ficha, it) }

            if (RutaAVivienda.tieneUbicacion(ficha.latitud, ficha.longitud)) {
                BotonSecundarioRuralitos(
                    texto = "Cómo llegar a la vivienda",
                    descripcion = "Abre el mapa de Ruralitos con la ruta hasta la casa, desde donde estás.",
                    onClick = onComoLlegar,
                    modifier = Modifier.testTag("como_llegar")
                )
            }

            if (!EtiquetasFicha.esRecibida(ficha) && ficha.syncEstado == "SINCRONIZADO") {
                BotonSecundarioRuralitos(
                    texto = "Con quién compartiste esta ficha",
                    descripcion = "Ver permisos, quitar el acceso o traspasarla.",
                    onClick = { verAccesos = true },
                    modifier = Modifier.testTag("ver_accesos_ficha")
                )
            }

            if (EtiquetasFicha.soloLectura(ficha)) {
                ResumenSoloLectura(ficha)
                BotonPrincipalRuralitos(
                    texto = "Descargar PDF o Excel",
                    onClick = { onAbrirSeccion("revision") },
                    descripcion = "Se guardan en Descargas/Ruralitos; no cambia la ficha.",
                    modifier = Modifier.testTag("solo_lectura_descargar")
                )
                BotonSecundarioRuralitos(
                    texto = "Historial de cambios",
                    onClick = { onAbrirSeccion("historial") },
                    modifier = Modifier.testTag("solo_lectura_historial")
                )
                Spacer(Modifier.height(24.dp))
                return@Column
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.97f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BordeClinico)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // En tabletas los cuatro grupos se reparten en dos columnas.
                    com.ruralitos.app.ui.components.CuadriculaAdaptable(
                        items = grupos.indices.toList(),
                        columnas = com.ruralitos.app.ui.components.columnasAdaptables(1, 2, 2),
                        espacio = 14.dp
                    ) { index, modificador ->
                        Box(modificador) {
                            GrupoFichaDesplegable(
                                grupo = grupos[index],
                                abierto = expandido[index],
                                onAlternar = { expandido[index] = !expandido[index] },
                                onAbrirSeccion = onAbrirSeccion
                            )
                        }
                    }
                }
            }

            PanelAdministracionFicha(
                ficha = ficha,
                onCambiarArchivado = onCambiarArchivado,
                onEliminar = { confirmarEliminacion = true },
                puedeEliminar = EtiquetasFicha.puedeEliminar(ficha)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BannerFichaPanel(
    ficha: FichaFamiliarEntity,
    onVolver: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AzulClinicoOscuro)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BotonVolverRuralitos(
                onVolver,
                Modifier.padding(end = 14.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Panel de la ficha",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = ficha.nombreApellidoJefeFamilia.ifBlank { "Ficha familiar" },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = "Ficha ${ficha.numeroFichaFamiliar} · Estado: ${ficha.estado.replace('_', ' ')}",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun GrupoFichaDesplegable(
    grupo: GrupoFicha,
    abierto: Boolean,
    onAlternar: () -> Unit,
    onAbrirSeccion: (String) -> Unit
) {
    val color = colorGrupoPrincipal(grupo.titulo)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BordeClinico)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAlternar)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    color = color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = grupo.titulo.substringBefore(".").ifBlank { "•" },
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp, end = 10.dp)
                ) {
                    Text(
                        text = grupo.titulo.substringAfter(". ").ifBlank { grupo.titulo },
                        style = MaterialTheme.typography.titleMedium,
                        color = AzulClinicoOscuro,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = grupo.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }

                FlechaDesplegable(color = color, arriba = abierto)
            }

            if (abierto) {
                HorizontalDivider(color = color.copy(alpha = 0.25f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color.copy(alpha = 0.09f))
                ) {
                    grupo.secciones.forEachIndexed { indice, seccion ->
                        TarjetaSeccionRedisenada(
                            seccion = seccion,
                            modifier = Modifier.fillMaxWidth(),
                            onAbrir = onAbrirSeccion
                        )
                        if (indice < grupo.secciones.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 70.dp),
                                color = color.copy(alpha = 0.22f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelAdministracionFicha(
    ficha: FichaFamiliarEntity,
    onCambiarArchivado: () -> Unit,
    onEliminar: () -> Unit,
    puedeEliminar: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.97f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            CianRuralitos.copy(alpha = 0.10f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    color = CianRuralitos.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "⚙",
                            color = CianRuralitos,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(start = 14.dp)
                ) {
                    Text(
                        text = "Administración de la ficha",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0A2A5E)
                    )
                    Text(
                        text = "Estas opciones cambian el estado o eliminan información.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AccionBordeFicha(
                texto = if (ficha.estado == "ARCHIVADA") {
                    "Reactivar esta ficha"
                } else {
                    "Archivar esta ficha"
                },
                descripcion = if (ficha.estado == "ARCHIVADA") {
                    "Volverá a aparecer entre las fichas activas"
                } else {
                    "Podrás reactivarla más adelante"
                },
                color = CianRuralitos,
                icono = "▣",
                onClick = onCambiarArchivado
            )

            if (puedeEliminar) {
                AccionBordeFicha(
                    texto = "Eliminar ficha y todos sus datos",
                    descripcion = "Acción permanente; se pedirá confirmación",
                    color = RojoClinico,
                    icono = "🗑",
                    onClick = onEliminar
                )
            }
        }
    }
}

/** Franja que dice de quién es la ficha y qué se puede hacer con ella. */
@Composable
private fun AvisoFichaCompartida(ficha: FichaFamiliarEntity, texto: String) {
    val recibida = EtiquetasFicha.esRecibida(ficha)
    val color = if (!recibida) CianRuralitos else if (EtiquetasFicha.soloLectura(ficha)) Color(0xFFB26A00) else Color(0xFF2E7D32)
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("aviso_ficha_compartida"),
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(texto, color = color, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Text(
                when {
                    !recibida -> "Tú la creaste. Quienes la reciben ven su avance cuando sincronizas." +
                        (EtiquetasFicha.textoEdicionAjena(ficha)?.let { "\n$it." } ?: "")
                    EtiquetasFicha.soloLectura(ficha) ->
                        "Solo puedes verla y descargar su PDF o Excel. No se puede modificar."
                    else -> "Puedes editarla; tus cambios los ve su autor al sincronizar. Solo su autor puede eliminarla."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Lo esencial de una ficha compartida solo para ver: datos del hogar, integrantes y nivel de riesgo. */
@Composable
private fun ResumenSoloLectura(ficha: FichaFamiliarEntity) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val contenido = remember(context) {
        com.ruralitos.app.data.local.database.RuralitosDatabase.obtenerBaseDatos(context).fichaContenidoDao()
    }
    val miembros by contenido.listarMiembros(ficha.id).collectAsState(initial = emptyList())
    val calificaciones by contenido.listarCalificaciones(ficha.id).collectAsState(initial = emptyList())
    val riesgo = calificaciones.maxByOrNull { it.id }?.nivel?.replace('_', ' ')?.lowercase()
        ?.replaceFirstChar { it.uppercase() }
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("resumen_solo_lectura"),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BordeClinico)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Datos de la ficha", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = AzulClinicoOscuro)
            DatoSoloLectura("Jefe o jefa del hogar", ficha.nombreApellidoJefeFamilia)
            DatoSoloLectura("Cédula", ficha.cedulaJefeHogar)
            DatoSoloLectura("Teléfono", ficha.numeroTelefono)
            DatoSoloLectura(
                "Dirección",
                listOf(ficha.barrio, ficha.sector, ficha.comunidad, ficha.numeroCasa.takeIf { it.isNotBlank() }?.let { "Casa $it" })
                    .filterNot { it.isNullOrBlank() }.joinToString(" · ")
            )
            DatoSoloLectura("Fecha de llenado", ficha.fechaLlenado)
            DatoSoloLectura("Responsable", ficha.responsableNombre)
            DatoSoloLectura("Riesgo familiar", riesgo.orEmpty())
            HorizontalDivider(color = BordeClinico)
            Text(
                "Integrantes (${miembros.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AzulClinicoOscuro
            )
            if (miembros.isEmpty()) {
                Text("Todavía no tiene integrantes registrados.", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
            miembros.forEach { m ->
                Text(
                    "• ${m.apellidosNombres.ifBlank { "Sin nombre" }}" +
                        listOf(m.parentesco, m.grupoEdad).filter { it.isNotBlank() }.joinToString(" · ", prefix = " — ").takeIf { it != " — " }.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DatoSoloLectura(etiqueta: String, valor: String) {
    if (valor.isBlank()) return
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
        Text(valor, style = MaterialTheme.typography.bodyLarge, color = AzulClinicoOscuro)
    }
}

@Composable
private fun AccionBordeFicha(
    texto: String,
    descripcion: String,
    color: Color,
    icono: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.04f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.4.dp,
            color.copy(alpha = 0.70f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = color.copy(alpha = 0.11f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = icono,
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = texto,
                    color = color,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = descripcion,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun TarjetaSeccionRedisenada(
    seccion: SeccionFicha,
    modifier: Modifier,
    onAbrir: (String) -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onAbrir(seccion.id) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = seccion.simbolo,
                    color = seccion.color,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(
                text = seccion.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AzulClinicoOscuro,
            )
            Text(
                text = seccion.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }

        Text(
            text = "›",
            color = TextoSecundario,
            fontSize = 22.sp,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

private fun colorGrupoPrincipal(titulo: String): Color {
    return when {
        titulo.startsWith("1.") -> AzulClinico
        titulo.startsWith("2.") -> CianRuralitos
        titulo.startsWith("3.") -> MoradoClinico
        titulo.startsWith("4.") -> NaranjaClinico
        else -> AzulClinico
    }
}

private fun colorEstadoFicha(estado: String): Color {
    return when (estado.uppercase()) {
        "COMPLETA" -> VerdeSalud
        "BORRADOR" -> NaranjaClinico
        "ARCHIVADA" -> MoradoClinico
        else -> AzulClinico
    }
}
