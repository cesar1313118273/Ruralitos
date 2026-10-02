package com.ruralitos.app.data.agenda

import com.ruralitos.app.domain.GrupoDispensarizacion
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class PlanificadorSeguimientoTest {
    @Test
    fun frecuenciasConservanFechasDistintasPorGrupo() {
        val base = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 29, 11, 20, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val mesesEsperados = mapOf(
            GrupoDispensarizacion.I to 12,
            GrupoDispensarizacion.II to 6,
            GrupoDispensarizacion.III to 4,
            GrupoDispensarizacion.IV to 3
        )
        mesesEsperados.forEach { (grupo, meses) ->
            val esperado = Calendar.getInstance().apply {
                timeInMillis = base
                add(Calendar.MONTH, meses)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            assertEquals(grupo.codigo, esperado, PlanificadorSeguimiento.proximaFecha(base, grupo))
        }
    }
}
