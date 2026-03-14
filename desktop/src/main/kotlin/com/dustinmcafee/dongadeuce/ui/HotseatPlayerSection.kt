package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.viewmodel.GameViewModel

/**
 * A complete player section for hotseat mode including hand, battlefield, and zone controls
 *
 * @param isActivePlayer Whether this player is the active player (whose turn it is) - controls turn-based UI
 * @param isLocalPlayer Whether this is the local player's section (for network mode) - controls hand visibility
 *                      In hotseat mode, this should match isActivePlayer. In network mode, this is the local player.
 */
@Composable
fun HotseatPlayerSection(
    player: Player,
    viewModel: GameViewModel,
    gameState: GameState?,
    isActivePlayer: Boolean,
    onCardAction: (CardAction) -> Unit,
    dragDropState: DragDropState? = null,
    selectionState: SelectionState? = null,
    allPlayers: List<Player> = emptyList(),
    modifier: Modifier = Modifier,
    inverted: Boolean = false, // If true, hand at bottom; if false, hand at top
    isLocalPlayer: Boolean = isActivePlayer, // Defaults to isActivePlayer for hotseat mode compatibility
    onCardFocus: ((CardInstance) -> Unit)? = null // Callback when card is hovered for persistent viewer
) {
    // Show hand cards if this is the local player (in network mode) or active player (in hotseat mode)
    val handCards = if (isLocalPlayer) viewModel.getCards(player.id, Zone.HAND) else emptyList()
    val handCount = viewModel.getCardCount(player.id, Zone.HAND)
    val battlefieldCards = viewModel.getCards(player.id, Zone.BATTLEFIELD)

    // Compute grid positions for this player's battlefield (single source of truth)
    val gridPositions = remember(gameState, player.id) {
        gameState?.computeBattlefieldPositions(player.id) ?: emptyMap()
    }
    val commandZoneCards = viewModel.getCards(player.id, Zone.COMMAND_ZONE)
    val libraryCount = viewModel.getCardCount(player.id, Zone.LIBRARY)
    val graveyardCount = viewModel.getCardCount(player.id, Zone.GRAVEYARD)
    val exileCount = viewModel.getCardCount(player.id, Zone.EXILE)
    val topCard = viewModel.getTopCards(player.id, 1).firstOrNull()

    var showGraveyardDialog by remember { mutableStateOf(false) }
    var showExileDialog by remember { mutableStateOf(false) }
    var showGraveyardViewerDialog by remember { mutableStateOf(false) } // Read-only for non-local player
    var showExileViewerDialog by remember { mutableStateOf(false) } // Read-only for non-local player
    var showLibrarySearchDialog by remember { mutableStateOf(false) }
    var showLibraryOperationsDialog by remember { mutableStateOf(false) }
    var showLibraryPeekDialog by remember { mutableStateOf(false) }
    var libraryPeekCards by remember { mutableStateOf<List<CardInstance>>(emptyList()) }
    var libraryPeekLocation by remember { mutableStateOf(PeekLocation.TOP) }
    var showTokenCreationDialog by remember { mutableStateOf(false) }
    var showSetLifeDialog by remember { mutableStateOf(false) }
    var showPlayerCountersDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clipToBounds()
            .background(
                if (isActivePlayer) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                else Color.Transparent
            ),
        verticalArrangement = if (inverted) Arrangement.Bottom else Arrangement.Top
    ) {
        if (!inverted) {
            // Hand at top (normal orientation)
            if (isLocalPlayer) {
                CompactHandStrip(
                    player = player,
                    handCards = handCards,
                    handCount = handCount,
                    showCards = true,
                    onCardAction = onCardAction,
                    viewModel = viewModel,
                    selectionState = selectionState,
                    dragDropState = dragDropState,
                    allPlayers = allPlayers,
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    onCardFocus = onCardFocus
                )
            }
            // Hand count now shown in sidebar for opponents
        }

        // Battlefield in center - light green for active player, dark green for inactive
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = if (isActivePlayer) Color(0xFF1B5E20) else Color(0xFF2E4A2E)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Player info sidebar
                PlayerInfoSidebar(
                    player = player,
                    viewModel = viewModel,
                    isLocalPlayer = isLocalPlayer,
                    handCount = handCount,
                    commandZoneCards = commandZoneCards,
                    libraryCount = libraryCount,
                    topCard = topCard,
                    graveyardCount = graveyardCount,
                    exileCount = exileCount,
                    onCardAction = onCardAction,
                    allPlayers = allPlayers,
                    dragDropState = dragDropState,
                    onShowSetLifeDialog = { showSetLifeDialog = true },
                    onShowPlayerCountersDialog = { showPlayerCountersDialog = true },
                    onShowGraveyardDialog = {
                        if (isLocalPlayer) {
                            showGraveyardDialog = true
                        } else {
                            showGraveyardViewerDialog = true
                        }
                    },
                    onShowExileDialog = {
                        if (isLocalPlayer) {
                            showExileDialog = true
                        } else {
                            showExileViewerDialog = true
                        }
                    },
                    onShowLibraryOperationsDialog = { showLibraryOperationsDialog = true },
                    onShowTokenCreationDialog = { showTokenCreationDialog = true },
                    modifier = Modifier.width(150.dp).fillMaxHeight().padding(4.dp),
                    onCardFocus = onCardFocus
                )

                // Battlefield cards
                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp)) {
                    DraggableBattlefieldGrid(
                        cards = battlefieldCards,
                        gridPositions = gridPositions,
                        isLocalPlayer = isLocalPlayer,
                        onCardClick = { viewModel.toggleTap(it.instanceId) },
                        onContextAction = onCardAction,
                        onCardPositionChanged = { cardId, gridX, gridY ->
                            viewModel.updateCardGridPosition(cardId, gridX, gridY)
                        },
                        modifier = Modifier.fillMaxSize(),
                        selectionState = selectionState,
                        currentPlayerId = if (isLocalPlayer) player.id else null,
                        allPlayers = allPlayers,
                        dragDropState = if (isLocalPlayer) dragDropState else null,
                        onDropToZone = if (isLocalPlayer) { cardIds, zone ->
                            cardIds.forEach { cardId ->
                                when (zone) {
                                    Zone.LIBRARY -> viewModel.moveCardToTopOfLibrary(cardId)
                                    else -> viewModel.moveCard(cardId, zone)
                                }
                            }
                        } else null,
                        // Invert rows for opponents across from us (at top of screen, inverted=false)
                        invertRows = !inverted,
                        onCardFocus = onCardFocus
                    )
                }
            }
            }
        }

        if (inverted) {
            // Hand at bottom (inverted orientation)
            if (isLocalPlayer) {
                CompactHandStrip(
                    player = player,
                    handCards = handCards,
                    handCount = handCount,
                    showCards = true,
                    onCardAction = onCardAction,
                    viewModel = viewModel,
                    selectionState = selectionState,
                    dragDropState = dragDropState,
                    allPlayers = allPlayers,
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    onCardFocus = onCardFocus
                )
            }
            // Hand count now shown in sidebar for opponents
        }
    }

    // Zone dialogs for local player (the one who can interact with their zones)
    if (isLocalPlayer) {
        HotseatPlayerDialogs(
            player = player,
            viewModel = viewModel,
            onCardAction = onCardAction,
            allPlayers = allPlayers,
            showGraveyardDialog = showGraveyardDialog,
            onDismissGraveyard = { showGraveyardDialog = false },
            showExileDialog = showExileDialog,
            onDismissExile = { showExileDialog = false },
            showLibrarySearchDialog = showLibrarySearchDialog,
            onDismissLibrarySearch = { showLibrarySearchDialog = false },
            showLibraryOperationsDialog = showLibraryOperationsDialog,
            onDismissLibraryOperations = { showLibraryOperationsDialog = false },
            showLibraryPeekDialog = showLibraryPeekDialog,
            onDismissLibraryPeek = { showLibraryPeekDialog = false },
            libraryPeekCards = libraryPeekCards,
            libraryPeekLocation = libraryPeekLocation,
            onLibraryPeekCardsChange = { libraryPeekCards = it },
            onShowLibraryPeek = { cards, location ->
                libraryPeekCards = cards
                libraryPeekLocation = location
                showLibraryPeekDialog = true
                showLibraryOperationsDialog = false
            },
            showTokenCreationDialog = showTokenCreationDialog,
            onDismissTokenCreation = { showTokenCreationDialog = false },
            showSetLifeDialog = showSetLifeDialog,
            onDismissSetLife = { showSetLifeDialog = false },
            showPlayerCountersDialog = showPlayerCountersDialog,
            onDismissPlayerCounters = { showPlayerCountersDialog = false }
        )
    }

    // Read-only zone viewer dialogs (for viewing other players' zones when not local player)
    if (showGraveyardViewerDialog) {
        GraveyardViewerDialog(
            cards = viewModel.getCards(player.id, Zone.GRAVEYARD),
            playerName = player.name,
            onDismiss = { showGraveyardViewerDialog = false },
            onViewDetails = { cardInstance -> onCardAction(CardAction.ViewDetails(cardInstance)) }
        )
    }

    if (showExileViewerDialog) {
        ExileViewerDialog(
            cards = viewModel.getCards(player.id, Zone.EXILE),
            playerName = player.name,
            onDismiss = { showExileViewerDialog = false },
            onViewDetails = { cardInstance -> onCardAction(CardAction.ViewDetails(cardInstance)) }
        )
    }
}

/**
 * Player info sidebar with life, counters, and zone controls
 */
@Composable
private fun PlayerInfoSidebar(
    player: Player,
    viewModel: GameViewModel,
    isLocalPlayer: Boolean,
    handCount: Int,
    commandZoneCards: List<CardInstance>,
    libraryCount: Int,
    topCard: CardInstance?,
    graveyardCount: Int,
    exileCount: Int,
    onCardAction: (CardAction) -> Unit,
    allPlayers: List<Player>,
    dragDropState: DragDropState?,
    onShowSetLifeDialog: () -> Unit,
    onShowPlayerCountersDialog: () -> Unit,
    onShowGraveyardDialog: () -> Unit,
    onShowExileDialog: () -> Unit,
    onShowLibraryOperationsDialog: () -> Unit,
    onShowTokenCreationDialog: () -> Unit,
    modifier: Modifier = Modifier,
    onCardFocus: ((CardInstance) -> Unit)? = null
) {
    // Determine the image to show for library: revealed card, looked-at card, or card back
    val libraryImageUrl = when {
        player.revealTopCard && topCard != null -> topCard.card.imageUri
        player.lookAtTopCard && isLocalPlayer && topCard != null -> topCard.card.imageUri
        else -> "https://cards.scryfall.io/back.png"  // Standard card back
    }

    // ALL elements use weights to guarantee they fit regardless of container size
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // Header: Name + Hand count (for opponents) + Life - weight 1
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    player.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                if (!isLocalPlayer) {
                    Text(
                        "($handCount)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            // Life row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "-",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.clickable { viewModel.updateLife(player.id, player.life - 1) }
                        .padding(horizontal = 8.dp)
                )
                Text(
                    "${player.life}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.clickable { onShowSetLifeDialog() }
                )
                Text(
                    "+",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.clickable { viewModel.updateLife(player.id, player.life + 1) }
                        .padding(horizontal = 8.dp)
                )
            }
        }

        // Counters - weight 0.8
        Box(
            modifier = Modifier.fillMaxWidth().weight(0.8f),
            contentAlignment = Alignment.Center
        ) {
            PlayerCountersDisplay(
                player = player,
                onClick = if (isLocalPlayer) onShowPlayerCountersDialog else null
            )
        }

        // Command Zone - weight 2
        Box(modifier = Modifier.fillMaxWidth().weight(2f)) {
            CommandZoneDisplay(
                commanderCards = commandZoneCards,
                isActivePlayer = isLocalPlayer,
                onCardAction = onCardAction,
                allPlayers = allPlayers,
                dragDropState = dragDropState,
                onDropCards = if (isLocalPlayer) {
                    { cardIds ->
                        dragDropState?.markHandledByZone()
                        cardIds.forEach { cardId ->
                            viewModel.moveCard(cardId, Zone.COMMAND_ZONE)
                        }
                        dragDropState?.endDrag()
                    }
                } else null,
                modifier = Modifier.fillMaxSize(),
                onCardFocus = onCardFocus
            )
        }

        // Library - weight 2
        // Determine if top card should be shown in persistent viewer on hover
        val libraryHoverCard = when {
            player.revealTopCard && topCard != null -> topCard
            player.lookAtTopCard && isLocalPlayer && topCard != null -> topCard
            else -> null
        }
        ZoneCard(
            "Library",
            Zone.LIBRARY,
            libraryCount,
            Modifier.fillMaxWidth().weight(2f),
            onClick = null,
            onDoubleClick = if (isLocalPlayer) ({ viewModel.drawCard(player.id) }) else null,
            onRightClick = if (isLocalPlayer) onShowLibraryOperationsDialog else null,
            dragDropState = if (isLocalPlayer) dragDropState else null,
            onDropCards = if (isLocalPlayer) {
                { cardIds ->
                    dragDropState?.markHandledByZone()
                    cardIds.forEach { cardId ->
                        viewModel.moveCardToTopOfLibrary(cardId)
                    }
                    dragDropState?.endDrag()
                }
            } else null,
            imageUrl = libraryImageUrl,
            hoverCard = libraryHoverCard,
            onCardFocus = onCardFocus
        )

        // Graveyard - weight 1.5
        ZoneCard(
            "Graveyard",
            Zone.GRAVEYARD,
            graveyardCount,
            Modifier.fillMaxWidth().weight(1.5f),
            onClick = onShowGraveyardDialog,
            dragDropState = if (isLocalPlayer) dragDropState else null,
            onDropCards = if (isLocalPlayer) {
                { cardIds ->
                    dragDropState?.markHandledByZone()
                    cardIds.forEach { cardId ->
                        viewModel.moveCard(cardId, Zone.GRAVEYARD)
                    }
                    dragDropState?.endDrag()
                }
            } else null
        )

        // Exile - weight 1.5
        ZoneCard(
            "Exile",
            Zone.EXILE,
            exileCount,
            Modifier.fillMaxWidth().weight(1.5f),
            onClick = onShowExileDialog,
            dragDropState = if (isLocalPlayer) dragDropState else null,
            onDropCards = if (isLocalPlayer) {
                { cardIds ->
                    dragDropState?.markHandledByZone()
                    cardIds.forEach { cardId ->
                        viewModel.moveCard(cardId, Zone.EXILE)
                    }
                    dragDropState?.endDrag()
                }
            } else null
        )

        // Token button (local player only) - weight 0.7
        if (isLocalPlayer) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(0.7f),
                contentAlignment = Alignment.Center
            ) {
                OutlinedButton(
                    onClick = onShowTokenCreationDialog,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("Token", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * All dialogs for the hotseat player section
 */
@Composable
private fun HotseatPlayerDialogs(
    player: Player,
    viewModel: GameViewModel,
    onCardAction: (CardAction) -> Unit,
    allPlayers: List<Player>,
    showGraveyardDialog: Boolean,
    onDismissGraveyard: () -> Unit,
    showExileDialog: Boolean,
    onDismissExile: () -> Unit,
    showLibrarySearchDialog: Boolean,
    onDismissLibrarySearch: () -> Unit,
    showLibraryOperationsDialog: Boolean,
    onDismissLibraryOperations: () -> Unit,
    showLibraryPeekDialog: Boolean,
    onDismissLibraryPeek: () -> Unit,
    libraryPeekCards: List<CardInstance>,
    libraryPeekLocation: PeekLocation,
    onLibraryPeekCardsChange: (List<CardInstance>) -> Unit,
    onShowLibraryPeek: (List<CardInstance>, PeekLocation) -> Unit,
    showTokenCreationDialog: Boolean,
    onDismissTokenCreation: () -> Unit,
    showSetLifeDialog: Boolean,
    onDismissSetLife: () -> Unit,
    showPlayerCountersDialog: Boolean,
    onDismissPlayerCounters: () -> Unit
) {
    val libraryCount = viewModel.getCardCount(player.id, Zone.LIBRARY)

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

    if (showLibrarySearchDialog) {
        LibrarySearchDialog(
            cards = viewModel.getCards(player.id, Zone.LIBRARY),
            playerName = player.name,
            onDismiss = onDismissLibrarySearch,
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
            allPlayers = allPlayers,
            onDismiss = onDismissLibraryOperations,
            onViewTopCards = { count ->
                onShowLibraryPeek(viewModel.getTopCards(player.id, count), PeekLocation.TOP)
            },
            onViewBottomCards = { count ->
                onShowLibraryPeek(viewModel.getBottomCards(player.id, count), PeekLocation.BOTTOM)
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
                viewModel.toggleRevealTopCard(player.id)
            },
            onRevealTopNCards = { count, targetPlayerIds ->
                val topCards = viewModel.getTopCards(player.id, count)
                if (topCards.isNotEmpty()) {
                    viewModel.revealCards(player.id, topCards.map { it.instanceId }, targetPlayerIds)
                }
            },
            onRevealBottomNCards = { count, targetPlayerIds ->
                val bottomCards = viewModel.getBottomCards(player.id, count)
                if (bottomCards.isNotEmpty()) {
                    viewModel.revealCards(player.id, bottomCards.map { it.instanceId }, targetPlayerIds)
                }
            },
            onViewLibrary = {
                val allCards = viewModel.getCards(player.id, Zone.LIBRARY)
                onShowLibraryPeek(allCards, PeekLocation.ALL)
            }
        )
    }

    if (showLibraryPeekDialog) {
        LibraryPeekDialog(
            cards = libraryPeekCards,
            playerName = player.name,
            peekLocation = libraryPeekLocation,
            onDismiss = {
                onDismissLibraryPeek()
                onLibraryPeekCardsChange(emptyList())
            },
            onMoveCard = { cardInstance, zone ->
                viewModel.moveCard(cardInstance.instanceId, zone)
                // Update the peek list to remove the moved card
                onLibraryPeekCardsChange(libraryPeekCards.filter { it.instanceId != cardInstance.instanceId })
            },
            onMoveAllToZone = { zone ->
                libraryPeekCards.forEach { cardInstance ->
                    viewModel.moveCard(cardInstance.instanceId, zone)
                }
                onDismissLibraryPeek()
                onLibraryPeekCardsChange(emptyList())
            },
            onShuffleCards = {
                when (libraryPeekLocation) {
                    PeekLocation.TOP -> viewModel.shuffleTopCards(player.id, libraryPeekCards.size)
                    PeekLocation.BOTTOM -> viewModel.shuffleBottomCards(player.id, libraryPeekCards.size)
                    PeekLocation.ALL -> viewModel.shuffleLibrary(player.id)
                }
                onDismissLibraryPeek()
                onLibraryPeekCardsChange(emptyList())
            },
            onViewDetails = { cardInstance ->
                onCardAction(CardAction.ViewDetails(cardInstance))
            }
        )
    }

    if (showTokenCreationDialog) {
        TokenCreationDialog(
            viewModel = viewModel,
            onDismiss = onDismissTokenCreation,
            onCreateToken = { tokenName, tokenType, power, toughness, color, imageUri, quantity ->
                viewModel.createToken(player.id, tokenName, tokenType, power, toughness, color, imageUri, quantity)
            }
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

    if (showPlayerCountersDialog) {
        PlayerCountersDialog(
            player = player,
            onDismiss = onDismissPlayerCounters,
            onAddCounter = { counterType, amount ->
                viewModel.addPlayerCounter(player.id, counterType, amount)
            },
            onRemoveCounter = { counterType, amount ->
                viewModel.removePlayerCounter(player.id, counterType, amount)
            },
            onSetCounter = { counterType, amount ->
                viewModel.setPlayerCounter(player.id, counterType, amount)
            }
        )
    }
}
