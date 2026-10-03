package com.ruralitos.app

import com.ruralitos.app.domain.RecorridoVisitas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecorridoVisitasTest {
    private fun linea(vararg x: Double) = Array(x.size) { i -> DoubleArray(x.size) { j -> kotlin.math.abs(x[i] - x[j]) } }

    @Test fun `en una calle recta visita en orden desde el inicio`() {
        // posiciones: 0 (inicio), 9, 3, 6
        assertEquals(listOf(0, 2, 3, 1), RecorridoVisitas.ordenar(linea(0.0, 9.0, 3.0, 6.0), inicio = 0))
    }

    @Test fun `sin inicio elige el extremo que da el camino mas corto`() {
        val orden = RecorridoVisitas.ordenar(linea(5.0, 0.0, 10.0, 2.0))
        assertEquals(10.0, RecorridoVisitas.costoTotal(orden, linea(5.0, 0.0, 10.0, 2.0)), 1e-9)
    }

    @Test fun `el intercambio 2-opt corrige el vecino mas cercano`() {
        // Cuatro puntos en cuadro: cualquier orden por el borde cuesta 3.
        val m = arrayOf(
            doubleArrayOf(0.0, 1.0, 1.4, 1.0),
            doubleArrayOf(1.0, 0.0, 1.0, 1.4),
            doubleArrayOf(1.4, 1.0, 0.0, 1.0),
            doubleArrayOf(1.0, 1.4, 1.0, 0.0)
        )
        val orden = RecorridoVisitas.ordenar(m, inicio = 0)
        assertEquals(3.0, RecorridoVisitas.costoTotal(orden, m), 1e-9)
        assertEquals(0, orden.first())
    }

    @Test fun `casos pequenos`() {
        assertEquals(emptyList<Int>(), RecorridoVisitas.ordenar(emptyArray()))
        assertEquals(listOf(0), RecorridoVisitas.ordenar(arrayOf(doubleArrayOf(0.0))))
        assertEquals(listOf(1, 0), RecorridoVisitas.ordenar(linea(0.0, 1.0), inicio = 1))
    }

    @Test fun `un tramo sin camino se evita si hay otra salida`() {
        val inf = Double.POSITIVE_INFINITY
        val m = arrayOf(
            doubleArrayOf(0.0, inf, 1.0),
            doubleArrayOf(inf, 0.0, 1.0),
            doubleArrayOf(1.0, 1.0, 0.0)
        )
        val orden = RecorridoVisitas.ordenar(m, inicio = 0)
        assertEquals(listOf(0, 2, 1), orden)
        assertTrue(RecorridoVisitas.costoTotal(orden, m).isFinite())
    }

    @Test fun `con 25 paradas termina rapido`() {
        val puntos = (0 until 25).map { i -> (-1.8 + (i * 37 % 11) * 0.002) to (-78.2 + (i * 53 % 13) * 0.002) }
        val inicio = System.nanoTime()
        val orden = RecorridoVisitas.ordenar(RecorridoVisitas.matrizEnLinea(puntos), inicio = 0)
        assertEquals(25, orden.toSet().size)
        assertTrue((System.nanoTime() - inicio) / 1_000_000 < 5_000)
    }
}
