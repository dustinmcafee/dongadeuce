package com.dustinmcafee.dongadeuce.network

import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.platform.*
import com.dustinmcafee.dongadeuce.tls.ServerTlsConfig
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * WebSocket game server for hosting multiplayer games.
 * Delegates all game logic to GameEngine.
 *
 * All players (including the host in P2P mode) connect via WebSocket.
 * The first player to connect becomes the admin.
 */
class GameServer(
    private val port: Int = 8080,
    private val maxPlayers: Int = 6,
    private val tlsConfig: ServerTlsConfig? = null
) {
    private var server: ServerWrapper? = null
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    // Game logic engine
    private val engine = GameEngine(maxPlayers)

    // Mutex for thread-safe access to shared data
    private val mutex = Mutex()

    // Connected clients: playerId -> session
    private val clients = mutableMapOf<String, WebSocketSession>()

    // Expose engine state flows
    val lobbyState: StateFlow<GameMessage.LobbyState> = engine.lobbyState
    val gameState: StateFlow<GameState?> = engine.gameState
    val isPaused: StateFlow<Boolean> = engine.isPaused
    val pauseReason: StateFlow<String?> = engine.pauseReason
    val gameStarted: StateFlow<Boolean> = engine.gameStarted

    // JSON serializer
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Start the WebSocket server.
     * All players (including the P2P host) connect via WebSocket as clients.
     */
    fun start(): String {
        server = createServer(port, tlsConfig = tlsConfig, module = {
            install(WebSockets) {
                pingPeriodMillis = 15000
                timeoutMillis = 30000
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                webSocket("/game") {
                    handleConnection(this)
                }
            }
        })
        server?.start(wait = false)

        val scheme = if (tlsConfig != null) "wss" else "ws"
        return "$scheme://localhost:$port/game"
    }

    fun getFingerprint(): String? = server?.certificateFingerprint

    /**
     * Stop the server
     */
    fun stop() {
        scope.launch {
            // Close all connections
            mutex.withLock {
                clients.values.forEach { session ->
                    try {
                        session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server shutting down"))
                    } catch (_: Exception) {}
                }
                clients.clear()
            }
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
                                if (engine.isGameStarted()) {
                                    sendTo(session, GameMessage.Error(ErrorCode.GAME_ALREADY_STARTED, "Game has already started"))
                                    session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Game already started"))
                                    return
                                }

                                if (engine.isLobbyFull()) {
                                    sendTo(session, GameMessage.Error(ErrorCode.LOBBY_FULL, "Lobby is full"))
                                    session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Lobby full"))
                                    return
                                }

                                // First player to connect becomes admin
                                val isFirstPlayer = engine.getPlayerCount() == 0
                                val newPlayerId = engine.addPlayer(message.playerName, message.deck, isAdmin = isFirstPlayer)
                                playerId = newPlayerId

                                mutex.withLock {
                                    clients[newPlayerId] = session
                                }

                                // Get the unique name that was assigned
                                val playerInfo = engine.getPlayer(newPlayerId)
                                val assignedName = playerInfo?.name ?: message.playerName

                                // Confirm join
                                sendTo(session, GameMessage.PlayerJoined(newPlayerId, assignedName))

                                // Broadcast updated lobby state
                                broadcastToAll(engine.lobbyState.value)
                            }

                            is GameMessage.PlayerReady -> {
                                if (playerId != null) {
                                    engine.setPlayerReady(playerId, message.isReady)
                                    broadcastToAll(engine.lobbyState.value)
                                }
                            }

                            is GameMessage.GameAction -> {
                                if (engine.isGameStarted() && !engine.isPaused.value) {
                                    handleGameAction(message.action, message.playerId, message.actionId)
                                }
                            }

                            is GameMessage.Chat -> {
                                // Broadcast chat to all players
                                broadcastToAll(message)

                                // Also add to game log if game has started
                                if (engine.isGameStarted()) {
                                    val chatAction = NetworkAction.SendChatMessage(message.playerId, message.message)
                                    engine.executeAction(chatAction, message.playerId)
                                    broadcastStateUpdate()
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
        val player = engine.getPlayer(playerId) ?: return

        mutex.withLock {
            clients.remove(playerId)
        }

        if (engine.isGameStarted()) {
            // Game in progress - eliminate the disconnected player and continue
            engine.eliminatePlayer(playerId)
            broadcastToAll(GameMessage.PlayerLeft(
                playerId = playerId,
                playerName = player.name,
                reason = "Disconnected"
            ))
            broadcastStateUpdate()
        } else {
            // Still in lobby - just remove player
            engine.removePlayer(playerId)
            broadcastToAll(engine.lobbyState.value)
            broadcastToAll(GameMessage.PlayerLeft(
                playerId = playerId,
                playerName = player.name,
                reason = "Disconnected"
            ))
        }
    }

    /**
     * Kick a player (admin only)
     */
    suspend fun kickPlayer(playerId: String) {
        val player = engine.getPlayer(playerId) ?: return
        if (player.isAdmin) return // Can't kick admin

        val session = mutex.withLock { clients[playerId] }

        // Send kick message
        session?.let {
            sendTo(it, GameMessage.KickPlayer(playerId, "Kicked by host"))
            it.close(CloseReason(CloseReason.Codes.NORMAL, "Kicked by host"))
        }

        // Remove from tracking
        mutex.withLock {
            clients.remove(playerId)
        }
        engine.removePlayer(playerId)

        // Broadcast
        broadcastToAll(engine.lobbyState.value)
        broadcastToAll(GameMessage.PlayerLeft(
            playerId = playerId,
            playerName = player.name,
            reason = "Kicked by host"
        ))
    }

    /**
     * Start the game (admin only)
     */
    fun startGame(): Boolean {
        val result = engine.startGame()
        if (result) {
            val initialState = engine.getCurrentState() ?: return false
            // Broadcast game start
            scope.launch {
                broadcastToAll(GameMessage.GameStarting(initialState))
            }
        }
        return result
    }

    /**
     * Handle a game action from a client
     */
    private suspend fun handleGameAction(action: NetworkAction, playerId: String, actionId: Long) {
        val result = engine.executeAction(action, playerId)
        if (!result.success) {
            val session = mutex.withLock { clients[playerId] }
            session?.let {
                sendTo(it, GameMessage.ActionRejected(actionId, result.reason))
            }
            return
        }

        // Broadcast updated state
        broadcastStateUpdate(actionId)
    }

    /**
     * Broadcast state update to all clients
     */
    private suspend fun broadcastStateUpdate(actionId: Long? = null) {
        val state = engine.getCurrentState() ?: return
        broadcastToAll(GameMessage.StateUpdate(state, actionId))
    }

    /**
     * Broadcast a message to all connected clients
     */
    private suspend fun broadcastToAll(message: GameMessage) {
        val text = json.encodeToString(message)
        val clientsCopy = mutex.withLock { clients.values.toList() }
        clientsCopy.forEach { session ->
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
        engine.resumeGame()
        broadcastToAll(GameMessage.Resume())
    }

    /**
     * Get the admin's player ID (first player who connected)
     */
    fun getHostId(): String = engine.getAdminId()

    /**
     * Get the server port
     */
    fun getPort(): Int = port
}
