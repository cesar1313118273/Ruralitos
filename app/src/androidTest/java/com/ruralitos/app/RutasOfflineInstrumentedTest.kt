package com.ruralitos.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ruralitos.app.data.mapa.GestorRutasOffline
import com.ruralitos.app.data.mapa.GestorMapaDetalle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng

@RunWith(AndroidJUnit4::class)
class RutasOfflineInstrumentedTest {
    @Test
    fun zoom15VieneIncluidoYSePreparaSinInternet() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ruta = GestorMapaDetalle.preparar(context)
        assertTrue(ruta?.contains("ecuador_zoom15.pmtiles") == true)
    }

    @Test
    fun calculaRutaDeQuitoSinInternet() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inicio = LatLng(-0.220164, -78.512327)
        val destino = LatLng(-0.203313, -78.490726)
        val ruta = GestorRutasOffline.calcular(context, inicio, destino, "auto").getOrThrow()
        assertTrue(ruta.kilometros in 1.0..20.0)
        assertTrue(ruta.minutos in 1..120)
        assertTrue(ruta.puntos.size > 10)
        assertTrue(ruta.instrucciones.isNotEmpty())
    }
}
