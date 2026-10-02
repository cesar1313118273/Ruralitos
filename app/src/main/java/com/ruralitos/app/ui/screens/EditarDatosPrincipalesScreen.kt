package com.ruralitos.app.ui.screens

import com.ruralitos.app.ui.components.FlechaDesplegable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ruralitos.app.data.local.entity.FichaFamiliarEntity
import com.ruralitos.app.domain.ValidadorIdentidadEcuador
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.EncabezadoRuralitos
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.SeccionFormularioRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.CianRuralitos

data class DatosPrincipalesForm(
    val cedula: String,
    val nombre: String,
    val telefono: String,
    val numeroFicha: String,
    val fecha: String,
    val responsableNombre: String,
    val responsableCodigo: String
)

@Composable
fun EditarDatosPrincipalesScreen(
    ficha: FichaFamiliarEntity,
    onGuardar: (DatosPrincipalesForm) -> Unit,
    onCancelar: () -> Unit
) {
    var cedula by remember(ficha.id) { mutableStateOf(ficha.cedulaJefeHogar) }
    var nombre by remember(ficha.id) { mutableStateOf(ficha.nombreApellidoJefeFamilia) }
    var telefono by remember(ficha.id) { mutableStateOf(ficha.numeroTelefono) }
    var fecha by remember(ficha.id) { mutableStateOf(ficha.fechaLlenado) }
    var calendario by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    PantallaRuralitos(
        titulo = "Datos personales y de la ficha",
        descripcion = "Revisa o corrige los datos del jefe o jefa de la familia y el control de la ficha.",
        paso = 1,
        totalPasos = 10,
        etiquetaPaso = "Información del hogar",
        onVolver = onCancelar,
        barraAccion = {
            BotonPrincipalRuralitos(
                texto = "Guardar datos principales",
                descripcion = "Conservar los cambios y volver a la ficha",
                onClick = {
                    if (!ValidadorIdentidadEcuador.esDocumentoFamiliarAceptable(cedula)) {
                        error = "La identificación debe contener exactamente 10 o 13 números."
                    } else if (nombre.isBlank() || ficha.numeroFichaFamiliar.isBlank()) {
                        error = "Completa los apellidos y nombres."
                    } else {
                        onGuardar(
                            DatosPrincipalesForm(
                                cedula.trim(),
                                nombre.trim(),
                                telefono.trim(),
                                ficha.numeroFichaFamiliar,
                                fecha,
                                ficha.responsableNombre.trim(),
                                ficha.responsableCodigo.trim()
                            )
                        )
                    }
                },
                color = CianRuralitos
            )

        }
    ) {
        SeccionFormularioRuralitos(
            titulo = "Jefe o jefa de la familia",
            descripcion = "La identificación también se utilizará como número de historia clínica."
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 680.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            CampoPersonal(
                                valor = cedula,
                                onCambio = { cedula = it.filter(Char::isDigit).take(13) },
                                etiqueta = "Cédula o identificación",
                                ayuda = "10 dígitos para cédula o 13 para otra identificación",
                                teclado = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )
                            CampoPersonal(
                                valor = telefono,
                                onCambio = { telefono = it },
                                etiqueta = "Número de teléfono",
                                teclado = KeyboardType.Phone,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        CampoPersonal(
                            valor = nombre,
                            onCambio = { nombre = it },
                            etiqueta = "Apellidos y nombres completos"
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        CampoPersonal(
                            valor = cedula,
                            onCambio = { cedula = it.filter(Char::isDigit).take(13) },
                            etiqueta = "Cédula o identificación",
                            ayuda = "10 dígitos para cédula o 13 para otra identificación",
                            teclado = KeyboardType.Number
                        )
                        CampoPersonal(
                            valor = nombre,
                            onCambio = { nombre = it },
                            etiqueta = "Apellidos y nombres completos"
                        )
                        CampoPersonal(
                            valor = telefono,
                            onCambio = { telefono = it },
                            etiqueta = "Número de teléfono",
                            teclado = KeyboardType.Phone
                        )
                    }
                }
            }
        }
        SeccionFormularioRuralitos(
            titulo = "Control de la ficha",
            descripcion = "Información administrativa del documento familiar."
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 680.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            CampoPersonal(
                                valor = ficha.numeroFichaFamiliar,
                                onCambio = {},
                                etiqueta = "Número de ficha familiar",
                                ayuda = "Asignado automáticamente por Ruralitos",
                                soloLectura = true,
                                modifier = Modifier.weight(1f)
                            )
                            CampoFechaPersonal(
                                valor = fecha,
                                etiqueta = "Fecha de llenado",
                                onClick = { calendario = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        CampoPersonal(
                            valor = ficha.numeroFichaFamiliar,
                            onCambio = {},
                            etiqueta = "Número de ficha familiar",
                            ayuda = "Asignado automáticamente por Ruralitos",
                            soloLectura = true
                        )
                        CampoFechaPersonal(
                            valor = fecha,
                            etiqueta = "Fecha de llenado",
                            onClick = { calendario = true }
                        )
                    }
                }
            }
        }

        error?.let {
            Surface(
                color = Color(0xFFFFECEF),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }

    if (calendario) {
        SelectorFechaDialog(
            onFechaSeleccionada = {
                fecha = it
                calendario = false
            },
            onCerrar = { calendario = false }
        )
    }
}

@Composable
private fun CampoPersonal(
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
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 5.dp, start = 2.dp)
            )
        }
    }
}

@Composable
private fun CampoFechaPersonal(
    valor: String,
    etiqueta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(top = 10.dp, bottom = 4.dp)) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF5B7083),
            fontWeight = FontWeight.SemiBold
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            OutlinedTextField(
                value = valor,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { FlechaDesplegable(color = AzulClinico) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AzulClinico,
                    unfocusedBorderColor = BordeClinico,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)
            )
            Box(Modifier.matchParentSize().clickable(onClick = onClick))
        }
        Text(
            text = "Toca el cuadro para seleccionar la fecha",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 5.dp, start = 2.dp)
        )
    }
}

