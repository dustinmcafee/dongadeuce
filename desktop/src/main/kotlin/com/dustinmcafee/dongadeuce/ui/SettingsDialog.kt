package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.settings.UserSettings
import javax.swing.JFileChooser

/**
 * Settings dialog for configuring application preferences.
 * Only includes functional settings that actually affect behavior.
 */
@Composable
fun SettingsDialog(
    userSettings: UserSettings,
    currentPlayerName: String,
    currentServerAddress: String,
    currentServerPort: Int,
    onPlayerNameChange: (String) -> Unit,
    onServerAddressChange: (String) -> Unit,
    onServerPortChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var playerName by remember { mutableStateOf(currentPlayerName) }
    var serverAddress by remember { mutableStateOf(currentServerAddress) }
    var serverPort by remember { mutableStateOf(currentServerPort.toString()) }
    var defaultDeckDir by remember { mutableStateOf(userSettings.getLastDeckDirectory() ?: "") }
    var uiScale by remember { mutableStateOf(userSettings.getUiScale()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                modifier = Modifier.width(350.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Player Settings Section
                Text(
                    "Player",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it },
                    label = { Text("Player Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()

                // Network Settings Section
                Text(
                    "Network",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = serverAddress,
                    onValueChange = { serverAddress = it },
                    label = { Text("Default Server Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = serverPort,
                    onValueChange = { newValue ->
                        // Only allow digits
                        serverPort = newValue.filter { it.isDigit() }
                    },
                    label = { Text("Default Server Port") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Valid range: 1024-65535") }
                )

                Divider()

                // File Settings Section
                Text(
                    "Files",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = defaultDeckDir,
                        onValueChange = { defaultDeckDir = it },
                        label = { Text("Default Deck Directory") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        readOnly = true
                    )

                    OutlinedButton(
                        onClick = {
                            val fileChooser = JFileChooser().apply {
                                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                if (defaultDeckDir.isNotBlank()) {
                                    currentDirectory = java.io.File(defaultDeckDir)
                                }
                            }
                            val result = fileChooser.showOpenDialog(null)
                            if (result == JFileChooser.APPROVE_OPTION) {
                                defaultDeckDir = fileChooser.selectedFile.absolutePath
                            }
                        }
                    ) {
                        Text("Browse")
                    }
                }

                if (defaultDeckDir.isBlank()) {
                    Text(
                        "No default directory set. File picker will open to last used location.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Divider()

                // Display Settings Section
                Text(
                    "Display",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    "UI Scale: ${(uiScale * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = uiScale,
                    onValueChange = { uiScale = it },
                    valueRange = 0.5f..2.0f,
                    steps = 5,  // 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("50%", style = MaterialTheme.typography.labelSmall)
                    Text("200%", style = MaterialTheme.typography.labelSmall)
                }

                Text(
                    "Adjust if UI elements are too large or small on your display. Requires restart.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Save all settings
                    onPlayerNameChange(playerName)
                    onServerAddressChange(serverAddress)
                    val port = serverPort.toIntOrNull()?.coerceIn(1024, 65535) ?: 8080
                    onServerPortChange(port)
                    userSettings.setLastDeckDirectory(defaultDeckDir.ifBlank { null })
                    userSettings.setUiScale(uiScale)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
