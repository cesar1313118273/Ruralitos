package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import com.ruralitos.app.domain.MapaSeguimiento
import com.ruralitos.app.ui.screens.AgendaScreen
import com.ruralitos.app.ui.screens.EstadoMapaSeguimiento
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
import kotlin.math.abs

/**
 * Confirmar que una visita ya se realizó (desde Seguimiento y desde el Mapa de visitas): queda COMPLETADA con la fecha
 * real, el resto de pendientes de la ficha se reemplaza por las próximas visitas calculadas desde esa fecha, y el
 * botón + de agendar está en Seguimiento y en Agenda pero no en el mapa.
 */
@RunWith(AndroidJUnit4::class)
class AgendaConfirmarVisitaTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }
    private lateinit var ids: Map<String, Long>
    private val usuario = 1L

    @Before
    fun sembrar() {
        runCatching {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission("com.ruralitos.app", "android.permission.POST_NOTIFICATIONS")
        }
        ids = runBlocking { DatosMapaParlante.sembrar(database) }
    }

    @After
    fun limpiar() {
        database.openHelper.writableDatabase.execSQL(
            "DELETE FROM actividades_agenda WHERE fichaId IN (SELECT id FROM fichas_familiares WHERE numeroFichaFamiliar LIKE '${DatosMapaParlante.PREFIJO}%')"
        )
        DatosMapaParlante.limpiar(database)
    }

    private fun existe(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    private fun esperar(condicion: () -> Boolean) = rule.waitUntil(30_000, condicion)

    private fun pendientes(fichas: Collection<Long>): List<ActividadAgendaEntity> = runBlocking {
        database.agendaDao().observar(usuario).first()
            .filter { it.origen == "SEGUIMIENTO" && it.estado == "PENDIENTE" && it.fichaId in fichas }
            .sortedWith(compareBy({ it.fechaHora }, { it.id }))
    }

    private fun comprobarRegistro(visita: ActividadAgendaEntity) {
        val fichaId = visita.fichaId!!
        esperar { runBlocking { database.agendaDao().buscar(visita.id) }?.estado == "COMPLETADA" }
        val despues = runBlocking { database.agendaDao().buscar(visita.id) }!!
        assertTrue("queda con la fecha real de la visita", abs(despues.fechaHora - System.currentTimeMillis()) < 180_000)
        assertEquals(despues.fechaHora, despues.fechaBase)
        val nuevas = runBlocking { database.agendaDao().seguimientosPendientes(fichaId, usuario) }
        assertTrue("se calcularon las próximas visitas", nuevas.isNotEmpty())
        assertTrue(
            "las próximas visitas parten de la fecha real y son futuras",
            nuevas.all { it.fechaBase == despues.fechaBase && it.fechaHora > System.currentTimeMillis() }
        )
        assertTrue("las pendientes anteriores se reemplazaron", nuevas.none { it.id == visita.id })
    }

    private val dia = 24 * 3600_000L
    private val hoy get() = MapaSeguimiento.inicioDia(System.currentTimeMillis())

    /** Toma una visita automática (la más temprana) de las fichas dadas y la deja en la fecha y el estado pedidos. */
    private fun prepararVisita(fichas: Collection<Long>, fecha: Long, confirmada: Boolean): ActividadAgendaEntity {
        esperar { pendientes(fichas).isNotEmpty() }
        val visita = pendientes(fichas).first().copy(fechaHora = fecha, fechaEditada = confirmada)
        runBlocking { database.agendaDao().actualizar(visita) }
        return visita
    }

    private fun mostrarMapa(visita: ActividadAgendaEntity): EstadoMapaSeguimiento {
        val estadoMapa = EstadoMapaSeguimiento().apply {
            desde = hoy - 30 * dia; hasta = hoy + 3650L * dia; fechasElegidas = true
        }
        rule.setContent {
            RuralitosTheme {
                AgendaScreen(usuarioId = usuario, organizacionId = "", onRegresar = {}, onAbrirFicha = {}, estadoMapa = estadoMapa, pestanaInicial = 2)
            }
        }
        rule.runOnIdle { estadoMapa.seleccionadaId = visita.fichaId }
        esperar { existe("ver_visita_mapa") }
        return estadoMapa
    }

    private val conUbicacion get() = listOf("c1", "c2", "c3", "e1").map { ids.getValue(it) }

    @Test
    fun elDiaDeLaVisitaProgramadaSeMarcaRealizadaDesdeLaAgenda() {
        // el evento ya corre con la pantalla abierta: se crea el plan y luego se deja una visita confirmada para hoy
        rule.setContent {
            RuralitosTheme { AgendaScreen(usuarioId = usuario, organizacionId = "", onRegresar = {}, onAbrirFicha = {}, pestanaInicial = 1) }
        }
        val visita = prepararVisita(ids.values, hoy + 12 * 3600_000L, confirmada = true)
        esperar { existe("tarjeta_actividad_${visita.id}") }
        assertTrue("el + de agendar está en la Agenda", existe("boton_agendar"))
        rule.onNodeWithTag("tarjeta_actividad_${visita.id}").performClick()
        esperar { existe("marcar_realizada") }
        rule.onNodeWithTag("marcar_realizada").performClick()
        esperar { existe("confirmar_registro_visita") }
        rule.onNodeWithTag("confirmar_registro_visita").performClick()
        comprobarRegistro(visita)
    }

    @Test
    fun elDiaDeLaVisitaProgramadaSeMarcaRealizadaDesdeElMapa() {
        rule.setContent {
            RuralitosTheme { AgendaScreen(usuarioId = usuario, organizacionId = "", onRegresar = {}, onAbrirFicha = {}) }
        }
        val visita = prepararVisita(conUbicacion, hoy + 12 * 3600_000L, confirmada = true)
        rule.activityRule.scenario.recreate()
        val estadoMapa = mostrarMapa(visita)
        assertTrue("en el mapa no hay botón de agendar ni de abrir ficha", !existe("boton_agendar") && !existe("agendar_mapa") && !existe("abrir_ficha_mapa"))
        rule.onNodeWithTag("ver_visita_mapa").performClick()
        esperar { existe("marcar_realizada") }
        rule.onNodeWithTag("marcar_realizada").performClick()
        esperar { existe("confirmar_registro_visita") }
        rule.onNodeWithTag("confirmar_registro_visita").performClick()
        comprobarRegistro(visita)
    }

    @Test
    fun fueraDelDiaOSinConfirmarElBotonNoAparece() {
        rule.setContent {
            RuralitosTheme { AgendaScreen(usuarioId = usuario, organizacionId = "", onRegresar = {}, onAbrirFicha = {}) }
        }
        var visita = prepararVisita(conUbicacion, hoy + dia + 12 * 3600_000L, confirmada = true) // mañana, confirmada
        rule.activityRule.scenario.recreate()
        mostrarMapa(visita)
        fun abrirYComprobar(motivo: String, confirmarFecha: Boolean) {
            rule.onNodeWithTag("ver_visita_mapa").performClick()
            esperar { rule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Cerrar")).fetchSemanticsNodes().isNotEmpty() }
            assertTrue(motivo, !existe("marcar_realizada"))
            assertEquals("el botón de programar visita $motivo", confirmarFecha, existe("programar_visita"))
            rule.onNode(androidx.compose.ui.test.hasContentDescription("Cerrar")).performClick()
            rule.waitForIdle()
        }
        abrirYComprobar("visita de mañana", confirmarFecha = false)

        visita = visita.copy(fechaHora = hoy - dia + 12 * 3600_000L, fechaEditada = true) // ayer, confirmada
        runBlocking { database.agendaDao().actualizar(visita) }
        rule.waitForIdle()
        abrirYComprobar("visita de ayer", confirmarFecha = false)

        visita = visita.copy(fechaHora = hoy + 12 * 3600_000L, fechaEditada = false) // hoy, sin confirmar
        runBlocking { database.agendaDao().actualizar(visita) }
        rule.waitForIdle()
        abrirYComprobar("visita de hoy sin confirmar", confirmarFecha = true)
    }
}
