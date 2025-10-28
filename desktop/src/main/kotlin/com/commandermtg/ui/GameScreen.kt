package com.commandermtg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.commandermtg.models.Player
import com.commandermtg.models.Zone
import com.commandermtg.models.CardInstance
import com.commandermtg.viewmodel.GameViewModel
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    loadedDeck: com.commandermtg.models.Deck? = null,
    hotseatDecks: Map<Int, com.commandermtg.models.Deck> = emptyMap(),
    playerCount: Int = 2, // Total players (including local player): 2, 3, or 4
    isHotseatMode: Boolean = false,
    viewModel: GameViewModel = remember { GameViewModel() }
) {
    val dragDropState = rememberDragDropState()
    val selectionState = rememberSelectionState()
    var showDropZoneDialog by remember { mutableStateOf(false) }
    var cardToDrop by remember { mutableStateOf<CardInstance?>(null) }

    val uiState by viewModel.uiState.collectAsState()
    var cardDetailsToShow by remember { mutableStateOf<com.commandermtg.models.CardInstance?>(null) }

    // Handle drag end - show drop zone selection dialog
    LaunchedEffect(dragDropState.isDragging) {
        if (!dragDropState.isDragging && dragDropState.draggedCard != null) {
            cardToDrop = dragDropState.draggedCard
            showDropZoneDialog = true
        }
    }

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
                                dragDropState = dragDropState,
                                selectionState = selectionState,
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                            HotseatPlayerSection(
                                player = rotatedPlayers[0],
                                viewModel = viewModel,
                                isActivePlayer = true,
                                onCardAction = handleAction,
                                dragDropState = dragDropState,
                                selectionState = selectionState,
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
                                    modifier = Modifier.weight(1f)
                                )
                                HotseatPlayerSection(
                                    player = rotatedPlayers[2],
                                    viewModel = viewModel,
                                    isActivePlayer = false,
                                    onCardAction = handleAction,
                                    dragDropState = dragDropState,
                                    selectionState = selectionState,
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
                                    modifier = Modifier.weight(1f)
                                )
                                HotseatPlayerSection(
                                    player = rotatedPlayers[3],
                                    viewModel = viewModel,
                                    isActivePlayer = false,
                                    onCardAction = handleAction,
                                    dragDropState = dragDropState,
                                    selectionState = selectionState,
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

    // Drop zone selection dialog
    if (showDropZoneDialog && cardToDrop != null) {
        AlertDialog(
            onDismissRequest = {
                showDropZoneDialog = false
                cardToDrop = null
            },
            title = { Text("Move ${cardToDrop?.card?.name} to:") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            cardToDrop?.let { viewModel.moveCard(it.instanceId, Zone.BATTLEFIELD) }
                            showDropZoneDialog = false
                            cardToDrop = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Battlefield")
                    }
                    Button(
                        onClick = {
                            cardToDrop?.let { viewModel.moveCard(it.instanceId, Zone.GRAVEYARD) }
                            showDropZoneDialog = false
                            cardToDrop = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Graveyard")
                    }
                    Button(
                        onClick = {
                            cardToDrop?.let { viewModel.moveCard(it.instanceId, Zone.EXILE) }
                            showDropZoneDialog = false
                            cardToDrop = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Exile")
                    }
                    Button(
                        onClick = {
                            cardToDrop?.let { viewModel.moveCard(it.instanceId, Zone.LIBRARY) }
                            showDropZoneDialog = false
                            cardToDrop = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Top of Library")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDropZoneDialog = false
                    cardToDrop = null
                }) {
                    Text("Cancel")
                }
            }
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
    var showCommandZoneDialog by remember { mutableStateOf(false) }
    var showTokenCreationDialog by remember { mutableStateOf(false) }

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
                            color = Color.White
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
                        onClick = if (isActivePlayer) ({ showLibrarySearchDialog = true }) else null
                    )
                    ZoneCard(
                        "Graveyard",
                        Zone.GRAVEYARD,
                        graveyardCount,
                        Modifier.fillMaxWidth().height(50.dp),
                        onClick = if (isActivePlayer) ({ showGraveyardDialog = true }) else null
                    )
                    ZoneCard(
                        "Exile",
                        Zone.EXILE,
                        exileCount,
                        Modifier.fillMaxWidth().height(50.dp),
                        onClick = if (isActivePlayer) ({ showExileDialog = true }) else null
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
                dragDropState = dragDropState,
                selectionState = if (isActivePlayer) selectionState else null,
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
                onShuffle = { viewModel.shuffleLibrary(player.id) }
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
                onDismiss = { showTokenCreationDialog = false },
                onCreateToken = { tokenName, tokenType, power, toughness, color, imageUri, quantity ->
                    viewModel.createToken(player.id, tokenName, tokenType, power, toughness, color, imageUri, quantity)
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
                                selectionState = selectionState
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
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectionState.selectionCount} selected",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(120.dp)
                    )
                    Button(
                        onClick = {
                            selectionState.selectedCards.forEach { cardId ->
                                handCards.find { it.instanceId == cardId }?.let {
                                    onCardAction(CardAction.ToBattlefield(it))
                                }
                            }
                            selectionState.clearSelection()
                        },
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("To Battlefield", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = {
                            selectionState.selectedCards.forEach { cardId ->
                                handCards.find { it.instanceId == cardId }?.let {
                                    onCardAction(CardAction.ToGraveyard(it))
                                }
                            }
                            selectionState.clearSelection()
                        },
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("To Graveyard", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = {
                            selectionState.selectedCards.forEach { cardId ->
                                handCards.find { it.instanceId == cardId }?.let {
                                    onCardAction(CardAction.ToExile(it))
                                }
                            }
                            selectionState.clearSelection()
                        },
                        modifier = Modifier.weight(1f).height(32.dp)
                    ) {
                        Text("To Exile", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { selectionState.clearSelection() },
                        modifier = Modifier.width(80.dp).height(32.dp)
                    ) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
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
            onDismiss = { showTokenCreationDialog = false },
            onCreateToken = { tokenName, tokenType, power, toughness, color, imageUri, quantity ->
                viewModel.createToken(player.id, tokenName, tokenType, power, toughness, color, imageUri, quantity)
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
                                    selectionState = selectionState
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
    onContextAction: (CardAction) -> Unit,
    dragDropState: DragDropState? = null,
    selectionState: SelectionState? = null
) {
    var lastClickTime by remember { mutableStateOf(0L) }
    val isDragging = dragDropState?.isDragging == true && dragDropState.draggedCard?.instanceId == cardInstance.instanceId
    val isSelected = selectionState?.isSelected(cardInstance.instanceId) == true

    CardWithContextMenu(
        cardInstance = cardInstance,
        onAction = onContextAction
    ) {
        Card(
            modifier = Modifier
                .width(60.dp)
                .height(84.dp)
                // Drag gesture support (always available if dragDropState exists)
                .then(
                    if (dragDropState != null) {
                        Modifier.pointerInput(cardInstance.instanceId) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragDropState.startDrag(cardInstance, offset)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragDropState.updateDragPosition(
                                        dragDropState.dragOffset + dragAmount
                                    )
                                },
                                onDragEnd = {
                                    dragDropState.endDrag()
                                },
                                onDragCancel = {
                                    dragDropState.endDrag()
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                // Click gesture support (always available)
                .clickable {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime < 300) {
                        // Double-click detected
                        selectionState?.clearSelection() // Clear selection on double-click
                        onDoubleClick()
                        lastClickTime = 0L
                    } else {
                        // Single click - toggle selection if selectionState exists
                        lastClickTime = currentTime
                        if (selectionState != null) {
                            selectionState.toggleSelection(cardInstance.instanceId)
                        } else {
                            onCardClick(cardInstance)
                        }
                    }
                }
                .then(
                    if (isDragging) Modifier.alpha(0.5f) else Modifier
                )
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

/**
 * Dialog for creating tokens
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TokenCreationDialog(
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
    var searchResults by remember { mutableStateOf<List<com.commandermtg.models.Card>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val colors = listOf("Colorless", "White", "Blue", "Black", "Red", "Green", "Multicolor")
    val coroutineScope = rememberCoroutineScope()
    val scryfallApi = remember { com.commandermtg.api.ScryfallApi() }

    // Cleanup on dismiss
    DisposableEffect(Unit) {
        onDispose {
            scryfallApi.close()
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
                        if (it.isNotBlank()) {
                            isSearching = true
                            coroutineScope.launch {
                                try {
                                    searchResults = scryfallApi.searchTokens(it)
                                } catch (e: Exception) {
                                    println("Search error: ${e.message}")
                                    searchResults = emptyList()
                                } finally {
                                    isSearching = false
                                }
                            }
                        } else {
                            searchResults = emptyList()
                        }
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
                                            searchResults = emptyList()
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
