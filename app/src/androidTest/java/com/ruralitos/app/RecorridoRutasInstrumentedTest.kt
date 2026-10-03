package com.ruralitos.app

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.mapa.GestorRutasOffline
import com.ruralitos.app.domain.RecorridoVisitas
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng

/** Matriz de tiempos y ruta con varias paradas sobre la red vial incluida, sin internet. */
@RunWith(AndroidJUnit4::class)
class RecorridoRutasInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val quito = listOf(
        LatLng(-0.220164, -78.512327), LatLng(-0.203313, -78.490726),
        LatLng(-0.212000, -78.500000), LatLng(-0.190000, -78.480000)
    )

    @Test
    fun laMatrizDaTiemposYElRecorridoPasaPorTodasLasParadas() = runBlocking {
        val t0 = System.currentTimeMillis()
        val matriz = GestorRutasOffline.matriz(context, quito, "auto").getOrThrow()
        Log.i("RecorridoRutas", "matriz en ${System.currentTimeMillis() - t0} ms")
        assertEquals(4, matriz.size)
        assertTrue("los tiempos entre puntos distintos son positivos y finitos",
            (0 until 4).all { i -> (0 until 4).all { j -> i == j || (matriz[i][j] > 0 && matriz[i][j].isFinite()) } })
        val orden = RecorridoVisitas.ordenar(matriz, inicio = 0)
        assertEquals(0, orden.first())
        assertEquals(4, orden.toSet().size)
        val t1 = System.currentTimeMillis()
        val ruta = GestorRutasOffline.calcularVarios(context, orden.map { quito[it] }, "auto").getOrThrow()
        Log.i("RecorridoRutas", "ruta en ${System.currentTimeMillis() - t1} ms")
        assertEquals("un tramo entre cada par de paradas", 3, ruta.tramos.size)
        assertTrue(ruta.kilometros > 0)
    }

    @Test
    fun elModoMotoTambienCalcula() = runBlocking {
        val ruta = GestorRutasOffline.calcular(context, quito[0], quito[1], "motor_scooter").getOrThrow()
        assertTrue(ruta.kilometros in 1.0..30.0)
    }

    @Test
    fun unPuntoSinCaminoFallaRapidoEnVezDeBloquearElMotor() = runBlocking {
        val enElMar = LatLng(-1.0, -83.5)
        val t0 = System.currentTimeMillis()
        val resultado = GestorRutasOffline.calcularVarios(context, listOf(quito[0], enElMar), "auto")
        assertTrue("no debe haber ruta", resultado.isFailure)
        assertTrue("debe responder pronto", System.currentTimeMillis() - t0 < 30_000)
    }
}
