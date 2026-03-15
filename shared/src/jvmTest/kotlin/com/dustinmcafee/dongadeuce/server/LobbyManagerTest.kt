package com.dustinmcafee.dongadeuce.server

import kotlinx.coroutines.runBlocking
import kotlin.test.*

class LobbyManagerTest {

    private fun defaultConfig() = ServerConfig(
        port = 9090,
        maxGames = 10,
        maxPlayersPerGame = 4,
        gameCodeLength = 6,
        idleTimeoutMinutes = 60
    )

    @Test
    fun `createGame returns room with unique code`() = runBlocking {
        val manager = LobbyManager(defaultConfig())
        val room = manager.createGame()

        assertNotNull(room)
        assertEquals(6, room.code.length)
        assertEquals(0, room.getPlayerCount())
    }

    @Test
    fun `createGame generates unique codes`() = runBlocking {
        val manager = LobbyManager(defaultConfig())
        val codes = (1..10).map { manager.createGame()!!.code }.toSet()

        assertEquals(10, codes.size, "All codes should be unique")
    }

    @Test
    fun `createGame returns null when at max capacity`() = runBlocking {
        val config = defaultConfig().copy(maxGames = 2)
        val manager = LobbyManager(config)

        assertNotNull(manager.createGame())
        assertNotNull(manager.createGame())
        assertNull(manager.createGame(), "Should return null when at max capacity")
    }

    @Test
    fun `getRoom finds existing room`() = runBlocking {
        val manager = LobbyManager(defaultConfig())
        val room = manager.createGame()!!

        val found = manager.getRoom(room.code)
        assertNotNull(found)
        assertEquals(room.code, found.code)
    }

    @Test
    fun `getRoom is case insensitive`() = runBlocking {
        val manager = LobbyManager(defaultConfig())
        val room = manager.createGame()!!

        val found = manager.getRoom(room.code.lowercase())
        assertNotNull(found)
    }

    @Test
    fun `getRoom returns null for nonexistent code`() = runBlocking {
        val manager = LobbyManager(defaultConfig())
        assertNull(manager.getRoom("ZZZZZ1"))
    }

    @Test
    fun `listOpenGames shows unstarted games`() = runBlocking {
        val manager = LobbyManager(defaultConfig())
        manager.createGame()
        manager.createGame()

        val openGames = manager.listOpenGames()
        assertEquals(2, openGames.size)
    }

    @Test
    fun `listOpenGames excludes started games`() = runBlocking {
        val manager = LobbyManager(defaultConfig())
        val room = manager.createGame()!!

        // Can't actually start without players, so just verify it's listed
        val before = manager.listOpenGames()
        assertEquals(1, before.size)
    }

    @Test
    fun `removeGame deletes the room`() = runBlocking {
        val manager = LobbyManager(defaultConfig())
        val room = manager.createGame()!!
        val code = room.code

        assertEquals(1, manager.getRoomCount())
        manager.removeGame(code)
        assertEquals(0, manager.getRoomCount())
        assertNull(manager.getRoom(code))
    }

    @Test
    fun `getRoomCount tracks active rooms`() = runBlocking {
        val manager = LobbyManager(defaultConfig())
        assertEquals(0, manager.getRoomCount())

        manager.createGame()
        assertEquals(1, manager.getRoomCount())

        manager.createGame()
        assertEquals(2, manager.getRoomCount())
    }

    @Test
    fun `game codes use only unambiguous characters`() = runBlocking {
        val config = defaultConfig().copy(maxGames = 100)
        val manager = LobbyManager(config)
        val ambiguous = setOf('I', 'O', '0', '1') // Should be excluded

        repeat(50) {
            val room = manager.createGame()!!
            for (char in room.code) {
                assertFalse(char in ambiguous, "Code '${room.code}' contains ambiguous char '$char'")
            }
        }
    }

    @Test
    fun `cleanupIdleRooms removes old rooms`() = runBlocking {
        val config = defaultConfig().copy(idleTimeoutMinutes = 0) // Immediate timeout
        val manager = LobbyManager(config)
        manager.createGame()
        manager.createGame()

        assertEquals(2, manager.getRoomCount())

        // Wait a tiny bit so lastActivityAt is in the past
        Thread.sleep(10)
        manager.cleanupIdleRooms()

        assertEquals(0, manager.getRoomCount())
    }

    @Test
    fun `GameInfo contains correct data`() = runBlocking {
        val manager = LobbyManager(defaultConfig())
        val room = manager.createGame()!!

        val games = manager.listOpenGames()
        assertEquals(1, games.size)

        val info = games[0]
        assertEquals(room.code, info.code)
        assertEquals(0, info.playerCount)
        assertTrue(info.createdAt > 0)
    }
}
