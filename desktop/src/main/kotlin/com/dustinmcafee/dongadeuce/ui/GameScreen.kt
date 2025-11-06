@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.dustinmcafee.dongadeuce.models.Player
import kotlin.math.roundToInt
import com.dustinmcafee.dongadeuce.models.Zone
import com.dustinmcafee.dongadeuce.models.CardInstance
import com.dustinmcafee.dongadeuce.viewmodel.GameViewModel
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    loadedDeck: com.dustinmcafee.dongadeuce.models.Deck? = null,
    hotseatDecks: Map<Int, com.dustinmcafee.dongadeuce.models.Deck> = emptyMap(),
    playerCount: Int = 2, // Total players (including local player): 2, 3, or 4
    isHotseatMode: Boolean = false,
    viewModel: GameViewModel = remember { GameViewModel() }
) {
    val dragDropState = rememberDragDropState()
    val selectionState = rememberSelectionState()

    val uiState by viewModel.uiState.collectAsState()
    var cardDetailsToShow by remember { mutableStateOf<com.dustinmcafee.dongadeuce.models.CardInstance?>(null) }
    var showLibraryPositionDialog by remember { mutableStateOf(false) }
    var cardForLibraryPosition by remember { mutableStateOf<com.dustinmcafee.dongadeuce.models.CardInstance?>(null) }
    var showCounterDialog by remember { mutableStateOf(false) }
    var cardForCounterDialog by remember { mutableStateOf<com.dustinmcafee.dongadeuce.models.CardInstance?>(null) }
    var counterTypeForDialog by remember { mutableStateOf("") }

    // Handler for card actions - delegates business logic to ViewModel
    val handleAction: (CardAction) -> Unit = { action ->
        when (action) {
            is CardAction.ViewDetails -> {
                // Anyone can view card details - handle in UI
                cardDetailsToShow = action.cardInstance
            }
            is CardAction.ShowLibraryPositionDialog -> {
                // Show dialog for choosing library position
                cardForLibraryPosition = action.cardInstance
                showLibraryPositionDialog = true
            }
            is CardAction.ShowCounterDialog -> {
                // Show dialog for managing counters
                cardForCounterDialog = action.cardInstance
                counterTypeForDialog = action.counterType
                showCounterDialog = true
            }
            else -> {
                // Delegate to ViewModel with multi-selection support
                viewModel.handleBatchCardAction(
                    action = action,
                    selectedCardIds = selectionState.selectedCards.toSet()
                )
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

    Box(modifier = Modifier.fillMaxSize()) {
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

                // Rotate players so active player is first (will be at bottom)
                val rotatedPlayers = allPlayers.drop(activePlayerIndex) + allPlayers.take(activePlayerIndex)

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
                                dragDropState = dragDropState,
                                selectionState = selectionState,
                                otherPlayers = rotatedPlayers.filter { it.id != rotatedPlayers[1].id },
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                            HotseatPlayerSection(
                                player = rotatedPlayers[0],
                                viewModel = viewModel,
                                isActivePlayer = true,
                                onCardAction = handleAction,
                                dragDropState = dragDropState,
                                selectionState = selectionState,
                                otherPlayers = rotatedPlayers.filter { it.id != rotatedPlayers[0].id },
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
                                    dragDropState = dragDropState,
                                    selectionState = selectionState,
                                    otherPlayers = rotatedPlayers.filter { it.id != rotatedPlayers[1].id },
                                    modifier = Modifier.weight(1f)
                                )
                                HotseatPlayerSection(
                                    player = rotatedPlayers[2],
                                    viewModel = viewModel,
                                    isActivePlayer = false,
                                    onCardAction = handleAction,
                                    dragDropState = dragDropState,
                                    selectionState = selectionState,
                                    otherPlayers = rotatedPlayers.filter { it.id != rotatedPlayers[2].id },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                HotseatPlayerSection(
                                    player = rotatedPlayers[0],
                                    viewModel = viewModel,
                                    isActivePlayer = true,
                                    onCardAction = handleAction,
                                    dragDropState = dragDropState,
                                    selectionState = selectionState,
                                    otherPlayers = rotatedPlayers.filter { it.id != rotatedPlayers[0].id },
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
                                    dragDropState = dragDropState,
                                    selectionState = selectionState,
                                    otherPlayers = rotatedPlayers.filter { it.id != rotatedPlayers[2].id },
                                    modifier = Modifier.weight(1f)
                                )
                                HotseatPlayerSection(
                                    player = rotatedPlayers[3],
                                    viewModel = viewModel,
                                    isActivePlayer = false,
                                    onCardAction = handleAction,
                                    dragDropState = dragDropState,
                                    selectionState = selectionState,
                                    otherPlayers = rotatedPlayers.filter { it.id != rotatedPlayers[3].id },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                HotseatPlayerSection(
                                    player = rotatedPlayers[0],
                                    viewModel = viewModel,
                                    isActivePlayer = true,
                                    onCardAction = handleAction,
                                    dragDropState = dragDropState,
                                    selectionState = selectionState,
                                    otherPlayers = rotatedPlayers.filter { it.id != rotatedPlayers[0].id },
                                    modifier = Modifier.weight(1f),
                                    inverted = true
                                )
                                HotseatPlayerSection(
                                    player = rotatedPlayers[1],
                                    viewModel = viewModel,
                                    isActivePlayer = false,
                                    onCardAction = handleAction,
                                    dragDropState = dragDropState,
                                    selectionState = selectionState,
                                    otherPlayers = rotatedPlayers.filter { it.id != rotatedPlayers[1].id },
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
                            .weight(0.4f),
                        selectionState = selectionState,
                        allPlayers = opponents + listOfNotNull(localPlayer)
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
                        dragDropState = dragDropState,
                        selectionState = selectionState,
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

    // Library position dialog
    if (showLibraryPositionDialog && cardForLibraryPosition != null) {
        val card = cardForLibraryPosition!!
        val librarySize = viewModel.getCardCount(card.ownerId, Zone.LIBRARY)

        LibraryPositionDialog(
            cardName = card.card.name,
            librarySize = librarySize,
            onDismiss = {
                showLibraryPositionDialog = false
                cardForLibraryPosition = null
            },
            onToTop = {
                viewModel.moveCardToTopOfLibrary(card.instanceId)
            },
            onToBottom = {
                viewModel.moveCardToBottomOfLibrary(card.instanceId)
            },
            onToPositionFromTop = { position ->
                viewModel.moveCardToLibraryPosition(card.instanceId, position)
            },
            onToPositionFromBottom = { position ->
                viewModel.moveCardToLibraryPositionFromBottom(card.instanceId, position)
            }
        )
    }

    // Counter dialog
    if (showCounterDialog && cardForCounterDialog != null) {
        val card = cardForCounterDialog!!
        val currentCount = card.counters[counterTypeForDialog] ?: 0

        CounterDialog(
            cardName = card.card.name,
            counterType = counterTypeForDialog,
            currentCount = currentCount,
            onDismiss = {
                showCounterDialog = false
                cardForCounterDialog = null
                counterTypeForDialog = ""
            },
            onSet = { amount ->
                viewModel.setCounter(card.instanceId, counterTypeForDialog, amount)
            },
            onAdd = { amount ->
                viewModel.addCounter(card.instanceId, counterTypeForDialog, amount)
            },
            onSubtract = { amount ->
                viewModel.removeCounter(card.instanceId, counterTypeForDialog, amount)
            }
        )
    }

    // Game over dialog
    if (uiState.gameEnded) {
        val winner = uiState.allPlayers.firstOrNull { !it.hasLost }
        AlertDialog(
            onDismissRequest = { /* Game has ended, cannot dismiss */ },
            title = { Text("Game Over") },
            text = {
                Column {
                    if (winner != null) {
                        Text(
                            "Winner: ${winner.name}!",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    } else {
                        Text(
                            "All players have been eliminated.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Final Standings:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    uiState.allPlayers.forEach { player ->
                        Text(
                            "${player.name}: ${if (player.hasLost) "Defeated" else "Winner"} (Life: ${player.life})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { /* Acknowledges game over, dialog stays visible */ }) {
                    Text("OK")
                }
            }
        )
    }

        // Logo overlay in top-left corner
        Image(
            painter = painterResource("dongadeuce_logo.png"),
            contentDescription = "Dong-A-Deuce Logo",
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.TopStart)
                .padding(8.dp)
                .alpha(0.7f)
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
    dragDropState: DragDropState? = null,
    selectionState: SelectionState? = null,
    otherPlayers: List<Player> = emptyList(),
    modifier: Modifier = Modifier,
    inverted: Boolean = false // If true, hand at bottom; if false, hand at top
) {
    val handCards = if (isActivePlayer) viewModel.getCards(player.id, Zone.HAND) else emptyList()
    val handCount = viewModel.getCardCount(player.id, Zone.HAND)
    val battlefieldCards = viewModel.getCards(player.id, Zone.BATTLEFIELD)
    val libraryCount = viewModel.getCardCount(player.id, Zone.LIBRARY)
    val graveyardCount = viewModel.getCardCount(player.id, Zone.GRAVEYARD)
    val exileCount = viewModel.getCardCount(player.id, Zone.EXILE)
    val commanderCount = viewModel.getCardCount(player.id, Zone.COMMAND_ZONE)

    var showGraveyardDialog by remember { mutableStateOf(false) }
    var showExileDialog by remember { mutableStateOf(false) }
    var showLibrarySearchDialog by remember { mutableStateOf(false) }
    var showLibraryOperationsDialog by remember { mutableStateOf(false) }
    var showLibraryPeekDialog by remember { mutableStateOf(false) }
    var libraryPeekCards by remember { mutableStateOf<List<CardInstance>>(emptyList()) }
    var libraryPeekLocation by remember { mutableStateOf(PeekLocation.TOP) }
    var showCommandZoneDialog by remember { mutableStateOf(false) }
    var showTokenCreationDialog by remember { mutableStateOf(false) }
    var showSetLifeDialog by remember { mutableStateOf(false) }

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
                dragDropState = dragDropState,
                selectionState = if (isActivePlayer) selectionState else null,
                otherPlayers = otherPlayers,
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )
        }

        // Battlefield in center
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Player info sidebar
                Column(
                    modifier = Modifier.width(150.dp).fillMaxHeight().padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        player.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )

                    // Life with increment/decrement buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.updateLife(player.id, player.life - 1) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Text("-", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                        Text(
                            "Life: ${player.life}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.clickable { showSetLifeDialog = true }
                        )
                        IconButton(
                            onClick = { viewModel.updateLife(player.id, player.life + 1) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Text("+", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Clickable zone cards
                    ZoneCard(
                        "Commander",
                        Zone.COMMAND_ZONE,
                        commanderCount,
                        Modifier.fillMaxWidth().height(50.dp),
                        onClick = if (isActivePlayer) ({ showCommandZoneDialog = true }) else null
                    )
                    ZoneCard(
                        "Library",
                        Zone.LIBRARY,
                        libraryCount,
                        Modifier.fillMaxWidth().height(50.dp),
                        onClick = null, // No single-click action
                        onDoubleClick = if (isActivePlayer) ({ viewModel.drawCard(player.id) }) else null,
                        onRightClick = if (isActivePlayer) ({ showLibraryOperationsDialog = true }) else null,
                        dragDropState = if (isActivePlayer) dragDropState else null,
                        onDropCards = if (isActivePlayer) {
                            { cardIds ->
                                // Mark that zone is handling this drop
                                dragDropState?.markHandledByZone()
                                cardIds.forEach { cardId ->
                                    viewModel.moveCardToTopOfLibrary(cardId)
                                }
                                // Clear drag state after handling drop
                                dragDropState?.endDrag()
                            }
                        } else null
                    )
                    ZoneCard(
                        "Graveyard",
                        Zone.GRAVEYARD,
                        graveyardCount,
                        Modifier.fillMaxWidth().height(50.dp),
                        onClick = if (isActivePlayer) ({ showGraveyardDialog = true }) else null,
                        dragDropState = if (isActivePlayer) dragDropState else null,
                        onDropCards = if (isActivePlayer) {
                            { cardIds ->
                                // Mark that zone is handling this drop
                                dragDropState?.markHandledByZone()
                                cardIds.forEach { cardId ->
                                    viewModel.moveCard(cardId, Zone.GRAVEYARD)
                                }
                                // Clear drag state after handling drop
                                dragDropState?.endDrag()
                            }
                        } else null
                    )
                    ZoneCard(
                        "Exile",
                        Zone.EXILE,
                        exileCount,
                        Modifier.fillMaxWidth().height(50.dp),
                        onClick = if (isActivePlayer) ({ showExileDialog = true }) else null,
                        dragDropState = if (isActivePlayer) dragDropState else null,
                        onDropCards = if (isActivePlayer) {
                            { cardIds ->
                                // Mark that zone is handling this drop
                                dragDropState?.markHandledByZone()
                                cardIds.forEach { cardId ->
                                    viewModel.moveCard(cardId, Zone.EXILE)
                                }
                                // Clear drag state after handling drop
                                dragDropState?.endDrag()
                            }
                        } else null
                    )

                    // Token creation button (only for active player)
                    if (isActivePlayer) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showTokenCreationDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Create Token", style = MaterialTheme.typography.labelSmall)
                        }
                    }
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
                        selectionState = selectionState,
                        currentPlayerId = if (isActivePlayer) player.id else null,
                        otherPlayers = otherPlayers,
                        allPlayers = otherPlayers + listOf(player),
                        dragDropState = if (isActivePlayer) dragDropState else null,
                        onDropToZone = if (isActivePlayer) { cardIds, zone ->
                            cardIds.forEach { cardId ->
                                when (zone) {
                                    Zone.LIBRARY -> viewModel.moveCardToTopOfLibrary(cardId)
                                    else -> viewModel.moveCard(cardId, zone)
                                }
                            }
                        } else null
                    )
                }
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
                dragDropState = dragDropState,
                selectionState = if (isActivePlayer) selectionState else null,
                otherPlayers = otherPlayers,
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )
        }
    }

    // Zone dialogs for active player
    if (isActivePlayer) {
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

        if (showLibrarySearchDialog) {
            LibrarySearchDialog(
                cards = viewModel.getCards(player.id, Zone.LIBRARY),
                playerName = player.name,
                onDismiss = { showLibrarySearchDialog = false },
                onToHand = { cardInstance ->
                    viewModel.moveCard(cardInstance.instanceId, Zone.HAND)
                },
                onToBattlefield = { cardInstance ->
                    viewModel.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
                },
                onToTop = { cardInstance ->
                    viewModel.moveCardToTopOfLibrary(cardInstance.instanceId)
                },
                onToBottom = { cardInstance ->
                    viewModel.moveCardToBottomOfLibrary(cardInstance.instanceId)
                },
                onShuffle = { viewModel.shuffleLibrary(player.id) }
            )
        }

        if (showLibraryOperationsDialog) {
            LibraryOperationsDialog(
                playerName = player.name,
                librarySize = libraryCount,
                onDismiss = { showLibraryOperationsDialog = false },
                onViewTopCards = { count ->
                    libraryPeekCards = viewModel.getTopCards(player.id, count)
                    libraryPeekLocation = PeekLocation.TOP
                    showLibraryPeekDialog = true
                    showLibraryOperationsDialog = false
                },
                onViewBottomCards = { count ->
                    libraryPeekCards = viewModel.getBottomCards(player.id, count)
                    libraryPeekLocation = PeekLocation.BOTTOM
                    showLibraryPeekDialog = true
                    showLibraryOperationsDialog = false
                },
                onShuffleTopCards = { count ->
                    viewModel.shuffleTopCards(player.id, count)
                },
                onShuffleBottomCards = { count ->
                    viewModel.shuffleBottomCards(player.id, count)
                },
                onMoveTopToZone = { count, zone ->
                    viewModel.moveTopCardsToZone(player.id, count, zone)
                },
                onMoveBottomToZone = { count, zone ->
                    viewModel.moveBottomCardsToZone(player.id, count, zone)
                },
                onRevealTopCard = {
                    val topCard = viewModel.getTopCards(player.id, 1).firstOrNull()
                    if (topCard != null) {
                        libraryPeekCards = listOf(topCard)
                        libraryPeekLocation = PeekLocation.TOP
                        showLibraryPeekDialog = true
                        showLibraryOperationsDialog = false
                    }
                },
                onFullSearch = {
                    showLibraryOperationsDialog = false
                    showLibrarySearchDialog = true
                }
            )
        }

        if (showLibraryPeekDialog) {
            LibraryPeekDialog(
                cards = libraryPeekCards,
                playerName = player.name,
                peekLocation = libraryPeekLocation,
                onDismiss = {
                    showLibraryPeekDialog = false
                    libraryPeekCards = emptyList()
                },
                onMoveCard = { cardInstance, zone ->
                    viewModel.moveCard(cardInstance.instanceId, zone)
                    // Update the peek list to remove the moved card
                    libraryPeekCards = libraryPeekCards.filter { it.instanceId != cardInstance.instanceId }
                },
                onMoveAllToZone = { zone ->
                    libraryPeekCards.forEach { cardInstance ->
                        viewModel.moveCard(cardInstance.instanceId, zone)
                    }
                    showLibraryPeekDialog = false
                    libraryPeekCards = emptyList()
                },
                onShuffleCards = {
                    when (libraryPeekLocation) {
                        PeekLocation.TOP -> viewModel.shuffleTopCards(player.id, libraryPeekCards.size)
                        PeekLocation.BOTTOM -> viewModel.shuffleBottomCards(player.id, libraryPeekCards.size)
                    }
                    showLibraryPeekDialog = false
                    libraryPeekCards = emptyList()
                }
            )
        }

        if (showCommandZoneDialog) {
            CommandZoneDialog(
                cards = viewModel.getCards(player.id, Zone.COMMAND_ZONE),
                playerName = player.name,
                onDismiss = { showCommandZoneDialog = false },
                onCastToBattlefield = { cardInstance ->
                    viewModel.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
                },
                onToHand = { cardInstance ->
                    viewModel.moveCard(cardInstance.instanceId, Zone.HAND)
                }
            )
        }

        if (showTokenCreationDialog) {
            TokenCreationDialog(
                viewModel = viewModel,
                onDismiss = { showTokenCreationDialog = false },
                onCreateToken = { tokenName, tokenType, power, toughness, color, imageUri, quantity ->
                    viewModel.createToken(player.id, tokenName, tokenType, power, toughness, color, imageUri, quantity)
                }
            )
        }

        if (showSetLifeDialog) {
            SetLifeDialog(
                playerName = player.name,
                currentLife = player.life,
                onDismiss = { showSetLifeDialog = false },
                onConfirm = { newLife ->
                    viewModel.updateLife(player.id, newLife)
                }
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
    dragDropState: DragDropState? = null,
    selectionState: SelectionState? = null,
    otherPlayers: List<Player> = emptyList(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${player.name}'s Hand ($handCount)",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(120.dp)
                )

                // Shared drag state for hand cards
                var draggedHandCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }
                var handDragOffset by remember { mutableStateOf(Offset.Zero) }

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
                                onContextAction = onCardAction,
                                dragDropState = dragDropState,
                                selectionState = selectionState,
                                sharedDraggedCardIds = draggedHandCardIds,
                                sharedDragOffset = handDragOffset,
                                onDragStateChange = { draggedIds, offset ->
                                    draggedHandCardIds = draggedIds
                                    handDragOffset = offset
                                },
                                otherPlayers = otherPlayers
                            )
                        }
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
                onCardAction = onCardAction,
                modifier = modifier,
                selectionState = selectionState,
                otherPlayers = allPlayers.filter { it.id != opponents[0].id },
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
                    onCardAction = onCardAction,
                    modifier = Modifier.weight(1f),
                    selectionState = selectionState,
                    otherPlayers = allPlayers.filter { it.id != opponents[0].id },
                    allPlayers = allPlayers
                )
                OpponentArea(
                    player = opponents[1],
                    viewModel = viewModel,
                    onCardAction = onCardAction,
                    modifier = Modifier.weight(1f),
                    selectionState = selectionState,
                    otherPlayers = allPlayers.filter { it.id != opponents[1].id },
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
                        onCardAction = onCardAction,
                        modifier = Modifier.weight(1f),
                        selectionState = selectionState,
                        otherPlayers = allPlayers.filter { it.id != opponent.id },
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
                        onCardAction = onCardAction,
                        modifier = Modifier.weight(1f),
                        selectionState = selectionState,
                        otherPlayers = allPlayers.filter { it.id != opponent.id },
                        allPlayers = allPlayers
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
    modifier: Modifier = Modifier,
    selectionState: SelectionState? = null,
    otherPlayers: List<Player> = emptyList(),
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
                selectionState = selectionState,
                currentPlayerId = null, // Opponent cards cannot be dragged
                otherPlayers = otherPlayers,
                allPlayers = allPlayers
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
    dragDropState: DragDropState? = null,
    selectionState: SelectionState? = null,
    modifier: Modifier = Modifier
) {
    var showHandDialog by remember { mutableStateOf(false) }
    var showGraveyardDialog by remember { mutableStateOf(false) }
    var showExileDialog by remember { mutableStateOf(false) }
    var showCommanderDamageDialog by remember { mutableStateOf(false) }
    var showLibrarySearchDialog by remember { mutableStateOf(false) }
    var showCommandZoneDialog by remember { mutableStateOf(false) }
    var showTokenCreationDialog by remember { mutableStateOf(false) }
    var showSetLifeDialog by remember { mutableStateOf(false) }

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

    // Show token creation dialog if requested
    if (showTokenCreationDialog) {
        TokenCreationDialog(
            viewModel = viewModel,
            onDismiss = { showTokenCreationDialog = false },
            onCreateToken = { tokenName, tokenType, power, toughness, color, imageUri, quantity ->
                viewModel.createToken(player.id, tokenName, tokenType, power, toughness, color, imageUri, quantity)
            }
        )
    }

    // Show set life dialog if requested
    if (showSetLifeDialog) {
        SetLifeDialog(
            playerName = player.name,
            currentLife = player.life,
            onDismiss = { showSetLifeDialog = false },
            onConfirm = { newLife ->
                viewModel.updateLife(player.id, newLife)
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
                selectionState = selectionState,
                currentPlayerId = player.id, // Only this player can drag their cards
                otherPlayers = allPlayers.filter { it.id != player.id },
                allPlayers = allPlayers,
                dragDropState = dragDropState,
                onDropToZone = { cardIds, zone ->
                    cardIds.forEach { cardId ->
                        when (zone) {
                            Zone.LIBRARY -> viewModel.moveCardToTopOfLibrary(cardId)
                            else -> viewModel.moveCard(cardId, zone)
                        }
                    }
                }
            )
        }

        // Player zones row
        Row(
            modifier = Modifier.fillMaxWidth().weight(0.3f).heightIn(min = 120.dp),
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
                                Text(
                                    "Life: ${player.life}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.clickable { showSetLifeDialog = true }
                                )
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

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                    OutlinedButton(
                        onClick = { showTokenCreationDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Token")
                    }

                    OutlinedButton(
                        onClick = {
                            // Mark player as having lost
                            viewModel.markPlayerAsLost(player.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Leave Game")
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
                    onClick = { showLibrarySearchDialog = true },
                    dragDropState = dragDropState,
                    onDropCards = { cardIds ->
                        // Mark that zone is handling this drop
                        dragDropState?.markHandledByZone()
                        cardIds.forEach { cardId ->
                            viewModel.moveCardToTopOfLibrary(cardId)
                        }
                        // Clear drag state after handling drop
                        dragDropState?.endDrag()
                    }
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
                        onClick = { showGraveyardDialog = true },
                        dragDropState = dragDropState,
                        onDropCards = { cardIds ->
                            // Mark that zone is handling this drop
                            dragDropState?.markHandledByZone()
                            cardIds.forEach { cardId ->
                                viewModel.moveCard(cardId, Zone.GRAVEYARD)
                            }
                            // Clear drag state after handling drop
                            dragDropState?.endDrag()
                        }
                    )
                    ZoneCard(
                        "Exile",
                        Zone.EXILE,
                        exileCount,
                        Modifier.weight(1f).fillMaxHeight(),
                        onClick = { showExileDialog = true },
                        dragDropState = dragDropState,
                        onDropCards = { cardIds ->
                            // Mark that zone is handling this drop
                            dragDropState?.markHandledByZone()
                            cardIds.forEach { cardId ->
                                viewModel.moveCard(cardId, Zone.EXILE)
                            }
                            // Clear drag state after handling drop
                            dragDropState?.endDrag()
                        }
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
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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

                    // Shared drag state for hand cards
                    var draggedHandCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }
                    var handDragOffset by remember { mutableStateOf(Offset.Zero) }

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
                                    onContextAction = onCardAction,
                                    dragDropState = dragDropState,
                                    selectionState = selectionState,
                                    sharedDraggedCardIds = draggedHandCardIds,
                                    sharedDragOffset = handDragOffset,
                                    onDragStateChange = { draggedIds, offset ->
                                        draggedHandCardIds = draggedIds
                                        handDragOffset = offset
                                    },
                                    otherPlayers = allPlayers.filter { it.id != player.id }
                                )
                            }
                        }
                    }
                }

                // Batch actions row (only show when cards are selected)
                if (selectionState?.hasSelection == true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${selectionState.selectionCount} card${if (selectionState.selectionCount > 1) "s" else ""} selected",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Button(
                            onClick = {
                                selectionState.selectedCards.forEach { cardId ->
                                    viewModel.moveCard(cardId, Zone.BATTLEFIELD)
                                }
                                selectionState.clearSelection()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("To Battlefield")
                        }
                        Button(
                            onClick = {
                                selectionState.selectedCards.forEach { cardId ->
                                    viewModel.moveCard(cardId, Zone.GRAVEYARD)
                                }
                                selectionState.clearSelection()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("To Graveyard")
                        }
                        Button(
                            onClick = {
                                selectionState.selectedCards.forEach { cardId ->
                                    viewModel.moveCard(cardId, Zone.EXILE)
                                }
                                selectionState.clearSelection()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("To Exile")
                        }
                        OutlinedButton(
                            onClick = { selectionState.clearSelection() }
                        ) {
                            Text("Clear")
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
    onClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onRightClick: (() -> Unit)? = null,
    dragDropState: DragDropState? = null,
    onDropCards: ((List<String>) -> Unit)? = null
) {
    var isHovering by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }

    // Check if cards are being dragged over this zone
    val isDraggingOver = dragDropState != null &&
                        dragDropState.draggedCardIds.isNotEmpty() &&
                        isHovering

    Card(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                // Register zone bounds for accurate drop detection
                if (dragDropState != null) {
                    val bounds = Rect(
                        coordinates.positionInWindow().x,
                        coordinates.positionInWindow().y,
                        coordinates.positionInWindow().x + coordinates.size.width,
                        coordinates.positionInWindow().y + coordinates.size.height
                    )
                    dragDropState.registerZoneBounds(zone, bounds)
                }
            }
            .border(
                width = if (isDraggingOver) 3.dp else 1.dp,
                color = if (isDraggingOver) Color(0xFF00FF00) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .then(
                if (onDoubleClick != null || onRightClick != null || onClick != null) {
                    Modifier.pointerInput(zone) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()

                                // Handle right-click
                                if (event.buttons.isSecondaryPressed && onRightClick != null) {
                                    onRightClick()
                                }
                                // Handle left-click for double-click detection
                                else if (event.changes.any { !it.pressed && it.previousPressed }) {
                                    val change = event.changes.first { !it.pressed && it.previousPressed }
                                    change.consume()

                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastClickTime < 300L && onDoubleClick != null) {
                                        // Double-click detected
                                        onDoubleClick()
                                        lastClickTime = 0L
                                    } else {
                                        // First click
                                        lastClickTime = currentTime
                                        onClick?.invoke()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (onDropCards != null && dragDropState != null) {
                    Modifier.pointerInput(zone) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        if (dragDropState.draggedCardIds.isNotEmpty()) {
                                            isHovering = true
                                            dragDropState.setHoveredZone(zone)
                                        }
                                    }
                                    PointerEventType.Exit -> {
                                        isHovering = false
                                        if (dragDropState.hoveredZone == zone) {
                                            dragDropState.setHoveredZone(null)
                                        }
                                    }
                                    PointerEventType.Release -> {
                                        // Only handle drop if we're hovering AND cards are being dragged
                                        // Don't consume the event - let battlefield handle its own drops
                                        if (isHovering && dragDropState.draggedCardIds.isNotEmpty()) {
                                            onDropCards(dragDropState.draggedCardIds.toList())
                                            isHovering = false
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
        colors = CardDefaults.cardColors(
            containerColor = if (isDraggingOver)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
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
                if (isDraggingOver) {
                    Text(
                        "Drop here",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
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
    cards: List<com.dustinmcafee.dongadeuce.models.CardInstance>,
    onDismiss: () -> Unit,
    onPlayCard: (com.dustinmcafee.dongadeuce.models.CardInstance) -> Unit,
    onDiscard: (com.dustinmcafee.dongadeuce.models.CardInstance) -> Unit = {},
    onExile: (com.dustinmcafee.dongadeuce.models.CardInstance) -> Unit = {},
    onToLibrary: (com.dustinmcafee.dongadeuce.models.CardInstance) -> Unit = {},
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
    onContextAction: (CardAction) -> Unit,
    dragDropState: DragDropState? = null,
    selectionState: SelectionState? = null,
    sharedDraggedCardIds: Set<String> = emptySet(),
    sharedDragOffset: Offset = Offset.Zero,
    onDragStateChange: (Set<String>, Offset) -> Unit = { _, _ -> },
    otherPlayers: List<com.dustinmcafee.dongadeuce.models.Player> = emptyList()
) {
    var lastClickTime by remember { mutableStateOf(0L) }
    val isSelected = selectionState?.isSelected(cardInstance.instanceId) == true
    val isDragged = sharedDraggedCardIds.contains(cardInstance.instanceId)

    CardWithContextMenu(
        cardInstance = cardInstance,
        onAction = onContextAction,
        otherPlayers = otherPlayers
    ) {
        Box {
            Card(
                modifier = Modifier
                    .width(UIConstants.HAND_CARD_WIDTH)
                    .height(UIConstants.HAND_CARD_HEIGHT)
                    .graphicsLayer {
                        if (isDragged) {
                            alpha = 0.5f
                            translationX = sharedDragOffset.x
                            translationY = sharedDragOffset.y
                        }
                    }
                    // Combined click and drag gesture support
                    .pointerInput(cardInstance.instanceId) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()

                                if (event.type == PointerEventType.Press) {
                                    // Skip right-clicks (they trigger context menu)
                                    // Check if this is NOT a primary (left) button press
                                    val isPrimaryClick = event.button == PointerButton.Primary
                                    val isRightClick = !isPrimaryClick && event.button != null

                                    if (isRightClick) {
                                        // Right-click detected - consume the release event and skip all logic
                                        awaitPointerEvent()
                                        continue
                                    }

                                    // Left click - handle selection and drag
                                    val isShiftPressed = event.keyboardModifiers.isShiftPressed
                                    var totalDrag = Offset.Zero
                                    var isDragging = false

                                    // Track drag or click
                                    while (true) {
                                        val moveEvent = awaitPointerEvent()

                                        if (moveEvent.type == PointerEventType.Move) {
                                            val change = moveEvent.changes.first()
                                            val dragAmount = change.position - change.previousPosition
                                            totalDrag += dragAmount

                                            // Start drag if moved more than threshold
                                            if (!isDragging && totalDrag.getDistance() > UIConstants.DRAG_THRESHOLD_PX) {
                                                isDragging = true
                                                // Determine which cards to drag
                                                val cardsToDrag = if (selectionState?.isSelected(cardInstance.instanceId) == true &&
                                                                       selectionState.selectedCards.size > 1) {
                                                    // Drag all selected cards
                                                    selectionState.selectedCards.toSet()
                                                } else {
                                                    // Drag just this card
                                                    setOf(cardInstance.instanceId)
                                                }
                                                onDragStateChange(cardsToDrag, Offset.Zero)
                                            }

                                            if (isDragging) {
                                                change.consume()
                                                onDragStateChange(sharedDraggedCardIds, sharedDragOffset + dragAmount)
                                            }
                                        } else if (moveEvent.type == PointerEventType.Release) {
                                            if (isDragging) {
                                                // Drag ended
                                                if (sharedDragOffset.getDistance() > UIConstants.DRAG_DISTANCE_THRESHOLD_PX) {
                                                    // User dragged the card(s) - play to battlefield
                                                    // The action will be applied to all selected cards via handleBatchCardAction
                                                    onContextAction(CardAction.ToBattlefield(cardInstance))
                                                }
                                                onDragStateChange(emptySet(), Offset.Zero)
                                            } else {
                                                // Click (no drag)
                                                val clickTime = System.currentTimeMillis()
                                                val timeSinceLastClick = clickTime - lastClickTime
                                                lastClickTime = clickTime

                                                if (timeSinceLastClick < UIConstants.DOUBLE_CLICK_DELAY_MS) {
                                                    // Double-click - clear selection and play card
                                                    selectionState?.clearSelection()
                                                    onDoubleClick()
                                                } else if (isShiftPressed && selectionState != null) {
                                                    // Shift+click - toggle selection
                                                    selectionState.toggleSelection(cardInstance.instanceId)
                                                } else {
                                                    // Regular click - always clear and select only this card
                                                    if (selectionState != null) {
                                                        selectionState.clearSelection()
                                                        selectionState.select(cardInstance.instanceId)
                                                    } else {
                                                        onCardClick(cardInstance)
                                                    }
                                                }
                                            }
                                            break // Exit inner loop after release
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .then(
                        if (isSelected) Modifier.border(3.dp, Color(0xFF00FF00), RoundedCornerShape(8.dp)) else Modifier
                    ),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer
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
}

/**
 * Dialog for creating tokens
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TokenCreationDialog(
    viewModel: com.dustinmcafee.dongadeuce.viewmodel.GameViewModel,
    onDismiss: () -> Unit,
    onCreateToken: (tokenName: String, tokenType: String, power: String?, toughness: String?, color: String, imageUri: String?, quantity: Int) -> Unit
) {
    var tokenName by remember { mutableStateOf("") }
    var tokenType by remember { mutableStateOf("Creature Token") }
    var power by remember { mutableStateOf("") }
    var toughness by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("Colorless") }
    var tokenImageUri by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var searchQuery by remember { mutableStateOf("") }

    val colors = listOf("Colorless", "White", "Blue", "Black", "Red", "Green", "Multicolor")
    val uiState by viewModel.uiState.collectAsState()
    val searchResults = uiState.tokenSearchResults
    val isSearching = uiState.isSearchingTokens

    // Clear search on dismiss
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearTokenSearch()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Token") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchTokens(it)
                    },
                    label = { Text("Search Scryfall Tokens") },
                    placeholder = { Text("e.g., Goblin, Soldier, Treasure") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                )

                // Search results
                if (searchResults.isNotEmpty()) {
                    Text("Search Results (tap to use):", style = MaterialTheme.typography.labelMedium)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            searchResults.forEach { card ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Auto-fill form with selected token
                                            tokenName = card.name
                                            tokenType = card.type ?: "Creature Token"
                                            power = card.power ?: ""
                                            toughness = card.toughness ?: ""
                                            tokenImageUri = card.imageUri
                                            selectedColor = when {
                                                card.colors.isEmpty() -> "Colorless"
                                                card.colors.size > 1 -> "Multicolor"
                                                card.colors.contains("W") -> "White"
                                                card.colors.contains("U") -> "Blue"
                                                card.colors.contains("B") -> "Black"
                                                card.colors.contains("R") -> "Red"
                                                card.colors.contains("G") -> "Green"
                                                else -> "Colorless"
                                            }
                                            searchQuery = ""
                                            viewModel.clearTokenSearch()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(card.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "${card.type ?: "Token"} ${if (card.power != null && card.toughness != null) "${card.power}/${card.toughness}" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Divider()

                Text("Or create custom token:", style = MaterialTheme.typography.labelMedium)

                // Token Name
                OutlinedTextField(
                    value = tokenName,
                    onValueChange = { tokenName = it },
                    label = { Text("Token Name") },
                    placeholder = { Text("e.g., Goblin, Soldier, Treasure") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Token Type
                OutlinedTextField(
                    value = tokenType,
                    onValueChange = { tokenType = it },
                    label = { Text("Type") },
                    placeholder = { Text("e.g., Creature — Goblin, Artifact") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Power/Toughness Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = power,
                        onValueChange = { power = it },
                        label = { Text("Power") },
                        placeholder = { Text("1") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = toughness,
                        onValueChange = { toughness = it },
                        label = { Text("Toughness") },
                        placeholder = { Text("1") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Color Dropdown
                Text("Color", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { color ->
                        FilterChip(
                            selected = selectedColor == color,
                            onClick = { selectedColor = color },
                            label = { Text(color) }
                        )
                    }
                }

                // Quantity
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.all { char -> char.isDigit() }) quantity = it },
                    label = { Text("Quantity") },
                    placeholder = { Text("1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = tokenName.trim().ifBlank { "Token" }
                    val type = tokenType.trim().ifBlank { "Token" }
                    val pow = power.trim().ifBlank { null }
                    val tough = toughness.trim().ifBlank { null }
                    val qty = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val colorValue = if (selectedColor == "Colorless") "" else selectedColor

                    onCreateToken(name, type, pow, tough, colorValue, tokenImageUri, qty)
                    onDismiss()
                },
                enabled = tokenName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
