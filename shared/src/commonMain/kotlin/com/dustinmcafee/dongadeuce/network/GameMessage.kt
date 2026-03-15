package com.dustinmcafee.dongadeuce.network

import com.dustinmcafee.dongadeuce.models.Deck
import com.dustinmcafee.dongadeuce.models.GameState
import kotlinx.serialization.Serializable

/**
 * Network protocol messages for multiplayer communication.
 * All messages are serializable for WebSocket transmission.
 */
@Serializable
sealed class GameMessage {
    // ==================== Connection Messages ====================

    /**
     * Client -> Host: Request to join the game lobby
     */
    @Serializable
    data class PlayerJoin(
        val playerName: String,
        val deck: Deck
    ) : GameMessage()

    /**
     * Host -> Client: Confirmation of successful join with assigned player ID
     */
    @Serializable
    data class PlayerJoined(
        val playerId: String,
        val playerName: String
    ) : GameMessage()

    /**
     * Host -> All: A player has left the game
     */
    @Serializable
    data class PlayerLeft(
        val playerId: String,
        val playerName: String,
        val reason: String
    ) : GameMessage()

    /**
     * Client -> Host: Player is ready/not ready to start
     */
    @Serializable
    data class PlayerReady(
        val playerId: String,
        val isReady: Boolean
    ) : GameMessage()

    // ==================== Lobby Messages ====================

    /**
     * Client -> Server: Create a new game room (dedicated server mode)
     */
    @Serializable
    data class CreateGame(
        val playerName: String,
        val deck: Deck,
        val maxPlayers: Int = 4
    ) : GameMessage()

    /**
     * Server -> Client: Game room created with code
     */
    @Serializable
    data class GameCreated(
        val gameCode: String,
        val playerId: String
    ) : GameMessage()

    /**
     * Client -> Server: Join an existing game room by code (dedicated server mode)
     */
    @Serializable
    data class JoinGame(
        val gameCode: String,
        val playerName: String,
        val deck: Deck
    ) : GameMessage()

    /**
     * Host/Server -> All: Current state of the lobby
     */
    @Serializable
    data class LobbyState(
        val players: List<LobbyPlayer>,
        val hostId: String,
        val adminId: String = "",
        val gameCode: String = "",
        val maxPlayers: Int = 4
    ) : GameMessage()

    /**
     * Client -> Server: Admin requests game to start (dedicated server mode)
     */
    @Serializable
    data class StartGame(
        val playerId: String
    ) : GameMessage()

    /**
     * Host -> All: Game is starting with initial state
     */
    @Serializable
    data class GameStarting(
        val gameState: GameState
    ) : GameMessage()

    /**
     * Host -> Target: You have been kicked from the game
     * Also Host -> All: Player was kicked (using PlayerLeft)
     */
    @Serializable
    data class KickPlayer(
        val playerId: String,
        val reason: String = "Kicked by host"
    ) : GameMessage()

    // ==================== Game Action Messages ====================

    /**
     * Client -> Host: Request to perform a game action
     */
    @Serializable
    data class GameAction(
        val action: NetworkAction,
        val playerId: String,
        val actionId: Long = System.currentTimeMillis() // For tracking/deduplication
    ) : GameMessage()

    /**
     * Host -> All: Updated game state after action(s)
     */
    @Serializable
    data class StateUpdate(
        val gameState: GameState,
        val lastActionId: Long? = null // ID of action that triggered this update
    ) : GameMessage()

    /**
     * Host -> Client: Action was rejected
     */
    @Serializable
    data class ActionRejected(
        val actionId: Long,
        val reason: String
    ) : GameMessage()

    // ==================== Connection Management ====================

    /**
     * Bidirectional: Keep-alive ping
     */
    @Serializable
    data class Ping(
        val timestamp: Long = System.currentTimeMillis()
    ) : GameMessage()

    /**
     * Bidirectional: Response to ping
     */
    @Serializable
    data class Pong(
        val timestamp: Long,
        val receivedAt: Long = System.currentTimeMillis()
    ) : GameMessage()

    /**
     * Host -> All: Game is paused
     */
    @Serializable
    data class Pause(
        val reason: String,
        val disconnectedPlayerId: String? = null
    ) : GameMessage()

    /**
     * Host -> All: Game is resuming
     */
    @Serializable
    data class Resume(
        val reconnectedPlayerId: String? = null
    ) : GameMessage()

    // ==================== Chat Messages ====================

    /**
     * Client -> Host -> All: Chat message
     */
    @Serializable
    data class Chat(
        val playerId: String,
        val playerName: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : GameMessage()

    // ==================== Error Messages ====================

    /**
     * Host -> Client: Connection error or protocol error
     */
    @Serializable
    data class Error(
        val code: ErrorCode,
        val message: String
    ) : GameMessage()
}

/**
 * Player information in the lobby
 */
@Serializable
data class LobbyPlayer(
    val id: String,
    val name: String,
    val hasDeck: Boolean,
    val isReady: Boolean,
    val isHost: Boolean = false,
    val isAdmin: Boolean = false
)

/**
 * Error codes for network errors
 */
@Serializable
enum class ErrorCode {
    LOBBY_FULL,
    GAME_ALREADY_STARTED,
    INVALID_DECK,
    INVALID_ACTION,
    NOT_YOUR_TURN,
    UNAUTHORIZED,
    DISCONNECTED,
    UNKNOWN
}
