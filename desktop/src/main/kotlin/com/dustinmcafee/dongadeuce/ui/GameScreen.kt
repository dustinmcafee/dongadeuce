package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.globalKeyEventHandler
import com.dustinmcafee.dongadeuce.models.Zone
import com.dustinmcafee.dongadeuce.viewmodel.GameViewModel

@Composable
fun GameScreen(
    hotseatDecks: Map<Int, com.dustinmcafee.dongadeuce.models.Deck> = emptyMap(),
    playerCount: Int = 2, // Total players (including local player): 2, 3, or 4
    isHotseatMode: Boolean = false,
    viewModel: GameViewModel = remember { GameViewModel() },
    // Network mode parameters
    isPaused: Boolean = false,
    pauseReason: String? = null,
    isHost: Boolean = false,
    onResumeGame: () -> Unit = {},
    onReturnToMenu: () -> Unit = {}
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
    var showPowerToughnessDialog by remember { mutableStateOf(false) }
    var cardForPowerToughnessDialog by remember { mutableStateOf<com.dustinmcafee.dongadeuce.models.CardInstance?>(null) }
    var showAnnotationDialog by remember { mutableStateOf(false) }
    var cardForAnnotationDialog by remember { mutableStateOf<com.dustinmcafee.dongadeuce.models.CardInstance?>(null) }
    var showDieRollerDialog by remember { mutableStateOf(false) }
    var showSetLifeDialog by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var showPlayerCountersDialog by remember { mutableStateOf(false) }
    // Zone viewing dialogs
    var showLibraryDialog by remember { mutableStateOf(false) }
    var showGraveyardDialog by remember { mutableStateOf(false) }
    var showExileDialog by remember { mutableStateOf(false) }
    var showCommandZoneDialog by remember { mutableStateOf(false) }
    var showPeekTopDialog by remember { mutableStateOf(false) }
    var showPeekBottomDialog by remember { mutableStateOf(false) }
    var peekCount by remember { mutableStateOf(5) } // Default peek count
    // Number input dialog state
    var showNumberInputDialog by remember { mutableStateOf(false) }
    var numberInputTitle by remember { mutableStateOf("") }
    var numberInputDefault by remember { mutableStateOf(1) }
    var numberInputCallback by remember { mutableStateOf<((Int) -> Unit)?>(null) }
    // Stack until found dialog state
    var showStackUntilFoundDialog by remember { mutableStateOf(false) }
    var stackUntilFoundResults by remember { mutableStateOf<List<com.dustinmcafee.dongadeuce.models.CardInstance>>(emptyList()) }
    var stackUntilFoundMatch by remember { mutableStateOf<com.dustinmcafee.dongadeuce.models.CardInstance?>(null) }

    // Keyboard shortcut handler
    val keyboardState = rememberKeyboardShortcutState(viewModel, selectionState)

    // Register global key handler for window-level keyboard events
    DisposableEffect(keyboardState) {
        globalKeyEventHandler = { event ->
            keyboardState.handleKeyEvent(event)
        }
        onDispose {
            globalKeyEventHandler = null
        }
    }

    // Check if any dialog is currently open
    val isAnyDialogOpen = cardDetailsToShow != null ||
            showLibraryPositionDialog ||
            showCounterDialog ||
            showPowerToughnessDialog ||
            showAnnotationDialog ||
            showDieRollerDialog ||
            showSetLifeDialog ||
            showTokenDialog ||
            showPlayerCountersDialog ||
            showLibraryDialog ||
            showGraveyardDialog ||
            showExileDialog ||
            showCommandZoneDialog ||
            showPeekTopDialog ||
            showPeekBottomDialog ||
            showNumberInputDialog ||
            showStackUntilFoundDialog

    // Update keyboard state with dialog status
    LaunchedEffect(isAnyDialogOpen) {
        keyboardState.isDialogOpen = isAnyDialogOpen
    }

    // Setup keyboard shortcut callbacks
    LaunchedEffect(Unit) {
        keyboardState.onShowDieRollerDialog = { showDieRollerDialog = true }
        keyboardState.onShowSetLifeDialog = { showSetLifeDialog = true }
        keyboardState.onShowTokenDialog = { showTokenDialog = true }
        keyboardState.onShowPlayerCountersDialog = { showPlayerCountersDialog = true }
        keyboardState.onShowPowerToughnessDialog = { card ->
            cardForPowerToughnessDialog = card
            showPowerToughnessDialog = true
        }
        keyboardState.onShowAnnotationDialog = { card ->
            cardForAnnotationDialog = card
            showAnnotationDialog = true
        }
        // Zone viewing dialog callbacks
        keyboardState.onShowLibraryDialog = { showLibraryDialog = true }
        keyboardState.onShowGraveyardDialog = { showGraveyardDialog = true }
        keyboardState.onShowExileDialog = { showExileDialog = true }
        keyboardState.onShowCommandZoneDialog = { showCommandZoneDialog = true }
        keyboardState.onShowPeekTopDialog = { showPeekTopDialog = true }
        keyboardState.onShowPeekBottomDialog = { showPeekBottomDialog = true }
        keyboardState.onShowNumberInputDialog = { title, default, callback ->
            numberInputTitle = title
            numberInputDefault = default
            numberInputCallback = callback
            showNumberInputDialog = true
        }
        keyboardState.onShowStackUntilFoundDialog = {
            stackUntilFoundResults = emptyList()
            stackUntilFoundMatch = null
            showStackUntilFoundDialog = true
        }
        keyboardState.onCloseDialog = {
            cardDetailsToShow = null
            showLibraryPositionDialog = false
            showCounterDialog = false
            showPowerToughnessDialog = false
            showAnnotationDialog = false
            showDieRollerDialog = false
            showSetLifeDialog = false
            showTokenDialog = false
            showPlayerCountersDialog = false
            showLibraryDialog = false
            showGraveyardDialog = false
            showExileDialog = false
            showCommandZoneDialog = false
            showPeekTopDialog = false
            showPeekBottomDialog = false
            showNumberInputDialog = false
            showStackUntilFoundDialog = false
        }
        keyboardState.onFocusChat = {
            // Focus the chat input - would need a reference to the chat input field
            // This would require passing focusRequester through the compose hierarchy
        }
        keyboardState.onLeaveGame = {
            onReturnToMenu()
        }
    }

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
            is CardAction.ShowPowerToughnessDialog -> {
                // Show dialog for modifying power/toughness
                cardForPowerToughnessDialog = action.cardInstance
                showPowerToughnessDialog = true
            }
            is CardAction.SetAnnotation -> {
                // Show dialog for setting annotation
                cardForAnnotationDialog = action.cardInstance
                showAnnotationDialog = true
            }
            is CardAction.CreateCopy -> {
                // Create a copy of the card
                viewModel.cloneCard(
                    cardId = action.cardInstance.instanceId,
                    newOwnerId = action.ownerId
                )
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

    // Initialize game for hotseat mode only
    // Network mode: game state comes from the server via GameViewModel's network client
    LaunchedEffect(Unit) {
        if (isHotseatMode && uiState.localPlayer == null) {
            val playerNames = List(playerCount) { index -> "Player ${index + 1}" }
            viewModel.initializeGame(
                localPlayerName = playerNames[0],
                opponentNames = playerNames.drop(1),
                isHotseatMode = true
            )
        }
    }

    // Load decks for hotseat mode only
    // Network mode: decks are loaded on the server when the game starts
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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
                val gameState = uiState.gameState
                val allPlayers = gameState?.players ?: emptyList()
                val activePlayerId = gameState?.activePlayer?.id

                if (isHotseatMode) {
                    // Hotseat mode: Rotate so active player is always at bottom
                    val activePlayerIndex = gameState?.activePlayerIndex ?: 0
                    val rotatedPlayers = allPlayers.drop(activePlayerIndex) + allPlayers.take(activePlayerIndex)

                    // Use key to force recomposition when active player changes
                    key(activePlayerId) {
                        Column(modifier = Modifier.fillMaxSize()) {
                        when (rotatedPlayers.size) {
                            2 -> {
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
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            4 -> {
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
                    // Network mode: Fixed layout - local player always at bottom, opponents at top
                    val localPlayer = uiState.localPlayer
                    val opponents = uiState.opponents

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Opponents at top
                        when (opponents.size) {
                            1 -> {
                                HotseatPlayerSection(
                                    player = opponents[0],
                                    viewModel = viewModel,
                                    isActivePlayer = opponents[0].id == activePlayerId,
                                    onCardAction = handleAction,
                                    dragDropState = null, // Opponents can't drag
                                    selectionState = null,
                                    otherPlayers = allPlayers.filter { it.id != opponents[0].id },
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    isLocalPlayer = false
                                )
                            }
                            2 -> {
                                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                    opponents.forEach { opponent ->
                                        HotseatPlayerSection(
                                            player = opponent,
                                            viewModel = viewModel,
                                            isActivePlayer = opponent.id == activePlayerId,
                                            onCardAction = handleAction,
                                            dragDropState = null,
                                            selectionState = null,
                                            otherPlayers = allPlayers.filter { it.id != opponent.id },
                                            modifier = Modifier.weight(1f),
                                            isLocalPlayer = false
                                        )
                                    }
                                }
                            }
                            3 -> {
                                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                    opponents.forEach { opponent ->
                                        HotseatPlayerSection(
                                            player = opponent,
                                            viewModel = viewModel,
                                            isActivePlayer = opponent.id == activePlayerId,
                                            onCardAction = handleAction,
                                            dragDropState = null,
                                            selectionState = null,
                                            otherPlayers = allPlayers.filter { it.id != opponent.id },
                                            modifier = Modifier.weight(1f),
                                            isLocalPlayer = false
                                        )
                                    }
                                }
                            }
                        }

                        // Local player at bottom
                        if (localPlayer != null) {
                            HotseatPlayerSection(
                                player = localPlayer,
                                viewModel = viewModel,
                                isActivePlayer = localPlayer.id == activePlayerId,
                                onCardAction = handleAction,
                                dragDropState = dragDropState,
                                selectionState = selectionState,
                                otherPlayers = opponents,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                inverted = true,
                                isLocalPlayer = true
                            )
                        }
                    }
                }
        }

        // Right sidebar with Turn indicator and Game Log
        val gameState = uiState.gameState
        if (gameState != null) {
            Column(
                modifier = Modifier.width(280.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Turn indicator at top
                TurnIndicator(
                    activePlayer = gameState.activePlayer,
                    currentPhase = gameState.phase,
                    turnNumber = gameState.turnNumber,
                    onNextPhase = { viewModel.nextPhase() },
                    onPassTurn = { viewModel.passTurn() },
                    onUntapAll = { viewModel.untapAll(gameState.activePlayer.id) },
                    onRollDice = { showDieRollerDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )

                // Game log panel fills remaining space
                GameLogPanel(
                    gameLog = gameState.gameLog,
                    players = gameState.players,
                    onSendMessage = { message ->
                        viewModel.sendChatMessage(gameState.activePlayer.id, message)
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
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

    // Power/Toughness dialog
    if (showPowerToughnessDialog && cardForPowerToughnessDialog != null) {
        val card = cardForPowerToughnessDialog!!

        PowerToughnessDialog(
            cardName = card.card.name,
            basePower = card.card.power,
            baseToughness = card.card.toughness,
            currentPowerMod = card.powerModifier,
            currentToughnessMod = card.toughnessModifier,
            onDismiss = {
                showPowerToughnessDialog = false
                cardForPowerToughnessDialog = null
            },
            onModifyPower = { amount ->
                viewModel.modifyPower(card.instanceId, amount)
            },
            onModifyToughness = { amount ->
                viewModel.modifyToughness(card.instanceId, amount)
            },
            onModifyBoth = { amount ->
                viewModel.modifyPowerToughness(card.instanceId, amount)
            },
            onSetPT = { newPower, newToughness ->
                viewModel.setPowerToughness(card.instanceId, newPower, newToughness)
            },
            onReset = {
                viewModel.resetPowerToughness(card.instanceId)
            },
            onFlowP = {
                viewModel.flowPower(card.instanceId)
            },
            onFlowT = {
                viewModel.flowToughness(card.instanceId)
            }
        )
    }

    // Annotation dialog
    if (showAnnotationDialog && cardForAnnotationDialog != null) {
        val card = cardForAnnotationDialog!!

        AnnotationDialog(
            cardName = card.card.name,
            currentAnnotation = card.annotation,
            onDismiss = {
                showAnnotationDialog = false
                cardForAnnotationDialog = null
            },
            onConfirm = { annotation ->
                viewModel.setAnnotation(card.instanceId, annotation)
            }
        )
    }

    // Die roller dialog
    if (showDieRollerDialog) {
        val activePlayer = uiState.gameState?.activePlayer
        val playerName = activePlayer?.name ?: "Player"
        DieRollerDialog(
            playerName = playerName,
            onDismiss = { showDieRollerDialog = false },
            onRollLogged = { dieType, result, numberOfDice ->
                activePlayer?.let {
                    viewModel.logDieRoll(it.id, dieType, result, numberOfDice)
                }
            }
        )
    }

    // Set life dialog
    if (showSetLifeDialog) {
        val activePlayer = uiState.gameState?.activePlayer
        if (activePlayer != null) {
            SetLifeDialog(
                playerName = activePlayer.name,
                currentLife = activePlayer.life,
                onDismiss = { showSetLifeDialog = false },
                onConfirm = { newLife ->
                    viewModel.updateLife(activePlayer.id, newLife)
                    showSetLifeDialog = false
                }
            )
        }
    }

    // Token creation dialog
    if (showTokenDialog) {
        val activePlayer = uiState.gameState?.activePlayer
        if (activePlayer != null) {
            TokenCreationDialog(
                viewModel = viewModel,
                onDismiss = { showTokenDialog = false },
                onCreateToken = { tokenName, tokenType, power, toughness, color, imageUri, quantity ->
                    viewModel.createToken(
                        playerId = activePlayer.id,
                        tokenName = tokenName,
                        tokenType = tokenType,
                        power = power,
                        toughness = toughness,
                        color = color,
                        imageUri = imageUri,
                        quantity = quantity
                    )
                    showTokenDialog = false
                }
            )
        }
    }

    // Player counters dialog
    if (showPlayerCountersDialog) {
        val activePlayer = uiState.gameState?.activePlayer
        if (activePlayer != null) {
            PlayerCountersDialog(
                player = activePlayer,
                onDismiss = { showPlayerCountersDialog = false },
                onAddCounter = { counterType, amount ->
                    viewModel.addPlayerCounter(activePlayer.id, counterType, amount)
                },
                onRemoveCounter = { counterType, amount ->
                    viewModel.removePlayerCounter(activePlayer.id, counterType, amount)
                },
                onSetCounter = { counterType, amount ->
                    viewModel.setPlayerCounter(activePlayer.id, counterType, amount)
                }
            )
        }
    }

    // Zone viewing dialogs - show for active player
    val activePlayer = uiState.gameState?.activePlayer
    val activePlayerId = activePlayer?.id

    // Library search dialog
    if (showLibraryDialog && activePlayerId != null) {
        val libraryCards = uiState.gameState?.cardInstances?.filter {
            it.ownerId == activePlayerId && it.zone == Zone.LIBRARY
        } ?: emptyList()

        LibrarySearchDialog(
            cards = libraryCards,
            playerName = activePlayer.name,
            onDismiss = { showLibraryDialog = false },
            onToHand = { card -> viewModel.moveCard(card.instanceId, Zone.HAND) },
            onToBattlefield = { card -> viewModel.moveCard(card.instanceId, Zone.BATTLEFIELD) },
            onToTop = { card -> viewModel.moveCardToTopOfLibrary(card.instanceId) },
            onToBottom = { card -> viewModel.moveCardToBottomOfLibrary(card.instanceId) },
            onShuffle = { viewModel.shuffleLibrary(activePlayerId) }
        )
    }

    // Graveyard dialog
    if (showGraveyardDialog && activePlayerId != null) {
        val graveyardCards = uiState.gameState?.cardInstances?.filter {
            it.ownerId == activePlayerId && it.zone == Zone.GRAVEYARD
        } ?: emptyList()

        GraveyardDialog(
            cards = graveyardCards,
            playerName = activePlayer.name,
            onDismiss = { showGraveyardDialog = false },
            onReturnToHand = { card -> viewModel.moveCard(card.instanceId, Zone.HAND) },
            onReturnToBattlefield = { card -> viewModel.moveCard(card.instanceId, Zone.BATTLEFIELD) }
        )
    }

    // Exile zone dialog
    if (showExileDialog && activePlayerId != null) {
        val exileCards = uiState.gameState?.cardInstances?.filter {
            it.ownerId == activePlayerId && it.zone == Zone.EXILE
        } ?: emptyList()

        ExileDialog(
            cards = exileCards,
            playerName = activePlayer.name,
            onDismiss = { showExileDialog = false },
            onReturnToHand = { card -> viewModel.moveCard(card.instanceId, Zone.HAND) },
            onReturnToBattlefield = { card -> viewModel.moveCard(card.instanceId, Zone.BATTLEFIELD) }
        )
    }

    // Command zone dialog
    if (showCommandZoneDialog && activePlayerId != null) {
        val commandZoneCards = uiState.gameState?.cardInstances?.filter {
            it.ownerId == activePlayerId && it.zone == Zone.COMMAND_ZONE
        } ?: emptyList()

        CommandZoneDialog(
            cards = commandZoneCards,
            playerName = activePlayer.name,
            onDismiss = { showCommandZoneDialog = false },
            onCastToBattlefield = { card -> viewModel.moveCard(card.instanceId, Zone.BATTLEFIELD) },
            onToHand = { card -> viewModel.moveCard(card.instanceId, Zone.HAND) }
        )
    }

    // Library peek top dialog
    if (showPeekTopDialog && activePlayerId != null) {
        val libraryCards = uiState.gameState?.cardInstances?.filter {
            it.ownerId == activePlayerId && it.zone == Zone.LIBRARY
        } ?: emptyList()
        val topCards = libraryCards.take(peekCount)

        LibraryPeekDialog(
            cards = topCards,
            playerName = activePlayer.name,
            peekLocation = PeekLocation.TOP,
            onDismiss = { showPeekTopDialog = false },
            onMoveCard = { card, zone -> viewModel.moveCard(card.instanceId, zone) },
            onMoveAllToZone = { zone ->
                topCards.forEach { card -> viewModel.moveCard(card.instanceId, zone) }
            },
            onShuffleCards = { viewModel.shuffleTopCards(activePlayerId, peekCount) }
        )
    }

    // Library peek bottom dialog
    if (showPeekBottomDialog && activePlayerId != null) {
        val libraryCards = uiState.gameState?.cardInstances?.filter {
            it.ownerId == activePlayerId && it.zone == Zone.LIBRARY
        } ?: emptyList()
        val bottomCards = libraryCards.takeLast(peekCount)

        LibraryPeekDialog(
            cards = bottomCards,
            playerName = activePlayer.name,
            peekLocation = PeekLocation.BOTTOM,
            onDismiss = { showPeekBottomDialog = false },
            onMoveCard = { card, zone -> viewModel.moveCard(card.instanceId, zone) },
            onMoveAllToZone = { zone ->
                bottomCards.forEach { card -> viewModel.moveCard(card.instanceId, zone) }
            },
            onShuffleCards = { viewModel.shuffleBottomCards(activePlayerId, peekCount) }
        )
    }

    // Number input dialog (for draw/mill/shuffle multiple)
    if (showNumberInputDialog) {
        var inputValue by remember(numberInputDefault) { mutableStateOf(numberInputDefault.toString()) }
        AlertDialog(
            onDismissRequest = {
                showNumberInputDialog = false
                numberInputCallback = null
            },
            title = { Text(numberInputTitle) },
            text = {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            inputValue = newValue
                        }
                    },
                    label = { Text("Count") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = inputValue.toIntOrNull() ?: numberInputDefault
                        if (count > 0) {
                            numberInputCallback?.invoke(count)
                        }
                        showNumberInputDialog = false
                        numberInputCallback = null
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNumberInputDialog = false
                        numberInputCallback = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Stack Until Found dialog
    if (showStackUntilFoundDialog && activePlayerId != null) {
        var searchText by remember { mutableStateOf("") }
        var hasSearched by remember { mutableStateOf(false) }

        if (!hasSearched) {
            // Stage 1: Enter search term
            AlertDialog(
                onDismissRequest = { showStackUntilFoundDialog = false },
                title = { Text("Stack Until Found") },
                text = {
                    Column {
                        Text(
                            "Enter a card name or type to search for. Cards will be revealed from the top of your library until a match is found.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            label = { Text("Search (name or type)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (searchText.isNotBlank()) {
                                // Perform search
                                val libraryCards = uiState.gameState?.cardInstances?.filter {
                                    it.ownerId == activePlayerId && it.zone == Zone.LIBRARY
                                }?.reversed() ?: emptyList() // reversed so top is first

                                val searchLower = searchText.lowercase()
                                val revealed = mutableListOf<com.dustinmcafee.dongadeuce.models.CardInstance>()
                                var matchFound: com.dustinmcafee.dongadeuce.models.CardInstance? = null

                                for (card in libraryCards) {
                                    val nameMatches = card.card.name.lowercase().contains(searchLower)
                                    val typeMatches = card.card.type?.lowercase()?.contains(searchLower) == true
                                    if (nameMatches || typeMatches) {
                                        matchFound = card
                                        break
                                    } else {
                                        revealed.add(card)
                                    }
                                }

                                stackUntilFoundResults = revealed
                                stackUntilFoundMatch = matchFound
                                hasSearched = true
                            }
                        },
                        enabled = searchText.isNotBlank()
                    ) {
                        Text("Search")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStackUntilFoundDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        } else {
            // Stage 2: Show results
            AlertDialog(
                onDismissRequest = { showStackUntilFoundDialog = false },
                title = { Text("Stack Until Found - Results") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        if (stackUntilFoundMatch != null) {
                            Text(
                                "Found: ${stackUntilFoundMatch?.card?.name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } else {
                            Text(
                                "No match found in library",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        if (stackUntilFoundResults.isNotEmpty()) {
                            Text(
                                "Revealed ${stackUntilFoundResults.size} card(s):",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            stackUntilFoundResults.forEach { card ->
                                Text(
                                    "• ${card.card.name}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else if (stackUntilFoundMatch != null) {
                            Text(
                                "Match was on top of library (no cards revealed)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Choose where to put the revealed cards:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                // Put revealed cards to graveyard
                                stackUntilFoundResults.forEach { card ->
                                    viewModel.moveCard(card.instanceId, Zone.GRAVEYARD)
                                }
                                showStackUntilFoundDialog = false
                            }
                        ) {
                            Text("To Graveyard")
                        }
                        Button(
                            onClick = {
                                // Put revealed cards on bottom of library (in order)
                                stackUntilFoundResults.forEach { card ->
                                    viewModel.moveCardToBottomOfLibrary(card.instanceId)
                                }
                                showStackUntilFoundDialog = false
                            }
                        ) {
                            Text("To Bottom")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStackUntilFoundDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
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

    // Network game paused dialog
    if (isPaused) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss while paused */ },
            title = { Text("Game Paused") },
            text = {
                Column {
                    Text(
                        pauseReason ?: "A player has disconnected.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isHost) {
                            "Waiting for player to reconnect. You can kick the player and resume, or return to menu."
                        } else {
                            "Waiting for the host to resolve the situation..."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                if (isHost) {
                    TextButton(onClick = onResumeGame) {
                        Text("Resume Game")
                    }
                }
            },
            dismissButton = {
                if (isHost) {
                    TextButton(onClick = onReturnToMenu) {
                        Text("Return to Menu")
                    }
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

