package com.ruralitos.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.domain.CatalogoCie10
import com.ruralitos.app.domain.DiagnosticoCie10
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogoCie10InstrumentedTest {
    @Test
    fun conservaLaDescompensacionDentroDelDiagnostico() {
        val original = listOf(
            DiagnosticoCie10("I10", "Hipertensión esencial", descompensada = true)
        )

        val restaurado = CatalogoCie10.decodificar(CatalogoCie10.codificar(original)).single()

        assertEquals("I10", restaurado.codigo)
        assertTrue(restaurado.descompensada)
    }
}
