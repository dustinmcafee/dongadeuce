package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.CardInstance
import java.awt.Cursor

/**
 * State holder for the focused card in the persistent viewer
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
 * State holder for resizable sidebar dimensions
 */
class ResizableSidebarState(
    initialWidth: Dp = 280.dp,
    initialViewerHeight: Dp = 300.dp
) {
    var sidebarWidth by mutableStateOf(initialWidth)
        private set
    var viewerHeight by mutableStateOf(initialViewerHeight)
        private set

    fun updateWidth(delta: Float) {
        // Negative delta = dragging left = increase width
        val newWidth = sidebarWidth - delta.dp
        sidebarWidth = newWidth.coerceIn(200.dp, 500.dp)
    }

    fun updateViewerHeight(delta: Float) {
        val newHeight = viewerHeight + delta.dp
        viewerHeight = newHeight.coerceIn(150.dp, 600.dp)
    }
}

@Composable
fun rememberResizableSidebarState(
    initialWidth: Dp = 280.dp,
    initialViewerHeight: Dp = 300.dp
): ResizableSidebarState {
    return remember { ResizableSidebarState(initialWidth, initialViewerHeight) }
}

/**
 * Vertical resize handle (for left edge of sidebar)
 */
@Composable
fun VerticalResizeHandle(
    onDrag: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(6.dp)
            .fillMaxHeight()
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            }
            .background(Color.Transparent)
    ) {
        // Visual indicator on hover - subtle line
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .align(Alignment.Center)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )
    }
}

/**
 * Horizontal resize handle (for bottom edge of card viewer)
 */
@Composable
fun HorizontalResizeHandle(
    onDrag: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(6.dp)
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.S_RESIZE_CURSOR)))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.y)
                }
            }
            .background(Color.Transparent)
    ) {
        // Visual indicator - subtle line
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .align(Alignment.Center)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )
    }
}

private enum class ViewerTab { IMAGE, TEXT }

/**
 * Persistent card viewer that displays the last hovered/interacted card.
 * Designed to be placed in a fixed position on screen.
 * Has tabs to switch between Image view and Text view.
 */
@Composable
fun PersistentCardViewer(
    cardInstance: CardInstance?,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ViewerTab.IMAGE) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (cardInstance == null) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hover over a card\nto preview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Tab row
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Tab(
                        selected = selectedTab == ViewerTab.IMAGE,
                        onClick = { selectedTab = ViewerTab.IMAGE },
                        text = { Text("Image", style = MaterialTheme.typography.labelSmall) }
                    )
                    Tab(
                        selected = selectedTab == ViewerTab.TEXT,
                        onClick = { selectedTab = ViewerTab.TEXT },
                        text = { Text("Text", style = MaterialTheme.typography.labelSmall) }
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

@Composable
private fun ImageTabContent(cardInstance: CardInstance) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Card image - fills available space
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
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
        )

        // Card name below image
        Text(
            text = cardInstance.card.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Status indicators row
        StatusIndicatorsRow(cardInstance)
    }
}

@Composable
private fun TextTabContent(cardInstance: CardInstance) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Card name
        Text(
            text = cardInstance.card.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Mana cost
        cardInstance.card.manaCost?.let { manaCost ->
            if (manaCost.isNotEmpty()) {
                Text(
                    text = manaCost,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Type line
        Text(
            text = cardInstance.card.type ?: "Unknown",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider()

        // Oracle text
        cardInstance.card.oracleText?.let { oracleText ->
            if (oracleText.isNotEmpty()) {
                Text(
                    text = oracleText,
                    style = MaterialTheme.typography.bodySmall,
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
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isModified) {
                            "$currentPower/$currentToughness (base: $basePower/$baseToughness)"
                        } else {
                            "$basePower/$baseToughness"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            !isModified -> Color.White
                            cardInstance.powerModifier > 0 || cardInstance.toughnessModifier > 0 -> Color.Green
                            else -> Color.Red
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Card state info (counters, status, etc.)
        if (cardInstance.zone == com.dustinmcafee.dongadeuce.models.Zone.BATTLEFIELD) {
            Divider()

            // Status indicators
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Counters:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                cardInstance.counters.forEach { (type, count) ->
                    val counterType = UIConstants.COUNTER_TYPES.find { it.id == type }
                    val displayName = counterType?.displayName ?: type
                    Text(
                        text = "• $displayName: $count",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Annotation
            if (!cardInstance.annotation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color.Yellow.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "📝 ${cardInstance.annotation}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicatorsRow(cardInstance: CardInstance) {
    if (cardInstance.zone != com.dustinmcafee.dongadeuce.models.Zone.BATTLEFIELD) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (cardInstance.isTapped) {
            MiniStatusChip("T", MaterialTheme.colorScheme.error)
        }
        if (cardInstance.counters.isNotEmpty()) {
            val totalCounters = cardInstance.counters.values.sum()
            MiniStatusChip("$totalCounters", Color.Green)
        }
        if (cardInstance.isToken) {
            MiniStatusChip("Tk", Color.Green)
        }
        if (cardInstance.isClone) {
            MiniStatusChip("C", Color.Cyan)
        }
    }
}

@Composable
private fun MiniStatusChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.3f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
