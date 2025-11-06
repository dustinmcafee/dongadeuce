package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CounterDialog(
    cardName: String,
    counterType: String,
    currentCount: Int,
    onDismiss: () -> Unit,
    onSet: (Int) -> Unit,
    onAdd: (Int) -> Unit,
    onSubtract: (Int) -> Unit
) {
    var countInput by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage $counterType Counters on $cardName") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Current: $currentCount $counterType counter(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Divider()

                // Amount input
                OutlinedTextField(
                    value = countInput,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            countInput = it
                        }
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Divider()

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val count = countInput.toIntOrNull()
                            if (count != null && count > 0) {
                                onAdd(count)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = countInput.toIntOrNull()?.let { it > 0 } == true
                    ) {
                        Text("Add")
                    }

                    Button(
                        onClick = {
                            val count = countInput.toIntOrNull()
                            if (count != null && count > 0) {
                                onSubtract(count)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = countInput.toIntOrNull()?.let { it > 0 } == true
                    ) {
                        Text("Subtract")
                    }
                }

                Button(
                    onClick = {
                        val count = countInput.toIntOrNull()
                        if (count != null && count >= 0) {
                            onSet(count)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = countInput.toIntOrNull()?.let { it >= 0 } == true
                ) {
                    Text("Set To")
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
