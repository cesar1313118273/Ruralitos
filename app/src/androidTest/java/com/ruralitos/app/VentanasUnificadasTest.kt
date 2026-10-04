package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.ui.components.AvisosRuralitos
import com.ruralitos.app.ui.components.BuscadorDeFactores
import com.ruralitos.app.ui.components.CalendarioRuralitos
import com.ruralitos.app.ui.components.HoraRuralitos
import com.ruralitos.app.ui.components.HostAvisosRuralitos
import com.ruralitos.app.ui.components.ItemMenuRuralitos
import com.ruralitos.app.ui.components.MenuDesplegableRuralitos
import com.ruralitos.app.ui.components.OpcionBusqueda
import com.ruralitos.app.ui.components.VentanaConfirmarRuralitos
import com.ruralitos.app.ui.theme.RuralitosTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

/** Ventanas, calendario, hora, menús, buscador y avisos con el diseño único de la app. */
@RunWith(AndroidJUnit4::class)
class VentanasUnificadasTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun existe(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    private fun hay(texto: String) = rule.onAllNodes(hasText(texto, substring = true)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun laVentanaDeConfirmarTieneDosBotonesYLaDePeligroNoSeCierraTocandoFuera() {
        var evento = ""
        rule.setContent {
            RuralitosTheme {
                VentanaConfirmarRuralitos(
                    titulo = "Eliminar integrante", mensaje = "¿Deseas eliminar a RAMÓN LUIS?",
                    textoConfirmar = "Sí, eliminar", textoCancelar = "Conservar integrante",
                    onConfirmar = { evento = "confirmar" }, onCancelar = { evento = "cancelar" }
                )
            }
        }
        listOf("Eliminar integrante", "¿Deseas eliminar a RAMÓN LUIS?", "Sí, eliminar", "Conservar integrante").forEach { assertTrue(it, hay(it)) }
        rule.onNodeWithText("Sí, eliminar").performClick()
        assertEquals("confirmar", evento)
        rule.onNodeWithText("Conservar integrante").performClick()
        assertEquals("cancelar", evento)
    }

    @Test
    fun elCalendarioEligeUnDiaYDevuelveElInicioDeEseDia() {
        var elegida = 0L
        val inicial = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 4) }.timeInMillis
        rule.setContent { RuralitosTheme { CalendarioRuralitos("Fecha de nacimiento", inicial, { elegida = it }, {}) } }
        assertTrue(hay("Octubre") && hay("2026"))
        rule.onNodeWithTag("dia_15").performClick()
        rule.onNodeWithTag("calendario_aceptar").assertIsEnabled().performClick()
        val c = Calendar.getInstance().apply { timeInMillis = elegida }
        assertEquals(listOf(2026, Calendar.OCTOBER, 15), listOf(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)))
    }

    @Test
    fun elCalendarioCambiaDeAnioConLaListaYDeMesConLasFlechas() {
        var elegida = 0L
        val inicial = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 4) }.timeInMillis
        rule.setContent { RuralitosTheme { CalendarioRuralitos("Fecha", inicial, { elegida = it }, {}) } }
        rule.onNodeWithTag("calendario_anio").performClick()
        rule.waitUntil(5_000) { existe("anio_1990") }
        rule.onNodeWithTag("anio_1990").performClick()
        rule.onNodeWithTag("mes_anterior").performClick()
        assertTrue(hay("Septiembre") && hay("1990"))
        rule.onNodeWithTag("dia_3").performClick()
        rule.onNodeWithTag("calendario_aceptar").performClick()
        val c = Calendar.getInstance().apply { timeInMillis = elegida }
        assertEquals(listOf(1990, Calendar.SEPTEMBER, 3), listOf(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)))
    }

    @Test
    fun laHoraSubeBajaYAceptaHorasFrecuentes() {
        var hora = -1
        var minuto = -1
        rule.setContent { RuralitosTheme { HoraRuralitos("Hora de la visita", 9, 30, { h, m -> hora = h; minuto = m }, {}) } }
        rule.onNodeWithTag("hora_subir").performClick()
        rule.onNodeWithTag("minuto_bajar").performClick()
        rule.onNodeWithTag("hora_aceptar").performClick()
        assertEquals(10 to 29, hora to minuto)
        rule.onNodeWithTag("hora_14_00").performClick()
        rule.onNodeWithTag("hora_aceptar").performClick()
        assertEquals(14 to 0, hora to minuto)
    }

    @Test
    fun elMenuMarcaLaOpcionElegidaYBloqueaLasNoDisponibles() {
        var abierto by mutableStateOf(true)
        var elegido = ""
        rule.setContent {
            RuralitosTheme {
                androidx.compose.foundation.layout.Box {
                    MenuDesplegableRuralitos(expanded = abierto, onDismissRequest = { abierto = false }) {
                        ItemMenuRuralitos(text = { Text("Jefe de familia (ya registrado)") }, enabled = false, onClick = { elegido = "jefe" })
                        ItemMenuRuralitos(text = { Text("Hijo") }, seleccionado = true, onClick = { elegido = "hijo" })
                        ItemMenuRuralitos(text = { Text("Hija") }, casilla = false, onClick = { elegido = "hija" })
                    }
                }
            }
        }
        rule.onNodeWithText("Jefe de familia (ya registrado)").assertIsNotEnabled()
        assertTrue(hay("✓"))
        rule.onNodeWithText("Hija").performClick()
        assertEquals("hija", elegido)
    }

    @Test
    fun elBuscadorMuestraLasSugerenciasEnUnaTarjetaYLoElegidoEnEtiquetasQueSeQuitan() {
        var elegidos by mutableStateOf(emptySet<String>())
        rule.setContent {
            RuralitosTheme {
                BuscadorDeFactores(
                    titulo = "Escribe un factor", prefijoPrueba = "factor", elegidos = elegidos, onCambio = { elegidos = it },
                    opciones = listOf(
                        OpcionBusqueda("OBESIDAD", "Obesidad"),
                        OpcionBusqueda("EPILEPSIA", "Epilepsia", grupoIII = true)
                    )
                )
            }
        }
        rule.onNodeWithTag("factor_buscar").performTextInput("epi")
        rule.waitUntil(5_000) { existe("factor_sugerencia_EPILEPSIA") }
        assertTrue("la marca de grupo", hay("Grupo III"))
        rule.onNodeWithTag("factor_sugerencia_EPILEPSIA").performClick()
        rule.waitUntil(5_000) { existe("factor_EPILEPSIA") }
        assertEquals(setOf("EPILEPSIA"), elegidos)
        rule.onNodeWithTag("factor_EPILEPSIA").performClick()
        rule.waitUntil(5_000) { !existe("factor_EPILEPSIA") }
        assertTrue(elegidos.isEmpty())
    }

    @Test
    fun losAvisosBrevesSeMuestranEnVerdeOEnRojoYSeQuitanSolos() {
        rule.setContent { RuralitosTheme { HostAvisosRuralitos() } }
        assertFalse(AvisosRuralitos.esError("Visita registrada."))
        assertTrue(AvisosRuralitos.esError("No se pudo guardar el integrante."))
        AvisosRuralitos.mostrar("Visita registrada.")
        rule.waitUntil(5_000) { hay("Visita registrada.") }
        AvisosRuralitos.mostrar("No se pudo guardar el integrante.")
        rule.waitUntil(5_000) { hay("No se pudo guardar el integrante.") }
        rule.waitUntil(12_000) { !hay("Visita registrada.") && !hay("No se pudo guardar") }
    }
}
