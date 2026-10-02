package com.ruralitos.app.ui.screens

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
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.VerdeClinico

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
    onRegresar: () -> Unit
) {
    var confirmarEliminacion by remember { mutableStateOf(false) }
    val grupos = listOf(
        GrupoFicha(
            "1. Información del hogar",
            "Identificación, dirección y personas que integran la familia.",
            listOf(
                SeccionFicha("datos", "1", "Datos personales y de la ficha", "Jefe de familia, teléfono, fecha, número de ficha y responsable.", AzulClinico),
                SeccionFicha("ubicacion", "2", "Dirección y vivienda", "Sector, barrio, referencias y número de casa. El GPS está en Croquis.", VerdeClinico),
                SeccionFicha("miembros", "3", "Integrantes de la familia", "Añade o corrige los datos de cada miembro del hogar.", MoradoClinico)
            )
        ),
        GrupoFicha(
            "2. Salud y evaluación familiar",
            "Registro clínico, calificación de riesgos y compromisos de seguimiento.",
            listOf(
                SeccionFicha("salud", "4", "Embarazo y mortalidad", "Registra embarazadas y antecedentes de mortalidad familiar.", NaranjaClinico),
                SeccionFicha("dispensarizacion", "🩺", "Registro general", "Grupos, indicadores y Excel de esta familia.", AzulClinico),
                SeccionFicha("riesgos", "5", "Calificación del riesgo familiar", "Escoge descripciones claras; Ruralitos calcula la puntuación numérica.", RojoClinico),
                SeccionFicha("gestion", "6", "Plan y seguimiento del riesgo", "Compromisos, evaluación del cumplimiento y observaciones.", AzulClinico)
            )
        ),
        GrupoFicha(
            "3. Evidencias y entorno",
            "Imágenes, ubicación exacta y condiciones ambientales de la familia.",
            listOf(
                SeccionFicha("familiograma", "7", "Imagen del familiograma", "Sube o reemplaza la imagen que aparecerá en Excel y PDF.", VerdeClinico),
                SeccionFicha("croquis", "8", "Croquis, GPS y mapa", "Obtén latitud, longitud y altitud; mueve el punto y guarda el croquis.", AzulClinico),
                SeccionFicha("contaminacion", "9", "Contaminación ambiental", "Fecha, tipo, descripción y causante de la contaminación.", NaranjaClinico),
                SeccionFicha("tratamiento", "10", "Lugar de atención o persona", "Indica dónde o con quién recibe atención la familia.", VerdeClinico)
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
        AlertDialog(
            onDismissRequest = { confirmarEliminacion = false },
            title = { Text("Eliminar ficha definitivamente") },
            text = { Text("Se eliminarán la ficha, sus integrantes, evaluaciones, firma y adjuntos internos. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarEliminacion = false
                        onEliminar()
                    }
                ) {
                    Text(
                        "Sí, eliminar definitivamente",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminacion = false }) {
                    Text("No eliminar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFEAF8FF),
                        Color(0xFFF8FCFF),
                        Color(0xFFF4FFFB)
                    )
                )
            )
            .formularioSeguro()
            .verticalScroll(rememberScrollState())
    ) {
        BannerFichaPanel(
            ficha = ficha
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-26).dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.97f),
                shape = RoundedCornerShape(30.dp),
                shadowElevation = 5.dp,
                border = BorderStroke(
                    1.dp,
                    AzulClinico.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    grupos.forEachIndexed { index, grupo ->
                        GrupoFichaDesplegable(
                            grupo = grupo,
                            abierto = expandido[index],
                            onAlternar = {
                                expandido[index] = !expandido[index]
                            },
                            onAbrirSeccion = onAbrirSeccion
                        )
                    }
                }
            }

            PanelAdministracionFicha(
                ficha = ficha,
                onCambiarArchivado = onCambiarArchivado,
                onEliminar = { confirmarEliminacion = true }
            )

            AccionBordeFicha(
                texto = "Volver al listado de fichas",
                descripcion = "Los cambios guardados se conservan",
                color = AzulClinico,
                icono = "↩",
                onClick = onRegresar
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BannerFichaPanel(
    ficha: FichaFamiliarEntity
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(R.drawable.panel_ficha_cabecera_medico),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp),
            contentScale = ContentScale.Crop
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = 108.dp),
            color = Color.White.copy(alpha = 0.97f),
            shape = RoundedCornerShape(
                topStart = 30.dp,
                topEnd = 30.dp,
                bottomStart = 30.dp,
                bottomEnd = 30.dp
            ),
            shadowElevation = 5.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 18.dp,
                        end = 18.dp,
                        top = 18.dp,
                        bottom = 18.dp
                    )
            ) {
                Text(
                    text = "PANEL DE LA FICHA",
                    color = AzulClinico,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = ficha.nombreApellidoJefeFamilia.ifBlank { "Ficha familiar" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0B2556),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ficha ${ficha.numeroFichaFamiliar}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = " · ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "Estado: ${ficha.estado.replace('_', ' ')}",
                        color = colorEstadoFicha(ficha.estado),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(188.dp))
    }

    Spacer(Modifier.height(118.dp))
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
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(
            1.dp,
            color.copy(alpha = 0.16f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAlternar)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    color = color.copy(alpha = 0.11f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = grupo.titulo.substringBefore(".").ifBlank { "•" },
                            color = color,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
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
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF10295D),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = grupo.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Text(
                    text = if (abierto) "⌃" else "⌄",
                    color = color,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val columnas = if (maxWidth >= 720.dp) 2 else 1
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            grupo.secciones.chunked(columnas).forEach { fila ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    fila.forEach { seccion ->
                                        TarjetaSeccionRedisenada(
                                            seccion = seccion,
                                            modifier = Modifier.weight(1f),
                                            onAbrir = onAbrirSeccion
                                        )
                                    }
                                    repeat(columnas - fila.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
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
    onEliminar: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.97f),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 4.dp,
        border = BorderStroke(
            1.dp,
            VerdeClinico.copy(alpha = 0.10f)
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
                    color = VerdeClinico.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "⚙",
                            color = VerdeClinico,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(start = 14.dp)
                ) {
                    Text(
                        text = "Administración de la ficha",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10295D)
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
                color = VerdeClinico,
                icono = "▣",
                onClick = onCambiarArchivado
            )

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
        shape = RoundedCornerShape(22.dp),
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
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = icono,
                        color = color,
                        fontWeight = FontWeight.ExtraBold,
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
                    fontWeight = FontWeight.ExtraBold
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
    Card(
        modifier = modifier
            .heightIn(min = 92.dp)
            .clickable { onAbrir(seccion.id) },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            seccion.color.copy(alpha = 0.14f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(18.dp),
                color = seccion.color.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = seccion.simbolo,
                        color = seccion.color,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF10295D),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = seccion.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "›",
                color = seccion.color,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

private fun colorGrupoPrincipal(titulo: String): Color {
    return when {
        titulo.startsWith("1.") -> AzulClinico
        titulo.startsWith("2.") -> VerdeClinico
        titulo.startsWith("3.") -> MoradoClinico
        titulo.startsWith("4.") -> NaranjaClinico
        else -> AzulClinico
    }
}

private fun colorEstadoFicha(estado: String): Color {
    return when (estado.uppercase()) {
        "COMPLETA" -> VerdeClinico
        "BORRADOR" -> NaranjaClinico
        "ARCHIVADA" -> MoradoClinico
        else -> AzulClinico
    }
}

