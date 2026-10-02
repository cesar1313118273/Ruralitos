package com.ruralitos.app.domain

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

data class DiagnosticoCie10(
    val codigo: String,
    val descripcion: String,
    val descompensada: Boolean = false
) {
    val etiqueta: String get() = "$codigo · $descripcion"
}

/** Catálogo local: se carga una sola vez y nunca necesita internet. */
object CatalogoCie10 {
    @Volatile private var cache: List<DiagnosticoCie10>? = null
    @Volatile private var indiceCache: IndiceCie10? = null

    private data class EntradaCie10(
        val diagnostico: DiagnosticoCie10,
        val codigo: String,
        val descripcion: String
    )

    private class IndiceCie10(catalogo: List<DiagnosticoCie10>) {
        val entradas = catalogo.map { diagnostico ->
            EntradaCie10(
                diagnostico = diagnostico,
                codigo = normalizar(diagnostico.codigo),
                descripcion = normalizar(diagnostico.descripcion)
            )
        }
    }

    fun cargar(context: Context): List<DiagnosticoCie10> = cache ?: synchronized(this) {
        cache ?: context.assets.open("cie10.json").bufferedReader().use { lector ->
            val json = JSONArray(lector.readText())
            List(json.length()) { indice ->
                val item = json.getJSONObject(indice)
                DiagnosticoCie10(item.getString("codigo_mostrar"), item.getString("descripcion"))
            }.also {
                // El costo de normalizar el catálogo se paga una sola vez durante la carga en IO.
                indiceCache = IndiceCie10(it)
                cache = it
            }
        }
    }

    fun buscar(catalogo: List<DiagnosticoCie10>, consulta: String, limite: Int = 12): List<DiagnosticoCie10> {
        val q = normalizar(consulta)
        if (q.length < 2) return emptyList()
        val tokens = q.split(' ').filter(String::isNotBlank)
        val indice = if (catalogo === cache) {
            indiceCache ?: IndiceCie10(catalogo).also { indiceCache = it }
        } else {
            IndiceCie10(catalogo)
        }
        val coincidencias = Array(4) { mutableListOf<DiagnosticoCie10>() }
        indice.entradas.forEach { entrada ->
            if (tokens.all { token -> token in entrada.codigo || token in entrada.descripcion }) {
                val prioridad = when {
                    entrada.codigo == q -> 0
                    entrada.codigo.startsWith(q) -> 1
                    entrada.descripcion.startsWith(q) -> 2
                    else -> 3
                }
                coincidencias[prioridad] += entrada.diagnostico
            }
        }
        return coincidencias.asSequence()
            .flatMap { grupo -> grupo.asSequence().sortedBy { it.codigo } }
            .take(limite.coerceAtLeast(0))
            .toList()
    }

    fun decodificar(json: String): List<DiagnosticoCie10> = runCatching {
        val arreglo = JSONArray(json)
        List(arreglo.length()) { i ->
            arreglo.getJSONObject(i).let {
                DiagnosticoCie10(
                    codigo = it.getString("codigo"),
                    descripcion = it.getString("descripcion"),
                    descompensada = it.optBoolean("descompensada", false)
                )
            }
        }
    }.getOrDefault(emptyList())

    fun codificar(items: List<DiagnosticoCie10>): String = JSONArray().apply {
        items.distinctBy { it.codigo }.forEach {
            put(
                JSONObject()
                    .put("codigo", it.codigo)
                    .put("descripcion", it.descripcion)
                    .put("descompensada", it.descompensada)
            )
        }
    }.toString()

    private val diacriticos = Regex("\\p{Mn}+")

    private fun normalizar(texto: String): String = Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace(diacriticos, "").uppercase(Locale.ROOT).trim()
}
