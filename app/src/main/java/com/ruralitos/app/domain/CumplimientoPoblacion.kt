package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Los grupos de edad de la tabla «Población asignada» (ciclos de vida). */
enum class GrupoCumplimiento(
    val titulo: String,
    val edadMinima: Int,
    val edadMaxima: Int,
    val icono: String
) {
    MENOR_UN_ANIO("Menores de 1 año", 0, 0, IconosMais.RIESGO_I_MENOR_DOS),
    UNO_A_CUATRO("1 a 4 años", 1, 4, IconosMais.RIESGO_I_DOS_NUEVE),
    CINCO_A_NUEVE("5 a 9 años", 5, 9, IconosMais.RIESGO_I_DOS_NUEVE),
    DIEZ_A_CATORCE("10 a 14 años", 10, 14, IconosMais.RIESGO_II_ADOLESCENTE),
    QUINCE_A_DIECINUEVE("15 a 19 años", 15, 19, IconosMais.RIESGO_II_ADOLESCENTE),
    VEINTE_A_SESENTA_Y_CUATRO("20 a 64 años", 20, 64, IconosMais.RIESGO_II_ADULTO),
    SESENTA_Y_CINCO_MAS("65 años y más", 65, Int.MAX_VALUE, IconosMais.RIESGO_II_ADULTO_MAYOR);

    companion object {
        fun deEdad(edad: Int): GrupoCumplimiento? = entries.firstOrNull { edad in it.edadMinima..it.edadMaxima }
    }
}

enum class SexoCumplimiento { HOMBRE, MUJER }

/** Lo que el usuario escribe: personas asignadas por grupo y sexo. */
data class PoblacionAsignada(val valores: Map<GrupoCumplimiento, Pair<Int, Int>> = emptyMap()) {
    fun hombres(grupo: GrupoCumplimiento) = valores[grupo]?.first ?: 0
    fun mujeres(grupo: GrupoCumplimiento) = valores[grupo]?.second ?: 0
    fun total(grupo: GrupoCumplimiento) = hombres(grupo) + mujeres(grupo)
    val totalHombres get() = GrupoCumplimiento.entries.sumOf { hombres(it) }
    val totalMujeres get() = GrupoCumplimiento.entries.sumOf { mujeres(it) }
    val total get() = totalHombres + totalMujeres
    val vacia get() = total == 0
}

enum class NivelCumplimiento { BAJO, MEDIO, ALTO, SIN_META }

data class CumplimientoGrupo(
    val grupo: GrupoCumplimiento,
    val asignadosHombres: Int,
    val asignadosMujeres: Int,
    val registradosHombres: Int,
    val registradosMujeres: Int
) {
    val asignados get() = asignadosHombres + asignadosMujeres
    val registrados get() = registradosHombres + registradosMujeres
    val faltan get() = (asignados - registrados).coerceAtLeast(0)
    /** Cuántos pasan de lo asignado (si hay más fichas que población asignada). */
    val sobran get() = (registrados - asignados).coerceAtLeast(0)
    val porcentaje get() = CumplimientoPoblacion.porcentaje(registrados, asignados)
    val nivel get() = CumplimientoPoblacion.nivel(registrados, asignados)
}

data class ResumenCumplimiento(
    val grupos: List<CumplimientoGrupo>,
    /** Personas con ficha que no entran en la tabla (sin fecha de nacimiento válida o sexo). */
    val sinClasificar: Int
) {
    val asignados get() = grupos.sumOf { it.asignados }
    val registrados get() = grupos.sumOf { it.registrados }
    val asignadosHombres get() = grupos.sumOf { it.asignadosHombres }
    val asignadosMujeres get() = grupos.sumOf { it.asignadosMujeres }
    val registradosHombres get() = grupos.sumOf { it.registradosHombres }
    val registradosMujeres get() = grupos.sumOf { it.registradosMujeres }
    val porcentaje get() = CumplimientoPoblacion.porcentaje(registrados, asignados)
    val nivel get() = CumplimientoPoblacion.nivel(registrados, asignados)
}

object CumplimientoPoblacion {
    const val CORTE_MEDIO = 50
    const val CORTE_ALTO = 80
    const val MAXIMO_POR_CASILLA = 99_999

    fun porcentaje(registrados: Int, asignados: Int): Int =
        if (asignados <= 0) 0 else (registrados * 100L / asignados).toInt()

    fun nivel(registrados: Int, asignados: Int): NivelCumplimiento = when {
        asignados <= 0 -> NivelCumplimiento.SIN_META
        porcentaje(registrados, asignados) >= CORTE_ALTO -> NivelCumplimiento.ALTO
        porcentaje(registrados, asignados) >= CORTE_MEDIO -> NivelCumplimiento.MEDIO
        else -> NivelCumplimiento.BAJO
    }

    /** Texto de una casilla: solo dígitos, sin ceros a la izquierda y con tope. */
    fun limpiarCasilla(texto: String): String =
        texto.filter(Char::isDigit).trimStart('0').take(5)

    fun sexoDe(valor: String): SexoCumplimiento? = when (valor.trim().uppercase(Locale.ROOT)) {
        "H", "HOMBRE", "MASCULINO" -> SexoCumplimiento.HOMBRE
        "M", "MUJER", "FEMENINO", "F" -> SexoCumplimiento.MUJER
        else -> null
    }

    fun edadEnAnios(fechaNacimiento: String, hoy: Date = Date()): Int? {
        val fecha = runCatching {
            SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply { isLenient = false }.parse(fechaNacimiento)
        }.getOrNull() ?: return null
        if (fecha.after(hoy)) return null
        val nacimiento = Calendar.getInstance().apply { time = fecha }
        val referencia = Calendar.getInstance().apply { time = hoy }
        var edad = referencia.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR)
        val aunNoCumple = referencia.get(Calendar.MONTH) < nacimiento.get(Calendar.MONTH) ||
            (referencia.get(Calendar.MONTH) == nacimiento.get(Calendar.MONTH) &&
                referencia.get(Calendar.DAY_OF_MONTH) < nacimiento.get(Calendar.DAY_OF_MONTH))
        if (aunNoCumple) edad--
        return edad
    }

    fun calcular(asignada: PoblacionAsignada, miembros: List<MiembroFamiliaEntity>, hoy: Date = Date()): ResumenCumplimiento {
        val hombres = mutableMapOf<GrupoCumplimiento, Int>()
        val mujeres = mutableMapOf<GrupoCumplimiento, Int>()
        var sinClasificar = 0
        miembros.forEach { miembro ->
            val grupo = edadEnAnios(miembro.fechaNacimiento, hoy)?.let(GrupoCumplimiento::deEdad)
            val sexo = sexoDe(miembro.sexo)
            when {
                grupo == null || sexo == null -> sinClasificar++
                sexo == SexoCumplimiento.HOMBRE -> hombres.merge(grupo, 1, Int::plus)
                else -> mujeres.merge(grupo, 1, Int::plus)
            }
        }
        return ResumenCumplimiento(
            grupos = GrupoCumplimiento.entries.map {
                CumplimientoGrupo(it, asignada.hombres(it), asignada.mujeres(it), hombres[it] ?: 0, mujeres[it] ?: 0)
            },
            sinClasificar = sinClasificar
        )
    }
}
