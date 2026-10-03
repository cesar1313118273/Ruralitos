package com.ruralitos.app

import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import com.ruralitos.app.domain.IconosMais
import com.ruralitos.app.domain.MapaParlante
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapaParlanteTest {
    private fun ficha(id: Long, barrio: String, lat: Double? = -4.0, lon: Double? = -79.2, estado: String = "COMPLETA") = FichaFamiliarEntity(
        id = id, cedulaJefeHogar = "0$id", institucionSistema = "", unidadOperativa = "", codigoUo = "", areaNumero = "",
        codigoLocalizacion = "", parroquiaCodigoLocalizacion = "", cantonCodigoLocalizacion = "", provinciaCodigoLocalizacion = "",
        numeroFichaFamiliar = "F$id", provincia = "", canton = "", parroquia = "", sector = "", manzana = "", numeroFamilia = "",
        direccionHabitualFamilia = "", barrio = barrio, numeroCasa = "", comunidad = "", grupoCultural = "",
        nombreApellidoJefeFamilia = "JEFE $id", numeroTelefono = "", fechaLlenado = "", numeroCarpeta = "",
        responsableNombre = "", responsableCodigo = "", latitud = lat, longitud = lon, estado = estado
    )

    private fun persona(ficha: Long, nombre: String, nacimiento: String, hta: Boolean = false, dm: Boolean = false) = MiembroFamiliaEntity(
        fichaId = ficha, grupoEdad = "", apellidosNombres = nombre, parentesco = "", fechaNacimiento = nacimiento,
        ocupacion = "", sexo = "", escolaridad = "", vacunasCompletas = true, saludBucalAdecuada = true,
        estadoNutricional = "SIN_ALTERACION", hipertensionArterial = hta, diabetesMellitus = dm,
        tuberculosis = false, problemaSaludMental = false, consumoAlcoholDrogas = false, enfermedadCronica = false
    )

    @Test
    fun soloAparecenLasFigurasQueExistenEnElBarrio() {
        val fichas = listOf(ficha(1, "Cerezal"), ficha(2, "Cerezal"), ficha(3, "El Carmen"))
        val miembros = listOf(
            persona(1, "A", "01/01/1960", hta = true),
            persona(1, "B", "01/01/1970", hta = true),
            persona(2, "C", "01/01/1965", dm = true),
            persona(3, "D", "01/01/1990")
        )
        val barrios = MapaParlante.barrios(fichas, miembros, emptyList())
        val cerezal = barrios.first { it.nombre == "Cerezal" }
        assertEquals(2, cerezal.fichas)
        assertEquals(3, cerezal.personas)
        val porId = cerezal.stickers.associate { it.id to it.personas }
        assertEquals(2, porId[IconosMais.HIPERTENSION])
        assertEquals(1, porId[IconosMais.DIABETES])
        assertTrue("sin gestantes no hay figura de embarazo", porId.keys.none { it.startsWith("embarazo") })
        val carmen = barrios.first { it.nombre == "El Carmen" }
        assertTrue(carmen.stickers.none { it.id == IconosMais.HIPERTENSION })
    }

    @Test
    fun elPuntoMedioEsElPromedioDeLasFichasUbicadas() {
        val fichas = listOf(
            ficha(1, "Cerezal", -4.00, -79.20), ficha(2, "Cerezal", -4.02, -79.22), ficha(3, "Cerezal", null, null),
            ficha(4, "Cerezal", -1.8312, -78.1834) // punto de ejemplo antiguo: no cuenta
        )
        val b = MapaParlante.barrios(fichas, emptyList(), emptyList()).single()
        assertEquals(-4.01, b.centro!!.first, 1e-9)
        assertEquals(-79.21, b.centro!!.second, 1e-9)
        assertEquals(2, b.fichasSinUbicacion)
    }

    @Test
    fun sinNingunaUbicacionNoHayPuntoMedio() {
        val b = MapaParlante.barrios(listOf(ficha(1, "Cerezal", null, null)), emptyList(), emptyList()).single()
        assertNull(b.centro)
    }

    @Test
    fun elBarrioSeAgrupaSinImportarTildesNiMayusculasYSeIgnoranLasArchivadas() {
        val fichas = listOf(ficha(1, "San José"), ficha(2, "SAN JOSE"), ficha(3, "San José", estado = "ARCHIVADA"))
        val barrios = MapaParlante.barrios(fichas, emptyList(), emptyList())
        assertEquals(1, barrios.size)
        assertEquals(2, barrios.single().fichas)
    }

    @Test
    fun lasFigurasNuncaSeEmpalmanEntreSi() {
        for (n in 1..40) {
            val celdas = MapaParlante.desplazamientos(n, 64f, 72f)
            assertEquals(n, celdas.size)
            for (i in 0 until n) for (j in i + 1 until n) {
                val separadas = kotlin.math.abs(celdas[i].first - celdas[j].first) >= 64f - 0.01f ||
                    kotlin.math.abs(celdas[i].second - celdas[j].second) >= 72f - 0.01f
                assertTrue("figuras $i y $j de $n se empalman", separadas)
            }
        }
    }

    @Test
    fun laCuadriculaEsCasiCuadrada() {
        assertEquals(1 to 1, MapaParlante.cuadricula(1))
        assertEquals(3 to 1, MapaParlante.cuadricula(3))
        assertEquals(2 to 2, MapaParlante.cuadricula(4))
        assertEquals(3 to 2, MapaParlante.cuadricula(6))
        assertEquals(4 to 3, MapaParlante.cuadricula(10))
        assertEquals(0 to 0, MapaParlante.cuadricula(0))
    }
}
