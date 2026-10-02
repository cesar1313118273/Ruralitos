package com.ruralitos.app.domain

data class CamposPermitidosGrupoEdad(
    val ocupacion: Boolean,
    val escolaridades: Set<String>
)

object ReglasGrupoEdadFamiliar {
    private val todasLasEscolaridades = setOf("SIN", "BAS", "BACH", "SUP", "ESP")

    fun camposPermitidos(grupoEdad: String?): CamposPermitidosGrupoEdad = when (grupoEdad) {
        GrupoEdadFamiliar.MENOR_UN_ANIO,
        GrupoEdadFamiliar.UNO_A_CUATRO -> CamposPermitidosGrupoEdad(
            ocupacion = false,
            escolaridades = emptySet()
        )
        GrupoEdadFamiliar.CINCO_A_NUEVE -> CamposPermitidosGrupoEdad(
            ocupacion = true,
            escolaridades = setOf("SIN", "BAS")
        )
        GrupoEdadFamiliar.DIEZ_A_DIECINUEVE -> CamposPermitidosGrupoEdad(
            ocupacion = true,
            escolaridades = setOf("SIN", "BAS", "BACH", "ESP")
        )
        GrupoEdadFamiliar.VEINTE_A_SESENTA_Y_CUATRO,
        GrupoEdadFamiliar.SESENTA_Y_CINCO_MAS -> CamposPermitidosGrupoEdad(
            ocupacion = true,
            escolaridades = todasLasEscolaridades
        )
        else -> CamposPermitidosGrupoEdad(ocupacion = false, escolaridades = emptySet())
    }

    fun permiteEscolaridad(grupoEdad: String?, escolaridad: String): Boolean =
        escolaridad in camposPermitidos(grupoEdad).escolaridades
}
