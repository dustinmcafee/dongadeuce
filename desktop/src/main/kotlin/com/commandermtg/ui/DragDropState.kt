package com.commandermtg.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import com.commandermtg.models.CardInstance
import com.commandermtg.models.Zone

/**
 * State holder for drag and drop operations
 * Supports both single and multi-card dragging
 */
@Stable
class DragDropState {
    var draggedCard by mutableStateOf<CardInstance?>(null)
        private set

    // Track multiple dragged cards for multi-select drag operations
    var draggedCardIds by mutableStateOf<Set<String>>(emptySet())
        private set

    var dragOffset by mutableStateOf(Offset.Zero)
        private set

    var isDragging by mutableStateOf(false)
        private set

    fun startDrag(card: CardInstance, offset: Offset = Offset.Zero) {
        draggedCard = card
        draggedCardIds = setOf(card.instanceId)
        dragOffset = offset
        isDragging = true
    }

    fun startDragMultiple(cardIds: Set<String>, offset: Offset = Offset.Zero) {
        draggedCardIds = cardIds
        draggedCard = null // Not tracking single card in multi-drag
        dragOffset = offset
        isDragging = true
    }

    fun updateDragPosition(offset: Offset) {
        dragOffset = offset
    }

    fun endDrag() {
        draggedCard = null
        draggedCardIds = emptySet()
        dragOffset = Offset.Zero
        isDragging = false
    }
}

/**
 * Composable to provide drag-drop state
 */
@Composable
fun rememberDragDropState(): DragDropState {
    return remember { DragDropState() }
}

/**
 * Drop target zones
 */
enum class DropTarget {
    BATTLEFIELD,
    GRAVEYARD,
    EXILE,
    LIBRARY,
    COMMAND_ZONE,
    NONE
}

/**
 * Convert DropTarget to Zone
 */
fun DropTarget.toZone(): Zone? {
    return when (this) {
        DropTarget.BATTLEFIELD -> Zone.BATTLEFIELD
        DropTarget.GRAVEYARD -> Zone.GRAVEYARD
        DropTarget.EXILE -> Zone.EXILE
        DropTarget.LIBRARY -> Zone.LIBRARY
        DropTarget.COMMAND_ZONE -> Zone.COMMAND_ZONE
        DropTarget.NONE -> null
    }
}
