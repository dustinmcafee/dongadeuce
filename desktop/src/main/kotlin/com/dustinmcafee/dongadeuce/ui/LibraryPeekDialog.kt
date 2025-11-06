package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.CardInstance
import com.dustinmcafee.dongadeuce.models.Zone

@Composable
fun LibraryPeekDialog(
    cards: List<CardInstance>,
    playerName: String,
    peekLocation: PeekLocation,
    onDismiss: () -> Unit,
    onMoveCard: (CardInstance, Zone) -> Unit,
    onMoveAllToZone: (Zone) -> Unit,
    onShuffleCards: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (peekLocation) {
                    PeekLocation.TOP -> "$playerName's Library - Top ${cards.size} card(s)"
                    PeekLocation.BOTTOM -> "$playerName's Library - Bottom ${cards.size} card(s)"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cards.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No cards available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Batch operation buttons
                    Text(
                        text = "Batch Operations:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onMoveAllToZone(Zone.HAND) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("All to Hand", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = { onMoveAllToZone(Zone.GRAVEYARD) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("All to GY", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = { onMoveAllToZone(Zone.EXILE) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("All to Exile", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    OutlinedButton(
                        onClick = onShuffleCards,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Shuffle These ${cards.size} Card(s)", style = MaterialTheme.typography.labelSmall)
                    }

                    Divider()

                    // Card list
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // For TOP cards, reverse so topmost card appears first
                        val displayCards = when (peekLocation) {
                            PeekLocation.TOP -> cards.reversed()
                            PeekLocation.BOTTOM -> cards
                        }

                        displayCards.forEachIndexed { index, cardInstance ->
                            LibraryPeekCard(
                                cardInstance = cardInstance,
                                cardNumber = index + 1, // #1 is always first displayed card
                                onMoveToZone = { zone -> onMoveCard(cardInstance, zone) }
                            )
                        }
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
private fun LibraryPeekCard(
    cardInstance: CardInstance,
    cardNumber: Int,
    onMoveToZone: (Zone) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card image thumbnail
            CardImageThumbnail(
                imageUrl = cardInstance.card.imageUri,
                contentDescription = cardInstance.card.name
            )

            // Card info and actions
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Position badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(40.dp)
                ) {
                    Text(
                        text = "#$cardNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Card name and mana cost
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cardInstance.card.name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    val manaCost = cardInstance.card.manaCost
                    if (manaCost != null && manaCost.isNotEmpty()) {
                        Text(
                            text = manaCost,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Card type
                val cardType = cardInstance.card.type
                if (cardType != null && cardType.isNotEmpty()) {
                    Text(
                        text = cardType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }

                // Power/Toughness for creatures
                val power = cardInstance.card.power
                val toughness = cardInstance.card.toughness
                if (power != null && toughness != null) {
                    Text(
                        text = "$power/$toughness",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // Action buttons - Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { onMoveToZone(Zone.HAND) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("To Hand", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { onMoveToZone(Zone.BATTLEFIELD) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("To Field", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { onMoveToZone(Zone.GRAVEYARD) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("To GY", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Action buttons - Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = { onMoveToZone(Zone.EXILE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("To Exile", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { onMoveToZone(Zone.LIBRARY) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Keep", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

enum class PeekLocation {
    TOP,
    BOTTOM
}
