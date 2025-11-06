package com.dustinmcafee.dongadeuce.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.dustinmcafee.dongadeuce.models.CardInstance
import com.dustinmcafee.dongadeuce.models.Zone

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

    // Track if a zone handled the drop (to prevent battlefield from repositioning)
    var wasHandledByZone by mutableStateOf(false)
        private set

    // Track which zone is currently being hovered over during drag
    private var _hoveredZone by mutableStateOf<Zone?>(null)
    val hoveredZone: Zone? get() = _hoveredZone

    // Track zone bounds for accurate drop detection
    private val zoneBounds = mutableStateMapOf<Zone, androidx.compose.ui.geometry.Rect>()

    fun registerZoneBounds(zone: Zone, bounds: androidx.compose.ui.geometry.Rect) {
        zoneBounds[zone] = bounds
    }

    fun getZoneAtPosition(position: Offset): Zone? {
        return zoneBounds.entries.firstOrNull { (_, bounds) ->
            bounds.contains(position)
        }?.key
    }

    fun startDrag(card: CardInstance, offset: Offset = Offset.Zero) {
        draggedCard = card
        draggedCardIds = setOf(card.instanceId)
        dragOffset = offset
        isDragging = true
        wasHandledByZone = false
        _hoveredZone = null
    }

    fun startDragMultiple(cardIds: Set<String>, offset: Offset = Offset.Zero) {
        draggedCardIds = cardIds
        draggedCard = null // Not tracking single card in multi-drag
        dragOffset = offset
        isDragging = true
        wasHandledByZone = false
        _hoveredZone = null
    }

    fun updateDragPosition(offset: Offset) {
        dragOffset = offset
    }

    fun setHoveredZone(zone: Zone?) {
        _hoveredZone = zone
    }

    fun markHandledByZone() {
        wasHandledByZone = true
    }

    fun endDrag() {
        draggedCard = null
        draggedCardIds = emptySet()
        dragOffset = Offset.Zero
        isDragging = false
        wasHandledByZone = false
        _hoveredZone = null
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
