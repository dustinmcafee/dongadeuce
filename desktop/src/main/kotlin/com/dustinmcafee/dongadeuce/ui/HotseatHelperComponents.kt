package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.viewmodel.GameViewModel

/**
 * Compact player counters display (shows poison/energy/experience if any)
 */
@Composable
fun PlayerCountersDisplay(
    player: Player,
    onClick: (() -> Unit)?
) {
    val poisonCount = player.getCounter("poison")
    val energyCount = player.getCounter("energy")
    val experienceCount = player.getCounter("experience")
    val hasCounters = poisonCount > 0 || energyCount > 0 || experienceCount > 0 || player.counters.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (poisonCount > 0) {
            CounterChip(
                label = "Poison",
                count = poisonCount,
                color = Color(0xFF4CAF50), // Green for poison
                isLethal = poisonCount >= com.dustinmcafee.dongadeuce.models.GameConstants.POISON_THRESHOLD
            )
        }
        if (energyCount > 0) {
            CounterChip(
                label = "E",
                count = energyCount,
                color = Color(0xFFFF9800) // Orange for energy
            )
        }
        if (experienceCount > 0) {
            CounterChip(
                label = "XP",
                count = experienceCount,
                color = Color(0xFF9C27B0) // Purple for experience
            )
        }
        // Show "Counters" button if no counters yet but clickable
        if (!hasCounters && onClick != null) {
            Text(
                "Counters",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
internal fun CounterChip(
    label: String,
    count: Int,
    color: Color,
    isLethal: Boolean = false
) {
    Surface(
        color = if (isLethal) Color(0xFFB71C1C) else color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/**
 * Command Zone display - shows the commander card(s) visually
 */
@Composable
fun CommandZoneDisplay(
    commanderCards: List<CardInstance>,
    isActivePlayer: Boolean,
    onCardAction: (CardAction) -> Unit,
    allPlayers: List<Player>,
    dragDropState: DragDropState?,
    onDropCards: ((List<String>) -> Unit)?,
    modifier: Modifier = Modifier
) {
    var isHovering by remember { mutableStateOf(false) }

    // Check if cards are being dragged over this zone
    val isDraggingOver = dragDropState != null &&
                        dragDropState.draggedCardIds.isNotEmpty() &&
                        isHovering

    Card(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                // Register zone bounds for accurate drop detection
                if (dragDropState != null) {
                    val bounds = Rect(
                        coordinates.positionInWindow().x,
                        coordinates.positionInWindow().y,
                        coordinates.positionInWindow().x + coordinates.size.width,
                        coordinates.positionInWindow().y + coordinates.size.height
                    )
                    dragDropState.registerZoneBounds(Zone.COMMAND_ZONE, bounds)
                }
            }
            .then(
                if (onDropCards != null && dragDropState != null) {
                    Modifier.pointerInput(Zone.COMMAND_ZONE) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        if (dragDropState.draggedCardIds.isNotEmpty()) {
                                            isHovering = true
                                            dragDropState.setHoveredZone(Zone.COMMAND_ZONE)
                                        }
                                    }
                                    PointerEventType.Exit -> {
                                        isHovering = false
                                        if (dragDropState.hoveredZone == Zone.COMMAND_ZONE) {
                                            dragDropState.setHoveredZone(null)
                                        }
                                    }
                                    PointerEventType.Release -> {
                                        // Only handle drop if we're hovering AND cards are being dragged
                                        if (isHovering && dragDropState.draggedCardIds.isNotEmpty()) {
                                            onDropCards(dragDropState.draggedCardIds.toList())
                                            isHovering = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDraggingOver) Color(0xFF4A148C).copy(alpha = 0.9f) else Color(0xFF4A148C).copy(alpha = 0.6f)
        ),
        border = if (isDraggingOver) BorderStroke(3.dp, Color(0xFF00FF00)) else null
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Command Zone",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )

            if (commanderCards.isEmpty()) {
                // Empty command zone placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No Commander",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            } else {
                // Show commander card(s)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    commanderCards.forEach { commander ->
                        CommanderCardDisplay(
                            cardInstance = commander,
                            isActivePlayer = isActivePlayer,
                            onCardAction = onCardAction,
                            allPlayers = allPlayers
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual commander card display in the command zone
 */
@Composable
private fun CommanderCardDisplay(
    cardInstance: CardInstance,
    isActivePlayer: Boolean,
    onCardAction: (CardAction) -> Unit,
    allPlayers: List<Player>
) {
    CardWithContextMenu(
        cardInstance = cardInstance,
        onAction = onCardAction,
        allPlayers = allPlayers
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .then(
                    if (isActivePlayer) {
                        Modifier.clickable {
                            // Double-click to cast to battlefield
                            onCardAction(CardAction.ToBattlefield(cardInstance))
                        }
                    } else Modifier
                ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, Color(0xFFFFD700)) // Gold border for commanders
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card image
                CardImage(
                    imageUrl = cardInstance.card.imageUri,
                    contentDescription = cardInstance.card.name,
                    modifier = Modifier
                        .width(60.dp)
                        .fillMaxHeight()
                )

                // Card info
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        cardInstance.card.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 2
                    )

                    if (isActivePlayer) {
                        Text(
                            "Click to cast",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact hand display strip
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactHandStrip(
    player: Player,
    handCards: List<CardInstance>,
    handCount: Int,
    showCards: Boolean,
    onCardAction: (CardAction) -> Unit,
    viewModel: GameViewModel? = null,
    selectionState: SelectionState? = null,
    dragDropState: DragDropState? = null,
    allPlayers: List<Player> = emptyList(),
    modifier: Modifier = Modifier,
    onCardFocus: ((CardInstance) -> Unit)? = null
) {
    // State for right-click context menu and View Hand dialog
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var showViewHandDialog by remember { mutableStateOf(false) }

    // Track hand area position for reordering calculations
    var handAreaPositionX by remember { mutableStateOf(0f) }
    var handAreaWidth by remember { mutableStateOf(0f) }

    // View Hand dialog
    if (showViewHandDialog && showCards) {
        ViewHandDialog(
            cards = handCards,
            playerName = player.name,
            allPlayers = allPlayers,
            onDismiss = { showViewHandDialog = false },
            onPlayCard = { cardInstance ->
                viewModel?.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
                    ?: onCardAction(CardAction.ToBattlefield(cardInstance))
            },
            onDiscard = { cardInstance ->
                viewModel?.moveCard(cardInstance.instanceId, Zone.GRAVEYARD)
                    ?: onCardAction(CardAction.ToGraveyard(cardInstance))
            },
            onExile = { cardInstance ->
                viewModel?.moveCard(cardInstance.instanceId, Zone.EXILE)
                    ?: onCardAction(CardAction.ToExile(cardInstance))
            },
            onToLibrary = { cardInstance ->
                viewModel?.moveCard(cardInstance.instanceId, Zone.LIBRARY)
                    ?: onCardAction(CardAction.ToLibrary(cardInstance))
            },
            onContextAction = onCardAction,
            onRevealHandTo = { targetPlayerIds ->
                viewModel?.revealHand(player.id, targetPlayerIds)
            }
        )
    }

    Card(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                // Register hand as a drop zone so battlefield cards can be dropped here
                if (dragDropState != null) {
                    val position = coordinates.positionInWindow()
                    val bounds = Rect(
                        left = position.x,
                        top = position.y,
                        right = position.x + coordinates.size.width,
                        bottom = position.y + coordinates.size.height
                    )
                    dragDropState.registerZoneBounds(Zone.HAND, bounds)
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${player.name}'s Hand ($handCount)",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(120.dp)
                    )

                    // Hand area with right-click support
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .onGloballyPositioned { coordinates ->
                                // Track hand area position for reordering
                                handAreaPositionX = coordinates.positionInWindow().x
                                handAreaWidth = coordinates.size.width.toFloat()
                            }
                            .then(
                                if (showCards) {
                                    Modifier.pointerInput(Unit) {
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
                                } else Modifier
                            )
                    ) {
                        if (!showCards) {
                            // For non-active players, just show card count, not actual cards
                            Text(
                                "Hidden",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        } else if (handCards.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    "No cards (Right-click for options)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        } else {
                            // Overlapping hand cards layout
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val density = LocalDensity.current
                                val availableWidth = with(density) { maxWidth.toPx() }
                                val cardWidthPx = with(density) { UIConstants.HAND_CARD_WIDTH.toPx() }
                                val minSpacing = with(density) { 4.dp.toPx() }

                                // Calculate spacing between cards
                                // If cards fit without overlap, use normal spacing
                                // Otherwise, calculate overlap to fit all cards
                                val totalCardWidth = cardWidthPx * handCards.size
                                val totalSpacingNeeded = minSpacing * (handCards.size - 1).coerceAtLeast(0)

                                val cardSpacingPx = if (totalCardWidth + totalSpacingNeeded <= availableWidth) {
                                    // Cards fit - use normal spacing
                                    cardWidthPx + minSpacing
                                } else {
                                    // Cards don't fit - calculate overlap
                                    // We want: firstCardPos + (n-1) * spacing + cardWidth = availableWidth
                                    // spacing = (availableWidth - cardWidth) / (n - 1)
                                    if (handCards.size > 1) {
                                        ((availableWidth - cardWidthPx) / (handCards.size - 1)).coerceAtLeast(20f)
                                    } else {
                                        cardWidthPx
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    handCards.forEachIndexed { index, cardInstance ->
                                        Box(
                                            modifier = Modifier
                                                .offset(x = with(density) { ((index * cardSpacingPx) - (index * cardWidthPx)).toDp() })
                                                .zIndex(index.toFloat())
                                        ) {
                                            HandCardDisplay(
                                                cardInstance = cardInstance,
                                                onCardClick = { /* Single click - could open dialog */ },
                                                onDoubleClick = {
                                                    // Double-click plays card to battlefield
                                                    onCardAction(CardAction.ToBattlefield(cardInstance))
                                                },
                                                onContextAction = onCardAction,
                                                selectionState = selectionState,
                                                allPlayers = allPlayers,
                                                onCardFocus = onCardFocus,
                                                dragDropState = dragDropState,
                                                onDropToZone = { cardIds, zone ->
                                                    cardIds.forEach { cardId ->
                                                        when (zone) {
                                                            Zone.LIBRARY -> viewModel?.moveCardToTopOfLibrary(cardId)
                                                                ?: onCardAction(CardAction.ToTop(cardInstance))
                                                            Zone.BATTLEFIELD -> viewModel?.moveCard(cardId, zone)
                                                                ?: onCardAction(CardAction.ToBattlefield(cardInstance))
                                                            Zone.GRAVEYARD -> viewModel?.moveCard(cardId, zone)
                                                                ?: onCardAction(CardAction.ToGraveyard(cardInstance))
                                                            Zone.EXILE -> viewModel?.moveCard(cardId, zone)
                                                                ?: onCardAction(CardAction.ToExile(cardInstance))
                                                            Zone.HAND -> viewModel?.moveCard(cardId, zone)
                                                                ?: onCardAction(CardAction.ToHand(cardInstance))
                                                            Zone.COMMAND_ZONE -> viewModel?.moveCard(cardId, zone)
                                                                ?: onCardAction(CardAction.ToCommandZone(cardInstance))
                                                            else -> { /* STACK, SIDEBOARD not applicable */ }
                                                        }
                                                    }
                                                },
                                                onReorderInHand = { cardId, dropXPosition ->
                                                    // Calculate target position based on drop X coordinate
                                                    // Use the same spacing calculation as the layout
                                                    val relativeX = dropXPosition - handAreaPositionX
                                                    val cardCount = handCards.size

                                                    // Calculate target index based on card spacing
                                                    val targetIndex = if (cardCount > 1 && cardSpacingPx > 0) {
                                                        // Divide by spacing to get approximate index
                                                        // Add half card width to center the drop zone on each card
                                                        ((relativeX + cardWidthPx / 2) / cardSpacingPx).toInt().coerceIn(0, cardCount - 1)
                                                    } else {
                                                        0
                                                    }
                                                    viewModel?.reorderHandCard(cardId, targetIndex)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Context menu dropdown
            if (showCards) {
                val density = LocalDensity.current.density
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false },
                    offset = DpOffset(
                        x = (contextMenuOffset.x / density).dp,
                        y = (contextMenuOffset.y / density).dp
                    )
                ) {
                    DropdownMenuItem(
                        text = { Text("View Hand") },
                        onClick = {
                            showContextMenu = false
                            showViewHandDialog = true
                        }
                    )
                }
            }
        }
    }
}
