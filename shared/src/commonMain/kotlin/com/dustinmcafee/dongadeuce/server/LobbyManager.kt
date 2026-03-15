package com.dustinmcafee.dongadeuce.server

import com.dustinmcafee.dongadeuce.platform.currentTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Configuration for the dedicated game server.
 */
data class ServerConfig(
    val port: Int = 9090,
    val maxGames: Int = 100,
    val maxPlayersPerGame: Int = 6,
    val gameCodeLength: Int = 6,
    val idleTimeoutMinutes: Long = 60,
    val tlsEnabled: Boolean = false
) {
    companion object
}

/**
 * Info about an open game for the lobby browser.
 */
data class GameInfo(
    val code: String,
    val playerCount: Int,
    val createdAt: Long
)

/**
 * Manages game rooms on the dedicated server.
 * Creates, tracks, and cleans up game rooms by their unique codes.
 */
class LobbyManager(private val config: ServerConfig) {
    private val rooms = mutableMapOf<String, GameRoom>()
    private val mutex = Mutex()

    /**
     * Create a new game room. Returns the room with its generated code.
     */
    suspend fun createGame(maxPlayers: Int = config.maxPlayersPerGame): GameRoom? {
        return mutex.withLock {
            if (rooms.size >= config.maxGames) return@withLock null

            val code = generateGameCode()
            val room = GameRoom(code, maxPlayers)
            rooms[code] = room
            room
        }
    }

    /**
     * Get a game room by code.
     */
    suspend fun getRoom(code: String): GameRoom? = mutex.withLock { rooms[code.uppercase()] }

    /**
     * List all open games (not yet started, with available slots).
     */
    suspend fun listOpenGames(): List<GameInfo> {
        return mutex.withLock {
            rooms.values
                .filter { !it.isGameStarted() }
                .map { room ->
                    GameInfo(
                        code = room.code,
                        playerCount = room.getPlayerCount(),
                        createdAt = room.createdAt
                    )
                }
        }
    }

    /**
     * Remove a game room.
     */
    suspend fun removeGame(code: String) {
        val room = mutex.withLock { rooms.remove(code.uppercase()) }
        room?.close()
    }

    /**
     * Clean up idle/empty rooms.
     */
    suspend fun cleanupIdleRooms() {
        val now = currentTimeMillis()
        val idleThreshold = config.idleTimeoutMinutes * 60 * 1000

        val toRemove = mutex.withLock {
            rooms.entries.filter { (_, room) ->
                (now - room.lastActivityAt) > idleThreshold
            }.map { it.key to it.value }
        }

        toRemove.forEach { (code, room) ->
            room.close()
            mutex.withLock { rooms.remove(code) }
        }
    }

    /**
     * Get total number of active rooms.
     */
    suspend fun getRoomCount(): Int = mutex.withLock { rooms.size }

    /**
     * Generate a unique 6-character game code.
     * Must be called while holding the mutex.
     */
    private fun generateGameCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // No I/O/0/1 to avoid ambiguity
        var code: String
        do {
            code = (1..config.gameCodeLength).map { chars.random() }.joinToString("")
        } while (rooms.containsKey(code))
        return code
    }
}
