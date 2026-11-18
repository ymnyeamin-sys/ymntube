package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val VeloTubeDarkColorScheme = darkColorScheme(
    primary = CrimsonAccent,
    onPrimary = TextPrimary,
    primaryContainer = CrimsonSoft,
    onPrimaryContainer = CrimsonAccent,
    secondary = CyanElectric,
    onSecondary = ObsidianBg,
    secondaryContainer = CyanSoft,
    onSecondaryContainer = CyanElectric,
    tertiary = EmeraldGallery,
    onTertiary = ObsidianBg,
    tertiaryContainer = EmeraldSoft,
    onTertiaryContainer = EmeraldGallery,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SurfaceLevel1,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLevel2,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = SurfaceLevel1,
    surfaceContainer = SurfaceLevel2,
    surfaceContainerHigh = SurfaceLevel3,
    outline = OutlineSubtle,
    outlineVariant = SurfaceLevel3,
    error = CrimsonAccent,
    onError = TextPrimary
)

val VeloTubeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VeloTubeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VeloTubeDarkColorScheme,
        typography = AppTypography,
        shapes = VeloTubeShapes,
        content = content
    )
}
