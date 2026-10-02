package com.ruralitos.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val RuralitosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val RuralitosColorScheme = lightColorScheme(
    primary = CianRuralitos,
    onPrimary = Color.White,
    primaryContainer = CianSuave,
    onPrimaryContainer = CianRuralitosOscuro,
    secondary = AzulClinico,
    onSecondary = Color.White,
    secondaryContainer = AzulSuaveRuralitos,
    onSecondaryContainer = AzulClinicoOscuro,
    tertiary = VerdeSalud,
    onTertiary = Color.White,
    background = FondoClinico,
    onBackground = TextoClinico,
    surface = SuperficieClinica,
    onSurface = TextoClinico,
    surfaceVariant = Color(0xFFEEF3F6),
    onSurfaceVariant = TextoSecundario,
    outline = BordeCampo,
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
