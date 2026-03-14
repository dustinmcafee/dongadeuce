package com.dustinmcafee.dongadeuce.network

import com.dustinmcafee.dongadeuce.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.test.*

/**
 * Integration tests that spin up a real GameServer (Netty WebSocket)
 * and connect real GameClients over localhost.
 *
 * Validates the full P2P flow: host starts server → connects as client →
 * second player joins → lobby sync → ready → game start → actions → state sync.
 */
class P2PIntegrationTest {

    private fun createTestDeck(): Deck {
        return Deck(
            name = "Test Deck",
            commander = Card(name = "Test Commander", type = "Legendary Creature", power = "4", toughness = "4"),
            cards = (1..99).map { i ->
                if (i <= 35) Card(name = "Land $i", type = "Basic Land")
                else Card(name = "Creature $i", type = "Creature", power = "2", toughness = "2")
            }
        )
    }

    private fun findFreePort(): Int {
        val socket = java.net.ServerSocket(0)
        val port = socket.localPort
        socket.close()
        return port
    }

    @Test
    fun `P2P host starts server and connects as client`() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()

        // Give server time to bind
        delay(500)

        val client = GameClient()
        val deck = createTestDeck()

        // Connect as host (first player)
        val connectJob = launch {
            client.connect("localhost", port, "Alice", deck)
        }

        // Wait for connection
        withTimeout(5000) {
            client.connectionState.first { it is ConnectionState.Connected }
        }

        val playerId = client.playerId.value
        assertNotNull(playerId, "Should receive player ID")

        // Verify lobby state
        withTimeout(3000) {
            client.lobbyState.first { it != null && it.players.isNotEmpty() }
        }

        val lobby = client.lobbyState.value
        assertNotNull(lobby)
        assertEquals(1, lobby.players.size)
        assertEquals("Alice", lobby.players[0].name)
        assertTrue(lobby.players[0].isAdmin, "First player should be admin")

        // Cleanup
        client.disconnect()
        connectJob.cancel()
        server.stop()
    }

    @Test
    fun `P2P two players connect and see each other in lobby`() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(500)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        // Host connects
        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(5000) {
            client1.connectionState.first { it is ConnectionState.Connected }
        }

        // Second player connects
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(5000) {
            client2.connectionState.first { it is ConnectionState.Connected }
        }

        // Both should see 2 players in lobby
        withTimeout(3000) {
            client1.lobbyState.first { it != null && it.players.size == 2 }
        }
        withTimeout(3000) {
            client2.lobbyState.first { it != null && it.players.size == 2 }
        }

        val lobby1 = client1.lobbyState.value!!
        val lobby2 = client2.lobbyState.value!!

        assertEquals(2, lobby1.players.size)
        assertEquals(2, lobby2.players.size)

        // Both lobbies should have same players
        val names1 = lobby1.players.map { it.name }.toSet()
        val names2 = lobby2.players.map { it.name }.toSet()
        assertEquals(setOf("Alice", "Bob"), names1)
        assertEquals(setOf("Alice", "Bob"), names2)

        // Cleanup
        client1.disconnect()
        client2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop()
    }

    @Test
    fun `P2P ready up and start game`() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(500)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        // Both connect
        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(5000) { client1.connectionState.first { it is ConnectionState.Connected } }

        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(5000) { client2.connectionState.first { it is ConnectionState.Connected } }

        // Wait for both to see each other
        withTimeout(3000) { client1.lobbyState.first { it?.players?.size == 2 } }
        withTimeout(3000) { client2.lobbyState.first { it?.players?.size == 2 } }

        // Client 2 readies up
        client2.setReady(true)

        // Wait for lobby update with ready status
        withTimeout(3000) {
            client1.lobbyState.first { lobby ->
                lobby != null && lobby.players.any { !it.isAdmin && it.isReady }
            }
        }

        // Host starts game
        val started = server.startGame()
        assertTrue(started, "Game should start when all non-admin players are ready")

        // Both clients should receive game state
        withTimeout(5000) {
            client1.gameStarted.first { it }
        }
        withTimeout(5000) {
            client2.gameStarted.first { it }
        }

        assertTrue(client1.gameStarted.value)
        assertTrue(client2.gameStarted.value)

        // Both should have a game state with 2 players at 40 life
        val state1 = client1.gameState.value
        val state2 = client2.gameState.value
        assertNotNull(state1)
        assertNotNull(state2)

        assertEquals(2, state1.players.size)
        assertEquals(2, state2.players.size)
        state1.players.forEach { assertEquals(GameConstants.STARTING_LIFE, it.life) }

        // Each player should have 100 cards total (commander + 99)
        val player1Id = client1.playerId.value!!
        val p1Cards = state1.cardInstances.count { it.ownerId == player1Id }
        assertEquals(100, p1Cards)

        // Each should have 7 in hand, 92 in library, 1 in command zone
        val p1Hand = state1.cardInstances.count { it.ownerId == player1Id && it.zone == Zone.HAND }
        val p1Lib = state1.cardInstances.count { it.ownerId == player1Id && it.zone == Zone.LIBRARY }
        val p1Cmd = state1.cardInstances.count { it.ownerId == player1Id && it.zone == Zone.COMMAND_ZONE }
        assertEquals(7, p1Hand)
        assertEquals(92, p1Lib)
        assertEquals(1, p1Cmd)

        // Cleanup
        client1.disconnect()
        client2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop()
    }

    @Test
    fun `P2P host sends action and both clients receive state update`() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(500)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        // Setup: connect, ready, start
        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(5000) { client1.connectionState.first { it is ConnectionState.Connected } }
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(5000) { client2.connectionState.first { it is ConnectionState.Connected } }
        withTimeout(3000) { client2.lobbyState.first { it?.players?.size == 2 } }

        client2.setReady(true)
        withTimeout(3000) {
            client1.lobbyState.first { it?.players?.any { p -> !p.isAdmin && p.isReady } == true }
        }
        server.startGame()
        withTimeout(5000) { client1.gameStarted.first { it } }
        withTimeout(5000) { client2.gameStarted.first { it } }

        val initialState = client1.gameState.value!!
        val initialTurn = initialState.turnNumber

        // Host (client1) sends a draw action
        val hostId = client1.playerId.value!!
        client1.sendAction(NetworkAction.DrawCard(hostId))

        // Wait for state update (turn hasn't changed but card counts should)
        withTimeout(3000) {
            client1.gameState.first { state ->
                state != null && state.cardInstances.count {
                    it.ownerId == hostId && it.zone == Zone.HAND
                } == 8 // 7 starting + 1 drawn
            }
        }

        // Client2 should also see the updated state
        withTimeout(3000) {
            client2.gameState.first { state ->
                state != null && state.cardInstances.count {
                    it.ownerId == hostId && it.zone == Zone.HAND
                } == 8
            }
        }

        val state1 = client1.gameState.value!!
        val state2 = client2.gameState.value!!

        // Both should see 8 cards in host's hand
        val hostHand1 = state1.cardInstances.count { it.ownerId == hostId && it.zone == Zone.HAND }
        val hostHand2 = state2.cardInstances.count { it.ownerId == hostId && it.zone == Zone.HAND }
        assertEquals(8, hostHand1)
        assertEquals(8, hostHand2)

        // Cleanup
        client1.disconnect()
        client2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop()
    }

    @Test
    fun `P2P joiner sends action through WebSocket`() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(500)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        // Setup game
        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(5000) { client1.connectionState.first { it is ConnectionState.Connected } }
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(5000) { client2.connectionState.first { it is ConnectionState.Connected } }
        withTimeout(3000) { client2.lobbyState.first { it?.players?.size == 2 } }
        client2.setReady(true)
        withTimeout(3000) {
            client1.lobbyState.first { it?.players?.any { p -> !p.isAdmin && p.isReady } == true }
        }
        server.startGame()
        withTimeout(5000) { client1.gameStarted.first { it } }
        withTimeout(5000) { client2.gameStarted.first { it } }

        // Pass turn to Bob so Bob can take turn-based actions
        val aliceId = client1.playerId.value!!
        client1.sendAction(NetworkAction.PassTurn)

        // Wait for turn to pass
        withTimeout(3000) {
            client2.gameState.first { it != null && it.turnNumber == 2 }
        }

        // Bob (client2) draws a card
        val bobId = client2.playerId.value!!
        client2.sendAction(NetworkAction.DrawCard(bobId))

        // Both clients should see Bob's hand grow to 8
        withTimeout(3000) {
            client1.gameState.first { state ->
                state != null && state.cardInstances.count {
                    it.ownerId == bobId && it.zone == Zone.HAND
                } == 8
            }
        }

        // Cleanup
        client1.disconnect()
        client2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop()
    }

    @Test
    fun `P2P duplicate name gets unique suffix`() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(500)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        val job1 = launch { client1.connect("localhost", port, "Player", deck) }
        withTimeout(5000) { client1.connectionState.first { it is ConnectionState.Connected } }

        val job2 = launch { client2.connect("localhost", port, "Player", deck) }
        withTimeout(5000) { client2.connectionState.first { it is ConnectionState.Connected } }

        withTimeout(3000) { client1.lobbyState.first { it?.players?.size == 2 } }

        val lobby = client1.lobbyState.value!!
        val names = lobby.players.map { it.name }.toSet()
        assertEquals(2, names.size, "Names should be unique: $names")
        assertTrue("Player" in names)
        assertTrue("Player (1)" in names)

        // Cleanup
        client1.disconnect()
        client2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop()
    }

    @Test
    fun `P2P lobby full rejects additional players`() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 2) // Only 2 slots
        server.start()
        delay(500)

        val client1 = GameClient()
        val client2 = GameClient()
        val client3 = GameClient()
        val deck = createTestDeck()

        // Fill both slots
        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(5000) { client1.connectionState.first { it is ConnectionState.Connected } }
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(5000) { client2.connectionState.first { it is ConnectionState.Connected } }

        // Third player should be rejected
        val job3 = launch { client3.connect("localhost", port, "Charlie", deck) }

        withTimeout(5000) {
            client3.connectionState.first { it is ConnectionState.Error }
        }

        val errorState = client3.connectionState.value
        assertTrue(errorState is ConnectionState.Error)

        // Cleanup
        client1.disconnect()
        client2.disconnect()
        client3.disconnect()
        job1.cancel()
        job2.cancel()
        job3.cancel()
        server.stop()
    }

    @Test
    fun `P2P life change syncs across clients`() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(500)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        // Setup game
        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(5000) { client1.connectionState.first { it is ConnectionState.Connected } }
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(5000) { client2.connectionState.first { it is ConnectionState.Connected } }
        withTimeout(3000) { client2.lobbyState.first { it?.players?.size == 2 } }
        client2.setReady(true)
        withTimeout(3000) {
            client1.lobbyState.first { it?.players?.any { p -> !p.isAdmin && p.isReady } == true }
        }
        server.startGame()
        withTimeout(5000) { client1.gameStarted.first { it } }
        withTimeout(5000) { client2.gameStarted.first { it } }

        // Alice changes her life
        val aliceId = client1.playerId.value!!
        client1.sendAction(NetworkAction.UpdateLife(aliceId, 33))

        // Both see the change
        withTimeout(3000) {
            client1.gameState.first { state ->
                state?.players?.find { it.id == aliceId }?.life == 33
            }
        }
        withTimeout(3000) {
            client2.gameState.first { state ->
                state?.players?.find { it.id == aliceId }?.life == 33
            }
        }

        assertEquals(33, client1.gameState.value!!.players.find { it.id == aliceId }!!.life)
        assertEquals(33, client2.gameState.value!!.players.find { it.id == aliceId }!!.life)

        // Cleanup
        client1.disconnect()
        client2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop()
    }

    // Helper to set up a started 2-player game, returns (server, client1, client2, job1, job2)
    private suspend fun CoroutineScope.setupStartedGame(port: Int): StartedGame {
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(500)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(5000) { client1.connectionState.first { it is ConnectionState.Connected } }
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(5000) { client2.connectionState.first { it is ConnectionState.Connected } }
        withTimeout(3000) { client2.lobbyState.first { it?.players?.size == 2 } }

        client2.setReady(true)
        withTimeout(3000) {
            client1.lobbyState.first { it?.players?.any { p -> !p.isAdmin && p.isReady } == true }
        }
        server.startGame()
        withTimeout(5000) { client1.gameStarted.first { it } }
        withTimeout(5000) { client2.gameStarted.first { it } }

        return StartedGame(server, client1, client2, job1, job2)
    }

    private data class StartedGame(
        val server: GameServer,
        val client1: GameClient,
        val client2: GameClient,
        val job1: Job,
        val job2: Job
    ) {
        fun cleanup() {
            client1.disconnect()
            client2.disconnect()
            job1.cancel()
            job2.cancel()
            server.stop()
        }
    }

    @Test
    fun `P2P player disconnect during game pauses for all`() = runBlocking {
        val port = findFreePort()
        val game = setupStartedGame(port)

        try {
            // Bob disconnects
            game.client2.disconnect()

            // Alice should see game pause
            withTimeout(5000) {
                game.client1.isPaused.first { it }
            }

            assertTrue(game.client1.isPaused.value)
        } finally {
            game.cleanup()
        }
    }

    @Test
    fun `P2P kick player removes from lobby`() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(500)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(5000) { client1.connectionState.first { it is ConnectionState.Connected } }
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(5000) { client2.connectionState.first { it is ConnectionState.Connected } }
        withTimeout(3000) { client1.lobbyState.first { it?.players?.size == 2 } }

        // Get Bob's ID
        val bobId = client2.playerId.value!!

        // Host kicks Bob
        server.kickPlayer(bobId)

        // Alice should see lobby go back to 1 player
        withTimeout(3000) {
            client1.lobbyState.first { it?.players?.size == 1 }
        }

        assertEquals(1, client1.lobbyState.value!!.players.size)

        // Bob should get disconnected or error state
        withTimeout(5000) {
            client2.connectionState.first {
                it is ConnectionState.Error || it is ConnectionState.Disconnected
            }
        }

        // Cleanup
        client1.disconnect()
        client2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop()
    }

    @Test
    fun `P2P chat message received by other client`() = runBlocking {
        val port = findFreePort()
        val game = setupStartedGame(port)

        try {
            val aliceId = game.client1.playerId.value!!

            // Alice sends chat
            game.client1.sendChat("Hello Bob!")

            // Bob should see state update with chat event in game log
            withTimeout(3000) {
                game.client2.gameState.first { state ->
                    state != null && state.gameLog.any {
                        it is GameEvent.ChatMessage && it.message == "Hello Bob!"
                    }
                }
            }
        } finally {
            game.cleanup()
        }
    }

    @Test
    fun `P2P three player game works`() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(500)

        val client1 = GameClient()
        val client2 = GameClient()
        val client3 = GameClient()
        val deck = createTestDeck()

        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(5000) { client1.connectionState.first { it is ConnectionState.Connected } }
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(5000) { client2.connectionState.first { it is ConnectionState.Connected } }
        val job3 = launch { client3.connect("localhost", port, "Charlie", deck) }
        withTimeout(5000) { client3.connectionState.first { it is ConnectionState.Connected } }

        // All 3 should see each other
        withTimeout(3000) { client1.lobbyState.first { it?.players?.size == 3 } }
        withTimeout(3000) { client2.lobbyState.first { it?.players?.size == 3 } }
        withTimeout(3000) { client3.lobbyState.first { it?.players?.size == 3 } }

        val names = client1.lobbyState.value!!.players.map { it.name }.toSet()
        assertEquals(setOf("Alice", "Bob", "Charlie"), names)

        // Ready up non-admin players
        client2.setReady(true)
        client3.setReady(true)

        withTimeout(3000) {
            client1.lobbyState.first { lobby ->
                lobby != null && lobby.players.filter { !it.isAdmin }.all { it.isReady }
            }
        }

        // Start game
        assertTrue(server.startGame())
        withTimeout(5000) { client1.gameStarted.first { it } }
        withTimeout(5000) { client2.gameStarted.first { it } }
        withTimeout(5000) { client3.gameStarted.first { it } }

        // All 3 should have game state with 3 players
        assertEquals(3, client1.gameState.value!!.players.size)
        assertEquals(3, client2.gameState.value!!.players.size)
        assertEquals(3, client3.gameState.value!!.players.size)

        // Turn passes through all 3
        val aliceId = client1.playerId.value!!
        client1.sendAction(NetworkAction.PassTurn)
        withTimeout(3000) { client2.gameState.first { it?.turnNumber == 2 } }

        val bobId = client2.playerId.value!!
        client2.sendAction(NetworkAction.PassTurn)
        withTimeout(3000) { client3.gameState.first { it?.turnNumber == 3 } }

        val charlieId = client3.playerId.value!!
        client3.sendAction(NetworkAction.PassTurn)
        withTimeout(3000) { client1.gameState.first { it?.turnNumber == 4 } }

        // Back to Alice
        assertEquals(aliceId, client1.gameState.value!!.activePlayer.id)

        // Cleanup
        client1.disconnect()
        client2.disconnect()
        client3.disconnect()
        job1.cancel()
        job2.cancel()
        job3.cancel()
        server.stop()
    }

    @Test
    fun `P2P four player game initializes correctly`() = runBlocking {
        val port = findFreePort()
        // maxPlayers must be > 4 since the server itself doesn't consume a slot,
        // but each client connect becomes a player via the engine
        val server = GameServer(port = port, maxPlayers = 6)
        server.start()
        delay(500)

        val clients = (1..4).map { GameClient() }
        val deck = createTestDeck()
        val names = listOf("Alice", "Bob", "Charlie", "Diana")

        // Connect sequentially to avoid races
        val jobs = mutableListOf<Job>()
        for (i in 0 until 4) {
            val job = launch { clients[i].connect("localhost", port, names[i], deck) }
            jobs.add(job)
            withTimeout(5000) { clients[i].connectionState.first { it is ConnectionState.Connected } }
            // Wait for lobby to reflect this player
            withTimeout(3000) { clients[0].lobbyState.first { it?.players?.size == i + 1 } }
        }

        // All 4 see each other
        val lobbyNames = clients[0].lobbyState.value!!.players.map { it.name }.toSet()
        assertEquals(setOf("Alice", "Bob", "Charlie", "Diana"), lobbyNames)

        // Ready up non-admin
        for (c in clients.drop(1)) {
            c.setReady(true)
            delay(100) // Stagger to avoid message ordering issues
        }
        withTimeout(5000) {
            clients[0].lobbyState.first { lobby ->
                lobby != null && lobby.players.filter { !it.isAdmin }.all { it.isReady }
            }
        }

        server.startGame()
        clients.forEach { client ->
            withTimeout(5000) { client.gameStarted.first { it } }
        }

        // All 4 should have game state with 4 players at 40 life
        clients.forEach { client ->
            val state = client.gameState.value!!
            assertEquals(4, state.players.size)
            state.players.forEach { p -> assertEquals(GameConstants.STARTING_LIFE, p.life) }
        }

        // Cleanup
        clients.forEach { it.disconnect() }
        jobs.forEach { it.cancel() }
        server.stop()
    }

    @Test
    fun `P2P resume after pause`() = runBlocking {
        val port = findFreePort()
        val game = setupStartedGame(port)

        try {
            // Bob disconnects → game pauses
            game.client2.disconnect()
            game.job2.cancel()

            withTimeout(5000) {
                game.client1.isPaused.first { it }
            }
            assertTrue(game.client1.isPaused.value)

            // Host resumes
            game.server.resumeGame()

            withTimeout(3000) {
                game.client1.isPaused.first { !it }
            }
            assertFalse(game.client1.isPaused.value)

            // Alice can still take actions
            val aliceId = game.client1.playerId.value!!
            game.client1.sendAction(NetworkAction.DrawCard(aliceId))

            withTimeout(3000) {
                game.client1.gameState.first { state ->
                    state != null && state.cardInstances.count {
                        it.ownerId == aliceId && it.zone == Zone.HAND
                    } == 8
                }
            }
        } finally {
            game.client1.disconnect()
            game.job1.cancel()
            game.server.stop()
        }
    }

    @Test
    fun `P2P game cannot start before all players ready`() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(500)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(5000) { client1.connectionState.first { it is ConnectionState.Connected } }
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(5000) { client2.connectionState.first { it is ConnectionState.Connected } }
        withTimeout(3000) { client1.lobbyState.first { it?.players?.size == 2 } }

        // Try starting without ready — should fail
        assertFalse(server.startGame())
        assertFalse(client1.gameStarted.value)

        // Cleanup
        client1.disconnect()
        client2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop()
    }
}
