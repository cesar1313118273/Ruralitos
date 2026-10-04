package com.ruralitos.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.ruralitos.app.domain.ElementoCroquis
import com.ruralitos.app.domain.SimboloCroquis
import com.ruralitos.app.domain.TipoElementoCroquis

/**
 * Dibuja los símbolos y textos del croquis como imágenes: se pegan sobre el mapa y salen en la captura que va al
 * Excel y al PDF, sin depender de fuentes del mapa, así que funcionan sin internet.
 */
object DibujoCroquis {
    private const val AZUL = 0xFF185FA5.toInt()
    private const val NARANJA = 0xFFEF790F.toInt()
    private const val TINTA = 0xFF27415A.toInt()
    private const val GRIS = 0xFF7C8DA0.toInt()
    private const val MAX_CARACTERES = 26

    /** Nombre único de la imagen de un elemento en su estado actual (cambia si cambia su texto o su selección). */
    fun nombreImagen(elemento: ElementoCroquis, seleccionado: Boolean): String =
        "croquis_${elemento.id}_${(elemento.tipo.name + elemento.codigo + elemento.etiqueta).hashCode()}_${if (seleccionado) "s" else "n"}"

    fun bitmapElemento(densidad: Float, elemento: ElementoCroquis, seleccionado: Boolean): Bitmap =
        if (elemento.tipo == TipoElementoCroquis.TEXTO) bitmapTexto(densidad, elemento.etiqueta, seleccionado)
        else bitmapSimbolo(
            densidad, SimboloCroquis.deCodigo(elemento.codigo) ?: SimboloCroquis.CASA, seleccionado, elemento.etiqueta
        )

    /** Círculo con el dibujo del símbolo y, si hay [etiqueta], su nombre debajo; el centro del círculo es el del bitmap. */
    fun bitmapSimbolo(densidad: Float, simbolo: SimboloCroquis, seleccionado: Boolean = false, etiqueta: String? = null): Bitmap {
        val diametro = 34f * densidad
        val letra = pintaTexto(11f * densidad, false)
        val textoFinal = etiqueta?.takeIf { it.isNotBlank() }?.let(::acortar)
        val anchoTexto = textoFinal?.let { letra.measureText(it) + 12f * densidad } ?: 0f
        val altoEtiqueta = if (textoFinal != null) 17f * densidad else 0f
        val hueco = if (textoFinal != null) 2f * densidad else 0f
        val ancho = maxOf(diametro, anchoTexto) + 6f * densidad
        val alto = diametro + 2f * (altoEtiqueta + hueco) + 6f * densidad
        val bitmap = Bitmap.createBitmap(ancho.toInt() + 1, alto.toInt() + 1, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(bitmap)
        val cx = ancho / 2f
        val cy = alto / 2f
        val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (seleccionado) 0xFFFFF3E3.toInt() else 0xFFFFFFFF.toInt() }
        lienzo.drawCircle(cx, cy, diametro / 2f, relleno)
        val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (if (seleccionado) 3f else 2f) * densidad
            color = if (seleccionado) NARANJA else AZUL
        }
        lienzo.drawCircle(cx, cy, diametro / 2f - borde.strokeWidth / 2f, borde)
        glifo(lienzo, simbolo, cx, cy, diametro * 0.62f, if (seleccionado) 0xFFB25A00.toInt() else TINTA)
        if (textoFinal != null) {
            val arriba = cy + diametro / 2f + hueco
            etiquetaRedondeada(lienzo, textoFinal, cx, arriba, altoEtiqueta, anchoTexto, letra, densidad, seleccionado, false)
        }
        return bitmap
    }

    /** Un texto libre sobre el mapa, en una etiqueta de bordes finos. */
    fun bitmapTexto(densidad: Float, texto: String, seleccionado: Boolean): Bitmap {
        val letra = pintaTexto(12f * densidad, true)
        val textoFinal = acortar(texto.ifBlank { " " })
        val ancho = letra.measureText(textoFinal) + 16f * densidad
        val alto = 24f * densidad
        val bitmap = Bitmap.createBitmap((ancho + 6f * densidad).toInt() + 1, (alto + 6f * densidad).toInt() + 1, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(bitmap)
        etiquetaRedondeada(lienzo, textoFinal, bitmap.width / 2f, (bitmap.height - alto) / 2f, alto, ancho, letra, densidad, seleccionado, true)
        return bitmap
    }

    private fun acortar(texto: String) = if (texto.length > MAX_CARACTERES) texto.take(MAX_CARACTERES - 1) + "…" else texto

    private fun pintaTexto(tamano: Float, negrita: Boolean) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TINTA
        textSize = tamano
        isFakeBoldText = negrita
        textAlign = Paint.Align.CENTER
    }

    private fun etiquetaRedondeada(
        lienzo: Canvas, texto: String, centroX: Float, arriba: Float, alto: Float, ancho: Float,
        letra: Paint, densidad: Float, seleccionado: Boolean, conBorde: Boolean
    ) {
        val caja = RectF(centroX - ancho / 2f, arriba, centroX + ancho / 2f, arriba + alto)
        val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2FFFFFF.toInt() }
        lienzo.drawRoundRect(caja, 6f * densidad, 6f * densidad, fondo)
        if (conBorde || seleccionado) {
            val linea = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = (if (seleccionado) 2f else 1f) * densidad
                color = if (seleccionado) NARANJA else GRIS
            }
            lienzo.drawRoundRect(caja, 6f * densidad, 6f * densidad, linea)
        }
        val base = caja.centerY() - (letra.ascent() + letra.descent()) / 2f
        lienzo.drawText(texto, centroX, base, letra)
    }

    /** Dibujo de línea de cada símbolo en una cuadrícula de 24 × 24. */
    private fun glifo(lienzo: Canvas, simbolo: SimboloCroquis, cx: Float, cy: Float, lado: Float, color: Int) {
        val u = lado / 24f
        val x0 = cx - lado / 2f
        val y0 = cy - lado / 2f
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.9f * u
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.color = color
        }
        fun x(v: Float) = x0 + v * u
        fun y(v: Float) = y0 + v * u
        fun linea(a: Float, b: Float, c: Float, d: Float) = lienzo.drawLine(x(a), y(b), x(c), y(d), pincel)
        fun caja(l: Float, t: Float, r: Float, b: Float) = lienzo.drawRect(x(l), y(t), x(r), y(b), pincel)
        fun circulo(px: Float, py: Float, r: Float) = lienzo.drawCircle(x(px), y(py), r * u, pincel)
        fun poligono(cerrado: Boolean, vararg v: Float) {
            val camino = Path().apply {
                moveTo(x(v[0]), y(v[1]))
                var i = 2
                while (i < v.size) { lineTo(x(v[i]), y(v[i + 1])); i += 2 }
                if (cerrado) close()
            }
            lienzo.drawPath(camino, pincel)
        }
        when (simbolo) {
            SimboloCroquis.CASA -> {
                poligono(false, 3f, 12f, 12f, 4f, 21f, 12f)
                caja(6f, 12f, 18f, 20f)
                caja(10.5f, 15f, 13.5f, 20f)
            }
            SimboloCroquis.IGLESIA -> {
                caja(6f, 11f, 18f, 21f)
                poligono(false, 6f, 11f, 12f, 7f, 18f, 11f)
                linea(12f, 2f, 12f, 7f); linea(10f, 4f, 14f, 4f)
                caja(10.5f, 16f, 13.5f, 21f)
            }
            SimboloCroquis.ESCUELA -> {
                caja(4f, 10f, 20f, 20f)
                poligono(false, 3f, 10f, 12f, 4f, 21f, 10f)
                caja(6f, 12.5f, 9.5f, 15.5f); caja(14.5f, 12.5f, 18f, 15.5f)
                caja(10.5f, 15f, 13.5f, 20f)
            }
            SimboloCroquis.PARQUE -> {
                circulo(12f, 9f, 5.5f)
                linea(12f, 14.5f, 12f, 21f); linea(8f, 21f, 16f, 21f)
            }
            SimboloCroquis.CANCHA -> {
                caja(3f, 6f, 21f, 18f)
                linea(12f, 6f, 12f, 18f); circulo(12f, 12f, 3f)
            }
            SimboloCroquis.TIENDA -> {
                poligono(false, 4f, 9f, 6f, 4f, 18f, 4f, 20f, 9f)
                caja(5f, 9f, 19f, 20f); caja(10f, 13f, 14f, 20f)
            }
            SimboloCroquis.SALUD -> {
                lienzo.drawRoundRect(RectF(x(4f), y(4f), x(20f), y(20f)), 3f * u, 3f * u, pincel)
                linea(12f, 8f, 12f, 16f); linea(8f, 12f, 16f, 12f)
            }
            SimboloCroquis.PARADA -> {
                lienzo.drawRoundRect(RectF(x(5f), y(4f), x(19f), y(17f)), 2.5f * u, 2.5f * u, pincel)
                linea(5f, 10f, 19f, 10f); circulo(8.5f, 19.5f, 1.5f); circulo(15.5f, 19.5f, 1.5f)
            }
            SimboloCroquis.RIO -> {
                for (altura in floatArrayOf(8f, 12.5f, 17f)) {
                    val onda = Path().apply {
                        moveTo(x(3f), y(altura))
                        quadTo(x(6f), y(altura - 3f), x(9f), y(altura))
                        quadTo(x(12f), y(altura + 3f), x(15f), y(altura))
                        quadTo(x(18f), y(altura - 3f), x(21f), y(altura))
                    }
                    lienzo.drawPath(onda, pincel)
                }
            }
            SimboloCroquis.PUENTE -> {
                linea(2f, 14f, 22f, 14f)
                val arco = Path().apply { moveTo(x(4f), y(14f)); quadTo(x(12f), y(4f), x(20f), y(14f)) }
                lienzo.drawPath(arco, pincel)
                linea(8f, 10f, 8f, 14f); linea(12f, 8.5f, 12f, 14f); linea(16f, 10f, 16f, 14f)
                linea(4f, 14f, 4f, 20f); linea(20f, 14f, 20f, 20f)
            }
            SimboloCroquis.COMUNAL -> {
                poligono(false, 3f, 11f, 12f, 5f, 21f, 11f)
                linea(12f, 5f, 12f, 2f); poligono(true, 12f, 2f, 16f, 3.5f, 12f, 5f)
                linea(7f, 12f, 7f, 20f); linea(12f, 12f, 12f, 20f); linea(17f, 12f, 17f, 20f); linea(4f, 20f, 20f, 20f)
            }
            SimboloCroquis.MERCADO -> {
                poligono(true, 3f, 9f, 21f, 9f, 19f, 19f, 5f, 19f)
                val asa = Path().apply { moveTo(x(8f), y(9f)); quadTo(x(12f), y(1f), x(16f), y(9f)) }
                lienzo.drawPath(asa, pincel)
                linea(10f, 12f, 10f, 16f); linea(14f, 12f, 14f, 16f)
            }
            SimboloCroquis.CEMENTERIO -> {
                linea(12f, 3f, 12f, 19f); linea(8f, 8f, 16f, 8f); linea(7f, 21f, 17f, 21f)
            }
            SimboloCroquis.FABRICA -> {
                poligono(true, 4f, 20f, 4f, 10f, 10f, 14f, 10f, 10f, 16f, 14f, 16f, 6f, 20f, 6f, 20f, 20f)
            }
        }
    }
}
