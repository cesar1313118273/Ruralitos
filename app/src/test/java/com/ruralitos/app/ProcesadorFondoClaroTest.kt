package com.ruralitos.app

import com.ruralitos.app.domain.ProcesadorFondoClaro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcesadorFondoClaroTest {
    @Test
    fun vuelveTransparenteElPapelBlancoYConservaLaTinta() {
        val blanco = ProcesadorFondoClaro.alfaResultante(255, 255, 255, 255, 55)
        val negro = ProcesadorFondoClaro.alfaResultante(18, 24, 38, 255, 55)

        assertEquals(0, blanco)
        assertEquals(255, negro)
    }

    @Test
    fun laBarraEliminaProgresivamenteSombrasClaras() {
        val suave = ProcesadorFondoClaro.alfaResultante(205, 205, 205, 255, 15)
        val intenso = ProcesadorFondoClaro.alfaResultante(205, 205, 205, 255, 90)

        assertTrue(intenso < suave)
    }
    @Test
    fun retiraPapelGrisFotografiadoYConservaTrazosDeColor() {
        val papelSombreado = 0xFFC8C5C0.toInt()
        val tintaAzul = 0xFF1859A9.toInt()
        val pixeles = intArrayOf(
            papelSombreado, papelSombreado, papelSombreado,
            papelSombreado, tintaAzul, papelSombreado,
            papelSombreado, papelSombreado, papelSombreado
        )

        val resultado = ProcesadorFondoClaro.procesar(3, 3, pixeles, 82)

        assertTrue(resultado.porcentajeTransparente >= 80)
        assertEquals(255, resultado.pixeles[4] ushr 24 and 0xFF)
        assertEquals(0, resultado.pixeles[0] ushr 24 and 0xFF)
    }
    @Test
    fun reutilizaElArregloDePixelesParaEvitarUnaCopiaGrande() {
        val pixeles = IntArray(16) { 0xFFFFFFFF.toInt() }

        val resultado = ProcesadorFondoClaro.procesar(4, 4, pixeles, 55)

        assertSame(pixeles, resultado.pixeles)
    }
}
