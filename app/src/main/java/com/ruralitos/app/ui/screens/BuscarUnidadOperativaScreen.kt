package com.ruralitos.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.EstablecimientoSaludEntity
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.PantallaListaRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun BuscarUnidadOperativaScreen(
    onEstablecimientoSeleccionado: (EstablecimientoSaludEntity) -> Unit,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    val database = remember(context) {
        RuralitosDatabase.obtenerBaseDatos(context)
    }
    var textoBusqueda by remember { mutableStateOf("") }
    var textoConsulta by remember { mutableStateOf("") }

    LaunchedEffect(textoBusqueda) {
        delay(280)
        textoConsulta = textoBusqueda.trim()
    }

    val resultadosFlow = remember(textoConsulta) {
        if (textoConsulta.length >= 3) {
            database.establecimientoSaludDao().buscarEstablecimientos(textoConsulta)
        } else {
            emptyFlow()
        }
    }
    val resultados by resultadosFlow.collectAsState(initial = emptyList())

    PantallaListaRuralitos(
        titulo = "Unidad operativa",
        descripcion = "Encuentra el centro de salud correspondiente a tu territorio.",
        subtitulo = "Red de atención",
        onVolver = onRegresar
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BordeClinico),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(CianRuralitos)
                    )
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text(
                            text = "Buscar centro de salud",
                            style = MaterialTheme.typography.headlineSmall,
                            color = AzulClinicoOscuro,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Busca por código, nombre, provincia, cantón o parroquia.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 15.dp)
                        )
                        OutlinedTextField(
                            value = textoBusqueda,
                            onValueChange = { textoBusqueda = it },
                            label = { Text("Buscar centro de salud") },
                            supportingText = {
                                Text("Escribe al menos tres letras o números")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }
        if (textoBusqueda.isNotBlank() && textoBusqueda.trim().length < 3) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Continúa escribiendo para iniciar la búsqueda.",
                        color = AzulClinicoOscuro,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        if (textoConsulta.length >= 3 && resultados.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BordeClinico),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text(
                            "No encontramos unidades operativas",
                            fontWeight = FontWeight.SemiBold,
                            color = AzulClinicoOscuro
                        )
                        Text(
                            "Comprueba el texto o intenta con el nombre de la parroquia.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                }
            }
        }
        items(resultados, key = { it.id }) { establecimiento ->
            EstablecimientoCard(
                establecimiento = establecimiento,
                onClick = {
                    Toast.makeText(
                        context,
                        establecimiento.nombreCentroSalud,
                        Toast.LENGTH_SHORT
                    ).show()
                    onEstablecimientoSeleccionado(establecimiento)
                }
            )
        }
    }
}

@Composable
fun EstablecimientoCard(
    establecimiento: EstablecimientoSaludEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, BordeClinico)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AzulClinico, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "UO",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = establecimiento.nombreCentroSalud,
                    style = MaterialTheme.typography.titleMedium,
                    color = AzulClinicoOscuro,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Código UO: ${establecimiento.codigoUo}",
                    color = AzulClinico,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 5.dp)
                )
                Text(
                    text = "${establecimiento.parroquia} · ${establecimiento.canton} · ${establecimiento.provincia}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = listOf(establecimiento.sector, establecimiento.areaNumero)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
                Text(
                    "Seleccionar esta unidad operativa",
                    color = CianRuralitos,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 11.dp)
                )
            }
        }
    }
}
