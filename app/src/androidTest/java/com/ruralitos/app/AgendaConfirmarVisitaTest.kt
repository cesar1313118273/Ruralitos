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

    @Test
    fun marcarRealizadaDesdeSeguimientoGuardaLaVisitaYCalculaLasProximas() {
        rule.setContent {
            RuralitosTheme { AgendaScreen(usuarioId = usuario, organizacionId = "", onRegresar = {}, onAbrirFicha = {}) }
        }
        esperar { existe("boton_agendar") && pendientes(ids.values).isNotEmpty() }
        val visita = pendientes(ids.values).first()
        esperar { existe("tarjeta_actividad_${visita.id}") }
        rule.onNodeWithTag("tarjeta_actividad_${visita.id}").performClick()
        esperar { existe("marcar_realizada") }
        rule.onNodeWithTag("marcar_realizada").performClick()
        esperar { existe("confirmar_registro_visita") }
        rule.onNodeWithTag("confirmar_registro_visita").performClick()
        comprobarRegistro(visita)
    }

    @Test
    fun marcarRealizadaDesdeElMapaDeVisitasGuardaLaVisita() {
        val conUbicacion = listOf("c1", "c2", "c3", "e1").map { ids.getValue(it) }
        val estadoMapa = EstadoMapaSeguimiento().apply {
            val hoy = MapaSeguimiento.inicioDia(System.currentTimeMillis())
            desde = hoy; hasta = hoy + 3650L * 24 * 3600_000; fechasElegidas = true
        }
        rule.setContent {
            RuralitosTheme {
                AgendaScreen(usuarioId = usuario, organizacionId = "", onRegresar = {}, onAbrirFicha = {}, estadoMapa = estadoMapa, pestanaInicial = 2)
            }
        }
        esperar { pendientes(conUbicacion).isNotEmpty() }
        val visita = pendientes(conUbicacion).first()
        rule.runOnIdle { estadoMapa.seleccionadaId = visita.fichaId }
        esperar { existe("ver_visita_mapa") }
        assertTrue("en el mapa no hay botón de agendar", !existe("boton_agendar") && !existe("agendar_mapa"))
        rule.onNodeWithTag("ver_visita_mapa").performClick()
        esperar { existe("marcar_realizada") }
        rule.onNodeWithTag("marcar_realizada").performClick()
        esperar { existe("confirmar_registro_visita") }
        rule.onNodeWithTag("confirmar_registro_visita").performClick()
        comprobarRegistro(visita)
    }
}
