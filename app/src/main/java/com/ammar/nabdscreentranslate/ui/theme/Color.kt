package com.ammar.nabdscreentranslate.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Nabd Screen Translate — "Ember on Graphite" Design System
 *
 * Identity: privacy-first, on-device translation. Warm ember accent (#FF7000)
 * = the living "pulse" (نبض). Warm graphite surfaces give depth and calm.
 * Cold cyan is demoted to a quiet informational tint only.
 */

// ─── Warm Graphite Backgrounds ──────────────────────────────────────────────
// Slightly warm-tinted near-black for a premium, non-clinical dark feel.
val Ink900 = Color(0xFF0B0A09)  // app background (warm black)
val Ink800 = Color(0xFF131110)  // base surface
val Ink700 = Color(0xFF1B1816)  // raised surface
val Ink600 = Color(0xFF231F1C)  // elevated
val Ink500 = Color(0xFF2C2723)  // highest

// ─── Glass / Card Surfaces (warm graphite) ──────────────────────────────────
val Glass900 = Color(0xFF14110F)
val Glass800 = Color(0xFF1C1815)
val Glass700 = Color(0xFF26211D)
val Glass600 = Color(0xFF332B25)
val GlassBorder = Color(0xFF3A322C)

// ─── Ember Accent (primary — the pulse) ─────────────────────────────────────
val Ember300 = Color(0xFFFF9A4D)  // light ember (hover / glow)
val Ember400 = Color(0xFFFF8526)  // bright ember
val Ember500 = Color(0xFFFF7000)  // PRIMARY brand accent
val Ember600 = Color(0xFFE85D00)  // pressed
val Ember700 = Color(0xFFC74D00)  // deep
val EmberGlow = Color(0x33FF7000) // 20% alpha radial glow
val EmberSoft = Color(0x1AFF7000) // 10% alpha fill

// ─── Secondary warm tone (amber/gold) ───────────────────────────────────────
val Amber400 = Color(0xFFFBBF24)
val Amber500 = Color(0xFFF59E0B)

// ─── Quiet informational cyan (demoted, used sparingly) ─────────────────────
val Cyan400 = Color(0xFF38BDF8)
val Cyan500 = Color(0xFF0EA5E9)
val Cyan600 = Color(0xFF0284C7)
val CyanGlow = Color(0x2238BDF8)

// ─── Semantic ───────────────────────────────────────────────────────────────
val Success400 = Color(0xFF4ADE80)
val Success500 = Color(0xFF22C55E)
val Error400 = Color(0xFFF87171)
val Error500 = Color(0xFFEF4444)
val Warning400 = Color(0xFFFBBF24)

// ─── Text (warm-neutral) ─────────────────────────────────────────────────────
val TextWhite = Color(0xFFFBF7F4)
val TextLight = Color(0xFFE3DBD4)
val TextMuted = Color(0xFFA89B90)
val TextDim = Color(0xFF6F645B)

// ─── Backward-compat aliases (overlay + legacy code) ─────────────────────────
val DarkBackground = Ink900
val DarkSurface = Glass900
val DarkSurfaceVariant = Glass700
val DarkCard = Glass800

// Primary now maps to Ember (was Cyan). Keeps old references working.
val PrimaryBlue = Ember500
val PrimaryPurple = Ember400
val PrimaryGradientStart = Ember600
val PrimaryGradientEnd = Ember400

val AccentGreen = Success400
val AccentOrange = Ember500
val AccentRed = Error400

val TextPrimary = TextWhite
val TextSecondary = TextMuted
val TextTertiary = TextDim

val BorderColor = GlassBorder

// Light theme (app is dark-first; warm light tones for completeness)
val LightBackground = Color(0xFFFBF6F1)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF3ECE4)
val LightCard = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF1A1512)
val LightTextSecondary = Color(0xFF6B5D52)
val LightBorderColor = Color(0xFFE7DCD1)
