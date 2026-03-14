package com.dustinmcafee.dongadeuce.server

import com.dustinmcafee.dongadeuce.models.Deck
import com.dustinmcafee.dongadeuce.network.*
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A single game room on the dedicated server.
 * Wraps a GameEngine and manages WebSocket sessions for that room.
 */
class GameRoom(
    val code: String,
    private val maxPlayers: Int = 6
) {
    // Game logic engine — same class used by P2P mode
    private val engine = GameEngine(maxPlayers)

    // Connected clients: playerId -> session
    private val clients = mutableMapOf<String, WebSocketSession>()
    private val mutex = Mutex()

    // Track creation time for idle cleanup
    val createdAt: Long = System.currentTimeMillis()
    var lastActivityAt: Long = System.currentTimeMillis()
        private set

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Handle a new player connecting to this room.
     */
    suspend fun handleConnection(session: WebSocketSession) {
        var playerId: String? = null
        lastActivityAt = System.currentTimeMillis()

        try {
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        lastActivityAt = System.currentTimeMillis()
                        val text = frame.readText()
                        val message = try {
                            json.decodeFromString<GameMessage>(text)
                        } catch (e: Exception) {
                            sendTo(session, GameMessage.Error(ErrorCode.UNKNOWN, "Invalid message format"))
                            continue
                        }

                        when (message) {
                            is GameMessage.PlayerJoin -> {
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

                                val isFirstPlayer = engine.getPlayerCount() == 0
                                val newPlayerId = engine.addPlayer(message.playerName, message.deck, isAdmin = isFirstPlayer)
                                playerId = newPlayerId

                                mutex.withLock {
                                    clients[newPlayerId] = session
                                }

                                val playerInfo = engine.getPlayer(newPlayerId)
                                val assignedName = playerInfo?.name ?: message.playerName

                                sendTo(session, GameMessage.PlayerJoined(newPlayerId, assignedName))
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
                                broadcastToAll(message)
                                if (engine.isGameStarted()) {
                                    val chatAction = NetworkAction.SendChatMessage(message.playerId, message.message)
                                    engine.executeAction(chatAction, message.playerId)
                                    broadcastStateUpdate()
                                }
                            }

                            is GameMessage.Ping -> {
                                sendTo(session, GameMessage.Pong(message.timestamp))
                            }

                            else -> {}
                        }
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            // Connection closed or error
        } finally {
            if (playerId != null) {
                handleDisconnect(playerId)
            }
        }
    }

    /**
     * Start the game (admin only, called via REST or first-player message).
     */
    fun startGame(): Boolean {
        val result = engine.startGame()
        if (result) {
            val initialState = engine.getCurrentState() ?: return false
            kotlinx.coroutines.runBlocking {
                broadcastToAll(GameMessage.GameStarting(initialState))
            }
        }
        return result
    }

    fun isGameStarted(): Boolean = engine.isGameStarted()
    fun getPlayerCount(): Int = engine.getPlayerCount()
    fun getAdminId(): String = engine.getAdminId()

    private suspend fun handleGameAction(action: NetworkAction, playerId: String, actionId: Long) {
        val result = engine.executeAction(action, playerId)
        if (!result.success) {
            val session = mutex.withLock { clients[playerId] }
            session?.let {
                sendTo(it, GameMessage.ActionRejected(actionId, result.reason))
            }
            return
        }
        broadcastStateUpdate(actionId)
    }

    private suspend fun handleDisconnect(playerId: String) {
        val player = engine.getPlayer(playerId) ?: return

        mutex.withLock {
            clients.remove(playerId)
        }

        if (engine.isGameStarted()) {
            engine.pauseGame("${player.name} disconnected")
            broadcastToAll(GameMessage.Pause(
                reason = "${player.name} disconnected",
                disconnectedPlayerId = playerId
            ))
        } else {
            engine.removePlayer(playerId)
            broadcastToAll(engine.lobbyState.value)
            broadcastToAll(GameMessage.PlayerLeft(
                playerId = playerId,
                playerName = player.name,
                reason = "Disconnected"
            ))
        }
    }

    private suspend fun broadcastStateUpdate(actionId: Long? = null) {
        val state = engine.getCurrentState() ?: return
        broadcastToAll(GameMessage.StateUpdate(state, actionId))
    }

    private suspend fun broadcastToAll(message: GameMessage) {
        val text = json.encodeToString(message)
        val clientsCopy = mutex.withLock { clients.values.toList() }
        clientsCopy.forEach { session ->
            try {
                session.send(Frame.Text(text))
            } catch (_: Exception) {}
        }
    }

    private suspend fun sendTo(session: WebSocketSession, message: GameMessage) {
        try {
            session.send(Frame.Text(json.encodeToString(message)))
        } catch (_: Exception) {}
    }

    /**
     * Check if the room is empty (no connected clients).
     */
    suspend fun isEmpty(): Boolean = mutex.withLock { clients.isEmpty() }

    /**
     * Close all connections and clean up.
     */
    suspend fun close() {
        mutex.withLock {
            clients.values.forEach { session ->
                try {
                    session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Room closed"))
                } catch (_: Exception) {}
            }
            clients.clear()
        }
    }
}
