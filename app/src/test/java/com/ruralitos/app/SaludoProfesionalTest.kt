package com.ruralitos.app

import com.ruralitos.app.domain.SaludoProfesional
import org.junit.Assert.assertEquals
import org.junit.Test

class SaludoProfesionalTest {
    @Test
    fun respetaLosLimitesHorariosSolicitados() {
        assertEquals("Buenas noches", SaludoProfesional.segunHora(5))
        assertEquals("Buenos días", SaludoProfesional.segunHora(6))
        assertEquals("Buenos días", SaludoProfesional.segunHora(11))
        assertEquals("Buenas tardes", SaludoProfesional.segunHora(12))
        assertEquals("Buenas tardes", SaludoProfesional.segunHora(17))
        assertEquals("Buenas noches", SaludoProfesional.segunHora(18))
        assertEquals("Buenas noches", SaludoProfesional.segunHora(23))
    }

    @Test
    fun construyeElSaludoConCargoYNombres() {
        assertEquals(
            "Buenas tardes, Doctor/a Ana Pérez",
            SaludoProfesional.completo(14, "Médico/a", " Ana   Pérez ")
        )
    }

    @Test
    fun aplicaTratamientosProfesionalesPorCargo() {
        assertEquals("Doctor/a", SaludoProfesional.tratamiento("Médico/a"))
        assertEquals("Doctor/a", SaludoProfesional.tratamiento("Odontólogo/a"))
        assertEquals("Licenciado/a", SaludoProfesional.tratamiento("Enfermero/a"))
        assertEquals("Licenciado/a", SaludoProfesional.tratamiento("Obstetra"))
        assertEquals("Licenciado/a", SaludoProfesional.tratamiento("Psicólogo/a"))
        assertEquals("Señor/a", SaludoProfesional.tratamiento("TAPS"))
        assertEquals("Señor/a", SaludoProfesional.tratamiento("Administrativo/a"))
    }
}
