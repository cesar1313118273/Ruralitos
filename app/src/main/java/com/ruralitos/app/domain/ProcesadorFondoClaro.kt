package com.ruralitos.app.domain

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Elimina papel blanco o claro conservando la tinta y los colores del dibujo.
 *
 * El color del papel se estima desde los bordes de la fotografía. Así también se
 * pueden retirar sombras grises o papeles ligeramente amarillentos, no solo el
 * blanco puro.
 */
object ProcesadorFondoClaro {
    data class Resultado(
        val pixeles: IntArray,
        val porcentajeTransparente: Int
    )

    fun procesar(
        ancho: Int,
        alto: Int,
        pixelesOriginales: IntArray,
        intensidad: Int
    ): Resultado {
        require(ancho > 0 && alto > 0)
        require(pixelesOriginales.size == ancho * alto)

        val fondo = estimarColorPapel(ancho, alto, pixelesOriginales)
        // El arreglo recibido se acaba de crear exclusivamente para este procesamiento.
        // Reutilizarlo evita reservar otra copia de varios megabytes en fotografías grandes.
        val salida = pixelesOriginales
        var transparentes = 0

        pixelesOriginales.forEachIndexed { indice, color ->
            val alfaOriginal = color ushr 24 and 0xFF
            val rojo = color ushr 16 and 0xFF
            val verde = color ushr 8 and 0xFF
            val azul = color and 0xFF
            val alfa = alfaResultante(
                rojo = rojo,
                verde = verde,
                azul = azul,
                alfaOriginal = alfaOriginal,
                intensidad = intensidad,
                rojoFondo = fondo[0],
                verdeFondo = fondo[1],
                azulFondo = fondo[2]
            )
            if (alfa <= 8) transparentes++
            salida[indice] = (alfa shl 24) or (rojo shl 16) or (verde shl 8) or azul
        }

        return Resultado(
            pixeles = salida,
            porcentajeTransparente =
                (transparentes * 100f / salida.size.coerceAtLeast(1)).roundToInt()
        )
    }

    fun alfaResultante(
        rojo: Int,
        verde: Int,
        azul: Int,
        alfaOriginal: Int,
        intensidad: Int
    ): Int = alfaResultante(
        rojo = rojo,
        verde = verde,
        azul = azul,
        alfaOriginal = alfaOriginal,
        intensidad = intensidad,
        rojoFondo = 255,
        verdeFondo = 255,
        azulFondo = 255
    )

    private fun alfaResultante(
        rojo: Int,
        verde: Int,
        azul: Int,
        alfaOriginal: Int,
        intensidad: Int,
        rojoFondo: Int,
        verdeFondo: Int,
        azulFondo: Int
    ): Int {
        if (alfaOriginal <= 0) return 0
        val nivel = intensidad.coerceIn(0, 100) / 100f
        val r = rojo.coerceIn(0, 255)
        val g = verde.coerceIn(0, 255)
        val b = azul.coerceIn(0, 255)
        val minimo = minOf(r, g, b)
        val maximo = maxOf(r, g, b)

        // Papel blanco: exige que los tres canales sean claros; así no borra
        // amarillos, verdes u otros colores vivos del familiograma.
        val umbralBlanco = 248f - nivel * 92f
        val blancura = ((minimo - umbralBlanco) / 34f).coerceIn(0f, 1f)
        val saturacion = (maximo - minimo) / 255f
        val afinidadBlanco = (blancura * (1f - saturacion * 0.78f)).coerceIn(0f, 1f)

        // Papel sombreado o con tono cálido: compara con el color claro que se
        // encontró en los bordes de la fotografía.
        val distanciaFondo = maxOf(
            abs(r - rojoFondo),
            abs(g - verdeFondo),
            abs(b - azulFondo)
        ).toFloat()
        val toleranciaFondo = 12f + nivel * 112f
        val afinidadFondo = ((toleranciaFondo - distanciaFondo) / 38f).coerceIn(0f, 1f)
        val luminancia = (r * 299 + g * 587 + b * 114) / 1000f
        val claridadMinima = 128f + (1f - nivel) * 42f
        val factorClaridad = ((luminancia - claridadMinima) / 52f).coerceIn(0f, 1f)

        val remocion = maxOf(afinidadBlanco, afinidadFondo * factorClaridad)
        return (alfaOriginal.coerceIn(0, 255) * (1f - remocion))
            .roundToInt()
            .coerceIn(0, 255)
    }

    private fun estimarColorPapel(ancho: Int, alto: Int, pixeles: IntArray): IntArray {
        val muestras = ArrayList<Int>(300)
        val pasoX = (ancho / 80).coerceAtLeast(1)
        val pasoY = (alto / 80).coerceAtLeast(1)

        fun agregar(x: Int, y: Int) {
            val color = pixeles[y.coerceIn(0, alto - 1) * ancho + x.coerceIn(0, ancho - 1)]
            if ((color ushr 24 and 0xFF) > 16) muestras += color
        }

        for (x in 0 until ancho step pasoX) {
            agregar(x, 0)
            agregar(x, (alto - 1).coerceAtLeast(0))
        }
        for (y in 0 until alto step pasoY) {
            agregar(0, y)
            agregar((ancho - 1).coerceAtLeast(0), y)
        }
        if (muestras.isEmpty()) return intArrayOf(255, 255, 255)

        // Se toma la mitad más clara del borde para evitar que una mesa oscura
        // o la sombra del teléfono sea confundida con el papel.
        val claras = muestras.sortedByDescending { color ->
            val r = color ushr 16 and 0xFF
            val g = color ushr 8 and 0xFF
            val b = color and 0xFF
            r * 299 + g * 587 + b * 114
        }.take((muestras.size / 2).coerceAtLeast(1))

        fun mediana(selector: (Int) -> Int): Int {
            val valores = claras.map(selector).sorted()
            return valores[valores.size / 2]
        }
        return intArrayOf(
            mediana { it ushr 16 and 0xFF },
            mediana { it ushr 8 and 0xFF },
            mediana { it and 0xFF }
        )
    }
}
