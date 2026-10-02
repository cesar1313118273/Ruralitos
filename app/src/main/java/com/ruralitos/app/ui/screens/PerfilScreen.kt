package com.ruralitos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ruralitos.app.data.local.dao.UsuarioDao
import com.ruralitos.app.data.local.entity.UsuarioEntity
import com.ruralitos.app.domain.SaludoProfesional
import com.ruralitos.app.domain.ValidadorIdentidadEcuador
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PerfilScreen(
    usuario: UsuarioEntity,
    usuarioDao: UsuarioDao,
    onGuardarRemoto: suspend (UsuarioEntity) -> Unit,
    onActualizado: (UsuarioEntity) -> Unit,
    onCambiarClave: () -> Unit,
    onRegresar: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var cedula by remember(usuario.id) { mutableStateOf(usuario.cedula) }
    var nombres by remember(usuario.id) { mutableStateOf(usuario.nombres) }
    var cargo by remember(usuario.id) { mutableStateOf(usuario.cargo) }
    var correo by remember(usuario.id) { mutableStateOf(usuario.correo) }
    var telefono by remember(usuario.id) { mutableStateOf(usuario.telefono) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var esError by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    val guardarPerfil: () -> Unit = guardar@{
        val correoValido = correo.isBlank() ||
            (correo.contains('@') && correo.substringAfter('@').contains('.'))
        val error = when {
            !ValidadorIdentidadEcuador.esIdentificacionAceptable(cedula) ->
                "Ingresa una cédula o RUC válido."
            nombres.trim().length < 4 -> "Ingresa tus apellidos y nombres."
            cargo.isBlank() -> "Ingresa el cargo."
            !correoValido -> "Ingresa un correo electrónico válido."
            telefono.isNotBlank() && telefono.length < 7 -> "Ingresa un teléfono válido."
            else -> null
        }
        if (error != null) {
            mensaje = error
            esError = true
            return@guardar
        }

        guardando = true
        scope.launch {
            val actualizado = usuario.copy(
                cedula = cedula.trim(),
                nombres = nombres.trim(),
                cargo = cargo.trim(),
                correo = correo.trim(),
                telefono = telefono.trim()
            )
            runCatching {
                onGuardarRemoto(actualizado)
                withContext(Dispatchers.IO) {
                    usuarioDao.actualizarPerfil(
                        actualizado.id,
                        actualizado.cedula,
                        actualizado.nombres,
                        actualizado.cargo,
                        actualizado.correo,
                        actualizado.telefono,
                        actualizado.codigoSenescyt
                    )
                }
            }.onSuccess {
                onActualizado(actualizado)
                mensaje = "Perfil actualizado correctamente."
                esError = false
            }.onFailure {
                mensaje = "No se pudo guardar. Verifica que la cédula no pertenezca a otra cuenta."
                esError = true
            }
            guardando = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .formularioSeguro()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EncabezadoRuralitos(
            titulo = "Datos personales",
            descripcion = "Actualiza la información que identifica tu cuenta profesional en Ruralitos.",
            paso = if (usuario.esAdministrador) "Perfil de administrador" else "Perfil del personal de salud",
            color = MoradoClinico
        )

        SeccionFormularioRuralitos(
            titulo = "Identificación personal",
            descripcion = "Las etiquetas permanecen separadas de los cuadros para facilitar la lectura."
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 700.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            CampoPerfilSeparado(
                                valor = cedula,
                                onCambio = {
                                    cedula = it.filter(Char::isDigit).take(13)
                                    mensaje = null
                                },
                                etiqueta = "Cédula o identificación",
                                teclado = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )
                            CampoPerfilSeparado(
                                valor = nombres,
                                onCambio = {
                                    nombres = it.take(120)
                                    mensaje = null
                                },
                                etiqueta = "Apellidos y nombres",
                                modifier = Modifier.weight(2f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            CampoCargoPredeterminado(
                                valor = cargo,
                                onCambio = {
                                    cargo = it.take(80)
                                    mensaje = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                            CampoPerfilSeparado(
                                valor = telefono,
                                onCambio = {
                                    telefono = it.filter(Char::isDigit).take(15)
                                    mensaje = null
                                },
                                etiqueta = "Número de teléfono",
                                teclado = KeyboardType.Phone,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        CampoPerfilSeparado(
                            cedula,
                            {
                                cedula = it.filter(Char::isDigit).take(13)
                                mensaje = null
                            },
                            "Cédula o identificación",
                            teclado = KeyboardType.Number
                        )
                        CampoPerfilSeparado(
                            nombres,
                            {
                                nombres = it.take(120)
                                mensaje = null
                            },
                            "Apellidos y nombres"
                        )
                        CampoCargoPredeterminado(
                            valor = cargo,
                            onCambio = {
                                cargo = it.take(80)
                                mensaje = null
                            }
                        )
                        CampoPerfilSeparado(
                            telefono,
                            {
                                telefono = it.filter(Char::isDigit).take(15)
                                mensaje = null
                            },
                            "Número de teléfono",
                            teclado = KeyboardType.Phone
                        )
                    }
                }
            }
        }

        SeccionFormularioRuralitos(
            titulo = "Acceso y seguridad",
            descripcion = "El correo identifica la cuenta y la contraseña se administra por separado."
        ) {
            CampoPerfilSeparado(
                valor = correo,
                onCambio = {},
                etiqueta = "Correo de acceso",
                ayuda = "El correo no se modifica durante esta etapa de prueba.",
                soloLectura = true,
                teclado = KeyboardType.Email
            )
            BotonSecundarioRuralitos(
                texto = "Cambiar mi contraseña",
                descripcion = "Abrir las opciones de seguridad de la cuenta",
                onClick = onCambiarClave,
                enabled = !guardando,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        mensaje?.let {
            Surface(
                color = if (esError) Color(0xFFFFECEF) else Color(0xFFE3F4F7),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    it,
                    color = if (esError) MaterialTheme.colorScheme.error else CianRuralitos,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(15.dp)
                )
            }
        }

        BotonPrincipalRuralitos(
            texto = if (guardando) "Guardando datos…" else "Guardar mis datos personales",
            descripcion = "Actualizar la información local y en la cuenta",
            onClick = guardarPerfil,
            enabled = !guardando,
            color = CianRuralitos
        )
        BotonSecundarioRuralitos(
            texto = "Regresar al menú principal",
            descripcion = "Salir de Datos personales",
            onClick = onRegresar,
            enabled = !guardando,
            modifier = Modifier.padding(bottom = 28.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CampoCargoPredeterminado(
    valor: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }
    val predeterminados = SaludoProfesional.cargosPredeterminados
    val opciones = remember(valor) {
        if (valor.isNotBlank() && valor !in predeterminados) listOf(valor) + predeterminados
        else predeterminados
    }
    Column(modifier.padding(top = 10.dp, bottom = 4.dp)) {
        Text(
            "Cargo profesional",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF5B7083),
            fontWeight = FontWeight.Bold
        )
        ExposedDropdownMenuBox(
            expanded = abierto,
            onExpandedChange = { abierto = !abierto },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        ) {
            OutlinedTextField(
                value = valor,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                placeholder = { Text("Selecciona tu cargo") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(abierto) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MoradoClinico,
                    unfocusedBorderColor = BordeClinico,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = abierto,
                onDismissRequest = { abierto = false }
            ) {
                opciones.forEach { opcion ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(opcion, fontWeight = FontWeight.Bold)
                                if (opcion == valor) {
                                    Text(
                                        "Cargo seleccionado",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CianRuralitos
                                    )
                                }
                            }
                        },
                        onClick = {
                            onCambio(opcion)
                            abierto = false
                        }
                    )
                }
            }
        }
        Text(
            "Este cargo se utilizará en el saludo profesional de la sesión.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 5.dp, start = 2.dp)
        )
    }
}
@Composable
private fun CampoPerfilSeparado(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    soloLectura: Boolean = false,
    teclado: KeyboardType = KeyboardType.Text
) {
    Column(modifier.padding(top = 10.dp, bottom = 4.dp)) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF5B7083),
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            readOnly = soloLectura,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = teclado),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AzulClinico,
                unfocusedBorderColor = BordeClinico,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = if (soloLectura) Color(0xFFE8EFFA) else Color.White,
                focusedTextColor = if (soloLectura) Color(0xFF1565C0) else MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = if (soloLectura) Color(0xFF1565C0) else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .padding(top = 6.dp)
        )
        ayuda?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 5.dp, start = 2.dp)
            )
        }
    }
}
