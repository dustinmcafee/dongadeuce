package com.commandermtg.ui

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
import androidx.compose.ui.unit.dp
import com.commandermtg.models.CardInstance

@Composable
fun BattlefieldCard(
    cardInstance: CardInstance,
    isLocalPlayer: Boolean,
    onCardClick: (CardInstance) -> Unit,
    onContextAction: (CardAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var lastClickTime by remember { mutableStateOf(0L) }

    val borderColor = if (isLocalPlayer) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    val rotation = if (cardInstance.isTapped) 90f else 0f

    // When tapped, card rotates so width and height swap
    // Reserve enough space to prevent overlap
    val containerSize = if (cardInstance.isTapped) {
        Modifier.size(width = 168.dp, height = 168.dp) // Reserve square space for rotated card
    } else {
        Modifier.size(width = 120.dp, height = 168.dp)
    }

    Box(
        modifier = modifier.then(containerSize),
        contentAlignment = Alignment.Center
    ) {
        CardWithContextMenu(
            cardInstance = cardInstance,
            onAction = onContextAction
        ) {
            Card(
                modifier = Modifier
                    .size(width = 120.dp, height = 168.dp)
                    .rotate(rotation)
                    .then(
                        if (isLocalPlayer) {
                            Modifier.clickable {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTime < 300) {
                                    // Double-click detected - tap/untap
                                    onCardClick(cardInstance)
                                    lastClickTime = 0L
                                } else {
                                    // Single click - do nothing or could show details
                                    lastClickTime = currentTime
                                }
                            }
                        } else {
                            Modifier
                        }
                    ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(3.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Card image as background (show card back if flipped)
                val imageUrl = if (cardInstance.isFlipped) {
                    // Magic card back image from Scryfall
                    "https://cards.scryfall.io/large/back/0/0/0aeebaf5-8c7d-4636-9e82-8c27447861f7.jpg"
                } else {
                    cardInstance.card.imageUri
                }

                CardImage(
                    imageUrl = imageUrl,
                    contentDescription = if (cardInstance.isFlipped) "Card Back" else cardInstance.card.name,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay for counters and controller info (only show when not flipped)
                if (!cardInstance.isFlipped) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Counters (top)
                        if (cardInstance.counters.isNotEmpty()) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    cardInstance.counters.forEach { (counterType, count) ->
                                        Text(
                                            text = "$count $counterType",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(1.dp))
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Power/Toughness indicator (bottom right for creatures)
                        val power = cardInstance.card.power
                        val toughness = cardInstance.card.toughness
                        if (power != null && toughness != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "$power/$toughness",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
