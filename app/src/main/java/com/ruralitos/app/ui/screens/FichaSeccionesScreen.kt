package com.ruralitos.app.ui.screens

import com.ruralitos.app.ui.theme.TextoSecundario
import androidx.compose.material3.HorizontalDivider
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import androidx.compose.foundation.layout.statusBarsPadding
import com.ruralitos.app.ui.theme.FondoClinico
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
    onRegresar: () -> Unit
) {
    var confirmarEliminacion by remember { mutableStateOf(false) }
    val grupos = listOf(
        GrupoFicha(
            "1. Información del hogar",
            "Identificación, dirección y personas que integran la familia.",
            listOf(
                SeccionFicha("datos", "1", "Datos personales y de la ficha", "Jefe de familia, teléfono, fecha, número de ficha y responsable.", AzulClinico),
                SeccionFicha("ubicacion", "2", "Dirección y vivienda", "Sector, barrio, referencias y número de casa. El GPS está en Croquis.", CianRuralitos),
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
                SeccionFicha("familiograma", "7", "Imagen del familiograma", "Sube o reemplaza la imagen que aparecerá en Excel y PDF.", CianRuralitos),
                SeccionFicha("croquis", "8", "Croquis, GPS y mapa", "Obtén latitud, longitud y altitud; mueve el punto y guarda el croquis.", AzulClinico),
                SeccionFicha("contaminacion", "9", "Contaminación ambiental", "Fecha, tipo, descripción y causante de la contaminación.", NaranjaClinico),
                SeccionFicha("tratamiento", "10", "Lugar de atención o persona", "Indica dónde o con quién recibe atención la familia.", CianRuralitos)
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
            .background(FondoClinico)
            .formularioSeguro()
            .verticalScroll(rememberScrollState())
    ) {
        BannerFichaPanel(
            ficha = ficha,
            onVolver = onRegresar
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp)
    ) {
        BotonVolverRuralitos(onVolver, Modifier.padding(bottom = 12.dp))

        Text(
            text = "Panel de la ficha",
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelLarge
        )

        Text(
            text = ficha.nombreApellidoJefeFamilia.ifBlank { "Ficha familiar" },
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ficha ${ficha.numeroFichaFamiliar} · Estado: ${ficha.estado.replace('_', ' ')}",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
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

                Text(
                    text = if (abierto) "⌃" else "⌄",
                    color = color,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
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
    onEliminar: () -> Unit
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

