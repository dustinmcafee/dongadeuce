package com.commandermtg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.commandermtg.models.Player
import com.commandermtg.models.Zone
import com.commandermtg.viewmodel.GameViewModel

@Composable
fun GameScreen(
    loadedDeck: com.commandermtg.models.Deck? = null,
    viewModel: GameViewModel = remember { GameViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()

    // Initialize game and load deck when entering the screen
    LaunchedEffect(Unit) {
        // Initialize game if not already done
        if (uiState.localPlayer == null) {
            viewModel.initializeGame(
                localPlayerName = "You",
                opponentNames = listOf("Opponent")
            )
        }
    }

    // Load deck when it becomes available and game is initialized
    LaunchedEffect(loadedDeck, uiState.gameState, uiState.localPlayer) {
        val localPlayer = uiState.localPlayer
        if (loadedDeck != null && uiState.gameState != null && localPlayer != null) {
            val currentHandCount = viewModel.getCardCount(localPlayer.id, Zone.HAND)

            // Only load deck if we haven't already loaded it (hand is empty and library is empty)
            val libraryCount = viewModel.getCardCount(localPlayer.id, Zone.LIBRARY)
            if (currentHandCount == 0 && libraryCount == 0) {
                viewModel.loadDeck(loadedDeck)
                // Draw starting hand for local player (opponents will load their own decks in multiplayer)
                viewModel.drawStartingHand(localPlayer.id, 7)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Get opponent (first opponent in list)
        val opponent = uiState.opponents.firstOrNull()
        val localPlayer = uiState.localPlayer

        // Opponent's area (top)
        if (opponent != null) {
            OpponentArea(
                player = opponent,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Shared battlefield (middle)
        BattlefieldArea(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Your area (bottom)
        if (localPlayer != null) {
            PlayerArea(
                player = localPlayer,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
            )
        }
    }
}

@Composable
fun OpponentArea(
    player: Player,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val libraryCount = viewModel.getCardCount(player.id, Zone.LIBRARY)
    val handCount = viewModel.getCardCount(player.id, Zone.HAND)
    val graveyardCount = viewModel.getCardCount(player.id, Zone.GRAVEYARD)
    val exileCount = viewModel.getCardCount(player.id, Zone.EXILE)
    val commanderCount = viewModel.getCardCount(player.id, Zone.COMMAND_ZONE)

    Row(modifier = modifier) {
        // Opponent's library, graveyard, exile
        Column(
            modifier = Modifier.width(200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZoneCard("Library", Zone.LIBRARY, libraryCount, Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZoneCard("GY", Zone.GRAVEYARD, graveyardCount, Modifier.weight(1f))
                ZoneCard("Exile", Zone.EXILE, exileCount, Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Opponent's hand and info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZoneCard("Hand", Zone.HAND, handCount, Modifier.height(100.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(player.name, style = MaterialTheme.typography.titleMedium)
                    Text("Life: ${player.life}", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Commander zone
        ZoneCard("Commander", Zone.COMMAND_ZONE, commanderCount, Modifier.width(120.dp))
    }
}

@Composable
fun BattlefieldArea(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Battlefield",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun PlayerArea(
    player: Player,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    var showHandDialog by remember { mutableStateOf(false) }

    val libraryCount = viewModel.getCardCount(player.id, Zone.LIBRARY)
    val handCount = viewModel.getCardCount(player.id, Zone.HAND)
    val graveyardCount = viewModel.getCardCount(player.id, Zone.GRAVEYARD)
    val exileCount = viewModel.getCardCount(player.id, Zone.EXILE)
    val commanderCount = viewModel.getCardCount(player.id, Zone.COMMAND_ZONE)

    // Show hand dialog if requested
    if (showHandDialog) {
        HandDialog(
            cards = viewModel.getCards(player.id, Zone.HAND),
            onDismiss = { showHandDialog = false },
            onPlayCard = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
                showHandDialog = false
            }
        )
    }

    Row(modifier = modifier) {
        // Commander zone
        ZoneCard("Commander", Zone.COMMAND_ZONE, commanderCount, Modifier.width(120.dp))

        Spacer(modifier = Modifier.width(16.dp))

        // Your hand and info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(player.name, style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.updateLife(player.id, player.life - 1) }) {
                                Text("-", style = MaterialTheme.typography.headlineSmall)
                            }
                            Text("Life: ${player.life}", style = MaterialTheme.typography.headlineMedium)
                            IconButton(onClick = { viewModel.updateLife(player.id, player.life + 1) }) {
                                Text("+", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.drawCard(player.id) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Draw")
                }

                OutlinedButton(
                    onClick = { showHandDialog = true },
                    modifier = Modifier.weight(2f)
                ) {
                    Text("Hand ($handCount)")
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Your library, graveyard, exile
        Column(
            modifier = Modifier.width(200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZoneCard("Library", Zone.LIBRARY, libraryCount, Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZoneCard("Graveyard", Zone.GRAVEYARD, graveyardCount, Modifier.weight(1f))
                ZoneCard("Exile", Zone.EXILE, exileCount, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ZoneCard(
    label: String,
    zone: Zone,
    cardCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                if (cardCount > 0) {
                    Text("($cardCount)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun HandDialog(
    cards: List<com.commandermtg.models.CardInstance>,
    onDismiss: () -> Unit,
    onPlayCard: (com.commandermtg.models.CardInstance) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your Hand (${cards.size} cards)") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cards.isEmpty()) {
                    Text("No cards in hand", style = MaterialTheme.typography.bodyMedium)
                } else {
                    cards.forEach { cardInstance ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

                                Button(
                                    onClick = { onPlayCard(cardInstance) },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text("Play")
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
