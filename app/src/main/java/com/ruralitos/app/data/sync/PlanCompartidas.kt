package com.ruralitos.app.data.sync

import com.ruralitos.app.data.remote.InfoFichaCompartida

/** Lo que este teléfono sabe de una ficha para decidir si falta bajarla o si ya no corresponde tenerla. */
data class FichaLocalCompartida(val syncId: String, val miPermiso: String, val syncEstado: String)

data class PlanCompartidas(
    /** Fichas que me compartieron, de esta Sala, y que todavía no están en el teléfono. */
    val faltantes: List<String>,
    /** Fichas que tenía como compartidas y que el servidor ya no menciona: hay que comprobar si de verdad perdí el acceso. */
    val candidatasARetirar: List<String>
)

object PlaneadorCompartidas {
    /**
     * El modo incremental solo trae fichas cuya fecha de cambio es reciente: una ficha antigua que acaban de compartirme
     * (o una a la que me quitaron el acceso) no aparece ni desaparece por sí sola. Esta comparación con la lista de
     * «fichas que me compartieron» que da el servidor lo resuelve en cada sincronización, sin esperar a la descarga completa.
     */
    fun planear(
        organizacionId: String,
        locales: List<FichaLocalCompartida>,
        filas: List<InfoFichaCompartida>,
        enBaja: Set<String>,
        /** Fichas que esta cuenta entregó con un traspaso: si ya no las ve, se retiran del teléfono. */
        traspasadas: Set<String> = emptySet()
    ): PlanCompartidas {
        val recibidas = filas.filter { it.recibida }
        val idsLocales = locales.mapTo(mutableSetOf()) { it.syncId }
        val faltantes = recibidas
            .filter { it.organizacionId == organizacionId && it.fichaId !in idsLocales && it.fichaId !in enBaja }
            .map { it.fichaId }
        val idsRecibidas = recibidas.mapTo(mutableSetOf()) { it.fichaId }
        val candidatas = locales
            .filter {
                it.syncEstado == "SINCRONIZADO" && it.syncId !in idsRecibidas &&
                    (it.miPermiso.isNotBlank() || it.syncId in traspasadas)
            }
            .map { it.syncId }
        return PlanCompartidas(faltantes, candidatas)
    }
}
