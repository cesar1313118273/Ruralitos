package com.ruralitos.app.domain.familiograma

/**
 * Modelo del familiograma dibujado dentro de la app.
 *
 * Todo es inmutable: cada cambio crea una copia, lo que permite deshacer/rehacer guardando
 * simplemente los estados anteriores. Las coordenadas están en "unidades de lienzo"; la vista
 * las escala al tamaño de la pantalla y la exportación al tamaño del recuadro del Excel.
 */

/** Mitad del lado del cuadrado (o radio del círculo) de una persona. */
const val MEDIA_PERSONA = 18f

/** Mitad del lado del recuadro de un ícono del entorno. */
const val MEDIA_ENTORNO = 23f

enum class Lado(val clave: String) {
    ARRIBA("t"),
    DERECHA("r"),
    ABAJO("b"),
    IZQUIERDA("l");

    val horizontal: Boolean get() = this == DERECHA || this == IZQUIERDA

    companion object {
        fun desde(clave: String): Lado = entries.firstOrNull { it.clave == clave } ?: ARRIBA
    }
}

data class Punto(val x: Float, val y: Float)

/** Uno de los cuatro puntos de conexión de una persona o de un ícono del entorno. */
data class Ancla(val elementoId: String, val lado: Lado)

enum class SexoPersona(val clave: String) {
    HOMBRE("H"),
    MUJER("M");

    companion object {
        fun desde(clave: String): SexoPersona =
            if (clave.trim().uppercase().startsWith("M")) MUJER else HOMBRE
    }
}

data class Persona(
    val id: String,
    val sexo: SexoPersona,
    val x: Float,
    val y: Float,
    val edad: String = "",
    val patologias: List<String> = emptyList(),
    val fallecido: Boolean = false,
    val informante: Boolean = false,
    val enHogar: Boolean = true,
    val nombre: String = "",
    val parentesco: String = ""
)

enum class TipoUnion(val etiqueta: String) {
    MATRIMONIO("Matrimonio o unión legítima"),
    SEPARACION("Separación"),
    DIVORCIO("Divorcio"),
    CONSANGUINEA("Unión consanguínea")
}

/** Línea entre dos personas (pareja). */
data class Union(
    val id: String,
    val a: Ancla,
    val b: Ancla,
    val tipo: TipoUnion = TipoUnion.MATRIMONIO
)

/**
 * Línea que baja desde una unión (o desde un solo progenitor) hasta un hijo.
 * Si [unionId] es nulo, nace del ancla [progenitor].
 */
data class Filiacion(
    val id: String,
    val hijo: Ancla,
    val unionId: String? = null,
    val progenitor: Ancla? = null,
    val adoptado: Boolean = false
)

/** Símbolo de aborto: cuelga de una unión y termina en un punto. */
data class Aborto(val id: String, val unionId: String, val x: Float, val y: Float)

enum class TipoEntorno(val etiqueta: String) {
    IGLESIA("Iglesia"),
    ESCUELA("Escuela"),
    CENTRO_SALUD("Centro de salud"),
    MERCADO("Mercado"),
    CANCHA("Cancha"),
    FINCA("Finca"),
    JUNTA("Junta comunal"),
    TRABAJO("Trabajo"),
    RIO("Río o agua"),
    TRANSPORTE("Transporte"),
    PARQUE("Parque")
}

/** Ícono de una actividad o lugar de la comunidad (iglesia, escuela…). */
data class Entorno(
    val id: String,
    val tipo: TipoEntorno,
    val x: Float,
    val y: Float,
    val etiqueta: String = tipo.etiqueta
)

/** Línea punteada entre dos elementos (por ejemplo, una persona y la escuela). */
data class Vinculo(val id: String, val a: Ancla, val b: Ancla)

enum class TipoTrazo { LAPIZ, LINEA, FLECHA, ENCIERRO }

/** Dibujo libre: lápiz, línea recta, flecha o encierro (óvalo) punteado. */
data class Trazo(
    val id: String,
    val tipo: TipoTrazo,
    val puntos: List<Punto>,
    val color: Int = COLOR_TINTA,
    val grosor: Float = 2.4f,
    val punteado: Boolean = false
)

data class Texto(val id: String, val x: Float, val y: Float, val texto: String)

/** Abreviatura creada por el profesional para una patología que no está en la ficha. */
data class PatologiaNueva(val nombre: String, val codigo: String)

data class Familiograma(
    val personas: List<Persona> = emptyList(),
    val uniones: List<Union> = emptyList(),
    val filiaciones: List<Filiacion> = emptyList(),
    val abortos: List<Aborto> = emptyList(),
    val entornos: List<Entorno> = emptyList(),
    val vinculos: List<Vinculo> = emptyList(),
    val trazos: List<Trazo> = emptyList(),
    val textos: List<Texto> = emptyList(),
    val patologiasNuevas: List<PatologiaNueva> = emptyList(),
    val mostrarNombres: Boolean = false
) {
    fun persona(id: String): Persona? = personas.firstOrNull { it.id == id }
    fun entorno(id: String): Entorno? = entornos.firstOrNull { it.id == id }
    fun union(id: String): Union? = uniones.firstOrNull { it.id == id }

    val vacio: Boolean
        get() = personas.isEmpty() && entornos.isEmpty() && trazos.isEmpty() && textos.isEmpty()

    // ---- ediciones sencillas ---------------------------------------------------------

    fun conPersona(nueva: Persona): Familiograma =
        copy(personas = personas.map { if (it.id == nueva.id) nueva else it })

    fun moverElemento(id: String, x: Float, y: Float): Familiograma = copy(
        personas = personas.map { if (it.id == id) it.copy(x = x, y = y) else it },
        entornos = entornos.map { if (it.id == id) it.copy(x = x, y = y) else it },
        textos = textos.map { if (it.id == id) it.copy(x = x, y = y) else it },
        abortos = abortos.map { if (it.id == id) it.copy(x = x, y = y) else it }
    )

    /** Quita cualquier elemento y todo lo que dependa de él (líneas, hijos, abortos). */
    fun quitar(id: String): Familiograma {
        val unionesQuitadas = uniones.filter { it.id == id || it.a.elementoId == id || it.b.elementoId == id }
            .mapTo(mutableSetOf()) { it.id }
        return copy(
            personas = personas.filterNot { it.id == id },
            entornos = entornos.filterNot { it.id == id },
            textos = textos.filterNot { it.id == id },
            trazos = trazos.filterNot { it.id == id },
            uniones = uniones.filterNot { it.id in unionesQuitadas },
            filiaciones = filiaciones.filterNot {
                it.id == id || it.hijo.elementoId == id || it.progenitor?.elementoId == id ||
                    it.unionId in unionesQuitadas
            },
            abortos = abortos.filterNot { it.id == id || it.unionId in unionesQuitadas },
            vinculos = vinculos.filterNot { it.id == id || it.a.elementoId == id || it.b.elementoId == id }
        ).limpiarPatologias()
    }

    /** Descarta las abreviaturas nuevas que ya no usa ninguna persona. */
    fun limpiarPatologias(): Familiograma {
        val enUso = personas.flatMapTo(mutableSetOf()) { p -> p.patologias.map { AbreviaturasPatologia.normalizar(it) } }
        val vigentes = patologiasNuevas.filter { AbreviaturasPatologia.normalizar(it.nombre) in enUso }
        return if (vigentes.size == patologiasNuevas.size) this else copy(patologiasNuevas = vigentes)
    }

    /** Identificador nuevo y único dentro de este familiograma. */
    fun nuevoId(prefijo: String): String {
        val usados = buildSet {
            personas.forEach { add(it.id) }
            uniones.forEach { add(it.id) }
            filiaciones.forEach { add(it.id) }
            abortos.forEach { add(it.id) }
            entornos.forEach { add(it.id) }
            vinculos.forEach { add(it.id) }
            trazos.forEach { add(it.id) }
            textos.forEach { add(it.id) }
        }
        var n = usados.size + 1
        while ("$prefijo$n" in usados) n++
        return "$prefijo$n"
    }
}

const val COLOR_TINTA = 0xFF0A2A5E.toInt()
