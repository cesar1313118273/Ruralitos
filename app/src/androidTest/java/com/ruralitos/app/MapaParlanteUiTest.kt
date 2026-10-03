package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.domain.IconosMais
import com.ruralitos.app.domain.MapaParlante
import com.ruralitos.app.ui.components.ClaseAncho
import com.ruralitos.app.ui.components.LocalClaseAncho
import com.ruralitos.app.ui.screens.MapaParlanteScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Mapa parlante con dos barrios de ejemplo: totales por barrio y solo las figuras que existen en cada uno. */
@RunWith(AndroidJUnit4::class)
class MapaParlanteUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }

    @Before
    fun sembrar() { runBlocking { DatosMapaParlante.sembrar(database) } }

    @After
    fun limpiar() { DatosMapaParlante.limpiar(database) }

    private fun mostrar(clase: ClaseAncho) {
        rule.activityRule.scenario.onActivity {
            it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        rule.waitUntil(10_000) {
            rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        }
        rule.setContent {
            RuralitosTheme {
                CompositionLocalProvider(LocalClaseAncho provides clase) { MapaParlanteScreen(onRegresar = {}) }
            }
        }
    }

    private fun hayTag(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    /** Abre la ventana del selector y elige el barrio (o «todos» si la clave es nula). */
    private fun elegirBarrio(clave: String?) {
        rule.onNodeWithTag("selector_barrio").performClick()
        val etiqueta = "barrio_${clave ?: "todos"}"
        rule.waitUntil(10_000) { hayTag(etiqueta) }
        rule.onNodeWithTag(etiqueta).performClick()
        rule.waitUntil(5_000) { !hayTag(etiqueta) }
    }

    @Test
    fun cadaBarrioMuestraSusTotalesYSoloLasFigurasQueTiene() {
        mostrar(ClaseAncho.EXPANDIDA)
        rule.waitUntil(20_000) { hayTag("selector_barrio") && rule.onAllNodes(hasText("Cerezal")).fetchSemanticsNodes().isNotEmpty() }
        // sin barrio elegido: la lista muestra cada barrio con su total de personas
        rule.onNodeWithText("Cerezal").assertExists()
        rule.onNodeWithText("El Carmen").assertExists()
        rule.onNodeWithText("Todos los barrios").assertExists()

        val esperado = runBlocking {
            MapaParlante.barrios(
                database.fichaFamiliarDao().listarFichas().first().filter { it.numeroFichaFamiliar.startsWith(DatosMapaParlante.PREFIJO) },
                database.fichaContenidoDao().listarTodosMiembros().first(),
                database.fichaContenidoDao().listarTodasEmbarazadas().first()
            )
        }
        val cerezal = esperado.first { it.nombre == "Cerezal" }
        val carmen = esperado.first { it.nombre == "El Carmen" }
        assertEquals(4, cerezal.fichas)
        assertEquals(9, cerezal.personas)
        assertEquals(1, cerezal.fichasSinUbicacion)
        val hipertensos = cerezal.stickers.first { it.id == IconosMais.HIPERTENSION }
        assertEquals("tres personas solo con hipertensión", 3, hipertensos.personas)
        assertTrue("la figura de embarazo existe en Cerezal", cerezal.stickers.any { it.id.startsWith("embarazo") })
        assertTrue("El Carmen no tiene hipertensos", carmen.stickers.none { it.id == IconosMais.HIPERTENSION })

        elegirBarrio(cerezal.clave)
        rule.waitUntil(10_000) { rule.onAllNodes(hasTestTag("fila_figura")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(
            "una fila por cada figura que existe en el barrio",
            cerezal.stickers.size, rule.onAllNodes(hasTestTag("fila_figura")).fetchSemanticsNodes().size
        )
        rule.onNodeWithText("9 personas · 4 familias").assertExists()

        elegirBarrio(carmen.clave)
        rule.waitUntil(10_000) {
            rule.onAllNodes(hasTestTag("fila_figura")).fetchSemanticsNodes().size == carmen.stickers.size
        }
        rule.onNodeWithText("1 persona · 1 familia").assertExists()
    }

    @Test
    fun enElTelefonoElResumenDelBarrioSeAbreYMuestraLasFiguras() {
        mostrar(ClaseAncho.COMPACTA)
        // en el teléfono no hay lista de barrios a la vista: se espera a que el encabezado diga cuántos hay
        rule.waitUntil(20_000) { hayTag("selector_barrio") && rule.onAllNodes(hasText("elige uno para ver sus figuras", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        elegirBarrio("cerezal")
        rule.waitUntil(10_000) { rule.onAllNodes(hasTestTag("resumen_barrio")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Ver detalle ▴").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("fila_figura")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("1 ficha sin ubicación no cuenta para el punto medio, pero sus personas sí están en los totales.").assertExists()
    }

    @Test
    fun conUnSoloBarrioElMapaSeCentraEnElSinCerrarse() {
        // Un encuadre necesita dos puntos como mínimo: con un barrio no debe fallar.
        database.openHelper.writableDatabase.execSQL(
            "DELETE FROM fichas_familiares WHERE numeroFichaFamiliar LIKE '${DatosMapaParlante.PREFIJO}%' AND barrio = 'Cerezal'"
        )
        mostrar(ClaseAncho.EXPANDIDA)
        rule.waitUntil(20_000) { hayTag("selector_barrio") && rule.onAllNodes(hasText("El Carmen")).fetchSemanticsNodes().isNotEmpty() }
        elegirBarrio("el carmen")
        rule.waitUntil(10_000) { rule.onAllNodes(hasTestTag("fila_figura")).fetchSemanticsNodes().isNotEmpty() }
        Thread.sleep(3_000)
        rule.waitForIdle()
        rule.onNodeWithText("1 persona · 1 familia").assertExists()
    }
}
