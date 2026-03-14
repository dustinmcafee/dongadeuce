package com.dustinmcafee.dongadeuce.server

import java.util.concurrent.ConcurrentHashMap

/**
 * Manages game rooms on the dedicated server.
 * Creates, tracks, and cleans up game rooms by their unique codes.
 */
class LobbyManager(private val config: ServerConfig) {
    private val rooms = ConcurrentHashMap<String, GameRoom>()

    /**
     * Create a new game room. Returns the room with its generated code.
     */
    fun createGame(maxPlayers: Int = config.maxPlayersPerGame): GameRoom? {
        if (rooms.size >= config.maxGames) return null

        val code = generateGameCode()
        val room = GameRoom(code, maxPlayers)
        rooms[code] = room
        return room
    }

    /**
     * Get a game room by code.
     */
    fun getRoom(code: String): GameRoom? = rooms[code.uppercase()]

    /**
     * List all open games (not yet started, with available slots).
     */
    fun listOpenGames(): List<GameInfo> {
        return rooms.values
            .filter { !it.isGameStarted() }
            .map { room ->
                GameInfo(
                    code = room.code,
                    playerCount = room.getPlayerCount(),
                    createdAt = room.createdAt
                )
            }
    }

    /**
     * Remove a game room.
     */
    suspend fun removeGame(code: String) {
        rooms.remove(code.uppercase())?.close()
    }

    /**
     * Clean up idle/empty rooms.
     */
    suspend fun cleanupIdleRooms() {
        val now = System.currentTimeMillis()
        val idleThreshold = config.idleTimeoutMinutes * 60 * 1000

        val toRemove = rooms.entries.filter { (_, room) ->
            (now - room.lastActivityAt) > idleThreshold
        }

        toRemove.forEach { (code, room) ->
            room.close()
            rooms.remove(code)
        }
    }

    /**
     * Get total number of active rooms.
     */
    fun getRoomCount(): Int = rooms.size

    /**
     * Generate a unique 6-character game code.
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

/**
 * Info about an open game for the lobby browser.
 */
data class GameInfo(
    val code: String,
    val playerCount: Int,
    val createdAt: Long
)
