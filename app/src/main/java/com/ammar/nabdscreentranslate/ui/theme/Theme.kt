package com.ammar.nabdscreentranslate.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Ember500,
    onPrimary = Ink900,
    primaryContainer = Ember700,
    onPrimaryContainer = TextWhite,
    secondary = Amber400,
    onSecondary = Ink900,
    tertiary = Cyan400,
    background = Ink900,
    surface = Glass900,
    surfaceVariant = Glass700,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextMuted,
    error = Error400,
    onError = Ink900,
    outline = GlassBorder,
    outlineVariant = Glass600
)

private val LightColorScheme = lightColorScheme(
    primary = Ember600,
    onPrimary = Color(0xFFFFFFFF),
    secondary = Amber500,
    tertiary = Cyan600,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    error = Error500,
    outline = LightBorderColor
)

@Composable
fun NabdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NabdTypography,
        content = content
    )
}
