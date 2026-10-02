package com.ruralitos.app.domain

data class ResultadoRiesgo(
    val total: Int,
    val nivel: String
)

object RiesgoFamiliar {
    val componentes = listOf(
        "Personas con vacunación incompleta",
        "Personas con malnutrición",
        "Personas con enfermedad de impacto",
        "Embarazadas con problemas",
        "Personas con discapacidad",
        "Personas con problemas mentales",
        "Consumo de agua insegura",
        "Mala eliminación de basura y excretas",
        "Mala eliminación de desechos líquidos",
        "Impacto ecológico por industrias",
        "Animales intradomiciliarios",
        "Pobreza",
        "Desempleo o empleo informal del jefe de familia",
        "Analfabetismo del padre o la madre",
        "Desestructuración familiar",
        "Violencia, alcoholismo o drogadicción",
        "Malas condiciones de la vivienda",
        "Hacinamiento"
    )

    fun calcular(valores: List<Int>): ResultadoRiesgo {
        require(valores.size == componentes.size) {
            "La calificación debe contener los 18 componentes."
        }
        require(valores.all { it in 0..4 }) {
            "Cada riesgo debe tener un valor entre 0 y 4."
        }

        val total = valores.sum()
        val nivel = when (total) {
            0 -> "SIN_RIESGO"
            in 1..14 -> "BAJO"
            in 15..34 -> "MEDIO"
            else -> "ALTO"
        }
        return ResultadoRiesgo(total, nivel)
    }
}
