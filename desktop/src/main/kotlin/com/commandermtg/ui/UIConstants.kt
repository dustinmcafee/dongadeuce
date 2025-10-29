package com.commandermtg.ui

import androidx.compose.ui.unit.dp

/**
 * UI constants for card dimensions and layout
 */
object UIConstants {
    // Battlefield card dimensions
    val BATTLEFIELD_CARD_WIDTH = 120.dp
    val BATTLEFIELD_CARD_HEIGHT = 168.dp
    val BATTLEFIELD_CARD_TAPPED_SIZE = 168.dp // Square space when rotated

    // Hand card dimensions
    val HAND_CARD_WIDTH = 60.dp
    val HAND_CARD_HEIGHT = 84.dp

    // Card selection border
    val SELECTION_BORDER_WIDTH = 3.dp
    val SELECTED_BORDER_WIDTH = 5.dp

    // Card stacking offsets (25% of card size)
    val STACK_OFFSET_RATIO = 0.25f

    // Drag threshold to distinguish click from drag
    val DRAG_THRESHOLD_PX = 5f
    val DRAG_DISTANCE_THRESHOLD_PX = 20f

    // Double-click timing
    const val DOUBLE_CLICK_DELAY_MS = 300L

    // Card corner radius
    val CARD_CORNER_RADIUS = 8.dp

    // Card elevation
    val CARD_ELEVATION = 4.dp
}
