package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.CalificacionRiesgoEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.ui.components.ClaseAncho
import com.ruralitos.app.ui.components.LocalClaseAncho
import com.ruralitos.app.ui.screens.MapaViviendasScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Mapa general de viviendas con fichas de ejemplo: contadores, filtros, búsqueda, tarjeta y fichas sin ubicación. */
@RunWith(AndroidJUnit4::class)
class MapaViviendasUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }
    private val ids = mutableMapOf<String, Long>()

    /** Escribe sin pasar por el teclado del emulador (que puede mostrar avisos propios y tragarse el texto). */
    private fun escribirBusqueda(texto: String) {
        rule.onNodeWithTag("busqueda_mapa").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetText) {
            it(androidx.compose.ui.text.AnnotatedString(texto))
        }
    }

    private fun ficha(nombre: String, lat: Double?, lon: Double?, estado: String, sync: String, barrio: String = "San José") = FichaFamiliarEntity(
        cedulaJefeHogar = "09${UUID.randomUUID().toString().filter { it.isDigit() }.take(8).padEnd(8, '0')}",
        institucionSistema = "", unidadOperativa = "QA", codigoUo = "1", areaNumero = "1", codigoLocalizacion = "1",
        parroquiaCodigoLocalizacion = "1", cantonCodigoLocalizacion = "1", provinciaCodigoLocalizacion = "1",
        numeroFichaFamiliar = "QA-MAPA-${UUID.randomUUID()}", provincia = "P", canton = "C", parroquia = "R",
        sector = "S", manzana = "1", numeroFamilia = "1", direccionHabitualFamilia = "D", barrio = barrio,
        numeroCasa = "12", comunidad = "C", grupoCultural = "MESTIZO", nombreApellidoJefeFamilia = nombre,
        numeroTelefono = "0", fechaLlenado = "01/10/2026", numeroCarpeta = "1", responsableNombre = "QA",
        responsableCodigo = "1", latitud = lat, longitud = lon, estado = estado, syncEstado = sync
    )

    @Before
    fun sembrar() { runBlocking {
        val sync = database.sincronizacionDao()
        suspend fun nueva(clave: String, f: FichaFamiliarEntity) { ids[clave] = sync.guardarFichaRemota(f) }
        nueva("aldia", ficha("PÉREZ LUIS", -0.2201, -78.5123, "COMPLETA", "SINCRONIZADO"))
        nueva("riesgo", ficha("GÓMEZ ANA", -0.2033, -78.4907, "COMPLETA", "SINCRONIZADO"))
        nueva("borrador", ficha("TORRES JUAN", -0.2120, -78.5000, "BORRADOR", "SINCRONIZADO", barrio = "El Carmen"))
        nueva("sinsync", ficha("RUIZ MARÍA", -0.1900, -78.4800, "COMPLETA", "PENDIENTE"))
        nueva("sinubicacion", ficha("SIN PUNTO LUCÍA", null, null, "BORRADOR", "SINCRONIZADO"))
        database.fichaContenidoDao().guardarCalificacion(
            CalificacionRiesgoEntity(fichaId = ids.getValue("riesgo"), fechaCalificacion = "01/10/2026", responsable = "QA", total = 40, nivel = "ALTO")
        )
        // la calificación marca la ficha como pendiente; aquí interesa su riesgo, no su sincronización
        sync.marcarSincronizadaDescarga(ids.getValue("riesgo"), 1)
    } }

    @After
    fun limpiar() {
        database.openHelper.writableDatabase.execSQL("DELETE FROM fichas_familiares WHERE numeroFichaFamiliar LIKE 'QA-MAPA-%'")
    }

    @Test
    fun muestraContadoresFiltrosBusquedaTarjetaYFichasSinUbicacion() {
        var fichaAbierta: Long? = null
        var rutaAbierta: Long? = null
        rule.activityRule.scenario.onActivity {
            it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        rule.waitUntil(10_000) {
            rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        }
        rule.setContent {
            RuralitosTheme {
                CompositionLocalProvider(LocalClaseAncho provides ClaseAncho.EXPANDIDA) {
                    MapaViviendasScreen(usuarioId = 1, onRegresar = {}, onAbrirFicha = { fichaAbierta = it }, onAbrirRuta = { rutaAbierta = it })
                }
            }
        }
        rule.waitUntil(15_000) { rule.onAllNodes(hasText("4 viviendas con ubicación")).fetchSemanticsNodes().isNotEmpty() }

        // contadores por filtro
        rule.onNodeWithText("Todas · 4").assertExists()
        rule.onNodeWithText("Pendientes · 1").assertExists()
        rule.onNodeWithText("Riesgo alto · 1").assertExists()
        rule.onNodeWithText("Sin sincronizar · 1").assertExists()

        // el filtro deja solo las de riesgo alto
        rule.onNodeWithText("Riesgo alto · 1").performScrollTo().performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("1 de 4 viviendas")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("GÓMEZ ANA").assertExists()
        rule.onNodeWithText("Todas · 4").performScrollTo().performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("4 viviendas con ubicación")).fetchSemanticsNodes().isNotEmpty() }

        // búsqueda sin tildes ni mayúsculas
        escribirBusqueda("perez")
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("1 de 4 viviendas")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("PÉREZ LUIS").assertExists()

        // tocar la fila abre la tarjeta con sus acciones
        rule.onNodeWithText("PÉREZ LUIS").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Cómo llegar")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("abrir_ficha_mapa").performClick()
        rule.onNodeWithTag("como_llegar_mapa").performClick()
        rule.waitUntil(5_000) { fichaAbierta != null }
        assertEquals(ids["aldia"], fichaAbierta)
        rule.waitUntil(5_000) { rutaAbierta != null }
        assertEquals(ids["aldia"], rutaAbierta)

        // la ficha sin punto no aparece en el mapa, pero se puede encontrar
        rule.onNodeWithText("1 sin ubicación · ver").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("SIN PUNTO LUCÍA")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("SIN PUNTO LUCÍA").performClick()
        assertEquals(ids["sinubicacion"], fichaAbierta)
    }

    @Test
    fun tocarUnaViviendaEnElMapaAbreSuTarjeta() {
        rule.activityRule.scenario.onActivity {
            it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        rule.waitUntil(10_000) {
            rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        }
        rule.setContent {
            RuralitosTheme {
                CompositionLocalProvider(LocalClaseAncho provides ClaseAncho.COMPACTA) {
                    MapaViviendasScreen(usuarioId = 1, onRegresar = {}, onAbrirFicha = {}, onAbrirRuta = {})
                }
            }
        }
        rule.waitUntil(15_000) { rule.onAllNodes(hasText("4 viviendas con ubicación")).fetchSemanticsNodes().isNotEmpty() }
        // con una sola vivienda a la vista el mapa se centra en ella: tocar el centro del mapa la selecciona
        escribirBusqueda("perez")
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("1 de 4 viviendas")).fetchSemanticsNodes().isNotEmpty() }

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
        org.junit.Assert.assertTrue("al tocar la vivienda debe abrirse su tarjeta", tarjeta)
        rule.onNodeWithText("PÉREZ LUIS").assertExists()
    }

    private fun mostrar(onFicha: (Long) -> Unit = {}, onUbicar: (Long) -> Unit = {}) {
        rule.activityRule.scenario.onActivity {
            it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        rule.waitUntil(10_000) {
            rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        }
        rule.setContent {
            RuralitosTheme {
                CompositionLocalProvider(LocalClaseAncho provides ClaseAncho.EXPANDIDA) {
                    MapaViviendasScreen(usuarioId = 1, onRegresar = {}, onAbrirFicha = onFicha, onAbrirRuta = {}, onUbicarFicha = onUbicar)
                }
            }
        }
        rule.waitUntil(15_000) { rule.onAllNodes(hasText("4 viviendas con ubicación")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun filtraPorBarrioYMuestraGruposDeRiesgo() {
        mostrar()
        rule.onNodeWithTag("chip_barrio").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("El Carmen (1)")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("El Carmen (1)").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("1 de 4 viviendas")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("TORRES JUAN").assertExists()
        rule.onNodeWithText("Gestantes · 0").performScrollTo().assertExists()
        rule.onNodeWithText("Menores de 5 · 0").assertExists()
        rule.onNodeWithText("Mayores de 65 · 0").assertExists()
    }

    @Test
    fun elRecorridoDelDiaOrdenaLasViviendasElegidas() {
        mostrar()
        rule.onNodeWithTag("chip_recorrido").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Toca las viviendas que vas a visitar")).fetchSemanticsNodes().isNotEmpty() }
        // con menos de dos elegidas no se puede ordenar
        rule.onNodeWithText("PÉREZ LUIS").performClick()
        rule.onNodeWithText("GÓMEZ ANA").performClick()
        rule.onNodeWithText("TORRES JUAN").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("3 elegidas")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("recorrido_ordenar").performClick()
        // la primera vez el motor copia la red vial del país, por eso la espera es larga
        rule.waitUntil(240_000) { rule.onAllNodes(hasText("Recorrido de 3 viviendas")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("resumen_recorrido").assertExists()
        rule.onNodeWithTag("ir_primera_parada").assertExists()
        // salir del modo deja el mapa como estaba
        rule.onNodeWithTag("salir_recorrido").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Toca las viviendas que vas a visitar")).fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun ubicarAhoraLlevaAlCroquisDeLaFicha() {
        var ubicada: Long? = null
        mostrar(onUbicar = { ubicada = it })
        rule.onNodeWithText("1 sin ubicación · ver").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("SIN PUNTO LUCÍA")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("ubicar_ahora").performClick()
        assertEquals(ids["sinubicacion"], ubicada)
    }
}
