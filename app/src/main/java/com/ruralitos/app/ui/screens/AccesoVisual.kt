package com.ruralitos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ruralitos.app.R
import com.ruralitos.app.ui.components.BotonPrincipalRuralitos
import com.ruralitos.app.ui.components.BotonSecundarioRuralitos
import com.ruralitos.app.ui.components.LogoRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinico
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.MoradoClinico
import com.ruralitos.app.ui.theme.NaranjaClinico
import com.ruralitos.app.ui.theme.SuperficieClinica
import com.ruralitos.app.ui.theme.TextoClinico
import com.ruralitos.app.ui.theme.TextoSecundario
import com.ruralitos.app.ui.theme.CianRuralitos

@Composable
internal fun MarcoAccesoRuralitos(
    titulo: String,
    subtitulo: String,
    contenido: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .formularioSeguro()
    ) {
        val esTablet = maxWidth >= 760.dp
        val margenHorizontal = if (maxWidth < 390.dp) 14.dp else if (esTablet) 28.dp else 20.dp
        val margenVertical = if (esTablet) 28.dp else 18.dp

        if (esTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = margenHorizontal, vertical = margenVertical),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MarcaAccesoRuralitos(
                    compacta = false,
                    modifier = Modifier.weight(0.82f)
                )
                Column(
                    modifier = Modifier
                        .weight(1.18f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    TarjetaAccesoRuralitos(titulo, subtitulo, contenido)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = margenHorizontal, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                MarcaAccesoRuralitos(compacta = true)
                TarjetaAccesoRuralitos(
                    titulo = titulo,
                    subtitulo = subtitulo,
                    contenido = contenido,
                    modifier = Modifier.widthIn(max = 620.dp).offset(y = (-20).dp)
                )
            }
        }
    }
}

@Composable
private fun MarcaAccesoRuralitos(
    compacta: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compacta) 158.dp else 316.dp)
            .clip(RoundedCornerShape(if (compacta) 24.dp else 30.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.ruralitos_paisaje_cabecera),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = if (compacta) 8.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoRuralitos(modifier = Modifier.size(if (compacta) 65.dp else 132.dp))
            Text(
                text = "Ruralitos",
                style = if (compacta) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                color = TextoClinico,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Fichas familiares, incluso sin conexión",
                style = MaterialTheme.typography.bodySmall,
                color = TextoClinico,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TarjetaAccesoRuralitos(
    titulo: String,
    subtitulo: String,
    contenido: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SuperficieClinica),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BordeClinico)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 22.dp,
                vertical = 24.dp
            )
        ) {
            Text(
                text = titulo,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                color = TextoClinico,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitulo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp, bottom = 18.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario,
                textAlign = TextAlign.Center
            )
            contenido()
        }
    }
}

@Composable
internal fun BotonAccesoPrincipal(
    texto: String,
    onClick: () -> Unit,
    enabled: Boolean,
    color: Color = CianRuralitos,
    descripcion: String? = null,
    modifier: Modifier = Modifier
) {
    BotonPrincipalRuralitos(
        texto = texto,
        descripcion = descripcion,
        onClick = onClick,
        enabled = enabled,
        color = color,
        modifier = modifier.padding(top = 16.dp)
    )
}

@Composable
internal fun BotonAccesoSecundario(
    texto: String,
    onClick: () -> Unit,
    enabled: Boolean,
    descripcion: String? = null,
    modifier: Modifier = Modifier
) {
    BotonSecundarioRuralitos(
        texto = texto,
        descripcion = descripcion,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.padding(top = 10.dp)
    )
}

@Composable
internal fun OpcionAccesoRuralitos(
    titulo: String,
    descripcion: String,
    simbolo: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(forma)
            .background(Color.White)
            .border(1.dp, BordeClinico, forma)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.10f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = simbolo,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 13.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                color = TextoClinico,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun AvisoAccesoRuralitos(
    texto: String,
    color: Color = AzulClinico,
    simbolo: String = "i",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.09f))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(26.dp),
            shape = CircleShape,
            color = color
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(simbolo, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            text = texto,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = TextoClinico
        )
    }
}

@Composable
internal fun TituloSeccionAcceso(
    numero: String,
    titulo: String,
    descripcion: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = RoundedCornerShape(11.dp),
            color = color
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = numero,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 11.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                color = TextoClinico,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

internal val ColorAccesoAzul = AzulClinico
internal val ColorAccesoVerde = CianRuralitos
internal val ColorAccesoNaranja = NaranjaClinico
internal val ColorAccesoMorado = MoradoClinico
internal val ColorBordeAcceso = BordeClinico
