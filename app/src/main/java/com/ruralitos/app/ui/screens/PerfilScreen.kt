package com.ruralitos.app.ui.screens

import com.ruralitos.app.ui.theme.BordeCampo
import com.ruralitos.app.ui.components.MenuDesplegableRuralitos
import com.ruralitos.app.ui.components.BotonSelectorRuralitos
import com.ruralitos.app.ui.components.ItemMenuRuralitos
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
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
import com.ruralitos.app.ui.components.PantallaRuralitos
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
    val sexoInicial = usuario.sexo.ifBlank { SaludoProfesional.inferirSexo(usuario.cargo) }
    val (apellidosInicial, nombresInicial) =
        SaludoProfesional.separarNombre(usuario.nombres, usuario.apellidos)
    var sexo by remember(usuario.id) { mutableStateOf(sexoInicial) }
    var apellidos by remember(usuario.id) { mutableStateOf(apellidosInicial) }
    var nombres by remember(usuario.id) { mutableStateOf(nombresInicial) }
    var cargo by remember(usuario.id) {
        mutableStateOf(SaludoProfesional.cargoEquivalente(usuario.cargo, sexoInicial))
    }
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
            sexo.isBlank() -> "Elige tu sexo."
            apellidos.trim().length < 2 -> "Ingresa tus apellidos."
            nombres.trim().length < 2 -> "Ingresa tus nombres."
            cargo.isBlank() -> "Elige tu cargo."
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
                nombres = "${apellidos.trim()} ${nombres.trim()}".replace(Regex("\\s+"), " "),
                cargo = cargo.trim(),
                correo = correo.trim(),
                telefono = telefono.trim(),
                sexo = sexo,
                apellidos = apellidos.trim()
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
                        actualizado.codigoSenescyt,
                        actualizado.sexo,
                        actualizado.apellidos
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

    PantallaRuralitos(
        titulo = "Datos personales",
        descripcion = "Actualiza la información que identifica tu cuenta profesional en Ruralitos.",
        subtitulo = "Perfil del personal de salud",
        onVolver = onRegresar,
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = if (guardando) "Guardando datos…" else "Guardar mis datos personales",
                descripcion = "Actualizar la información local y en la cuenta",
                onClick = guardarPerfil,
                enabled = !guardando,
                color = CianRuralitos
            )

        }
    ) {
        SeccionFormularioRuralitos(
            titulo = "Identificación personal",
            descripcion = "Tu sexo define la lista de cargos y el título del saludo (Dr., Dra., Lcda.…)."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                CampoPerfilSeparado(
                    valor = cedula,
                    onCambio = {
                        cedula = it.filter(Char::isDigit).take(13)
                        mensaje = null
                    },
                    etiqueta = "Cédula o identificación",
                    teclado = KeyboardType.Number
                )
                SelectorSexoProfesional(
                    valor = sexo,
                    onCambio = {
                        sexo = it
                        cargo = SaludoProfesional.cargoEquivalente(cargo, it)
                        mensaje = null
                    }
                )
                CampoPerfilSeparado(
                    valor = apellidos,
                    onCambio = {
                        apellidos = it.take(80)
                        mensaje = null
                    },
                    etiqueta = "Apellidos",
                    ayuda = "Primero los dos apellidos, por ejemplo: Pérez Gómez."
                )
                CampoPerfilSeparado(
                    valor = nombres,
                    onCambio = {
                        nombres = it.take(80)
                        mensaje = null
                    },
                    etiqueta = "Nombres",
                    ayuda = "Por ejemplo: Ana María. El saludo usa tu primer nombre."
                )
                CampoCargoPredeterminado(
                    valor = cargo,
                    sexo = sexo,
                    onCambio = {
                        cargo = it.take(80)
                        mensaje = null
                    }
                )
                CampoPerfilSeparado(
                    valor = telefono,
                    onCambio = {
                        telefono = it.filter(Char::isDigit).take(15)
                        mensaje = null
                    },
                    etiqueta = "Número de teléfono",
                    teclado = KeyboardType.Phone
                )
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
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(15.dp)
                )
            }
        }
    }
}

/** Elección de sexo (Hombre / Mujer): de ella dependen la lista de cargos y el título del saludo. */
@Composable
internal fun SelectorSexoProfesional(
    valor: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(top = 10.dp, bottom = 4.dp)) {
        Text(
            "Sexo",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF5B7083),
            fontWeight = FontWeight.SemiBold
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(SaludoProfesional.SEXO_HOMBRE to "Hombre", SaludoProfesional.SEXO_MUJER to "Mujer")
                .forEach { (clave, etiqueta) ->
                    val seleccionado = valor == clave
                    Surface(
                        onClick = { onCambio(clave) },
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (seleccionado) Color(0xFFE3F4F7) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            if (seleccionado) 1.5.dp else 1.dp,
                            if (seleccionado) CianRuralitos else BordeCampo
                        )
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                etiqueta,
                                color = if (seleccionado) CianRuralitos else Color(0xFF0A2A5E),
                                fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CampoCargoPredeterminado(
    valor: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
    sexo: String = ""
) {
    var abierto by remember { mutableStateOf(false) }
    // Sin repetidos: cada sexo tiene su propia lista de cargos.
    val opciones = SaludoProfesional.cargosPara(sexo)
    Column(modifier.padding(top = 10.dp, bottom = 4.dp)) {
        Text(
            "Cargo profesional",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF5B7083),
            fontWeight = FontWeight.SemiBold
        )
        Box(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            BotonSelectorRuralitos(
                onClick = { if (opciones.isNotEmpty()) abierto = true },
                enabled = opciones.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(
                    valor.ifBlank { if (opciones.isEmpty()) "Primero elige tu sexo" else "Selecciona tu cargo" },
                    color = if (valor.isBlank()) Color(0xFF5B7083) else CianRuralitos,
                    fontWeight = FontWeight.SemiBold
                )
            }
            MenuDesplegableRuralitos(
                expanded = abierto && opciones.isNotEmpty(),
                onDismissRequest = { abierto = false }
            ) {
                opciones.forEach { opcion ->
                    ItemMenuRuralitos(
                        text = { Text(opcion) },
                        seleccionado = opcion == valor,
                        onClick = {
                            onCambio(opcion)
                            abierto = false
                        }
                    )
                }
            }
        }
        Text(
            "Este cargo y tu sexo se usan en el saludo de la pantalla de inicio (por ejemplo: Dra. Ana Pérez).",
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
            fontWeight = FontWeight.SemiBold
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
