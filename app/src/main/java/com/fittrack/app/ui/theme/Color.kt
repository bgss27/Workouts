package com.fittrack.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// FitTrack brand palette
//
// Primary   — electric indigo: energetic, reads well on both light and dark.
// Secondary — slate: quiet supporting tone for chips, secondary containers.
// Tertiary  — coral: warm accent for upgrade prompts and highlights so the
//             app doesn't feel monochrome blue.
// ---------------------------------------------------------------------------

// Light scheme
val IndigoPrimary = Color(0xFF4F46E5)
val IndigoOnPrimary = Color(0xFFFFFFFF)
val IndigoPrimaryContainer = Color(0xFFE0E0FF)
val IndigoOnPrimaryContainer = Color(0xFF1A1266)

val SlateSecondary = Color(0xFF5B6478)
val SlateOnSecondary = Color(0xFFFFFFFF)
val SlateSecondaryContainer = Color(0xFFE1E6F2)
val SlateOnSecondaryContainer = Color(0xFF1B2232)

val CoralTertiary = Color(0xFFE2543E)
val CoralOnTertiary = Color(0xFFFFFFFF)
val CoralTertiaryContainer = Color(0xFFFFDAD3)
val CoralOnTertiaryContainer = Color(0xFF410002)

val LightBackground = Color(0xFFF3F3FA)
val LightOnBackground = Color(0xFF1B1B22)
// Surface matches background so app bars, list rows and sheets blend in
// instead of drawing a seam; cards use the container roles below.
val LightSurface = Color(0xFFF3F3FA)
val LightOnSurface = Color(0xFF1B1B22)
val LightSurfaceVariant = Color(0xFFE4E3F0)
val LightOnSurfaceVariant = Color(0xFF474656)
val LightOutline = Color(0xFF787687)
val LightOutlineVariant = Color(0xFFC8C6D6)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFFFFFF)
val LightSurfaceContainer = Color(0xFFEAEAF3)
val LightSurfaceContainerHigh = Color(0xFFE3E3EE)
// Default Card color. White on the tinted background reads as a clean,
// elevated surface rather than a grey slab.
val LightSurfaceContainerHighest = Color(0xFFFFFFFF)

// Dark scheme
val IndigoPrimaryDark = Color(0xFFBFC2FF)
val IndigoOnPrimaryDark = Color(0xFF1E1B8F)
val IndigoPrimaryContainerDark = Color(0xFF3730A3)
val IndigoOnPrimaryContainerDark = Color(0xFFE0E0FF)

val SlateSecondaryDark = Color(0xFFC3CAD9)
val SlateOnSecondaryDark = Color(0xFF2D3446)
val SlateSecondaryContainerDark = Color(0xFF434B5D)
val SlateOnSecondaryContainerDark = Color(0xFFE1E6F2)

val CoralTertiaryDark = Color(0xFFFFB4A6)
val CoralOnTertiaryDark = Color(0xFF5F1500)
val CoralTertiaryContainerDark = Color(0xFF862313)
val CoralOnTertiaryContainerDark = Color(0xFFFFDAD3)

val DarkBackground = Color(0xFF0F1017)
val DarkOnBackground = Color(0xFFE4E1EC)
val DarkSurface = Color(0xFF0F1017)
val DarkOnSurface = Color(0xFFE4E1EC)
val DarkSurfaceVariant = Color(0xFF474656)
val DarkOnSurfaceVariant = Color(0xFFC8C6D6)
val DarkOutline = Color(0xFF928FA0)
val DarkOutlineVariant = Color(0xFF474656)
val DarkSurfaceContainerLowest = Color(0xFF0A0B12)
val DarkSurfaceContainerLow = Color(0xFF1B1C28)
val DarkSurfaceContainer = Color(0xFF20212E)
val DarkSurfaceContainerHigh = Color(0xFF2A2B39)
val DarkSurfaceContainerHighest = Color(0xFF353645)

// Semantic accent colors — accessed via FitTrackTheme.colors, not directly.
// Light variants are darker for contrast on white; dark variants are lighter
// so they don't burn out on a dark surface.
val SuccessLight = Color(0xFF1E8E3E)
val WarningLight = Color(0xFFE8710A)
val DangerLight = Color(0xFFD93025)

val SuccessDark = Color(0xFF6DD58C)
val WarningDark = Color(0xFFFFB55A)
val DangerDark = Color(0xFFFF8A80)

// Gradient stops for hero surfaces (stats card, paywall header).
val HeroGradientStart = Color(0xFF4F46E5)
val HeroGradientEnd = Color(0xFF7C3AED)

// Legacy aliases kept so existing call sites that tint icons directly keep
// compiling. Prefer MaterialTheme.colorScheme roles for new code.
val Blue40 = IndigoPrimary
val Teal40 = Color(0xFF0D9488)
val Blue80 = IndigoPrimaryDark
val BlueGrey40 = SlateSecondary
val BlueGrey80 = SlateSecondaryDark
val Teal80 = Color(0xFF5EEAD4)
