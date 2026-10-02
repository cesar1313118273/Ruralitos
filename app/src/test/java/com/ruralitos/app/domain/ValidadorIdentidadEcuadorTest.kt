package com.ruralitos.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorIdentidadEcuadorTest {
    @Test
    fun documentoFamiliarAceptaDiezDigitosAunqueNoTengaChecksumCivil() {
        assertTrue(ValidadorIdentidadEcuador.esDocumentoFamiliarAceptable("1234567890"))
        assertTrue(ValidadorIdentidadEcuador.esDocumentoFamiliarAceptable("1234567890123"))
        assertFalse(ValidadorIdentidadEcuador.esDocumentoFamiliarAceptable("123456789"))
        assertFalse(ValidadorIdentidadEcuador.esDocumentoFamiliarAceptable("123456789X"))
    }

    @Test
    fun `acepta cedulas con digito verificador correcto`() {
        assertTrue(ValidadorIdentidadEcuador.esCedulaValida("1710034065"))
        assertTrue(ValidadorIdentidadEcuador.esCedulaValida("0926687856"))
    }

    @Test
    fun `rechaza provincia digito y longitud incorrectos`() {
        assertFalse(ValidadorIdentidadEcuador.esCedulaValida("1300000000"))
        assertFalse(ValidadorIdentidadEcuador.esCedulaValida("9910034065"))
        assertFalse(ValidadorIdentidadEcuador.esCedulaValida("171003406"))
    }

    @Test
    fun `acepta identificacion tributaria de trece digitos`() {
        assertTrue(ValidadorIdentidadEcuador.esIdentificacionAceptable("1790012345001"))
    }
}
