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
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.*

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
    otherPlayers: List<Player>,
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
                        .height(80.dp)
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
                            otherPlayers = otherPlayers
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
    otherPlayers: List<Player>
) {
    CardWithContextMenu(
        cardInstance = cardInstance,
        onAction = onCardAction,
        otherPlayers = otherPlayers
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
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
    selectionState: SelectionState? = null,
    otherPlayers: List<Player> = emptyList(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
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

                // Shared drag state for hand cards
                var draggedHandCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }
                var handDragOffset by remember { mutableStateOf(Offset.Zero) }

                if (!showCards) {
                    // For non-active players, just show card count, not actual cards
                    Text(
                        "Hidden",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                } else if (handCards.isEmpty()) {
                    Text(
                        "No cards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        handCards.forEach { cardInstance ->
                            HandCardDisplay(
                                cardInstance = cardInstance,
                                onCardClick = { /* Single click - could open dialog */ },
                                onDoubleClick = {
                                    // Double-click plays card to battlefield
                                    onCardAction(CardAction.ToBattlefield(cardInstance))
                                },
                                onContextAction = onCardAction,
                                selectionState = selectionState,
                                sharedDraggedCardIds = draggedHandCardIds,
                                sharedDragOffset = handDragOffset,
                                onDragStateChange = { draggedIds, offset ->
                                    draggedHandCardIds = draggedIds
                                    handDragOffset = offset
                                },
                                otherPlayers = otherPlayers
                            )
                        }
                    }
                }
            }
        }
    }
}
