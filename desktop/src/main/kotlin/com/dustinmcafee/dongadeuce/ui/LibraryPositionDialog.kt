package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun LibraryPositionDialog(
    cardName: String,
    librarySize: Int,
    onDismiss: () -> Unit,
    onToTop: () -> Unit,
    onToBottom: () -> Unit,
    onToPositionFromTop: (Int) -> Unit,
    onToPositionFromBottom: (Int) -> Unit
) {
    var positionInput by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move $cardName to Library") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Library size: $librarySize cards",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Divider()

                // Quick options
                Button(
                    onClick = {
                        onToTop()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("To Top of Library")
                }

                Button(
                    onClick = {
                        onToBottom()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("To Bottom of Library")
                }

                Divider()

                // Position input
                Text(
                    text = "Specific Position",
                    style = MaterialTheme.typography.titleSmall
                )

                OutlinedTextField(
                    value = positionInput,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            positionInput = it
                        }
                    },
                    label = { Text("Position (1 = top)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val position = positionInput.toIntOrNull()
                            if (position != null && position > 0) {
                                onToPositionFromTop(position)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = positionInput.toIntOrNull()?.let { it > 0 } == true
                    ) {
                        Text("X from Top")
                    }

                    Button(
                        onClick = {
                            val position = positionInput.toIntOrNull()
                            if (position != null && position > 0) {
                                onToPositionFromBottom(position)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = positionInput.toIntOrNull()?.let { it > 0 } == true
                    ) {
                        Text("X from Bottom")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
