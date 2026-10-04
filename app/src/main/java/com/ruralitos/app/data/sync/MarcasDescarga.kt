package com.ruralitos.app.data.sync

import android.content.Context

/**
 * Hasta dónde se ha descargado cada tabla de cada Sala, para pedir solo lo que cambió.
 * Solo guarda marcas de tiempo del servidor (ningún dato de pacientes).
 */
internal class MarcasDescarga(context: Context) {
    private val preferencias = context.applicationContext
        .getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun marca(organizacionId: String, tabla: String): String? =
        preferencias.getString("marca_${organizacionId}_$tabla", null)

    fun guardarMarcas(organizacionId: String, marcas: Map<String, String>) {
        if (marcas.isEmpty()) return
        preferencias.edit().apply {
            marcas.forEach { (tabla, valor) -> putString("marca_${organizacionId}_$tabla", valor) }
        }.apply()
    }

    /**
     * Una descarga completa cada cierto tiempo (y la primera) recoge lo que el modo incremental no puede
     * ver: filas borradas de verdad en el servidor, o cambios de permisos.
     */
    fun necesitaDescargaCompleta(organizacionId: String, ahora: Long, sinFichasLocales: Boolean): Boolean {
        if (sinFichasLocales) return true
        val ultima = preferencias.getLong("completa_$organizacionId", 0L)
        return ultima == 0L || ahora - ultima > INTERVALO_COMPLETA_MS || ahora < ultima
    }

    fun registrarCompleta(organizacionId: String, ahora: Long) {
        preferencias.edit().putLong("completa_$organizacionId", ahora).apply()
    }

    /** Última «huella de cambios» de la Sala con la que este teléfono quedó al día (ver `huella_de_cambios`). */
    fun huella(organizacionId: String): String? = preferencias.getString("huella_$organizacionId", null)

    fun guardarHuella(organizacionId: String, valor: String) {
        preferencias.edit().putString("huella_$organizacionId", valor).apply()
    }

    fun olvidar(organizacionId: String) {
        preferencias.edit().apply {
            preferencias.all.keys.filter { it.endsWith("_$organizacionId") || it.contains("_${organizacionId}_") }
                .forEach { remove(it) }
        }.apply()
    }

    companion object {
        private const val ARCHIVO = "ruralitos_sync_marcas"
        private const val INTERVALO_COMPLETA_MS = 24L * 60 * 60 * 1000

        /** Al cerrar sesión o cambiar de cuenta: la próxima descarga debe ser completa. */
        fun borrarTodo(context: Context) {
            context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
                .edit().clear().apply()
        }
    }
}
