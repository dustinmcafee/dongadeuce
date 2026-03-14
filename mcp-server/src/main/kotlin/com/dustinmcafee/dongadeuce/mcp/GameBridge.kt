package com.dustinmcafee.dongadeuce.mcp

import com.dustinmcafee.dongadeuce.viewmodel.GameViewModel
import com.dustinmcafee.dongadeuce.viewmodel.GameUiState
import com.dustinmcafee.dongadeuce.models.*

/**
 * Headless wrapper around GameViewModel for MCP server use.
 * No UI needed — just the game engine.
 */
class GameBridge {
    private val viewModel = GameViewModel()

    fun state(): GameUiState = viewModel.uiState.value
    fun gameState(): GameState? = state().gameState
    fun players(): List<Player> = state().allPlayers

    fun findPlayerByName(name: String): Player? =
        players().find { it.name.equals(name, ignoreCase = true) }

    fun findCardByName(playerName: String, cardName: String, zone: Zone? = null): CardInstance? {
        val gs = gameState() ?: return null
        val player = findPlayerByName(playerName) ?: return null
        return gs.cardInstances.find { card ->
            card.card.name.equals(cardName, ignoreCase = true) &&
            card.ownerId == player.id &&
            (zone == null || card.zone == zone)
        }
    }

    // === Game Setup ===

    fun initializeGame(localPlayer: String, opponents: List<String>): String {
        viewModel.initializeGame(localPlayer, opponents, isHotseatMode = true)
        val state = state()
        val playerNames = state.allPlayers.map { "${it.name} (${it.id})" }
        return "Game initialized with ${state.allPlayers.size} players: ${playerNames.joinToString(", ")}"
    }

    fun drawStartingHand(playerId: String): String {
        viewModel.drawStartingHand(playerId)
        val handCount = gameState()?.getPlayerCards(playerId, Zone.HAND)?.size ?: 0
        return "Drew starting hand: $handCount cards"
    }

    // === Card Actions ===

    fun drawCard(playerId: String, count: Int = 1): String {
        if (count > 1) {
            viewModel.drawCards(playerId, count)
        } else {
            viewModel.drawCard(playerId)
        }
        val handCount = gameState()?.getPlayerCards(playerId, Zone.HAND)?.size ?: 0
        return "Drew $count card(s). Hand now has $handCount cards."
    }

    fun moveCard(cardId: String, targetZone: Zone): String {
        val gs = gameState() ?: return "Error: No active game"
        val card = gs.cardInstances.find { it.instanceId == cardId }
            ?: return "Error: Card not found with ID '$cardId'"
        val fromZone = card.zone
        viewModel.moveCard(cardId, targetZone)
        return "Moved '${card.card.name}' from ${fromZone.name} to ${targetZone.name}"
    }

    fun toggleTap(cardId: String): String {
        val gs = gameState() ?: return "Error: No active game"
        val card = gs.cardInstances.find { it.instanceId == cardId }
            ?: return "Error: Card not found"
        viewModel.toggleTap(cardId)
        val newState = if (card.isTapped) "untapped" else "tapped"
        return "'${card.card.name}' is now $newState"
    }

    fun untapAll(playerId: String): String {
        viewModel.untapAll(playerId)
        return "Untapped all permanents"
    }

    // === Life ===

    fun updateLife(playerId: String, newLife: Int): String {
        val gs = gameState() ?: return "Error: No active game"
        val player = gs.players.find { it.id == playerId } ?: return "Error: Player not found"
        val oldLife = player.life
        viewModel.updateLife(playerId, newLife)
        return "${player.name}'s life: $oldLife → $newLife"
    }

    fun changeLife(playerId: String, amount: Int): String {
        val gs = gameState() ?: return "Error: No active game"
        val player = gs.players.find { it.id == playerId } ?: return "Error: Player not found"
        viewModel.changeLife(playerId, amount)
        val newLife = player.life + amount
        val sign = if (amount >= 0) "+" else ""
        return "${player.name}'s life: ${player.life} → $newLife ($sign$amount)"
    }

    // === Turn/Phase ===

    fun passTurn(): String {
        viewModel.passTurn()
        val gs = gameState() ?: return "Turn passed"
        return "Turn ${gs.turnNumber}: ${gs.activePlayer.name}'s turn (${gs.phase.name})"
    }

    fun nextPhase(): String {
        viewModel.nextPhase()
        val gs = gameState() ?: return "Phase advanced"
        return "Phase: ${gs.phase.name} (${gs.activePlayer.name}'s turn ${gs.turnNumber})"
    }

    fun setPhase(phaseName: String): String {
        val phase = try {
            GamePhase.valueOf(phaseName.uppercase())
        } catch (e: Exception) {
            return "Error: Invalid phase '$phaseName'. Valid: ${GamePhase.values().joinToString()}"
        }
        viewModel.setPhase(phase)
        return "Phase set to ${phase.name}"
    }

    // === Counters ===

    fun addCounter(cardId: String, type: String, amount: Int = 1): String {
        viewModel.addCounter(cardId, type, amount)
        return "Added $amount $type counter(s)"
    }

    fun removeCounter(cardId: String, type: String, amount: Int = 1): String {
        viewModel.removeCounter(cardId, type, amount)
        return "Removed $amount $type counter(s)"
    }

    fun addPlayerCounter(playerId: String, type: String, amount: Int = 1): String {
        viewModel.addPlayerCounter(playerId, type, amount)
        return "Added $amount $type counter(s) to player"
    }

    // === Library ===

    fun shuffleLibrary(playerId: String): String {
        viewModel.shuffleLibrary(playerId)
        return "Library shuffled"
    }

    fun millCards(playerId: String, count: Int): String {
        viewModel.millCards(playerId, count)
        return "Milled $count card(s)"
    }

    fun mulligan(playerId: String): String {
        viewModel.mulligan(playerId)
        val handCount = gameState()?.getPlayerCards(playerId, Zone.HAND)?.size ?: 0
        return "Mulligan taken. New hand: $handCount cards"
    }

    // === Advanced ===

    fun flipCard(cardId: String): String {
        viewModel.flipCard(cardId)
        return "Card flipped"
    }

    fun giveControl(cardId: String, newControllerId: String): String {
        viewModel.giveControlTo(cardId, newControllerId)
        return "Control given"
    }

    fun createToken(
        playerId: String, name: String, type: String,
        power: String?, toughness: String?, color: String, quantity: Int = 1
    ): String {
        viewModel.createToken(playerId, name, type, power, toughness, color, quantity = quantity)
        return "Created $quantity $name token(s)"
    }

    fun cloneCard(cardId: String, ownerId: String): String {
        viewModel.cloneCard(cardId, ownerId)
        return "Card cloned"
    }

    fun concede(playerId: String): String {
        viewModel.concede(playerId)
        return "Player conceded"
    }

    fun updateCommanderDamage(playerId: String, commanderId: String, damage: Int): String {
        viewModel.updateCommanderDamage(playerId, commanderId, damage)
        return "Commander damage updated to $damage"
    }

    fun setAnnotation(cardId: String, annotation: String?): String {
        viewModel.setAnnotation(cardId, annotation)
        return "Annotation set"
    }

    fun modifyPowerToughness(cardId: String, power: Int, toughness: Int): String {
        viewModel.setPowerToughness(cardId, power, toughness)
        return "Power/toughness set to $power/$toughness"
    }

    fun moveCardToLibraryPosition(cardId: String, position: Int): String {
        viewModel.moveCardToLibraryPosition(cardId, position)
        return "Card moved to position $position from top of library"
    }

    fun attachCard(sourceId: String, targetId: String): String {
        viewModel.attachCard(sourceId, targetId)
        return "Card attached"
    }

    // === Deck Loading (simple - no enrichment) ===

    fun loadDeckForPlayer(playerId: String, deck: Deck): String {
        viewModel.loadDeckForPlayer(playerId, deck)
        val libCount = gameState()?.getPlayerCards(playerId, Zone.LIBRARY)?.size ?: 0
        return "Deck '${deck.name}' loaded. Library: $libCount cards, Commander: ${deck.commander.name}"
    }
}
