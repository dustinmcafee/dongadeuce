package com.dustinmcafee.dongadeuce.viewmodel

import com.dustinmcafee.dongadeuce.api.ScryfallApi
import com.dustinmcafee.dongadeuce.models.*
import kotlinx.coroutines.*
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
    val error: String? = null,
    val isHotseatMode: Boolean = false,
    val gameEnded: Boolean = false,
    val tokenSearchResults: List<Card> = emptyList(),
    val isSearchingTokens: Boolean = false
) {
    val allPlayers: List<Player>
        get() = listOfNotNull(localPlayer) + opponents
}

class GameViewModel {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Use SupervisorJob so exceptions don't cancel the whole scope
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scryfallApi = ScryfallApi()

    /**
     * Clean up resources when ViewModel is no longer needed
     */
    fun cleanup() {
        viewModelScope.cancel()
        scryfallApi.close()
    }

    /**
     * Helper function to sync player references after updating game state
     * This eliminates duplicate code pattern throughout the ViewModel
     */
    private fun syncPlayerReferences(updatedGameState: GameState) {
        _uiState.update { currentState ->
            currentState.copy(
                gameState = updatedGameState,
                localPlayer = if (currentState.localPlayer?.id != null) {
                    updatedGameState.players.find { it.id == currentState.localPlayer.id }
                } else {
                    currentState.localPlayer
                },
                opponents = currentState.opponents.map { opponent ->
                    updatedGameState.players.find { it.id == opponent.id } ?: opponent
                }
            )
        }
    }

    /**
     * Initialize a new game with players
     */
    fun initializeGame(localPlayerName: String, opponentNames: List<String>, isHotseatMode: Boolean = false) {
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
                gameState = gameState,
                isHotseatMode = isHotseatMode
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
     * Load a deck for a specific player (for hotseat mode)
     */
    fun loadDeckForPlayer(playerId: String, deck: Deck) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            // Create card instances for all cards in the deck
            val cardInstances = mutableListOf<CardInstance>()

            // Commander goes to command zone
            cardInstances.add(
                CardInstance(
                    card = deck.commander,
                    ownerId = playerId,
                    zone = Zone.COMMAND_ZONE
                )
            )

            // All other cards start in library
            deck.cards.forEach { card ->
                cardInstances.add(
                    CardInstance(
                        card = card,
                        ownerId = playerId,
                        zone = Zone.LIBRARY
                    )
                )
            }

            // Shuffle library (simple random shuffle)
            val shuffledInstances = cardInstances.shuffled()

            // Replace existing cards for this player to avoid duplicates
            val otherPlayerCards = gameState.cardInstances.filter { it.ownerId != playerId }

            val updatedGameState = gameState.copy(
                cardInstances = otherPlayerCards + shuffledInstances
            )

            // Sync player references within the same update
            currentState.copy(
                gameState = updatedGameState,
                localPlayer = if (currentState.localPlayer?.id != null) {
                    updatedGameState.players.find { it.id == currentState.localPlayer.id }
                } else {
                    currentState.localPlayer
                },
                opponents = currentState.opponents.map { opponent ->
                    updatedGameState.players.find { it.id == opponent.id } ?: opponent
                }
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
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updatePlayer(playerId) { player ->
                player.setLife(newLife)
            }

            currentState.copy(
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

        // Check if game should end (less than 2 active players)
        checkGameEnd()
    }

    /**
     * Mark a player as having lost (they left the game or were defeated)
     */
    fun markPlayerAsLost(playerId: String) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updatePlayer(playerId) { player ->
                player.copy(hasLost = true)
            }

            currentState.copy(
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

        // Check if game should end
        checkGameEnd()
    }

    /**
     * Check if game should end (less than 2 active players remaining)
     */
    private fun checkGameEnd() {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        val activePlayers = gameState.players.count { !it.hasLost }

        if (activePlayers < 2) {
            // Game ends when less than 2 active players
            _uiState.update {
                it.copy(gameEnded = true)
            }
        }
    }

    /**
     * Draw a card from library to hand
     * If library is empty, player loses the game
     */
    fun drawCard(playerId: String) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            // Find the top card of player's library
            val topCard = gameState.cardInstances
                .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }
                .lastOrNull() // Last card in list = top of library (stack-based)

            if (topCard == null) {
                // Player loses when trying to draw from empty library
                val updatedGameState = gameState.updatePlayer(playerId) { player ->
                    player.copy(hasLost = true)
                }

                return@update currentState.copy(
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

            val updatedGameState = gameState.updateCardInstance(topCard.instanceId) {
                it.moveToZone(Zone.HAND)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Move a card between zones
     */
    fun moveCard(cardInstanceId: String, targetZone: Zone) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardInstanceId) {
                it.moveToZone(targetZone)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Give control of a card to another player
     * Moves the card to their battlefield
     */
    fun giveControlTo(cardInstanceId: String, newControllerId: String) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardInstanceId) {
                it.changeController(newControllerId).moveToZone(Zone.BATTLEFIELD)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Tap/untap a card
     */
    fun toggleTap(cardInstanceId: String) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardInstanceId) {
                if (it.isTapped) it.untap() else it.tap()
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Get card count in a specific zone for a player
     * For battlefield, this counts cards controlled by the player
     * For other zones, this counts cards owned by the player
     */
    fun getCardCount(playerId: String, zone: Zone): Int {
        val gameState = _uiState.value.gameState ?: return 0
        return if (zone == Zone.BATTLEFIELD) {
            gameState.getPlayerBattlefield(playerId).size
        } else {
            gameState.getPlayerCards(playerId, zone).size
        }
    }

    /**
     * Get card instances in a specific zone for a player
     * For battlefield, this returns cards controlled by the player
     * For other zones, this returns cards owned by the player
     */
    fun getCards(playerId: String, zone: Zone): List<CardInstance> {
        val gameState = _uiState.value.gameState ?: return emptyList()
        return if (zone == Zone.BATTLEFIELD) {
            gameState.getPlayerBattlefield(playerId)
        } else {
            gameState.getPlayerCards(playerId, zone)
        }
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
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.nextPhase()

            // If we just moved to UNTAP phase (new turn), untap all cards for the active player
            // We'll let untapAll handle its own state update instead
            if (updatedGameState.phase == com.dustinmcafee.dongadeuce.models.GamePhase.UNTAP) {
                // Store updated game state and let untapAll handle the actual untapping
                currentState.copy(gameState = updatedGameState)
            } else {
                currentState.copy(gameState = updatedGameState)
            }
        }

        // Call untapAll after state update if needed
        _uiState.value.gameState?.let { gameState ->
            if (gameState.phase == com.dustinmcafee.dongadeuce.models.GamePhase.UNTAP) {
                val activePlayerId = gameState.activePlayer.id
                untapAll(activePlayerId)
            }
        }
    }

    /**
     * Pass turn (advance through all phases to next player's untap)
     */
    fun passTurn() {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            // Keep advancing phases until we reach the next UNTAP phase (new turn)
            var updatedState = gameState
            do {
                updatedState = updatedState.nextPhase()
            } while (updatedState.phase != com.dustinmcafee.dongadeuce.models.GamePhase.UNTAP)

            // Don't automatically untap - player must click "Untap All" button
            val finalGameState = updatedState

            // In hotseat mode, rotate the local player to match the active player
            if (currentState.isHotseatMode) {
                val allPlayers = finalGameState.players
                val activePlayerIndex = finalGameState.activePlayerIndex
                val newLocalPlayer = allPlayers[activePlayerIndex]
                val newOpponents = allPlayers.filterIndexed { index, _ -> index != activePlayerIndex }

                currentState.copy(
                    gameState = finalGameState,
                    localPlayer = newLocalPlayer,
                    opponents = newOpponents
                )
            } else {
                currentState.copy(
                    gameState = finalGameState
                )
            }
        }

        // Check if game should end after turn change
        checkGameEnd()
    }

    /**
     * Untap all permanents for a player
     */
    fun untapAll(playerId: String) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val untappedCards = gameState.cardInstances.map { card ->
                if (card.controllerId == playerId && card.zone == com.dustinmcafee.dongadeuce.models.Zone.BATTLEFIELD) {
                    card.untap()
                } else {
                    card
                }
            }

            currentState.copy(
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

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updatePlayer(playerId) { player ->
                val currentDamage = player.commanderDamage[commanderId] ?: 0
                val damageChange = newDamage - currentDamage

                if (damageChange > 0) {
                    player.takeCommanderDamage(commanderId, damageChange)
                } else if (damageChange < 0) {
                    // Manual decrease - update the map directly
                    // IMPORTANT: Preserve existing loss state - once lost, always lost
                    player.copy(
                        commanderDamage = player.commanderDamage + (commanderId to newDamage),
                        hasLost = player.hasLost || player.commanderDamage.any { (id, damage) ->
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

            // Sync player references within the same update
            currentState.copy(
                gameState = updatedGameState,
                localPlayer = if (currentState.localPlayer?.id != null) {
                    updatedGameState.players.find { it.id == currentState.localPlayer.id }
                } else {
                    currentState.localPlayer
                },
                opponents = currentState.opponents.map { opponent ->
                    updatedGameState.players.find { it.id == opponent.id } ?: opponent
                }
            )
        }
    }

    /**
     * Shuffle a player's library
     */
    fun shuffleLibrary(playerId: String) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            // Get all library cards for this player
            val libraryCards = gameState.cardInstances
                .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }
                .shuffled()

            // Get all non-library cards
            val otherCards = gameState.cardInstances
                .filter { !(it.ownerId == playerId && it.zone == Zone.LIBRARY) }

            // Update game state with shuffled library
            currentState.copy(
                gameState = gameState.copy(cardInstances = otherCards + libraryCards)
            )
        }
    }

    /**
     * Get all battlefield cards for a specific player (by controller)
     */
    fun getPlayerBattlefieldCards(playerId: String): List<CardInstance> {
        val gameState = _uiState.value.gameState ?: return emptyList()
        return gameState.getPlayerBattlefield(playerId)
    }

    /**
     * Add counter(s) to a card
     */
    fun addCounter(cardId: String, type: String, amount: Int = 1) {
        require(type.isNotBlank()) { "Counter type cannot be blank" }
        require(amount > 0) { "Counter amount must be positive, got $amount" }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardId) {
                it.addCounter(type, amount)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Remove counter(s) from a card
     */
    fun removeCounter(cardId: String, type: String, amount: Int = 1) {
        require(type.isNotBlank()) { "Counter type cannot be blank" }
        require(amount > 0) { "Counter amount must be positive, got $amount" }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

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

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Attach a card (aura/equipment) to another card
     */
    fun attachCard(sourceId: String, targetId: String) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(sourceId) {
                it.copy(attachedTo = targetId)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Detach a card (remove attachment)
     */
    fun detachCard(cardId: String) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardId) {
                it.copy(attachedTo = null)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Flip a card (for flip cards, morph, etc.)
     */
    fun flipCard(cardId: String) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardId) {
                it.flip()
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Mill cards from top of library to graveyard
     */
    fun millCards(playerId: String, count: Int) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            // Get cards from library (last cards = top of library)
            val libraryCards = gameState.cardInstances
                .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }

            // Take up to 'count' cards from top of library
            val cardsToMill = libraryCards.takeLast(count.coerceAtMost(libraryCards.size))

            // If we tried to mill more cards than exist, player loses
            val playerLost = count > libraryCards.size && libraryCards.isNotEmpty()

            // Move milled cards to graveyard
            val updatedGameState = gameState.copy(
                cardInstances = gameState.cardInstances.map { card ->
                    if (card.instanceId in cardsToMill.map { it.instanceId }) {
                        card.moveToZone(Zone.GRAVEYARD)
                    } else {
                        card
                    }
                },
                players = if (playerLost) {
                    gameState.players.map { player ->
                        if (player.id == playerId) player.copy(hasLost = true) else player
                    }
                } else {
                    gameState.players
                }
            )

            currentState.copy(gameState = updatedGameState)
        }

        // Check if game should end after milling
        checkGameEnd()
    }

    /**
     * Mulligan - return hand to library, shuffle, and draw new hand
     */
    fun mulligan(playerId: String) {
        // Get hand cards atomically
        val handCards = _uiState.value.gameState?.cardInstances
            ?.filter { it.ownerId == playerId && it.zone == Zone.HAND }
            ?: return

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
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            // Find the card
            val card = gameState.cardInstances.find { it.instanceId == cardId } ?: return@update currentState
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

            currentState.copy(
                gameState = gameState.copy(cardInstances = reorderedCards)
            )
        }
    }

    /**
     * Move a card to the bottom of its owner's library
     * Convention: First card in library list = bottom of library (stack-based)
     */
    fun moveCardToBottomOfLibrary(cardId: String) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            // Find the card
            val card = gameState.cardInstances.find { it.instanceId == cardId } ?: return@update currentState
            val ownerId = card.ownerId

            // Move card to library first
            val updatedCard = card.moveToZone(Zone.LIBRARY)

            // Get all cards except the target card
            val otherCards = gameState.cardInstances.filter { it.instanceId != cardId }

            // Get all library cards for this player (excluding the moved card)
            val libraryCards = otherCards.filter { it.ownerId == ownerId && it.zone == Zone.LIBRARY }

            // Get all non-library cards
            val nonLibraryCards = otherCards.filter { !(it.ownerId == ownerId && it.zone == Zone.LIBRARY) }

            // Rebuild card list: non-library cards + target card (bottom = first) + rest of library
            val reorderedCards = nonLibraryCards + listOf(updatedCard) + libraryCards

            currentState.copy(
                gameState = gameState.copy(cardInstances = reorderedCards)
            )
        }
    }

    /**
     * Move a card to a specific position from the top of its owner's library
     * Convention: position 1 = top (last in list), position 2 = second from top, etc.
     */
    fun moveCardToLibraryPosition(cardId: String, positionFromTop: Int) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            // Find the card
            val card = gameState.cardInstances.find { it.instanceId == cardId } ?: return@update currentState
            val ownerId = card.ownerId

            // Move card to library first
            val updatedCard = card.moveToZone(Zone.LIBRARY)

            // Get all cards except the target card
            val otherCards = gameState.cardInstances.filter { it.instanceId != cardId }

            // Get all library cards for this player (excluding the moved card)
            val libraryCards = otherCards.filter { it.ownerId == ownerId && it.zone == Zone.LIBRARY }.toMutableList()

            // Get all non-library cards
            val nonLibraryCards = otherCards.filter { !(it.ownerId == ownerId && it.zone == Zone.LIBRARY) }

            // Calculate insertion index: position 1 = last (top), position N = (size - N + 1) from start
            val insertIndex = (libraryCards.size - positionFromTop + 1).coerceIn(0, libraryCards.size)

            // Insert the card at the specified position
            libraryCards.add(insertIndex, updatedCard)

            // Rebuild card list
            val reorderedCards = nonLibraryCards + libraryCards

            currentState.copy(
                gameState = gameState.copy(cardInstances = reorderedCards)
            )
        }
    }

    /**
     * Move a card to a specific position from the bottom of its owner's library
     * Convention: position 1 = bottom (first in list), position 2 = second from bottom, etc.
     */
    fun moveCardToLibraryPositionFromBottom(cardId: String, positionFromBottom: Int) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            // Find the card
            val card = gameState.cardInstances.find { it.instanceId == cardId } ?: return@update currentState
            val ownerId = card.ownerId

            // Move card to library first
            val updatedCard = card.moveToZone(Zone.LIBRARY)

            // Get all cards except the target card
            val otherCards = gameState.cardInstances.filter { it.instanceId != cardId }

            // Get all library cards for this player (excluding the moved card)
            val libraryCards = otherCards.filter { it.ownerId == ownerId && it.zone == Zone.LIBRARY }.toMutableList()

            // Get all non-library cards
            val nonLibraryCards = otherCards.filter { !(it.ownerId == ownerId && it.zone == Zone.LIBRARY) }

            // Calculate insertion index: position 1 = first (bottom), position 2 = second, etc.
            val insertIndex = (positionFromBottom - 1).coerceIn(0, libraryCards.size)

            // Insert the card at the specified position
            libraryCards.add(insertIndex, updatedCard)

            // Rebuild card list
            val reorderedCards = nonLibraryCards + libraryCards

            currentState.copy(
                gameState = gameState.copy(cardInstances = reorderedCards)
            )
        }
    }

    /**
     * Get the top N cards from a player's library
     * Convention: Last cards in library list = top of library (stack-based)
     */
    fun getTopCards(playerId: String, count: Int): List<CardInstance> {
        val gameState = _uiState.value.gameState ?: return emptyList()
        val libraryCards = gameState.cardInstances
            .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }
        return libraryCards.takeLast(count.coerceAtMost(libraryCards.size))
    }

    /**
     * Get the bottom N cards from a player's library
     * Convention: First cards in library list = bottom of library (stack-based)
     */
    fun getBottomCards(playerId: String, count: Int): List<CardInstance> {
        val gameState = _uiState.value.gameState ?: return emptyList()
        val libraryCards = gameState.cardInstances
            .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }
        return libraryCards.take(count.coerceAtMost(libraryCards.size))
    }

    /**
     * Shuffle the top N cards of a player's library
     */
    fun shuffleTopCards(playerId: String, count: Int) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            // Get library cards
            val libraryCards = gameState.cardInstances
                .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }

            if (libraryCards.size <= 1 || count <= 1) {
                return@update currentState // Nothing to shuffle
            }

            val actualCount = count.coerceAtMost(libraryCards.size)

            // Split library into top N and rest
            val topCards = libraryCards.takeLast(actualCount).shuffled()
            val remainingCards = libraryCards.dropLast(actualCount)

            // Get all non-library cards
            val otherCards = gameState.cardInstances
                .filter { !(it.ownerId == playerId && it.zone == Zone.LIBRARY) }

            // Rebuild: other cards + remaining library + shuffled top cards
            val reorderedCards = otherCards + remainingCards + topCards

            currentState.copy(
                gameState = gameState.copy(cardInstances = reorderedCards)
            )
        }
    }

    /**
     * Shuffle the bottom N cards of a player's library
     */
    fun shuffleBottomCards(playerId: String, count: Int) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            // Get library cards
            val libraryCards = gameState.cardInstances
                .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }

            if (libraryCards.size <= 1 || count <= 1) {
                return@update currentState // Nothing to shuffle
            }

            val actualCount = count.coerceAtMost(libraryCards.size)

            // Split library into bottom N and rest
            val bottomCards = libraryCards.take(actualCount).shuffled()
            val remainingCards = libraryCards.drop(actualCount)

            // Get all non-library cards
            val otherCards = gameState.cardInstances
                .filter { !(it.ownerId == playerId && it.zone == Zone.LIBRARY) }

            // Rebuild: other cards + shuffled bottom cards + remaining library
            val reorderedCards = otherCards + bottomCards + remainingCards

            currentState.copy(
                gameState = gameState.copy(cardInstances = reorderedCards)
            )
        }
    }

    /**
     * Move top N cards from library to a specific zone
     */
    fun moveTopCardsToZone(playerId: String, count: Int, targetZone: Zone) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val topCards = getTopCards(playerId, count)
            if (topCards.isEmpty()) return@update currentState

            // Move each card to the target zone
            var updatedGameState = gameState
            topCards.forEach { card ->
                updatedGameState = updatedGameState.updateCardInstance(card.instanceId) {
                    it.moveToZone(targetZone)
                }
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Move bottom N cards from library to a specific zone
     */
    fun moveBottomCardsToZone(playerId: String, count: Int, targetZone: Zone) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val bottomCards = getBottomCards(playerId, count)
            if (bottomCards.isEmpty()) return@update currentState

            // Move each card to the target zone
            var updatedGameState = gameState
            bottomCards.forEach { card ->
                updatedGameState = updatedGameState.updateCardInstance(card.instanceId) {
                    it.moveToZone(targetZone)
                }
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Create token(s) on the battlefield
     */
    fun createToken(
        playerId: String,
        tokenName: String,
        tokenType: String,
        power: String?,
        toughness: String?,
        color: String,
        imageUri: String? = null,
        quantity: Int = 1
    ) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            // Create a Card object for the token
            val tokenCard = Card(
                name = tokenName,
                type = tokenType,
                power = power,
                toughness = toughness,
                colors = if (color.isNotBlank()) listOf(color) else emptyList(),
                imageUri = imageUri,
                scryfallId = null
            )

            // Create the specified number of token instances
            val tokenInstances = List(quantity) {
                CardInstance(
                    card = tokenCard,
                    ownerId = playerId,
                    zone = Zone.BATTLEFIELD
                )
            }

            // Add tokens to the game state
            val updatedGameState = gameState.copy(
                cardInstances = gameState.cardInstances + tokenInstances
            )

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Update a card's grid position on the battlefield
     */
    fun updateCardGridPosition(cardId: String, gridX: Int, gridY: Int) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardId) {
                it.setGridPosition(gridX, gridY)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Search for tokens on Scryfall
     */
    fun searchTokens(query: String) {
        if (query.isBlank()) {
            clearTokenSearch()
            return
        }

        _uiState.update { it.copy(isSearchingTokens = true) }

        viewModelScope.launch {
            try {
                val results = scryfallApi.searchTokens(query)
                _uiState.update {
                    it.copy(
                        tokenSearchResults = results,
                        isSearchingTokens = false
                    )
                }
            } catch (e: Exception) {
                // Log error but don't crash
                println("Token search error: ${e.message}")
                _uiState.update {
                    it.copy(
                        tokenSearchResults = emptyList(),
                        isSearchingTokens = false
                    )
                }
            }
        }
    }

    /**
     * Clear token search results
     */
    fun clearTokenSearch() {
        _uiState.update {
            it.copy(
                tokenSearchResults = emptyList(),
                isSearchingTokens = false
            )
        }
    }

    /**
     * Handle a card action with ownership validation and multi-card support
     * This encapsulates the business logic for:
     * - Checking ownership (hotseat mode vs network mode)
     * - Applying actions to multiple selected cards
     * - Enforcing player permissions
     *
     * @param action The card action to perform
     * @param selectedCardIds Optional set of selected card IDs for batch operations
     * @return Number of cards successfully acted upon
     */
    fun handleBatchCardAction(
        action: com.dustinmcafee.dongadeuce.ui.CardAction,
        selectedCardIds: Set<String> = emptySet()
    ): Int {
        // ViewDetails doesn't require ownership check
        if (action is com.dustinmcafee.dongadeuce.ui.CardAction.ViewDetails) {
            return 1 // Handled separately in UI
        }

        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return 0

        // Get the primary card instance from the action
        val primaryCard = when (action) {
            is com.dustinmcafee.dongadeuce.ui.CardAction.Tap -> action.cardInstance
            is com.dustinmcafee.dongadeuce.ui.CardAction.Untap -> action.cardInstance
            is com.dustinmcafee.dongadeuce.ui.CardAction.FlipCard -> action.cardInstance
            is com.dustinmcafee.dongadeuce.ui.CardAction.ToHand -> action.cardInstance
            is com.dustinmcafee.dongadeuce.ui.CardAction.ToBattlefield -> action.cardInstance
            is com.dustinmcafee.dongadeuce.ui.CardAction.ToGraveyard -> action.cardInstance
            is com.dustinmcafee.dongadeuce.ui.CardAction.ToExile -> action.cardInstance
            is com.dustinmcafee.dongadeuce.ui.CardAction.ToLibrary -> action.cardInstance
            is com.dustinmcafee.dongadeuce.ui.CardAction.ToTop -> action.cardInstance
            is com.dustinmcafee.dongadeuce.ui.CardAction.ToCommandZone -> action.cardInstance
            is com.dustinmcafee.dongadeuce.ui.CardAction.AddCounter -> action.cardInstance
            is com.dustinmcafee.dongadeuce.ui.CardAction.RemoveCounter -> action.cardInstance
            is com.dustinmcafee.dongadeuce.ui.CardAction.GiveControlTo -> action.cardInstance
            else -> return 0
        }

        // Determine which player can perform actions
        val authorizedPlayerId = if (currentState.isHotseatMode) {
            gameState.activePlayer.id
        } else {
            currentState.localPlayer?.id
        }

        if (authorizedPlayerId == null) return 0

        // Determine which cards to act on
        val cardsToAct = if (selectedCardIds.contains(primaryCard.instanceId) && selectedCardIds.size > 1) {
            // Multi-card action: get all selected cards
            gameState.cardInstances.filter { it.instanceId in selectedCardIds }
        } else {
            // Single card action
            listOf(primaryCard)
        }

        // Filter to only cards owned by authorized player and perform action
        var actionCount = 0
        cardsToAct.forEach { card ->
            if (card.ownerId == authorizedPlayerId) {
                // Dispatch to appropriate ViewModel method
                when (action) {
                    is com.dustinmcafee.dongadeuce.ui.CardAction.Tap -> toggleTap(card.instanceId)
                    is com.dustinmcafee.dongadeuce.ui.CardAction.Untap -> toggleTap(card.instanceId)
                    is com.dustinmcafee.dongadeuce.ui.CardAction.FlipCard -> flipCard(card.instanceId)
                    is com.dustinmcafee.dongadeuce.ui.CardAction.ToHand -> moveCard(card.instanceId, Zone.HAND)
                    is com.dustinmcafee.dongadeuce.ui.CardAction.ToBattlefield -> moveCard(card.instanceId, Zone.BATTLEFIELD)
                    is com.dustinmcafee.dongadeuce.ui.CardAction.ToGraveyard -> moveCard(card.instanceId, Zone.GRAVEYARD)
                    is com.dustinmcafee.dongadeuce.ui.CardAction.ToExile -> moveCard(card.instanceId, Zone.EXILE)
                    is com.dustinmcafee.dongadeuce.ui.CardAction.ToLibrary -> moveCard(card.instanceId, Zone.LIBRARY)
                    is com.dustinmcafee.dongadeuce.ui.CardAction.ToTop -> moveCardToTopOfLibrary(card.instanceId)
                    is com.dustinmcafee.dongadeuce.ui.CardAction.ToCommandZone -> moveCard(card.instanceId, Zone.COMMAND_ZONE)
                    is com.dustinmcafee.dongadeuce.ui.CardAction.AddCounter -> addCounter(card.instanceId, action.counterType, 1)
                    is com.dustinmcafee.dongadeuce.ui.CardAction.RemoveCounter -> removeCounter(card.instanceId, action.counterType, 1)
                    is com.dustinmcafee.dongadeuce.ui.CardAction.GiveControlTo -> giveControlTo(card.instanceId, action.newControllerId)
                    else -> {}
                }
                actionCount++
            }
        }

        return actionCount
    }
}
