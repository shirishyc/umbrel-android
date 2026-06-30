package com.umbrel.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = UmbrelAccent,
    onPrimary = UmbrelTextPrimary,
    primaryContainer = UmbrelAccentContainer,
    onPrimaryContainer = UmbrelAccent,
    secondary = UmbrelAccentDark,
    onSecondary = UmbrelTextPrimary,
    tertiary = UmbrelWarning,
    background = UmbrelDarkBackground,
    onBackground = UmbrelTextPrimary,
    surface = UmbrelDarkSurface,
    onSurface = UmbrelTextPrimary,
    surfaceVariant = UmbrelDarkSurfaceVariant,
    onSurfaceVariant = UmbrelTextSecondary,
    outline = UmbrelBorder,
    error = UmbrelError,
    onError = UmbrelTextPrimary,
)

@Composable
fun UmbrelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
