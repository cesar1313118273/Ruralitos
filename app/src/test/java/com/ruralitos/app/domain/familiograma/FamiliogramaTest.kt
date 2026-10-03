package com.ruralitos.app.domain.familiograma

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AbreviaturasPatologiaTest {
    @Test
    fun usaLaAbreviaturaDeLaFichaCuandoExiste() {
        assertEquals("DI", AbreviaturasPatologia.resolver("Diabetes mellitus tipo 2", emptyList()).codigo)
        assertEquals("HT", AbreviaturasPatologia.resolver("hipertensión", emptyList()).codigo)
        assertEquals("HT", AbreviaturasPatologia.resolver("HTA", emptyList()).codigo)
        assertEquals("CA", AbreviaturasPatologia.resolver("Cáncer de mama", emptyList()).codigo)
        assertTrue(AbreviaturasPatologia.resolver("asma bronquial", emptyList()).deFicha)
    }

    @Test
    fun noConfundeMigranaConMigrante() {
        assertNull(AbreviaturasPatologia.buscarEnFicha("Migraña"))
        assertEquals("MG", AbreviaturasPatologia.buscarEnFicha("Migrante")?.codigo)
    }

    @Test
    fun generaIncialesParaVariasPalabrasYLetrasParaUna() {
        assertEquals("IRC", AbreviaturasPatologia.resolver("Insuficiencia renal crónica", emptyList()).codigo)
        assertEquals("AR", AbreviaturasPatologia.resolver("Artritis reumatoide", emptyList()).codigo)
        assertEquals("GAS", AbreviaturasPatologia.resolver("Gastritis", emptyList()).codigo)
        assertTrue(AbreviaturasPatologia.resolver("Gastritis", emptyList()).pendiente)
    }

    @Test
    fun nuncaRepiteUnCodigoYaUsado() {
        var doc = Familiograma()
        doc = AbreviaturasPatologia.registrar(doc, "Hipotiroidismo")
        doc = AbreviaturasPatologia.registrar(doc, "Hipertiroidismo")
        val codigos = doc.patologiasNuevas.map { it.codigo }
        assertEquals(2, codigos.toSet().size)
        assertEquals("HIP", codigos[0])
        assertEquals("HIPE", codigos[1])
        // un código de la ficha tampoco se reutiliza ("Diabetes insípida" ya es DI de la ficha)
        assertEquals("DI", AbreviaturasPatologia.resolver("Diabetes insípida", doc.patologiasNuevas).codigo)
    }

    @Test
    fun registrarDosVecesLaMismaPatologiaConservaElCodigo() {
        var doc = AbreviaturasPatologia.registrar(Familiograma(), "Insuficiencia renal crónica")
        doc = AbreviaturasPatologia.registrar(doc, "  insuficiencia RENAL crónica ")
        assertEquals(1, doc.patologiasNuevas.size)
        assertEquals("IRC", doc.patologiasNuevas.single().codigo)
    }

    @Test
    fun permiteCambiarLaAbreviaturaSiEstaLibre() {
        var doc = AbreviaturasPatologia.registrar(Familiograma(), "Insuficiencia renal crónica")
        doc = AbreviaturasPatologia.registrar(doc, "Insuficiencia renal crónica", "erc")
        assertEquals("ERC", doc.patologiasNuevas.single().codigo)
        // un código de la ficha no se acepta: se conserva el anterior
        doc = AbreviaturasPatologia.registrar(doc, "Insuficiencia renal crónica", "DI")
        assertEquals("ERC", doc.patologiasNuevas.single().codigo)
    }

    @Test
    fun laListaDeNuevasSoloIncluyeLasQueSeUsan() {
        var doc = Familiograma(personas = listOf(Persona("p1", SexoPersona.MUJER, 0f, 0f, patologias = listOf("Gastritis", "Diabetes"))))
        doc = AbreviaturasPatologia.registrar(doc, "Gastritis")
        doc = AbreviaturasPatologia.registrar(doc, "Lupus")
        assertEquals(listOf("Gastritis"), AbreviaturasPatologia.nuevasEnUso(doc).map { it.nombre })
        assertEquals(listOf("DI"), AbreviaturasPatologia.codigosDeFichaEnUso(doc).map { it.codigo })
        assertEquals(1, doc.limpiarPatologias().patologiasNuevas.size)
    }

    @Test
    fun sugiereAbreviaturasDeLaFichaMientrasSeEscribe() {
        assertEquals(listOf("DI"), AbreviaturasPatologia.sugerencias("dia").map { it.codigo })
        assertTrue(AbreviaturasPatologia.sugerencias("d").isEmpty())
    }
}

class ArmadoFamiliogramaTest {
    private fun integrante(parentesco: String, sexo: String, edad: Int, nombre: String = parentesco) =
        IntegranteFamiliograma(nombre, parentesco, sexo, edad.toString())

    @Test
    fun armaLosCincoIntegrantesConSuPareja() {
        val doc = ArmadoFamiliograma.desdeIntegrantes(
            listOf(
                integrante("JEFE/A DE FAMILIA", "H", 45),
                integrante("CÓNYUGE/PAREJA", "M", 42),
                integrante("HIJO/A", "H", 22),
                integrante("HIJO/A", "M", 18),
                integrante("HIJO/A", "H", 9)
            )
        )
        assertEquals(5, doc.personas.size)
        assertEquals(1, doc.uniones.size)
        assertEquals(3, doc.filiaciones.size)
        assertTrue(doc.personas.all { it.enHogar })
        val jefe = doc.personas.first { it.parentesco.startsWith("JEFE") }
        val conyuge = doc.personas.first { it.parentesco.startsWith("CÓNY") }
        assertTrue(conyuge.x > jefe.x)
        assertEquals(jefe.y, conyuge.y, 0.01f)
        val hijos = doc.personas.filter { it.parentesco == "HIJO/A" }
        assertTrue(hijos.all { it.y > jefe.y })
        assertEquals("45", jefe.edad)
        // los hijos quedan centrados bajo la pareja
        assertEquals((jefe.x + conyuge.x) / 2, hijos.map { it.x }.average().toFloat(), 0.5f)
    }

    @Test
    fun conUnSoloProgenitorLosHijosCuelganDeEl() {
        val doc = ArmadoFamiliograma.desdeIntegrantes(
            listOf(integrante("JEFE/A DE FAMILIA", "M", 38), integrante("HIJO/A", "M", 10))
        )
        assertEquals(0, doc.uniones.size)
        val filiacion = doc.filiaciones.single()
        assertNull(filiacion.unionId)
        assertEquals(Lado.ABAJO, filiacion.progenitor?.lado)
    }

    @Test
    fun colocaPadresArribaYLosUneConElJefeComoHijo() {
        val doc = ArmadoFamiliograma.desdeIntegrantes(
            listOf(
                integrante("JEFE/A DE FAMILIA", "H", 50),
                integrante("PADRE/MADRE", "M", 75),
                integrante("PADRE/MADRE", "H", 78)
            )
        )
        val jefe = doc.personas.first { it.parentesco.startsWith("JEFE") }
        val padres = doc.personas.filter { it.parentesco.startsWith("PADRE") }
        assertTrue(padres.all { it.y < jefe.y })
        assertEquals(SexoPersona.HOMBRE, padres.minByOrNull { it.x }!!.sexo)
        assertEquals(1, doc.uniones.size)
        assertEquals(doc.uniones.single().id, doc.filiaciones.single().unionId)
    }

    @Test
    fun registraLasAbreviaturasNuevasDeLosIntegrantes() {
        val doc = ArmadoFamiliograma.desdeIntegrantes(
            listOf(
                IntegranteFamiliograma("Ana", "JEFE/A DE FAMILIA", "M", "40", listOf("Diabetes", "Gastritis"))
            )
        )
        assertEquals(listOf("Gastritis"), doc.patologiasNuevas.map { it.nombre })
    }
}

class GeometriaFamiliogramaTest {
    private fun docBase(): Familiograma {
        val p = Persona("p1", SexoPersona.HOMBRE, 0f, 0f)
        val m = Persona("p2", SexoPersona.MUJER, 100f, 0f)
        val h = Persona("p3", SexoPersona.HOMBRE, 50f, 120f)
        return Familiograma(
            personas = listOf(p, m, h),
            uniones = listOf(Union("u1", Ancla("p1", Lado.DERECHA), Ancla("p2", Lado.IZQUIERDA))),
            filiaciones = listOf(Filiacion("f1", Ancla("p3", Lado.ARRIBA), unionId = "u1"))
        )
    }

    @Test
    fun lasLineasSeAdaptanAlMoverUnaPersona() {
        val doc = docBase()
        val antes = GeometriaFamiliograma.rutaUnion(doc, doc.uniones.single())
        assertEquals(Punto(MEDIA_PERSONA, 0f), antes.first())
        assertEquals(Punto(100f - MEDIA_PERSONA, 0f), antes.last())
        val movido = doc.moverElemento("p2", 200f, 60f)
        val despues = GeometriaFamiliograma.rutaUnion(movido, movido.uniones.single())
        assertEquals(Punto(200f - MEDIA_PERSONA, 60f), despues.last())
        // la línea del hijo sigue naciendo en la mitad de la unión
        val origen = GeometriaFamiliograma.origenFiliacion(movido, movido.filiaciones.single())!!
        val mitad = GeometriaFamiliograma.puntoMedioUnion(movido, movido.uniones.single())!!.punto
        assertEquals(mitad, origen)
    }

    @Test
    fun cadaFiguraTieneCuatroPuntosDeConexion() {
        val doc = docBase()
        val p = doc.persona("p1")!!
        assertEquals(Punto(p.x, p.y - MEDIA_PERSONA), GeometriaFamiliograma.posicion(doc, Ancla("p1", Lado.ARRIBA)))
        assertEquals(Punto(p.x, p.y + MEDIA_PERSONA), GeometriaFamiliograma.posicion(doc, Ancla("p1", Lado.ABAJO)))
        assertEquals(Punto(p.x - MEDIA_PERSONA, p.y), GeometriaFamiliograma.posicion(doc, Ancla("p1", Lado.IZQUIERDA)))
        assertEquals(Punto(p.x + MEDIA_PERSONA, p.y), GeometriaFamiliograma.posicion(doc, Ancla("p1", Lado.DERECHA)))
    }

    @Test
    fun encuentraElAnclaMasCercanaYLaUnionMasCercana() {
        val doc = docBase()
        assertEquals(Ancla("p2", Lado.IZQUIERDA), GeometriaFamiliograma.anclaCercana(doc, Punto(100f - MEDIA_PERSONA + 3f, 3f), "p3", 20f))
        assertNull(GeometriaFamiliograma.anclaCercana(doc, Punto(300f, 300f), null, 20f))
        assertEquals("u1", GeometriaFamiliograma.unionCercana(doc, Punto(50f, 4f), 12f)?.id)
        assertNull(GeometriaFamiliograma.unionCercana(doc, Punto(50f, 60f), 12f))
    }

    @Test
    fun quitarUnaPersonaQuitaSusLineas() {
        val doc = docBase().quitar("p2")
        assertEquals(2, doc.personas.size)
        assertTrue(doc.uniones.isEmpty())
        assertTrue(doc.filiaciones.isEmpty())
    }

    @Test
    fun elHogarEncierraSoloAQuienesViveEnEl() {
        val doc = docBase().conPersona(docBase().persona("p3")!!.copy(enHogar = false))
        val caja = GeometriaFamiliograma.cajaHogar(doc)!!
        assertTrue(caja.abajo < 120f)
        assertFalse(doc.personas.none { it.enHogar })
    }
}

class SerializadorFamiliogramaTest {
    @Test
    fun guardaYRecuperaTodoElDibujo() {
        val base = ArmadoFamiliograma.desdeIntegrantes(
            listOf(
                IntegranteFamiliograma("Pérez Gómez Luis", "JEFE/A DE FAMILIA", "H", "45", listOf("Hipertensión arterial")),
                IntegranteFamiliograma("Pérez Gómez Ana", "CÓNYUGE/PAREJA", "M", "42", listOf("Insuficiencia renal crónica")),
                IntegranteFamiliograma("Pérez Ruiz Leo", "HIJO/A", "H", "9")
            )
        )
        var doc = base.copy(
            uniones = base.uniones.map { it.copy(tipo = TipoUnion.SEPARACION) },
            filiaciones = base.filiaciones.map { it.copy(adoptado = true) },
            entornos = listOf(Entorno("e1", TipoEntorno.IGLESIA, -120f, 200f, "Iglesia del barrio")),
            vinculos = listOf(Vinculo("v1", Ancla("e1", Lado.DERECHA), Ancla("p1", Lado.IZQUIERDA))),
            trazos = listOf(
                Trazo("t1", TipoTrazo.LAPIZ, listOf(Punto(0f, 0f), Punto(5f, 7.5f)), 0xFF1565C0.toInt(), 3f, true)
            ),
            textos = listOf(Texto("x1", 10f, 20f, "Comunidad\nCentro")),
            mostrarNombres = true
        )
        doc = doc.copy(abortos = listOf(Aborto("a1", doc.uniones.single().id, 40f, 150f)))
        doc = doc.conPersona(doc.persona("p1")!!.copy(fallecido = true, informante = true, enHogar = false))

        val recuperado = SerializadorFamiliograma.desdeJson(SerializadorFamiliograma.aJson(doc))
        assertNotNull(recuperado)
        assertEquals(doc, recuperado)
    }

    @Test
    fun unTextoQueNoEsUnFamiliogramaDevuelveNulo() {
        assertNull(SerializadorFamiliograma.desdeJson("no es json"))
        assertNull(SerializadorFamiliograma.desdeJson("{\"personas\":[{\"id\":1}]}"))
    }

    @Test
    fun elPngConserva_elDibujoDentro() {
        val png = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52, 0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 0x1F, 0x15, 0xC4.toByte(), 0x89.toByte(),
            0, 0, 0, 0, 0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
        )
        val json = "{\"mensaje\":\"Niño con ñandú y acentos áéíóú\"}"
        val conDatos = PngConDatos.insertar(png, json)
        assertTrue(conDatos.size > png.size)
        assertEquals(json, PngConDatos.leer(conDatos))
        assertNull(PngConDatos.leer(png))
        // insertar otra vez reemplaza el bloque anterior en lugar de duplicarlo
        val otra = PngConDatos.insertar(conDatos, "{\"v\":2}")
        assertEquals("{\"v\":2}", PngConDatos.leer(otra))
        assertEquals(PngConDatos.insertar(png, "{\"v\":2}").size, otra.size)
    }
}

class EdadFamiliogramaTest {
    private fun fecha(texto: String): java.util.Date =
        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ROOT).parse(texto)!!

    @Test
    fun calculaAniosYMesesParaLosBebes() {
        val hoy = fecha("15/10/2026")
        assertEquals("41", EdadFamiliograma.desdeFechaNacimiento("16/10/1984", hoy))
        assertEquals("42", EdadFamiliograma.desdeFechaNacimiento("15/10/1984", hoy))
        assertEquals("8m", EdadFamiliograma.desdeFechaNacimiento("10/02/2026", hoy))
        assertEquals("0m", EdadFamiliograma.desdeFechaNacimiento("01/10/2026", hoy))
        assertEquals("1", EdadFamiliograma.desdeFechaNacimiento("15/10/2025", hoy))
        assertNull(EdadFamiliograma.desdeFechaNacimiento("", hoy))
        assertNull(EdadFamiliograma.desdeFechaNacimiento("fecha mala", hoy))
        assertNull(EdadFamiliograma.desdeFechaNacimiento("20/10/2026", hoy))
    }
}

class HojaFamiliogramaTest {
    @Test
    fun cadaGeneracionQuedaEnSuFranjaDeLaFicha() {
        val doc = ArmadoFamiliograma.desdeIntegrantes(
            listOf(
                IntegranteFamiliograma("A", "JEFE/A DE FAMILIA", "H", "45"),
                IntegranteFamiliograma("B", "CÓNYUGE/PAREJA", "M", "42"),
                IntegranteFamiliograma("C", "HIJO/A", "H", "9"),
                IntegranteFamiliograma("D", "PADRE/MADRE", "M", "70")
            )
        )
        fun y(parentesco: String) = doc.personas.first { it.parentesco.startsWith(parentesco) }.y
        assertTrue(y("PADRE") < HojaFamiliograma.FIN_ABUELOS)
        assertTrue(y("JEFE") > HojaFamiliograma.FIN_ABUELOS && y("JEFE") < HojaFamiliograma.FIN_PADRES)
        assertTrue(y("HIJO") > HojaFamiliograma.FIN_PADRES && y("HIJO") < HojaFamiliograma.ALTO)
        assertFalse(HojaFamiliograma.hayElementosFuera(doc))
        assertTrue(HojaFamiliograma.hayElementosFuera(doc.moverElemento("p1", 2000f, 100f)))
    }

    @Test
    fun conMuchosHijosSeAcercanParaQueTodosCaigan()  {
        val hijos = List(11) { IntegranteFamiliograma("H$it", "HIJO/A", "H", "5") }
        val doc = ArmadoFamiliograma.desdeIntegrantes(
            listOf(IntegranteFamiliograma("A", "JEFE/A DE FAMILIA", "H", "45"), IntegranteFamiliograma("B", "CÓNYUGE/PAREJA", "M", "42")) + hijos
        )
        assertFalse(HojaFamiliograma.hayElementosFuera(doc))
    }
}
