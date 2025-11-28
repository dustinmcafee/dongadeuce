package com.dustinmcafee.dongadeuce.network

import com.dustinmcafee.dongadeuce.models.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.time.Duration

/**
 * WebSocket game server for hosting multiplayer games.
 * Host-authoritative: all game state changes go through this server.
 */
class GameServer(
    private val port: Int = 8080,
    private val hostName: String = "Host",
    private val hostDeck: Deck,
    private val maxPlayers: Int = 4
) {
    private var server: ApplicationEngine? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Connected clients: playerId -> session
    private val clients = ConcurrentHashMap<String, WebSocketSession>()

    // Player info: playerId -> LobbyPlayer
    private val players = ConcurrentHashMap<String, LobbyPlayer>()

    // Player decks: playerId -> Deck
    private val playerDecks = ConcurrentHashMap<String, Deck>()

    // Host player ID
    private var hostId: String = UUID.randomUUID().toString()

    // Lobby state
    private val _lobbyState = MutableStateFlow(
        GameMessage.LobbyState(
            players = emptyList(),
            hostId = hostId,
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

    // JSON serializer
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Start the WebSocket server
     */
    fun start(): String {
        // Add host as first player
        players[hostId] = LobbyPlayer(
            id = hostId,
            name = hostName,
            hasDeck = true,
            isReady = true,
            isHost = true
        )
        playerDecks[hostId] = hostDeck
        updateLobbyState()

        server = embeddedServer(Netty, port = port) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(30)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                webSocket("/game") {
                    handleConnection(this)
                }
            }
        }.start(wait = false)

        return "ws://localhost:$port/game"
    }

    /**
     * Stop the server
     */
    fun stop() {
        scope.launch {
            // Notify all clients
            broadcastToAll(GameMessage.PlayerLeft(
                playerId = hostId,
                playerName = hostName,
                reason = "Host closed the game"
            ))

            // Close all connections
            clients.values.forEach { session ->
                try {
                    session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server shutting down"))
                } catch (_: Exception) {}
            }
            clients.clear()
            players.clear()
            playerDecks.clear()
        }

        server?.stop(1000, 2000)
        server = null
        scope.cancel()
    }

    /**
     * Handle a new WebSocket connection
     */
    private suspend fun handleConnection(session: WebSocketSession) {
        var playerId: String? = null

        try {
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        val message = try {
                            json.decodeFromString<GameMessage>(text)
                        } catch (e: Exception) {
                            sendTo(session, GameMessage.Error(ErrorCode.UNKNOWN, "Invalid message format"))
                            continue
                        }

                        when (message) {
                            is GameMessage.PlayerJoin -> {
                                // New player joining
                                if (_gameStarted.value) {
                                    sendTo(session, GameMessage.Error(ErrorCode.GAME_ALREADY_STARTED, "Game has already started"))
                                    session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Game already started"))
                                    return
                                }

                                if (players.size >= maxPlayers) {
                                    sendTo(session, GameMessage.Error(ErrorCode.LOBBY_FULL, "Lobby is full"))
                                    session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Lobby full"))
                                    return
                                }

                                // Assign player ID and register
                                playerId = UUID.randomUUID().toString()
                                clients[playerId] = session

                                // Generate unique name if there's a duplicate
                                val uniqueName = generateUniqueName(message.playerName)

                                players[playerId] = LobbyPlayer(
                                    id = playerId,
                                    name = uniqueName,
                                    hasDeck = true,
                                    isReady = false,
                                    isHost = false
                                )
                                playerDecks[playerId] = message.deck

                                // Confirm join to the player with the (possibly modified) unique name
                                sendTo(session, GameMessage.PlayerJoined(playerId, uniqueName))

                                // Broadcast updated lobby state
                                updateLobbyState()
                                broadcastToAll(_lobbyState.value)
                            }

                            is GameMessage.PlayerReady -> {
                                if (playerId != null) {
                                    players[playerId]?.let { player ->
                                        players[playerId] = player.copy(isReady = message.isReady)
                                        updateLobbyState()
                                        broadcastToAll(_lobbyState.value)
                                    }
                                }
                            }

                            is GameMessage.GameAction -> {
                                if (_gameStarted.value && !_isPaused.value) {
                                    handleGameAction(message.action, message.playerId, message.actionId)
                                }
                            }

                            is GameMessage.Chat -> {
                                // Broadcast chat to all players
                                broadcastToAll(message)

                                // Also add to game log if game has started
                                if (_gameStarted.value) {
                                    _gameState.value?.let { state ->
                                        val player = state.players.find { it.id == message.playerId }
                                        if (player != null) {
                                            val event = GameEvent.ChatMessage(
                                                playerId = message.playerId,
                                                playerName = player.name,
                                                message = message.message
                                            )
                                            _gameState.value = state.addEvent(event)
                                            broadcastStateUpdate()
                                        }
                                    }
                                }
                            }

                            is GameMessage.Ping -> {
                                sendTo(session, GameMessage.Pong(message.timestamp))
                            }

                            else -> {
                                // Ignore other message types from clients
                            }
                        }
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            // Connection closed or error
        } finally {
            // Handle disconnect
            if (playerId != null) {
                handleDisconnect(playerId)
            }
        }
    }

    /**
     * Handle player disconnect
     */
    private suspend fun handleDisconnect(playerId: String) {
        val player = players[playerId] ?: return

        clients.remove(playerId)

        if (_gameStarted.value) {
            // Game in progress - pause
            _isPaused.value = true
            _pauseReason.value = "${player.name} disconnected"

            broadcastToAll(GameMessage.Pause(
                reason = "${player.name} disconnected",
                disconnectedPlayerId = playerId
            ))
        } else {
            // Still in lobby - just remove player
            players.remove(playerId)
            playerDecks.remove(playerId)
            updateLobbyState()
            broadcastToAll(_lobbyState.value)
            broadcastToAll(GameMessage.PlayerLeft(
                playerId = playerId,
                playerName = player.name,
                reason = "Disconnected"
            ))
        }
    }

    /**
     * Kick a player (host only)
     */
    suspend fun kickPlayer(playerId: String) {
        val player = players[playerId] ?: return
        if (player.isHost) return // Can't kick host

        val session = clients[playerId]

        // Send kick message
        session?.let {
            sendTo(it, GameMessage.KickPlayer(playerId, "Kicked by host"))
            it.close(CloseReason(CloseReason.Codes.NORMAL, "Kicked by host"))
        }

        // Remove from tracking
        clients.remove(playerId)
        players.remove(playerId)
        playerDecks.remove(playerId)

        // Broadcast
        updateLobbyState()
        broadcastToAll(_lobbyState.value)
        broadcastToAll(GameMessage.PlayerLeft(
            playerId = playerId,
            playerName = player.name,
            reason = "Kicked by host"
        ))
    }

    /**
     * Start the game (host only)
     */
    fun startGame(): Boolean {
        if (_gameStarted.value) return false
        if (players.size < 2) return false

        // Check all players are ready
        val nonHostPlayers = players.values.filter { !it.isHost }
        if (nonHostPlayers.any { !it.isReady }) return false

        // Create initial game state
        val playerList = players.values.mapIndexed { index, lobbyPlayer ->
            Player(
                id = lobbyPlayer.id,
                name = lobbyPlayer.name,
                life = GameConstants.STARTING_LIFE
            )
        }

        val gameId = UUID.randomUUID().toString()
        var initialState = GameState(
            gameId = gameId,
            players = playerList,
            cardInstances = emptyList(),
            activePlayerIndex = 0,
            turnNumber = 1,
            phase = GamePhase.UNTAP,
            gameLog = listOf(
                GameEvent.GameStarted(
                    playerId = hostId,
                    playerName = hostName,
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

            val libraryCards = deck.cards.shuffled().mapIndexed { _, card ->
                CardInstance(
                    card = card,
                    ownerId = lobbyPlayer.id,
                    zone = Zone.LIBRARY
                )
            }

            initialState = initialState.copy(
                cardInstances = initialState.cardInstances + commanderInstance + libraryCards
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

        // Broadcast game start
        scope.launch {
            broadcastToAll(GameMessage.GameStarting(initialState))
        }

        return true
    }

    /**
     * Execute an action from the host directly (not via WebSocket)
     * This allows the host to participate in the game without a separate client connection
     */
    fun executeHostAction(action: NetworkAction, playerId: String) {
        if (!_gameStarted.value || _isPaused.value) return

        val currentState = _gameState.value ?: return
        val player = currentState.players.find { it.id == playerId } ?: return

        // Validate the action
        val validationResult = validateAction(action, playerId, currentState)
        if (!validationResult.isValid) {
            println("Host action rejected: ${validationResult.reason}")
            return
        }

        // Execute the action
        val newState = executeAction(action, playerId, currentState)
        _gameState.value = newState

        // Broadcast updated state to all clients
        scope.launch {
            broadcastStateUpdate(System.currentTimeMillis())
        }
    }

    /**
     * Generate a unique player name by appending (1), (2), etc. if name already exists
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
     * Handle a game action from a client
     */
    private suspend fun handleGameAction(action: NetworkAction, playerId: String, actionId: Long) {
        val currentState = _gameState.value ?: return
        val player = currentState.players.find { it.id == playerId } ?: return

        // Validate the action
        val validationResult = validateAction(action, playerId, currentState)
        if (!validationResult.isValid) {
            clients[playerId]?.let { session ->
                sendTo(session, GameMessage.ActionRejected(actionId, validationResult.reason))
            }
            return
        }

        // Execute the action
        val newState = executeAction(action, playerId, currentState)
        _gameState.value = newState

        // Broadcast updated state
        broadcastStateUpdate(actionId)
    }

    /**
     * Validate if a player can perform an action
     */
    private fun validateAction(action: NetworkAction, playerId: String, state: GameState): ValidationResult {
        val player = state.players.find { it.id == playerId }
            ?: return ValidationResult(false, "Player not found")

        if (player.hasLost) {
            return ValidationResult(false, "You have been eliminated")
        }

        // Check turn-based actions
        val isActivePlayer = state.activePlayer.id == playerId

        return when (action) {
            // Actions that require being the active player
            is NetworkAction.NextPhase,
            is NetworkAction.PassTurn,
            is NetworkAction.SetPhase -> {
                if (!isActivePlayer) {
                    ValidationResult(false, "Not your turn")
                } else {
                    ValidationResult(true)
                }
            }

            // Concede - only for yourself
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

            // Actions on your own cards - can do anytime
            is NetworkAction.DrawCard -> {
                if (action.playerId != playerId) {
                    ValidationResult(false, "Cannot draw for another player")
                } else {
                    ValidationResult(true)
                }
            }

            // Card actions - verify ownership/control
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

            // Player-specific actions
            is NetworkAction.UpdateLife,
            is NetworkAction.AddPlayerCounter,
            is NetworkAction.RemovePlayerCounter,
            is NetworkAction.SetPlayerCounter -> {
                // Players can modify their own stats, or active player can modify others (for dealing damage)
                ValidationResult(true)
            }

            // Default: allow action
            else -> ValidationResult(true)
        }
    }

    /**
     * Execute an action and return the new game state
     */
    private fun executeAction(action: NetworkAction, playerId: String, state: GameState): GameState {
        val player = state.players.find { it.id == playerId } ?: return state

        return when (action) {
            is NetworkAction.DrawCard -> {
                val targetPlayer = state.players.find { it.id == action.playerId } ?: return state
                val libraryCards = state.cardInstances.filter {
                    it.ownerId == action.playerId && it.zone == Zone.LIBRARY
                }
                if (libraryCards.isEmpty()) {
                    // Draw from empty library - player loses
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
                        cardName = "a card" // Hide card name
                    )
                    state.updateCardInstance(cardToDraw.instanceId) {
                        it.moveToZone(Zone.HAND)
                    }.addEvent(event)
                }
            }

            is NetworkAction.MoveCard -> {
                val card = state.cardInstances.find { it.instanceId == action.cardId } ?: return state
                val oldZone = card.zone
                val event = if (action.targetZone == Zone.BATTLEFIELD) {
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
                        cardName = card.card.name,
                        fromZone = oldZone,
                        toZone = action.targetZone
                    )
                }
                state.updateCardInstance(action.cardId) {
                    it.moveToZone(action.targetZone)
                }.addEvent(event)
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

                // Check for death
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
                val event = GameEvent.PhaseChanged(
                    playerId = state.activePlayer.id,
                    playerName = state.activePlayer.name,
                    newPhase = action.phase
                )
                state.copy(phase = action.phase).addEvent(event)
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

                // Check for commander damage death
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
                val event = GameEvent.ControlChanged(
                    playerId = fromPlayer.id,
                    playerName = fromPlayer.name,
                    cardName = card.card.name,
                    toPlayerName = toPlayer.name
                )
                state.updateCardInstance(action.cardId) {
                    it.changeController(action.newControllerId).moveToZone(Zone.BATTLEFIELD)
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
                state.updateCardInstance(action.cardId) { card ->
                    card.copy(
                        isFaceDown = true,
                        zone = Zone.BATTLEFIELD,
                        placedTimestamp = System.currentTimeMillis()
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

                // Move hand to library
                var newState = state.copy(
                    cardInstances = state.cardInstances.map { card ->
                        if (card.ownerId == action.playerId && card.zone == Zone.HAND) {
                            card.moveToZone(Zone.LIBRARY)
                        } else {
                            card
                        }
                    }
                )

                // Shuffle library
                val libraryCards = newState.cardInstances.filter {
                    it.ownerId == action.playerId && it.zone == Zone.LIBRARY
                }.shuffled()
                val otherCards = newState.cardInstances.filter {
                    !(it.ownerId == action.playerId && it.zone == Zone.LIBRARY)
                }
                newState = newState.copy(cardInstances = otherCards + libraryCards)

                // Draw 7 cards
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
                val tokenInstances = List(action.quantity) {
                    CardInstance(
                        card = tokenCard,
                        ownerId = action.playerId,
                        zone = Zone.BATTLEFIELD
                    )
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
                val clones = List(action.quantity) {
                    originalCard.createClone(action.newOwnerId, action.targetZone)
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
                    numberOfDice = action.numberOfDice
                )
                state.addEvent(event)
            }

            is NetworkAction.UpdateCardGridPosition -> {
                state.updateCardInstance(action.cardId) { card ->
                    card.setGridPosition(action.gridX, action.gridY)
                }
            }
        }
    }

    /**
     * Broadcast state update to all clients
     */
    private suspend fun broadcastStateUpdate(actionId: Long? = null) {
        val state = _gameState.value ?: return
        broadcastToAll(GameMessage.StateUpdate(state, actionId))
    }

    /**
     * Update lobby state from players map
     */
    private fun updateLobbyState() {
        _lobbyState.update {
            it.copy(players = players.values.toList())
        }
    }

    /**
     * Broadcast a message to all connected clients
     */
    private suspend fun broadcastToAll(message: GameMessage) {
        val text = json.encodeToString(message)
        clients.values.forEach { session ->
            try {
                session.send(Frame.Text(text))
            } catch (_: Exception) {
                // Client disconnected, will be handled by incoming loop
            }
        }
    }

    /**
     * Send a message to a specific session
     */
    private suspend fun sendTo(session: WebSocketSession, message: GameMessage) {
        try {
            session.send(Frame.Text(json.encodeToString(message)))
        } catch (_: Exception) {}
    }

    /**
     * Resume the game after a pause
     */
    suspend fun resumeGame() {
        _isPaused.value = false
        _pauseReason.value = null
        broadcastToAll(GameMessage.Resume())
    }

    /**
     * Get the host's player ID
     */
    fun getHostId(): String = hostId

    /**
     * Get the server port
     */
    fun getPort(): Int = port
}

/**
 * Validation result for action validation
 */
private data class ValidationResult(
    val isValid: Boolean,
    val reason: String = ""
)
