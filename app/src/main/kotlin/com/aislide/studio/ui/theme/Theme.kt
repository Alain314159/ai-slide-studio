package com.aislide.studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Dark scheme — primary theme (this is a creative-editor app, dark is the default)
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = OnPrimary,
    secondaryContainer = SecondaryDark,
    tertiary = Tertiary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = SurfaceTint,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
    onError = OnError
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = OnPrimary,
    tertiary = Tertiary,
    background = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1A1D26),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1A1D26),
    error = Error,
    onError = OnError
)

/**
 * App theme. Default to dark theme (creative app, personal use).
 */
@Composable
fun AiSlideStudioTheme(
    darkTheme: Boolean = true, // App defaults to dark, ignore system for now
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
