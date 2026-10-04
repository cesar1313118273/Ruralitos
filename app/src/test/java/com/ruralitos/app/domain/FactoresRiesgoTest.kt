package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class FactoresRiesgoTest {
    private val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).parse("15/06/2026")!!

    private fun persona(
        nacimiento: String = "10/10/1990",
        factores: Set<String> = emptySet(),
        escolaridad: String = "BAS",
        consumo: Boolean? = false,
        nutricion: String = DispensarizacionAutomatica.NUTRICION_SIN_ALTERACION,
        diagnosticosJson: String = "[]",
        hipertension: Boolean = false
    ) = MiembroFamiliaEntity(
        fichaId = 1, grupoEdad = "", apellidosNombres = "Ana Ejemplo", parentesco = "HIJO/A",
        fechaNacimiento = nacimiento, ocupacion = "", sexo = "M", escolaridad = escolaridad,
        vacunasCompletas = true, saludBucalAdecuada = true, estadoNutricional = nutricion,
        hipertensionArterial = hipertension, diabetesMellitus = false, tuberculosis = false, problemaSaludMental = false,
        consumoAlcoholDrogas = consumo, enfermedadCronica = false, discapacidadVisual = false, discapacidadAuditiva = false,
        discapacidadLenguaje = false, discapacidadFisica = false, discapacidadIntelectual = false, discapacidadPsicosocial = false,
        comorbilidadesCie10Json = diagnosticosJson, factoresRiesgoEdadJson = FactoresRiesgoEdad.codificar(factores)
    )

    private fun embarazo(factores: Set<String> = emptySet(), gestas: Int? = null, abortos: Int? = null) = EmbarazadaEntity(
        fichaId = 1, apellidosNombres = "Ana Ejemplo", fechaUltimaMenstruacion = "01/03/2026", fechaProbableParto = "08/12/2026",
        gestas = gestas, abortos = abortos, factoresObstetricosJson = FactoresObstetricos.codificar(factores)
    )

    // ---------- factores por edad ----------

    @Test
    fun cadaEdadMuestraSuPropiaLista() {
        val adulto = FactoresRiesgoEdad.disponibles(GrupoEdadRiesgo.ADULTO).map { it.codigo }
        val mayor = FactoresRiesgoEdad.disponibles(GrupoEdadRiesgo.ADULTO_MAYOR).map { it.codigo }
        assertTrue("ANTECEDENTES_FAMILIARES" in adulto)
        assertFalse("RIESGO_CAIDA" in adulto)
        assertTrue("RIESGO_CAIDA" in mayor)
        assertTrue(FactoresRiesgoEdad.disponibles(GrupoEdadRiesgo.MENOR_DOS).any { it.codigo == "PESO_BAJO" })
        // la violencia está en todas las edades; el consumo, desde los 10 años
        GrupoEdadRiesgo.entries.filter { it != GrupoEdadRiesgo.EMBARAZADA && it != GrupoEdadRiesgo.EMBARAZADA_ADOLESCENTE }
            .forEach { assertTrue(it.name, FactoresRiesgoEdad.disponibles(it).any { f -> f.codigo == FactoresRiesgoEdad.VIOLENCIA }) }
        assertFalse(FactoresRiesgoEdad.disponibles(GrupoEdadRiesgo.DOS_NUEVE).any { it.codigo == FactoresRiesgoEdad.CONSUMO_ALCOHOL })
        assertTrue(FactoresRiesgoEdad.disponibles(GrupoEdadRiesgo.ADOLESCENTE).any { it.codigo == FactoresRiesgoEdad.CONSUMO_ALCOHOL })
    }

    @Test
    fun unFactorMarcadoPasaAlGrupoIIConSuMotivo() {
        val resultado = DispensarizacionAutomatica.clasificar(persona(factores = setOf("SEDENTARISMO")))
        assertEquals(GrupoDispensarizacion.II, resultado.grupo)
        assertTrue(resultado.razones.any { it.contains("sedentarismo") })
    }

    @Test
    fun sinFactoresMarcadosSigueEnGrupoI() {
        assertEquals(GrupoDispensarizacion.I, DispensarizacionAutomatica.clasificar(persona()).grupo)
    }

    @Test
    fun unFactorDeOtraEdadYaNoCuenta() {
        // marcó «riesgo de caída» cuando era mayor; hoy es adulto de 35 años
        val resultado = DispensarizacionAutomatica.clasificar(persona(nacimiento = "10/10/1990", factores = setOf("RIESGO_CAIDA")))
        assertEquals(GrupoDispensarizacion.I, resultado.grupo)
    }

    @Test
    fun consumoYViolenciaSeLeenDeLaLista() {
        val m = persona(factores = setOf(FactoresRiesgoEdad.CONSUMO_ALCOHOL, FactoresRiesgoEdad.VIOLENCIA))
        assertTrue(FactoresRiesgoEdad.hayConsumo(m))
        assertTrue(FactoresRiesgoEdad.hayViolencia(m))
        assertFalse(FactoresRiesgoEdad.hayConsumo(persona(nacimiento = "10/10/2018", factores = setOf(FactoresRiesgoEdad.CONSUMO_ALCOHOL))))
    }

    @Test
    fun laListaSeGuardaYSeLeeSinPerderNada() {
        val texto = FactoresRiesgoEdad.codificar(setOf("B", "A", "A"))
        assertEquals("[\"A\",\"B\"]", texto)
        assertEquals(setOf("A", "B"), FactoresRiesgoEdad.decodificar(texto))
        assertEquals(emptySet<String>(), FactoresRiesgoEdad.decodificar("[]"))
    }

    // ---------- diagnósticos CIE-10 ----------

    @Test
    fun losDiagnosticosCalculanLasEstrategiasNacionales() {
        assertTrue(EstrategiasDesdeCie10.hipertension(listOf("I10X")))
        assertTrue(EstrategiasDesdeCie10.diabetes(listOf("E11.9")))
        assertTrue(EstrategiasDesdeCie10.tuberculosis(listOf("A15.0")))
        assertTrue(EstrategiasDesdeCie10.vih(listOf("B20.1")))
        assertTrue(EstrategiasDesdeCie10.vih(listOf("Z21")))
        assertTrue(EstrategiasDesdeCie10.saludMental(listOf("F32.9")))
        assertTrue(EstrategiasDesdeCie10.cuidadosPaliativos(listOf("Z51.5")))
        assertFalse(EstrategiasDesdeCie10.hipertension(listOf("E11.9")))
        assertFalse(EstrategiasDesdeCie10.saludMental(listOf("Z51.5")))
    }

    @Test
    fun lasAlertasEpidemiologicasSoloSePidenConTuberculosisOVih() {
        assertTrue(EstrategiasDesdeCie10.pideAlertasEpidemiologicas(listOf("A15.0")))
        assertTrue(EstrategiasDesdeCie10.pideAlertasEpidemiologicas(listOf("B24X")))
        assertFalse(EstrategiasDesdeCie10.pideAlertasEpidemiologicas(listOf("I10X", "E11.9")))
        assertFalse(EstrategiasDesdeCie10.pideAlertasEpidemiologicas(emptyList()))
    }

    @Test
    fun seLeenLosCodigosDelTextoGuardado() {
        val json = "[{\"codigo\":\"I10.X\",\"descripcion\":\"HTA\",\"descompensada\":false},{\"codigo\":\"E11.9\",\"descripcion\":\"DM\"}]"
        assertEquals(listOf("I10.X", "E11.9"), EstrategiasDesdeCie10.codigos(json))
        assertEquals(emptyList<String>(), EstrategiasDesdeCie10.codigos("[]"))
    }

    // ---------- embarazadas ----------

    private fun evaluar(
        e: EmbarazadaEntity, m: MiembroFamiliaEntity = persona(nacimiento = "10/10/1996"), diag: List<String> = emptyList()
    ) = FactoresObstetricos.evaluar(FactoresObstetricos.decodificar(e.factoresObstetricosJson), m, e.gestas, e.abortos, diag, hoy)

    @Test
    fun embarazadaSinCriteriosEstaEnGrupoIConNivelCero() {
        val m = persona(nacimiento = "10/10/1996")
        val e = embarazo()
        val resultado = DispensarizacionAutomatica.clasificar(m, e)
        assertEquals(GrupoDispensarizacion.I, resultado.grupo)
        val ev = evaluar(e, m)
        assertEquals(0, ev.nivel)
        assertEquals("SIN_RIESGO", ev.riesgoObstetrico)
    }

    @Test
    fun unCriterioDeRiesgo1LlevaAlGrupoII() {
        val m = persona(nacimiento = "10/10/1996")
        val e = embarazo(factores = setOf("CONTROL_INSUFICIENTE"))
        assertEquals(GrupoDispensarizacion.II, DispensarizacionAutomatica.clasificar(m, e).grupo)
        val ev = evaluar(e, m)
        assertEquals(1, ev.nivel)
        assertEquals("BAJO", ev.riesgoObstetrico)
    }

    @Test
    fun unCriterioNoCronicoDeRiesgo2SigueSiendoGrupoII() {
        val m = persona(nacimiento = "10/10/1996")
        val e = embarazo(factores = setOf("ANEMIA", "POLIHIDRAMNIOS"))
        val resultado = DispensarizacionAutomatica.clasificar(m, e)
        assertEquals("no es una patología crónica", GrupoDispensarizacion.II, resultado.grupo)
        assertEquals(2, evaluar(e, m).nivel)
        assertEquals("ALTO", evaluar(e, m).riesgoObstetrico)
    }

    @Test
    fun unaPatologiaCronicaMarcadaLlevaAlGrupoIII() {
        val m = persona(nacimiento = "10/10/1996")
        val resultado = DispensarizacionAutomatica.clasificar(m, embarazo(factores = setOf("EPILEPSIA")))
        assertEquals(GrupoDispensarizacion.III, resultado.grupo)
        assertTrue(resultado.razones.any { it.contains("epilepsia") })
    }

    @Test
    fun riesgoInminenteNoCronicoEsGrupoIIPeroConAvisoDeUrgencia() {
        val m = persona(nacimiento = "10/10/1996")
        val e = embarazo(factores = setOf("HEMORRAGIA_VAGINAL"))
        assertEquals(GrupoDispensarizacion.II, DispensarizacionAutomatica.clasificar(m, e).grupo)
        val ev = evaluar(e, m)
        assertEquals(3, ev.nivel)
        assertTrue(ev.inminente)
        assertEquals("MUY_ALTO", ev.riesgoObstetrico)
    }

    @Test
    fun unaCardiopatiaEsCronicaAunqueSeaDeRiesgo3() {
        val m = persona(nacimiento = "10/10/1996")
        assertEquals(GrupoDispensarizacion.III, DispensarizacionAutomatica.clasificar(m, embarazo(factores = setOf("CARDIOPATIA"))).grupo)
    }

    @Test
    fun laEdadLasGestasYLosAbortosSeDetectanSolos() {
        val adolescente = persona(nacimiento = "10/10/2009") // 16 años
        var ev = evaluar(embarazo(), adolescente)
        assertTrue(ev.razones.any { it.codigo == "EDAD_EXTREMA" && it.automatica })
        assertEquals(1, ev.nivel)

        val mayorPrimeriza = persona(nacimiento = "10/10/1988") // 37 años
        ev = evaluar(embarazo(gestas = 1), mayorPrimeriza)
        assertTrue(ev.razones.any { it.codigo == "EDAD_EXTREMA" })
        ev = evaluar(embarazo(gestas = 3), mayorPrimeriza)
        assertFalse("con 3 gestas ya no es el primer embarazo", ev.razones.any { it.codigo == "EDAD_EXTREMA" })
        ev = evaluar(embarazo(gestas = null), mayorPrimeriza)
        assertFalse("sin el dato de gestas no se supone nada", ev.razones.any { it.codigo == "EDAD_EXTREMA" })

        ev = evaluar(embarazo(gestas = 4))
        assertTrue(ev.razones.any { it.codigo == "GRAN_MULTIPARIDAD" })
        ev = evaluar(embarazo(gestas = 3))
        assertFalse(ev.razones.any { it.codigo == "GRAN_MULTIPARIDAD" })

        ev = evaluar(embarazo(abortos = 1))
        assertTrue(ev.razones.any { it.codigo == "ABORTOS_ESPONTANEOS" })
        assertEquals(2, ev.nivel)
    }

    @Test
    fun losDiagnosticosDelEmbarazoSeConviertenEnCriterios() {
        val ev = evaluar(embarazo(), diag = listOf("O60.0", "O30.0", "D50.9"))
        val codigos = ev.razones.map { it.codigo }
        assertTrue("AMENAZA_PARTO_PRETERMINO" in codigos)
        assertTrue("EMBARAZO_MULTIPLE" in codigos)
        assertTrue("ANEMIA" in codigos)
        assertEquals(3, ev.nivel)
    }

    @Test
    fun loDeLaFichaDeLaPersonaTambienCuenta() {
        val m = persona(nacimiento = "10/10/1996", escolaridad = "SIN", consumo = true, nutricion = DispensarizacionAutomatica.NUTRICION_OBESIDAD)
        val ev = evaluar(embarazo(), m)
        val codigos = ev.razones.map { it.codigo }
        assertTrue("ANALFABETISMO" in codigos)
        assertTrue("CONSUMO_ALCOHOL" in codigos)
        assertTrue("OBESIDAD" in codigos)
        // una embarazada con hipertensión registrada es Grupo III por la definición del grupo
        val hta = persona(nacimiento = "10/10/1996", hipertension = true)
        assertEquals(GrupoDispensarizacion.III, DispensarizacionAutomatica.clasificar(hta, embarazo()).grupo)
    }

    @Test
    fun laEscalaTieneTodosLosCriteriosDelDocumento() {
        assertEquals(14, FactoresObstetricos.riesgo1.size)
        assertEquals(29, FactoresObstetricos.riesgo2.size)
        assertEquals(9, FactoresObstetricos.riesgo3.size)
        assertEquals(FactoresObstetricos.todos.size, FactoresObstetricos.todos.map { it.codigo }.toSet().size)
        assertTrue(FactoresObstetricos.todos.none { it.codigo.any { c -> c.isLowerCase() } })
    }

    // ---------- del grupo más alto al más bajo ----------

    @Test
    fun conHipertensionLosFactoresNoMuevenAlGrupoII() {
        // 35 años con hipertensión (diagnóstico I10) y un factor de riesgo elegido: sigue en Grupo III
        val m = persona(factores = setOf("SEDENTARISMO"), hipertension = true, diagnosticosJson = "[{\"codigo\":\"I10.X\",\"descripcion\":\"HTA\"}]")
        val r = DispensarizacionAutomatica.clasificar(m)
        assertEquals(GrupoDispensarizacion.III, r.grupo)
        assertFalse("no se mezclan los motivos del Grupo II", r.razones.any { it.contains("sedentarismo") })
    }

    @Test
    fun elConsumoProblematicoYElRiesgoSuicidaSonGrupoIIINoGrupoII() {
        assertEquals(GrupoDispensarizacion.III, DispensarizacionAutomatica.clasificar(persona(factores = setOf("CONSUMO_ALCOHOL"))).grupo)
        assertEquals(GrupoDispensarizacion.III, DispensarizacionAutomatica.clasificar(persona(factores = setOf("CONSUMO_DROGAS"))).grupo)
        assertEquals(GrupoDispensarizacion.III, DispensarizacionAutomatica.clasificar(persona(factores = setOf("RIESGO_SUICIDA"))).grupo)
        assertEquals(GrupoDispensarizacion.III, DispensarizacionAutomatica.clasificar(persona(factores = setOf("INTENTO_AUTOLITICO"))).grupo)
        // el tabaquismo sigue siendo un factor del Grupo II
        assertEquals(GrupoDispensarizacion.II, DispensarizacionAutomatica.clasificar(persona(factores = setOf("TABAQUISMO"))).grupo)
    }

    @Test
    fun losCodigosDeLaPrimeraVersionSiguenValiendoComoGrupoIII() {
        assertEquals(GrupoDispensarizacion.III, DispensarizacionAutomatica.clasificar(persona(factores = setOf("CONSUMO"))).grupo)
        assertEquals(GrupoDispensarizacion.III, DispensarizacionAutomatica.clasificar(persona(factores = setOf("INTENTO_SUICIDA"))).grupo)
    }

    @Test
    fun elConsumoYElRiesgoSuicidaNoSalenEnNinoPequenos() {
        val nino = persona(nacimiento = "10/10/2018", factores = setOf("CONSUMO_ALCOHOL", "RIESGO_SUICIDA"))
        assertEquals(GrupoDispensarizacion.I, DispensarizacionAutomatica.clasificar(nino).grupo)
    }

    @Test
    fun soloLosDiagnosticosDelGrupoIIIcuentan() {
        listOf("I10.X", "E11.9", "A15.0", "B20.1", "F32.9", "F10.2", "G40.9", "C50.9", "J45.9", "N18.9", "X70", "Z91.5", "M05.9", "Q90.9")
            .forEach { assertTrue(it, EstrategiasDesdeCie10.esGrupoIII(it)) }
        listOf("J00", "A09", "S72.0", "N39.0", "K35.8", "R05", "H66.9", "O60.0", "Z00.0", "L02.9", "B34.9")
            .forEach { assertFalse(it, EstrategiasDesdeCie10.esGrupoIII(it)) }
    }

    @Test
    fun unDiagnosticoAgudoNoLlevaAlGrupoIII() {
        val gripe = persona(diagnosticosJson = "[{\"codigo\":\"J00\",\"descripcion\":\"RESFRIADO\"}]")
        assertEquals(GrupoDispensarizacion.I, DispensarizacionAutomatica.clasificar(gripe).grupo)
        val epilepsia = persona(diagnosticosJson = "[{\"codigo\":\"G40.9\",\"descripcion\":\"EPILEPSIA\"}]")
        assertEquals(GrupoDispensarizacion.III, DispensarizacionAutomatica.clasificar(epilepsia).grupo)
    }

    @Test
    fun laDiscapacidadSigueMandandoAlGrupoIV() {
        val m = persona(factores = setOf("CONSUMO_ALCOHOL"), hipertension = true).copy(discapacidadFisica = true)
        assertEquals(GrupoDispensarizacion.IV, DispensarizacionAutomatica.clasificar(m).grupo)
    }

    // ---------- escribir y elegir ----------

    @Test
    fun alEscribirSeSugierenLasOpcionesSinImportarTildesNiMayusculas() {
        val adulto = GrupoEdadRiesgo.ADULTO
        assertEquals(listOf("SEDENTARISMO"), FactoresRiesgoEdad.sugerencias(adulto, "sedent", emptySet()).map { it.codigo })
        assertTrue(FactoresRiesgoEdad.sugerencias(adulto, "CONSUMO", emptySet()).map { it.codigo }.containsAll(listOf("CONSUMO_ALCOHOL", "CONSUMO_DROGAS")))
        assertEquals(listOf("RIESGO_CAIDA"), FactoresRiesgoEdad.sugerencias(GrupoEdadRiesgo.ADULTO_MAYOR, "caida", emptySet()).map { it.codigo })
        assertTrue("lo ya elegido no se vuelve a ofrecer", FactoresRiesgoEdad.sugerencias(adulto, "sedent", setOf("SEDENTARISMO")).isEmpty())
        assertTrue(FactoresRiesgoEdad.sugerencias(adulto, "", emptySet()).isEmpty())
        assertTrue(FactoresRiesgoEdad.sugerencias(adulto, "zzzz", emptySet()).isEmpty())
        assertTrue(FactoresObstetricos.sugerencias("anemia", emptySet()).any { it.codigo == "ANEMIA" })
    }

    @Test
    fun enLaEmbarazadaElConsumoProblematicoEsGrupoIII() {
        val m = persona(nacimiento = "10/10/1996")
        val e = embarazo(factores = setOf("CONSUMO_DROGAS"))
        assertEquals(GrupoDispensarizacion.III, DispensarizacionAutomatica.clasificar(m, e).grupo)
        // y si la persona ya lo tiene marcado en su lista, se detecta sola
        val conLista = persona(nacimiento = "10/10/1996", factores = setOf("CONSUMO_ALCOHOL"))
        assertTrue(evaluar(embarazo(), conLista).razones.any { it.codigo == "CONSUMO_ALCOHOL" && it.automatica })
    }

    // ---------- rol familiar ----------

    @Test
    fun cadaRolEsUnaOpcionSeparada() {
        val roles = RolFamiliar.opciones.map { it.first }
        listOf("JEFE DE FAMILIA", "JEFA DE FAMILIA", "HIJO", "HIJA", "PADRE", "MADRE").forEach { assertTrue(it, it in roles) }
        assertFalse(roles.any { it.contains("/A") && !it.startsWith("CÓNYUGE") })
    }

    @Test
    fun losRolesAntiguosSePasanALasOpcionesNuevasSegunElSexo() {
        assertEquals("JEFE DE FAMILIA", RolFamiliar.normalizar("JEFE/A DE FAMILIA", "H"))
        assertEquals("JEFA DE FAMILIA", RolFamiliar.normalizar("JEFE/A DE FAMILIA", "M"))
        assertEquals("HIJA", RolFamiliar.normalizar("HIJO/A", "M"))
        assertEquals("PADRE", RolFamiliar.normalizar("PADRE/MADRE", "H"))
        assertEquals("HIJO", RolFamiliar.normalizar("HIJO", "M"))
        assertEquals("", RolFamiliar.normalizar("", "H"))
        assertTrue(RolFamiliar.esJefe("JEFA DE FAMILIA") && RolFamiliar.esJefe("JEFE DE FAMILIA") && RolFamiliar.esJefe("JEFE/A DE FAMILIA"))
        assertFalse(RolFamiliar.esJefe("HIJA"))
        assertEquals("M", RolFamiliar.sexoDelRol("JEFA DE FAMILIA"))
        assertEquals("H", RolFamiliar.sexoDelRol("PADRE"))
        assertEquals(null, RolFamiliar.sexoDelRol("OTRO FAMILIAR"))
    }
}
