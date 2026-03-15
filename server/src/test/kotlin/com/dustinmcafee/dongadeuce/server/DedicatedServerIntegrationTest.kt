package com.dustinmcafee.dongadeuce.server

import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.network.*
import io.ktor.client.*
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.websocket.WebSockets as ClientWS
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json as installJson
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerCN
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets as ServerWS
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * End-to-end integration tests for the dedicated server.
 * Spins up a real Ktor Netty server with LobbyManager + GameRoom routing,
 * connects real WebSocket clients by game code, and validates full gameplay.
 */
class DedicatedServerIntegrationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

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

    /**
     * Start a dedicated server using the actual routing from Main.kt pattern.
     */
    private fun startDedicatedServer(
        port: Int,
        lobbyManager: LobbyManager
    ): io.ktor.server.engine.ApplicationEngine {
        return embeddedServer(Netty, port = port) {
            install(ServerWS) {
                pingPeriodMillis = 15000
                timeoutMillis = 30000
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            install(ServerCN) {
                installJson(json)
            }
            routing {
                webSocket("/game/{code}") {
                    val code = call.parameters["code"]
                    if (code == null) {
                        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing code"))
                        return@webSocket
                    }
                    val room = lobbyManager.getRoom(code)
                    if (room == null) {
                        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Game not found"))
                        return@webSocket
                    }
                    room.handleConnection(this)
                }
            }
        }.also { it.start(wait = false) }
    }

    private fun createWsClient(): HttpClient {
        return HttpClient(ClientCIO) {
            install(ClientWS)
        }
    }

    /**
     * Connect a WS client, send PlayerJoin, return session + playerId.
     */
    private suspend fun connectPlayer(
        client: HttpClient,
        port: Int,
        code: String,
        name: String,
        deck: Deck
    ): Pair<DefaultClientWebSocketSession, String> {
        val session = client.webSocketSession(
            method = HttpMethod.Get,
            host = "localhost",
            port = port,
            path = "/game/$code"
        )

        session.send(Frame.Text(json.encodeToString<GameMessage>(
            GameMessage.PlayerJoin(name, deck)
        )))

        val joinFrame = session.incoming.receive() as Frame.Text
        val joinMsg = json.decodeFromString<GameMessage>(joinFrame.readText())
        assertTrue(joinMsg is GameMessage.PlayerJoined, "Expected PlayerJoined, got $joinMsg")
        val playerId = (joinMsg as GameMessage.PlayerJoined).playerId

        return Pair(session, playerId)
    }

    private suspend fun waitForLobbySize(
        session: DefaultClientWebSocketSession,
        expectedSize: Int,
        timeout: Long = 3000
    ): GameMessage.LobbyState {
        return withTimeout(timeout) {
            while (true) {
                val frame = session.incoming.receive() as Frame.Text
                val msg = json.decodeFromString<GameMessage>(frame.readText())
                if (msg is GameMessage.LobbyState && msg.players.size == expectedSize) {
                    return@withTimeout msg
                }
            }
            @Suppress("UNREACHABLE_CODE")
            error("unreachable")
        }
    }

    private suspend fun waitForGameStart(
        session: DefaultClientWebSocketSession,
        timeout: Long = 5000
    ): GameMessage.GameStarting {
        return withTimeout(timeout) {
            while (true) {
                val frame = session.incoming.receive() as Frame.Text
                val msg = json.decodeFromString<GameMessage>(frame.readText())
                if (msg is GameMessage.GameStarting) {
                    return@withTimeout msg
                }
            }
            @Suppress("UNREACHABLE_CODE")
            error("unreachable")
        }
    }

    private suspend fun waitForStateUpdate(
        session: DefaultClientWebSocketSession,
        timeout: Long = 3000
    ): GameMessage.StateUpdate {
        return withTimeout(timeout) {
            while (true) {
                val frame = session.incoming.receive() as Frame.Text
                val msg = json.decodeFromString<GameMessage>(frame.readText())
                if (msg is GameMessage.StateUpdate) {
                    return@withTimeout msg
                }
            }
            @Suppress("UNREACHABLE_CODE")
            error("unreachable")
        }
    }

    // ==================== Tests ====================

    @Test
    fun `connect to game room by code`() = runBlocking {
        val port = findFreePort()
        val config = ServerConfig(port = port, maxGames = 10, maxPlayersPerGame = 4)
        val lobbyManager = LobbyManager(config)
        val room = lobbyManager.createGame()!!
        val server = startDedicatedServer(port, lobbyManager)
        delay(500)
        val client = createWsClient()

        try {
            val (session, playerId) = connectPlayer(client, port, room.code, "Alice", createTestDeck())
            assertNotNull(playerId)
            assertTrue(playerId.isNotEmpty())

            val lobby = waitForLobbySize(session, 1)
            assertEquals(1, lobby.players.size)
            assertEquals("Alice", lobby.players[0].name)
            assertTrue(lobby.players[0].isAdmin)

            session.close()
        } finally {
            client.close()
            server.stop(500, 1000)
        }
    }

    @Test
    fun `two players join same room by code`() = runBlocking {
        val port = findFreePort()
        val config = ServerConfig(port = port, maxGames = 10, maxPlayersPerGame = 4)
        val lobbyManager = LobbyManager(config)
        val room = lobbyManager.createGame()!!
        val server = startDedicatedServer(port, lobbyManager)
        delay(500)
        val client = createWsClient()

        try {
            val (session1, id1) = connectPlayer(client, port, room.code, "Alice", createTestDeck())
            waitForLobbySize(session1, 1)

            val (session2, id2) = connectPlayer(client, port, room.code, "Bob", createTestDeck())
            assertNotEquals(id1, id2)

            val lobby1 = waitForLobbySize(session1, 2)
            val lobby2 = waitForLobbySize(session2, 2)

            assertEquals(2, lobby1.players.size)
            assertEquals(2, lobby2.players.size)
            assertEquals(setOf("Alice", "Bob"), lobby1.players.map { it.name }.toSet())

            session1.close()
            session2.close()
        } finally {
            client.close()
            server.stop(500, 1000)
        }
    }

    @Test
    fun `players in different rooms are isolated`() = runBlocking {
        val port = findFreePort()
        val config = ServerConfig(port = port, maxGames = 10, maxPlayersPerGame = 4)
        val lobbyManager = LobbyManager(config)
        val room1 = lobbyManager.createGame()!!
        val room2 = lobbyManager.createGame()!!
        val server = startDedicatedServer(port, lobbyManager)
        delay(500)
        val client = createWsClient()

        try {
            val (session1, _) = connectPlayer(client, port, room1.code, "Alice", createTestDeck())
            val lobby1 = waitForLobbySize(session1, 1)
            assertEquals(1, lobby1.players.size)
            assertEquals("Alice", lobby1.players[0].name)

            val (session2, _) = connectPlayer(client, port, room2.code, "Bob", createTestDeck())
            val lobby2 = waitForLobbySize(session2, 1)
            assertEquals(1, lobby2.players.size)
            assertEquals("Bob", lobby2.players[0].name)

            // Room 1 should NOT see Bob
            assertEquals(1, room1.getPlayerCount())
            assertEquals(1, room2.getPlayerCount())

            session1.close()
            session2.close()
        } finally {
            client.close()
            server.stop(500, 1000)
        }
    }

    @Test
    fun `full game flow through dedicated server`() = runBlocking {
        val port = findFreePort()
        val config = ServerConfig(port = port, maxGames = 10, maxPlayersPerGame = 4)
        val lobbyManager = LobbyManager(config)
        val room = lobbyManager.createGame()!!
        val server = startDedicatedServer(port, lobbyManager)
        delay(500)
        val client = createWsClient()

        try {
            // Two players join
            val (session1, id1) = connectPlayer(client, port, room.code, "Alice", createTestDeck())
            waitForLobbySize(session1, 1)
            val (session2, id2) = connectPlayer(client, port, room.code, "Bob", createTestDeck())
            waitForLobbySize(session1, 2)
            waitForLobbySize(session2, 2)

            // Bob readies up
            session2.send(Frame.Text(json.encodeToString<GameMessage>(
                GameMessage.PlayerReady(id2, true)
            )))
            withTimeout(3000) {
                while (true) {
                    val frame = session1.incoming.receive() as Frame.Text
                    val msg = json.decodeFromString<GameMessage>(frame.readText())
                    if (msg is GameMessage.LobbyState && msg.players.any { it.id == id2 && it.isReady }) break
                }
            }

            // Start game
            assertTrue(room.startGame())

            val start1 = waitForGameStart(session1)
            val start2 = waitForGameStart(session2)

            assertEquals(2, start1.gameState.players.size)
            assertEquals(2, start2.gameState.players.size)

            // Verify starting hands
            val p1Hand = start1.gameState.cardInstances.count { it.ownerId == id1 && it.zone == Zone.HAND }
            val p2Hand = start1.gameState.cardInstances.count { it.ownerId == id2 && it.zone == Zone.HAND }
            assertEquals(7, p1Hand)
            assertEquals(7, p2Hand)

            // Alice draws
            session1.send(Frame.Text(json.encodeToString<GameMessage>(
                GameMessage.GameAction(NetworkAction.DrawCard(id1), id1)
            )))

            val update1 = waitForStateUpdate(session1)
            val update2 = waitForStateUpdate(session2)
            assertEquals(8, update1.gameState.cardInstances.count { it.ownerId == id1 && it.zone == Zone.HAND })
            assertEquals(8, update2.gameState.cardInstances.count { it.ownerId == id1 && it.zone == Zone.HAND })

            // Alice passes turn
            session1.send(Frame.Text(json.encodeToString<GameMessage>(
                GameMessage.GameAction(NetworkAction.PassTurn, id1)
            )))
            val turnUpdate = waitForStateUpdate(session1)
            assertEquals(2, turnUpdate.gameState.turnNumber)
            assertEquals(id2, turnUpdate.gameState.activePlayer.id)

            // Drain session2 to get past the turn-pass update
            withTimeout(3000) {
                while (true) {
                    val frame = session2.incoming.receive() as Frame.Text
                    val msg = json.decodeFromString<GameMessage>(frame.readText())
                    if (msg is GameMessage.StateUpdate && msg.gameState.turnNumber == 2) break
                }
            }

            // Bob draws on his turn
            session2.send(Frame.Text(json.encodeToString<GameMessage>(
                GameMessage.GameAction(NetworkAction.DrawCard(id2), id2)
            )))
            val bobUpdate = waitForStateUpdate(session2)
            assertEquals(8, bobUpdate.gameState.cardInstances.count { it.ownerId == id2 && it.zone == Zone.HAND })

            session1.close()
            session2.close()
        } finally {
            client.close()
            server.stop(500, 1000)
        }
    }

    @Test
    fun `action rejected for wrong player on dedicated server`() = runBlocking {
        val port = findFreePort()
        val config = ServerConfig(port = port, maxGames = 10, maxPlayersPerGame = 4)
        val lobbyManager = LobbyManager(config)
        val room = lobbyManager.createGame()!!
        val server = startDedicatedServer(port, lobbyManager)
        delay(500)
        val client = createWsClient()

        try {
            val (session1, id1) = connectPlayer(client, port, room.code, "Alice", createTestDeck())
            waitForLobbySize(session1, 1)
            val (session2, id2) = connectPlayer(client, port, room.code, "Bob", createTestDeck())
            waitForLobbySize(session1, 2)
            waitForLobbySize(session2, 2)

            session2.send(Frame.Text(json.encodeToString<GameMessage>(
                GameMessage.PlayerReady(id2, true)
            )))
            withTimeout(3000) {
                while (true) {
                    val frame = session1.incoming.receive() as Frame.Text
                    val msg = json.decodeFromString<GameMessage>(frame.readText())
                    if (msg is GameMessage.LobbyState && msg.players.any { it.id == id2 && it.isReady }) break
                }
            }

            room.startGame()
            waitForGameStart(session1)
            waitForGameStart(session2)

            // Bob tries PassTurn on Alice's turn
            val actionId = System.currentTimeMillis()
            session2.send(Frame.Text(json.encodeToString<GameMessage>(
                GameMessage.GameAction(NetworkAction.PassTurn, id2, actionId)
            )))

            // Bob should receive ActionRejected
            withTimeout(3000) {
                while (true) {
                    val frame = session2.incoming.receive() as Frame.Text
                    val msg = json.decodeFromString<GameMessage>(frame.readText())
                    if (msg is GameMessage.ActionRejected) {
                        assertEquals("Not your turn", msg.reason)
                        return@withTimeout
                    }
                }
            }

            session1.close()
            session2.close()
        } finally {
            client.close()
            server.stop(500, 1000)
        }
    }

    @Test
    fun `state stays consistent across multiple actions`() = runBlocking {
        val port = findFreePort()
        val config = ServerConfig(port = port, maxGames = 10, maxPlayersPerGame = 4)
        val lobbyManager = LobbyManager(config)
        val room = lobbyManager.createGame()!!
        val server = startDedicatedServer(port, lobbyManager)
        delay(500)
        val client = createWsClient()

        try {
            val (session1, id1) = connectPlayer(client, port, room.code, "Alice", createTestDeck())
            waitForLobbySize(session1, 1)
            val (session2, id2) = connectPlayer(client, port, room.code, "Bob", createTestDeck())
            waitForLobbySize(session1, 2)
            waitForLobbySize(session2, 2)

            session2.send(Frame.Text(json.encodeToString<GameMessage>(
                GameMessage.PlayerReady(id2, true)
            )))
            withTimeout(3000) {
                while (true) {
                    val frame = session1.incoming.receive() as Frame.Text
                    val msg = json.decodeFromString<GameMessage>(frame.readText())
                    if (msg is GameMessage.LobbyState && msg.players.any { it.id == id2 && it.isReady }) break
                }
            }

            room.startGame()
            waitForGameStart(session1)
            waitForGameStart(session2)

            // Send several actions
            val actions = listOf(
                NetworkAction.DrawCard(id1),
                NetworkAction.UpdateLife(id1, 35),
                NetworkAction.NextPhase,
                NetworkAction.NextPhase,
                NetworkAction.NextPhase
            )

            for (action in actions) {
                session1.send(Frame.Text(json.encodeToString<GameMessage>(
                    GameMessage.GameAction(action, id1)
                )))
                waitForStateUpdate(session1)
            }

            // Drain Bob's updates to get latest
            var lastState: GameState? = null
            withTimeout(3000) {
                try {
                    while (true) {
                        val frame = withTimeout(500) { session2.incoming.receive() } as Frame.Text
                        val msg = json.decodeFromString<GameMessage>(frame.readText())
                        if (msg is GameMessage.StateUpdate) lastState = msg.gameState
                    }
                } catch (_: TimeoutCancellationException) {}
            }

            assertNotNull(lastState)
            assertEquals(8, lastState!!.cardInstances.count { it.ownerId == id1 && it.zone == Zone.HAND })
            assertEquals(35, lastState!!.players.find { it.id == id1 }!!.life)
            assertEquals(GamePhase.MAIN_1, lastState!!.phase)

            session1.close()
            session2.close()
        } finally {
            client.close()
            server.stop(500, 1000)
        }
    }

    @Test
    fun `three player game on dedicated server`() = runBlocking {
        val port = findFreePort()
        val config = ServerConfig(port = port, maxGames = 10, maxPlayersPerGame = 4)
        val lobbyManager = LobbyManager(config)
        val room = lobbyManager.createGame()!!
        val server = startDedicatedServer(port, lobbyManager)
        delay(500)
        val client = createWsClient()

        try {
            val (s1, id1) = connectPlayer(client, port, room.code, "Alice", createTestDeck())
            waitForLobbySize(s1, 1)
            val (s2, id2) = connectPlayer(client, port, room.code, "Bob", createTestDeck())
            waitForLobbySize(s1, 2)
            waitForLobbySize(s2, 2)
            val (s3, id3) = connectPlayer(client, port, room.code, "Charlie", createTestDeck())
            waitForLobbySize(s1, 3)
            waitForLobbySize(s2, 3)
            waitForLobbySize(s3, 3)

            // Ready non-admin
            s2.send(Frame.Text(json.encodeToString<GameMessage>(GameMessage.PlayerReady(id2, true))))
            s3.send(Frame.Text(json.encodeToString<GameMessage>(GameMessage.PlayerReady(id3, true))))

            withTimeout(3000) {
                while (true) {
                    val frame = s1.incoming.receive() as Frame.Text
                    val msg = json.decodeFromString<GameMessage>(frame.readText())
                    if (msg is GameMessage.LobbyState &&
                        msg.players.filter { !it.isAdmin }.all { it.isReady }) break
                }
            }

            assertTrue(room.startGame())
            val start1 = waitForGameStart(s1)
            waitForGameStart(s2)
            waitForGameStart(s3)

            assertEquals(3, start1.gameState.players.size)

            // Turn cycle: Alice → Bob → Charlie → Alice
            s1.send(Frame.Text(json.encodeToString<GameMessage>(
                GameMessage.GameAction(NetworkAction.PassTurn, id1)
            )))
            val t2 = waitForStateUpdate(s1)
            assertEquals(2, t2.gameState.turnNumber)
            assertEquals(id2, t2.gameState.activePlayer.id)

            // Drain s2 to current state
            withTimeout(3000) {
                while (true) {
                    val frame = s2.incoming.receive() as Frame.Text
                    val msg = json.decodeFromString<GameMessage>(frame.readText())
                    if (msg is GameMessage.StateUpdate && msg.gameState.turnNumber == 2) break
                }
            }

            s2.send(Frame.Text(json.encodeToString<GameMessage>(
                GameMessage.GameAction(NetworkAction.PassTurn, id2)
            )))
            val t3 = waitForStateUpdate(s2)
            assertEquals(3, t3.gameState.turnNumber)
            assertEquals(id3, t3.gameState.activePlayer.id)

            s1.close()
            s2.close()
            s3.close()
        } finally {
            client.close()
            server.stop(500, 1000)
        }
    }

    @Test
    fun `GameClient connects to dedicated server by game code`() = runBlocking {
        val port = findFreePort()
        val config = ServerConfig(port = port, maxGames = 10, maxPlayersPerGame = 4)
        val lobbyManager = LobbyManager(config)
        val room = lobbyManager.createGame()!!
        val server = startDedicatedServer(port, lobbyManager)
        delay(500)

        // Use actual GameClient with gameCode parameter
        val gc1 = GameClient()
        val gc2 = GameClient()
        val deck = createTestDeck()

        val job1 = launch { gc1.connect("localhost", port, "Alice", deck, gameCode = room.code) }
        withTimeout(5000) { gc1.connectionState.first { it is ConnectionState.Connected } }

        val job2 = launch { gc2.connect("localhost", port, "Bob", deck, gameCode = room.code) }
        withTimeout(5000) { gc2.connectionState.first { it is ConnectionState.Connected } }

        // Both should see each other
        withTimeout(3000) { gc1.lobbyState.first { it?.players?.size == 2 } }
        withTimeout(3000) { gc2.lobbyState.first { it?.players?.size == 2 } }

        val names = gc1.lobbyState.value!!.players.map { it.name }.toSet()
        assertEquals(setOf("Alice", "Bob"), names)

        // Ready + start
        gc2.setReady(true)
        withTimeout(3000) {
            gc1.lobbyState.first { it?.players?.any { p -> !p.isAdmin && p.isReady } == true }
        }
        room.startGame()
        withTimeout(5000) { gc1.gameStarted.first { it } }
        withTimeout(5000) { gc2.gameStarted.first { it } }

        assertEquals(2, gc1.gameState.value!!.players.size)

        // Action works through GameClient
        val aliceId = gc1.playerId.value!!
        gc1.sendAction(NetworkAction.DrawCard(aliceId))

        withTimeout(3000) {
            gc2.gameState.first { state ->
                state != null && state.cardInstances.count {
                    it.ownerId == aliceId && it.zone == Zone.HAND
                } == 8
            }
        }

        gc1.disconnect()
        gc2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop(500, 1000)
    }

    @Test
    fun `chat message broadcasts to all players in room`() = runBlocking {
        val port = findFreePort()
        val config = ServerConfig(port = port, maxGames = 10, maxPlayersPerGame = 4)
        val lobbyManager = LobbyManager(config)
        val room = lobbyManager.createGame()!!
        val server = startDedicatedServer(port, lobbyManager)
        delay(500)
        val client = createWsClient()

        try {
            val (session1, id1) = connectPlayer(client, port, room.code, "Alice", createTestDeck())
            waitForLobbySize(session1, 1)
            val (session2, id2) = connectPlayer(client, port, room.code, "Bob", createTestDeck())
            waitForLobbySize(session1, 2)
            waitForLobbySize(session2, 2)

            // Ready + start
            session2.send(Frame.Text(json.encodeToString<GameMessage>(
                GameMessage.PlayerReady(id2, true)
            )))
            withTimeout(3000) {
                while (true) {
                    val frame = session1.incoming.receive() as Frame.Text
                    val msg = json.decodeFromString<GameMessage>(frame.readText())
                    if (msg is GameMessage.LobbyState && msg.players.any { it.id == id2 && it.isReady }) break
                }
            }
            room.startGame()
            waitForGameStart(session1)
            waitForGameStart(session2)

            // Alice sends chat
            session1.send(Frame.Text(json.encodeToString<GameMessage>(
                GameMessage.Chat(id1, "Alice", "Hello!")
            )))

            // Bob should receive the chat
            withTimeout(3000) {
                while (true) {
                    val frame = session2.incoming.receive() as Frame.Text
                    val msg = json.decodeFromString<GameMessage>(frame.readText())
                    if (msg is GameMessage.Chat && msg.message == "Hello!") {
                        assertEquals("Alice", msg.playerName)
                        return@withTimeout
                    }
                }
            }

            session1.close()
            session2.close()
        } finally {
            client.close()
            server.stop(500, 1000)
        }
    }
}
