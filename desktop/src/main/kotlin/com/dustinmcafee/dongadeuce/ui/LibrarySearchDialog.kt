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

/**
 * Card type categories for column-based display (reused from ZoneComponents)
 */
private enum class LibraryCardTypeCategory(val displayName: String) {
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
private fun getLibraryCardCategory(typeLine: String?): LibraryCardTypeCategory {
    val type = typeLine?.lowercase() ?: ""
    return when {
        type.contains("creature") -> LibraryCardTypeCategory.CREATURE
        type.contains("planeswalker") -> LibraryCardTypeCategory.PLANESWALKER
        type.contains("instant") -> LibraryCardTypeCategory.INSTANT
        type.contains("sorcery") -> LibraryCardTypeCategory.SORCERY
        type.contains("enchantment") -> LibraryCardTypeCategory.ENCHANTMENT
        type.contains("artifact") -> LibraryCardTypeCategory.ARTIFACT
        type.contains("land") -> LibraryCardTypeCategory.LAND
        else -> LibraryCardTypeCategory.OTHER
    }
}

@Composable
fun LibrarySearchDialog(
    cards: List<CardInstance>,
    playerName: String,
    onDismiss: () -> Unit,
    onToHand: (CardInstance) -> Unit,
    onToBattlefield: (CardInstance) -> Unit,
    onToTop: (CardInstance) -> Unit,
    onToBottom: (CardInstance) -> Unit = {},
    onShuffle: () -> Unit,
    onViewDetails: (CardInstance) -> Unit = {},
    onCardFocus: ((CardInstance) -> Unit)? = null
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

    // Group cards by type category and sort alphabetically within each group
    val cardsByCategory = remember(filteredCards) {
        filteredCards
            .groupBy { getLibraryCardCategory(it.card.type) }
            .mapValues { (_, cardList) -> cardList.sortedBy { it.card.name.lowercase() } }
    }

    // Get non-empty categories in display order
    val activeCategories = remember(cardsByCategory) {
        LibraryCardTypeCategory.entries.filter { cardsByCategory[it]?.isNotEmpty() == true }
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
                    "$playerName's Library (${cards.size} cards)",
                    style = MaterialTheme.typography.headlineSmall
                )

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

                if (filteredCards.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Library is empty" else "No cards found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
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
                                                        LibraryCardItem(
                                                            cardInstance = cardInstance,
                                                            onToHand = onToHand,
                                                            onToBattlefield = onToBattlefield,
                                                            onToTop = onToTop,
                                                            onToBottom = onToBottom,
                                                            onViewDetails = onViewDetails,
                                                            onCardFocus = onCardFocus
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

                Divider()

                // Bottom buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onShuffle()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Shuffle Library and Close")
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

/**
 * Individual card item in the library search dialog with right-click context menu
 */
@Composable
private fun LibraryCardItem(
    cardInstance: CardInstance,
    onToHand: (CardInstance) -> Unit,
    onToBattlefield: (CardInstance) -> Unit,
    onToTop: (CardInstance) -> Unit,
    onToBottom: (CardInstance) -> Unit,
    onViewDetails: (CardInstance) -> Unit,
    onCardFocus: ((CardInstance) -> Unit)? = null
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var lastClickTime by remember { mutableStateOf(0L) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onCardFocus != null) {
                        Modifier.pointerInput(cardInstance.instanceId) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Final)
                                    if (event.type == PointerEventType.Enter) {
                                        onCardFocus(cardInstance)
                                    }
                                }
                            }
                        }
                    } else Modifier
                )
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
                        onToHand(cardInstance)
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
                    onToHand(cardInstance)
                }
            )
            DropdownMenuItem(
                text = { Text("To Battlefield") },
                onClick = {
                    showContextMenu = false
                    onToBattlefield(cardInstance)
                }
            )
            DropdownMenuItem(
                text = { Text("To Top of Library") },
                onClick = {
                    showContextMenu = false
                    onToTop(cardInstance)
                }
            )
            DropdownMenuItem(
                text = { Text("To Bottom of Library") },
                onClick = {
                    showContextMenu = false
                    onToBottom(cardInstance)
                }
            )
            DropdownMenuItem(
                text = { Text("View Details") },
                onClick = {
                    showContextMenu = false
                    onViewDetails(cardInstance)
                }
            )
        }
    }
}
