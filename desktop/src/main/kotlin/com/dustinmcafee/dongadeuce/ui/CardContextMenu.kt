package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.dustinmcafee.dongadeuce.models.CardInstance
import com.dustinmcafee.dongadeuce.models.Player
import com.dustinmcafee.dongadeuce.models.Zone

/**
 * Context menu actions for cards in different zones
 */
sealed class CardAction {
    data class Tap(val cardInstance: CardInstance) : CardAction()
    data class Untap(val cardInstance: CardInstance) : CardAction()
    data class FlipCard(val cardInstance: CardInstance) : CardAction()
    data class ToHand(val cardInstance: CardInstance) : CardAction()
    data class ToBattlefield(val cardInstance: CardInstance) : CardAction()
    data class ToGraveyard(val cardInstance: CardInstance) : CardAction()
    data class ToExile(val cardInstance: CardInstance) : CardAction()
    data class ToLibrary(val cardInstance: CardInstance) : CardAction()
    data class ToTop(val cardInstance: CardInstance) : CardAction()
    data class ToCommandZone(val cardInstance: CardInstance) : CardAction()
    data class AddCounter(val cardInstance: CardInstance, val counterType: String) : CardAction()
    data class RemoveCounter(val cardInstance: CardInstance, val counterType: String) : CardAction()
    data class GiveControlTo(val cardInstance: CardInstance, val newControllerId: String, val newControllerName: String) : CardAction()
    data class ViewDetails(val cardInstance: CardInstance) : CardAction()
    data class ShowLibraryPositionDialog(val cardInstance: CardInstance) : CardAction()
    data class ShowCounterDialog(val cardInstance: CardInstance, val counterType: String) : CardAction()
    data class ShowPowerToughnessDialog(val cardInstance: CardInstance) : CardAction()
    data class ToggleDoesntUntap(val cardInstance: CardInstance) : CardAction()
    data class SetAnnotation(val cardInstance: CardInstance) : CardAction()
    data class PlayFaceDown(val cardInstance: CardInstance) : CardAction()
    data class ToggleFaceDown(val cardInstance: CardInstance) : CardAction()
}

/**
 * Menu state for simulated sub-menus
 */
enum class MenuState {
    MAIN,
    COUNTERS,
    MOVE_TO,
    GIVE_CONTROL,
    CARD_STATE
}

/**
 * Wraps content with a context menu appropriate for the card's current zone
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CardWithContextMenu(
    cardInstance: CardInstance,
    onAction: (CardAction) -> Unit,
    modifier: Modifier = Modifier,
    otherPlayers: List<Player> = emptyList(),
    content: @Composable () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var menuPosition by remember { mutableStateOf(IntOffset.Zero) }
    var menuState by remember { mutableStateOf(MenuState.MAIN) }

    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                        val position = event.changes.first().position
                        menuPosition = IntOffset(position.x.toInt(), position.y.toInt())
                        menuState = MenuState.MAIN
                        showMenu = true
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
    ) {
        content()

        if (showMenu) {
            CustomContextMenu(
                cardInstance = cardInstance,
                onAction = { action ->
                    onAction(action)
                    showMenu = false
                },
                otherPlayers = otherPlayers,
                menuState = menuState,
                onMenuStateChange = { menuState = it },
                onDismiss = { showMenu = false },
                offset = menuPosition
            )
        }
    }
}

/**
 * Custom context menu with persistent state for sub-menu navigation
 */
@Composable
private fun CustomContextMenu(
    cardInstance: CardInstance,
    onAction: (CardAction) -> Unit,
    otherPlayers: List<Player>,
    menuState: MenuState,
    onMenuStateChange: (MenuState) -> Unit,
    onDismiss: () -> Unit,
    offset: IntOffset
) {
    Popup(
        offset = offset,
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier.width(220.dp),
            shape = RoundedCornerShape(4.dp),
            shadowElevation = 8.dp,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                val items = buildMenuItems(
                    cardInstance = cardInstance,
                    onAction = onAction,
                    otherPlayers = otherPlayers,
                    menuState = menuState,
                    onMenuStateChange = onMenuStateChange
                )

                items.forEach { item ->
                    MenuItem(
                        text = item.label,
                        onClick = item.onClick
                    )
                }
            }
        }
    }
}

/**
 * Individual menu item
 */
@Composable
private fun MenuItem(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium
    )
}

/**
 * Data class for menu items
 */
private data class MenuItemData(
    val label: String,
    val onClick: () -> Unit
)

/**
 * Builds context menu items based on card's current zone and menu state
 */
private fun buildMenuItems(
    cardInstance: CardInstance,
    onAction: (CardAction) -> Unit,
    otherPlayers: List<Player> = emptyList(),
    menuState: MenuState,
    onMenuStateChange: (MenuState) -> Unit
): List<MenuItemData> {
    return when (menuState) {
        MenuState.MAIN -> buildMainMenuItems(cardInstance, onAction, otherPlayers, onMenuStateChange)
        MenuState.COUNTERS -> buildCountersMenuItems(cardInstance, onAction, onMenuStateChange)
        MenuState.MOVE_TO -> buildMoveToMenuItems(cardInstance, onAction, onMenuStateChange)
        MenuState.GIVE_CONTROL -> buildGiveControlMenuItems(cardInstance, onAction, otherPlayers, onMenuStateChange)
        MenuState.CARD_STATE -> buildCardStateMenuItems(cardInstance, onAction, onMenuStateChange)
    }
}

/**
 * Builds context menu items based on card's current zone and menu state (DEPRECATED - kept for compatibility)
 */
private fun buildMenuItemDatas(
    cardInstance: CardInstance,
    onAction: (CardAction) -> Unit,
    otherPlayers: List<Player> = emptyList(),
    menuState: MenuState,
    onMenuStateChange: (MenuState) -> Unit
): List<MenuItemData> {
    return when (menuState) {
        MenuState.MAIN -> buildMainMenuItems(cardInstance, onAction, otherPlayers, onMenuStateChange)
        MenuState.COUNTERS -> buildCountersMenuItems(cardInstance, onAction, onMenuStateChange)
        MenuState.MOVE_TO -> buildMoveToMenuItems(cardInstance, onAction, onMenuStateChange)
        MenuState.GIVE_CONTROL -> buildGiveControlMenuItems(cardInstance, onAction, otherPlayers, onMenuStateChange)
        MenuState.CARD_STATE -> buildCardStateMenuItems(cardInstance, onAction, onMenuStateChange)
    }
}

/**
 * Builds the main context menu items
 */
private fun buildMainMenuItems(
    cardInstance: CardInstance,
    onAction: (CardAction) -> Unit,
    otherPlayers: List<Player>,
    onMenuStateChange: (MenuState) -> Unit
): List<MenuItemData> {
    val items = mutableListOf<MenuItemData>()

    when (cardInstance.zone) {
        Zone.BATTLEFIELD -> {
            // Quick actions
            if (cardInstance.isTapped) {
                items.add(MenuItemData("Untap") { onAction(CardAction.Untap(cardInstance)) })
            } else {
                items.add(MenuItemData("Tap") { onAction(CardAction.Tap(cardInstance)) })
            }

            items.add(MenuItemData("Flip Card") { onAction(CardAction.FlipCard(cardInstance)) })

            // P/T modifications (for creatures)
            if (cardInstance.card.power != null && cardInstance.card.toughness != null) {
                items.add(MenuItemData("Modify Power/Toughness...") {
                    onAction(CardAction.ShowPowerToughnessDialog(cardInstance))
                })
            }

            // Sub-menus
            items.add(MenuItemData("Card State ►") { onMenuStateChange(MenuState.CARD_STATE) })
            items.add(MenuItemData("Counters ►") { onMenuStateChange(MenuState.COUNTERS) })
            items.add(MenuItemData("Move To ►") { onMenuStateChange(MenuState.MOVE_TO) })

            if (otherPlayers.isNotEmpty()) {
                items.add(MenuItemData("Give Control ►") { onMenuStateChange(MenuState.GIVE_CONTROL) })
            }

            // Always add view details option
            items.add(MenuItemData("View Details") { onAction(CardAction.ViewDetails(cardInstance)) })
        }

        Zone.HAND -> {
            items.add(MenuItemData("Play to Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) })
            items.add(MenuItemData("Play Face Down") { onAction(CardAction.PlayFaceDown(cardInstance)) })
            items.add(MenuItemData("Move To ►") { onMenuStateChange(MenuState.MOVE_TO) })

            if (otherPlayers.isNotEmpty()) {
                items.add(MenuItemData("Give Control ►") { onMenuStateChange(MenuState.GIVE_CONTROL) })
            }

            items.add(MenuItemData("View Details") { onAction(CardAction.ViewDetails(cardInstance)) })
        }

        Zone.GRAVEYARD -> {
            items.add(MenuItemData("Return to Hand") { onAction(CardAction.ToHand(cardInstance)) })
            items.add(MenuItemData("Return to Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) })
            items.add(MenuItemData("Move To ►") { onMenuStateChange(MenuState.MOVE_TO) })

            if (otherPlayers.isNotEmpty()) {
                items.add(MenuItemData("Give Control ►") { onMenuStateChange(MenuState.GIVE_CONTROL) })
            }

            items.add(MenuItemData("View Details") { onAction(CardAction.ViewDetails(cardInstance)) })
        }

        Zone.EXILE -> {
            items.add(MenuItemData("Return to Hand") { onAction(CardAction.ToHand(cardInstance)) })
            items.add(MenuItemData("Return to Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) })
            items.add(MenuItemData("Move To ►") { onMenuStateChange(MenuState.MOVE_TO) })

            if (otherPlayers.isNotEmpty()) {
                items.add(MenuItemData("Give Control ►") { onMenuStateChange(MenuState.GIVE_CONTROL) })
            }

            items.add(MenuItemData("View Details") { onAction(CardAction.ViewDetails(cardInstance)) })
        }

        Zone.LIBRARY -> {
            items.add(MenuItemData("To Hand") { onAction(CardAction.ToHand(cardInstance)) })
            items.add(MenuItemData("To Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) })
            items.add(MenuItemData("Move To ►") { onMenuStateChange(MenuState.MOVE_TO) })

            if (otherPlayers.isNotEmpty()) {
                items.add(MenuItemData("Give Control ►") { onMenuStateChange(MenuState.GIVE_CONTROL) })
            }

            items.add(MenuItemData("View Details") { onAction(CardAction.ViewDetails(cardInstance)) })
        }

        Zone.COMMAND_ZONE -> {
            items.add(MenuItemData("Cast to Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) })
            items.add(MenuItemData("To Hand") { onAction(CardAction.ToHand(cardInstance)) })
            items.add(MenuItemData("View Details") { onAction(CardAction.ViewDetails(cardInstance)) })
        }

        Zone.STACK -> {
            items.add(MenuItemData("To Graveyard") { onAction(CardAction.ToGraveyard(cardInstance)) })
            items.add(MenuItemData("To Exile") { onAction(CardAction.ToExile(cardInstance)) })
            items.add(MenuItemData("View Details") { onAction(CardAction.ViewDetails(cardInstance)) })
        }
    }

    return items
}

/**
 * Builds the Card State sub-menu
 */
private fun buildCardStateMenuItems(
    cardInstance: CardInstance,
    onAction: (CardAction) -> Unit,
    onMenuStateChange: (MenuState) -> Unit
): List<MenuItemData> {
    val items = mutableListOf<MenuItemData>()

    items.add(MenuItemData("← Back") { onMenuStateChange(MenuState.MAIN) })

    // Face down toggle
    if (cardInstance.isFaceDown) {
        items.add(MenuItemData("Turn Face Up") { onAction(CardAction.ToggleFaceDown(cardInstance)) })
    } else {
        items.add(MenuItemData("Turn Face Down") { onAction(CardAction.ToggleFaceDown(cardInstance)) })
    }

    // Doesn't untap
    items.add(MenuItemData(
        if (cardInstance.doesntUntap) "Remove 'Doesn't Untap'" else "Mark 'Doesn't Untap'"
    ) {
        onAction(CardAction.ToggleDoesntUntap(cardInstance))
    })

    // Annotation
    items.add(MenuItemData("Set Annotation...") {
        onAction(CardAction.SetAnnotation(cardInstance))
    })

    return items
}

/**
 * Builds the Counters sub-menu
 */
private fun buildCountersMenuItems(
    cardInstance: CardInstance,
    onAction: (CardAction) -> Unit,
    onMenuStateChange: (MenuState) -> Unit
): List<MenuItemData> {
    val items = mutableListOf<MenuItemData>()

    items.add(MenuItemData("← Back") { onMenuStateChange(MenuState.MAIN) })

    // Show all 6 configurable counter types
    UIConstants.COUNTER_TYPES.forEach { counterType ->
        items.add(MenuItemData("Add ${counterType.displayName}") {
            onAction(CardAction.AddCounter(cardInstance, counterType.id))
        })
        items.add(MenuItemData("Manage ${counterType.displayName}...") {
            onAction(CardAction.ShowCounterDialog(cardInstance, counterType.id))
        })
    }

    // Show remove counter options if card has counters
    if (cardInstance.counters.isNotEmpty()) {
        cardInstance.counters.keys.forEach { counterType ->
            items.add(MenuItemData("Remove $counterType") {
                onAction(CardAction.RemoveCounter(cardInstance, counterType))
            })
        }
    }

    return items
}

/**
 * Builds the Move To sub-menu
 */
private fun buildMoveToMenuItems(
    cardInstance: CardInstance,
    onAction: (CardAction) -> Unit,
    onMenuStateChange: (MenuState) -> Unit
): List<MenuItemData> {
    val items = mutableListOf<MenuItemData>()

    items.add(MenuItemData("← Back") { onMenuStateChange(MenuState.MAIN) })

    when (cardInstance.zone) {
        Zone.BATTLEFIELD -> {
            items.add(MenuItemData("To Hand") { onAction(CardAction.ToHand(cardInstance)) })
            items.add(MenuItemData("To Graveyard") { onAction(CardAction.ToGraveyard(cardInstance)) })
            items.add(MenuItemData("To Exile") { onAction(CardAction.ToExile(cardInstance)) })
            items.add(MenuItemData("To Library (Bottom)") { onAction(CardAction.ToLibrary(cardInstance)) })
            items.add(MenuItemData("To Library (Position)...") { onAction(CardAction.ShowLibraryPositionDialog(cardInstance)) })

            if (cardInstance.card.isLegendary && cardInstance.card.isCreature) {
                items.add(MenuItemData("To Command Zone") { onAction(CardAction.ToCommandZone(cardInstance)) })
            }
        }
        Zone.HAND -> {
            items.add(MenuItemData("Discard") { onAction(CardAction.ToGraveyard(cardInstance)) })
            items.add(MenuItemData("To Exile") { onAction(CardAction.ToExile(cardInstance)) })
            items.add(MenuItemData("To Library (Bottom)") { onAction(CardAction.ToLibrary(cardInstance)) })
            items.add(MenuItemData("To Library (Top)") { onAction(CardAction.ToTop(cardInstance)) })
            items.add(MenuItemData("To Library (Position)...") { onAction(CardAction.ShowLibraryPositionDialog(cardInstance)) })

            if (cardInstance.card.isLegendary && cardInstance.card.isCreature) {
                items.add(MenuItemData("To Command Zone") { onAction(CardAction.ToCommandZone(cardInstance)) })
            }
        }
        Zone.GRAVEYARD -> {
            items.add(MenuItemData("To Exile") { onAction(CardAction.ToExile(cardInstance)) })
            items.add(MenuItemData("To Library (Bottom)") { onAction(CardAction.ToLibrary(cardInstance)) })
            items.add(MenuItemData("To Library (Top)") { onAction(CardAction.ToTop(cardInstance)) })

            if (cardInstance.card.isLegendary && cardInstance.card.isCreature) {
                items.add(MenuItemData("To Command Zone") { onAction(CardAction.ToCommandZone(cardInstance)) })
            }
        }
        Zone.EXILE -> {
            items.add(MenuItemData("To Graveyard") { onAction(CardAction.ToGraveyard(cardInstance)) })
            items.add(MenuItemData("To Library (Bottom)") { onAction(CardAction.ToLibrary(cardInstance)) })

            if (cardInstance.card.isLegendary && cardInstance.card.isCreature) {
                items.add(MenuItemData("To Command Zone") { onAction(CardAction.ToCommandZone(cardInstance)) })
            }
        }
        Zone.LIBRARY -> {
            items.add(MenuItemData("To Top of Library") { onAction(CardAction.ToTop(cardInstance)) })
        }
        else -> {
            // No additional move options for COMMAND_ZONE, STACK
        }
    }

    return items
}

/**
 * Builds the Give Control sub-menu
 */
private fun buildGiveControlMenuItems(
    cardInstance: CardInstance,
    onAction: (CardAction) -> Unit,
    otherPlayers: List<Player>,
    onMenuStateChange: (MenuState) -> Unit
): List<MenuItemData> {
    val items = mutableListOf<MenuItemData>()

    items.add(MenuItemData("← Back") { onMenuStateChange(MenuState.MAIN) })

    otherPlayers.forEach { player: Player ->
        val actionText = when (cardInstance.zone) {
            Zone.BATTLEFIELD -> "Give Control to ${player.name}"
            else -> "Give to ${player.name}'s Battlefield"
        }
        items.add(MenuItemData(actionText) {
            onAction(CardAction.GiveControlTo(cardInstance, player.id, player.name))
        })
    }

    return items
}

/**
 * Helper to handle common context menu actions with ViewModel
 */
fun handleCardAction(
    action: CardAction,
    viewModel: com.dustinmcafee.dongadeuce.viewmodel.GameViewModel
) {
    when (action) {
        is CardAction.Tap -> viewModel.toggleTap(action.cardInstance.instanceId)
        is CardAction.Untap -> viewModel.toggleTap(action.cardInstance.instanceId)
        is CardAction.FlipCard -> viewModel.flipCard(action.cardInstance.instanceId)
        is CardAction.ToHand -> viewModel.moveCard(action.cardInstance.instanceId, Zone.HAND)
        is CardAction.ToBattlefield -> viewModel.moveCard(action.cardInstance.instanceId, Zone.BATTLEFIELD)
        is CardAction.ToGraveyard -> viewModel.moveCard(action.cardInstance.instanceId, Zone.GRAVEYARD)
        is CardAction.ToExile -> viewModel.moveCard(action.cardInstance.instanceId, Zone.EXILE)
        is CardAction.ToLibrary -> viewModel.moveCard(action.cardInstance.instanceId, Zone.LIBRARY)
        is CardAction.ToTop -> viewModel.moveCardToTopOfLibrary(action.cardInstance.instanceId)
        is CardAction.ToCommandZone -> viewModel.moveCard(action.cardInstance.instanceId, Zone.COMMAND_ZONE)
        is CardAction.AddCounter -> viewModel.addCounter(action.cardInstance.instanceId, action.counterType, 1)
        is CardAction.RemoveCounter -> viewModel.removeCounter(action.cardInstance.instanceId, action.counterType, 1)
        is CardAction.GiveControlTo -> viewModel.giveControlTo(action.cardInstance.instanceId, action.newControllerId)
        is CardAction.ViewDetails -> {
            // Handled in UI layer (GameScreen)
            println("View details for: ${action.cardInstance.card.name}")
        }
        is CardAction.ShowLibraryPositionDialog -> {
            // Handled in UI layer (GameScreen)
            // Dialog will be shown there
        }
        is CardAction.ShowCounterDialog -> {
            // Handled in UI layer (GameScreen)
            // Dialog will be shown there
        }
        is CardAction.ShowPowerToughnessDialog -> {
            // Handled in UI layer (GameScreen)
            // Dialog will be shown there
        }
        is CardAction.ToggleDoesntUntap -> viewModel.toggleDoesntUntap(action.cardInstance.instanceId)
        is CardAction.SetAnnotation -> {
            // Handled in UI layer (GameScreen)
            // Dialog will be shown there
        }
        is CardAction.PlayFaceDown -> viewModel.playFaceDown(action.cardInstance.instanceId)
        is CardAction.ToggleFaceDown -> viewModel.toggleFaceDown(action.cardInstance.instanceId)
    }
}
