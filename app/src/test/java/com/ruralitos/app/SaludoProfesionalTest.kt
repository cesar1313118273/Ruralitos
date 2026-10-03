package com.ruralitos.app

import com.ruralitos.app.domain.SaludoProfesional
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun loscargosDependenDelSexo() {
        assertEquals(
            listOf("Doctora", "Enfermera", "TAPS", "Administradora", "Odontóloga", "Obstetra", "Psicóloga"),
            SaludoProfesional.cargosPara("M")
        )
        assertEquals(
            listOf("Doctor", "Enfermero", "TAPS", "Administrador", "Odontólogo", "Obstetra", "Psicólogo"),
            SaludoProfesional.cargosPara("H")
        )
        assertTrue(SaludoProfesional.cargosPara("").isEmpty())
    }

    @Test
    fun noHayCargosRepetidosEnLasListas() {
        assertEquals(7, SaludoProfesional.cargosPara("M").toSet().size)
        assertEquals(7, SaludoProfesional.cargosPara("H").toSet().size)
    }

    @Test
    fun construyeElSaludoConTituloPrimerNombreYPrimerApellido() {
        assertEquals(
            "Buenas tardes, Dra. Ana Pérez",
            SaludoProfesional.completo(14, "Doctora", "M", "Pérez Gómez", "Pérez Gómez Ana María")
        )
        assertEquals(
            "Buenos días, Dr. Luis Mora",
            SaludoProfesional.completo(8, "Doctor", "H", "MORA VERA", "MORA VERA LUIS ALBERTO")
        )
    }

    @Test
    fun aplicaTitulosPorCargoYSexo() {
        assertEquals("Dra.", SaludoProfesional.tratamiento("Doctora", "M"))
        assertEquals("Dr.", SaludoProfesional.tratamiento("Doctor", "H"))
        assertEquals("Dra.", SaludoProfesional.tratamiento("Odontóloga", "M"))
        assertEquals("Dr.", SaludoProfesional.tratamiento("Odontólogo", "H"))
        assertEquals("Lcda.", SaludoProfesional.tratamiento("Enfermera", "M"))
        assertEquals("Lcdo.", SaludoProfesional.tratamiento("Enfermero", "H"))
        assertEquals("Lcda.", SaludoProfesional.tratamiento("Obstetra", "M"))
        assertEquals("Lcda.", SaludoProfesional.tratamiento("Psicóloga", "M"))
        assertEquals("Sra.", SaludoProfesional.tratamiento("TAPS", "M"))
        assertEquals("Sr.", SaludoProfesional.tratamiento("Administrador", "H"))
    }

    @Test
    fun cuentasAntiguasSinApellidosUsanElFormatoDeEcuador() {
        assertEquals("Ana Pérez", SaludoProfesional.nombreCorto("", "PEREZ GOMEZ ANA MARIA").let {
            // PEREZ GOMEZ ANA MARIA -> primer nombre Ana, primer apellido Perez
            it.replace("Perez", "Pérez")
        })
    }

    @Test
    fun cambiaElCargoAntiguoAlEquivalenteDelSexo() {
        assertEquals("Doctora", SaludoProfesional.cargoEquivalente("Médico/a", "M"))
        assertEquals("Doctor", SaludoProfesional.cargoEquivalente("Médico/a", "H"))
        assertEquals("Enfermero", SaludoProfesional.cargoEquivalente("Enfermero/a", "H"))
        assertEquals("Administradora", SaludoProfesional.cargoEquivalente("Administrativo/a", "M"))
        assertEquals("Obstetra", SaludoProfesional.cargoEquivalente("Obstetra", "H"))
        assertEquals("Doctora", SaludoProfesional.cargoEquivalente("Doctor", "M"))
    }

    @Test
    fun deduceElSexoSoloSiElCargoLoIndica() {
        assertEquals("M", SaludoProfesional.inferirSexo("Doctora"))
        assertEquals("H", SaludoProfesional.inferirSexo("Psicólogo"))
        assertEquals("", SaludoProfesional.inferirSexo("Médico/a"))
        assertEquals("", SaludoProfesional.inferirSexo("TAPS"))
    }
}
