package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Right-side panel showing game log and chat
 */
@Composable
fun GameLogPanel(
    gameLog: List<GameEvent>,
    players: List<Player>,
    currentPlayerId: String,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }

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

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Game Log",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        "${gameLog.size} events",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }

            // Player color legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
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
                                .size(8.dp)
                                .background(color, RoundedCornerShape(2.dp))
                        )
                        Text(
                            player.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Divider()

            // Log entries
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
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(gameLog) { event ->
                        GameLogEntry(
                            event = event,
                            playerColor = playerColors[event.playerId] ?: Color.Gray,
                            isChat = event is GameEvent.ChatMessage
                        )
                    }
                }
            }

            Divider()

            // Chat input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (messageText.isNotBlank()) {
                                onSendMessage(messageText)
                                messageText = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                            messageText = ""
                        }
                    },
                    enabled = messageText.isNotBlank()
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun GameLogEntry(
    event: GameEvent,
    playerColor: Color,
    isChat: Boolean = false
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = timeFormatter.format(Date(event.timestamp))

    val backgroundColor = if (isChat) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        getEventBackgroundColor(event)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            text = timeString,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp)
        )

        // Player color indicator
        Box(
            modifier = Modifier
                .size(3.dp, 14.dp)
                .background(playerColor, RoundedCornerShape(1.dp))
        )

        // Event icon
        Text(
            text = getEventIcon(event),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(18.dp)
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
        is GameEvent.CardDrawn -> "\uD83C\uDCCF"
        is GameEvent.CardPlayed -> "\u2B07\uFE0F"
        is GameEvent.CardMoved -> "\u21C4"
        is GameEvent.LifeChanged -> if (event.change >= 0) "\u2764\uFE0F" else "\uD83D\uDC94"
        is GameEvent.CommanderDamageDealt -> "\u2694\uFE0F"
        is GameEvent.PhaseChanged -> "\u23F1\uFE0F"
        is GameEvent.TurnPassed -> "\u27A1\uFE0F"
        is GameEvent.CardCounterChanged -> "\uD83D\uDD22"
        is GameEvent.PlayerCounterChanged -> "\u2622\uFE0F"
        is GameEvent.CardTapped -> "\u21BB"
        is GameEvent.UntapAll -> "\u21BA"
        is GameEvent.TokenCreated -> "\u2728"
        is GameEvent.CardCloned -> "\uD83D\uDCC4"
        is GameEvent.PlayerLost -> "\uD83D\uDC80"
        is GameEvent.GameStarted -> "\uD83C\uDFAE"
        is GameEvent.DieRolled -> "\uD83C\uDFB2"
        is GameEvent.ControlChanged -> "\uD83E\uDD1D"
        is GameEvent.CardsMilled -> "\uD83D\uDDD1\uFE0F"
        is GameEvent.LibraryShuffled -> "\uD83C\uDCCF"
        is GameEvent.MulliganTaken -> "\uD83D\uDD04"
        is GameEvent.ChatMessage -> "\uD83D\uDCAC"
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
        is GameEvent.ChatMessage -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    return baseColor.copy(alpha = 0.3f)
}
