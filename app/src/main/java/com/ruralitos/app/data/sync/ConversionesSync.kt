package com.ruralitos.app.data.sync

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Lectura tolerante de filas JSON que llegan de Supabase. */
internal fun JSONObject.texto(clave: String): String =
    if (!has(clave) || isNull(clave)) "" else optString(clave, "")

internal fun JSONObject.intNullable(clave: String): Int? =
    if (!has(clave) || isNull(clave)) null else optInt(clave)

internal fun JSONObject.doubleNullable(clave: String): Double? =
    if (!has(clave) || isNull(clave)) null else optDouble(clave)

internal fun JSONObject.booleanNullable(clave: String): Boolean? =
    if (!has(clave) || isNull(clave)) null else optBoolean(clave)

/** Una fila con `deleted_at` es una baja lógica que hay que propagar al teléfono. */
internal fun JSONObject.estaEliminada(): Boolean = has("deleted_at") && !isNull("deleted_at")

internal object ConversionesSync {
    private val FORMATO_ISO get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    private val FORMATO_LOCAL get() = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }

    private val FORMATOS_HORA_ENTRADA get() = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply { isLenient = false },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply { isLenient = false },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
    )

    fun fechaLocal(valor: String): String {
        if (valor.isBlank()) return ""
        if (valor.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) return valor
        return runCatching {
            val fecha = FORMATO_ISO.parse(valor) ?: return@runCatching ""
            FORMATO_LOCAL.format(fecha)
        }.getOrDefault("")
    }

    fun timestampMillis(valor: String, fallback: Long): Long {
        if (valor.isBlank()) return fallback
        val normalizado = valor.replace(Regex("(\\.\\d{3})\\d+"), "\$1")
        FORMATOS_HORA_ENTRADA.forEach { formato ->
            runCatching { formato.parse(normalizado)?.time }.getOrNull()?.let { return it }
        }
        return fallback
    }

    /**
     * Retrocede [segundos] a una marca del servidor (por ejemplo `2026-10-02T10:11:12.123456+00:00`).
     * Devuelve nulo si no se puede interpretar, y entonces se hace una descarga completa.
     */
    fun retroceder(marca: String, segundos: Long): String? {
        val millis = timestampMillis(marca, Long.MIN_VALUE)
        if (millis == Long.MIN_VALUE) return null
        val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formato.format(java.util.Date(millis - segundos * 1000L))
    }
}
