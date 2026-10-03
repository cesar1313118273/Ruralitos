package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.EmbarazadaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.MiembroFamiliaEntity
import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

/** Una figura del mapa parlante y a cuántas personas del barrio representa. */
data class StickerBarrio(val id: String, val etiqueta: String, val personas: Int)

/** Todo lo que el mapa parlante muestra de un barrio: su punto medio y los totales de cada figura que existe en él. */
data class BarrioParlante(
    val clave: String,
    val nombre: String,
    val fichas: Int,
    val personas: Int,
    /** Latitud y longitud del punto medio de las fichas ubicadas; nulo si ninguna tiene ubicación. */
    val centro: Pair<Double, Double>?,
    val fichasSinUbicacion: Int,
    val stickers: List<StickerBarrio>
)

/**
 * Mapa parlante: en la práctica, el equipo coloca sobre el mapa del barrio una figura por cada situación presente
 * (hipertensos, embarazadas, menores de 2 años…) y encima su número. Aquí se calculan esos números con las mismas
 * reglas del registro general, por persona, para que coincidan con la sección de indicadores.
 */
object MapaParlante {
    private val ordenCatalogo: Map<String, Int> = IconosMais.todos.withIndex().associate { it.value to it.index }

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "").lowercase().trim()

    /** El barrio de una ficha: su barrio, o su comunidad si no tiene. */
    fun nombreBarrio(ficha: FichaFamiliarEntity): String = ficha.barrio.ifBlank { ficha.comunidad }.trim()

    /** El punto de ejemplo antiguo no es una ubicación real. */
    private fun ubicada(f: FichaFamiliarEntity): Boolean {
        val lat = f.latitud ?: return false
        val lon = f.longitud ?: return false
        if (!lat.isFinite() || !lon.isFinite()) return false
        return !(abs(lat - (-1.8312)) < 0.000001 && abs(lon - (-78.1834)) < 0.000001)
    }

    fun barrios(
        fichas: List<FichaFamiliarEntity>,
        miembros: List<MiembroFamiliaEntity>,
        embarazadas: List<EmbarazadaEntity>
    ): List<BarrioParlante> {
        val miembrosPorFicha = miembros.groupBy { it.fichaId }
        val embarazosPorFicha = embarazadas.groupBy { it.fichaId }
        return fichas.filter { it.estado != "ARCHIVADA" && nombreBarrio(it).isNotEmpty() }
            .groupBy { normalizar(nombreBarrio(it)) }
            .map { (clave, grupo) ->
                val conteo = linkedMapOf<String, Int>()
                val etiquetas = mutableMapOf<String, String>()
                var personas = 0
                grupo.forEach { ficha ->
                    miembrosPorFicha[ficha.id].orEmpty().forEach { miembro ->
                        personas++
                        val embarazo = DispensarizacionAutomatica.buscarEmbarazo(miembro, embarazosPorFicha[ficha.id].orEmpty())
                        DispensarizacionAutomatica.clasificar(miembro, embarazo).pictogramas.distinctBy { it.id }.forEach {
                            conteo[it.id] = (conteo[it.id] ?: 0) + 1
                            etiquetas.putIfAbsent(it.id, it.etiqueta)
                        }
                    }
                }
                val ubicadas = grupo.filter(::ubicada)
                BarrioParlante(
                    clave = clave,
                    nombre = nombreBarrio(grupo.first()),
                    fichas = grupo.size,
                    personas = personas,
                    centro = if (ubicadas.isEmpty()) null
                    else ubicadas.map { it.latitud!! }.average() to ubicadas.map { it.longitud!! }.average(),
                    fichasSinUbicacion = grupo.size - ubicadas.size,
                    stickers = conteo.map { (id, n) -> StickerBarrio(id, etiquetas.getValue(id), n) }
                        .sortedBy { ordenCatalogo[it.id] ?: Int.MAX_VALUE }
                )
            }
            .sortedBy { it.clave }
    }

    /** Columnas y filas de la cuadrícula de figuras: casi cuadrada y de cinco columnas como máximo, para que quepa en pantalla. */
    fun cuadricula(cantidad: Int): Pair<Int, Int> {
        if (cantidad <= 0) return 0 to 0
        val columnas = when {
            cantidad <= 3 -> cantidad
            cantidad <= 8 -> ceil(cantidad / 2.0).toInt()
            else -> minOf(5, ceil(sqrt(cantidad.toDouble())).toInt())
        }
        return columnas to ceil(cantidad / columnas.toDouble()).toInt()
    }

    /**
     * Centro de cada celda respecto del centro del tablero, en las mismas unidades que [ancho] y [alto] de la celda.
     * Como cada figura vive dentro de su celda, nunca se empalman entre sí ni sus números.
     */
    fun desplazamientos(cantidad: Int, ancho: Float, alto: Float): List<Pair<Float, Float>> {
        val (columnas, filas) = cuadricula(cantidad)
        return (0 until cantidad).map { i ->
            val fila = i / columnas
            val columna = i % columnas
            // la última fila, si queda incompleta, se centra
            val enFila = if (fila == filas - 1) cantidad - fila * columnas else columnas
            val x = (columna - (enFila - 1) / 2f) * ancho
            val y = (fila - (filas - 1) / 2f) * alto
            x to y
        }
    }
}
