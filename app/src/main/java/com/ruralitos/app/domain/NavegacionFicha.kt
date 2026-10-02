package com.ruralitos.app.domain

/**
 * Única fuente de verdad para avanzar o retroceder dentro de una ficha.
 * Evita que el flujo de creación y el de edición intercambien sus botones.
 */
object NavegacionFicha {
    private val siguiente = mapOf(
        "ubicacion" to "miembros",
        "miembros" to "saludFamiliar",
        "saludFamiliar" to "riesgos",
        "riesgos" to "gestionRiesgo",
        "gestionRiesgo" to "familiograma",
        "familiograma" to "croquisMapa",
        "croquisMapa" to "contaminacion",
        "contaminacion" to "tratamiento",
        "tratamiento" to "revisionFicha"
    )

    private val anterior = mapOf(
        "ubicacion" to "menuFicha",
        "miembros" to "ubicacion",
        "saludFamiliar" to "miembros",
        "riesgos" to "saludFamiliar",
        "gestionRiesgo" to "riesgos",
        "familiograma" to "gestionRiesgo",
        "croquisMapa" to "familiograma",
        "contaminacion" to "croquisMapa",
        "tratamiento" to "contaminacion",
        "revisionFicha" to "tratamiento"
    )

    fun avanzar(desde: String, desdeRevision: Boolean): String =
        if (desdeRevision) "revisionFicha" else siguiente[desde] ?: "menuFicha"

    fun regresar(
        desde: String,
        modoEdicion: Boolean,
        desdeRevision: Boolean
    ): String = when {
        desdeRevision -> "revisionFicha"
        modoEdicion -> "menuFicha"
        else -> anterior[desde] ?: "inicio"
    }

    fun textoRegresar(modoEdicion: Boolean, desdeRevision: Boolean): String = when {
        desdeRevision -> "Regresar a la revisión de la ficha"
        modoEdicion -> "Volver al panel de la ficha"
        else -> "Regresar a la sección anterior"
    }

    fun descripcionRegresar(modoEdicion: Boolean, desdeRevision: Boolean): String = when {
        desdeRevision -> "Volver a comprobar la información pendiente"
        modoEdicion -> "Abrir el listado de secciones de esta ficha"
        else -> "Volver al paso anterior sin salir de la ficha"
    }
}