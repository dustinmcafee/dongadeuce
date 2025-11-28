package com.dustinmcafee.dongadeuce

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.dustinmcafee.dongadeuce.ui.MainScreen

// Global key event handler that can be set by GameScreen
var globalKeyEventHandler: ((KeyEvent) -> Boolean)? = null

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Dong-A-Deuce",
        state = rememberWindowState(),
        onPreviewKeyEvent = { keyEvent ->
            // Forward to global handler if set
            globalKeyEventHandler?.invoke(keyEvent) ?: false
        }
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            MainScreen()
        }
    }
}
