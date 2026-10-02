package com.ruralitos.app.data.mapa

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng

/** Inicio predeterminado por profesional y comienzo excepcional por ficha, disponibles sin red. */
object UbicacionesRuta {
    private const val ARCHIVO = "ubicaciones_ruta"

    data class Punto(val latitud: Double, val longitud: Double, val altitud: Double? = null) {
        val coordenada: LatLng get() = LatLng(latitud, longitud)
    }

    fun centro(context: Context, usuarioId: Long?): Punto? {
        if (usuarioId == null) return null
        return leer(context, "centro_$usuarioId")
    }

    fun guardarCentro(context: Context, usuarioId: Long, punto: Punto) {
        guardar(context, "centro_$usuarioId", punto)
        marcarIntroduccionVista(context, usuarioId)
    }

    fun inicioFicha(context: Context, fichaId: Long, usuarioId: Long?): Punto? =
        leer(context, "inicio_ficha_$fichaId") ?: centro(context, usuarioId)

    fun guardarInicioFicha(context: Context, fichaId: Long, punto: Punto) =
        guardar(context, "inicio_ficha_$fichaId", punto)

    fun introduccionVista(context: Context, usuarioId: Long): Boolean =
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            .getBoolean("introduccion_$usuarioId", false)

    fun marcarIntroduccionVista(context: Context, usuarioId: Long) {
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            .edit().putBoolean("introduccion_$usuarioId", true).apply()
    }

    private fun leer(context: Context, clave: String): Punto? = runCatching {
        val texto = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            .getString(clave, null) ?: return@runCatching null
        val valores = JSONArray(texto)
        val lat = valores.getDouble(0)
        val lon = valores.getDouble(1)
        if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0)
            return@runCatching null
        Punto(lat, lon, if (valores.isNull(2)) null else valores.getDouble(2))
    }.getOrNull()

    private fun guardar(context: Context, clave: String, punto: Punto) {
        require(punto.latitud.isFinite() && punto.longitud.isFinite())
        val valores = JSONArray().put(punto.latitud).put(punto.longitud)
            .put(punto.altitud ?: JSONObject.NULL)
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            .edit().putString(clave, valores.toString()).apply()
    }
}
