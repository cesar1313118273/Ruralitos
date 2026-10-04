package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.EaisSalaEntity
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.data.local.entity.SalaEntity
import com.ruralitos.app.data.local.entity.TerritorioSalaEntity

/**
 * Niveles con los que se elige qué fichas se comparten o se exportan. Los usan «Compartir acceso» y «Exportar fichas»,
 * así que las dos pantallas ofrecen exactamente las mismas opciones.
 */
enum class NivelAlcance(val etiqueta: String) {
    CENTRO_ACTIVO("Todo el centro"),
    CENTRO("Centro de salud"),
    EAIS("EAIS"),
    BARRIO("Barrio"),
    FICHA("Ficha")
}

/** Todo lo que el teléfono sabe de la Sala: centros, EAIS, barrios y fichas. */
data class CatalogoAlcance(
    val salas: List<SalaEntity>,
    val eais: List<EaisSalaEntity>,
    val territorios: List<TerritorioSalaEntity>,
    val fichas: List<FichaFamiliarEntity>,
    val salaActivaId: String
) {
    val salaActiva: SalaEntity? get() = salas.firstOrNull { it.organizacionId == salaActivaId }

    /** Las fichas hechas sin cuenta en línea no tienen Sala todavía: se consideran del centro activo. */
    fun esDelCentro(ficha: FichaFamiliarEntity, organizacionId: String): Boolean =
        ficha.organizacionId == organizacionId ||
            (ficha.organizacionId.isBlank() && organizacionId == salaActivaId)
}

/** Una fila de la lista que se muestra al usuario. [fichas] es cuántas fichas abarca. */
data class OpcionAlcance(
    val clave: String,
    val titulo: String,
    val detalle: String = "",
    val fichas: Int = 0,
    val habilitada: Boolean = true
)

/** Una parte de la Sala que se entrega con un código de acceso. [alcance] es SALA, EAIS, TERRITORIO o FICHA. */
data class ConcesionAcceso(
    val organizacionId: String,
    val alcance: String,
    val eaisId: String? = null,
    val territorioId: String? = null,
    val fichaId: String? = null
)

object AlcanceFichas {
    /** Cuántas filas se dibujan a la vez; el resto se encuentra con el buscador. */
    const val MAXIMO_FILAS = 60

    fun opciones(
        nivel: NivelAlcance,
        catalogo: CatalogoAlcance,
        filtroEaisId: String = "",
        filtroBarrioId: String = "",
        consulta: String = "",
        /** Al compartir, una ficha que aún no llegó a la nube no se puede entregar. */
        soloSincronizadas: Boolean = false
    ): List<OpcionAlcance> {
        val activa = catalogo.salaActivaId
        val texto = normalizar(consulta)
        val lista = when (nivel) {
            NivelAlcance.CENTRO_ACTIVO -> emptyList()
            NivelAlcance.CENTRO -> catalogo.salas.map { sala ->
                OpcionAlcance(
                    clave = sala.organizacionId,
                    titulo = sala.nombreCentroSalud.ifBlank { sala.nombreSala },
                    detalle = listOf(sala.canton, sala.parroquia).filter(String::isNotBlank).joinToString(" · "),
                    fichas = catalogo.fichas.count { catalogo.esDelCentro(it, sala.organizacionId) }
                )
            }
            NivelAlcance.EAIS -> catalogo.eais.filter { it.salaId == activa && it.activo }.map { eais ->
                val barrios = catalogo.territorios.count { it.eaisId == eais.id && it.activo }
                OpcionAlcance(
                    clave = eais.id,
                    titulo = eais.nombre,
                    detalle = if (barrios == 1) "1 barrio" else "$barrios barrios",
                    fichas = catalogo.fichas.count { it.eaisId == eais.id }
                )
            }
            NivelAlcance.BARRIO -> catalogo.territorios
                .filter { it.salaId == activa && it.activo && (filtroEaisId.isBlank() || it.eaisId == filtroEaisId) }
                .map { barrio ->
                    OpcionAlcance(
                        clave = barrio.id,
                        titulo = barrio.nombre,
                        detalle = catalogo.eais.firstOrNull { it.id == barrio.eaisId }?.nombre.orEmpty(),
                        fichas = catalogo.fichas.count { it.territorioId == barrio.id }
                    )
                }
            NivelAlcance.FICHA -> catalogo.fichas
                .filter { catalogo.esDelCentro(it, activa) }
                .filter { filtroEaisId.isBlank() || it.eaisId == filtroEaisId }
                .filter { filtroBarrioId.isBlank() || it.territorioId == filtroBarrioId }
                .map { ficha ->
                    val lista = !soloSincronizadas || ficha.syncEstado == "SINCRONIZADO"
                    OpcionAlcance(
                        clave = ficha.id.toString(),
                        titulo = tituloFicha(ficha),
                        detalle = listOf(
                            ficha.barrio.ifBlank { nombreBarrio(catalogo, ficha) },
                            ficha.cedulaJefeHogar,
                            if (lista) "" else "aún sin sincronizar"
                        ).filter(String::isNotBlank).joinToString(" · "),
                        fichas = 1,
                        habilitada = lista
                    )
                }
        }
        return if (texto.isEmpty()) lista else lista.filter {
            normalizar(it.titulo).contains(texto) || normalizar(it.detalle).contains(texto)
        }
    }

    /** Las fichas que quedan dentro de la selección (sin repetir). */
    fun fichasDe(
        nivel: NivelAlcance,
        elegidos: Set<String>,
        catalogo: CatalogoAlcance
    ): List<FichaFamiliarEntity> = when (nivel) {
        NivelAlcance.CENTRO_ACTIVO -> catalogo.fichas.filter { catalogo.esDelCentro(it, catalogo.salaActivaId) }
        NivelAlcance.CENTRO -> catalogo.fichas.filter { ficha -> elegidos.any { catalogo.esDelCentro(ficha, it) } }
        NivelAlcance.EAIS -> catalogo.fichas.filter { it.eaisId in elegidos }
        NivelAlcance.BARRIO -> catalogo.fichas.filter { it.territorioId in elegidos }
        NivelAlcance.FICHA -> catalogo.fichas.filter { it.id.toString() in elegidos }
    }.distinctBy { it.id }

    /** Lo que se entrega al compartir: una concesión por cada centro, EAIS, barrio o ficha elegidos. */
    fun concesiones(
        nivel: NivelAlcance,
        elegidos: Set<String>,
        catalogo: CatalogoAlcance
    ): List<ConcesionAcceso> = when (nivel) {
        NivelAlcance.CENTRO_ACTIVO -> listOf(ConcesionAcceso(catalogo.salaActivaId, "SALA"))
        NivelAlcance.CENTRO -> elegidos.map { ConcesionAcceso(it, "SALA") }
        NivelAlcance.EAIS -> catalogo.eais.filter { it.id in elegidos }
            .map { ConcesionAcceso(it.salaId, "EAIS", eaisId = it.id) }
        NivelAlcance.BARRIO -> catalogo.territorios.filter { it.id in elegidos }
            .map { ConcesionAcceso(it.salaId, "TERRITORIO", eaisId = it.eaisId, territorioId = it.id) }
        NivelAlcance.FICHA -> catalogo.fichas.filter { it.id.toString() in elegidos && it.syncEstado == "SINCRONIZADO" }
            .map { ConcesionAcceso(it.organizacionId.ifBlank { catalogo.salaActivaId }, "FICHA", fichaId = it.syncId) }
    }

    fun resumen(nivel: NivelAlcance, elegidos: Set<String>, catalogo: CatalogoAlcance): String {
        val fichas = fichasDe(nivel, elegidos, catalogo).size
        val cuantas = if (fichas == 1) "1 ficha" else "$fichas fichas"
        val n = if (nivel == NivelAlcance.CENTRO_ACTIVO) 1 else elegidos.size
        val parte = when (nivel) {
            NivelAlcance.CENTRO_ACTIVO -> "Todo el centro"
            NivelAlcance.CENTRO -> plural(n, "centro", "centros")
            NivelAlcance.EAIS -> plural(n, "EAIS", "EAIS")
            NivelAlcance.BARRIO -> plural(n, "barrio", "barrios")
            NivelAlcance.FICHA -> return cuantas
        }
        return "$parte · $cuantas"
    }

    /** ¿Hay algo elegido que se pueda compartir o exportar? */
    fun hayElegidos(nivel: NivelAlcance, elegidos: Set<String>): Boolean =
        nivel == NivelAlcance.CENTRO_ACTIVO || elegidos.isNotEmpty()

    fun tituloFicha(ficha: FichaFamiliarEntity): String {
        val numero = ficha.numeroFichaFamiliar.ifBlank { "s/n" }
        val jefe = ficha.nombreApellidoJefeFamilia.ifBlank { "Jefe sin nombre" }
        return "$numero · $jefe"
    }

    private fun nombreBarrio(catalogo: CatalogoAlcance, ficha: FichaFamiliarEntity): String =
        catalogo.territorios.firstOrNull { it.id == ficha.territorioId }?.nombre.orEmpty()

    private fun plural(n: Int, uno: String, varios: String) = if (n == 1) "1 $uno" else "$n $varios"

    private fun normalizar(texto: String): String =
        java.text.Normalizer.normalize(texto.trim().lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
