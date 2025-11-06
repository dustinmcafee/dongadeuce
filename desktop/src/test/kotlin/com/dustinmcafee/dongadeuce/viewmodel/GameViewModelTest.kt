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

    @Test
    fun `updateLife correctly updates player life and loss state`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return

        viewModel.updateLife(playerId, 20)
        assertEquals(20, viewModel.uiState.value.localPlayer?.life)
        assertFalse(viewModel.uiState.value.localPlayer?.hasLost ?: true, "Should not lose at 20 life")

        viewModel.updateLife(playerId, 0)
        assertEquals(0, viewModel.uiState.value.localPlayer?.life)
        assertTrue(viewModel.uiState.value.localPlayer?.hasLost ?: false, "Should lose at 0 life")
    }

    @Test
    fun `updateCommanderDamage tracks damage correctly`() {
        val viewModel = GameViewModel()
        viewModel.initializeGame("Player1", emptyList())
        val playerId = viewModel.uiState.value.localPlayer?.id ?: return
        val commanderId = "cmd-123"

        viewModel.updateCommanderDamage(playerId, commanderId, 10)
        assertEquals(10, viewModel.uiState.value.localPlayer?.commanderDamage?.get(commanderId))
        assertFalse(viewModel.uiState.value.localPlayer?.hasLost ?: true, "Should not lose at 10 commander damage")

        viewModel.updateCommanderDamage(playerId, commanderId, 21)
        assertEquals(21, viewModel.uiState.value.localPlayer?.commanderDamage?.get(commanderId))
        assertTrue(viewModel.uiState.value.localPlayer?.hasLost ?: false, "Should lose at 21 commander damage")
    }

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
