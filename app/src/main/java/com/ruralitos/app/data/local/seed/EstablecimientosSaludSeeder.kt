package com.ruralitos.app.data.local.seed

import android.content.Context
import com.ruralitos.app.data.local.database.RuralitosDatabase
import com.ruralitos.app.data.local.entity.EstablecimientoSaludEntity

object EstablecimientosSaludSeeder {

    suspend fun cargarSiEstaVacio(context: Context) {
        val database = RuralitosDatabase.obtenerBaseDatos(context)
        val dao = database.establecimientoSaludDao()

        val total = dao.contarEstablecimientos()

        if (total > 0) {
            return
        }

        val establecimientos = leerCsv(context)

        dao.guardarEstablecimientos(establecimientos)
    }

    private fun leerCsv(context: Context): List<EstablecimientoSaludEntity> {
        val establecimientos = mutableListOf<EstablecimientoSaludEntity>()

        context.assets.open("establecimientos_salud.csv")
            .bufferedReader()
            .useLines { lineas ->
                lineas.drop(1).forEach { linea ->
                    val columnas = parsearLineaCsv(linea)

                    if (columnas.size >= 11) {
                        establecimientos.add(
                            EstablecimientoSaludEntity(
                                codigoUo = columnas[0],
                                nombreCentroSalud = columnas[1],
                                institucionSistema = columnas[2],
                                provinciaCodigoLocalizacion = columnas[3],
                                provincia = columnas[4],
                                cantonCodigoLocalizacion = columnas[5],
                                canton = columnas[6],
                                parroquiaCodigoLocalizacion = columnas[7],
                                parroquia = columnas[8],
                                sector = columnas[9],
                                areaNumero = columnas[10]
                            )
                        )
                    }
                }
            }

        return establecimientos
    }

    private fun parsearLineaCsv(linea: String): List<String> {
        val resultado = mutableListOf<String>()
        val actual = StringBuilder()
        var dentroDeComillas = false
        var i = 0

        while (i < linea.length) {
            val caracter = linea[i]

            when {
                caracter == '"' -> {
                    if (dentroDeComillas && i + 1 < linea.length && linea[i + 1] == '"') {
                        actual.append('"')
                        i++
                    } else {
                        dentroDeComillas = !dentroDeComillas
                    }
                }

                caracter == ',' && !dentroDeComillas -> {
                    resultado.add(actual.toString().trim())
                    actual.clear()
                }

                else -> {
                    actual.append(caracter)
                }
            }

            i++
        }

        resultado.add(actual.toString().trim())

        return resultado
    }
}