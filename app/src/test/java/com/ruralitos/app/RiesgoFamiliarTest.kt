package com.ruralitos.app

import com.ruralitos.app.domain.RiesgoFamiliar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RiesgoFamiliarTest {
    @Test
    fun clasificaTodosLosLimitesDelFormulario() {
        assertEquals("SIN_RIESGO", resultadoConTotal(0))
        assertEquals("BAJO", resultadoConTotal(1))
        assertEquals("BAJO", resultadoConTotal(14))
        assertEquals("MEDIO", resultadoConTotal(15))
        assertEquals("MEDIO", resultadoConTotal(34))
        assertEquals("ALTO", resultadoConTotal(35))
        assertEquals("ALTO", resultadoConTotal(72))
    }

    @Test
    fun rechazaValoresFueraDeCeroACuatro() {
        val valores = MutableList(18) { 0 }
        valores[0] = 5
        assertThrows(IllegalArgumentException::class.java) {
            RiesgoFamiliar.calcular(valores)
        }
    }

    private fun resultadoConTotal(total: Int): String {
        var restante = total
        val valores = MutableList(18) {
            val valor = restante.coerceAtMost(4)
            restante -= valor
            valor
        }
        return RiesgoFamiliar.calcular(valores).nivel
    }
}
