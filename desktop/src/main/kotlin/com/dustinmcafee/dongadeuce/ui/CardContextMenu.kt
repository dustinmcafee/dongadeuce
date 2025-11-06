package com.dustinmcafee.dongadeuce.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 * Wraps content with a context menu appropriate for the card's current zone
 */
@Composable
fun CardWithContextMenu(
    cardInstance: CardInstance,
    onAction: (CardAction) -> Unit,
    modifier: Modifier = Modifier,
    otherPlayers: List<Player> = emptyList(),
    content: @Composable () -> Unit
) {
    val menuItems = buildContextMenuItems(cardInstance, onAction, otherPlayers)

    ContextMenuArea(
        items = { menuItems }
    ) {
        content()
    }
}

/**
 * Builds context menu items based on card's current zone
 */
private fun buildContextMenuItems(
    cardInstance: CardInstance,
    onAction: (CardAction) -> Unit,
    otherPlayers: List<Player> = emptyList()
): List<ContextMenuItem> {
    val items = mutableListOf<ContextMenuItem>()

    when (cardInstance.zone) {
        Zone.BATTLEFIELD -> {
            // Tap/Untap
            if (cardInstance.isTapped) {
                items.add(ContextMenuItem("Untap") { onAction(CardAction.Untap(cardInstance)) })
            } else {
                items.add(ContextMenuItem("Tap") { onAction(CardAction.Tap(cardInstance)) })
            }

            items.add(ContextMenuItem("Flip Card") { onAction(CardAction.FlipCard(cardInstance)) })

            // Face down
            if (cardInstance.isFaceDown) {
                items.add(ContextMenuItem("Turn Face Up") { onAction(CardAction.ToggleFaceDown(cardInstance)) })
            } else {
                items.add(ContextMenuItem("Turn Face Down") { onAction(CardAction.ToggleFaceDown(cardInstance)) })
            }

            // P/T modifications (for creatures)
            if (cardInstance.card.power != null && cardInstance.card.toughness != null) {
                items.add(ContextMenuItem("Modify Power/Toughness...") {
                    onAction(CardAction.ShowPowerToughnessDialog(cardInstance))
                })
            }

            // Card state
            items.add(ContextMenuItem(
                if (cardInstance.doesntUntap) "Remove 'Doesn't Untap'" else "Mark 'Doesn't Untap'"
            ) {
                onAction(CardAction.ToggleDoesntUntap(cardInstance))
            })

            items.add(ContextMenuItem("Set Annotation...") {
                onAction(CardAction.SetAnnotation(cardInstance))
            })

            // Counters - Show all 6 configurable types
            UIConstants.COUNTER_TYPES.forEach { counterType ->
                items.add(ContextMenuItem("Add ${counterType.displayName} Counter") {
                    onAction(CardAction.AddCounter(cardInstance, counterType.id))
                })
                items.add(ContextMenuItem("Manage ${counterType.displayName} Counters...") {
                    onAction(CardAction.ShowCounterDialog(cardInstance, counterType.id))
                })
            }

            // Show remove counter options if card has counters
            if (cardInstance.counters.isNotEmpty()) {
                cardInstance.counters.keys.forEach { counterType ->
                    items.add(ContextMenuItem("Remove $counterType Counter") {
                        onAction(CardAction.RemoveCounter(cardInstance, counterType))
                    })
                }
            }

            // Give control to other players
            otherPlayers.forEach { player: Player ->
                items.add(ContextMenuItem("Give Control to ${player.name}") {
                    onAction(CardAction.GiveControlTo(cardInstance, player.id, player.name))
                })
            }

            // Move to zones
            items.add(ContextMenuItem("Return to Hand") { onAction(CardAction.ToHand(cardInstance)) })
            items.add(ContextMenuItem("To Graveyard") { onAction(CardAction.ToGraveyard(cardInstance)) })
            items.add(ContextMenuItem("Exile") { onAction(CardAction.ToExile(cardInstance)) })
            items.add(ContextMenuItem("To Library") { onAction(CardAction.ToLibrary(cardInstance)) })
            items.add(ContextMenuItem("To Library (Choose Position)...") { onAction(CardAction.ShowLibraryPositionDialog(cardInstance)) })

            // Commander-specific: can go to command zone from anywhere
            if (cardInstance.card.isLegendary && cardInstance.card.isCreature) {
                items.add(ContextMenuItem("To Command Zone") { onAction(CardAction.ToCommandZone(cardInstance)) })
            }
        }

        Zone.HAND -> {
            items.add(ContextMenuItem("Play to Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) })
            items.add(ContextMenuItem("Play Face Down") { onAction(CardAction.PlayFaceDown(cardInstance)) })
            items.add(ContextMenuItem("Discard") { onAction(CardAction.ToGraveyard(cardInstance)) })
            items.add(ContextMenuItem("Exile") { onAction(CardAction.ToExile(cardInstance)) })
            items.add(ContextMenuItem("To Library") { onAction(CardAction.ToLibrary(cardInstance)) })
            items.add(ContextMenuItem("To Top of Library") { onAction(CardAction.ToTop(cardInstance)) })
            items.add(ContextMenuItem("To Library (Choose Position)...") { onAction(CardAction.ShowLibraryPositionDialog(cardInstance)) })

            // Give control to other players
            otherPlayers.forEach { player: Player ->
                items.add(ContextMenuItem("Give to ${player.name}'s Battlefield") {
                    onAction(CardAction.GiveControlTo(cardInstance, player.id, player.name))
                })
            }

            // Commander-specific: can go to command zone from anywhere
            if (cardInstance.card.isLegendary && cardInstance.card.isCreature) {
                items.add(ContextMenuItem("To Command Zone") { onAction(CardAction.ToCommandZone(cardInstance)) })
            }
        }

        Zone.GRAVEYARD -> {
            items.add(ContextMenuItem("Return to Hand") { onAction(CardAction.ToHand(cardInstance)) })
            items.add(ContextMenuItem("Return to Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) })
            items.add(ContextMenuItem("Exile") { onAction(CardAction.ToExile(cardInstance)) })
            items.add(ContextMenuItem("To Library") { onAction(CardAction.ToLibrary(cardInstance)) })
            items.add(ContextMenuItem("To Top of Library") { onAction(CardAction.ToTop(cardInstance)) })

            // Give control to other players
            otherPlayers.forEach { player: Player ->
                items.add(ContextMenuItem("Give to ${player.name}'s Battlefield") {
                    onAction(CardAction.GiveControlTo(cardInstance, player.id, player.name))
                })
            }

            // Commander-specific: can go to command zone from anywhere
            if (cardInstance.card.isLegendary && cardInstance.card.isCreature) {
                items.add(ContextMenuItem("To Command Zone") { onAction(CardAction.ToCommandZone(cardInstance)) })
            }
        }

        Zone.EXILE -> {
            items.add(ContextMenuItem("Return to Hand") { onAction(CardAction.ToHand(cardInstance)) })
            items.add(ContextMenuItem("Return to Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) })
            items.add(ContextMenuItem("To Graveyard") { onAction(CardAction.ToGraveyard(cardInstance)) })
            items.add(ContextMenuItem("To Library") { onAction(CardAction.ToLibrary(cardInstance)) })

            // Give control to other players
            otherPlayers.forEach { player: Player ->
                items.add(ContextMenuItem("Give to ${player.name}'s Battlefield") {
                    onAction(CardAction.GiveControlTo(cardInstance, player.id, player.name))
                })
            }

            // Commander-specific: can go to command zone from anywhere
            if (cardInstance.card.isLegendary && cardInstance.card.isCreature) {
                items.add(ContextMenuItem("To Command Zone") { onAction(CardAction.ToCommandZone(cardInstance)) })
            }
        }

        Zone.LIBRARY -> {
            items.add(ContextMenuItem("To Hand") { onAction(CardAction.ToHand(cardInstance)) })
            items.add(ContextMenuItem("To Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) })
            items.add(ContextMenuItem("To Top of Library") { onAction(CardAction.ToTop(cardInstance)) })

            // Give control to other players
            otherPlayers.forEach { player: Player ->
                items.add(ContextMenuItem("Give to ${player.name}'s Battlefield") {
                    onAction(CardAction.GiveControlTo(cardInstance, player.id, player.name))
                })
            }
        }

        Zone.COMMAND_ZONE -> {
            items.add(ContextMenuItem("Cast to Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) })
            items.add(ContextMenuItem("To Hand") { onAction(CardAction.ToHand(cardInstance)) })
        }

        Zone.STACK -> {
            // Stack zone - card is resolving, limited actions
            items.add(ContextMenuItem("To Graveyard") { onAction(CardAction.ToGraveyard(cardInstance)) })
            items.add(ContextMenuItem("To Exile") { onAction(CardAction.ToExile(cardInstance)) })
        }
    }

    // Always add view details option
    items.add(ContextMenuItem("View Details") { onAction(CardAction.ViewDetails(cardInstance)) })

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
