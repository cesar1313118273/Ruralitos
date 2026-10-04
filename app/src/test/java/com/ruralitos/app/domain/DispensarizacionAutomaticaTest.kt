package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.data.local.entity.ValorRiesgoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class DispensarizacionAutomaticaTest {
    @Test
    fun evaluacionCompletaSinHallazgosAsignaGrupoI() {
        val resultado = DispensarizacionAutomatica.clasificar(miembroEvaluado())

        assertEquals(GrupoDispensarizacion.I, resultado.grupo)
        assertTrue(resultado.completa)
    }

    @Test
    fun factorDeRiesgoAsignaGrupoIIConPictograma() {
        val resultado = DispensarizacionAutomatica.clasificar(
            miembroEvaluado(
                fechaNacimiento = "01/01/2018",
                vacunasCompletas = false
            )
        )

        assertEquals(GrupoDispensarizacion.II, resultado.grupo)
        assertTrue(resultado.pictogramas.any { it.id == "vacunacion" })
    }

    @Test
    fun enfermedadCronicaAsignaGrupoIII() {
        val resultado = DispensarizacionAutomatica.clasificar(
            miembroEvaluado(hipertensionArterial = true)
        )

        assertEquals(GrupoDispensarizacion.III, resultado.grupo)
        assertTrue(resultado.razones.any { it.contains("Hipertensión") })
    }

    @Test
    fun discapacidadTienePrioridadYAsignaGrupoIV() {
        val resultado = DispensarizacionAutomatica.clasificar(
            miembroEvaluado(
                hipertensionArterial = true,
                discapacidadFisica = true
            )
        )

        assertEquals(GrupoDispensarizacion.IV, resultado.grupo)
        assertTrue(resultado.pictogramas.any { it.id == "disc_fisica" })
    }

    @Test
    fun ausenciaDeEvaluacionNoClasificaComoSano() {
        val miembro = MiembroFamiliaEntity(
            fichaId = 1,
            grupoEdad = "20 - 64 AÑOS",
            apellidosNombres = "Persona pendiente",
            parentesco = "HIJO/A",
            fechaNacimiento = "01/01/1990",
            ocupacion = "",
            sexo = "H",
            escolaridad = ""
        )

        val resultado = DispensarizacionAutomatica.clasificar(miembro)

        assertEquals(GrupoDispensarizacion.PENDIENTE, resultado.grupo)
        assertTrue(resultado.camposPendientes.isNotEmpty())
    }

    @Test
    fun embarazoSinFactoresRegistradosPuedePermanecerEnGrupoI() {
        val miembro = miembroEvaluado(apellidosNombres = "María Pérez")
        val embarazo = EmbarazadaEntity(
            fichaId = 1,
            apellidosNombres = "Maria Perez",
            fechaUltimaMenstruacion = "01/01/2026",
            fechaProbableParto = "08/10/2026"
        )

        val encontrado = DispensarizacionAutomatica.buscarEmbarazo(miembro, listOf(embarazo))
        val resultado = DispensarizacionAutomatica.clasificar(miembro, encontrado)

        assertEquals(GrupoDispensarizacion.I, resultado.grupo)
        assertTrue(resultado.pictogramas.any { it.id == IconosMais.RIESGO_I_EMBARAZO })
        assertTrue(resultado.pictogramas.any { it.id == IconosMais.EMBARAZO_BAJO_RIESGO })
    }

    @Test
    fun embarazoConAntecedentePasaAlGrupoII() {
        val miembro = miembroEvaluado(apellidosNombres = "María Pérez")
        val embarazo = EmbarazadaEntity(
            fichaId = 1,
            apellidosNombres = "María Pérez",
            fechaUltimaMenstruacion = "01/01/2026",
            fechaProbableParto = "08/10/2026",
            antecedentesPatologicosObstetricos = "Antecedente obstétrico"
        )
        val resultado = DispensarizacionAutomatica.clasificar(miembro, embarazo)
        assertEquals(GrupoDispensarizacion.II, resultado.grupo)
        assertTrue(resultado.pictogramas.any { it.id == IconosMais.RIESGO_II_EMBARAZO })
    }

    @Test
    fun instrumentoFamiliarGeneraIconosMaisDeSaneamientoYConsumo() {
        val valores = listOf(
            ValorRiesgoEntity(calificacionId = 9, componente = 7, valor = 4),
            ValorRiesgoEntity(calificacionId = 9, componente = 8, valor = 4),
            ValorRiesgoEntity(calificacionId = 9, componente = 16, valor = 2)
        )

        val pictogramas = DispensarizacionAutomatica.pictogramasRiesgoFamiliar(valores)

        assertEquals(1, pictogramas.count { it.id == "saneamiento" })
        assertTrue(pictogramas.any { it.id == "consumo" })
    }

    @Test
    fun hipertensionYDiabetesUsanUnSoloPictogramaCombinadoMais() {
        val resultado = DispensarizacionAutomatica.clasificar(
            miembroEvaluado(
                hipertensionArterial = true,
                diabetesMellitus = true
            )
        )

        assertTrue(resultado.pictogramas.any { it.id == IconosMais.RIESGO_III_ADULTO })
        assertTrue(resultado.pictogramas.any { it.id == IconosMais.HIPERTENSION_DIABETES })
    }

    @Test
    fun obesidadSeleccionaElPictogramaMaisPorEdad() {
        val resultado = DispensarizacionAutomatica.clasificar(
            miembroEvaluado(
                fechaNacimiento = "01/01/2018",
                estadoNutricional = DispensarizacionAutomatica.NUTRICION_OBESIDAD
            )
        )

        assertTrue(resultado.pictogramas.any { it.id == IconosMais.OBESIDAD_CINCO_ONCE })
    }

    @Test
    fun riesgoFamiliarDeEmbarazoSeleccionaNivelMais() {
        val valores = listOf(
            ValorRiesgoEntity(calificacionId = 10, componente = 4, valor = 3)
        )

        val pictogramas = DispensarizacionAutomatica.pictogramasRiesgoFamiliar(valores)

        assertEquals(listOf(IconosMais.EMBARAZO_RIESGO), pictogramas.map { it.id })
    }

    @Test
    fun nuncaGeneraPictogramasFueraDelCatalogo() {
        val resultado = DispensarizacionAutomatica.clasificar(
            miembroEvaluado(
                fechaNacimiento = "01/01/2018",
                vacunasCompletas = false,
                hipertensionArterial = true,
                diabetesMellitus = true,
                discapacidadFisica = true,
                estadoNutricional = DispensarizacionAutomatica.NUTRICION_OBESIDAD
            )
        )

        assertTrue(resultado.pictogramas.all { it.id in IconosMais.todos })
    }

    @Test
    fun edadesLimiteUsanTramosIgualesAlRegistroGeneral() {
        val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).parse("26/09/2026")!!
        assertEquals(
            GrupoEdadRiesgo.MENOR_DOS,
            DispensarizacionAutomatica.categoriaGrupoEdad(
                miembroEvaluado(fechaNacimiento = "01/10/2024"), hoy = hoy
            )
        )
        assertEquals(
            GrupoEdadRiesgo.DOS_NUEVE,
            DispensarizacionAutomatica.categoriaGrupoEdad(
                miembroEvaluado(fechaNacimiento = "26/09/2024"), hoy = hoy
            )
        )
        assertEquals(
            GrupoEdadRiesgo.ADULTO,
            DispensarizacionAutomatica.categoriaGrupoEdad(
                miembroEvaluado(fechaNacimiento = "26/09/2006"), hoy = hoy
            )
        )
    }

    @Test
    fun embarazoAdolescenteCronicoTieneIconoEspecifico() {
        val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).parse("26/09/2026")!!
        val miembro = miembroEvaluado(fechaNacimiento = "26/09/2010")
        val embarazo = EmbarazadaEntity(
            fichaId = 1,
            apellidosNombres = miembro.apellidosNombres,
            fechaUltimaMenstruacion = "01/01/2026",
            fechaProbableParto = "08/10/2026"
        )
        val categoria = DispensarizacionAutomatica.categoriaGrupoEdad(miembro, embarazo, hoy)
        assertEquals(GrupoEdadRiesgo.EMBARAZADA_ADOLESCENTE, categoria)
        assertEquals(
            IconosMais.RIESGO_III_EMBARAZO_ADOLESCENTE,
            DispensarizacionAutomatica.iconoGrupoEdad(GrupoDispensarizacion.III, categoria)?.id
        )
    }

    @Test
    fun apoyoFisicoYPaliativosUsanIconosNuevos() {
        val resultado = DispensarizacionAutomatica.clasificar(
            miembroEvaluado(discapacidadFisica = true).copy(
                necesitaAyudaTecnica = true,
                cuidadosPaliativos = true,
                vih = true
            )
        )
        assertTrue(resultado.pictogramas.any { it.id == IconosMais.DISCAPACIDAD_FISICA_APOYO })
        assertTrue(resultado.pictogramas.any { it.id == IconosMais.CUIDADOS_PALIATIVOS })
        assertTrue(resultado.pictogramas.any { it.id == IconosMais.VIH })
    }

    private fun miembroEvaluado(
        apellidosNombres: String = "Ana Ejemplo",
        fechaNacimiento: String = "01/01/1990",
        vacunasCompletas: Boolean = true,
        hipertensionArterial: Boolean = false,
        diabetesMellitus: Boolean = false,
        discapacidadFisica: Boolean = false,
        estadoNutricional: String = DispensarizacionAutomatica.NUTRICION_SIN_ALTERACION
    ) = MiembroFamiliaEntity(
        fichaId = 1,
        grupoEdad = "20 - 64 AÑOS",
        apellidosNombres = apellidosNombres,
        parentesco = "JEFE/A DE FAMILIA",
        fechaNacimiento = fechaNacimiento,
        ocupacion = "",
        sexo = "M",
        escolaridad = "",
        vacunasCompletas = vacunasCompletas,
        saludBucalAdecuada = true,
        estadoNutricional = estadoNutricional,
        hipertensionArterial = hipertensionArterial,
        diabetesMellitus = diabetesMellitus,
        tuberculosis = false,
        problemaSaludMental = false,
        consumoAlcoholDrogas = false,
        enfermedadCronica = false,
        discapacidadVisual = false,
        discapacidadAuditiva = false,
        discapacidadLenguaje = false,
        discapacidadFisica = discapacidadFisica,
        discapacidadIntelectual = false,
        discapacidadPsicosocial = false
    )
}
