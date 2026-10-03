package com.ruralitos.app.domain

/**
 * Orden de un recorrido de visitas: el camino más corto que pasa por todas las viviendas elegidas, sin volver al punto de
 * partida. Con pocas paradas (hasta 25) basta el vecino más cercano mejorado con intercambios 2-opt.
 */
object RecorridoVisitas {
    const val MAXIMO_PARADAS = 25

    /**
     * [costos]\[i]\[j] es lo que cuesta ir de i a j (metros o segundos; puede ser infinito si no hay camino).
     * Si [inicio] no es nulo, el recorrido empieza ahí. Devuelve los índices en el orden de visita.
     */
    fun ordenar(costos: Array<DoubleArray>, inicio: Int? = null): List<Int> {
        val n = costos.size
        if (n <= 2) return if (inicio != null && n == 2 && inicio == 1) listOf(1, 0) else (0 until n).toList()
        val salidas = inicio?.let { listOf(it) } ?: (0 until n).toList()
        var mejor: List<Int>? = null
        var mejorCosto = Double.POSITIVE_INFINITY
        for (salida in salidas) {
            val ruta = mejorar(vecinoMasCercano(costos, salida), costos, fijarPrimero = inicio != null)
            val c = costoTotal(ruta, costos)
            if (mejor == null || c < mejorCosto) { mejor = ruta; mejorCosto = c }
        }
        return mejor!!
    }

    fun costoTotal(orden: List<Int>, costos: Array<DoubleArray>): Double =
        (0 until orden.size - 1).sumOf { costos[orden[it]][orden[it + 1]] }

    private fun vecinoMasCercano(costos: Array<DoubleArray>, salida: Int): List<Int> {
        val pendientes = costos.indices.filter { it != salida }.toMutableList()
        val orden = mutableListOf(salida)
        while (pendientes.isNotEmpty()) {
            val actual = orden.last()
            val siguiente = pendientes.minByOrNull { costos[actual][it] }!!
            orden += siguiente
            pendientes -= siguiente
        }
        return orden
    }

    /** Invierte tramos mientras eso acorte el recorrido. */
    private fun mejorar(inicial: List<Int>, costos: Array<DoubleArray>, fijarPrimero: Boolean): List<Int> {
        var orden = inicial
        var costo = costoTotal(orden, costos)
        var mejoro = true
        val desde = if (fijarPrimero) 1 else 0
        while (mejoro) {
            mejoro = false
            for (i in desde until orden.size - 1) {
                for (j in i + 1 until orden.size) {
                    val candidato = orden.take(i) + orden.subList(i, j + 1).reversed() + orden.drop(j + 1)
                    val c = costoTotal(candidato, costos)
                    if (c < costo - 1e-9) { orden = candidato; costo = c; mejoro = true }
                }
            }
        }
        return orden
    }

    /** Matriz de distancias en línea recta (metros) para cuando no hay red vial a mano. */
    fun matrizEnLinea(puntos: List<Pair<Double, Double>>): Array<DoubleArray> =
        Array(puntos.size) { i ->
            DoubleArray(puntos.size) { j ->
                if (i == j) 0.0
                else MapaViviendas.distanciaMetros(puntos[i].first, puntos[i].second, puntos[j].first, puntos[j].second)
            }
        }
}
