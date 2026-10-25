package com.fittrack.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Volt,
    onPrimary = VoltInk,
    primaryContainer = VoltContainerDark,
    onPrimaryContainer = VoltOnContainerDark,
    secondary = Sky,
    onSecondary = SkyInk,
    secondaryContainer = SkyContainerDark,
    onSecondaryContainer = SkyOnContainerDark,
    tertiary = Coral,
    onTertiary = CoralInk,
    tertiaryContainer = CoralContainerDark,
    onTertiaryContainer = CoralOnContainerDark,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    error = DangerDark,
)

private val LightColorScheme = lightColorScheme(
    primary = Forest,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = ForestContainer,
    onPrimaryContainer = ForestOnContainer,
    secondary = SkyLight,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = SkyContainerLight,
    onSecondaryContainer = SkyOnContainerLight,
    tertiary = CoralLight,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = CoralContainerLight,
    onTertiaryContainer = CoralOnContainerLight,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    error = DangerLight,
)

/** Generous radii: tiles 20, sheets/dialogs 28, inputs and pills 14. */
val FitTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** User-selectable appearance. Persisted by [com.fittrack.app.data.ThemePrefs]. */
enum class ThemeMode { SYSTEM, DARK, LIGHT }

/**
 * Dark-first: when [mode] is SYSTEM the app follows the OS, but the default
 * stored preference is DARK so a fresh install shows the showcase theme.
 */
@Composable
fun FitTrackTheme(
    mode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkFitTrackColors else LightFitTrackColors

    CompositionLocalProvider(
        LocalFitTrackColors provides semanticColors,
        LocalIsDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = FitTrackShapes,
            content = content
        )
    }
}
