package com.fittrack.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors that aren't covered by Material 3's role-based scheme.
 * Used for trend/recommendation accents in insights and suggestion cards.
 * Light/dark variants are tuned for contrast on each surface.
 */
data class FitTrackColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
)

val LightFitTrackColors = FitTrackColors(
    success = SuccessLight,
    warning = WarningLight,
    danger = DangerLight,
)

val DarkFitTrackColors = FitTrackColors(
    success = SuccessDark,
    warning = WarningDark,
    danger = DangerDark,
)

val LocalFitTrackColors = staticCompositionLocalOf { LightFitTrackColors }

object FitTrackTheme {
    val colors: FitTrackColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFitTrackColors.current
}
