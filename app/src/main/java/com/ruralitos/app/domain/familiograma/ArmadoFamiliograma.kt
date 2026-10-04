package com.ruralitos.app.domain.familiograma

import java.text.Normalizer
import java.util.Locale
import kotlin.math.min

/** Integrante de la ficha tal como lo necesita el dibujo. */
data class IntegranteFamiliograma(
    val nombre: String,
    val parentesco: String,
    val sexo: String,
    /** Edad ya formateada: "45", o "8m" si es menor de un año. */
    val edad: String,
    val patologias: List<String> = emptyList()
)

/**
 * Coloca automáticamente a los integrantes de la ficha sobre la hoja del familiograma, cada generación en
 * su franja: ABUELOS (padres o abuelos del jefe), PADRES (el jefe de familia con su pareja y sus hermanos)
 * e HIJOS (hijos y nietos). Une a la pareja y cuelga a los hijos; el profesional completa el resto.
 */
object ArmadoFamiliograma {
    private const val SEPARACION_MAXIMA = 100f
    private const val SEPARACION_PAREJA = 140f
    private const val CENTRO = HojaFamiliograma.ANCHO / 2f
    private const val ANCHO_UTIL = 880f

    private enum class Categoria { JEFE, CONYUGE, HIJO, PADRE, ABUELO, NIETO, HERMANO, OTRO }

    private fun categoria(parentesco: String): Categoria {
        val texto = Normalizer.normalize(parentesco, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").uppercase(Locale.ROOT)
        return when {
            "JEF" in texto -> Categoria.JEFE
            "CONYUGE" in texto || "PAREJA" in texto -> Categoria.CONYUGE
            "HIJO" in texto -> Categoria.HIJO
            "ABUEL" in texto -> Categoria.ABUELO
            "NIET" in texto -> Categoria.NIETO
            "HERMAN" in texto -> Categoria.HERMANO
            "PADRE" in texto || "MADRE" in texto -> Categoria.PADRE
            else -> Categoria.OTRO
        }
    }

    private fun separacion(cantidad: Int): Float =
        if (cantidad <= 1) SEPARACION_MAXIMA else min(SEPARACION_MAXIMA, ANCHO_UTIL / cantidad).coerceAtLeast(52f)

    fun desdeIntegrantes(integrantes: List<IntegranteFamiliograma>): Familiograma {
        var doc = Familiograma()
        var contador = 0
        fun nuevaPersona(origen: IntegranteFamiliograma, x: Float, generacion: Int): Persona {
            contador++
            return Persona(
                id = "p$contador",
                sexo = SexoPersona.desde(origen.sexo),
                x = x,
                y = HojaFamiliograma.alturaGeneracion(generacion),
                edad = origen.edad,
                patologias = origen.patologias.distinctBy { AbreviaturasPatologia.normalizar(it) },
                enHogar = true,
                nombre = origen.nombre,
                parentesco = origen.parentesco
            )
        }

        val porCategoria = integrantes.groupBy { categoria(it.parentesco) }
        val jefe = porCategoria[Categoria.JEFE]?.firstOrNull()
        val conyuge = porCategoria[Categoria.CONYUGE]?.firstOrNull()
        val otros = porCategoria[Categoria.OTRO].orEmpty() +
            porCategoria[Categoria.JEFE].orEmpty().drop(1) +
            porCategoria[Categoria.CONYUGE].orEmpty().drop(1)
        val hijos = porCategoria[Categoria.HIJO].orEmpty()
        val padres = porCategoria[Categoria.PADRE].orEmpty().sortedBy { SexoPersona.desde(it.sexo).ordinal }
        val abuelos = porCategoria[Categoria.ABUELO].orEmpty()
        val nietos = porCategoria[Categoria.NIETO].orEmpty()
        val hermanos = porCategoria[Categoria.HERMANO].orEmpty()

        val personas = mutableListOf<Persona>()
        val uniones = mutableListOf<Union>()
        val filiaciones = mutableListOf<Filiacion>()

        // Franja PADRES: hermanos | jefe | pareja | otros, con la pareja centrada en la hoja
        val hayPareja = jefe != null && conyuge != null
        val xJefe = if (hayPareja) CENTRO - SEPARACION_PAREJA / 2 else CENTRO
        val pJefe = jefe?.let { nuevaPersona(it, xJefe, 1) }?.also(personas::add)
        val pConyuge = conyuge?.let {
            nuevaPersona(it, if (pJefe != null) xJefe + SEPARACION_PAREJA else CENTRO, 1)
        }?.also(personas::add)
        val xBase = if (hayPareja) CENTRO else (pJefe?.x ?: pConyuge?.x ?: CENTRO)
        val sepFila = separacion(hermanos.size + otros.size + 2)
        val pHermanos = hermanos.mapIndexed { i, h ->
            nuevaPersona(h, (pJefe?.x ?: xBase) - sepFila * (i + 1), 1)
        }.also(personas::addAll)
        val ultimoX = pConyuge?.x ?: pJefe?.x ?: (xBase - sepFila)
        otros.forEachIndexed { i, o -> personas.add(nuevaPersona(o, ultimoX + sepFila * (i + 1), 1)) }

        var unionPareja: Union? = null
        if (pJefe != null && pConyuge != null) {
            unionPareja = Union("u1", Ancla(pJefe.id, Lado.DERECHA), Ancla(pConyuge.id, Lado.IZQUIERDA))
            uniones.add(unionPareja)
        }

        // Franja HIJOS: los hijos centrados bajo la pareja y, a su derecha, los nietos
        val progenitorUnico = pJefe ?: pConyuge
        val sepHijos = separacion(hijos.size + nietos.size + (if (nietos.isNotEmpty()) 1 else 0))
        val anchoHijos = (hijos.size - 1).coerceAtLeast(0) * sepHijos
        val inicioHijos = xBase - anchoHijos / 2
        val hijosFila = hijos.mapIndexed { i, h -> nuevaPersona(h, inicioHijos + i * sepHijos, 2) }
        personas.addAll(hijosFila)
        hijosFila.forEach { h ->
            val id = "f${filiaciones.size + 1}"
            if (unionPareja != null) filiaciones.add(Filiacion(id, Ancla(h.id, Lado.ARRIBA), unionId = unionPareja.id))
            else if (progenitorUnico != null) {
                filiaciones.add(Filiacion(id, Ancla(h.id, Lado.ARRIBA), progenitor = Ancla(progenitorUnico.id, Lado.ABAJO)))
            }
        }
        val inicioNietos = inicioHijos + anchoHijos + sepHijos * 1.6f
        nietos.forEachIndexed { i, n -> personas.add(nuevaPersona(n, inicioNietos + i * sepHijos, 2)) }

        // Franja ABUELOS: los padres del jefe sobre él y sus hermanos; los abuelos a su lado
        val hijosDePadres = listOfNotNull(pJefe) + pHermanos
        val centroPadres = if (hijosDePadres.isEmpty()) xBase else hijosDePadres.map { it.x }.average().toFloat()
        val pPadres = padres.mapIndexed { i, p ->
            val x = if (padres.size >= 2) centroPadres + (i - (padres.size - 1) / 2f) * SEPARACION_PAREJA else centroPadres
            nuevaPersona(p, x, 0)
        }
        personas.addAll(pPadres)
        if (pPadres.size >= 2) {
            val u = Union("u${uniones.size + 1}", Ancla(pPadres[0].id, Lado.DERECHA), Ancla(pPadres[1].id, Lado.IZQUIERDA))
            uniones.add(u)
            hijosDePadres.forEach {
                filiaciones.add(Filiacion("f${filiaciones.size + 1}", Ancla(it.id, Lado.ARRIBA), unionId = u.id))
            }
        } else if (pPadres.size == 1) {
            hijosDePadres.forEach {
                filiaciones.add(
                    Filiacion("f${filiaciones.size + 1}", Ancla(it.id, Lado.ARRIBA), progenitor = Ancla(pPadres[0].id, Lado.ABAJO))
                )
            }
        }
        val derechaPadres = pPadres.maxOfOrNull { it.x } ?: (centroPadres - SEPARACION_MAXIMA)
        val sepAbuelos = separacion(abuelos.size + 2)
        abuelos.forEachIndexed { i, a ->
            personas.add(nuevaPersona(a, derechaPadres + sepAbuelos * 1.5f + i * sepAbuelos, 0))
        }

        doc = doc.copy(personas = personas, uniones = uniones, filiaciones = filiaciones)
        personas.forEach { p -> p.patologias.forEach { doc = AbreviaturasPatologia.registrar(doc, it) } }
        return doc
    }
}
