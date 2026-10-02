package com.ruralitos.app

import com.ruralitos.app.domain.GrupoEdadFamiliar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class GrupoEdadFamiliarTest {
    private val formato = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
    private val referencia = formato.parse("16/07/2026")!!

    @Test
    fun clasificaLosLimitesDeEdadDeLaPlantilla() {
        assertEquals(GrupoEdadFamiliar.MENOR_UN_ANIO, grupo("17/07/2025"))
        assertEquals(GrupoEdadFamiliar.UNO_A_CUATRO, grupo("16/07/2025"))
        assertEquals(GrupoEdadFamiliar.UNO_A_CUATRO, grupo("17/07/2021"))
        assertEquals(GrupoEdadFamiliar.CINCO_A_NUEVE, grupo("16/07/2021"))
        assertEquals(GrupoEdadFamiliar.DIEZ_A_DIECINUEVE, grupo("16/07/2016"))
        assertEquals(GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO, grupo("16/07/2006"))
        assertEquals(GrupoEdadFamiliar.SESENTA_Y_CINCO_MAS, grupo("16/07/1961"))
    }

    @Test
    fun rechazaFechasInvalidasOFuturas() {
        assertNull(grupo("31/02/2020"))
        assertNull(grupo("17/07/2026"))
    }

    private fun grupo(fecha: String) = GrupoEdadFamiliar.calcular(fecha, referencia)
}
