package com.commandermtg.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.commandermtg.models.CardInstance
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
    currentPlayerId: String? = null // ID of the player who can drag cards
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
    val cardWidth = 168.dp // Max width for tapped cards
    val cardHeight = 168.dp // Max height for tapped cards
    val gridSpacing = 8.dp
    val cellWidth = cardWidth.value + gridSpacing.value
    val cellHeight = cardHeight.value + gridSpacing.value

    // Track dragging state
    var draggedCardId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartPixelPos by remember { mutableStateOf(Offset.Zero) }

    // Container size
    var containerWidth by remember { mutableStateOf(0f) }

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

    // Card stacking offsets (Cockatrice-style)
    val stackOffsetX = (cardWidth.value * 0.33f).dp // CARD_WIDTH / 3
    val stackOffsetY = (gridSpacing.value * 0.33f).dp // PADDING_Y / 3

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
            positionMap[card.instanceId] = Pair(card.gridX!!, card.gridY!!)
        }

        // Then, auto-arrange cards without positions
        val cardsWithoutPositions = cards.filter { it.gridX == null || it.gridY == null }
        var nextRow = 0
        var nextCol = 0

        // Find next available position (with stacking support)
        fun findNextAvailablePosition(): Pair<Int, Int> {
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
            return findNextAvailablePosition()
        }

        cardsWithoutPositions.forEach { card ->
            val (col, row) = findNextAvailablePosition()
            positionMap[card.instanceId] = Pair(col, row)
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

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width.toFloat()
            }
            .fillMaxWidth()
            .height((totalRows * cellHeight).dp)
    ) {
                cards.forEach { card ->
                    val position = gridPositions[card.instanceId] ?: Pair(0, 0)
                    val (col, row) = position
                    val stackInfo = stackInfoMap[card.instanceId]

                    val isDragged = draggedCardId == card.instanceId
                    val canDrag = currentPlayerId != null && card.ownerId == currentPlayerId

                    // Base position
                    var xPos = col * cellWidth
                    var yPos = row * cellHeight

                    // Apply stacking offset (Cockatrice-style)
                    if (stackInfo != null) {
                        xPos += stackInfo.stackIndex * stackOffsetX.value
                        yPos += stackInfo.stackIndex * stackOffsetY.value
                    }

                    val finalOffset = if (isDragged) {
                        IntOffset(
                            (xPos + dragOffset.x).roundToInt(),
                            (yPos + dragOffset.y).roundToInt()
                        )
                    } else {
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
                                            onDragStart = { _ ->
                                                draggedCardId = card.instanceId
                                                dragOffset = Offset.Zero
                                                // Look up current position from map and calculate pixel position
                                                val currentPos = gridPositions[card.instanceId] ?: Pair(0, 0)
                                                val currentStack = stackInfoMap[card.instanceId]
                                                val startX = currentPos.first * cellWidth + (currentStack?.stackIndex ?: 0) * stackOffsetX.value
                                                val startY = currentPos.second * cellHeight + (currentStack?.stackIndex ?: 0) * stackOffsetY.value
                                                dragStartPixelPos = Offset(startX, startY)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount
                                            },
                                            onDragEnd = {
                                                // Calculate drop position from captured start + drag offset
                                                val finalX = dragStartPixelPos.x + dragOffset.x + (cardWidth.value / 2)
                                                val finalY = dragStartPixelPos.y + dragOffset.y + (cardHeight.value / 2)

                                                // Calculate target grid cell from center point
                                                val newCol = (finalX / cellWidth)
                                                    .toInt()
                                                    .coerceIn(0, columns - 1)
                                                val newRow = (finalY / cellHeight)
                                                    .toInt()
                                                    .coerceAtLeast(0)
                                                    .coerceAtMost(9)

                                                // Update position
                                                onCardPositionChanged(card.instanceId, newCol, newRow)

                                                // Reset drag state
                                                draggedCardId = null
                                                dragOffset = Offset.Zero
                                                dragStartPixelPos = Offset.Zero
                                            },
                                            onDragCancel = {
                                                draggedCardId = null
                                                dragOffset = Offset.Zero
                                                dragStartPixelPos = Offset.Zero
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
                            onContextAction = onContextAction
                        )
                    }
        }
    }
}
