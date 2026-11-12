package com.example.ircamapa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF00668B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC3E8FF),
    onPrimaryContainer = Color(0xFF001E2D),
    secondary = Color(0xFF4E616D),
    onSecondary = Color.White,
    surface = Color(0xFFFBFCFE),
    onSurface = Color(0xFF191C1E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CD0FF),
    onPrimary = Color(0xFF00344A),
    primaryContainer = Color(0xFF004C6A),
    onPrimaryContainer = Color(0xFFC3E8FF),
    secondary = Color(0xFFB5C9D7),
    onSecondary = Color(0xFF20333E),
    surface = Color(0xFF191C1E),
    onSurface = Color(0xFFE1E2E5)
)

@Composable
fun IrcaMapaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
