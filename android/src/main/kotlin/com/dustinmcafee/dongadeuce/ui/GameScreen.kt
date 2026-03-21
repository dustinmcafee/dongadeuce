@file:OptIn(ExperimentalMaterial3Api::class)

package com.dustinmcafee.dongadeuce.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.dustinmcafee.dongadeuce.R
import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.service.GameSessionService
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
    gameViewModel: GameViewModel
) {
    val gameUiState by gameViewModel.uiState.collectAsState()
    val revealedCardsState by gameViewModel.revealedCardsState.collectAsState()
    val selectionState = rememberSelectionState()
    val keyboardState = rememberKeyboardShortcutState(gameViewModel, selectionState)
    val focusedCardState = rememberFocusedCardState()
    val cardViewerDrawerState = rememberCardViewerDrawerState()

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
    var showLibraryActionsDialog by remember { mutableStateOf(false) }
    var showGraveyardDialog by remember { mutableStateOf(false) }
    var showExileDialog by remember { mutableStateOf(false) }
    var showHandDialog by remember { mutableStateOf(false) }
    var showGameLogDialog by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
    var viewingOpponentZone by remember { mutableStateOf<Pair<Player, Zone>?>(null) }
    var showScryDialog by remember { mutableStateOf(false) }
    var scryCount by remember { mutableStateOf(1) }
    var showCommanderDamageDialog by remember { mutableStateOf(false) }
    var showCommandZoneDialog by remember { mutableStateOf(false) }
    var showLibraryPeekTopDialog by remember { mutableStateOf(false) }
    var showLibraryPeekBottomDialog by remember { mutableStateOf(false) }
    var peekCount by remember { mutableStateOf(1) }
    var handOwnerIdToShow by remember { mutableStateOf<String?>(null) }
    var showNumberInputDialog by remember { mutableStateOf(false) }
    var numberInputTitle by remember { mutableStateOf("") }
    var numberInputDefault by remember { mutableStateOf(0) }
    var numberInputCallback by remember { mutableStateOf<((Int) -> Unit)?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showStackUntilFoundDialog by remember { mutableStateOf(false) }
    var showSideboardDialog by remember { mutableStateOf(false) }
    var showManaDialog by remember { mutableStateOf(false) }

    // Update notification with latest game events (Issue 14)
    val context = LocalContext.current
    val gameLogSize = gameUiState.gameState?.gameLog?.size ?: 0
    LaunchedEffect(gameLogSize) {
        if (GameSessionService.isActive.value && gameLogSize > 0) {
            val latestEvents = gameViewModel.getLatestEvents(3)
            if (latestEvents.isNotEmpty()) {
                GameSessionService.updateWithLog(context, latestEvents)
            }
        }
    }

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
        keyboardState.onShowCommandZoneDialog = { showCommandZoneDialog = true }
        keyboardState.onShowPeekTopDialog = {
            peekCount = 1
            showLibraryPeekTopDialog = true
        }
        keyboardState.onShowPeekBottomDialog = {
            peekCount = 1
            showLibraryPeekBottomDialog = true
        }
        keyboardState.onShowNumberInputDialog = { title, defaultValue, callback ->
            numberInputTitle = title
            numberInputDefault = defaultValue
            numberInputCallback = callback
            showNumberInputDialog = true
        }
        keyboardState.onShowStackUntilFoundDialog = {
            showStackUntilFoundDialog = true
        }
        keyboardState.onShowSideboardDialog = {
            showSideboardDialog = true
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
            showCommandZoneDialog = false
            showCommanderDamageDialog = false
            showLibraryPeekTopDialog = false
            showLibraryPeekBottomDialog = false
            showScryDialog = false
            showHandDialog = false
            showNumberInputDialog = false
            showStackUntilFoundDialog = false
            showSideboardDialog = false
            showSettingsDialog = false
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
            is CardAction.ViewDetails -> {
                cardDetailsToShow = action.cardInstance
                focusedCardState.updateFocusedCard(action.cardInstance)
            }
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
            is CardAction.ViewHand -> {
                handOwnerIdToShow = action.playerId
                showHandDialog = true
            }
            is CardAction.Mulligan -> {
                gameViewModel.mulligan(action.playerId)
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
                // Resizable weights for opponent vs local player sections
                var opponentWeight by remember { mutableStateOf(0.5f) }
                var localWeight by remember { mutableStateOf(0.5f) }
                val sectionDensity = LocalDensity.current
                var columnHeightPx by remember { mutableStateOf(1f) }

                Column(modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { columnHeightPx = it.size.height.toFloat().coerceAtLeast(1f) }
                ) {
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
                            onShowOpponentGraveyard = { opponent ->
                                viewingOpponentZone = Pair(opponent, Zone.GRAVEYARD)
                            },
                            onShowOpponentExile = { opponent ->
                                viewingOpponentZone = Pair(opponent, Zone.EXILE)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(opponentWeight.coerceAtLeast(0.1f))
                        )

                        // ── Handle 1: Between opponent and local battlefield ──
                        // Drag UP = shrink opponent, grow local battlefield
                        // Drag DOWN = grow opponent, shrink local battlefield
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { _, dragAmount ->
                                        // Convert pixel drag to proportional weight change
                                        // based on actual column height for 1:1 finger tracking
                                        val totalWeight = opponentWeight + localWeight
                                        val pixelRatio = dragAmount.y / columnHeightPx
                                        val delta = pixelRatio * totalWeight
                                        opponentWeight = (opponentWeight + delta).coerceIn(0.1f, 2f)
                                        localWeight = (localWeight - delta).coerceIn(0.1f, 2f)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    // Local player section (bottom)
                    localPlayer?.let { player ->
                        LocalPlayerSection(
                            player = player,
                            gameState = gameState,
                            gameViewModel = gameViewModel,
                            isActivePlayer = player.id == activePlayer?.id,
                            onCardAction = handleAction,
                            selectionState = selectionState,
                            onShowContextMenu = { card ->
                                contextMenuCard = card
                                showContextMenu = true
                            },
                            onShowLibrary = { showLibraryDialog = true },
                            onShowGraveyard = { showGraveyardDialog = true },
                            onShowExile = { showExileDialog = true },
                            onShowHand = { showHandDialog = true },
                            onCardFocus = { card -> focusedCardState.updateFocusedCard(card) },
                            onShowMana = { showManaDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(localWeight.coerceAtLeast(0.1f))
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
                onShowTokenCreation = { showTokenDialog = true },
                onShowMana = { showManaDialog = true }
            )
        }

        // Paused overlay
        if (uiState.isPaused) {
            PausedOverlay(
                pauseReason = uiState.pauseReason,
                isAdmin = menuViewModel.isHost(),
                onResume = { menuViewModel.resumeGame() },
                onReturnToMenu = { menuViewModel.returnToMenu() }
            )
        }

        // Swipe edge detector on left side of screen
        SwipeEdgeDetector(
            drawerState = cardViewerDrawerState,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        // Card viewer drawer (slides in from left) — look up live state so flip/tap updates are reflected
        val liveCard = focusedCardState.focusedCard?.let { focused ->
            gameState?.cardInstances?.find { it.instanceId == focused.instanceId } ?: focused
        }
        CardViewerDrawer(
            cardInstance = liveCard,
            drawerState = cardViewerDrawerState
        )
    }

    // Context menu bottom sheet
    contextMenuCard?.let { card ->
        if (showContextMenu) {
            CardContextMenuBottomSheet(
                cardInstance = card,
                allPlayers = allPlayers,
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
                onToPositionFromTop = { pos -> gameViewModel.moveCardToLibraryPosition(card.instanceId, pos) }
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
            playerName = localPlayer?.name ?: "Player",
            onDismiss = { showDieRollerDialog = false },
            onRollLogged = { dieType, result, numDice, individualResults ->
                localPlayer?.let { gameViewModel.logDieRoll(it.id, dieType, result, numDice, individualResults) }
            }
        )
    }

    // Mana pool dialog (Issue 15)
    if (showManaDialog && localPlayer != null) {
        ManaPoolDialog(
            player = localPlayer,
            gameViewModel = gameViewModel,
            onDismiss = { showManaDialog = false }
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
            onCreateToken = { name, type, power, toughness, color, imageUri, quantity, oracleText ->
                gameViewModel.createToken(activePlayer.id, name, type, power, toughness, color, imageUri, quantity, oracleText)
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

    if (showLibraryActionsDialog && localPlayer != null) {
        val libraryCards = gameViewModel.getCards(localPlayer.id, Zone.LIBRARY)
        val topCard = libraryCards.lastOrNull()
        val canSeeTopCard = localPlayer.revealTopCard || localPlayer.lookAtTopCard
        LibraryActionsDialog(
            librarySize = libraryCards.size,
            topCard = topCard,
            canSeeTopCard = canSeeTopCard,
            onDismiss = { showLibraryActionsDialog = false },
            onDraw = { gameViewModel.drawCard(localPlayer.id) },
            onDrawMultiple = { count -> gameViewModel.drawCards(localPlayer.id, count) },
            onSearch = {
                showLibraryActionsDialog = false
                showLibraryDialog = true
            },
            onShuffle = { gameViewModel.shuffleLibrary(localPlayer.id) },
            onMill = { count -> gameViewModel.millCards(localPlayer.id, count) },
            onScry = { count ->
                scryCount = count
                showLibraryActionsDialog = false
                showScryDialog = true
            },
            onPlayTopCard = {
                topCard?.let { gameViewModel.moveCard(it.instanceId, Zone.BATTLEFIELD) }
            },
            onRevealTop = { gameViewModel.toggleRevealTopCard(localPlayer.id) },
            onLookAtTop = { gameViewModel.toggleLookAtTopCard(localPlayer.id) },
            onViewTopCard = { card ->
                cardDetailsToShow = card
            },
            onSendTopTo = { count, zone ->
                // Get the top X cards from library and move them to the target zone
                val cards = gameViewModel.getCards(localPlayer.id, Zone.LIBRARY)
                cards.takeLast(count.coerceAtMost(cards.size)).forEach { card ->
                    gameViewModel.moveCard(card.instanceId, zone)
                }
            },
            onSendBottomTo = { count, zone ->
                // Get the bottom X cards from library and move them to the target zone
                val cards = gameViewModel.getCards(localPlayer.id, Zone.LIBRARY)
                cards.take(count.coerceAtMost(cards.size)).forEach { card ->
                    gameViewModel.moveCard(card.instanceId, zone)
                }
            },
            onViewTopN = { count ->
                peekCount = count
                showLibraryActionsDialog = false
                showLibraryPeekTopDialog = true
            },
            onViewBottomN = { count ->
                peekCount = count
                showLibraryActionsDialog = false
                showLibraryPeekBottomDialog = true
            },
            onRevealTopN = { count ->
                val topCards = gameViewModel.getTopCards(localPlayer.id, count)
                gameViewModel.revealCards(localPlayer.id, topCards.map { it.instanceId }, emptyList())
                showLibraryActionsDialog = false
            },
            onRevealBottomN = { count ->
                val bottomCards = gameViewModel.getBottomCards(localPlayer.id, count)
                gameViewModel.revealCards(localPlayer.id, bottomCards.map { it.instanceId }, emptyList())
                showLibraryActionsDialog = false
            },
            onShuffleTopN = { count ->
                gameViewModel.shuffleTopCards(localPlayer.id, count)
                showLibraryActionsDialog = false
            },
            onShuffleBottomN = { count ->
                gameViewModel.shuffleBottomCards(localPlayer.id, count)
                showLibraryActionsDialog = false
            },
            onViewLibrary = {
                val allCards = gameViewModel.getCards(localPlayer.id, Zone.LIBRARY)
                peekCount = allCards.size
                showLibraryActionsDialog = false
                showLibraryPeekTopDialog = true
            }
        )
    }

    // Scry dialog
    if (showScryDialog && localPlayer != null) {
        val libraryCards = gameViewModel.getCards(localPlayer.id, Zone.LIBRARY)
        // Get top X cards (library is stored with bottom at index 0, top at end)
        val scryCards = libraryCards.takeLast(scryCount.coerceAtMost(libraryCards.size))
        ScryDialog(
            cards = scryCards,
            onDismiss = { showScryDialog = false },
            onReorder = { orderedIds ->
                // Reorder the top of the library based on the new order
                gameViewModel.reorderLibraryTop(localPlayer.id, orderedIds)
            },
            onToHand = { card ->
                gameViewModel.moveCard(card.instanceId, Zone.HAND)
            },
            onToBattlefield = { card ->
                gameViewModel.moveCard(card.instanceId, Zone.BATTLEFIELD)
            },
            onToBottom = { card ->
                gameViewModel.moveCardToBottomOfLibrary(card.instanceId)
            },
            onToGraveyard = { card ->
                gameViewModel.moveCard(card.instanceId, Zone.GRAVEYARD)
            }
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

    if (showHandDialog) {
        // Use handOwnerIdToShow if set, otherwise fall back to localPlayer
        val handOwnerId = handOwnerIdToShow ?: localPlayer?.id
        val handOwner = allPlayers.find { it.id == handOwnerId } ?: localPlayer
        if (handOwner != null) {
            val handCards = gameViewModel.getCards(handOwner.id, Zone.HAND)
            HandViewDialog(
                cards = handCards,
                playerName = handOwner.name,
                onDismiss = {
                    showHandDialog = false
                    handOwnerIdToShow = null
                },
                onPlayCard = { card ->
                    gameViewModel.moveCard(card.instanceId, Zone.BATTLEFIELD)
                },
                onDiscardCard = { card ->
                    gameViewModel.moveCard(card.instanceId, Zone.GRAVEYARD)
                },
                onCardDetails = { card ->
                    cardDetailsToShow = card
                }
            )
        }
    }

    // Opponent zone viewing dialog (read-only)
    viewingOpponentZone?.let { (opponent, zone) ->
        val cards = gameViewModel.getCards(opponent.id, zone)
        val zoneName = when (zone) {
            Zone.GRAVEYARD -> "Graveyard"
            Zone.EXILE -> "Exile"
            else -> zone.name
        }
        OpponentZoneDialog(
            cards = cards,
            playerName = opponent.name,
            zoneName = zoneName,
            onDismiss = { viewingOpponentZone = null }
        )
    }

    if (showGameLogDialog && gameState != null) {
        GameLogDialog(
            gameLog = gameState.gameLog,
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

    // Commander damage tracking dialog
    if (showCommanderDamageDialog && gameState != null) {
        // Get all commanders (creatures/planeswalkers in battlefield or command zone)
        val commanders = gameState.cardInstances.filter { card ->
            card.card.canBeCommander &&
            (card.zone == Zone.BATTLEFIELD || card.zone == Zone.COMMAND_ZONE)
        }
        CommanderDamageDialog(
            players = gameState.players,
            commanders = commanders,
            onDismiss = { showCommanderDamageDialog = false },
            onDamageChange = { playerId, commanderId, newDamage ->
                gameViewModel.updateCommanderDamage(playerId, commanderId, newDamage)
            }
        )
    }

    // Command zone dialog
    if (showCommandZoneDialog && localPlayer != null) {
        val commandZoneCards = gameViewModel.getCards(localPlayer.id, Zone.COMMAND_ZONE)
        CommandZoneDialog(
            cards = commandZoneCards,
            playerName = localPlayer.name,
            onDismiss = { showCommandZoneDialog = false },
            onCast = { card ->
                gameViewModel.moveCard(card.instanceId, Zone.BATTLEFIELD)
            },
            onToHand = { card ->
                gameViewModel.moveCard(card.instanceId, Zone.HAND)
            }
        )
    }

    // Library peek top dialog
    if (showLibraryPeekTopDialog && localPlayer != null) {
        val libraryCards = gameViewModel.getCards(localPlayer.id, Zone.LIBRARY)
        val topCards = libraryCards.takeLast(peekCount.coerceAtMost(libraryCards.size))
        LibraryPeekDialog(
            cards = topCards.reversed(), // Show top card first
            title = "Top ${topCards.size} Cards",
            onDismiss = { showLibraryPeekTopDialog = false },
            onToHand = { card -> gameViewModel.moveCard(card.instanceId, Zone.HAND) },
            onToBattlefield = { card -> gameViewModel.moveCard(card.instanceId, Zone.BATTLEFIELD) },
            onToGraveyard = { card -> gameViewModel.moveCard(card.instanceId, Zone.GRAVEYARD) },
            onToExile = { card -> gameViewModel.moveCard(card.instanceId, Zone.EXILE) },
            onToTop = { card -> gameViewModel.moveCardToTopOfLibrary(card.instanceId) },
            onToBottom = { card -> gameViewModel.moveCardToBottomOfLibrary(card.instanceId) }
        )
    }

    // Library peek bottom dialog
    if (showLibraryPeekBottomDialog && localPlayer != null) {
        val libraryCards = gameViewModel.getCards(localPlayer.id, Zone.LIBRARY)
        val bottomCards = libraryCards.take(peekCount.coerceAtMost(libraryCards.size))
        LibraryPeekDialog(
            cards = bottomCards, // Bottom card first
            title = "Bottom ${bottomCards.size} Cards",
            onDismiss = { showLibraryPeekBottomDialog = false },
            onToHand = { card -> gameViewModel.moveCard(card.instanceId, Zone.HAND) },
            onToBattlefield = { card -> gameViewModel.moveCard(card.instanceId, Zone.BATTLEFIELD) },
            onToGraveyard = { card -> gameViewModel.moveCard(card.instanceId, Zone.GRAVEYARD) },
            onToExile = { card -> gameViewModel.moveCard(card.instanceId, Zone.EXILE) },
            onToTop = { card -> gameViewModel.moveCardToTopOfLibrary(card.instanceId) },
            onToBottom = { card -> gameViewModel.moveCardToBottomOfLibrary(card.instanceId) }
        )
    }

    // Number input dialog
    if (showNumberInputDialog) {
        NumberInputDialog(
            title = numberInputTitle,
            defaultValue = numberInputDefault,
            onDismiss = {
                showNumberInputDialog = false
                numberInputCallback = null
            },
            onConfirm = { value ->
                numberInputCallback?.invoke(value)
                showNumberInputDialog = false
                numberInputCallback = null
            }
        )
    }

    // Revealed cards dialog - shows when another player reveals cards to us
    revealedCardsState?.let { state ->
        // Only show if we're one of the target players (or it's revealed to all)
        val isTargeted = state.targetPlayerIds.isEmpty() ||
            localPlayer?.id in state.targetPlayerIds
        if (isTargeted) {
            RevealedCardsDialog(
                cards = state.cards,
                revealingPlayerName = state.revealingPlayerName,
                title = state.title,
                onDismiss = { gameViewModel.dismissRevealedCards() },
                onViewDetails = { card -> cardDetailsToShow = card }
            )
        }
    }

    // Settings dialog
    if (showSettingsDialog) {
        val userSettings = menuViewModel.userSettings
        SettingsDialog(
            currentPlayerName = userSettings.getPlayerName(),
            currentServerAddress = userSettings.getServerAddress(),
            currentServerPort = userSettings.getServerPort(),
            currentTlsEnabled = userSettings.getTlsEnabled(),
            onPlayerNameChange = { userSettings.setPlayerName(it) },
            onServerAddressChange = { userSettings.setServerAddress(it) },
            onServerPortChange = { userSettings.setServerPort(it) },
            onTlsEnabledChange = { menuViewModel.setTlsEnabled(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Stack Until Found dialog
    if (showStackUntilFoundDialog && localPlayer != null) {
        val libraryCards = gameViewModel.getCards(localPlayer.id, Zone.LIBRARY)
        StackUntilFoundDialog(
            libraryCards = libraryCards,
            onDismiss = { showStackUntilFoundDialog = false },
            onMoveToGraveyard = { card ->
                gameViewModel.moveCard(card.instanceId, Zone.GRAVEYARD)
            },
            onMoveToBottom = { card ->
                gameViewModel.moveCardToBottomOfLibrary(card.instanceId)
            }
        )
    }

    // Sideboard dialog
    if (showSideboardDialog && localPlayer != null) {
        val sideboardCards = gameViewModel.getCards(localPlayer.id, Zone.SIDEBOARD)
        SideboardDialog(
            sideboardCards = sideboardCards,
            onDismiss = { showSideboardDialog = false },
            onToHand = { card ->
                gameViewModel.moveCard(card.instanceId, Zone.HAND)
            },
            onToBattlefield = { card ->
                gameViewModel.moveCard(card.instanceId, Zone.BATTLEFIELD)
            },
            onToGraveyard = { card ->
                gameViewModel.moveCard(card.instanceId, Zone.GRAVEYARD)
            }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Turn $turnNumber",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (isHotseatMode) {
                        Text(
                            " (Hotseat)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
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
    onShowOpponentGraveyard: (Player) -> Unit,
    onShowOpponentExile: (Player) -> Unit,
    modifier: Modifier = Modifier
) {
    val gameState = gameViewModel.uiState.collectAsState().value.gameState

    // Use Column with verticalScroll for opponents - allows pinch-to-zoom inside each section
    // Each opponent section has its own background color based on active state
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opponents.forEach { opponent ->
            OpponentBattlefieldSection(
                player = opponent,
                gameState = gameState,
                gameViewModel = gameViewModel,
                isActive = opponent.id == activePlayerId,
                onCardAction = onCardAction,
                onShowGraveyard = { onShowOpponentGraveyard(opponent) },
                onShowExile = { onShowOpponentExile(opponent) },
                modifier = Modifier.fillMaxWidth()
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
    onShowGraveyard: () -> Unit,
    onShowExile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val battlefieldCards = gameViewModel.getCards(player.id, Zone.BATTLEFIELD)
    val handCount = gameViewModel.getCardCount(player.id, Zone.HAND)
    val libraryCount = gameViewModel.getCardCount(player.id, Zone.LIBRARY)
    val graveyardCount = gameViewModel.getCardCount(player.id, Zone.GRAVEYARD)
    val exileCount = gameViewModel.getCardCount(player.id, Zone.EXILE)

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

            // Zone counts - Graveyard and Exile are clickable
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ZoneCount("Hand", handCount)
                ZoneCount("Library", libraryCount)
                ClickableZoneCount("Graveyard", graveyardCount, onClick = onShowGraveyard)
                ClickableZoneCount("Exile", exileCount, onClick = onShowExile)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Battlefield cards (horizontal scroll)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(60.dp)
            ) {
                items(battlefieldCards, key = { it.instanceId }) { card ->
                    SmallBattlefieldCard(
                        cardInstance = card,
                        onClick = { onCardAction(CardAction.ViewDetails(card)) }
                    )
                }
            }
        }
    }
}

/**
 * Full-height opponent battlefield section with proper grid layout.
 * Matches Desktop opponent view with 3-row battlefield grid.
 */
@Composable
private fun OpponentBattlefieldSection(
    player: Player,
    gameState: GameState?,
    gameViewModel: GameViewModel,
    isActive: Boolean,
    onCardAction: (CardAction) -> Unit,
    onShowGraveyard: () -> Unit,
    onShowExile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val battlefieldCards = gameViewModel.getCards(player.id, Zone.BATTLEFIELD)
    val handCount = gameViewModel.getCardCount(player.id, Zone.HAND)
    val libraryCount = gameViewModel.getCardCount(player.id, Zone.LIBRARY)
    val graveyardCount = gameViewModel.getCardCount(player.id, Zone.GRAVEYARD)
    val exileCount = gameViewModel.getCardCount(player.id, Zone.EXILE)

    // Compute grid positions for this opponent's battlefield
    val gridPositions = remember(gameState, battlefieldCards) {
        gameState?.computeBattlefieldPositions(player.id) ?: emptyMap()
    }

    // Light green for active player, dark green for inactive
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF1B5E20) else Color(0xFF2E4A2E)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Player info header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    player.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    "${player.life} life",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (player.life <= 10) Color.Red else Color.Unspecified
                )
            }

            // Zone counts row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ZoneCount("Hand", handCount)
                ZoneCount("Library", libraryCount)
                ClickableZoneCount("Graveyard", graveyardCount, onClick = onShowGraveyard)
                ClickableZoneCount("Exile", exileCount, onClick = onShowExile)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Full battlefield grid with same size as local player
            if (battlefieldCards.isNotEmpty()) {
                OpponentBattlefieldGrid(
                    cards = battlefieldCards,
                    gridPositions = gridPositions,
                    onCardClick = { card -> onCardAction(CardAction.ViewDetails(card)) }
                )
            } else {
                // Empty battlefield placeholder - same size as actual battlefield (3 rows)
                val cardSize = 70.dp
                // Spacing accounts for max stack extension: 2 * 10% * 70dp = ~14dp, plus buffer
                val spacing = 16.dp
                val battlefieldHeight = (cardSize + spacing) * UIConstants.BATTLEFIELD_ROWS

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(battlefieldHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No cards on battlefield",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * Opponent battlefield grid with pinch-to-zoom and pan support.
 * Cards are view-only (tap to view details).
 */
@Composable
private fun OpponentBattlefieldGrid(
    cards: List<CardInstance>,
    gridPositions: Map<String, Pair<Int, Int>>,
    onCardClick: (CardInstance) -> Unit
) {
    val cardSize = 70.dp
    // Spacing accounts for max stack extension: 2 * 10% * 70dp = ~14dp, plus buffer
    val spacing = 16.dp
    val density = LocalDensity.current

    val cardSizePx = with(density) { cardSize.toPx() }
    val spacingPx = with(density) { spacing.toPx() }
    val cellSize = cardSizePx + spacingPx

    // Stack offset (10% of card size for visual separation)
    val stackOffsetPx = cardSizePx * UIConstants.STACK_OFFSET_RATIO

    // Calculate stack indices for visual stacking
    val stackInfoMap = remember(gridPositions, cards) {
        val stackInfo = mutableMapOf<String, CardStackInfo>()

        // Group cards by position
        val positionToCards = mutableMapOf<Pair<Int, Int>, MutableList<String>>()
        gridPositions.forEach { (cardId, pos) ->
            positionToCards.getOrPut(pos) { mutableListOf() }.add(cardId)
        }

        positionToCards.forEach { (gridPos, cardIds) ->
            val sortedCardIds = cardIds.sortedBy { cardId ->
                cards.find { it.instanceId == cardId }?.placedTimestamp ?: 0L
            }

            sortedCardIds.forEachIndexed { index, cardId ->
                stackInfo[cardId] = CardStackInfo(
                    gridPos = gridPos,
                    stackIndex = index.coerceAtMost(2)
                )
            }
        }

        stackInfo
    }

    // Fixed 3 rows
    val totalRows = UIConstants.BATTLEFIELD_ROWS

    // Pinch-to-zoom and pan state using transformable
    var scale by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(0.5f, 5f)
        scale = newScale
        panOffset = Offset(
            x = panOffset.x + panChange.x,
            y = panOffset.y + panChange.y
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(((totalRows * cellSize) / density.density).dp)
            .transformable(state = transformState, lockRotationOnZoomPan = true)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = panOffset.x
                translationY = panOffset.y
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
            }
    ) {
        cards.forEach { card ->
            val position = gridPositions[card.instanceId] ?: Pair(0, 0)
            val (col, row) = position
            val stackInfo = stackInfoMap[card.instanceId]

            // Invert row for opponent battlefield - their lands (row 2) should appear at top,
            // their creatures (row 0) should appear at bottom from our perspective
            val invertedRow = totalRows - 1 - row

            var xPos = col * cellSize
            var yPos = invertedRow * cellSize

            if (stackInfo != null) {
                xPos += stackInfo.stackIndex * stackOffsetPx
                yPos += stackInfo.stackIndex * stackOffsetPx
            }

            val finalOffset = IntOffset(xPos.roundToInt(), yPos.roundToInt())
            val zIndex = (invertedRow * 1000 + col * 10 + (stackInfo?.stackIndex ?: 0)).toFloat()

            key(card.instanceId) {
                Box(
                    modifier = Modifier
                        .offset { finalOffset }
                        .size(cardSize)
                        .zIndex(zIndex)
                        .clickable { onCardClick(card) }
                ) {
                    BattlefieldCard(
                        cardInstance = card,
                        isSelected = false,
                        onClick = { onCardClick(card) },
                        onLongClick = { onCardClick(card) },
                        onDoubleClick = { onCardClick(card) }
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
private fun ClickableZoneCount(label: String, count: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SmallBattlefieldCard(
    cardInstance: CardInstance,
    onClick: () -> Unit
) {
    // Square container to hold rotated card without clipping
    Box(
        modifier = Modifier
            .size(56.dp) // Square container (height of card)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 56.dp)
                .graphicsLayer {
                    rotationZ = if (cardInstance.isTapped) 90f else 0f
                    clip = false
                }
                .clip(RoundedCornerShape(4.dp))
                .border(
                    width = 1.dp,
                    color = if (cardInstance.isTapped) Color.Gray else Color.White,
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            CardImage(
                imageUrl = when {
                    cardInstance.isFaceDown -> null
                    cardInstance.isFlipped -> cardInstance.card.backFaceImageUri ?: "https://cards.scryfall.io/back.png"
                    else -> cardInstance.card.imageUri
                },
                contentDescription = cardInstance.card.name,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
internal fun LocalPlayerSection(
    player: Player,
    gameState: GameState?,
    gameViewModel: GameViewModel,
    isActivePlayer: Boolean,
    onCardAction: (CardAction) -> Unit,
    selectionState: SelectionState,
    onShowContextMenu: (CardInstance) -> Unit,
    onShowLibrary: () -> Unit,
    onShowGraveyard: () -> Unit,
    onShowExile: () -> Unit,
    onShowHand: () -> Unit,
    onCardFocus: (CardInstance) -> Unit = {},
    onShowMana: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val battlefieldCards = gameViewModel.getCards(player.id, Zone.BATTLEFIELD)
    val handCards = gameViewModel.getCards(player.id, Zone.HAND)
    val commandZoneCards = gameViewModel.getCards(player.id, Zone.COMMAND_ZONE)

    // Compute grid positions for this player's battlefield
    val gridPositions = remember(gameState, player.id) {
        gameState?.computeBattlefieldPositions(player.id) ?: emptyMap()
    }

    // Track zone bounds for drag-drop between zones
    var handZoneTop by remember { mutableStateOf(0f) }
    var battlefieldZoneBottom by remember { mutableStateOf(0f) }

    // Track active card drag for zone button drop targets (Issue 12)
    var draggedCardInstanceId by remember { mutableStateOf<String?>(null) }
    var dragPositionY by remember { mutableStateOf(0f) }
    var graveyardButtonTop by remember { mutableStateOf(0f) }
    var graveyardButtonBottom by remember { mutableStateOf(0f) }
    var exileButtonTop by remember { mutableStateOf(0f) }
    var exileButtonBottom by remember { mutableStateOf(0f) }

    // Resizable battlefield + hand heights (Issue 4)
    // battlefieldWeight and handHeight are user-adjustable by dragging the resize handles
    val density = LocalDensity.current
    var battlefieldWeight by remember { mutableStateOf(1f) } // proportional weight for battlefield
    var playerInfoHeightDp by remember { mutableStateOf(48f) } // command zone bar height in dp
    var handHeightDp by remember { mutableStateOf(100f) } // hand height in dp

    // Light green for active player, dark green for inactive
    Column(modifier = modifier.background(if (isActivePlayer) Color(0xFF1B5E20) else Color(0xFF2E4A2E))) {
        // Battlefield — resizable via drag handle below
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(battlefieldWeight.coerceAtLeast(0.1f))
                .clipToBounds()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
                .onGloballyPositioned { coordinates ->
                    battlefieldZoneBottom = coordinates.positionInWindow().y + coordinates.size.height
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onShowMana() }
                    )
                }
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
                    gridPositions = gridPositions,
                    players = gameState?.players ?: emptyList(),
                    selectionState = selectionState,
                    onCardClick = { card ->
                        onCardFocus(card)
                    },
                    onCardDoubleClick = { card ->
                        onCardFocus(card)
                        gameViewModel.toggleTap(card.instanceId)
                    },
                    onCardLongPress = { card ->
                        onCardFocus(card)
                        onShowContextMenu(card)
                    },
                    onCardPositionChanged = { cardId, col, row ->
                        gameViewModel.updateCardGridPosition(cardId, col, row)
                    },
                    handZoneTop = handZoneTop,
                    onCardDroppedToHand = { cardId ->
                        gameViewModel.moveCard(cardId, Zone.HAND)
                    },
                    onCardDragStateChanged = { cardId, y ->
                        draggedCardInstanceId = cardId
                        dragPositionY = y
                    },
                    graveyardBounds = graveyardButtonTop to graveyardButtonBottom,
                    exileBounds = exileButtonTop to exileButtonBottom,
                    onCardDroppedToGraveyard = { cardId ->
                        gameViewModel.moveCard(cardId, Zone.GRAVEYARD)
                    },
                    onCardDroppedToExile = { cardId ->
                        gameViewModel.moveCard(cardId, Zone.EXILE)
                    }
                )
            }
        }

        // ── Handle 2: Between battlefield and PlayerInfoBar (command zone) ──
        // Drag DOWN = bigger command zone bar (pull boundary down)
        // Drag UP = smaller command zone bar (push boundary up, more battlefield)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        val deltaDp = with(density) { dragAmount.y.toDp().value }
                        // Drag DOWN = shrink command zone (more battlefield above)
                        // Drag UP = grow command zone (less battlefield above)
                        playerInfoHeightDp = (playerInfoHeightDp - deltaDp).coerceIn(32f, 200f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Visual handle indicator
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            )
        }

        // Player info bar (command zone) — resizable via handle 2
        PlayerInfoBar(
            player = player,
            gameViewModel = gameViewModel,
            commandZoneCards = commandZoneCards,
            isActivePlayer = isActivePlayer,
            heightDp = playerInfoHeightDp,
            onCardAction = onCardAction,
            onShowLibrary = onShowLibrary,
            onShowGraveyard = onShowGraveyard,
            onShowExile = onShowExile,
            isCardBeingDragged = draggedCardInstanceId != null,
            dragY = dragPositionY,
            onGraveyardBoundsChanged = { top, bottom -> graveyardButtonTop = top; graveyardButtonBottom = bottom },
            onExileBoundsChanged = { top, bottom -> exileButtonTop = top; exileButtonBottom = bottom },
            onDropOnGraveyard = {
                draggedCardInstanceId?.let { cardId ->
                    gameViewModel.moveCard(cardId, Zone.GRAVEYARD)
                    draggedCardInstanceId = null
                }
            },
            onDropOnExile = {
                draggedCardInstanceId?.let { cardId ->
                    gameViewModel.moveCard(cardId, Zone.EXILE)
                    draggedCardInstanceId = null
                }
            },
            onGrabTopFromLibrary = {
                // Draw the top card from library to hand
                gameViewModel.drawCard(player.id)
            },
            onGrabTopFromGraveyard = {
                // Move top card of graveyard to hand
                val topCard = gameViewModel.getCards(player.id, Zone.GRAVEYARD).lastOrNull()
                topCard?.let { gameViewModel.moveCard(it.instanceId, Zone.HAND) }
            },
            onGrabTopFromExile = {
                // Move top card of exile to hand
                val topCard = gameViewModel.getCards(player.id, Zone.EXILE).lastOrNull()
                topCard?.let { gameViewModel.moveCard(it.instanceId, Zone.HAND) }
            }
        )

        // ── Handle 3: Between PlayerInfoBar and hand strip ──
        // Drag UP = bigger hand, drag DOWN = smaller hand
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        val deltaDp = with(density) { dragAmount.y.toDp().value }
                        handHeightDp = (handHeightDp - deltaDp).coerceIn(40f, 250f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            )
        }

        // Hand
        HandStrip(
            handCards = handCards,
            onCardClick = { card ->
                // Play to battlefield on double-tap
                onCardFocus(card)
                gameViewModel.moveCard(card.instanceId, Zone.BATTLEFIELD)
            },
            onCardLongPress = { card ->
                onCardFocus(card)
                onShowContextMenu(card)
            },
            onCardFocus = onCardFocus,
            onViewHand = onShowHand,
            onReorderCard = { cardId, newPosition ->
                gameViewModel.reorderHandCard(cardId, newPosition)
            },
            battlefieldZoneBottom = battlefieldZoneBottom,
            onCardDroppedToBattlefield = { cardId ->
                gameViewModel.moveCard(cardId, Zone.BATTLEFIELD)
            },
            onZonePositioned = { top -> handZoneTop = top },
            modifier = Modifier
                .fillMaxWidth()
                .height(handHeightDp.dp)
        )
    }
}

/**
 * Data class to track card stacking info
 */
private data class CardStackInfo(
    val gridPos: Pair<Int, Int>,
    val stackIndex: Int  // 0 = bottom, 1 = middle, 2 = top
)

@Composable
internal fun BattlefieldGrid(
    cards: List<CardInstance>,
    gridPositions: Map<String, Pair<Int, Int>>,
    players: List<Player>,
    selectionState: SelectionState,
    onCardClick: (CardInstance) -> Unit,
    onCardDoubleClick: (CardInstance) -> Unit,
    onCardLongPress: (CardInstance) -> Unit,
    onCardPositionChanged: ((String, Int, Int) -> Unit)? = null,
    handZoneTop: Float = 0f,
    onCardDroppedToHand: ((String) -> Unit)? = null,
    onCardDragStateChanged: ((cardId: String?, dragY: Float) -> Unit)? = null,
    graveyardBounds: Pair<Float, Float> = 0f to 0f,
    exileBounds: Pair<Float, Float> = 0f to 0f,
    onCardDroppedToGraveyard: ((String) -> Unit)? = null,
    onCardDroppedToExile: ((String) -> Unit)? = null
) {
    val cardSize = 70.dp
    // Spacing accounts for max stack extension: 2 * 10% * 70dp = ~14dp, plus buffer
    val spacing = 16.dp
    val density = LocalDensity.current

    // Track which card is being dragged and its offset
    var draggingCard by remember { mutableStateOf<CardInstance?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartCol by remember { mutableStateOf(0) }
    var dragStartRow by remember { mutableStateOf(0) }

    // Track grid position for zone detection
    var gridPositionInWindow by remember { mutableStateOf(Offset.Zero) }

    // Calculate card positions for drop target detection
    val cardSizePx = with(density) { cardSize.toPx() }
    val spacingPx = with(density) { spacing.toPx() }
    val cellSize = cardSizePx + spacingPx

    // Stack offset (10% of card size for visual separation)
    val stackOffsetPx = cardSizePx * UIConstants.STACK_OFFSET_RATIO

    // Calculate stack indices for visual stacking
    val stackInfoMap = remember(gridPositions, cards) {
        val stackInfo = mutableMapOf<String, CardStackInfo>()

        // Group cards by position
        val positionToCards = mutableMapOf<Pair<Int, Int>, MutableList<String>>()
        gridPositions.forEach { (cardId, pos) ->
            positionToCards.getOrPut(pos) { mutableListOf() }.add(cardId)
        }

        positionToCards.forEach { (gridPos, cardIds) ->
            // Sort cards by placement timestamp to maintain stack order
            val sortedCardIds = cardIds.sortedBy { cardId ->
                cards.find { it.instanceId == cardId }?.placedTimestamp ?: 0L
            }

            sortedCardIds.forEachIndexed { index, cardId ->
                stackInfo[cardId] = CardStackInfo(
                    gridPos = gridPos,
                    stackIndex = index.coerceAtMost(2) // Visual limit of 3 visible
                )
            }
        }

        stackInfo
    }

    // Fixed 3 rows: lands (bottom), artifacts/enchantments (middle), creatures (top)
    val totalRows = UIConstants.BATTLEFIELD_ROWS

    // Pinch-to-zoom and pan state using transformable (handles multi-touch better)
    var scale by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(0.5f, 5f)
        scale = newScale
        panOffset = Offset(
            x = panOffset.x + panChange.x,
            y = panOffset.y + panChange.y
        )
    }

    // Use Box with absolute positioning and zoom/pan support
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(((totalRows * cellSize) / density.density).dp)
            .onGloballyPositioned { coordinates ->
                gridPositionInWindow = coordinates.positionInWindow()
            }
            .transformable(state = transformState, lockRotationOnZoomPan = true)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = panOffset.x
                translationY = panOffset.y
                // Anchor scaling to top-left
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
            }
    ) {
        cards.forEach { card ->
            // Use card's actual gridX/gridY if set, otherwise use computed position
            val position = gridPositions[card.instanceId] ?: Pair(0, 0)
            val (col, row) = position
            val stackInfo = stackInfoMap[card.instanceId]
            val isDragging = draggingCard?.instanceId == card.instanceId

            // Base pixel position
            var xPos = col * cellSize
            var yPos = row * cellSize

            // Apply stacking offset for visual separation
            if (stackInfo != null) {
                xPos += stackInfo.stackIndex * stackOffsetPx
                yPos += stackInfo.stackIndex * stackOffsetPx
            }

            val finalOffset = if (isDragging) {
                IntOffset(
                    (xPos + dragOffset.x).roundToInt(),
                    (yPos + dragOffset.y).roundToInt()
                )
            } else {
                IntOffset(xPos.roundToInt(), yPos.roundToInt())
            }

            // Z-index: dragging cards on top, otherwise based on row, col, and stack index
            val zIndex = if (isDragging) {
                100f
            } else {
                (row * 1000 + col * 10 + (stackInfo?.stackIndex ?: 0)).toFloat()
            }

            key(card.instanceId) {
                // Look up owner name if owner != controller
                val ownerName = if (card.ownerId != card.controllerId) {
                    players.find { it.id == card.ownerId }?.name ?: ""
                } else ""

                Box(
                    modifier = Modifier
                        .offset { finalOffset }
                        .size(cardSize)
                        .zIndex(zIndex)
                ) {
                    DraggableBattlefieldCard(
                        cardInstance = card,
                        isSelected = selectionState.isSelected(card.instanceId),
                        isDragging = isDragging,
                        onClick = { onCardClick(card) },
                        onLongClick = { onCardLongPress(card) },
                        onDoubleClick = { onCardDoubleClick(card) },
                        ownerName = ownerName,
                        onDragStart = {
                            draggingCard = card
                            dragStartCol = col
                            dragStartRow = row
                            dragOffset = Offset.Zero
                            onCardDragStateChanged?.invoke(card.instanceId, gridPositionInWindow.y + row * cellSize)
                        },
                        onDrag = { offset ->
                            dragOffset += offset
                            val absoluteY = gridPositionInWindow.y + row * cellSize + dragOffset.y
                            onCardDragStateChanged?.invoke(draggingCard?.instanceId, absoluteY)
                        },
                        onDragEnd = {
                            onCardDragStateChanged?.invoke(null, 0f)
                            draggingCard?.let { dragCard ->
                                // Calculate absolute Y position of the card
                                val cardYInGrid = row * cellSize + dragOffset.y
                                val absoluteY = gridPositionInWindow.y + cardYInGrid

                                // Check if dropped on graveyard zone button
                                if (graveyardBounds.first > 0f && absoluteY in graveyardBounds.first..graveyardBounds.second && onCardDroppedToGraveyard != null) {
                                    onCardDroppedToGraveyard(dragCard.instanceId)
                                // Check if dropped on exile zone button
                                } else if (exileBounds.first > 0f && absoluteY in exileBounds.first..exileBounds.second && onCardDroppedToExile != null) {
                                    onCardDroppedToExile(dragCard.instanceId)
                                // Check if dropped in hand zone
                                } else if (handZoneTop > 0f && absoluteY > handZoneTop && onCardDroppedToHand != null) {
                                    // Dropped in hand zone - move to hand
                                    onCardDroppedToHand(dragCard.instanceId)
                                } else {
                                    // Normal battlefield repositioning
                                    val cellsMoveX = (dragOffset.x / cellSize).let {
                                        if (it >= 0) (it + 0.5f).toInt() else (it - 0.5f).toInt()
                                    }
                                    val cellsMoveY = (dragOffset.y / cellSize).let {
                                        if (it >= 0) (it + 0.5f).toInt() else (it - 0.5f).toInt()
                                    }

                                    val targetCol = (dragStartCol + cellsMoveX).coerceAtLeast(0)
                                    val targetRow = (dragStartRow + cellsMoveY).coerceAtLeast(0).coerceAtMost(UIConstants.BATTLEFIELD_ROWS - 1)

                                    // Report user intent to ViewModel - it will enforce the 3-card stack limit
                                    onCardPositionChanged?.invoke(dragCard.instanceId, targetCol, targetRow)
                                }
                            }

                            draggingCard = null
                            dragOffset = Offset.Zero
                        },
                        onDragCancel = {
                            draggingCard = null
                            dragOffset = Offset.Zero
                        }
                    )
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

    // Use a consistent fixed-size container for ALL cards - critical for proper touch targets
    // All cards get the same size container regardless of tap state
    // Key pointerInput by cardInstance.instanceId to ensure correct card is targeted
    Box(
        modifier = Modifier
            .size(70.dp) // Square container for all cards - allows rotated cards to fit
            .pointerInput(cardInstance.instanceId) {
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
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner card that rotates - use graphicsLayer to prevent clipping when rotated
        Box(
            modifier = Modifier
                .size(width = 50.dp, height = 70.dp)
                .graphicsLayer {
                    rotationZ = if (cardInstance.isTapped) 90f else 0f
                    clip = false
                }
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
        ) {
            CardImage(
                imageUrl = when {
                    cardInstance.isFaceDown -> null
                    cardInstance.isFlipped -> cardInstance.card.backFaceImageUri ?: "https://cards.scryfall.io/back.png"
                    else -> cardInstance.card.imageUri
                },
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
}

/**
 * Battlefield card that supports drag gesture for repositioning.
 * Uses combined gesture detection for tap, long-press, and drag.
 */
@Composable
private fun DraggableBattlefieldCard(
    cardInstance: CardInstance,
    isSelected: Boolean,
    isDragging: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    ownerName: String = ""
) {
    var lastClickTime by remember { mutableStateOf(0L) }
    var isDragInProgress by remember { mutableStateOf(false) }

    // Use square container for consistent touch target
    // Key by instanceId AND grid position so closures update when position changes
    Box(
        modifier = Modifier
            .size(70.dp)
            .pointerInput(cardInstance.instanceId, cardInstance.gridX, cardInstance.gridY) {
                // Custom drag gesture that cancels when multi-touch is detected
                // This allows pinch-to-zoom to work without card drag interfering
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragStarted = false

                    try {
                        drag(down.id) { change ->
                            // Check if there are multiple pointers - if so, cancel drag for pinch-to-zoom
                            val currentEvent = currentEvent
                            if (currentEvent.changes.size > 1) {
                                // Multi-touch detected - cancel drag and let parent handle pinch
                                if (dragStarted) {
                                    isDragInProgress = false
                                    onDragCancel()
                                }
                                throw kotlin.coroutines.cancellation.CancellationException("Multi-touch detected")
                            }

                            if (!dragStarted) {
                                dragStarted = true
                                isDragInProgress = true
                                onDragStart()
                            }

                            val dragAmount = change.positionChange()
                            if (dragAmount != Offset.Zero) {
                                change.consume()
                                onDrag(dragAmount)
                            }
                        }

                        // Drag completed successfully
                        if (dragStarted) {
                            isDragInProgress = false
                            onDragEnd()
                        }
                    } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                        // Drag was cancelled (multi-touch or other reason)
                        if (dragStarted) {
                            isDragInProgress = false
                            onDragCancel()
                        }
                    }
                }
            }
            .pointerInput(cardInstance.instanceId, cardInstance.gridX, cardInstance.gridY) {
                detectTapGestures(
                    onTap = {
                        if (!isDragInProgress) {
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime < UIConstants.DOUBLE_CLICK_DELAY_MS) {
                                onDoubleClick()
                                lastClickTime = 0L
                            } else {
                                onClick()
                                lastClickTime = now
                            }
                        }
                    },
                    onLongPress = {
                        if (!isDragInProgress) {
                            onLongClick()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner card that rotates - use graphicsLayer to prevent clipping when rotated
        Box(
            modifier = Modifier
                .size(width = 50.dp, height = 70.dp)
                .graphicsLayer {
                    rotationZ = if (cardInstance.isTapped) 90f else 0f
                    clip = false
                }
                .clip(RoundedCornerShape(4.dp))
                .border(
                    width = when {
                        isDragging -> 3.dp
                        isSelected -> 2.dp
                        else -> 1.dp
                    },
                    color = when {
                        isDragging -> Color.Yellow
                        isSelected -> MaterialTheme.colorScheme.primary
                        cardInstance.isTapped -> Color.Gray
                        else -> Color.Transparent
                    },
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            CardImage(
                imageUrl = when {
                    cardInstance.isFaceDown -> null
                    cardInstance.isFlipped -> cardInstance.card.backFaceImageUri ?: "https://cards.scryfall.io/back.png"
                    else -> cardInstance.card.imageUri
                },
                contentDescription = cardInstance.card.name,
                modifier = Modifier.fillMaxSize()
            )

            // Check if owner != controller
            val showOwnerTag = cardInstance.ownerId != cardInstance.controllerId && ownerName.isNotEmpty()

            // Top-left column for status overlays
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // Owner tag (only show if controller != owner)
                if (showOwnerTag) {
                    Box(
                        modifier = Modifier
                            .background(Color.Blue.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = ownerName,
                            color = Color.White,
                            fontSize = 6.sp,
                            maxLines = 1
                        )
                    }
                }

                // Counters - show each type with color
                cardInstance.counters.forEach { (counterTypeId, count) ->
                    val counterType = UIConstants.COUNTER_TYPES.find { it.id == counterTypeId }
                    val counterColor = counterType?.color ?: Color.White
                    Box(
                        modifier = Modifier
                            .background(counterColor.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "$count",
                            color = Color.Black,
                            fontSize = 8.sp
                        )
                    }
                }

                // "Doesn't Untap" indicator
                if (cardInstance.doesntUntap) {
                    Box(
                        modifier = Modifier
                            .background(Color.Magenta.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "⊘",
                            color = Color.White,
                            fontSize = 8.sp
                        )
                    }
                }

                // Annotation indicator
                if (!cardInstance.annotation.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .background(Color.Yellow.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "📝",
                            color = Color.Black,
                            fontSize = 8.sp
                        )
                    }
                }

                // Clone indicator
                if (cardInstance.isClone) {
                    Box(
                        modifier = Modifier
                            .background(Color.Cyan.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "Copy",
                            color = Color.Black,
                            fontSize = 6.sp
                        )
                    }
                }

                // Token indicator
                if (cardInstance.isToken) {
                    Box(
                        modifier = Modifier
                            .background(Color.Green.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "Token",
                            color = Color.Black,
                            fontSize = 6.sp
                        )
                    }
                }
            }

            // P/T indicator (bottom right for creatures)
            if (!cardInstance.isFaceDown) {
                val basePower = cardInstance.card.power
                val baseToughness = cardInstance.card.toughness
                if (basePower != null && baseToughness != null) {
                    val currentPower = (basePower.toIntOrNull() ?: 0) + cardInstance.powerModifier
                    val currentToughness = (baseToughness.toIntOrNull() ?: 0) + cardInstance.toughnessModifier
                    val isModified = cardInstance.powerModifier != 0 || cardInstance.toughnessModifier != 0

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = "$currentPower/$currentToughness",
                            color = when {
                                !isModified -> Color.White
                                cardInstance.powerModifier > 0 || cardInstance.toughnessModifier > 0 -> Color.Green
                                else -> Color.Red
                            },
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlayerInfoBar(
    player: Player,
    gameViewModel: GameViewModel,
    commandZoneCards: List<CardInstance>,
    isActivePlayer: Boolean,
    onCardAction: (CardAction) -> Unit,
    onShowLibrary: () -> Unit,
    onShowGraveyard: () -> Unit,
    onShowExile: () -> Unit,
    isCardBeingDragged: Boolean = false,
    dragY: Float = 0f,
    onGraveyardBoundsChanged: (top: Float, bottom: Float) -> Unit = { _, _ -> },
    onExileBoundsChanged: (top: Float, bottom: Float) -> Unit = { _, _ -> },
    onDropOnGraveyard: () -> Unit = {},
    onDropOnExile: () -> Unit = {},
    onGrabTopFromLibrary: () -> Unit = {},
    onGrabTopFromGraveyard: () -> Unit = {},
    onGrabTopFromExile: () -> Unit = {},
    heightDp: Float = 48f
) {
    val libraryCount = gameViewModel.getCardCount(player.id, Zone.LIBRARY)
    val graveyardCount = gameViewModel.getCardCount(player.id, Zone.GRAVEYARD)
    val exileCount = gameViewModel.getCardCount(player.id, Zone.EXILE)

    // Track zone button bounds for drop target highlighting
    var graveyardButtonTopLocal by remember { mutableStateOf(0f) }
    var graveyardButtonBottomLocal by remember { mutableStateOf(0f) }
    var exileButtonTopLocal by remember { mutableStateOf(0f) }
    var exileButtonBottomLocal by remember { mutableStateOf(0f) }

    // Scale factor based on bar height (default 48dp)
    val scaleFactor = (heightDp / 48f).coerceIn(0.5f, 4f)
    val commanderCardWidth = (30 * scaleFactor).dp
    val commanderCardHeight = (42 * scaleFactor).dp
    val zoneIconSize = (13 * scaleFactor).coerceIn(10f, 36f).dp
    val zoneTextSize = (9 * scaleFactor).coerceIn(7f, 20f).sp

    // Active player gets a highlighted bar color
    val barColor = if (isActivePlayer) Color(0xFF4CAF50) else Color(0xFF2E7D32)

    Surface(
        color = barColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .then(
                if (isActivePlayer) {
                    Modifier.border(2.dp, Color.Yellow)
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Life counter with long-press for exact value
            var showSetLifeDialog by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { gameViewModel.updateLife(player.id, player.life - 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "${player.life}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (player.life <= 10) Color.Red else Color.White,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { showSetLifeDialog = true }
                        )
                    }
                )
                IconButton(
                    onClick = { gameViewModel.updateLife(player.id, player.life + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, "Increase life", tint = Color.White)
                }
            }

            // Mana pool display
            val manaColors = listOf(
                "manaW" to Color(0xFFF9FAF4),
                "manaU" to Color(0xFF0E68AB),
                "manaB" to Color(0xFF150B00),
                "manaR" to Color(0xFFD3202A),
                "manaG" to Color(0xFF00733E),
                "manaC" to Color(0xFF808080)
            )
            val hasMana = manaColors.any { (key, _) -> (player.counters[key] ?: 0) > 0 }
            if (hasMana) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    manaColors.forEach { (key, color) ->
                        val count = player.counters[key] ?: 0
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(color, CircleShape)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$count",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (key == "manaW") Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Set life dialog (Issue 13)
            if (showSetLifeDialog) {
                var lifeInput by remember { mutableStateOf("${player.life}") }
                AlertDialog(
                    onDismissRequest = { showSetLifeDialog = false },
                    title = { Text("Set Life Total") },
                    text = {
                        OutlinedTextField(
                            value = lifeInput,
                            onValueChange = { lifeInput = it.filter { c -> c.isDigit() || c == '-' } },
                            label = { Text("Life") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            lifeInput.toIntOrNull()?.let { newLife ->
                                gameViewModel.updateLife(player.id, newLife)
                            }
                            showSetLifeDialog = false
                        }) { Text("Set") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSetLifeDialog = false }) { Text("Cancel") }
                    }
                )
            }

            // Commander — scales with bar height
            if (commandZoneCards.isNotEmpty()) {
                Row {
                    commandZoneCards.forEach { commander ->
                        Box(
                            modifier = Modifier
                                .size(width = commanderCardWidth, height = commanderCardHeight)
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

            // Zone counts with icons — scale with bar height
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Library — shows MTG card back image
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { gameViewModel.drawCard(player.id) },
                            onLongPress = { onShowLibrary() }
                        )
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = commanderCardWidth * 0.6f, height = commanderCardHeight * 0.6f)
                            .clip(RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CardImage(
                            imageUrl = "https://cards.scryfall.io/back.png",
                            contentDescription = "Library",
                            modifier = Modifier.fillMaxSize()
                        )
                        // Card count overlaid on card back
                        Text(
                            libraryCount.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (zoneTextSize.value * 0.8f).sp
                        )
                    }
                }
                ZoneIconButton(
                    iconResId = R.drawable.ic_graveyard,
                    fallbackLabel = "G",
                    count = graveyardCount,
                    iconSize = zoneIconSize,
                    textSize = zoneTextSize,
                    onClick = onShowGraveyard,
                    onLongClick = onShowGraveyard,
                    isDropHighlighted = isCardBeingDragged && dragY in graveyardButtonTopLocal..graveyardButtonBottomLocal,
                    onBoundsChanged = { top, bottom ->
                        graveyardButtonTopLocal = top
                        graveyardButtonBottomLocal = bottom
                        onGraveyardBoundsChanged(top, bottom)
                    },
                    onDragTopCard = onGrabTopFromGraveyard
                )
                ZoneIconButton(
                    iconResId = R.drawable.ic_exile,
                    fallbackLabel = "E",
                    count = exileCount,
                    iconSize = zoneIconSize,
                    textSize = zoneTextSize,
                    onClick = onShowExile,
                    onLongClick = onShowExile,
                    isDropHighlighted = isCardBeingDragged && dragY in exileButtonTopLocal..exileButtonBottomLocal,
                    onBoundsChanged = { top, bottom ->
                        exileButtonTopLocal = top
                        exileButtonBottomLocal = bottom
                        onExileBoundsChanged(top, bottom)
                    },
                    onDragTopCard = onGrabTopFromExile
                )
            }
        }
    }
}

@Composable
private fun ZoneButton(
    label: String,
    count: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { onClick() },
                onLongPress = { onLongClick() }
            )
        }
    ) {
        Text(count.toString(), color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

@Composable
internal fun ZoneIconButton(
    iconResId: Int?,
    fallbackLabel: String,
    count: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isDropHighlighted: Boolean = false,
    onBoundsChanged: (top: Float, bottom: Float) -> Unit = { _, _ -> },
    onDragTopCard: (() -> Unit)? = null,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    textSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    val highlightColor = if (isDropHighlighted) Color.Yellow.copy(alpha = 0.4f) else Color.Transparent
    var isDragSource by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                if (isDragSource) Color.Cyan.copy(alpha = 0.4f) else highlightColor,
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .onGloballyPositioned { coordinates ->
                val pos = coordinates.positionInWindow()
                onBoundsChanged(pos.y, pos.y + coordinates.size.height)
            }
            .pointerInput(onDragTopCard) {
                if (onDragTopCard != null) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                        val startTime = System.currentTimeMillis()

                        // Wait for pointer up or drag
                        var dragged = false
                        var released = false
                        while (!dragged && !released) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull() ?: break

                            if (change.changedToUp()) {
                                released = true
                                val elapsed = System.currentTimeMillis() - startTime
                                if (elapsed < longPressTimeout) {
                                    onClick()
                                } else {
                                    onLongClick()
                                }
                                change.consume()
                            } else {
                                val dragDist = (change.position - down.position).getDistance()
                                val elapsed = System.currentTimeMillis() - startTime
                                if (dragDist > 20f && elapsed >= longPressTimeout) {
                                    // Long-press + drag detected — grab top card
                                    isDragSource = true
                                    onDragTopCard()
                                    isDragSource = false
                                    dragged = true
                                    change.consume()
                                }
                            }
                        }
                    }
                } else {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongClick() }
                    )
                }
            }
    ) {
        if (iconResId != null) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = fallbackLabel,
                tint = when {
                    isDragSource -> Color.Cyan
                    isDropHighlighted -> Color.Yellow
                    else -> Color.Unspecified // preserve vector's own colors
                },
                modifier = Modifier.size(iconSize)
            )
            // Label below icon, above count — tight spacing
            val labelSize = (textSize.value * 0.65f).sp
            Text(
                fallbackLabel,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = labelSize,
                lineHeight = labelSize
            )
        } else {
            Text(fallbackLabel, color = Color.White.copy(alpha = 0.7f), fontSize = textSize, fontWeight = FontWeight.Bold)
        }
        Text(
            count.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = textSize,
            lineHeight = textSize
        )
    }
}

@Composable
internal fun HandStrip(
    handCards: List<CardInstance>,
    onCardClick: (CardInstance) -> Unit,
    onCardLongPress: (CardInstance) -> Unit,
    onCardFocus: (CardInstance) -> Unit = {},
    onViewHand: () -> Unit = {},
    onReorderCard: (String, Int) -> Unit = { _, _ -> },
    battlefieldZoneBottom: Float = 0f,
    onCardDroppedToBattlefield: (String) -> Unit = {},
    onZonePositioned: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Drag state for reordering
    var draggedCardId by remember { mutableStateOf<String?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    // Card dimensions for calculating position
    val cardWidth = 60.dp
    val cardSpacing = (-20).dp // Overlap
    val density = LocalDensity.current
    val effectiveCardWidth = with(density) { (cardWidth + cardSpacing).toPx() }

    // Track hand zone position
    var handZonePositionY by remember { mutableStateOf(0f) }

    Surface(
        color = Color(0xFF1565C0),
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                handZonePositionY = coordinates.positionInWindow().y
                onZonePositioned(handZonePositionY)
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(cardSpacing),
                contentPadding = PaddingValues(start = 8.dp, end = 50.dp, top = 4.dp, bottom = 4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onViewHand() }
                        )
                    }
            ) {
                items(handCards.size, key = { handCards[it].instanceId }) { index ->
                    val card = handCards[index]
                    val isDragging = draggedCardId == card.instanceId

                    HandCard(
                        cardInstance = card,
                        onSingleClick = { onCardFocus(card) },
                        onDoubleClick = { onCardClick(card) },
                        onLongClick = { onCardLongPress(card) },
                        isDragging = isDragging,
                        dragOffsetX = if (isDragging) dragOffsetX else 0f,
                        dragOffsetY = if (isDragging) dragOffsetY else 0f,
                        onDragStart = { draggedCardId = card.instanceId },
                        onDrag = { deltaX, deltaY ->
                            dragOffsetX += deltaX
                            dragOffsetY += deltaY
                        },
                        onDragEnd = {
                            if (draggedCardId != null) {
                                // Calculate absolute Y position
                                val absoluteY = handZonePositionY + dragOffsetY

                                // Check if dragged upward into battlefield zone
                                if (battlefieldZoneBottom > 0f && absoluteY < battlefieldZoneBottom) {
                                    // Dropped in battlefield zone
                                    onCardDroppedToBattlefield(card.instanceId)
                                } else if (handCards.size > 1) {
                                    // Reorder within hand
                                    val positionDelta = (dragOffsetX / effectiveCardWidth).roundToInt()
                                    if (positionDelta != 0) {
                                        val newPosition = (index + positionDelta).coerceIn(0, handCards.size - 1)
                                        onReorderCard(card.instanceId, newPosition)
                                    }
                                }
                            }
                            draggedCardId = null
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                        }
                    )
                }
            }

            // Hand count badge with hand icon
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .clickable { onViewHand() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_hand),
                    contentDescription = "Hand",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "${handCards.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
internal fun HandCard(
    cardInstance: CardInstance,
    onSingleClick: () -> Unit = {},
    onDoubleClick: () -> Unit,
    onLongClick: () -> Unit,
    isDragging: Boolean = false,
    dragOffsetX: Float = 0f,
    dragOffsetY: Float = 0f,
    onDragStart: () -> Unit = {},
    onDrag: (Float, Float) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {}
) {
    var lastClickTime by remember { mutableStateOf(0L) }

    Box(
        modifier = Modifier
            .size(width = 60.dp, height = 90.dp)
            .graphicsLayer {
                if (isDragging) {
                    translationX = dragOffsetX
                    translationY = dragOffsetY
                    scaleX = 1.1f
                    scaleY = 1.1f
                    alpha = 0.8f
                }
            }
            .zIndex(if (isDragging) 10f else 0f)
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = if (isDragging) 2.dp else 1.dp,
                color = if (isDragging) Color.Yellow else Color.White,
                shape = RoundedCornerShape(4.dp)
            )
            .pointerInput(cardInstance.instanceId) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .pointerInput(cardInstance.instanceId) {
                detectTapGestures(
                    onTap = {
                        val now = System.currentTimeMillis()
                        if (now - lastClickTime < UIConstants.DOUBLE_CLICK_DELAY_MS) {
                            onDoubleClick()
                            lastClickTime = 0L // Reset to prevent triple-click triggering
                        } else {
                            onSingleClick()
                            lastClickTime = now
                        }
                    },
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
internal fun BottomActionBar(
    activePlayer: Player?,
    localPlayer: Player?,
    gameViewModel: GameViewModel,
    onShowDieRoller: () -> Unit,
    onShowTokenCreation: () -> Unit,
    onShowMana: () -> Unit = {}
) {

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isActivePlayer = activePlayer != null && localPlayer != null && activePlayer.id == localPlayer.id
            IconButton(
                onClick = { if (isActivePlayer) gameViewModel.passTurn() },
                enabled = isActivePlayer
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pass_turn),
                    contentDescription = "Pass Turn",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = { localPlayer?.let { gameViewModel.untapAll(it.id) } }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_untap),
                    contentDescription = "Untap All",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = { localPlayer?.let { gameViewModel.drawCard(it.id) } }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_draw_card),
                    contentDescription = "Draw",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onShowTokenCreation) {
                Text("🪙", fontSize = 20.sp)
            }
            IconButton(onClick = onShowMana) {
                // Mana pool button - 5 colored dots
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    listOf(Color(0xFFF9FAF4), Color(0xFF0E68AB), Color(0xFF150B00), Color(0xFFD3202A), Color(0xFF00733E)).forEach { c ->
                        Box(Modifier.size(6.dp).background(c, CircleShape))
                    }
                }
            }
            IconButton(onClick = onShowDieRoller) {
                Text("🎲", fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun PausedOverlay(
    pauseReason: String?,
    isAdmin: Boolean,
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

                if (isAdmin) {
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

/**
 * Mana pool management dialog - tap to add, long-press to remove mana
 */
@Composable
internal fun ManaPoolDialog(
    player: Player,
    gameViewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val manaColors = listOf(
        Triple("manaW", "W", Color(0xFFF9FAF4)),
        Triple("manaU", "U", Color(0xFF0E68AB)),
        Triple("manaB", "B", Color(0xFF150B00)),
        Triple("manaR", "R", Color(0xFFD3202A)),
        Triple("manaG", "G", Color(0xFF00733E)),
        Triple("manaC", "C", Color(0xFF808080))
    )

    // Track which color is being edited for exact input
    var editingColor by remember { mutableStateOf<String?>(null) }
    var editingInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mana Pool") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tap count to set exact value", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    manaColors.forEach { (key, label, color) ->
                        val count = player.counters[key] ?: 0
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Color circle
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(color, CircleShape)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (key == "manaW") Color.Black else Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // + button
                            Text(
                                "+",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.clickable { gameViewModel.addMana(player.id, key) }
                            )
                            // Count — tap to edit exact value
                            if (editingColor == key) {
                                OutlinedTextField(
                                    value = editingInput,
                                    onValueChange = { editingInput = it.filter { c -> c.isDigit() } },
                                    modifier = Modifier.width(40.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                TextButton(
                                    onClick = {
                                        editingInput.toIntOrNull()?.let { value ->
                                            gameViewModel.setPlayerCounter(player.id, key, value.coerceAtLeast(0))
                                        }
                                        editingColor = null
                                    },
                                    modifier = Modifier.height(24.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("Set", fontSize = 10.sp) }
                            } else {
                                Text(
                                    "$count",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        editingColor = key
                                        editingInput = "$count"
                                    }
                                )
                            }
                            // - button
                            Text(
                                "-",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.clickable { gameViewModel.removeMana(player.id, key) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    gameViewModel.clearMana(player.id)
                }) { Text("Clear All") }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    )
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
