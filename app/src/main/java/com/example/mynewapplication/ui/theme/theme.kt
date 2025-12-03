package com.example.mynewapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = PrimaryPurple,
    error = ErrorRed,
    background = DarkBackground,
    surface = DarkSurface
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBlue,
    secondary = PrimaryPurple,
    error = ErrorRed,
    background = DarkBackground,
    surface = DarkSurface
)

@Composable
fun LguinahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography, // <- use AppTypography
        shapes = AppShapes,         // <- use AppShapes
        content = content
    )
}
