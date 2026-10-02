package com.ruralitos.app.ui.screens

import com.ruralitos.app.ui.components.TipoIconoTerritorio
import com.ruralitos.app.ui.components.IconoTerritorioRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.ItemMenuRuralitos
import com.ruralitos.app.ui.components.MenuDesplegableRuralitos
import com.ruralitos.app.ui.components.BotonSelectorRuralitos
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.R
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.EaisSalaEntity
import com.ruralitos.app.data.local.entity.SalaEntity
import com.ruralitos.app.data.local.entity.TerritorioSalaEntity
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.CianRuralitos

private val FondoPantalla = Color(0xFFEFFBFD)
private val FondoTarjeta = Color(0xFFF6F9FB)
private val AzulTitulo = Color(0xFF0A2A5E)
private val AzulAccion = Color(0xFF1565C0)
private val VerdeAccion = Color(0xFF0889A0)
private val VerdeTexto = Color(0xFF0889A0)
private val GrisTexto = Color(0xFF5B7083)
private val BordeSelector = Color(0xFFCFDDE5)
private val FondoInfo = Color(0xFFE3F4F7)

@Composable
fun SeleccionTerritorioFichaScreen(
    database: RuralitosDatabase,
    organizacionActiva: String?,
    onContinuar: (SalaEntity, EaisSalaEntity, TerritorioSalaEntity) -> Unit,
    onConfigurarSala: () -> Unit,
    onRegresar: () -> Unit
) {
    var salas by remember { mutableStateOf<List<SalaEntity>>(emptyList()) }
    var eais by remember { mutableStateOf<List<EaisSalaEntity>>(emptyList()) }
    var territorios by remember { mutableStateOf<List<TerritorioSalaEntity>>(emptyList()) }

    var salaId by remember { mutableStateOf("") }
    var eaisId by remember { mutableStateOf("") }
    var territorioId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        salas = database.salaDao().listarSalas()
        salaId = salas.firstOrNull { it.organizacionId == organizacionActiva }?.organizacionId
            ?: salas.firstOrNull()?.organizacionId.orEmpty()
    }

    LaunchedEffect(salaId) {
        eais = if (salaId.isBlank()) {
            emptyList()
        } else {
            database.salaDao().listarEais(salaId)
        }
        eaisId = eais.firstOrNull()?.id.orEmpty()
    }

    LaunchedEffect(eaisId) {
        territorios = if (eaisId.isBlank()) {
            emptyList()
        } else {
            database.salaDao().listarTerritorios(eaisId)
        }
        territorioId = territorios.firstOrNull()?.id.orEmpty()
    }

    val sala = salas.firstOrNull { it.organizacionId == salaId }
    val eaisElegido = eais.firstOrNull { it.id == eaisId }
    val territorio = territorios.firstOrNull { it.id == territorioId }

    PantallaRuralitos(
        titulo = "Ubicación organizativa",
        descripcion = "Selecciona el territorio de la nueva familia.",
        subtitulo = "Nueva ficha · paso 1",
        onVolver = onRegresar,
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = "Continuar con los datos de la familia",
                enabled = sala != null && eaisElegido != null && territorio != null,
                color = CianRuralitos,
                onClick = {
                    if (sala != null && eaisElegido != null && territorio != null) {
                        onContinuar(sala, eaisElegido, territorio)
                    }
                }
            )

            BotonSecundarioRuralitos(
                texto = "Administrar mis Salas",
                onClick = onConfigurarSala
            )
        }
    ) {
        InfoTerritorio()

        SeccionSelector(
            titulo = "Centro de salud"
        ) {
            SelectorGenerico(
                texto = sala?.let {
                    it.nombreCentroSalud.ifBlank { it.nombreSala }
                }.orEmpty().ifBlank { "Seleccionar centro de salud" },
                opciones = salas,
                etiqueta = { item ->
                    item.nombreCentroSalud.ifBlank { item.nombreSala }
                },
                enabled = salas.isNotEmpty(),
                icono = TipoIconoTerritorio.CENTRO_SALUD,
                onElegir = { salaId = it.organizacionId }
            )

            sala?.let {
                val referencia = listOf(
                    it.codigoUo,
                    it.canton,
                    it.parroquia
                )
                    .filter(String::isNotBlank)
                    .joinToString(" · ")

                if (referencia.isNotBlank()) {
                    Text(
                        text = referencia,
                        color = GrisTexto,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        SeccionSelector(
            titulo = "Equipo EAIS"
        ) {
            SelectorGenerico(
                texto = eaisElegido?.nombre ?: "Seleccionar EAIS",
                opciones = eais,
                etiqueta = { it.nombre },
                enabled = sala != null,
                icono = TipoIconoTerritorio.EQUIPO,
                onElegir = { eaisId = it.id }
            )
        }

        SeccionSelector(
            titulo = "Barrio"
        ) {
            SelectorGenerico(
                texto = territorio?.let { "${it.etiqueta}: ${it.nombre}" }
                    ?: "Seleccionar barrio",
                opciones = territorios,
                etiqueta = { "${it.etiqueta}: ${it.nombre}" },
                enabled = eaisElegido != null,
                icono = TipoIconoTerritorio.BARRIO,
                onElegir = { territorioId = it.id }
            )
        }

        when {
            salas.isEmpty() -> MensajeEstadoRuralitos(
                titulo = "Aún no tienes una Sala",
                descripcion = "Agrega tu centro de salud para empezar a organizar fichas.",
                color = NaranjaClinico,
                simbolo = "1"
            )

            eais.isEmpty() -> MensajeEstadoRuralitos(
                titulo = "Falta crear el EAIS",
                descripcion = "Entra a Sala y agrega el número de tu equipo.",
                color = MoradoClinico,
                simbolo = "2"
            )

            territorios.isEmpty() -> MensajeEstadoRuralitos(
                titulo = "Falta un barrio",
                descripcion = "Agrégalo dentro del EAIS antes de crear la ficha.",
                color = CianRuralitos,
                simbolo = "3"
            )
        }
    }
}

@Composable
private fun InfoTerritorio() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE3F4F7), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconoTerritorioRuralitos(
            tipo = TipoIconoTerritorio.INFORMACION,
            color = CianRuralitos,
            tamano = 36.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Territorio de la ficha",
                color = AzulTitulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Las opciones están disponibles sin internet.",
                color = GrisTexto,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SeccionSelector(
    titulo: String,
    contenido: @Composable () -> Unit
) {
    SeccionFormularioRuralitos(titulo = titulo) {
        contenido()
    }
}

@Composable
private fun <T> SelectorGenerico(
    texto: String,
    opciones: List<T>,
    etiqueta: (T) -> String,
    enabled: Boolean,
    icono: TipoIconoTerritorio,
    onElegir: (T) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        BotonSelectorRuralitos(
            onClick = { abierto = true },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            IconoTerritorioRuralitos(
                tipo = icono,
                color = if (enabled) CianRuralitos else GrisTexto,
                tamano = 36.dp
            )

            Text(
                text = texto,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                color = if (enabled) AzulTitulo else GrisTexto,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        MenuDesplegableRuralitos(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            opciones.forEach { opcion ->
                ItemMenuRuralitos(
                    text = {
                        Text(
                            text = etiqueta(opcion),
                            color = AzulTitulo,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        abierto = false
                        onElegir(opcion)
                    }
                )
            }
        }
    }
}
