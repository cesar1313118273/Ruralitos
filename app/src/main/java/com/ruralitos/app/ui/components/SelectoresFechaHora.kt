package com.ruralitos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.AzulSuaveRuralitos
import com.ruralitos.app.ui.theme.BordeCampo
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.CianSuave
import com.ruralitos.app.ui.theme.TextoSecundario
import java.util.Calendar

private val NOMBRES_MES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

/** El instante del inicio del día local de [millis]. */
fun inicioDiaLocal(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/**
 * Calendario con el diseño de la app, para todas las fechas: mes con flechas, año que se toca para elegirlo de una
 * lista, hoy con un anillo y el día elegido en un círculo de color. Entrega el inicio del día local elegido.
 */
@Composable
fun CalendarioRuralitos(
    titulo: String = "Elige una fecha",
    fechaInicialMillis: Long?,
    onElegida: (Long) -> Unit,
    onCerrar: () -> Unit,
    /** Si se da, aparece «Quitar fecha» para dejar el campo vacío. */
    onQuitar: (() -> Unit)? = null
) {
    val hoy = remember { inicioDiaLocal(System.currentTimeMillis()) }
    val base = remember(fechaInicialMillis) { Calendar.getInstance().apply { timeInMillis = fechaInicialMillis ?: hoy } }
    var anio by remember { mutableIntStateOf(base.get(Calendar.YEAR)) }
    var mes by remember { mutableIntStateOf(base.get(Calendar.MONTH)) }
    var seleccion by remember(fechaInicialMillis) { mutableStateOf(fechaInicialMillis?.let(::inicioDiaLocal)) }
    var verAnios by remember { mutableStateOf(false) }

    VentanaRuralitos(
        titulo = titulo,
        onCerrar = onCerrar,
        simbolo = "▦",
        color = CianRuralitos,
        contenido = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BotonMes("‹", "Mes anterior") {
                    if (mes == 0) { mes = 11; anio -= 1 } else mes -= 1
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(NOMBRES_MES[mes], color = AzulClinicoOscuro, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "  $anio ⌄",
                        color = CianRuralitos,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.clickable { verAnios = !verAnios }.testTag("calendario_anio")
                    )
                }
                BotonMes("›", "Mes siguiente") {
                    if (mes == 11) { mes = 0; anio += 1 } else mes += 1
                }
            }
            if (verAnios) {
                val anios = remember { (1900..2100).toList() }
                val estado = rememberLazyGridState()
                LaunchedEffect(Unit) { estado.scrollToItem(((anio - 1900) - 4).coerceAtLeast(0)) }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = estado,
                    modifier = Modifier.fillMaxWidth().height(224.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(anios) { a ->
                        Box(
                            Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (a == anio) CianRuralitos else Color.Transparent)
                                .clickable { anio = a; verAnios = false }
                                .testTag("anio_$a"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                a.toString(),
                                color = if (a == anio) Color.White else AzulClinicoOscuro,
                                fontWeight = if (a == anio) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth()) {
                    listOf("L", "M", "X", "J", "V", "S", "D").forEach {
                        Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
                val primero = Calendar.getInstance().apply { clear(); set(anio, mes, 1) }
                val desfase = (primero.get(Calendar.DAY_OF_WEEK) + 5) % 7
                val dias = primero.getActualMaximum(Calendar.DAY_OF_MONTH)
                val anterior = Calendar.getInstance().apply { clear(); set(anio, mes, 1); add(Calendar.MONTH, -1) }
                    .getActualMaximum(Calendar.DAY_OF_MONTH)
                val filas = (desfase + dias + 6) / 7
                repeat(filas) { fila ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { columna ->
                            val indice = fila * 7 + columna
                            val numero = indice - desfase + 1
                            val enMes = numero in 1..dias
                            val mostrado = when {
                                numero < 1 -> anterior + numero
                                numero > dias -> numero - dias
                                else -> numero
                            }
                            val inicio = if (enMes) Calendar.getInstance().apply {
                                clear(); set(anio, mes, numero)
                            }.timeInMillis else null
                            val elegido = inicio != null && inicio == seleccion
                            val esHoy = inicio != null && inicio == hoy
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1.15f)
                                    .padding(1.dp)
                                    .clip(CircleShape)
                                    .background(if (elegido) CianRuralitos else Color.Transparent)
                                    .then(if (esHoy && !elegido) Modifier.border(1.5.dp, CianRuralitos, CircleShape) else Modifier)
                                    .then(if (enMes) Modifier.clickable { seleccion = inicio }.testTag("dia_$numero") else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    mostrado.toString(),
                                    fontSize = 13.sp,
                                    color = when {
                                        elegido -> Color.White
                                        !enMes -> Color(0xFFB5C2CE)
                                        esHoy -> Color(0xFF066B7E)
                                        else -> AzulClinicoOscuro
                                    },
                                    fontWeight = if (elegido || esHoy) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        acciones = {
            if (onQuitar != null) {
                BotonSecundarioRuralitos(texto = "Quitar fecha", onClick = onQuitar, modifier = Modifier.testTag("calendario_quitar"))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonSecundarioRuralitos(texto = "Cancelar", onClick = onCerrar, modifier = Modifier.weight(1f).testTag("calendario_cancelar"))
                BotonPrincipalRuralitos(
                    texto = "Aceptar",
                    color = CianRuralitos,
                    enabled = seleccion != null,
                    modifier = Modifier.weight(1f).testTag("calendario_aceptar"),
                    onClick = { seleccion?.let(onElegida) }
                )
            }
        }
    )
}

@Composable
private fun BotonMes(simbolo: String, descripcion: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AzulSuaveRuralitos)
            .clickable(onClick = onClick)
            .testTag(if (simbolo == "‹") "mes_anterior" else "mes_siguiente"),
        contentAlignment = Alignment.Center
    ) { Text(simbolo, color = AzulClinico, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) }
}

/**
 * Selector de hora con el diseño de la app: dos casillas (hora y minutos) con flechas para subir o bajar y horas
 * frecuentes para elegir de un toque. Entrega hora (0–23) y minuto (0–59).
 */
@Composable
fun HoraRuralitos(
    titulo: String = "Elige la hora",
    horaInicial: Int,
    minutoInicial: Int,
    onElegida: (Int, Int) -> Unit,
    onCerrar: () -> Unit
) {
    var hora by remember { mutableIntStateOf(horaInicial.coerceIn(0, 23)) }
    var minuto by remember { mutableIntStateOf(minutoInicial.coerceIn(0, 59)) }

    VentanaRuralitos(
        titulo = titulo,
        onCerrar = onCerrar,
        simbolo = "◔",
        color = CianRuralitos,
        contenido = {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CasillaTiempo(hora, "hora", { hora = (hora + 1) % 24 }, { hora = (hora + 23) % 24 })
                Text(":", color = AzulClinicoOscuro, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp))
                CasillaTiempo(minuto, "minuto", { minuto = (minuto + 1) % 60 }, { minuto = (minuto + 59) % 60 })
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                listOf(8 to 0, 9 to 0, 10 to 0, 14 to 0).forEach { (h, m) ->
                    val activa = hora == h && minuto == m
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activa) CianRuralitos else Color.White)
                            .border(1.dp, if (activa) CianRuralitos else BordeCampo, RoundedCornerShape(10.dp))
                            .clickable { hora = h; minuto = m }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("hora_%02d_%02d".format(h, m))
                    ) {
                        Text("%02d:%02d".format(h, m), color = if (activa) Color.White else Color(0xFF066B7E), fontSize = 12.sp)
                    }
                }
            }
        },
        acciones = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonSecundarioRuralitos(texto = "Cancelar", onClick = onCerrar, modifier = Modifier.weight(1f))
                BotonPrincipalRuralitos(
                    texto = "Aceptar",
                    color = CianRuralitos,
                    modifier = Modifier.weight(1f).testTag("hora_aceptar"),
                    onClick = { onElegida(hora, minuto) }
                )
            }
        }
    )
}

@Composable
private fun CasillaTiempo(valor: Int, nombre: String, subir: () -> Unit, bajar: () -> Unit) {
    Column(Modifier.width(84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(32.dp).fillMaxWidth().clickable(onClick = subir).testTag("${nombre}_subir"), contentAlignment = Alignment.Center) {
            Text("▲", color = AzulClinico, fontSize = 14.sp)
        }
        Box(
            Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(14.dp)).background(CianSuave),
            contentAlignment = Alignment.Center
        ) {
            Text("%02d".format(valor), color = Color(0xFF066B7E), fontSize = 30.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("${nombre}_valor"))
        }
        Box(Modifier.height(32.dp).fillMaxWidth().clickable(onClick = bajar).testTag("${nombre}_bajar"), contentAlignment = Alignment.Center) {
            Text("▼", color = AzulClinico, fontSize = 14.sp)
        }
    }
}
