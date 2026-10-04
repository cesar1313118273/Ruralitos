package com.ruralitos.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.domain.OPCIONES_ORIGEN_MAPA
import com.ruralitos.app.domain.OrigenFicha
import com.ruralitos.app.ui.theme.CianRuralitos

/**
 * Filtro de los mapas: «Mías · Compartidas conmigo · Todas», con una línea que dice de dónde salen los datos
 * (para no confundir mis fichas con las que otra persona me compartió).
 */
@Composable
fun FiltroOrigenFichas(
    origen: OrigenFicha,
    resumen: String,
    onCambiar: (OrigenFicha) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OPCIONES_ORIGEN_MAPA.forEach { opcion ->
                val activa = opcion == origen
                Surface(
                    onClick = { onCambiar(opcion) },
                    shape = RoundedCornerShape(50),
                    color = if (activa) CianRuralitos else Color.White,
                    border = BorderStroke(1.dp, if (activa) CianRuralitos else Color(0xFFCFDDE5)),
                    modifier = Modifier.testTag("mapa_origen_${opcion.name}")
                ) {
                    Text(
                        opcion.etiqueta,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = if (activa) Color.White else Color(0xFF0A2A5E),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
                    )
                }
            }
        }
        Text(
            resumen,
            color = Color(0xFF5B7083), fontSize = 11.sp,
            modifier = Modifier.testTag("mapa_origen_resumen")
        )
    }
}
