package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.ui.screens.CumplimientoScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Revisión visual de Cumplimiento: deja `cumplimiento_<etiqueta>_<n>.png`. Se activa con `-e dejarAbierto 1`. */
@RunWith(AndroidJUnit4::class)
class CumplimientoCapturaTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }
    private val sala = "SALA-CAPTURA"

    @After
    fun limpiar() {
        rule.activity.getSharedPreferences("cumplimiento_poblacion", 0).edit().clear().commit()
        DatosMapaParlante.limpiar(database)
    }

    private fun foto(etiqueta: String, n: Int) {
        repeat(4) { rule.waitForIdle(); Thread.sleep(400) }
        val captura = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        if (captura == null) { Thread.sleep(9_000); return } // otro proceso fotografía con screencap
        java.io.File(rule.activity.getExternalFilesDir(null), "cumplimiento_${etiqueta}_$n.png")
            .outputStream().use { captura.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it) }
    }

    @Test
    fun dejaLaPantallaParaFotografiarla() {
        val args = InstrumentationRegistry.getArguments()
        if (args.getString("dejarAbierto") != "1") return
        val etiqueta = args.getString("etiqueta") ?: "x"
        runBlocking { DatosMapaParlante.sembrar(database) }
        // la tabla de ejemplo del usuario
        val tabla = mapOf(
            "MENOR_UN_ANIO" to (12 to 14), "UNO_A_CUATRO" to (15 to 18), "CINCO_A_NUEVE" to (19 to 22),
            "DIEZ_A_CATORCE" to (13 to 17), "QUINCE_A_DIECINUEVE" to (12 to 17),
            "VEINTE_A_SESENTA_Y_CUATRO" to (1 to 1), "SESENTA_Y_CINCO_MAS" to (25 to 30)
        )
        rule.activity.getSharedPreferences("cumplimiento_poblacion", 0).edit().apply {
            tabla.forEach { (g, v) -> putInt("$sala|$g|H", v.first); putInt("$sala|$g|M", v.second) }
        }.commit()
        rule.setContent { RuralitosTheme { CumplimientoScreen(organizacionId = sala, onRegresar = {}) } }
        rule.waitUntil(20_000) { rule.onAllNodes(hasTestTag("cumplimiento_general")).fetchSemanticsNodes().isNotEmpty() }
        foto(etiqueta, 1)
        rule.onRoot().performTouchInput { swipeUp(startY = height * 0.85f, endY = height * 0.2f) }
        foto(etiqueta, 2)
        rule.onRoot().performTouchInput { swipeUp(startY = height * 0.85f, endY = height * 0.2f) }
        foto(etiqueta, 3)
        rule.onRoot().performTouchInput { swipeUp(startY = height * 0.85f, endY = height * 0.1f) }
        rule.onRoot().performTouchInput { swipeUp(startY = height * 0.85f, endY = height * 0.1f) }
        rule.onRoot().performTouchInput { swipeUp(startY = height * 0.85f, endY = height * 0.1f) }
        rule.onRoot().performTouchInput { swipeDown(startY = height * 0.2f, endY = height * 0.9f) }
        rule.onRoot().performTouchInput { swipeDown(startY = height * 0.2f, endY = height * 0.9f) }
        rule.onRoot().performTouchInput { swipeDown(startY = height * 0.2f, endY = height * 0.9f) }
        rule.onRoot().performTouchInput { swipeDown(startY = height * 0.2f, endY = height * 0.9f) }
        rule.onNodeWithTag("boton_poblacion").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("ventana_poblacion")).fetchSemanticsNodes().isNotEmpty() }
        foto(etiqueta, 4)
    }
}
