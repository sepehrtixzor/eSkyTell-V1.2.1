package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ObsidianDarkScheme = darkColorScheme(
    primary = MintJade,
    onPrimary = Color(0xFF00241C),
    primaryContainer = Color(0xFF0D3D33),
    onPrimaryContainer = MintJadeBright,
    secondary = ChampagneGold,
    onSecondary = Color(0xFF2B1C00),
    secondaryContainer = Color(0xFF3A2C12),
    onSecondaryContainer = ChampagneSoft,
    tertiary = EmeraldSuccess,
    onTertiary = Color(0xFF022C22),
    background = ObsidianBg,
    onBackground = TextObsidianPrimary,
    surface = ObsidianSurface,
    onSurface = TextObsidianPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextObsidianSecondary,
    outline = ObsidianBorder,
    error = RoseError,
    onError = Color(0xFF2E0404)
)

private val JadeLightScheme = lightColorScheme(
    primary = LightJadePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF00382B),
    secondary = Color(0xFFB4690E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    background = LightJadeBg,
    onBackground = TextLightPrimary,
    surface = LightJadeSurface,
    onSurface = TextLightPrimary,
    surfaceVariant = LightJadeSurfaceVariant,
    onSurfaceVariant = TextLightSecondary,
    outline = Color(0xFFC4DBD3),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ESkyTellTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ObsidianDarkScheme else JadeLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
