package com.ruralitos.app.domain.familiograma

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Edad que se dibuja dentro del símbolo: años, o meses con una "m" si es menor de un año. */
object EdadFamiliograma {
    fun desdeFechaNacimiento(fecha: String, hoy: Date = Date()): String? {
        val texto = fecha.trim()
        if (texto.isEmpty()) return null
        val nacimiento = listOf("dd/MM/yyyy", "yyyy-MM-dd").firstNotNullOfOrNull { patron ->
            runCatching {
                SimpleDateFormat(patron, Locale.ROOT).apply { isLenient = false }.parse(texto)
            }.getOrNull()
        } ?: return null
        val a = Calendar.getInstance().apply { time = nacimiento }
        val b = Calendar.getInstance().apply { time = hoy }
        if (b.before(a)) return null
        var meses = (b.get(Calendar.YEAR) - a.get(Calendar.YEAR)) * 12 + (b.get(Calendar.MONTH) - a.get(Calendar.MONTH))
        if (b.get(Calendar.DAY_OF_MONTH) < a.get(Calendar.DAY_OF_MONTH)) meses--
        if (meses < 0) return null
        return if (meses < 12) "${meses}m" else (meses / 12).toString()
    }
}
