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
    var dragStartGridPos by remember { mutableStateOf<Pair<Int, Int>?>(null) }

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

    // Organize cards into grid positions
    // Key by the grid positions of all cards to ensure recalculation when positions change
    val gridPositionsKey = remember(cards) {
        cards.map { "${it.instanceId}:${it.gridX}:${it.gridY}" }.joinToString(",")
    }

    val cardPositions = remember(gridPositionsKey, columns) {
        val positionMap = mutableMapOf<String, Pair<Int, Int>>()

        // First, place cards that have explicit grid positions
        val cardsWithPositions = cards.filter { it.gridX != null && it.gridY != null }
        cardsWithPositions.forEach { card ->
            positionMap[card.instanceId] = Pair(card.gridX!!, card.gridY!!)
        }

        // Then, auto-arrange cards without positions
        val cardsWithoutPositions = cards.filter { it.gridX == null || it.gridY == null }
        var nextRow = 0
        var nextCol = 0

        // Find next available position
        fun findNextAvailablePosition(): Pair<Int, Int> {
            while (positionMap.containsValue(Pair(nextCol, nextRow))) {
                nextCol++
                if (nextCol >= columns) {
                    nextCol = 0
                    nextRow++
                }
            }
            return Pair(nextCol, nextRow)
        }

        cardsWithoutPositions.forEach { card ->
            val (col, row) = findNextAvailablePosition()
            positionMap[card.instanceId] = Pair(col, row)
            nextCol++
            if (nextCol >= columns) {
                nextCol = 0
                nextRow++
            }
        }

        positionMap
    }

    // Calculate total rows needed
    val totalRows = ((cardPositions.values.maxOfOrNull { it.second } ?: 0) + 1).coerceAtMost(10)

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width.toFloat()
            }
            .fillMaxWidth()
            .height((totalRows * cellHeight).dp)
    ) {
                cards.forEach { card ->
                    val position = cardPositions[card.instanceId] ?: Pair(0, 0)
                    val (col, row) = position

                    val isDragged = draggedCardId == card.instanceId
                    val canDrag = currentPlayerId != null && card.ownerId == currentPlayerId

                    val xPos = col * cellWidth
                    val yPos = row * cellHeight

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
                            .then(
                                if (canDrag) {
                                    Modifier.pointerInput(card.instanceId) {
                                        detectDragGestures(
                                            onDragStart = { _ ->
                                                draggedCardId = card.instanceId
                                                dragOffset = Offset.Zero
                                                // Store the starting grid position
                                                dragStartGridPos = Pair(col, row)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount
                                            },
                                            onDragEnd = {
                                                // Use the captured starting grid position to calculate drop position
                                                val startGridPos = dragStartGridPos ?: Pair(col, row)
                                                val startX = startGridPos.first * cellWidth
                                                val startY = startGridPos.second * cellHeight

                                                // Calculate final position from start + drag offset
                                                val finalX = startX + dragOffset.x + (cardWidth.value / 2)
                                                val finalY = startY + dragOffset.y + (cardHeight.value / 2)

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
                                                dragStartGridPos = null
                                            },
                                            onDragCancel = {
                                                draggedCardId = null
                                                dragOffset = Offset.Zero
                                                dragStartGridPos = null
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
