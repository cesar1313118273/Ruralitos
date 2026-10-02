package com.ruralitos.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class TipoIconoTerritorio { CENTRO_SALUD, EQUIPO, BARRIO, INFORMACION }

/** Icono de línea dibujado dentro de un cuadro suave con esquinas redondeadas. */
@Composable
fun IconoTerritorioRuralitos(
    tipo: TipoIconoTerritorio,
    color: Color,
    modifier: Modifier = Modifier,
    tamano: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(tamano)
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(tamano * 0.58f)) {
            val w = size.width
            val h = size.height
            val trazo = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            when (tipo) {
                TipoIconoTerritorio.CENTRO_SALUD -> {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(w * 0.12f, h * 0.22f),
                        size = Size(w * 0.76f, h * 0.68f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f),
                        style = trazo
                    )
                    // cruz de salud
                    drawLine(color, Offset(w * 0.5f, h * 0.38f), Offset(w * 0.5f, h * 0.74f), 2.dp.toPx(), StrokeCap.Round)
                    drawLine(color, Offset(w * 0.32f, h * 0.56f), Offset(w * 0.68f, h * 0.56f), 2.dp.toPx(), StrokeCap.Round)
                    // techo
                    drawLine(color, Offset(w * 0.30f, h * 0.10f), Offset(w * 0.70f, h * 0.10f), 2.dp.toPx(), StrokeCap.Round)
                }

                TipoIconoTerritorio.EQUIPO -> {
                    drawCircle(color, radius = w * 0.14f, center = Offset(w * 0.36f, h * 0.30f), style = trazo)
                    drawPath(
                        Path().apply {
                            moveTo(w * 0.10f, h * 0.88f)
                            quadraticTo(w * 0.36f, h * 0.38f, w * 0.62f, h * 0.88f)
                        },
                        color, style = trazo
                    )
                    drawCircle(color, radius = w * 0.11f, center = Offset(w * 0.70f, h * 0.36f), style = trazo)
                    drawPath(
                        Path().apply {
                            moveTo(w * 0.66f, h * 0.60f)
                            quadraticTo(w * 0.86f, h * 0.60f, w * 0.92f, h * 0.88f)
                        },
                        color, style = trazo
                    )
                }

                TipoIconoTerritorio.BARRIO -> {
                    val centro = Offset(w * 0.5f, h * 0.40f)
                    val radio = w * 0.28f
                    drawCircle(color, radius = radio, center = centro, style = trazo)
                    drawLine(color, Offset(w * 0.5f - radio * 0.8f, h * 0.40f + radio * 0.6f), Offset(w * 0.5f, h * 0.92f), 2.dp.toPx(), StrokeCap.Round)
                    drawLine(color, Offset(w * 0.5f + radio * 0.8f, h * 0.40f + radio * 0.6f), Offset(w * 0.5f, h * 0.92f), 2.dp.toPx(), StrokeCap.Round)
                    drawCircle(color, radius = w * 0.07f, center = centro)
                }

                TipoIconoTerritorio.INFORMACION -> {
                    drawCircle(color, radius = w * 0.40f, center = Offset(w * 0.5f, h * 0.5f), style = trazo)
                    drawLine(color, Offset(w * 0.5f, h * 0.46f), Offset(w * 0.5f, h * 0.70f), 2.dp.toPx(), StrokeCap.Round)
                    drawCircle(color, radius = w * 0.045f, center = Offset(w * 0.5f, h * 0.30f))
                }
            }
        }
    }
}
