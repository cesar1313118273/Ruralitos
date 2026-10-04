package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.domain.ElementoCroquis
import com.ruralitos.app.domain.ElementosCroquis
import com.ruralitos.app.domain.TipoElementoCroquis
import com.ruralitos.app.ui.screens.CroquisMapaScreen
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

/** Símbolos y textos sobre el mapa del croquis: paleta, ventana de texto y guardado en la ficha. */
@RunWith(AndroidJUnit4::class)
class CroquisSimbolosUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }
    private var fichaId: Long = 0

    @Before
    fun sembrar() { fichaId = runBlocking { DatosMapaParlante.sembrar(database) }.getValue("c1") }

    @After
    fun limpiar() = DatosMapaParlante.limpiar(database)

    private fun existe(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    private fun esperar(condicion: () -> Boolean) = rule.waitUntil(30_000, condicion)

    private fun mostrar() {
        rule.setContent {
            RuralitosTheme {
                CroquisMapaScreen(fichaId = fichaId, usuarioId = 1L, modoEdicion = true, onContinuar = {}, onRegresar = {})
            }
        }
        esperar { existe("boton_simbolo") && existe("boton_texto_mapa") }
    }

    @Test
    fun elBotonMasAbreLaPaletaYAlElegirUnSimboloPideTocarElMapa() {
        mostrar()
        rule.onNodeWithTag("boton_simbolo").performClick()
        esperar { existe("paleta_simbolos") }
        listOf("IGLESIA", "ESCUELA", "PARQUE", "CANCHA", "TIENDA", "SALUD").forEach { assertTrue(it, existe("simbolo_$it")) }
        rule.onNodeWithTag("simbolo_IGLESIA").performClick()
        esperar { existe("aviso_colocar") }
        rule.onNode(hasText("Toca el mapa para colocar: Iglesia", substring = true)).assertExists()
        rule.onNodeWithTag("cancelar_colocar").performClick()
        esperar { !existe("aviso_colocar") }
    }

    @Test
    fun elBotonTPideElTextoYLuegoLoColoca() {
        mostrar()
        rule.onNodeWithTag("boton_texto_mapa").performClick()
        esperar { existe("texto_croquis") }
        rule.onNodeWithTag("texto_croquis").performTextInput("  Camino   al río ")
        rule.onNodeWithTag("guardar_texto_croquis").performClick()
        esperar { existe("aviso_colocar") }
        rule.onNode(hasText("Toca el mapa para colocar: Camino al río", substring = true)).assertExists()
    }

    @Test
    fun losElementosGuardadosSeLeenDeLaFicha() {
        val lista = listOf(
            ElementoCroquis("a", TipoElementoCroquis.SIMBOLO, "PARQUE", "Parque central", -0.2201, -78.5123),
            ElementoCroquis("b", TipoElementoCroquis.TEXTO, "", "Tienda de Rosa", -0.2203, -78.5125)
        )
        runBlocking { database.fichaFamiliarDao().guardarElementosCroquis(fichaId, ElementosCroquis.codificar(lista)) }
        val ficha = runBlocking { database.fichaFamiliarDao().observarPorId(fichaId).first() }!!
        assertEquals(lista, ElementosCroquis.decodificar(ficha.croquisElementosJson))
        assertEquals("PENDIENTE", ficha.syncEstado)
        mostrar()
    }
}
