package com.ruralitos.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.ui.screens.MapaViviendasScreen
import com.ruralitos.app.ui.theme.RuralitosTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.random.Random

/**
 * Herramienta de revisión visual: abre el mapa general con 45 viviendas de ejemplo y lo deja abierto unos segundos
 * para fotografiar la pantalla con `adb screencap`. Se activa con `-e dejarAbierto 1`; sin eso no hace nada.
 */
@RunWith(AndroidJUnit4::class)
class MapaViviendasCapturaTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val database by lazy { RuralitosDatabase.obtenerBaseDatos(rule.activity) }

    @After
    fun limpiar() {
        database.openHelper.writableDatabase.execSQL("DELETE FROM fichas_familiares WHERE numeroFichaFamiliar LIKE 'QA-CAPTURA-%'")
    }

    @Test
    fun dejaElMapaAbiertoParaFotografiarlo() {
        if (InstrumentationRegistry.getArguments().getString("dejarAbierto") != "1") return
        val azar = Random(7)
        runBlocking {
            val sync = database.sincronizacionDao()
            repeat(45) { n ->
                val agrupadas = n < 30
                val lat = if (agrupadas) -3.9930 + azar.nextDouble(-0.0035, 0.0035) else -3.9930 + azar.nextDouble(-0.02, 0.02)
                val lon = if (agrupadas) -79.2040 + azar.nextDouble(-0.0035, 0.0035) else -79.2040 + azar.nextDouble(-0.02, 0.02)
                val estado = if (n % 5 == 0) "BORRADOR" else "COMPLETA"
                val syncEstado = if (n % 11 == 0) "PENDIENTE" else "SINCRONIZADO"
                val id = sync.guardarFichaRemota(
                    FichaFamiliarEntity(
                        cedulaJefeHogar = "09%08d".format(n), institucionSistema = "", unidadOperativa = "QA", codigoUo = "1",
                        areaNumero = "1", codigoLocalizacion = "1", parroquiaCodigoLocalizacion = "1", cantonCodigoLocalizacion = "1",
                        provinciaCodigoLocalizacion = "1", numeroFichaFamiliar = "QA-CAPTURA-${UUID.randomUUID()}",
                        provincia = "Loja", canton = "Loja", parroquia = "Vilcabamba", sector = "S", manzana = "1", numeroFamilia = "1",
                        direccionHabitualFamilia = "D", barrio = if (n % 2 == 0) "San José" else "Centro", numeroCasa = "${n + 1}",
                        comunidad = "C", grupoCultural = "MESTIZO", nombreApellidoJefeFamilia = "FAMILIA EJEMPLO $n",
                        numeroTelefono = "0", fechaLlenado = "01/10/2026", numeroCarpeta = "1", responsableNombre = "QA",
                        responsableCodigo = "1", latitud = lat, longitud = lon, estado = estado, syncEstado = syncEstado
                    )
                )
                if (n % 7 == 0) {
                    database.fichaContenidoDao().guardarCalificacion(
                        com.ruralitos.app.data.local.entity.CalificacionRiesgoEntity(
                            fichaId = id, fechaCalificacion = "01/10/2026", responsable = "QA", total = 40, nivel = "ALTO"
                        )
                    )
                    sync.marcarSincronizadaDescarga(id, 1)
                }
            }
        }
        rule.activityRule.scenario.onActivity {
            it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        rule.setContent {
            RuralitosTheme { MapaViviendasScreen(usuarioId = 1, onRegresar = {}, onAbrirFicha = {}, onAbrirRuta = {}) }
        }
        Thread.sleep((InstrumentationRegistry.getArguments().getString("espera")?.toLongOrNull() ?: 14L) * 1000)
        val etiqueta = InstrumentationRegistry.getArguments().getString("etiqueta") ?: "mapa"
        // La captura de la pantalla completa puede salir negra en algunos emuladores; la del mapa sola siempre sirve.
        fun buscarMapa(v: android.view.View): org.maplibre.android.maps.MapView? =
            if (v is org.maplibre.android.maps.MapView) v
            else (v as? android.view.ViewGroup)?.let { g -> (0 until g.childCount).firstNotNullOfOrNull { buscarMapa(g.getChildAt(it)) } }
        val vistaMapa = buscarMapa(rule.activity.window.decorView)
        val listo = java.util.concurrent.CountDownLatch(1)
        rule.activity.runOnUiThread {
            vistaMapa?.getMapAsync { m ->
                m.snapshot { bmp ->
                    java.io.File(rule.activity.getExternalFilesDir(null), "mapa_solo_${InstrumentationRegistry.getArguments().getString("etiqueta") ?: "mapa"}.png")
                        .outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it) }
                    listo.countDown()
                }
            } ?: listo.countDown()
        }
        listo.await(20, java.util.concurrent.TimeUnit.SECONDS)
        val captura = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(rule.activity.getExternalFilesDir(null), "mapa_$etiqueta.png").outputStream().use {
            captura.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it)
        }
    }
}
