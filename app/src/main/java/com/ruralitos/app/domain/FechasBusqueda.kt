package com.ruralitos.app.domain

/**
 * Regla de fechas de las búsquedas: sin elegir fechas se ve solo lo de HOY (el día en que se mira), de modo que al
 * pasar la medianoche la lista del día anterior desaparece sola y lo antiguo se busca eligiendo fechas.
 * Las fechas se escriben como `dd/MM/yyyy`.
 */
object FechasBusqueda {
    /** Fechas con las que se consulta: `todas` quita el límite; sin fechas elegidas es el día de [hoy]. */
    fun efectivas(inicio: String, fin: String, todas: Boolean, hoy: String): Pair<String, String> = when {
        todas -> "" to ""
        inicio.isBlank() && fin.isBlank() -> hoy to hoy
        else -> inicio to fin
    }

    /** Es la vista del día: no se eligieron fechas ni «todas las fechas». */
    fun esDelDia(inicio: String, fin: String, todas: Boolean): Boolean = !todas && inicio.isBlank() && fin.isBlank()

    /** Frase que dice qué fechas se están mostrando. */
    fun descripcion(inicio: String, fin: String, todas: Boolean, hoy: String): String {
        val (desde, hasta) = efectivas(inicio, fin, todas, hoy)
        return when {
            todas -> "Mostrando fichas de todas las fechas"
            esDelDia(inicio, fin, false) -> "Mostrando solo las fichas de hoy, $hoy"
            desde == hasta -> "Mostrando las fichas del $desde"
            desde.isBlank() -> "Mostrando las fichas hasta el $hasta"
            hasta.isBlank() -> "Mostrando las fichas desde el $desde"
            else -> "Mostrando las fichas del $desde al $hasta"
        }
    }

    /** `dd/MM/yyyy` → `yyyyMMdd` para comparar como texto; vacío si no es una fecha. */
    fun clave(fecha: String): String {
        val partes = fecha.split('/')
        if (partes.size != 3) return ""
        val dia = partes[0].toIntOrNull() ?: return ""
        val mes = partes[1].toIntOrNull() ?: return ""
        val anio = partes[2].toIntOrNull() ?: return ""
        return String.format(java.util.Locale.US, "%04d%02d%02d", anio, mes, dia)
    }
}
