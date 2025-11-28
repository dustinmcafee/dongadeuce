@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.CardInstance
import com.dustinmcafee.dongadeuce.models.Zone

/**
 * Card type categories for column-based display
 */
private enum class PeekCardTypeCategory(val displayName: String) {
    CREATURE("Creatures"),
    PLANESWALKER("Planeswalkers"),
    INSTANT("Instants"),
    SORCERY("Sorceries"),
    ENCHANTMENT("Enchantments"),
    ARTIFACT("Artifacts"),
    LAND("Lands"),
    OTHER("Other")
}

/**
 * Get the category for a card based on its type line
 */
private fun getPeekCardCategory(typeLine: String?): PeekCardTypeCategory {
    val type = typeLine?.lowercase() ?: ""
    return when {
        type.contains("creature") -> PeekCardTypeCategory.CREATURE
        type.contains("planeswalker") -> PeekCardTypeCategory.PLANESWALKER
        type.contains("instant") -> PeekCardTypeCategory.INSTANT
        type.contains("sorcery") -> PeekCardTypeCategory.SORCERY
        type.contains("enchantment") -> PeekCardTypeCategory.ENCHANTMENT
        type.contains("artifact") -> PeekCardTypeCategory.ARTIFACT
        type.contains("land") -> PeekCardTypeCategory.LAND
        else -> PeekCardTypeCategory.OTHER
    }
}

@Composable
fun LibraryPeekDialog(
    cards: List<CardInstance>,
    playerName: String,
    peekLocation: PeekLocation,
    onDismiss: () -> Unit,
    onMoveCard: (CardInstance, Zone) -> Unit,
    onMoveAllToZone: (Zone) -> Unit,
    onShuffleCards: () -> Unit,
    onViewDetails: (CardInstance) -> Unit = {}
) {
    // Group cards by type category and sort alphabetically within each group
    val cardsByCategory = remember(cards) {
        cards
            .groupBy { getPeekCardCategory(it.card.type) }
            .mapValues { (_, cardList) -> cardList.sortedBy { it.card.name.lowercase() } }
    }

    // Get non-empty categories in display order
    val activeCategories = remember(cardsByCategory) {
        PeekCardTypeCategory.entries.filter { cardsByCategory[it]?.isNotEmpty() == true }
    }

    // Use Dialog for better control over sizing
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title
                Text(
                    when (peekLocation) {
                        PeekLocation.TOP -> "$playerName's Library - Top ${cards.size} card(s)"
                        PeekLocation.BOTTOM -> "$playerName's Library - Bottom ${cards.size} card(s)"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )

                if (cards.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
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

                    // Horizontal scrollable row of columns (one per card type)
                    val horizontalScrollState = rememberScrollState()
                    val columnWidth = 200.dp

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            val columnHeight = maxHeight

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(horizontalScrollState)
                            ) {
                                activeCategories.forEach { category ->
                                    val categoryCards = cardsByCategory[category] ?: emptyList()
                                    val columnScrollState = rememberScrollState()

                                    // Column for this card type
                                    Card(
                                        modifier = Modifier
                                            .width(columnWidth)
                                            .height(columnHeight),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Column header
                                            Text(
                                                "${category.displayName} (${categoryCards.size})",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                                            // Vertically scrollable list of cards with scrollbar
                                            Box(modifier = Modifier.weight(1f)) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .verticalScroll(columnScrollState)
                                                        .padding(end = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    categoryCards.forEach { cardInstance ->
                                                        PeekCardItem(
                                                            cardInstance = cardInstance,
                                                            onMoveToZone = { zone -> onMoveCard(cardInstance, zone) },
                                                            onViewDetails = { onViewDetails(cardInstance) }
                                                        )
                                                    }
                                                }
                                                VerticalScrollbar(
                                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                                    adapter = rememberScrollbarAdapter(columnScrollState),
                                                    style = LocalScrollbarStyle.current.copy(
                                                        unhoverColor = androidx.compose.ui.graphics.Color.Gray,
                                                        hoverColor = androidx.compose.ui.graphics.Color.LightGray
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Horizontal scrollbar with visible colors
                        HorizontalScrollbar(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            adapter = rememberScrollbarAdapter(horizontalScrollState),
                            style = LocalScrollbarStyle.current.copy(
                                unhoverColor = androidx.compose.ui.graphics.Color.Gray,
                                hoverColor = androidx.compose.ui.graphics.Color.LightGray
                            )
                        )
                    }
                }

                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

/**
 * Individual card item in the peek dialog with right-click context menu
 */
@Composable
private fun PeekCardItem(
    cardInstance: CardInstance,
    onMoveToZone: (Zone) -> Unit,
    onViewDetails: () -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var lastClickTime by remember { mutableStateOf(0L) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(cardInstance.instanceId) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.buttons.isSecondaryPressed && event.type == PointerEventType.Press) {
                                contextMenuOffset = event.changes.first().position
                                showContextMenu = true
                            }
                        }
                    }
                }
                .clickable {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime < 300) {
                        // Double-click: move to hand
                        onMoveToZone(Zone.HAND)
                    }
                    lastClickTime = currentTime
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Card image thumbnail (smaller)
                CardImage(
                    imageUrl = cardInstance.card.imageUri,
                    contentDescription = cardInstance.card.name,
                    modifier = Modifier.width(40.dp).height(56.dp)
                )

                // Card info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cardInstance.card.name,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2
                    )
                    val manaCost = cardInstance.card.manaCost
                    if (manaCost != null) {
                        Text(
                            text = manaCost,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Right-click context menu
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("To Hand") },
                onClick = {
                    showContextMenu = false
                    onMoveToZone(Zone.HAND)
                }
            )
            DropdownMenuItem(
                text = { Text("To Battlefield") },
                onClick = {
                    showContextMenu = false
                    onMoveToZone(Zone.BATTLEFIELD)
                }
            )
            DropdownMenuItem(
                text = { Text("To Graveyard") },
                onClick = {
                    showContextMenu = false
                    onMoveToZone(Zone.GRAVEYARD)
                }
            )
            DropdownMenuItem(
                text = { Text("To Exile") },
                onClick = {
                    showContextMenu = false
                    onMoveToZone(Zone.EXILE)
                }
            )
            DropdownMenuItem(
                text = { Text("Keep in Library") },
                onClick = {
                    showContextMenu = false
                    onMoveToZone(Zone.LIBRARY)
                }
            )
            DropdownMenuItem(
                text = { Text("View Details") },
                onClick = {
                    showContextMenu = false
                    onViewDetails()
                }
            )
        }
    }
}

enum class PeekLocation {
    TOP,
    BOTTOM
}
