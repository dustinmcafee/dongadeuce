package com.dustinmcafee.dongadeuce.network

import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.platform.currentTimeMillis
import com.dustinmcafee.dongadeuce.platform.generateUUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Core game logic engine shared by both P2P (GameServer) and dedicated server (GameRoom).
 * Handles validation, execution, player/deck management, and game state.
 */
class GameEngine(private val maxPlayers: Int = 6) {

    // Player info: playerId -> LobbyPlayer
    private val players = mutableMapOf<String, LobbyPlayer>()

    // Player decks: playerId -> Deck
    private val playerDecks = mutableMapOf<String, Deck>()

    // Lobby state
    private val _lobbyState = MutableStateFlow(
        GameMessage.LobbyState(
            players = emptyList(),
            hostId = "",
            adminId = "",
            maxPlayers = maxPlayers
        )
    )
    val lobbyState: StateFlow<GameMessage.LobbyState> = _lobbyState.asStateFlow()

    // Game state (null until game starts)
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    // Game paused state
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    // Pause reason
    private val _pauseReason = MutableStateFlow<String?>(null)
    val pauseReason: StateFlow<String?> = _pauseReason.asStateFlow()

    // Game started flag
    private val _gameStarted = MutableStateFlow(false)
    val gameStarted: StateFlow<Boolean> = _gameStarted.asStateFlow()

    // Admin player ID (first player to join)
    private var adminId: String = ""

    /**
     * Add a player to the game. Returns the assigned player ID.
     */
    fun addPlayer(name: String, deck: Deck, isAdmin: Boolean = false): String {
        val playerId = generateUUID()
        val uniqueName = generateUniqueName(name)

        players[playerId] = LobbyPlayer(
            id = playerId,
            name = uniqueName,
            hasDeck = true,
            isReady = isAdmin, // Admin is auto-ready
            isHost = isAdmin,  // Keep backward compat
            isAdmin = isAdmin
        )
        playerDecks[playerId] = deck

        if (isAdmin || adminId.isEmpty()) {
            adminId = playerId
        }

        updateLobbyState()
        return playerId
    }

    /**
     * Remove a player from the game.
     */
    fun removePlayer(playerId: String) {
        players.remove(playerId)
        playerDecks.remove(playerId)
        updateLobbyState()
    }

    /**
     * Get player info by ID.
     */
    fun getPlayer(playerId: String): LobbyPlayer? = players[playerId]

    /**
     * Get all player IDs.
     */
    fun getPlayerIds(): Set<String> = players.keys.toSet()

    /**
     * Get the number of players currently in the lobby.
     */
    fun getPlayerCount(): Int = players.size

    /**
     * Check if lobby is full.
     */
    fun isLobbyFull(): Boolean = players.size >= maxPlayers

    /**
     * Set a player's ready status.
     */
    fun setPlayerReady(playerId: String, ready: Boolean) {
        players[playerId]?.let { player ->
            players[playerId] = player.copy(isReady = ready)
        }
        updateLobbyState()
    }

    /**
     * Start the game. Returns true if successful.
     */
    fun startGame(): Boolean {
        if (_gameStarted.value) return false
        if (players.size < 2) return false

        // Check all non-admin players are ready
        val nonAdminPlayers = players.values.filter { !it.isAdmin }
        if (nonAdminPlayers.any { !it.isReady }) return false

        // Create initial game state
        val playerList = players.values.map { lobbyPlayer ->
            Player(
                id = lobbyPlayer.id,
                name = lobbyPlayer.name,
                life = GameConstants.STARTING_LIFE
            )
        }

        val adminPlayer = players[adminId]
        val gameId = generateUUID()
        var initialState = GameState(
            gameId = gameId,
            players = playerList,
            cardInstances = emptyList(),
            activePlayerIndex = 0,
            turnNumber = 1,
            phase = GamePhase.UNTAP,
            gameLog = listOf(
                GameEvent.GameStarted(
                    playerId = adminId,
                    playerName = adminPlayer?.name ?: "Unknown",
                    playerNames = playerList.map { it.name },
                    playerCount = playerList.size
                )
            )
        )

        // Load decks for each player
        players.values.forEach { lobbyPlayer ->
            val deck = playerDecks[lobbyPlayer.id] ?: return false

            // Create card instances for the deck
            val commanderInstance = CardInstance(
                card = deck.commander,
                ownerId = lobbyPlayer.id,
                zone = Zone.COMMAND_ZONE
            )

            // Partner commander if present
            val partnerInstance = deck.partnerCommander?.let {
                CardInstance(
                    card = it,
                    ownerId = lobbyPlayer.id,
                    zone = Zone.COMMAND_ZONE
                )
            }

            val libraryCards = deck.cards.shuffled().map { card ->
                CardInstance(
                    card = card,
                    ownerId = lobbyPlayer.id,
                    zone = Zone.LIBRARY
                )
            }

            initialState = initialState.copy(
                cardInstances = initialState.cardInstances +
                        commanderInstance +
                        listOfNotNull(partnerInstance) +
                        libraryCards
            )
        }

        // Draw starting hands
        playerList.forEach { player ->
            repeat(GameConstants.STARTING_HAND_SIZE) {
                val libraryCards = initialState.cardInstances.filter {
                    it.ownerId == player.id && it.zone == Zone.LIBRARY
                }
                if (libraryCards.isNotEmpty()) {
                    val cardToDraw = libraryCards.first()
                    initialState = initialState.updateCardInstance(cardToDraw.instanceId) {
                        it.moveToZone(Zone.HAND)
                    }
                }
            }
        }

        _gameState.value = initialState
        _gameStarted.value = true

        return true
    }

    /**
     * Execute an action and return the result.
     */
    fun executeAction(action: NetworkAction, playerId: String): ActionResult {
        if (!_gameStarted.value || _isPaused.value) {
            return ActionResult(false, "Game not active")
        }

        val currentState = _gameState.value ?: return ActionResult(false, "No game state")
        if (currentState.players.none { it.id == playerId }) {
            return ActionResult(false, "Player not found")
        }

        // Validate
        val validationResult = validateAction(action, playerId, currentState)
        if (!validationResult.isValid) {
            return ActionResult(false, validationResult.reason)
        }

        // Execute
        val newState = applyAction(action, playerId, currentState)
        _gameState.value = newState

        return ActionResult(true, newState = newState)
    }

    /**
     * Pause the game.
     */
    fun pauseGame(reason: String) {
        _isPaused.value = true
        _pauseReason.value = reason
    }

    /**
     * Resume the game.
     */
    fun resumeGame() {
        _isPaused.value = false
        _pauseReason.value = null
    }

    /**
     * Get the admin player ID.
     */
    fun getAdminId(): String = adminId

    /**
     * Get the current game state.
     */
    fun getCurrentState(): GameState? = _gameState.value

    /**
     * Check if the game has started.
     */
    fun isGameStarted(): Boolean = _gameStarted.value

    /**
     * Validate if a player can perform an action.
     */
    private fun validateAction(action: NetworkAction, playerId: String, state: GameState): ValidationResult {
        val player = state.players.find { it.id == playerId }
            ?: return ValidationResult(false, "Player not found")

        if (player.hasLost) {
            return ValidationResult(false, "You have been eliminated")
        }

        val isActivePlayer = state.activePlayer.id == playerId

        return when (action) {
            is NetworkAction.NextPhase,
            is NetworkAction.PassTurn,
            is NetworkAction.SetPhase -> {
                if (!isActivePlayer) {
                    ValidationResult(false, "Not your turn")
                } else {
                    ValidationResult(true)
                }
            }

            is NetworkAction.Concede -> {
                if (action.playerId != playerId) {
                    ValidationResult(false, "Cannot concede for another player")
                } else {
                    ValidationResult(true)
                }
            }

            is NetworkAction.ToggleRevealTopCard,
            is NetworkAction.ToggleLookAtTopCard -> {
                val targetPlayerId = when (action) {
                    is NetworkAction.ToggleRevealTopCard -> action.playerId
                    is NetworkAction.ToggleLookAtTopCard -> action.playerId
                    else -> return ValidationResult(true)
                }
                if (targetPlayerId != playerId) {
                    ValidationResult(false, "Cannot toggle library visibility for another player")
                } else {
                    ValidationResult(true)
                }
            }

            is NetworkAction.DrawCard -> {
                if (action.playerId != playerId) {
                    ValidationResult(false, "Cannot draw for another player")
                } else {
                    ValidationResult(true)
                }
            }

            is NetworkAction.MoveCard,
            is NetworkAction.ToggleTap,
            is NetworkAction.FlipCard,
            is NetworkAction.ToggleFaceDown -> {
                val cardId = when (action) {
                    is NetworkAction.MoveCard -> action.cardId
                    is NetworkAction.ToggleTap -> action.cardId
                    is NetworkAction.FlipCard -> action.cardId
                    is NetworkAction.ToggleFaceDown -> action.cardId
                    else -> return ValidationResult(true)
                }
                val card = state.cardInstances.find { it.instanceId == cardId }
                if (card == null) {
                    ValidationResult(false, "Card not found")
                } else if (card.ownerId != playerId && card.controllerId != playerId) {
                    ValidationResult(false, "You don't control this card")
                } else {
                    ValidationResult(true)
                }
            }

            is NetworkAction.GiveControlTo -> {
                val card = state.cardInstances.find { it.instanceId == action.cardId }
                if (card == null) {
                    ValidationResult(false, "Card not found")
                } else if (card.controllerId != playerId) {
                    ValidationResult(false, "You don't control this card")
                } else {
                    ValidationResult(true)
                }
            }

            is NetworkAction.UpdateLife,
            is NetworkAction.AddPlayerCounter,
            is NetworkAction.RemovePlayerCounter,
            is NetworkAction.SetPlayerCounter -> {
                ValidationResult(true)
            }

            else -> ValidationResult(true)
        }
    }

    /**
     * Execute an action and return the new game state.
     */
    private fun applyAction(action: NetworkAction, playerId: String, state: GameState): GameState {
        val player = state.players.find { it.id == playerId } ?: return state

        return when (action) {
            is NetworkAction.DrawCard -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val libraryCards = state.cardInstances.filter {
                    it.ownerId == action.playerId && it.zone == Zone.LIBRARY
                }
                if (libraryCards.isEmpty()) {
                    val updatedPlayer = targetPlayer.copy(hasLost = true)
                    val event = GameEvent.PlayerLost(
                        playerId = action.playerId,
                        playerName = targetPlayer.name,
                        reason = "Drew from empty library"
                    )
                    state.updatePlayer(action.playerId) { updatedPlayer }.addEvent(event)
                } else {
                    val cardToDraw = libraryCards.first()
                    val event = GameEvent.CardDrawn(
                        playerId = action.playerId,
                        playerName = targetPlayer.name,
                        cardName = "a card"
                    )
                    state.updateCardInstance(cardToDraw.instanceId) {
                        it.moveToZone(Zone.HAND)
                    }.addEvent(event)
                }
            }

            is NetworkAction.MoveCard -> {
                val card = state.cardInstances.find { it.instanceId == action.cardId } ?: return state
                val oldZone = card.zone

                val shouldRemove = (card.isToken || card.isClone) &&
                    oldZone == Zone.BATTLEFIELD &&
                    action.targetZone != Zone.BATTLEFIELD

                val hideCardName = oldZone == Zone.LIBRARY && action.targetZone == Zone.HAND
                val displayCardName = if (hideCardName) "a card" else card.card.name

                val event = if (action.targetZone == Zone.BATTLEFIELD && oldZone != Zone.BATTLEFIELD) {
                    GameEvent.CardPlayed(
                        playerId = playerId,
                        playerName = player.name,
                        cardName = card.card.name,
                        fromZone = oldZone
                    )
                } else {
                    GameEvent.CardMoved(
                        playerId = playerId,
                        playerName = player.name,
                        cardName = displayCardName,
                        fromZone = oldZone,
                        toZone = action.targetZone
                    )
                }

                if (shouldRemove) {
                    state.copy(
                        cardInstances = state.cardInstances.filter { it.instanceId != action.cardId }
                    ).addEvent(event)
                } else {
                    state.updateCardInstance(action.cardId) { c ->
                        var updated = c.moveToZone(action.targetZone)

                        if (oldZone == Zone.BATTLEFIELD && action.targetZone != Zone.BATTLEFIELD) {
                            updated = updated.copy(
                                counters = emptyMap(),
                                powerModifier = 0,
                                toughnessModifier = 0,
                                isTapped = false,
                                isFlipped = false,
                                doesntUntap = false,
                                attachedTo = null,
                                controllerId = c.ownerId
                            )
                        }

                        if (action.targetZone == Zone.BATTLEFIELD && oldZone != Zone.BATTLEFIELD) {
                            val (gridX, gridY) = state.findNextGridPosition(c.controllerId, updated, excludeCardId = action.cardId)
                            updated = updated.copy(
                                placedTimestamp = currentTimeMillis(),
                                gridX = gridX,
                                gridY = gridY
                            )
                        }
                        updated
                    }.addEvent(event)
                }
            }

            is NetworkAction.ToggleTap -> {
                val card = state.cardInstances.find { it.instanceId == action.cardId } ?: return state
                val newTapped = !card.isTapped
                val event = GameEvent.CardTapped(
                    playerId = playerId,
                    playerName = player.name,
                    cardName = card.card.name,
                    isTapped = newTapped
                )
                state.updateCardInstance(action.cardId) {
                    if (it.isTapped) it.untap() else it.tap()
                }.addEvent(event)
            }

            is NetworkAction.UpdateLife -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val oldLife = targetPlayer.life
                val event = GameEvent.LifeChanged(
                    playerId = action.playerId,
                    playerName = targetPlayer.name,
                    oldLife = oldLife,
                    newLife = action.newLife
                )
                var newState = state.updatePlayer(action.playerId) {
                    it.setLife(action.newLife)
                }.addEvent(event)

                if (action.newLife <= 0) {
                    val lossEvent = GameEvent.PlayerLost(
                        playerId = action.playerId,
                        playerName = targetPlayer.name,
                        reason = "Life reached 0"
                    )
                    newState = newState.updatePlayer(action.playerId) {
                        it.copy(hasLost = true)
                    }.addEvent(lossEvent)
                }
                newState
            }

            is NetworkAction.NextPhase -> {
                val newPhase = state.phase.next()
                val event = GameEvent.PhaseChanged(
                    playerId = state.activePlayer.id,
                    playerName = state.activePlayer.name,
                    newPhase = newPhase
                )
                state.copy(phase = newPhase).addEvent(event)
            }

            is NetworkAction.PassTurn -> {
                val nextIndex = (state.activePlayerIndex + 1) % state.players.size
                val nextPlayer = state.players[nextIndex]
                val event = GameEvent.TurnPassed(
                    playerId = state.activePlayer.id,
                    playerName = state.activePlayer.name,
                    toPlayerId = nextPlayer.id,
                    toPlayerName = nextPlayer.name,
                    turnNumber = state.turnNumber + 1
                )
                state.copy(
                    activePlayerIndex = nextIndex,
                    turnNumber = state.turnNumber + 1,
                    phase = GamePhase.UNTAP
                ).addEvent(event)
            }

            is NetworkAction.SetPhase -> {
                var updatedState = state.copy(phase = action.phase)

                if (action.phase == GamePhase.UNTAP) {
                    val untappedCards = updatedState.cardInstances.map { card ->
                        if (card.controllerId == state.activePlayer.id &&
                            card.zone == Zone.BATTLEFIELD &&
                            card.isTapped &&
                            !card.doesntUntap) {
                            card.copy(isTapped = false)
                        } else {
                            card
                        }
                    }
                    updatedState = updatedState.copy(cardInstances = untappedCards)
                }

                val event = GameEvent.PhaseChanged(
                    playerId = state.activePlayer.id,
                    playerName = state.activePlayer.name,
                    newPhase = action.phase
                )
                updatedState.addEvent(event)
            }

            is NetworkAction.Concede -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val event = GameEvent.PlayerLost(
                    playerId = action.playerId,
                    playerName = targetPlayer.name,
                    reason = "Conceded"
                )
                state.updatePlayer(action.playerId) { p ->
                    p.copy(hasLost = true, life = 0)
                }.addEvent(event)
            }

            is NetworkAction.ToggleRevealTopCard -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val newRevealState = !targetPlayer.revealTopCard
                val event = GameEvent.GenericAction(
                    playerId = action.playerId,
                    playerName = targetPlayer.name,
                    description = if (newRevealState) "is now revealing top card of library" else "stopped revealing top card of library"
                )
                state.updatePlayer(action.playerId) { p ->
                    p.copy(
                        revealTopCard = newRevealState,
                        lookAtTopCard = if (newRevealState) true else p.lookAtTopCard
                    )
                }.addEvent(event)
            }

            is NetworkAction.ToggleLookAtTopCard -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val newLookState = !targetPlayer.lookAtTopCard
                val event = GameEvent.GenericAction(
                    playerId = action.playerId,
                    playerName = targetPlayer.name,
                    description = if (newLookState) "is now looking at top card of library" else "stopped looking at top card of library"
                )
                state.updatePlayer(action.playerId) { p ->
                    p.copy(
                        lookAtTopCard = newLookState,
                        revealTopCard = if (!newLookState) false else p.revealTopCard
                    )
                }.addEvent(event)
            }

            is NetworkAction.UntapAll -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val cardsToUntap = state.cardInstances.filter {
                    it.controllerId == action.playerId &&
                    it.zone == Zone.BATTLEFIELD &&
                    it.isTapped &&
                    !it.doesntUntap
                }

                var newState = state
                cardsToUntap.forEach { card ->
                    newState = newState.updateCardInstance(card.instanceId) { it.untap() }
                }

                if (cardsToUntap.isNotEmpty()) {
                    val event = GameEvent.UntapAll(
                        playerId = action.playerId,
                        playerName = targetPlayer.name,
                        cardCount = cardsToUntap.size
                    )
                    newState = newState.addEvent(event)
                }
                newState
            }

            is NetworkAction.AddCardCounter -> {
                val card = state.cardInstances.find { it.instanceId == action.cardId } ?: return state
                val oldAmount = card.counters[action.counterType] ?: 0
                val newAmount = oldAmount + action.amount
                val event = GameEvent.CardCounterChanged(
                    playerId = playerId,
                    playerName = player.name,
                    cardName = card.card.name,
                    counterType = action.counterType,
                    oldAmount = oldAmount,
                    newAmount = newAmount
                )
                state.updateCardInstance(action.cardId) {
                    it.addCounter(action.counterType, action.amount)
                }.addEvent(event)
            }

            is NetworkAction.UpdateCommanderDamage -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val commander = state.cardInstances.find { it.instanceId == action.commanderId } ?: return state
                val sourcePlayer = state.players.find { it.id == commander.ownerId } ?: return state

                val event = GameEvent.CommanderDamageDealt(
                    playerId = sourcePlayer.id,
                    playerName = sourcePlayer.name,
                    sourceCommanderName = commander.card.name,
                    targetPlayerName = targetPlayer.name,
                    damage = action.newDamage - (targetPlayer.commanderDamage[action.commanderId] ?: 0),
                    totalDamage = action.newDamage
                )

                var newState = state.updatePlayer(action.playerId) {
                    it.takeCommanderDamage(action.commanderId, action.newDamage - (it.commanderDamage[action.commanderId] ?: 0))
                }.addEvent(event)

                if (action.newDamage >= GameConstants.COMMANDER_DAMAGE_THRESHOLD) {
                    val lossEvent = GameEvent.PlayerLost(
                        playerId = action.playerId,
                        playerName = targetPlayer.name,
                        reason = "Received ${action.newDamage} commander damage from ${commander.card.name}"
                    )
                    newState = newState.updatePlayer(action.playerId) {
                        it.copy(hasLost = true)
                    }.addEvent(lossEvent)
                }
                newState
            }

            is NetworkAction.SendChatMessage -> {
                val event = GameEvent.ChatMessage(
                    playerId = action.playerId,
                    playerName = player.name,
                    message = action.message
                )
                state.addEvent(event)
            }

            is NetworkAction.AddPlayerCounter -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val oldAmount = targetPlayer.getCounter(action.counterType)
                val newAmount = oldAmount + action.amount
                val event = GameEvent.PlayerCounterChanged(
                    playerId = action.playerId,
                    playerName = targetPlayer.name,
                    counterType = action.counterType,
                    oldAmount = oldAmount,
                    newAmount = newAmount
                )
                state.updatePlayer(action.playerId) {
                    it.addCounter(action.counterType, action.amount)
                }.addEvent(event)
            }

            is NetworkAction.RemovePlayerCounter -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val oldAmount = targetPlayer.getCounter(action.counterType)
                val newAmount = (oldAmount - action.amount).coerceAtLeast(0)
                val event = GameEvent.PlayerCounterChanged(
                    playerId = action.playerId,
                    playerName = targetPlayer.name,
                    counterType = action.counterType,
                    oldAmount = oldAmount,
                    newAmount = newAmount
                )
                state.updatePlayer(action.playerId) {
                    it.addCounter(action.counterType, -action.amount)
                }.addEvent(event)
            }

            is NetworkAction.SetPlayerCounter -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val oldAmount = targetPlayer.getCounter(action.counterType)
                val event = GameEvent.PlayerCounterChanged(
                    playerId = action.playerId,
                    playerName = targetPlayer.name,
                    counterType = action.counterType,
                    oldAmount = oldAmount,
                    newAmount = action.amount
                )
                state.updatePlayer(action.playerId) {
                    it.setCounter(action.counterType, action.amount)
                }.addEvent(event)
            }

            is NetworkAction.RemoveCardCounter -> {
                val card = state.cardInstances.find { it.instanceId == action.cardId } ?: return state
                val oldAmount = card.counters[action.counterType] ?: 0
                val newAmount = (oldAmount - action.amount).coerceAtLeast(0)
                val event = GameEvent.CardCounterChanged(
                    playerId = playerId,
                    playerName = player.name,
                    cardName = card.card.name,
                    counterType = action.counterType,
                    oldAmount = oldAmount,
                    newAmount = newAmount
                )
                state.updateCardInstance(action.cardId) { c ->
                    c.copy(
                        counters = if (newAmount > 0) {
                            c.counters + (action.counterType to newAmount)
                        } else {
                            c.counters - action.counterType
                        }
                    )
                }.addEvent(event)
            }

            is NetworkAction.SetCardCounter -> {
                val card = state.cardInstances.find { it.instanceId == action.cardId } ?: return state
                val oldAmount = card.counters[action.counterType] ?: 0
                val event = GameEvent.CardCounterChanged(
                    playerId = playerId,
                    playerName = player.name,
                    cardName = card.card.name,
                    counterType = action.counterType,
                    oldAmount = oldAmount,
                    newAmount = action.amount
                )
                state.updateCardInstance(action.cardId) { c ->
                    c.copy(
                        counters = if (action.amount > 0) {
                            c.counters + (action.counterType to action.amount)
                        } else {
                            c.counters - action.counterType
                        }
                    )
                }.addEvent(event)
            }

            is NetworkAction.GiveControlTo -> {
                val card = state.cardInstances.find { it.instanceId == action.cardId } ?: return state
                val fromPlayer = state.players.find { it.id == card.controllerId } ?: return state
                val toPlayer = state.players.find { it.id == action.newControllerId } ?: return state
                val (gridX, gridY) = state.findNextGridPosition(action.newControllerId, excludeCardId = action.cardId)
                val event = GameEvent.ControlChanged(
                    playerId = fromPlayer.id,
                    playerName = fromPlayer.name,
                    cardName = card.card.name,
                    toPlayerName = toPlayer.name
                )
                state.updateCardInstance(action.cardId) {
                    it.changeController(action.newControllerId).moveToZone(Zone.BATTLEFIELD)
                        .copy(gridX = gridX, gridY = gridY, placedTimestamp = currentTimeMillis())
                }.addEvent(event)
            }

            is NetworkAction.ModifyPower -> {
                state.updateCardInstance(action.cardId) { card ->
                    card.copy(powerModifier = card.powerModifier + action.amount)
                }
            }

            is NetworkAction.ModifyToughness -> {
                state.updateCardInstance(action.cardId) { card ->
                    card.copy(toughnessModifier = card.toughnessModifier + action.amount)
                }
            }

            is NetworkAction.ModifyPowerToughness -> {
                state.updateCardInstance(action.cardId) { card ->
                    card.copy(
                        powerModifier = card.powerModifier + action.amount,
                        toughnessModifier = card.toughnessModifier + action.amount
                    )
                }
            }

            is NetworkAction.SetPowerToughness -> {
                state.updateCardInstance(action.cardId) { card ->
                    val basePower = card.card.power?.toIntOrNull() ?: 0
                    val baseToughness = card.card.toughness?.toIntOrNull() ?: 0
                    card.copy(
                        powerModifier = action.newPower - basePower,
                        toughnessModifier = action.newToughness - baseToughness
                    )
                }
            }

            is NetworkAction.ResetPowerToughness -> {
                state.updateCardInstance(action.cardId) { card ->
                    card.copy(powerModifier = 0, toughnessModifier = 0)
                }
            }

            is NetworkAction.FlowPower -> {
                state.updateCardInstance(action.cardId) { card ->
                    card.copy(
                        powerModifier = card.powerModifier + 1,
                        toughnessModifier = card.toughnessModifier - 1
                    )
                }
            }

            is NetworkAction.FlowToughness -> {
                state.updateCardInstance(action.cardId) { card ->
                    card.copy(
                        powerModifier = card.powerModifier - 1,
                        toughnessModifier = card.toughnessModifier + 1
                    )
                }
            }

            is NetworkAction.ToggleDoesntUntap -> {
                state.updateCardInstance(action.cardId) { card ->
                    card.copy(doesntUntap = !card.doesntUntap)
                }
            }

            is NetworkAction.SetAnnotation -> {
                state.updateCardInstance(action.cardId) { card ->
                    card.copy(annotation = if (action.annotation.isNullOrBlank()) null else action.annotation)
                }
            }

            is NetworkAction.FlipCard -> {
                state.updateCardInstance(action.cardId) { it.flip() }
            }

            is NetworkAction.ToggleFaceDown -> {
                state.updateCardInstance(action.cardId) { card ->
                    card.copy(isFaceDown = !card.isFaceDown)
                }
            }

            is NetworkAction.PlayFaceDown -> {
                val card = state.cardInstances.find { it.instanceId == action.cardId } ?: return state
                val (gridX, gridY) = state.findNextGridPosition(card.controllerId, excludeCardId = action.cardId)
                state.updateCardInstance(action.cardId) { c ->
                    c.copy(
                        isFaceDown = true,
                        zone = Zone.BATTLEFIELD,
                        placedTimestamp = currentTimeMillis(),
                        gridX = gridX,
                        gridY = gridY
                    )
                }
            }

            is NetworkAction.AttachCard -> {
                state.updateCardInstance(action.sourceId) { card ->
                    card.copy(attachedTo = action.targetId)
                }
            }

            is NetworkAction.DetachCard -> {
                state.updateCardInstance(action.cardId) { card ->
                    card.copy(attachedTo = null)
                }
            }

            is NetworkAction.MillCards -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val libraryCards = state.cardInstances.filter {
                    it.ownerId == action.playerId && it.zone == Zone.LIBRARY
                }
                val cardsToMill = libraryCards.takeLast(action.count.coerceAtMost(libraryCards.size))

                var newState = state.copy(
                    cardInstances = state.cardInstances.map { card ->
                        if (card.instanceId in cardsToMill.map { it.instanceId }) {
                            card.moveToZone(Zone.GRAVEYARD)
                        } else {
                            card
                        }
                    }
                )

                if (cardsToMill.isNotEmpty()) {
                    val event = GameEvent.CardsMilled(
                        playerId = action.playerId,
                        playerName = targetPlayer.name,
                        cardCount = cardsToMill.size
                    )
                    newState = newState.addEvent(event)
                }
                newState
            }

            is NetworkAction.Mulligan -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state

                var newState = state.copy(
                    cardInstances = state.cardInstances.map { card ->
                        if (card.ownerId == action.playerId && card.zone == Zone.HAND) {
                            card.moveToZone(Zone.LIBRARY)
                        } else {
                            card
                        }
                    }
                )

                val libraryCards = newState.cardInstances.filter {
                    it.ownerId == action.playerId && it.zone == Zone.LIBRARY
                }.shuffled()
                val otherCards = newState.cardInstances.filter {
                    !(it.ownerId == action.playerId && it.zone == Zone.LIBRARY)
                }
                newState = newState.copy(cardInstances = otherCards + libraryCards)

                val updatedLibrary = newState.cardInstances.filter {
                    it.ownerId == action.playerId && it.zone == Zone.LIBRARY
                }
                val cardsToDraw = updatedLibrary.take(GameConstants.STARTING_HAND_SIZE.coerceAtMost(updatedLibrary.size))

                newState = newState.copy(
                    cardInstances = newState.cardInstances.map { card ->
                        if (card.instanceId in cardsToDraw.map { it.instanceId }) {
                            card.moveToZone(Zone.HAND)
                        } else {
                            card
                        }
                    }
                )

                val event = GameEvent.MulliganTaken(
                    playerId = action.playerId,
                    playerName = targetPlayer.name,
                    newHandSize = cardsToDraw.size
                )
                newState.addEvent(event)
            }

            is NetworkAction.MoveCardToTopOfLibrary -> {
                val card = state.cardInstances.find { it.instanceId == action.cardId } ?: return state
                val updatedCard = card.moveToZone(Zone.LIBRARY)
                val otherCards = state.cardInstances.filter { it.instanceId != action.cardId }
                val libraryCards = otherCards.filter { it.ownerId == card.ownerId && it.zone == Zone.LIBRARY }
                val nonLibraryCards = otherCards.filter { !(it.ownerId == card.ownerId && it.zone == Zone.LIBRARY) }
                state.copy(cardInstances = nonLibraryCards + libraryCards + listOf(updatedCard))
            }

            is NetworkAction.MoveCardToBottomOfLibrary -> {
                val card = state.cardInstances.find { it.instanceId == action.cardId } ?: return state
                val updatedCard = card.moveToZone(Zone.LIBRARY)
                val otherCards = state.cardInstances.filter { it.instanceId != action.cardId }
                val libraryCards = otherCards.filter { it.ownerId == card.ownerId && it.zone == Zone.LIBRARY }
                val nonLibraryCards = otherCards.filter { !(it.ownerId == card.ownerId && it.zone == Zone.LIBRARY) }
                state.copy(cardInstances = nonLibraryCards + listOf(updatedCard) + libraryCards)
            }

            is NetworkAction.MoveCardToLibraryPosition -> {
                val card = state.cardInstances.find { it.instanceId == action.cardId } ?: return state
                val updatedCard = card.moveToZone(Zone.LIBRARY)
                val otherCards = state.cardInstances.filter { it.instanceId != action.cardId }
                val libraryCards = otherCards.filter { it.ownerId == card.ownerId && it.zone == Zone.LIBRARY }.toMutableList()
                val nonLibraryCards = otherCards.filter { !(it.ownerId == card.ownerId && it.zone == Zone.LIBRARY) }
                val insertIndex = (libraryCards.size - action.positionFromTop + 1).coerceIn(0, libraryCards.size)
                libraryCards.add(insertIndex, updatedCard)
                state.copy(cardInstances = nonLibraryCards + libraryCards)
            }

            is NetworkAction.MoveCardToLibraryPositionFromBottom -> {
                val card = state.cardInstances.find { it.instanceId == action.cardId } ?: return state
                val updatedCard = card.moveToZone(Zone.LIBRARY)
                val otherCards = state.cardInstances.filter { it.instanceId != action.cardId }
                val libraryCards = otherCards.filter { it.ownerId == card.ownerId && it.zone == Zone.LIBRARY }.toMutableList()
                val nonLibraryCards = otherCards.filter { !(it.ownerId == card.ownerId && it.zone == Zone.LIBRARY) }
                val insertIndex = (action.positionFromBottom - 1).coerceIn(0, libraryCards.size)
                libraryCards.add(insertIndex, updatedCard)
                state.copy(cardInstances = nonLibraryCards + libraryCards)
            }

            is NetworkAction.ShuffleLibrary -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val libraryCards = state.cardInstances.filter {
                    it.ownerId == action.playerId && it.zone == Zone.LIBRARY
                }.shuffled()
                val otherCards = state.cardInstances.filter {
                    !(it.ownerId == action.playerId && it.zone == Zone.LIBRARY)
                }
                val event = GameEvent.LibraryShuffled(
                    playerId = action.playerId,
                    playerName = targetPlayer.name
                )
                state.copy(cardInstances = otherCards + libraryCards).addEvent(event)
            }

            is NetworkAction.ShuffleTopCards -> {
                val libraryCards = state.cardInstances.filter {
                    it.ownerId == action.playerId && it.zone == Zone.LIBRARY
                }
                if (libraryCards.size <= 1 || action.count <= 1) return state

                val actualCount = action.count.coerceAtMost(libraryCards.size)
                val topCards = libraryCards.takeLast(actualCount).shuffled()
                val remainingCards = libraryCards.dropLast(actualCount)
                val otherCards = state.cardInstances.filter {
                    !(it.ownerId == action.playerId && it.zone == Zone.LIBRARY)
                }
                state.copy(cardInstances = otherCards + remainingCards + topCards)
            }

            is NetworkAction.ShuffleBottomCards -> {
                val libraryCards = state.cardInstances.filter {
                    it.ownerId == action.playerId && it.zone == Zone.LIBRARY
                }
                if (libraryCards.size <= 1 || action.count <= 1) return state

                val actualCount = action.count.coerceAtMost(libraryCards.size)
                val bottomCards = libraryCards.take(actualCount).shuffled()
                val remainingCards = libraryCards.drop(actualCount)
                val otherCards = state.cardInstances.filter {
                    !(it.ownerId == action.playerId && it.zone == Zone.LIBRARY)
                }
                state.copy(cardInstances = otherCards + bottomCards + remainingCards)
            }

            is NetworkAction.MoveTopCardsToZone -> {
                val libraryCards = state.cardInstances.filter {
                    it.ownerId == action.playerId && it.zone == Zone.LIBRARY
                }
                val topCards = libraryCards.takeLast(action.count.coerceAtMost(libraryCards.size))

                state.copy(
                    cardInstances = state.cardInstances.map { card ->
                        if (card.instanceId in topCards.map { it.instanceId }) {
                            card.moveToZone(action.targetZone)
                        } else {
                            card
                        }
                    }
                )
            }

            is NetworkAction.MoveBottomCardsToZone -> {
                val libraryCards = state.cardInstances.filter {
                    it.ownerId == action.playerId && it.zone == Zone.LIBRARY
                }
                val bottomCards = libraryCards.take(action.count.coerceAtMost(libraryCards.size))

                state.copy(
                    cardInstances = state.cardInstances.map { card ->
                        if (card.instanceId in bottomCards.map { it.instanceId }) {
                            card.moveToZone(action.targetZone)
                        } else {
                            card
                        }
                    }
                )
            }

            is NetworkAction.MoveBottomCardToTop -> {
                val libraryCards = state.cardInstances.filter {
                    it.ownerId == action.playerId && it.zone == Zone.LIBRARY
                }
                if (libraryCards.isEmpty()) {
                    state
                } else {
                    val bottomCard = libraryCards.first()
                    val remainingCards = libraryCards.drop(1)
                    val otherCards = state.cardInstances.filter {
                        !(it.ownerId == action.playerId && it.zone == Zone.LIBRARY)
                    }
                    state.copy(cardInstances = otherCards + remainingCards + bottomCard)
                }
            }

            is NetworkAction.CreateToken -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val tokenCard = Card(
                    name = action.tokenName,
                    type = action.tokenType,
                    power = action.power,
                    toughness = action.toughness,
                    colors = if (action.color.isNotBlank()) listOf(action.color) else emptyList(),
                    imageUri = action.imageUri,
                    scryfallId = null
                )
                var tempState = state
                val tokenInstances = List(action.quantity) {
                    val (gridX, gridY) = tempState.findNextGridPosition(action.playerId)
                    val token = CardInstance(
                        card = tokenCard,
                        ownerId = action.playerId,
                        zone = Zone.BATTLEFIELD,
                        gridX = gridX,
                        gridY = gridY,
                        isToken = true
                    )
                    tempState = tempState.copy(cardInstances = tempState.cardInstances + token)
                    token
                }
                val event = GameEvent.TokenCreated(
                    playerId = action.playerId,
                    playerName = targetPlayer.name,
                    tokenName = action.tokenName,
                    quantity = action.quantity
                )
                state.copy(cardInstances = state.cardInstances + tokenInstances).addEvent(event)
            }

            is NetworkAction.CloneCard -> {
                val targetPlayer = state.players.find { it.id == action.newOwnerId } ?: return state
                val originalCard = state.cardInstances.find { it.instanceId == action.cardId } ?: return state
                var tempState = state
                val clones = List(action.quantity) {
                    var clone = originalCard.createClone(action.newOwnerId, action.targetZone)
                    if (action.targetZone == Zone.BATTLEFIELD) {
                        val (gridX, gridY) = tempState.findNextGridPosition(action.newOwnerId)
                        clone = clone.copy(gridX = gridX, gridY = gridY)
                        tempState = tempState.copy(cardInstances = tempState.cardInstances + clone)
                    }
                    clone
                }
                val event = GameEvent.CardCloned(
                    playerId = action.newOwnerId,
                    playerName = targetPlayer.name,
                    cardName = originalCard.card.name,
                    quantity = action.quantity
                )
                state.copy(cardInstances = state.cardInstances + clones).addEvent(event)
            }

            is NetworkAction.LogDieRoll -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val event = GameEvent.DieRolled(
                    playerId = action.playerId,
                    playerName = targetPlayer.name,
                    dieType = action.dieType,
                    result = action.result,
                    numberOfDice = action.numberOfDice,
                    individualResults = action.individualResults
                )
                state.addEvent(event)
            }

            is NetworkAction.UpdateCardGridPosition -> {
                val card = state.cardInstances.find { it.instanceId == action.cardId } ?: return state
                val positions = state.computeBattlefieldPositions(card.controllerId)
                val targetPos = Pair(action.gridX, action.gridY)
                val cardsAtTarget = positions.count { (id, pos) -> id != action.cardId && pos == targetPos }
                if (cardsAtTarget >= 3) {
                    state
                } else {
                    state.updateCardInstance(action.cardId) { c ->
                        c.setGridPosition(action.gridX, action.gridY)
                    }
                }
            }
        }
    }

    /**
     * Generate a unique player name by appending (1), (2), etc. if name already exists.
     */
    private fun generateUniqueName(baseName: String): String {
        val existingNames = players.values.map { it.name }.toSet()
        if (baseName !in existingNames) return baseName

        var counter = 1
        var newName = "$baseName ($counter)"
        while (newName in existingNames) {
            counter++
            newName = "$baseName ($counter)"
        }
        return newName
    }

    /**
     * Update lobby state from players map.
     */
    private fun updateLobbyState() {
        _lobbyState.update {
            it.copy(
                players = players.values.toList(),
                hostId = adminId,
                adminId = adminId
            )
        }
    }
}

/**
 * Result of executing a game action.
 */
data class ActionResult(
    val success: Boolean,
    val reason: String = "",
    val newState: GameState? = null
)

/**
 * Validation result for action validation.
 */
private data class ValidationResult(
    val isValid: Boolean,
    val reason: String = ""
)

