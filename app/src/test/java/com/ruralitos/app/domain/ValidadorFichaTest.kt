package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorFichaTest {
    private fun fichaValida() = FichaFamiliarEntity(
        cedulaJefeHogar = "0926687856",
        institucionSistema = "CLINICA DE PRUEBA",
        unidadOperativa = "Centro",
        codigoUo = "001",
        areaNumero = "1",
        codigoLocalizacion = "010101",
        parroquiaCodigoLocalizacion = "01",
        cantonCodigoLocalizacion = "01",
        provinciaCodigoLocalizacion = "01",
        numeroFichaFamiliar = "F-1",
        provincia = "Guayas",
        canton = "Guayaquil",
        parroquia = "Tarqui",
        sector = "Norte",
        manzana = "1",
        numeroFamilia = "1",
        direccionHabitualFamilia = "Calle principal",
        barrio = "Barrio",
        numeroCasa = "1",
        comunidad = "",
        grupoCultural = "",
        nombreApellidoJefeFamilia = "Familia Prueba",
        numeroTelefono = "0999999999",
        fechaLlenado = "17/07/2026",
        numeroCarpeta = "1",
        responsableNombre = "Médico Prueba",
        firmaUri = "file:///firma.png"
    )

    @Test
    fun `una ficha con requisitos completos no tiene pendientes`() {
        val requisitos = ValidadorFicha.revisar(
            fichaValida(),
            cantidadMiembros = 1,
            cantidadCalificaciones = 1,
            tiposAdjuntos = setOf("FAMILIOGRAMA", "CROQUIS")
        )

        assertTrue(requisitos.all { it.cumplido })
    }

    @Test
    fun `detecta firma miembros riesgos y adjuntos faltantes`() {
        val requisitos = ValidadorFicha.revisar(
            fichaValida().copy(firmaUri = null),
            cantidadMiembros = 0,
            cantidadCalificaciones = 0,
            tiposAdjuntos = emptySet()
        )

        assertEquals(
            setOf("miembros", "riesgos", "familiograma", "croquis", "firma"),
            requisitos.filterNot { it.cumplido }.map { it.id }.toSet()
        )
    }
}
