package com.ruralitos.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.R
import com.ruralitos.app.data.local.entity.EstablecimientoSaludEntity
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.components.PantallaRuralitos
import com.ruralitos.app.ui.components.TarjetaFormularioRuralitos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val FondoFichaDatos = Color(0xFFF6F9FB)
private val AzulTituloFicha = Color(0xFF0A2A5E)
private val AzulAccionFicha = Color(0xFF1565C0)
private val GrisTextoFicha = Color(0xFF5B7083)
private val VerdeAccionFicha = Color(0xFF0889A0)
private val BordeCampoFicha = Color(0xFFCFDDE5)
private val AzulSuaveFicha = Color(0xFFE8EFFA)

@Composable
fun DatosFamiliaScreen(
    establecimiento: EstablecimientoSaludEntity,
    numeroFichaAutomatico: String,
    onGuardar: (
        cedulaJefeHogar: String,
        nombreJefeFamilia: String,
        numeroTelefono: String,
        numeroFichaFamiliar: String,
        fechaLlenado: String
    ) -> Unit,
    onRegresar: () -> Unit
) {
    var cedulaJefeHogar by remember { mutableStateOf("") }
    var nombreJefeFamilia by remember { mutableStateOf("") }
    var numeroTelefono by remember { mutableStateOf("") }

    val numeroFichaFamiliar = numeroFichaAutomatico

    val formatoFechaHoy = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    }
    var fechaLlenado by remember {
        mutableStateOf(formatoFechaHoy.format(Date()))
    }
    var mostrarCalendario by remember { mutableStateOf(false) }

    PantallaRuralitos(
        titulo = "Datos personales de la familia",
        subtitulo = "Registra al jefe o jefa del hogar.",
        paso = 1,
        totalPasos = 10,
        etiquetaPaso = "Información del hogar",
        onVolver = onRegresar,
        barraAccion = {
            BotonGuardarYContinuar(
                onClick = {
                    onGuardar(
                        cedulaJefeHogar,
                        nombreJefeFamilia,
                        numeroTelefono,
                        numeroFichaFamiliar,
                        fechaLlenado
                    )
                }
            )
        }
    ) {
        TarjetaFormularioRuralitos {
            TituloSeccionConIcono(
                titulo = "Unidad operativa seleccionada",
                iconoRes = R.drawable.fichadatos_icono_ubicacion_unidad_operativa_2026
            )
            ResumenUnidadOperativa(establecimiento)
        }

        TarjetaFormularioRuralitos {
            TituloSeccionConIcono(
                titulo = "Jefe o jefa del hogar",
                iconoRes = R.drawable.fichadatos_icono_jefe_hogar_persona_principal_2026
            )
            CampoTextoFichaVisual(
                valor = cedulaJefeHogar,
                onValorChange = {
                    cedulaJefeHogar = it.filter(Char::isDigit).take(13)
                },
                etiqueta = "Cédula o identificación",
                iconoRes = R.drawable.fichadatos_icono_cedula_identificacion_personal_2026,
                keyboardType = KeyboardType.Number
            )
            CampoTextoFichaVisual(
                valor = nombreJefeFamilia,
                onValorChange = { nombreJefeFamilia = it },
                etiqueta = "Apellidos y nombres completos",
                iconoRes = R.drawable.fichadatos_icono_nombres_apellidos_usuario_2026
            )
            CampoTextoFichaVisual(
                valor = numeroTelefono,
                onValorChange = { numeroTelefono = it },
                etiqueta = "Número de teléfono",
                iconoRes = R.drawable.fichadatos_icono_numero_telefono_contacto_2026,
                keyboardType = KeyboardType.Phone
            )
        }

        TarjetaFormularioRuralitos {
            TituloSeccionConIcono(
                titulo = "Control de la ficha",
                iconoRes = R.drawable.fichadatos_icono_control_numero_ficha_documento_2026
            )
            TarjetaNumeroFicha(numeroFichaFamiliar = numeroFichaFamiliar)
            CampoFecha(
                valor = fechaLlenado,
                etiqueta = "Fecha de llenado",
                onClick = { mostrarCalendario = true }
            )
        }
    }

    if (mostrarCalendario) {
        SelectorFechaDialog(
            fechaInicial = fechaLlenado,
            onFechaSeleccionada = { nuevaFecha ->
                fechaLlenado = nuevaFecha
                mostrarCalendario = false
            },
            onCerrar = { mostrarCalendario = false }
        )
    }
}

@Composable
private fun TituloSeccionConIcono(
    titulo: String,
    iconoRes: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(iconoRes),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Fit
        )

        Text(
            text = titulo,
            color = AzulTituloFicha,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ResumenUnidadOperativa(
    establecimiento: EstablecimientoSaludEntity
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF6F9FB),
        border = BorderStroke(1.dp, Color(0xFFE2ECF1)),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(
                    id = R.drawable.fichadatos_icono_establecimiento_salud_edificio_2026
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = establecimiento.nombreCentroSalud,
                    color = AzulTituloFicha,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Código UO: ${establecimiento.codigoUo}",
                    color = GrisTextoFicha,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = listOf(
                        establecimiento.parroquia,
                        establecimiento.canton,
                        establecimiento.provincia
                    )
                        .filter(String::isNotBlank)
                        .joinToString(" · "),
                    color = GrisTextoFicha,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CampoTexto(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String
) {
    /*
     * Firma ORIGINAL conservada para no romper otras pantallas
     * que puedan reutilizar CampoTexto().
     */
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AzulAccionFicha,
            unfocusedBorderColor = BordeCampoFicha,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor = AzulAccionFicha
        )
    )
}

@Composable
private fun CampoTextoFichaVisual(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    iconoRes: Int,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        placeholder = {
            Text(
                text = etiqueta,
                color = GrisTextoFicha
            )
        },
        leadingIcon = {
            Image(
                painter = painterResource(iconoRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                contentScale = ContentScale.Fit
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AzulAccionFicha,
            unfocusedBorderColor = BordeCampoFicha,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor = AzulAccionFicha
        )
    )
}

@Composable
private fun TarjetaNumeroFicha(
    numeroFichaFamiliar: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AzulSuaveFicha,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFE2ECF1)
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 13.dp
            )
        ) {
            Text(
                text = "NÚMERO DE FICHA",
                color = AzulAccionFicha,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = numeroFichaFamiliar,
                color = AzulTituloFicha,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun CampoFecha(
    valor: String,
    etiqueta: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BordeCampoFicha)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(
                    id = R.drawable.fichadatos_icono_calendario_fecha_llenado_2026
                ),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 13.dp)
            ) {
                Text(
                    text = etiqueta,
                    color = AzulAccionFicha,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = valor,
                    color = AzulTituloFicha,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Text(
                text = "⌄",
                color = AzulAccionFicha,
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BotonGuardarYContinuar(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VerdeAccionFicha,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 9.dp
        )
    ) {
        // Corrección solicitada: la flecha se mantiene A LA IZQUIERDA
        // dentro del mismo círculo blanco del botón anterior.
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = Color.White
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        id = R.drawable.fichadatos_icono_continuar_flecha_boton_verde_2026
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Text(
            text = "Guardar datos y continuar a la dirección",
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            textAlign = TextAlign.Center,
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Compensa el círculo izquierdo para que el texto quede centrado.
        Spacer(
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
private fun BotonVolverCentroSalud(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = VerdeAccionFicha
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = VerdeAccionFicha
        )
    ) {
        Text(
            text = "Volver a seleccionar el centro de salud",
            color = VerdeAccionFicha,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFechaDialog(
    onFechaSeleccionada: (String) -> Unit,
    onCerrar: () -> Unit
) {
    /*
     * Firma ORIGINAL conservada para compatibilidad con cualquier otra
     * pantalla del proyecto que llame SelectorFechaDialog() con 2 parámetros.
     */
    SelectorFechaDialog(
        fechaInicial = "",
        onFechaSeleccionada = onFechaSeleccionada,
        onCerrar = onCerrar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFechaDialog(
    fechaInicial: String,
    onFechaSeleccionada: (String) -> Unit,
    onCerrar: () -> Unit
) {
    /*
     * Corrección de fecha:
     * Material DatePicker trabaja con días UTC. Se inicializa el selector usando
     * también UTC para evitar desfases de un día por zona horaria.
     */
    val formatoUtc = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
    }

    val fechaInicialMillis = remember(fechaInicial) {
        if (fechaInicial.isBlank()) {
            val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            runCatching {
                formatoUtc.parse(hoy)?.time
            }.getOrNull()
        } else {
            runCatching {
                formatoUtc.parse(fechaInicial)?.time
            }.getOrNull()
        }
    }

    val estadoFecha = rememberDatePickerState(
        initialSelectedDateMillis = fechaInicialMillis
    )

    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = estadoFecha.selectedDateMillis

                    if (millis != null) {
                        val textoFecha = formatoUtc.format(Date(millis))
                        onFechaSeleccionada(textoFecha)
                    } else {
                        onCerrar()
                    }
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(
            state = estadoFecha
        )
    }
}

