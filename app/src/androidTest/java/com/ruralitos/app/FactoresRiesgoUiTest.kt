package com.ruralitos.app

import androidx.activity.ComponentActivity
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
import com.ruralitos.app.ui.screens.SaludFamiliarScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
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
    fun laListaTieneDosBotonesFlotantesYNoElCuadroContadorNiElBotonGrande() {
        mostrarMiembros(ids.getValue("c1"))
        assertTrue(existe("boton_agregar_integrante"))
        assertTrue(existe("boton_actores_comunitarios"))
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

    // ---------- embarazada ----------

    private fun mostrarSalud(fichaId: Long) {
        rule.setContent { RuralitosTheme { SaludFamiliarScreen(fichaId = fichaId, onContinuar = {}, onSalir = {}) } }
        esperar { hayTexto("Embarazos registrados") }
    }

    @Test
    fun laEmbarazadaEscribeSusCriteriosYUnCuadroDiceSuGrupoReal() {
        mostrarSalud(ids.getValue("c2"))
        esperar { hayEditar() }
        botonEditar(0).performClick() // TORRES LUCÍA, 24 años
        esperar { hayTexto("5. Riesgo obstétrico") }
        assertFalse("los 4 botones de riesgo ya no existen", hayTexto("Muy alto"))
        assertTrue(hayTexto("Rol familiar"))

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

        rule.onNodeWithText("Guardar cambios del embarazo").performClick()
        esperar {
            runBlocking { database.sincronizacionDao().embarazadas(ids.getValue("c2")) }
                .any { it.riesgoObstetrico == "MUY_ALTO" && it.factoresObstetricosJson.contains("EPILEPSIA") }
        }
    }

    @Test
    fun enLaEmbarazadaElConsumoProblematicoDiceGrupoIII() {
        mostrarSalud(ids.getValue("c2"))
        esperar { hayEditar() }
        botonEditar(0).performClick()
        esperar { hayTexto("5. Riesgo obstétrico") }
        rule.onNodeWithTag("criterio_buscar").performScrollTo().performTextInput("alcohol")
        esperar { existe("criterio_sugerencia_CONSUMO_ALCOHOL") }
        assertTrue(hayTexto("Grupo III"))
        rule.onNodeWithTag("criterio_sugerencia_CONSUMO_ALCOHOL").performScrollTo().performClick()
        esperar { hayTexto("Motivos del Grupo III") }
    }
}
