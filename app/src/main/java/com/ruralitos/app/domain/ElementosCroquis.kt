package com.ruralitos.app.domain

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Símbolos que se pueden colocar sobre el mapa del croquis para señalar el entorno de la vivienda. */
enum class SimboloCroquis(val codigo: String, val etiqueta: String) {
    CASA("CASA", "Casa"),
    IGLESIA("IGLESIA", "Iglesia"),
    ESCUELA("ESCUELA", "Escuela"),
    PARQUE("PARQUE", "Parque"),
    CANCHA("CANCHA", "Cancha"),
    TIENDA("TIENDA", "Tienda"),
    SALUD("SALUD", "Centro de salud"),
    PARADA("PARADA", "Parada de bus"),
    RIO("RIO", "Río"),
    PUENTE("PUENTE", "Puente"),
    COMUNAL("COMUNAL", "Casa comunal"),
    MERCADO("MERCADO", "Mercado"),
    CEMENTERIO("CEMENTERIO", "Cementerio"),
    FABRICA("FABRICA", "Fábrica");

    companion object {
        fun deCodigo(codigo: String): SimboloCroquis? = entries.firstOrNull { it.codigo == codigo }
    }
}

enum class TipoElementoCroquis { SIMBOLO, TEXTO }

/**
 * Un símbolo (con su nombre debajo) o un texto libre colocado en el mapa. [codigo] es el de un [SimboloCroquis]
 * cuando es un símbolo; [texto] es el nombre del símbolo o la frase del texto.
 */
data class ElementoCroquis(
    val id: String = UUID.randomUUID().toString(),
    val tipo: TipoElementoCroquis,
    val codigo: String = "",
    val texto: String = "",
    val latitud: Double,
    val longitud: Double
) {
    /** Lo que se escribe debajo del símbolo, o la frase del texto. */
    val etiqueta: String get() = texto.ifBlank { SimboloCroquis.deCodigo(codigo)?.etiqueta.orEmpty() }
}

object ElementosCroquis {
    /** Tope por ficha, para que el croquis no se sature. */
    const val MAXIMO = 30
    const val MAXIMO_TEXTO = 40

    fun codificar(elementos: List<ElementoCroquis>): String = JSONArray().also { lista ->
        elementos.take(MAXIMO).forEach {
            lista.put(
                JSONObject()
                    .put("id", it.id).put("tipo", it.tipo.name).put("codigo", it.codigo)
                    .put("texto", it.texto).put("lat", it.latitud).put("lon", it.longitud)
            )
        }
    }.toString()

    /** Lee la lista guardada; lo que esté dañado o sea de un símbolo desconocido se omite. */
    fun decodificar(json: String?): List<ElementoCroquis> {
        if (json.isNullOrBlank()) return emptyList()
        val lista = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        return (0 until lista.length()).mapNotNull { indice ->
            val fila = lista.optJSONObject(indice) ?: return@mapNotNull null
            val tipo = runCatching { TipoElementoCroquis.valueOf(fila.optString("tipo")) }.getOrNull()
                ?: return@mapNotNull null
            val codigo = fila.optString("codigo")
            if (tipo == TipoElementoCroquis.SIMBOLO && SimboloCroquis.deCodigo(codigo) == null) return@mapNotNull null
            if (!fila.has("lat") || !fila.has("lon")) return@mapNotNull null
            val latitud = fila.optDouble("lat", Double.NaN)
            val longitud = fila.optDouble("lon", Double.NaN)
            if (latitud.isNaN() || longitud.isNaN()) return@mapNotNull null
            ElementoCroquis(
                id = fila.optString("id").ifBlank { UUID.randomUUID().toString() },
                tipo = tipo, codigo = codigo, texto = fila.optString("texto"),
                latitud = latitud, longitud = longitud
            )
        }.take(MAXIMO)
    }

    fun limpiarTexto(texto: String): String = texto.trim().replace(Regex("\\s+"), " ").take(MAXIMO_TEXTO)
}
