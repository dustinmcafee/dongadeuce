@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.CardInstance
import com.dustinmcafee.dongadeuce.ui.UIConstants.BATTLEFIELD_CARD_HEIGHT
import com.dustinmcafee.dongadeuce.ui.UIConstants.BATTLEFIELD_CARD_TAPPED_SIZE
import com.dustinmcafee.dongadeuce.ui.UIConstants.BATTLEFIELD_CARD_WIDTH
import com.dustinmcafee.dongadeuce.ui.UIConstants.CARD_CORNER_RADIUS
import com.dustinmcafee.dongadeuce.ui.UIConstants.CARD_ELEVATION
import com.dustinmcafee.dongadeuce.ui.UIConstants.DOUBLE_CLICK_DELAY_MS
import com.dustinmcafee.dongadeuce.ui.UIConstants.SELECTED_BORDER_WIDTH
import com.dustinmcafee.dongadeuce.ui.UIConstants.SELECTION_BORDER_WIDTH

@Composable
fun BattlefieldCard(
    cardInstance: CardInstance,
    isLocalPlayer: Boolean,
    onCardClick: (CardInstance) -> Unit,
    onContextAction: (CardAction) -> Unit = {},
    selectionState: SelectionState? = null,
    modifier: Modifier = Modifier,
    otherPlayers: List<com.dustinmcafee.dongadeuce.models.Player> = emptyList(),
    ownerName: String = ""
) {
    var lastClickTime by remember { mutableStateOf(0L) }
    val isSelected = selectionState?.isSelected(cardInstance.instanceId) == true

    val borderColor = when {
        isSelected -> Color(0xFF00FF00) // Green border for selected
        isLocalPlayer -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }

    val borderWidth = if (isSelected) SELECTED_BORDER_WIDTH else SELECTION_BORDER_WIDTH

    val rotation = if (cardInstance.isTapped) 90f else 0f

    // When tapped, card rotates so width and height swap
    // Reserve enough space to prevent overlap
    val containerSize = if (cardInstance.isTapped) {
        Modifier.size(width = BATTLEFIELD_CARD_TAPPED_SIZE, height = BATTLEFIELD_CARD_TAPPED_SIZE) // Reserve square space for rotated card
    } else {
        Modifier.size(width = BATTLEFIELD_CARD_WIDTH, height = BATTLEFIELD_CARD_HEIGHT)
    }

    // Check if owner != controller
    val showOwnerTag = cardInstance.ownerId != cardInstance.controllerId && ownerName.isNotEmpty()

    Box(
        modifier = modifier.then(containerSize),
        contentAlignment = Alignment.Center
    ) {
        CardWithContextMenu(
            cardInstance = cardInstance,
            onAction = onContextAction,
            otherPlayers = otherPlayers
        ) {
            Card(
                modifier = Modifier
                    .size(width = BATTLEFIELD_CARD_WIDTH, height = BATTLEFIELD_CARD_HEIGHT)
                    .rotate(rotation)
                    .then(
                        if (isLocalPlayer) {
                            Modifier.pointerInput(cardInstance.instanceId) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()

                                        if (event.type == PointerEventType.Press) {
                                            // Skip right-clicks (they trigger context menu)
                                            // Check if this is NOT a primary (left) button press
                                            val isPrimaryClick = event.button == PointerButton.Primary
                                            val isRightClick = !isPrimaryClick && event.button != null

                                            if (isRightClick) {
                                                // Right-click detected - consume the release event and skip selection logic
                                                awaitPointerEvent()
                                                continue
                                            }

                                            val isShiftPressed = event.keyboardModifiers.isShiftPressed

                                            // Wait for release
                                            val releaseEvent = awaitPointerEvent()

                                            if (releaseEvent.type == PointerEventType.Release) {
                                                // Check for double-click
                                                val clickTime = System.currentTimeMillis()
                                                val timeSinceLastClick = clickTime - lastClickTime
                                                lastClickTime = clickTime

                                                if (timeSinceLastClick < DOUBLE_CLICK_DELAY_MS) {
                                                    // Double-click - tap/untap
                                                    onCardClick(cardInstance)
                                                } else if (isShiftPressed && selectionState != null) {
                                                    // Shift+click - toggle selection
                                                    selectionState.toggleSelection(cardInstance.instanceId)
                                                } else if (selectionState != null) {
                                                    // Regular click - always clear and select only this card
                                                    selectionState.clearSelection()
                                                    selectionState.select(cardInstance.instanceId)
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
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(CARD_CORNER_RADIUS),
            border = BorderStroke(borderWidth, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Card image as background (show card back if flipped)
                val imageUrl = if (cardInstance.isFlipped) {
                    // Standard Magic card back image from Scryfall
                    "https://cards.scryfall.io/back.png"
                } else {
                    cardInstance.card.imageUri
                }

                CardImage(
                    imageUrl = imageUrl,
                    contentDescription = if (cardInstance.isFlipped) "Card Back" else cardInstance.card.name,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay for counters and controller info (always show)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Owner tag and counters (top) - always visible
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Owner tag (only show if controller != owner)
                        if (showOwnerTag) {
                            Surface(
                                color = Color.Blue.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Owner: $ownerName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Counters - always show even when flipped
                        if (cardInstance.counters.isNotEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                cardInstance.counters.forEach { (counterTypeId, count) ->
                                    // Find matching counter type for color
                                    val counterType = UIConstants.COUNTER_TYPES.find { it.id == counterTypeId }
                                    val counterColor = counterType?.color ?: Color.White
                                    val displayName = counterType?.displayName ?: counterTypeId

                                    Surface(
                                        color = counterColor.copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "$count $displayName",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // "Doesn't Untap" indicator
                        if (cardInstance.doesntUntap) {
                            Surface(
                                color = Color.Magenta.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "⊘ Doesn't Untap",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Annotation indicator
                        if (!cardInstance.annotation.isNullOrBlank()) {
                            Surface(
                                color = Color.Yellow.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "📝 ${cardInstance.annotation}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Power/Toughness indicator (bottom right for creatures) - only show when not flipped
                    if (!cardInstance.isFlipped) {
                        val basePower = cardInstance.card.power
                        val baseToughness = cardInstance.card.toughness
                        if (basePower != null && baseToughness != null) {
                            val currentPower = (basePower.toIntOrNull() ?: 0) + cardInstance.powerModifier
                            val currentToughness = (baseToughness.toIntOrNull() ?: 0) + cardInstance.toughnessModifier
                            val isModified = cardInstance.powerModifier != 0 || cardInstance.toughnessModifier != 0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "$currentPower/$currentToughness",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = when {
                                                !isModified -> Color.White
                                                cardInstance.powerModifier > 0 || cardInstance.toughnessModifier > 0 -> Color.Green
                                                else -> Color.Red
                                            }
                                        )
                                        if (isModified) {
                                            Text(
                                                text = "($basePower/$baseToughness)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
