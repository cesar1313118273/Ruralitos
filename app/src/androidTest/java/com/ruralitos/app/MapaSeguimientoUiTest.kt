package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.local.dao.ViviendaMapaFila
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.domain.EstadoVisita
import com.ruralitos.app.domain.PuntoSeguimiento
import com.ruralitos.app.ui.components.ClaseAncho
import com.ruralitos.app.ui.components.LocalClaseAncho
import com.ruralitos.app.ui.screens.EstadoMapaSeguimiento
import com.ruralitos.app.ui.screens.MapaSeguimientoVista
import com.ruralitos.app.ui.screens.PlanRecorrido
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Mapa de seguimiento con visitas de ejemplo en cada estado: contadores, filtros, tarjeta, recorrido y aviso de llegada. */
@RunWith(AndroidJUnit4::class)
class MapaSeguimientoUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }
    private val ids = mutableMapOf<String, Long>()
    private val visitas = mutableMapOf<String, Long>()
    private val hora = 3600_000L
    private val dia = 24 * hora

    private fun ficha(nombre: String, lat: Double?, lon: Double?, barrio: String = "San José") = FichaFamiliarEntity(
        cedulaJefeHogar = "09${UUID.randomUUID().toString().filter { it.isDigit() }.take(8).padEnd(8, '0')}",
        institucionSistema = "", unidadOperativa = "QA", codigoUo = "1", areaNumero = "1", codigoLocalizacion = "1",
        parroquiaCodigoLocalizacion = "1", cantonCodigoLocalizacion = "1", provinciaCodigoLocalizacion = "1",
        numeroFichaFamiliar = "QA-SEGUIMIENTO-${UUID.randomUUID()}", provincia = "P", canton = "C", parroquia = "R",
        sector = "S", manzana = "1", numeroFamilia = "1", direccionHabitualFamilia = "D", barrio = barrio,
        numeroCasa = "12", comunidad = "C", grupoCultural = "MESTIZO", nombreApellidoJefeFamilia = nombre,
        numeroTelefono = "0", fechaLlenado = "01/10/2026", numeroCarpeta = "1", responsableNombre = "QA",
        responsableCodigo = "1", latitud = lat, longitud = lon, estado = "COMPLETA", syncEstado = "SINCRONIZADO"
    )

    private fun visita(ficha: Long, nombre: String, fecha: Long, estado: String = "PENDIENTE", origen: String = "MANUAL", editada: Boolean = false) =
        ActividadAgendaEntity(
            usuarioId = 1, fichaId = ficha, persona = nombre, barrio = "San José", fechaHora = fecha,
            tipo = "Visita domiciliaria", estado = estado, origen = origen, fechaEditada = editada
        )

    @Before
    fun sembrar() { runBlocking {
        val sync = database.sincronizacionDao()
        val agenda = database.agendaDao()
        val ahora = System.currentTimeMillis()
        suspend fun nueva(clave: String, f: FichaFamiliarEntity) { ids[clave] = sync.guardarFichaRemota(f) }
        nueva("confirmada", ficha("PÉREZ LUIS", -0.2201, -78.5123))
        nueva("atrasada", ficha("GÓMEZ ANA", -0.2033, -78.4907))
        nueva("porconfirmar", ficha("TORRES JUAN", -0.2120, -78.5000, barrio = "El Carmen"))
        nueva("realizada", ficha("RUIZ MARÍA", -0.1900, -78.4800))
        nueva("sinubicacion", ficha("SIN PUNTO LUCÍA", null, null))
        nueva("sinvisitas", ficha("GARCÍA SOFÍA", -0.2300, -78.5200))
        visitas["confirmada"] = agenda.crear(visita(ids.getValue("confirmada"), "PÉREZ LUIS", ahora + dia))
        visitas["atrasada"] = agenda.crear(visita(ids.getValue("atrasada"), "GÓMEZ ANA", ahora - 2 * dia))
        visitas["porconfirmar"] = agenda.crear(visita(ids.getValue("porconfirmar"), "TORRES JUAN", ahora + 3 * hora, origen = "SEGUIMIENTO"))
        visitas["realizada"] = agenda.crear(visita(ids.getValue("realizada"), "RUIZ MARÍA", ahora - 3 * dia, estado = "COMPLETADA"))
        visitas["sinubicacion"] = agenda.crear(visita(ids.getValue("sinubicacion"), "SIN PUNTO LUCÍA", ahora + dia))
    } }

    @After
    fun limpiar() {
        val db = database.openHelper.writableDatabase
        db.execSQL("DELETE FROM actividades_agenda WHERE fichaId IN (SELECT id FROM fichas_familiares WHERE numeroFichaFamiliar LIKE 'QA-SEGUIMIENTO-%')")
        db.execSQL("DELETE FROM fichas_familiares WHERE numeroFichaFamiliar LIKE 'QA-SEGUIMIENTO-%'")
    }

    private var grupoAbierto: List<ActividadAgendaEntity>? = null
    private var fichaAbierta: Long? = null
    private var rutaAbierta: Long? = null
    private var agendarFicha: Long? = -1L
    private var ubicada: Long? = null
    private val estadoMapa = EstadoMapaSeguimiento()

    private fun mostrar(clase: ClaseAncho, horizontal: Boolean = true) {
        rule.activityRule.scenario.onActivity {
            it.requestedOrientation = if (horizontal) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        rule.waitUntil(10_000) {
            (rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) == horizontal
        }
        rule.setContent {
            RuralitosTheme {
                CompositionLocalProvider(LocalClaseAncho provides clase) {
                    val actividades by database.agendaDao().observar(1).collectAsState(initial = emptyList())
                    MapaSeguimientoVista(
                        usuarioId = 1, actividades = actividades, ahora = System.currentTimeMillis(), estado = estadoMapa,
                        onAbrirVisita = { grupoAbierto = it }, onAgendar = { agendarFicha = it },
                        onAbrirFicha = { fichaAbierta = it }, onAbrirRuta = { rutaAbierta = it }, onUbicarFicha = { ubicada = it }
                    )
                }
            }
        }
        rule.waitUntil(15_000) { rule.onAllNodes(hasText("Todas · 4")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun escribirBusqueda(texto: String) {
        rule.onNodeWithTag("busqueda_mapa").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetText) {
            it(androidx.compose.ui.text.AnnotatedString(texto))
        }
    }

    @Test
    fun cadaEstadoSeFiltraYSeCuentaPorSeparado() {
        mostrar(ClaseAncho.EXPANDIDA)
        rule.onNodeWithText("Por confirmar · 1").assertExists()
        rule.onNodeWithText("Confirmadas · 1").assertExists()
        rule.onNodeWithText("Atrasadas · 1").assertExists()
        rule.onNodeWithText("Realizadas · 1").assertExists()
        // la vivienda sin visitas no aparece
        rule.onNodeWithText("GARCÍA SOFÍA").assertDoesNotExist()

        // «todos los atendidos»: solo la vivienda con visita realizada
        rule.onNodeWithTag("chip_estado_REALIZADAS").performScrollTo().performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("PÉREZ LUIS")).fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("RUIZ MARÍA").assertExists()

        rule.onNodeWithTag("chip_estado_ATRASADAS").performScrollTo().performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("GÓMEZ ANA")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("RUIZ MARÍA").assertDoesNotExist()

        rule.onNodeWithTag("chip_estado_POR_CONFIRMAR").performScrollTo().performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("TORRES JUAN")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("GÓMEZ ANA").assertDoesNotExist()

        rule.onNodeWithTag("chip_estado_CONFIRMADAS").performScrollTo().performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("PÉREZ LUIS")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("TORRES JUAN").assertDoesNotExist()
    }

    @Test
    fun elPeriodoHoyTrabajaConLasVisitasDeHoyYLasAtrasadas() {
        mostrar(ClaseAncho.EXPANDIDA)
        rule.onNodeWithTag("chip_periodo_HOY").performScrollTo().performClick()
        // hoy: la de seguimiento por confirmar (en 3 horas) y la atrasada; la de mañana y la realizada no
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("PÉREZ LUIS")).fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("GÓMEZ ANA").assertExists()
        rule.onNodeWithText("RUIZ MARÍA").assertDoesNotExist()
    }

    @Test
    fun laBusquedaYElBarrioReducenLasViviendas() {
        mostrar(ClaseAncho.EXPANDIDA)
        escribirBusqueda("perez")
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("GÓMEZ ANA")).fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("PÉREZ LUIS").assertExists()
        escribirBusqueda("")
        rule.onNodeWithTag("chip_barrio").performScrollTo().performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("El Carmen (1)")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("El Carmen (1)").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("PÉREZ LUIS")).fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("TORRES JUAN").assertExists()
    }

    @Test
    fun laTarjetaAbreFichaRutaVisitaYAgenda() {
        mostrar(ClaseAncho.EXPANDIDA)
        rule.onNodeWithText("PÉREZ LUIS").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("tarjeta_vivienda")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("abrir_ficha_mapa").performClick()
        assertEquals(ids["confirmada"], fichaAbierta)
        rule.onNodeWithTag("como_llegar_mapa").performClick()
        assertEquals(ids["confirmada"], rutaAbierta)
        rule.onNodeWithTag("ver_visita_mapa").performClick()
        assertEquals(listOf(visitas["confirmada"]), grupoAbierto?.map { it.id })
        rule.onNodeWithTag("agendar_visita_mapa").performClick()
        assertEquals(ids["confirmada"], agendarFicha)
        // el botón general de agendar no lleva ninguna ficha
        rule.onNodeWithTag("agendar_mapa").performClick()
        assertEquals(null, agendarFicha)
    }

    @Test
    fun lasFichasConVisitaPeroSinUbicacionSePuedenUbicarAhora() {
        mostrar(ClaseAncho.EXPANDIDA)
        rule.onNodeWithText("1 sin ubicación · ver").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("SIN PUNTO LUCÍA")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("ubicar_ahora").performClick()
        assertEquals(ids["sinubicacion"], ubicada)
    }

    @Test
    fun elRecorridoDelDiaOrdenaLasVisitasPorHacerYAvisaAlLlegar() {
        mostrar(ClaseAncho.EXPANDIDA)
        rule.onNodeWithTag("chip_recorrido").performScrollTo().performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Toca las viviendas que vas a visitar")).fetchSemanticsNodes().isNotEmpty() }
        // «Elegir visibles» deja las visitas por hacer: confirmada, atrasada y por confirmar (la realizada no)
        rule.onNodeWithTag("recorrido_visibles").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("3 elegidas")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("recorrido_ordenar").performClick()
        // la primera vez el motor copia la red vial del país, por eso la espera es larga
        rule.waitUntil(240_000) { rule.onAllNodes(hasText("Recorrido de 3 viviendas")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("resumen_recorrido").assertExists()
        rule.onNodeWithTag("ir_primera_parada").performClick()
        val primera = estadoMapa.plan!!.paradas.first().vivienda.fichaId
        assertEquals(primera, rutaAbierta)

        // aviso de llegada: se simula que el GPS ya llegó a la primera parada
        rule.runOnIdle { estadoMapa.avisoLlegadaId = primera }
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("aviso_llegada")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Llegaste a la vivienda").assertExists()
        rule.onNodeWithTag("registrar_llegada").performClick()
        assertTrue("registrar abre la visita de esa vivienda", grupoAbierto!!.all { it.fichaId == primera })
        rule.onNodeWithTag("siguiente_parada").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("aviso_llegada")).fetchSemanticsNodes().isEmpty() }
        assertEquals(1, estadoMapa.paradaActual)
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Ir a la parada 2")).fetchSemanticsNodes().isNotEmpty() }
        // cerrar el recorrido ordenado devuelve la barra de elegir viviendas; su ✕ sale del modo
        rule.onNodeWithTag("cerrar_recorrido").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("salir_recorrido")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("salir_recorrido").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Toca las viviendas que vas a visitar")).fetchSemanticsNodes().isEmpty() }
        assertEquals(null, estadoMapa.plan)
    }

    @Test
    fun elRecorridoSeConservaAlSalirYVolver() {
        // El estado vive fuera de la pantalla: un recorrido hecho antes de abrir una ficha sigue ahí al volver.
        val vivienda = ViviendaMapaFila(1, "A", "1", "F1", "B", "1", -0.2, -78.5, "COMPLETA", "SINCRONIZADO", "", 1, 0)
        val actividad = visita(1, "A", System.currentTimeMillis() + hora)
        val punto = PuntoSeguimiento(vivienda, listOf(actividad), EstadoVisita.CONFIRMADA, actividad)
        estadoMapa.plan = PlanRecorrido(listOf(punto), null, 1.0, null, "pedestrian", false)
        estadoMapa.paradaActual = 0
        estadoMapa.modoRecorrido = true
        mostrar(ClaseAncho.EXPANDIDA)
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Recorrido de 1 viviendas")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun tocarUnaViviendaEnElMapaAbreSuTarjeta() {
        mostrar(ClaseAncho.COMPACTA)
        // con una sola vivienda a la vista el mapa se centra en ella: tocar el centro del mapa la selecciona
        escribirBusqueda("perez")
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Confirmadas · 1")).fetchSemanticsNodes().isNotEmpty() && rule.onAllNodes(hasText("Atrasadas · 0")).fetchSemanticsNodes().isNotEmpty() }

        fun buscarMapa(v: android.view.View): org.maplibre.android.maps.MapView? =
            if (v is org.maplibre.android.maps.MapView) v
            else (v as? android.view.ViewGroup)?.let { g -> (0 until g.childCount).firstNotNullOfOrNull { buscarMapa(g.getChildAt(it)) } }
        val mapa = buscarMapa(rule.activity.window.decorView)!!
        val instrumentacion = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        var tarjeta = false
        repeat(6) {
            if (tarjeta) return@repeat
            Thread.sleep(2_500)
            val posicion = IntArray(2)
            mapa.getLocationInWindow(posicion)
            val x = (posicion[0] + mapa.width / 2).toFloat()
            val y = (posicion[1] + mapa.height / 2).toFloat()
            val t0 = android.os.SystemClock.uptimeMillis()
            instrumentacion.sendPointerSync(android.view.MotionEvent.obtain(t0, t0, android.view.MotionEvent.ACTION_DOWN, x, y, 0))
            instrumentacion.sendPointerSync(android.view.MotionEvent.obtain(t0, t0 + 80, android.view.MotionEvent.ACTION_UP, x, y, 0))
            Thread.sleep(800)
            tarjeta = rule.onAllNodes(hasText("Cómo llegar")).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue("al tocar la vivienda debe abrirse su tarjeta", tarjeta)
        rule.onNodeWithText("PÉREZ LUIS").assertExists()
    }
}
