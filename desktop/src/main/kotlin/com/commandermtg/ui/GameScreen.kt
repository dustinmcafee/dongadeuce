package com.commandermtg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.commandermtg.models.Player
import com.commandermtg.models.Zone
import com.commandermtg.models.CardInstance
import com.commandermtg.viewmodel.GameViewModel
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@Composable
fun GameScreen(
    loadedDeck: com.commandermtg.models.Deck? = null,
    hotseatDecks: Map<Int, com.commandermtg.models.Deck> = emptyMap(),
    playerCount: Int = 2, // Total players (including local player): 2, 3, or 4
    isHotseatMode: Boolean = false,
    viewModel: GameViewModel = remember { GameViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    var cardDetailsToShow by remember { mutableStateOf<com.commandermtg.models.CardInstance?>(null) }

    // Handler for card actions that shows details dialog when needed
    // and enforces ownership restrictions
    val handleAction: (CardAction) -> Unit = { action ->
        when (action) {
            is CardAction.ViewDetails -> {
                // Anyone can view card details
                cardDetailsToShow = action.cardInstance
            }
            else -> {
                val cardInstance = when (action) {
                    is CardAction.Tap -> action.cardInstance
                    is CardAction.Untap -> action.cardInstance
                    is CardAction.FlipCard -> action.cardInstance
                    is CardAction.ToHand -> action.cardInstance
                    is CardAction.ToBattlefield -> action.cardInstance
                    is CardAction.ToGraveyard -> action.cardInstance
                    is CardAction.ToExile -> action.cardInstance
                    is CardAction.ToLibrary -> action.cardInstance
                    is CardAction.ToTop -> action.cardInstance
                    is CardAction.ToCommandZone -> action.cardInstance
                    is CardAction.AddCounter -> action.cardInstance
                    is CardAction.RemoveCounter -> action.cardInstance
                    else -> null
                }

                if (isHotseatMode) {
                    // In hotseat mode, current active player can interact with their cards
                    val activePlayer = uiState.gameState?.activePlayer
                    if (activePlayer != null && cardInstance != null) {
                        if (cardInstance.ownerId == activePlayer.id) {
                            handleCardAction(action, viewModel)
                        } else {
                            println("Cannot interact with other player's card: ${cardInstance.card.name}")
                        }
                    }
                } else {
                    // In network mode, you can only interact with your cards
                    val localPlayer = uiState.localPlayer
                    if (localPlayer != null && cardInstance != null) {
                        if (cardInstance.ownerId == localPlayer.id) {
                            handleCardAction(action, viewModel)
                        } else {
                            println("Cannot interact with opponent's card: ${cardInstance.card.name}")
                        }
                    }
                }
            }
        }
    }

    // Initialize game and load deck when entering the screen
    LaunchedEffect(Unit) {
        // Initialize game if not already done
        if (uiState.localPlayer == null) {
            // Generate player names based on mode
            val playerNames = if (isHotseatMode) {
                List(playerCount) { index -> "Player ${index + 1}" }
            } else {
                val opponentCount = (playerCount - 1).coerceIn(1, 3)
                val opponentNames = List(opponentCount) { index ->
                    "Opponent ${index + 1}"
                }
                listOf("You") + opponentNames
            }

            viewModel.initializeGame(
                localPlayerName = playerNames[0],
                opponentNames = playerNames.drop(1),
                isHotseatMode = isHotseatMode
            )
        }
    }

    // Load decks for hotseat mode
    LaunchedEffect(hotseatDecks, uiState.gameState, uiState.allPlayers) {
        if (isHotseatMode && hotseatDecks.isNotEmpty() && uiState.gameState != null) {
            val allPlayers = uiState.allPlayers

            // Load deck for each player
            hotseatDecks.forEach { (playerIndex, deck) ->
                if (playerIndex < allPlayers.size) {
                    val player = allPlayers[playerIndex]
                    val libraryCount = viewModel.getCardCount(player.id, Zone.LIBRARY)
                    val handCount = viewModel.getCardCount(player.id, Zone.HAND)

                    // Only load if not already loaded
                    if (libraryCount == 0 && handCount == 0) {
                        viewModel.loadDeckForPlayer(player.id, deck)
                        viewModel.drawStartingHand(player.id, 7)
                    }
                }
            }
        }
    }

    // Load deck for network mode (single deck for local player)
    LaunchedEffect(loadedDeck, uiState.gameState, uiState.localPlayer) {
        if (!isHotseatMode) {
            val localPlayer = uiState.localPlayer
            if (loadedDeck != null && uiState.gameState != null && localPlayer != null) {
                val currentHandCount = viewModel.getCardCount(localPlayer.id, Zone.HAND)
                val libraryCount = viewModel.getCardCount(localPlayer.id, Zone.LIBRARY)

                // Only load deck if we haven't already loaded it
                if (currentHandCount == 0 && libraryCount == 0) {
                    viewModel.loadDeck(loadedDeck)
                    viewModel.drawStartingHand(localPlayer.id, 7)
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main game area (left side)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (isHotseatMode) {
                // Hotseat mode: Compact layout with battlefields touching
                // Active player always at bottom-left
                val gameState = uiState.gameState
                // IMPORTANT: Use gameState.players, NOT uiState.allPlayers
                // because allPlayers is derived from localPlayer+opponents which is already rotated
                val allPlayers = gameState?.players ?: emptyList()
                val activePlayerIndex = gameState?.activePlayerIndex ?: 0
                val activePlayerId = gameState?.activePlayer?.id

                println("[GameScreen] Hotseat mode - activePlayerIndex: $activePlayerIndex, activePlayerId: $activePlayerId, allPlayers: ${allPlayers.map { it.name }}")

                // Rotate players so active player is first (will be at bottom)
                val rotatedPlayers = allPlayers.drop(activePlayerIndex) + allPlayers.take(activePlayerIndex)
                println("[GameScreen] Rotated players: ${rotatedPlayers.map { it.name }}")

                // Use key to force recomposition when active player changes
                key(activePlayerId) {
                    Column(modifier = Modifier.fillMaxSize()) {
                    when (rotatedPlayers.size) {
                        2 -> {
                            // 2 players: vertical split, opponent on top, active player on bottom
                            HotseatPlayerSection(
                                player = rotatedPlayers[1],
                                viewModel = viewModel,
                                isActivePlayer = false,
                                onCardAction = handleAction,
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                            HotseatPlayerSection(
                                player = rotatedPlayers[0],
                                viewModel = viewModel,
                                isActivePlayer = true,
                                onCardAction = handleAction,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                inverted = true
                            )
                        }
                        3 -> {
                            // 3 players: opponents on top, active player bottom-left
                            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                HotseatPlayerSection(
                                    player = rotatedPlayers[1],
                                    viewModel = viewModel,
                                    isActivePlayer = false,
                                    onCardAction = handleAction,
                                    modifier = Modifier.weight(1f)
                                )
                                HotseatPlayerSection(
                                    player = rotatedPlayers[2],
                                    viewModel = viewModel,
                                    isActivePlayer = false,
                                    onCardAction = handleAction,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                HotseatPlayerSection(
                                    player = rotatedPlayers[0],
                                    viewModel = viewModel,
                                    isActivePlayer = true,
                                    onCardAction = handleAction,
                                    modifier = Modifier.weight(1f),
                                    inverted = true
                                )
                                // Empty space to keep active player on left
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        4 -> {
                            // 4 players: 2 opponents on top, active player bottom-left, 1 opponent bottom-right
                            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                HotseatPlayerSection(
                                    player = rotatedPlayers[2],
                                    viewModel = viewModel,
                                    isActivePlayer = false,
                                    onCardAction = handleAction,
                                    modifier = Modifier.weight(1f)
                                )
                                HotseatPlayerSection(
                                    player = rotatedPlayers[3],
                                    viewModel = viewModel,
                                    isActivePlayer = false,
                                    onCardAction = handleAction,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                HotseatPlayerSection(
                                    player = rotatedPlayers[0],
                                    viewModel = viewModel,
                                    isActivePlayer = true,
                                    onCardAction = handleAction,
                                    modifier = Modifier.weight(1f),
                                    inverted = true
                                )
                                HotseatPlayerSection(
                                    player = rotatedPlayers[1],
                                    viewModel = viewModel,
                                    isActivePlayer = false,
                                    onCardAction = handleAction,
                                    modifier = Modifier.weight(1f),
                                    inverted = true
                                )
                            }
                        }
                    }
                    }
                }
            } else {
                // Network mode: Traditional local player vs opponents layout
                val opponents = uiState.opponents
                val localPlayer = uiState.localPlayer

                // Opponents' area (top) - dynamic layout based on player count
                if (opponents.isNotEmpty()) {
                    OpponentsArea(
                        opponents = opponents,
                        viewModel = viewModel,
                        onCardAction = handleAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.4f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Your area (bottom)
                if (localPlayer != null) {
                    PlayerArea(
                        player = localPlayer,
                        viewModel = viewModel,
                        allPlayers = uiState.allPlayers,
                        onCardAction = handleAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f)
                    )
                }
            }
        }

        // Turn indicator (right sidebar)
        val gameState = uiState.gameState
        if (gameState != null) {
            TurnIndicator(
                activePlayer = gameState.activePlayer,
                currentPhase = gameState.phase,
                turnNumber = gameState.turnNumber,
                onNextPhase = { viewModel.nextPhase() },
                onPassTurn = { viewModel.passTurn() },
                onUntapAll = { viewModel.untapAll(gameState.activePlayer.id) },
                modifier = Modifier.width(250.dp)
            )
        }
    }

    // Card details dialog
    cardDetailsToShow?.let { cardInstance ->
        CardDetailsDialog(
            cardInstance = cardInstance,
            onDismiss = { cardDetailsToShow = null }
        )
    }
}

/**
 * Compact player section for hotseat mode
 * Shows: hand strip (top/bottom), battlefield (center), player info (side)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HotseatPlayerSection(
    player: Player,
    viewModel: GameViewModel,
    isActivePlayer: Boolean,
    onCardAction: (CardAction) -> Unit,
    modifier: Modifier = Modifier,
    inverted: Boolean = false // If true, hand at bottom; if false, hand at top
) {
    val handCards = if (isActivePlayer) viewModel.getCards(player.id, Zone.HAND) else emptyList()
    val handCount = viewModel.getCardCount(player.id, Zone.HAND)
    val battlefieldCards = viewModel.getCards(player.id, Zone.BATTLEFIELD)
    val libraryCount = viewModel.getCardCount(player.id, Zone.LIBRARY)
    val graveyardCount = viewModel.getCardCount(player.id, Zone.GRAVEYARD)

    Column(
        modifier = modifier.background(
            if (isActivePlayer) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            else Color.Transparent
        ),
        verticalArrangement = if (inverted) Arrangement.Bottom else Arrangement.Top
    ) {
        if (!inverted) {
            // Hand at top (normal orientation)
            CompactHandStrip(
                player = player,
                handCards = handCards,
                handCount = handCount,
                showCards = isActivePlayer,
                onCardAction = onCardAction,
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )
        }

        // Battlefield in center
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Player info sidebar
                Column(
                    modifier = Modifier.width(120.dp).fillMaxHeight().padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        player.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        "Life: ${player.life}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Lib: $libraryCount", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Text("GY: $graveyardCount", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }

                // Battlefield cards
                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp)) {
                    DraggableBattlefieldGrid(
                        cards = battlefieldCards,
                        isLocalPlayer = isActivePlayer,
                        onCardClick = { viewModel.toggleTap(it.instanceId) },
                        onContextAction = onCardAction,
                        onCardPositionChanged = { cardId, gridX, gridY ->
                            viewModel.updateCardGridPosition(cardId, gridX, gridY)
                        },
                        modifier = Modifier.fillMaxSize(),
                        currentPlayerId = if (isActivePlayer) player.id else null
                    )
                }
            }
        }

        if (inverted) {
            // Hand at bottom (inverted orientation)
            CompactHandStrip(
                player = player,
                handCards = handCards,
                handCount = handCount,
                showCards = isActivePlayer,
                onCardAction = onCardAction,
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )
        }
    }
}

/**
 * Compact hand display strip
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactHandStrip(
    player: Player,
    handCards: List<CardInstance>,
    handCount: Int,
    showCards: Boolean,
    onCardAction: (CardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${player.name}'s Hand ($handCount)",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.width(120.dp)
            )

            if (!showCards) {
                // For non-active players, just show card count, not actual cards
                Text(
                    "Hidden",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            } else if (handCards.isEmpty()) {
                Text(
                    "No cards",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            } else {
                FlowRow(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    handCards.forEach { cardInstance ->
                        HandCardDisplay(
                            cardInstance = cardInstance,
                            onCardClick = { /* Single click - could open dialog */ },
                            onDoubleClick = {
                                // Double-click plays card to battlefield
                                onCardAction(CardAction.ToBattlefield(cardInstance))
                            },
                            onContextAction = onCardAction
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dynamic opponents layout that arranges 1-3 opponents based on player count
 */
@Composable
fun OpponentsArea(
    opponents: List<Player>,
    viewModel: GameViewModel,
    onCardAction: (CardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    when (opponents.size) {
        1 -> {
            // Single opponent - full width
            OpponentArea(
                player = opponents[0],
                viewModel = viewModel,
                onCardAction = onCardAction,
                modifier = modifier
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
                    onCardAction = onCardAction,
                    modifier = Modifier.weight(1f)
                )
                OpponentArea(
                    player = opponents[1],
                    viewModel = viewModel,
                    onCardAction = onCardAction,
                    modifier = Modifier.weight(1f)
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
                        onCardAction = onCardAction,
                        modifier = Modifier.weight(1f)
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
                        onCardAction = onCardAction,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpponentArea(
    player: Player,
    viewModel: GameViewModel,
    onCardAction: (CardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showGraveyardDialog by remember { mutableStateOf(false) }
    var showExileDialog by remember { mutableStateOf(false) }

    val libraryCount = viewModel.getCardCount(player.id, Zone.LIBRARY)
    val handCount = viewModel.getCardCount(player.id, Zone.HAND)
    val graveyardCount = viewModel.getCardCount(player.id, Zone.GRAVEYARD)
    val exileCount = viewModel.getCardCount(player.id, Zone.EXILE)
    val commanderCount = viewModel.getCardCount(player.id, Zone.COMMAND_ZONE)
    val battlefieldCards = viewModel.getCards(player.id, Zone.BATTLEFIELD)

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
            }
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
            }
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
                isLocalPlayer = false,
                onCardClick = { viewModel.toggleTap(it.instanceId) },
                onContextAction = onCardAction,
                onCardPositionChanged = { cardId, gridX, gridY ->
                    viewModel.updateCardGridPosition(cardId, gridX, gridY)
                },
                modifier = Modifier.fillMaxSize().padding(8.dp),
                currentPlayerId = null // Opponent cards cannot be dragged
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerArea(
    player: Player,
    viewModel: GameViewModel,
    allPlayers: List<Player>,
    onCardAction: (CardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showHandDialog by remember { mutableStateOf(false) }
    var showGraveyardDialog by remember { mutableStateOf(false) }
    var showExileDialog by remember { mutableStateOf(false) }
    var showCommanderDamageDialog by remember { mutableStateOf(false) }
    var showLibrarySearchDialog by remember { mutableStateOf(false) }
    var showCommandZoneDialog by remember { mutableStateOf(false) }

    val libraryCount = viewModel.getCardCount(player.id, Zone.LIBRARY)
    val handCount = viewModel.getCardCount(player.id, Zone.HAND)
    val graveyardCount = viewModel.getCardCount(player.id, Zone.GRAVEYARD)
    val exileCount = viewModel.getCardCount(player.id, Zone.EXILE)
    val commanderCount = viewModel.getCardCount(player.id, Zone.COMMAND_ZONE)
    val battlefieldCards = viewModel.getCards(player.id, Zone.BATTLEFIELD)
    val handCards = viewModel.getCards(player.id, Zone.HAND)

    // Show hand dialog if requested
    if (showHandDialog) {
        HandDialog(
            cards = viewModel.getCards(player.id, Zone.HAND),
            onDismiss = { showHandDialog = false },
            onPlayCard = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
                showHandDialog = false
            },
            onDiscard = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.GRAVEYARD)
                showHandDialog = false
            },
            onExile = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.EXILE)
                showHandDialog = false
            },
            onToLibrary = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.LIBRARY)
                showHandDialog = false
            },
            onContextAction = onCardAction
        )
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
            }
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
            }
        )
    }

    // Show commander damage dialog if requested
    if (showCommanderDamageDialog) {
        CommanderDamageDialog(
            players = allPlayers,
            commanders = viewModel.getAllCommanders(),
            onDismiss = { showCommanderDamageDialog = false },
            onDamageChange = { playerId, commanderId, newDamage ->
                viewModel.updateCommanderDamage(playerId, commanderId, newDamage)
            }
        )
    }

    // Show library search dialog if requested
    if (showLibrarySearchDialog) {
        LibrarySearchDialog(
            cards = viewModel.getCards(player.id, Zone.LIBRARY),
            playerName = player.name,
            onDismiss = { showLibrarySearchDialog = false },
            onToHand = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.HAND)
                showLibrarySearchDialog = false
            },
            onToBattlefield = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
                showLibrarySearchDialog = false
            },
            onToTop = { cardInstance ->
                viewModel.moveCardToTopOfLibrary(cardInstance.instanceId)
                showLibrarySearchDialog = false
            },
            onShuffle = {
                viewModel.shuffleLibrary(player.id)
            }
        )
    }

    // Show command zone dialog if requested
    if (showCommandZoneDialog) {
        CommandZoneDialog(
            cards = viewModel.getCards(player.id, Zone.COMMAND_ZONE),
            playerName = player.name,
            onDismiss = { showCommandZoneDialog = false },
            onCastToBattlefield = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
                showCommandZoneDialog = false
            },
            onToHand = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.HAND)
                showCommandZoneDialog = false
            }
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Player's battlefield
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
        ) {
            DraggableBattlefieldGrid(
                cards = battlefieldCards,
                isLocalPlayer = true,
                onCardClick = { viewModel.toggleTap(it.instanceId) },
                onContextAction = onCardAction,
                onCardPositionChanged = { cardId, gridX, gridY ->
                    viewModel.updateCardGridPosition(cardId, gridX, gridY)
                },
                modifier = Modifier.fillMaxSize().padding(8.dp),
                currentPlayerId = player.id // Only this player can drag their cards
            )
        }

        // Player zones row
        Row(
            modifier = Modifier.fillMaxWidth().weight(0.3f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Commander zone
            ZoneCard(
                "Commander",
                Zone.COMMAND_ZONE,
                commanderCount,
                Modifier.width(120.dp).fillMaxHeight(),
                onClick = { showCommandZoneDialog = true }
            )

            // Your hand and info
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (player.hasLost) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    )
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
                        onClick = { showCommanderDamageDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Commander Damage")
                    }
                }
            }

            // Your library, graveyard, exile
            Column(
                modifier = Modifier.width(200.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZoneCard(
                    "Library",
                    Zone.LIBRARY,
                    libraryCount,
                    Modifier.weight(1f).fillMaxWidth(),
                    onClick = { showLibrarySearchDialog = true }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ZoneCard(
                        "Graveyard",
                        Zone.GRAVEYARD,
                        graveyardCount,
                        Modifier.weight(1f).fillMaxHeight(),
                        onClick = { showGraveyardDialog = true }
                    )
                    ZoneCard(
                        "Exile",
                        Zone.EXILE,
                        exileCount,
                        Modifier.weight(1f).fillMaxHeight(),
                        onClick = { showExileDialog = true }
                    )
                }
            }
        }

        // Hand display - always visible for local player
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.3f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Hand ($handCount)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    OutlinedButton(
                        onClick = { showHandDialog = true }
                    ) {
                        Text("Expand")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (handCards.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No cards in hand",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        handCards.forEach { cardInstance ->
                            HandCardDisplay(
                                cardInstance = cardInstance,
                                onCardClick = { showHandDialog = true },
                                onDoubleClick = {
                                    // Double-click plays card to battlefield
                                    viewModel.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
                                },
                                onContextAction = onCardAction
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZoneCard(
    label: String,
    zone: Zone,
    cardCount: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickableWithRipple { onClick() }
                } else {
                    Modifier
                }
            ),
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
private fun Modifier.clickableWithRipple(onClick: () -> Unit): Modifier {
    return this.clickable(onClick = onClick)
}

@Composable
fun HandDialog(
    cards: List<com.commandermtg.models.CardInstance>,
    onDismiss: () -> Unit,
    onPlayCard: (com.commandermtg.models.CardInstance) -> Unit,
    onDiscard: (com.commandermtg.models.CardInstance) -> Unit = {},
    onExile: (com.commandermtg.models.CardInstance) -> Unit = {},
    onToLibrary: (com.commandermtg.models.CardInstance) -> Unit = {},
    onContextAction: (CardAction) -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your Hand (${cards.size} cards)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cards.isEmpty()) {
                    Text("No cards in hand", style = MaterialTheme.typography.bodyMedium)
                } else {
                    cards.forEach { cardInstance ->
                        CardWithContextMenu(
                            cardInstance = cardInstance,
                            onAction = onContextAction
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
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Card image thumbnail
                                    CardImageThumbnail(
                                        imageUrl = cardInstance.card.imageUri,
                                        contentDescription = cardInstance.card.name
                                    )

                                    // Card info
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
                                }

                                // Action buttons row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Button(
                                        onClick = { onPlayCard(cardInstance) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Play", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = { onDiscard(cardInstance) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Discard", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = { onExile(cardInstance) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Exile", style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = { onToLibrary(cardInstance) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("To Library", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
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

@Composable
fun HandCardDisplay(
    cardInstance: CardInstance,
    onCardClick: (CardInstance) -> Unit,
    onDoubleClick: () -> Unit = {},
    onContextAction: (CardAction) -> Unit
) {
    var lastClickTime by remember { mutableStateOf(0L) }

    CardWithContextMenu(
        cardInstance = cardInstance,
        onAction = onContextAction
    ) {
        Card(
            modifier = Modifier
                .width(60.dp)
                .height(84.dp)
                .clickable {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime < 300) {
                        // Double-click detected
                        onDoubleClick()
                        lastClickTime = 0L
                    } else {
                        // Single click
                        lastClickTime = currentTime
                        onCardClick(cardInstance)
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (cardInstance.card.imageUri != null) {
                    CardImage(
                        imageUrl = cardInstance.card.imageUri,
                        contentDescription = cardInstance.card.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback text display
                    Text(
                        text = cardInstance.card.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(4.dp),
                        maxLines = 3,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
