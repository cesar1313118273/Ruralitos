package com.ruralitos.app.domain.familiograma

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColocacionEtiquetasTest {
    private fun persona(id: String, x: Float, y: Float, sexo: SexoPersona = SexoPersona.MUJER) =
        Persona(id, sexo, x, y, patologias = listOf("Gastritis"))

    private fun cajaDe(p: Persona) = Caja(p.x - MEDIA_PERSONA, p.y - MEDIA_PERSONA, p.x + MEDIA_PERSONA, p.y + MEDIA_PERSONA)

    @Test
    fun sinLineasAlrededorLasSiglasVanDebajoDeLaPersona() {
        val madre = persona("m", 200f, 100f)
        val doc = Familiograma(personas = listOf(madre))
        val caja = ColocacionEtiquetas.colocar(doc, listOf(BloqueEtiqueta("m", TipoEtiqueta.SIGLAS, 30f, 15f)))
            .getValue("m" to TipoEtiqueta.SIGLAS)
        assertEquals(200f, (caja.izquierda + caja.derecha) / 2f, 0.01f)
        assertTrue("debajo de la figura", caja.arriba > madre.y + MEDIA_PERSONA)
    }

    @Test
    fun lasSiglasNoSePonenSobreLaLineaQueBajaHastaUnHijo() {
        val madre = persona("m", 200f, 100f)
        val hijo = persona("h", 200f, 260f, SexoPersona.HOMBRE)
        val doc = Familiograma(
            personas = listOf(madre, hijo),
            filiaciones = listOf(Filiacion("f1", hijo = Ancla("h", Lado.ARRIBA), progenitor = Ancla("m", Lado.ABAJO)))
        )
        val caja = ColocacionEtiquetas.colocar(doc, listOf(BloqueEtiqueta("m", TipoEtiqueta.SIGLAS, 30f, 15f)))
            .getValue("m" to TipoEtiqueta.SIGLAS)
        // la línea vertical está en x = 200: el texto queda a un lado de ella
        assertTrue("a un lado de la línea: $caja", caja.izquierda > 200f || caja.derecha < 200f)
        assertEquals(0, ColocacionEtiquetas.obstaculos(doc).choques(caja))
    }

    @Test
    fun siTodoElLadoDeAbajoEstaOcupadoBuscaOtroLado() {
        val madre = persona("m", 200f, 100f)
        val bajo = persona("b", 200f, 150f, SexoPersona.HOMBRE) // pegado debajo de ella
        val doc = Familiograma(personas = listOf(madre, bajo))
        val caja = ColocacionEtiquetas.colocar(doc, listOf(BloqueEtiqueta("m", TipoEtiqueta.SIGLAS, 30f, 15f)))
            .getValue("m" to TipoEtiqueta.SIGLAS)
        assertFalse("no tapa a la otra persona", Obstaculos.solapan(caja, cajaDe(bajo)))
        assertFalse("no tapa a la propia persona", Obstaculos.solapan(caja, cajaDe(madre)))
    }

    @Test
    fun dosPersonasCercaNoSeTapanSusSiglas() {
        val a = persona("a", 200f, 100f)
        val b = persona("b", 235f, 100f)
        val doc = Familiograma(personas = listOf(a, b))
        val cajas = ColocacionEtiquetas.colocar(
            doc,
            listOf(BloqueEtiqueta("a", TipoEtiqueta.SIGLAS, 40f, 15f), BloqueEtiqueta("b", TipoEtiqueta.SIGLAS, 40f, 15f))
        )
        assertFalse(Obstaculos.solapan(cajas.getValue("a" to TipoEtiqueta.SIGLAS), cajas.getValue("b" to TipoEtiqueta.SIGLAS)))
    }

    @Test
    fun elNombreVaDebajoDeLasSiglasCuandoHayLugar() {
        val doc = Familiograma(personas = listOf(persona("m", 200f, 100f)))
        val cajas = ColocacionEtiquetas.colocar(
            doc,
            listOf(BloqueEtiqueta("m", TipoEtiqueta.SIGLAS, 30f, 15f), BloqueEtiqueta("m", TipoEtiqueta.NOMBRE, 50f, 14f))
        )
        val siglas = cajas.getValue("m" to TipoEtiqueta.SIGLAS)
        val nombre = cajas.getValue("m" to TipoEtiqueta.NOMBRE)
        assertTrue(nombre.arriba >= siglas.abajo)
    }

    @Test
    fun laLeyendaNoTapaNingunaFigura() {
        val personas = listOf(100f, 300f, 500f, 700f, 900f).mapIndexed { i, x -> persona("p$i", x, 100f) }
        val doc = Familiograma(personas = personas)
        val caja = ColocacionEtiquetas.colocarLeyenda(doc, emptyList(), 270f, 80f)
        personas.forEach { assertFalse("tapa a ${it.id}", Obstaculos.solapan(caja, cajaDe(it))) }
        assertNotNull(caja)
    }

    @Test
    fun laLeyendaQuedaALaDerechaDeTodoElDibujoCuandoCabe() {
        val doc = Familiograma(personas = listOf(persona("p", 100f, 300f)))
        val caja = ColocacionEtiquetas.colocarLeyenda(doc, emptyList(), 270f, 80f)
        assertTrue("a la derecha de la figura", caja.izquierda > 100f + MEDIA_PERSONA)
        assertTrue("dentro de la hoja", caja.derecha <= HojaFamiliograma.ANCHO)
    }

    @Test
    fun siNoCabeDentroDeLaHojaSeColocaFueraALaDerecha() {
        val doc = Familiograma(personas = listOf(persona("p", 50f, 50f)))
        val hoja = Caja(0f, 0f, 100f, 100f)
        val caja = ColocacionEtiquetas.colocarLeyenda(doc, emptyList(), 270f, 80f, hoja, margen = 2f)
        assertTrue("fuera de la hoja: $caja", caja.izquierda >= hoja.derecha)
    }

    @Test
    fun elSegmentoQueAtraviesaUnaCajaSeDetecta() {
        val caja = Caja(10f, 10f, 20f, 20f)
        assertTrue(Obstaculos.cortaCaja(Segmento(Punto(0f, 15f), Punto(30f, 15f)), caja))
        assertTrue(Obstaculos.cortaCaja(Segmento(Punto(15f, 0f), Punto(15f, 30f)), caja))
        assertFalse(Obstaculos.cortaCaja(Segmento(Punto(0f, 0f), Punto(5f, 30f)), caja))
        assertFalse(Obstaculos.cortaCaja(Segmento(Punto(25f, 0f), Punto(25f, 30f)), caja))
    }
}
