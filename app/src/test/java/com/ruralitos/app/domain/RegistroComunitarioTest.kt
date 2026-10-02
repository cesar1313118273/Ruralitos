package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class RegistroComunitarioTest {
    @Test
    fun `mapea grupos estrategias alertas y prestadores en las filas de la plantilla`() {
        val sano = miembro(
            fichaId = 1,
            fecha = "01/01/2024"
        )
        val hipertenso = miembro(
            fichaId = 1,
            fecha = "01/01/1990",
            hipertension = true,
            evento = true,
            confirmado = true,
            prestador = true
        )
        val discapacidad = miembro(
            fichaId = 2,
            fecha = "01/01/1985",
            visual = true,
            psicosocial = true
        )

        val columnas = CalculadorRegistroComunitario.calcular(
            agrupaciones = listOf(
                AgrupacionRegistroComunitario("Barrio A", setOf(1)),
                AgrupacionRegistroComunitario("Barrio B", setOf(2))
            ),
            miembros = listOf(sano, hipertenso, discapacidad),
            embarazadas = emptyList()
        )

        assertEquals(1, columnas[0].valor(4))
        assertEquals(1, columnas[0].valor(17))
        assertEquals(1, columnas[0].valor(32))
        assertEquals(1, columnas[0].valor(35))
        assertEquals(1, columnas[0].valor(36))
        assertEquals(1, columnas[0].valor(40))
        assertEquals(1, columnas[1].valor(23))
        assertEquals(1, columnas[1].valor(25))
    }

    private fun miembro(
        fichaId: Long,
        fecha: String,
        hipertension: Boolean = false,
        evento: Boolean = false,
        confirmado: Boolean = false,
        prestador: Boolean = false,
        visual: Boolean = false,
        psicosocial: Boolean = false
    ) = MiembroFamiliaEntity(
        fichaId = fichaId,
        grupoEdad = "",
        apellidosNombres = "Persona de prueba",
        parentesco = "HIJO",
        fechaNacimiento = fecha,
        ocupacion = "",
        sexo = "H",
        escolaridad = "",
        vacunasCompletas = true,
        saludBucalAdecuada = true,
        estadoNutricional = DispensarizacionAutomatica.NUTRICION_SIN_ALTERACION,
        hipertensionArterial = hipertension,
        diabetesMellitus = false,
        tuberculosis = false,
        problemaSaludMental = false,
        consumoAlcoholDrogas = false,
        enfermedadCronica = false,
        discapacidadVisual = visual,
        discapacidadAuditiva = false,
        discapacidadLenguaje = false,
        discapacidadFisica = false,
        discapacidadIntelectual = false,
        discapacidadPsicosocial = psicosocial,
        cuidadosPaliativos = false,
        vih = false,
        eventoSalud = evento,
        casoConfirmado = confirmado,
        casoSospechosoUno = false,
        casoSospechosoDos = false,
        prestadorComunitario = prestador,
        parteroAncestral = false,
        sabiduriaAncestral = false
    )
}
