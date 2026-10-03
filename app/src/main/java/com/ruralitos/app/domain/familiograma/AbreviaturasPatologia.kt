package com.ruralitos.app.domain.familiograma

import java.text.Normalizer
import java.util.Locale

/** Una abreviatura que ya trae la ficha familiar. */
data class AbreviaturaFicha(val codigo: String, val nombre: String, val raices: List<String>)

/** Resultado de buscar la abreviatura de una patología. */
data class AbreviaturaResuelta(
    val codigo: String,
    val nombre: String,
    /** `true` si viene de la ficha; `false` si es una abreviatura nueva generada para esa patología. */
    val deFicha: Boolean,
    /** `true` si la abreviatura todavía no está guardada en el familiograma. */
    val pendiente: Boolean = false
)

/**
 * Abreviaturas de las patologías.
 *
 * Si la patología escrita es una de las que trae la ficha, se usa su abreviatura. Si no, se genera
 * una nueva (iniciales si son varias palabras, primeras letras si es una sola) que no se repita
 * con ninguna otra, y se anota en la lista que se dibuja junto al familiograma.
 */
object AbreviaturasPatologia {
    val FICHA: List<AbreviaturaFicha> = listOf(
        AbreviaturaFicha("IN", "Infarto", listOf("infart")),
        AbreviaturaFicha("HT", "Hipertensión arterial", listOf("hipertens", "hta")),
        AbreviaturaFicha("DI", "Diabetes", listOf("diabet")),
        AbreviaturaFicha("CA", "Cáncer", listOf("cancer", "carcinoma", "tumor maligno")),
        AbreviaturaFicha("EP", "Epilepsia", listOf("epilep")),
        AbreviaturaFicha("TB", "Tuberculosis", listOf("tubercul", "tbc")),
        AbreviaturaFicha("AS", "Asma", listOf("asma")),
        AbreviaturaFicha("TA", "Tabaquismo", listOf("tabaq")),
        AbreviaturaFicha("AL", "Alcoholismo", listOf("alcohol")),
        AbreviaturaFicha("DD", "Drogodependencia", listOf("droga", "drogo")),
        AbreviaturaFicha("DC", "Discapacidad", listOf("discapac")),
        AbreviaturaFicha("MG", "Migrante", listOf("migrant")),
        AbreviaturaFicha("EM", "Embarazo", listOf("embaraz"))
    )

    private val palabrasVacias = setOf("de", "del", "la", "el", "los", "las", "y", "e", "en", "por", "con", "a", "al")
    private const val LARGO_MAXIMO = 5

    fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .trim()

    fun nombreVisible(texto: String): String {
        val limpio = texto.trim().replace(Regex("\\s+"), " ")
        return limpio.replaceFirstChar { it.titlecase(Locale.ROOT) }
    }

    /** Abreviatura de la ficha que corresponde al texto, o `null` si la ficha no la trae. */
    fun buscarEnFicha(texto: String): AbreviaturaFicha? {
        val normal = normalizar(texto)
        if (normal.isEmpty()) return null
        val palabras = normal.split(' ')
        return FICHA.firstOrNull { entrada ->
            entrada.raices.any { raiz ->
                if (' ' in raiz) normal.contains(raiz)
                else palabras.any { it.startsWith(raiz) }
            }
        }
    }

    /** Códigos ya ocupados: los de la ficha más los nuevos del familiograma. */
    private fun codigosEnUso(nuevas: List<PatologiaNueva>, excepto: String? = null): Set<String> =
        buildSet {
            FICHA.forEach { add(it.codigo) }
            add("X")
            nuevas.filter { excepto == null || normalizar(it.nombre) != excepto }.forEach { add(it.codigo) }
        }

    /** Propone un código nuevo, único, para [nombre]. */
    fun generarCodigo(nombre: String, nuevas: List<PatologiaNueva>): String {
        val ocupados = codigosEnUso(nuevas, excepto = normalizar(nombre))
        val palabras = normalizar(nombre)
            .split(Regex("[^a-z0-9ñ]+"))
            .filter { it.isNotBlank() && it !in palabrasVacias }
        val primera = palabras.firstOrNull() ?: "x"
        val candidatos = buildList {
            if (palabras.size > 1) add(palabras.map { it.first() }.joinToString("").take(4))
            for (largo in 3..LARGO_MAXIMO) add(primera.take(largo))
            if (palabras.size > 1) add(palabras.joinToString("") { it.take(2) }.take(6))
        }
        candidatos.map { it.uppercase(Locale.ROOT) }
            .firstOrNull { it.length >= 2 && it !in ocupados }
            ?.let { return it }
        val base = (candidatos.firstOrNull() ?: "X").uppercase(Locale.ROOT)
        var n = 2
        while ("$base$n" in ocupados) n++
        return "$base$n"
    }

    /** Busca (o propone) la abreviatura de [nombre] sin modificar el familiograma. */
    fun resolver(nombre: String, nuevas: List<PatologiaNueva>): AbreviaturaResuelta {
        buscarEnFicha(nombre)?.let { return AbreviaturaResuelta(it.codigo, it.nombre, deFicha = true) }
        val clave = normalizar(nombre)
        nuevas.firstOrNull { normalizar(it.nombre) == clave }?.let {
            return AbreviaturaResuelta(it.codigo, it.nombre, deFicha = false)
        }
        return AbreviaturaResuelta(
            codigo = generarCodigo(nombre, nuevas),
            nombre = nombreVisible(nombre),
            deFicha = false,
            pendiente = true
        )
    }

    fun codigoDe(doc: Familiograma, nombre: String): String =
        resolver(nombre, doc.patologiasNuevas).codigo

    /** Anota en el familiograma la abreviatura nueva de [nombre] (si la ficha no la trae). */
    fun registrar(doc: Familiograma, nombre: String, codigoElegido: String? = null): Familiograma {
        if (buscarEnFicha(nombre) != null) return doc
        val clave = normalizar(nombre)
        if (clave.isEmpty()) return doc
        val existente = doc.patologiasNuevas.firstOrNull { normalizar(it.nombre) == clave }
        val codigo = codigoElegido?.let(::limpiarCodigo)?.takeIf { it.isNotEmpty() }
            ?.takeIf { esCodigoLibre(it, doc.patologiasNuevas, clave) }
            ?: existente?.codigo
            ?: generarCodigo(nombre, doc.patologiasNuevas)
        val nueva = PatologiaNueva(existente?.nombre ?: nombreVisible(nombre), codigo)
        val lista = if (existente == null) doc.patologiasNuevas + nueva
        else doc.patologiasNuevas.map { if (normalizar(it.nombre) == clave) nueva else it }
        return doc.copy(patologiasNuevas = lista)
    }

    fun limpiarCodigo(texto: String): String =
        texto.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() }.take(LARGO_MAXIMO)

    fun esCodigoLibre(codigo: String, nuevas: List<PatologiaNueva>, nombreNormalizado: String): Boolean =
        codigo.length >= 2 && codigo !in codigosEnUso(nuevas, excepto = nombreNormalizado)

    /** Abreviaturas de la ficha cuyo nombre empieza como el texto escrito (para sugerir mientras se escribe). */
    fun sugerencias(texto: String, limite: Int = 3): List<AbreviaturaFicha> {
        val normal = normalizar(texto)
        if (normal.length < 2) return emptyList()
        return FICHA.filter { normalizar(it.nombre).startsWith(normal) || it.codigo.equals(normal, ignoreCase = true) }
            .take(limite)
    }

    /** Patologías con abreviatura nueva que usa realmente alguna persona (para la lista del dibujo). */
    fun nuevasEnUso(doc: Familiograma): List<PatologiaNueva> {
        val enUso = doc.personas.flatMapTo(mutableSetOf()) { p -> p.patologias.map { normalizar(it) } }
        return doc.patologiasNuevas.filter { normalizar(it.nombre) in enUso }
    }

    /** Códigos de la ficha que aparecen en el dibujo, en el orden de la ficha. */
    fun codigosDeFichaEnUso(doc: Familiograma): List<AbreviaturaFicha> {
        val usados = doc.personas.flatMapTo(mutableSetOf()) { p ->
            p.patologias.mapNotNull { buscarEnFicha(it)?.codigo }
        }
        return FICHA.filter { it.codigo in usados }
    }
}
