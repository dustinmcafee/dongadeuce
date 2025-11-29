@file:OptIn(ExperimentalMaterial3Api::class)

package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.ui.UIConstants
import com.dustinmcafee.dongadeuce.viewmodel.AndroidMenuViewModel
import com.dustinmcafee.dongadeuce.viewmodel.GameViewModel
import com.dustinmcafee.dongadeuce.viewmodel.MenuUiState

/**
 * Full-featured game screen for Android with feature parity to desktop
 */
@Composable
fun AndroidGameScreen(
    menuViewModel: AndroidMenuViewModel,
    uiState: MenuUiState,
    gameViewModel: GameViewModel = remember { GameViewModel() }
) {
    val gameUiState by gameViewModel.uiState.collectAsState()
    val selectionState = rememberSelectionState()
    val keyboardState = rememberKeyboardShortcutState(gameViewModel, selectionState)

    // Dialog states
    var cardDetailsToShow by remember { mutableStateOf<CardInstance?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuCard by remember { mutableStateOf<CardInstance?>(null) }
    var showLibraryPositionDialog by remember { mutableStateOf(false) }
    var cardForLibraryPosition by remember { mutableStateOf<CardInstance?>(null) }
    var showCounterDialog by remember { mutableStateOf(false) }
    var cardForCounterDialog by remember { mutableStateOf<CardInstance?>(null) }
    var counterTypeForDialog by remember { mutableStateOf("") }
    var showPowerToughnessDialog by remember { mutableStateOf(false) }
    var cardForPowerToughnessDialog by remember { mutableStateOf<CardInstance?>(null) }
    var showAnnotationDialog by remember { mutableStateOf(false) }
    var cardForAnnotationDialog by remember { mutableStateOf<CardInstance?>(null) }
    var showDieRollerDialog by remember { mutableStateOf(false) }
    var showSetLifeDialog by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var showPlayerCountersDialog by remember { mutableStateOf(false) }
    var showLibraryDialog by remember { mutableStateOf(false) }
    var showGraveyardDialog by remember { mutableStateOf(false) }
    var showExileDialog by remember { mutableStateOf(false) }
    var showGameLogDialog by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }

    // Setup keyboard shortcut callbacks and initialize game
    LaunchedEffect(Unit) {
        // Setup keyboard callbacks first
        keyboardState.onShowDieRollerDialog = { showDieRollerDialog = true }
        keyboardState.onShowSetLifeDialog = { showSetLifeDialog = true }
        keyboardState.onShowTokenDialog = { showTokenDialog = true }
        keyboardState.onShowPlayerCountersDialog = { showPlayerCountersDialog = true }
        keyboardState.onShowLibraryDialog = { showLibraryDialog = true }
        keyboardState.onShowGraveyardDialog = { showGraveyardDialog = true }
        keyboardState.onShowExileDialog = { showExileDialog = true }
        keyboardState.onShowPowerToughnessDialog = { card ->
            cardForPowerToughnessDialog = card
            showPowerToughnessDialog = true
        }
        keyboardState.onShowAnnotationDialog = { card ->
            cardForAnnotationDialog = card
            showAnnotationDialog = true
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
            showGameLogDialog = false
        }
        keyboardState.onLeaveGame = { menuViewModel.returnToMenu() }

        // Then initialize game for hotseat mode
        if (uiState.hotseatMode && gameUiState.localPlayer == null) {
            val playerNames = List(uiState.playerCount) { "Player ${it + 1}" }
            gameViewModel.initializeGame(
                localPlayerName = playerNames[0],
                opponentNames = playerNames.drop(1),
                isHotseatMode = true
            )
        }
    }

    // Load decks for hotseat mode
    LaunchedEffect(uiState.hotseatDecks, gameUiState.gameState, gameUiState.allPlayers) {
        if (uiState.hotseatMode && uiState.hotseatDecks.isNotEmpty() && gameUiState.gameState != null) {
            val allPlayers = gameUiState.allPlayers
            uiState.hotseatDecks.forEach { (playerIndex, deck) ->
                if (playerIndex < allPlayers.size) {
                    val player = allPlayers[playerIndex]
                    val libraryCount = gameViewModel.getCardCount(player.id, Zone.LIBRARY)
                    val handCount = gameViewModel.getCardCount(player.id, Zone.HAND)
                    if (libraryCount == 0 && handCount == 0) {
                        gameViewModel.loadDeckForPlayer(player.id, deck)
                        gameViewModel.drawStartingHand(player.id, 7)
                    }
                }
            }
        }
    }

    // Handle card actions
    val handleAction: (CardAction) -> Unit = { action ->
        when (action) {
            is CardAction.ViewDetails -> cardDetailsToShow = action.cardInstance
            is CardAction.ShowLibraryPositionDialog -> {
                cardForLibraryPosition = action.cardInstance
                showLibraryPositionDialog = true
            }
            is CardAction.ShowCounterDialog -> {
                cardForCounterDialog = action.cardInstance
                counterTypeForDialog = action.counterType
                showCounterDialog = true
            }
            is CardAction.ShowPowerToughnessDialog -> {
                cardForPowerToughnessDialog = action.cardInstance
                showPowerToughnessDialog = true
            }
            is CardAction.SetAnnotation -> {
                cardForAnnotationDialog = action.cardInstance
                showAnnotationDialog = true
            }
            is CardAction.CreateCopy -> {
                gameViewModel.cloneCard(action.cardInstance.instanceId, action.ownerId)
            }
            else -> {
                gameViewModel.handleBatchCardAction(
                    action = action,
                    selectedCardIds = selectionState.selectedCards.toSet()
                )
            }
        }
    }

    val gameState = gameUiState.gameState
    val allPlayers = gameState?.players ?: emptyList()
    val activePlayer = gameState?.activePlayer
    val localPlayer = if (uiState.hotseatMode) activePlayer else gameUiState.localPlayer

    Box(
        modifier = Modifier
            .fillMaxSize()
            .keyboardShortcuts(keyboardState)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with game info
            TopGameBar(
                activePlayer = activePlayer,
                currentPhase = gameState?.phase,
                turnNumber = gameState?.turnNumber ?: 0,
                isHotseatMode = uiState.hotseatMode,
                onShowGameLog = { showGameLogDialog = true },
                onShowActions = { showActionsMenu = true },
                onReturnToMenu = { menuViewModel.returnToMenu() }
            )

            // Main game area
            if (gameState != null && allPlayers.isNotEmpty()) {
                Column(modifier = Modifier.weight(1f)) {
                    // Opponents at top (scrollable horizontally for 3+ players)
                    val opponents = if (uiState.hotseatMode) {
                        val activeIndex = gameState.activePlayerIndex
                        val rotated = allPlayers.drop(activeIndex) + allPlayers.take(activeIndex)
                        rotated.drop(1) // Everyone except active player
                    } else {
                        gameUiState.opponents
                    }

                    if (opponents.isNotEmpty()) {
                        OpponentSection(
                            opponents = opponents,
                            gameViewModel = gameViewModel,
                            activePlayerId = activePlayer?.id,
                            onCardAction = handleAction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.35f)
                        )
                    }

                    // Local player section (bottom)
                    localPlayer?.let { player ->
                        LocalPlayerSection(
                            player = player,
                            gameViewModel = gameViewModel,
                            isActivePlayer = player.id == activePlayer?.id,
                            onCardAction = handleAction,
                            selectionState = selectionState,
                            otherPlayers = opponents,
                            onShowContextMenu = { card ->
                                contextMenuCard = card
                                showContextMenu = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.65f)
                        )
                    }
                }
            } else {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Bottom action bar
            BottomActionBar(
                activePlayer = activePlayer,
                localPlayer = localPlayer,
                gameViewModel = gameViewModel,
                onShowDieRoller = { showDieRollerDialog = true },
                onShowSetLife = { showSetLifeDialog = true },
                onShowTokenCreation = { showTokenDialog = true },
                onShowPlayerCounters = { showPlayerCountersDialog = true },
                onShowLibrary = { showLibraryDialog = true },
                onShowGraveyard = { showGraveyardDialog = true },
                onShowExile = { showExileDialog = true }
            )
        }

        // Paused overlay
        if (uiState.isPaused) {
            PausedOverlay(
                pauseReason = uiState.pauseReason,
                isHost = menuViewModel.isHost(),
                onResume = { menuViewModel.resumeGame() },
                onReturnToMenu = { menuViewModel.returnToMenu() }
            )
        }
    }

    // Context menu bottom sheet
    contextMenuCard?.let { card ->
        if (showContextMenu) {
            CardContextMenuBottomSheet(
                cardInstance = card,
                otherPlayers = allPlayers.filter { it.id != localPlayer?.id },
                onAction = handleAction,
                onDismiss = {
                    showContextMenu = false
                    contextMenuCard = null
                }
            )
        }
    }

    // Dialogs
    cardDetailsToShow?.let { card ->
        CardDetailsDialog(cardInstance = card, onDismiss = { cardDetailsToShow = null })
    }

    cardForLibraryPosition?.let { card ->
        if (showLibraryPositionDialog) {
            val librarySize = gameViewModel.getCardCount(card.ownerId, Zone.LIBRARY)
            LibraryPositionDialog(
                cardName = card.card.name,
                librarySize = librarySize,
                onDismiss = { showLibraryPositionDialog = false; cardForLibraryPosition = null },
                onToTop = { gameViewModel.moveCardToTopOfLibrary(card.instanceId) },
                onToBottom = { gameViewModel.moveCardToBottomOfLibrary(card.instanceId) },
                onToPositionFromTop = { pos -> gameViewModel.moveCardToLibraryPosition(card.instanceId, pos) },
                onToPositionFromBottom = { pos -> gameViewModel.moveCardToLibraryPositionFromBottom(card.instanceId, pos) }
            )
        }
    }

    cardForCounterDialog?.let { card ->
        if (showCounterDialog) {
            val currentCount = card.counters[counterTypeForDialog] ?: 0
            CounterDialog(
                cardName = card.card.name,
                counterType = counterTypeForDialog,
                currentCount = currentCount,
                onDismiss = { showCounterDialog = false; cardForCounterDialog = null },
                onSet = { amount -> gameViewModel.setCounter(card.instanceId, counterTypeForDialog, amount) },
                onAdd = { amount -> gameViewModel.addCounter(card.instanceId, counterTypeForDialog, amount) },
                onSubtract = { amount -> gameViewModel.removeCounter(card.instanceId, counterTypeForDialog, amount) }
            )
        }
    }

    cardForPowerToughnessDialog?.let { card ->
        if (showPowerToughnessDialog) {
            PowerToughnessDialog(
                cardName = card.card.name,
                basePower = card.card.power,
                baseToughness = card.card.toughness,
                currentPowerMod = card.powerModifier,
                currentToughnessMod = card.toughnessModifier,
                onDismiss = { showPowerToughnessDialog = false; cardForPowerToughnessDialog = null },
                onModifyPower = { amount -> gameViewModel.modifyPower(card.instanceId, amount) },
                onModifyToughness = { amount -> gameViewModel.modifyToughness(card.instanceId, amount) },
                onModifyBoth = { amount -> gameViewModel.modifyPowerToughness(card.instanceId, amount) },
                onSetPT = { p, t -> if (p != null && t != null) gameViewModel.setPowerToughness(card.instanceId, p, t) },
                onReset = { gameViewModel.resetPowerToughness(card.instanceId) },
                onFlowP = { gameViewModel.flowPower(card.instanceId) },
                onFlowT = { gameViewModel.flowToughness(card.instanceId) }
            )
        }
    }

    cardForAnnotationDialog?.let { card ->
        if (showAnnotationDialog) {
            AnnotationDialog(
                cardName = card.card.name,
                currentAnnotation = card.annotation ?: "",
                onDismiss = { showAnnotationDialog = false; cardForAnnotationDialog = null },
                onConfirm = { annotation -> gameViewModel.setAnnotation(card.instanceId, annotation) }
            )
        }
    }

    if (showDieRollerDialog) {
        DieRollerDialog(
            playerName = activePlayer?.name ?: "Player",
            onDismiss = { showDieRollerDialog = false },
            onRollLogged = { dieType, result, numDice ->
                activePlayer?.let { gameViewModel.logDieRoll(it.id, dieType, result, numDice) }
            }
        )
    }

    if (showSetLifeDialog && activePlayer != null) {
        SetLifeDialog(
            playerName = activePlayer.name,
            currentLife = activePlayer.life,
            onDismiss = { showSetLifeDialog = false },
            onConfirm = { newLife -> gameViewModel.updateLife(activePlayer.id, newLife) }
        )
    }

    if (showTokenDialog && activePlayer != null) {
        TokenCreationDialog(
            viewModel = gameViewModel,
            onDismiss = { showTokenDialog = false },
            onCreateToken = { name, type, power, toughness, color, imageUri, quantity ->
                gameViewModel.createToken(activePlayer.id, name, type, power, toughness, color, imageUri, quantity)
                showTokenDialog = false
            }
        )
    }

    if (showPlayerCountersDialog && activePlayer != null) {
        PlayerCountersDialog(
            player = activePlayer,
            onDismiss = { showPlayerCountersDialog = false },
            onAddCounter = { type, amount -> gameViewModel.addPlayerCounter(activePlayer.id, type, amount) },
            onRemoveCounter = { type, amount -> gameViewModel.removePlayerCounter(activePlayer.id, type, amount) },
            onSetCounter = { type, amount -> gameViewModel.setPlayerCounter(activePlayer.id, type, amount) }
        )
    }

    if (showLibraryDialog && localPlayer != null) {
        val libraryCards = gameViewModel.getCards(localPlayer.id, Zone.LIBRARY)
        LibrarySearchDialog(
            cards = libraryCards,
            playerName = localPlayer.name,
            onDismiss = { showLibraryDialog = false },
            onToHand = { card -> gameViewModel.moveCard(card.instanceId, Zone.HAND) },
            onToBattlefield = { card -> gameViewModel.moveCard(card.instanceId, Zone.BATTLEFIELD) },
            onToTop = { card -> gameViewModel.moveCardToTopOfLibrary(card.instanceId) },
            onToBottom = { card -> gameViewModel.moveCardToBottomOfLibrary(card.instanceId) },
            onShuffle = { gameViewModel.shuffleLibrary(localPlayer.id) }
        )
    }

    if (showGraveyardDialog && localPlayer != null) {
        val graveyardCards = gameViewModel.getCards(localPlayer.id, Zone.GRAVEYARD)
        GraveyardDialog(
            cards = graveyardCards,
            playerName = localPlayer.name,
            onDismiss = { showGraveyardDialog = false },
            onReturnToHand = { card -> gameViewModel.moveCard(card.instanceId, Zone.HAND) },
            onReturnToBattlefield = { card -> gameViewModel.moveCard(card.instanceId, Zone.BATTLEFIELD) }
        )
    }

    if (showExileDialog && localPlayer != null) {
        val exileCards = gameViewModel.getCards(localPlayer.id, Zone.EXILE)
        ExileDialog(
            cards = exileCards,
            playerName = localPlayer.name,
            onDismiss = { showExileDialog = false },
            onReturnToHand = { card -> gameViewModel.moveCard(card.instanceId, Zone.HAND) },
            onReturnToBattlefield = { card -> gameViewModel.moveCard(card.instanceId, Zone.BATTLEFIELD) }
        )
    }

    if (showGameLogDialog && gameState != null) {
        GameLogDialog(
            gameLog = gameState.gameLog,
            players = allPlayers,
            onDismiss = { showGameLogDialog = false },
            onSendMessage = { message ->
                activePlayer?.let { gameViewModel.sendChatMessage(it.id, message) }
            }
        )
    }

    // Actions menu
    if (showActionsMenu && localPlayer != null) {
        ActionsMenuSheet(
            player = localPlayer,
            gameViewModel = gameViewModel,
            onDismiss = { showActionsMenu = false }
        )
    }
}

@Composable
private fun TopGameBar(
    activePlayer: Player?,
    currentPhase: GamePhase?,
    turnNumber: Int,
    isHotseatMode: Boolean,
    onShowGameLog: () -> Unit,
    onShowActions: () -> Unit,
    onReturnToMenu: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Turn info
            Column {
                Text(
                    "Turn $turnNumber",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    activePlayer?.name ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Phase indicator
            currentPhase?.let { phase ->
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(phase.displayName, fontSize = 12.sp) }
                )
            }

            // Action buttons
            Row {
                IconButton(onClick = onShowGameLog) {
                    Icon(Icons.Default.List, "Game Log")
                }
                IconButton(onClick = onShowActions) {
                    Icon(Icons.Default.Menu, "Actions")
                }
                IconButton(onClick = onReturnToMenu) {
                    Icon(Icons.Default.Close, "Leave")
                }
            }
        }
    }
}

@Composable
private fun OpponentSection(
    opponents: List<Player>,
    gameViewModel: GameViewModel,
    activePlayerId: String?,
    onCardAction: (CardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.background(Color(0xFF2E4A2E)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(opponents) { opponent ->
            OpponentCard(
                player = opponent,
                gameViewModel = gameViewModel,
                isActive = opponent.id == activePlayerId,
                onCardAction = onCardAction,
                modifier = Modifier.fillParentMaxWidth(
                    if (opponents.size == 1) 1f else 0.5f
                )
            )
        }
    }
}

@Composable
private fun OpponentCard(
    player: Player,
    gameViewModel: GameViewModel,
    isActive: Boolean,
    onCardAction: (CardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val battlefieldCards = gameViewModel.getCards(player.id, Zone.BATTLEFIELD)
    val handCount = gameViewModel.getCardCount(player.id, Zone.HAND)
    val libraryCount = gameViewModel.getCardCount(player.id, Zone.LIBRARY)
    val graveyardCount = gameViewModel.getCardCount(player.id, Zone.GRAVEYARD)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Player info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    player.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    "${player.life} life",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (player.life <= 10) Color.Red else Color.Unspecified
                )
            }

            // Zone counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ZoneCount("Hand", handCount)
                ZoneCount("Library", libraryCount)
                ZoneCount("Graveyard", graveyardCount)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Battlefield cards (horizontal scroll)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(60.dp)
            ) {
                items(battlefieldCards) { card ->
                    SmallBattlefieldCard(
                        cardInstance = card,
                        onClick = { onCardAction(CardAction.ViewDetails(card)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneCount(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SmallBattlefieldCard(
    cardInstance: CardInstance,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 56.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = 1.dp,
                color = if (cardInstance.isTapped) Color.Gray else Color.White,
                shape = RoundedCornerShape(4.dp)
            )
            .rotate(if (cardInstance.isTapped) 90f else 0f)
            .clickable(onClick = onClick)
    ) {
        CardImage(
            imageUrl = if (cardInstance.isFaceDown) null else cardInstance.card.imageUri,
            contentDescription = cardInstance.card.name,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun LocalPlayerSection(
    player: Player,
    gameViewModel: GameViewModel,
    isActivePlayer: Boolean,
    onCardAction: (CardAction) -> Unit,
    selectionState: SelectionState,
    otherPlayers: List<Player>,
    onShowContextMenu: (CardInstance) -> Unit,
    modifier: Modifier = Modifier
) {
    val battlefieldCards = gameViewModel.getCards(player.id, Zone.BATTLEFIELD)
    val handCards = gameViewModel.getCards(player.id, Zone.HAND)
    val commandZoneCards = gameViewModel.getCards(player.id, Zone.COMMAND_ZONE)

    Column(modifier = modifier.background(Color(0xFF1B5E20))) {
        // Battlefield
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp)
        ) {
            if (battlefieldCards.isEmpty()) {
                Text(
                    "Tap to play cards",
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                BattlefieldGrid(
                    cards = battlefieldCards,
                    selectionState = selectionState,
                    onCardClick = { card ->
                        gameViewModel.toggleTap(card.instanceId)
                    },
                    onCardLongPress = { card ->
                        onShowContextMenu(card)
                    },
                    onCardAction = onCardAction
                )
            }
        }

        // Player info bar
        PlayerInfoBar(
            player = player,
            gameViewModel = gameViewModel,
            commandZoneCards = commandZoneCards,
            isActivePlayer = isActivePlayer,
            onCardAction = onCardAction
        )

        // Hand
        HandStrip(
            handCards = handCards,
            onCardClick = { card ->
                // Play to battlefield on tap
                gameViewModel.moveCard(card.instanceId, Zone.BATTLEFIELD)
            },
            onCardLongPress = { card ->
                onShowContextMenu(card)
            },
            onCardAction = onCardAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )
    }
}

@Composable
private fun BattlefieldGrid(
    cards: List<CardInstance>,
    selectionState: SelectionState,
    onCardClick: (CardInstance) -> Unit,
    onCardLongPress: (CardInstance) -> Unit,
    onCardAction: (CardAction) -> Unit
) {
    // Simple grid layout for battlefield
    val columns = 6
    val rows = (cards.size + columns - 1) / columns

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (row in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < cards.size) {
                        val card = cards[index]
                        BattlefieldCard(
                            cardInstance = card,
                            isSelected = selectionState.isSelected(card.instanceId),
                            onClick = { onCardClick(card) },
                            onLongClick = { onCardLongPress(card) },
                            onDoubleClick = { onCardAction(CardAction.ViewDetails(card)) }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(width = 50.dp, height = 70.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BattlefieldCard(
    cardInstance: CardInstance,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    var lastClickTime by remember { mutableStateOf(0L) }

    Box(
        modifier = Modifier
            .size(width = 50.dp, height = 70.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    cardInstance.isTapped -> Color.Gray
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(4.dp)
            )
            .rotate(if (cardInstance.isTapped) 90f else 0f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val now = System.currentTimeMillis()
                        if (now - lastClickTime < UIConstants.DOUBLE_CLICK_DELAY_MS) {
                            onDoubleClick()
                            lastClickTime = 0L // Reset to prevent triple-click triggering
                        } else {
                            onClick()
                            lastClickTime = now
                        }
                    },
                    onLongPress = { onLongClick() }
                )
            }
    ) {
        CardImage(
            imageUrl = if (cardInstance.isFaceDown) null else cardInstance.card.imageUri,
            contentDescription = cardInstance.card.name,
            modifier = Modifier.fillMaxSize()
        )

        // Counter indicators
        if (cardInstance.counters.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                    .padding(2.dp)
            ) {
                Text(
                    text = cardInstance.counters.values.sum().toString(),
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }

        // P/T modification indicator
        if (cardInstance.powerModifier != 0 || cardInstance.toughnessModifier != 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color.Blue.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 2.dp)
            ) {
                val mod = "${if (cardInstance.powerModifier >= 0) "+" else ""}${cardInstance.powerModifier}/${if (cardInstance.toughnessModifier >= 0) "+" else ""}${cardInstance.toughnessModifier}"
                Text(text = mod, color = Color.White, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun PlayerInfoBar(
    player: Player,
    gameViewModel: GameViewModel,
    commandZoneCards: List<CardInstance>,
    isActivePlayer: Boolean,
    onCardAction: (CardAction) -> Unit
) {
    val libraryCount = gameViewModel.getCardCount(player.id, Zone.LIBRARY)
    val graveyardCount = gameViewModel.getCardCount(player.id, Zone.GRAVEYARD)
    val exileCount = gameViewModel.getCardCount(player.id, Zone.EXILE)

    Surface(
        color = Color(0xFF2E7D32),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Life counter
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { gameViewModel.updateLife(player.id, player.life - 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "Decrease life", tint = Color.White)
                }
                Text(
                    "${player.life}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (player.life <= 10) Color.Red else Color.White
                )
                IconButton(
                    onClick = { gameViewModel.updateLife(player.id, player.life + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, "Increase life", tint = Color.White)
                }
            }

            // Commander
            if (commandZoneCards.isNotEmpty()) {
                Row {
                    commandZoneCards.forEach { commander ->
                        Box(
                            modifier = Modifier
                                .size(width = 30.dp, height = 42.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .clickable { onCardAction(CardAction.ToBattlefield(commander)) }
                        ) {
                            CardImage(
                                imageUrl = commander.card.imageUri,
                                contentDescription = commander.card.name
                            )
                        }
                    }
                }
            }

            // Zone counts
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZoneButton("L", libraryCount) { gameViewModel.drawCard(player.id) }
                ZoneButton("G", graveyardCount) {}
                ZoneButton("E", exileCount) {}
            }
        }
    }
}

@Composable
private fun ZoneButton(label: String, count: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(count.toString(), color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

@Composable
private fun HandStrip(
    handCards: List<CardInstance>,
    onCardClick: (CardInstance) -> Unit,
    onCardLongPress: (CardInstance) -> Unit,
    onCardAction: (CardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1565C0),
        modifier = modifier
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy((-20).dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(handCards) { card ->
                HandCard(
                    cardInstance = card,
                    onClick = { onCardClick(card) },
                    onLongClick = { onCardLongPress(card) }
                )
            }
        }
    }
}

@Composable
private fun HandCard(
    cardInstance: CardInstance,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 60.dp, height = 90.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
    ) {
        CardImage(
            imageUrl = cardInstance.card.imageUri,
            contentDescription = cardInstance.card.name,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun BottomActionBar(
    activePlayer: Player?,
    localPlayer: Player?,
    gameViewModel: GameViewModel,
    onShowDieRoller: () -> Unit,
    onShowSetLife: () -> Unit,
    onShowTokenCreation: () -> Unit,
    onShowPlayerCounters: () -> Unit,
    onShowLibrary: () -> Unit,
    onShowGraveyard: () -> Unit,
    onShowExile: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { activePlayer?.let { gameViewModel.passTurn() } }) {
                Icon(Icons.Default.KeyboardArrowRight, "Pass Turn")
            }
            IconButton(onClick = { activePlayer?.let { gameViewModel.untapAll(it.id) } }) {
                Icon(Icons.Default.Refresh, "Untap All")
            }
            IconButton(onClick = { localPlayer?.let { gameViewModel.drawCard(it.id) } }) {
                Icon(Icons.Default.Add, "Draw")
            }
            IconButton(onClick = onShowTokenCreation) {
                Icon(Icons.Default.Create, "Token")
            }
            IconButton(onClick = onShowDieRoller) {
                Icon(Icons.Default.Star, "Dice")
            }
            IconButton(onClick = onShowLibrary) {
                Icon(Icons.Default.Search, "Library")
            }
        }
    }
}

@Composable
private fun PausedOverlay(
    pauseReason: String?,
    isHost: Boolean,
    onResume: () -> Unit,
    onReturnToMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Game Paused", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(pauseReason ?: "A player has disconnected.")
                Spacer(modifier = Modifier.height(16.dp))

                if (isHost) {
                    Button(onClick = onResume) {
                        Text("Resume Game")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedButton(onClick = onReturnToMenu) {
                    Text("Return to Menu")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionsMenuSheet(
    player: Player,
    gameViewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Game Actions", style = MaterialTheme.typography.titleMedium)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Phase shortcuts
            Text("Phases", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(GamePhase.entries) { phase ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            gameViewModel.setPhase(phase)
                            onDismiss()
                        },
                        label = { Text(phase.displayName, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Common actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = {
                    gameViewModel.shuffleLibrary(player.id)
                    onDismiss()
                }) {
                    Text("Shuffle")
                }
                OutlinedButton(onClick = {
                    gameViewModel.mulligan(player.id)
                    onDismiss()
                }) {
                    Text("Mulligan")
                }
                OutlinedButton(onClick = {
                    gameViewModel.concede(player.id)
                    onDismiss()
                }) {
                    Text("Concede")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
