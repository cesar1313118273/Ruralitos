package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EtiquetasFichaTest {
    private fun fichaCon(
        miPermiso: String = "", creadoPor: Long? = null, autorRemoto: String = "", editadaEn: Long = 0L, editor: String = ""
    ) = ficha(miPermiso).copy(creadoPorUsuarioId = creadoPor, autorRemotoId = autorRemoto, editadaPorOtroEn = editadaEn, editorNombre = editor)

    private fun ficha(
        miPermiso: String = "",
        autor: String = "",
        personas: Int = 0
    ) = FichaFamiliarEntity(
        cedulaJefeHogar = "", institucionSistema = "", unidadOperativa = "", codigoUo = "", areaNumero = "",
        codigoLocalizacion = "", parroquiaCodigoLocalizacion = "", cantonCodigoLocalizacion = "",
        provinciaCodigoLocalizacion = "", numeroFichaFamiliar = "", provincia = "", canton = "", parroquia = "",
        sector = "", manzana = "", numeroFamilia = "", direccionHabitualFamilia = "", barrio = "", numeroCasa = "",
        comunidad = "", grupoCultural = "", nombreApellidoJefeFamilia = "", numeroTelefono = "", fechaLlenado = "",
        numeroCarpeta = "", miPermiso = miPermiso, autorNombre = autor, compartidaConPersonas = personas
    )

    @Test
    fun fichaPropiaSinCompartirNoTieneEtiqueta() {
        val f = ficha()
        assertFalse(EtiquetasFicha.esRecibida(f))
        assertFalse(EtiquetasFicha.soloLectura(f))
        assertTrue(EtiquetasFicha.puedeEliminar(f))
        assertNull(EtiquetasFicha.texto(f))
    }

    @Test
    fun fichaRecibidaDiceQuienLaCreoYElPermiso() {
        assertEquals("Compartida por Ana · Solo lectura", EtiquetasFicha.texto(ficha("LECTOR", "Ana")))
        assertEquals("Compartida por Ana · Puede editar", EtiquetasFicha.texto(ficha("EDITOR", "Ana")))
        assertEquals("Compartida por otra persona · Solo lectura", EtiquetasFicha.texto(ficha("LECTOR")))
    }

    @Test
    fun soloLecturaSeBloqueaYElEditorNoPuedeEliminar() {
        assertTrue(EtiquetasFicha.soloLectura(ficha("LECTOR")))
        assertFalse(EtiquetasFicha.soloLectura(ficha("EDITOR")))
        assertFalse(EtiquetasFicha.puedeEliminar(ficha("EDITOR")))
        assertFalse(EtiquetasFicha.puedeEliminar(ficha("LECTOR")))
    }

    @Test
    fun fichaPropiaCompartidaDiceConCuantasPersonas() {
        assertEquals("Compartida por ti con 1 persona", EtiquetasFicha.texto(ficha(personas = 1)))
        assertEquals("Compartida por ti con 3 personas", EtiquetasFicha.texto(ficha(personas = 3)))
    }

    @Test
    fun esMiaSiLaCreeAquiOConMiCuentaEnOtroTelefono() {
        assertTrue(EtiquetasFicha.esMia(fichaCon(creadoPor = 5L), 5L, "cuenta"))
        assertTrue(EtiquetasFicha.esMia(fichaCon(autorRemoto = "cuenta"), 5L, "cuenta"))
        assertFalse(EtiquetasFicha.esMia(fichaCon(autorRemoto = "otra"), 5L, "cuenta"))
        assertFalse(EtiquetasFicha.esMia(fichaCon(autorRemoto = "cuenta"), 5L, ""))
    }

    @Test
    fun unaFichaTraspasadaOCompartidaYaNoEsMia() {
        assertFalse(EtiquetasFicha.esMia(fichaCon(miPermiso = "EDITOR", creadoPor = 5L, autorRemoto = "cuenta"), 5L, "cuenta"))
    }

    @Test
    fun avisaCuandoOtraPersonaEditoUnaFichaPropia() {
        assertNull(EtiquetasFicha.textoEdicionAjena(fichaCon()))
        assertNull(EtiquetasFicha.textoEdicionAjena(fichaCon(miPermiso = "LECTOR", editadaEn = 1L, editor = "Luis")))
        val texto = EtiquetasFicha.textoEdicionAjena(fichaCon(editadaEn = 1_700_000_000_000L, editor = "Luis"))
        assertTrue(texto!!.startsWith("Última modificación de otra persona: Luis, "))
    }
}

class FiltroOrigenMapaTest {
    @Test
    fun porDefectoSeVenMisFichasPrimero() {
        assertEquals(OrigenFicha.MIAS, OPCIONES_ORIGEN_MAPA.first())
        assertEquals(listOf(OrigenFicha.MIAS, OrigenFicha.RECIBIDAS, OrigenFicha.TODAS), OPCIONES_ORIGEN_MAPA)
    }

    @Test
    fun cadaFiltroAdmiteLoQueLeCorresponde() {
        assertTrue(OrigenFicha.MIAS.admiteVivienda(""))
        assertFalse(OrigenFicha.MIAS.admiteVivienda("LECTOR"))
        assertFalse(OrigenFicha.RECIBIDAS.admiteVivienda(""))
        assertTrue(OrigenFicha.RECIBIDAS.admiteVivienda("EDITOR"))
        assertTrue(OrigenFicha.TODAS.admiteVivienda("") && OrigenFicha.TODAS.admiteVivienda("LECTOR"))
    }

    @Test
    fun laLineaDiceDeDondeSalenLosDatos() {
        assertEquals("Datos de tus fichas (23)", textoDatosDe(OrigenFicha.MIAS, 23, 10))
        assertEquals("Datos de fichas compartidas contigo (10)", textoDatosDe(OrigenFicha.RECIBIDAS, 23, 10))
        assertEquals("Tus fichas (23) + compartidas contigo (10)", textoDatosDe(OrigenFicha.TODAS, 23, 10))
    }
}
