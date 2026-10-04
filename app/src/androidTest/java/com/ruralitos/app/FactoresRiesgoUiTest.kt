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

/** Formulario de la persona (factores por edad, secciones que se quitan o se ocultan), actores comunitarios y embarazada. */
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
        esperar { hayTexto("integrante(s) registrado(s)") }
    }

    private fun abrirSeccion(titulo: String) {
        rule.onNodeWithText(titulo).performScrollTo().performClick()
        rule.waitForIdle()
    }

    // ---------- formulario de la persona ----------

    @Test
    fun elFormularioQuitaEstrategiasYActoresYMuestraLosFactoresDeSuEdad() {
        mostrarMiembros(ids.getValue("c1"))
        // «RAMÓN NIÑO» (14 meses) es la tercera tarjeta
        botonEditar(2).performScrollTo().performClick()
        esperar { hayTexto("4. Factores de riesgo según la edad") }

        assertFalse("Estrategias Nacionales ya no se llena a mano", hayTexto("Estrategias Nacionales"))
        assertFalse("Actores comunitarios pasó al botón verde", hayTexto("7. Actores comunitarios"))
        assertFalse("Alertas epidemiológicas está oculta sin tuberculosis ni VIH", hayTexto("Alertas Epidemiológicas"))
        assertTrue(hayTexto("7. Otros riesgos prioritarios"))

        abrirSeccion("4. Factores de riesgo según la edad")
        esperar { existe("factor_PESO_BAJO") }
        rule.onNodeWithTag("banda_factores").assertTextEquals("0 a 23 meses")
        assertFalse("un factor de adultos mayores no aparece", existe("factor_RIESGO_CAIDA"))
        rule.onNodeWithTag("factor_PESO_BAJO").performScrollTo().performClick()
        rule.onNodeWithTag("resumen_factores").assertExists()
        assertTrue(hayTexto("Grupo II · con factores de riesgo"))

        rule.onNodeWithText("Guardar cambios del integrante").performClick()
        esperar { miembros(ids.getValue("c1")).first { it.apellidosNombres == "RAMÓN NIÑO" }.factoresRiesgoEdadJson.contains("PESO_BAJO") }
    }

    @Test
    fun laListaDeUnAdultoMayorEsOtra() {
        mostrarMiembros(ids.getValue("c1"))
        botonEditar(0).performClick() // RAMÓN LUIS, 66 años
        esperar { hayTexto("4. Factores de riesgo según la edad") }
        abrirSeccion("4. Factores de riesgo según la edad")
        esperar { existe("factor_RIESGO_CAIDA") }
        rule.onNodeWithTag("banda_factores").assertTextEquals("65 años o más")
        assertTrue(existe("factor_FRAGILIDAD"))
        assertFalse(existe("factor_PESO_BAJO"))
    }

    @Test
    fun consumoYViolenciaSeMarcanEnLaListaYAlimentanLosDatosDeSiempre() {
        mostrarMiembros(ids.getValue("c1"))
        botonEditar(1).performClick() // RAMÓN SOFÍA, 63 años
        esperar { hayTexto("4. Factores de riesgo según la edad") }
        abrirSeccion("4. Factores de riesgo según la edad")
        esperar { existe("factor_CONSUMO") }
        rule.onNodeWithTag("factor_CONSUMO").performScrollTo().performClick()
        rule.onNodeWithTag("factor_VIOLENCIA").performScrollTo().performClick()
        rule.onNodeWithText("Guardar cambios del integrante").performClick()
        esperar { miembros(ids.getValue("c1")).first { it.apellidosNombres == "RAMÓN SOFÍA" }.consumoAlcoholDrogas == true }
        val sofia = miembros(ids.getValue("c1")).first { it.apellidosNombres == "RAMÓN SOFÍA" }
        assertEquals(true, sofia.victimaViolencia)
        // la diabetes que traía de antes se conserva: no se pierde al quitar la pregunta manual
        assertEquals(true, sofia.diabetesMellitus)
    }

    @Test
    fun alElegirUnDiagnosticoDeTuberculosisApareceLaSeccionDeAlertas() {
        mostrarMiembros(ids.getValue("c1"))
        botonEditar(0).performClick()
        esperar { hayTexto("3. Seguimiento preventivo") }
        assertFalse(hayTexto("6. Alertas Epidemiológicas"))
        abrirSeccion("3. Seguimiento preventivo")
        val campo = rule.onNodeWithText("Otros riesgos, enfermedad o discapacidad · CIE-10")
        campo.performScrollTo().performTextInput("A15")
        esperar { hayTexto("+ A15") }
        rule.onAllNodes(hasText("+ A15", substring = true))[0].performClick()
        esperar { hayTexto("6. Alertas Epidemiológicas") }
    }

    // ---------- actores comunitarios ----------

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
    fun laEmbarazadaVeLaListaYUnCuadroQueDiceSuGrupo() {
        mostrarSalud(ids.getValue("c2"))
        esperar { hayEditar() }
        botonEditar(0).performClick() // TORRES LUCÍA, 24 años
        esperar { hayTexto("5. Riesgo 1 · Bajo") }
        assertFalse("los 4 botones de riesgo ya no existen", hayTexto("Muy alto"))

        rule.onNodeWithTag("resultado_obstetrico").performScrollTo()
        assertTrue("sin criterios: Grupo I", hayTexto("Grupo I"))

        abrirSeccion("5. Riesgo 1 · Bajo")
        esperar { existe("criterio_CONTROL_INSUFICIENTE") }
        rule.onNodeWithTag("criterio_CONTROL_INSUFICIENTE").performScrollTo().performClick()
        esperar { hayTexto("Grupo II") }
        assertTrue(hayTexto("Riesgo 1 · Bajo."))

        abrirSeccion("7. Riesgo 3 · Inminente")
        esperar { existe("criterio_HEMORRAGIA_VAGINAL") }
        rule.onNodeWithTag("criterio_HEMORRAGIA_VAGINAL").performScrollTo().performClick()
        esperar { existe("aviso_riesgo_inminente") }
        assertTrue("no es una patología crónica: sigue en Grupo II", hayTexto("Grupo II"))

        abrirSeccion("6. Riesgo 2 · Alto")
        esperar { existe("criterio_EPILEPSIA") }
        rule.onNodeWithTag("criterio_EPILEPSIA").performScrollTo().performClick()
        esperar { hayTexto("Grupo III") }

        rule.onNodeWithText("Guardar cambios del embarazo").performClick()
        esperar {
            runBlocking { database.sincronizacionDao().embarazadas(ids.getValue("c2")) }
                .any { it.riesgoObstetrico == "MUY_ALTO" && it.factoresObstetricosJson.contains("EPILEPSIA") }
        }
    }
}
