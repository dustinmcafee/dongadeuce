package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun PowerToughnessDialog(
    cardName: String,
    basePower: String?,
    baseToughness: String?,
    currentPowerMod: Int,
    currentToughnessMod: Int,
    onDismiss: () -> Unit,
    onModifyPower: (Int) -> Unit,
    onModifyToughness: (Int) -> Unit,
    onModifyBoth: (Int) -> Unit,
    onSetPT: (Int, Int) -> Unit,
    onReset: () -> Unit,
    onFlowP: () -> Unit,
    onFlowT: () -> Unit
) {
    var powerInput by remember { mutableStateOf("1") }
    var toughnessInput by remember { mutableStateOf("1") }
    var setP by remember { mutableStateOf(basePower ?: "0") }
    var setT by remember { mutableStateOf(baseToughness ?: "0") }

    val currentP = (basePower?.toIntOrNull() ?: 0) + currentPowerMod
    val currentT = (baseToughness?.toIntOrNull() ?: 0) + currentToughnessMod

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Power/Toughness: $cardName") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current P/T display
                Text(
                    text = "Current: $currentP/$currentT" +
                          if (currentPowerMod != 0 || currentToughnessMod != 0)
                              " (Base: $basePower/$baseToughness)"
                          else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Divider()

                // Quick adjust buttons
                Text("Quick Adjust:", style = MaterialTheme.typography.titleSmall)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(onClick = { onModifyPower(1) }, modifier = Modifier.weight(1f)) {
                        Text("+P")
                    }
                    Button(onClick = { onModifyPower(-1) }, modifier = Modifier.weight(1f)) {
                        Text("-P")
                    }
                    Button(onClick = { onModifyToughness(1) }, modifier = Modifier.weight(1f)) {
                        Text("+T")
                    }
                    Button(onClick = { onModifyToughness(-1) }, modifier = Modifier.weight(1f)) {
                        Text("-T")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(onClick = { onModifyBoth(1) }, modifier = Modifier.weight(1f)) {
                        Text("+1/+1")
                    }
                    Button(onClick = { onModifyBoth(-1) }, modifier = Modifier.weight(1f)) {
                        Text("-1/-1")
                    }
                    Button(onClick = onFlowP, modifier = Modifier.weight(1f)) {
                        Text("Flow P")
                    }
                    Button(onClick = onFlowT, modifier = Modifier.weight(1f)) {
                        Text("Flow T")
                    }
                }

                Divider()

                // Custom amount
                Text("Modify by Amount:", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value = powerInput,
                    onValueChange = {
                        if (it.isEmpty() || it == "-" || it.toIntOrNull() != null) {
                            powerInput = it
                        }
                    },
                    label = { Text("Amount") },
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
                            powerInput.toIntOrNull()?.let { amount ->
                                onModifyPower(amount)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = powerInput.toIntOrNull() != null
                    ) {
                        Text("Add to Power")
                    }
                    Button(
                        onClick = {
                            powerInput.toIntOrNull()?.let { amount ->
                                onModifyToughness(amount)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = powerInput.toIntOrNull() != null
                    ) {
                        Text("Add to Tough")
                    }
                }

                Divider()

                // Set P/T
                Text("Set to Specific Value:", style = MaterialTheme.typography.titleSmall)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = setP,
                        onValueChange = {
                            if (it.isEmpty() || it == "-" || it.toIntOrNull() != null) {
                                setP = it
                            }
                        },
                        label = { Text("Power") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = setT,
                        onValueChange = {
                            if (it.isEmpty() || it == "-" || it.toIntOrNull() != null) {
                                setT = it
                            }
                        },
                        label = { Text("Toughness") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Button(
                    onClick = {
                        val p = setP.toIntOrNull()
                        val t = setT.toIntOrNull()
                        if (p != null && t != null) {
                            onSetPT(p, t)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = setP.toIntOrNull() != null && setT.toIntOrNull() != null
                ) {
                    Text("Set P/T")
                }

                Button(
                    onClick = {
                        onReset()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset to Base")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
