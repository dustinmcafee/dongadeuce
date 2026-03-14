package com.dustinmcafee.dongadeuce.viewmodel

import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.test.*

class GameViewModelTest {

    private fun createTestDeck(): Deck {
        return Deck(
            name = "Test Deck",
            commander = Card(name = "Test Commander", type = "Legendary Creature", power = "4", toughness = "4"),
            cards = (1..99).map { Card(name = "Card $it", type = "Creature", power = "2", toughness = "2") }
        )
    }

    @Test
    fun `initial state has correct defaults`() {
        val vm = GameViewModel()
        val state = vm.uiState.value

        assertNull(state.localPlayer)
        assertTrue(state.opponents.isEmpty())
        assertNull(state.gameState)
        assertNull(state.selectedCardId)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isHotseatMode)
        assertFalse(state.isNetworkMode)
        assertFalse(state.isPaused)
        assertFalse(state.gameEnded)
    }

    @Test
    fun `isNetworkGame false without client`() {
        val vm = GameViewModel()
        assertFalse(vm.isNetworkGame)
    }

    @Test
    fun `isNetworkGame true with client`() {
        val client = GameClient()
        val vm = GameViewModel(networkClient = client)
        assertTrue(vm.isNetworkGame)
    }

    @Test
    fun `initializeGame creates players and game state`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob", "Charlie"), isHotseatMode = true)

        val state = vm.uiState.value
        assertNotNull(state.localPlayer)
        assertEquals("Alice", state.localPlayer!!.name)
        assertEquals(2, state.opponents.size)
        assertEquals("Bob", state.opponents[0].name)
        assertEquals("Charlie", state.opponents[1].name)
        assertTrue(state.isHotseatMode)

        assertNotNull(state.gameState)
        assertEquals(3, state.gameState!!.players.size)
        assertEquals(1, state.gameState!!.turnNumber)
    }

    @Test
    fun `initializeGame sets 40 life for all players`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))

        val state = vm.uiState.value
        assertEquals(40, state.localPlayer!!.life)
        assertEquals(40, state.opponents[0].life)
    }

    @Test
    fun `loadDeck puts cards in correct zones`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))

        val deck = createTestDeck()
        vm.loadDeck(deck)

        val state = vm.uiState.value
        val gameState = state.gameState!!
        val localId = state.localPlayer!!.id

        val cmdZone = gameState.cardInstances.filter { it.ownerId == localId && it.zone == Zone.COMMAND_ZONE }
        val library = gameState.cardInstances.filter { it.ownerId == localId && it.zone == Zone.LIBRARY }

        assertEquals(1, cmdZone.size)
        assertEquals("Test Commander", cmdZone[0].card.name)
        assertEquals(99, library.size)
    }

    @Test
    fun `loadDeckForPlayer loads deck for specific player`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))

        val state = vm.uiState.value
        val bobId = state.opponents[0].id
        val deck = createTestDeck()

        vm.loadDeckForPlayer(bobId, deck)

        val newState = vm.uiState.value
        val bobCards = newState.gameState!!.cardInstances.filter { it.ownerId == bobId }
        assertEquals(100, bobCards.size) // 99 + 1 commander
    }

    @Test
    fun `drawCard in hotseat mode moves card from library to hand`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        val deck = createTestDeck()
        vm.loadDeck(deck)

        val localId = vm.uiState.value.localPlayer!!.id
        val libBefore = vm.uiState.value.gameState!!.cardInstances.count {
            it.ownerId == localId && it.zone == Zone.LIBRARY
        }

        vm.drawCard(localId)

        val libAfter = vm.uiState.value.gameState!!.cardInstances.count {
            it.ownerId == localId && it.zone == Zone.LIBRARY
        }
        assertEquals(libBefore - 1, libAfter)
    }

    @Test
    fun `updateLife in hotseat mode changes life`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))

        val localId = vm.uiState.value.localPlayer!!.id
        vm.updateLife(localId, 35)

        assertEquals(35, vm.uiState.value.gameState!!.players.find { it.id == localId }!!.life)
    }

    @Test
    fun `moveCard in hotseat mode changes zone`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())

        val localId = vm.uiState.value.localPlayer!!.id
        val handCard = vm.uiState.value.gameState!!.cardInstances
            .filter { it.ownerId == localId && it.zone == Zone.LIBRARY }
            .first()

        // Move from library to hand first
        vm.moveCard(handCard.instanceId, Zone.HAND)

        val movedCard = vm.uiState.value.gameState!!.cardInstances
            .find { it.instanceId == handCard.instanceId }!!
        assertEquals(Zone.HAND, movedCard.zone)
    }

    @Test
    fun `toggleTap in hotseat mode toggles card`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())

        val localId = vm.uiState.value.localPlayer!!.id
        val card = vm.uiState.value.gameState!!.cardInstances
            .first { it.ownerId == localId && it.zone == Zone.LIBRARY }

        vm.moveCard(card.instanceId, Zone.BATTLEFIELD)
        vm.toggleTap(card.instanceId)

        val tapped = vm.uiState.value.gameState!!.cardInstances
            .find { it.instanceId == card.instanceId }!!
        assertTrue(tapped.isTapped)
    }

    @Test
    fun `addPlayerCounter in hotseat mode adds counter`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))

        val localId = vm.uiState.value.localPlayer!!.id
        vm.addPlayerCounter(localId, "poison", 3)

        val player = vm.uiState.value.gameState!!.players.find { it.id == localId }!!
        assertEquals(3, player.getCounter("poison"))
    }

    @Test
    fun `removePlayerCounter in hotseat mode removes counter`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))

        val localId = vm.uiState.value.localPlayer!!.id
        vm.addPlayerCounter(localId, "energy", 5)
        vm.removePlayerCounter(localId, "energy", 2)

        val player = vm.uiState.value.gameState!!.players.find { it.id == localId }!!
        assertEquals(3, player.getCounter("energy"))
    }

    @Test
    fun `setPlayerCounter in hotseat mode sets exact value`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))

        val localId = vm.uiState.value.localPlayer!!.id
        vm.setPlayerCounter(localId, "experience", 10)

        val player = vm.uiState.value.gameState!!.players.find { it.id == localId }!!
        assertEquals(10, player.getCounter("experience"))
    }

    @Test
    fun `network mode delegates to client`() = runBlocking {
        // Create a real server+client to test the network path
        val port = java.net.ServerSocket(0).use { it.localPort }
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

        // Create GameViewModel with network client
        val aliceId = client1.playerId.value!!
        val vm = GameViewModel(networkClient = client1, localPlayerId = aliceId)
        assertTrue(vm.isNetworkGame)

        // Wait for state to propagate
        withTimeout(3000) {
            vm.uiState.first { it.gameState != null }
        }

        // Network draw should go through client
        vm.drawCard(aliceId)

        withTimeout(3000) {
            vm.uiState.first { state ->
                state.gameState?.cardInstances?.count {
                    it.ownerId == aliceId && it.zone == Zone.HAND
                } == 8
            }
        }

        assertEquals(8, vm.uiState.value.gameState!!.cardInstances.count {
            it.ownerId == aliceId && it.zone == Zone.HAND
        })

        // Cleanup
        vm.cleanup()
        client1.disconnect()
        client2.disconnect()
        job1.cancel()
        job2.cancel()
        server.stop()
    }

    @Test
    fun `cleanup cancels scope`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.cleanup()
        // Should not throw
    }

    @Test
    fun `drawStartingHand draws correct number`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())

        val localId = vm.uiState.value.localPlayer!!.id
        vm.drawStartingHand(localId)

        val hand = vm.uiState.value.gameState!!.cardInstances.count {
            it.ownerId == localId && it.zone == Zone.HAND
        }
        assertEquals(GameConstants.STARTING_HAND_SIZE, hand)
    }

    @Test
    fun `markPlayerAsLost sets hasLost`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))

        val bobId = vm.uiState.value.opponents[0].id
        vm.markPlayerAsLost(bobId, "conceded")

        val bob = vm.uiState.value.gameState!!.players.find { it.id == bobId }!!
        assertTrue(bob.hasLost)
    }

    @Test
    fun `giveControlTo changes controller in hotseat`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())

        val aliceId = vm.uiState.value.localPlayer!!.id
        val bobId = vm.uiState.value.opponents[0].id
        val card = vm.uiState.value.gameState!!.cardInstances
            .first { it.ownerId == aliceId && it.zone == Zone.LIBRARY }

        vm.moveCard(card.instanceId, Zone.BATTLEFIELD)
        vm.giveControlTo(card.instanceId, bobId)

        val updated = vm.uiState.value.gameState!!.cardInstances
            .find { it.instanceId == card.instanceId }!!
        assertEquals(bobId, updated.controllerId)
        assertEquals(aliceId, updated.ownerId) // ownership stays
    }
}
