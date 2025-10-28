package com.commandermtg.viewmodel

import com.commandermtg.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * UI state for the game screen
 */
data class GameUiState(
    val localPlayer: Player? = null,
    val opponents: List<Player> = emptyList(),
    val gameState: GameState? = null,
    val selectedCardId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val allPlayers: List<Player>
        get() = listOfNotNull(localPlayer) + opponents
}

class GameViewModel {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    /**
     * Initialize a new game with players
     */
    fun initializeGame(localPlayerName: String, opponentNames: List<String>) {
        val localPlayerId = UUID.randomUUID().toString()
        val localPlayer = Player(
            id = localPlayerId,
            name = localPlayerName,
            life = 40
        )

        val opponents = opponentNames.map { name ->
            Player(
                id = UUID.randomUUID().toString(),
                name = name,
                life = 40
            )
        }

        val gameState = GameState(
            gameId = UUID.randomUUID().toString(),
            players = listOf(localPlayer) + opponents,
            cardInstances = emptyList(),
            activePlayerIndex = 0,
            turnNumber = 1
        )

        _uiState.update {
            it.copy(
                localPlayer = localPlayer,
                opponents = opponents,
                gameState = gameState
            )
        }
    }

    /**
     * Load a deck for the local player
     */
    fun loadDeck(deck: Deck) {
        val currentState = _uiState.value
        val localPlayer = currentState.localPlayer ?: return
        val gameState = currentState.gameState ?: return

        // Create card instances for all cards in the deck
        val cardInstances = mutableListOf<CardInstance>()

        // Commander goes to command zone
        cardInstances.add(
            CardInstance(
                card = deck.commander,
                ownerId = localPlayer.id,
                zone = Zone.COMMAND_ZONE
            )
        )

        // All other cards start in library
        deck.cards.forEach { card ->
            cardInstances.add(
                CardInstance(
                    card = card,
                    ownerId = localPlayer.id,
                    zone = Zone.LIBRARY
                )
            )
        }

        // Shuffle library (simple random shuffle)
        val shuffledInstances = cardInstances.shuffled()

        // Replace existing cards for this player to avoid duplicates
        val otherPlayerCards = gameState.cardInstances.filter { it.ownerId != localPlayer.id }

        _uiState.update {
            it.copy(
                gameState = gameState.copy(
                    cardInstances = otherPlayerCards + shuffledInstances
                )
            )
        }
    }

    /**
     * Draw starting hand (7 cards)
     */
    fun drawStartingHand(playerId: String, cardCount: Int = 7) {
        repeat(cardCount) {
            drawCard(playerId)
        }
    }

    /**
     * Update life total for a player
     */
    fun updateLife(playerId: String, newLife: Int) {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        val updatedGameState = gameState.updatePlayer(playerId) { player ->
            player.setLife(newLife)
        }

        _uiState.update {
            it.copy(
                gameState = updatedGameState,
                localPlayer = if (currentState.localPlayer?.id == playerId) {
                    updatedGameState.players.find { p -> p.id == playerId }
                } else {
                    currentState.localPlayer
                },
                opponents = currentState.opponents.map { opponent ->
                    if (opponent.id == playerId) {
                        updatedGameState.players.find { p -> p.id == playerId } ?: opponent
                    } else {
                        opponent
                    }
                }
            )
        }
    }

    /**
     * Draw a card from library to hand
     */
    fun drawCard(playerId: String) {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        // Find the top card of player's library
        val topCard = gameState.cardInstances
            .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }
            .firstOrNull() ?: return

        val updatedGameState = gameState.updateCardInstance(topCard.instanceId) {
            it.moveToZone(Zone.HAND)
        }

        _uiState.update {
            it.copy(gameState = updatedGameState)
        }
    }

    /**
     * Move a card between zones
     */
    fun moveCard(cardInstanceId: String, targetZone: Zone) {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        val updatedGameState = gameState.updateCardInstance(cardInstanceId) {
            it.moveToZone(targetZone)
        }

        _uiState.update {
            it.copy(gameState = updatedGameState)
        }
    }

    /**
     * Tap/untap a card
     */
    fun toggleTap(cardInstanceId: String) {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        val updatedGameState = gameState.updateCardInstance(cardInstanceId) {
            if (it.isTapped) it.untap() else it.tap()
        }

        _uiState.update {
            it.copy(gameState = updatedGameState)
        }
    }

    /**
     * Get card count in a specific zone for a player
     */
    fun getCardCount(playerId: String, zone: Zone): Int {
        val gameState = _uiState.value.gameState ?: return 0
        return gameState.getPlayerCards(playerId, zone).size
    }

    /**
     * Get card instances in a specific zone for a player
     */
    fun getCards(playerId: String, zone: Zone): List<CardInstance> {
        val gameState = _uiState.value.gameState ?: return emptyList()
        return gameState.getPlayerCards(playerId, zone)
    }

    /**
     * Advance to next phase
     */
    fun nextPhase() {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        val updatedGameState = gameState.nextPhase()

        _uiState.update {
            it.copy(gameState = updatedGameState)
        }
    }

    /**
     * Select a card for interaction
     */
    fun selectCard(cardInstanceId: String?) {
        _uiState.update {
            it.copy(selectedCardId = cardInstanceId)
        }
    }
}
