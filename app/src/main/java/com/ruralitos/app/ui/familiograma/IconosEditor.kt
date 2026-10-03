package com.ruralitos.app.ui.familiograma

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class IconoEditorTipo {
    MOVER, PERSONA, UNIR, TEXTO, LAPIZ, BORRAR, DESHACER, REHACER,
    SIMBOLOS, ENTORNO, GUARDAR, MAS, CERRAR, AJUSTAR, REGRESAR
}

/** Íconos de línea de las herramientas del editor, dibujados a mano (sin depender de librerías de íconos). */
@Composable
fun IconoEditor(tipo: IconoEditorTipo, color: Color, modifier: Modifier = Modifier, tamano: Dp = 22.dp) {
    Canvas(modifier.size(tamano)) {
        val trazo = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        dibujarIcono(tipo, color, trazo)
    }
}

private fun DrawScope.p(x: Float, y: Float) = Offset(size.width * x, size.height * y)

private fun DrawScope.poligono(color: Color, trazo: Stroke, vararg puntos: Pair<Float, Float>) {
    val ruta = Path()
    puntos.forEachIndexed { i, (x, y) ->
        val o = p(x, y)
        if (i == 0) ruta.moveTo(o.x, o.y) else ruta.lineTo(o.x, o.y)
    }
    ruta.close()
    drawPath(ruta, color, style = trazo)
}

private fun DrawScope.linea(color: Color, trazo: Stroke, x1: Float, y1: Float, x2: Float, y2: Float) =
    drawLine(color, p(x1, y1), p(x2, y2), trazo.width, StrokeCap.Round)

private fun DrawScope.dibujarIcono(tipo: IconoEditorTipo, color: Color, trazo: Stroke) {
    when (tipo) {
        IconoEditorTipo.MOVER -> poligono(
            color, trazo,
            0.28f to 0.12f, 0.28f to 0.80f, 0.45f to 0.64f, 0.57f to 0.90f,
            0.68f to 0.85f, 0.56f to 0.60f, 0.78f to 0.58f
        )
        IconoEditorTipo.PERSONA -> {
            drawCircle(color, size.width * 0.15f, p(0.40f, 0.30f), style = trazo)
            val cuerpo = Path().apply {
                moveTo(size.width * 0.10f, size.height * 0.88f)
                quadraticTo(size.width * 0.40f, size.height * 0.40f, size.width * 0.70f, size.height * 0.88f)
            }
            drawPath(cuerpo, color, style = trazo)
            linea(color, trazo, 0.84f, 0.18f, 0.84f, 0.46f)
            linea(color, trazo, 0.70f, 0.32f, 0.98f, 0.32f)
        }
        IconoEditorTipo.UNIR -> {
            drawCircle(color, size.width * 0.13f, p(0.22f, 0.76f), style = trazo)
            drawCircle(color, size.width * 0.13f, p(0.78f, 0.24f), style = trazo)
            linea(color, trazo, 0.31f, 0.67f, 0.69f, 0.33f)
        }
        IconoEditorTipo.TEXTO -> {
            linea(color, trazo, 0.20f, 0.22f, 0.80f, 0.22f)
            linea(color, trazo, 0.50f, 0.22f, 0.50f, 0.82f)
            linea(color, trazo, 0.36f, 0.82f, 0.64f, 0.82f)
        }
        IconoEditorTipo.LAPIZ -> {
            poligono(color, trazo, 0.16f to 0.84f, 0.23f to 0.62f, 0.66f to 0.19f, 0.81f to 0.34f, 0.38f to 0.77f)
            linea(color, trazo, 0.56f, 0.29f, 0.71f, 0.44f)
        }
        IconoEditorTipo.BORRAR -> {
            poligono(color, trazo, 0.18f to 0.62f, 0.52f to 0.22f, 0.84f to 0.52f, 0.60f to 0.82f, 0.40f to 0.82f)
            linea(color, trazo, 0.33f, 0.45f, 0.66f, 0.74f)
            linea(color, trazo, 0.62f, 0.90f, 0.92f, 0.90f)
        }
        IconoEditorTipo.DESHACER, IconoEditorTipo.REHACER -> {
            val espejo = tipo == IconoEditorTipo.REHACER
            fun x(v: Float) = if (espejo) 1f - v else v
            val curva = Path().apply {
                moveTo(size.width * x(0.20f), size.height * 0.40f)
                cubicTo(
                    size.width * x(0.50f), size.height * 0.30f,
                    size.width * x(0.88f), size.height * 0.46f,
                    size.width * x(0.80f), size.height * 0.84f
                )
            }
            drawPath(curva, color, style = trazo)
            linea(color, trazo, x(0.20f), 0.40f, x(0.40f), 0.20f)
            linea(color, trazo, x(0.20f), 0.40f, x(0.42f), 0.58f)
        }
        IconoEditorTipo.SIMBOLOS -> {
            drawRect(color, p(0.12f, 0.46f), Size(size.width * 0.34f, size.height * 0.34f), style = trazo)
            drawCircle(color, size.width * 0.19f, p(0.68f, 0.34f), style = trazo)
            linea(color, trazo, 0.30f, 0.30f, 0.30f, 0.46f)
        }
        IconoEditorTipo.ENTORNO -> {
            poligono(color, trazo, 0.14f to 0.46f, 0.50f to 0.14f, 0.86f to 0.46f)
            drawRect(color, p(0.24f, 0.46f), Size(size.width * 0.52f, size.height * 0.40f), style = trazo)
            drawRect(color, p(0.43f, 0.62f), Size(size.width * 0.14f, size.height * 0.24f), style = trazo)
        }
        IconoEditorTipo.GUARDAR -> {
            poligono(color, trazo, 0.16f to 0.16f, 0.72f to 0.16f, 0.86f to 0.30f, 0.86f to 0.84f, 0.16f to 0.84f)
            drawRect(color, p(0.30f, 0.16f), Size(size.width * 0.30f, size.height * 0.22f), style = trazo)
            drawRect(color, p(0.28f, 0.54f), Size(size.width * 0.44f, size.height * 0.30f), style = trazo)
        }
        IconoEditorTipo.MAS -> {
            linea(color, trazo, 0.50f, 0.18f, 0.50f, 0.82f)
            linea(color, trazo, 0.18f, 0.50f, 0.82f, 0.50f)
        }
        IconoEditorTipo.CERRAR -> {
            linea(color, trazo, 0.22f, 0.22f, 0.78f, 0.78f)
            linea(color, trazo, 0.78f, 0.22f, 0.22f, 0.78f)
        }
        IconoEditorTipo.AJUSTAR -> {
            for ((cx, cy, dx, dy) in listOf(
                listOf(0.18f, 0.18f, 1f, 1f), listOf(0.82f, 0.18f, -1f, 1f),
                listOf(0.18f, 0.82f, 1f, -1f), listOf(0.82f, 0.82f, -1f, -1f)
            )) {
                linea(color, trazo, cx, cy, cx + 0.22f * dx, cy)
                linea(color, trazo, cx, cy, cx, cy + 0.22f * dy)
            }
        }
        IconoEditorTipo.REGRESAR -> {
            linea(color, trazo, 0.62f, 0.20f, 0.30f, 0.50f)
            linea(color, trazo, 0.30f, 0.50f, 0.62f, 0.80f)
        }
    }
}
