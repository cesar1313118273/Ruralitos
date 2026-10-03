package com.ruralitos.app.data.cumplimiento

import android.content.Context
import com.ruralitos.app.domain.GrupoCumplimiento
import com.ruralitos.app.domain.PoblacionAsignada

/**
 * Población asignada que escribe el usuario (la tabla de ciclos de vida de su unidad). Se guarda en el teléfono,
 * separada por Sala, para que cambiar de Sala no mezcle las metas.
 */
class PoblacionAsignadaRepositorio(context: Context) {
    private val preferencias = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    private fun clave(sala: String, grupo: GrupoCumplimiento, sexo: String) =
        "${sala.ifBlank { SALA_LOCAL }}|${grupo.name}|$sexo"

    fun cargar(sala: String): PoblacionAsignada = PoblacionAsignada(
        GrupoCumplimiento.entries.associateWith {
            preferencias.getInt(clave(sala, it, "H"), 0) to preferencias.getInt(clave(sala, it, "M"), 0)
        }
    )

    fun guardar(sala: String, poblacion: PoblacionAsignada) {
        preferencias.edit().apply {
            GrupoCumplimiento.entries.forEach {
                putInt(clave(sala, it, "H"), poblacion.hombres(it))
                putInt(clave(sala, it, "M"), poblacion.mujeres(it))
            }
        }.apply()
    }

    private companion object {
        const val ARCHIVO = "cumplimiento_poblacion"
        const val SALA_LOCAL = "local"
    }
}
