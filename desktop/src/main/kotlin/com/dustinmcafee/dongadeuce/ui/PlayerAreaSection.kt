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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
                currentPlayerId = player.id,
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
        PlayerZonesRow(
            player = player,
            viewModel = viewModel,
            libraryCount = libraryCount,
            graveyardCount = graveyardCount,
            exileCount = exileCount,
            commanderCount = commanderCount,
            dragDropState = dragDropState,
            onShowSetLifeDialog = { showSetLifeDialog = true },
            onShowCommandZoneDialog = { showCommandZoneDialog = true },
            onShowLibrarySearchDialog = { showLibrarySearchDialog = true },
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
            dragDropState = dragDropState,
            selectionState = selectionState,
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
}

/**
 * Row containing player zones (commander, library, graveyard, exile) and info
 */
@Composable
private fun PlayerZonesRow(
    player: Player,
    viewModel: GameViewModel,
    libraryCount: Int,
    graveyardCount: Int,
    exileCount: Int,
    commanderCount: Int,
    dragDropState: DragDropState?,
    onShowSetLifeDialog: () -> Unit,
    onShowCommandZoneDialog: () -> Unit,
    onShowLibrarySearchDialog: () -> Unit,
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
            ZoneCard(
                "Library",
                Zone.LIBRARY,
                libraryCount,
                Modifier.weight(1f).fillMaxWidth(),
                onClick = onShowLibrarySearchDialog,
                dragDropState = dragDropState,
                onDropCards = { cardIds ->
                    dragDropState?.markHandledByZone()
                    cardIds.forEach { cardId ->
                        viewModel.moveCardToTopOfLibrary(cardId)
                    }
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
    dragDropState: DragDropState?,
    selectionState: SelectionState?,
    onShowHandDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
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
                                onCardClick = { onShowHandDialog() },
                                onDoubleClick = {
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

            // Batch actions row
            if (selectionState?.hasSelection == true) {
                HandBatchActionsRow(
                    selectionState = selectionState,
                    viewModel = viewModel
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
