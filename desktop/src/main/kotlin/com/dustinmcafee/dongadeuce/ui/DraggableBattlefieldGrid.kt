package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dustinmcafee.dongadeuce.models.CardInstance
import com.dustinmcafee.dongadeuce.models.Zone
import com.dustinmcafee.dongadeuce.ui.UIConstants.BATTLEFIELD_CARD_TAPPED_SIZE
import com.dustinmcafee.dongadeuce.ui.UIConstants.STACK_OFFSET_RATIO
import kotlin.math.roundToInt

/**
 * A draggable grid layout for battlefield cards.
 * Allows users to drag and drop cards to rearrange them in a grid.
 * Only allows dragging cards owned by the current player.
 */
@Composable
fun DraggableBattlefieldGrid(
    cards: List<CardInstance>,
    isLocalPlayer: Boolean,
    onCardClick: (CardInstance) -> Unit,
    onContextAction: (CardAction) -> Unit,
    onCardPositionChanged: (String, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    currentPlayerId: String? = null, // ID of the player who can drag cards
    selectionState: SelectionState? = null,
    otherPlayers: List<com.dustinmcafee.dongadeuce.models.Player> = emptyList(),
    allPlayers: List<com.dustinmcafee.dongadeuce.models.Player> = emptyList(),
    dragDropState: DragDropState? = null,
    onDropToZone: ((Set<String>, com.dustinmcafee.dongadeuce.models.Zone) -> Unit)? = null
) {
    if (cards.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Battlefield",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        return
    }

    // Calculate grid dimensions based on card size
    val cardWidth = BATTLEFIELD_CARD_TAPPED_SIZE // Max width for tapped cards
    val cardHeight = BATTLEFIELD_CARD_TAPPED_SIZE // Max height for tapped cards
    val gridSpacing = 8.dp
    val cellWidth = cardWidth.value + gridSpacing.value
    val cellHeight = cardHeight.value + gridSpacing.value

    // Track dragging state locally - ALWAYS use local state for drag calculations
    // to avoid timing issues with shared state updates
    var localDraggedCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var localDragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartPixelPos by remember { mutableStateOf(Offset.Zero) }
    var dragStartCursorOffset by remember { mutableStateOf(Offset.Zero) } // Where on the card the drag started

    // Track target positions for dragged cards to prevent snap-back
    var targetPositions by remember { mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap()) }

    // For rendering: use local state first, fall back to shared state
    val draggedCardIds = if (localDraggedCardIds.isNotEmpty()) localDraggedCardIds else (dragDropState?.draggedCardIds ?: emptySet())
    val dragOffset = localDragOffset

    // Container size and position
    var containerWidth by remember { mutableStateOf(0f) }
    var containerPositionInWindow by remember { mutableStateOf(Offset.Zero) }

    // Clear drag offset and target positions once all cards have reached their targets
    LaunchedEffect(targetPositions, cards) {
        if (targetPositions.isNotEmpty()) {
            // Check if all cards have reached their target positions
            val allReached = targetPositions.all { (cardId, target) ->
                val card = cards.find { it.instanceId == cardId }
                card != null && card.gridX == target.first && card.gridY == target.second
            }

            if (allReached) {
                // All cards have reached their targets, clear everything
                targetPositions = emptyMap()
                localDragOffset = Offset.Zero
                dragStartPixelPos = Offset.Zero
                dragStartCursorOffset = Offset.Zero
            }
        }
    }

    // Calculate grid columns based on container width
    val columns = remember(containerWidth) {
        if (containerWidth > 0) {
            ((containerWidth / cellWidth).toInt()).coerceAtLeast(1)
        } else {
            4 // Default
        }
    }

    // Organize cards into grid positions with stacking support
    // Key by the grid positions of all cards to ensure recalculation when positions change
    val gridPositionsKey = remember(cards) {
        cards.map { "${it.instanceId}:${it.gridX}:${it.gridY}" }.joinToString(",")
    }

    // Card stacking offsets for visual clarity
    val stackOffsetX = (cardWidth.value * STACK_OFFSET_RATIO).dp
    val stackOffsetY = (cardHeight.value * STACK_OFFSET_RATIO).dp

    data class CardStackInfo(
        val gridPos: Pair<Int, Int>,
        val stackIndex: Int  // 0 = bottom, 1 = middle, 2 = top
    )

    val cardPositions = remember(gridPositionsKey, columns) {
        val positionMap = mutableMapOf<String, Pair<Int, Int>>()
        val stackInfo = mutableMapOf<String, CardStackInfo>()

        // First, place cards that have explicit grid positions
        val cardsWithPositions = cards.filter { it.gridX != null && it.gridY != null }
        cardsWithPositions.forEach { card ->
            // Safe call: filter above ensures both are non-null, but use safe access for robustness
            val x = card.gridX ?: 0
            val y = card.gridY ?: 0
            positionMap[card.instanceId] = Pair(x, y)
        }

        // Then, auto-arrange cards without positions
        val cardsWithoutPositions = cards.filter { it.gridX == null || it.gridY == null }
        var nextRow = 0
        var nextCol = 0

        // Find next available position (with stacking support)
        // Returns null if battlefield is full (all positions have 3 cards)
        fun findNextAvailablePosition(): Pair<Int, Int>? {
            val maxPositions = columns * 10 // 4 columns x 10 rows = 40 positions
            var positionsChecked = 0

            while (positionsChecked < maxPositions) {
                // Count how many cards are at the current position
                val cardsAtPosition = positionMap.values.count { it == Pair(nextCol, nextRow) }

                // If less than 3 cards, can stack here
                if (cardsAtPosition < 3) {
                    return Pair(nextCol, nextRow)
                }

                // Move to next position
                nextCol++
                if (nextCol >= columns) {
                    nextCol = 0
                    nextRow++
                }

                positionsChecked++
            }

            // Battlefield is full
            return null
        }

        cardsWithoutPositions.forEach { card ->
            val position = findNextAvailablePosition()
            if (position != null) {
                positionMap[card.instanceId] = position
            } else {
                // Battlefield full - place at (0, 0) and stack (will show at max stack depth)
                // This is a fallback to prevent crashes
                positionMap[card.instanceId] = Pair(0, 0)
            }
        }

        // Calculate stack indices for all cards
        val gridGroupedCards = cards.groupBy { card ->
            positionMap[card.instanceId] ?: Pair(0, 0)
        }

        gridGroupedCards.forEach { (gridPos, cardsAtPos) ->
            cardsAtPos.forEachIndexed { index, card ->
                stackInfo[card.instanceId] = CardStackInfo(
                    gridPos = gridPos,
                    stackIndex = index.coerceAtMost(2) // Max 3 cards per stack
                )
            }
        }

        Pair(positionMap, stackInfo)
    }

    val (gridPositions, stackInfoMap) = cardPositions

    // Calculate total rows needed
    val totalRows = ((gridPositions.values.maxOfOrNull { it.second } ?: 0) + 1).coerceAtMost(10)

    // Scroll state for vertical scrolling
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width.toFloat()
                containerPositionInWindow = coordinates.positionInWindow()
            }
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .height((totalRows * cellHeight).dp)
    ) {
                cards.forEach { card ->
                    val position = gridPositions[card.instanceId] ?: Pair(0, 0)
                    val (col, row) = position
                    val stackInfo = stackInfoMap[card.instanceId]

                    val isDragged = draggedCardIds.contains(card.instanceId)
                    val canDrag = currentPlayerId != null && card.ownerId == currentPlayerId

                    // Base position
                    var xPos = col * cellWidth
                    var yPos = row * cellHeight

                    // Apply stacking offset for visual separation
                    if (stackInfo != null) {
                        xPos += stackInfo.stackIndex * stackOffsetX.value
                        yPos += stackInfo.stackIndex * stackOffsetY.value
                    }

                    // Check if this card has a pending position update (to prevent snap-back)
                    val hasTargetPosition = targetPositions.containsKey(card.instanceId)
                    val targetReached = if (hasTargetPosition) {
                        val target = targetPositions[card.instanceId]!!
                        card.gridX == target.first && card.gridY == target.second
                    } else {
                        true
                    }

                    val finalOffset = if (isDragged) {
                        // Card is being actively dragged
                        IntOffset(
                            (xPos + dragOffset.x).roundToInt(),
                            (yPos + dragOffset.y).roundToInt()
                        )
                    } else if (hasTargetPosition && !targetReached) {
                        // Card has a target but hasn't reached it yet - keep offset to prevent snap
                        IntOffset(
                            (xPos + dragOffset.x).roundToInt(),
                            (yPos + dragOffset.y).roundToInt()
                        )
                    } else {
                        // Normal rendering at grid position
                        IntOffset(xPos.roundToInt(), yPos.roundToInt())
                    }

                    Box(
                        modifier = Modifier
                            .offset { finalOffset }
                            .zIndex((row * 1000 + col * 10 + (stackInfo?.stackIndex ?: 0)).toFloat())
                            .then(
                                if (canDrag) {
                                    Modifier.pointerInput(card.instanceId, gridPositions) {
                                        detectDragGestures(
                                            onDragStart = { cursorOffset ->
                                                // Look up current position from map and calculate pixel position
                                                val currentPos = gridPositions[card.instanceId] ?: Pair(0, 0)
                                                val currentStack = stackInfoMap[card.instanceId]
                                                val startX = currentPos.first * cellWidth + (currentStack?.stackIndex ?: 0) * stackOffsetX.value
                                                val startY = currentPos.second * cellHeight + (currentStack?.stackIndex ?: 0) * stackOffsetY.value
                                                dragStartPixelPos = Offset(startX, startY)
                                                dragStartCursorOffset = cursorOffset // Where on the card the cursor was when drag started

                                                // Determine which cards to drag:
                                                // 1. If this card is selected and multiple cards are selected, drag all selected cards
                                                // 2. Otherwise, drag just this card
                                                val cardsToDrag = if (selectionState?.isSelected(card.instanceId) == true &&
                                                                     selectionState.selectedCards.size > 1) {
                                                    // Drag all selected cards that are owned by current player
                                                    selectionState.selectedCards.filter { cardId ->
                                                        cards.find { it.instanceId == cardId }?.ownerId == currentPlayerId
                                                    }.toSet()
                                                } else {
                                                    // Drag just this card
                                                    setOf(card.instanceId)
                                                }

                                                // Update LOCAL drag state for calculations
                                                localDraggedCardIds = cardsToDrag
                                                localDragOffset = Offset.Zero

                                                // Also update shared state for zone detection
                                                dragDropState?.startDragMultiple(cardsToDrag, Offset.Zero)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                // ALWAYS accumulate in local state to avoid timing issues
                                                localDragOffset += dragAmount

                                                // Sync to shared state for zone hover detection
                                                dragDropState?.updateDragPosition(localDragOffset)
                                            },
                                            onDragEnd = {
                                                // IMPORTANT: Use LOCAL state directly for repositioning, NOT the computed property
                                                // The computed property might fall back to shared state which could be cleared
                                                val cardsBeingDragged = localDraggedCardIds.toSet()

                                                // Calculate absolute window position of the cursor (not the card corner)
                                                // This is: battlefield position + card start position + where cursor was on card + how far it was dragged
                                                val absoluteDropX = containerPositionInWindow.x + dragStartPixelPos.x + dragStartCursorOffset.x + localDragOffset.x
                                                val absoluteDropY = containerPositionInWindow.y + dragStartPixelPos.y + dragStartCursorOffset.y + localDragOffset.y
                                                val absoluteDropPos = Offset(absoluteDropX, absoluteDropY)

                                                // Check if dropped on a zone using accurate bounds detection
                                                if (dragDropState != null && onDropToZone != null && cardsBeingDragged.isNotEmpty()) {
                                                    val targetZone = dragDropState.getZoneAtPosition(absoluteDropPos)
                                                    if (targetZone != null) {
                                                        // Drop to detected zone
                                                        onDropToZone(cardsBeingDragged, targetZone)

                                                        // Clear local state
                                                        localDraggedCardIds = emptySet()
                                                        localDragOffset = Offset.Zero
                                                        dragStartPixelPos = Offset.Zero
                                                        dragStartCursorOffset = Offset.Zero
                                                        targetPositions = emptyMap()

                                                        // Clear shared state
                                                        dragDropState.endDrag()
                                                        return@detectDragGestures
                                                    }
                                                }

                                                // Check if we're hovering over a zone - if so, drop to that zone
                                                val hoveredZone = dragDropState?.hoveredZone
                                                if (hoveredZone != null && onDropToZone != null && cardsBeingDragged.isNotEmpty()) {
                                                    // Drop to zone
                                                    onDropToZone(cardsBeingDragged, hoveredZone)

                                                    // Clear local state
                                                    localDraggedCardIds = emptySet()
                                                    localDragOffset = Offset.Zero
                                                    dragStartPixelPos = Offset.Zero
                                                    dragStartCursorOffset = Offset.Zero
                                                    targetPositions = emptyMap()

                                                    // Clear shared state
                                                    dragDropState?.endDrag()
                                                    return@detectDragGestures
                                                }

                                                // Check if a zone handled the drop - if so, skip battlefield repositioning
                                                if (dragDropState?.wasHandledByZone == true) {
                                                    localDraggedCardIds = emptySet()
                                                    localDragOffset = Offset.Zero
                                                    dragStartPixelPos = Offset.Zero
                                                    dragStartCursorOffset = Offset.Zero
                                                    targetPositions = emptyMap()
                                                    return@detectDragGestures
                                                }

                                                // If cards were already handled by a zone drop, skip repositioning
                                                if (cardsBeingDragged.isEmpty()) {
                                                    // Reset local state
                                                    localDraggedCardIds = emptySet()
                                                    localDragOffset = Offset.Zero
                                                    dragStartPixelPos = Offset.Zero
                                                    dragStartCursorOffset = Offset.Zero
                                                    return@detectDragGestures
                                                }

                                                // Additional safety check: verify cards are still on battlefield
                                                // If a zone already moved them, they won't be in our cards list
                                                val cardsStillOnBattlefield = cardsBeingDragged.filter { cardId ->
                                                    cards.any { it.instanceId == cardId }
                                                }.toSet()

                                                if (cardsStillOnBattlefield.isEmpty()) {
                                                    // Cards were moved to a zone, don't reposition
                                                    localDraggedCardIds = emptySet()
                                                    localDragOffset = Offset.Zero
                                                    dragStartPixelPos = Offset.Zero
                                                    dragStartCursorOffset = Offset.Zero
                                                    dragDropState?.endDrag()
                                                    return@detectDragGestures
                                                }

                                                // Use only cards still on battlefield for repositioning
                                                val finalCardsToReposition = cardsStillOnBattlefield

                                                // Calculate drop position from captured start + LOCAL drag offset
                                                val finalX = dragStartPixelPos.x + localDragOffset.x + (cardWidth.value / 2)
                                                val finalY = dragStartPixelPos.y + localDragOffset.y + (cardHeight.value / 2)

                                                // Calculate target grid cell from center point
                                                var targetCol = (finalX / cellWidth)
                                                    .toInt()
                                                    .coerceIn(0, columns - 1)
                                                var targetRow = (finalY / cellHeight)
                                                    .toInt()
                                                    .coerceAtLeast(0)
                                                    .coerceAtMost(9)

                                                // Count existing cards at target position (excluding cards being dragged)
                                                val cardsAtTarget = cards.filter { c ->
                                                    !finalCardsToReposition.contains(c.instanceId) &&
                                                    c.gridX == targetCol && c.gridY == targetRow
                                                }.size

                                                // Distribute cards to avoid exceeding stack limit of 3
                                                val cardsList = finalCardsToReposition.toList()
                                                cardsList.forEachIndexed { index, cardId ->
                                                    val currentStackSize = cardsAtTarget + index

                                                    if (currentStackSize < 3) {
                                                        // Still room in current stack
                                                        onCardPositionChanged(cardId, targetCol, targetRow)
                                                    } else {
                                                        // Find next available position
                                                        var searchCol = targetCol
                                                        var searchRow = targetRow
                                                        var found = false

                                                        while (!found && searchRow < 10) {
                                                            searchCol++
                                                            if (searchCol >= columns) {
                                                                searchCol = 0
                                                                searchRow++
                                                            }

                                                            if (searchRow >= 10) break

                                                            // Count cards at this search position (including already-placed dragged cards)
                                                            val cardsAtSearch = cards.filter { c ->
                                                                !finalCardsToReposition.contains(c.instanceId) &&
                                                                c.gridX == searchCol && c.gridY == searchRow
                                                            }.size + cardsList.take(index).count { placedId ->
                                                                // Check if we already placed a dragged card here
                                                                cards.find { it.instanceId == placedId }?.let {
                                                                    it.gridX == searchCol && it.gridY == searchRow
                                                                } ?: false
                                                            }

                                                            if (cardsAtSearch < 3) {
                                                                targetCol = searchCol
                                                                targetRow = searchRow
                                                                found = true
                                                            }
                                                        }

                                                        onCardPositionChanged(cardId, targetCol, targetRow)
                                                    }

                                                    // Store target position to prevent snap-back
                                                    targetPositions = targetPositions + (cardId to Pair(targetCol, targetRow))
                                                }

                                                // Clear dragged IDs immediately to stop drag gesture
                                                localDraggedCardIds = emptySet()

                                                // Clear shared state immediately so zones stop showing hover
                                                dragDropState?.endDrag()

                                                // Keep localDragOffset and dragStartPixelPos - they will be used
                                                // to maintain visual position until cards reach their target grid positions
                                                // The offset will be cleared once all targets are reached (see LaunchedEffect below)
                                            },
                                            onDragCancel = {
                                                // Reset local drag state
                                                localDraggedCardIds = emptySet()
                                                localDragOffset = Offset.Zero
                                                dragStartPixelPos = Offset.Zero
                                                dragStartCursorOffset = Offset.Zero
                                                targetPositions = emptyMap()

                                                // Also clear shared state
                                                dragDropState?.endDrag()
                                            }
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        BattlefieldCard(
                            cardInstance = card,
                            isLocalPlayer = isLocalPlayer,
                            onCardClick = onCardClick,
                            onContextAction = onContextAction,
                            selectionState = selectionState,
                            otherPlayers = otherPlayers,
                            ownerName = allPlayers.firstOrNull { it.id == card.ownerId }?.name ?: ""
                        )
                    }
        }
    }
}
