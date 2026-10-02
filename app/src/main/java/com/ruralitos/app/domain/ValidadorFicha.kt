package com.ruralitos.app.domain

import com.ruralitos.app.data.local.entity.FichaFamiliarEntity

data class RequisitoFicha(
    val id: String,
    val titulo: String,
    val cumplido: Boolean,
    val seccion: String
)

object ValidadorFicha {
    fun revisar(
        ficha: FichaFamiliarEntity,
        cantidadMiembros: Int,
        cantidadCalificaciones: Int,
        tiposAdjuntos: Set<String>
    ): List<RequisitoFicha> = listOf(
        RequisitoFicha(
            "datos",
            "Datos principales y responsable",
            ValidadorIdentidadEcuador.esDocumentoFamiliarAceptable(ficha.cedulaJefeHogar) &&
                ficha.nombreApellidoJefeFamilia.isNotBlank() &&
                ficha.numeroFichaFamiliar.isNotBlank() &&
                ficha.fechaLlenado.isNotBlank() &&
                ficha.responsableNombre.isNotBlank(),
            "datos"
        ),
        RequisitoFicha(
            "ubicacion",
            "Ubicación de la familia",
            ficha.provincia.isNotBlank() && ficha.canton.isNotBlank() &&
                ficha.parroquia.isNotBlank() && ficha.sector.isNotBlank() &&
                (ficha.direccionHabitualFamilia.isNotBlank() ||
                    ficha.barrio.isNotBlank() || ficha.comunidad.isNotBlank()),
            "ubicacion"
        ),
        RequisitoFicha(
            "miembros",
            "Al menos un miembro familiar",
            cantidadMiembros > 0,
            "miembros"
        ),
        RequisitoFicha(
            "riesgos",
            "Al menos una calificación de riesgos",
            cantidadCalificaciones > 0,
            "riesgos"
        ),
        RequisitoFicha(
            "familiograma",
            "Imagen del familiograma",
            "FAMILIOGRAMA" in tiposAdjuntos,
            "familiograma"
        ),
        RequisitoFicha(
            "croquis",
            "Imagen del croquis",
            "CROQUIS" in tiposAdjuntos,
            "croquis"
        ),
        RequisitoFicha(
            "firma",
            "Firma del responsable",
            !ficha.firmaUri.isNullOrBlank(),
            "firma"
        )
    )
}
