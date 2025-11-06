package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AnnotationDialog(
    cardName: String,
    currentAnnotation: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var annotationInput by remember { mutableStateOf(currentAnnotation ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Annotation: $cardName") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!currentAnnotation.isNullOrBlank()) {
                    Text(
                        text = "Current: $currentAnnotation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = annotationInput,
                    onValueChange = { annotationInput = it },
                    label = { Text("Annotation text") },
                    placeholder = { Text("Enter note...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Leave blank to remove annotation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val annotation = annotationInput.trim().ifBlank { null }
                    onConfirm(annotation)
                    onDismiss()
                }
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
