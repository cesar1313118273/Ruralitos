package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.ui.screens.AnimacionInicioRuralitos
import com.ruralitos.app.ui.theme.RuralitosTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** La animación de entrada muestra el nombre y la frase, y avisa al terminar (unos 2,5 s). */
@RunWith(AndroidJUnit4::class)
class AnimacionInicioTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun muestraElNombreYTerminaSola() {
        var terminada = false
        rule.setContent { RuralitosTheme { AnimacionInicioRuralitos(onTerminar = { terminada = true }) } }
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Ruralitos")).fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(10_000) { terminada }
        assertTrue(terminada)
    }
}
