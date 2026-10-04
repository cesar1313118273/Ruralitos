package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.FichaFamiliarEntity

/** De quién es cada ficha según como se compartió: sirve para filtrar listas y para las etiquetas. */
enum class OrigenFicha(val etiqueta: String) {
    TODAS("Todas"),
    MIAS("Mías"),
    RECIBIDAS("Compartidas conmigo"),
    OTORGADAS("Compartidas por mí")
}

object EtiquetasFicha {
    /** Otra persona me la compartió (la creó ella). */
    fun esRecibida(ficha: FichaFamiliarEntity): Boolean = ficha.miPermiso.isNotBlank()

    /**
     * La ficha es mía si no me la compartieron y la creé yo, en este teléfono o en otro con mi misma cuenta
     * (en ese caso su autor en la nube es mi cuenta).
     */
    fun esMia(ficha: FichaFamiliarEntity, usuarioLocalId: Long, miCuentaRemota: String): Boolean =
        !esRecibida(ficha) && (
            ficha.creadoPorUsuarioId == usuarioLocalId ||
                (miCuentaRemota.isNotBlank() && ficha.autorRemotoId == miCuentaRemota)
            )

    /** Me la compartieron solo para ver: no se puede modificar, pero sí descargar PDF y Excel. */
    fun soloLectura(ficha: FichaFamiliarEntity): Boolean = ficha.miPermiso == "LECTOR"

    /** Eliminar definitivamente una ficha es cosa de quien la creó. */
    fun puedeEliminar(ficha: FichaFamiliarEntity): Boolean = !esRecibida(ficha)

    fun permisoTexto(ficha: FichaFamiliarEntity): String = if (soloLectura(ficha)) "Solo lectura" else "Puede editar"

    /** «Última modificación de otra persona: Luis, 06/10/2026 14:30», solo en fichas propias que otra persona tocó. */
    fun textoEdicionAjena(ficha: FichaFamiliarEntity): String? {
        if (esRecibida(ficha) || ficha.editadaPorOtroEn <= 0L) return null
        val cuando = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(ficha.editadaPorOtroEn))
        return "Última modificación de otra persona: ${ficha.editorNombre.ifBlank { "alguien de tu equipo" }}, $cuando"
    }

    /** «Compartida por Ana · Solo lectura», «Compartida por ti con 2 personas» o nulo si no se compartió. */
    fun texto(ficha: FichaFamiliarEntity): String? = when {
        esRecibida(ficha) -> "Compartida por ${ficha.autorNombre.ifBlank { "otra persona" }} · ${permisoTexto(ficha)}"
        ficha.compartidaConPersonas == 1 -> "Compartida por ti con 1 persona"
        ficha.compartidaConPersonas > 1 -> "Compartida por ti con ${ficha.compartidaConPersonas} personas"
        else -> null
    }
}

/** Opciones del filtro de los mapas: mis fichas (primero), las que me compartieron, o todas juntas. */
val OPCIONES_ORIGEN_MAPA = listOf(OrigenFicha.MIAS, OrigenFicha.RECIBIDAS, OrigenFicha.TODAS)

/** ¿Esta ficha entra en ese filtro? */
fun OrigenFicha.admite(ficha: FichaFamiliarEntity): Boolean = when (this) {
    OrigenFicha.TODAS -> true
    OrigenFicha.MIAS -> !EtiquetasFicha.esRecibida(ficha)
    OrigenFicha.RECIBIDAS -> EtiquetasFicha.esRecibida(ficha)
    OrigenFicha.OTORGADAS -> ficha.compartidaConPersonas > 0
}

/** Lo mismo para una vivienda del mapa, que solo conoce el permiso con el que la recibí. */
fun OrigenFicha.admiteVivienda(miPermiso: String): Boolean = when (this) {
    OrigenFicha.TODAS, OrigenFicha.OTORGADAS -> true
    OrigenFicha.MIAS -> miPermiso.isBlank()
    OrigenFicha.RECIBIDAS -> miPermiso.isNotBlank()
}

/** Línea que dice de dónde salen los números de un mapa: «Datos de tus fichas (23)». */
fun textoDatosDe(origen: OrigenFicha, propias: Int, recibidas: Int): String = when (origen) {
    OrigenFicha.MIAS -> "Datos de tus fichas ($propias)"
    OrigenFicha.RECIBIDAS -> "Datos de fichas compartidas contigo ($recibidas)"
    else -> "Tus fichas ($propias) + compartidas contigo ($recibidas)"
}
