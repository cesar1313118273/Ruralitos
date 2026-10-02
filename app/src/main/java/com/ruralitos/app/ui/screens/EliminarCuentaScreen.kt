package com.ruralitos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.MensajeEstadoRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.RojoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import kotlinx.coroutines.launch

@Composable
fun EliminarCuentaScreen(
    correo: String,
    onCrearRespaldo: () -> Unit,
    onEnviarCodigo: suspend () -> Unit,
    onEliminar: suspend (String) -> Unit,
    onEliminada: () -> Unit,
    onRegresar: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var codigoEnviado by remember { mutableStateOf(false) }
    var codigo by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var procesando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var confirmarFinal by remember { mutableStateOf(false) }

    confirmarFinal.takeIf { it }?.let {
        AlertDialog(
            onDismissRequest = { confirmarFinal = false },
            title = { Text("Última confirmación") },
            text = {
                Text(
                    "La cuenta, las Salas que solo te pertenezcan y sus fichas remotas se eliminarán. " +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !procesando,
                    onClick = {
                        confirmarFinal = false
                        procesando = true
                        mensaje = null
                        scope.launch {
                            runCatching { onEliminar(codigo) }
                                .onSuccess { onEliminada() }
                                .onFailure {
                                    mensaje = it.message
                                        ?: "No se pudo eliminar la cuenta. Solicita un código nuevo."
                                    codigo = ""
                                }
                            procesando = false
                        }
                    }
                ) { Text("Sí, eliminar definitivamente", color = RojoClinico) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarFinal = false }) { Text("Cancelar") }
            }
        )
    }

    PantallaRuralitos(
        titulo = "Eliminar mi cuenta",
        descripcion = "Proceso protegido mediante un código temporal enviado al correo confirmado.",
        subtitulo = "Acción irreversible",
        barraAccion = {
            BotonSecundarioRuralitos(
                texto = "Conservar mi cuenta y regresar",
                descripcion = "Salir sin eliminar información",
                onClick = onRegresar,
                enabled = !procesando
            )
        }
    ) {
        MensajeEstadoRuralitos(
            titulo = "Crea un respaldo antes de continuar",
            descripcion = "El respaldo portable permite recuperar tus fichas posteriormente desde otra cuenta.",
            color = NaranjaClinico,
            simbolo = "!"
        )
        BotonPrincipalRuralitos(
            texto = "Crear o revisar mi respaldo",
            descripcion = "Ir a Seguridad y respaldos antes de eliminar",
            onClick = onCrearRespaldo,
            color = CianRuralitos
        )
        SeccionFormularioRuralitos(
            titulo = "1. Verificar el correo",
            descripcion = "Enviaremos un código de un solo uso a ${ocultarCorreo(correo)}."
        ) {
            BotonPrincipalRuralitos(
                texto = when {
                    procesando -> "Procesando…"
                    codigoEnviado -> "Enviar un código nuevo"
                    else -> "Enviar código de verificación"
                },
                descripcion = "El código caduca y solo sirve para esta acción",
                onClick = {
                    procesando = true
                    mensaje = null
                    scope.launch {
                        runCatching { onEnviarCodigo() }
                            .onSuccess {
                                codigoEnviado = true
                                mensaje = "Código enviado. Revisa la bandeja de entrada y correo no deseado."
                            }
                            .onFailure {
                                mensaje = it.message ?: "No se pudo enviar el código."
                            }
                        procesando = false
                    }
                },
                enabled = !procesando,
                color = AzulClinico
            )
        }
        if (codigoEnviado) {
            SeccionFormularioRuralitos(
                titulo = "2. Confirmar eliminación",
                descripcion = "Escribe el código recibido y la palabra ELIMINAR."
            ) {
                OutlinedTextField(
                    value = codigo,
                    onValueChange = {
                        codigo = it.filter(Char::isDigit).take(8)
                        mensaje = null
                    },
                    label = { Text("Código del correo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmacion,
                    onValueChange = {
                        confirmacion = it.uppercase().filter(Char::isLetter).take(8)
                        mensaje = null
                    },
                    label = { Text("Escribe ELIMINAR") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
            }
        }
        mensaje?.let {
            MensajeEstadoRuralitos(
                titulo = if (it.startsWith("Código enviado")) "Verificación iniciada" else "Revisa el proceso",
                descripcion = it,
                color = if (it.startsWith("Código enviado")) CianRuralitos else RojoClinico,
                simbolo = if (it.startsWith("Código enviado")) "✓" else "!"
            )
        }
        if (codigoEnviado) {
            BotonPrincipalRuralitos(
                texto = if (procesando) "Eliminando cuenta…" else "Eliminar mi cuenta definitivamente",
                descripcion = "Borrar cuenta y datos exclusivos después de la última confirmación",
                onClick = {
                    mensaje = when {
                        codigo.length < 6 -> "Ingresa el código completo recibido por correo."
                        confirmacion != "ELIMINAR" -> "Escribe exactamente la palabra ELIMINAR."
                        else -> null
                    }
                    if (mensaje == null) confirmarFinal = true
                },
                enabled = !procesando,
                color = RojoClinico
            )
        }
    }
}

private fun ocultarCorreo(correo: String): String {
    val usuario = correo.substringBefore('@')
    val dominio = correo.substringAfter('@', "")
    if (usuario.isBlank() || dominio.isBlank()) return "tu correo registrado"
    val visible = usuario.take(2)
    return "$visible***@${dominio}"
}
