package com.ruralitos.app

import com.ruralitos.app.data.export.ConsolidadoExcelExporter
import com.ruralitos.app.domain.DiagnosticoCie10
import org.junit.Assert.assertEquals
import org.junit.Test

class ConsolidadoExcelExporterTest {
    @Test
    fun cronicasUnoYDosRespetanElOrdenDeSeleccionCie10() {
        val categorias = ConsolidadoExcelExporter.categoriasCronicasPrueba(
            listOf(
                DiagnosticoCie10("I10", "Hipertensión esencial"),
                DiagnosticoCie10("E11.9", "Diabetes mellitus tipo 2"),
                DiagnosticoCie10("C50", "Cáncer de mama")
            )
        )

        assertEquals(listOf(2, 1), categorias)
    }

}
