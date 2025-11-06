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

            // Counters
            items.add(ContextMenuItem("Add +1/+1 Counter") {
                onAction(CardAction.AddCounter(cardInstance, "+1/+1"))
            })
            items.add(ContextMenuItem("Add Charge Counter") {
                onAction(CardAction.AddCounter(cardInstance, "charge"))
            })

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

            // Commander-specific: can go to command zone from anywhere
            if (cardInstance.card.isLegendary && cardInstance.card.isCreature) {
                items.add(ContextMenuItem("To Command Zone") { onAction(CardAction.ToCommandZone(cardInstance)) })
            }
        }

        Zone.HAND -> {
            items.add(ContextMenuItem("Play to Battlefield") { onAction(CardAction.ToBattlefield(cardInstance)) })
            items.add(ContextMenuItem("Discard") { onAction(CardAction.ToGraveyard(cardInstance)) })
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
            // TODO: Implement card details dialog
            println("View details for: ${action.cardInstance.card.name}")
        }
    }
}
