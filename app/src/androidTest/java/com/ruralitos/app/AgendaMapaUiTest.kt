package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.ui.screens.AgendaScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** La agenda tiene tres pestañas, la tercera es el mapa, y ya no existe el botón verde «+». */
@RunWith(AndroidJUnit4::class)
class AgendaMapaUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    // La agenda pide permiso para avisos al abrirse; el cuadro del sistema taparía la pantalla en la prueba.
    @org.junit.Before
    fun concederPermisoDeAvisos() {
        runCatching {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission("com.ruralitos.app", android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Test
    fun elMapaEsUnaPestanaDeLaAgendaYElBotonMasYaNoEsta() {
        var pestana = -1
        rule.setContent {
            RuralitosTheme {
                AgendaScreen(
                    usuarioId = 0, organizacionId = "", onRegresar = {}, onAbrirFicha = {}, onAbrirRuta = {},
                    onPestanaCambiada = { pestana = it }
                )
            }
        }
        rule.waitUntil(15_000) { rule.onAllNodes(hasText("Mapa")).fetchSemanticsNodes().isNotEmpty() }
        // tres pestañas: Seguimiento, Agenda y Mapa
        assertEquals(true, rule.onAllNodes(hasText("Seguimiento")).fetchSemanticsNodes().isNotEmpty())
        assertEquals(1, rule.onAllNodes(hasText("Agenda")).fetchSemanticsNodes().size)
        assertEquals(1, rule.onAllNodes(hasText("Mapa")).fetchSemanticsNodes().size)
        // el botón verde de «+» ya no está en Seguimiento ni en Agenda
        rule.onAllNodes(hasText("+")).fetchSemanticsNodes().let { assertEquals(0, it.size) }
        rule.onNodeWithText("Agenda").performClick()
        rule.waitForIdle()
        rule.onAllNodes(hasText("+")).fetchSemanticsNodes().let { assertEquals(0, it.size) }

        rule.onNodeWithText("Mapa").performClick()
        rule.waitUntil(15_000) { rule.onAllNodes(hasTestTag("mapa_viviendas")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2, pestana)
        rule.onNodeWithText("Mapa de visitas").assertExists()
        rule.onAllNodes(hasTestTag("agendar_mapa")).fetchSemanticsNodes().let { assertEquals(1, it.size) }
    }
}
