package com.ruralitos.app.ui.components

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Avisos breves de la app (guardado, error, sincronizado…): una franja abajo con ícono, verde si salió bien y roja si
 * hubo un problema. Reemplazan a los mensajes grises de Android. Se pueden lanzar desde cualquier parte.
 */
object AvisosRuralitos {
    class Aviso(val id: Long, val mensaje: String, val error: Boolean)

    internal val vigentes = mutableStateListOf<Aviso>()
    private var siguiente = 0L
    private val principal = Handler(Looper.getMainLooper())

    /** Un mensaje que empieza por «No…» o habla de un problema es un error; lo demás es un aviso de éxito. */
    fun esError(mensaje: String): Boolean {
        val m = mensaje.trim().lowercase()
        return m.startsWith("no ") || m.startsWith("error") || m.contains("no se pudo") ||
            m.contains("no se pudieron") || m.contains("falló") || m.contains("fallo")
    }

    fun mostrar(mensaje: String, error: Boolean = esError(mensaje)) {
        if (mensaje.isBlank()) return
        principal.post {
            vigentes.removeAll { it.mensaje == mensaje }
            vigentes.add(Aviso(siguiente++, mensaje, error))
            while (vigentes.size > 2) vigentes.removeAt(0)
        }
    }

    internal fun quitar(id: Long) {
        principal.post { vigentes.removeAll { it.id == id } }
    }
}

/** Se coloca una sola vez encima de toda la app; dibuja los avisos vigentes y los quita solos. */
@Composable
fun HostAvisosRuralitos(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AvisosRuralitos.vigentes.toList().forEach { aviso ->
                androidx.compose.runtime.key(aviso.id) { TarjetaAviso(aviso) }
            }
        }
    }
}

@Composable
private fun TarjetaAviso(aviso: AvisosRuralitos.Aviso) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(aviso.id) {
        visible = true
        delay(if (aviso.mensaje.length > 70) 5_000 else 3_500)
        visible = false
        delay(250)
        AvisosRuralitos.quitar(aviso.id)
    }
    val fondo = if (aviso.error) Color(0xFFFCE8EA) else Color(0xFFE9F5DF)
    val borde = if (aviso.error) Color(0x59D93F4C) else Color(0x593A7F1F)
    val texto = if (aviso.error) Color(0xFFA02834) else Color(0xFF27500A)
    val punto = if (aviso.error) Color(0xFFD93F4C) else Color(0xFF3A7F1F)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().testTag("aviso_ruralitos"),
            shape = RoundedCornerShape(14.dp),
            color = fondo,
            border = BorderStroke(1.dp, borde),
            shadowElevation = 4.dp
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = punto, modifier = Modifier.size(24.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (aviso.error) "!" else "✓", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(
                    aviso.mensaje,
                    color = texto,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}
