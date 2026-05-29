package com.ammar.nabdscreentranslate.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Nabd Screen Translate — "Dark Liquid Lens" Design System
 * Privacy-first, fast, intelligent local translation tool.
 */

// ─── Core Palette ───────────────────────────────────────────────────────────

// Backgrounds - near black with subtle warmth
val Ink900 = Color(0xFF08090C)
val Ink800 = Color(0xFF0F1115)
val Ink700 = Color(0xFF161920)
val Ink600 = Color(0xFF1D2029)
val Ink500 = Color(0xFF252833)

// Surfaces - dark graphite with glass feel
val Glass900 = Color(0xFF12151C)
val Glass800 = Color(0xFF1A1E28)
val Glass700 = Color(0xFF222733)
val Glass600 = Color(0xFF2A303E)
val GlassBorder = Color(0xFF2E3442)

// Accent - Electric Cyan (primary action color)
val Cyan400 = Color(0xFF22D3EE)
val Cyan500 = Color(0xFF06B6D4)
val Cyan600 = Color(0xFF0891B2)
val CyanGlow = Color(0x3322D3EE) // 20% alpha for glow effects

// Accent - Amber (secondary/warm accent)
val Amber400 = Color(0xFFFBBF24)
val Amber500 = Color(0xFFF59E0B)

// Semantic
val Success400 = Color(0xFF4ADE80)
val Success500 = Color(0xFF22C55E)
val Error400 = Color(0xFFF87171)
val Error500 = Color(0xFFEF4444)
val Warning400 = Color(0xFFFBBF24)

// Text
val TextWhite = Color(0xFFF1F5F9)
val TextLight = Color(0xFFCBD5E1)
val TextMuted = Color(0xFF94A3B8)
val TextDim = Color(0xFF64748B)

// ─── Design Tokens ──────────────────────────────────────────────────────────

// Keep old names as aliases for backward compatibility with overlay code
val DarkBackground = Ink900
val DarkSurface = Glass900
val DarkSurfaceVariant = Glass700
val DarkCard = Glass800

val PrimaryBlue = Cyan400
val PrimaryPurple = Color(0xFF8B5CF6) // kept for compatibility
val PrimaryGradientStart = Cyan500
val PrimaryGradientEnd = Cyan400

val AccentGreen = Success400
val AccentOrange = Amber400
val AccentRed = Error400

val TextPrimary = TextWhite
val TextSecondary = TextMuted
val TextTertiary = TextDim

val BorderColor = GlassBorder

// Light theme (minimal - app is dark-first)
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightCard = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightBorderColor = Color(0xFFE2E8F0)
