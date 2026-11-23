package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AxiomDarkColorScheme = darkColorScheme(
    primary = AxiomGold,
    onPrimary = AxiomObsidian,
    primaryContainer = AxiomGoldSubtleContainer,
    onPrimaryContainer = AxiomGoldChampagne,
    secondary = AxiomGoldChampagne,
    onSecondary = AxiomObsidian,
    secondaryContainer = AxiomSurfaceElevated,
    onSecondaryContainer = AxiomIvory,
    tertiary = AxiomCobalt,
    onTertiary = AxiomIvory,
    background = AxiomObsidian,
    onBackground = AxiomIvory,
    surface = AxiomCharcoal,
    onSurface = AxiomIvory,
    surfaceVariant = AxiomSlate,
    onSurfaceVariant = AxiomSilver,
    surfaceContainerLowest = AxiomObsidian,
    surfaceContainerLow = AxiomCharcoal,
    surfaceContainer = AxiomSlate,
    surfaceContainerHigh = AxiomSurfaceElevated,
    outline = AxiomHairline,
    outlineVariant = AxiomHairline,
    error = AxiomCrimson,
    onError = AxiomIvory,
    errorContainer = AxiomCrimsonBg,
    onErrorContainer = AxiomCrimson,
)

val AxiomShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun AxiomTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AxiomDarkColorScheme,
        typography = AxiomTypography,
        shapes = AxiomShapes,
        content = content,
    )
}
