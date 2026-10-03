package com.ruralitos.app

import com.ruralitos.app.data.sync.ConversionesSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversionesSyncTest {
    @Test
    fun retrocederRestaLosSegundosYAceptaLaMarcaDelServidor() {
        val marca = "2026-10-02T10:11:12.123456+00:00"
        assertEquals("2026-10-02T10:09:12.123Z", ConversionesSync.retroceder(marca, 120))
    }

    @Test
    fun retrocederAceptaZonaHorariaConDesplazamiento() {
        assertEquals("2026-10-02T15:09:12.500Z", ConversionesSync.retroceder("2026-10-02T10:11:12.5+00:00".replace(".5+", ".500+").replace("T10", "T15"), 120))
    }

    @Test
    fun retrocederDevuelveNuloSiNoEntiendeLaMarca() {
        assertNull(ConversionesSync.retroceder("no es una fecha", 120))
        assertNull(ConversionesSync.retroceder("", 120))
    }

    @Test
    fun fechaLocalConvierteDeIsoYConservaLaLocal() {
        assertEquals("02/10/2026", ConversionesSync.fechaLocal("2026-10-02"))
        assertEquals("02/10/2026", ConversionesSync.fechaLocal("02/10/2026"))
        assertEquals("", ConversionesSync.fechaLocal(""))
    }

    @Test
    fun timestampMillisLeeFormatosDelServidor() {
        val a = ConversionesSync.timestampMillis("2026-10-02T10:11:12.123456+00:00", -1L)
        val b = ConversionesSync.timestampMillis("2026-10-02T10:11:12.123Z", -1L)
        assertEquals(a, b)
        assertEquals(-1L, ConversionesSync.timestampMillis("basura", -1L))
    }
}
