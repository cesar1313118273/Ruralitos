package com.ruralitos.app.data.session

import android.content.Context

class SesionLocal(context: Context) {
    private val preferencias = context.applicationContext.getSharedPreferences(
        "sesion_ruralitos",
        Context.MODE_PRIVATE
    )

    fun guardar(usuarioId: Long) {
        preferencias.edit()
            .putLong(CLAVE_USUARIO_ID, usuarioId)
            .remove(CLAVE_SALIDA)
            .apply()
    }

    fun usuarioId(): Long? {
        if (!preferencias.contains(CLAVE_USUARIO_ID)) return null
        return preferencias.getLong(CLAVE_USUARIO_ID, -1L).takeIf { it > 0L }
    }

    fun cerrar() {
        preferencias.edit().remove(CLAVE_USUARIO_ID).remove(CLAVE_SALIDA).apply()
    }

    fun registrarSalida() {
        if (usuarioId() != null) {
            preferencias.edit().putLong(CLAVE_SALIDA, System.currentTimeMillis()).apply()
        }
    }

    fun estaVencida(ahora: Long = System.currentTimeMillis()): Boolean {
        val salida = preferencias.getLong(CLAVE_SALIDA, 0L)
        return salida > 0L && ahora - salida >= TIEMPO_BLOQUEO_MS
    }

    private companion object {
        const val CLAVE_USUARIO_ID = "usuario_id"
        const val CLAVE_SALIDA = "ultima_salida"
        const val TIEMPO_BLOQUEO_MS = 15 * 60 * 1000L
    }
}
