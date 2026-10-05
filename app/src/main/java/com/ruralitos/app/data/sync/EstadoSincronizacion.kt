package com.ruralitos.app.data.sync

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

/** Estado mínimo y sin datos clínicos para informar al usuario del último intento. */
object EstadoSincronizacion {
    val revision = MutableStateFlow(0L)

    /** Verdadero mientras hay una sincronización en marcha (para no mostrar mensajes contradictorios a mitad de camino). */
    val enCurso = MutableStateFlow(false)

    private val errores = java.util.concurrent.ConcurrentLinkedQueue<String>()

    /** Anota por qué falló un paso de la sincronización (para mostrar el motivo y poder diagnosticarlo). */
    fun anotarError(origen: String, error: Throwable) {
        val texto = "$origen: ${error.message.orEmpty().ifBlank { error.javaClass.simpleName }}".take(220)
        errores.add(texto)
        while (errores.size > 8) errores.poll()
        if (com.ruralitos.app.BuildConfig.DEBUG) android.util.Log.w("RuralitosSync", texto)
    }

    /** El primer motivo anotado en la última sincronización, o vacío. */
    fun motivo(): String = errores.firstOrNull().orEmpty()

    fun iniciar() {
        errores.clear()
        enCurso.value = true
    }

    fun terminar() { enCurso.value = false }

    fun registrar(context: Context, usuarioId: String, correcto: Boolean) {
        val preferencias = context.getSharedPreferences("ruralitos_sync", Context.MODE_PRIVATE)
        val fallosSeguidos = if (correcto) 0 else preferencias.getInt("fallos_seguidos", 0) + 1
        preferencias.edit()
            .putLong("ultimo_intento", System.currentTimeMillis())
            .putString("ultimo_usuario", usuarioId)
            .putBoolean("ultimo_intento_correcto", correcto)
            .putInt("fallos_seguidos", fallosSeguidos)
            .putString("ultimo_motivo", if (correcto) "" else motivo())
            .apply()
        revision.value = System.currentTimeMillis()
    }

    /** El motivo del último fallo (queda en el teléfono; no se envía a ninguna parte). */
    fun motivoGuardado(context: Context): String =
        context.getSharedPreferences("ruralitos_sync", Context.MODE_PRIVATE).getString("ultimo_motivo", "").orEmpty()

    fun ultimoIntentoCorrecto(context: Context, usuarioId: String): Boolean? {
        val preferencias = context.getSharedPreferences("ruralitos_sync", Context.MODE_PRIVATE)
        if (!preferencias.contains("ultimo_intento") ||
            usuarioId.isBlank() || preferencias.getString("ultimo_usuario", null) != usuarioId) return null
        return preferencias.getBoolean("ultimo_intento_correcto", false)
    }

    /**
     * Cuántos intentos seguidos han fallado. Un fallo aislado (una red que se cae un instante) no debe mostrarse como
     * «no se pudo sincronizar»: eso solo se avisa cuando se repite.
     */
    fun fallosSeguidos(context: Context, usuarioId: String): Int {
        val preferencias = context.getSharedPreferences("ruralitos_sync", Context.MODE_PRIVATE)
        if (usuarioId.isBlank() || preferencias.getString("ultimo_usuario", null) != usuarioId) return 0
        return preferencias.getInt("fallos_seguidos", 0)
    }

    const val FALLOS_PARA_AVISAR = 2

    /** El texto que se muestra en el inicio; separado para poder probarlo. */
    fun mensaje(
        conflictos: Int,
        pendientes: Int,
        enCurso: Boolean,
        hayInternet: Boolean,
        fallosSeguidos: Int,
        ultimoCorrecto: Boolean?,
        motivo: String = ""
    ): String = when {
        conflictos > 0 -> "$conflictos cambios por revisar"
        // Con internet y algo por subir: se está sincronizando (o se va a sincronizar en un instante).
        pendientes > 0 && hayInternet && enCurso -> "Sincronizando $pendientes cambio${if (pendientes == 1) "" else "s"}…"
        pendientes > 0 && !hayInternet -> "Sin conexión · $pendientes pendiente${if (pendientes == 1) "" else "s"} de sincronizar"
        fallosSeguidos >= FALLOS_PARA_AVISAR ->
            "No se pudo sincronizar · datos guardados, se reintentará solo" +
                (if (motivo.isBlank()) "" else "\nMotivo: $motivo")
        pendientes > 0 -> "$pendientes pendiente${if (pendientes == 1) "" else "s"} de sincronizar"
        !hayInternet -> "Sin conexión · datos guardados en el teléfono"
        ultimoCorrecto == true || ultimoCorrecto == false -> "Sincronizado"
        else -> "Guardado local"
    }
}
