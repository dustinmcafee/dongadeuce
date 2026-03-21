package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.viewmodel.GameViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

/**
 * Player area for network mode - local player's full game view
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerArea(
    player: Player,
    viewModel: GameViewModel,
    gameState: GameState?,
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
    var showLibraryOperationsDialog by remember { mutableStateOf(false) }
    var showSetLifeDialog by remember { mutableStateOf(false) }

    val libraryCount = viewModel.getCardCount(player.id, Zone.LIBRARY)
    val handCount = viewModel.getCardCount(player.id, Zone.HAND)
    val graveyardCount = viewModel.getCardCount(player.id, Zone.GRAVEYARD)
    val exileCount = viewModel.getCardCount(player.id, Zone.EXILE)
    val commanderCount = viewModel.getCardCount(player.id, Zone.COMMAND_ZONE)
    val battlefieldCards = viewModel.getCards(player.id, Zone.BATTLEFIELD)
    val handCards = viewModel.getCards(player.id, Zone.HAND)
    val topCard = viewModel.getTopCards(player.id, 1).firstOrNull()

    // Compute grid positions for this player's battlefield (single source of truth)
    val gridPositions = remember(gameState, player.id) {
        gameState?.computeBattlefieldPositions(player.id) ?: emptyMap()
    }

    // Dialogs
    PlayerAreaDialogs(
        player = player,
        viewModel = viewModel,
        allPlayers = allPlayers,
        onCardAction = onCardAction,
        showHandDialog = showHandDialog,
        onDismissHand = { showHandDialog = false },
        showGraveyardDialog = showGraveyardDialog,
        onDismissGraveyard = { showGraveyardDialog = false },
        showExileDialog = showExileDialog,
        onDismissExile = { showExileDialog = false },
        showCommanderDamageDialog = showCommanderDamageDialog,
        onDismissCommanderDamage = { showCommanderDamageDialog = false },
        showLibrarySearchDialog = showLibrarySearchDialog,
        onDismissLibrarySearch = { showLibrarySearchDialog = false },
        showCommandZoneDialog = showCommandZoneDialog,
        onDismissCommandZone = { showCommandZoneDialog = false },
        showTokenCreationDialog = showTokenCreationDialog,
        onDismissTokenCreation = { showTokenCreationDialog = false },
        showSetLifeDialog = showSetLifeDialog,
        onDismissSetLife = { showSetLifeDialog = false }
    )

    // Library operations dialog (needs access to showLibrarySearchDialog state)
    if (showLibraryOperationsDialog) {
        LibraryOperationsDialog(
            playerName = player.name,
            librarySize = viewModel.getCardCount(player.id, Zone.LIBRARY),
            allPlayers = allPlayers,
            onDismiss = { showLibraryOperationsDialog = false },
            onViewTopCards = { showLibraryOperationsDialog = false; showLibrarySearchDialog = true },
            onViewBottomCards = { showLibraryOperationsDialog = false; showLibrarySearchDialog = true },
            onShuffleTopCards = { viewModel.shuffleLibrary(player.id) },
            onShuffleBottomCards = { viewModel.shuffleLibrary(player.id) },
            onMoveTopToZone = { count, zone ->
                viewModel.getTopCards(player.id, count).forEach { viewModel.moveCard(it.instanceId, zone) }
            },
            onMoveBottomToZone = { count, zone ->
                viewModel.getBottomCards(player.id, count).forEach { viewModel.moveCard(it.instanceId, zone) }
            },
            onRevealTopCard = { viewModel.toggleRevealTopCard(player.id) },
            onViewLibrary = { showLibraryOperationsDialog = false; showLibrarySearchDialog = true }
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
                gridPositions = gridPositions,
                isLocalPlayer = true,
                onCardClick = { viewModel.toggleTap(it.instanceId) },
                onContextAction = onCardAction,
                onCardPositionChanged = { cardId, gridX, gridY ->
                    viewModel.updateCardGridPosition(cardId, gridX, gridY)
                },
                modifier = Modifier.fillMaxSize().padding(8.dp),
                selectionState = selectionState,
                currentPlayerId = player.id,
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
        PlayerZonesRow(
            player = player,
            viewModel = viewModel,
            libraryCount = libraryCount,
            topCard = topCard,
            graveyardCount = graveyardCount,
            exileCount = exileCount,
            commanderCount = commanderCount,
            dragDropState = dragDropState,
            onShowSetLifeDialog = { showSetLifeDialog = true },
            onShowCommandZoneDialog = { showCommandZoneDialog = true },
            onShowLibrarySearchDialog = { showLibrarySearchDialog = true },
            onShowLibraryOperationsDialog = { showLibraryOperationsDialog = true },
            onShowGraveyardDialog = { showGraveyardDialog = true },
            onShowExileDialog = { showExileDialog = true },
            onShowCommanderDamageDialog = { showCommanderDamageDialog = true },
            onShowTokenCreationDialog = { showTokenCreationDialog = true },
            modifier = Modifier.fillMaxWidth().weight(0.3f).heightIn(min = 120.dp)
        )

        // Hand display
        PlayerHandDisplay(
            handCards = handCards,
            handCount = handCount,
            player = player,
            viewModel = viewModel,
            allPlayers = allPlayers,
            onCardAction = onCardAction,
            selectionState = selectionState,
            dragDropState = dragDropState,
            onShowHandDialog = { showHandDialog = true },
            modifier = Modifier.fillMaxWidth().weight(0.3f)
        )
    }
}

/**
 * All dialogs for the player area
 */
@Composable
private fun PlayerAreaDialogs(
    player: Player,
    viewModel: GameViewModel,
    allPlayers: List<Player>,
    onCardAction: (CardAction) -> Unit,
    showHandDialog: Boolean,
    onDismissHand: () -> Unit,
    showGraveyardDialog: Boolean,
    onDismissGraveyard: () -> Unit,
    showExileDialog: Boolean,
    onDismissExile: () -> Unit,
    showCommanderDamageDialog: Boolean,
    onDismissCommanderDamage: () -> Unit,
    showLibrarySearchDialog: Boolean,
    onDismissLibrarySearch: () -> Unit,
    showCommandZoneDialog: Boolean,
    onDismissCommandZone: () -> Unit,
    showTokenCreationDialog: Boolean,
    onDismissTokenCreation: () -> Unit,
    showSetLifeDialog: Boolean,
    onDismissSetLife: () -> Unit
) {
    if (showHandDialog) {
        HandDialog(
            cards = viewModel.getCards(player.id, Zone.HAND),
            onDismiss = onDismissHand,
            onPlayCard = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
                onDismissHand()
            },
            onDiscard = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.GRAVEYARD)
                onDismissHand()
            },
            onExile = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.EXILE)
                onDismissHand()
            },
            onToLibrary = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.LIBRARY)
                onDismissHand()
            },
            onContextAction = onCardAction
        )
    }

    if (showGraveyardDialog) {
        GraveyardDialog(
            cards = viewModel.getCards(player.id, Zone.GRAVEYARD),
            playerName = player.name,
            onDismiss = onDismissGraveyard,
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

    if (showExileDialog) {
        ExileDialog(
            cards = viewModel.getCards(player.id, Zone.EXILE),
            playerName = player.name,
            onDismiss = onDismissExile,
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

    if (showCommanderDamageDialog) {
        CommanderDamageDialog(
            players = allPlayers,
            commanders = viewModel.getAllCommanders(),
            onDismiss = onDismissCommanderDamage,
            onDamageChange = { playerId, commanderId, newDamage ->
                viewModel.updateCommanderDamage(playerId, commanderId, newDamage)
            }
        )
    }

    if (showLibrarySearchDialog) {
        LibrarySearchDialog(
            cards = viewModel.getCards(player.id, Zone.LIBRARY),
            playerName = player.name,
            onDismiss = onDismissLibrarySearch,
            onToHand = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.HAND)
                onDismissLibrarySearch()
            },
            onToBattlefield = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
                onDismissLibrarySearch()
            },
            onToTop = { cardInstance ->
                viewModel.moveCardToTopOfLibrary(cardInstance.instanceId)
                onDismissLibrarySearch()
            },
            onShuffle = {
                viewModel.shuffleLibrary(player.id)
            },
            onViewDetails = { cardInstance ->
                onCardAction(CardAction.ViewDetails(cardInstance))
            }
        )
    }

    if (showCommandZoneDialog) {
        CommandZoneDialog(
            cards = viewModel.getCards(player.id, Zone.COMMAND_ZONE),
            playerName = player.name,
            onDismiss = onDismissCommandZone,
            onCastToBattlefield = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
                onDismissCommandZone()
            },
            onToHand = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.HAND)
                onDismissCommandZone()
            }
        )
    }

    if (showTokenCreationDialog) {
        TokenCreationDialog(
            viewModel = viewModel,
            onDismiss = onDismissTokenCreation,
            onCreateToken = { tokenName, tokenType, power, toughness, color, imageUri, quantity, oracleText ->
                viewModel.createToken(player.id, tokenName, tokenType, power, toughness, color, imageUri, quantity, oracleText)
            }
        )
    }

    if (false) { // Removed — rendered in PlayerArea
        LibraryOperationsDialog(
            playerName = "",
            librarySize = 0,
            onDismiss = {},
            onViewTopCards = {},
            onViewBottomCards = {},
            onShuffleTopCards = {},
            onShuffleBottomCards = {},
            onMoveTopToZone = { _, _ -> },
            onMoveBottomToZone = { _, _ -> },
            onRevealTopCard = {}
        )
    }

    if (showSetLifeDialog) {
        SetLifeDialog(
            playerName = player.name,
            currentLife = player.life,
            onDismiss = onDismissSetLife,
            onConfirm = { newLife ->
                viewModel.updateLife(player.id, newLife)
            }
        )
    }
}

/**
 * Row containing player zones (commander, library, graveyard, exile) and info
 */
@Composable
private fun PlayerZonesRow(
    player: Player,
    viewModel: GameViewModel,
    libraryCount: Int,
    topCard: CardInstance?,
    graveyardCount: Int,
    exileCount: Int,
    commanderCount: Int,
    dragDropState: DragDropState?,
    onShowSetLifeDialog: () -> Unit,
    onShowCommandZoneDialog: () -> Unit,
    onShowLibrarySearchDialog: () -> Unit,
    onShowLibraryOperationsDialog: () -> Unit,
    onShowGraveyardDialog: () -> Unit,
    onShowExileDialog: () -> Unit,
    onShowCommanderDamageDialog: () -> Unit,
    onShowTokenCreationDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Commander zone
        ZoneCard(
            "Commander",
            Zone.COMMAND_ZONE,
            commanderCount,
            Modifier.width(120.dp).fillMaxHeight(),
            onClick = onShowCommandZoneDialog
        )

        // Player info and actions
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
                                modifier = Modifier.clickable { onShowSetLifeDialog() }
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        onClick = onShowCommanderDamageDialog,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Commander Damage")
                    }
                }

                OutlinedButton(
                    onClick = onShowTokenCreationDialog,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Token")
                }

                OutlinedButton(
                    onClick = { viewModel.markPlayerAsLost(player.id, "conceded") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Leave Game")
                }
            }
        }

        // Library, graveyard, exile
        Column(
            modifier = Modifier.width(200.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Determine the image to show: revealed card, looked-at card, or card back
            val libraryImageUrl = when {
                player.revealTopCard && topCard != null -> topCard.card.imageUri
                player.lookAtTopCard && topCard != null -> topCard.card.imageUri  // PlayerArea is always for local player
                else -> "https://cards.scryfall.io/back.png"  // Standard card back
            }

            ZoneCard(
                label = "Library",
                zone = Zone.LIBRARY,
                cardCount = libraryCount,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onClick = onShowLibrarySearchDialog,
                onRightClick = onShowLibraryOperationsDialog,
                dragDropState = dragDropState,
                onDropCards = { cardIds ->
                    dragDropState?.markHandledByZone()
                    cardIds.forEach { cardId ->
                        viewModel.moveCardToTopOfLibrary(cardId)
                    }
                    dragDropState?.endDrag()
                },
                imageUrl = libraryImageUrl
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
                    onClick = onShowGraveyardDialog,
                    dragDropState = dragDropState,
                    onDropCards = { cardIds ->
                        dragDropState?.markHandledByZone()
                        cardIds.forEach { cardId ->
                            viewModel.moveCard(cardId, Zone.GRAVEYARD)
                        }
                        dragDropState?.endDrag()
                    }
                )
                ZoneCard(
                    "Exile",
                    Zone.EXILE,
                    exileCount,
                    Modifier.weight(1f).fillMaxHeight(),
                    onClick = onShowExileDialog,
                    dragDropState = dragDropState,
                    onDropCards = { cardIds ->
                        dragDropState?.markHandledByZone()
                        cardIds.forEach { cardId ->
                            viewModel.moveCard(cardId, Zone.EXILE)
                        }
                        dragDropState?.endDrag()
                    }
                )
            }
        }
    }
}

/**
 * Player's hand display with cards
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerHandDisplay(
    handCards: List<CardInstance>,
    handCount: Int,
    player: Player,
    viewModel: GameViewModel,
    allPlayers: List<Player>,
    onCardAction: (CardAction) -> Unit,
    selectionState: SelectionState?,
    dragDropState: DragDropState?,
    onShowHandDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State for right-click context menu and View Hand dialog
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var showViewHandDialog by remember { mutableStateOf(false) }

    // Track hand area position for reordering calculations
    var handAreaPositionX by remember { mutableStateOf(0f) }
    var handAreaWidth by remember { mutableStateOf(0f) }

    // View Hand dialog
    if (showViewHandDialog) {
        ViewHandDialog(
            cards = handCards,
            playerName = player.name,
            allPlayers = allPlayers,
            onDismiss = { showViewHandDialog = false },
            onPlayCard = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
            },
            onDiscard = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.GRAVEYARD)
            },
            onExile = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.EXILE)
            },
            onToLibrary = { cardInstance ->
                viewModel.moveCard(cardInstance.instanceId, Zone.LIBRARY)
            },
            onContextAction = onCardAction,
            onRevealHandTo = { targetPlayerIds ->
                viewModel.revealHand(player.id, targetPlayerIds)
            }
        )
    }

    Card(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                // Register hand as a drop zone so battlefield cards can be dropped here
                if (dragDropState != null) {
                    val position = coordinates.positionInWindow()
                    val bounds = Rect(
                        left = position.x,
                        top = position.y,
                        right = position.x + coordinates.size.width,
                        bottom = position.y + coordinates.size.height
                    )
                    dragDropState.registerZoneBounds(Zone.HAND, bounds)
                }
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box {
            Column(modifier = Modifier.fillMaxSize()) {
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
                        Text("Hand ($handCount)", style = MaterialTheme.typography.titleSmall)
                        OutlinedButton(onClick = onShowHandDialog) {
                            Text("Expand")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Hand area with right-click support
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coordinates ->
                                // Track hand area position for reordering
                                handAreaPositionX = coordinates.positionInWindow().x
                                handAreaWidth = coordinates.size.width.toFloat()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.buttons.isSecondaryPressed && event.type == PointerEventType.Press) {
                                            contextMenuOffset = event.changes.first().position
                                            showContextMenu = true
                                        }
                                    }
                                }
                            }
                    ) {
                        if (handCards.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No cards in hand\n(Right-click for options)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            // Overlapping hand cards layout
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val density = LocalDensity.current
                                val availableWidth = with(density) { maxWidth.toPx() }
                                val cardWidthPx = with(density) { UIConstants.HAND_CARD_WIDTH.toPx() }
                                val minSpacing = with(density) { 4.dp.toPx() }

                                // Calculate spacing between cards
                                // If cards fit without overlap, use normal spacing
                                // Otherwise, calculate overlap to fit all cards
                                val totalCardWidth = cardWidthPx * handCards.size
                                val totalSpacingNeeded = minSpacing * (handCards.size - 1).coerceAtLeast(0)

                                val cardSpacingPx = if (totalCardWidth + totalSpacingNeeded <= availableWidth) {
                                    // Cards fit - use normal spacing
                                    cardWidthPx + minSpacing
                                } else {
                                    // Cards don't fit - calculate overlap
                                    // We want: firstCardPos + (n-1) * spacing + cardWidth = availableWidth
                                    // spacing = (availableWidth - cardWidth) / (n - 1)
                                    if (handCards.size > 1) {
                                        ((availableWidth - cardWidthPx) / (handCards.size - 1)).coerceAtLeast(20f)
                                    } else {
                                        cardWidthPx
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    handCards.forEachIndexed { index, cardInstance ->
                                        Box(
                                            modifier = Modifier
                                                .offset(x = with(density) { ((index * cardSpacingPx) - (index * cardWidthPx)).toDp() })
                                                .zIndex(index.toFloat())
                                        ) {
                                            HandCardDisplay(
                                                cardInstance = cardInstance,
                                                onCardClick = { onShowHandDialog() },
                                                onDoubleClick = {
                                                    viewModel.moveCard(cardInstance.instanceId, Zone.BATTLEFIELD)
                                                },
                                                onContextAction = onCardAction,
                                                selectionState = selectionState,
                                                allPlayers = allPlayers,
                                                dragDropState = dragDropState,
                                                onDropToZone = { cardIds, zone ->
                                                    cardIds.forEach { cardId ->
                                                        if (zone == Zone.LIBRARY) {
                                                            viewModel.moveCardToTopOfLibrary(cardId)
                                                        } else {
                                                            viewModel.moveCard(cardId, zone)
                                                        }
                                                    }
                                                },
                                                onReorderInHand = { cardId, dropXPosition ->
                                                    // Calculate target position based on drop X coordinate
                                                    // Use the same spacing calculation as the layout
                                                    val relativeX = dropXPosition - handAreaPositionX
                                                    val cardCount = handCards.size

                                                    // Calculate target index based on card spacing
                                                    val targetIndex = if (cardCount > 1 && cardSpacingPx > 0) {
                                                        // Divide by spacing to get approximate index
                                                        // Add half card width to center the drop zone on each card
                                                        ((relativeX + cardWidthPx / 2) / cardSpacingPx).toInt().coerceIn(0, cardCount - 1)
                                                    } else {
                                                        0
                                                    }
                                                    viewModel.reorderHandCard(cardId, targetIndex)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Batch actions row
                if (selectionState?.hasSelection == true) {
                    HandBatchActionsRow(
                        selectionState = selectionState,
                        viewModel = viewModel
                    )
                }
            }

            // Context menu dropdown
            val density = LocalDensity.current.density
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                offset = DpOffset(
                    x = (contextMenuOffset.x / density).dp,
                    y = (contextMenuOffset.y / density).dp
                )
            ) {
                DropdownMenuItem(
                    text = { Text("View Hand") },
                    onClick = {
                        showContextMenu = false
                        showViewHandDialog = true
                    }
                )
            }
        }
    }
}

/**
 * Batch actions for selected hand cards
 */
@Composable
private fun HandBatchActionsRow(
    selectionState: SelectionState,
    viewModel: GameViewModel
) {
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
