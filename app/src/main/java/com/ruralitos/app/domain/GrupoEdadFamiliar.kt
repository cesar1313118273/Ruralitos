package com.ruralitos.app.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object GrupoEdadFamiliar {
    const val MENOR_UN_ANIO = "MENOR 1 AÑO"
    const val UNO_A_CUATRO = "1 - 4 AÑOS"
    const val CINCO_A_NUEVE = "5 - 9 AÑOS"
    const val DIEZ_A_DIECINUEVE = "10 - 19 AÑOS"
    const val VEINTE_A_SESENTA_Y_CUATRO = "20 - 64 AÑOS"
    const val SESENTA_Y_CINCO_MAS = "65 AÑOS Y MÁS"

    fun calcular(
        fechaNacimiento: String,
        fechaReferencia: Date = Date()
    ): String? {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply {
            isLenient = false
        }
        val nacimiento = runCatching { formato.parse(fechaNacimiento) }.getOrNull() ?: return null
        if (nacimiento.after(fechaReferencia)) return null

        val nacimientoCal = Calendar.getInstance().apply { time = nacimiento }
        val referenciaCal = Calendar.getInstance().apply { time = fechaReferencia }
        var edad = referenciaCal.get(Calendar.YEAR) - nacimientoCal.get(Calendar.YEAR)
        val mesReferencia = referenciaCal.get(Calendar.MONTH)
        val mesNacimiento = nacimientoCal.get(Calendar.MONTH)
        val aunNoCumple = mesReferencia < mesNacimiento ||
            (mesReferencia == mesNacimiento &&
                referenciaCal.get(Calendar.DAY_OF_MONTH) < nacimientoCal.get(Calendar.DAY_OF_MONTH))
        if (aunNoCumple) {
            edad--
        }

        return when (edad) {
            0 -> MENOR_UN_ANIO
            in 1..4 -> UNO_A_CUATRO
            in 5..9 -> CINCO_A_NUEVE
            in 10..19 -> DIEZ_A_DIECINUEVE
            in 20..64 -> VEINTE_A_SESENTA_Y_CUATRO
            else -> SESENTA_Y_CINCO_MAS
        }
    }
}
