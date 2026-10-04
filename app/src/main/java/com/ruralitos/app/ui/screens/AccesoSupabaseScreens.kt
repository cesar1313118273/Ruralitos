package com.ruralitos.app.ui.screens

import androidx.compose.foundation.BorderStroke
import com.ruralitos.app.ui.components.VentanaConfirmarRuralitos
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.domain.SaludoProfesional
import com.ruralitos.app.VerdeOscuro
import com.ruralitos.app.domain.ValidadorIdentidadEcuador
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.LogoRuralitos
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.TarjetaFormularioRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeCampo
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.FondoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.TextoSecundario
import com.ruralitos.app.ui.theme.VerdeSalud

// ---------------------------------------------------------------------------------------------------------------
// Pantallas de acceso con el diseño general de la app: encabezado con botón de volver, tarjetas blancas de bordes
// finos, secciones desplegables con barra de avance y el botón principal fijo abajo.
// ---------------------------------------------------------------------------------------------------------------

@Composable
fun AccesoSupabaseScreen(
    procesando: Boolean,
    mensaje: String?,
    onIngresar: (String, String) -> Unit,
    onCrearCuenta: () -> Unit,
    onRecuperar: () -> Unit,
    onSinInternet: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }

    BoxWithConstraints(Modifier.fillMaxSize().formularioSeguro().background(FondoClinico)) {
        val altoDisponible = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = altoDisponible)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoRuralitos(modifier = Modifier.size(76.dp))
            Text(
                "Ruralitos",
                color = AzulClinicoOscuro,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Fichas familiares, incluso sin conexión",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            TarjetaFormularioRuralitos(Modifier.widthIn(max = 480.dp).padding(top = 20.dp)) {
                Column {
                    Text("Ingresa a tu cuenta", style = MaterialTheme.typography.titleMedium, color = AzulClinicoOscuro, fontWeight = FontWeight.SemiBold)
                    Text("Usa el correo con el que te registraste.", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
                }
                CampoCorreo(correo) { correo = it }
                CampoClave(clave, "Contraseña") { clave = it }
                TextButton(onClick = onRecuperar, enabled = !procesando, modifier = Modifier.align(Alignment.End)) {
                    Text("¿Olvidaste tu contraseña?", color = AzulClinico, fontWeight = FontWeight.SemiBold)
                }
                MensajeAcceso(mensaje)
                BotonPrincipalRuralitos(
                    texto = if (procesando) "Entrando…" else "Entrar",
                    color = CianRuralitos,
                    enabled = !procesando && correoValido(correo) && clave.isNotBlank(),
                    modifier = Modifier.testTag("boton_entrar"),
                    onClick = { onIngresar(correo.trim(), clave) }
                )
            }

            Row(
                Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(Modifier.weight(1f), color = BordeClinico)
                Text("  o  ", color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(Modifier.weight(1f), color = BordeClinico)
            }
            BotonSecundarioRuralitos(
                texto = "Entrar sin internet con PIN",
                onClick = onSinInternet,
                enabled = !procesando,
                modifier = Modifier.widthIn(max = 480.dp).testTag("boton_sin_internet")
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Text("¿Aún no tienes cuenta?", color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onCrearCuenta, enabled = !procesando, modifier = Modifier.testTag("boton_crear_cuenta")) {
                    Text("Crear una cuenta", color = AzulClinico, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun RegistroSupabaseScreen(
    procesando: Boolean,
    mensaje: String?,
    onRegistrar: (String, String, String, String, String, String, String, String, String) -> Unit,
    onVolver: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var nombres by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var codigoSenescyt by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    FormularioBase(
        "Crear cuenta",
        "Completa tus datos para empezar a crear fichas familiares",
        onVolver = onVolver,
        accion = {
            BotonPrincipalRuralitos(
                texto = if (procesando) "Creando cuenta…" else "Crear mi cuenta",
                color = CianRuralitos,
                enabled = !procesando,
                modifier = Modifier.testTag("boton_crear_mi_cuenta"),
                onClick = {
                    error = when {
                        !correoValido(correo) -> "Ingresa un correo válido."
                        !ValidadorIdentidadEcuador.esIdentificacionAceptable(cedula) -> "Ingresa una cédula o RUC válido."
                        sexo.isBlank() -> "Elige tu sexo."
                        apellidos.isBlank() -> "Ingresa tus apellidos."
                        nombres.isBlank() -> "Ingresa tus nombres."
                        cargo.isBlank() -> "Elige tu cargo."
                        !codigoSenescytValido(codigoSenescyt) -> "Completa el código SENESCYT con sus 15 dígitos."
                        clave.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
                        clave != confirmar -> "Las contraseñas no coinciden."
                        else -> null
                    }
                    if (error == null) {
                        onRegistrar(
                            correo.trim(),
                            clave,
                            cedula.trim(),
                            "${apellidos.trim()} ${nombres.trim()}".replace(Regex("\\s+"), " "),
                            cargo.trim(),
                            telefono.trim(),
                            codigoSenescyt.trim(),
                            sexo,
                            apellidos.trim()
                        )
                    }
                }
            )
        }
    ) {
        SeccionFormularioRuralitos(
            titulo = "1. Datos de acceso",
            descripcion = "El correo será tu usuario para ingresar.",
            desplegable = true,
            abiertaInicial = true,
            progreso = fraccion(correoValido(correo))
        ) {
            CampoCorreo(correo) { correo = it; error = null }
        }
        SeccionFormularioRuralitos(
            titulo = "2. Información personal y profesional",
            descripcion = "Estos datos identificarán al responsable de las fichas.",
            desplegable = true,
            abiertaInicial = false,
            progreso = fraccion(
                ValidadorIdentidadEcuador.esIdentificacionAceptable(cedula), sexo.isNotBlank(), apellidos.isNotBlank(),
                nombres.isNotBlank(), cargo.isNotBlank(), codigoSenescytValido(codigoSenescyt)
            )
        ) {
            OutlinedTextField(
                value = cedula,
                onValueChange = { cedula = it.filter(Char::isDigit).take(13); error = null },
                label = { Text("Cédula") },
                supportingText = { Text("Será también tu número de historia clínica") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = FormaCampo,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
            SelectorSexoProfesional(
                valor = sexo,
                onCambio = {
                    sexo = it
                    cargo = SaludoProfesional.cargoEquivalente(cargo, it)
                    error = null
                }
            )
            CampoTexto(apellidos, "Apellidos") { apellidos = it.take(80); error = null }
            CampoTexto(nombres, "Nombres") { nombres = it.take(80); error = null }
            CampoCargoPredeterminado(valor = cargo, sexo = sexo, onCambio = { cargo = it; error = null })
            CampoCodigoProfesional(codigoSenescyt) { codigoSenescyt = it; error = null }
            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it.filter { c -> c.isDigit() || c == '+' }.take(20) },
                label = { Text("Teléfono (opcional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = FormaCampo,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
        }
        SeccionFormularioRuralitos(
            titulo = "3. Seguridad",
            descripcion = "Usa al menos 8 caracteres y confirma la contraseña.",
            desplegable = true,
            abiertaInicial = false,
            progreso = fraccion(clave.length >= 8, confirmar.isNotBlank() && confirmar == clave)
        ) {
            CampoClave(clave, "Contraseña") { clave = it; error = null }
            CampoClave(confirmar, "Confirmar contraseña") { confirmar = it; error = null }
        }
        MensajeAcceso(error ?: mensaje)
    }
}

@Composable
fun RecuperarCuentaScreen(
    procesando: Boolean,
    mensaje: String?,
    onEnviar: (String) -> Unit,
    onVolver: () -> Unit
) {
    var correo by remember { mutableStateOf("") }

    FormularioBase(
        "Recuperar contraseña",
        "Te enviaremos un enlace seguro al correo registrado",
        onVolver = onVolver,
        accion = {
            BotonPrincipalRuralitos(
                texto = if (procesando) "Enviando enlace…" else "Enviar enlace de recuperación",
                color = CianRuralitos,
                enabled = !procesando && correoValido(correo),
                modifier = Modifier.testTag("boton_enviar_enlace"),
                onClick = { onEnviar(correo.trim()) }
            )
        }
    ) {
        TarjetaFormularioRuralitos {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                IconoAcceso("✉", CianRuralitos)
                Text(
                    "¿Olvidaste tu contraseña?",
                    style = MaterialTheme.typography.titleMedium, color = AzulClinicoOscuro,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "Escribe tu correo y te enviamos un enlace para crear una nueva.",
                    style = MaterialTheme.typography.bodyMedium, color = TextoSecundario, textAlign = TextAlign.Center
                )
            }
            CampoCorreo(correo) { correo = it }
        }
        AvisoAccesoRuralitos(
            texto = "Abre el enlace desde este mismo teléfono para volver a Ruralitos y crear una contraseña nueva.",
            color = AzulClinico,
            simbolo = "i"
        )
        MensajeAcceso(mensaje)
    }
}

@Composable
fun OrganizacionInicialScreen(
    procesando: Boolean,
    mensaje: String?,
    onAceptarCodigo: (String) -> Unit,
    onCrearSala: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var invitacion by remember { mutableStateOf("") }

    FormularioBase(
        "Código del grupo",
        "Vincula las fichas con tu institución",
        accion = {
            BotonPrincipalRuralitos(
                texto = if (procesando) "Comprobando…" else "Continuar con el código",
                color = CianRuralitos,
                enabled = !procesando && invitacion.length >= 8,
                onClick = { onAceptarCodigo(invitacion.trim()) }
            )
        }
    ) {
        AvisoAccesoRuralitos(
            texto = "Ingresa el código entregado por la administración. Así las fichas podrán revisarse posteriormente desde la plataforma web.",
            color = AzulClinico,
            simbolo = "i"
        )
        TarjetaFormularioRuralitos {
            OutlinedTextField(
                value = invitacion,
                onValueChange = { invitacion = it.uppercase().filter(Char::isLetterOrDigit).take(32) },
                label = { Text("Código de invitación") },
                supportingText = { Text("Mínimo 8 caracteres") },
                singleLine = true,
                shape = FormaCampo,
                modifier = Modifier.fillMaxWidth()
            )
        }
        MensajeAcceso(mensaje)
        TarjetaFormularioRuralitos {
            Text("¿Aún no tienes un código?", style = MaterialTheme.typography.titleMedium, color = AzulClinicoOscuro, fontWeight = FontWeight.SemiBold)
            Text(
                "Elige tu centro de salud y crea tu propia Sala. Luego podrás agregar EAIS y barrios.",
                style = MaterialTheme.typography.bodyMedium, color = TextoSecundario
            )
            BotonSecundarioRuralitos(texto = "Crear mi propia Sala", onClick = onCrearSala, enabled = !procesando)
        }
        BotonSecundarioRuralitos(texto = "Cerrar sesión", onClick = onCerrarSesion, enabled = !procesando)
    }
}

@Composable
fun ConfigurarPinScreen(
    procesando: Boolean,
    mensaje: String?,
    onGuardar: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    FormularioBase(
        "Protege el acceso sin internet",
        "Crea un PIN de 6 dígitos para este teléfono",
        accion = {
            BotonPrincipalRuralitos(
                texto = if (procesando) "Protegiendo acceso…" else "Guardar PIN seguro",
                color = CianRuralitos,
                enabled = !procesando,
                modifier = Modifier.testTag("boton_guardar_pin"),
                onClick = {
                    error = when {
                        pin.length != 6 -> "El PIN debe tener exactamente 6 dígitos."
                        pin != confirmar -> "Los PIN no coinciden."
                        pin.toSet().size == 1 -> "Elige un PIN menos predecible."
                        else -> null
                    }
                    if (error == null) onGuardar(pin)
                }
            )
        }
    ) {
        AvisoAccesoRuralitos(
            texto = "Este PIN permite abrir Ruralitos cuando no hay conexión. No sustituye la contraseña de tu cuenta.",
            color = AzulClinico,
            simbolo = "#"
        )
        TarjetaFormularioRuralitos {
            CampoPinCasillas(pin, "PIN de 6 dígitos", "pin_nuevo") { pin = it; error = null }
            CampoPinCasillas(confirmar, "Confirmar PIN", "pin_confirmar") { confirmar = it; error = null }
        }
        MensajeAcceso(error ?: mensaje)
    }
}

@Composable
fun DesbloqueoOfflineScreen(
    procesando: Boolean,
    mensaje: String?,
    onEntrar: (String, String) -> Unit,
    onVolver: () -> Unit
) {
    var cedula by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    val completo = cedula.length >= 10 && pin.length == 6

    FormularioBase(
        "Entrar sin internet",
        "Cédula y PIN guardados en este teléfono",
        onVolver = onVolver,
        accion = {
            BotonPrincipalRuralitos(
                texto = if (procesando) "Comprobando…" else "Entrar sin conexión",
                color = VerdeSalud,
                enabled = !procesando && completo,
                modifier = Modifier.testTag("boton_entrar_sin_conexion"),
                onClick = { onEntrar(cedula, pin) }
            )
        }
    ) {
        AvisoAccesoRuralitos(
            texto = "Sin conexión. Podrás consultar y crear fichas locales; se sincronizarán cuando vuelva internet.",
            color = NaranjaClinico,
            simbolo = "!"
        )
        TarjetaFormularioRuralitos {
            Text("Tu identificación", style = MaterialTheme.typography.titleMedium, color = AzulClinicoOscuro, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = cedula,
                onValueChange = { cedula = it.filter(Char::isDigit).take(13) },
                label = { Text("Cédula") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = FormaCampo,
                modifier = Modifier.fillMaxWidth().testTag("campo_cedula")
            )
            CampoPinCasillas(pin, "PIN de 6 dígitos", "pin_acceso") { pin = it }
        }
        MensajeAcceso(mensaje)
    }
}

@Composable
fun NuevaClaveSupabaseScreen(
    procesando: Boolean,
    mensaje: String?,
    onGuardar: (String) -> Unit
) {
    var clave by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    FormularioBase(
        "Crear nueva contraseña",
        "Elige una clave segura para recuperar tu cuenta",
        accion = {
            BotonPrincipalRuralitos(
                texto = if (procesando) "Actualizando…" else "Guardar nueva contraseña",
                color = CianRuralitos,
                enabled = !procesando,
                onClick = {
                    error = when {
                        clave.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
                        clave != confirmar -> "Las contraseñas no coinciden."
                        else -> null
                    }
                    if (error == null) onGuardar(clave)
                }
            )
        }
    ) {
        AvisoAccesoRuralitos(
            texto = "La contraseña debe tener al menos 8 caracteres. Evita usar tu cédula o datos fáciles de adivinar.",
            color = AzulClinico,
            simbolo = "i"
        )
        TarjetaFormularioRuralitos {
            CampoClave(clave, "Nueva contraseña") { clave = it; error = null }
            CampoClave(confirmar, "Confirmar nueva contraseña") { confirmar = it; error = null }
        }
        MensajeAcceso(error ?: mensaje)
    }
}

@Composable
fun CambiarClaveCuentaScreen(
    procesando: Boolean,
    mensaje: String?,
    onGuardar: (String, String) -> Unit,
    onVolver: () -> Unit
) {
    var actual by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    FormularioBase(
        "Cambiar contraseña",
        "Confirma tu contraseña actual antes de crear una nueva",
        onVolver = onVolver,
        accion = {
            BotonPrincipalRuralitos(
                texto = if (procesando) "Actualizando…" else "Cambiar contraseña",
                color = CianRuralitos,
                enabled = !procesando,
                onClick = {
                    error = when {
                        actual.isBlank() -> "Ingresa la contraseña actual."
                        nueva.length < 8 -> "La nueva contraseña debe tener al menos 8 caracteres."
                        nueva != confirmar -> "Las contraseñas nuevas no coinciden."
                        nueva == actual -> "La nueva contraseña debe ser diferente."
                        else -> null
                    }
                    if (error == null) onGuardar(actual, nueva)
                }
            )
        }
    ) {
        SeccionFormularioRuralitos(
            titulo = "1. Verifica tu identidad",
            descripcion = "Escribe la contraseña que usas actualmente.",
            desplegable = true,
            abiertaInicial = true,
            progreso = fraccion(actual.isNotBlank())
        ) {
            CampoClave(actual, "Contraseña actual") { actual = it; error = null }
        }
        SeccionFormularioRuralitos(
            titulo = "2. Crea la contraseña nueva",
            descripcion = "Debe tener al menos 8 caracteres.",
            desplegable = true,
            abiertaInicial = false,
            progreso = fraccion(nueva.length >= 8, confirmar.isNotBlank() && confirmar == nueva)
        ) {
            CampoClave(nueva, "Nueva contraseña") { nueva = it; error = null }
            CampoClave(confirmar, "Confirmar nueva contraseña") { confirmar = it; error = null }
        }
        MensajeAcceso(error ?: mensaje)
    }
}

@Composable
fun GestionEquipoSupabaseScreen(
    procesando: Boolean,
    mensaje: String?,
    codigoGenerado: String?,
    onCrearInvitacion: (String, String) -> Unit,
    equipo: List<com.ruralitos.app.data.remote.MiembroEquipoRemoto> = emptyList(),
    cargandoEquipo: Boolean = false,
    usuarioSupabaseId: String = "",
    onRevocar: (com.ruralitos.app.data.remote.MiembroEquipoRemoto) -> Unit = {},
    onRegresar: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf("MEDICO") }
    var porRevocar by remember { mutableStateOf<com.ruralitos.app.data.remote.MiembroEquipoRemoto?>(null) }
    porRevocar?.let { persona ->
        VentanaConfirmarRuralitos(
            titulo = "Quitar acceso",
            mensaje = "${persona.nombre} dejará de ver las fichas de esta Sala y su teléfono las retirará en la próxima " +
                                "sincronización. Lo que ya registró seguirá en la Sala. Si luego la invitas de nuevo recuperará el acceso.",
            textoConfirmar = "Quitar acceso",
            onConfirmar = { porRevocar = null; onRevocar(persona) },
            textoCancelar = "Cancelar",
            peligro = true,
            onCancelar = { porRevocar = null }
        )
    }
    FormularioBase("Equipo de trabajo", "Invita personal para compartir las fichas de tu organización", onVolver = onRegresar) {
        Text(
            "La persona debe crear su cuenta con este correo y luego ingresar el código.",
            style = MaterialTheme.typography.bodyMedium
        )
        CampoCorreo(correo) { correo = it }
        Text("Rol del usuario", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (rol == "MEDICO") {
                Button(onClick = { rol = "MEDICO" }, modifier = Modifier.weight(1f)) { Text("Médico") }
            } else {
                OutlinedButton(onClick = { rol = "MEDICO" }, modifier = Modifier.weight(1f)) { Text("Médico") }
            }
            if (rol == "ESTADISTICA") {
                Button(onClick = { rol = "ESTADISTICA" }, modifier = Modifier.weight(1f)) { Text("Estadística") }
            } else {
                OutlinedButton(onClick = { rol = "ESTADISTICA" }, modifier = Modifier.weight(1f)) { Text("Estadística") }
            }
        }
        Button(
            onClick = { onCrearInvitacion(correo.trim(), rol) },
            enabled = !procesando && correoValido(correo),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        ) { Text(if (procesando) "Generando…" else "Generar invitación") }
        codigoGenerado?.let { codigo ->
            Text("Código de invitación", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp))
            Text(
                codigo,
                style = MaterialTheme.typography.headlineSmall,
                color = VerdeOscuro,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
            )
            Text("Válido durante 7 días y para un solo uso.", style = MaterialTheme.typography.bodySmall)
        }
        Text("Personas con acceso", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 26.dp))
        if (cargandoEquipo) {
            Text("Cargando…", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        } else if (equipo.isEmpty()) {
            Text(
                "No se pudo cargar la lista. Comprueba la conexión a internet.",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)
            )
        }
        equipo.forEach { persona ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(persona.nombre + if (persona.usuarioId == usuarioSupabaseId) " (tú)" else "", fontWeight = FontWeight.SemiBold)
                    Text(
                        listOf(persona.cargo, persona.correo).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        when {
                            !persona.activo -> "Acceso retirado"
                            persona.rol == "ADMIN" -> "Administrador"
                            persona.permiso == "LECTOR" -> "Solo lectura"
                            persona.alcanceLimitado -> "Acceso a una parte de la Sala"
                            else -> "Puede editar"
                        },
                        style = MaterialTheme.typography.labelSmall, color = VerdeOscuro
                    )
                }
                if (persona.activo && persona.rol != "ADMIN" && persona.usuarioId != usuarioSupabaseId) {
                    TextButton(onClick = { porRevocar = persona }, enabled = !procesando) {
                        Text("Quitar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        MensajeAcceso(mensaje)
    }
}


// ---------------------------------------------------------------------------------------------------------------
// Piezas comunes
// ---------------------------------------------------------------------------------------------------------------

private val FormaCampo = RoundedCornerShape(12.dp)

private fun fraccion(vararg cumplidos: Boolean): Float =
    if (cumplidos.isEmpty()) 0f else cumplidos.count { it }.toFloat() / cumplidos.size

@Composable
private fun FormularioBase(
    titulo: String,
    subtitulo: String,
    onVolver: (() -> Unit)? = null,
    accion: (@Composable ColumnScope.() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    PantallaRuralitos(
        titulo = titulo,
        descripcion = subtitulo,
        onVolver = onVolver,
        barraAccion = accion,
        anchoMaximo = 560.dp,
        contenido = contenido
    )
}

@Composable
private fun IconoAcceso(simbolo: String, color: Color) {
    Box(
        Modifier.size(46.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) { Text(simbolo, color = color, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun CampoCorreo(valor: String, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { onCambio(it.trim().take(254)) },
        label = { Text("Correo electrónico") },
        placeholder = { Text("nombre@correo.com") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        shape = FormaCampo,
        modifier = Modifier.fillMaxWidth().testTag("campo_correo")
    )
}

@Composable
private fun CampoTexto(valor: String, etiqueta: String, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = true,
        shape = FormaCampo,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
    )
}

@Composable
private fun CampoCodigoProfesional(valor: String, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevoValor ->
            val entradaAjustada = if (
                nuevoValor.length < valor.length &&
                (valor.endsWith('.') || valor.endsWith('-')) &&
                nuevoValor == valor.dropLast(1)
            ) nuevoValor.dropLast(1) else nuevoValor
            onCambio(formatearCodigoSenescyt(entradaAjustada))
        },
        label = { Text("Código profesional SENESCYT") },
        supportingText = { Text("Formato automático: 0000.0000-0000000") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = FormaCampo,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
    )
}

internal fun formatearCodigoSenescyt(entrada: String): String {
    val digitos = entrada.filter(Char::isDigit).take(15)
    return buildString {
        append(digitos.take(4))
        if (digitos.length >= 4) append('.')
        if (digitos.length > 4) append(digitos.substring(4, minOf(8, digitos.length)))
        if (digitos.length >= 8) append('-')
        if (digitos.length > 8) append(digitos.substring(8))
    }
}

internal fun codigoSenescytValido(codigo: String): Boolean =
    Regex("""\d{4}\.\d{4}-\d{7}""").matches(codigo)

@Composable
private fun CampoClave(valor: String, etiqueta: String, onCambio: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = { onCambio(it.take(128)) },
        label = { Text(etiqueta) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) {
                Text(if (visible) "Ocultar" else "Ver", color = CianRuralitos, fontWeight = FontWeight.SemiBold)
            }
        },
        singleLine = true,
        shape = FormaCampo,
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BordeCampo),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("campo_clave_${etiqueta.lowercase().replace(' ', '_')}")
    )
}

/**
 * PIN de 6 dígitos en casillas separadas. Un campo de texto invisible recibe el teclado numérico; las casillas solo
 * dibujan lo escrito (un punto por dígito) y resaltan la que sigue.
 */
@Composable
private fun CampoPinCasillas(valor: String, titulo: String, etiquetaPrueba: String, onCambio: (String) -> Unit) {
    val foco = remember { FocusRequester() }
    val teclado = LocalSoftwareKeyboardController.current
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, color = AzulClinicoOscuro, fontWeight = FontWeight.SemiBold)
        BasicTextField(
            value = valor,
            onValueChange = { onCambio(it.filter(Char::isDigit).take(6)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .focusRequester(foco)
                .testTag(etiquetaPrueba)
                .semantics { contentDescription = "$titulo: ${valor.length} de 6 dígitos" },
            decorationBox = { campoInterno ->
                Box {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { foco.requestFocus(); teclado?.show() },
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        repeat(6) { indice ->
                            val siguiente = indice == valor.length
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .border(
                                        if (siguiente) 2.dp else 1.dp,
                                        if (siguiente) CianRuralitos else BordeCampo,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (indice < valor.length) {
                                    Text("•", color = AzulClinicoOscuro, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Box(Modifier.size(1.dp).alpha(0f)) { campoInterno() }
                }
            }
        )
        Text(
            "${valor.length}/6 dígitos",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun MensajeAcceso(mensaje: String?) {
    mensaje?.let {
        val exito = it.contains("enviado", ignoreCase = true) ||
            it.contains("correct", ignoreCase = true) ||
            it.contains("guard", ignoreCase = true)
        AvisoAccesoRuralitos(
            texto = it,
            color = if (exito) CianRuralitos else MaterialTheme.colorScheme.error,
            simbolo = if (exito) "✓" else "!"
        )
    }
}

private fun correoValido(correo: String): Boolean =
    correo.contains('@') && correo.substringAfter('@').contains('.')
