package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.ui.screens.BuscarFichasScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Date

/**
 * Buscar fichas: sin elegir fechas solo se ven las fichas de HOY; al pasar el día la lista del día anterior
 * desaparece sola y lo antiguo se busca eligiendo fechas o «todas las fechas».
 */
@RunWith(AndroidJUnit4::class)
class BuscarFichasFechasTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }
    private val prefijo = "QA-BFECHA-"

    // Días fijos y lejanos para que no se mezclen con fichas reales ni dependan de la fecha del emulador.
    private fun dia(d: Int) = Calendar.getInstance().apply { clear(); set(2031, Calendar.MARCH, d, 10, 0, 0) }.time
    private fun texto(d: Int) = "%02d/03/2031".format(d)

    private fun ficha(codigo: String, fecha: String) = FichaFamiliarEntity(
        cedulaJefeHogar = "0000000011", institucionSistema = "", unidadOperativa = "QA", codigoUo = "1",
        areaNumero = "1", codigoLocalizacion = "1", parroquiaCodigoLocalizacion = "1", cantonCodigoLocalizacion = "1",
        provinciaCodigoLocalizacion = "1", numeroFichaFamiliar = "$prefijo$codigo", provincia = "P", canton = "C",
        parroquia = "R", sector = "S", manzana = "1", numeroFamilia = "1", direccionHabitualFamilia = "D", barrio = "B",
        numeroCasa = "1", comunidad = "C", grupoCultural = "MESTIZO", nombreApellidoJefeFamilia = "JEFE $codigo",
        numeroTelefono = "0", fechaLlenado = fecha, numeroCarpeta = "1", responsableNombre = "QA", responsableCodigo = "1"
    )

    @Before
    fun sembrar() = runBlocking {
        runCatching {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission("com.ruralitos.app", "android.permission.POST_NOTIFICATIONS")
        }
        borrar()
        val dao = database.fichaFamiliarDao()
        dao.guardarFicha(ficha("D10", texto(10)))
        dao.guardarFicha(ficha("D11", texto(11)))
        dao.guardarFicha(ficha("D05", texto(5)))
        Unit
    }

    private fun borrar() {
        database.openHelper.writableDatabase.execSQL("DELETE FROM fichas_familiares WHERE numeroFichaFamiliar LIKE '$prefijo%'")
    }

    @After
    fun limpiar() = borrar()

    private fun hay(codigo: String) = rule.onAllNodes(hasText("Ficha $prefijo$codigo", substring = true)).fetchSemanticsNodes().isNotEmpty()
    private fun esperar(condicion: () -> Boolean) = rule.waitUntil(15_000, condicion)
    private fun alcance() = rule.onNodeWithTag("alcance_fechas")

    private fun mostrar(reloj: androidx.compose.runtime.MutableState<Date>) {
        rule.setContent {
            RuralitosTheme {
                BuscarFichasScreen(onFichaSeleccionada = {}, onRegresar = {}, ahora = { reloj.value }, intervaloRelojMs = 100L)
            }
        }
    }

    @Test
    fun alAbrirSoloSeVenLasFichasDeHoy() {
        val reloj = mutableStateOf(dia(10))
        mostrar(reloj)
        esperar { hay("D10") }
        assertFalse("la de otro día no se muestra", hay("D11"))
        assertFalse("tampoco una antigua", hay("D05"))
        alcance().assertTextContains("solo las fichas de hoy, ${texto(10)}")
    }

    @Test
    fun alPasarElDiaLaListaSeVaciaSolaYSeMuestraLaDelNuevoHoy() {
        val reloj = mutableStateOf(dia(10))
        mostrar(reloj)
        esperar { hay("D10") }

        reloj.value = dia(11) // pasó la medianoche
        esperar { hay("D11") }
        assertFalse("la del día anterior ya no aparece", hay("D10"))
        alcance().assertTextContains("hoy, ${texto(11)}")
    }

    @Test
    fun enUnDiaSinFichasLasAntiguasSeBuscanConTodasLasFechas() {
        val reloj = mutableStateOf(dia(20))
        mostrar(reloj)
        esperar { rule.onAllNodes(hasTestTag("buscar_todas_fechas")).fetchSemanticsNodes().isNotEmpty() }
        assertFalse(hay("D10") || hay("D11") || hay("D05"))

        rule.onNodeWithTag("buscar_todas_fechas").performClick()
        esperar { hay("D10") }
        assertTrue(hay("D11"))
        assertTrue(hay("D05"))
        alcance().assertTextContains("todas las fechas")

        // aunque pase otro día, «todas las fechas» sigue siendo todas
        reloj.value = dia(21)
        rule.waitForIdle()
        assertTrue(hay("D10"))
    }

    @Test
    fun dejaLaPantallaParaFotografiarla() {
        val args = InstrumentationRegistry.getArguments()
        if (args.getString("dejarAbierto") != "1") return
        val reloj = mutableStateOf(dia(10))
        mostrar(reloj)
        esperar { hay("D10") }
        for (n in 1..2) {
            if (n == 2) rule.onNodeWithText("Fechas, barrio y estado").performClick()
            repeat(4) { rule.waitForIdle(); Thread.sleep(400) }
            val captura = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            java.io.File(rule.activity.getExternalFilesDir(null), "buscar_fechas_$n.png")
                .outputStream().use { captura.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it) }
        }
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertTextContains(texto: String) {
    this.assert(androidx.compose.ui.test.hasText(texto, substring = true))
}
