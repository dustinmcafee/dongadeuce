package com.dustinmcafee.dongadeuce.models

import kotlin.test.*

class GameStateTest {

    @Test
    fun `activePlayer returns correct player`() {
        val player1 = Player(id = "1", name = "Player 1")
        val player2 = Player(id = "2", name = "Player 2")
        val gameState = GameState(
            gameId = "game1",
            players = listOf(player1, player2),
            cardInstances = emptyList(),
            activePlayerIndex = 0
        )

        assertEquals(player1, gameState.activePlayer, "Active player should be first player")

        val updatedState = gameState.copy(activePlayerIndex = 1)
        assertEquals(player2, updatedState.activePlayer, "Active player should be second player")
    }

    @Test
    fun `activePlayer coerces out of bounds index to valid range`() {
        val player1 = Player(id = "1", name = "Player 1")
        val player2 = Player(id = "2", name = "Player 2")
        val gameState = GameState(
            gameId = "game1",
            players = listOf(player1, player2),
            cardInstances = emptyList(),
            activePlayerIndex = 5 // Out of bounds - should coerce to valid range
        )

        // Should return last player instead of throwing
        assertEquals(player2, gameState.activePlayer)
    }

    @Test
    fun `activePlayer coerces negative index to first player`() {
        val player1 = Player(id = "1", name = "Player 1")
        val gameState = GameState(
            gameId = "game1",
            players = listOf(player1),
            cardInstances = emptyList(),
            activePlayerIndex = -1 // Negative index - should coerce to 0
        )

        // Should return first player instead of throwing
        assertEquals(player1, gameState.activePlayer)
    }

    @Test
    fun `getPlayerCards returns only cards for specified player`() {
        val player1Id = "player1"
        val player2Id = "player2"

        val card1 = CardInstance(card = Card(name = "Card 1"), ownerId = player1Id, zone = Zone.HAND)
        val card2 = CardInstance(card = Card(name = "Card 2"), ownerId = player1Id, zone = Zone.BATTLEFIELD)
        val card3 = CardInstance(card = Card(name = "Card 3"), ownerId = player2Id, zone = Zone.HAND)

        val gameState = GameState(
            gameId = "game1",
            players = emptyList(),
            cardInstances = listOf(card1, card2, card3)
        )

        val player1Cards = gameState.getPlayerCards(player1Id)
        assertEquals(2, player1Cards.size, "Player 1 should have 2 cards")
        assertTrue(player1Cards.all { it.ownerId == player1Id }, "All cards should belong to Player 1")

        val player2Cards = gameState.getPlayerCards(player2Id)
        assertEquals(1, player2Cards.size, "Player 2 should have 1 card")
    }

    @Test
    fun `getPlayerCards with zone filter returns only cards in specified zone`() {
        val playerId = "player1"

        val card1 = CardInstance(card = Card(name = "Card 1"), ownerId = playerId, zone = Zone.HAND)
        val card2 = CardInstance(card = Card(name = "Card 2"), ownerId = playerId, zone = Zone.BATTLEFIELD)
        val card3 = CardInstance(card = Card(name = "Card 3"), ownerId = playerId, zone = Zone.HAND)

        val gameState = GameState(
            gameId = "game1",
            players = emptyList(),
            cardInstances = listOf(card1, card2, card3)
        )

        val handCards = gameState.getPlayerCards(playerId, Zone.HAND)
        assertEquals(2, handCards.size, "Should have 2 cards in hand")
        assertTrue(handCards.all { it.zone == Zone.HAND }, "All cards should be in hand")

        val battlefieldCards = gameState.getPlayerCards(playerId, Zone.BATTLEFIELD)
        assertEquals(1, battlefieldCards.size, "Should have 1 card on battlefield")
    }

    @Test
    fun `updateCardInstance updates correct card`() {
        val card1 = CardInstance(card = Card(name = "Card 1"), ownerId = "player1", zone = Zone.HAND)
        val card2 = CardInstance(card = Card(name = "Card 2"), ownerId = "player1", zone = Zone.HAND)

        val gameState = GameState(
            gameId = "game1",
            players = emptyList(),
            cardInstances = listOf(card1, card2)
        )

        val updatedState = gameState.updateCardInstance(card1.instanceId) {
            it.tap()
        }

        val updatedCard = updatedState.cardInstances.find { it.instanceId == card1.instanceId }
        assertNotNull(updatedCard, "Updated card should exist")
        assertTrue(updatedCard.isTapped, "Card should be tapped")

        val otherCard = updatedState.cardInstances.find { it.instanceId == card2.instanceId }
        assertNotNull(otherCard, "Other card should still exist")
        assertFalse(otherCard.isTapped, "Other card should not be affected")
    }

    @Test
    fun `updateCardInstance with nonexistent id returns unchanged state`() {
        val card1 = CardInstance(card = Card(name = "Card 1"), ownerId = "player1", zone = Zone.HAND)

        val gameState = GameState(
            gameId = "game1",
            players = emptyList(),
            cardInstances = listOf(card1)
        )

        val updatedState = gameState.updateCardInstance("nonexistent-id") {
            it.tap()
        }

        assertEquals(gameState.cardInstances.size, updatedState.cardInstances.size)
        assertFalse(updatedState.cardInstances[0].isTapped, "Card should not be tapped")
    }

    @Test
    fun `updatePlayer updates correct player`() {
        val player1 = Player(id = "1", name = "Player 1", life = 40)
        val player2 = Player(id = "2", name = "Player 2", life = 40)

        val gameState = GameState(
            gameId = "game1",
            players = listOf(player1, player2),
            cardInstances = emptyList()
        )

        val updatedState = gameState.updatePlayer(player1.id) {
            it.takeDamage(10)
        }

        val updatedPlayer = updatedState.players.find { it.id == player1.id }
        assertNotNull(updatedPlayer, "Updated player should exist")
        assertEquals(30, updatedPlayer.life, "Player should have taken 10 damage")

        val otherPlayer = updatedState.players.find { it.id == player2.id }
        assertNotNull(otherPlayer, "Other player should still exist")
        assertEquals(40, otherPlayer.life, "Other player should not be affected")
    }

    @Test
    fun `nextPhase advances through phases correctly`() {
        val player = Player(id = "1", name = "Player 1")
        val gameState = GameState(
            gameId = "game1",
            players = listOf(player),
            cardInstances = emptyList(),
            phase = GamePhase.UNTAP,
            turnNumber = 1
        )

        val state1 = gameState.nextPhase()
        assertEquals(GamePhase.UPKEEP, state1.phase, "Should advance to UPKEEP")
        assertEquals(1, state1.turnNumber, "Turn number should not change")
        assertEquals(0, state1.activePlayerIndex, "Active player should not change")

        val state2 = state1.nextPhase()
        assertEquals(GamePhase.DRAW, state2.phase, "Should advance to DRAW")

        val state3 = state2.nextPhase()
        assertEquals(GamePhase.MAIN_1, state3.phase, "Should advance to MAIN_1")
    }

    @Test
    fun `nextPhase wraps to UNTAP and advances turn`() {
        val player1 = Player(id = "1", name = "Player 1")
        val player2 = Player(id = "2", name = "Player 2")
        val gameState = GameState(
            gameId = "game1",
            players = listOf(player1, player2),
            cardInstances = emptyList(),
            phase = GamePhase.CLEANUP,
            turnNumber = 5,
            activePlayerIndex = 0
        )

        val updatedState = gameState.nextPhase()

        assertEquals(GamePhase.UNTAP, updatedState.phase, "Should wrap to UNTAP")
        assertEquals(6, updatedState.turnNumber, "Turn number should increment")
        assertEquals(1, updatedState.activePlayerIndex, "Active player should advance to next player")
    }

    @Test
    fun `nextPhase wraps active player index correctly`() {
        val player1 = Player(id = "1", name = "Player 1")
        val player2 = Player(id = "2", name = "Player 2")
        val player3 = Player(id = "3", name = "Player 3")
        val gameState = GameState(
            gameId = "game1",
            players = listOf(player1, player2, player3),
            cardInstances = emptyList(),
            phase = GamePhase.CLEANUP,
            turnNumber = 1,
            activePlayerIndex = 2 // Last player
        )

        val updatedState = gameState.nextPhase()

        assertEquals(GamePhase.UNTAP, updatedState.phase, "Should wrap to UNTAP")
        assertEquals(2, updatedState.turnNumber, "Turn number should increment")
        assertEquals(0, updatedState.activePlayerIndex, "Active player should wrap to first player")
    }

    @Test
    fun `GamePhase next cycles through all phases`() {
        val phases = GamePhase.values()
        var currentPhase = GamePhase.UNTAP

        // Cycle through all phases
        for (i in 0 until phases.size) {
            currentPhase = currentPhase.next()
        }

        assertEquals(GamePhase.UNTAP, currentPhase, "Should cycle back to UNTAP after all phases")
    }

    @Test
    fun `GamePhase next order is correct`() {
        assertEquals(GamePhase.UPKEEP, GamePhase.UNTAP.next())
        assertEquals(GamePhase.DRAW, GamePhase.UPKEEP.next())
        assertEquals(GamePhase.MAIN_1, GamePhase.DRAW.next())
        assertEquals(GamePhase.COMBAT_BEGIN, GamePhase.MAIN_1.next())
        assertEquals(GamePhase.COMBAT_DECLARE_ATTACKERS, GamePhase.COMBAT_BEGIN.next())
        assertEquals(GamePhase.COMBAT_DECLARE_BLOCKERS, GamePhase.COMBAT_DECLARE_ATTACKERS.next())
        assertEquals(GamePhase.COMBAT_DAMAGE, GamePhase.COMBAT_DECLARE_BLOCKERS.next())
        assertEquals(GamePhase.COMBAT_END, GamePhase.COMBAT_DAMAGE.next())
        assertEquals(GamePhase.MAIN_2, GamePhase.COMBAT_END.next())
        assertEquals(GamePhase.END, GamePhase.MAIN_2.next())
        assertEquals(GamePhase.CLEANUP, GamePhase.END.next())
        assertEquals(GamePhase.UNTAP, GamePhase.CLEANUP.next())
    }

    @Test
    fun `multiple phase advances work correctly`() {
        val player = Player(id = "1", name = "Player 1")
        var gameState = GameState(
            gameId = "game1",
            players = listOf(player),
            cardInstances = emptyList(),
            phase = GamePhase.UNTAP,
            turnNumber = 1
        )

        // Advance through 4 phases
        repeat(4) {
            gameState = gameState.nextPhase()
        }

        assertEquals(GamePhase.COMBAT_BEGIN, gameState.phase, "Should be at COMBAT_BEGIN after 4 advances")
        assertEquals(1, gameState.turnNumber, "Turn should still be 1")
    }
}
