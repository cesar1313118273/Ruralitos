package com.ruralitos.app.domain

data class OpcionRiesgo(
    val valor: Int,
    val descripcion: String
) {
    val textoVisible: String
        get() = "${InstrumentoRiesgoFamiliar.etiqueta(valor)}: $descripcion"
}

data class DefinicionRiesgo(
    val nombre: String,
    val opciones: List<OpcionRiesgo>
)

private fun riesgo(nombre: String, vararg opciones: Pair<Int, String>) =
    DefinicionRiesgo(nombre, opciones.map { OpcionRiesgo(it.first, it.second) })

object InstrumentoRiesgoFamiliar {
    val definiciones = listOf(
        riesgo(
            "Personas con vacunaci\u00f3n incompleta",
            0 to "Vacunas completas para la edad.",
            1 to "Refiere esquema completo, pero no presenta un respaldo verificable.",
            2 to "Esquema atrasado con posibilidad de intervenci\u00f3n.",
            3 to "Esquema atrasado sin posibilidad de intervenci\u00f3n.",
            4 to "No se aplic\u00f3 alguna vacuna del esquema nacional o existe rechazo a la vacunaci\u00f3n."
        ),
        riesgo(
            "Personas con malnutrici\u00f3n",
            0 to "Sin sobrepeso, obesidad ni desnutrici\u00f3n y con estilos de vida saludables.",
            2 to "Riesgo de peso bajo, talla baja o sobrepeso.",
            4 to "Obesidad o desnutrici\u00f3n y ausencia de estilos de vida saludables."
        ),
        riesgo(
            "Personas con enfermedad de impacto",
            0 to "Sin enfermedades.",
            1 to "Enfermedad aguda al momento de la visita domiciliaria.",
            2 to "Enfermedad transmisible o no transmisible que no afecta las actividades diarias y no presenta complicaciones.",
            3 to "Enfermedad que afecta las actividades diarias o presenta complicaciones.",
            4 to "Enfermedad que necesita tratamiento permanente o enfermedad catastr\u00f3fica."
        ),
        riesgo(
            "Embarazadas con problemas",
            0 to "No existen embarazadas.",
            1 to "Existe al menos una embarazada con controles prenatales.",
            2 to "Existe al menos una embarazada sin controles prenatales.",
            3 to "Embarazo adolescente, mayor de 35 a\u00f1os, m\u00faltiple o con otros factores obst\u00e9tricos y controles insuficientes.",
            4 to "Embarazada con diagn\u00f3stico previo de enfermedad cr\u00f3nica transmisible o no transmisible."
        ),
        riesgo(
            "Personas con discapacidad",
            0 to "No existen personas con discapacidad.",
            1 to "Persona con deficiencia o condici\u00f3n incapacitante.",
            2 to "Discapacidad o condici\u00f3n incapacitante que no limita su autonom\u00eda.",
            3 to "Discapacidad o condici\u00f3n incapacitante que limita su autonom\u00eda.",
            4 to "Discapacidad con dependencia o necesidad de cuidados paliativos."
        ),
        riesgo(
            "Personas con problemas mentales",
            0 to "No existen personas con problemas de salud mental.",
            2 to "Problema de salud mental con tratamiento controlado que no afecta al entorno familiar.",
            4 to "Problema de salud mental, con o sin tratamiento, que afecta al entorno familiar."
        ),
        riesgo("Consumo de agua insegura", 0 to "Consumo de agua tratada.", 4 to "Consumo de agua no tratada."),
        riesgo(
            "Mala eliminaci\u00f3n de basura y excretas",
            0 to "Eliminaci\u00f3n correcta de basura y excretas.",
            4 to "Eliminaci\u00f3n incorrecta de basura y excretas."
        ),
        riesgo(
            "Mala eliminaci\u00f3n de desechos l\u00edquidos",
            0 to "La vivienda cuenta con alcantarillado.",
            4 to "La vivienda no cuenta con alcantarillado."
        ),
        riesgo(
            "Impacto ecol\u00f3gico por industrias",
            0 to "No existe industria contaminante dentro de la comunidad.",
            4 to "Existe una o m\u00e1s industrias contaminantes dentro de la comunidad."
        ),
        riesgo(
            "Animales intradomiciliarios",
            0 to "No hay animales en el domicilio.",
            1 to "La familia posee animales dom\u00e9sticos con espacio apropiado y cuidados adecuados.",
            2 to "La familia posee animales dom\u00e9sticos con espacio apropiado y cuidados adecuados.",
            3 to "La familia posee animales dom\u00e9sticos sin espacio apropiado ni cuidados adecuados.",
            4 to "Existen animales dom\u00e9sticos o no dom\u00e9sticos con ingreso a las \u00e1reas internas de la vivienda."
        ),
        riesgo(
            "Pobreza",
            0 to "Los ingresos cubren las necesidades b\u00e1sicas de la familia.",
            2 to "Los ingresos cubren parcialmente las necesidades b\u00e1sicas de la familia.",
            4 to "Los ingresos no cubren las necesidades b\u00e1sicas de la familia."
        ),
        riesgo(
            "Desempleo o empleo informal del jefe de familia",
            0 to "El proveedor o los proveedores cuentan con empleo formal.",
            2 to "El proveedor o los proveedores cuentan con empleo informal.",
            4 to "El proveedor o los proveedores no cuentan con empleo."
        ),
        riesgo(
            "Analfabetismo del padre o la madre",
            0 to "Alfabetizados.", 1 to "Sabe leer, pero no escribir.", 2 to "Sabe escribir, pero no leer.",
            3 to "Padre o madre analfabeto.", 4 to "Padre y madre analfabetos."
        ),
        riesgo(
            "Desestructuraci\u00f3n familiar",
            0 to "Familia estructurada de acuerdo con su ciclo familiar y con cumplimiento de roles.",
            2 to "Familia estructurada con alteraci\u00f3n de roles.",
            4 to "Familia desestructurada y sin cumplimiento de roles."
        ),
        riesgo(
            "Violencia, alcoholismo, drogadicci\u00f3n o tabaquismo",
            0 to "Sin violencia y sin consumo de alcohol, tabaco ni drogas.",
            1 to "Consumo de alcohol sin dependencia ni afectaci\u00f3n social, familiar, econ\u00f3mica o afectiva.",
            2 to "Abuso de alcohol sin dependencia, pero con afectaci\u00f3n social, familiar, econ\u00f3mica o afectiva.",
            3 to "Dependencia, s\u00edndrome de abstinencia o consumo frecuente peligroso para la salud.",
            4 to "Consumo problem\u00e1tico de alcohol, tabaco u otra droga, o presencia de violencia."
        ),
        riesgo(
            "Malas condiciones de la vivienda",
            0 to "Techo, paredes y piso de materiales adecuados y protectores.",
            1 to "Techo de zinc o piso de ladrillo o cemento; casa de bloque en buenas condiciones.",
            2 to "Techo de teja o casa de adobe en buenas condiciones.",
            3 to "Techo de palma, paja u hoja, o paredes de tabla o bahareque.",
            4 to "Piso de tierra, materiales precarios o paredes que no protegen de agentes externos."
        ),
        riesgo(
            "Hacinamiento",
            0 to "No hay hacinamiento; persona sola o pareja.",
            1 to "Promedio de dos personas por dormitorio, sin contar a los padres.",
            2 to "Promedio de tres personas por dormitorio.",
            3 to "Promedio de cuatro o cinco personas por dormitorio.",
            4 to "M\u00e1s de cinco personas por habitaci\u00f3n o ambientes compartidos en malas condiciones."
        )
    )

    fun etiqueta(valor: Int): String = when (valor) {
        0 -> "Sin riesgo"
        1 -> "Riesgo muy bajo"
        2 -> "Riesgo bajo"
        3 -> "Riesgo moderado"
        4 -> "Riesgo alto"
        else -> "Sin seleccionar"
    }

    fun valorValido(indice: Int, valor: Int): Boolean =
        definiciones.getOrNull(indice)?.opciones?.any { it.valor == valor } == true
}
