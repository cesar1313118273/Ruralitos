package com.ruralitos.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.ui.theme.TextoSecundario

/** El código se muestra de a cuatro letras para leerlo fácil; al copiarlo sale seguido, sin espacios. */
fun codigoAgrupado(codigo: String): String = codigo.chunked(4).joinToString(" ")

/** Texto listo para pegar en un chat con las instrucciones para usar el código. */
fun mensajeDeCodigo(codigo: String): String =
    // Solo letras, números y signos comunes: algunas aplicaciones de chat no muestran bien flechas ni símbolos especiales.
    "*Ruralitos*\n" +
        "Te compartieron acceso a fichas familiares.\n\n" +
        "*Tu código de acceso* (mantén presionado para copiarlo):\n" +
        "$codigo\n\n" +
        "*Cómo usarlo*\n" +
        "1. Abre Ruralitos e inicia sesión.\n" +
        "2. Entra a Mis Salas, luego a Compartir acceso.\n" +
        "3. Toca la sección Ingresar un código y pega el código.\n" +
        "4. Toca Verificar y agregar acceso.\n\n" +
        "El código caduca en 24 horas y se usa una sola vez."

/**
 * El código recién creado, grande y seleccionable, con un botón para copiarlo y otro para mandarlo por chat
 * (WhatsApp, Telegram, correo…) con las instrucciones incluidas.
 */
@Composable
fun TarjetaCodigoRuralitos(
    codigo: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Código de acceso",
                style = MaterialTheme.typography.labelLarge,
                color = TextoSecundario
            )
            SelectionContainer {
                Text(
                    codigoAgrupado(codigo),
                    modifier = Modifier.fillMaxWidth().testTag("codigo_generado"),
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = color
                )
            }
            Text(
                "Caduca en 24 horas y se usa una sola vez.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario,
                textAlign = TextAlign.Center
            )
            BotonPrincipalRuralitos(
                texto = "Copiar código",
                color = color,
                onClick = {
                    val portapapeles = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    portapapeles.setPrimaryClip(ClipData.newPlainText("Código de acceso de Ruralitos", codigo))
                    AvisosRuralitos.mostrar("Código copiado. Pégalo en el chat.")
                },
                modifier = Modifier.testTag("copiar_codigo")
            )
            BotonSecundarioRuralitos(
                texto = "Enviar por chat",
                onClick = {
                    val envio = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, mensajeDeCodigo(codigo))
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(envio, "Enviar el código").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }.onFailure { AvisosRuralitos.mostrar("No se encontró una aplicación para enviarlo.") }
                },
                modifier = Modifier.testTag("enviar_codigo")
            )
        }
    }
}
