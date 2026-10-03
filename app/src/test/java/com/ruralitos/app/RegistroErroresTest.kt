package com.ruralitos.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** El texto del informe no debe filtrar datos de pacientes: solo tipos de error y rutas del código. */
class RegistroErroresTest {
    @Test
    fun elMensajeDeLaExcepcionNuncaSeEscribe() {
        val error = IllegalStateException("Paciente PÉREZ LUIS cédula 0102030405")
        val texto = com.ruralitos.app.data.diagnostico.RegistroErrores.cuerpo("main", error)
        assertTrue(texto.contains("IllegalStateException"))
        assertFalse(texto.contains("PÉREZ"))
        assertFalse(texto.contains("0102030405"))
    }
}
