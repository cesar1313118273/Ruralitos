package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.ActividadAgendaEntity
import com.ruralitos.app.ui.screens.AgendaScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Herramienta de revisión visual: abre la agenda en la pestaña Mapa con visitas de ejemplo y deja
 * `seguimiento_<etiqueta>.png` en los archivos externos de la app. Se activa con `-e dejarAbierto 1`;
 * con `-e elegir cerezal` también toca la primera vivienda de la lista, con `-e recorrido 1` ordena un recorrido.
 */
@RunWith(AndroidJUnit4::class)
class MapaSeguimientoCapturaTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }

    @After
    fun limpiar() {
        database.openHelper.writableDatabase.execSQL("DELETE FROM actividades_agenda WHERE fichaId IN (SELECT id FROM fichas_familiares WHERE numeroFichaFamiliar LIKE '${DatosMapaParlante.PREFIJO}%')")
        DatosMapaParlante.limpiar(database)
    }

    @Test
    fun dejaElMapaDeSeguimientoAbiertoParaFotografiarlo() {
        val args = InstrumentationRegistry.getArguments()
        if (args.getString("dejarAbierto") != "1") return
        runCatching {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission("com.ruralitos.app", android.Manifest.permission.POST_NOTIFICATIONS)
        }
        val ahora = System.currentTimeMillis()
        val hora = 3600_000L
        runBlocking {
            val ids = DatosMapaParlante.sembrar(database)
            val agenda = database.agendaDao()
            fun v(clave: String, nombre: String, fecha: Long, estado: String = "PENDIENTE", origen: String = "MANUAL", editada: Boolean = false) =
                ActividadAgendaEntity(
                    usuarioId = 1, fichaId = ids.getValue(clave), persona = nombre, barrio = "Cerezal", fechaHora = fecha,
                    tipo = "Visita domiciliaria", estado = estado, origen = origen, fechaEditada = editada
                )
            agenda.crear(v("c1", "RAMÓN LUIS", ahora + 24 * hora))
            agenda.crear(v("c2", "TORRES ANA", ahora - 48 * hora))
            agenda.crear(v("c3", "PÉREZ JUAN", ahora + 3 * hora, origen = "SEGUIMIENTO"))
            agenda.crear(v("e1", "GÓMEZ PEDRO", ahora - 72 * hora, estado = "COMPLETADA"))
        }
        rule.setContent {
            RuralitosTheme {
                AgendaScreen(usuarioId = 1, organizacionId = "", onRegresar = {}, onAbrirFicha = {}, onAbrirRuta = {}, pestanaInicial = 2)
            }
        }
        rule.waitUntil(30_000) { rule.onAllNodes(hasTestTag("mapa_viviendas")).fetchSemanticsNodes().isNotEmpty() }
        args.getString("elegir")?.let {
            rule.onNodeWithTag("chip_estado_ATRASADAS").performScrollTo().performClick()
        }
        if (args.getString("recorrido") == "1") {
            rule.onNodeWithTag("chip_recorrido").performScrollTo().performClick()
            rule.waitUntil(10_000) { rule.onAllNodes(hasTestTag("recorrido_visibles")).fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("recorrido_visibles").performScrollTo().performClick()
            rule.waitUntil(10_000) { rule.onAllNodes(androidx.compose.ui.test.hasText("elegidas", substring = true)).fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("recorrido_ordenar").performClick()
            rule.waitUntil(240_000) { rule.onAllNodes(hasTestTag("tarjeta_recorrido")).fetchSemanticsNodes().isNotEmpty() }
        }
        repeat(((args.getString("espera")?.toLongOrNull() ?: 10L) * 2).toInt()) {
            rule.waitForIdle()
            Thread.sleep(500)
        }
        val etiqueta = args.getString("etiqueta") ?: "mapa"
        fun buscarMapa(v: android.view.View): org.maplibre.android.maps.MapView? =
            if (v is org.maplibre.android.maps.MapView) v
            else (v as? android.view.ViewGroup)?.let { g -> (0 until g.childCount).firstNotNullOfOrNull { buscarMapa(g.getChildAt(it)) } }
        val vistaMapa = buscarMapa(rule.activity.window.decorView)
        val listo = java.util.concurrent.CountDownLatch(1)
        rule.activity.runOnUiThread {
            vistaMapa?.getMapAsync { m ->
                m.snapshot { bmp ->
                    java.io.File(rule.activity.getExternalFilesDir(null), "seguimiento_solo_$etiqueta.png")
                        .outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it) }
                    listo.countDown()
                }
            } ?: listo.countDown()
        }
        listo.await(20, java.util.concurrent.TimeUnit.SECONDS)
        val captura = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(rule.activity.getExternalFilesDir(null), "seguimiento_$etiqueta.png").outputStream().use {
            captura.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it)
        }
    }
}
