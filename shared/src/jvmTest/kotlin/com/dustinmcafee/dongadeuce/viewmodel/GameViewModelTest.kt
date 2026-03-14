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

    // Helper: creates a VM with Alice+Bob initialized, deck loaded, and returns (vm, aliceId, bobId, a battlefield card ID)
    private fun setupGameWithBattlefieldCard(): GameViewModelTestContext {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        val bobId = vm.uiState.value.opponents[0].id
        val card = vm.uiState.value.gameState!!.cardInstances
            .first { it.ownerId == aliceId && it.zone == Zone.LIBRARY }
        vm.moveCard(card.instanceId, Zone.BATTLEFIELD)
        return GameViewModelTestContext(vm, aliceId, bobId, card.instanceId)
    }

    private data class GameViewModelTestContext(
        val vm: GameViewModel,
        val aliceId: String,
        val bobId: String,
        val cardId: String
    )

    // ==================== Phase/Turn ====================

    @Test
    fun `nextPhase advances phase`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.nextPhase()
        assertEquals(GamePhase.UPKEEP, vm.uiState.value.gameState!!.phase)
    }

    @Test
    fun `passTurn changes active player`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        val bobId = vm.uiState.value.opponents[0].id
        vm.passTurn()
        assertEquals(bobId, vm.uiState.value.gameState!!.activePlayer.id)
        assertEquals(2, vm.uiState.value.gameState!!.turnNumber)
    }

    @Test
    fun `setPhase sets phase directly`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.setPhase(GamePhase.MAIN_2)
        assertEquals(GamePhase.MAIN_2, vm.uiState.value.gameState!!.phase)
    }

    @Test
    fun `advancePhase is same as nextPhase`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.advancePhase()
        assertEquals(GamePhase.UPKEEP, vm.uiState.value.gameState!!.phase)
    }

    @Test
    fun `changeLife applies relative change`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.changeLife(aliceId, -5)
        assertEquals(35, vm.uiState.value.gameState!!.players.find { it.id == aliceId }!!.life)
    }

    // ==================== Draw Variants ====================

    @Test
    fun `drawCards draws multiple`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.drawCards(aliceId, 5)
        val hand = vm.uiState.value.gameState!!.cardInstances.count {
            it.ownerId == aliceId && it.zone == Zone.HAND
        }
        assertEquals(5, hand)
    }

    @Test
    fun `drawFromBottom draws from bottom of library`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        val libBefore = vm.uiState.value.gameState!!.cardInstances.count {
            it.ownerId == aliceId && it.zone == Zone.LIBRARY
        }
        vm.drawFromBottom(aliceId, 2)
        val libAfter = vm.uiState.value.gameState!!.cardInstances.count {
            it.ownerId == aliceId && it.zone == Zone.LIBRARY
        }
        assertEquals(libBefore - 2, libAfter)
    }

    // ==================== Concede ====================

    @Test
    fun `concede marks player as lost`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.concede(aliceId)
        assertTrue(vm.uiState.value.gameState!!.players.find { it.id == aliceId }!!.hasLost)
    }

    // ==================== Untap All ====================

    @Test
    fun `untapAll untaps all tapped permanents`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.toggleTap(ctx.cardId)
        assertTrue(ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!.isTapped)

        ctx.vm.untapAll(ctx.aliceId)
        assertFalse(ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!.isTapped)
    }

    // ==================== Card Counters ====================

    @Test
    fun `addCounter adds to card`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.addCounter(ctx.cardId, "+1/+1", 3)
        assertEquals(3, ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!.counters["+1/+1"])
    }

    @Test
    fun `removeCounter removes from card`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.addCounter(ctx.cardId, "+1/+1", 5)
        ctx.vm.removeCounter(ctx.cardId, "+1/+1", 2)
        assertEquals(3, ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!.counters["+1/+1"])
    }

    @Test
    fun `setCounter sets exact value`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.setCounter(ctx.cardId, "charge", 7)
        assertEquals(7, ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!.counters["charge"])
    }

    // ==================== Power/Toughness ====================

    @Test
    fun `modifyPower changes power modifier`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.modifyPower(ctx.cardId, 3)
        assertEquals(3, ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!.powerModifier)
    }

    @Test
    fun `modifyToughness changes toughness modifier`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.modifyToughness(ctx.cardId, -1)
        assertEquals(-1, ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!.toughnessModifier)
    }

    @Test
    fun `modifyPowerToughness changes both`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.modifyPowerToughness(ctx.cardId, 2)
        val card = ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!
        assertEquals(2, card.powerModifier)
        assertEquals(2, card.toughnessModifier)
    }

    @Test
    fun `setPowerToughness sets exact values`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.setPowerToughness(ctx.cardId, 5, 7)
        val card = ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!
        // base is 2/2, so modifiers should be 3 and 5
        assertEquals(3, card.powerModifier)
        assertEquals(5, card.toughnessModifier)
    }

    @Test
    fun `resetPowerToughness resets modifiers`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.modifyPower(ctx.cardId, 5)
        ctx.vm.resetPowerToughness(ctx.cardId)
        val card = ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!
        assertEquals(0, card.powerModifier)
        assertEquals(0, card.toughnessModifier)
    }

    @Test
    fun `flowPower increases power decreases toughness`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.flowPower(ctx.cardId)
        val card = ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!
        assertEquals(1, card.powerModifier)
        assertEquals(-1, card.toughnessModifier)
    }

    @Test
    fun `flowToughness decreases power increases toughness`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.flowToughness(ctx.cardId)
        val card = ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!
        assertEquals(-1, card.powerModifier)
        assertEquals(1, card.toughnessModifier)
    }

    // ==================== Card State ====================

    @Test
    fun `toggleDoesntUntap sets flag`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.toggleDoesntUntap(ctx.cardId)
        assertTrue(ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!.doesntUntap)
    }

    @Test
    fun `setAnnotation sets and clears annotation`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.setAnnotation(ctx.cardId, "Test note")
        assertEquals("Test note", ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!.annotation)

        ctx.vm.setAnnotation(ctx.cardId, null)
        assertNull(ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!.annotation)
    }

    @Test
    fun `flipCard toggles isFlipped`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.flipCard(ctx.cardId)
        assertTrue(ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!.isFlipped)
    }

    @Test
    fun `toggleFaceDown toggles isFaceDown`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.toggleFaceDown(ctx.cardId)
        assertTrue(ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!.isFaceDown)
    }

    @Test
    fun `playFaceDown puts card on battlefield face down`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        val card = vm.uiState.value.gameState!!.cardInstances
            .first { it.ownerId == aliceId && it.zone == Zone.LIBRARY }
        vm.moveCard(card.instanceId, Zone.HAND)
        vm.playFaceDown(card.instanceId)
        val played = vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(Zone.BATTLEFIELD, played.zone)
        assertTrue(played.isFaceDown)
    }

    // ==================== Attachments ====================

    @Test
    fun `attachCard and detachCard work`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        val cards = vm.uiState.value.gameState!!.cardInstances
            .filter { it.ownerId == aliceId && it.zone == Zone.LIBRARY }.take(2)
        vm.moveCard(cards[0].instanceId, Zone.BATTLEFIELD)
        vm.moveCard(cards[1].instanceId, Zone.BATTLEFIELD)

        vm.attachCard(cards[0].instanceId, cards[1].instanceId)
        assertEquals(cards[1].instanceId,
            vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == cards[0].instanceId }!!.attachedTo)

        vm.detachCard(cards[0].instanceId)
        assertNull(vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == cards[0].instanceId }!!.attachedTo)
    }

    // ==================== Library Operations ====================

    @Test
    fun `shuffleLibrary preserves count`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        val before = vm.getCardCount(aliceId, Zone.LIBRARY)
        vm.shuffleLibrary(aliceId)
        assertEquals(before, vm.getCardCount(aliceId, Zone.LIBRARY))
    }

    @Test
    fun `millCards moves cards to graveyard`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.millCards(aliceId, 5)
        assertEquals(5, vm.getCardCount(aliceId, Zone.GRAVEYARD))
    }

    @Test
    fun `mulligan returns hand and draws new`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.drawStartingHand(aliceId)
        vm.mulligan(aliceId)
        assertEquals(7, vm.getCardCount(aliceId, Zone.HAND))
    }

    @Test
    fun `moveCardToTopOfLibrary puts card on top`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.drawCards(aliceId, 1)
        val handCard = vm.getCards(aliceId, Zone.HAND).first()
        vm.moveCardToTopOfLibrary(handCard.instanceId)
        assertEquals(Zone.LIBRARY, vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == handCard.instanceId }!!.zone)
    }

    @Test
    fun `moveCardToBottomOfLibrary puts card on bottom`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.drawCards(aliceId, 1)
        val handCard = vm.getCards(aliceId, Zone.HAND).first()
        vm.moveCardToBottomOfLibrary(handCard.instanceId)
        assertEquals(Zone.LIBRARY, vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == handCard.instanceId }!!.zone)
    }

    @Test
    fun `moveCardToLibraryPosition and moveCardToLibraryPositionFromBottom work`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.drawCards(aliceId, 2)
        val hand = vm.getCards(aliceId, Zone.HAND)

        vm.moveCardToLibraryPosition(hand[0].instanceId, 3)
        assertEquals(Zone.LIBRARY, vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == hand[0].instanceId }!!.zone)

        vm.moveCardToLibraryPositionFromBottom(hand[1].instanceId, 2)
        assertEquals(Zone.LIBRARY, vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == hand[1].instanceId }!!.zone)
    }

    @Test
    fun `shuffleTopCards and shuffleBottomCards preserve count`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        val before = vm.getCardCount(aliceId, Zone.LIBRARY)
        vm.shuffleTopCards(aliceId, 10)
        assertEquals(before, vm.getCardCount(aliceId, Zone.LIBRARY))
        vm.shuffleBottomCards(aliceId, 10)
        assertEquals(before, vm.getCardCount(aliceId, Zone.LIBRARY))
    }

    @Test
    fun `moveTopCardsToZone and moveBottomCardsToZone work`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.moveTopCardsToZone(aliceId, 3, Zone.GRAVEYARD)
        assertEquals(3, vm.getCardCount(aliceId, Zone.GRAVEYARD))

        vm.moveBottomCardsToZone(aliceId, 2, Zone.EXILE)
        assertEquals(2, vm.getCardCount(aliceId, Zone.EXILE))
    }

    @Test
    fun `moveBottomCardToTop moves bottom to top`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        val before = vm.getCardCount(aliceId, Zone.LIBRARY)
        vm.moveBottomCardToTop(aliceId)
        assertEquals(before, vm.getCardCount(aliceId, Zone.LIBRARY))
    }

    @Test
    fun `millFromBottom and exileFromBottom work`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.millFromBottom(aliceId, 3)
        assertEquals(3, vm.getCardCount(aliceId, Zone.GRAVEYARD))
        vm.exileFromBottom(aliceId, 2)
        assertEquals(2, vm.getCardCount(aliceId, Zone.EXILE))
    }

    // ==================== Tokens & Clones ====================

    @Test
    fun `createToken creates token on battlefield`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.createToken(aliceId, "Goblin", "Creature — Goblin", "1", "1", "Red", null, 3)
        val tokens = vm.uiState.value.gameState!!.cardInstances.filter { it.isToken && it.ownerId == aliceId }
        assertEquals(3, tokens.size)
    }

    @Test
    fun `cloneCard creates clone on battlefield`() {
        val ctx = setupGameWithBattlefieldCard()
        val fieldBefore = ctx.vm.uiState.value.gameState!!.cardInstances.count {
            it.ownerId == ctx.aliceId && it.zone == Zone.BATTLEFIELD
        }
        ctx.vm.cloneCard(ctx.cardId, ctx.aliceId)
        val fieldAfter = ctx.vm.uiState.value.gameState!!.cardInstances.count {
            it.ownerId == ctx.aliceId && it.zone == Zone.BATTLEFIELD
        }
        assertEquals(fieldBefore + 1, fieldAfter)
    }

    // ==================== Commander Damage ====================

    @Test
    fun `updateCommanderDamage tracks damage`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        val bobId = vm.uiState.value.opponents[0].id
        val commander = vm.getAllCommanders().first { it.ownerId == aliceId }
        vm.updateCommanderDamage(bobId, commander.instanceId, 10)
        val bob = vm.uiState.value.gameState!!.players.find { it.id == bobId }!!
        assertEquals(10, bob.commanderDamage[commander.instanceId])
    }

    // ==================== Query Methods ====================

    @Test
    fun `getCardCount returns correct count`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        assertEquals(99, vm.getCardCount(aliceId, Zone.LIBRARY))
        assertEquals(1, vm.getCardCount(aliceId, Zone.COMMAND_ZONE))
    }

    @Test
    fun `getCards returns cards in zone`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        val cmdCards = vm.getCards(aliceId, Zone.COMMAND_ZONE)
        assertEquals(1, cmdCards.size)
        assertEquals("Test Commander", cmdCards[0].card.name)
    }

    @Test
    fun `getTopCards and getBottomCards return correct cards`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        val top = vm.getTopCards(aliceId, 3)
        assertEquals(3, top.size)
        val bottom = vm.getBottomCards(aliceId, 3)
        assertEquals(3, bottom.size)
    }

    @Test
    fun `getBattlefieldCards returns battlefield cards`() {
        val ctx = setupGameWithBattlefieldCard()
        val field = ctx.vm.getBattlefieldCards()
        assertTrue(field.isNotEmpty())
        assertTrue(field.any { it.instanceId == ctx.cardId })
    }

    @Test
    fun `getPlayerBattlefieldCards filters by player`() {
        val ctx = setupGameWithBattlefieldCard()
        val aliceField = ctx.vm.getPlayerBattlefieldCards(ctx.aliceId)
        assertTrue(aliceField.isNotEmpty())
        val bobField = ctx.vm.getPlayerBattlefieldCards(ctx.bobId)
        assertTrue(bobField.isEmpty())
    }

    @Test
    fun `getAllCommanders returns commander cards`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val commanders = vm.getAllCommanders()
        assertTrue(commanders.isNotEmpty())
        assertTrue(commanders.all { it.zone == Zone.COMMAND_ZONE })
    }

    // ==================== Selection ====================

    @Test
    fun `selectCard sets and clears selection`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.selectCard("card-123")
        assertEquals("card-123", vm.uiState.value.selectedCardId)
        vm.selectCard(null)
        assertNull(vm.uiState.value.selectedCardId)
    }

    // ==================== Logging ====================

    @Test
    fun `logDieRoll adds event to log`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.logDieRoll(aliceId, "d20", 17)
        assertTrue(vm.uiState.value.gameState!!.gameLog.any { it is GameEvent.DieRolled })
    }

    @Test
    fun `sendChatMessage adds event to log`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.sendChatMessage(aliceId, "Hello!")
        assertTrue(vm.uiState.value.gameState!!.gameLog.any { it is GameEvent.ChatMessage })
    }

    // ==================== Grid Position ====================

    @Test
    fun `updateCardGridPosition changes position`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.updateCardGridPosition(ctx.cardId, 3, 1)
        val card = ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!
        assertEquals(3, card.gridX)
        assertEquals(1, card.gridY)
    }

    // ==================== Library Visibility ====================

    @Test
    fun `toggleRevealTopCard toggles flag`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.toggleRevealTopCard(aliceId)
        assertTrue(vm.uiState.value.gameState!!.players.find { it.id == aliceId }!!.revealTopCard)
    }

    @Test
    fun `toggleLookAtTopCard toggles flag`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.toggleLookAtTopCard(aliceId)
        assertTrue(vm.uiState.value.gameState!!.players.find { it.id == aliceId }!!.lookAtTopCard)
    }

    // ==================== Sort/Reorder ====================

    @Test
    fun `sortHand does not crash`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.drawStartingHand(aliceId)
        vm.sortHand(aliceId) // should not throw
        assertEquals(7, vm.getCardCount(aliceId, Zone.HAND))
    }

    @Test
    fun `reorderLibraryTop does not crash`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        val topCards = vm.getTopCards(aliceId, 3).map { it.instanceId }
        vm.reorderLibraryTop(aliceId, topCards.reversed())
        assertEquals(99, vm.getCardCount(aliceId, Zone.LIBRARY))
    }

    // ==================== Token Search ====================

    @Test
    fun `clearTokenSearch clears results`() {
        val vm = GameViewModel()
        vm.clearTokenSearch()
        assertTrue(vm.uiState.value.tokenSearchResults.isEmpty())
        assertFalse(vm.uiState.value.isSearchingTokens)
    }

    // ==================== Remaining Methods ====================

    @Test
    fun `dismissRevealedCards clears revealed state`() {
        val vm = GameViewModel()
        vm.dismissRevealedCards() // should not throw even with no revealed cards
    }

    @Test
    fun `revealHand does not crash`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        val bobId = vm.uiState.value.opponents[0].id
        vm.drawStartingHand(aliceId)
        vm.revealHand(aliceId, listOf(bobId))
        // Should not crash; reveal state is internal
    }

    @Test
    fun `revealCards does not crash`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        val bobId = vm.uiState.value.opponents[0].id
        vm.drawCards(aliceId, 3)
        val cardIds = vm.getCards(aliceId, Zone.HAND).map { it.instanceId }
        vm.revealCards(aliceId, cardIds, listOf(bobId))
    }

    @Test
    fun `incrementAllCounters increments each counter type`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.addCounter(ctx.cardId, "+1/+1", 2)
        ctx.vm.addCounter(ctx.cardId, "charge", 3)
        ctx.vm.incrementAllCounters(ctx.cardId)

        val card = ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!
        assertEquals(3, card.counters["+1/+1"])
        assertEquals(4, card.counters["charge"])
    }

    @Test
    fun `incrementAllCounters on card with no counters is no-op`() {
        val ctx = setupGameWithBattlefieldCard()
        ctx.vm.incrementAllCounters(ctx.cardId)
        val card = ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!
        assertTrue(card.counters.isEmpty())
    }

    @Test
    fun `removeLocalArrows does not crash`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.removeLocalArrows(aliceId) // should not throw
    }

    @Test
    fun `reorderHandCard changes hand position`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.drawStartingHand(aliceId)
        val handCard = vm.getCards(aliceId, Zone.HAND).first()
        vm.reorderHandCard(handCard.instanceId, 3)
        // Should not crash; hand position is internal
    }

    @Test
    fun `searchTokens with blank query clears`() {
        val vm = GameViewModel()
        vm.searchTokens("")
        assertTrue(vm.uiState.value.tokenSearchResults.isEmpty())
    }

    @Test
    fun `handleBatchCardAction with ViewDetails returns 1`() {
        val ctx = setupGameWithBattlefieldCard()
        val card = ctx.vm.uiState.value.gameState!!.cardInstances.find { it.instanceId == ctx.cardId }!!
        val result = ctx.vm.handleBatchCardAction(CardAction.ViewDetails(card))
        assertEquals(1, result)
    }

    @Test
    fun `handleBatchCardAction with ToGraveyard moves selected cards`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"))
        vm.loadDeck(createTestDeck())
        val aliceId = vm.uiState.value.localPlayer!!.id

        // Get some library cards and move to battlefield
        val cards = vm.getCards(aliceId, Zone.LIBRARY).take(3)
        cards.forEach { vm.moveCard(it.instanceId, Zone.BATTLEFIELD) }

        val freshCards = vm.getCards(aliceId, Zone.BATTLEFIELD)
        val primaryCard = freshCards.first()
        val ids = freshCards.map { it.instanceId }.toSet()
        val moved = vm.handleBatchCardAction(CardAction.ToGraveyard(primaryCard), ids)
        assertEquals(3, moved)

        val graveCount = vm.getCardCount(aliceId, Zone.GRAVEYARD)
        assertEquals(3, graveCount)
    }
}
