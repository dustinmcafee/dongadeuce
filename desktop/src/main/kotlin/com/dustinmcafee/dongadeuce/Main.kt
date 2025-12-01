package com.dustinmcafee.dongadeuce

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.dustinmcafee.dongadeuce.settings.UserSettings
import com.dustinmcafee.dongadeuce.ui.MainScreen

// Global key event handler that can be set by GameScreen
var globalKeyEventHandler: ((KeyEvent) -> Boolean)? = null

// Global UI scale state that can be updated from SettingsDialog
object UiScaleState {
    var scale by mutableStateOf(1.0f)
}

fun main() {
    // Load UI scale from settings
    val userSettings = UserSettings()
    val initialScale = userSettings.getUiScale()
    UiScaleState.scale = initialScale

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Dong-A-Deuce",
            state = rememberWindowState(),
            onPreviewKeyEvent = { keyEvent ->
                // Forward to global handler if set
                globalKeyEventHandler?.invoke(keyEvent) ?: false
            }
        ) {
            val baseDensity = LocalDensity.current
            val scaledDensity = Density(
                density = baseDensity.density * UiScaleState.scale,
                fontScale = baseDensity.fontScale * UiScaleState.scale
            )

            // Custom dark color scheme with better contrast
            val customDarkColors = darkColorScheme(
                primary = Color(0xFF90CAF9),           // Light blue
                onPrimary = Color.Black,
                primaryContainer = Color(0xFF1565C0), // Dark blue
                onPrimaryContainer = Color.White,
                secondary = Color(0xFFA5D6A7),        // Light green
                onSecondary = Color.Black,
                secondaryContainer = Color(0xFF2E7D32), // Dark green
                onSecondaryContainer = Color.White,
                background = Color(0xFF121212),       // Very dark grey
                onBackground = Color.White,
                surface = Color(0xFF1E1E1E),          // Dark grey
                onSurface = Color.White,
                surfaceVariant = Color(0xFF2D2D2D),   // Slightly lighter dark grey
                onSurfaceVariant = Color(0xFFE0E0E0), // Light grey text
                outline = Color(0xFF757575),
                error = Color(0xFFCF6679),
                onError = Color.Black
            )

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                MaterialTheme(colorScheme = customDarkColors) {
                    MainScreen()
                }
            }
        }
    }
}
