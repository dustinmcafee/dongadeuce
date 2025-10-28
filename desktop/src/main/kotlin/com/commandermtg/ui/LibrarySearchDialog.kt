package com.commandermtg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.commandermtg.models.CardInstance

@Composable
fun LibrarySearchDialog(
    cards: List<CardInstance>,
    playerName: String,
    onDismiss: () -> Unit,
    onToHand: (CardInstance) -> Unit,
    onToBattlefield: (CardInstance) -> Unit,
    onToTop: (CardInstance) -> Unit,
    onShuffle: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter cards based on search query
    val filteredCards = remember(cards, searchQuery) {
        if (searchQuery.isBlank()) {
            cards
        } else {
            cards.filter { cardInstance ->
                cardInstance.card.name.contains(searchQuery, ignoreCase = true) ||
                cardInstance.card.type?.contains(searchQuery, ignoreCase = true) == true ||
                cardInstance.card.oracleText?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$playerName's Library (${cards.size} cards)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search by name, type, or text") },
                    singleLine = true,
                    placeholder = { Text("Enter search query...") }
                )

                // Results count
                Text(
                    text = "Showing ${filteredCards.size} of ${cards.size} cards",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider()

                // Card list
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredCards.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "Library is empty" else "No cards found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        filteredCards.forEach { cardInstance ->
                            LibraryCard(
                                cardInstance = cardInstance,
                                onToHand = { onToHand(it) },
                                onToBattlefield = { onToBattlefield(it) },
                                onToTop = { onToTop(it) }
                            )
                        }
                    }
                }

                Divider()

                // Shuffle button
                OutlinedButton(
                    onClick = {
                        onShuffle()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Shuffle Library and Close")
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
private fun LibraryCard(
    cardInstance: CardInstance,
    onToHand: (CardInstance) -> Unit,
    onToBattlefield: (CardInstance) -> Unit,
    onToTop: (CardInstance) -> Unit
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

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { onToHand(cardInstance) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("To Hand", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { onToBattlefield(cardInstance) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("To Field", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { onToTop(cardInstance) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("To Top", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
