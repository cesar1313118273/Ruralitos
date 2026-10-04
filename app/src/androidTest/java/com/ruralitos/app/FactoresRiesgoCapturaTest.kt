package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.ui.screens.MiembrosFamiliaScreen
import com.ruralitos.app.ui.screens.SaludFamiliarScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Revisión visual: `factores_<n>.png` en los archivos externos. Se activa con `-e dejarAbierto 1`. */
@RunWith(AndroidJUnit4::class)
class FactoresRiesgoCapturaTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }

    @After
    fun limpiar() = DatosMapaParlante.limpiar(database)

    private fun foto(n: Int) {
        repeat(4) { rule.waitForIdle(); Thread.sleep(400) }
        val captura = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        java.io.File(rule.activity.getExternalFilesDir(null), "factores_$n.png")
            .outputStream().use { captura.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it) }
    }

    private fun hay(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    private fun hayTexto(t: String) = rule.onAllNodes(hasText(t, substring = true)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun dejaLasPantallasParaFotografiarlas() {
        if (InstrumentationRegistry.getArguments().getString("dejarAbierto") != "1") return
        runCatching {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission("com.ruralitos.app", "android.permission.POST_NOTIFICATIONS")
        }
        val ids = runBlocking { DatosMapaParlante.sembrar(database) }

        // 1. lista de integrantes con el botón verde
        rule.setContent { RuralitosTheme { MiembrosFamiliaScreen(fichaId = ids.getValue("c1"), usuarioId = 7L, onContinuar = {}, onSalir = {}) } }
        rule.waitUntil(20_000) { hayTexto("integrante(s) registrado(s)") }
        foto(1)
        rule.onNodeWithTag("boton_actores_comunitarios").performClick()
        rule.waitUntil(10_000) { hay("lista_actores") }
        foto(2)
        rule.onAllNodes(hasTestTag("lista_actores"))[0].let { }
        // 2. formulario de la persona, sección de factores (adulto mayor)
        rule.onNodeWithText("Cerrar").performClick()
        rule.onAllNodes(hasText("Editar"))[0].performClick()
        rule.waitUntil(20_000) { hayTexto("4. Factores de riesgo según la edad") }
        rule.onNodeWithText("4. Factores de riesgo según la edad").performScrollTo().performClick()
        rule.waitUntil(10_000) { hay("factor_RIESGO_CAIDA") }
        rule.onNodeWithTag("factor_FRAGILIDAD").performScrollTo().performClick()
        rule.onNodeWithTag("resumen_factores").performScrollTo()
        foto(3)
    }

    @Test
    fun dejaLaEmbarazadaParaFotografiarla() {
        if (InstrumentationRegistry.getArguments().getString("dejarAbierto") != "1") return
        val ids = runBlocking { DatosMapaParlante.sembrar(database) }
        rule.setContent { RuralitosTheme { SaludFamiliarScreen(fichaId = ids.getValue("c2"), onContinuar = {}, onSalir = {}) } }
        rule.waitUntil(20_000) { hayTexto("Embarazos registrados") }
        rule.waitUntil(20_000) { rule.onAllNodes(hasText("Editar")).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(hasText("Editar"))[0].performClick()
        rule.waitUntil(20_000) { hayTexto("5. Riesgo 1 · Bajo") }
        rule.onNodeWithText("5. Riesgo 1 · Bajo").performScrollTo().performClick()
        rule.waitUntil(10_000) { hay("criterio_CONTROL_INSUFICIENTE") }
        rule.onNodeWithTag("criterio_ANALFABETISMO").performScrollTo().performClick()
        rule.onNodeWithTag("criterio_CONTROL_INSUFICIENTE").performScrollTo().performClick()
        foto(4)
        rule.onNodeWithTag("resultado_obstetrico").performScrollTo()
        foto(5)
    }
}
