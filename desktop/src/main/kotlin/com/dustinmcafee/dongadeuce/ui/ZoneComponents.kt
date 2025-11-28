@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.*

/**
 * A card representing a game zone (Library, Hand, Graveyard, Exile, Command Zone)
 * Supports click, double-click, right-click, and drag-drop
 */
@Composable
fun ZoneCard(
    label: String,
    zone: Zone,
    cardCount: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onRightClick: (() -> Unit)? = null,
    dragDropState: DragDropState? = null,
    onDropCards: ((List<String>) -> Unit)? = null,
    imageUrl: String? = null  // Optional image to display in the zone (e.g., card back for library)
) {
    var isHovering by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }

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
                    dragDropState.registerZoneBounds(zone, bounds)
                }
            }
            .border(
                width = if (isDraggingOver) 3.dp else 1.dp,
                color = if (isDraggingOver) Color(0xFF00FF00) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .then(
                if (onDoubleClick != null || onRightClick != null || onClick != null) {
                    Modifier.pointerInput(zone) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()

                                // Handle right-click
                                if (event.buttons.isSecondaryPressed && onRightClick != null) {
                                    onRightClick()
                                }
                                // Handle left-click for double-click detection
                                else if (event.changes.any { !it.pressed && it.previousPressed }) {
                                    val change = event.changes.first { !it.pressed && it.previousPressed }
                                    change.consume()

                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastClickTime < 300L && onDoubleClick != null) {
                                        // Double-click detected
                                        onDoubleClick()
                                        lastClickTime = 0L
                                    } else {
                                        // First click
                                        lastClickTime = currentTime
                                        onClick?.invoke()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (onDropCards != null && dragDropState != null) {
                    Modifier.pointerInput(zone) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        if (dragDropState.draggedCardIds.isNotEmpty()) {
                                            isHovering = true
                                            dragDropState.setHoveredZone(zone)
                                        }
                                    }
                                    PointerEventType.Exit -> {
                                        isHovering = false
                                        if (dragDropState.hoveredZone == zone) {
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
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDraggingOver)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                // Show image with count overlay
                Box(modifier = Modifier.fillMaxSize()) {
                    CardImage(
                        imageUrl = imageUrl,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Count overlay at bottom
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(2.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            "$label ($cardCount)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    // Drop indicator overlay
                    if (isDraggingOver) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x8800FF00)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Drop here",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // Standard text-based display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                    if (cardCount > 0) {
                        Text("($cardCount)", style = MaterialTheme.typography.bodySmall)
                    }
                    if (isDraggingOver) {
                        Text(
                            "Drop here",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Library zone card that can optionally show the top card
 * Supports "reveal top card" (visible to all) and "look at top card" (visible to owner only)
 */
@Composable
fun LibraryZoneCard(
    cardCount: Int,
    topCard: CardInstance?,
    revealTopCard: Boolean,
    lookAtTopCard: Boolean,
    isOwner: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    dragDropState: DragDropState? = null,
    onDropCards: ((List<String>) -> Unit)? = null
) {
    var isHovering by remember { mutableStateOf(false) }

    val isDraggingOver = dragDropState != null &&
                        dragDropState.draggedCardIds.isNotEmpty() &&
                        isHovering

    // Determine if we should show the top card
    val showTopCard = topCard != null && (revealTopCard || (lookAtTopCard && isOwner))

    Card(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                if (dragDropState != null) {
                    val bounds = Rect(
                        coordinates.positionInWindow().x,
                        coordinates.positionInWindow().y,
                        coordinates.positionInWindow().x + coordinates.size.width,
                        coordinates.positionInWindow().y + coordinates.size.height
                    )
                    dragDropState.registerZoneBounds(Zone.LIBRARY, bounds)
                }
            }
            .border(
                width = if (isDraggingOver) 3.dp else 1.dp,
                color = if (isDraggingOver) Color(0xFF00FF00) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .then(
                if (onClick != null) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.any { !it.pressed && it.previousPressed }) {
                                    val change = event.changes.first { !it.pressed && it.previousPressed }
                                    change.consume()
                                    onClick()
                                }
                            }
                        }
                    }
                } else Modifier
            )
            .then(
                if (onDropCards != null && dragDropState != null) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        if (dragDropState.draggedCardIds.isNotEmpty()) {
                                            isHovering = true
                                            dragDropState.setHoveredZone(Zone.LIBRARY)
                                        }
                                    }
                                    PointerEventType.Exit -> {
                                        isHovering = false
                                        if (dragDropState.hoveredZone == Zone.LIBRARY) {
                                            dragDropState.setHoveredZone(null)
                                        }
                                    }
                                    PointerEventType.Release -> {
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
            containerColor = if (isDraggingOver)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Always show the library with card image
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                // Card image - show card back or revealed top card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(2.dp)
                ) {
                    if (showTopCard && topCard != null) {
                        // Show the revealed top card
                        if (topCard.card.imageUri != null) {
                            CardImage(
                                imageUrl = topCard.card.imageUri,
                                contentDescription = topCard.card.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Fallback text for cards without images
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        topCard.card.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Indicator badge for reveal/look mode
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = if (revealTopCard)
                                Color(0xFF4CAF50).copy(alpha = 0.9f)  // Green for revealed
                            else
                                Color(0xFF2196F3).copy(alpha = 0.9f)  // Blue for look only
                        ) {
                            Text(
                                if (revealTopCard) "R" else "L",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        // Show standard Magic card back
                        CardImage(
                            imageUrl = "https://cards.scryfall.io/back.png",
                            contentDescription = "Library",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Drop indicator overlay
                    if (isDraggingOver) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x8800FF00)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Drop here",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }

                // Card count at bottom
                Text(
                    "Library ($cardCount)",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Dialog showing expanded hand view with card actions
 */
@Composable
fun HandDialog(
    cards: List<CardInstance>,
    onDismiss: () -> Unit,
    onPlayCard: (CardInstance) -> Unit,
    onDiscard: (CardInstance) -> Unit = {},
    onExile: (CardInstance) -> Unit = {},
    onToLibrary: (CardInstance) -> Unit = {},
    onContextAction: (CardAction) -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your Hand (${cards.size} cards)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cards.isEmpty()) {
                    Text("No cards in hand", style = MaterialTheme.typography.bodyMedium)
                } else {
                    cards.forEach { cardInstance ->
                        CardWithContextMenu(
                            cardInstance = cardInstance,
                            onAction = onContextAction
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Card image thumbnail
                                        CardImageThumbnail(
                                            imageUrl = cardInstance.card.imageUri,
                                            contentDescription = cardInstance.card.name
                                        )

                                        // Card info
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cardInstance.card.name,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            val manaCost = cardInstance.card.manaCost
                                            if (manaCost != null) {
                                                Text(
                                                    text = manaCost,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            val cardType = cardInstance.card.type
                                            if (cardType != null) {
                                                Text(
                                                    text = cardType,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }

                                    // Action buttons row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Button(
                                            onClick = { onPlayCard(cardInstance) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Play", style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = { onDiscard(cardInstance) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Discard", style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = { onExile(cardInstance) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Exile", style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = { onToLibrary(cardInstance) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("To Library", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Card type categories for column-based display
 */
enum class CardTypeCategory(val displayName: String) {
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
private fun getCardCategory(typeLine: String?): CardTypeCategory {
    val type = typeLine?.lowercase() ?: ""
    return when {
        type.contains("creature") -> CardTypeCategory.CREATURE
        type.contains("planeswalker") -> CardTypeCategory.PLANESWALKER
        type.contains("instant") -> CardTypeCategory.INSTANT
        type.contains("sorcery") -> CardTypeCategory.SORCERY
        type.contains("enchantment") -> CardTypeCategory.ENCHANTMENT
        type.contains("artifact") -> CardTypeCategory.ARTIFACT
        type.contains("land") -> CardTypeCategory.LAND
        else -> CardTypeCategory.OTHER
    }
}

/**
 * Comprehensive hand/zone viewer dialog with column-based layout by card type (Cockatrice-style)
 * Cards are grouped by type into columns and sorted alphabetically within each column
 */
@Composable
fun ViewHandDialog(
    cards: List<CardInstance>,
    playerName: String,
    onDismiss: () -> Unit,
    onPlayCard: (CardInstance) -> Unit,
    onDiscard: (CardInstance) -> Unit = {},
    onExile: (CardInstance) -> Unit = {},
    onToLibrary: (CardInstance) -> Unit = {},
    onContextAction: (CardAction) -> Unit = {}
) {
    // Group cards by type category and sort alphabetically within each group
    val cardsByCategory = remember(cards) {
        cards
            .groupBy { getCardCategory(it.card.type) }
            .mapValues { (_, cardList) -> cardList.sortedBy { it.card.name.lowercase() } }
    }

    // Get non-empty categories in display order
    val activeCategories = remember(cardsByCategory) {
        CardTypeCategory.entries.filter { cardsByCategory[it]?.isNotEmpty() == true }
    }

    // Use Dialog for better control over sizing
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
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
                    "${playerName}'s Hand (${cards.size} cards)",
                    style = MaterialTheme.typography.headlineSmall
                )

                Divider()

                if (cards.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No cards in hand", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    // Horizontal scrollable row of columns (one per card type)
                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeCategories.forEach { category ->
                                val categoryCards = cardsByCategory[category] ?: emptyList()

                                // Column for this card type
                                Card(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .fillMaxHeight(),
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

                                        // Vertically scrollable list of cards
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            categoryCards.forEach { cardInstance ->
                                                ViewHandCardItem(
                                                    cardInstance = cardInstance,
                                                    onPlayCard = onPlayCard,
                                                    onDiscard = onDiscard,
                                                    onExile = onExile,
                                                    onToLibrary = onToLibrary,
                                                    onContextAction = onContextAction
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

/**
 * Individual card item in the View Hand dialog with right-click context menu
 */
@Composable
private fun ViewHandCardItem(
    cardInstance: CardInstance,
    onPlayCard: (CardInstance) -> Unit,
    onDiscard: (CardInstance) -> Unit,
    onExile: (CardInstance) -> Unit,
    onToLibrary: (CardInstance) -> Unit,
    onContextAction: (CardAction) -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
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
                .clickable { onPlayCard(cardInstance) },
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
                text = { Text("To Battlefield") },
                onClick = {
                    showContextMenu = false
                    onPlayCard(cardInstance)
                }
            )
            DropdownMenuItem(
                text = { Text("To Graveyard") },
                onClick = {
                    showContextMenu = false
                    onDiscard(cardInstance)
                }
            )
            DropdownMenuItem(
                text = { Text("To Exile") },
                onClick = {
                    showContextMenu = false
                    onExile(cardInstance)
                }
            )
            DropdownMenuItem(
                text = { Text("To Library") },
                onClick = {
                    showContextMenu = false
                    onToLibrary(cardInstance)
                }
            )
            DropdownMenuItem(
                text = { Text("To Hand") },
                onClick = {
                    showContextMenu = false
                    onContextAction(CardAction.ToHand(cardInstance))
                }
            )
        }
    }
}

/**
 * Display a single card in the hand area (compact thumbnail view)
 */
@Composable
fun HandCardDisplay(
    cardInstance: CardInstance,
    onCardClick: (CardInstance) -> Unit,
    onDoubleClick: () -> Unit = {},
    onContextAction: (CardAction) -> Unit,
    selectionState: SelectionState? = null,
    sharedDraggedCardIds: Set<String> = emptySet(),
    sharedDragOffset: Offset = Offset.Zero,
    onDragStateChange: (Set<String>, Offset) -> Unit = { _, _ -> },
    otherPlayers: List<Player> = emptyList()
) {
    var lastClickTime by remember { mutableStateOf(0L) }
    val isSelected = selectionState?.isSelected(cardInstance.instanceId) == true
    val isDragged = sharedDraggedCardIds.contains(cardInstance.instanceId)

    CardWithContextMenu(
        cardInstance = cardInstance,
        onAction = onContextAction,
        otherPlayers = otherPlayers
    ) {
        Box {
            Card(
                modifier = Modifier
                    .width(UIConstants.HAND_CARD_WIDTH)
                    .height(UIConstants.HAND_CARD_HEIGHT)
                    .graphicsLayer {
                        if (isDragged) {
                            alpha = 0.5f
                            translationX = sharedDragOffset.x
                            translationY = sharedDragOffset.y
                        }
                    }
                    // Combined click and drag gesture support
                    .pointerInput(cardInstance.instanceId) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()

                                if (event.type == PointerEventType.Press) {
                                    // Skip right-clicks (they trigger context menu)
                                    val isPrimaryClick = event.button == PointerButton.Primary
                                    val isRightClick = !isPrimaryClick && event.button != null

                                    if (isRightClick) {
                                        // Right-click detected - consume and skip
                                        awaitPointerEvent()
                                        continue
                                    }

                                    // Left click - handle selection and drag
                                    val isShiftPressed = event.keyboardModifiers.isShiftPressed
                                    var totalDrag = Offset.Zero
                                    var isDragging = false

                                    // Track drag or click
                                    while (true) {
                                        val moveEvent = awaitPointerEvent()

                                        if (moveEvent.type == PointerEventType.Move) {
                                            val change = moveEvent.changes.first()
                                            val dragAmount = change.position - change.previousPosition
                                            totalDrag += dragAmount

                                            // Start drag if moved more than threshold
                                            if (!isDragging && totalDrag.getDistance() > UIConstants.DRAG_THRESHOLD_PX) {
                                                isDragging = true
                                                val cardsToDrag = if (selectionState?.isSelected(cardInstance.instanceId) == true &&
                                                                       selectionState.selectedCards.size > 1) {
                                                    selectionState.selectedCards.toSet()
                                                } else {
                                                    setOf(cardInstance.instanceId)
                                                }
                                                onDragStateChange(cardsToDrag, Offset.Zero)
                                            }

                                            if (isDragging) {
                                                change.consume()
                                                onDragStateChange(sharedDraggedCardIds, sharedDragOffset + dragAmount)
                                            }
                                        } else if (moveEvent.type == PointerEventType.Release) {
                                            if (isDragging) {
                                                // Drag ended
                                                if (sharedDragOffset.getDistance() > UIConstants.DRAG_DISTANCE_THRESHOLD_PX) {
                                                    onContextAction(CardAction.ToBattlefield(cardInstance))
                                                }
                                                onDragStateChange(emptySet(), Offset.Zero)
                                            } else {
                                                // Click (no drag)
                                                val clickTime = System.currentTimeMillis()
                                                val timeSinceLastClick = clickTime - lastClickTime
                                                lastClickTime = clickTime

                                                if (timeSinceLastClick < UIConstants.DOUBLE_CLICK_DELAY_MS) {
                                                    selectionState?.clearSelection()
                                                    onDoubleClick()
                                                } else if (isShiftPressed && selectionState != null) {
                                                    selectionState.toggleSelection(cardInstance.instanceId)
                                                } else {
                                                    if (selectionState != null) {
                                                        selectionState.clearSelection()
                                                        selectionState.select(cardInstance.instanceId)
                                                    } else {
                                                        onCardClick(cardInstance)
                                                    }
                                                }
                                            }
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .then(
                        if (isSelected) Modifier.border(3.dp, Color(0xFF00FF00), RoundedCornerShape(8.dp)) else Modifier
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (cardInstance.card.imageUri != null) {
                        CardImage(
                            imageUrl = cardInstance.card.imageUri,
                            contentDescription = cardInstance.card.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Fallback text display
                        Text(
                            text = cardInstance.card.name,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(4.dp),
                            maxLines = 3,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
