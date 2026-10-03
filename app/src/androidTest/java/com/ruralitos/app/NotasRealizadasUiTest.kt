package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.NotaDiariaEntity
import com.ruralitos.app.ui.screens.AcercaDeScreen
import com.ruralitos.app.ui.screens.NotasDiariasScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Una nota marcada como realizada se elimina; las que ya estaban realizadas se eliminan al abrir las notas. */
@RunWith(AndroidJUnit4::class)
class NotasRealizadasUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }
    private val usuarioId = 7L
    private lateinit var ids: Map<String, Long>
    private var miembroId = 0L

    private fun hoy() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    @Before
    fun sembrar() { runBlocking {
        runCatching {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission("com.ruralitos.app", "android.permission.POST_NOTIFICATIONS")
        }
        ids = DatosMapaParlante.sembrar(database)
        miembroId = database.sincronizacionDao().miembros(ids.getValue("c1")).first().id
    } }

    @After
    fun limpiar() { DatosMapaParlante.limpiar(database) }

    private fun nota(contenido: String, realizada: Boolean = false) =
        NotaDiariaEntity(miembroId = miembroId, fechaLocal = hoy(), contenido = contenido, realizada = realizada, usuarioId = usuarioId)

    private fun mostrarNotas() {
        rule.setContent { RuralitosTheme { NotasDiariasScreen(organizacionId = "", usuarioId = usuarioId, onRegresar = {}) } }
    }

    @Test
    fun alMarcarUnaNotaComoRealizadaSeElimina() {
        val id = runBlocking { database.notaDiariaDao().crear(nota("Llevar medicina")) }
        mostrarNotas()
        rule.waitUntil(20_000) { rule.onAllNodes(hasText("RAMÓN LUIS")).fetchSemanticsNodes().isNotEmpty() }
        // la tarjeta de la persona se abre con su botón «Ver»
        rule.onNodeWithText("Ver").performClick()
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("Al marcarla, la nota se elimina.")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Realizada").performClick()
        rule.waitUntil(10_000) { runBlocking { database.notaDiariaDao().buscar(id)?.eliminadoEn != null } }
        val eliminada = runBlocking { database.notaDiariaDao().buscar(id) }
        assertNotNull(eliminada?.eliminadoEn)
        // con una nota que sí se sincroniza, la baja queda pendiente para viajar a la nube
        assertEquals("PENDIENTE", eliminada?.syncEstado)
        // sin más notas, el cuadro se cierra y la persona deja de aparecer
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("Al marcarla, la nota se elimina.")).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun lasNotasQueYaEstabanRealizadasSeEliminanAlAbrirLasNotas() {
        val vieja = runBlocking { database.notaDiariaDao().crear(nota("Ya hecha", realizada = true)) }
        val pendiente = runBlocking { database.notaDiariaDao().crear(nota("Por hacer")) }
        mostrarNotas()
        rule.waitUntil(20_000) { runBlocking { database.notaDiariaDao().buscar(vieja)?.eliminadoEn != null } }
        assertNull("la pendiente no se toca", runBlocking { database.notaDiariaDao().buscar(pendiente) }?.eliminadoEn)
    }

    @Test
    fun acercaDeYaNoOfreceCargarUnaPlantilla() {
        rule.setContent { RuralitosTheme { AcercaDeScreen(onRegresar = {}) } }
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("Herramienta independiente")).fetchSemanticsNodes().isNotEmpty() }
        assertFalse(rule.onAllNodes(hasText("Cargar mi plantilla")).fetchSemanticsNodes().isNotEmpty())
        assertFalse(rule.onAllNodes(hasText("Plantilla del Excel de la ficha")).fetchSemanticsNodes().isNotEmpty())
        assertFalse(rule.onAllNodes(hasTestTag("cargar_plantilla")).fetchSemanticsNodes().isNotEmpty())
    }
}
