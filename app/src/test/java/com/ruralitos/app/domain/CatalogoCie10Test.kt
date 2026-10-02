package com.ruralitos.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogoCie10Test {
    private val catalogo = listOf(
        DiagnosticoCie10("I10", "Hipertensión esencial (primaria)"),
        DiagnosticoCie10("E11.9", "Diabetes mellitus tipo 2 sin complicaciones"),
        DiagnosticoCie10("J45.9", "Asma no especificada")
    )

    @Test fun buscaPorCodigoYDescripcionIgnorandoTildes() {
        assertEquals("I10", CatalogoCie10.buscar(catalogo, "i10").single().codigo)
        assertEquals("I10", CatalogoCie10.buscar(catalogo, "hipertension esencial").single().codigo)
        assertEquals("J45.9", CatalogoCie10.buscar(catalogo, "asma").single().codigo)
    }
}
