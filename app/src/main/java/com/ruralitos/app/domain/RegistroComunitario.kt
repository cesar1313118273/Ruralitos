package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AgrupacionRegistroComunitario(
    val titulo: String,
    val fichaIds: Set<Long>
)

data class ColumnaRegistroComunitario(
    val titulo: String,
    /** Índice por número de fila real de la primera hoja (1..42). */
    val valores: Map<Int, Int>
) {
    fun valor(fila: Int): Int = valores[fila] ?: 0
}

data class ResumenRegistroComunitario(
    val grupos: List<Pair<String, Int>>,
    val estrategias: List<Pair<String, Int>>,
    val alertas: List<Pair<String, Int>>,
    val prestadores: List<Pair<String, Int>>
)

object CalculadorRegistroComunitario {
    const val MAXIMO_COLUMNAS = 300

    val filasNumericas = (3..5) + (7..12) + (14..20) + (22..25) +
        (27..33) + (35..38) + (40..42)

    fun calcular(
        agrupaciones: List<AgrupacionRegistroComunitario>,
        miembros: List<MiembroFamiliaEntity>,
        embarazadas: List<EmbarazadaEntity>
    ): List<ColumnaRegistroComunitario> = agrupaciones
        .map { agrupacion ->
            val filas = filasNumericas.associateWith { 0 }.toMutableMap()
            miembros.asSequence()
                .filter { it.fichaId in agrupacion.fichaIds }
                .forEach { miembro ->
                    val embarazo = DispensarizacionAutomatica.buscarEmbarazo(
                        miembro,
                        embarazadas.filter { it.fichaId == miembro.fichaId }
                    )
                    acumular(miembro, embarazo, filas)
                }
            ColumnaRegistroComunitario(agrupacion.titulo, filas)
        }

    fun resumir(columnas: List<ColumnaRegistroComunitario>): ResumenRegistroComunitario {
        fun total(fila: Int) = columnas.sumOf { it.valor(fila) }
        fun total(rango: IntRange) = rango.sumOf(::total)
        return ResumenRegistroComunitario(
            grupos = listOf(
                "Grupo I" to total(3..5),
                "Grupo II" to total(7..12),
                "Grupo III" to total(14..20),
                "Grupo IV" to total(22..25)
            ),
            estrategias = listOf(
                "Desnutrición crónica" to total(27),
                "Salud mental" to total(28),
                "Cuidados paliativos" to total(29),
                "Tuberculosis" to total(30),
                "Diabetes" to total(31),
                "Hipertensión" to total(32),
                "VIH" to total(33)
            ),
            alertas = listOf(
                "Eventos de salud" to total(35),
                "Casos confirmados" to total(36),
                "Casos sospechosos 1" to total(37),
                "Casos sospechosos 2" to total(38)
            ),
            prestadores = listOf(
                "Prestadores comunitarios" to total(40),
                "Parteras/os ancestrales" to total(41),
                "Sabiduría ancestral" to total(42)
            )
        )
    }

    private fun acumular(
        miembro: MiembroFamiliaEntity,
        embarazo: EmbarazadaEntity?,
        filas: MutableMap<Int, Int>
    ) {
        val resultado = DispensarizacionAutomatica.clasificar(miembro, embarazo)
        val edadMeses = edadEnMeses(miembro.fechaNacimiento)

        when (resultado.grupo) {
            GrupoDispensarizacion.I -> filaPorGrupoEdad(edadMeses, embarazo != null, 3, 4, null, null, null, 5)
            GrupoDispensarizacion.II -> filaPorGrupoEdad(edadMeses, embarazo != null, 7, 8, 9, 10, 11, 12)
            GrupoDispensarizacion.III -> when {
                embarazo != null && edadMeses != null && edadMeses in 120..239 -> 20
                embarazo != null -> 19
                else -> filaPorGrupoEdad(edadMeses, false, 14, 15, 16, 17, 18, null)
            }
            else -> null
        }?.let { incrementar(filas, it) }

        if (resultado.grupo == GrupoDispensarizacion.IV) {
            if (miembro.discapacidadFisica == true) incrementar(filas, 22)
            if (
                miembro.discapacidadVisual == true || miembro.discapacidadAuditiva == true ||
                miembro.discapacidadLenguaje == true
            ) incrementar(filas, 23)
            if (miembro.discapacidadIntelectual == true) incrementar(filas, 24)
            if (miembro.discapacidadPsicosocial == true) incrementar(filas, 25)
        }

        if (miembro.estadoNutricional == DispensarizacionAutomatica.NUTRICION_DESNUTRICION_CRONICA) incrementar(filas, 27)
        if (miembro.problemaSaludMental == true) incrementar(filas, 28)
        if (miembro.cuidadosPaliativos == true) incrementar(filas, 29)
        if (miembro.tuberculosis == true) incrementar(filas, 30)
        if (miembro.diabetesMellitus == true) incrementar(filas, 31)
        if (miembro.hipertensionArterial == true) incrementar(filas, 32)
        if (miembro.vih == true) incrementar(filas, 33)
        if (miembro.eventoSalud == true) incrementar(filas, 35)
        if (miembro.casoConfirmado == true) incrementar(filas, 36)
        if (miembro.casoSospechosoUno == true) incrementar(filas, 37)
        if (miembro.casoSospechosoDos == true) incrementar(filas, 38)
        if (miembro.prestadorComunitario == true) incrementar(filas, 40)
        if (miembro.parteroAncestral == true) incrementar(filas, 41)
        if (miembro.sabiduriaAncestral == true) incrementar(filas, 42)
    }

    private fun filaPorGrupoEdad(
        edadMeses: Int?,
        embarazada: Boolean,
        menorDos: Int?,
        dosNueve: Int?,
        adolescente: Int?,
        adulto: Int?,
        adultoMayor: Int?,
        filaEmbarazo: Int?
    ): Int? = when {
        embarazada -> filaEmbarazo
        edadMeses == null -> null
        edadMeses in 0..23 -> menorDos
        edadMeses in 24..119 -> dosNueve
        edadMeses in 120..239 -> adolescente
        edadMeses in 240..779 -> adulto
        edadMeses >= 780 -> adultoMayor
        else -> null
    }

    private fun incrementar(filas: MutableMap<Int, Int>, fila: Int) {
        filas[fila] = (filas[fila] ?: 0) + 1
    }

    private fun edadEnMeses(fechaNacimiento: String, hoy: Date = Date()): Int? {
        val fecha = runCatching {
            SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply { isLenient = false }
                .parse(fechaNacimiento)
        }.getOrNull() ?: return null
        if (fecha.after(hoy)) return null
        val nacimiento = Calendar.getInstance().apply { time = fecha }
        val referencia = Calendar.getInstance().apply { time = hoy }
        var meses = (referencia.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR)) * 12 +
            referencia.get(Calendar.MONTH) - nacimiento.get(Calendar.MONTH)
        if (referencia.get(Calendar.DAY_OF_MONTH) < nacimiento.get(Calendar.DAY_OF_MONTH)) meses--
        return meses.coerceAtLeast(0)
    }
}
