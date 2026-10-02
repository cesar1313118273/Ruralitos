package com.ruralitos.app

import com.ruralitos.app.domain.GrupoDispensarizacion
import com.ruralitos.app.domain.GrupoEdadRiesgo
import com.ruralitos.app.ui.screens.categoriasResumenGrupo
import com.ruralitos.app.ui.screens.datosGruposDona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumenRegistroGeneralTest {
    @Test
    fun grupoUnoSoloMuestraEdadesConIconoPropio() {
        assertEquals(
            listOf(GrupoEdadRiesgo.MENOR_DOS, GrupoEdadRiesgo.DOS_NUEVE, GrupoEdadRiesgo.EMBARAZADA),
            categoriasResumenGrupo(GrupoDispensarizacion.I)
        )
        assertTrue(categoriasResumenGrupo(GrupoDispensarizacion.IV).isEmpty())
    }

    @Test
    fun donaExcluyePendientesYCerosSinCambiarOrdenDeGrupos() {
        val visibles = datosGruposDona(
            listOf(
                "Grupo ?" to 2,
                "Grupo III" to 3,
                "Grupo I" to 1,
                "Grupo II" to 0,
                "Con discapacidad" to 4
            )
        )
        assertEquals(
            listOf(GrupoDispensarizacion.III, GrupoDispensarizacion.I, GrupoDispensarizacion.IV),
            visibles.map { it.third }
        )
        assertEquals(8, visibles.sumOf { it.second })
    }
}
