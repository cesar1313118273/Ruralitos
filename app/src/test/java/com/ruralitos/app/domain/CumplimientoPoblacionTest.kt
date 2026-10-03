package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class CumplimientoPoblacionTest {
    private val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).parse("15/06/2026")!!

    private fun miembro(nacimiento: String, sexo: String) = MiembroFamiliaEntity(
        fichaId = 1, grupoEdad = "", apellidosNombres = "X", parentesco = "", fechaNacimiento = nacimiento,
        ocupacion = "", sexo = sexo, escolaridad = ""
    )

    @Test
    fun cadaEdadCaeEnSuGrupo() {
        assertEquals(GrupoCumplimiento.MENOR_UN_ANIO, GrupoCumplimiento.deEdad(0))
        assertEquals(GrupoCumplimiento.UNO_A_CUATRO, GrupoCumplimiento.deEdad(1))
        assertEquals(GrupoCumplimiento.UNO_A_CUATRO, GrupoCumplimiento.deEdad(4))
        assertEquals(GrupoCumplimiento.CINCO_A_NUEVE, GrupoCumplimiento.deEdad(5))
        assertEquals(GrupoCumplimiento.DIEZ_A_CATORCE, GrupoCumplimiento.deEdad(14))
        assertEquals(GrupoCumplimiento.QUINCE_A_DIECINUEVE, GrupoCumplimiento.deEdad(15))
        assertEquals(GrupoCumplimiento.QUINCE_A_DIECINUEVE, GrupoCumplimiento.deEdad(19))
        assertEquals(GrupoCumplimiento.VEINTE_A_SESENTA_Y_CUATRO, GrupoCumplimiento.deEdad(20))
        assertEquals(GrupoCumplimiento.VEINTE_A_SESENTA_Y_CUATRO, GrupoCumplimiento.deEdad(64))
        assertEquals(GrupoCumplimiento.SESENTA_Y_CINCO_MAS, GrupoCumplimiento.deEdad(65))
        assertEquals(GrupoCumplimiento.SESENTA_Y_CINCO_MAS, GrupoCumplimiento.deEdad(104))
        assertNull(GrupoCumplimiento.deEdad(-1))
    }

    @Test
    fun laEdadDependeDelCumpleanios() {
        assertEquals(0, CumplimientoPoblacion.edadEnAnios("16/06/2025", hoy))
        assertEquals(1, CumplimientoPoblacion.edadEnAnios("15/06/2025", hoy))
        assertNull(CumplimientoPoblacion.edadEnAnios("01/01/2030", hoy))
        assertNull(CumplimientoPoblacion.edadEnAnios("31/02/2020", hoy))
        assertNull(CumplimientoPoblacion.edadEnAnios("", hoy))
    }

    @Test
    fun comparaLasFichasConLaPoblacionAsignada() {
        val asignada = PoblacionAsignada(
            mapOf(
                GrupoCumplimiento.MENOR_UN_ANIO to (12 to 14),
                GrupoCumplimiento.VEINTE_A_SESENTA_Y_CUATRO to (80 to 94)
            )
        )
        val miembros = listOf(
            miembro("01/03/2026", "H"),
            miembro("01/03/2026", "M"),
            miembro("01/03/2026", "M"),
            miembro("10/10/1990", "H"),
            miembro("10/10/1990", "Mujer"),
            miembro("", "H"),
            miembro("10/10/1990", "")
        )
        val r = CumplimientoPoblacion.calcular(asignada, miembros, hoy)
        val bebes = r.grupos.first { it.grupo == GrupoCumplimiento.MENOR_UN_ANIO }
        assertEquals(26, bebes.asignados)
        assertEquals(3, bebes.registrados)
        assertEquals(1, bebes.registradosHombres)
        assertEquals(2, bebes.registradosMujeres)
        assertEquals(23, bebes.faltan)
        assertEquals(11, bebes.porcentaje)
        assertEquals(NivelCumplimiento.BAJO, bebes.nivel)
        assertEquals(2, r.sinClasificar)
        assertEquals(200, r.asignados)
        assertEquals(5, r.registrados)
        assertEquals(2, r.registradosHombres)
        assertEquals(3, r.registradosMujeres)
    }

    @Test
    fun losNivelesUsanLosCortes() {
        assertEquals(NivelCumplimiento.SIN_META, CumplimientoPoblacion.nivel(5, 0))
        assertEquals(NivelCumplimiento.BAJO, CumplimientoPoblacion.nivel(49, 100))
        assertEquals(NivelCumplimiento.MEDIO, CumplimientoPoblacion.nivel(50, 100))
        assertEquals(NivelCumplimiento.MEDIO, CumplimientoPoblacion.nivel(79, 100))
        assertEquals(NivelCumplimiento.ALTO, CumplimientoPoblacion.nivel(80, 100))
        assertEquals(NivelCumplimiento.ALTO, CumplimientoPoblacion.nivel(150, 100))
    }

    @Test
    fun conMasFichasQueAsignadosNoFaltanYSobran() {
        val g = CumplimientoGrupo(GrupoCumplimiento.CINCO_A_NUEVE, 5, 5, 8, 7)
        assertEquals(0, g.faltan)
        assertEquals(5, g.sobran)
        assertEquals(150, g.porcentaje)
    }

    @Test
    fun laCasillaSoloAceptaDigitos() {
        assertEquals("12", CumplimientoPoblacion.limpiarCasilla("012"))
        assertEquals("45", CumplimientoPoblacion.limpiarCasilla("4a5-"))
        assertEquals("", CumplimientoPoblacion.limpiarCasilla("000"))
        assertEquals("12345", CumplimientoPoblacion.limpiarCasilla("1234567"))
    }

    @Test
    fun laTablaSumaCiclosDeVida() {
        val a = PoblacionAsignada(
            mapOf(
                GrupoCumplimiento.MENOR_UN_ANIO to (12 to 14),
                GrupoCumplimiento.UNO_A_CUATRO to (15 to 18),
                GrupoCumplimiento.CINCO_A_NUEVE to (19 to 22),
                GrupoCumplimiento.DIEZ_A_CATORCE to (13 to 17),
                GrupoCumplimiento.QUINCE_A_DIECINUEVE to (12 to 17),
                GrupoCumplimiento.VEINTE_A_SESENTA_Y_CUATRO to (80 to 94),
                GrupoCumplimiento.SESENTA_Y_CINCO_MAS to (25 to 30)
            )
        )
        assertEquals(176, a.totalHombres)
        assertEquals(212, a.totalMujeres)
        assertEquals(388, a.total)
    }
}
