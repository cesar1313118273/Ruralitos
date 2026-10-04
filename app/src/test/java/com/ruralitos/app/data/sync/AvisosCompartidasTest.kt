package com.ruralitos.app.data.sync

import com.ruralitos.app.data.remote.InfoFichaCompartida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AvisosCompartidasTest {
    private fun recibida(id: String, autor: String) = InfoFichaCompartida(id, true, "a", autor, "LECTOR", 0)
    private fun propia(id: String, editadaEn: Long = 0L, editor: String = "") =
        InfoFichaCompartida(id, false, "yo", "", "", 1, editor, editadaEn)

    @Test
    fun avisaSoloLasFichasRecibidasQueAntesNoLoEran() {
        val previas = mapOf(
            "f1" to EtiquetaPrevia("", 0),
            "f2" to EtiquetaPrevia("LECTOR", 0),
            "f3" to EtiquetaPrevia("", 0)
        )
        val n = AvisosCompartidas.detectar(previas, listOf(recibida("f1", "Ana"), recibida("f2", "Ana"), recibida("f3", "Ana")))
        assertEquals(mapOf("Ana" to 2), n.recibidasPorAutor)
    }

    @Test
    fun unaFichaQueTodaviaNoBajoNoSeAvisa() {
        val n = AvisosCompartidas.detectar(emptyMap(), listOf(recibida("nueva", "Ana")))
        assertFalse(n.hayNovedades)
    }

    @Test
    fun avisaCuandoOtraPersonaEditaUnaFichaMiaDespuesDeLoVisto() {
        val previas = mapOf("f1" to EtiquetaPrevia("", 100), "f2" to EtiquetaPrevia("", 100), "f3" to EtiquetaPrevia("", 0))
        val n = AvisosCompartidas.detectar(
            previas,
            listOf(propia("f1", 200, "Luis"), propia("f2", 100, "Luis"), propia("f3", 0))
        )
        assertEquals(1, n.editadas)
    }

    @Test
    fun redactaElTextoSegunLasNovedades() {
        assertNull(AvisosCompartidas.texto(NovedadesCompartidas(emptyMap(), 0)))
        assertEquals("Ana te compartió 1 ficha.", AvisosCompartidas.texto(NovedadesCompartidas(mapOf("Ana" to 1), 0)))
        assertEquals(
            "Te compartieron 3 fichas (Ana y Luis).",
            AvisosCompartidas.texto(NovedadesCompartidas(mapOf("Ana" to 2, "Luis" to 1), 0))
        )
        assertEquals(
            "Ana te compartió 2 fichas. 1 ficha tuya fue modificada por otra persona.",
            AvisosCompartidas.texto(NovedadesCompartidas(mapOf("Ana" to 2), 1))
        )
        assertEquals(
            "3 fichas tuyas fueron modificadas por otras personas.",
            AvisosCompartidas.texto(NovedadesCompartidas(emptyMap(), 3))
        )
    }
}
