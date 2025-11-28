package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog displaying the game log/history
 */
@Composable
fun GameLogDialog(
    gameLog: List<GameEvent>,
    players: List<Player>,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new events are added
    LaunchedEffect(gameLog.size) {
        if (gameLog.isNotEmpty()) {
            listState.animateScrollToItem(gameLog.size - 1)
        }
    }

    // Create a color map for players
    val playerColors = remember(players) {
        val colors = listOf(
            Color(0xFF2196F3), // Blue
            Color(0xFFE91E63), // Pink
            Color(0xFF4CAF50), // Green
            Color(0xFFFF9800), // Orange
            Color(0xFF9C27B0), // Purple
            Color(0xFF00BCD4), // Cyan
        )
        players.mapIndexed { index, player ->
            player.id to colors[index % colors.size]
        }.toMap()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Game Log")
                Text(
                    "${gameLog.size} events",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 400.dp, max = 600.dp)
            ) {
                // Player color legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    players.forEach { player ->
                        val color = playerColors[player.id] ?: Color.Gray
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(color, RoundedCornerShape(2.dp))
                            )
                            Text(
                                player.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Divider()

                if (gameLog.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No events yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(gameLog) { event ->
                            GameLogEntry(
                                event = event,
                                playerColor = playerColors[event.playerId] ?: Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun GameLogEntry(
    event: GameEvent,
    playerColor: Color
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeString = timeFormatter.format(Date(event.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                getEventBackgroundColor(event),
                RoundedCornerShape(4.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            text = timeString,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )

        // Player color indicator
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .background(playerColor, RoundedCornerShape(2.dp))
        )

        // Event icon
        Text(
            text = getEventIcon(event),
            modifier = Modifier.width(24.dp)
        )

        // Event description
        Text(
            text = event.toDisplayString(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Get an icon/emoji for the event type
 */
private fun getEventIcon(event: GameEvent): String {
    return when (event) {
        is GameEvent.CardDrawn -> "\uD83C\uDCCF" // Playing card
        is GameEvent.CardPlayed -> "\u2B07\uFE0F" // Down arrow
        is GameEvent.CardMoved -> "\u21C4" // Left-right arrows
        is GameEvent.LifeChanged -> if (event.change >= 0) "\u2764\uFE0F" else "\uD83D\uDC94" // Heart or broken heart
        is GameEvent.CommanderDamageDealt -> "\u2694\uFE0F" // Crossed swords
        is GameEvent.PhaseChanged -> "\u23F1\uFE0F" // Stopwatch
        is GameEvent.TurnPassed -> "\u27A1\uFE0F" // Right arrow
        is GameEvent.CardCounterChanged -> "\uD83D\uDD22" // Counter
        is GameEvent.PlayerCounterChanged -> "\u2622\uFE0F" // Radioactive (for poison etc)
        is GameEvent.CardTapped -> "\u21BB" // Rotate
        is GameEvent.UntapAll -> "\u21BA" // Counter-rotate
        is GameEvent.TokenCreated -> "\u2728" // Sparkles
        is GameEvent.CardCloned -> "\uD83D\uDCC4" // Document
        is GameEvent.PlayerLost -> "\uD83D\uDC80" // Skull
        is GameEvent.GameStarted -> "\uD83C\uDFAE" // Game controller
        is GameEvent.DieRolled -> "\uD83C\uDFB2" // Die
        is GameEvent.ControlChanged -> "\uD83E\uDD1D" // Handshake
        is GameEvent.CardsMilled -> "\uD83D\uDDD1\uFE0F" // Trash
        is GameEvent.LibraryShuffled -> "\uD83C\uDCCF" // Cards
        is GameEvent.MulliganTaken -> "\uD83D\uDD04" // Refresh
        is GameEvent.ChatMessage -> "\uD83D\uDCAC" // Speech bubble
        is GameEvent.GenericAction -> "\u2139\uFE0F" // Info
    }
}

/**
 * Get a subtle background color based on event type
 */
@Composable
private fun getEventBackgroundColor(event: GameEvent): Color {
    val baseColor = when (event) {
        is GameEvent.PlayerLost -> MaterialTheme.colorScheme.errorContainer
        is GameEvent.GameStarted -> MaterialTheme.colorScheme.primaryContainer
        is GameEvent.TurnPassed -> MaterialTheme.colorScheme.secondaryContainer
        is GameEvent.CommanderDamageDealt -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        is GameEvent.LifeChanged -> if (event.change < 0) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        }
        else -> Color.Transparent
    }
    return baseColor.copy(alpha = 0.3f)
}
