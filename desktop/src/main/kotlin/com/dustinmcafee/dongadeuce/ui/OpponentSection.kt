package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.viewmodel.GameViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/**
 * Container for multiple opponent areas with dynamic layout based on opponent count
 */
@Composable
fun OpponentsArea(
    opponents: List<Player>,
    viewModel: GameViewModel,
    gameState: GameState?,
    onCardAction: (CardAction) -> Unit,
    modifier: Modifier = Modifier,
    selectionState: SelectionState? = null,
    allPlayers: List<Player> = emptyList()
) {
    when (opponents.size) {
        1 -> {
            // Single opponent - full width
            OpponentArea(
                player = opponents[0],
                viewModel = viewModel,
                gameState = gameState,
                onCardAction = onCardAction,
                modifier = modifier,
                selectionState = selectionState,
                allPlayers = allPlayers
            )
        }
        2 -> {
            // Two opponents - side by side
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OpponentArea(
                    player = opponents[0],
                    viewModel = viewModel,
                    gameState = gameState,
                    onCardAction = onCardAction,
                    modifier = Modifier.weight(1f),
                    selectionState = selectionState,
                    allPlayers = allPlayers
                )
                OpponentArea(
                    player = opponents[1],
                    viewModel = viewModel,
                    gameState = gameState,
                    onCardAction = onCardAction,
                    modifier = Modifier.weight(1f),
                    selectionState = selectionState,
                    allPlayers = allPlayers
                )
            }
        }
        3 -> {
            // Three opponents - all in a row
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                opponents.forEach { opponent ->
                    OpponentArea(
                        player = opponent,
                        viewModel = viewModel,
                        gameState = gameState,
                        onCardAction = onCardAction,
                        modifier = Modifier.weight(1f),
                        selectionState = selectionState,
                        allPlayers = allPlayers
                    )
                }
            }
        }
        else -> {
            // Fallback for > 3 opponents (not typical in Commander)
            // Display first 3 opponents
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                opponents.take(3).forEach { opponent ->
                    OpponentArea(
                        player = opponent,
                        viewModel = viewModel,
                        gameState = gameState,
                        onCardAction = onCardAction,
                        modifier = Modifier.weight(1f),
                        selectionState = selectionState,
                        allPlayers = allPlayers
                    )
                }
            }
        }
    }
}

/**
 * Display area for a single opponent showing their zones and battlefield
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpponentArea(
    player: Player,
    viewModel: GameViewModel,
    gameState: GameState?,
    onCardAction: (CardAction) -> Unit,
    modifier: Modifier = Modifier,
    selectionState: SelectionState? = null,
    allPlayers: List<Player> = emptyList()
) {
    var showGraveyardDialog by remember { mutableStateOf(false) }
    var showExileDialog by remember { mutableStateOf(false) }

    val libraryCount = viewModel.getCardCount(player.id, Zone.LIBRARY)
    val handCount = viewModel.getCardCount(player.id, Zone.HAND)
    val graveyardCount = viewModel.getCardCount(player.id, Zone.GRAVEYARD)
    val exileCount = viewModel.getCardCount(player.id, Zone.EXILE)
    val commanderCount = viewModel.getCardCount(player.id, Zone.COMMAND_ZONE)
    val battlefieldCards = viewModel.getCards(player.id, Zone.BATTLEFIELD)

    // Compute grid positions for this opponent's battlefield (single source of truth)
    val gridPositions = remember(gameState, player.id) {
        gameState?.computeBattlefieldPositions(player.id) ?: emptyMap()
    }

    // Show graveyard dialog if requested
    if (showGraveyardDialog) {
        GraveyardDialog(
            cards = viewModel.getCards(player.id, Zone.GRAVEYARD),
            playerName = player.name,
            onDismiss = { showGraveyardDialog = false },
            onReturnToHand = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.HAND)
            },
            onReturnToBattlefield = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
            },
            onAction = onCardAction,
            allPlayers = allPlayers
        )
    }

    // Show exile dialog if requested
    if (showExileDialog) {
        ExileDialog(
            cards = viewModel.getCards(player.id, Zone.EXILE),
            playerName = player.name,
            onDismiss = { showExileDialog = false },
            onReturnToHand = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.HAND)
            },
            onReturnToBattlefield = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
            },
            onAction = onCardAction,
            allPlayers = allPlayers
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Opponent zones row
        Row {
            // Opponent's library, graveyard, exile
            Column(
                modifier = Modifier.width(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZoneCard("Library", Zone.LIBRARY, libraryCount, Modifier.height(100.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneCard(
                        "Graveyard",
                        Zone.GRAVEYARD,
                        graveyardCount,
                        Modifier.weight(1f),
                        onClick = { showGraveyardDialog = true }
                    )
                    ZoneCard(
                        "Exile",
                        Zone.EXILE,
                        exileCount,
                        Modifier.weight(1f),
                        onClick = { showExileDialog = true }
                    )
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
                    colors = CardDefaults.cardColors(
                        containerColor = if (player.hasLost) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(player.name, style = MaterialTheme.typography.titleMedium)
                        Text("Life: ${player.life}", style = MaterialTheme.typography.headlineMedium)
                        if (player.hasLost) {
                            Text(
                                "DEFEATED",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Commander zone
            ZoneCard("Commander", Zone.COMMAND_ZONE, commanderCount, Modifier.width(120.dp))
        }

        // Opponent's battlefield
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
        ) {
            DraggableBattlefieldGrid(
                cards = battlefieldCards,
                gridPositions = gridPositions,
                isLocalPlayer = false,
                onCardClick = { viewModel.toggleTap(it.instanceId) },
                onContextAction = onCardAction,
                onCardPositionChanged = { cardId, gridX, gridY ->
                    viewModel.updateCardGridPosition(cardId, gridX, gridY)
                },
                modifier = Modifier.fillMaxSize().padding(8.dp),
                selectionState = selectionState,
                currentPlayerId = null, // Opponent cards cannot be dragged
                allPlayers = allPlayers
            )
        }
    }
}
