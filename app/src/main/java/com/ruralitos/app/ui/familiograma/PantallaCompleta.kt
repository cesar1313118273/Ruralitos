package com.ruralitos.app.ui.familiograma

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Pantalla completa para el lienzo: la ventana se extiende bajo la cámara (muesca), se ocultan las barras
 * de estado y de navegación (reaparecen un momento al deslizar desde el borde) y, al salir, todo vuelve
 * a como estaba.
 */
internal class PantallaCompleta private constructor(
    private val ventana: Window,
    private val modoMuescaAnterior: Int
) {
    fun restaurar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ventana.attributes = ventana.attributes.apply { layoutInDisplayCutoutMode = modoMuescaAnterior }
        }
        WindowCompat.setDecorFitsSystemWindows(ventana, true)
        WindowCompat.getInsetsController(ventana, ventana.decorView).show(WindowInsetsCompat.Type.systemBars())
    }

    companion object {
        fun activar(ventana: Window): PantallaCompleta {
            val anterior = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ventana.attributes.layoutInDisplayCutoutMode
            } else 0
            WindowCompat.setDecorFitsSystemWindows(ventana, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ventana.attributes = ventana.attributes.apply {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            val controlador = WindowCompat.getInsetsController(ventana, ventana.decorView)
            controlador.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controlador.hide(WindowInsetsCompat.Type.systemBars())
            return PantallaCompleta(ventana, anterior)
        }
    }
}
