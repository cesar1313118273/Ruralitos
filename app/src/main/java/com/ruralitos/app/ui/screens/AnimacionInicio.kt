package com.ruralitos.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruralitos.app.ui.components.LogoRuralitos
import com.ruralitos.app.ui.components.formularioSeguro
import com.ruralitos.app.ui.theme.AzulClinicoOscuro
import com.ruralitos.app.ui.theme.BordeClinico
import com.ruralitos.app.ui.theme.CianRuralitos
import com.ruralitos.app.ui.theme.FondoClinico
import com.ruralitos.app.ui.theme.TextoSecundario
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animación de entrada de Ruralitos: el logo aparece con un pequeño rebote, un anillo fino se dibuja a su alrededor
 * y una onda suave sale de él; después entran el nombre y la frase mientras una barra delgada se llena. Termina con
 * un desvanecimiento hacia la pantalla que la app ya tiene cargada debajo. Dura unos 2,5 segundos.
 */
@Composable
fun AnimacionInicioRuralitos(onTerminar: () -> Unit) {
    val logo = remember { Animatable(0f) }
    val anillo = remember { Animatable(0f) }
    val onda = remember { Animatable(0f) }
    val nombre = remember { Animatable(0f) }
    val frase = remember { Animatable(0f) }
    val barraVisible = remember { Animatable(0f) }
    val barra = remember { Animatable(0f) }
    val salida = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        val rebote = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
        launch { delay(50); logo.animateTo(1f, tween(700, easing = rebote)) }
        launch { delay(150); anillo.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch { delay(500); onda.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) }
        launch { delay(650); nombre.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch { delay(850); frase.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch { delay(500); barraVisible.animateTo(1f, tween(300)) }
        launch { delay(500); barra.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }
        delay(2150)
        salida.animateTo(0f, tween(350, easing = LinearEasing))
        onTerminar()
    }

    Box(
        Modifier
            .fillMaxSize()
            .formularioSeguro()
            .alpha(salida.value)
            .background(FondoClinico)
            // Mientras dura, no deja tocar lo que hay debajo.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val grosor = 3.dp.toPx()
                    val lado = size.minDimension - grosor * 2
                    // Onda que sale del logo y se desvanece.
                    if (onda.value > 0f) {
                        val crecer = 0.8f + 0.5f * onda.value
                        val radio = size.minDimension / 2f * 0.84f * crecer
                        drawCircle(
                            color = CianRuralitos.copy(alpha = 0.55f * (1f - onda.value)),
                            radius = radio,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    // Anillo que se dibuja.
                    drawArc(
                        color = CianRuralitos,
                        startAngle = -90f,
                        sweepAngle = 360f * anillo.value,
                        useCenter = false,
                        topLeft = Offset(grosor, grosor),
                        size = Size(lado, lado),
                        style = Stroke(width = grosor, cap = StrokeCap.Round)
                    )
                }
                LogoRuralitos(
                    modifier = Modifier
                        .size(76.dp)
                        .graphicsLayer {
                            val escala = 0.72f + 0.28f * logo.value
                            scaleX = escala
                            scaleY = escala
                            alpha = logo.value
                        }
                )
            }
            Text(
                "Ruralitos",
                color = AzulClinicoOscuro,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .graphicsLayer { alpha = nombre.value; translationY = (1f - nombre.value) * 24f }
            )
            Text(
                "Fichas familiares, incluso sin conexión",
                color = TextoSecundario,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(top = 3.dp)
                    .graphicsLayer { alpha = frase.value; translationY = (1f - frase.value) * 24f }
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
                .width(140.dp)
                .size(width = 140.dp, height = 3.dp)
                .alpha(barraVisible.value)
                .clip(RoundedCornerShape(3.dp))
                .background(BordeClinico)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = barra.value
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                    }
                    .background(CianRuralitos)
            )
        }
    }
}
