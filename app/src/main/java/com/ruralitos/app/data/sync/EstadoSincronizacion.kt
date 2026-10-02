package com.ruralitos.app.data.sync

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

/** Estado mínimo y sin datos clínicos para informar al usuario del último intento. */
object EstadoSincronizacion {
    val revision = MutableStateFlow(0L)

    fun registrar(context: Context, usuarioId: String, correcto: Boolean) {
        val preferencias = context.getSharedPreferences("ruralitos_sync", Context.MODE_PRIVATE)
        preferencias.edit()
            .putLong("ultimo_intento", System.currentTimeMillis())
            .putString("ultimo_usuario", usuarioId)
            .putBoolean("ultimo_intento_correcto", correcto)
            .apply()
        revision.value = System.currentTimeMillis()
    }

    fun ultimoIntentoCorrecto(context: Context, usuarioId: String): Boolean? {
        val preferencias = context.getSharedPreferences("ruralitos_sync", Context.MODE_PRIVATE)
        if (!preferencias.contains("ultimo_intento") ||
            usuarioId.isBlank() || preferencias.getString("ultimo_usuario", null) != usuarioId) return null
        return preferencias.getBoolean("ultimo_intento_correcto", false)
    }
}
