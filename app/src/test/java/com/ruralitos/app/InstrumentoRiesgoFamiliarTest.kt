package com.ruralitos.app

import com.ruralitos.app.domain.GrupoEdadFamiliar
import com.ruralitos.app.domain.InstrumentoRiesgoFamiliar
import com.ruralitos.app.domain.ReglasGrupoEdadFamiliar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumentoRiesgoFamiliarTest {
    @Test
    fun contieneLasDieciochoDefinicionesYUnicamenteValoresAplicables() {
        assertEquals(18, InstrumentoRiesgoFamiliar.definiciones.size)
        assertEquals(listOf(0, 2, 4), valores(1))
        assertEquals(listOf(0, 2, 4), valores(5))
        assertEquals(listOf(0, 4), valores(6))
        assertEquals(listOf(0, 4), valores(7))
        assertEquals(listOf(0, 4), valores(8))
        assertEquals(listOf(0, 4), valores(9))
        assertEquals(listOf(0, 2, 4), valores(11))
        assertEquals(listOf(0, 2, 4), valores(12))
        assertEquals(listOf(0, 2, 4), valores(14))
        assertTrue(InstrumentoRiesgoFamiliar.definiciones.all { it.opciones.isNotEmpty() })
        assertFalse(InstrumentoRiesgoFamiliar.valorValido(6, 1))
    }

    @Test
    fun muestraCategoriasConPalabrasSinExponerElPuntaje() {
        val opcion = InstrumentoRiesgoFamiliar.definiciones.first().opciones.last()
        assertTrue(opcion.textoVisible.startsWith("Riesgo alto:"))
        assertFalse(opcion.textoVisible.startsWith("4"))
    }

    @Test
    fun replicaLasCeldasGrisesDeLaTablaPorGrupoDeEdad() {
        val bebe = ReglasGrupoEdadFamiliar.camposPermitidos(GrupoEdadFamiliar.MENOR_UN_ANIO)
        assertFalse(bebe.ocupacion)
        assertTrue(bebe.escolaridades.isEmpty())

        val cincoANueve = ReglasGrupoEdadFamiliar.camposPermitidos(GrupoEdadFamiliar.CINCO_A_NUEVE)
        assertTrue(cincoANueve.ocupacion)
        assertEquals(setOf("SIN", "BAS"), cincoANueve.escolaridades)

        val adulto = ReglasGrupoEdadFamiliar.camposPermitidos(GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO)
        assertTrue(adulto.ocupacion)
        assertEquals(setOf("SIN", "BAS", "BACH", "SUP", "ESP"), adulto.escolaridades)
    }

    private fun valores(indice: Int) =
        InstrumentoRiesgoFamiliar.definiciones[indice].opciones.map { it.valor }
}
