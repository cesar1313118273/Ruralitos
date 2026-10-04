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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.local.dao.ViviendaMapaFila
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.domain.EstadoVisita
import com.ruralitos.app.domain.MapaSeguimiento
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Mapa de seguimiento con visitas de ejemplo en cada estado. Las visitas «de hoy» se ponen dentro del día de hoy (a
 * primera y a última hora) para que las pruebas no dependan de la hora en que corran.
 */
@RunWith(AndroidJUnit4::class)
class MapaSeguimientoUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }
    private val ids = mutableMapOf<String, Long>()
    private val visitas = mutableMapOf<String, Long>()
    private val dia = 24 * 3600_000L
    private val ahora = System.currentTimeMillis()
    private val hoy = MapaSeguimiento.inicioDia(ahora)

    private fun ficha(nombre: String, lat: Double?, lon: Double?) = FichaFamiliarEntity(
        cedulaJefeHogar = "09${UUID.randomUUID().toString().filter { it.isDigit() }.take(8).padEnd(8, '0')}",
        institucionSistema = "", unidadOperativa = "QA", codigoUo = "1", areaNumero = "1", codigoLocalizacion = "1",
        parroquiaCodigoLocalizacion = "1", cantonCodigoLocalizacion = "1", provinciaCodigoLocalizacion = "1",
        numeroFichaFamiliar = "QA-SEGUIMIENTO-${UUID.randomUUID()}", provincia = "P", canton = "C", parroquia = "R",
        sector = "S", manzana = "1", numeroFamilia = "1", direccionHabitualFamilia = "D", barrio = "San José",
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
        suspend fun nueva(clave: String, f: FichaFamiliarEntity) { ids[clave] = sync.guardarFichaRemota(f) }
        nueva("confirmada", ficha("PÉREZ LUIS", -0.2201, -78.5123))
        nueva("atrasada", ficha("GÓMEZ ANA", -0.2033, -78.4907))
        nueva("porconfirmar", ficha("TORRES JUAN", -0.2120, -78.5000))
        nueva("realizada", ficha("RUIZ MARÍA", -0.1900, -78.4800))
        nueva("sinubicacion", ficha("SIN PUNTO LUCÍA", null, null))
        nueva("sinvisitas", ficha("GARCÍA SOFÍA", -0.2300, -78.5200))
        nueva("fuera", ficha("FUERA DE RANGO", -0.2250, -78.5150))
        visitas["confirmada"] = agenda.crear(visita(ids.getValue("confirmada"), "PÉREZ LUIS", MapaSeguimiento.finDia(ahora) - 1_000))
        visitas["atrasada"] = agenda.crear(visita(ids.getValue("atrasada"), "GÓMEZ ANA", hoy + 500))
        visitas["porconfirmar"] = agenda.crear(visita(ids.getValue("porconfirmar"), "TORRES JUAN", hoy + 1_000, origen = "SEGUIMIENTO"))
        visitas["realizada"] = agenda.crear(visita(ids.getValue("realizada"), "RUIZ MARÍA", hoy + 2_000, estado = "COMPLETADA"))
        visitas["sinubicacion"] = agenda.crear(visita(ids.getValue("sinubicacion"), "SIN PUNTO LUCÍA", MapaSeguimiento.finDia(ahora) - 2_000))
        visitas["fuera"] = agenda.crear(visita(ids.getValue("fuera"), "FUERA DE RANGO", MapaSeguimiento.finDia(ahora) + 5 * dia))
    } }

    @After
    fun limpiar() {
        val db = database.openHelper.writableDatabase
        db.execSQL("DELETE FROM actividades_agenda WHERE fichaId IN (SELECT id FROM fichas_familiares WHERE numeroFichaFamiliar LIKE 'QA-SEGUIMIENTO-%')")
        db.execSQL("DELETE FROM fichas_familiares WHERE numeroFichaFamiliar LIKE 'QA-SEGUIMIENTO-%'")
    }

    private var grupoAbierto: List<ActividadAgendaEntity>? = null
    private var fichaAbierta: Long? = null
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
                        onAbrirVisita = { grupoAbierto = it },
                        onAbrirFicha = { fichaAbierta = it }, onUbicarFicha = { ubicada = it }
                    )
                }
            }
        }
        rule.waitUntil(15_000) { hayTag("boton_fecha") }
        if (clase != ClaseAncho.COMPACTA) rule.waitUntil(15_000) { hay("PÉREZ LUIS") }
    }

    private fun hay(texto: String) = rule.onAllNodes(hasText(texto)).fetchSemanticsNodes().isNotEmpty()
    private fun hayTag(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun fechaTexto(t: Long) = "%02d/%02d/%d".format(MapaSeguimiento.dia(t), MapaSeguimiento.mes(t), MapaSeguimiento.anio(t))

    /** Elige día, mes y año de la fecha inicio ("ini") o fin ("fin") en la ventana de fechas. */
    private fun elegirFecha(marca: String, t: Long) {
        val actual = if (marca == "ini") estadoMapa.desde else estadoMapa.hasta
        rule.onNodeWithTag("campo_fecha_$marca").performClick()
        rule.waitUntil(5_000) { hayTag("calendario_aceptar") }
        if (MapaSeguimiento.anio(t) != MapaSeguimiento.anio(actual)) {
            rule.onNodeWithTag("calendario_anio").performClick()
            rule.waitUntil(5_000) { hayTag("anio_${MapaSeguimiento.anio(t)}") }
            rule.onNodeWithTag("anio_${MapaSeguimiento.anio(t)}").performClick()
        }
        // el calendario abre en el mes de la fecha actual; se avanza o retrocede hasta el mes pedido
        val pasos = MapaSeguimiento.mes(t) - MapaSeguimiento.mes(actual)
        repeat(kotlin.math.abs(pasos)) { rule.onNodeWithTag(if (pasos > 0) "mes_siguiente" else "mes_anterior").performClick() }
        rule.onNodeWithTag("dia_${MapaSeguimiento.dia(t)}").performClick()
        rule.onNodeWithTag("calendario_aceptar").performClick()
        rule.waitUntil(5_000) { !hayTag("calendario_aceptar") }
    }

    @Test
    fun alEntrarElMapaMuestraSoloLasVisitasDeHoyConLasFechasDeHoy() {
        mostrar(ClaseAncho.EXPANDIDA)
        rule.onNodeWithText("${fechaTexto(hoy)} – ${fechaTexto(hoy)}").assertExists()
        rule.onNodeWithText("Estado · todos").assertExists()
        // hoy: las cuatro viviendas con visita; no la que tiene visita dentro de cinco días ni la que no tiene visitas
        listOf("PÉREZ LUIS", "GÓMEZ ANA", "TORRES JUAN", "RUIZ MARÍA").forEach { rule.onNodeWithText(it).assertExists() }
        rule.onNodeWithText("FUERA DE RANGO").assertDoesNotExist()
        rule.onNodeWithText("GARCÍA SOFÍA").assertDoesNotExist()
        // no hay búsqueda, ni barrio, ni riesgo
        assertFalse(hayTag("busqueda_mapa")); assertFalse(hayTag("chip_barrio")); assertFalse(hayTag("chip_riesgo"))
    }

    @Test
    fun elBotonEstadoOfreceLasCuatroOpcionesYSeMarcanVarias() {
        mostrar(ClaseAncho.EXPANDIDA)
        rule.onNodeWithTag("boton_estado").performClick()
        rule.waitUntil(5_000) { hayTag("estado_PENDIENTE") }
        listOf("PENDIENTE", "PROGRAMADA", "ATRASADA", "REALIZADA").forEach { assertTrue(hayTag("estado_$it")) }

        // solo realizadas: se quitan las otras tres; el menú sigue abierto
        listOf("PENDIENTE", "PROGRAMADA", "ATRASADA").forEach { rule.onNodeWithTag("estado_$it").performClick() }
        rule.waitUntil(5_000) { !hay("PÉREZ LUIS") && !hay("GÓMEZ ANA") && !hay("TORRES JUAN") }
        rule.onNodeWithText("RUIZ MARÍA").assertExists()
        // el último estado marcado no se puede quitar
        rule.onNodeWithTag("estado_REALIZADA").performClick()
        rule.waitForIdle()
        assertEquals(setOf(EstadoVisita.REALIZADA), estadoMapa.estados)
        // se vuelve a marcar una
        rule.onNodeWithTag("estado_ATRASADA").performClick()
        rule.waitUntil(5_000) { hay("GÓMEZ ANA") }
        rule.onNodeWithText("RUIZ MARÍA").assertExists()
        rule.onNodeWithText("TORRES JUAN").assertDoesNotExist()
    }

    @Test
    fun laVentanaDeFechasEligeDiaMesYAnioDeInicioYDeFin() {
        mostrar(ClaseAncho.EXPANDIDA)
        val enCincoDias = MapaSeguimiento.inicioDia(hoy + 5 * dia + 12 * 3600_000)
        rule.onNodeWithTag("boton_fecha").performClick()
        rule.waitUntil(5_000) { hayTag("aplicar_fechas") }
        elegirFecha("fin", enCincoDias)
        rule.onNodeWithTag("aplicar_fechas").performClick()
        rule.waitUntil(5_000) { hay("FUERA DE RANGO") }
        rule.onNodeWithText("${fechaTexto(hoy)} – ${fechaTexto(enCincoDias)}").assertExists()
        assertEquals(enCincoDias, estadoMapa.hasta)
        assertEquals(hoy, estadoMapa.desde)
        rule.onNodeWithText("PÉREZ LUIS").assertExists()

        // elegir como inicio una fecha posterior a las visitas de hoy deja solo la del futuro
        rule.onNodeWithTag("boton_fecha").performClick()
        rule.waitUntil(5_000) { hayTag("aplicar_fechas") }
        elegirFecha("ini", MapaSeguimiento.inicioDia(hoy + 3 * dia + 12 * 3600_000))
        rule.onNodeWithTag("aplicar_fechas").performClick()
        rule.waitUntil(5_000) { !hay("PÉREZ LUIS") }
        rule.onNodeWithText("FUERA DE RANGO").assertExists()
    }

    @Test
    fun unaFechaFinAnteriorAlInicioNoSeAplica() {
        mostrar(ClaseAncho.EXPANDIDA)
        rule.onNodeWithTag("boton_fecha").performClick()
        rule.waitUntil(5_000) { hayTag("aplicar_fechas") }
        elegirFecha("fin", MapaSeguimiento.inicioDia(hoy - 3 * dia + 12 * 3600_000))
        rule.onNodeWithTag("aplicar_fechas").performClick()
        rule.waitUntil(5_000) { hayTag("error_fechas") }
        rule.onNodeWithText("La fecha fin no puede ser anterior a la fecha inicio.").assertExists()
        assertEquals("el rango no cambió", hoy, estadoMapa.hasta)
        // «Hoy» deja las dos fechas en la fecha actual
        rule.onNodeWithTag("fechas_hoy").performClick()
        rule.waitUntil(5_000) { !hayTag("aplicar_fechas") }
        assertEquals(hoy, estadoMapa.desde); assertEquals(hoy, estadoMapa.hasta)
    }

    @Test
    fun laTarjetaSoloTraeVerVisitaYNoTraeAgendarNiRutaNiFichaNiLista() {
        mostrar(ClaseAncho.EXPANDIDA)
        rule.onNodeWithText("PÉREZ LUIS").performClick()
        rule.waitUntil(5_000) { hayTag("tarjeta_vivienda") }
        assertFalse("la tarjeta solo trae Ver visita", hayTag("abrir_ficha_mapa"))
        assertFalse("y ya no lista las actividades de la vivienda", hay("Visita domiciliaria"))
        rule.onNodeWithTag("ver_visita_mapa").performClick()
        assertEquals(listOf(visitas["confirmada"]), grupoAbierto?.map { it.id })
        // agendar solo existe en la Agenda: ni en la tarjeta ni como botón general del mapa
        assertFalse(hayTag("agendar_visita_mapa"))
        assertFalse(hayTag("agendar_mapa"))
        assertFalse("la ruta aparte se eliminó", hayTag("como_llegar_mapa"))
        assertFalse(hay("＋ Agendar"))
    }

    @Test
    fun lasFichasConVisitaPeroSinUbicacionSePuedenUbicarAhora() {
        mostrar(ClaseAncho.EXPANDIDA)
        rule.onNodeWithText("1 sin ubicación · ver").performClick()
        rule.waitUntil(5_000) { hay("SIN PUNTO LUCÍA") }
        rule.onNodeWithTag("ubicar_ahora").performClick()
        assertEquals(ids["sinubicacion"], ubicada)
    }

    @Test
    fun elRecorridoDelDiaOrdenaLasVisitasPorHacerYAvisaAlLlegar() {
        mostrar(ClaseAncho.EXPANDIDA)
        rule.onNodeWithTag("chip_recorrido").performClick()
        rule.waitUntil(5_000) { hay("Toca las viviendas que vas a visitar") }
        // «Elegir visibles» deja las visitas por hacer: programada, atrasada y pendiente (la realizada no)
        rule.onNodeWithTag("recorrido_visibles").performClick()
        rule.waitUntil(5_000) { hay("3 elegidas") }
        rule.onNodeWithTag("recorrido_ordenar").performClick()
        // la primera vez el motor copia la red vial del país, por eso la espera es larga
        rule.waitUntil(240_000) { hay("Recorrido de 3 viviendas") }
        rule.onNodeWithTag("resumen_recorrido").assertExists()
        assertFalse("el botón de ir a la ruta aparte se eliminó", hayTag("ir_primera_parada"))
        val primera = estadoMapa.plan!!.paradas.first().vivienda.fichaId

        // aviso de llegada: se simula que el GPS ya llegó a la primera parada
        rule.runOnIdle { estadoMapa.avisoLlegadaId = primera }
        rule.waitUntil(5_000) { hayTag("aviso_llegada") }
        rule.onNodeWithText("Llegaste a la vivienda").assertExists()
        rule.onNodeWithTag("registrar_llegada").performClick()
        assertTrue("registrar abre la visita de esa vivienda", grupoAbierto!!.all { it.fichaId == primera })
        rule.onNodeWithTag("siguiente_parada").performClick()
        rule.waitUntil(5_000) { !hayTag("aviso_llegada") }
        assertEquals(1, estadoMapa.paradaActual)
        // cerrar el recorrido ordenado devuelve la barra de elegir viviendas; su ✕ sale del modo
        rule.onNodeWithTag("cerrar_recorrido").performClick()
        rule.waitUntil(5_000) { hayTag("salir_recorrido") }
        rule.onNodeWithTag("salir_recorrido").performClick()
        rule.waitUntil(5_000) { !hay("Toca las viviendas que vas a visitar") }
        assertEquals(null, estadoMapa.plan)
    }

    @Test
    fun elRecorridoSeConservaAlSalirYVolver() {
        // El estado vive fuera de la pantalla: un recorrido hecho antes de abrir una ficha sigue ahí al volver.
        val vivienda = ViviendaMapaFila(1, "A", "1", "F1", "B", "1", -0.2, -78.5, "COMPLETA", "SINCRONIZADO", "", 1, 0)
        val actividad = visita(1, "A", ahora + 3600_000)
        val punto = PuntoSeguimiento(vivienda, listOf(actividad), EstadoVisita.PROGRAMADA, actividad)
        estadoMapa.plan = PlanRecorrido(listOf(punto), null, 1.0, null, "pedestrian", false)
        estadoMapa.paradaActual = 0
        estadoMapa.modoRecorrido = true
        mostrar(ClaseAncho.EXPANDIDA)
        rule.waitUntil(5_000) { hay("Recorrido de 1 viviendas") }
    }

    @Test
    fun tocarUnaViviendaEnElMapaAbreSuTarjeta() {
        mostrar(ClaseAncho.COMPACTA)
        // solo «confirmadas»: queda una vivienda a la vista, el mapa se centra en ella y tocar su centro la selecciona
        rule.onNodeWithTag("boton_estado").performClick()
        rule.waitUntil(5_000) { hayTag("estado_PENDIENTE") }
        listOf("PENDIENTE", "ATRASADA", "REALIZADA").forEach { rule.onNodeWithTag("estado_$it").performClick() }
        rule.waitUntil(5_000) { estadoMapa.estados == setOf(EstadoVisita.PROGRAMADA) }

        val instrumentacion = InstrumentationRegistry.getInstrumentation()
        fun tocar(x: Float, y: Float) {
            val t0 = android.os.SystemClock.uptimeMillis()
            instrumentacion.sendPointerSync(android.view.MotionEvent.obtain(t0, t0, android.view.MotionEvent.ACTION_DOWN, x, y, 0))
            instrumentacion.sendPointerSync(android.view.MotionEvent.obtain(t0, t0 + 80, android.view.MotionEvent.ACTION_UP, x, y, 0))
        }
        // un toque fuera del menú lo cierra (abajo, dentro de la ventana de la aplicación)
        val vista = rule.activity.window.decorView
        tocar(vista.width * 0.5f, vista.height * 0.9f)
        rule.waitUntil(5_000) { !hayTag("estado_PENDIENTE") }

        fun buscarMapa(v: android.view.View): org.maplibre.android.maps.MapView? =
            if (v is org.maplibre.android.maps.MapView) v
            else (v as? android.view.ViewGroup)?.let { g -> (0 until g.childCount).firstNotNullOfOrNull { buscarMapa(g.getChildAt(it)) } }
        val mapa = buscarMapa(rule.activity.window.decorView)!!
        var tarjeta = false
        repeat(6) {
            if (tarjeta) return@repeat
            Thread.sleep(2_500)
            val posicion = IntArray(2)
            mapa.getLocationInWindow(posicion)
            tocar((posicion[0] + mapa.width / 2).toFloat(), (posicion[1] + mapa.height / 2).toFloat())
            Thread.sleep(800)
            tarjeta = hay("Cómo llegar")
        }
        assertTrue("al tocar la vivienda debe abrirse su tarjeta", tarjeta)
        rule.onNodeWithText("PÉREZ LUIS").assertExists()
    }
}
