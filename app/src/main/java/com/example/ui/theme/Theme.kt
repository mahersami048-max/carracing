package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HighwayDarkColorScheme =
  darkColorScheme(
    primary = RacingCrimson,
    onPrimary = TextPrimary,
    primaryContainer = RacingCrimsonDark,
    onPrimaryContainer = TextPrimary,
    secondary = NitroCyan,
    onSecondary = CarbonBlack,
    secondaryContainer = NitroCyanDark,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberGold,
    onTertiary = CarbonBlack,
    background = CarbonBlack,
    onBackground = TextPrimary,
    surface = AsphaltSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedSlate,
    onSurfaceVariant = TextSecondary,
    outline = PanelBorder,
    error = RacingCrimson,
    onError = TextPrimary,
  )

@Composable
fun HighwayRushTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = HighwayDarkColorScheme,
    typography = HighwayTypography,
    content = content,
  )
}
