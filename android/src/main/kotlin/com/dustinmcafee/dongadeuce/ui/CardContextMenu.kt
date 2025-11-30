package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dustinmcafee.dongadeuce.models.CardAction
import com.dustinmcafee.dongadeuce.models.CardInstance
import com.dustinmcafee.dongadeuce.models.Player
import com.dustinmcafee.dongadeuce.models.Zone

/**
 * Menu state for bottom sheet navigation
 */
enum class BottomSheetMenuState {
    MAIN,
    COUNTERS,
    MOVE_TO,
    GIVE_CONTROL,
    CARD_STATE,
    REVEAL_TO
}

/**
 * Bottom sheet context menu for card actions (Android equivalent of right-click menu)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardContextMenuBottomSheet(
    cardInstance: CardInstance,
    allPlayers: List<Player>,
    onAction: (CardAction) -> Unit,
    onDismiss: () -> Unit
) {
    var menuState by remember { mutableStateOf(BottomSheetMenuState.MAIN) }

    // For "Give Control", exclude the current controller (not just the local player)
    // This allows giving control back to the original owner
    val otherPlayers = allPlayers.filter { it.id != cardInstance.controllerId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header with card name
            Text(
                text = cardInstance.card.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Divider()

            when (menuState) {
                BottomSheetMenuState.MAIN -> MainMenu(
                    cardInstance = cardInstance,
                    otherPlayers = otherPlayers,
                    onAction = { action ->
                        onAction(action)
                        onDismiss()
                    },
                    onNavigate = { menuState = it }
                )
                BottomSheetMenuState.COUNTERS -> CountersMenu(
                    cardInstance = cardInstance,
                    onAction = { action ->
                        onAction(action)
                        onDismiss()
                    },
                    onBack = { menuState = BottomSheetMenuState.MAIN }
                )
                BottomSheetMenuState.MOVE_TO -> MoveToMenu(
                    cardInstance = cardInstance,
                    onAction = { action ->
                        onAction(action)
                        onDismiss()
                    },
                    onBack = { menuState = BottomSheetMenuState.MAIN }
                )
                BottomSheetMenuState.GIVE_CONTROL -> GiveControlMenu(
                    cardInstance = cardInstance,
                    otherPlayers = otherPlayers,
                    onAction = { action ->
                        onAction(action)
                        onDismiss()
                    },
                    onBack = { menuState = BottomSheetMenuState.MAIN }
                )
                BottomSheetMenuState.CARD_STATE -> CardStateMenu(
                    cardInstance = cardInstance,
                    onAction = { action ->
                        onAction(action)
                        onDismiss()
                    },
                    onBack = { menuState = BottomSheetMenuState.MAIN }
                )
                BottomSheetMenuState.REVEAL_TO -> RevealToMenu(
                    cardInstance = cardInstance,
                    otherPlayers = otherPlayers,
                    onAction = { action ->
                        onAction(action)
                        onDismiss()
                    },
                    onBack = { menuState = BottomSheetMenuState.MAIN }
                )
            }
        }
    }
}

@Composable
private fun MainMenu(
    cardInstance: CardInstance,
    otherPlayers: List<Player>,
    onAction: (CardAction) -> Unit,
    onNavigate: (BottomSheetMenuState) -> Unit
) {
    Column {
        when (cardInstance.zone) {
            Zone.BATTLEFIELD -> {
                if (cardInstance.isTapped) {
                    MenuItem("Untap") { onAction(CardAction.Untap(cardInstance)) }
                } else {
                    MenuItem("Tap") { onAction(CardAction.Tap(cardInstance)) }
                }

                MenuItem("Flip Card") { onAction(CardAction.FlipCard(cardInstance)) }

                if (cardInstance.card.power != null && cardInstance.card.toughness != null) {
                    MenuItem("Modify Power/Toughness...") {
                        onAction(CardAction.ShowPowerToughnessDialog(cardInstance))
                    }
                }

                SubMenuItem("Card State") { onNavigate(BottomSheetMenuState.CARD_STATE) }
                SubMenuItem("Counters") { onNavigate(BottomSheetMenuState.COUNTERS) }
                SubMenuItem("Move To") { onNavigate(BottomSheetMenuState.MOVE_TO) }

                if (otherPlayers.isNotEmpty()) {
                    SubMenuItem("Give Control") { onNavigate(BottomSheetMenuState.GIVE_CONTROL) }
                }

                MenuItem("Create Copy") {
                    onAction(CardAction.CreateCopy(cardInstance, cardInstance.controllerId))
                }

                MenuItem("View Details") { onAction(CardAction.ViewDetails(cardInstance)) }
            }

            Zone.HAND -> {
                MenuItem("Play to Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) }
                MenuItem("Play Face Down") { onAction(CardAction.PlayFaceDown(cardInstance)) }
                SubMenuItem("Move To") { onNavigate(BottomSheetMenuState.MOVE_TO) }

                if (otherPlayers.isNotEmpty()) {
                    SubMenuItem("Reveal To") { onNavigate(BottomSheetMenuState.REVEAL_TO) }
                    SubMenuItem("Give Control") { onNavigate(BottomSheetMenuState.GIVE_CONTROL) }
                }

                MenuItem("View Hand") { onAction(CardAction.ViewHand(cardInstance.ownerId)) }
                MenuItem("View Details") { onAction(CardAction.ViewDetails(cardInstance)) }
            }

            Zone.GRAVEYARD -> {
                MenuItem("Return to Hand") { onAction(CardAction.ToHand(cardInstance)) }
                MenuItem("Return to Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) }
                SubMenuItem("Move To") { onNavigate(BottomSheetMenuState.MOVE_TO) }

                if (otherPlayers.isNotEmpty()) {
                    SubMenuItem("Give Control") { onNavigate(BottomSheetMenuState.GIVE_CONTROL) }
                }

                MenuItem("View Details") { onAction(CardAction.ViewDetails(cardInstance)) }
            }

            Zone.EXILE -> {
                MenuItem("Return to Hand") { onAction(CardAction.ToHand(cardInstance)) }
                MenuItem("Return to Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) }
                SubMenuItem("Move To") { onNavigate(BottomSheetMenuState.MOVE_TO) }

                if (otherPlayers.isNotEmpty()) {
                    SubMenuItem("Give Control") { onNavigate(BottomSheetMenuState.GIVE_CONTROL) }
                }

                MenuItem("View Details") { onAction(CardAction.ViewDetails(cardInstance)) }
            }

            Zone.LIBRARY -> {
                MenuItem("To Hand") { onAction(CardAction.ToHand(cardInstance)) }
                MenuItem("To Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) }
                SubMenuItem("Move To") { onNavigate(BottomSheetMenuState.MOVE_TO) }

                if (otherPlayers.isNotEmpty()) {
                    SubMenuItem("Reveal To") { onNavigate(BottomSheetMenuState.REVEAL_TO) }
                    SubMenuItem("Give Control") { onNavigate(BottomSheetMenuState.GIVE_CONTROL) }
                }

                MenuItem("View Details") { onAction(CardAction.ViewDetails(cardInstance)) }
            }

            Zone.COMMAND_ZONE -> {
                MenuItem("Cast to Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) }
                MenuItem("To Hand") { onAction(CardAction.ToHand(cardInstance)) }
                MenuItem("View Details") { onAction(CardAction.ViewDetails(cardInstance)) }
            }

            Zone.STACK -> {
                MenuItem("To Graveyard") { onAction(CardAction.ToGraveyard(cardInstance)) }
                MenuItem("To Exile") { onAction(CardAction.ToExile(cardInstance)) }
                MenuItem("View Details") { onAction(CardAction.ViewDetails(cardInstance)) }
            }

            Zone.SIDEBOARD -> {
                MenuItem("To Hand") { onAction(CardAction.ToHand(cardInstance)) }
                MenuItem("To Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) }
                MenuItem("To Graveyard") { onAction(CardAction.ToGraveyard(cardInstance)) }
                MenuItem("View Details") { onAction(CardAction.ViewDetails(cardInstance)) }
            }
        }
    }
}

@Composable
private fun CountersMenu(
    cardInstance: CardInstance,
    onAction: (CardAction) -> Unit,
    onBack: () -> Unit
) {
    Column {
        BackMenuItem(onBack)

        UIConstants.COUNTER_TYPES.forEach { counterType ->
            MenuItem("Add ${counterType.displayName}") {
                onAction(CardAction.AddCounter(cardInstance, counterType.id))
            }
            MenuItem("Manage ${counterType.displayName}...") {
                onAction(CardAction.ShowCounterDialog(cardInstance, counterType.id))
            }
        }

        if (cardInstance.counters.isNotEmpty()) {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            cardInstance.counters.keys.forEach { counterType ->
                MenuItem("Remove $counterType") {
                    onAction(CardAction.RemoveCounter(cardInstance, counterType))
                }
            }
        }
    }
}

@Composable
private fun MoveToMenu(
    cardInstance: CardInstance,
    onAction: (CardAction) -> Unit,
    onBack: () -> Unit
) {
    Column {
        BackMenuItem(onBack)

        // Allow creatures and planeswalkers to command zone (supports house rules)
        val canGoToCommandZone = cardInstance.card.canBeCommander

        when (cardInstance.zone) {
            Zone.BATTLEFIELD -> {
                MenuItem("To Hand") { onAction(CardAction.ToHand(cardInstance)) }
                MenuItem("To Graveyard") { onAction(CardAction.ToGraveyard(cardInstance)) }
                MenuItem("To Exile") { onAction(CardAction.ToExile(cardInstance)) }
                MenuItem("To Library (Bottom)") { onAction(CardAction.ToLibrary(cardInstance)) }
                MenuItem("To Library (Position)...") {
                    onAction(CardAction.ShowLibraryPositionDialog(cardInstance))
                }
                if (canGoToCommandZone) {
                    MenuItem("To Command Zone") { onAction(CardAction.ToCommandZone(cardInstance)) }
                }
            }
            Zone.HAND -> {
                MenuItem("Discard") { onAction(CardAction.ToGraveyard(cardInstance)) }
                MenuItem("To Exile") { onAction(CardAction.ToExile(cardInstance)) }
                MenuItem("To Library (Bottom)") { onAction(CardAction.ToLibrary(cardInstance)) }
                MenuItem("To Library (Top)") { onAction(CardAction.ToTop(cardInstance)) }
                MenuItem("To Library (Position)...") {
                    onAction(CardAction.ShowLibraryPositionDialog(cardInstance))
                }
                if (canGoToCommandZone) {
                    MenuItem("To Command Zone") { onAction(CardAction.ToCommandZone(cardInstance)) }
                }
            }
            Zone.GRAVEYARD -> {
                MenuItem("To Exile") { onAction(CardAction.ToExile(cardInstance)) }
                MenuItem("To Library (Bottom)") { onAction(CardAction.ToLibrary(cardInstance)) }
                MenuItem("To Library (Top)") { onAction(CardAction.ToTop(cardInstance)) }
                if (canGoToCommandZone) {
                    MenuItem("To Command Zone") { onAction(CardAction.ToCommandZone(cardInstance)) }
                }
            }
            Zone.EXILE -> {
                MenuItem("To Graveyard") { onAction(CardAction.ToGraveyard(cardInstance)) }
                MenuItem("To Library (Bottom)") { onAction(CardAction.ToLibrary(cardInstance)) }
                if (canGoToCommandZone) {
                    MenuItem("To Command Zone") { onAction(CardAction.ToCommandZone(cardInstance)) }
                }
            }
            Zone.LIBRARY -> {
                MenuItem("To Top of Library") { onAction(CardAction.ToTop(cardInstance)) }
                if (canGoToCommandZone) {
                    MenuItem("To Command Zone") { onAction(CardAction.ToCommandZone(cardInstance)) }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun GiveControlMenu(
    cardInstance: CardInstance,
    otherPlayers: List<Player>,
    onAction: (CardAction) -> Unit,
    onBack: () -> Unit
) {
    Column {
        BackMenuItem(onBack)

        otherPlayers.forEach { player ->
            val actionText = when (cardInstance.zone) {
                Zone.BATTLEFIELD -> "Give Control to ${player.name}"
                else -> "Give to ${player.name}'s Battlefield"
            }
            MenuItem(actionText) {
                onAction(CardAction.GiveControlTo(cardInstance, player.id, player.name))
            }
        }
    }
}

@Composable
private fun CardStateMenu(
    cardInstance: CardInstance,
    onAction: (CardAction) -> Unit,
    onBack: () -> Unit
) {
    Column {
        BackMenuItem(onBack)

        if (cardInstance.isFaceDown) {
            MenuItem("Turn Face Up") { onAction(CardAction.ToggleFaceDown(cardInstance)) }
        } else {
            MenuItem("Turn Face Down") { onAction(CardAction.ToggleFaceDown(cardInstance)) }
        }

        MenuItem(
            if (cardInstance.doesntUntap) "Remove 'Doesn't Untap'" else "Mark 'Doesn't Untap'"
        ) {
            onAction(CardAction.ToggleDoesntUntap(cardInstance))
        }

        MenuItem("Set Annotation...") { onAction(CardAction.SetAnnotation(cardInstance)) }
    }
}

@Composable
private fun RevealToMenu(
    cardInstance: CardInstance,
    otherPlayers: List<Player>,
    onAction: (CardAction) -> Unit,
    onBack: () -> Unit
) {
    Column {
        BackMenuItem(onBack)

        MenuItem("Reveal to All") {
            onAction(CardAction.RevealTo(cardInstance, emptyList()))
        }

        otherPlayers.forEach { player ->
            MenuItem("Reveal to ${player.name}") {
                onAction(CardAction.RevealTo(cardInstance, listOf(player.id)))
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun SubMenuItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "$text  ›",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun BackMenuItem(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Back",
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = "Back",
            style = MaterialTheme.typography.bodyLarge
        )
    }
    Divider()
}
