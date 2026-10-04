package com.ruralitos.app.ui.screens

import com.ruralitos.app.ui.components.FlechaDesplegable
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

            FlechaDesplegable(color = AzulAccionFicha)
        }
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

@Composable
fun SelectorFechaDialog(
    fechaInicial: String,
    onFechaSeleccionada: (String) -> Unit,
    onCerrar: () -> Unit
) {
    // El calendario de la app trabaja con días locales: no hay desfases de un día por zona horaria.
    val formato = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false } }
    val inicial = remember(fechaInicial) {
        if (fechaInicial.isBlank()) null else runCatching { formato.parse(fechaInicial)?.time }.getOrNull()
    }
    com.ruralitos.app.ui.components.CalendarioRuralitos(
        fechaInicialMillis = inicial ?: com.ruralitos.app.ui.components.inicioDiaLocal(System.currentTimeMillis()),
        onElegida = { onFechaSeleccionada(formato.format(Date(it))) },
        onCerrar = onCerrar
    )
}

