package com.ruralitos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.components.LogoRuralitos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.TextoSecundario
import com.ruralitos.app.ui.theme.CianRuralitos

@Composable
fun LoginClinicoScreen(
    procesando: Boolean,
    mensajeError: String?,
    onIngresar: (cedula: String, clave: String) -> Unit
) {
    var cedula by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }

    BoxWithConstraints(Modifier.fillMaxSize().formularioSeguro()) {
        val tableta = maxWidth >= 780.dp
        if (tableta) {
            Row(Modifier.fillMaxSize().formularioSeguro().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                BienvenidaClinica(
                    modifier = Modifier.weight(0.92f).fillMaxHeight(),
                    mostrarDetalles = true
                )
                Box(
                    modifier = Modifier.weight(1.08f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    TarjetaLogin(
                        cedula = cedula,
                        clave = clave,
                        procesando = procesando,
                        mensajeError = mensajeError,
                        onCedula = { cedula = it.filter(Char::isDigit).take(13) },
                        onClave = { clave = it },
                        onIngresar = { onIngresar(cedula.trim(), clave) }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().formularioSeguro().verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BienvenidaClinica(Modifier.fillMaxWidth(), mostrarDetalles = false)
                TarjetaLogin(
                    cedula = cedula,
                    clave = clave,
                    procesando = procesando,
                    mensajeError = mensajeError,
                    onCedula = { cedula = it.filter(Char::isDigit).take(13) },
                    onClave = { clave = it },
                    onIngresar = { onIngresar(cedula.trim(), clave) },
                    modifier = Modifier.padding(top = 12.dp)
                )
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun BienvenidaClinica(modifier: Modifier, mostrarDetalles: Boolean) {
    Box(
        modifier = modifier
            .background(
                com.ruralitos.app.ui.theme.VerdeSuaveRuralitos,
                RoundedCornerShape(24.dp)
            )
            .padding(if (mostrarDetalles) 38.dp else 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoRuralitos(Modifier.size(if (mostrarDetalles) 180.dp else 104.dp))
            Text(
                "Ruralitos",
                color = AzulClinicoOscuro,
                fontSize = if (mostrarDetalles) 40.sp else 27.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            Text(
                "Fichas familiares digitales para la atención rural",
                color = TextoSecundario,
                fontSize = if (mostrarDetalles) 18.sp else 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp)
            )
            if (mostrarDetalles) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Caracteristica("✓", "Funciona sin internet")
                    Caracteristica("✓", "Información local cifrada")
                    Caracteristica("✓", "Excel, PDF y respaldos seguros")
                }
            }
        }
    }
}

@Composable
private fun Caracteristica(simbolo: String, texto: String) {
    Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(34.dp).background(Color.White, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) { Text(simbolo, color = CianRuralitos, fontWeight = FontWeight.Bold) }
        Text(texto, color = AzulClinicoOscuro, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun TarjetaLogin(
    cedula: String,
    clave: String,
    procesando: Boolean,
    mensajeError: String?,
    onCedula: (String) -> Unit,
    onClave: (String) -> Unit,
    onIngresar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var claveVisible by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth().widthIn(max = 530.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BordeClinico),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
            Text(
                "Bienvenido",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                color = AzulClinicoOscuro,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "Ingresa con tu cuenta del personal de salud",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp, bottom = 18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            OutlinedTextField(
                value = cedula,
                onValueChange = onCedula,
                label = { Text("Cédula") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = clave,
                onValueChange = onClave,
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = if (claveVisible) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    androidx.compose.material3.TextButton(onClick = { claveVisible = !claveVisible }) {
                        Text(if (claveVisible) "Ocultar" else "Ver")
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
            mensajeError?.let {
                AvisoAccesoRuralitos(
                    texto = it,
                    color = MaterialTheme.colorScheme.error,
                    simbolo = "!",
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            BotonAccesoPrincipal(
                texto = if (procesando) "Comprobando…" else "Ingresar a mi cuenta",
                onClick = onIngresar,
                enabled = !procesando && cedula.isNotBlank() && clave.isNotBlank(),
                color = ColorAccesoVerde
            )
            AvisoAccesoRuralitos(
                texto = "La sesión y las fichas permanecen únicamente en este dispositivo.",
                color = ColorAccesoAzul,
                simbolo = "i",
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}
