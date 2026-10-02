package com.ruralitos.app.ui.screens

import com.ruralitos.app.ui.theme.FondoClinico
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ruralitos.app.ui.components.formularioSeguro
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ruralitos.app.VerdeOscuro
import com.ruralitos.app.domain.ValidadorIdentidadEcuador
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.ruralitos.app.ui.components.LogoRuralitos

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .formularioSeguro()
            .background(FondoClinico)
    ) {
        FondoAbstractoLogin(
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 26.dp,
                    bottom = 28.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CabeceraLoginRuralitos()

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                shape = RoundedCornerShape(34.dp),
                color = Color.White.copy(alpha = 0.97f),
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.90f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 26.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Iniciar sesión",
                        color = AzulLoginNuevoOscuro,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Ingresa con tu correo y contraseña",
                        color = TextoLoginNuevoSecundario,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(
                            top = 5.dp,
                            bottom = 20.dp
                        )
                    )

                    CampoCorreoLogin(
                        valor = correo,
                        onCambio = {
                            correo = it
                                .trim()
                                .take(254)
                        }
                    )

                    CampoClaveLogin(
                        valor = clave,
                        onCambio = {
                            clave = it.take(128)
                        }
                    )

                    MensajeAccesoLogin(
                        mensaje = mensaje
                    )

                    BotonPrincipalLogin(
                        texto = if (procesando) {
                            "Iniciando…"
                        } else {
                            "Iniciar sesión"
                        },
                        enabled = !procesando &&
                            correoValido(correo) &&
                            clave.isNotBlank(),
                        onClick = {
                            onIngresar(
                                correo.trim(),
                                clave
                            )
                        }
                    )

                    SeparadorOpcionesLogin(
                        modifier = Modifier.padding(top = 22.dp)
                    )

                    OpcionLoginRuralitos(
                        titulo = "Crear una cuenta",
                        descripcion = "Registra tus datos profesionales",
                        simbolo = "+",
                        color = AzulLoginNuevo,
                        onClick = onCrearCuenta,
                        enabled = !procesando
                    )

                    OpcionLoginRuralitos(
                        titulo = "Recuperar contraseña",
                        descripcion = "Recibe un enlace seguro en tu correo",
                        simbolo = "↻",
                        color = Color(0xFFF58A18),
                        onClick = onRecuperar,
                        enabled = !procesando,
                        modifier = Modifier.padding(top = 11.dp)
                    )

                    OpcionLoginRuralitos(
                        titulo = "Entrar sin internet con PIN",
                        descripcion = "Usa el acceso protegido de este teléfono",
                        simbolo = "#",
                        color = Color(0xFF426FE5),
                        onClick = onSinInternet,
                        enabled = !procesando,
                        modifier = Modifier.padding(top = 11.dp)
                    )

                    AvisoLocalLogin(
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }
    }
}

private val AzulLoginNuevo = Color(0xFF1565C0)
private val AzulLoginNuevoOscuro = Color(0xFF0A2A5E)
private val VerdeLoginNuevo = Color(0xFF0889A0)
private val TextoLoginNuevoSecundario = Color(0xFF5B7083)
private val BordeLoginNuevo = Color(0xFFE2ECF1)

@Composable
private fun CabeceraLoginRuralitos() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(116.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.84f),
            shadowElevation = 7.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.90f)
            )
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                LogoRuralitos(
                    modifier = Modifier.size(94.dp)
                )
            }
        }

        Text(
            text = "Ruralitos",
            color = AzulLoginNuevoOscuro,
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )

        Text(
            text = "Fichas familiares, incluso sin conexión",
            color = TextoLoginNuevoSecundario,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun CampoCorreoLogin(
    valor: String,
    onCambio: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = {
            Text("Correo electrónico")
        },
        placeholder = {
            Text("nombre@correo.com")
        },
        leadingIcon = {
            IconoCampoLogin(
                simbolo = "✉",
                color = AzulLoginNuevo
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email
        ),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AzulLoginNuevo,
            unfocusedBorderColor = BordeLoginNuevo,
            focusedLabelColor = AzulLoginNuevo,
            cursorColor = AzulLoginNuevo,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CampoClaveLogin(
    valor: String,
    onCambio: (String) -> Unit
) {
    var visible by remember {
        mutableStateOf(false)
    }

    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = {
            Text("Contraseña")
        },
        leadingIcon = {
            IconoCampoLogin(
                simbolo = "▢",
                color = AzulLoginNuevo
            )
        },
        visualTransformation = if (visible) {
            androidx.compose.ui.text.input.VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            TextButton(
                onClick = {
                    visible = !visible
                }
            ) {
                Text(
                    text = if (visible) {
                        "Ocultar"
                    } else {
                        "Ver"
                    },
                    color = VerdeLoginNuevo,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AzulLoginNuevo,
            unfocusedBorderColor = BordeLoginNuevo,
            focusedLabelColor = AzulLoginNuevo,
            cursorColor = AzulLoginNuevo,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 13.dp)
    )
}

@Composable
private fun IconoCampoLogin(
    simbolo: String,
    color: Color
) {
    Surface(
        modifier = Modifier.size(34.dp),
        shape = RoundedCornerShape(11.dp),
        color = color.copy(alpha = 0.10f)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = simbolo,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MensajeAccesoLogin(
    mensaje: String?
) {
    mensaje?.let {
        val exito =
            it.contains(
                "enviado",
                ignoreCase = true
            ) ||
            it.contains(
                "correct",
                ignoreCase = true
            ) ||
            it.contains(
                "guard",
                ignoreCase = true
            )

        val color = if (exito) {
            VerdeLoginNuevo
        } else {
            Color(0xFFEF4357)
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            color = color.copy(alpha = 0.09f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                color.copy(alpha = 0.30f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 13.dp,
                        vertical = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = color
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (exito) {
                                "✓"
                            } else {
                                "!"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = it,
                    color = if (exito) {
                        AzulLoginNuevoOscuro
                    } else {
                        Color(0xFF9E2231)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun BotonPrincipalLogin(
    texto: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .height(60.dp),
        color = if (enabled) {
            VerdeLoginNuevo
        } else {
            VerdeLoginNuevo.copy(alpha = 0.48f)
        },
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (enabled) {
            6.dp
        } else {
            0.dp
        }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = texto,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "  →",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SeparadorOpcionesLogin(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    AzulLoginNuevo.copy(alpha = 0.22f)
                )
        )

        Text(
            text = "Otras opciones",
            color = AzulLoginNuevoOscuro,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    AzulLoginNuevo.copy(alpha = 0.22f)
                )
        )
    }
}

@Composable
private fun OpcionLoginRuralitos(
    titulo: String,
    descripcion: String,
    simbolo: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            BordeLoginNuevo
        ),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.11f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = simbolo,
                        color = color,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 13.dp)
            ) {
                Text(
                    text = titulo,
                    color = AzulLoginNuevoOscuro,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = descripcion,
                    color = TextoLoginNuevoSecundario,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Text(
                text = "›",
                color = color,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AvisoLocalLogin(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFE3F4F7),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            VerdeLoginNuevo.copy(alpha = 0.28f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = VerdeLoginNuevo
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text =
                    "Las fichas se guardan primero en este teléfono y se sincronizan automáticamente cuando vuelve internet.",
                color = AzulLoginNuevoOscuro,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 11.dp)
            )
        }
    }
}

@Composable
private fun FondoAbstractoLogin(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        drawCircle(
            color = Color(0xFF79CBFF).copy(alpha = 0.25f),
            radius = size.width * 0.50f,
            center = Offset(
                size.width * 0.04f,
                size.height * 0.02f
            )
        )

        val derecha = Path().apply {
            moveTo(
                size.width,
                size.height * 0.05f
            )

            cubicTo(
                size.width * 0.77f,
                size.height * 0.12f,
                size.width * 0.91f,
                size.height * 0.24f,
                size.width * 0.72f,
                size.height * 0.32f
            )

            cubicTo(
                size.width * 0.91f,
                size.height * 0.35f,
                size.width * 0.88f,
                size.height * 0.46f,
                size.width,
                size.height * 0.52f
            )

            close()
        }

        drawPath(
            path = derecha,
            color = Color(0xFF46D1A5).copy(alpha = 0.27f)
        )

        val izquierda = Path().apply {
            moveTo(
                0f,
                size.height * 0.48f
            )

            cubicTo(
                size.width * 0.16f,
                size.height * 0.55f,
                size.width * 0.11f,
                size.height * 0.68f,
                0f,
                size.height * 0.74f
            )

            close()
        }

        drawPath(
            path = izquierda,
            color = Color(0xFF1565C0).copy(alpha = 0.22f)
        )

        val inferior = Path().apply {
            moveTo(
                0f,
                size.height * 0.88f
            )

            cubicTo(
                size.width * 0.18f,
                size.height * 0.82f,
                size.width * 0.36f,
                size.height * 0.98f,
                size.width * 0.56f,
                size.height * 0.92f
            )

            cubicTo(
                size.width * 0.74f,
                size.height * 0.87f,
                size.width * 0.86f,
                size.height * 0.81f,
                size.width,
                size.height * 0.86f
            )

            lineTo(
                size.width,
                size.height
            )

            lineTo(
                0f,
                size.height
            )

            close()
        }

        drawPath(
            path = inferior,
            color = Color(0xFF62D8D1).copy(alpha = 0.25f)
        )
    }
}
@Composable
fun RegistroSupabaseScreen(
    procesando: Boolean,
    mensaje: String?,
    onRegistrar: (String, String, String, String, String, String, String) -> Unit,
    onVolver: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var nombres by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var codigoSenescyt by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    FormularioBase("Crear cuenta", "Completa tus datos para empezar a crear fichas familiares", estiloLogin = true) {
        TituloSeccionAcceso(
            numero = "1",
            titulo = "Datos de acceso",
            descripcion = "El correo será tu usuario para ingresar",
            color = ColorAccesoAzul
        )
        CampoCorreo(correo) { correo = it; error = null }

        TituloSeccionAcceso(
            numero = "2",
            titulo = "Información personal y profesional",
            descripcion = "Estos datos identificarán al responsable de las fichas",
            color = ColorAccesoVerde
        )
        OutlinedTextField(
            value = cedula,
            onValueChange = { cedula = it.filter(Char::isDigit).take(13); error = null },
            label = { Text("Cédula") },
            supportingText = { Text("Será también tu número de historia clínica") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
        CampoTexto(nombres, "Apellidos y nombres") { nombres = it.take(160); error = null }
        CampoCargoPredeterminado(
            valor = cargo,
            onCambio = { cargo = it; error = null }
        )
        CampoCodigoProfesional(codigoSenescyt) { codigoSenescyt = it; error = null }
        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it.filter { c -> c.isDigit() || c == '+' }.take(20) },
            label = { Text("Teléfono (opcional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )

        TituloSeccionAcceso(
            numero = "3",
            titulo = "Seguridad",
            descripcion = "Usa al menos 8 caracteres y confirma la contraseña",
            color = ColorAccesoMorado
        )
        CampoClave(clave, "Contraseña") { clave = it; error = null }
        CampoClave(confirmar, "Confirmar contraseña") { confirmar = it; error = null }
        MensajeAcceso(error ?: mensaje)

        BotonAccesoPrincipal(
            texto = if (procesando) "Creando cuenta…" else "Crear mi cuenta",
            descripcion = if (procesando) null else "Guardar datos y continuar",
            onClick = {
                error = when {
                    !correoValido(correo) -> "Ingresa un correo válido."
                    !ValidadorIdentidadEcuador.esIdentificacionAceptable(cedula) -> "Ingresa una cédula o RUC válido."
                    nombres.isBlank() -> "Ingresa tus apellidos y nombres."
                    cargo.isBlank() -> "Ingresa tu cargo."
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
                        nombres.trim(),
                        cargo.trim(),
                        telefono.trim(),
                        codigoSenescyt.trim()
                    )
                }
            },
            enabled = !procesando,
            color = ColorAccesoAzul
        )
        BotonVolver(onVolver, procesando)
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
        estiloLogin = true
    ) {
        AvisoAccesoRuralitos(
            texto = "Abre el enlace desde este mismo teléfono para volver a Ruralitos y crear una contraseña nueva.",
            color = ColorAccesoNaranja,
            simbolo = "1"
        )
        CampoCorreo(correo) { correo = it }
        MensajeAcceso(mensaje)
        BotonAccesoPrincipal(
            texto = if (procesando) "Enviando enlace…" else "Enviar enlace de recuperación",
            onClick = { onEnviar(correo.trim()) },
            enabled = !procesando && correoValido(correo),
            color = ColorAccesoNaranja
        )
        BotonVolver(onVolver, procesando)
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
        "Vincula las fichas con tu institución"
    ) {
        AvisoAccesoRuralitos(
            texto = "Ingresa el código entregado por la administración. Así las fichas podrán revisarse posteriormente desde la plataforma web.",
            color = ColorAccesoAzul,
            simbolo = "i"
        )
        OutlinedTextField(
            value = invitacion,
            onValueChange = { invitacion = it.uppercase().filter(Char::isLetterOrDigit).take(32) },
            label = { Text("Código de invitación") },
            supportingText = { Text("Mínimo 8 caracteres") },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        )
        MensajeAcceso(mensaje)
        BotonAccesoPrincipal(
            texto = if (procesando) "Comprobando…" else "Continuar con el código",
            onClick = { onAceptarCodigo(invitacion.trim()) },
            enabled = !procesando && invitacion.length >= 8,
            color = ColorAccesoAzul
        )
        BotonAccesoSecundario(
            texto = "Crear mi propia Sala",
            onClick = onCrearSala,
            enabled = !procesando
        )
        AvisoAccesoRuralitos(
            texto = "Si todavía no tienes un código de asignación, elige tu centro de salud y crea tu propia Sala. Luego podrás agregar EAIS y barrios.",
            color = ColorAccesoMorado,
            simbolo = "+"
        )
        BotonAccesoSecundario(
            texto = "Cerrar sesión",
            onClick = onCerrarSesion,
            enabled = !procesando
        )
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
        "Crea un PIN de 6 dígitos para este teléfono"
    ) {
        AvisoAccesoRuralitos(
            texto = "Este PIN permite abrir Ruralitos cuando no hay conexión. No sustituye la contraseña de tu cuenta.",
            color = ColorAccesoMorado,
            simbolo = "#"
        )
        CampoPin(pin, "PIN de 6 dígitos") { pin = it; error = null }
        CampoPin(confirmar, "Confirmar PIN") { confirmar = it; error = null }
        MensajeAcceso(error ?: mensaje)
        BotonAccesoPrincipal(
            texto = if (procesando) "Protegiendo acceso…" else "Guardar PIN seguro",
            onClick = {
                error = when {
                    pin.length != 6 -> "El PIN debe tener exactamente 6 dígitos."
                    pin != confirmar -> "Los PIN no coinciden."
                    pin.toSet().size == 1 -> "Elige un PIN menos predecible."
                    else -> null
                }
                if (error == null) onGuardar(pin)
            },
            enabled = !procesando,
            color = ColorAccesoMorado
        )
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

    FormularioBase(
        "Entrar sin internet",
        "Usa la cédula y el PIN guardados en este teléfono",
        estiloLogin = true
    ) {
        AvisoAccesoRuralitos(
            texto = "Podrás consultar y crear fichas locales. La sincronización se reanudará cuando vuelva la conexión.",
            color = ColorAccesoMorado,
            simbolo = "✓"
        )
        OutlinedTextField(
            value = cedula,
            onValueChange = { cedula = it.filter(Char::isDigit).take(13) },
            label = { Text("Cédula") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        )
        CampoPin(pin, "PIN de 6 dígitos") { pin = it }
        MensajeAcceso(mensaje)
        BotonAccesoPrincipal(
            texto = if (procesando) "Comprobando…" else "Entrar sin conexión",
            onClick = { onEntrar(cedula, pin) },
            enabled = !procesando && cedula.length >= 10 && pin.length == 6,
            color = ColorAccesoMorado
        )
        BotonVolver(onVolver, procesando)
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
        "Elige una clave segura para recuperar tu cuenta"
    ) {
        AvisoAccesoRuralitos(
            texto = "La contraseña debe tener al menos 8 caracteres. Evita usar tu cédula o datos fáciles de adivinar.",
            color = ColorAccesoMorado,
            simbolo = "✓"
        )
        CampoClave(clave, "Nueva contraseña") { clave = it; error = null }
        CampoClave(confirmar, "Confirmar nueva contraseña") { confirmar = it; error = null }
        MensajeAcceso(error ?: mensaje)
        BotonAccesoPrincipal(
            texto = if (procesando) "Actualizando…" else "Guardar nueva contraseña",
            onClick = {
                error = when {
                    clave.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
                    clave != confirmar -> "Las contraseñas no coinciden."
                    else -> null
                }
                if (error == null) onGuardar(clave)
            },
            enabled = !procesando,
            color = ColorAccesoMorado
        )
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
        "Confirma tu contraseña actual antes de crear una nueva"
    ) {
        TituloSeccionAcceso(
            numero = "1",
            titulo = "Verifica tu identidad",
            descripcion = "Escribe la contraseña que usas actualmente",
            color = ColorAccesoNaranja
        )
        CampoClave(actual, "Contraseña actual") { actual = it; error = null }
        TituloSeccionAcceso(
            numero = "2",
            titulo = "Crea la contraseña nueva",
            descripcion = "Debe tener al menos 8 caracteres",
            color = ColorAccesoMorado
        )
        CampoClave(nueva, "Nueva contraseña") { nueva = it; error = null }
        CampoClave(confirmar, "Confirmar nueva contraseña") { confirmar = it; error = null }
        MensajeAcceso(error ?: mensaje)
        BotonAccesoPrincipal(
            texto = if (procesando) "Actualizando…" else "Cambiar contraseña",
            onClick = {
                error = when {
                    actual.isBlank() -> "Ingresa la contraseña actual."
                    nueva.length < 8 -> "La nueva contraseña debe tener al menos 8 caracteres."
                    nueva != confirmar -> "Las contraseñas nuevas no coinciden."
                    nueva == actual -> "La nueva contraseña debe ser diferente."
                    else -> null
                }
                if (error == null) onGuardar(actual, nueva)
            },
            enabled = !procesando,
            color = ColorAccesoMorado
        )
        BotonVolver(onVolver, procesando)
    }
}

@Composable
fun GestionEquipoSupabaseScreen(
    procesando: Boolean,
    mensaje: String?,
    codigoGenerado: String?,
    onCrearInvitacion: (String, String) -> Unit,
    onRegresar: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf("MEDICO") }
    FormularioBase("Equipo de trabajo", "Invita personal para compartir las fichas de tu organización") {
        Text(
            "La persona debe crear su cuenta con este correo y luego ingresar el código.",
            style = MaterialTheme.typography.bodyMedium
        )
        CampoCorreo(correo) { correo = it }
        Text("Rol del usuario", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
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
            Text("Código de invitación", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp))
            Text(
                codigo,
                style = MaterialTheme.typography.headlineSmall,
                color = VerdeOscuro,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
            )
            Text("Válido durante 7 días y para un solo uso.", style = MaterialTheme.typography.bodySmall)
        }
        MensajeAcceso(mensaje)
        BotonVolver(onRegresar, procesando)
    }
}

@Composable
private fun FormularioBase(
    titulo: String,
    subtitulo: String,
    estiloLogin: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit
) {
    if (estiloLogin) {
        FormularioConDisenoLogin(titulo, subtitulo, contenido)
    } else {
        MarcoAccesoRuralitos(
            titulo = titulo,
            subtitulo = subtitulo,
            contenido = contenido
        )
    }
}

@Composable
private fun FormularioConDisenoLogin(
    titulo: String,
    subtitulo: String,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .formularioSeguro()
            .background(FondoClinico)
    ) {
        FondoAbstractoLogin(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CabeceraLoginRuralitos()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                shape = RoundedCornerShape(34.dp),
                color = Color.White.copy(alpha = 0.97f),
                shadowElevation = 10.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.90f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = titulo,
                        color = AzulLoginNuevoOscuro,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = subtitulo,
                        color = TextoLoginNuevoSecundario,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 5.dp, bottom = 20.dp)
                    )
                    contenido()
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
    )
}

@Composable
private fun CampoTexto(valor: String, etiqueta: String, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = true,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
       
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
        visualTransformation = if (visible) {
            androidx.compose.ui.text.input.VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) {
                Text(if (visible) "Ocultar" else "Ver")
            }
        },
        singleLine = true,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
    )
}

@Composable
private fun CampoPin(valor: String, etiqueta: String, onCambio: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = { onCambio(it.filter(Char::isDigit).take(6)) },
        label = { Text(etiqueta) },
        supportingText = { Text("${valor.length}/6 dígitos") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = if (visible) {
            androidx.compose.ui.text.input.VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) {
                Text(if (visible) "Ocultar" else "Ver")
            }
        },
        singleLine = true,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
    )
}

@Composable
private fun BotonVolver(onVolver: () -> Unit, procesando: Boolean) {
    BotonAccesoSecundario(
        texto = "Regresar",
        onClick = onVolver,
        enabled = !procesando
    )
}

@Composable
private fun MensajeAcceso(mensaje: String?) {
    mensaje?.let {
        AvisoAccesoRuralitos(
            texto = it,
            color = if (
                it.contains("enviado", ignoreCase = true) ||
                it.contains("correct", ignoreCase = true) ||
                it.contains("guard", ignoreCase = true)
            ) ColorAccesoVerde else MaterialTheme.colorScheme.error,
            simbolo = if (
                it.contains("enviado", ignoreCase = true) ||
                it.contains("correct", ignoreCase = true) ||
                it.contains("guard", ignoreCase = true)
            ) "✓" else "!",
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

private fun correoValido(correo: String): Boolean =
    correo.contains('@') && correo.substringAfter('@').contains('.')
