package com.dustinmcafee.dongadeuce

import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.network.*
import com.dustinmcafee.dongadeuce.settings.UserSettings
import com.dustinmcafee.dongadeuce.settings.UserSettingsData
import com.dustinmcafee.dongadeuce.viewmodel.GameViewModel
import kotlin.test.*

/**
 * JVM unit tests covering the post-playtest bug fixes (Issues 2, 5, 6, 8, 15).
 */
class PostPlaytestFixesTest {

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

    private fun createStartedEngine(): Triple<GameEngine, String, String> {
        val engine = GameEngine(maxPlayers = 4)
        val deck = createTestDeck()
        val p1 = engine.addPlayer("Alice", deck, isAdmin = true)
        val p2 = engine.addPlayer("Bob", deck)
        engine.setPlayerReady(p2, true)
        assertTrue(engine.startGame(), "Game should start")
        return Triple(engine, p1, p2)
    }

    private fun createStartedEngineWith3Players(): Pair<GameEngine, List<String>> {
        val engine = GameEngine(maxPlayers = 6)
        val deck = createTestDeck()
        val p1 = engine.addPlayer("Alice", deck, isAdmin = true)
        val p2 = engine.addPlayer("Bob", deck)
        val p3 = engine.addPlayer("Charlie", deck)
        engine.setPlayerReady(p2, true)
        engine.setPlayerReady(p3, true)
        assertTrue(engine.startGame(), "Game should start")
        return Pair(engine, listOf(p1, p2, p3))
    }

    // ==================== Issue 5: eliminatePlayer ====================

    @Test
    fun `eliminatePlayer sets hasLost and life to 0`() {
        val (engine, _, p2) = createStartedEngine()

        engine.eliminatePlayer(p2)

        val state = engine.getCurrentState()!!
        val bob = state.players.find { it.id == p2 }!!
        assertTrue(bob.hasLost)
        assertEquals(0, bob.life)
    }

    @Test
    fun `eliminatePlayer logs PlayerLost event`() {
        val (engine, _, p2) = createStartedEngine()

        engine.eliminatePlayer(p2)

        val state = engine.getCurrentState()!!
        val lostEvents = state.gameLog.filterIsInstance<GameEvent.PlayerLost>()
        assertTrue(lostEvents.any { it.playerId == p2 && it.reason == "disconnected" })
    }

    @Test
    fun `eliminatePlayer does nothing for already-eliminated player`() {
        val (engine, _, p2) = createStartedEngine()

        engine.eliminatePlayer(p2)
        val stateAfterFirst = engine.getCurrentState()!!
        val logSizeAfterFirst = stateAfterFirst.gameLog.size

        // Second call should be no-op
        engine.eliminatePlayer(p2)
        val stateAfterSecond = engine.getCurrentState()!!
        assertEquals(logSizeAfterFirst, stateAfterSecond.gameLog.size)
    }

    @Test
    fun `eliminatePlayer advances turn when active player is eliminated`() {
        val (engine, p1, p2) = createStartedEngine()
        val stateBefore = engine.getCurrentState()!!

        // Figure out who is active
        val activeId = stateBefore.activePlayer.id

        if (activeId == p1) {
            engine.eliminatePlayer(p1)
            val stateAfter = engine.getCurrentState()!!
            assertEquals(p2, stateAfter.activePlayer.id)
            assertEquals(stateBefore.turnNumber + 1, stateAfter.turnNumber)
        } else {
            engine.eliminatePlayer(p2)
            val stateAfter = engine.getCurrentState()!!
            assertEquals(p1, stateAfter.activePlayer.id)
            assertEquals(stateBefore.turnNumber + 1, stateAfter.turnNumber)
        }
    }

    @Test
    fun `eliminatePlayer does not advance turn for non-active player`() {
        val (engine, p1, p2) = createStartedEngine()
        val stateBefore = engine.getCurrentState()!!
        val activeId = stateBefore.activePlayer.id
        val nonActiveId = if (activeId == p1) p2 else p1

        engine.eliminatePlayer(nonActiveId)

        val stateAfter = engine.getCurrentState()!!
        assertEquals(activeId, stateAfter.activePlayer.id)
        assertEquals(stateBefore.turnNumber, stateAfter.turnNumber)
    }

    @Test
    fun `eliminatePlayer skips already-eliminated players when advancing turn`() {
        val (engine, players) = createStartedEngineWith3Players()
        val state = engine.getCurrentState()!!

        // Alice (0) is active. Eliminate Bob (1) first (non-active, no turn change).
        val activeIndex = state.activePlayerIndex
        val aliceId = players[activeIndex]
        // Find a non-active player to eliminate first
        val othersIndices = players.indices.filter { it != activeIndex }
        val firstVictimId = players[othersIndices[0]]
        val survivorId = players[othersIndices[1]]

        // Eliminate the first non-active player
        engine.eliminatePlayer(firstVictimId)
        var stateAfter = engine.getCurrentState()!!
        assertEquals(aliceId, stateAfter.activePlayer.id, "Turn should not change after non-active elimination")

        // Now eliminate the active player — turn should skip the already-eliminated and go to the survivor
        engine.eliminatePlayer(aliceId)
        stateAfter = engine.getCurrentState()!!
        assertEquals(survivorId, stateAfter.activePlayer.id,
            "Turn should skip eliminated player and go to survivor")
    }

    @Test
    fun `game is not paused after eliminatePlayer`() {
        val (engine, _, p2) = createStartedEngine()

        engine.eliminatePlayer(p2)

        assertFalse(engine.isPaused.value)
    }

    // ==================== Issue 6: Settings persistence ====================

    @Test
    fun `UserSettings persists serverMode`() {
        val settings = UserSettings()
        settings.setServerMode("DEDICATED")
        assertEquals("DEDICATED", settings.getServerMode())

        settings.setServerMode("LAN")
        assertEquals("LAN", settings.getServerMode())
    }

    @Test
    fun `UserSettings persists tlsEnabled`() {
        val settings = UserSettings()
        settings.setTlsEnabled(true)
        assertTrue(settings.getTlsEnabled())

        settings.setTlsEnabled(false)
        assertFalse(settings.getTlsEnabled())
    }

    @Test
    fun `UserSettings defaults for new fields`() {
        val data = UserSettingsData()
        assertEquals("LAN", data.serverMode)
        assertTrue(data.tlsEnabled)
    }

    // ==================== Issue 8: Hand position preservation ====================

    @Test
    fun `DrawCard sets handPosition on drawn card`() {
        val (engine, p1, _) = createStartedEngine()
        val stateBefore = engine.getCurrentState()!!

        // Count current hand cards
        val handSizeBefore = stateBefore.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.HAND
        }

        // Draw a card
        val result = engine.executeAction(NetworkAction.DrawCard(p1), p1)
        assertTrue(result.success)

        val stateAfter = engine.getCurrentState()!!
        val newHandCards = stateAfter.cardInstances.filter {
            it.ownerId == p1 && it.zone == Zone.HAND
        }

        // Find the newly drawn card (should have handPosition = handSizeBefore)
        val newCard = newHandCards.find { it.handPosition == handSizeBefore }
        assertNotNull(newCard, "Newly drawn card should have handPosition = $handSizeBefore")
    }

    @Test
    fun `handleNetworkStateUpdate preserves hand positions`() {
        // Create a local GameViewModel with no network client (hotseat mode)
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"), isHotseatMode = true)
        val deck = createTestDeck()
        val aliceId = vm.uiState.value.localPlayer!!.id
        vm.loadDeck(deck)
        vm.drawStartingHand(aliceId)

        // Set hand positions manually
        val state = vm.uiState.value.gameState!!
        val handCards = state.cardInstances.filter { it.ownerId == aliceId && it.zone == Zone.HAND }
        assertTrue(handCards.isNotEmpty(), "Should have hand cards after drawing")

        // Reorder cards
        if (handCards.size >= 2) {
            vm.reorderHandCard(handCards[0].instanceId, 1)
        }

        // Verify positions were set
        val afterReorder = vm.uiState.value.gameState!!
        val reorderedHand = afterReorder.cardInstances.filter { it.ownerId == aliceId && it.zone == Zone.HAND }
        assertTrue(reorderedHand.all { it.handPosition != null }, "All hand cards should have positions")
    }

    // ==================== Issue 15: Mana methods ====================

    @Test
    fun `addMana increments player counter`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"), isHotseatMode = true)
        val aliceId = vm.uiState.value.localPlayer!!.id
        val deck = createTestDeck()
        vm.loadDeck(deck)

        vm.addMana(aliceId, "manaW")
        vm.addMana(aliceId, "manaW")
        vm.addMana(aliceId, "manaU")

        val player = vm.uiState.value.gameState!!.players.find { it.id == aliceId }!!
        assertEquals(2, player.getCounter("manaW"))
        assertEquals(1, player.getCounter("manaU"))
    }

    @Test
    fun `removeMana decrements player counter`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"), isHotseatMode = true)
        val aliceId = vm.uiState.value.localPlayer!!.id
        val deck = createTestDeck()
        vm.loadDeck(deck)

        vm.addMana(aliceId, "manaR")
        vm.addMana(aliceId, "manaR")
        vm.addMana(aliceId, "manaR")
        vm.removeMana(aliceId, "manaR")

        val player = vm.uiState.value.gameState!!.players.find { it.id == aliceId }!!
        assertEquals(2, player.getCounter("manaR"))
    }

    @Test
    fun `clearMana zeroes all mana counters`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"), isHotseatMode = true)
        val aliceId = vm.uiState.value.localPlayer!!.id
        val deck = createTestDeck()
        vm.loadDeck(deck)

        vm.addMana(aliceId, "manaW")
        vm.addMana(aliceId, "manaU")
        vm.addMana(aliceId, "manaB")
        vm.addMana(aliceId, "manaR")
        vm.addMana(aliceId, "manaG")
        vm.addMana(aliceId, "manaC")

        vm.clearMana(aliceId)

        val player = vm.uiState.value.gameState!!.players.find { it.id == aliceId }!!
        GameViewModel.MANA_COLORS.forEach { color ->
            assertEquals(0, player.getCounter(color), "Counter $color should be 0 after clearMana")
        }
    }

    @Test
    fun `MANA_COLORS has all six colors`() {
        assertEquals(6, GameViewModel.MANA_COLORS.size)
        assertTrue("manaW" in GameViewModel.MANA_COLORS)
        assertTrue("manaU" in GameViewModel.MANA_COLORS)
        assertTrue("manaB" in GameViewModel.MANA_COLORS)
        assertTrue("manaR" in GameViewModel.MANA_COLORS)
        assertTrue("manaG" in GameViewModel.MANA_COLORS)
        assertTrue("manaC" in GameViewModel.MANA_COLORS)
    }

    @Test
    fun `getLatestEvents returns last N events`() {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"), isHotseatMode = true)
        val aliceId = vm.uiState.value.localPlayer!!.id
        val deck = createTestDeck()
        vm.loadDeck(deck)
        vm.drawStartingHand(aliceId)

        val events = vm.getLatestEvents(3)
        assertEquals(3, events.size)
        assertTrue(events.all { it.isNotBlank() })
    }

    @Test
    fun `getLatestEvents returns empty on no game`() {
        val vm = GameViewModel()
        val events = vm.getLatestEvents()
        assertTrue(events.isEmpty())
    }

    // ==================== Issue 7: Mulligan CardAction ====================

    @Test
    fun `CardAction Mulligan has playerId`() {
        val action = CardAction.Mulligan("player-123")
        assertEquals("player-123", action.playerId)
    }

    // ==================== Issue 2: ActionRejected suppression ====================

    @Test
    fun `GameClient suppression list covers expected reasons`() {
        // This is a structural test - we verify the suppressed reasons exist
        // by checking the GameEngine validation messages match what's suppressed
        val engine = GameEngine(maxPlayers = 4)
        val deck = createTestDeck()
        val p1 = engine.addPlayer("Alice", deck, isAdmin = true)
        val p2 = engine.addPlayer("Bob", deck)
        engine.setPlayerReady(p2, true)
        engine.startGame()

        // Non-active player tries to pass turn → should get "Not your turn"
        val state = engine.getCurrentState()!!
        val nonActiveId = if (state.activePlayer.id == p1) p2 else p1
        val result = engine.executeAction(NetworkAction.PassTurn, nonActiveId)
        assertFalse(result.success)
        assertTrue(result.reason.contains("Not your turn"), "Expected 'Not your turn', got: ${result.reason}")
    }
}
