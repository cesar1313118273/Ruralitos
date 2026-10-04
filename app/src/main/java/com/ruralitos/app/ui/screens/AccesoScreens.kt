package com.ruralitos.app.ui.screens

import com.ruralitos.app.domain.SaludoProfesional
import com.ruralitos.app.ui.theme.FondoClinico
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
    duracionMinimaMs: Long = 0L,
    contenido: @Composable () -> Unit
) {
    var tiempoMinimoCumplido by remember {
        mutableStateOf(duracionMinimaMs <= 0L)
    }

    LaunchedEffect(duracionMinimaMs) {
        if (duracionMinimaMs > 0L) {
            delay(duracionMinimaMs)
            tiempoMinimoCumplido = true
        }
    }

    if (cargandoReal || !tiempoMinimoCumplido) {
        CargandoAccesoScreen()
    } else {
        contenido()
    }
}

/** Mientras carga no se muestra nada: solo el fondo de la app, sin personaje ni mensajes. */
@Composable
fun CargandoAccesoScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .formularioSeguro()
            .background(FondoClinico)
    )
}

@Composable
fun CrearAdministradorScreen(
    procesando: Boolean,
    mensajeError: String?,
    onCrear: (cedula: String, nombres: String, cargo: String, clave: String, sexo: String) -> Unit
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
        Text("Ruralitos", style = MaterialTheme.typography.headlineLarge, color = VerdeOscuro, fontWeight = FontWeight.SemiBold)
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
    onEnviar: (String, String, String, String, String) -> Unit
) {
    var cedula by remember { mutableStateOf("") }
    var nombres by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("") }
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
        Text(titulo, style = MaterialTheme.typography.headlineMedium, color = VerdeOscuro, fontWeight = FontWeight.SemiBold)
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
            SelectorSexoProfesional(
                valor = sexo,
                onCambio = {
                    sexo = it
                    cargo = SaludoProfesional.cargoEquivalente(cargo, it)
                }
            )
            CampoCargoPredeterminado(
                valor = cargo,
                sexo = sexo,
                onCambio = { cargo = it }
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
                    sexo.isBlank() -> "Elige el sexo."
                    cargo.isBlank() -> "Elige el cargo."
                    clave.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
                    clave != confirmar -> "Las contraseñas no coinciden."
                    else -> null
                }
                if (errorLocal == null) onEnviar(cedula.trim(), nombres.trim(), cargo.trim(), clave, sexo)
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
            onEnviar = { cedula, nombres, cargo, clave, sexo ->
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
                                    sexo = sexo,
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
        Text("Usuarios locales", style = MaterialTheme.typography.headlineMedium, color = VerdeOscuro, fontWeight = FontWeight.SemiBold)
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
                            Text(usuario.nombres, fontWeight = FontWeight.SemiBold)
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
        Text("Cambiar contraseña", style = MaterialTheme.typography.headlineMedium, color = VerdeOscuro, fontWeight = FontWeight.SemiBold)
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
        Text(titulo, style = MaterialTheme.typography.headlineMedium, color = VerdeOscuro, fontWeight = FontWeight.SemiBold)
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

