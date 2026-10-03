package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.ui.components.ClaseAncho
import com.ruralitos.app.ui.components.ListaDeColumnasAdaptable
import com.ruralitos.app.ui.components.LocalClaseAncho
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Las tarjetas de una lista se reparten en columnas según el ancho de la ventana; los encabezados no. */
@RunWith(AndroidJUnit4::class)
class ListaAdaptableUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun dibujar(clase: ClaseAncho) {
        rule.setContent {
            CompositionLocalProvider(LocalClaseAncho provides clase) {
                ListaDeColumnasAdaptable(Modifier.fillMaxWidth()) {
                    item(key = "titulo") { Box(Modifier.fillMaxWidth().height(40.dp)) { Text("Encabezado") } }
                    items(listOf("A", "B", "C"), key = { it }) { nombre ->
                        Box(Modifier.fillMaxWidth().height(60.dp)) { Text("Tarjeta $nombre") }
                    }
                }
            }
        }
    }

    @Test
    fun enPantallaAnchaLasTarjetasVanDeADosYElEncabezadoOcupaTodo() {
        dibujar(ClaseAncho.EXPANDIDA)
        val titulo = rule.onNodeWithText("Encabezado").getUnclippedBoundsInRoot()
        val a = rule.onNodeWithText("Tarjeta A").getUnclippedBoundsInRoot()
        val b = rule.onNodeWithText("Tarjeta B").getUnclippedBoundsInRoot()
        val c = rule.onNodeWithText("Tarjeta C").getUnclippedBoundsInRoot()
        assertEquals("A y B en la misma fila", a.top, b.top)
        assertTrue("B a la derecha de A", b.left > a.left)
        assertTrue("C pasa a la fila siguiente", c.top > a.top)
        assertEquals("C vuelve a la primera columna", a.left, c.left)
        assertTrue(titulo.top < a.top)
        rule.onNodeWithText("Tarjeta C").assertIsDisplayed()
    }

    @Test
    fun enTelefonoTodoVaEnUnaColumna() {
        dibujar(ClaseAncho.COMPACTA)
        val a = rule.onNodeWithText("Tarjeta A").getUnclippedBoundsInRoot()
        val b = rule.onNodeWithText("Tarjeta B").getUnclippedBoundsInRoot()
        assertEquals(a.left, b.left)
        assertTrue(b.top > a.top)
    }
}
