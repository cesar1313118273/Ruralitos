package com.ruralitos.app

import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.mapa.GestorMapaCampo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MapaCampoRenderInstrumentedTest {
    @Test
    fun estiloLocalCargaConMapaDeEcuadorSinInternet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ruta = runBlocking { GestorMapaCampo.preparar(context) }
        assertNotNull("No se pudo preparar el MBTiles local", ruta)
        val estiloJson = GestorMapaCampo.estilo(context, ruta!!)
        val listo = CountDownLatch(1)
        val capturaLista = CountDownLatch(1)
        var vista: MapView? = null
        var tieneFuente = false
        var coloresDistintos = 0
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            escenario.onActivity { actividad ->
                MapLibre.getInstance(actividad)
                vista = MapView(actividad).apply {
                    onCreate(null)
                    actividad.setContentView(this)
                    onStart()
                    onResume()
                    getMapAsync { mapa ->
                        mapa.setStyle(Style.Builder().fromJson(estiloJson)) { estilo ->
                            mapa.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(-0.18, -78.5))
                                .zoom(14.0)
                                .build()
                            tieneFuente = estilo.getSource("ecuador") != null
                            listo.countDown()
                        }
                    }
                }
            }
            assertTrue("El estilo local no cargó", listo.await(30, TimeUnit.SECONDS))
            assertTrue("El mapa vectorial local no quedó disponible", tieneFuente)
            assertNotNull(vista)
            // Las teselas se leen del disco y se dibujan en segundo plano: se reintenta la captura unos segundos
            // (la primera vez, con la copia de los mapas recién hecha, puede tardar más).
            var intento = 0
            while (intento < 8 && coloresDistintos < 3) {
                intento++
                Thread.sleep(3_000)
                val captura = CountDownLatch(1)
                escenario.onActivity {
                    vista?.getMapAsync { mapa ->
                        mapa.snapshot { bitmap ->
                            File(context.getExternalFilesDir(null), "mapa_campo_verificacion.png")
                                .outputStream().use { salida -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida) }
                            val colores = mutableSetOf<Int>()
                            for (x in 1..8) for (y in 1..6) {
                                colores += bitmap.getPixel(x * bitmap.width / 9, y * bitmap.height / 7)
                            }
                            coloresDistintos = colores.size
                            captura.countDown()
                        }
                    }
                }
                assertTrue("El mapa local no produjo una imagen", captura.await(20, TimeUnit.SECONDS))
            }
            capturaLista.countDown()
            assertTrue("El mapa local aparece vacío", coloresDistintos >= 3)
            escenario.onActivity {
                vista?.onPause()
                vista?.onStop()
                vista?.onDestroy()
            }
        }
    }
}
