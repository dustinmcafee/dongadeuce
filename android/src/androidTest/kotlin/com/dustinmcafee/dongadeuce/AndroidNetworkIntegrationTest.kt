package com.dustinmcafee.dongadeuce

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumented network integration tests.
 * Spins up a real CIO WebSocket server on the device and connects
 * real OkHttp GameClients over localhost.
 *
 * This validates the actual Android networking stack (CIO server +
 * OkHttp client) which differs from desktop (Netty server + CIO client).
 */
@RunWith(AndroidJUnit4::class)
class AndroidNetworkIntegrationTest {

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
    fun androidServer_startsAndAcceptsConnection() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(1000) // CIO server may need more startup time

        val client = GameClient()
        val deck = createTestDeck()

        val job = launch { client.connect("localhost", port, "Alice", deck) }

        withTimeout(10000) {
            client.connectionState.first { it is ConnectionState.Connected }
        }

        assertNotNull(client.playerId.value)

        withTimeout(5000) {
            client.lobbyState.first { it != null && it.players.isNotEmpty() }
        }

        assertEquals(1, client.lobbyState.value!!.players.size)
        assertEquals("Alice", client.lobbyState.value!!.players[0].name)
        assertTrue(client.lobbyState.value!!.players[0].isAdmin)

        client.disconnect()
        job.cancel()
        server.stop()
    }

    @Test
    fun androidServer_twoPlayersConnectAndSeeLobby() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(1000)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(10000) { client1.connectionState.first { it is ConnectionState.Connected } }

        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(10000) { client2.connectionState.first { it is ConnectionState.Connected } }

        withTimeout(5000) { client1.lobbyState.first { it?.players?.size == 2 } }
        withTimeout(5000) { client2.lobbyState.first { it?.players?.size == 2 } }

        val names = client1.lobbyState.value!!.players.map { it.name }.toSet()
        assertEquals(setOf("Alice", "Bob"), names)

        assertNotEquals(client1.playerId.value, client2.playerId.value)

        client1.disconnect()
        client2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop()
    }

    @Test
    fun androidServer_readyAndStartGame() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(1000)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(10000) { client1.connectionState.first { it is ConnectionState.Connected } }
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(10000) { client2.connectionState.first { it is ConnectionState.Connected } }
        withTimeout(5000) { client2.lobbyState.first { it?.players?.size == 2 } }

        client2.setReady(true)
        withTimeout(5000) {
            client1.lobbyState.first { it?.players?.any { p -> !p.isAdmin && p.isReady } == true }
        }

        assertTrue(server.startGame())

        withTimeout(10000) { client1.gameStarted.first { it } }
        withTimeout(10000) { client2.gameStarted.first { it } }

        val state = client1.gameState.value!!
        assertEquals(2, state.players.size)
        state.players.forEach { assertEquals(GameConstants.STARTING_LIFE, it.life) }

        // Verify card distribution
        val aliceId = client1.playerId.value!!
        val aliceHand = state.cardInstances.count { it.ownerId == aliceId && it.zone == Zone.HAND }
        val aliceLib = state.cardInstances.count { it.ownerId == aliceId && it.zone == Zone.LIBRARY }
        val aliceCmd = state.cardInstances.count { it.ownerId == aliceId && it.zone == Zone.COMMAND_ZONE }
        assertEquals(7, aliceHand)
        assertEquals(92, aliceLib)
        assertEquals(1, aliceCmd)

        client1.disconnect()
        client2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop()
    }

    @Test
    fun androidServer_actionBroadcastsToAllClients() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(1000)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        // Setup game
        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(10000) { client1.connectionState.first { it is ConnectionState.Connected } }
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(10000) { client2.connectionState.first { it is ConnectionState.Connected } }
        withTimeout(5000) { client2.lobbyState.first { it?.players?.size == 2 } }
        client2.setReady(true)
        withTimeout(5000) {
            client1.lobbyState.first { it?.players?.any { p -> !p.isAdmin && p.isReady } == true }
        }
        server.startGame()
        withTimeout(10000) { client1.gameStarted.first { it } }
        withTimeout(10000) { client2.gameStarted.first { it } }

        // Alice draws a card
        val aliceId = client1.playerId.value!!
        client1.sendAction(NetworkAction.DrawCard(aliceId))

        // Both should see Alice now has 8 cards in hand
        withTimeout(10000) {
            client1.gameState.first { state ->
                state?.cardInstances?.count { it.ownerId == aliceId && it.zone == Zone.HAND } == 8
            }
        }
        withTimeout(10000) {
            client2.gameState.first { state ->
                state?.cardInstances?.count { it.ownerId == aliceId && it.zone == Zone.HAND } == 8
            }
        }

        client1.disconnect()
        client2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop()
    }

    @Test
    fun androidServer_lifeChangeSyncsAcrossClients() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(1000)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(10000) { client1.connectionState.first { it is ConnectionState.Connected } }
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(10000) { client2.connectionState.first { it is ConnectionState.Connected } }
        withTimeout(5000) { client2.lobbyState.first { it?.players?.size == 2 } }
        client2.setReady(true)
        withTimeout(5000) {
            client1.lobbyState.first { it?.players?.any { p -> !p.isAdmin && p.isReady } == true }
        }
        server.startGame()
        withTimeout(10000) { client1.gameStarted.first { it } }

        val aliceId = client1.playerId.value!!
        client1.sendAction(NetworkAction.UpdateLife(aliceId, 33))

        withTimeout(10000) {
            client2.gameState.first { it?.players?.find { p -> p.id == aliceId }?.life == 33 }
        }

        assertEquals(33, client2.gameState.value!!.players.find { it.id == aliceId }!!.life)

        client1.disconnect()
        client2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop()
    }

    @Test
    fun androidServer_turnPassAndPhaseAdvance() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(1000)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(10000) { client1.connectionState.first { it is ConnectionState.Connected } }
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(10000) { client2.connectionState.first { it is ConnectionState.Connected } }
        withTimeout(5000) { client2.lobbyState.first { it?.players?.size == 2 } }
        client2.setReady(true)
        withTimeout(5000) {
            client1.lobbyState.first { it?.players?.any { p -> !p.isAdmin && p.isReady } == true }
        }
        server.startGame()
        withTimeout(10000) { client1.gameStarted.first { it } }

        // Advance phase
        client1.sendAction(NetworkAction.NextPhase)
        withTimeout(5000) {
            client1.gameState.first { it?.phase == GamePhase.UPKEEP }
        }

        // Pass turn
        client1.sendAction(NetworkAction.PassTurn)
        val bobId = client2.playerId.value!!
        withTimeout(5000) {
            client2.gameState.first { it?.turnNumber == 2 && it.activePlayer.id == bobId }
        }

        assertEquals(2, client2.gameState.value!!.turnNumber)
        assertEquals(GamePhase.UNTAP, client2.gameState.value!!.phase)

        client1.disconnect()
        client2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop()
    }

    @Test
    fun androidServer_disconnectPausesGame() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 4)
        server.start()
        delay(1000)

        val client1 = GameClient()
        val client2 = GameClient()
        val deck = createTestDeck()

        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(10000) { client1.connectionState.first { it is ConnectionState.Connected } }
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(10000) { client2.connectionState.first { it is ConnectionState.Connected } }
        withTimeout(5000) { client2.lobbyState.first { it?.players?.size == 2 } }
        client2.setReady(true)
        withTimeout(5000) {
            client1.lobbyState.first { it?.players?.any { p -> !p.isAdmin && p.isReady } == true }
        }
        server.startGame()
        withTimeout(10000) { client1.gameStarted.first { it } }

        // Bob disconnects
        client2.disconnect()
        job2.cancel()

        // Alice should see pause
        withTimeout(10000) {
            client1.isPaused.first { it }
        }
        assertTrue(client1.isPaused.value)

        client1.disconnect()
        job1.cancel()
        server.stop()
    }

    @Test
    fun androidServer_lobbyFullRejectsPlayer() = runBlocking {
        val port = findFreePort()
        val server = GameServer(port = port, maxPlayers = 2)
        server.start()
        delay(1000)

        val client1 = GameClient()
        val client2 = GameClient()
        val client3 = GameClient()
        val deck = createTestDeck()

        val job1 = launch { client1.connect("localhost", port, "Alice", deck) }
        withTimeout(10000) { client1.connectionState.first { it is ConnectionState.Connected } }
        val job2 = launch { client2.connect("localhost", port, "Bob", deck) }
        withTimeout(10000) { client2.connectionState.first { it is ConnectionState.Connected } }

        // Third player should be rejected
        val job3 = launch { client3.connect("localhost", port, "Charlie", deck) }
        withTimeout(10000) {
            client3.connectionState.first { it is ConnectionState.Error }
        }
        assertTrue(client3.connectionState.value is ConnectionState.Error)

        client1.disconnect()
        client2.disconnect()
        client3.disconnect()
        job1.cancel()
        job2.cancel()
        job3.cancel()
        server.stop()
    }
}
