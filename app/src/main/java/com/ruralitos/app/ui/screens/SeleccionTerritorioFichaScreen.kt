package com.ruralitos.app.ui.screens

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
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.CianRuralitos

private val FondoPantalla = Color(0xFFEFFBFD)
private val FondoTarjeta = Color(0xFFFDFEFF)
private val AzulTitulo = Color(0xFF09295A)
private val AzulAccion = Color(0xFF1979BC)
private val VerdeAccion = Color(0xFF0BAF8E)
private val VerdeTexto = Color(0xFF08AA88)
private val GrisTexto = Color(0xFF71839D)
private val BordeSelector = Color(0xFFB9D8ED)
private val FondoInfo = Color(0xFFEAF9F6)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoPantalla)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .formularioSeguro()
                .verticalScroll(rememberScrollState())
        ) {
            Image(
                painter = painterResource(R.drawable.rural_header_ubicacion),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(941f / 236f),
                contentScale = ContentScale.Crop
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .offset(y = (-14).dp),
                shape = RoundedCornerShape(24.dp),
                color = FondoTarjeta,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CabeceraPagina()

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
                            iconoRes = R.drawable.icon_centro_salud,
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
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
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
                            iconoRes = R.drawable.icon_eais,
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
                            iconoRes = R.drawable.icon_barrio,
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

                    BotonPrincipal(
                        texto = "Continuar con los datos de la familia",
                        enabled = sala != null && eaisElegido != null && territorio != null,
                        onClick = {
                            if (sala != null && eaisElegido != null && territorio != null) {
                                onContinuar(sala, eaisElegido, territorio)
                            }
                        }
                    )

                    BotonSecundario(
                        texto = "Administrar mis Salas",
                        iconoRes = R.drawable.seleccion_territorio_ajustes,
                        onClick = onConfigurarSala
                    )

                    BotonSecundario(
                        texto = "Regresar al inicio",
                        iconoRes = R.drawable.seleccion_territorio_inicio,
                        onClick = onRegresar
                    )

                    Spacer(Modifier.height(4.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CabeceraPagina() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "NUEVA FICHA · PASO 1",
            color = AzulAccion,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )

        Text(
            text = "Ubicación organizativa",
            color = AzulTitulo,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            text = "Selecciona el territorio de la nueva familia.",
            color = GrisTexto,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

@Composable
private fun InfoTerritorio() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFE8F9F6),
                        Color(0xFFEAF8FC)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.icon_ubicacion),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Territorio de la ficha",
                    color = AzulTitulo,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Las opciones están disponibles sin internet.",
                    color = GrisTexto,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SeccionSelector(
    titulo: String,
    contenido: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 0.6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Text(
                text = titulo,
                color = AzulTitulo,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 9.dp)
            )

            contenido()
        }
    }
}

@Composable
private fun <T> SelectorGenerico(
    texto: String,
    opciones: List<T>,
    etiqueta: (T) -> String,
    enabled: Boolean,
    iconoRes: Int,
    onElegir: (T) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = { abierto = true },
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (enabled) BordeSelector else Color(0xFFDDE8EF)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                disabledContainerColor = Color(0xFFF8FBFD)
            )
        ) {
            Image(
                painter = painterResource(iconoRes),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Text(
                text = texto,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                color = if (enabled) VerdeTexto else GrisTexto,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "⌄",
                color = if (enabled) VerdeTexto else GrisTexto,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(Color.White)
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = etiqueta(opcion),
                            color = AzulTitulo,
                            fontWeight = FontWeight.SemiBold
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

@Composable
private fun BotonPrincipal(
    texto: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VerdeAccion,
            contentColor = Color.White,
            disabledContainerColor = VerdeAccion.copy(alpha = 0.45f),
            disabledContentColor = Color.White.copy(alpha = 0.90f)
        )
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = Color.White
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.seleccion_territorio_flecha),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Text(
            text = texto,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.size(36.dp))
    }
}

@Composable
private fun BotonSecundario(
    texto: String,
    iconoRes: Int,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF77C6EF)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White
        )
    ) {
        Image(
            painter = painterResource(iconoRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            contentScale = ContentScale.Fit
        )

        Text(
            text = texto,
            color = AzulAccion,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}


