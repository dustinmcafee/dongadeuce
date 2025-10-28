package com.commandermtg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.commandermtg.models.CardInstance
import com.commandermtg.models.GameConstants
import com.commandermtg.models.Player

@Composable
fun CommanderDamageDialog(
    players: List<Player>,
    commanders: List<CardInstance>,
    onDismiss: () -> Unit,
    onDamageChange: (playerId: String, commanderId: String, newDamage: Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Commander Damage Tracking") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (commanders.isEmpty()) {
                    Text(
                        "No commanders on the battlefield or in command zone",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    commanders.forEach { commander ->
                        CommanderDamageSection(
                            commander = commander,
                            players = players,
                            onDamageChange = onDamageChange
                        )
                    }
                }

                // Loss condition info
                Text(
                    text = "Note: A player loses if they take 21 or more combat damage from a single commander.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CommanderDamageSection(
    commander: CardInstance,
    players: List<Player>,
    onDamageChange: (playerId: String, commanderId: String, newDamage: Int) -> Unit
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Commander header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Commander thumbnail
                CardImageThumbnail(
                    imageUrl = commander.card.imageUri,
                    contentDescription = commander.card.name
                )

                // Commander name and owner
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = commander.card.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    val owner = players.find { it.id == commander.ownerId }
                    if (owner != null) {
                        Text(
                            text = "Owned by ${owner.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Divider()

            // Damage to each player
            players.forEach { player ->
                val damage = player.commanderDamage[commander.instanceId] ?: 0
                val isLethal = damage >= GameConstants.COMMANDER_DAMAGE_THRESHOLD

                DamageCounter(
                    playerName = player.name,
                    damage = damage,
                    isLethal = isLethal,
                    onIncrement = {
                        onDamageChange(player.id, commander.instanceId, damage + 1)
                    },
                    onDecrement = {
                        if (damage > 0) {
                            onDamageChange(player.id, commander.instanceId, damage - 1)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DamageCounter(
    playerName: String,
    damage: Int,
    isLethal: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isLethal) {
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.error,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                } else {
                    Modifier.padding(8.dp)
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playerName,
                style = MaterialTheme.typography.bodyLarge
            )
            if (isLethal) {
                Text(
                    text = "LETHAL DAMAGE!",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Damage controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDecrement,
                enabled = damage > 0
            ) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }

            Text(
                text = damage.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = if (isLethal) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.widthIn(min = 40.dp)
            )

            IconButton(onClick = onIncrement) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
