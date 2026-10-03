package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.ui.screens.DispensarizacionScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Herramienta de revisión visual de Registro general → Indicadores: abre el panel «Grupos de dispensarización» y deja
 * `indicadores_<etiqueta>.png` en los archivos externos de la app. Se activa con `-e dejarAbierto 1`.
 */
@RunWith(AndroidJUnit4::class)
class IndicadoresCapturaTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }

    @After
    fun limpiar() { DatosMapaParlante.limpiar(database) }

    @Test
    fun dejaLosIndicadoresAbiertosParaFotografiarlos() {
        val args = InstrumentationRegistry.getArguments()
        if (args.getString("dejarAbierto") != "1") return
        val ids = runBlocking { DatosMapaParlante.sembrar(database) }
        rule.setContent { RuralitosTheme { DispensarizacionScreen(fichaInicialId = ids.getValue("c1"), onAbrirFicha = {}, onRegresar = {}) } }
        rule.waitUntil(30_000) { rule.onAllNodes(hasText("Resumen de población")).fetchSemanticsNodes().isNotEmpty() }
        // el primer panel está abierto y empuja al segundo fuera de la pantalla: se pliega y, si hace falta, se desplaza
        rule.onNodeWithText("Resumen de población").performClick()
        rule.waitForIdle()
        var intentos = 0
        while (rule.onAllNodes(hasText("Grupos de dispensarización")).fetchSemanticsNodes().isEmpty() && intentos++ < 5) {
            rule.onRoot().performTouchInput { swipeUp() }
            rule.waitForIdle()
        }
        rule.onNodeWithText("Grupos de dispensarización").performClick()
        rule.waitForIdle()
        rule.onRoot().performTouchInput { swipeUp(startY = height * 0.80f, endY = height * 0.30f) }
        repeat(6) { rule.waitForIdle(); Thread.sleep(500) }
        val captura = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(rule.activity.getExternalFilesDir(null), "indicadores_${args.getString("etiqueta") ?: "x"}.png")
            .outputStream().use { captura.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it) }
    }
}
