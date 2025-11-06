package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dustinmcafee.dongadeuce.models.CardInstance

/**
 * Dialog showing detailed information about a card
 */
@Composable
fun CardDetailsDialog(
    cardInstance: CardInstance,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(500.dp)
                .heightIn(max = 700.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card name header
                Text(
                    text = cardInstance.card.name,
                    style = MaterialTheme.typography.headlineSmall
                )

                // Card image (if available)
                if (cardInstance.card.imageUri != null) {
                    CardImage(
                        imageUrl = cardInstance.card.imageUri,
                        contentDescription = cardInstance.card.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    )
                }

                Divider()

                // Mana cost
                cardInstance.card.manaCost?.let { manaCost ->
                    if (manaCost.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Mana Cost:",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = manaCost,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // Type line
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Type:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = cardInstance.card.type ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Oracle text
                cardInstance.card.oracleText?.let { oracleText ->
                    if (oracleText.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Oracle Text:",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = oracleText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Power/Toughness (for creatures)
                if (cardInstance.card.power != null && cardInstance.card.toughness != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "P/T:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "${cardInstance.card.power}/${cardInstance.card.toughness}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Card instance info
                Divider()

                Text(
                    text = "Card State",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Zone:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = cardInstance.zone.toString(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (cardInstance.isTapped) {
                    Text(
                        text = "Status: Tapped",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (cardInstance.isFlipped) {
                    Text(
                        text = "Status: Flipped",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Counters
                if (cardInstance.counters.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Counters:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        cardInstance.counters.forEach { (type, count) ->
                            Text(
                                text = "• $type: $count",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}
