package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    isActivePlayer: Boolean,
    onCardAction: (CardAction) -> Unit,
    dragDropState: DragDropState? = null,
    selectionState: SelectionState? = null,
    otherPlayers: List<Player> = emptyList(),
    modifier: Modifier = Modifier,
    inverted: Boolean = false, // If true, hand at bottom; if false, hand at top
    isLocalPlayer: Boolean = isActivePlayer // Defaults to isActivePlayer for hotseat mode compatibility
) {
    // Show hand cards if this is the local player (in network mode) or active player (in hotseat mode)
    val handCards = if (isLocalPlayer) viewModel.getCards(player.id, Zone.HAND) else emptyList()
    val handCount = viewModel.getCardCount(player.id, Zone.HAND)
    val battlefieldCards = viewModel.getCards(player.id, Zone.BATTLEFIELD)
    val commandZoneCards = viewModel.getCards(player.id, Zone.COMMAND_ZONE)
    val libraryCount = viewModel.getCardCount(player.id, Zone.LIBRARY)
    val graveyardCount = viewModel.getCardCount(player.id, Zone.GRAVEYARD)
    val exileCount = viewModel.getCardCount(player.id, Zone.EXILE)
    val topCard = viewModel.getTopCards(player.id, 1).firstOrNull()

    var showGraveyardDialog by remember { mutableStateOf(false) }
    var showExileDialog by remember { mutableStateOf(false) }
    var showLibrarySearchDialog by remember { mutableStateOf(false) }
    var showLibraryOperationsDialog by remember { mutableStateOf(false) }
    var showLibraryPeekDialog by remember { mutableStateOf(false) }
    var libraryPeekCards by remember { mutableStateOf<List<CardInstance>>(emptyList()) }
    var libraryPeekLocation by remember { mutableStateOf(PeekLocation.TOP) }
    var showTokenCreationDialog by remember { mutableStateOf(false) }
    var showSetLifeDialog by remember { mutableStateOf(false) }
    var showPlayerCountersDialog by remember { mutableStateOf(false) }

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
                showCards = isLocalPlayer,
                onCardAction = onCardAction,
                selectionState = if (isLocalPlayer) selectionState else null,
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
                PlayerInfoSidebar(
                    player = player,
                    viewModel = viewModel,
                    isLocalPlayer = isLocalPlayer,
                    commandZoneCards = commandZoneCards,
                    libraryCount = libraryCount,
                    topCard = topCard,
                    graveyardCount = graveyardCount,
                    exileCount = exileCount,
                    onCardAction = onCardAction,
                    otherPlayers = otherPlayers,
                    dragDropState = dragDropState,
                    onShowSetLifeDialog = { showSetLifeDialog = true },
                    onShowPlayerCountersDialog = { showPlayerCountersDialog = true },
                    onShowGraveyardDialog = { showGraveyardDialog = true },
                    onShowExileDialog = { showExileDialog = true },
                    onShowLibraryOperationsDialog = { showLibraryOperationsDialog = true },
                    onShowTokenCreationDialog = { showTokenCreationDialog = true },
                    modifier = Modifier.width(150.dp).fillMaxHeight().padding(4.dp)
                )

                // Battlefield cards
                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp)) {
                    DraggableBattlefieldGrid(
                        cards = battlefieldCards,
                        isLocalPlayer = isLocalPlayer,
                        onCardClick = { viewModel.toggleTap(it.instanceId) },
                        onContextAction = onCardAction,
                        onCardPositionChanged = { cardId, gridX, gridY ->
                            viewModel.updateCardGridPosition(cardId, gridX, gridY)
                        },
                        modifier = Modifier.fillMaxSize(),
                        selectionState = selectionState,
                        currentPlayerId = if (isLocalPlayer) player.id else null,
                        otherPlayers = otherPlayers,
                        allPlayers = otherPlayers + listOf(player),
                        dragDropState = if (isLocalPlayer) dragDropState else null,
                        onDropToZone = if (isLocalPlayer) { cardIds, zone ->
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
                showCards = isLocalPlayer,
                onCardAction = onCardAction,
                selectionState = if (isLocalPlayer) selectionState else null,
                otherPlayers = otherPlayers,
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )
        }
    }

    // Zone dialogs for local player (the one who can interact with their zones)
    if (isLocalPlayer) {
        HotseatPlayerDialogs(
            player = player,
            viewModel = viewModel,
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
            onShowLibrarySearch = {
                showLibraryOperationsDialog = false
                showLibrarySearchDialog = true
            },
            showTokenCreationDialog = showTokenCreationDialog,
            onDismissTokenCreation = { showTokenCreationDialog = false },
            showSetLifeDialog = showSetLifeDialog,
            onDismissSetLife = { showSetLifeDialog = false },
            showPlayerCountersDialog = showPlayerCountersDialog,
            onDismissPlayerCounters = { showPlayerCountersDialog = false }
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
    commandZoneCards: List<CardInstance>,
    libraryCount: Int,
    topCard: CardInstance?,
    graveyardCount: Int,
    exileCount: Int,
    onCardAction: (CardAction) -> Unit,
    otherPlayers: List<Player>,
    dragDropState: DragDropState?,
    onShowSetLifeDialog: () -> Unit,
    onShowPlayerCountersDialog: () -> Unit,
    onShowGraveyardDialog: () -> Unit,
    onShowExileDialog: () -> Unit,
    onShowLibraryOperationsDialog: () -> Unit,
    onShowTokenCreationDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
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
                modifier = Modifier.clickable { onShowSetLifeDialog() }
            )
            IconButton(
                onClick = { viewModel.updateLife(player.id, player.life + 1) },
                modifier = Modifier.size(20.dp)
            ) {
                Text("+", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }

        // Player counters display (clickable to open dialog)
        PlayerCountersDisplay(
            player = player,
            onClick = if (isLocalPlayer) onShowPlayerCountersDialog else null
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Command Zone - shows actual commander card(s)
        CommandZoneDisplay(
            commanderCards = commandZoneCards,
            isActivePlayer = isLocalPlayer,
            onCardAction = onCardAction,
            otherPlayers = otherPlayers,
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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Clickable zone cards - Library with card back image
        // Determine the image to show: revealed card, looked-at card, or card back
        val libraryImageUrl = when {
            player.revealTopCard && topCard != null -> topCard.card.imageUri
            player.lookAtTopCard && isLocalPlayer && topCard != null -> topCard.card.imageUri
            else -> "https://cards.scryfall.io/back.png"  // Standard card back
        }

        ZoneCard(
            "Library",
            Zone.LIBRARY,
            libraryCount,
            Modifier.fillMaxWidth().height(80.dp),  // Taller to show card image better
            onClick = null, // No single-click action
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
            imageUrl = libraryImageUrl
        )
        ZoneCard(
            "Graveyard",
            Zone.GRAVEYARD,
            graveyardCount,
            Modifier.fillMaxWidth().height(50.dp),
            onClick = if (isLocalPlayer) onShowGraveyardDialog else null,
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
        ZoneCard(
            "Exile",
            Zone.EXILE,
            exileCount,
            Modifier.fillMaxWidth().height(50.dp),
            onClick = if (isLocalPlayer) onShowExileDialog else null,
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

        // Token creation button (only for local player)
        if (isLocalPlayer) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onShowTokenCreationDialog,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Create Token", style = MaterialTheme.typography.labelSmall)
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
    onShowLibrarySearch: () -> Unit,
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
            }
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
                val topCard = viewModel.getTopCards(player.id, 1).firstOrNull()
                if (topCard != null) {
                    onShowLibraryPeek(listOf(topCard), PeekLocation.TOP)
                }
            },
            onFullSearch = onShowLibrarySearch
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
                }
                onDismissLibraryPeek()
                onLibraryPeekCardsChange(emptyList())
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
