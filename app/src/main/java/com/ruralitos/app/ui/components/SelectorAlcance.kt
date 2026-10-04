package com.ruralitos.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ruralitos.app.domain.AlcanceFichas
import com.ruralitos.app.domain.CatalogoAlcance
import com.ruralitos.app.domain.NivelAlcance
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeCampo
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.TextoSecundario

/** Lo que el usuario va eligiendo: el nivel, lo marcado en la lista, los filtros y el texto buscado. */
@Stable
class EstadoAlcance(nivelInicial: NivelAlcance = NivelAlcance.CENTRO_ACTIVO) {
    var nivel by mutableStateOf(nivelInicial)
        private set
    var elegidos by mutableStateOf(emptySet<String>())
    var filtroEais by mutableStateOf("")
    var filtroBarrio by mutableStateOf("")
    var consulta by mutableStateOf("")

    /** Cambiar de nivel empieza de cero: lo marcado en otro nivel no se arrastra. */
    fun cambiarNivel(nuevo: NivelAlcance) {
        if (nuevo == nivel) return
        nivel = nuevo
        elegidos = emptySet()
        filtroEais = ""
        filtroBarrio = ""
        consulta = ""
    }
}

@Composable
fun rememberEstadoAlcance(): EstadoAlcance = remember { EstadoAlcance() }

/**
 * Elegir qué fichas abarca una acción: todo el centro, uno o varios centros de salud, EAIS, barrios o fichas sueltas.
 * Es el mismo selector para compartir acceso y para exportar un respaldo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorAlcanceRuralitos(
    estado: EstadoAlcance,
    catalogo: CatalogoAlcance,
    titulo: String,
    color: Color,
    modifier: Modifier = Modifier,
    /** Al compartir, las fichas que aún no están en la nube salen marcadas y no se pueden elegir. */
    soloSincronizadas: Boolean = false,
    prefijoPrueba: String = "alcance"
) {
    val nivel = estado.nivel
    val opciones = AlcanceFichas.opciones(
        nivel, catalogo, estado.filtroEais, estado.filtroBarrio, estado.consulta, soloSincronizadas
    )
    val habilitadas = opciones.filter { it.habilitada }.map { it.clave }.toSet()

    SeccionFormularioRuralitos(titulo = titulo, modifier = modifier) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NivelAlcance.entries.forEach { opcion ->
                FilterChip(
                    selected = opcion == nivel,
                    onClick = { estado.cambiarNivel(opcion) },
                    modifier = Modifier.heightIn(min = 40.dp).testTag("${prefijoPrueba}_nivel_${opcion.name}"),
                    label = { Text(opcion.etiqueta, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color.copy(alpha = 0.14f),
                        selectedLabelColor = color
                    )
                )
            }
        }

        if (nivel == NivelAlcance.CENTRO_ACTIVO) {
            Text(
                "Incluye todas las fichas de " +
                    (catalogo.salaActiva?.let { it.nombreCentroSalud.ifBlank { it.nombreSala } } ?: "tu centro de salud") + ".",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
        } else {
            Text(
                when (nivel) {
                    NivelAlcance.CENTRO -> "Elige uno o varios centros de salud."
                    NivelAlcance.EAIS -> "Elige uno o varios EAIS."
                    NivelAlcance.BARRIO -> "Elige uno o varios barrios."
                    else -> "Elige las fichas."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
            if (nivel == NivelAlcance.BARRIO || nivel == NivelAlcance.FICHA) {
                FiltrosEncadenados(estado, catalogo, color, prefijoPrueba)
            }
            OutlinedTextField(
                value = estado.consulta,
                onValueChange = { estado.consulta = it.take(60) },
                label = {
                    Text(
                        when (nivel) {
                            NivelAlcance.CENTRO -> "Buscar centro de salud"
                            NivelAlcance.EAIS -> "Buscar EAIS"
                            NivelAlcance.BARRIO -> "Buscar barrio"
                            else -> "Buscar por jefe, cédula o número"
                        }
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = color,
                    unfocusedBorderColor = BordeCampo,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().testTag("${prefijoPrueba}_buscar")
            )
            if (opciones.isEmpty()) {
                Text(
                    when {
                        estado.consulta.isNotBlank() -> "Nada coincide con lo que escribiste."
                        nivel == NivelAlcance.FICHA -> "No hay fichas con estos filtros."
                        else -> "Todavía no hay nada para elegir aquí."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    modifier = Modifier.testTag("${prefijoPrueba}_vacio")
                )
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Seleccionar todos (${habilitadas.size})",
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clickable { estado.elegidos = estado.elegidos + habilitadas }
                            .padding(vertical = 6.dp)
                            .testTag("${prefijoPrueba}_todos")
                    )
                    Text(
                        "Quitar selección",
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clickable { estado.elegidos = emptySet() }
                            .padding(vertical = 6.dp)
                            .testTag("${prefijoPrueba}_ninguno")
                    )
                }
                Column(Modifier.fillMaxWidth().border(1.dp, BordeClinico, RoundedCornerShape(12.dp))) {
                    opciones.take(AlcanceFichas.MAXIMO_FILAS).forEachIndexed { indice, opcion ->
                        val marcada = opcion.clave in estado.elegidos
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = opcion.habilitada) {
                                    estado.elegidos =
                                        if (marcada) estado.elegidos - opcion.clave else estado.elegidos + opcion.clave
                                }
                                .background(if (marcada) color.copy(alpha = 0.07f) else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("${prefijoPrueba}_op_${opcion.clave}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CasillaRuralitos(marcada, Modifier.padding(end = 12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    opcion.titulo,
                                    color = if (opcion.habilitada) AzulClinicoOscuro else TextoSecundario,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (opcion.detalle.isNotBlank()) {
                                    Text(
                                        opcion.detalle,
                                        color = TextoSecundario,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (nivel != NivelAlcance.FICHA) {
                                Text(
                                    if (opcion.fichas == 1) "1 ficha" else "${opcion.fichas} fichas",
                                    color = TextoSecundario,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        if (indice < minOf(opciones.size, AlcanceFichas.MAXIMO_FILAS) - 1) {
                            HorizontalDivider(color = BordeClinico)
                        }
                    }
                }
                if (opciones.size > AlcanceFichas.MAXIMO_FILAS) {
                    Text(
                        "Se muestran ${AlcanceFichas.MAXIMO_FILAS} de ${opciones.size}. Usa el buscador para encontrar las demás.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario
                    )
                }
            }
        }

        val resumen = if (AlcanceFichas.hayElegidos(nivel, estado.elegidos)) {
            AlcanceFichas.resumen(nivel, estado.elegidos, catalogo)
        } else "Todavía no elegiste nada"
        MensajeEstadoRuralitos(
            titulo = resumen,
            descripcion = if (AlcanceFichas.hayElegidos(nivel, estado.elegidos)) "Esto es lo que se incluirá." else "Marca al menos una opción de la lista.",
            color = color,
            simbolo = if (AlcanceFichas.hayElegidos(nivel, estado.elegidos)) "✓" else "i",
            modifier = Modifier.testTag("${prefijoPrueba}_resumen")
        )
    }
}

/** «EAIS» y «Barrio» como filtros para llegar más rápido a una ficha o a un barrio. */
@Composable
private fun FiltrosEncadenados(
    estado: EstadoAlcance,
    catalogo: CatalogoAlcance,
    color: Color,
    prefijoPrueba: String
) {
    val eaisDelCentro = catalogo.eais.filter { it.salaId == catalogo.salaActivaId && it.activo }
    var menuEais by remember { mutableStateOf(false) }
    var menuBarrio by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            BotonSelectorRuralitos(
                onClick = { menuEais = true },
                modifier = Modifier.fillMaxWidth().testTag("${prefijoPrueba}_filtro_eais")
            ) {
                Text("EAIS: ", color = TextoSecundario)
                Text(
                    eaisDelCentro.firstOrNull { it.id == estado.filtroEais }?.nombre ?: "Todos",
                    color = AzulClinicoOscuro,
                    fontWeight = FontWeight.SemiBold
                )
            }
            MenuDesplegableRuralitos(expanded = menuEais, onDismissRequest = { menuEais = false }) {
                ItemMenuRuralitos(
                    text = { Text("Todos") },
                    seleccionado = estado.filtroEais.isBlank(),
                    onClick = { estado.filtroEais = ""; estado.filtroBarrio = ""; menuEais = false }
                )
                eaisDelCentro.forEach { eais ->
                    ItemMenuRuralitos(
                        text = { Text(eais.nombre) },
                        seleccionado = estado.filtroEais == eais.id,
                        onClick = { estado.filtroEais = eais.id; estado.filtroBarrio = ""; menuEais = false }
                    )
                }
            }
        }
        if (estado.nivel == NivelAlcance.FICHA) {
            val barrios = catalogo.territorios.filter {
                it.salaId == catalogo.salaActivaId && it.activo &&
                    (estado.filtroEais.isBlank() || it.eaisId == estado.filtroEais)
            }
            Box {
                BotonSelectorRuralitos(
                    onClick = { menuBarrio = true },
                    modifier = Modifier.fillMaxWidth().testTag("${prefijoPrueba}_filtro_barrio")
                ) {
                    Text("Barrio: ", color = TextoSecundario)
                    Text(
                        barrios.firstOrNull { it.id == estado.filtroBarrio }?.nombre ?: "Todos",
                        color = AzulClinicoOscuro,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                MenuDesplegableRuralitos(expanded = menuBarrio, onDismissRequest = { menuBarrio = false }) {
                    ItemMenuRuralitos(
                        text = { Text("Todos") },
                        seleccionado = estado.filtroBarrio.isBlank(),
                        onClick = { estado.filtroBarrio = ""; menuBarrio = false }
                    )
                    barrios.forEach { barrio ->
                        ItemMenuRuralitos(
                            text = { Text(barrio.nombre) },
                            seleccionado = estado.filtroBarrio == barrio.id,
                            onClick = { estado.filtroBarrio = barrio.id; menuBarrio = false }
                        )
                    }
                }
            }
        }
    }
}
