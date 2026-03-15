package com.dustinmcafee.dongadeuce.network

import com.dustinmcafee.dongadeuce.models.Deck
import com.dustinmcafee.dongadeuce.models.GameState
import com.dustinmcafee.dongadeuce.platform.createHttpClientEngine
import com.dustinmcafee.dongadeuce.platform.ioDispatcher
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * WebSocket client for connecting to a game server.
 */
class GameClient {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private var client: HttpClient? = null
    private var session: WebSocketSession? = null

    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Assigned player ID (after joining)
    private val _playerId = MutableStateFlow<String?>(null)
    val playerId: StateFlow<String?> = _playerId.asStateFlow()

    // Lobby state
    private val _lobbyState = MutableStateFlow<GameMessage.LobbyState?>(null)
    val lobbyState: StateFlow<GameMessage.LobbyState?> = _lobbyState.asStateFlow()

    // Game state (received from host)
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    // Game started flag
    private val _gameStarted = MutableStateFlow(false)
    val gameStarted: StateFlow<Boolean> = _gameStarted.asStateFlow()

    // Pause state
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _pauseReason = MutableStateFlow<String?>(null)
    val pauseReason: StateFlow<String?> = _pauseReason.asStateFlow()

    // Error messages
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Connection info for reconnection
    private var lastHost: String? = null
    private var lastPort: Int? = null
    private var lastPlayerName: String? = null
    private var lastDeck: Deck? = null

    // JSON serializer
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Game code for dedicated server mode
    private var lastGameCode: String? = null

    /**
     * Connect to a game server.
     * @param gameCode If set, connects to /game/{code} (dedicated server mode). If null, connects to /game (P2P mode).
     */
    suspend fun connect(host: String, port: Int, playerName: String, deck: Deck, gameCode: String? = null): Boolean {
        if (_connectionState.value is ConnectionState.Connected ||
            _connectionState.value is ConnectionState.Connecting) {
            return false
        }

        _connectionState.value = ConnectionState.Connecting
        _error.value = null

        // Store for reconnection
        lastHost = host
        lastPort = port
        lastPlayerName = playerName
        lastDeck = deck
        lastGameCode = gameCode

        val path = if (gameCode != null) "/game/$gameCode" else "/game"

        try {
            client = HttpClient(createHttpClientEngine()) {
                install(WebSockets)
            }

            client?.webSocket(host = host, port = port, path = path) {
                session = this
                _connectionState.value = ConnectionState.Connecting

                // Send join request
                send(Frame.Text(json.encodeToString<GameMessage>(
                    GameMessage.PlayerJoin(playerName, deck)
                )))

                // Listen for messages directly in this coroutine
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            handleMessage(frame.readText())
                        }
                        is Frame.Close -> {
                            handleDisconnect("Connection closed by server")
                            break
                        }
                        else -> {}
                    }
                }
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Failed to connect: ${e.message}")
            _error.value = "Failed to connect: ${e.message}"
            return false
        }

        return _connectionState.value is ConnectionState.Connected
    }

    /**
     * Handle incoming message
     */
    private fun handleMessage(text: String) {
        val message = try {
            json.decodeFromString<GameMessage>(text)
        } catch (e: Exception) {
            _error.value = "Invalid message from server"
            return
        }

        when (message) {
            is GameMessage.PlayerJoined -> {
                _playerId.value = message.playerId
                _connectionState.value = ConnectionState.Connected(message.playerId)
            }

            is GameMessage.LobbyState -> {
                _lobbyState.value = message
            }

            is GameMessage.GameStarting -> {
                _gameState.value = message.gameState
                _gameStarted.value = true
            }

            is GameMessage.StateUpdate -> {
                _gameState.value = message.gameState
            }

            is GameMessage.ActionRejected -> {
                _error.value = "Action rejected: ${message.reason}"
            }

            is GameMessage.Pause -> {
                _isPaused.value = true
                _pauseReason.value = message.reason
                _connectionState.value = ConnectionState.Paused(message.reason)
            }

            is GameMessage.Resume -> {
                _isPaused.value = false
                _pauseReason.value = null
                _playerId.value?.let { id ->
                    _connectionState.value = ConnectionState.Connected(id)
                }
            }

            is GameMessage.KickPlayer -> {
                if (message.playerId == _playerId.value) {
                    _connectionState.value = ConnectionState.Error("You were kicked: ${message.reason}")
                    _error.value = "You were kicked: ${message.reason}"
                    disconnect()
                }
            }

            is GameMessage.PlayerLeft -> {
                // Player left notification - lobby state will be updated separately
            }

            is GameMessage.Chat -> {
                // Chat messages are included in game state updates
            }

            is GameMessage.GameCreated -> {
                // Dedicated server: game room created with code
                _playerId.value = message.playerId
                _connectionState.value = ConnectionState.Connected(message.playerId)
            }

            is GameMessage.Pong -> {
                // Heartbeat response
            }

            is GameMessage.Error -> {
                _error.value = "${message.code}: ${message.message}"
                if (message.code == ErrorCode.LOBBY_FULL ||
                    message.code == ErrorCode.GAME_ALREADY_STARTED) {
                    _connectionState.value = ConnectionState.Error(message.message)
                }
            }

            else -> {
                // Ignore other message types
            }
        }
    }

    /**
     * Handle disconnect
     */
    private fun handleDisconnect(reason: String) {
        _connectionState.value = ConnectionState.Error(reason)
        session = null
    }

    /**
     * Disconnect from the server
     */
    fun disconnect() {
        scope.launch {
            try {
                session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnecting"))
            } catch (_: Exception) {}

            session = null
            client?.close()
            client = null

            _connectionState.value = ConnectionState.Disconnected
            _playerId.value = null
            _lobbyState.value = null
            _gameState.value = null
            _gameStarted.value = false
            _isPaused.value = false
            _pauseReason.value = null
        }
    }

    /**
     * Request game start (admin only, dedicated server mode)
     */
    suspend fun requestStartGame() {
        val id = _playerId.value ?: return
        val currentSession = session ?: return

        try {
            val message = GameMessage.StartGame(id)
            currentSession.send(Frame.Text(json.encodeToString<GameMessage>(message)))
        } catch (e: Exception) {
            _error.value = "Failed to start game: ${e.message}"
        }
    }

    /**
     * Send a game action to the host
     */
    suspend fun sendAction(action: NetworkAction) {
        val id = _playerId.value ?: return
        val currentSession = session ?: return

        if (_isPaused.value) return

        try {
            val message = GameMessage.GameAction(action, id)
            currentSession.send(Frame.Text(json.encodeToString<GameMessage>(message)))
        } catch (e: Exception) {
            _error.value = "Failed to send action: ${e.message}"
        }
    }

    /**
     * Send a chat message
     */
    suspend fun sendChat(message: String) {
        val id = _playerId.value ?: return
        val currentSession = session ?: return
        val name = lastPlayerName ?: return

        try {
            val chatMessage = GameMessage.Chat(id, name, message)
            currentSession.send(Frame.Text(json.encodeToString<GameMessage>(chatMessage)))
        } catch (e: Exception) {
            _error.value = "Failed to send chat: ${e.message}"
        }
    }

    /**
     * Set ready status
     */
    suspend fun setReady(ready: Boolean) {
        val id = _playerId.value ?: return
        val currentSession = session ?: return

        try {
            val message = GameMessage.PlayerReady(id, ready)
            currentSession.send(Frame.Text(json.encodeToString<GameMessage>(message)))
        } catch (e: Exception) {
            _error.value = "Failed to set ready status: ${e.message}"
        }
    }

    /**
     * Attempt to reconnect with stored credentials
     */
    suspend fun reconnect(): Boolean {
        val host = lastHost ?: return false
        val port = lastPort ?: return false
        val name = lastPlayerName ?: return false
        val deck = lastDeck ?: return false

        return connect(host, port, name, deck, lastGameCode)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}

/**
 * Connection state sealed class
 */
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val playerId: String) : ConnectionState()
    data class Paused(val reason: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
