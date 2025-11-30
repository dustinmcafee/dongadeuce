package com.dustinmcafee.dongadeuce.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.CardInstance
import com.dustinmcafee.dongadeuce.models.Zone

/**
 * State holder for the focused card in the persistent viewer (Android)
 */
class FocusedCardState {
    var focusedCard by mutableStateOf<CardInstance?>(null)
        private set

    fun updateFocusedCard(card: CardInstance?) {
        focusedCard = card
    }

    fun clearFocusedCard() {
        focusedCard = null
    }
}

@Composable
fun rememberFocusedCardState(): FocusedCardState {
    return remember { FocusedCardState() }
}

/**
 * State holder for the card viewer drawer
 */
class CardViewerDrawerState {
    var isOpen by mutableStateOf(false)
        private set

    var dragOffset by mutableStateOf(0f)
        private set

    fun open() {
        isOpen = true
        dragOffset = 0f
    }

    fun close() {
        isOpen = false
        dragOffset = 0f
    }

    fun toggle() {
        if (isOpen) close() else open()
    }

    fun updateDragOffset(delta: Float) {
        dragOffset = (dragOffset + delta).coerceAtLeast(0f)
    }

    fun resetDragOffset() {
        dragOffset = 0f
    }
}

@Composable
fun rememberCardViewerDrawerState(): CardViewerDrawerState {
    return remember { CardViewerDrawerState() }
}

private enum class ViewerTab { IMAGE, TEXT }

/**
 * Swipe-from-right card viewer drawer for Android.
 * Covers ~70% of screen width when open.
 */
@Composable
fun CardViewerDrawer(
    cardInstance: CardInstance?,
    drawerState: CardViewerDrawerState,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val drawerWidth = screenWidthDp * 0.75f
    val density = LocalDensity.current
    val drawerWidthPx = with(density) { drawerWidth.toPx() }

    // Animate the drawer position
    val targetOffset = if (drawerState.isOpen) 0f else drawerWidthPx
    val animatedOffset by animateFloatAsState(
        targetValue = targetOffset + drawerState.dragOffset,
        animationSpec = tween(durationMillis = if (drawerState.dragOffset != 0f) 0 else 250),
        label = "drawerOffset"
    )

    var selectedTab by remember { mutableStateOf(ViewerTab.IMAGE) }

    // Only render when drawer should be visible
    if (drawerState.isOpen || animatedOffset < drawerWidthPx) {
        Box(modifier = modifier.fillMaxSize()) {
            // Semi-transparent scrim when drawer is open
            if (drawerState.isOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f * (1f - animatedOffset / drawerWidthPx)))
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (drawerState.dragOffset > drawerWidthPx * 0.3f) {
                                        drawerState.close()
                                    } else {
                                        drawerState.resetDragOffset()
                                    }
                                },
                                onDragCancel = { drawerState.resetDragOffset() },
                                onHorizontalDrag = { _, dragAmount ->
                                    if (dragAmount > 0) { // Only allow dragging right (to close)
                                        drawerState.updateDragOffset(dragAmount)
                                    }
                                }
                            )
                        }
                )
            }

            // Drawer panel
            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(drawerWidth)
                    .align(Alignment.CenterEnd)
                    .offset(x = with(density) { animatedOffset.toDp() })
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (drawerState.dragOffset > drawerWidthPx * 0.3f) {
                                    drawerState.close()
                                } else {
                                    drawerState.resetDragOffset()
                                }
                            },
                            onDragCancel = { drawerState.resetDragOffset() },
                            onHorizontalDrag = { _, dragAmount ->
                                if (dragAmount > 0) { // Only allow dragging right (to close)
                                    drawerState.updateDragOffset(dragAmount)
                                }
                            }
                        )
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header with close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Card Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { drawerState.close() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Divider()

                    if (cardInstance == null) {
                        // Empty state
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tap a card to preview\n\nSwipe right to close",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Tab row
                        TabRow(
                            selectedTabIndex = selectedTab.ordinal,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = selectedTab == ViewerTab.IMAGE,
                                onClick = { selectedTab = ViewerTab.IMAGE },
                                text = { Text("Image") }
                            )
                            Tab(
                                selected = selectedTab == ViewerTab.TEXT,
                                onClick = { selectedTab = ViewerTab.TEXT },
                                text = { Text("Text") }
                            )
                        }

                        // Content based on selected tab
                        when (selectedTab) {
                            ViewerTab.IMAGE -> ImageTabContent(cardInstance)
                            ViewerTab.TEXT -> TextTabContent(cardInstance)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Invisible edge detector for swipe-from-right gesture.
 * Place this on the right edge of the screen.
 */
@Composable
fun SwipeEdgeDetector(
    drawerState: CardViewerDrawerState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var totalDrag by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .width(24.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        if (totalDrag < -100f) { // Swiped left enough
                            drawerState.open()
                        }
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    }
                )
            }
    )
}

@Composable
private fun ImageTabContent(cardInstance: CardInstance) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card image - large
        val imageUrl = if (cardInstance.isFlipped) {
            "https://cards.scryfall.io/back.png"
        } else {
            cardInstance.card.imageUri
        }

        CardImage(
            imageUrl = imageUrl,
            contentDescription = cardInstance.card.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.714f) // Standard MTG card ratio
                .clip(RoundedCornerShape(12.dp))
        )

        // Card name
        Text(
            text = cardInstance.card.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Mana cost
        cardInstance.card.manaCost?.let { manaCost ->
            if (manaCost.isNotEmpty()) {
                Text(
                    text = manaCost,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Type line
        Text(
            text = cardInstance.card.type ?: "Unknown",
            style = MaterialTheme.typography.bodyMedium
        )

        // P/T for creatures
        val basePower = cardInstance.card.power
        val baseToughness = cardInstance.card.toughness
        if (basePower != null && baseToughness != null) {
            val currentPower = (basePower.toIntOrNull() ?: 0) + cardInstance.powerModifier
            val currentToughness = (baseToughness.toIntOrNull() ?: 0) + cardInstance.toughnessModifier
            val isModified = cardInstance.powerModifier != 0 || cardInstance.toughnessModifier != 0

            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isModified) {
                        "$currentPower/$currentToughness (base: $basePower/$baseToughness)"
                    } else {
                        "$basePower/$baseToughness"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        !isModified -> Color.White
                        cardInstance.powerModifier > 0 || cardInstance.toughnessModifier > 0 -> Color.Green
                        else -> Color.Red
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Status indicators
        if (cardInstance.zone == Zone.BATTLEFIELD) {
            StatusIndicatorsSection(cardInstance)
        }
    }
}

@Composable
private fun TextTabContent(cardInstance: CardInstance) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card name
        Text(
            text = cardInstance.card.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Mana cost
        cardInstance.card.manaCost?.let { manaCost ->
            if (manaCost.isNotEmpty()) {
                Text(
                    text = manaCost,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Type line
        Text(
            text = cardInstance.card.type ?: "Unknown",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Divider()

        // Oracle text
        cardInstance.card.oracleText?.let { oracleText ->
            if (oracleText.isNotEmpty()) {
                Text(
                    text = oracleText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Power/Toughness for creatures
        val basePower = cardInstance.card.power
        val baseToughness = cardInstance.card.toughness
        if (basePower != null && baseToughness != null) {
            val currentPower = (basePower.toIntOrNull() ?: 0) + cardInstance.powerModifier
            val currentToughness = (baseToughness.toIntOrNull() ?: 0) + cardInstance.toughnessModifier
            val isModified = cardInstance.powerModifier != 0 || cardInstance.toughnessModifier != 0

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isModified) {
                            "$currentPower/$currentToughness (base: $basePower/$baseToughness)"
                        } else {
                            "$basePower/$baseToughness"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            !isModified -> Color.White
                            cardInstance.powerModifier > 0 || cardInstance.toughnessModifier > 0 -> Color.Green
                            else -> Color.Red
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Card state info (counters, status, etc.)
        if (cardInstance.zone == Zone.BATTLEFIELD) {
            Divider()
            StatusIndicatorsSection(cardInstance)
        }
    }
}

@Composable
private fun StatusIndicatorsSection(cardInstance: CardInstance) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Status chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (cardInstance.isTapped) {
                StatusChip("Tapped", MaterialTheme.colorScheme.error)
            }
            if (cardInstance.isFlipped) {
                StatusChip("Face Down", Color.Gray)
            }
            if (cardInstance.doesntUntap) {
                StatusChip("Doesn't Untap", Color.Magenta)
            }
            if (cardInstance.isToken) {
                StatusChip("Token", Color.Green)
            }
            if (cardInstance.isClone) {
                StatusChip("Copy", Color.Cyan)
            }
        }

        // Counters
        if (cardInstance.counters.isNotEmpty()) {
            Text(
                text = "Counters:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cardInstance.counters.forEach { (type, count) ->
                    val counterType = UIConstants.COUNTER_TYPES.find { it.id == type }
                    val displayName = counterType?.displayName ?: type
                    val counterColor = counterType?.color ?: Color.White

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = counterColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Annotation
        if (!cardInstance.annotation.isNullOrBlank()) {
            Surface(
                color = Color.Yellow.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📝", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = cardInstance.annotation!!,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
