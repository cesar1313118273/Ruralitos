package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.EaisSalaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.SalaEntity
import com.ruralitos.app.data.local.entity.TerritorioSalaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlcanceFichasTest {
    private fun sala(id: String, nombre: String) = SalaEntity(
        organizacionId = id, establecimientoId = null, nombreSala = nombre, codigoSala = id,
        rol = "ADMINISTRADOR", permiso = "ADMINISTRADOR", nombreCentroSalud = nombre
    )

    private fun ficha(
        id: Long, jefe: String, org: String = "S1", eais: String = "", barrio: String = "",
        sync: String = "SINCRONIZADO", numero: String = id.toString()
    ) = FichaFamiliarEntity(
        id = id, cedulaJefeHogar = "0$id", institucionSistema = "", unidadOperativa = "", codigoUo = "", areaNumero = "",
        codigoLocalizacion = "", parroquiaCodigoLocalizacion = "", cantonCodigoLocalizacion = "", provinciaCodigoLocalizacion = "",
        numeroFichaFamiliar = numero, provincia = "", canton = "", parroquia = "", sector = "", manzana = "", numeroFamilia = "",
        direccionHabitualFamilia = "", barrio = "", numeroCasa = "", comunidad = "", grupoCultural = "",
        nombreApellidoJefeFamilia = jefe, numeroTelefono = "", fechaLlenado = "", numeroCarpeta = "",
        organizacionId = org, eaisId = eais, territorioId = barrio, syncId = "sync-$id", syncEstado = sync
    )

    private val catalogo = CatalogoAlcance(
        salas = listOf(sala("S1", "Centro Uno"), sala("S2", "Centro Dos")),
        eais = listOf(EaisSalaEntity("E1", "S1", 1), EaisSalaEntity("E2", "S1", 2)),
        territorios = listOf(
            TerritorioSalaEntity("B1", "S1", "E1", "BARRIO", "San Pedro"),
            TerritorioSalaEntity("B2", "S1", "E1", "BARRIO", "La Esperanza"),
            TerritorioSalaEntity("B3", "S1", "E2", "BARRIO", "El Carmen")
        ),
        fichas = listOf(
            ficha(1, "Pérez Loja Juan", eais = "E1", barrio = "B1"),
            ficha(2, "Pérez Armijos Rosa", eais = "E1", barrio = "B1"),
            ficha(3, "Gómez Ruiz Ana", eais = "E1", barrio = "B2", sync = "PENDIENTE"),
            ficha(4, "Torres Vega Luis", eais = "E2", barrio = "B3"),
            ficha(5, "Otro Centro Pedro", org = "S2")
        ),
        salaActivaId = "S1"
    )

    @Test
    fun todoElCentroIncluyeSoloLasFichasDelCentroActivo() {
        assertEquals(listOf(1L, 2L, 3L, 4L), AlcanceFichas.fichasDe(NivelAlcance.CENTRO_ACTIVO, emptySet(), catalogo).map { it.id })
        assertEquals("Todo el centro · 4 fichas", AlcanceFichas.resumen(NivelAlcance.CENTRO_ACTIVO, emptySet(), catalogo))
    }

    @Test
    fun cadaNivelListaSusOpcionesConElNumeroDeFichas() {
        val centros = AlcanceFichas.opciones(NivelAlcance.CENTRO, catalogo)
        assertEquals(listOf("Centro Uno" to 4, "Centro Dos" to 1), centros.map { it.titulo to it.fichas })
        val eais = AlcanceFichas.opciones(NivelAlcance.EAIS, catalogo)
        assertEquals(listOf("EAIS 1" to 3, "EAIS 2" to 1), eais.map { it.titulo to it.fichas })
        assertEquals("2 barrios", eais[0].detalle)
        val barrios = AlcanceFichas.opciones(NivelAlcance.BARRIO, catalogo)
        assertEquals(3, barrios.size)
        assertEquals(2, barrios.first { it.titulo == "San Pedro" }.fichas)
    }

    @Test
    fun losBarriosSePuedenFiltrarPorEais() {
        val barrios = AlcanceFichas.opciones(NivelAlcance.BARRIO, catalogo, filtroEaisId = "E2")
        assertEquals(listOf("El Carmen"), barrios.map { it.titulo })
    }

    @Test
    fun lasFichasSeFiltranPorEaisBarrioYTexto() {
        assertEquals(4, AlcanceFichas.opciones(NivelAlcance.FICHA, catalogo).size)
        assertEquals(2, AlcanceFichas.opciones(NivelAlcance.FICHA, catalogo, filtroBarrioId = "B1").size)
        assertEquals(3, AlcanceFichas.opciones(NivelAlcance.FICHA, catalogo, filtroEaisId = "E1").size)
        // el buscador ignora tildes y mayúsculas
        assertEquals(listOf("3 · Gómez Ruiz Ana"), AlcanceFichas.opciones(NivelAlcance.FICHA, catalogo, consulta = "gomez").map { it.titulo })
        assertEquals(1, AlcanceFichas.opciones(NivelAlcance.FICHA, catalogo, consulta = "04").size)
    }

    @Test
    fun alCompartirLasFichasSinSincronizarNoSePuedenElegir() {
        val opciones = AlcanceFichas.opciones(NivelAlcance.FICHA, catalogo, soloSincronizadas = true)
        val pendiente = opciones.first { it.clave == "3" }
        assertFalse(pendiente.habilitada)
        assertTrue(pendiente.detalle.contains("sin sincronizar"))
        assertTrue(opciones.first { it.clave == "1" }.habilitada)
        // y aunque llegara a estar marcada, no se entrega
        assertTrue(AlcanceFichas.concesiones(NivelAlcance.FICHA, setOf("3"), catalogo).isEmpty())
    }

    @Test
    fun varios_barrios_dan_una_concesion_por_barrio_sin_repetir_fichas() {
        val elegidos = setOf("B1", "B2")
        assertEquals(listOf(1L, 2L, 3L), AlcanceFichas.fichasDe(NivelAlcance.BARRIO, elegidos, catalogo).map { it.id })
        assertEquals("2 barrios · 3 fichas", AlcanceFichas.resumen(NivelAlcance.BARRIO, elegidos, catalogo))
        val concesiones = AlcanceFichas.concesiones(NivelAlcance.BARRIO, elegidos, catalogo)
        assertEquals(setOf("B1", "B2"), concesiones.map { it.territorioId }.toSet())
        assertTrue(concesiones.all { it.alcance == "TERRITORIO" && it.eaisId == "E1" && it.organizacionId == "S1" })
    }

    @Test
    fun varios_centros_y_eais_y_fichas_sueltas_producen_sus_concesiones() {
        val centros = AlcanceFichas.concesiones(NivelAlcance.CENTRO, setOf("S1", "S2"), catalogo)
        assertEquals(listOf("SALA", "SALA"), centros.map { it.alcance })
        assertEquals(5, AlcanceFichas.fichasDe(NivelAlcance.CENTRO, setOf("S1", "S2"), catalogo).size)
        val eais = AlcanceFichas.concesiones(NivelAlcance.EAIS, setOf("E2"), catalogo)
        assertEquals(listOf(ConcesionAcceso("S1", "EAIS", eaisId = "E2")), eais)
        val fichas = AlcanceFichas.concesiones(NivelAlcance.FICHA, setOf("1", "4"), catalogo)
        assertEquals(listOf("sync-1", "sync-4"), fichas.map { it.fichaId })
        assertEquals("2 fichas", AlcanceFichas.resumen(NivelAlcance.FICHA, setOf("1", "4"), catalogo))
        assertEquals("1 ficha", AlcanceFichas.resumen(NivelAlcance.FICHA, setOf("1"), catalogo))
    }

    @Test
    fun sinNadaElegidoNoHayNadaQueEntregar() {
        assertFalse(AlcanceFichas.hayElegidos(NivelAlcance.BARRIO, emptySet()))
        assertTrue(AlcanceFichas.hayElegidos(NivelAlcance.CENTRO_ACTIVO, emptySet()))
        assertTrue(AlcanceFichas.concesiones(NivelAlcance.BARRIO, emptySet(), catalogo).isEmpty())
    }
}
