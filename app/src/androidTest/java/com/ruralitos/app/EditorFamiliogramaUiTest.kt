package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.domain.familiograma.AbreviaturasPatologia
import com.ruralitos.app.domain.familiograma.Ancla
import com.ruralitos.app.domain.familiograma.ArmadoFamiliograma
import com.ruralitos.app.domain.familiograma.Familiograma
import com.ruralitos.app.domain.familiograma.GeometriaFamiliograma
import com.ruralitos.app.domain.familiograma.IntegranteFamiliograma
import com.ruralitos.app.domain.familiograma.Lado
import com.ruralitos.app.domain.familiograma.Punto
import com.ruralitos.app.ui.familiograma.EditorFamiliogramaContenido
import com.ruralitos.app.ui.familiograma.EstadoEditorFamiliograma
import com.ruralitos.app.ui.theme.RuralitosTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Prueba el editor del familiograma con toques reales: mover figuras, unirlas desde sus puntos
 * y editar edad y patología. Requiere el teléfono o emulador en horizontal.
 */
@RunWith(AndroidJUnit4::class)
class EditorFamiliogramaUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun cinco(): Familiograma = ArmadoFamiliograma.desdeIntegrantes(
        listOf(
            IntegranteFamiliograma("Pérez Gómez Luis", "JEFE/A DE FAMILIA", "H", "45", listOf("Hipertensión arterial")),
            IntegranteFamiliograma("Pérez Ruiz Ana", "CÓNYUGE/PAREJA", "M", "42", listOf("Diabetes mellitus")),
            IntegranteFamiliograma("Pérez Ruiz Leo", "HIJO/A", "H", "22"),
            IntegranteFamiliograma("Pérez Ruiz Eva", "HIJO/A", "M", "18"),
            IntegranteFamiliograma("Pérez Ruiz Teo", "HIJO/A", "H", "9", listOf("Asma"))
        )
    )

    private fun abrir(): EstadoEditorFamiliograma {
        val estado = EstadoEditorFamiliograma(cinco())
        // se pasa a horizontal antes de dibujar, para que la pantalla no se reinicie con el contenido puesto
        rule.activityRule.scenario.onActivity { it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        rule.waitUntil(10_000) {
            rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        }
        rule.setContent {
            RuralitosTheme { EditorFamiliogramaContenido(estado, "Familia Pérez", { true }, {}, forzarHorizontal = false) }
        }
        rule.waitForIdle()
        return estado
    }

    private fun capturar(nombre: String, ventana: Boolean = false) {
        val raices = rule.onAllNodes(isRoot())
        val total = raices.fetchSemanticsNodes().size
        val destino = File(rule.activity.getExternalFilesDir(null), "familiograma").apply { mkdirs() }
        for (i in 0 until total) {
            val bitmap = raices[i].captureToImage().asAndroidBitmap()
            val sufijo = if (total > 1) "_v$i" else ""
            FileOutputStream(File(destino, "ui_$nombre$sufijo.png")).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun pantalla(estado: EstadoEditorFamiliograma, p: Punto): Offset = estado.aPantalla(p)

    @Test
    fun alMoverUnaPersonaSusLineasLaSiguen() {
        val estado = abrir()
        capturar("inicial")
        val antes = estado.doc.persona("p3")!!
        val inicio = pantalla(estado, Punto(antes.x, antes.y))
        rule.onNodeWithTag("lienzo").performTouchInput {
            down(inicio)
            moveTo(inicio + Offset(30f, 20f))
            moveTo(inicio + Offset(90f, 60f))
            up()
        }
        rule.waitForIdle()
        val despues = estado.doc.persona("p3")!!
        assertTrue("debe moverse a la derecha", despues.x > antes.x + 10f)
        assertTrue("debe moverse hacia abajo", despues.y > antes.y + 5f)
        // la línea del hijo termina exactamente en su punto de conexión superior
        val filiacion = estado.doc.filiaciones.first { it.hijo.elementoId == "p3" }
        val fin = GeometriaFamiliograma.rutaFiliacion(estado.doc, filiacion).last()
        assertEquals(despues.x, fin.x, 0.01f)
        assertEquals(despues.y - com.ruralitos.app.domain.familiograma.MEDIA_PERSONA, fin.y, 0.01f)
        assertTrue(estado.puedeDeshacer)
        capturar("movida")
        estado.deshacer()
        assertEquals(antes, estado.doc.persona("p3"))
    }

    @Test
    fun unePersonasArrastrandoDeUnPuntoAOtro() {
        val estado = abrir()
        estado.seleccionId = "p3"
        rule.waitForIdle()
        val a = GeometriaFamiliograma.posicion(estado.doc, Ancla("p3", Lado.DERECHA))!!
        val b = GeometriaFamiliograma.posicion(estado.doc, Ancla("p4", Lado.IZQUIERDA))!!
        val desde = pantalla(estado, a)
        val hasta = pantalla(estado, b)
        val unionesAntes = estado.doc.uniones.size
        rule.onNodeWithTag("lienzo").performTouchInput {
            down(desde)
            moveTo(Offset((desde.x + hasta.x) / 2, (desde.y + hasta.y) / 2))
            moveTo(hasta)
            up()
        }
        rule.waitForIdle()
        assertEquals(unionesAntes + 1, estado.doc.uniones.size)
        val nueva = estado.doc.uniones.last()
        assertEquals(Ancla("p3", Lado.DERECHA), nueva.a)
        assertEquals(Ancla("p4", Lado.IZQUIERDA), nueva.b)
    }

    @Test
    fun cuelgaUnHijoSoltandoSuPuntoSuperiorSobreLaUnion() {
        val estado = abrir()
        estado.cambiar { it.copy(filiaciones = it.filiaciones.filterNot { f -> f.hijo.elementoId == "p5" }) }
        estado.seleccionId = "p5"
        rule.waitForIdle()
        val union = estado.doc.uniones.first()
        val medio = GeometriaFamiliograma.puntoMedioUnion(estado.doc, union)!!.punto
        val desde = pantalla(estado, GeometriaFamiliograma.posicion(estado.doc, Ancla("p5", Lado.ARRIBA))!!)
        val hasta = pantalla(estado, medio)
        rule.onNodeWithTag("lienzo").performTouchInput {
            down(desde)
            moveTo(Offset((desde.x + hasta.x) / 2, (desde.y + hasta.y) / 2))
            moveTo(hasta)
            up()
        }
        rule.waitForIdle()
        val nueva = estado.doc.filiaciones.firstOrNull { it.hijo.elementoId == "p5" }
        assertNotNull(nueva)
        assertEquals(union.id, nueva!!.unionId)
    }

    @Test
    fun editaEdadYPatologiaYCreaUnaAbreviaturaNueva() {
        val estado = abrir()
        val p = estado.doc.persona("p1")!!
        val centro = pantalla(estado, Punto(p.x, p.y))
        rule.onNodeWithTag("lienzo").performTouchInput { click(centro) }
        rule.waitForIdle()
        capturar("dialogo_persona", ventana = true)
        rule.onNodeWithTag("campo_edad").performTextReplacement("46")
        rule.onNodeWithTag("campo_patologia").performTextInput("Insuficiencia renal crónica")
        rule.waitForIdle()
        capturar("dialogo_abreviatura_nueva", ventana = true)
        rule.onNodeWithTag("guardar_persona").performClick()
        rule.waitForIdle()
        val editada = estado.doc.persona("p1")!!
        assertEquals("46", editada.edad)
        assertEquals(listOf("Hipertensión arterial", "Insuficiencia renal crónica"), editada.patologias)
        assertEquals(listOf("Insuficiencia renal crónica"), estado.doc.patologiasNuevas.map { it.nombre })
        assertEquals("IRC", AbreviaturasPatologia.codigoDe(estado.doc, "Insuficiencia renal crónica"))
        capturar("con_abreviatura_nueva")
    }

    /** Deja el diálogo abierto unos segundos para poder fotografiar la pantalla real con adb. */
    @Test
    fun dejaElDialogoAbiertoParaFotografiarlo() {
        val estado = abrir()
        val p = estado.doc.persona("p2")!!
        rule.onNodeWithTag("lienzo").performTouchInput { click(pantalla(estado, Punto(p.x, p.y))) }
        rule.waitForIdle()
        rule.onNodeWithTag("campo_patologia").performTextInput("Gastritis")
        rule.waitForIdle()
        Thread.sleep(14_000)
    }
}
