package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SetLifeDialog(
    playerName: String,
    currentLife: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var lifeInput by remember { mutableStateOf(currentLife.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Life Total for $playerName") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Current life: $currentLife",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = lifeInput,
                    onValueChange = {
                        if (it.isEmpty() || it == "-" || it.toIntOrNull() != null) {
                            lifeInput = it
                        }
                    },
                    label = { Text("New life total") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newLife = lifeInput.toIntOrNull()
                    if (newLife != null) {
                        onConfirm(newLife)
                        onDismiss()
                    }
                },
                enabled = lifeInput.toIntOrNull() != null
            ) {
                Text("Set Life")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
