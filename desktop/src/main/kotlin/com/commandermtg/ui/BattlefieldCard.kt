package com.commandermtg.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.commandermtg.models.CardInstance
import com.commandermtg.models.Player

@Composable
fun BattlefieldCard(
    cardInstance: CardInstance,
    controller: Player,
    isLocalPlayer: Boolean,
    onCardClick: (CardInstance) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isLocalPlayer) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    val borderColor = if (isLocalPlayer) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    val rotation = if (cardInstance.isTapped) 90f else 0f

    Card(
        modifier = modifier
            .size(width = 120.dp, height = 160.dp)
            .rotate(rotation)
            .clickable { onCardClick(cardInstance) },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Card name (top)
            Text(
                text = cardInstance.card.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2
            )

            Spacer(modifier = Modifier.weight(1f))

            // Counters (middle)
            if (cardInstance.counters.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    cardInstance.counters.forEach { (counterType, count) ->
                        Text(
                            text = "$count $counterType",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Card info (bottom)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Mana cost
                val manaCost = cardInstance.card.manaCost
                if (manaCost != null && manaCost.isNotEmpty()) {
                    Text(
                        text = manaCost,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                // Power/Toughness for creatures
                val power = cardInstance.card.power
                val toughness = cardInstance.card.toughness
                if (power != null && toughness != null) {
                    Text(
                        text = "$power/$toughness",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Controller indicator
            Text(
                text = controller.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}
