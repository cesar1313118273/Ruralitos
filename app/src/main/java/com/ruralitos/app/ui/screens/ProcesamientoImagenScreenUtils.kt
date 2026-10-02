package com.ruralitos.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import com.ruralitos.app.domain.ProcesadorFondoClaro
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

internal data class ResultadoImagenFondo(
    val bitmap: Bitmap,
    val porcentajeTransparente: Int
)

internal fun cargarImagenReducida(
    context: Context,
    uri: Uri,
    dimensionMaxima: Int = 2400
): Bitmap? = runCatching {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, limites)
    }
    if (limites.outWidth <= 0 || limites.outHeight <= 0) return@runCatching null

    var muestra = 1
    while (max(limites.outWidth, limites.outHeight) / muestra > dimensionMaxima) muestra *= 2
    val opciones = BitmapFactory.Options().apply {
        inSampleSize = muestra
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opciones)
    }
}.getOrNull()

internal fun procesarFondoClaro(origen: Bitmap, intensidad: Int): ResultadoImagenFondo {
    val bitmap = origen.copy(Bitmap.Config.ARGB_8888, true).apply {
        setHasAlpha(true)
        setPremultiplied(true)
    }
    val pixeles = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixeles, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val resultado = ProcesadorFondoClaro.procesar(
        ancho = bitmap.width,
        alto = bitmap.height,
        pixelesOriginales = pixeles,
        intensidad = intensidad
    )
    bitmap.setPixels(resultado.pixeles, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val recortado = recortarBordesTransparentes(bitmap).apply { setHasAlpha(true) }
    return ResultadoImagenFondo(recortado, resultado.porcentajeTransparente)
}

internal fun tieneTransparenciaReal(bitmap: Bitmap): Boolean {
    if (!bitmap.hasAlpha()) return false
    val ancho = bitmap.width
    val alto = bitmap.height
    val paso = max(1, max(ancho, alto) / 900)
    val fila = IntArray(ancho)
    var y = 0
    while (y < alto) {
        bitmap.getPixels(fila, 0, ancho, 0, y, ancho, 1)
        var x = 0
        while (x < ancho) {
            if (AndroidColor.alpha(fila[x]) < 245) return true
            x += paso
        }
        y += paso
    }
    return false
}

/** Escribe PNG y vuelve a leerlo para comprobar que el canal alfa sobrevivió al guardado. */
internal fun guardarPngConTransparenciaVerificada(bitmap: Bitmap, archivo: File): Boolean {
    bitmap.setHasAlpha(true)
    FileOutputStream(archivo).use {
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) return false
    }
    val verificacion = BitmapFactory.decodeFile(archivo.absolutePath) ?: return false
    return try {
        tieneTransparenciaReal(verificacion)
    } finally {
        verificacion.recycle()
    }
}

/** Cuadrícula gris/blanca: permite ver de inmediato qué zonas son transparentes. */
internal fun Modifier.fondoCuadriculaTransparente(): Modifier = drawBehind {
    val lado = 18f.coerceAtMost(size.minDimension / 8f).coerceAtLeast(8f)
    var fila = 0
    var y = 0f
    while (y < size.height) {
        var columna = 0
        var x = 0f
        while (x < size.width) {
            drawRect(
                color = if ((fila + columna) % 2 == 0) Color.White else Color(0xFFDCE4EA),
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(
                    minOf(lado, size.width - x),
                    minOf(lado, size.height - y)
                )
            )
            columna++
            x += lado
        }
        fila++
        y += lado
    }
}

internal fun recortarBordesTransparentes(bitmap: Bitmap): Bitmap {
    var izquierda = bitmap.width
    var derecha = -1
    var arriba = bitmap.height
    var abajo = -1
    // Leer una fila cada vez mantiene el consumo estable incluso con fotos grandes.
    val fila = IntArray(bitmap.width)
    for (y in 0 until bitmap.height) {
        bitmap.getPixels(fila, 0, bitmap.width, 0, y, bitmap.width, 1)
        for (x in fila.indices) {
            if (AndroidColor.alpha(fila[x]) > 10) {
                if (x < izquierda) izquierda = x
                if (x > derecha) derecha = x
                if (y < arriba) arriba = y
                if (y > abajo) abajo = y
            }
        }
    }
    if (derecha < izquierda || abajo < arriba) return bitmap

    val margen = 12
    izquierda = (izquierda - margen).coerceAtLeast(0)
    arriba = (arriba - margen).coerceAtLeast(0)
    derecha = (derecha + margen).coerceAtMost(bitmap.width - 1)
    abajo = (abajo + margen).coerceAtMost(bitmap.height - 1)
    val recortado = Bitmap.createBitmap(
        bitmap,
        izquierda,
        arriba,
        derecha - izquierda + 1,
        abajo - arriba + 1
    ).apply { setHasAlpha(true) }
    if (recortado !== bitmap) bitmap.recycle()
    return recortado
}
