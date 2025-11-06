package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.CardInstance
import com.dustinmcafee.dongadeuce.models.Zone

@Composable
fun GraveyardDialog(
    cards: List<CardInstance>,
    playerName: String,
    onDismiss: () -> Unit,
    onReturnToHand: (CardInstance) -> Unit,
    onReturnToBattlefield: (CardInstance) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$playerName's Graveyard (${cards.size} cards)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cards.isEmpty()) {
                    Text("No cards in graveyard", style = MaterialTheme.typography.bodyMedium)
                } else {
                    cards.forEach { cardInstance ->
                        ZoneCard(
                            cardInstance = cardInstance,
                            onReturnToHand = { onReturnToHand(it) },
                            onReturnToBattlefield = { onReturnToBattlefield(it) },
                            showBattlefieldAction = true
                        )
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
fun ExileDialog(
    cards: List<CardInstance>,
    playerName: String,
    onDismiss: () -> Unit,
    onReturnToHand: (CardInstance) -> Unit,
    onReturnToBattlefield: (CardInstance) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$playerName's Exile Zone (${cards.size} cards)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cards.isEmpty()) {
                    Text("No cards in exile", style = MaterialTheme.typography.bodyMedium)
                } else {
                    cards.forEach { cardInstance ->
                        ZoneCard(
                            cardInstance = cardInstance,
                            onReturnToHand = { onReturnToHand(it) },
                            onReturnToBattlefield = { onReturnToBattlefield(it) },
                            showBattlefieldAction = true
                        )
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
fun CommandZoneDialog(
    cards: List<CardInstance>,
    playerName: String,
    onDismiss: () -> Unit,
    onCastToBattlefield: (CardInstance) -> Unit,
    onToHand: (CardInstance) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$playerName's Command Zone (${cards.size} cards)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cards.isEmpty()) {
                    Text("No cards in command zone", style = MaterialTheme.typography.bodyMedium)
                } else {
                    cards.forEach { cardInstance ->
                        CommandZoneCard(
                            cardInstance = cardInstance,
                            onCastToBattlefield = { onCastToBattlefield(it) },
                            onToHand = { onToHand(it) }
                        )
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
private fun CommandZoneCard(
    cardInstance: CardInstance,
    onCastToBattlefield: (CardInstance) -> Unit,
    onToHand: (CardInstance) -> Unit
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

                // Oracle text
                val oracleText = cardInstance.card.oracleText
                if (oracleText != null && oracleText.isNotEmpty()) {
                    Text(
                        text = oracleText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onToHand(cardInstance) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("To Hand")
                    }

                    Button(
                        onClick = { onCastToBattlefield(cardInstance) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cast")
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneCard(
    cardInstance: CardInstance,
    onReturnToHand: (CardInstance) -> Unit,
    onReturnToBattlefield: (CardInstance) -> Unit,
    showBattlefieldAction: Boolean
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

                // Oracle text
                val oracleText = cardInstance.card.oracleText
                if (oracleText != null && oracleText.isNotEmpty()) {
                    Text(
                        text = oracleText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onReturnToHand(cardInstance) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("To Hand")
                    }

                    if (showBattlefieldAction) {
                        Button(
                            onClick = { onReturnToBattlefield(cardInstance) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("To Battlefield")
                        }
                    }
                }
            }
        }
    }
}
