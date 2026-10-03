package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.ui.screens.CumplimientoScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pantalla Cumplimiento: escribir la población asignada y compararla con las personas con ficha. */
@RunWith(AndroidJUnit4::class)
class CumplimientoUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }
    private val sala = "SALA-QA-CUMPLIMIENTO"

    private fun limpiarPoblacion() {
        rule.activity.getSharedPreferences("cumplimiento_poblacion", 0).edit().clear().commit()
    }

    @Before
    fun preparar() {
        runCatching {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission("com.ruralitos.app", "android.permission.POST_NOTIFICATIONS")
        }
        runBlocking { DatosMapaParlante.sembrar(database) }
        limpiarPoblacion()
    }

    @After
    fun limpiar() {
        limpiarPoblacion()
        DatosMapaParlante.limpiar(database)
    }

    private fun mostrar() {
        rule.setContent { RuralitosTheme { CumplimientoScreen(organizacionId = sala, onRegresar = {}) } }
    }

    private fun existe(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun sinPoblacionPideEscribirlaYAunAsiMuestraLasFichas() {
        mostrar()
        rule.waitUntil(15_000) { existe("cumplimiento_sin_poblacion") }
        assertTrue(existe("escribir_poblacion"))
        assertTrue(existe("grupo_VEINTE_A_SESENTA_Y_CUATRO"))
        assertTrue("no hay porcentaje inventado", rule.onAllNodes(hasText("—")).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun alEscribirLaPoblacionApareceElPorcentajeYSeConserva() {
        mostrar()
        rule.waitUntil(15_000) { existe("escribir_poblacion") }
        rule.onNodeWithTag("escribir_poblacion").performScrollTo().performClick()
        rule.waitUntil(5_000) { existe("ventana_poblacion") }
        rule.onNodeWithTag("casilla_VEINTE_A_SESENTA_Y_CUATRO_H").performTextInput("50")
        rule.onNodeWithTag("casilla_VEINTE_A_SESENTA_Y_CUATRO_M").performTextInput("60")
        rule.onNodeWithTag("guardar_poblacion").performClick()
        rule.waitUntil(5_000) { !existe("ventana_poblacion") }
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("porcentaje_VEINTE_A_SESENTA_Y_CUATRO")).fetchSemanticsNodes().isNotEmpty() }
        assertTrue("la tarjeta de bienvenida desaparece", !existe("cumplimiento_sin_poblacion"))
        // lo escrito queda guardado en el teléfono
        val guardado = rule.activity.getSharedPreferences("cumplimiento_poblacion", 0)
        assertTrue(guardado.getInt("$sala|VEINTE_A_SESENTA_Y_CUATRO|H", 0) == 50)
        assertTrue(guardado.getInt("$sala|VEINTE_A_SESENTA_Y_CUATRO|M", 0) == 60)
    }
}
