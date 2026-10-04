package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.ui.screens.MiembrosFamiliaScreen
import com.ruralitos.app.ui.screens.ContaminacionAmbientalScreen
import com.ruralitos.app.ui.screens.GestionRiesgoScreen
import com.ruralitos.app.ui.screens.LugaresTratamientoScreen
import com.ruralitos.app.ui.screens.RiesgoFamiliarScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Formulario de la persona (factores que se escriben, grupo real, rol familiar), botones flotantes y embarazada. */
@RunWith(AndroidJUnit4::class)
class FactoresRiesgoUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }
    private lateinit var ids: Map<String, Long>

    @Before
    fun sembrar() = runBlocking {
        runCatching {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission("com.ruralitos.app", "android.permission.POST_NOTIFICATIONS")
        }
        ids = DatosMapaParlante.sembrar(database)
        Unit
    }

    @After
    fun limpiar() = DatosMapaParlante.limpiar(database)

    private fun existe(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    private fun hayTexto(texto: String) = rule.onAllNodes(hasText(texto, substring = true)).fetchSemanticsNodes().isNotEmpty()
    private fun esperar(condicion: () -> Boolean) = rule.waitUntil(20_000, condicion)

    private fun botonEditar(n: Int) = rule.onAllNodes(hasText("Editar"))[n]
    private fun hayEditar() = rule.onAllNodes(hasText("Editar")).fetchSemanticsNodes().isNotEmpty()

    private fun miembros(fichaId: Long) = runBlocking { database.sincronizacionDao().miembros(fichaId) }

    private fun mostrarMiembros(fichaId: Long) {
        rule.setContent {
            RuralitosTheme { MiembrosFamiliaScreen(fichaId = fichaId, usuarioId = 7L, onContinuar = {}, onSalir = {}) }
        }
        esperar { existe("boton_agregar_integrante") && hayEditar() }
    }

    private fun abrirSeccion(titulo: String) {
        rule.onNodeWithText(titulo).performScrollTo().performClick()
        rule.waitForIdle()
    }

    /** Escribe en el cuadro de factores y elige la sugerencia. */
    private fun elegir(prefijo: String, escrito: String, codigo: String) {
        rule.onNodeWithTag("${prefijo}_buscar").performScrollTo().performTextInput(escrito)
        esperar { existe("${prefijo}_sugerencia_$codigo") }
        rule.onNodeWithTag("${prefijo}_sugerencia_$codigo").performScrollTo().performClick()
        esperar { existe("${prefijo}_$codigo") }
    }

    // ---------- formulario de la persona ----------

    @Test
    fun elFormularioQuitaEstrategiasYActoresYMuestraElRolFamiliar() {
        mostrarMiembros(ids.getValue("c1"))
        botonEditar(2).performScrollTo().performClick() // RAMÓN NIÑO, 14 meses
        esperar { hayTexto("4. Factores de riesgo según la edad") }

        assertFalse("Estrategias Nacionales ya no se llena a mano", hayTexto("Estrategias Nacionales"))
        assertFalse("Actores comunitarios pasó a los botones flotantes", hayTexto("7. Actores comunitarios"))
        assertFalse("Alertas epidemiológicas está oculta sin tuberculosis ni VIH", hayTexto("Alertas Epidemiológicas"))
        assertTrue(hayTexto("7. Otros riesgos prioritarios"))
        assertTrue("el campo se llama Rol familiar", hayTexto("Rol familiar"))
        assertFalse(hayTexto("Parentesco con el jefe de familia"))
        // el rol es un menú desplegable con una opción por cada rol
        rule.onAllNodes(hasText("Jefe de familia"))[0].performScrollTo().performClick()
        esperar { hayTexto("Jefa de familia") }
        assertTrue("cada rol es una opción", hayTexto("Hija") && hayTexto("Madre") && hayTexto("Padre") && hayTexto("Abuela"))
        rule.onAllNodes(hasText("Hijo"))[0].performClick()

        abrirSeccion("4. Factores de riesgo según la edad")
        esperar { existe("factor_buscar") }
        rule.onNodeWithTag("banda_factores").assertTextEquals("0 a 23 meses")
        elegir("factor", "peso al", "PESO_BAJO")
        esperar { hayTexto("Grupo II") }

        rule.onNodeWithText("Guardar cambios del integrante").performClick()
        esperar { miembros(ids.getValue("c1")).first { it.apellidosNombres == "RAMÓN NIÑO" }.factoresRiesgoEdadJson.contains("PESO_BAJO") }
    }

    @Test
    fun conHipertensionElGrupoSigueSiendoIIIAunqueSeEscojanFactores() {
        mostrarMiembros(ids.getValue("c1"))
        botonEditar(0).performScrollTo().performClick() // RAMÓN LUIS, 66 años, con hipertensión
        esperar { hayTexto("4. Factores de riesgo según la edad") }
        abrirSeccion("4. Factores de riesgo según la edad")
        esperar { existe("factor_buscar") }
        rule.onNodeWithTag("banda_factores").assertTextEquals("65 años o más")
        elegir("factor", "caida", "RIESGO_CAIDA")
        rule.onNodeWithTag("resumen_factores").performScrollTo()
        assertTrue("el cuadro dice el grupo real", hayTexto("Grupo III"))
        assertFalse("y no el Grupo II", hayTexto("Grupo II ·"))
        assertTrue(hayTexto("ya cumple uno superior"))
    }

    @Test
    fun elConsumoProblematicoSeEscribeYLlevaAlGrupoIII() {
        mostrarMiembros(ids.getValue("c3"))
        botonEditar(0).performScrollTo().performClick() // PÉREZ JUAN, 40 años
        esperar { hayTexto("4. Factores de riesgo según la edad") }
        abrirSeccion("4. Factores de riesgo según la edad")
        esperar { existe("factor_buscar") }
        rule.onNodeWithTag("factor_buscar").performScrollTo().performTextInput("alcohol")
        esperar { existe("factor_sugerencia_CONSUMO_ALCOHOL") }
        assertTrue("la opción dice a qué grupo pertenece", hayTexto("Grupo III"))
        rule.onNodeWithTag("factor_sugerencia_CONSUMO_ALCOHOL").performScrollTo().performClick()
        rule.onNodeWithText("Guardar cambios del integrante").performClick()
        esperar { miembros(ids.getValue("c3")).any { it.consumoAlcoholDrogas == true } }
        assertTrue(miembros(ids.getValue("c3")).first { it.consumoAlcoholDrogas == true }.factoresRiesgoEdadJson.contains("CONSUMO_ALCOHOL"))
    }

    @Test
    fun alElegirUnDiagnosticoDeTuberculosisApareceLaSeccionDeAlertas() {
        mostrarMiembros(ids.getValue("c1"))
        botonEditar(0).performScrollTo().performClick()
        esperar { hayTexto("3. Seguimiento preventivo") }
        assertFalse(hayTexto("6. Alertas Epidemiológicas"))
        abrirSeccion("3. Seguimiento preventivo")
        val campo = rule.onNodeWithText("Otros riesgos, enfermedad o discapacidad · CIE-10")
        campo.performScrollTo().performTextInput("A15")
        esperar { hayTexto("+ A15") }
        rule.onAllNodes(hasText("+ A15", substring = true))[0].performClick()
        esperar { hayTexto("6. Alertas Epidemiológicas") }
    }

    // ---------- botones flotantes ----------

    @Test
    fun laListaTieneTresBotonesFlotantesRedondosYNoElCuadroContadorNiElBotonGrande() {
        mostrarMiembros(ids.getValue("c1"))
        assertTrue(existe("boton_agregar_integrante"))
        assertTrue(existe("boton_actores_comunitarios"))
        assertTrue("el botón rojo de mortalidad", existe("boton_mortalidad"))
        assertFalse("el cuadro contador se quitó", hayTexto("integrante(s) registrado(s)"))
        assertTrue("el botón grande se quitó", rule.onAllNodes(hasText("Agregar un integrante familiar")).fetchSemanticsNodes().isEmpty())
        assertTrue("el botón verde no lleva texto", rule.onAllNodes(hasText("Actores comunitarios")).fetchSemanticsNodes().isEmpty())
        rule.onNodeWithTag("boton_agregar_integrante").performClick()
        esperar { hayTexto("Agregar integrante") }
    }

    @Test
    fun elBotonVerdeAsignaUnActorComunitarioYMuestraSuIcono() {
        mostrarMiembros(ids.getValue("c1"))
        val luis = miembros(ids.getValue("c1")).first { it.apellidosNombres == "RAMÓN LUIS" }
        assertFalse(existe("iconos_actores_${luis.id}"))
        rule.onNodeWithTag("boton_actores_comunitarios").performClick()
        esperar { existe("lista_actores") }
        rule.onNodeWithTag("actor_persona_${luis.id}").performClick()
        esperar { existe("actor_partero") }
        rule.onNodeWithTag("actor_partero").performClick()
        rule.onNodeWithTag("actor_sabiduria").performClick()
        rule.onNodeWithTag("guardar_actores").performClick()
        esperar { miembros(ids.getValue("c1")).first { it.id == luis.id }.parteroAncestral == true }
        val guardado = miembros(ids.getValue("c1")).first { it.id == luis.id }
        assertEquals(true, guardado.sabiduriaAncestral)
        assertEquals(false, guardado.prestadorComunitario)
        esperar { existe("iconos_actores_${luis.id}") }
    }

    // ---------- mortalidad dentro de integrantes ----------

    @Test
    fun elBotonRojoAbreLaVentanaDeMortalidadYGuardaLosCuatroDatos() {
        mostrarMiembros(ids.getValue("c1"))
        rule.onNodeWithTag("boton_mortalidad").performClick()
        esperar { existe("ventana_mortalidad") }
        rule.onNodeWithTag("guardar_fallecimiento").performClick()
        esperar { hayTexto("Completa apellidos y nombres, parentesco, edad al fallecer y causa.") }
        rule.onNodeWithTag("mortalidad_nombre").performTextInput("RAMÓN PEDRO")
        rule.onNodeWithTag("mortalidad_parentesco").performTextInput("Abuelo")
        rule.onNodeWithTag("mortalidad_edad").performTextInput("81")
        rule.onNodeWithTag("mortalidad_causa").performTextInput("Infarto")
        rule.onNodeWithTag("guardar_fallecimiento").performClick()
        esperar {
            runBlocking { database.fichaContenidoDao().listarMortalidad(ids.getValue("c1")).first() }
                .any { it.nombre == "RAMÓN PEDRO" && it.edadAlFallecer == 81 && it.causa == "Infarto" }
        }
        esperar { hayTexto("Abuelo · 81 años · Infarto") }
    }

    // ---------- jefe de familia único ----------

    @Test
    fun conUnJefeRegistradoLasOpcionesJefeYJefaQuedanBloqueadas() {
        mostrarMiembros(ids.getValue("c1"))
        val luis = miembros(ids.getValue("c1")).first { it.apellidosNombres == "RAMÓN LUIS" }
        runBlocking { database.fichaContenidoDao().actualizarMiembro(luis.copy(parentesco = "JEFE DE FAMILIA")) }
        // RAMÓN NIÑO es otro integrante: ya no puede ser jefe.
        botonEditar(2).performScrollTo().performClick()
        esperar { hayTexto("4. Factores de riesgo según la edad") }
        rule.onAllNodes(hasText("Hijo"))[0].performScrollTo().performClick()
        esperar { hayTexto("Jefe de familia (ya registrado)") }
        rule.onNode(hasText("Jefe de familia (ya registrado)")).assertIsNotEnabled()
        rule.onNode(hasText("Jefa de familia (ya registrado)")).assertIsNotEnabled()
        rule.onNode(hasText("Hija")).assertIsEnabled()
    }

    // ---------- estado nutricional desde el CIE-10 ----------

    @Test
    fun elEstadoNutricionalSeTomaDelDiagnosticoCie10YYaNoHayListaParaElegirlo() {
        mostrarMiembros(ids.getValue("c3"))
        botonEditar(0).performScrollTo().performClick() // PÉREZ JUAN, 40 años
        esperar { hayTexto("3. Seguimiento preventivo") }
        abrirSeccion("3. Seguimiento preventivo")
        assertFalse("el spinner de estado nutricional se eliminó", hayTexto("Estado nutricional evaluado"))
        val campo = rule.onNodeWithText("Otros riesgos, enfermedad o discapacidad · CIE-10")
        campo.performScrollTo().performTextInput("E669")
        esperar { hayTexto("+ E66") }
        rule.onAllNodes(hasText("+ E66", substring = true))[0].performClick()
        rule.onNodeWithText("Guardar cambios del integrante").performClick()
        esperar { miembros(ids.getValue("c3")).any { it.estadoNutricional == "OBESIDAD" } }
    }

    // ---------- embarazada dentro de integrantes ----------

    private fun editarALucia() {
        mostrarMiembros(ids.getValue("c2"))
        // El nombre de la tarjeta identifica a la persona; el orden de «Editar» sigue el de la lista.
        val lucia = miembros(ids.getValue("c2")).indexOfFirst { it.apellidosNombres == "TORRES LUCÍA" }
        botonEditar(lucia).performScrollTo().performClick()
        esperar { hayTexto("Paciente embarazada") }
    }

    @Test
    fun laCasillaEmbarazadaMuestraLasSeccionesDelEmbarazoYElCuadroDiceElGrupoReal() {
        editarALucia()
        // TORRES LUCÍA ya tiene un embarazo registrado: la casilla está marcada y aparecen sus secciones.
        esperar { hayTexto("4.4 Riesgo obstétrico") }
        assertTrue(hayTexto("4.1 Gestación") && hayTexto("4.2 Vacunación dT") && hayTexto("4.3 Antecedentes obstétricos"))
        abrirSeccion("4.4 Riesgo obstétrico")
        assertFalse("los 4 botones de riesgo ya no existen", hayTexto("Muy alto"))

        rule.onNodeWithTag("resultado_obstetrico").performScrollTo()
        assertTrue("sin criterios: Grupo I", hayTexto("Grupo I"))

        elegir("criterio", "control", "CONTROL_INSUFICIENTE")
        esperar { hayTexto("Grupo II") }
        assertTrue(hayTexto("Riesgo 1 · Bajo."))

        elegir("criterio", "hemorragia", "HEMORRAGIA_VAGINAL")
        esperar { existe("aviso_riesgo_inminente") }
        assertTrue("no es una patología crónica: sigue en Grupo II", hayTexto("Grupo II"))

        elegir("criterio", "epilepsia", "EPILEPSIA")
        esperar { hayTexto("Grupo III") }
        assertTrue("solo se muestran los motivos del grupo en que quedó", hayTexto("Motivos del Grupo III"))

        rule.onNodeWithText("Guardar cambios del integrante").performClick()
        esperar {
            runBlocking { database.sincronizacionDao().embarazadas(ids.getValue("c2")) }
                .any { it.riesgoObstetrico == "MUY_ALTO" && it.factoresObstetricosJson.contains("EPILEPSIA") }
        }
    }

    @Test
    fun enLaEmbarazadaElConsumoProblematicoDiceGrupoIII() {
        editarALucia()
        esperar { hayTexto("4.4 Riesgo obstétrico") }
        abrirSeccion("4.4 Riesgo obstétrico")
        rule.onNodeWithTag("criterio_buscar").performScrollTo().performTextInput("alcohol")
        esperar { existe("criterio_sugerencia_CONSUMO_ALCOHOL") }
        assertTrue(hayTexto("Grupo III"))
        rule.onNodeWithTag("criterio_sugerencia_CONSUMO_ALCOHOL").performScrollTo().performClick()
        esperar { hayTexto("Motivos del Grupo III") }
    }

    @Test
    fun marcarYDesmarcarLaCasillaCreaYQuitaElRegistroDeEmbarazo() {
        mostrarMiembros(ids.getValue("c1"))
        val ana = miembros(ids.getValue("c1")).indexOfFirst { it.apellidosNombres == "RAMÓN NIÑO" }
        botonEditar(ana).performScrollTo().performClick()
        esperar { existe("casilla_embarazada") }
        assertFalse("sin marcar no hay secciones de embarazo", hayTexto("4.4 Riesgo obstétrico"))
        rule.onNodeWithTag("casilla_embarazada").performClick()
        esperar { hayTexto("4.4 Riesgo obstétrico") }
        rule.onNodeWithText("Guardar cambios del integrante").performClick()
        esperar { runBlocking { database.sincronizacionDao().embarazadas(ids.getValue("c1")) }.any { it.apellidosNombres == "RAMÓN NIÑO" } }

        // Se vuelve a abrir, se desmarca y el registro de embarazo desaparece.
        esperar { hayEditar() }
        val otra = miembros(ids.getValue("c1")).indexOfFirst { it.apellidosNombres == "RAMÓN NIÑO" }
        botonEditar(otra).performScrollTo().performClick()
        esperar { existe("casilla_embarazada") && hayTexto("4.4 Riesgo obstétrico") }
        rule.onNodeWithTag("casilla_embarazada").performClick()
        rule.onNodeWithText("Guardar cambios del integrante").performClick()
        esperar { runBlocking { database.sincronizacionDao().embarazadas(ids.getValue("c1")) }.none { it.apellidosNombres == "RAMÓN NIÑO" } }
    }

    // ---------- calificación del riesgo: ventana fija y botón flotante ----------

    @Test
    fun laCalificacionDelRiesgoTieneBotonFlotanteYUnaVentanaFijaConBotonesBloqueadosEnLosExtremos() {
        rule.setContent { RuralitosTheme { RiesgoFamiliarScreen(ids.getValue("c1"), "QA", {}, {}) } }
        esperar { existe("boton_agregar_riesgo") }
        assertTrue("el botón grande se quitó", rule.onAllNodes(hasText("Crear nueva calificación")).fetchSemanticsNodes().isEmpty())
        rule.onNodeWithTag("boton_agregar_riesgo").performClick()
        esperar { existe("contador_pregunta") }

        rule.onNodeWithTag("contador_pregunta").assertTextEquals("1 de 18")
        rule.onNodeWithTag("boton_anterior").assertIsNotEnabled()
        rule.onNodeWithTag("boton_siguiente").assertIsEnabled()
        repeat(17) { rule.onNodeWithTag("boton_siguiente").performClick() }
        esperar { runCatching { rule.onNodeWithTag("contador_pregunta").assertTextEquals("18 de 18") }.isSuccess }
        rule.onNodeWithTag("contador_pregunta").assertTextEquals("18 de 18")
        rule.onNodeWithTag("boton_siguiente").assertIsNotEnabled()
        rule.onNodeWithTag("boton_anterior").assertIsEnabled()
        rule.onNodeWithTag("boton_anterior").performClick()
        esperar { runCatching { rule.onNodeWithTag("contador_pregunta").assertTextEquals("17 de 18") }.isSuccess }
        rule.onNodeWithTag("boton_siguiente").assertIsEnabled()
        rule.onNodeWithTag("guardar_calificacion").assertIsNotEnabled()
    }

    // ---------- plan y seguimiento del riesgo ----------

    @Test
    fun alCrearLaFichaElSeguimientoNoPideEvaluarElCumplimiento() {
        val ficha = ids.getValue("c1")
        rule.setContent { RuralitosTheme { GestionRiesgoScreen(ficha, "QA", {}, {}, modoEdicion = false) } }
        esperar { existe("boton_agregar_seguimiento") }
        rule.onNodeWithTag("boton_agregar_seguimiento").performClick()
        esperar { hayTexto("Agregar seguimiento") }
        assertFalse("el formulario ya no trae la evaluación del cumplimiento", hayTexto("Evaluación del cumplimiento"))
        rule.onNodeWithText("Compromiso de la familia").performScrollTo().performTextInput("Control mensual de presión")
        rule.onNodeWithText("Guardar nuevo seguimiento").performClick()
        esperar { runBlocking { database.fichaContenidoDao().listarGestionRiesgo(ficha).first() }.isNotEmpty() }
        esperar { hayTexto("Seguimiento 1") }
        assertFalse("al crear la ficha no se evalúa", existe("evaluar_" + runBlocking { database.fichaContenidoDao().listarGestionRiesgo(ficha).first() }.first().id))
        assertTrue("la tarjeta conserva Editar y Eliminar", hayTexto("Editar") && hayTexto("Eliminar"))
        assertEquals("PENDIENTE", runBlocking { database.fichaContenidoDao().listarGestionRiesgo(ficha).first() }.first().cumplimiento)
    }

    @Test
    fun alEditarLaFichaCadaSeguimientoSeEvaluaEnUnaVentanaAparte() {
        val ficha = ids.getValue("c1")
        runBlocking {
            database.fichaContenidoDao().guardarGestionRiesgo(
                com.ruralitos.app.data.local.entity.GestionRiesgoEntity(
                    fichaId = ficha, fechaAnalisis = "04/10/2026", numero = 1,
                    compromisoFamilia = "Control mensual", compromisoEquipoSalud = "Visita", fechaEvaluacion = "",
                    cumplimiento = "PENDIENTE", causasIncumplimientoObservaciones = "", responsable = "QA"
                )
            )
        }
        val id = runBlocking { database.fichaContenidoDao().listarGestionRiesgo(ficha).first() }.first().id
        rule.setContent { RuralitosTheme { GestionRiesgoScreen(ficha, "QA", {}, {}, modoEdicion = true) } }
        esperar { existe("evaluar_$id") }
        rule.onNodeWithTag("evaluar_$id").performClick()
        esperar { existe("guardar_evaluacion") }
        rule.onNodeWithTag("resultado_SI_CUMPLE").performScrollTo().performClick()
        rule.onNodeWithTag("guardar_evaluacion").performClick()
        esperar { runBlocking { database.fichaContenidoDao().listarGestionRiesgo(ficha).first() }.first().cumplimiento == "SI_CUMPLE" }
    }

    // ---------- contaminación y lugar de atención ----------

    @Test
    fun contaminacionTieneBotonFlotanteRedondo() {
        rule.setContent { RuralitosTheme { ContaminacionAmbientalScreen(ids.getValue("c1"), {}, {}) } }
        esperar { existe("boton_agregar_contaminacion") }
        assertTrue(rule.onAllNodes(hasText("Registrar contaminación")).fetchSemanticsNodes().isEmpty())
        rule.onNodeWithTag("boton_agregar_contaminacion").performClick()
        esperar { hayTexto("Agregar contaminación") }
    }

    @Test
    fun elLugarDeAtencionSeEscribeEnUnaVentanaYYaNoHayCuadroDeConteo() {
        val ficha = ids.getValue("c1")
        rule.setContent { RuralitosTheme { LugaresTratamientoScreen(ficha, {}, {}) } }
        esperar { existe("boton_agregar_lugar") }
        rule.onNodeWithTag("boton_agregar_lugar").performClick()
        esperar { existe("texto_lugar") }
        rule.onNodeWithTag("texto_lugar").performTextInput("Centro de salud de Cerezal")
        rule.onNodeWithTag("guardar_lugar").performClick()
        esperar { runBlocking { database.fichaContenidoDao().listarLugaresTratamiento(ficha).first() }.size == 1 }
        esperar { hayTexto("Centro de salud de Cerezal") }
        assertFalse("el cuadro del conteo se quitó", hayTexto("lugar(es) o persona(s)"))
    }
}
