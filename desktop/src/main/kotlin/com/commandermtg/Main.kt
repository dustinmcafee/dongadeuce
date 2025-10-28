package com.commandermtg

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.commandermtg.ui.MainScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Commander MTG",
        state = rememberWindowState()
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            MainScreen()
        }
    }
}
