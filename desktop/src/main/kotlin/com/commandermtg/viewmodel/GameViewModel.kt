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
            name = localPlayerName
        )

        val opponents = opponentNames.map { name ->
            Player(
                id = UUID.randomUUID().toString(),
                name = name
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
     * Draw starting hand (7 cards by default)
     */
    fun drawStartingHand(playerId: String, cardCount: Int = GameConstants.STARTING_HAND_SIZE) {
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
     * If library is empty, player loses the game
     */
    fun drawCard(playerId: String) {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        // Find the top card of player's library
        val topCard = gameState.cardInstances
            .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }
            .lastOrNull() // Last card in list = top of library (stack-based)

        if (topCard == null) {
            // Player loses when trying to draw from empty library
            val updatedGameState = gameState.updatePlayer(playerId) { player ->
                player.copy(hasLost = true)
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
            return
        }

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
     * Get all cards in the battlefield (across all players)
     */
    fun getBattlefieldCards(): List<CardInstance> {
        val gameState = _uiState.value.gameState ?: return emptyList()
        return gameState.cardInstances.filter { it.zone == Zone.BATTLEFIELD }
    }

    /**
     * Advance to next phase
     */
    fun nextPhase() {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        val updatedGameState = gameState.nextPhase()

        // If we just moved to UNTAP phase (new turn), untap all cards for the active player
        if (updatedGameState.phase == com.commandermtg.models.GamePhase.UNTAP) {
            val activePlayerId = updatedGameState.activePlayer.id
            untapAll(activePlayerId)
        } else {
            _uiState.update {
                it.copy(gameState = updatedGameState)
            }
        }
    }

    /**
     * Pass turn (advance through all phases to next player's untap)
     */
    fun passTurn() {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        // Keep advancing phases until we reach the next UNTAP phase (new turn)
        var updatedState = gameState
        do {
            updatedState = updatedState.nextPhase()
        } while (updatedState.phase != com.commandermtg.models.GamePhase.UNTAP)

        // Untap all permanents for the new active player
        val newActivePlayerId = updatedState.activePlayer.id
        val untappedCards = updatedState.cardInstances.map { card ->
            if (card.controllerId == newActivePlayerId && card.zone == com.commandermtg.models.Zone.BATTLEFIELD) {
                card.untap()
            } else {
                card
            }
        }

        _uiState.update {
            it.copy(
                gameState = updatedState.copy(cardInstances = untappedCards)
            )
        }
    }

    /**
     * Untap all permanents for a player
     */
    fun untapAll(playerId: String) {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        val untappedCards = gameState.cardInstances.map { card ->
            if (card.controllerId == playerId && card.zone == com.commandermtg.models.Zone.BATTLEFIELD) {
                card.untap()
            } else {
                card
            }
        }

        _uiState.update {
            it.copy(
                gameState = gameState.copy(cardInstances = untappedCards)
            )
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

    /**
     * Get all commanders in the game (cards in command zone or battlefield)
     */
    fun getAllCommanders(): List<CardInstance> {
        val gameState = _uiState.value.gameState ?: return emptyList()
        return gameState.cardInstances.filter {
            it.zone == Zone.COMMAND_ZONE ||
            (it.zone == Zone.BATTLEFIELD && it.card.type?.contains("Legendary Creature", ignoreCase = true) == true)
        }
    }

    /**
     * Update commander damage dealt to a player
     */
    fun updateCommanderDamage(playerId: String, commanderId: String, newDamage: Int) {
        require(newDamage >= 0) { "Commander damage cannot be negative, got $newDamage" }

        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        val updatedGameState = gameState.updatePlayer(playerId) { player ->
            val currentDamage = player.commanderDamage[commanderId] ?: 0
            val damageChange = newDamage - currentDamage

            if (damageChange > 0) {
                player.takeCommanderDamage(commanderId, damageChange)
            } else if (damageChange < 0) {
                // Manual decrease - update the map directly
                player.copy(
                    commanderDamage = player.commanderDamage + (commanderId to newDamage),
                    hasLost = player.commanderDamage.any { (id, damage) ->
                        if (id == commanderId) {
                            newDamage >= GameConstants.COMMANDER_DAMAGE_THRESHOLD
                        } else {
                            damage >= GameConstants.COMMANDER_DAMAGE_THRESHOLD
                        }
                    }
                )
            } else {
                player
            }
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
     * Shuffle a player's library
     */
    fun shuffleLibrary(playerId: String) {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        // Get all library cards for this player
        val libraryCards = gameState.cardInstances
            .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }
            .shuffled()

        // Get all non-library cards
        val otherCards = gameState.cardInstances
            .filter { !(it.ownerId == playerId && it.zone == Zone.LIBRARY) }

        // Update game state with shuffled library
        _uiState.update {
            it.copy(
                gameState = gameState.copy(cardInstances = otherCards + libraryCards)
            )
        }
    }

    /**
     * Get all battlefield cards for a specific player
     */
    fun getPlayerBattlefieldCards(playerId: String): List<CardInstance> {
        val gameState = _uiState.value.gameState ?: return emptyList()
        return gameState.cardInstances.filter {
            it.ownerId == playerId && it.zone == Zone.BATTLEFIELD
        }
    }

    /**
     * Add counter(s) to a card
     */
    fun addCounter(cardId: String, type: String, amount: Int = 1) {
        require(type.isNotBlank()) { "Counter type cannot be blank" }
        require(amount > 0) { "Counter amount must be positive, got $amount" }

        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        val updatedGameState = gameState.updateCardInstance(cardId) {
            it.addCounter(type, amount)
        }

        _uiState.update {
            it.copy(gameState = updatedGameState)
        }
    }

    /**
     * Remove counter(s) from a card
     */
    fun removeCounter(cardId: String, type: String, amount: Int = 1) {
        require(type.isNotBlank()) { "Counter type cannot be blank" }
        require(amount > 0) { "Counter amount must be positive, got $amount" }

        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        val updatedGameState = gameState.updateCardInstance(cardId) { card ->
            val current = card.counters[type] ?: 0
            val newAmount = (current - amount).coerceAtLeast(0)
            card.copy(
                counters = if (newAmount > 0) {
                    card.counters + (type to newAmount)
                } else {
                    card.counters - type
                }
            )
        }

        _uiState.update {
            it.copy(gameState = updatedGameState)
        }
    }

    /**
     * Attach a card (aura/equipment) to another card
     */
    fun attachCard(sourceId: String, targetId: String) {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        val updatedGameState = gameState.updateCardInstance(sourceId) {
            it.copy(attachedTo = targetId)
        }

        _uiState.update {
            it.copy(gameState = updatedGameState)
        }
    }

    /**
     * Detach a card (remove attachment)
     */
    fun detachCard(cardId: String) {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        val updatedGameState = gameState.updateCardInstance(cardId) {
            it.copy(attachedTo = null)
        }

        _uiState.update {
            it.copy(gameState = updatedGameState)
        }
    }

    /**
     * Flip a card (for flip cards, morph, etc.)
     */
    fun flipCard(cardId: String) {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        val updatedGameState = gameState.updateCardInstance(cardId) {
            it.flip()
        }

        _uiState.update {
            it.copy(gameState = updatedGameState)
        }
    }

    /**
     * Mill cards from top of library to graveyard
     */
    fun millCards(playerId: String, count: Int) {
        repeat(count) {
            val currentState = _uiState.value
            val gameState = currentState.gameState ?: return@repeat

            // Get top card of library (last in list)
            val topCard = gameState.cardInstances
                .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }
                .lastOrNull()

            if (topCard != null) {
                moveCard(topCard.instanceId, Zone.GRAVEYARD)
            }
        }
    }

    /**
     * Mulligan - return hand to library, shuffle, and draw new hand
     */
    fun mulligan(playerId: String) {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        // Get all cards in hand
        val handCards = gameState.cardInstances
            .filter { it.ownerId == playerId && it.zone == Zone.HAND }

        // Move all hand cards to library
        handCards.forEach { card ->
            moveCard(card.instanceId, Zone.LIBRARY)
        }

        // Shuffle library
        shuffleLibrary(playerId)

        // Draw 7 cards (using default starting hand size)
        drawStartingHand(playerId, GameConstants.STARTING_HAND_SIZE)
    }

    /**
     * Move a card to the top of its owner's library
     * Convention: Last card in library list = top of library (stack-based)
     */
    fun moveCardToTopOfLibrary(cardId: String) {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        // Find the card
        val card = gameState.cardInstances.find { it.instanceId == cardId } ?: return
        val ownerId = card.ownerId

        // Move card to library first
        val updatedCard = card.moveToZone(Zone.LIBRARY)

        // Get all cards except the target card
        val otherCards = gameState.cardInstances.filter { it.instanceId != cardId }

        // Get all library cards for this player (excluding the moved card)
        val libraryCards = otherCards.filter { it.ownerId == ownerId && it.zone == Zone.LIBRARY }

        // Get all non-library cards
        val nonLibraryCards = otherCards.filter { !(it.ownerId == ownerId && it.zone == Zone.LIBRARY) }

        // Rebuild card list: non-library cards + rest of library + target card (top = last)
        val reorderedCards = nonLibraryCards + libraryCards + listOf(updatedCard)

        _uiState.update {
            it.copy(
                gameState = gameState.copy(cardInstances = reorderedCards)
            )
        }
    }
}
