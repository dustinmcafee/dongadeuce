package com.dustinmcafee.dongadeuce

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
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

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    MainScreen()
                }
            }
        }
    }
}
