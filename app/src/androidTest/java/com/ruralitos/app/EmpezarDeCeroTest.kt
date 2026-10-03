package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.fichas.EliminadorFichas
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import com.ruralitos.app.data.local.entity.NotaDiariaEntity
import com.ruralitos.app.data.local.entity.UsuarioEntity
import com.ruralitos.app.ui.screens.SeguridadRespaldoScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** «Empezar desde cero»: elimina todas las fichas con su contenido y deja anotado lo que hay que borrar en la nube. */
@RunWith(AndroidJUnit4::class)
class EmpezarDeCeroTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }
    private lateinit var ids: Map<String, Long>
    private val usuarioId = 7L

    private fun contar(tabla: String, donde: String = "1=1"): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $tabla WHERE $donde").use { it.moveToFirst(); it.getInt(0) }

    @Before
    fun sembrar() { runBlocking {
        // la base de pruebas parte limpia de fichas para poder comprobar que quedan en cero
        database.openHelper.writableDatabase.execSQL("DELETE FROM fichas_familiares")
        database.openHelper.writableDatabase.execSQL("DELETE FROM historial_fichas")
        ids = DatosMapaParlante.sembrar(database)
        // una ficha ya sincronizada con la nube y con una nota de esta cuenta
        database.openHelper.writableDatabase.execSQL("UPDATE fichas_familiares SET organizacionId = 'ORG-QA' WHERE id = ${ids.getValue("c1")}")
        val miembroId = database.fichaContenidoDao().let { database.sincronizacionDao().miembros(ids.getValue("c1")).first().id }
        database.notaDiariaDao().crear(NotaDiariaEntity(miembroId = miembroId, fechaLocal = "2026-10-03", contenido = "Llevar medicina", usuarioId = usuarioId))
        database.agendaDao().crear(
            ActividadAgendaEntity(usuarioId = usuarioId, fichaId = ids.getValue("c1"), persona = "RAMÓN LUIS", fechaHora = System.currentTimeMillis() + 3600_000, tipo = "Visita domiciliaria")
        )
    } }

    @After
    fun limpiar() {
        val db = database.openHelper.writableDatabase
        db.execSQL("DELETE FROM eliminaciones_sync WHERE organizacionId = 'ORG-QA'")
        db.execSQL("DELETE FROM historial_fichas")
        db.execSQL("DELETE FROM actividades_agenda WHERE usuarioId = $usuarioId")
        DatosMapaParlante.limpiar(database)
    }

    @Test
    fun eliminarTodasBorraLasFichasConTodoSuContenidoYAnotaLoDeLaNube() {
        val antes = contar("fichas_familiares")
        assertEquals(5, antes)
        assertTrue(contar("miembros_familia") > 0)

        val eliminadas = runBlocking { EliminadorFichas.eliminarTodas(rule.activity, database, usuarioId) }
        assertEquals(5, eliminadas)
        assertEquals("no queda ninguna ficha", 0, contar("fichas_familiares"))
        assertEquals("ni integrantes", 0, contar("miembros_familia"))
        assertEquals("ni embarazos", 0, contar("embarazadas"))
        assertEquals("ni notas sobre sus integrantes", 0, contar("notas_diarias"))
        assertEquals("ni historial de esas fichas", 0, contar("historial_fichas"))
        assertEquals("las visitas de la agenda quedan dadas de baja", 0, contar("actividades_agenda", "usuarioId = $usuarioId AND eliminadoEn IS NULL"))

        // lo que ya estaba en la nube queda anotado para borrarse: la ficha, sus integrantes y la nota de esta cuenta
        assertEquals(1, contar("eliminaciones_sync", "organizacionId = 'ORG-QA' AND tabla = 'fichas_familiares'"))
        assertEquals(3, contar("eliminaciones_sync", "organizacionId = 'ORG-QA' AND tabla = 'miembros_familia'"))
        assertEquals(1, contar("eliminaciones_sync", "organizacionId = 'ORG-QA' AND tabla = 'notas_privadas'"))
        // las fichas que nunca se sincronizaron no dejan nada pendiente
        assertEquals(5, contar("eliminaciones_sync", "organizacionId = 'ORG-QA'"))
    }

    @Test
    fun laCuentaYLaSalaNoSeTocan() {
        val usuarios = contar("usuarios")
        val salas = contar("salas")
        runBlocking { EliminadorFichas.eliminarTodas(rule.activity, database, usuarioId) }
        assertEquals(usuarios, contar("usuarios"))
        assertEquals(salas, contar("salas"))
    }

    @Test
    fun eliminarUnaFichaConservaLasDemasYSuHistorial() {
        runBlocking {
            val ficha = database.fichaFamiliarDao().buscarPorId(ids.getValue("e1"))!!
            database.historialFichaDao().registrar(
                com.ruralitos.app.data.local.entity.HistorialFichaEntity(
                    fichaId = ficha.id, numeroFicha = ficha.numeroFichaFamiliar, usuarioId = usuarioId, usuarioNombre = "QA", accion = "FICHA_ELIMINADA"
                )
            )
            EliminadorFichas.eliminarFicha(rule.activity, database, ficha, usuarioId)
        }
        assertEquals(4, contar("fichas_familiares"))
        assertEquals("la constancia de la baja se conserva", 1, contar("historial_fichas", "accion = 'FICHA_ELIMINADA'"))
    }

    @Test
    fun laPantallaPideEscribirEliminarYSoloEntoncesBorra() {
        val permiso = "android.permission.POST_NOTIFICATIONS"
        runCatching { InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission("com.ruralitos.app", permiso) }
        val usuario = UsuarioEntity(
            id = usuarioId, cedula = "0000000007", nombres = "QA", cargo = "Médico", rol = "ADMIN",
            claveHash = "", claveSalt = "", correo = "", telefono = ""
        )
        rule.setContent { RuralitosTheme { SeguridadRespaldoScreen(usuario = usuario, salaActiva = null, onRestaurado = {}, onRegresar = {}) } }
        rule.waitUntil(15_000) { rule.onAllNodes(hasTestTag("eliminar_todas_fichas")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("eliminar_todas_fichas").performScrollTo().performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("confirmar_borrado_total")).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(hasText("Se eliminarán las 5 ficha(s)", substring = true)).fetchSemanticsNodes().let { assertEquals(1, it.size) }

        // sin la palabra no se borra nada
        rule.onNodeWithTag("confirmar_borrado_total").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Escribe la palabra ELIMINAR", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(5, contar("fichas_familiares"))

        rule.onNodeWithTag("texto_confirmar_borrado").performTextInput("ELIMINAR")
        rule.onNodeWithTag("confirmar_borrado_total").performClick()
        rule.waitUntil(15_000) { contar("fichas_familiares") == 0 }
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("Se eliminaron 5 ficha(s)", substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }
}
