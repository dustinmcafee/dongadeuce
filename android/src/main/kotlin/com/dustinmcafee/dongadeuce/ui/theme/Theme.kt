package com.dustinmcafee.dongadeuce.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color.Black,
    secondary = Color(0xFFCE93D8),
    onSecondary = Color.Black,
    tertiary = Color(0xFF80CBC4),
    onTertiary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFE0E0E0),
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@Composable
fun DongAdeuceTheme(
    darkTheme: Boolean = true, // Always dark theme for game
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
