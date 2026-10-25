package com.fittrack.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// FitTrack palette — dark-first athletic.
//
// Dark is the showcase: near-black canvas, charcoal tiles with hairline
// borders, white numerals, and a single electric accent. Light keeps the
// same accent family at a darker tone so contrast holds on white.
// ---------------------------------------------------------------------------

// Accent family
val Volt = Color(0xFFC6FF4A)          // electric lime, dark-mode primary
val VoltDeep = Color(0xFF9BE22E)      // gradient end
val VoltInk = Color(0xFF0B1A00)       // text on Volt
val VoltContainerDark = Color(0xFF2E3F0E)
val VoltOnContainerDark = Color(0xFFD9FF8A)

val Forest = Color(0xFF1F7A1F)        // light-mode primary (same hue family, darker)
val ForestContainer = Color(0xFFD7FFB0)
val ForestOnContainer = Color(0xFF0B2A00)

// Secondary — cool blue for informational accents
val Sky = Color(0xFF7FB3FF)
val SkyInk = Color(0xFF00285C)
val SkyContainerDark = Color(0xFF1C3458)
val SkyOnContainerDark = Color(0xFFD4E4FF)
val SkyLight = Color(0xFF2F6FDB)
val SkyContainerLight = Color(0xFFDCE8FF)
val SkyOnContainerLight = Color(0xFF0A2B66)

// Tertiary — coral for upgrade prompts, missed days, destructive hints
val Coral = Color(0xFFFF8A73)
val CoralInk = Color(0xFF4A0E00)
val CoralContainerDark = Color(0xFF5C1F10)
val CoralOnContainerDark = Color(0xFFFFDAD1)
val CoralLight = Color(0xFFD9452B)
val CoralContainerLight = Color(0xFFFFDAD3)
val CoralOnContainerLight = Color(0xFF410002)

// Dark canvas
val DarkBackground = Color(0xFF0B0C10)
val DarkOnBackground = Color(0xFFF2F3F7)
val DarkSurface = Color(0xFF0B0C10)
val DarkOnSurface = Color(0xFFF2F3F7)
val DarkSurfaceVariant = Color(0xFF23252E)
val DarkOnSurfaceVariant = Color(0xFFA2A6B5)
val DarkOutline = Color(0xFF4A4D5A)
val DarkOutlineVariant = Color(0xFF262833)   // hairline tile border
val DarkSurfaceContainerLowest = Color(0xFF060709)
val DarkSurfaceContainerLow = Color(0xFF15161C) // tiles
val DarkSurfaceContainer = Color(0xFF1A1B22)    // nav bar, sheets
val DarkSurfaceContainerHigh = Color(0xFF21222B) // inputs, unselected pills
val DarkSurfaceContainerHighest = Color(0xFF15161C) // default Card

// Light canvas
val LightBackground = Color(0xFFF4F5F8)
val LightOnBackground = Color(0xFF15161C)
val LightSurface = Color(0xFFF4F5F8)
val LightOnSurface = Color(0xFF15161C)
val LightSurfaceVariant = Color(0xFFE2E4EB)
val LightOnSurfaceVariant = Color(0xFF5B5F6E)
val LightOutline = Color(0xFF8A8E9C)
val LightOutlineVariant = Color(0xFFE3E5EC)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFFFFFF)
val LightSurfaceContainer = Color(0xFFEBEDF2)
val LightSurfaceContainerHigh = Color(0xFFE4E6EC)
val LightSurfaceContainerHighest = Color(0xFFFFFFFF)

// Semantic accent colors — accessed via FitTrackTheme.colors, not directly.
val SuccessLight = Color(0xFF1E8E3E)
val WarningLight = Color(0xFFE8710A)
val DangerLight = Color(0xFFD93025)

val SuccessDark = Color(0xFF6DD58C)
val WarningDark = Color(0xFFFFB55A)
val DangerDark = Color(0xFFFF8A80)

// Gradient stops for hero surfaces. Text on them uses VoltInk.
val HeroGradientStart = Volt
val HeroGradientEnd = VoltDeep

// Legacy aliases kept so existing call sites that tint icons directly keep
// compiling. Prefer MaterialTheme.colorScheme roles for new code.
val Blue40 = SkyLight
val Teal40 = Color(0xFF0D9488)
val Blue80 = Sky
val BlueGrey40 = Color(0xFF5B5F6E)
val BlueGrey80 = Color(0xFFA2A6B5)
val Teal80 = Color(0xFF5EEAD4)
