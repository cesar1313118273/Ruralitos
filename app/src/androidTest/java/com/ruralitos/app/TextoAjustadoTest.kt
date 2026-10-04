package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.ui.components.TextoAjustado
import com.ruralitos.app.ui.components.cortaPalabra
import com.ruralitos.app.ui.theme.RuralitosTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** El texto ajustado se achica hasta que ninguna letra queda cortada. */
@RunWith(AndroidJUnit4::class)
class TextoAjustadoTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun resultado(texto: String): TextLayoutResult {
        val lista = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(texto).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(lista)
        return lista.last()
    }

    @Test
    fun unaPalabraLargaEnUnEspacioChicoSeAchicaEnVezDeCortarse() {
        rule.setContent { RuralitosTheme { Box(Modifier.width(70.dp)) { TextoAjustado("Programadas", tamano = 20.sp) } } }
        rule.waitForIdle()
        Thread.sleep(500)
        rule.waitForIdle()
        val r = resultado("Programadas")
        assertFalse("no debe salirse de la línea", r.didOverflowWidth)
        assertTrue("debe haberse achicado", r.layoutInput.style.fontSize.value < 20f)
        assertEquals(1, r.lineCount)
    }

    @Test
    fun unaFraseLargaBajaDeLineaPorPalabrasSinCortarNinguna() {
        val frase = "Aparentemente sano y con factores de riesgo"
        rule.setContent { RuralitosTheme { Box(Modifier.width(120.dp)) { TextoAjustado(frase, tamano = 18.sp, maxLineas = 4) } } }
        rule.waitForIdle()
        Thread.sleep(500)
        rule.waitForIdle()
        val r = resultado(frase)
        assertFalse("ninguna palabra debe partirse en dos", cortaPalabra(frase, r))
        assertFalse(r.didOverflowHeight)
        assertTrue("una frase puede ocupar más de una línea", r.lineCount > 1)
    }

    @Test
    fun siCabeConservaSuTamano() {
        rule.setContent { RuralitosTheme { Box(Modifier.width(300.dp)) { TextoAjustado("Hoy", tamano = 16.sp) } } }
        rule.waitForIdle()
        Thread.sleep(300)
        rule.waitForIdle()
        assertEquals(16f, resultado("Hoy").layoutInput.style.fontSize.value, 0.01f)
    }
}
