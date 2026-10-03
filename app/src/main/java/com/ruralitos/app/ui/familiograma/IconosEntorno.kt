package com.ruralitos.app.ui.familiograma

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.ruralitos.app.domain.familiograma.TipoEntorno

/**
 * Íconos sencillos de actividades y lugares de la comunidad, dibujados con trazos vectoriales
 * (quedan nítidos en pantalla, en el Excel y en el PDF). El origen (0, 0) es el centro del ícono
 * y caben en un cuadrado de unos 26 unidades.
 */
internal object IconosEntorno {
    private fun conColor(base: Paint, color: Int, grosor: Float? = null) = Paint(base).apply {
        this.color = color
        if (grosor != null) strokeWidth = grosor
    }

    fun dibujar(c: Canvas, tipo: TipoEntorno, linea: Paint, suave: Paint) {
        val verde = conColor(linea, COLOR_VERDE)
        val verdeRelleno = conColor(suave, 0xFFCDE8BE.toInt())
        val rojo = conColor(linea, COLOR_ROJO, 4.6f).apply { strokeCap = Paint.Cap.BUTT }
        val azul = conColor(linea, COLOR_AZUL)
        when (tipo) {
            TipoEntorno.IGLESIA -> {
                c.drawRect(-8f, -2f, 8f, 11f, suave); c.drawRect(-8f, -2f, 8f, 11f, linea)
                c.drawPath(triangulo(-10f, -2f, 0f, -11f, 10f, -2f), suave)
                c.drawPath(triangulo(-10f, -2f, 0f, -11f, 10f, -2f), linea)
                c.drawLine(0f, -17f, 0f, -11f, linea); c.drawLine(-2.6f, -14.5f, 2.6f, -14.5f, linea)
                c.drawRect(-2.5f, 4f, 2.5f, 11f, linea)
            }
            TipoEntorno.ESCUELA -> {
                c.drawRect(-10f, -1f, 10f, 10f, suave); c.drawRect(-10f, -1f, 10f, 10f, linea)
                c.drawPath(triangulo(-12f, -1f, 0f, -9f, 12f, -1f), suave)
                c.drawPath(triangulo(-12f, -1f, 0f, -9f, 12f, -1f), linea)
                c.drawLine(0f, -9f, 0f, -16f, linea)
                c.drawPath(triangulo(0f, -16f, 8f, -13.5f, 0f, -11f), conColor(suave, COLOR_VERDE))
                c.drawRect(-2.5f, 4f, 2.5f, 10f, linea)
            }
            TipoEntorno.CENTRO_SALUD -> {
                c.drawCircle(0f, 0f, 12f, Paint(suave).apply { color = 0xFFFFFFFF.toInt() })
                c.drawCircle(0f, 0f, 12f, linea)
                c.drawLine(0f, -7f, 0f, 7f, rojo); c.drawLine(-7f, 0f, 7f, 0f, rojo)
            }
            TipoEntorno.MERCADO -> {
                c.drawRect(-9f, -3f, 9f, 11f, suave); c.drawRect(-9f, -3f, 9f, 11f, linea)
                val toldo = Path().apply { moveTo(-12f, -3f); lineTo(-9f, -11f); lineTo(9f, -11f); lineTo(12f, -3f); close() }
                c.drawPath(toldo, suave); c.drawPath(toldo, linea)
                c.drawLine(-3f, -11f, -4f, -3f, linea); c.drawLine(3f, -11f, 4f, -3f, linea)
                c.drawRect(-3f, 2f, 3f, 11f, linea)
            }
            TipoEntorno.CANCHA -> {
                c.drawRect(-12f, -8f, 12f, 8f, verdeRelleno); c.drawRect(-12f, -8f, 12f, 8f, verde)
                c.drawLine(0f, -8f, 0f, 8f, verde); c.drawCircle(0f, 0f, 3.6f, verde)
                c.drawRect(-12f, -3.6f, -8f, 3.6f, verde); c.drawRect(8f, -3.6f, 12f, 3.6f, verde)
            }
            TipoEntorno.FINCA -> {
                c.drawLine(-11f, 10f, 11f, 10f, verde)
                c.drawLine(0f, 10f, 0f, -7f, verde)
                val hojaIzquierda = Path().apply { moveTo(0f, 3f); quadTo(-11f, 3f, -10f, -6f); quadTo(0f, -6f, 0f, 3f) }
                val hojaDerecha = Path().apply { moveTo(0f, -2f); quadTo(11f, -2f, 10f, -11f); quadTo(0f, -11f, 0f, -2f) }
                listOf(hojaIzquierda, hojaDerecha).forEach { c.drawPath(it, verdeRelleno); c.drawPath(it, verde) }
            }
            TipoEntorno.JUNTA -> {
                listOf(-8f to -5f, 0f to -8f, 8f to -5f).forEach { (x, y) -> c.drawCircle(x, y, 3.6f, linea) }
                val cuerpos = Path().apply {
                    moveTo(-13f, 9f); quadTo(-8f, -1f, -3f, 9f)
                    moveTo(-5f, 10f); quadTo(0f, -2f, 5f, 10f)
                    moveTo(3f, 9f); quadTo(8f, -1f, 13f, 9f)
                }
                c.drawPath(cuerpos, linea)
            }
            TipoEntorno.TRABAJO -> {
                c.drawRoundRect(RectF(-11f, -5f, 11f, 9f), 2.5f, 2.5f, suave)
                c.drawRoundRect(RectF(-11f, -5f, 11f, 9f), 2.5f, 2.5f, linea)
                val asa = Path().apply { moveTo(-4f, -5f); lineTo(-4f, -9f); lineTo(4f, -9f); lineTo(4f, -5f) }
                c.drawPath(asa, linea)
                c.drawLine(-11f, 1.5f, 11f, 1.5f, linea)
            }
            TipoEntorno.RIO -> {
                listOf(-7f, 0f, 7f).forEach { y ->
                    val ola = Path().apply {
                        moveTo(-12f, y)
                        rQuadTo(3f, -3.6f, 6f, 0f); rQuadTo(3f, 3.6f, 6f, 0f)
                        rQuadTo(3f, -3.6f, 6f, 0f); rQuadTo(3f, 3.6f, 6f, 0f)
                    }
                    c.drawPath(ola, azul)
                }
            }
            TipoEntorno.TRANSPORTE -> {
                c.drawRoundRect(RectF(-11f, -10f, 11f, 6f), 3.5f, 3.5f, suave)
                c.drawRoundRect(RectF(-11f, -10f, 11f, 6f), 3.5f, 3.5f, linea)
                c.drawRect(-8f, -7f, -1f, -2f, linea); c.drawRect(1f, -7f, 8f, -2f, linea)
                c.drawCircle(-6f, 8.5f, 2.6f, Paint(suave).apply { color = COLOR_CIAN_OSCURO })
                c.drawCircle(6f, 8.5f, 2.6f, Paint(suave).apply { color = COLOR_CIAN_OSCURO })
            }
            TipoEntorno.PARQUE -> {
                c.drawCircle(0f, -4f, 8f, verdeRelleno); c.drawCircle(0f, -4f, 8f, verde)
                c.drawLine(0f, 4f, 0f, 11f, conColor(linea, 0xFF7A5A3A.toInt(), 2.6f))
                c.drawLine(-7f, 11f, 7f, 11f, verde)
            }
        }
    }

    private fun triangulo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) = Path().apply {
        moveTo(x1, y1); lineTo(x2, y2); lineTo(x3, y3); close()
    }
}
