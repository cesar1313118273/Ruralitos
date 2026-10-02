package com.ruralitos.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val RuralitosShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

private val RuralitosColorScheme = lightColorScheme(
    primary = VerdeClinico,
    onPrimary = Color.White,
    primaryContainer = VerdeSuaveRuralitos,
    onPrimaryContainer = VerdeClinicoOscuro,
    secondary = AzulClinico,
    onSecondary = Color.White,
    secondaryContainer = AzulSuaveRuralitos,
    onSecondaryContainer = AzulClinicoOscuro,
    tertiary = TealRuralitos,
    onTertiary = Color.White,
    background = FondoClinico,
    onBackground = TextoClinico,
    surface = SuperficieClinica,
    onSurface = TextoClinico,
    surfaceVariant = Color(0xFFEDF6FC),
    onSurfaceVariant = TextoSecundario,
    outline = Color(0xFFBBD4E8),
    outlineVariant = BordeClinico,
    error = RojoClinico,
    onError = Color.White
)

@Composable
fun RuralitosTheme(content: @Composable () -> Unit) {
    // Paleta clara fija: no cambia cuando el teléfono activa el modo oscuro.
    MaterialTheme(
        colorScheme = RuralitosColorScheme,
        typography = Typography,
        shapes = RuralitosShapes,
        content = content
    )
}
