package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.familiograma.AlmacenFamiliograma
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.domain.GrupoEdadFamiliar
import com.ruralitos.app.ui.screens.FamiliogramaScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Flujo completo de la sección: entrar con los integrantes de la ficha, dibujar, guardar, y comprobar
 * que el adjunto FAMILIOGRAMA es un PNG que conserva el dibujo editable. Crea una ficha de prueba y la borra.
 */
@RunWith(AndroidJUnit4::class)
class FamiliogramaFlujoUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun miembro(fichaId: Long, parentesco: String, sexo: String, nombre: String, nacimiento: String) =
        MiembroFamiliaEntity(
            fichaId = fichaId, grupoEdad = GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO,
            apellidosNombres = nombre, parentesco = parentesco, fechaNacimiento = nacimiento,
            ocupacion = "", sexo = sexo, escolaridad = "BAS", hipertensionArterial = if (parentesco.startsWith("JEFE")) true else null
        )

    @Test
    fun entraConLosIntegrantesDibujaYGuarda() {
        val context = rule.activity
        val database = RuralitosDatabase.obtenerBaseDatos(context)
        val ficha = FichaFamiliarEntity(
            cedulaJefeHogar = "0000000001", institucionSistema = "MSP", unidadOperativa = "QA", codigoUo = "1",
            areaNumero = "1", codigoLocalizacion = "1", parroquiaCodigoLocalizacion = "1", cantonCodigoLocalizacion = "1",
            provinciaCodigoLocalizacion = "1", numeroFichaFamiliar = "QA-FAMILIOGRAMA-${System.currentTimeMillis()}",
            provincia = "P", canton = "C", parroquia = "R", sector = "S", manzana = "1", numeroFamilia = "1",
            direccionHabitualFamilia = "D", barrio = "B", numeroCasa = "1", comunidad = "C", grupoCultural = "MESTIZO",
            nombreApellidoJefeFamilia = "QA PRUEBA", numeroTelefono = "0", fechaLlenado = "01/01/2026", numeroCarpeta = "1",
            responsableNombre = "QA", responsableCodigo = "1"
        )
        val fichaId = runBlocking { database.fichaFamiliarDao().guardarFicha(ficha) }
        try {
            runBlocking {
                val dao = database.fichaContenidoDao()
                dao.guardarMiembro(miembro(fichaId, "JEFE/A DE FAMILIA", "H", "PRUEBA LUIS", "01/01/1980"))
                dao.guardarMiembro(miembro(fichaId, "CÓNYUGE/PAREJA", "M", "PRUEBA ANA", "01/01/1982"))
                dao.guardarMiembro(miembro(fichaId, "HIJO/A", "H", "PRUEBA LEO", "01/01/2010"))
            }
            rule.activityRule.scenario.onActivity {
                it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            rule.waitUntil(10_000) {
                rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            }
            rule.setContent { RuralitosTheme { FamiliogramaScreen(fichaId, {}, {}) } }
            rule.waitUntil(15_000) {
                rule.onAllNodes(hasText("Ingresar con los 3 integrantes de la ficha")).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("Ingresar con los 3 integrantes de la ficha").performClick()
            rule.waitUntil(15_000) {
                rule.onAllNodes(hasText("Dibujar familiograma")).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("lienzo").assertExists()

            rule.onNodeWithText("Guardar").performClick()
            rule.waitUntil(20_000) {
                runBlocking {
                    database.fichaContenidoDao().listarTodosLosAdjuntos()
                        .any { it.fichaId == fichaId && it.tipo == AlmacenFamiliograma.TIPO }
                }
            }
            val adjunto = runBlocking {
                database.fichaContenidoDao().listarTodosLosAdjuntos().first { it.fichaId == fichaId }
            }
            val doc = runBlocking { AlmacenFamiliograma.leerDocumento(context, adjunto.uri) }
            assertNotNull("el PNG debe traer el dibujo para poder editarlo", doc)
            assertEquals(3, doc!!.personas.size)
            assertNotNull("la edad sale de la fecha de nacimiento", doc.personas.first { it.parentesco.startsWith("JEFE") }.edad.toIntOrNull())
            assertEquals(listOf("Hipertensión arterial"), doc.personas.first { it.parentesco.startsWith("JEFE") }.patologias)
            val archivo = File(android.net.Uri.parse(adjunto.uri).path!!)
            assertTrue(archivo.length() > 5_000)
            archivo.delete()
        } finally {
            runBlocking {
                database.fichaContenidoDao().listarTodosLosAdjuntos().filter { it.fichaId == fichaId }.forEach {
                    runCatching { File(android.net.Uri.parse(it.uri).path!!).delete() }
                }
                database.fichaFamiliarDao().eliminarFicha(ficha.copy(id = fichaId))
            }
        }
    }
}
