package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CleanCreatorLightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = CobaltOnPrimary,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = CobaltOnContainer,
    secondary = CyanSecondary,
    onSecondary = CyanOnSecondary,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = CyanOnContainer,
    tertiary = AmberHype,
    onTertiary = AmberOnHype,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    background = StudioBackground,
    onBackground = InkPrimary,
    surface = StudioSurface,
    onSurface = InkPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = SlateOutline,
    outlineVariant = SlateBorder,
    error = CrimsonLive,
    onError = Color.White,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonOnContainer
)

private val CleanCreatorDarkColorScheme = darkColorScheme(
    primary = DarkCobaltPrimary,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = DarkCobaltContainer,
    onPrimaryContainer = CobaltContainer,
    secondary = FacecamFrameAccent,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = CyanContainer,
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = AmberContainer,
    background = DarkBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = DarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = CrimsonContainer
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GtaClippingTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CleanCreatorDarkColorScheme else CleanCreatorLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = StudioShapes,
        content = content
    )
}
