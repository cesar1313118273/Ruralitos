package com.ruralitos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ruralitos.app.ui.components.formularioSeguro
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ruralitos.app.VerdeOscuro
import com.ruralitos.app.data.local.dao.UsuarioDao
import com.ruralitos.app.data.local.entity.UsuarioEntity
import com.ruralitos.app.domain.SeguridadClave
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Usa este contenedor en el lugar donde actualmente decides si mostrar
 * CargandoAccesoScreen() o la siguiente pantalla.
 *
 * La pantalla de carga permanecerá visible COMO MÍNIMO durante
 * [duracionMinimaMs], aunque la carga real termine antes.
 *
 * Ejemplo:
 *
 * AccesoConCargaMinima(cargandoReal = cargando) {
 *     PantallaPrincipal()
 * }
 */
@Composable
fun AccesoConCargaMinima(
    cargandoReal: Boolean,
    duracionMinimaMs: Long = 4500L,
    contenido: @Composable () -> Unit
) {
    var tiempoMinimoCumplido by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(duracionMinimaMs) {
        delay(duracionMinimaMs)
        tiempoMinimoCumplido = true
    }

    if (cargandoReal || !tiempoMinimoCumplido) {
        CargandoAccesoScreen()
    } else {
        contenido()
    }
}

@Composable
fun CargandoAccesoScreen() {
    var mensajeMascota by remember {
        mutableStateOf("¡Hola!")
    }

    LaunchedEffect(Unit) {
        delay(1500)
        mensajeMascota = "Que tengas un bonito día"
    }

    val animacion = rememberInfiniteTransition(
        label = "mascota_carga"
    )

    val anguloSaludo by animacion.animateFloat(
        initialValue = -16f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 430,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mano_saludando"
    )

    val rebote by animacion.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 850,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rebote_mascota"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .formularioSeguro()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE9F7FF),
                        Color(0xFFF8FCFF),
                        Color(0xFFECFBF6)
                    )
                )
            )
    ) {
        FondoCargaMascota(
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp,
                    vertical = 34.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MascotaMedicoPixel(
                anguloSaludo = anguloSaludo,
                modifier = Modifier
                    .size(
                        width = 244.dp,
                        height = 304.dp
                    )
                    .graphicsLayer {
                        translationY = rebote
                    }
            )

            Crossfade(
                targetState = mensajeMascota,
                animationSpec = tween(
                    durationMillis = 300
                ),
                label = "mensaje_mascota"
            ) { mensaje ->
                BurbujaMensajeMascota(
                    mensaje = mensaje,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun BurbujaMensajeMascota(
    mensaje: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .widthIn(
                min = 150.dp,
                max = 290.dp
            ),
        color = Color.White.copy(alpha = 0.97f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            1.5.dp,
            Color(0xFF7DC8F4)
        ),
        shadowElevation = 5.dp
    ) {
        Text(
            text = mensaje,
            color = Color(0xFF08285D),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 13.dp
            )
        )
    }
}

@Composable
private fun MascotaMedicoPixel(
    anguloSaludo: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        val sx = size.width / 100f
        val sy = size.height / 125f

        fun rect(
            x: Float,
            y: Float,
            w: Float,
            h: Float,
            color: Color
        ) {
            drawRect(
                color = color,
                topLeft = Offset(
                    x * sx,
                    y * sy
                ),
                size = androidx.compose.ui.geometry.Size(
                    w * sx,
                    h * sy
                )
            )
        }

        val contorno = Color(0xFF18243A)
        val cabello = Color(0xFF4C302C)
        val cabelloLuz = Color(0xFF6E4437)
        val piel = Color(0xFFFFC18C)
        val pielSombra = Color(0xFFF29A68)
        val blanco = Color(0xFFF7FBFF)
        val blancoSombra = Color(0xFFD9EAF7)
        val verde = Color(0xFF08A98C)
        val verdeOscuro = Color(0xFF087E70)
        val azul = Color(0xFF0A73C9)
        val zapato = Color(0xFF30323D)
        val marron = Color(0xFF8B552E)
        val papel = Color(0xFFEAF6FF)

        // Sombra en el piso.
        drawOval(
            color = Color(0xFF7EA7C7).copy(alpha = 0.24f),
            topLeft = Offset(
                27f * sx,
                116f * sy
            ),
            size = androidx.compose.ui.geometry.Size(
                49f * sx,
                5f * sy
            )
        )

        // Piernas y zapatos.
        rect(35f, 88f, 14f, 26f, contorno)
        rect(53f, 88f, 14f, 26f, contorno)
        rect(37f, 89f, 10f, 23f, verde)
        rect(55f, 89f, 10f, 23f, verde)
        rect(31f, 111f, 19f, 7f, contorno)
        rect(52f, 111f, 20f, 7f, contorno)
        rect(34f, 112f, 15f, 4f, zapato)
        rect(55f, 112f, 15f, 4f, zapato)

        // Torso de bata.
        rect(27f, 53f, 48f, 40f, contorno)
        rect(30f, 55f, 42f, 36f, blanco)
        rect(30f, 82f, 13f, 9f, blancoSombra)
        rect(59f, 82f, 13f, 9f, blancoSombra)

        // Uniforme verde debajo de la bata.
        rect(44f, 57f, 15f, 31f, verde)
        rect(46f, 57f, 11f, 5f, verdeOscuro)

        // Brazo izquierdo con portapapeles.
        rect(20f, 59f, 13f, 31f, contorno)
        rect(23f, 61f, 8f, 27f, blanco)
        rect(20f, 83f, 12f, 10f, piel)
        rect(16f, 76f, 17f, 23f, contorno)
        rect(18f, 78f, 13f, 19f, marron)
        rect(20f, 80f, 9f, 13f, papel)
        rect(21f, 83f, 7f, 2f, Color(0xFF86B9DA))
        rect(21f, 87f, 7f, 2f, Color(0xFF86B9DA))
        rect(21f, 91f, 5f, 2f, Color(0xFF86B9DA))

        // Cuello.
        rect(45f, 43f, 13f, 13f, contorno)
        rect(47f, 44f, 9f, 11f, piel)

        // Cabeza/contorno.
        rect(27f, 15f, 48f, 33f, contorno)
        rect(24f, 22f, 5f, 17f, contorno)
        rect(74f, 22f, 5f, 17f, contorno)
        rect(30f, 18f, 42f, 28f, piel)
        rect(26f, 25f, 5f, 12f, piel)
        rect(72f, 25f, 5f, 12f, piel)

        // Cabello pixelado.
        rect(29f, 10f, 43f, 11f, contorno)
        rect(24f, 15f, 14f, 13f, contorno)
        rect(63f, 14f, 15f, 14f, contorno)
        rect(31f, 8f, 34f, 10f, cabello)
        rect(26f, 14f, 16f, 11f, cabello)
        rect(39f, 12f, 24f, 9f, cabello)
        rect(60f, 13f, 15f, 11f, cabello)
        rect(34f, 11f, 18f, 4f, cabelloLuz)
        rect(55f, 13f, 9f, 4f, cabelloLuz)

        // Ojos grandes.
        rect(36f, 28f, 9f, 11f, contorno)
        rect(58f, 28f, 9f, 11f, contorno)
        rect(38f, 29f, 5f, 8f, Color(0xFF4E2B28))
        rect(60f, 29f, 5f, 8f, Color(0xFF4E2B28))
        rect(39f, 29f, 2f, 3f, Color.White)
        rect(61f, 29f, 2f, 3f, Color.White)

        // Mejillas y boca.
        rect(31f, 39f, 7f, 3f, Color(0xFFF18D85))
        rect(66f, 39f, 7f, 3f, Color(0xFFF18D85))
        rect(47f, 39f, 11f, 6f, contorno)
        rect(49f, 40f, 7f, 3f, Color(0xFFF26066))

        // Bolsillos de bata.
        rect(33f, 76f, 10f, 10f, blancoSombra)
        rect(61f, 76f, 8f, 10f, blancoSombra)

        // Estetoscopio.
        drawLine(
            color = azul,
            start = Offset(41f * sx, 56f * sy),
            end = Offset(44f * sx, 70f * sy),
            strokeWidth = 2.3f * sx
        )
        drawLine(
            color = azul,
            start = Offset(62f * sx, 56f * sy),
            end = Offset(59f * sx, 70f * sy),
            strokeWidth = 2.3f * sx
        )
        drawCircle(
            color = contorno,
            radius = 4.0f * sx,
            center = Offset(
                58.5f * sx,
                72f * sy
            )
        )
        drawCircle(
            color = Color(0xFFB7CBD7),
            radius = 2.2f * sx,
            center = Offset(
                58.5f * sx,
                72f * sy
            )
        )

        // Brazo derecho animado: rota desde el hombro para saludar.
        rotate(
            degrees = anguloSaludo,
            pivot = Offset(
                71f * sx,
                62f * sy
            )
        ) {
            // Contorno de manga y antebrazo.
            rect(68f, 52f, 12f, 24f, contorno)
            rect(70f, 54f, 8f, 20f, blanco)

            // Mano.
            rect(69f, 44f, 14f, 12f, contorno)
            rect(71f, 46f, 10f, 9f, piel)

            // Dedos levantados, estilo pixel.
            rect(72f, 40f, 3f, 8f, contorno)
            rect(76f, 39f, 3f, 9f, contorno)
            rect(80f, 41f, 3f, 8f, contorno)

            rect(73f, 41f, 2f, 7f, piel)
            rect(77f, 40f, 2f, 8f, piel)
            rect(81f, 42f, 2f, 7f, piel)

            // Sombra de la palma.
            rect(71f, 52f, 9f, 3f, pielSombra)
        }
    }
}

@Composable
private fun FondoCargaMascota(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        drawCircle(
            color = Color(0xFF82D3FF).copy(alpha = 0.20f),
            radius = size.width * 0.48f,
            center = Offset(
                size.width * 0.05f,
                size.height * 0.08f
            )
        )

        drawCircle(
            color = Color(0xFF70E1BE).copy(alpha = 0.18f),
            radius = size.width * 0.42f,
            center = Offset(
                size.width * 0.96f,
                size.height * 0.89f
            )
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.50f),
            radius = size.width * 0.28f,
            center = Offset(
                size.width * 0.82f,
                size.height * 0.18f
            )
        )
    }
}
@Composable
fun CrearAdministradorScreen(
    procesando: Boolean,
    mensajeError: String?,
    onCrear: (cedula: String, nombres: String, cargo: String, clave: String) -> Unit
) {
    FormularioAcceso(
        titulo = "Configurar Ruralitos",
        explicacion = "Crea el primer administrador. Esta cuenta podrá registrar al resto del personal.",
        mostrarDatosPersonales = true,
        textoBoton = "Crear administrador",
        procesando = procesando,
        mensajeError = mensajeError,
        onEnviar = onCrear
    )
}

@Composable
fun LoginScreen(
    procesando: Boolean,
    mensajeError: String?,
    onIngresar: (cedula: String, clave: String) -> Unit
) {
    var cedula by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Ruralitos", style = MaterialTheme.typography.headlineLarge, color = VerdeOscuro, fontWeight = FontWeight.Bold)
        Text("Ingreso del personal de salud", modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))
        OutlinedTextField(
            value = cedula,
            onValueChange = { cedula = it.filter(Char::isDigit).take(13) },
            label = { Text("Cédula") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
        MensajeError(mensajeError)
        Button(
            onClick = { onIngresar(cedula.trim(), clave) },
            enabled = !procesando && cedula.isNotBlank() && clave.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
        ) {
            Text(if (procesando) "Comprobando…" else "Ingresar")
        }
        Text(
            "La sesión y las fichas se guardan únicamente en este dispositivo y funcionan sin internet.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 18.dp)
        )
    }
}

@Composable
private fun FormularioAcceso(
    titulo: String,
    explicacion: String,
    mostrarDatosPersonales: Boolean,
    textoBoton: String,
    procesando: Boolean,
    mensajeError: String?,
    onEnviar: (String, String, String, String) -> Unit
) {
    var cedula by remember { mutableStateOf("") }
    var nombres by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var errorLocal by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(titulo, style = MaterialTheme.typography.headlineMedium, color = VerdeOscuro, fontWeight = FontWeight.Bold)
        Text(explicacion, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
        OutlinedTextField(
            value = cedula,
            onValueChange = { cedula = it.filter(Char::isDigit).take(13) },
            label = { Text("Cédula") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (mostrarDatosPersonales) {
            OutlinedTextField(
                value = nombres,
                onValueChange = { nombres = it },
                label = { Text("Apellidos y nombres") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
            OutlinedTextField(
                value = cargo,
                onValueChange = { cargo = it },
                label = { Text("Cargo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
        }
        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it },
            label = { Text("Contraseña (mínimo 8 caracteres)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
        OutlinedTextField(
            value = confirmar,
            onValueChange = { confirmar = it },
            label = { Text("Confirmar contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
        MensajeError(errorLocal ?: mensajeError)
        Button(
            onClick = {
                errorLocal = when {
                    cedula.length < 10 -> "Ingresa una cédula válida."
                    nombres.isBlank() -> "Ingresa los apellidos y nombres."
                    cargo.isBlank() -> "Ingresa el cargo."
                    clave.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
                    clave != confirmar -> "Las contraseñas no coinciden."
                    else -> null
                }
                if (errorLocal == null) onEnviar(cedula.trim(), nombres.trim(), cargo.trim(), clave)
            },
            enabled = !procesando,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
        ) {
            Text(if (procesando) "Guardando…" else textoBoton)
        }
    }
}

@Composable
fun GestionUsuariosScreen(
    usuarioActualId: Long,
    usuarioDao: UsuarioDao,
    onRegresar: () -> Unit
) {
    val usuarios by usuarioDao.observarTodos().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var mostrarFormulario by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var procesando by remember { mutableStateOf(false) }
    var usuarioRestableciendo by remember { mutableStateOf<UsuarioEntity?>(null) }

    usuarioRestableciendo?.let { usuario ->
        FormularioRestablecerClave(
            titulo = "Restablecer contraseña",
            descripcion = "Asigna una nueva contraseña a ${usuario.nombres}.",
            procesando = procesando,
            mensajeError = mensaje,
            onGuardar = { nuevaClave ->
                procesando = true
                scope.launch {
                    runCatching {
                        val protegida = withContext(Dispatchers.Default) { SeguridadClave.proteger(nuevaClave) }
                        withContext(Dispatchers.IO) {
                            usuarioDao.actualizarClave(usuario.id, protegida.hash, protegida.salt)
                        }
                    }.onSuccess {
                        usuarioRestableciendo = null
                        mensaje = "Contraseña restablecida correctamente."
                    }.onFailure {
                        mensaje = "No se pudo restablecer la contraseña."
                    }
                    procesando = false
                }
            },
            onCancelar = { usuarioRestableciendo = null; mensaje = null }
        )
        return
    }

    if (mostrarFormulario) {
        FormularioAcceso(
            titulo = "Nuevo usuario",
            explicacion = "Registra una cuenta para otro integrante del personal de salud.",
            mostrarDatosPersonales = true,
            textoBoton = "Guardar usuario",
            procesando = procesando,
            mensajeError = mensaje,
            onEnviar = { cedula, nombres, cargo, clave ->
                procesando = true
                mensaje = null
                scope.launch {
                    val resultado = runCatching {
                        withContext(Dispatchers.Default) { SeguridadClave.proteger(clave) }
                    }.mapCatching { protegida ->
                        withContext(Dispatchers.IO) {
                            usuarioDao.guardar(
                                UsuarioEntity(
                                    cedula = cedula,
                                    nombres = nombres,
                                    cargo = cargo,
                                    rol = UsuarioEntity.ROL_MEDICO,
                                    claveHash = protegida.hash,
                                    claveSalt = protegida.salt
                                )
                            )
                        }
                    }
                    procesando = false
                    if (resultado.isSuccess) {
                        mostrarFormulario = false
                        mensaje = "Usuario creado correctamente."
                    } else {
                        mensaje = "No se pudo guardar. Verifica que la cédula no esté registrada."
                    }
                }
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Usuarios locales", style = MaterialTheme.typography.headlineMedium, color = VerdeOscuro, fontWeight = FontWeight.Bold)
        Text("Solo el administrador puede crear o desactivar cuentas.", modifier = Modifier.padding(top = 6.dp, bottom = 14.dp))
        mensaje?.let { Text(it, color = VerdeOscuro, modifier = Modifier.padding(bottom = 10.dp)) }
        Button(onClick = { mensaje = null; mostrarFormulario = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
            Text("Agregar personal de salud")
        }
        Spacer(Modifier.height(12.dp))
        usuarios.forEach { usuario ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(usuario.nombres, fontWeight = FontWeight.Bold)
                            Text("${usuario.cargo} · ${if (usuario.esAdministrador) "Administrador" else "Personal de salud"}")
                            Text("Cédula: ${usuario.cedula}", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = usuario.activo,
                            enabled = usuario.id != usuarioActualId,
                            onCheckedChange = { activo ->
                                scope.launch(Dispatchers.IO) { usuarioDao.cambiarEstado(usuario.id, activo) }
                            }
                        )
                    }
                    OutlinedButton(
                        onClick = { mensaje = null; usuarioRestableciendo = usuario },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) { Text("Restablecer contraseña") }
                }
            }
        }
        OutlinedButton(onClick = onRegresar, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(top = 6.dp)) {
            Text("Regresar")
        }
    }
}

@Composable
fun CambiarClaveScreen(
    usuario: UsuarioEntity,
    usuarioDao: UsuarioDao,
    onGuardada: () -> Unit,
    onCancelar: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var actual by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var procesando by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().formularioSeguro().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Cambiar contraseña", style = MaterialTheme.typography.headlineMedium, color = VerdeOscuro, fontWeight = FontWeight.Bold)
        Text(usuario.nombres, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
        OutlinedTextField(
            value = actual,
            onValueChange = { actual = it; error = null },
            label = { Text("Contraseña actual") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = nueva,
            onValueChange = { nueva = it; error = null },
            label = { Text("Nueva contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
        OutlinedTextField(
            value = confirmar,
            onValueChange = { confirmar = it; error = null },
            label = { Text("Confirmar nueva contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
        MensajeError(error)
        Button(
            onClick = {
                error = when {
                    nueva.length < 8 -> "La nueva contraseña debe tener al menos 8 caracteres."
                    nueva != confirmar -> "Las contraseñas nuevas no coinciden."
                    nueva == actual -> "La nueva contraseña debe ser diferente."
                    else -> null
                }
                if (error == null) {
                    procesando = true
                    scope.launch {
                        val actualCorrecta = withContext(Dispatchers.Default) {
                            SeguridadClave.verificar(actual, usuario.claveHash, usuario.claveSalt)
                        }
                        if (!actualCorrecta) {
                            error = "La contraseña actual es incorrecta."
                            procesando = false
                            return@launch
                        }
                        val protegida = withContext(Dispatchers.Default) { SeguridadClave.proteger(nueva) }
                        withContext(Dispatchers.IO) {
                            usuarioDao.actualizarClave(usuario.id, protegida.hash, protegida.salt)
                        }
                        procesando = false
                        onGuardada()
                    }
                }
            },
            enabled = !procesando,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) { Text(if (procesando) "Guardando…" else "Cambiar contraseña") }
        OutlinedButton(onClick = onCancelar, enabled = !procesando, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(top = 8.dp)) {
            Text("Cancelar")
        }
    }
}

@Composable
private fun FormularioRestablecerClave(
    titulo: String,
    descripcion: String,
    procesando: Boolean,
    mensajeError: String?,
    onGuardar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var clave by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var errorLocal by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxSize().formularioSeguro().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(titulo, style = MaterialTheme.typography.headlineMedium, color = VerdeOscuro, fontWeight = FontWeight.Bold)
        Text(descripcion, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it },
            label = { Text("Nueva contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirmar,
            onValueChange = { confirmar = it },
            label = { Text("Confirmar contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
        MensajeError(errorLocal ?: mensajeError)
        Button(
            onClick = {
                errorLocal = when {
                    clave.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
                    clave != confirmar -> "Las contraseñas no coinciden."
                    else -> null
                }
                if (errorLocal == null) onGuardar(clave)
            },
            enabled = !procesando,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) { Text(if (procesando) "Guardando…" else "Guardar nueva contraseña") }
        OutlinedButton(onClick = onCancelar, enabled = !procesando, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(top = 8.dp)) {
            Text("Cancelar")
        }
    }
}

@Composable
private fun MensajeError(mensaje: String?) {
    mensaje?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

