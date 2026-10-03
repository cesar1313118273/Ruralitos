package com.ruralitos.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * ¿La línea se parte en medio de una palabra? Una frase puede seguir en otra línea, pero una palabra no debe cortarse
 * («Seguimien» / «to»). Se comprueba mirando el carácter donde termina cada línea y el que le sigue.
 */
fun cortaPalabra(texto: CharSequence, resultado: TextLayoutResult): Boolean {
    for (linea in 0 until resultado.lineCount - 1) {
        val fin = resultado.getLineEnd(linea, visibleEnd = false)
        if (fin in 1 until texto.length && !texto[fin - 1].isWhitespace() && !texto[fin].isWhitespace() &&
            texto[fin - 1].isLetterOrDigit() && texto[fin].isLetterOrDigit()
        ) return true
    }
    return false
}

/**
 * Texto que se achica solo, de un punto en un punto, hasta que ninguna letra queda cortada: ni por salirse de la línea
 * ni partiendo una palabra a la mitad. Sirve para pestañas, botones y etiquetas con poco espacio; las frases largas
 * pueden pasar a otra línea (hasta [maxLineas]).
 */
@Composable
fun TextoAjustado(
    texto: String,
    modifier: Modifier = Modifier,
    tamano: TextUnit = 14.sp,
    tamanoMinimo: TextUnit = 7.sp,
    maxLineas: Int = 1,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalTextStyle.current
) {
    var actual by remember(texto, tamano, tamanoMinimo, maxLineas) { mutableStateOf(tamano) }
    var listo by remember(texto, tamano, tamanoMinimo, maxLineas) { mutableStateOf(false) }
    Box(modifier) {
        Text(
            texto,
            modifier = Modifier.drawWithContent { if (listo) drawContent() },
            color = color,
            fontSize = actual,
            fontWeight = fontWeight,
            textAlign = textAlign,
            style = style,
            maxLines = maxLineas,
            softWrap = maxLineas > 1,
            onTextLayout = { r ->
                val corta = r.didOverflowWidth || r.didOverflowHeight || cortaPalabra(texto, r)
                if (corta && actual.value > tamanoMinimo.value) actual = (actual.value - 0.5f).sp
                else listo = true
            }
        )
    }
}
