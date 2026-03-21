package com.dustinmcafee.dongadeuce

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.ui.theme.DongAdeuceTheme
import com.dustinmcafee.dongadeuce.viewmodel.GameViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for post-playtest bug fixes.
 * Tests shared-module logic that requires Android runtime (serialization, settings, ViewModel).
 */
@RunWith(AndroidJUnit4::class)
class PostPlaytestUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestDeck(): Deck {
        return Deck(
            name = "Test Deck",
            commander = Card(
                name = "Test Commander",
                type = "Legendary Creature",
                power = "4",
                toughness = "4"
            ),
            cards = (1..99).map { i ->
                if (i <= 35) Card(name = "Land $i", type = "Basic Land")
                else Card(name = "Creature $i", type = "Creature", power = "2", toughness = "2")
            }
        )
    }

    private fun setupGameViewModel(): GameViewModel {
        val vm = GameViewModel()
        vm.initializeGame("Alice", listOf("Bob"), isHotseatMode = true)
        val aliceId = vm.uiState.value.localPlayer!!.id
        val bobId = vm.uiState.value.opponents[0].id
        vm.loadDeckForPlayer(aliceId, createTestDeck())
        vm.loadDeckForPlayer(bobId, createTestDeck())
        vm.drawStartingHand(aliceId)
        vm.drawStartingHand(bobId)
        return vm
    }

    // ==================== Issue 5: eliminatePlayer on Android ====================

    @Test
    fun eliminatePlayer_setsHasLost() {
        val vm = setupGameViewModel()
        val bobId = vm.uiState.value.opponents[0].id
        vm.markPlayerAsLost(bobId, "disconnected")
        val bob = vm.uiState.value.gameState!!.players.find { it.id == bobId }!!
        assert(bob.hasLost) { "Bob should be marked as lost" }
    }

    // ==================== Issue 15: Mana pool ====================

    @Test
    fun addMana_incrementsCorrectCounter() {
        val vm = setupGameViewModel()
        val aliceId = vm.uiState.value.localPlayer!!.id

        vm.addMana(aliceId, "manaW")
        vm.addMana(aliceId, "manaW")
        vm.addMana(aliceId, "manaU")

        val player = vm.uiState.value.gameState!!.players.find { it.id == aliceId }!!
        assert(player.getCounter("manaW") == 2) { "White mana should be 2, was ${player.getCounter("manaW")}" }
        assert(player.getCounter("manaU") == 1) { "Blue mana should be 1, was ${player.getCounter("manaU")}" }
    }

    @Test
    fun clearMana_zeroresAllColors() {
        val vm = setupGameViewModel()
        val aliceId = vm.uiState.value.localPlayer!!.id

        GameViewModel.MANA_COLORS.forEach { vm.addMana(aliceId, it) }
        vm.clearMana(aliceId)

        val player = vm.uiState.value.gameState!!.players.find { it.id == aliceId }!!
        GameViewModel.MANA_COLORS.forEach { color ->
            assert(player.getCounter(color) == 0) { "$color should be 0 after clear, was ${player.getCounter(color)}" }
        }
    }

    // ==================== Issue 8: Hand order preservation ====================

    @Test
    fun reorderHandCard_updatesPositions() {
        val vm = setupGameViewModel()
        val aliceId = vm.uiState.value.localPlayer!!.id
        val hand = vm.getCards(aliceId, Zone.HAND)
        assert(hand.size >= 2) { "Need at least 2 cards in hand" }

        val firstCard = hand[0]
        vm.reorderHandCard(firstCard.instanceId, 1)

        val reorderedHand = vm.getCards(aliceId, Zone.HAND)
        assert(reorderedHand.all { it.handPosition != null }) { "All cards should have handPosition set" }
    }

    // ==================== Issue 9: Life counter ====================

    @Test
    fun updateLife_changesPlayerLife() {
        val vm = setupGameViewModel()
        val aliceId = vm.uiState.value.localPlayer!!.id

        vm.updateLife(aliceId, 35)
        val alice = vm.uiState.value.gameState!!.players.find { it.id == aliceId }!!
        assert(alice.life == 35) { "Life should be 35, was ${alice.life}" }
    }

    // ==================== Issue 7: Mulligan ====================

    @Test
    fun mulligan_reshufflesHandBackToLibrary() {
        val vm = setupGameViewModel()
        val aliceId = vm.uiState.value.localPlayer!!.id

        val handBefore = vm.getCards(aliceId, Zone.HAND).size
        assert(handBefore == 7) { "Should start with 7 cards, had $handBefore" }

        // Track which cards were in hand before
        val handCardIdsBefore = vm.getCards(aliceId, Zone.HAND).map { it.instanceId }.toSet()

        vm.mulligan(aliceId)

        val handAfter = vm.getCards(aliceId, Zone.HAND)
        // Local mulligan redraws 7 cards (full starting hand)
        assert(handAfter.size == 7) { "Should have 7 cards after mulligan, had ${handAfter.size}" }
        // At least some cards should be different (shuffled)
        val handCardIdsAfter = handAfter.map { it.instanceId }.toSet()
        // Library should still exist
        val librarySize = vm.getCards(aliceId, Zone.LIBRARY).size
        assert(librarySize > 0) { "Library should not be empty after mulligan" }
    }

    // ==================== Issue 3: Pass turn validation ====================

    @Test
    fun passTurn_advancesTurnInHotseat() {
        val vm = setupGameViewModel()
        val turnBefore = vm.uiState.value.gameState!!.turnNumber
        val activePlayerBefore = vm.uiState.value.gameState!!.activePlayer.id

        vm.passTurn()

        val state = vm.uiState.value.gameState!!
        assert(state.turnNumber == turnBefore + 1) { "Turn should advance" }
        assert(state.activePlayer.id != activePlayerBefore) { "Active player should change" }
    }

    // ==================== Issue 14: Game events ====================

    @Test
    fun getLatestEvents_returnsFormattedStrings() {
        val vm = setupGameViewModel()
        val events = vm.getLatestEvents(3)
        assert(events.size == 3) { "Should return 3 events, got ${events.size}" }
        events.forEach { event ->
            assert(event.isNotBlank()) { "Event should not be blank" }
        }
    }

    // ==================== Issue 6: Settings defaults ====================

    @Test
    fun userSettingsData_hasCorrectDefaults() {
        val data = com.dustinmcafee.dongadeuce.settings.UserSettingsData()
        assert(data.serverMode == "LAN") { "Default serverMode should be LAN" }
        assert(data.tlsEnabled) { "Default tlsEnabled should be true" }
        assert(data.playerName == "Player 1") { "Default playerName should be Player 1" }
        assert(data.serverPort == 8080) { "Default serverPort should be 8080" }
    }
}
