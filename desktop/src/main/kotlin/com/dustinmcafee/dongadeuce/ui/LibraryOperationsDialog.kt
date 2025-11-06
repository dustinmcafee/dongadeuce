package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.Zone

@Composable
fun LibraryOperationsDialog(
    playerName: String,
    librarySize: Int,
    onDismiss: () -> Unit,
    onViewTopCards: (Int) -> Unit,
    onViewBottomCards: (Int) -> Unit,
    onShuffleTopCards: (Int) -> Unit,
    onShuffleBottomCards: (Int) -> Unit,
    onMoveTopToZone: (Int, Zone) -> Unit,
    onMoveBottomToZone: (Int, Zone) -> Unit,
    onRevealTopCard: () -> Unit,
    onFullSearch: () -> Unit
) {
    var cardCount by remember { mutableStateOf("1") }
    var selectedZone by remember { mutableStateOf(Zone.HAND) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$playerName's Library Operations") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Library size indicator
                Text(
                    text = "Library: $librarySize card(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Full search button
                Button(
                    onClick = onFullSearch,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = librarySize > 0
                ) {
                    Text("Full Library Search")
                }

                Divider()

                // Card count input
                OutlinedTextField(
                    value = cardCount,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            cardCount = it
                        }
                    },
                    label = { Text("Number of cards") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Divider()

                // VIEW OPERATIONS
                Text(
                    text = "View Cards (Private)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val count = cardCount.toIntOrNull() ?: 1
                            onViewTopCards(count.coerceIn(1, librarySize))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = librarySize > 0
                    ) {
                        Text("View Top N")
                    }

                    Button(
                        onClick = {
                            val count = cardCount.toIntOrNull() ?: 1
                            onViewBottomCards(count.coerceIn(1, librarySize))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = librarySize > 0
                    ) {
                        Text("View Bottom N")
                    }
                }

                Divider()

                // REVEAL OPERATIONS
                Text(
                    text = "Reveal (All Players See)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedButton(
                    onClick = onRevealTopCard,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = librarySize > 0
                ) {
                    Text("Reveal Top Card to All")
                }

                Divider()

                // SHUFFLE OPERATIONS
                Text(
                    text = "Shuffle Partial Library",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val count = cardCount.toIntOrNull() ?: 1
                            onShuffleTopCards(count.coerceIn(1, librarySize))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = librarySize > 1
                    ) {
                        Text("Shuffle Top N")
                    }

                    OutlinedButton(
                        onClick = {
                            val count = cardCount.toIntOrNull() ?: 1
                            onShuffleBottomCards(count.coerceIn(1, librarySize))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = librarySize > 1
                    ) {
                        Text("Shuffle Bottom N")
                    }
                }

                Divider()

                // MOVE OPERATIONS
                Text(
                    text = "Move Cards to Zone",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Zone selector
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Target Zone:",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ZoneButton(
                            text = "Hand",
                            isSelected = selectedZone == Zone.HAND,
                            onClick = { selectedZone = Zone.HAND },
                            modifier = Modifier.weight(1f)
                        )
                        ZoneButton(
                            text = "Battlefield",
                            isSelected = selectedZone == Zone.BATTLEFIELD,
                            onClick = { selectedZone = Zone.BATTLEFIELD },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ZoneButton(
                            text = "Graveyard",
                            isSelected = selectedZone == Zone.GRAVEYARD,
                            onClick = { selectedZone = Zone.GRAVEYARD },
                            modifier = Modifier.weight(1f)
                        )
                        ZoneButton(
                            text = "Exile",
                            isSelected = selectedZone == Zone.EXILE,
                            onClick = { selectedZone = Zone.EXILE },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Move buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val count = cardCount.toIntOrNull() ?: 1
                            onMoveTopToZone(count.coerceIn(1, librarySize), selectedZone)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = librarySize > 0
                    ) {
                        Text("Move Top N", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = {
                            val count = cardCount.toIntOrNull() ?: 1
                            onMoveBottomToZone(count.coerceIn(1, librarySize), selectedZone)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = librarySize > 0
                    ) {
                        Text("Move Bottom N", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ZoneButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}
