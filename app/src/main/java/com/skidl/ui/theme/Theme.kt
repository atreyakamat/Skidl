package com.skidl.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = CoralPink,
    onPrimary = CreamWhite,
    primaryContainer = SunshineYellow,
    onPrimaryContainer = DarkCharcoal,
    secondary = SkyBlue,
    onSecondary = CreamWhite,
    secondaryContainer = MintGreen,
    onSecondaryContainer = DarkCharcoal,
    tertiary = PurplePop,
    onTertiary = CreamWhite,
    tertiaryContainer = OrangeJuice,
    onTertiaryContainer = DarkCharcoal,
    background = CreamWhite,
    onBackground = DarkCharcoal,
    surface = CardSurface,
    onSurface = DarkCharcoal,
    surfaceVariant = SunshineYellow.copy(alpha = 0.25f),
    onSurfaceVariant = DarkCharcoal,
    error = CoralPink,
    onError = CreamWhite
)

private val DarkColors = darkColorScheme(
    primary = SunshineYellow,
    onPrimary = DarkCharcoal,
    primaryContainer = CoralPink,
    onPrimaryContainer = CreamWhite,
    secondary = SkyBlue,
    onSecondary = DarkCharcoal,
    secondaryContainer = MintGreen,
    onSecondaryContainer = DarkCharcoal,
    tertiary = OrangeJuice,
    onTertiary = DarkCharcoal,
    tertiaryContainer = PurplePop,
    onTertiaryContainer = CreamWhite,
    background = DarkSurface,
    onBackground = CreamWhite,
    surface = DarkCard,
    onSurface = CreamWhite,
    surfaceVariant = DarkCard,
    onSurfaceVariant = CreamWhite.copy(alpha = 0.8f),
    error = CoralPink,
    onError = DarkCharcoal
)

private val SkidlShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun SkidlTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = SkidlTypography,
        shapes = SkidlShapes,
        content = content
    )
}
