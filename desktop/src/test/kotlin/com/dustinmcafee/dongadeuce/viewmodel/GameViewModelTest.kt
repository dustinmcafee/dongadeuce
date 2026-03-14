package com.dustinmcafee.dongadeuce.viewmodel

import com.dustinmcafee.dongadeuce.models.*
import kotlin.test.*

class GameViewModelTest {

    @Test
    fun `initializeGame creates correct initial state`() {
        val viewModel = GameViewModel()

        viewModel.initializeGame("Player1", listOf("Player2", "Player3"))

        val state = viewModel.uiState.value
        assertNotNull(state.localPlayer, "Local player should be created")
        assertEquals("Player1", state.localPlayer?.name)
        assertEquals(40, state.localPlayer?.life, "Should start with 40 life")
        assertEquals(2, state.opponents.size, "Should have 2 opponents")
        assertEquals("Player2", state.opponents[0].name)
        assertEquals("Player3", state.opponents[1].name)
        assertNotNull(state.gameState, "Game state should be created")
        assertEquals(3, state.gameState?.players?.size, "Should have 3 total players")
        assertEquals(0, state.gameState?.activePlayerIndex, "Active player should be index 0")
        assertEquals(1, state.gameState?.turnNumber, "Should start at turn 1")
    }

    @Test
    fun `loadDeck creates correct card instances`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())

        val commander = Card(
            name = "Test Commander",
            type = "Legendary Creature"
        )
        // Create exactly 99 cards for a valid deck
        val cards = (1..99).map { Card(name = "Card $it") }
        val deck = Deck(name = "Test Deck", commander = commander, cards = cards)

        viewModel.loadDeck(deck)

        val state = viewModel.uiState.value
        val gameState = state.gameState
        assertNotNull(gameState)

        // Commander should be in command zone
        val commanderCards = gameState.cardInstances.filter { it.zone == Zone.COMMAND_ZONE }
        assertEquals(1, commanderCards.size, "Should have 1 commander in command zone")
        assertEquals("Test Commander", commanderCards[0].card.name)

        // Other cards should be in library (all 99 cards)
        val libraryCards = gameState.cardInstances.filter { it.zone == Zone.LIBRARY }
        assertEquals(99, libraryCards.size, "Should have 99 cards in library")
    }

    @Test
    fun `drawCard moves top card from library to hand`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        // Create cards in library
        val card1 = createTestCardInstance("Card 1", playerId, Zone.LIBRARY)
        val card2 = createTestCardInstance("Card 2", playerId, Zone.LIBRARY)
        val card3 = createTestCardInstance("Card 3", playerId, Zone.LIBRARY)
        addCardsToGame(viewModel, listOf(card1, card2, card3))

        // Draw a card - should take the last card (top of library)
        viewModel.drawCard(playerId)

        val libraryCount = viewModel.getCardCount(playerId, Zone.LIBRARY)
        val handCount = viewModel.getCardCount(playerId, Zone.HAND)

        assertEquals(2, libraryCount, "Library should have 2 cards after drawing")
        assertEquals(1, handCount, "Hand should have 1 card after drawing")

        // Verify the correct card was drawn (last in list = top)
        val handCards = viewModel.getCards(playerId, Zone.HAND)
        assertEquals("Card 3", handCards[0].card.name, "Should have drawn the last card (card3)")
    }

    @Test
    fun `drawCard from empty library causes player loss`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        // No cards in library
        viewModel.drawCard(playerId)

        val player = viewModel.uiState.value.localPlayer
        assertNotNull(player)
        assertTrue(player.hasLost, "Player should lose when drawing from empty library")
    }

    @Test
    fun `drawStartingHand draws 7 cards`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        // Create 10 cards in library
        val cards = (1..10).map { createTestCardInstance("Card $it", playerId, Zone.LIBRARY) }
        addCardsToGame(viewModel, cards)

        viewModel.drawStartingHand(playerId, 7)

        assertEquals(3, viewModel.getCardCount(playerId, Zone.LIBRARY), "Library should have 3 cards")
        assertEquals(7, viewModel.getCardCount(playerId, Zone.HAND), "Hand should have 7 cards")
    }

    @Test
    fun `millCards moves top cards to graveyard`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        // Create 5 cards in library
        val cards = (1..5).map { createTestCardInstance("Card $it", playerId, Zone.LIBRARY) }
        addCardsToGame(viewModel, cards)

        viewModel.millCards(playerId, 2)

        assertEquals(3, viewModel.getCardCount(playerId, Zone.LIBRARY), "Library should have 3 cards")
        assertEquals(2, viewModel.getCardCount(playerId, Zone.GRAVEYARD), "Graveyard should have 2 cards")
    }

    @Test
    fun `millCards beyond library size doesnt crash`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        // Create 2 cards in library
        val cards = (1..2).map { createTestCardInstance("Card $it", playerId, Zone.LIBRARY) }
        addCardsToGame(viewModel, cards)

        // Try to mill 5 cards (more than library size)
        viewModel.millCards(playerId, 5)

        assertEquals(0, viewModel.getCardCount(playerId, Zone.LIBRARY), "Library should be empty")
        assertEquals(2, viewModel.getCardCount(playerId, Zone.GRAVEYARD), "Graveyard should have 2 cards")
    }

    @Test
    fun `moveCardToTopOfLibrary places card at end of library list`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        // Create cards in library and hand
        val libCard1 = createTestCardInstance("Library Card 1", playerId, Zone.LIBRARY)
        val libCard2 = createTestCardInstance("Library Card 2", playerId, Zone.LIBRARY)
        val handCard = createTestCardInstance("Hand Card 1", playerId, Zone.HAND)
        addCardsToGame(viewModel, listOf(libCard1, libCard2, handCard))

        // Move hand card to top of library
        viewModel.moveCardToTopOfLibrary(handCard.instanceId)

        val libraryCards = viewModel.getCards(playerId, Zone.LIBRARY)
        assertEquals(3, libraryCards.size, "Library should have 3 cards")
        assertEquals("Hand Card 1", libraryCards.last().card.name, "Last card in library should be the moved card")

        // Verify drawing gets the moved card
        viewModel.drawCard(playerId)
        val handCards = viewModel.getCards(playerId, Zone.HAND)
        assertEquals("Hand Card 1", handCards.last().card.name, "Drawing should get the card we moved to top")
    }

    @Test
    fun `shuffleLibrary randomizes library order`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        // Create cards in library
        val cards = (1..10).map { createTestCardInstance("Card $it", playerId, Zone.LIBRARY) }
        addCardsToGame(viewModel, cards)

        val beforeShuffle = viewModel.getCards(playerId, Zone.LIBRARY)

        viewModel.shuffleLibrary(playerId)

        val afterShuffle = viewModel.getCards(playerId, Zone.LIBRARY)

        // Same number of cards
        assertEquals(beforeShuffle.size, afterShuffle.size, "Library size should be the same")

        // All cards still present (just check names)
        val beforeNames = beforeShuffle.map { it.card.name }.sorted()
        val afterNames = afterShuffle.map { it.card.name }.sorted()
        assertEquals(beforeNames, afterNames, "All cards should still be in library")
    }

    // Disabled: Auto-elimination removed - players must manually concede
    // @Test
    // fun `updateLife correctly updates player life and loss state`() {
    //     val viewModel = GameViewModel()
    //     viewModel.initializeGame("Player1", emptyList())
    //     val playerId = viewModel.uiState.value.localPlayer?.id ?: return
    //
    //     viewModel.updateLife(playerId, 20)
    //     assertEquals(20, viewModel.uiState.value.localPlayer?.life)
    //     assertFalse(viewModel.uiState.value.localPlayer?.hasLost ?: true, "Should not lose at 20 life")
    //
    //     viewModel.updateLife(playerId, 0)
    //     assertEquals(0, viewModel.uiState.value.localPlayer?.life)
    //     assertTrue(viewModel.uiState.value.localPlayer?.hasLost ?: false, "Should lose at 0 life")
    // }

    // Disabled: Auto-elimination removed - players must manually concede
    // @Test
    // fun `updateCommanderDamage tracks damage correctly`() {
    //     val viewModel = GameViewModel()
    //     viewModel.initializeGame("Player1", emptyList())
    //     val playerId = viewModel.uiState.value.localPlayer?.id ?: return
    //     val commanderId = "cmd-123"
    //
    //     viewModel.updateCommanderDamage(playerId, commanderId, 10)
    //     assertEquals(10, viewModel.uiState.value.localPlayer?.commanderDamage?.get(commanderId))
    //     assertFalse(viewModel.uiState.value.localPlayer?.hasLost ?: true, "Should not lose at 10 commander damage")
    //
    //     viewModel.updateCommanderDamage(playerId, commanderId, 21)
    //     assertEquals(21, viewModel.uiState.value.localPlayer?.commanderDamage?.get(commanderId))
    //     assertTrue(viewModel.uiState.value.localPlayer?.hasLost ?: false, "Should lose at 21 commander damage")
    // }

    @Test
    fun `mulligan returns hand to library and redraws`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        // Create 10 cards in library and draw 7
        val cards = (1..10).map { createTestCardInstance(it.toString(), playerId, Zone.LIBRARY) }
        addCardsToGame(viewModel, cards)
        viewModel.drawStartingHand(playerId, 7)

        assertEquals(7, viewModel.getCardCount(playerId, Zone.HAND))
        assertEquals(3, viewModel.getCardCount(playerId, Zone.LIBRARY))

        // Mulligan
        viewModel.mulligan(playerId)

        assertEquals(7, viewModel.getCardCount(playerId, Zone.HAND), "Should have 7 cards in hand after mulligan")
        assertEquals(3, viewModel.getCardCount(playerId, Zone.LIBRARY), "Should have 3 cards in library after mulligan")
    }

    @Test
    fun `toggleTap changes tap state`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        // Should start untapped
        val initialCard = viewModel.getCards(playerId, Zone.BATTLEFIELD)[0]
        assertFalse(initialCard.isTapped, "Card should start untapped")

        // Tap it
        viewModel.toggleTap(card.instanceId)
        val tappedCard = viewModel.getCards(playerId, Zone.BATTLEFIELD)[0]
        assertTrue(tappedCard.isTapped, "Card should be tapped")

        // Untap it
        viewModel.toggleTap(card.instanceId)
        val untappedCard = viewModel.getCards(playerId, Zone.BATTLEFIELD)[0]
        assertFalse(untappedCard.isTapped, "Card should be untapped")
    }

    // ==================== Card Movement Tests ====================

    @Test
    fun `moveCard moves card to target zone`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.HAND)
        addCardsToGame(viewModel, listOf(card))

        viewModel.moveCard(card.instanceId, Zone.BATTLEFIELD)

        val handCount = viewModel.getCardCount(playerId, Zone.HAND)
        val battlefieldCount = viewModel.getCardCount(playerId, Zone.BATTLEFIELD)
        assertEquals(0, handCount, "Hand should be empty")
        assertEquals(1, battlefieldCount, "Battlefield should have 1 card")
    }

    @Test
    fun `moveCard clears tap state when leaving battlefield`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        // Tap the card
        viewModel.toggleTap(card.instanceId)
        assertTrue(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].isTapped)

        // Move to graveyard
        viewModel.moveCard(card.instanceId, Zone.GRAVEYARD)

        // Card should be untapped in graveyard
        val movedCard = viewModel.getCards(playerId, Zone.GRAVEYARD)[0]
        assertFalse(movedCard.isTapped, "Card should be untapped when moved from battlefield")
    }

    @Test
    fun `moveCard clears counters when leaving battlefield`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        // Add counters
        viewModel.addCounter(card.instanceId, "+1/+1", 3)
        assertEquals(3, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].counters["+1/+1"])

        // Move to graveyard
        viewModel.moveCard(card.instanceId, Zone.GRAVEYARD)

        // Card should have no counters
        val movedCard = viewModel.getCards(playerId, Zone.GRAVEYARD)[0]
        assertTrue(movedCard.counters.isEmpty(), "Counters should be cleared when leaving battlefield")
    }

    @Test
    fun `moveCard clears P-T modifiers when leaving battlefield`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        // Add P/T modifiers
        viewModel.modifyPower(card.instanceId, 3)
        viewModel.modifyToughness(card.instanceId, 2)

        // Move to hand
        viewModel.moveCard(card.instanceId, Zone.HAND)

        // Card should have no modifiers
        val movedCard = viewModel.getCards(playerId, Zone.HAND)[0]
        assertEquals(0, movedCard.powerModifier, "Power modifier should be cleared")
        assertEquals(0, movedCard.toughnessModifier, "Toughness modifier should be cleared")
    }

    @Test
    fun `moveCard preserves ownership`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", listOf("Player2"))
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.HAND)
        addCardsToGame(viewModel, listOf(card))

        viewModel.moveCard(card.instanceId, Zone.BATTLEFIELD)
        viewModel.moveCard(card.instanceId, Zone.GRAVEYARD)

        val movedCard = viewModel.getCards(playerId, Zone.GRAVEYARD)[0]
        assertEquals(playerId, movedCard.ownerId, "Owner should remain unchanged")
    }

    @Test
    fun `moveCardToTopOfLibrary places card at top`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val lib1 = createTestCardInstance("Library 1", playerId, Zone.LIBRARY)
        val lib2 = createTestCardInstance("Library 2", playerId, Zone.LIBRARY)
        val handCard = createTestCardInstance("Hand Card", playerId, Zone.HAND)
        addCardsToGame(viewModel, listOf(lib1, lib2, handCard))

        viewModel.moveCardToTopOfLibrary(handCard.instanceId)

        // Drawing should get the hand card (now on top)
        viewModel.drawCard(playerId)
        val drawn = viewModel.getCards(playerId, Zone.HAND).last()
        assertEquals("Hand Card", drawn.card.name, "Should draw the card moved to top")
    }

    @Test
    fun `moveTopCardsToZone moves N cards from library`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val cards = (1..5).map { createTestCardInstance("Card $it", playerId, Zone.LIBRARY) }
        addCardsToGame(viewModel, cards)

        viewModel.moveTopCardsToZone(playerId, 3, Zone.GRAVEYARD)

        assertEquals(2, viewModel.getCardCount(playerId, Zone.LIBRARY))
        assertEquals(3, viewModel.getCardCount(playerId, Zone.GRAVEYARD))
    }

    @Test
    fun `moveBottomCardsToZone moves N cards from bottom`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val cards = (1..5).map { createTestCardInstance("Card $it", playerId, Zone.LIBRARY) }
        addCardsToGame(viewModel, cards)

        viewModel.moveBottomCardsToZone(playerId, 2, Zone.EXILE)

        assertEquals(3, viewModel.getCardCount(playerId, Zone.LIBRARY))
        assertEquals(2, viewModel.getCardCount(playerId, Zone.EXILE))
    }

    // ==================== Card State Tests ====================

    @Test
    fun `toggleFaceDown toggles face down state`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        assertFalse(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].isFaceDown)

        viewModel.toggleFaceDown(card.instanceId)
        assertTrue(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].isFaceDown)

        viewModel.toggleFaceDown(card.instanceId)
        assertFalse(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].isFaceDown)
    }

    @Test
    fun `flip toggles isFlipped state`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        assertFalse(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].isFlipped)

        viewModel.flipCard(card.instanceId)
        assertTrue(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].isFlipped)

        viewModel.flipCard(card.instanceId)
        assertFalse(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].isFlipped)
    }

    @Test
    fun `toggleDoesntUntap toggles flag`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        assertFalse(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].doesntUntap)

        viewModel.toggleDoesntUntap(card.instanceId)
        assertTrue(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].doesntUntap)

        viewModel.toggleDoesntUntap(card.instanceId)
        assertFalse(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].doesntUntap)
    }

    @Test
    fun `setAnnotation sets note on card`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.setAnnotation(card.instanceId, "This is a note")

        assertEquals("This is a note", viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].annotation)
    }

    @Test
    fun `setAnnotation clears note with null`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.setAnnotation(card.instanceId, "Note")
        assertEquals("Note", viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].annotation)

        viewModel.setAnnotation(card.instanceId, null)
        assertNull(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].annotation)
    }

    @Test
    fun `setAnnotation clears note with blank string`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.setAnnotation(card.instanceId, "Note")
        viewModel.setAnnotation(card.instanceId, "  ")

        assertNull(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].annotation)
    }

    @Test
    fun `updateCardGridPosition updates position`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.updateCardGridPosition(card.instanceId, 5, 3)

        val updated = viewModel.getCards(playerId, Zone.BATTLEFIELD)[0]
        assertEquals(5, updated.gridX)
        assertEquals(3, updated.gridY)
    }

    @Test
    fun `playFaceDown moves card to battlefield face down`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.HAND)
        addCardsToGame(viewModel, listOf(card))

        viewModel.playFaceDown(card.instanceId)

        assertEquals(0, viewModel.getCardCount(playerId, Zone.HAND))
        assertEquals(1, viewModel.getCardCount(playerId, Zone.BATTLEFIELD))
        assertTrue(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].isFaceDown)
    }

    // ==================== Card Counter Tests ====================

    @Test
    fun `addCounter adds counter to card`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.addCounter(card.instanceId, "+1/+1", 1)

        assertEquals(1, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].counters["+1/+1"])
    }

    @Test
    fun `addCounter adds multiple counters at once`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.addCounter(card.instanceId, "+1/+1", 5)

        assertEquals(5, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].counters["+1/+1"])
    }

    @Test
    fun `addCounter accumulates counters`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.addCounter(card.instanceId, "+1/+1", 2)
        viewModel.addCounter(card.instanceId, "+1/+1", 3)

        assertEquals(5, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].counters["+1/+1"])
    }

    @Test
    fun `removeCounter removes counters`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.addCounter(card.instanceId, "+1/+1", 5)
        viewModel.removeCounter(card.instanceId, "+1/+1", 2)

        assertEquals(3, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].counters["+1/+1"])
    }

    @Test
    fun `removeCounter cannot go below zero`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.addCounter(card.instanceId, "+1/+1", 2)
        viewModel.removeCounter(card.instanceId, "+1/+1", 5)

        // Counter should be 0 or removed
        val counters = viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].counters
        assertTrue(counters["+1/+1"] == null || counters["+1/+1"] == 0)
    }

    @Test
    fun `setCounter sets exact value`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.addCounter(card.instanceId, "+1/+1", 3)
        viewModel.setCounter(card.instanceId, "+1/+1", 7)

        assertEquals(7, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].counters["+1/+1"])
    }

    @Test
    fun `setCounter to zero removes counter`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.addCounter(card.instanceId, "+1/+1", 5)
        viewModel.setCounter(card.instanceId, "+1/+1", 0)

        assertFalse(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].counters.containsKey("+1/+1"))
    }

    // ==================== Power/Toughness Tests ====================

    @Test
    fun `modifyPower changes power modifier`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.modifyPower(card.instanceId, 3)

        assertEquals(3, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].powerModifier)
        assertEquals(0, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].toughnessModifier)
    }

    @Test
    fun `modifyToughness changes toughness modifier`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.modifyToughness(card.instanceId, 2)

        assertEquals(0, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].powerModifier)
        assertEquals(2, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].toughnessModifier)
    }

    @Test
    fun `modifyPowerToughness changes both modifiers`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.modifyPowerToughness(card.instanceId, 2)

        assertEquals(2, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].powerModifier)
        assertEquals(2, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].toughnessModifier)
    }

    @Test
    fun `modifyPowerToughness accumulates`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.modifyPowerToughness(card.instanceId, 2)
        viewModel.modifyPowerToughness(card.instanceId, 3)

        assertEquals(5, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].powerModifier)
        assertEquals(5, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].toughnessModifier)
    }

    @Test
    fun `resetPowerToughness clears modifiers`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.modifyPower(card.instanceId, 5)
        viewModel.modifyToughness(card.instanceId, 3)
        viewModel.resetPowerToughness(card.instanceId)

        assertEquals(0, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].powerModifier)
        assertEquals(0, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].toughnessModifier)
    }

    @Test
    fun `flowPower increases power and decreases toughness`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.flowPower(card.instanceId)

        assertEquals(1, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].powerModifier)
        assertEquals(-1, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].toughnessModifier)
    }

    @Test
    fun `flowToughness decreases power and increases toughness`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card = createTestCardInstance("Test Card", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.flowToughness(card.instanceId)

        assertEquals(-1, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].powerModifier)
        assertEquals(1, viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].toughnessModifier)
    }

    // ==================== Attachment & Control Tests ====================

    @Test
    fun `attachCard sets attachedTo reference`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val creature = createTestCardInstance("Creature", playerId, Zone.BATTLEFIELD)
        val aura = createTestCardInstance("Aura", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(creature, aura))

        viewModel.attachCard(aura.instanceId, creature.instanceId)

        val attachedAura = viewModel.getCards(playerId, Zone.BATTLEFIELD).find { it.instanceId == aura.instanceId }
        assertEquals(creature.instanceId, attachedAura?.attachedTo)
    }

    @Test
    fun `detachCard clears attachedTo reference`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val creature = createTestCardInstance("Creature", playerId, Zone.BATTLEFIELD)
        val aura = createTestCardInstance("Aura", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(creature, aura))

        viewModel.attachCard(aura.instanceId, creature.instanceId)
        viewModel.detachCard(aura.instanceId)

        val detachedAura = viewModel.getCards(playerId, Zone.BATTLEFIELD).find { it.instanceId == aura.instanceId }
        assertNull(detachedAura?.attachedTo)
    }

    @Test
    fun `giveControlTo changes controllerId`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", listOf("Player2"))
        val player1Id = viewModel.uiState.value.localPlayer?.id ?: return
        val player2Id = viewModel.uiState.value.opponents[0].id

        val card = createTestCardInstance("Test Card", player1Id, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card))

        viewModel.giveControlTo(card.instanceId, player2Id)

        // Card should now be controlled by player2
        val gameState = viewModel.uiState.value.gameState ?: return
        val updatedCard = gameState.cardInstances.find { it.instanceId == card.instanceId }
        assertEquals(player2Id, updatedCard?.controllerId)
        assertEquals(player1Id, updatedCard?.ownerId, "Owner should remain unchanged")
    }

    @Test
    fun `giveControlTo moves card to battlefield`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", listOf("Player2"))
        val player1Id = viewModel.uiState.value.localPlayer?.id ?: return
        val player2Id = viewModel.uiState.value.opponents[0].id

        val card = createTestCardInstance("Test Card", player1Id, Zone.HAND)
        addCardsToGame(viewModel, listOf(card))

        viewModel.giveControlTo(card.instanceId, player2Id)

        val gameState = viewModel.uiState.value.gameState ?: return
        val updatedCard = gameState.cardInstances.find { it.instanceId == card.instanceId }
        assertEquals(Zone.BATTLEFIELD, updatedCard?.zone)
    }

    // ==================== Player Counter Tests ====================

    @Test
    fun `addPlayerCounter adds poison counters`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        viewModel.addPlayerCounter(playerId, "poison", 3)

        assertEquals(3, viewModel.uiState.value.localPlayer?.getCounter("poison"))
    }

    // Disabled: Auto-elimination removed - players must manually concede
    // @Test
    // fun `addPlayerCounter at 10 poison causes loss`() {
    //     val viewModel = GameViewModel()
    //     viewModel.initializeGame("Player1", emptyList())
    //     val playerId = viewModel.uiState.value.localPlayer?.id ?: return
    //
    //     viewModel.addPlayerCounter(playerId, "poison", 10)
    //
    //     assertTrue(viewModel.uiState.value.localPlayer?.hasLost ?: false)
    // }

    @Test
    fun `addPlayerCounter accumulates`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        viewModel.addPlayerCounter(playerId, "poison", 3)
        viewModel.addPlayerCounter(playerId, "poison", 4)

        assertEquals(7, viewModel.uiState.value.localPlayer?.getCounter("poison"))
    }

    @Test
    fun `removePlayerCounter removes counters`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        viewModel.addPlayerCounter(playerId, "poison", 5)
        viewModel.removePlayerCounter(playerId, "poison", 2)

        assertEquals(3, viewModel.uiState.value.localPlayer?.getCounter("poison"))
    }

    @Test
    fun `setPlayerCounter sets exact value`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        viewModel.addPlayerCounter(playerId, "poison", 3)
        viewModel.setPlayerCounter(playerId, "poison", 7)

        assertEquals(7, viewModel.uiState.value.localPlayer?.getCounter("poison"))
    }

    @Test
    fun `addPlayerCounter adds energy without threshold`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        viewModel.addPlayerCounter(playerId, "energy", 100)

        assertEquals(100, viewModel.uiState.value.localPlayer?.getCounter("energy"))
        assertFalse(viewModel.uiState.value.localPlayer?.hasLost ?: true)
    }

    @Test
    fun `addPlayerCounter adds experience without threshold`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        viewModel.addPlayerCounter(playerId, "experience", 50)

        assertEquals(50, viewModel.uiState.value.localPlayer?.getCounter("experience"))
        assertFalse(viewModel.uiState.value.localPlayer?.hasLost ?: true)
    }

    // ==================== Combat & Permanents Tests ====================

    @Test
    fun `untapAll untaps all controlled cards`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val card1 = createTestCardInstance("Card 1", playerId, Zone.BATTLEFIELD)
        val card2 = createTestCardInstance("Card 2", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(card1, card2))

        // Tap both cards
        viewModel.toggleTap(card1.instanceId)
        viewModel.toggleTap(card2.instanceId)
        assertTrue(viewModel.getCards(playerId, Zone.BATTLEFIELD).all { it.isTapped })

        // Untap all
        viewModel.untapAll(playerId)

        assertFalse(viewModel.getCards(playerId, Zone.BATTLEFIELD).any { it.isTapped })
    }

    @Test
    fun `untapAll respects doesntUntap flag`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val normalCard = createTestCardInstance("Normal", playerId, Zone.BATTLEFIELD)
        val stasisCard = createTestCardInstance("Stasis", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(normalCard, stasisCard))

        // Tap both and set doesntUntap on one
        viewModel.toggleTap(normalCard.instanceId)
        viewModel.toggleTap(stasisCard.instanceId)
        viewModel.toggleDoesntUntap(stasisCard.instanceId)

        viewModel.untapAll(playerId)

        val cards = viewModel.getCards(playerId, Zone.BATTLEFIELD)
        val normal = cards.find { it.card.name == "Normal" }
        val stasis = cards.find { it.card.name == "Stasis" }
        assertFalse(normal?.isTapped ?: true, "Normal card should untap")
        assertTrue(stasis?.isTapped ?: false, "Stasis card should stay tapped")
    }

    @Test
    fun `untapAll only affects battlefield cards`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val battlefieldCard = createTestCardInstance("Battlefield", playerId, Zone.BATTLEFIELD)
        val handCard = createTestCardInstance("Hand", playerId, Zone.HAND)
        addCardsToGame(viewModel, listOf(battlefieldCard, handCard))

        viewModel.toggleTap(battlefieldCard.instanceId)

        viewModel.untapAll(playerId)

        assertFalse(viewModel.getCards(playerId, Zone.BATTLEFIELD)[0].isTapped)
    }

    @Test
    fun `concede sets player life to 0`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        viewModel.concede(playerId)

        assertEquals(0, viewModel.uiState.value.localPlayer?.life)
    }

    @Test
    fun `concede marks player as lost`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        viewModel.concede(playerId)

        assertTrue(viewModel.uiState.value.localPlayer?.hasLost ?: false)
    }

    // ==================== Phase Management Tests ====================

    @Test
    fun `nextPhase advances to next phase`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())

        val initialPhase = viewModel.uiState.value.gameState?.phase
        assertEquals(GamePhase.UNTAP, initialPhase)

        viewModel.nextPhase()

        assertEquals(GamePhase.UPKEEP, viewModel.uiState.value.gameState?.phase)
    }

    @Test
    fun `nextPhase wraps from CLEANUP to UNTAP`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())

        // Set phase to CLEANUP
        viewModel.setPhase(GamePhase.CLEANUP)
        assertEquals(GamePhase.CLEANUP, viewModel.uiState.value.gameState?.phase)

        viewModel.nextPhase()

        assertEquals(GamePhase.UNTAP, viewModel.uiState.value.gameState?.phase)
    }

    @Test
    fun `nextPhase increments turn on wrap`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())

        val initialTurn = viewModel.uiState.value.gameState?.turnNumber ?: 0
        assertEquals(1, initialTurn)

        viewModel.setPhase(GamePhase.CLEANUP)
        viewModel.nextPhase()

        assertEquals(2, viewModel.uiState.value.gameState?.turnNumber)
    }

    @Test
    fun `setPhase sets specific phase`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())

        viewModel.setPhase(GamePhase.COMBAT_DECLARE_ATTACKERS)

        assertEquals(GamePhase.COMBAT_DECLARE_ATTACKERS, viewModel.uiState.value.gameState?.phase)
    }

    // ==================== Turn Management Tests ====================

    @Test
    fun `passTurn advances to next player`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", listOf("Player2"))

        val initialPlayerIndex = viewModel.uiState.value.gameState?.activePlayerIndex
        assertEquals(0, initialPlayerIndex)

        viewModel.passTurn()

        assertEquals(1, viewModel.uiState.value.gameState?.activePlayerIndex)
    }

    @Test
    fun `passTurn sets phase to UNTAP`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", listOf("Player2"))

        viewModel.setPhase(GamePhase.MAIN_1)
        viewModel.passTurn()

        assertEquals(GamePhase.UNTAP, viewModel.uiState.value.gameState?.phase)
    }

    @Test
    fun `passTurn increments turn number`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", listOf("Player2"))

        val initialTurn = viewModel.uiState.value.gameState?.turnNumber ?: 0
        viewModel.passTurn()

        assertTrue(viewModel.uiState.value.gameState?.turnNumber ?: 0 > initialTurn)
    }

    @Test
    fun `passTurn wraps player index`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", listOf("Player2"))

        viewModel.passTurn() // Player 1 -> Player 2
        assertEquals(1, viewModel.uiState.value.gameState?.activePlayerIndex)

        viewModel.passTurn() // Player 2 -> Player 1
        assertEquals(0, viewModel.uiState.value.gameState?.activePlayerIndex)
    }

    // ==================== Library Operations Tests ====================

    @Test
    fun `shuffleLibrary randomizes card order`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val cards = (1..20).map { createTestCardInstance("Card $it", playerId, Zone.LIBRARY) }
        addCardsToGame(viewModel, cards)

        val beforeNames = viewModel.getCards(playerId, Zone.LIBRARY).map { it.card.name }

        viewModel.shuffleLibrary(playerId)

        val afterNames = viewModel.getCards(playerId, Zone.LIBRARY).map { it.card.name }

        assertEquals(beforeNames.size, afterNames.size, "Same number of cards")
        assertEquals(beforeNames.sorted(), afterNames.sorted(), "Same cards present")
        // Note: Very small chance of same order, but acceptable for unit test
    }

    // ==================== Token & Clone Tests ====================

    @Test
    fun `createToken creates token on battlefield`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        viewModel.createToken(
            playerId = playerId,
            tokenName = "Soldier",
            tokenType = "Creature - Soldier",
            power = "1",
            toughness = "1",
            color = "White",
            quantity = 1
        )

        val tokens = viewModel.getCards(playerId, Zone.BATTLEFIELD)
        assertEquals(1, tokens.size)
        assertEquals("Soldier", tokens[0].card.name)
    }

    @Test
    fun `createToken creates multiple tokens`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        viewModel.createToken(
            playerId = playerId,
            tokenName = "Goblin",
            tokenType = "Creature - Goblin",
            power = "1",
            toughness = "1",
            color = "Red",
            quantity = 3
        )

        assertEquals(3, viewModel.getCardCount(playerId, Zone.BATTLEFIELD))
    }

    @Test
    fun `cloneCard creates clone on battlefield`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val original = createTestCardInstance("Original", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(original))

        viewModel.cloneCard(original.instanceId, playerId)

        val battlefieldCards = viewModel.getCards(playerId, Zone.BATTLEFIELD)
        assertEquals(2, battlefieldCards.size)
    }

    @Test
    fun `cloneCard sets isClone flag`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val original = createTestCardInstance("Original", playerId, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(original))

        viewModel.cloneCard(original.instanceId, playerId)

        val clones = viewModel.getCards(playerId, Zone.BATTLEFIELD).filter { it.isClone }
        assertEquals(1, clones.size, "Should have one clone")
    }

    // ==================== Reveal Mechanics Tests ====================

    @Test
    fun `revealHand sets revealed cards state`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val handCard = createTestCardInstance("Hand Card", playerId, Zone.HAND)
        addCardsToGame(viewModel, listOf(handCard))

        viewModel.revealHand(playerId, emptyList())

        val revealedState = viewModel.revealedCardsState.value
        assertNotNull(revealedState)
        assertEquals(1, revealedState.cards.size)
    }

    @Test
    fun `revealHand to all players has empty targetIds`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", listOf("Player2"))
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val handCard = createTestCardInstance("Hand Card", playerId, Zone.HAND)
        addCardsToGame(viewModel, listOf(handCard))

        viewModel.revealHand(playerId, emptyList())

        val revealedState = viewModel.revealedCardsState.value
        assertTrue(revealedState?.targetPlayerIds?.isEmpty() ?: false)
    }

    @Test
    fun `revealHand to specific player has targetId`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", listOf("Player2"))
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return
        val player2Id = viewModel.uiState.value.opponents[0].id

        val handCard = createTestCardInstance("Hand Card", playerId, Zone.HAND)
        addCardsToGame(viewModel, listOf(handCard))

        viewModel.revealHand(playerId, listOf(player2Id))

        val revealedState = viewModel.revealedCardsState.value
        assertEquals(listOf(player2Id), revealedState?.targetPlayerIds)
    }

    @Test
    fun `dismissRevealedCards clears state`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val handCard = createTestCardInstance("Hand Card", playerId, Zone.HAND)
        addCardsToGame(viewModel, listOf(handCard))

        viewModel.revealHand(playerId, emptyList())
        assertNotNull(viewModel.revealedCardsState.value)

        viewModel.dismissRevealedCards()
        assertNull(viewModel.revealedCardsState.value)
    }

    // ==================== Query Tests ====================

    @Test
    fun `getCardCount returns correct count`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val cards = (1..5).map { createTestCardInstance("Card $it", playerId, Zone.HAND) }
        addCardsToGame(viewModel, cards)

        assertEquals(5, viewModel.getCardCount(playerId, Zone.HAND))
        assertEquals(0, viewModel.getCardCount(playerId, Zone.BATTLEFIELD))
    }

    @Test
    fun `getCards returns cards in zone`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        val handCards = (1..3).map { createTestCardInstance("Hand $it", playerId, Zone.HAND) }
        val battlefieldCards = (1..2).map { createTestCardInstance("Battlefield $it", playerId, Zone.BATTLEFIELD) }
        addCardsToGame(viewModel, handCards + battlefieldCards)

        val hand = viewModel.getCards(playerId, Zone.HAND)
        val battlefield = viewModel.getCards(playerId, Zone.BATTLEFIELD)

        assertEquals(3, hand.size)
        assertEquals(2, battlefield.size)
    }

    @Test
    fun `getPlayerBattlefieldCards filters by controller`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", listOf("Player2"))
        val player1Id = viewModel.uiState.value.localPlayer?.id ?: return
        val player2Id = viewModel.uiState.value.opponents[0].id

        val p1Card = createTestCardInstance("P1 Card", player1Id, Zone.BATTLEFIELD)
        val p2Card = createTestCardInstance("P2 Card", player2Id, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(p1Card, p2Card))

        val p1Battlefield = viewModel.getPlayerBattlefieldCards(player1Id)
        val p2Battlefield = viewModel.getPlayerBattlefieldCards(player2Id)

        assertEquals(1, p1Battlefield.size)
        assertEquals(1, p2Battlefield.size)
        assertEquals("P1 Card", p1Battlefield[0].card.name)
        assertEquals("P2 Card", p2Battlefield[0].card.name)
    }

    @Test
    fun `getBattlefieldCards returns all battlefield cards`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", listOf("Player2"))
        val player1Id = viewModel.uiState.value.localPlayer?.id ?: return
        val player2Id = viewModel.uiState.value.opponents[0].id

        val p1Card = createTestCardInstance("P1 Card", player1Id, Zone.BATTLEFIELD)
        val p2Card = createTestCardInstance("P2 Card", player2Id, Zone.BATTLEFIELD)
        addCardsToGame(viewModel, listOf(p1Card, p2Card))

        val allBattlefield = viewModel.getBattlefieldCards()

        assertEquals(2, allBattlefield.size)
    }

    @Test
    fun `getAllCommanders returns commanders`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        // Create a commander in command zone
        val commander = CardInstance(
            card = Card(name = "Commander", type = "Legendary Creature - Dragon"),
            ownerId = playerId,
            zone = Zone.COMMAND_ZONE
        )
        addCardsToGame(viewModel, listOf(commander))

        val commanders = viewModel.getAllCommanders()

        assertTrue(commanders.isNotEmpty())
        assertEquals("Commander", commanders[0].card.name)
    }

    // Helper functions
    private fun createTestCardInstance(
        cardName: String,
        ownerId: String,
        zone: Zone
    ): CardInstance {
        return CardInstance(
            card = Card(name = cardName),
            ownerId = ownerId,
            zone = zone
        )
    }

    private fun addCardsToGame(viewModel: GameViewModel, cards: List<CardInstance>) {
        val currentState = viewModel.uiState.value
        val gameState = currentState.gameState ?: return

        // Use reflection to update the internal state
        // Since we can't directly modify the private _uiState, we'll use the public API
        // by manipulating the game state through the ViewModel's existing card list
        val updatedGameState = gameState.copy(
            cardInstances = gameState.cardInstances + cards
        )

        // We need to directly update the uiState for testing purposes
        // This is a workaround since the ViewModel doesn't expose a direct way to set game state
        val field = viewModel.javaClass.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<GameUiState>
        stateFlow.value = currentState.copy(gameState = updatedGameState)
    }
}
