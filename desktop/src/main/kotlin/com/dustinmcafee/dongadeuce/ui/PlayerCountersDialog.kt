package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.GameConstants
import com.dustinmcafee.dongadeuce.models.Player

/**
 * Dialog for managing player counters (poison, energy, experience, custom)
 */
@Composable
fun PlayerCountersDialog(
    player: Player,
    onDismiss: () -> Unit,
    onAddCounter: (String, Int) -> Unit,
    onRemoveCounter: (String, Int) -> Unit,
    onSetCounter: (String, Int) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Poison", "Energy", "Experience", "Custom")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${player.name}'s Counters") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab row
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Counter content based on selected tab
                when (selectedTab) {
                    0 -> PoisonCounterSection(
                        currentCount = player.getCounter("poison"),
                        onAdd = { onAddCounter("poison", it) },
                        onRemove = { onRemoveCounter("poison", it) },
                        onSet = { onSetCounter("poison", it) }
                    )
                    1 -> GenericCounterSection(
                        counterType = "Energy",
                        currentCount = player.getCounter("energy"),
                        onAdd = { onAddCounter("energy", it) },
                        onRemove = { onRemoveCounter("energy", it) },
                        onSet = { onSetCounter("energy", it) }
                    )
                    2 -> GenericCounterSection(
                        counterType = "Experience",
                        currentCount = player.getCounter("experience"),
                        onAdd = { onAddCounter("experience", it) },
                        onRemove = { onRemoveCounter("experience", it) },
                        onSet = { onSetCounter("experience", it) }
                    )
                    3 -> CustomCounterSection(
                        player = player,
                        onAdd = onAddCounter,
                        onRemove = onRemoveCounter
                    )
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

@Composable
private fun PoisonCounterSection(
    currentCount: Int,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onSet: (Int) -> Unit
) {
    val isLethal = currentCount >= GameConstants.POISON_THRESHOLD

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Warning for lethal poison
        if (isLethal) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "LETHAL POISON! (${GameConstants.POISON_THRESHOLD}+ = loss)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Current count display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isLethal) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Poison Counters",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "$currentCount",
                    style = MaterialTheme.typography.displayMedium,
                    color = if (isLethal) Color(0xFFB71C1C) else MaterialTheme.colorScheme.primary
                )
                Text(
                    "/ ${GameConstants.POISON_THRESHOLD} lethal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick +/- buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onRemove(1) },
                modifier = Modifier.weight(1f),
                enabled = currentCount > 0
            ) {
                Text("-1")
            }
            Button(
                onClick = { onAdd(1) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+1")
            }
        }

        // Advanced controls
        CounterAmountControls(
            currentCount = currentCount,
            onAdd = onAdd,
            onRemove = onRemove,
            onSet = onSet
        )
    }
}

@Composable
private fun GenericCounterSection(
    counterType: String,
    currentCount: Int,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onSet: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Current count display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "$counterType Counters",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "$currentCount",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Quick +/- buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onRemove(1) },
                modifier = Modifier.weight(1f),
                enabled = currentCount > 0
            ) {
                Text("-1")
            }
            Button(
                onClick = { onAdd(1) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+1")
            }
        }

        // Advanced controls
        CounterAmountControls(
            currentCount = currentCount,
            onAdd = onAdd,
            onRemove = onRemove,
            onSet = onSet
        )
    }
}

@Composable
private fun CustomCounterSection(
    player: Player,
    onAdd: (String, Int) -> Unit,
    onRemove: (String, Int) -> Unit
) {
    var customCounterName by remember { mutableStateOf("") }
    var customAmount by remember { mutableStateOf("1") }

    // Get all non-standard counters
    val customCounters = player.counters.filter { (key, _) ->
        key !in listOf("poison", "energy", "experience")
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Create new custom counter
        OutlinedTextField(
            value = customCounterName,
            onValueChange = { customCounterName = it },
            label = { Text("Custom Counter Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customAmount,
                onValueChange = {
                    if (it.isEmpty() || it.toIntOrNull() != null) {
                        customAmount = it
                    }
                },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Button(
                onClick = {
                    val amount = customAmount.toIntOrNull() ?: 1
                    if (customCounterName.isNotBlank() && amount > 0) {
                        onAdd(customCounterName.lowercase(), amount)
                        customCounterName = ""
                        customAmount = "1"
                    }
                },
                enabled = customCounterName.isNotBlank() && (customAmount.toIntOrNull() ?: 0) > 0
            ) {
                Text("Add")
            }
        }

        Divider()

        // Existing custom counters
        if (customCounters.isEmpty()) {
            Text(
                "No custom counters yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "Current Custom Counters:",
                style = MaterialTheme.typography.titleSmall
            )

            customCounters.forEach { (name, count) ->
                CustomCounterRow(
                    name = name,
                    count = count,
                    onAdd = { onAdd(name, 1) },
                    onRemove = { onRemove(name, 1) }
                )
            }
        }
    }
}

@Composable
private fun CustomCounterRow(
    name: String,
    count: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp),
                enabled = count > 0
            ) {
                Text("-", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(
                onClick = onAdd,
                modifier = Modifier.size(32.dp)
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun CounterAmountControls(
    currentCount: Int,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onSet: (Int) -> Unit
) {
    var amountInput by remember { mutableStateOf("1") }

    Divider()

    Text(
        "Advanced",
        style = MaterialTheme.typography.titleSmall
    )

    OutlinedTextField(
        value = amountInput,
        onValueChange = {
            if (it.isEmpty() || it.toIntOrNull() != null) {
                amountInput = it
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
        OutlinedButton(
            onClick = {
                val amount = amountInput.toIntOrNull()
                if (amount != null && amount > 0) {
                    onRemove(amount)
                }
            },
            modifier = Modifier.weight(1f),
            enabled = (amountInput.toIntOrNull() ?: 0) > 0 && currentCount > 0
        ) {
            Text("Remove")
        }

        OutlinedButton(
            onClick = {
                val amount = amountInput.toIntOrNull()
                if (amount != null && amount > 0) {
                    onAdd(amount)
                }
            },
            modifier = Modifier.weight(1f),
            enabled = (amountInput.toIntOrNull() ?: 0) > 0
        ) {
            Text("Add")
        }
    }

    Button(
        onClick = {
            val amount = amountInput.toIntOrNull()
            if (amount != null && amount >= 0) {
                onSet(amount)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = (amountInput.toIntOrNull() ?: -1) >= 0
    ) {
        Text("Set To")
    }
}
