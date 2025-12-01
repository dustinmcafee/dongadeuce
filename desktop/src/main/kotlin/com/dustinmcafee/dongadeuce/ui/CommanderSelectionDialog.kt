package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dustinmcafee.dongadeuce.game.ParsedDeckData
import com.dustinmcafee.dongadeuce.models.Card

/**
 * Dialog for selecting a commander from a list of eligible cards.
 * Shown when loading a deck that doesn't specify a commander.
 */
@Composable
fun CommanderSelectionDialog(
    deckData: ParsedDeckData,
    candidates: List<Card>,
    playerIndex: Int?, // null for single deck, index for hotseat
    onCommanderSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCard by remember { mutableStateOf<Card?>(null) }
    var filterLegendaries by remember { mutableStateOf(true) }

    // Filter candidates based on checkbox
    val filteredCandidates = remember(candidates, filterLegendaries) {
        if (filterLegendaries) {
            candidates.filter { it.isLegendary && it.canBeCommander }
        } else {
            candidates
        }
    }

    // Count legendaries for display
    val legendaryCount = remember(candidates) {
        candidates.count { it.isLegendary && it.canBeCommander }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(700.dp)
                .heightIn(max = 800.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = if (playerIndex != null) {
                        "Select Commander for Player ${playerIndex + 1}"
                    } else {
                        "Select Commander"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Deck info
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Deck: ${deckData.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${deckData.mainboardSize} cards in mainboard" +
                                if (deckData.sideboardSize > 0) ", ${deckData.sideboardSize} in sideboard" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Divider()

                // Filter checkbox and instruction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (filterLegendaries) {
                            "Select a legendary creature or planeswalker:"
                        } else {
                            "Select any card (house rules):"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Checkbox(
                            checked = filterLegendaries,
                            onCheckedChange = {
                                filterLegendaries = it
                                // Clear selection if filtered card is no longer visible
                                if (it && selectedCard != null && !(selectedCard!!.isLegendary && selectedCard!!.canBeCommander)) {
                                    selectedCard = null
                                }
                            }
                        )
                        Text(
                            text = "Legendaries only ($legendaryCount)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Commander grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCandidates) { card ->
                        CommanderCard(
                            card = card,
                            isSelected = selectedCard == card,
                            onClick = { selectedCard = card }
                        )
                    }
                }

                Divider()

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { selectedCard?.let { onCommanderSelected(it.name) } },
                        enabled = selectedCard != null
                    ) {
                        Text("Select Commander")
                    }
                }
            }
        }
    }
}

@Composable
private fun CommanderCard(
    card: Card,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Card image
        if (card.imageUri != null) {
            CardImage(
                imageUrl = card.imageUri,
                contentDescription = card.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            // Placeholder for cards without images
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Card name
        Text(
            text = card.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Card type
        Text(
            text = card.type ?: "Unknown Type",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
