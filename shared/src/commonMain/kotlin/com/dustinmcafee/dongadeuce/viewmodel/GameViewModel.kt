package com.dustinmcafee.dongadeuce.viewmodel

import com.dustinmcafee.dongadeuce.api.ScryfallApi
import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.network.*
import com.dustinmcafee.dongadeuce.platform.generateUUID
import com.dustinmcafee.dongadeuce.platform.ioDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
    val isNetworkMode: Boolean = false,
    val isPaused: Boolean = false,
    val pauseReason: String? = null,
    val gameEnded: Boolean = false,
    val tokenSearchResults: List<Card> = emptyList(),
    val isSearchingTokens: Boolean = false
) {
    val allPlayers: List<Player>
        get() = listOfNotNull(localPlayer) + opponents
}

class GameViewModel(
    private val networkClient: GameClient? = null,
    private val networkServer: GameServer? = null,
    private val localPlayerId: String? = null
) {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Use SupervisorJob so exceptions don't cancel the whole scope
    private val viewModelScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val scryfallApi = ScryfallApi()

    // Track if we're in network mode (either as host or client)
    val isNetworkGame: Boolean get() = networkClient != null || networkServer != null

    init {
        // If we have a network client (joining player), observe its state
        networkClient?.let { client ->
            viewModelScope.launch {
                client.gameState.collect { gameState ->
                    if (gameState != null) {
                        handleNetworkStateUpdate(gameState)
                    }
                }
            }

            viewModelScope.launch {
                client.isPaused.collect { paused ->
                    _uiState.update { it.copy(isPaused = paused) }
                }
            }

            viewModelScope.launch {
                client.pauseReason.collect { reason ->
                    _uiState.update { it.copy(pauseReason = reason) }
                }
            }
        }

        // If we have a network server (host), observe its state
        networkServer?.let { server ->
            viewModelScope.launch {
                server.gameState.collect { gameState ->
                    if (gameState != null) {
                        handleNetworkStateUpdate(gameState)
                    }
                }
            }

            viewModelScope.launch {
                server.isPaused.collect { paused ->
                    _uiState.update { it.copy(isPaused = paused) }
                }
            }

            viewModelScope.launch {
                server.pauseReason.collect { reason ->
                    _uiState.update { it.copy(pauseReason = reason) }
                }
            }
        }
    }

    /**
     * Handle state update from network
     */
    private fun handleNetworkStateUpdate(gameState: GameState) {
        val playerId = localPlayerId ?: (networkClient?.playerId?.value)

        _uiState.update { currentState ->
            val localPlayer = playerId?.let { id ->
                gameState.players.find { it.id == id }
            }
            val opponents = playerId?.let { id ->
                gameState.players.filter { it.id != id }
            } ?: gameState.players

            currentState.copy(
                gameState = gameState,
                localPlayer = localPlayer,
                opponents = opponents,
                isNetworkMode = true
            )
        }
    }

    /**
     * Clean up resources when ViewModel is no longer needed
     */
    fun cleanup() {
        viewModelScope.cancel()
        scryfallApi.close()
    }

    /**
     * Helper to send network action
     * For clients: sends action to server via WebSocket
     * For host: executes action directly on the server
     */
    private fun sendNetworkAction(action: NetworkAction) {
        viewModelScope.launch {
            if (networkClient != null) {
                // Client: send to server
                networkClient.sendAction(action)
            } else if (networkServer != null) {
                // Host: execute directly on server
                val hostId = networkServer.getHostId()
                networkServer.executeHostAction(action, hostId)
            }
        }
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
        val localPlayerId = generateUUID()
        val localPlayer = Player(
            id = localPlayerId,
            name = localPlayerName
        )

        val opponents = opponentNames.map { name ->
            Player(
                id = generateUUID(),
                name = name
            )
        }

        val allPlayers = listOf(localPlayer) + opponents
        val allPlayerNames = allPlayers.map { it.name }

        // Create game started event
        val gameStartedEvent = GameEvent.GameStarted(
            playerNames = allPlayerNames,
            playerCount = allPlayers.size
        )

        val gameState = GameState(
            gameId = generateUUID(),
            players = allPlayers,
            cardInstances = emptyList(),
            activePlayerIndex = 0,
            turnNumber = 1,
            gameLog = listOf(gameStartedEvent)
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

        // Sideboard cards go to sideboard zone
        deck.sideboard.forEach { card ->
            cardInstances.add(
                CardInstance(
                    card = card,
                    ownerId = localPlayer.id,
                    zone = Zone.SIDEBOARD
                )
            )
        }

        // Shuffle library (simple random shuffle) - only library cards
        val libraryCards = cardInstances.filter { it.zone == Zone.LIBRARY }.shuffled()
        val nonLibraryCards = cardInstances.filter { it.zone != Zone.LIBRARY }
        val shuffledInstances = nonLibraryCards + libraryCards

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

            // Sideboard cards go to sideboard zone
            deck.sideboard.forEach { card ->
                cardInstances.add(
                    CardInstance(
                        card = card,
                        ownerId = playerId,
                        zone = Zone.SIDEBOARD
                    )
                )
            }

            // Shuffle library (simple random shuffle) - only library cards
            val libraryCards = cardInstances.filter { it.zone == Zone.LIBRARY }.shuffled()
            val nonLibraryCards = cardInstances.filter { it.zone != Zone.LIBRARY }
            val shuffledInstances = nonLibraryCards + libraryCards

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
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.UpdateLife(playerId, newLife))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState
            val oldLife = player.life

            var updatedGameState = gameState.updatePlayer(playerId) { p ->
                p.setLife(newLife)
            }

            // Log life change event
            if (oldLife != newLife) {
                val event = GameEvent.LifeChanged(
                    playerId = playerId,
                    playerName = player.name,
                    oldLife = oldLife,
                    newLife = newLife
                )
                updatedGameState = updatedGameState.addEvent(event)
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
     * Add player counter (poison, energy, experience, etc.)
     */
    fun addPlayerCounter(playerId: String, counterType: String, amount: Int = 1) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.AddPlayerCounter(playerId, counterType, amount))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState
            val oldAmount = player.getCounter(counterType)

            var updatedGameState = gameState.updatePlayer(playerId) { p ->
                p.addCounter(counterType, amount)
            }

            // Log counter change event
            val newAmount = oldAmount + amount
            val event = GameEvent.PlayerCounterChanged(
                playerId = playerId,
                playerName = player.name,
                counterType = counterType,
                oldAmount = oldAmount,
                newAmount = newAmount
            )
            updatedGameState = updatedGameState.addEvent(event)

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

        // Check if game should end (poison can cause loss)
        checkGameEnd()
    }

    /**
     * Remove player counter (poison, energy, experience, etc.)
     */
    fun removePlayerCounter(playerId: String, counterType: String, amount: Int = 1) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.RemovePlayerCounter(playerId, counterType, amount))
            return
        }

        addPlayerCounter(playerId, counterType, -amount)
    }

    /**
     * Set player counter to specific value
     */
    fun setPlayerCounter(playerId: String, counterType: String, amount: Int) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.SetPlayerCounter(playerId, counterType, amount))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState
            val oldAmount = player.getCounter(counterType)

            var updatedGameState = gameState.updatePlayer(playerId) { p ->
                p.setCounter(counterType, amount)
            }

            // Log counter change event if value changed
            if (oldAmount != amount) {
                val event = GameEvent.PlayerCounterChanged(
                    playerId = playerId,
                    playerName = player.name,
                    counterType = counterType,
                    oldAmount = oldAmount,
                    newAmount = amount
                )
                updatedGameState = updatedGameState.addEvent(event)
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

        // Check if game should end (poison can cause loss)
        checkGameEnd()
    }

    /**
     * Mark a player as having lost (they left the game or were defeated)
     */
    fun markPlayerAsLost(playerId: String, reason: String = "eliminated") {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState

            // Only log if player wasn't already lost
            val wasAlreadyLost = player.hasLost

            var updatedGameState = gameState.updatePlayer(playerId) { p ->
                p.copy(hasLost = true)
            }

            // Log player lost event
            if (!wasAlreadyLost) {
                val event = GameEvent.PlayerLost(
                    playerId = playerId,
                    playerName = player.name,
                    reason = reason
                )
                updatedGameState = updatedGameState.addEvent(event)
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
     * NOTE: Disabled automatic game end - players can continue playing after "losing"
     * The hasLost flag and log messages remain for tracking, but game continues
     */
    private fun checkGameEnd() {
        // Intentionally do nothing - let players continue playing
        // Loss conditions still set hasLost and add log messages, but game doesn't auto-end
    }

    /**
     * Draw a card from library to hand
     * If library is empty, player loses the game
     */
    fun drawCard(playerId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.DrawCard(playerId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState

            // Find the top card of player's library
            val topCard = gameState.cardInstances
                .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }
                .lastOrNull() // Last card in list = top of library (stack-based)

            if (topCard == null) {
                // Player loses when trying to draw from empty library
                var updatedGameState = gameState.updatePlayer(playerId) { p ->
                    p.copy(hasLost = true)
                }

                // Log player lost event
                val lostEvent = GameEvent.PlayerLost(
                    playerId = playerId,
                    playerName = player.name,
                    reason = "drew from empty library"
                )
                updatedGameState = updatedGameState.addEvent(lostEvent)

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

            var updatedGameState = gameState.updateCardInstance(topCard.instanceId) {
                it.moveToZone(Zone.HAND)
            }

            // Log card drawn event (hide card name to prevent revealing hand info)
            val drawEvent = GameEvent.CardDrawn(
                playerId = playerId,
                playerName = player.name,
                cardName = "a card" // Don't reveal what was drawn
            )
            updatedGameState = updatedGameState.addEvent(drawEvent)

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Move a card between zones
     */
    fun moveCard(cardInstanceId: String, targetZone: Zone) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.MoveCard(cardInstanceId, targetZone))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val card = gameState.cardInstances.find { it.instanceId == cardInstanceId } ?: return@update currentState
            val player = gameState.players.find { it.id == card.ownerId } ?: return@update currentState
            val fromZone = card.zone

            // Tokens and clones cease to exist when they leave the battlefield
            // (moving to graveyard, exile, hand, or library)
            val shouldRemove = (card.isToken || card.isClone) &&
                fromZone == Zone.BATTLEFIELD &&
                targetZone != Zone.BATTLEFIELD

            var updatedGameState = if (shouldRemove) {
                // Remove the token/clone from the game entirely
                gameState.copy(
                    cardInstances = gameState.cardInstances.filter { it.instanceId != cardInstanceId }
                )
            } else {
                gameState.updateCardInstance(cardInstanceId) { c ->
                    var updated = c.moveToZone(targetZone)

                    // Reset battlefield-specific state when leaving battlefield
                    if (c.zone == Zone.BATTLEFIELD && targetZone != Zone.BATTLEFIELD) {
                        updated = updated.copy(
                            counters = emptyMap(),
                            powerModifier = 0,
                            toughnessModifier = 0,
                            isTapped = false,
                            isFlipped = false,
                            doesntUntap = false,
                            attachedTo = null,
                            // Control reverts to owner when leaving battlefield (MTG rules)
                            controllerId = c.ownerId
                        )
                    }

                    // Update timestamp and assign grid position when moving to battlefield
                    // Uses type-based row assignment (creatures top, lands bottom, artifacts/enchantments middle)
                    if (targetZone == Zone.BATTLEFIELD && c.zone != Zone.BATTLEFIELD) {
                        val (gridX, gridY) = gameState.findNextGridPosition(c.controllerId, c, excludeCardId = cardInstanceId)
                        updated = updated.copy(
                            placedTimestamp = System.currentTimeMillis(),
                            gridX = gridX,
                            gridY = gridY
                        )
                    }

                    updated
                }
            }

            // Log card moved event (only if zone actually changed)
            if (fromZone != targetZone) {
                val event = if (shouldRemove) {
                    // Token/clone removed from game
                    GameEvent.CardMoved(
                        playerId = player.id,
                        playerName = player.name,
                        cardName = card.card.name,
                        fromZone = fromZone,
                        toZone = targetZone // Still log the intended destination
                    )
                } else if (targetZone == Zone.BATTLEFIELD && fromZone != Zone.BATTLEFIELD) {
                    // Playing a card to battlefield
                    GameEvent.CardPlayed(
                        playerId = player.id,
                        playerName = player.name,
                        cardName = card.card.name,
                        fromZone = fromZone
                    )
                } else {
                    // Moving between zones
                    GameEvent.CardMoved(
                        playerId = player.id,
                        playerName = player.name,
                        cardName = card.card.name,
                        fromZone = fromZone,
                        toZone = targetZone
                    )
                }
                updatedGameState = updatedGameState.addEvent(event)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Give control of a card to another player
     * Moves the card to their battlefield
     */
    fun giveControlTo(cardInstanceId: String, newControllerId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.GiveControlTo(cardInstanceId, newControllerId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val card = gameState.cardInstances.find { it.instanceId == cardInstanceId } ?: return@update currentState
            val fromPlayer = gameState.players.find { it.id == card.controllerId } ?: return@update currentState
            val toPlayer = gameState.players.find { it.id == newControllerId } ?: return@update currentState

            // Get new grid position for the new controller's battlefield (type-based row)
            val (gridX, gridY) = gameState.findNextGridPosition(newControllerId, card, excludeCardId = cardInstanceId)

            var updatedGameState = gameState.updateCardInstance(cardInstanceId) {
                it.changeController(newControllerId).moveToZone(Zone.BATTLEFIELD)
                    .copy(gridX = gridX, gridY = gridY, placedTimestamp = System.currentTimeMillis())
            }

            // Log control change event
            val event = GameEvent.ControlChanged(
                playerId = fromPlayer.id,
                playerName = fromPlayer.name,
                cardName = card.card.name,
                toPlayerName = toPlayer.name
            )
            updatedGameState = updatedGameState.addEvent(event)

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Tap/untap a card
     */
    fun toggleTap(cardInstanceId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.ToggleTap(cardInstanceId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val card = gameState.cardInstances.find { it.instanceId == cardInstanceId } ?: return@update currentState
            val player = gameState.players.find { it.id == card.controllerId } ?: return@update currentState
            val willBeTapped = !card.isTapped

            var updatedGameState = gameState.updateCardInstance(cardInstanceId) {
                if (it.isTapped) it.untap() else it.tap()
            }

            // Log tap/untap event
            val event = GameEvent.CardTapped(
                playerId = player.id,
                playerName = player.name,
                cardName = card.card.name,
                isTapped = willBeTapped
            )
            updatedGameState = updatedGameState.addEvent(event)

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
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.NextPhase)
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val activePlayer = gameState.activePlayer

            var updatedGameState = gameState.nextPhase()

            // Log phase change event
            val event = GameEvent.PhaseChanged(
                playerId = activePlayer.id,
                playerName = activePlayer.name,
                newPhase = updatedGameState.phase
            )
            updatedGameState = updatedGameState.addEvent(event)

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
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.PassTurn)
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val fromPlayer = gameState.activePlayer

            // Keep advancing phases until we reach the next UNTAP phase (new turn)
            var updatedState = gameState
            do {
                updatedState = updatedState.nextPhase()
            } while (updatedState.phase != com.dustinmcafee.dongadeuce.models.GamePhase.UNTAP)

            // Log turn passed event
            val toPlayer = updatedState.activePlayer
            val event = GameEvent.TurnPassed(
                playerId = fromPlayer.id,
                playerName = fromPlayer.name,
                toPlayerId = toPlayer.id,
                toPlayerName = toPlayer.name,
                turnNumber = updatedState.turnNumber
            )
            updatedState = updatedState.addEvent(event)

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
     * Set the current phase directly
     */
    fun setPhase(phase: com.dustinmcafee.dongadeuce.models.GamePhase) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.SetPhase(phase))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val activePlayer = gameState.activePlayer

            var updatedGameState = gameState.copy(phase = phase)

            // If setting to UNTAP phase, untap all cards for the active player
            if (phase == com.dustinmcafee.dongadeuce.models.GamePhase.UNTAP) {
                val untappedCards = updatedGameState.cardInstances.map { card ->
                    if (card.controllerId == activePlayer.id &&
                        card.zone == com.dustinmcafee.dongadeuce.models.Zone.BATTLEFIELD &&
                        card.isTapped &&
                        !card.doesntUntap) {
                        card.copy(isTapped = false)
                    } else {
                        card
                    }
                }
                updatedGameState = updatedGameState.copy(cardInstances = untappedCards)
            }

            // Log phase change event
            val event = GameEvent.PhaseChanged(
                playerId = activePlayer.id,
                playerName = activePlayer.name,
                newPhase = updatedGameState.phase
            )
            updatedGameState = updatedGameState.addEvent(event)

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Advance to next phase (alias for nextPhase for keyboard shortcuts)
     */
    fun advancePhase() = nextPhase()

    /**
     * Change life by a relative amount
     */
    fun changeLife(playerId: String, amount: Int) {
        val currentLife = _uiState.value.gameState?.players?.find { it.id == playerId }?.life ?: return
        updateLife(playerId, currentLife + amount)
    }

    /**
     * Draw multiple cards
     */
    fun drawCards(playerId: String, count: Int) {
        repeat(count) {
            drawCard(playerId)
        }
    }

    /**
     * Concede the game
     */
    fun concede(playerId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.Concede(playerId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState

            var updatedGameState = gameState.updatePlayer(playerId) { p ->
                p.setLife(0)
            }

            // Log player lost event
            val event = GameEvent.PlayerLost(
                playerId = playerId,
                playerName = player.name,
                reason = "Conceded"
            )
            updatedGameState = updatedGameState.addEvent(event)

            currentState.copy(
                gameState = updatedGameState,
                localPlayer = if (currentState.localPlayer?.id == playerId) {
                    updatedGameState.players.find { it.id == playerId }
                } else {
                    currentState.localPlayer
                },
                opponents = currentState.opponents.map { opponent ->
                    if (opponent.id == playerId) {
                        updatedGameState.players.find { it.id == playerId } ?: opponent
                    } else {
                        opponent
                    }
                }
            )
        }

        checkGameEnd()
    }

    /**
     * Untap all permanents for a player
     */
    fun untapAll(playerId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.UntapAll(playerId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState

            // Count tapped cards that will be untapped (excluding doesntUntap cards)
            val tappedCards = gameState.cardInstances.filter { card ->
                card.controllerId == playerId &&
                card.zone == com.dustinmcafee.dongadeuce.models.Zone.BATTLEFIELD &&
                card.isTapped &&
                !card.doesntUntap
            }
            val cardCount = tappedCards.size

            val untappedCards = gameState.cardInstances.map { card ->
                if (card.controllerId == playerId && card.zone == com.dustinmcafee.dongadeuce.models.Zone.BATTLEFIELD && !card.doesntUntap) {
                    card.untap()
                } else {
                    card
                }
            }

            var updatedGameState = gameState.copy(cardInstances = untappedCards)

            // Log untap all event (only if cards were actually untapped)
            if (cardCount > 0) {
                val event = GameEvent.UntapAll(
                    playerId = playerId,
                    playerName = player.name,
                    cardCount = cardCount
                )
                updatedGameState = updatedGameState.addEvent(event)
            }

            currentState.copy(gameState = updatedGameState)
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

        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.UpdateCommanderDamage(playerId, commanderId, newDamage))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val targetPlayer = gameState.players.find { it.id == playerId } ?: return@update currentState
            val commander = gameState.cardInstances.find { it.instanceId == commanderId }
            val commanderOwner = commander?.let { gameState.players.find { p -> p.id == it.ownerId } }

            val currentDamage = targetPlayer.commanderDamage[commanderId] ?: 0
            val damageChange = newDamage - currentDamage

            var updatedGameState = gameState.updatePlayer(playerId) { player ->
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

            // Log commander damage event (only if damage increased)
            if (damageChange > 0 && commander != null && commanderOwner != null) {
                val event = GameEvent.CommanderDamageDealt(
                    playerId = commanderOwner.id,
                    playerName = commanderOwner.name,
                    sourceCommanderName = commander.card.name,
                    targetPlayerName = targetPlayer.name,
                    damage = damageChange,
                    totalDamage = newDamage
                )
                updatedGameState = updatedGameState.addEvent(event)

                // Check if player lost due to commander damage
                if (newDamage >= GameConstants.COMMANDER_DAMAGE_THRESHOLD && currentDamage < GameConstants.COMMANDER_DAMAGE_THRESHOLD) {
                    val lostEvent = GameEvent.PlayerLost(
                        playerId = playerId,
                        playerName = targetPlayer.name,
                        reason = "21+ commander damage from ${commander.card.name}"
                    )
                    updatedGameState = updatedGameState.addEvent(lostEvent)
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
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.ShuffleLibrary(playerId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState

            // Get all library cards for this player
            val libraryCards = gameState.cardInstances
                .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }
                .shuffled()

            // Get all non-library cards
            val otherCards = gameState.cardInstances
                .filter { !(it.ownerId == playerId && it.zone == Zone.LIBRARY) }

            var updatedGameState = gameState.copy(cardInstances = otherCards + libraryCards)

            // Log shuffle event
            val event = GameEvent.LibraryShuffled(
                playerId = playerId,
                playerName = player.name
            )
            updatedGameState = updatedGameState.addEvent(event)

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * State for revealed cards dialog - holds the cards and who revealed them
     */
    data class RevealedCardsState(
        val revealingPlayerName: String,
        val cards: List<CardInstance>,
        val targetPlayerIds: List<String>, // Empty means all players
        val title: String // e.g., "revealed their hand" or "revealed 3 card(s)"
    )

    private val _revealedCardsState = MutableStateFlow<RevealedCardsState?>(null)
    val revealedCardsState: StateFlow<RevealedCardsState?> = _revealedCardsState.asStateFlow()

    /**
     * Dismiss the revealed cards dialog
     */
    fun dismissRevealedCards() {
        _revealedCardsState.value = null
    }

    /**
     * Reveal hand to specified players (or all if targetPlayerIds is empty)
     * Logs a message and shows a dialog to the target players
     */
    fun revealHand(playerId: String, targetPlayerIds: List<String>) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState

            // Get hand cards
            val handCards = gameState.cardInstances
                .filter { it.ownerId == playerId && it.zone == Zone.HAND }
                .sortedBy { it.card.name }

            // Build reveal message (without card names)
            val targetDescription = if (targetPlayerIds.isEmpty()) {
                "all players"
            } else {
                val targetNames = gameState.players
                    .filter { it.id in targetPlayerIds }
                    .map { it.name }
                targetNames.joinToString(", ")
            }

            val description = "revealed their hand to $targetDescription"

            // Log reveal event
            val event = GameEvent.GenericAction(
                playerId = playerId,
                playerName = player.name,
                description = description
            )
            val updatedGameState = gameState.addEvent(event)

            // Set revealed cards state to show dialog
            _revealedCardsState.value = RevealedCardsState(
                revealingPlayerName = player.name,
                cards = handCards,
                targetPlayerIds = targetPlayerIds,
                title = "revealed their hand"
            )

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Reveal specific cards to specified players (or all if targetPlayerIds is empty)
     * Used for revealing selected cards from hand or library
     */
    fun revealCards(playerId: String, cardIds: List<String>, targetPlayerIds: List<String>) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState

            // Get the specific cards
            val cardsToReveal = gameState.cardInstances
                .filter { it.instanceId in cardIds }
                .sortedBy { it.card.name }

            if (cardsToReveal.isEmpty()) return@update currentState

            // Build reveal message (without card names)
            val targetDescription = if (targetPlayerIds.isEmpty()) {
                "all players"
            } else {
                val targetNames = gameState.players
                    .filter { it.id in targetPlayerIds }
                    .map { it.name }
                targetNames.joinToString(", ")
            }

            val cardCountText = if (cardsToReveal.size == 1) "a card" else "${cardsToReveal.size} card(s)"
            val description = "revealed $cardCountText to $targetDescription"

            // Log reveal event
            val event = GameEvent.GenericAction(
                playerId = playerId,
                playerName = player.name,
                description = description
            )
            val updatedGameState = gameState.addEvent(event)

            // Set revealed cards state to show dialog
            _revealedCardsState.value = RevealedCardsState(
                revealingPlayerName = player.name,
                cards = cardsToReveal,
                targetPlayerIds = targetPlayerIds,
                title = "revealed $cardCountText"
            )

            currentState.copy(gameState = updatedGameState)
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

        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.AddCardCounter(cardId, type, amount))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val card = gameState.cardInstances.find { it.instanceId == cardId } ?: return@update currentState
            val player = gameState.players.find { it.id == card.controllerId } ?: return@update currentState
            val oldAmount = card.counters[type] ?: 0

            var updatedGameState = gameState.updateCardInstance(cardId) {
                it.addCounter(type, amount)
            }

            // Log counter change event
            val event = GameEvent.CardCounterChanged(
                playerId = player.id,
                playerName = player.name,
                cardName = card.card.name,
                counterType = type,
                oldAmount = oldAmount,
                newAmount = oldAmount + amount
            )
            updatedGameState = updatedGameState.addEvent(event)

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Remove counter(s) from a card
     */
    fun removeCounter(cardId: String, type: String, amount: Int = 1) {
        require(type.isNotBlank()) { "Counter type cannot be blank" }
        require(amount > 0) { "Counter amount must be positive, got $amount" }

        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.RemoveCardCounter(cardId, type, amount))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val card = gameState.cardInstances.find { it.instanceId == cardId } ?: return@update currentState
            val player = gameState.players.find { it.id == card.controllerId } ?: return@update currentState
            val oldAmount = card.counters[type] ?: 0
            val newAmount = (oldAmount - amount).coerceAtLeast(0)

            var updatedGameState = gameState.updateCardInstance(cardId) { c ->
                c.copy(
                    counters = if (newAmount > 0) {
                        c.counters + (type to newAmount)
                    } else {
                        c.counters - type
                    }
                )
            }

            // Log counter change event
            val event = GameEvent.CardCounterChanged(
                playerId = player.id,
                playerName = player.name,
                cardName = card.card.name,
                counterType = type,
                oldAmount = oldAmount,
                newAmount = newAmount
            )
            updatedGameState = updatedGameState.addEvent(event)

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Set counter(s) on a card to a specific value
     */
    fun setCounter(cardId: String, type: String, amount: Int) {
        require(type.isNotBlank()) { "Counter type cannot be blank" }
        require(amount >= 0) { "Counter amount must be non-negative, got $amount" }

        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.SetCardCounter(cardId, type, amount))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val card = gameState.cardInstances.find { it.instanceId == cardId } ?: return@update currentState
            val player = gameState.players.find { it.id == card.controllerId } ?: return@update currentState
            val oldAmount = card.counters[type] ?: 0

            var updatedGameState = gameState.updateCardInstance(cardId) { c ->
                c.copy(
                    counters = if (amount > 0) {
                        c.counters + (type to amount)
                    } else {
                        c.counters - type
                    }
                )
            }

            // Log counter change event if value changed
            if (oldAmount != amount) {
                val event = GameEvent.CardCounterChanged(
                    playerId = player.id,
                    playerName = player.name,
                    cardName = card.card.name,
                    counterType = type,
                    oldAmount = oldAmount,
                    newAmount = amount
                )
                updatedGameState = updatedGameState.addEvent(event)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Increment all counters on a card by 1
     */
    fun incrementAllCounters(cardId: String) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val card = gameState.cardInstances.find { it.instanceId == cardId } ?: return@update currentState

            if (card.counters.isEmpty()) return@update currentState

            val player = gameState.players.find { it.id == card.controllerId } ?: return@update currentState

            var updatedGameState = gameState.updateCardInstance(cardId) { c ->
                c.copy(counters = c.counters.mapValues { (_, count) -> count + 1 })
            }

            // Log counter change event
            val event = GameEvent.GenericAction(
                playerId = player.id,
                playerName = player.name,
                description = "incremented all counters on ${card.card.name}"
            )
            updatedGameState = updatedGameState.addEvent(event)

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Remove all local arrows for a player
     */
    fun removeLocalArrows(playerId: String) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.copy(
                arrows = gameState.arrows.filter { it.fromPlayerId != playerId }
            )

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Sort hand cards alphabetically by name
     */
    fun sortHand(playerId: String) {
        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState

            // Get hand cards sorted by name
            val handCards = gameState.cardInstances
                .filter { it.ownerId == playerId && it.zone == Zone.HAND }
                .sortedBy { it.card.name }

            // Update their order/position
            var updatedGameState = gameState
            handCards.forEachIndexed { index, card ->
                updatedGameState = updatedGameState.updateCardInstance(card.instanceId) { c ->
                    c.copy(handPosition = index)
                }
            }

            // Log event
            val event = GameEvent.GenericAction(
                playerId = player.id,
                playerName = player.name,
                description = "sorted their hand"
            )
            updatedGameState = updatedGameState.addEvent(event)

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Modify a card's power
     */
    fun modifyPower(cardId: String, amount: Int) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.ModifyPower(cardId, amount))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardId) { card ->
                card.copy(powerModifier = card.powerModifier + amount)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Modify a card's toughness
     */
    fun modifyToughness(cardId: String, amount: Int) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.ModifyToughness(cardId, amount))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardId) { card ->
                card.copy(toughnessModifier = card.toughnessModifier + amount)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Modify both power and toughness by the same amount
     */
    fun modifyPowerToughness(cardId: String, amount: Int) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.ModifyPowerToughness(cardId, amount))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardId) { card ->
                card.copy(
                    powerModifier = card.powerModifier + amount,
                    toughnessModifier = card.toughnessModifier + amount
                )
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Set power and toughness to specific values (calculates needed modifier)
     */
    fun setPowerToughness(cardId: String, newPower: Int, newToughness: Int) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.SetPowerToughness(cardId, newPower, newToughness))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardId) { card ->
                val basePower = card.card.power?.toIntOrNull() ?: 0
                val baseToughness = card.card.toughness?.toIntOrNull() ?: 0

                card.copy(
                    powerModifier = newPower - basePower,
                    toughnessModifier = newToughness - baseToughness
                )
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Reset P/T modifiers to 0
     */
    fun resetPowerToughness(cardId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.ResetPowerToughness(cardId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardId) { card ->
                card.copy(powerModifier = 0, toughnessModifier = 0)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Flow P: increase power, decrease toughness
     */
    fun flowPower(cardId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.FlowPower(cardId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardId) { card ->
                card.copy(
                    powerModifier = card.powerModifier + 1,
                    toughnessModifier = card.toughnessModifier - 1
                )
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Flow T: decrease power, increase toughness
     */
    fun flowToughness(cardId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.FlowToughness(cardId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardId) { card ->
                card.copy(
                    powerModifier = card.powerModifier - 1,
                    toughnessModifier = card.toughnessModifier + 1
                )
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Toggle doesn't untap flag
     */
    fun toggleDoesntUntap(cardId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.ToggleDoesntUntap(cardId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardId) { card ->
                card.copy(doesntUntap = !card.doesntUntap)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Set annotation on a card
     */
    fun setAnnotation(cardId: String, annotation: String?) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.SetAnnotation(cardId, annotation))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardId) { card ->
                card.copy(annotation = if (annotation.isNullOrBlank()) null else annotation)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Play a card face down
     */
    fun playFaceDown(cardId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.PlayFaceDown(cardId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val card = gameState.cardInstances.find { it.instanceId == cardId } ?: return@update currentState
            val (gridX, gridY) = gameState.findNextGridPosition(card.controllerId, card, excludeCardId = cardId)

            val updatedGameState = gameState.updateCardInstance(cardId) { c ->
                c.copy(
                    isFaceDown = true,
                    zone = Zone.BATTLEFIELD,
                    placedTimestamp = System.currentTimeMillis(),
                    gridX = gridX,
                    gridY = gridY
                )
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Toggle face down status
     */
    fun toggleFaceDown(cardId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.ToggleFaceDown(cardId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val updatedGameState = gameState.updateCardInstance(cardId) { card ->
                card.copy(isFaceDown = !card.isFaceDown)
            }

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Attach a card (aura/equipment) to another card
     */
    fun attachCard(sourceId: String, targetId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.AttachCard(sourceId, targetId))
            return
        }

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
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.DetachCard(cardId))
            return
        }

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
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.FlipCard(cardId))
            return
        }

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
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.MillCards(playerId, count))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState

            // Get cards from library (last cards = top of library)
            val libraryCards = gameState.cardInstances
                .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }

            // Take up to 'count' cards from top of library
            val cardsToMill = libraryCards.takeLast(count.coerceAtMost(libraryCards.size))
            val actualMillCount = cardsToMill.size

            // If we tried to mill more cards than exist, player loses
            val playerLost = count > libraryCards.size && libraryCards.isNotEmpty()

            // Move milled cards to graveyard
            var updatedGameState = gameState.copy(
                cardInstances = gameState.cardInstances.map { card ->
                    if (card.instanceId in cardsToMill.map { it.instanceId }) {
                        card.moveToZone(Zone.GRAVEYARD)
                    } else {
                        card
                    }
                },
                players = if (playerLost) {
                    gameState.players.map { p ->
                        if (p.id == playerId) p.copy(hasLost = true) else p
                    }
                } else {
                    gameState.players
                }
            )

            // Log mill event
            if (actualMillCount > 0) {
                val millEvent = GameEvent.CardsMilled(
                    playerId = playerId,
                    playerName = player.name,
                    cardCount = actualMillCount
                )
                updatedGameState = updatedGameState.addEvent(millEvent)
            }

            // Log player lost event if applicable
            if (playerLost) {
                val lostEvent = GameEvent.PlayerLost(
                    playerId = playerId,
                    playerName = player.name,
                    reason = "milled out"
                )
                updatedGameState = updatedGameState.addEvent(lostEvent)
            }

            currentState.copy(gameState = updatedGameState)
        }

        // Check if game should end after milling
        checkGameEnd()
    }

    /**
     * Mulligan - return hand to library, shuffle, and draw new hand
     */
    fun mulligan(playerId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.Mulligan(playerId))
            return
        }

        // Validate player exists
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return
        val player = gameState.players.find { it.id == playerId } ?: return

        // Move all hand cards to library (without individual logging)
        _uiState.update { state ->
            val gs = state.gameState ?: return@update state
            val updatedCardInstances = gs.cardInstances.map { card ->
                if (card.ownerId == playerId && card.zone == Zone.HAND) {
                    card.moveToZone(Zone.LIBRARY)
                } else {
                    card
                }
            }
            state.copy(gameState = gs.copy(cardInstances = updatedCardInstances))
        }

        // Shuffle library (this will log its own event)
        shuffleLibrary(playerId)

        // Draw 7 cards (using default starting hand size) - these will log their own events
        drawStartingHand(playerId, GameConstants.STARTING_HAND_SIZE)

        // Log mulligan event
        _uiState.update { state ->
            val gs = state.gameState ?: return@update state
            val newHandSize = gs.cardInstances.count { it.ownerId == playerId && it.zone == Zone.HAND }
            val event = GameEvent.MulliganTaken(
                playerId = playerId,
                playerName = player.name,
                newHandSize = newHandSize
            )
            state.copy(gameState = gs.addEvent(event))
        }
    }

    /**
     * Move a card to the top of its owner's library
     * Convention: Last card in library list = top of library (stack-based)
     */
    fun moveCardToTopOfLibrary(cardId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.MoveCardToTopOfLibrary(cardId))
            return
        }

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
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.MoveCardToBottomOfLibrary(cardId))
            return
        }

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
     * Reorder the top cards of a player's library (for scry and similar effects).
     * The orderedCardIds list should be in top-to-bottom order (first = top of library).
     * Cards not in the list remain in their current positions below the reordered cards.
     */
    fun reorderLibraryTop(playerId: String, orderedCardIds: List<String>) {
        if (orderedCardIds.isEmpty()) return

        // In network mode, would need to send network action (TODO: implement network support)
        // For now, just handle locally

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            // Get all library cards for this player
            val libraryCards = gameState.cardInstances.filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }.toMutableList()

            // Get non-library cards
            val nonLibraryCards = gameState.cardInstances.filter { !(it.ownerId == playerId && it.zone == Zone.LIBRARY) }

            // Find the cards to reorder (these are in the orderedCardIds list)
            val cardsToReorder = orderedCardIds.mapNotNull { id ->
                libraryCards.find { it.instanceId == id }
            }

            // Remove the reordered cards from the library list
            val remainingLibrary = libraryCards.filter { it.instanceId !in orderedCardIds }

            // Rebuild library: remaining cards + reordered cards (reversed because orderedCardIds is top-first)
            // Library convention: index 0 = bottom, last index = top
            // So we add reordered cards (reversed) at the end
            val newLibrary = remainingLibrary + cardsToReorder.reversed()

            currentState.copy(
                gameState = gameState.copy(cardInstances = nonLibraryCards + newLibrary)
            )
        }
    }

    /**
     * Move a card to a specific position from the top of its owner's library
     * Convention: position 1 = top (last in list), position 2 = second from top, etc.
     */
    fun moveCardToLibraryPosition(cardId: String, positionFromTop: Int) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.MoveCardToLibraryPosition(cardId, positionFromTop))
            return
        }

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
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.MoveCardToLibraryPositionFromBottom(cardId, positionFromBottom))
            return
        }

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
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.ShuffleTopCards(playerId, count))
            return
        }

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
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.ShuffleBottomCards(playerId, count))
            return
        }

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
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.MoveTopCardsToZone(playerId, count, targetZone))
            return
        }

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
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.MoveBottomCardsToZone(playerId, count, targetZone))
            return
        }

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
     * Draw cards from the bottom of the library
     */
    fun drawFromBottom(playerId: String, count: Int) {
        moveBottomCardsToZone(playerId, count, Zone.HAND)
    }

    /**
     * Mill cards from the bottom of the library
     */
    fun millFromBottom(playerId: String, count: Int) {
        moveBottomCardsToZone(playerId, count, Zone.GRAVEYARD)
    }

    /**
     * Exile cards from the bottom of the library
     */
    fun exileFromBottom(playerId: String, count: Int) {
        moveBottomCardsToZone(playerId, count, Zone.EXILE)
    }

    /**
     * Move the bottom card of the library to the top
     */
    fun moveBottomCardToTop(playerId: String) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.MoveBottomCardToTop(playerId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState

            val libraryCards = gameState.cardInstances
                .filter { it.ownerId == playerId && it.zone == Zone.LIBRARY }

            if (libraryCards.isEmpty()) return@update currentState

            // Get the bottom card (first in list) and move it to the end (top)
            val bottomCard = libraryCards.first()
            val remainingCards = libraryCards.drop(1)

            // Rebuild: other cards + remaining library + bottom card at top
            val otherCards = gameState.cardInstances
                .filter { !(it.ownerId == playerId && it.zone == Zone.LIBRARY) }
            val reorderedCards = otherCards + remainingCards + bottomCard

            currentState.copy(
                gameState = gameState.copy(cardInstances = reorderedCards)
            )
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
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.CreateToken(playerId, tokenName, tokenType, power, toughness, color, imageUri, quantity))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState

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

            // Create the specified number of token instances with grid positions
            // Create a temporary token to determine type-based row placement
            val templateToken = CardInstance(
                card = tokenCard,
                ownerId = playerId,
                zone = Zone.BATTLEFIELD,
                isToken = true
            )
            var tempGameState = gameState
            val tokenInstances = List(quantity) {
                val (gridX, gridY) = tempGameState.findNextGridPosition(playerId, templateToken)
                val token = CardInstance(
                    card = tokenCard,
                    ownerId = playerId,
                    zone = Zone.BATTLEFIELD,
                    gridX = gridX,
                    gridY = gridY,
                    isToken = true
                )
                // Update tempGameState so next token gets a different position
                tempGameState = tempGameState.copy(
                    cardInstances = tempGameState.cardInstances + token
                )
                token
            }

            // Add tokens to the game state
            var updatedGameState = gameState.copy(
                cardInstances = gameState.cardInstances + tokenInstances
            )

            // Log token creation event
            val event = GameEvent.TokenCreated(
                playerId = playerId,
                playerName = player.name,
                tokenName = tokenName,
                quantity = quantity
            )
            updatedGameState = updatedGameState.addEvent(event)

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Clone/copy a card instance
     * Creates a new copy of the card belonging to the specified player
     * The clone enters the battlefield (or specified zone) as a fresh copy
     *
     * @param cardId The card to clone
     * @param newOwnerId The player who will own the clone
     * @param targetZone Where the clone should be placed (default: Battlefield)
     * @param quantity Number of copies to create (for token doublers, etc.)
     * @return The instanceId of the created clone (or first clone if multiple)
     */
    fun cloneCard(
        cardId: String,
        newOwnerId: String,
        targetZone: Zone = Zone.BATTLEFIELD,
        quantity: Int = 1
    ): String? {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.CloneCard(cardId, newOwnerId, targetZone, quantity))
            return null // Clone ID not available in network mode
        }

        var createdCloneId: String? = null

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == newOwnerId } ?: return@update currentState

            // Find the original card
            val originalCard = gameState.cardInstances.find { it.instanceId == cardId }
                ?: return@update currentState

            // Create clone(s) with grid positions if going to battlefield
            var tempGameState = gameState
            val clones = List(quantity) {
                var clone = originalCard.createClone(newOwnerId, targetZone)
                if (targetZone == Zone.BATTLEFIELD) {
                    // Use type-based row placement for the clone
                    val (gridX, gridY) = tempGameState.findNextGridPosition(newOwnerId, clone)
                    clone = clone.copy(gridX = gridX, gridY = gridY)
                    tempGameState = tempGameState.copy(
                        cardInstances = tempGameState.cardInstances + clone
                    )
                }
                clone
            }

            createdCloneId = clones.firstOrNull()?.instanceId

            // Add clones to the game state
            var updatedGameState = gameState.copy(
                cardInstances = gameState.cardInstances + clones
            )

            // Log clone event
            val event = GameEvent.CardCloned(
                playerId = newOwnerId,
                playerName = player.name,
                cardName = originalCard.card.name,
                quantity = quantity
            )
            updatedGameState = updatedGameState.addEvent(event)

            currentState.copy(gameState = updatedGameState)
        }

        return createdCloneId
    }

    /**
     * Log a die roll event
     */
    fun logDieRoll(playerId: String, dieType: String, result: Int, numberOfDice: Int = 1) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.LogDieRoll(playerId, dieType, result, numberOfDice))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState

            val event = GameEvent.DieRolled(
                playerId = playerId,
                playerName = player.name,
                dieType = dieType,
                result = result,
                numberOfDice = numberOfDice
            )
            val updatedGameState = gameState.addEvent(event)

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Send a chat message
     */
    fun sendChatMessage(playerId: String, message: String) {
        if (message.isBlank()) return

        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.SendChatMessage(playerId, message))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val player = gameState.players.find { it.id == playerId } ?: return@update currentState

            val event = GameEvent.ChatMessage(
                playerId = playerId,
                playerName = player.name,
                message = message.trim()
            )
            val updatedGameState = gameState.addEvent(event)

            currentState.copy(gameState = updatedGameState)
        }
    }

    /**
     * Update a card's grid position on the battlefield
     */
    fun updateCardGridPosition(cardId: String, gridX: Int, gridY: Int) {
        // In network mode, send action to server
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.UpdateCardGridPosition(cardId, gridX, gridY))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val card = gameState.cardInstances.find { it.instanceId == cardId } ?: return@update currentState

            // Check if target position already has 3 cards (max stack size)
            val positions = gameState.computeBattlefieldPositions(card.controllerId)
            val targetPos = Pair(gridX, gridY)
            val cardsAtTarget = positions.count { (id, pos) -> id != cardId && pos == targetPos }
            if (cardsAtTarget >= 3) {
                // Don't allow stacking more than 3 - keep card at current position
                return@update currentState
            }

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
        action: CardAction,
        selectedCardIds: Set<String> = emptySet()
    ): Int {
        // ViewDetails doesn't require ownership check
        if (action is CardAction.ViewDetails) {
            return 1 // Handled separately in UI
        }

        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return 0

        // Get the primary card instance from the action
        val primaryCard = when (action) {
            is CardAction.Tap -> action.cardInstance
            is CardAction.Untap -> action.cardInstance
            is CardAction.FlipCard -> action.cardInstance
            is CardAction.ToHand -> action.cardInstance
            is CardAction.ToBattlefield -> action.cardInstance
            is CardAction.ToGraveyard -> action.cardInstance
            is CardAction.ToExile -> action.cardInstance
            is CardAction.ToLibrary -> action.cardInstance
            is CardAction.ToTop -> action.cardInstance
            is CardAction.ToCommandZone -> action.cardInstance
            is CardAction.AddCounter -> action.cardInstance
            is CardAction.RemoveCounter -> action.cardInstance
            is CardAction.GiveControlTo -> action.cardInstance
            is CardAction.RevealTo -> action.cardInstance
            is CardAction.PlayFaceDown -> action.cardInstance
            is CardAction.ToggleFaceDown -> action.cardInstance
            is CardAction.ToggleDoesntUntap -> action.cardInstance
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

        // Filter to only cards owned/controlled by authorized player and perform action
        // For battlefield actions like GiveControlTo, check controllerId instead of ownerId
        var actionCount = 0
        cardsToAct.forEach { card ->
            val canActOnCard = when {
                // For GiveControlTo, check if player controls the card (not just owns it)
                action is CardAction.GiveControlTo -> card.controllerId == authorizedPlayerId
                // For battlefield cards, check controller; for other zones, check owner
                card.zone == Zone.BATTLEFIELD -> card.controllerId == authorizedPlayerId
                else -> card.ownerId == authorizedPlayerId
            }
            if (canActOnCard) {
                // Dispatch to appropriate ViewModel method
                when (action) {
                    is CardAction.Tap -> toggleTap(card.instanceId)
                    is CardAction.Untap -> toggleTap(card.instanceId)
                    is CardAction.FlipCard -> flipCard(card.instanceId)
                    is CardAction.ToHand -> moveCard(card.instanceId, Zone.HAND)
                    is CardAction.ToBattlefield -> moveCard(card.instanceId, Zone.BATTLEFIELD)
                    is CardAction.ToGraveyard -> moveCard(card.instanceId, Zone.GRAVEYARD)
                    is CardAction.ToExile -> moveCard(card.instanceId, Zone.EXILE)
                    is CardAction.ToLibrary -> moveCard(card.instanceId, Zone.LIBRARY)
                    is CardAction.ToTop -> moveCardToTopOfLibrary(card.instanceId)
                    is CardAction.ToCommandZone -> moveCard(card.instanceId, Zone.COMMAND_ZONE)
                    is CardAction.AddCounter -> addCounter(card.instanceId, action.counterType, 1)
                    is CardAction.RemoveCounter -> removeCounter(card.instanceId, action.counterType, 1)
                    is CardAction.GiveControlTo -> giveControlTo(card.instanceId, action.newControllerId)
                    is CardAction.RevealTo -> {
                        // For reveal, we collect all card IDs and reveal them together
                        val cardIdsToReveal = cardsToAct.filter { it.ownerId == authorizedPlayerId }.map { it.instanceId }
                        if (cardIdsToReveal.isNotEmpty()) {
                            revealCards(authorizedPlayerId, cardIdsToReveal, action.targetPlayerIds)
                        }
                        return cardIdsToReveal.size // Return early since we handle all at once
                    }
                    is CardAction.PlayFaceDown -> playFaceDown(card.instanceId)
                    is CardAction.ToggleFaceDown -> toggleFaceDown(card.instanceId)
                    is CardAction.ToggleDoesntUntap -> toggleDoesntUntap(card.instanceId)
                    else -> {}
                }
                actionCount++
            }
        }

        return actionCount
    }

    /**
     * Toggle always reveal top card of library (visible to all players)
     */
    fun toggleRevealTopCard(playerId: String) {
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.ToggleRevealTopCard(playerId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val playerIndex = gameState.players.indexOfFirst { it.id == playerId }
            if (playerIndex == -1) return@update currentState

            val player = gameState.players[playerIndex]
            val newRevealState = !player.revealTopCard
            val updatedPlayer = player.copy(
                revealTopCard = newRevealState,
                // If revealing to all, also enable look at (can't reveal without looking)
                lookAtTopCard = if (newRevealState) true else player.lookAtTopCard
            )
            val updatedPlayers = gameState.players.toMutableList()
            updatedPlayers[playerIndex] = updatedPlayer

            val event = GameEvent.GenericAction(
                playerId = playerId,
                playerName = player.name,
                description = if (newRevealState) "is now revealing top card of library" else "stopped revealing top card of library"
            )

            currentState.copy(
                gameState = gameState.copy(players = updatedPlayers).addEvent(event)
            )
        }
    }

    /**
     * Toggle always look at top card of library (visible only to owner)
     */
    fun toggleLookAtTopCard(playerId: String) {
        if (isNetworkGame) {
            sendNetworkAction(NetworkAction.ToggleLookAtTopCard(playerId))
            return
        }

        _uiState.update { currentState ->
            val gameState = currentState.gameState ?: return@update currentState
            val playerIndex = gameState.players.indexOfFirst { it.id == playerId }
            if (playerIndex == -1) return@update currentState

            val player = gameState.players[playerIndex]
            val newLookState = !player.lookAtTopCard
            val updatedPlayer = player.copy(
                lookAtTopCard = newLookState,
                // If stopping look, also stop reveal (can't reveal without looking)
                revealTopCard = if (!newLookState) false else player.revealTopCard
            )
            val updatedPlayers = gameState.players.toMutableList()
            updatedPlayers[playerIndex] = updatedPlayer

            val event = GameEvent.GenericAction(
                playerId = playerId,
                playerName = player.name,
                description = if (newLookState) "is now looking at top card of library" else "stopped looking at top card of library"
            )

            currentState.copy(
                gameState = gameState.copy(players = updatedPlayers).addEvent(event)
            )
        }
    }
}
