package com.ruralitos.app

import androidx.activity.ComponentActivity
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
import com.ruralitos.app.ui.screens.AccesoSupabaseScreen
import com.ruralitos.app.ui.screens.CargandoAccesoScreen
import com.ruralitos.app.ui.screens.ConfigurarPinScreen
import com.ruralitos.app.ui.screens.DesbloqueoOfflineScreen
import com.ruralitos.app.ui.screens.RecuperarCuentaScreen
import com.ruralitos.app.ui.screens.RegistroSupabaseScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pantallas de acceso con el diseño general: inicio de sesión, recuperar contraseña, registro y PIN sin internet. */
@RunWith(AndroidJUnit4::class)
class AccesoDisenoTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun hay(texto: String) = rule.onAllNodes(hasText(texto, substring = true)).fetchSemanticsNodes().isNotEmpty()
    private fun existe(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun elInicioDeSesionTieneEntrarRecuperarPinYCrearCuenta() {
        var evento = ""
        rule.setContent {
            RuralitosTheme {
                AccesoSupabaseScreen(
                    procesando = false, mensaje = null,
                    onIngresar = { c, _ -> evento = "entrar:$c" },
                    onCrearCuenta = { evento = "crear" }, onRecuperar = { evento = "recuperar" }, onSinInternet = { evento = "pin" }
                )
            }
        }
        listOf("Ingresa a tu cuenta", "¿Olvidaste tu contraseña?", "Entrar sin internet con PIN", "Crear una cuenta").forEach { assertTrue(it, hay(it)) }
        rule.onNodeWithTag("boton_entrar").assertIsNotEnabled()
        rule.onNodeWithTag("campo_correo").performTextInput("ana@correo.com")
        rule.onNodeWithTag("campo_clave_contraseña").performTextInput("secreta123")
        rule.onNodeWithTag("boton_entrar").assertIsEnabled().performClick()
        assertEquals("entrar:ana@correo.com", evento)
        rule.onNodeWithText("¿Olvidaste tu contraseña?").performClick()
        assertEquals("recuperar", evento)
        rule.onNodeWithTag("boton_sin_internet").performClick()
        assertEquals("pin", evento)
        rule.onNodeWithTag("boton_crear_cuenta").performClick()
        assertEquals("crear", evento)
    }

    @Test
    fun recuperarContrasenaNoTraeUnSegundoBotonDeVolver() {
        var enviado = ""
        rule.setContent { RuralitosTheme { RecuperarCuentaScreen(false, null, { enviado = it }, {}) } }
        assertTrue(hay("Enviar enlace de recuperación"))
        assertTrue("el regreso es el botón de arriba", !hay("Volver al inicio de sesión"))
        rule.onNodeWithTag("boton_enviar_enlace").assertIsNotEnabled()
        rule.onNodeWithTag("campo_correo").performTextInput("ana@correo.com")
        rule.onNodeWithTag("boton_enviar_enlace").assertIsEnabled().performClick()
        assertEquals("ana@correo.com", enviado)
    }

    @Test
    fun entrarSinInternetTieneCasillasDePinYNingunTextoDeMas() {
        var cedula = ""
        var pin = ""
        rule.setContent { RuralitosTheme { DesbloqueoOfflineScreen(false, null, { c, p -> cedula = c; pin = p }, {}) } }
        assertTrue(hay("Tu identificación") && hay("PIN de 6 dígitos") && hay("0/6 dígitos"))
        assertTrue(!hay("Usar correo y contraseña"))
        assertTrue(!hay("Se activa al completar"))
        rule.onNodeWithTag("boton_entrar_sin_conexion").assertIsNotEnabled()
        rule.onNodeWithTag("campo_cedula").performTextInput("1712345678")
        rule.onNodeWithTag("pin_acceso").performTextInput("123456")
        assertTrue(hay("6/6 dígitos"))
        rule.onNodeWithTag("boton_entrar_sin_conexion").assertIsEnabled().performClick()
        assertEquals("1712345678" to "123456", cedula to pin)
    }

    @Test
    fun crearCuentaVaPorSeccionesDesplegables() {
        rule.setContent { RuralitosTheme { RegistroSupabaseScreen(false, null, { _, _, _, _, _, _, _, _, _ -> }, {}) } }
        listOf("1. Datos de acceso", "2. Información personal y profesional", "3. Seguridad").forEach { assertTrue(it, hay(it)) }
        assertTrue(hay("Crear mi cuenta"))
        // al tocar «Crear mi cuenta» sin datos, avisa qué falta
        rule.onNodeWithTag("boton_crear_mi_cuenta").performClick()
        assertTrue(hay("Ingresa un correo válido."))
    }

    @Test
    fun configurarPinPideElPinDosVeces() {
        var guardado = ""
        rule.setContent { RuralitosTheme { ConfigurarPinScreen(false, null) { guardado = it } } }
        rule.onNodeWithTag("pin_nuevo").performTextInput("135790")
        rule.onNodeWithTag("pin_confirmar").performTextInput("135790")
        rule.onNodeWithTag("boton_guardar_pin").performClick()
        assertEquals("135790", guardado)
    }

    @Test
    fun laPantallaDeCargaYaNoMuestraPersonajeNiMensajes() {
        rule.setContent { RuralitosTheme { CargandoAccesoScreen() } }
        assertTrue(!hay("Hola") && !hay("bonito día"))
    }
}
