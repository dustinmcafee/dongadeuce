package com.commandermtg.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import com.commandermtg.models.CardInstance
import com.commandermtg.models.Zone

/**
 * State holder for drag and drop operations
 */
@Stable
class DragDropState {
    var draggedCard by mutableStateOf<CardInstance?>(null)
        private set

    var dragOffset by mutableStateOf(Offset.Zero)
        private set

    var isDragging by mutableStateOf(false)
        private set

    fun startDrag(card: CardInstance, offset: Offset = Offset.Zero) {
        draggedCard = card
        dragOffset = offset
        isDragging = true
    }

    fun updateDragPosition(offset: Offset) {
        dragOffset = offset
    }

    fun endDrag() {
        draggedCard = null
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
