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
import androidx.compose.ui.window.DialogProperties
import com.dustinmcafee.dongadeuce.game.ParsedDeckData
import com.dustinmcafee.dongadeuce.models.Card

/**
 * Dialog for selecting a commander from a list of eligible cards.
 * Android version using Compose Material3 Dialog.
 */
@Composable
fun CommanderSelectionDialog(
    deckData: ParsedDeckData,
    candidates: List<Card>,
    playerIndex: Int?, // null for single deck, index for hotseat
    onCommanderSelected: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCard by remember { mutableStateOf<Card?>(null) }
    var selectedPartner by remember { mutableStateOf<Card?>(null) }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Text(
                    text = if (playerIndex != null) {
                        "Select Commander for Player ${playerIndex + 1}"
                    } else {
                        "Select Commander"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Deck info
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Deck: ${deckData.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${deckData.mainboardSize} cards" +
                                if (deckData.sideboardSize > 0) " + ${deckData.sideboardSize} sideboard" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Divider()

                // Filter checkbox and instruction
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Checkbox(
                            checked = filterLegendaries,
                            onCheckedChange = {
                                filterLegendaries = it
                                // Clear selection if filtered card is no longer visible
                                if (it) {
                                    if (selectedCard != null && !(selectedCard!!.isLegendary && selectedCard!!.canBeCommander)) selectedCard = null
                                    if (selectedPartner != null && !(selectedPartner!!.isLegendary && selectedPartner!!.canBeCommander)) selectedPartner = null
                                }
                            }
                        )
                        Text(
                            text = "Legendaries only ($legendaryCount)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = buildString {
                            append(if (filterLegendaries) "Select commander(s):" else "Select any card(s) (house rules):")
                            if (selectedCard != null) append(" ✓ ${selectedCard!!.name}")
                            if (selectedPartner != null) append(" + ${selectedPartner!!.name}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap once = commander. Tap again = partner. Tap a third time = deselect.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Commander grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCandidates) { card ->
                        val selectionState = when (card) {
                            selectedCard -> 1
                            selectedPartner -> 2
                            else -> 0
                        }
                        CommanderCard(
                            card = card,
                            selectionState = selectionState,
                            onClick = {
                                when {
                                    selectedCard == card -> selectedCard = null
                                    selectedPartner == card -> selectedPartner = null
                                    selectedCard == null -> selectedCard = card
                                    selectedPartner == null -> selectedPartner = card
                                    else -> { selectedCard = card; selectedPartner = null }
                                }
                            }
                        )
                    }
                }

                Divider()

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { selectedCard?.let { onCommanderSelected(it.name, selectedPartner?.name) } },
                        enabled = selectedCard != null
                    ) {
                        Text(if (selectedPartner != null) "Select (2)" else "Select")
                    }
                }
            }
        }
    }
}

@Composable
private fun CommanderCard(
    card: Card,
    selectionState: Int, // 0=none, 1=commander, 2=partner
    onClick: () -> Unit
) {
    val borderColor = when (selectionState) {
        1 -> MaterialTheme.colorScheme.primary
        2 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    val backgroundColor = when (selectionState) {
        1 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        2 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (selectionState > 0) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Card image
        if (card.imageUri != null) {
            CardImage(
                imageUrl = card.imageUri,
                contentDescription = card.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        } else {
            // Placeholder for cards without images
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        // Card name
        Text(
            text = card.name,
            style = MaterialTheme.typography.labelSmall,
            color = when (selectionState) {
                1 -> MaterialTheme.colorScheme.primary
                2 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Selection label
        if (selectionState > 0) {
            Text(
                text = if (selectionState == 1) "Commander" else "Partner",
                style = MaterialTheme.typography.labelSmall,
                color = if (selectionState == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
