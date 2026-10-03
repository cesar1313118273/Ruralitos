package com.ruralitos.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruralitos.app.data.mapa.ArchivosMapa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/** El tamaño de cada mapa se lee del propio archivo empaquetado, no de un número escrito en el código. */
@RunWith(AndroidJUnit4::class)
class ArchivosMapaTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun leeElTamanoRealDeLosTresMapasEmpaquetados() {
        assertEquals(123_170_816L, ArchivosMapa.longitudAsset(context, "ecuador_base.mbtiles"))
        assertEquals(140_147_517L, ArchivosMapa.longitudAsset(context, "ecuador_zoom15.pmtiles"))
        assertEquals(224_225_280L, ArchivosMapa.longitudAsset(context, "ecuador_valhalla_tiles.tar"))
    }

    @Test
    fun rechazaCopiarSiNoHayEspacioLibre() {
        try {
            ArchivosMapa.verificarEspacio(context, Long.MAX_VALUE / 4)
            fail("debía rechazar por falta de espacio")
        } catch (esperado: IOException) {
            assertTrue(esperado.message!!.contains("Espacio insuficiente"))
        }
        ArchivosMapa.verificarEspacio(context, 1L)
    }
}
