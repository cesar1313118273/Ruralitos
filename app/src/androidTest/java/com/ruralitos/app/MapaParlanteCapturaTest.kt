package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.ui.screens.MapaParlanteScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Herramienta de revisión visual del mapa parlante. Se activa con `-e dejarAbierto 1`; con `-e barrio cerezal`
 * elige ese barrio antes de fotografiar. Deja `mapa_parlante_<etiqueta>.png` en los archivos externos de la app.
 */
@RunWith(AndroidJUnit4::class)
class MapaParlanteCapturaTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }

    @After
    fun limpiar() { DatosMapaParlante.limpiar(database) }

    @Test
    fun dejaElMapaParlanteAbiertoParaFotografiarlo() {
        val args = InstrumentationRegistry.getArguments()
        if (args.getString("dejarAbierto") != "1") return
        runBlocking { DatosMapaParlante.sembrar(database) }
        rule.activityRule.scenario.onActivity {
            it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        rule.setContent { RuralitosTheme { MapaParlanteScreen(onRegresar = {}) } }
        // La pantalla solo avanza cuando la prueba deja pasar fotogramas: se espera con waitUntil, no con sleep.
        rule.waitUntil(30_000) { rule.onAllNodes(androidx.compose.ui.test.hasTestTag("selector_barrio")).fetchSemanticsNodes().isNotEmpty() }
        if (args.getString("ventana") == "1") {
            rule.onNodeWithTag("selector_barrio").performClick()
            rule.waitUntil(30_000) { rule.onAllNodes(androidx.compose.ui.test.hasTestTag("barrio_cerezal")).fetchSemanticsNodes().isNotEmpty() }
        }
        args.getString("barrio")?.let {
            rule.onNodeWithTag("selector_barrio").performClick()
            rule.waitUntil(30_000) { rule.onAllNodes(androidx.compose.ui.test.hasTestTag("barrio_$it")).fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("barrio_$it").performClick()
            if (args.getString("detalle") == "1") {
                rule.onNodeWithTag("resumen_barrio").performClick()
            }
        }
        repeat(((args.getString("espera")?.toLongOrNull() ?: 10L) * 2).toInt()) {
            rule.waitForIdle()
            Thread.sleep(500)
        }
        val etiqueta = args.getString("etiqueta") ?: "mapa"
        fun buscarMapa(v: android.view.View): org.maplibre.android.maps.MapView? =
            if (v is org.maplibre.android.maps.MapView) v
            else (v as? android.view.ViewGroup)?.let { g -> (0 until g.childCount).firstNotNullOfOrNull { buscarMapa(g.getChildAt(it)) } }
        val vistaMapa = buscarMapa(rule.activity.window.decorView)
        val listo = java.util.concurrent.CountDownLatch(1)
        rule.activity.runOnUiThread {
            vistaMapa?.getMapAsync { m ->
                m.snapshot { bmp ->
                    java.io.File(rule.activity.getExternalFilesDir(null), "mapa_parlante_solo_$etiqueta.png")
                        .outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it) }
                    listo.countDown()
                }
            } ?: listo.countDown()
        }
        listo.await(20, java.util.concurrent.TimeUnit.SECONDS)
        val captura = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(rule.activity.getExternalFilesDir(null), "mapa_parlante_$etiqueta.png").outputStream().use {
            captura.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it)
        }
    }

    /** Guarda como PNG las imágenes que se entregan al mapa, para revisarlas sin depender del dibujo del mapa. */
    @Test
    fun guardaLasImagenesDelTablero() {
        val args = InstrumentationRegistry.getArguments()
        if (args.getString("dibujos") != "1") return
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking { DatosMapaParlante.sembrar(database) }
        val barrios = runBlocking {
            com.ruralitos.app.domain.MapaParlante.barrios(
                database.fichaFamiliarDao().listarFichas().kotlinx_first(),
                database.fichaContenidoDao().listarTodosMiembros().kotlinx_first(),
                database.fichaContenidoDao().listarTodasEmbarazadas().kotlinx_first()
            )
        }
        val cerezal = barrios.first { it.nombre == "Cerezal" }
        val destino = contexto.getExternalFilesDir(null)
        com.ruralitos.app.ui.screens.TableroParlante.imagenTablero(contexto, cerezal).let { b ->
            java.io.File(destino, "dibujo_tablero.png").outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
        cerezal.stickers.forEachIndexed { i, st ->
            com.ruralitos.app.ui.screens.TableroParlante.imagenSticker(contexto, st.id, st.personas)?.let { b ->
                java.io.File(destino, "dibujo_figura_$i.png").outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.kotlinx_first(): T = runBlocking { this@kotlinx_first.first() }
}
