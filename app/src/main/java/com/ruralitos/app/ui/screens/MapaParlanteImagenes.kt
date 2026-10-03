package com.ruralitos.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextUtils
import com.ruralitos.app.domain.BarrioParlante
import com.ruralitos.app.domain.MapaParlante
import com.ruralitos.app.ui.components.RecursosIconografiaMais
import kotlin.math.max

/** Medidas, en dp, del tablero de un barrio y de dónde cae cada figura respecto del punto medio del barrio. */
data class TableroMedidas(
    val anchoDp: Float,
    val altoDp: Float,
    /** Desplazamiento (x, y) en dp del centro de cada figura respecto del punto que señala la colita del tablero. */
    val desplazamientos: List<Pair<Float, Float>>
)

/**
 * Dibuja el "mapa parlante": un tablero con el nombre del barrio, sus totales y una figura por cada situación presente,
 * con su número encima. Cada figura se dibuja en su propia celda, así que ni las figuras ni los números se empalman.
 */
object TableroParlante {
    const val CELDA_ANCHO_DP = 62f
    const val CELDA_ALTO_DP = 72f
    private const val ICONO_DP = 46f
    private const val MARGEN_DP = 12f
    private const val TITULO_DP = 50f
    private const val COLITA_DP = 12f
    private const val ANCHO_MINIMO_DP = 190f

    private const val AZUL = "#0A2A5E"
    private const val CIAN = "#0889A0"

    fun medidas(barrio: BarrioParlante): TableroMedidas {
        val n = barrio.stickers.size
        val (columnas, filas) = MapaParlante.cuadricula(n)
        val ancho = max(ANCHO_MINIMO_DP, columnas * CELDA_ANCHO_DP + 2 * MARGEN_DP)
        val cuerpo = TITULO_DP + filas * CELDA_ALTO_DP + MARGEN_DP
        val alto = cuerpo + COLITA_DP
        val rejilla = MapaParlante.desplazamientos(n, CELDA_ANCHO_DP, CELDA_ALTO_DP)
        // La colita señala el punto medio; la rejilla está centrada en el cuerpo, bajo el título.
        val centroRejillaY = TITULO_DP + filas * CELDA_ALTO_DP / 2f
        val desplazamientos = rejilla.map { (dx, dy) -> dx to (-alto + centroRejillaY + dy) }
        return TableroMedidas(ancho, alto, desplazamientos)
    }

    fun imagenSticker(context: Context, id: String, personas: Int): Bitmap? {
        val densidad = context.resources.displayMetrics.density
        val recurso = RecursosIconografiaMais.recurso(id) ?: return null
        val original = BitmapFactory.decodeResource(context.resources, recurso) ?: return null
        val w = (CELDA_ANCHO_DP * densidad).toInt()
        val h = (CELDA_ALTO_DP * densidad).toInt()
        val resultado = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        resultado.density = context.resources.displayMetrics.densityDpi
        val canvas = Canvas(resultado)
        val icono = ICONO_DP * densidad
        val izquierda = (w - icono) / 2f
        val arriba = 3f * densidad
        canvas.drawBitmap(original, null, RectF(izquierda, arriba, izquierda + icono, arriba + icono), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        original.recycle()

        // El número va encima de la figura, en una pastilla que queda dentro de la misma celda.
        val texto = personas.toString()
        val pincelTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textSize = (if (texto.length >= 4) 11f else 14f) * densidad
            textAlign = Paint.Align.CENTER
        }
        val anchoPastilla = max(24f * densidad, pincelTexto.measureText(texto) + 14f * densidad)
        val altoPastilla = 22f * densidad
        val centroX = w / 2f
        val centroY = arriba + icono - 2f * densidad
        val pastilla = RectF(
            centroX - anchoPastilla / 2f, centroY - altoPastilla / 2f,
            centroX + anchoPastilla / 2f, centroY + altoPastilla / 2f
        )
        canvas.drawRoundRect(pastilla, altoPastilla / 2f, altoPastilla / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(AZUL) })
        canvas.drawRoundRect(pastilla, altoPastilla / 2f, altoPastilla / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f * densidad; color = Color.WHITE
        })
        val metricas = pincelTexto.fontMetrics
        canvas.drawText(texto, centroX, pastilla.centerY() - (metricas.ascent + metricas.descent) / 2f, pincelTexto)
        return resultado
    }

    fun imagenTablero(context: Context, barrio: BarrioParlante): Bitmap {
        val densidad = context.resources.displayMetrics.density
        val medidas = medidas(barrio)
        val w = (medidas.anchoDp * densidad).toInt()
        val h = (medidas.altoDp * densidad).toInt()
        val resultado = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        resultado.density = context.resources.displayMetrics.densityDpi
        val canvas = Canvas(resultado)
        val cuerpoAlto = h - COLITA_DP * densidad
        val borde = 2f * densidad

        val forma = Path().apply {
            addRoundRect(RectF(borde, borde, w - borde, cuerpoAlto), 18f * densidad, 18f * densidad, Path.Direction.CW)
            moveTo(w / 2f - 11f * densidad, cuerpoAlto - 1f)
            lineTo(w / 2f, h - borde)
            lineTo(w / 2f + 11f * densidad, cuerpoAlto - 1f)
            close()
        }
        canvas.drawPath(forma, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F5FFFFFF") })
        canvas.drawPath(forma, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = borde; color = Color.parseColor(CIAN); strokeJoin = Paint.Join.ROUND
        })

        val pincelNombre = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(AZUL); typeface = Typeface.DEFAULT_BOLD; textSize = 17f * densidad
        }
        val nombre = TextUtils.ellipsize(barrio.nombre.uppercase(), android.text.TextPaint(pincelNombre), w - 2 * MARGEN_DP * densidad, TextUtils.TruncateAt.END).toString()
        canvas.drawText(nombre, MARGEN_DP * densidad, 24f * densidad, pincelNombre)
        val pincelTotales = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5B7083"); textSize = 12f * densidad
        }
        val familias = if (barrio.fichas == 1) "1 familia" else "${barrio.fichas} familias"
        val personas = if (barrio.personas == 1) "1 persona" else "${barrio.personas} personas"
        canvas.drawText("$personas · $familias", MARGEN_DP * densidad, 42f * densidad, pincelTotales)
        return resultado
    }
}
