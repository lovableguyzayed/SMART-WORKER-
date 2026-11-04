package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Smart Worker ships one scheme: the dark canvas the design system is built on.
// Light mode and dynamic colour are intentionally not offered — the bright
// colour-blocked tiles rely on a dark surface behind them to read correctly.
private val SmartWorkerColors =
  darkColorScheme(
    primary = Flame,
    onPrimary = White,
    primaryContainer = FlameBg,
    onPrimaryContainer = Flame,
    secondary = Volt,
    onSecondary = White,
    secondaryContainer = VoltBg,
    onSecondaryContainer = Volt,
    tertiary = Lime,
    onTertiary = OnBright,
    background = Ink,
    onBackground = OnDark,
    surface = Surface1,
    onSurface = OnDark,
    surfaceVariant = Surface3,
    onSurfaceVariant = OnDarkMuted,
    surfaceContainer = Surface2,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface3,
    outline = Hairline,
    outlineVariant = SubtleDivider,
    error = Coral,
    onError = White,
    errorContainer = DangerBg,
    onErrorContainer = Coral,
    scrim = Scrim,
  )

@Composable
fun SmartWorkerTheme(
  @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
  @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = SmartWorkerColors, typography = Typography, content = content)
}
