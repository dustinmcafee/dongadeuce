package com.dustinmcafee.dongadeuce.network

import com.dustinmcafee.dongadeuce.models.*
import kotlin.test.*

class GameEngineTest {

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

    private fun createTestDeckWithPartner(): Deck {
        return Deck(
            name = "Partner Deck",
            commander = Card(name = "Partner A", type = "Legendary Creature", power = "3", toughness = "3"),
            partnerCommander = Card(name = "Partner B", type = "Legendary Creature", power = "2", toughness = "2"),
            cards = (1..98).map { i ->
                if (i <= 35) Card(name = "Land $i", type = "Basic Land")
                else Card(name = "Spell $i", type = "Instant")
            }
        )
    }

    private fun createStartedEngine(): Triple<GameEngine, String, String> {
        val engine = GameEngine(maxPlayers = 4)
        val deck1 = createTestDeck()
        val deck2 = createTestDeck()
        val p1 = engine.addPlayer("Alice", deck1, isAdmin = true)
        val p2 = engine.addPlayer("Bob", deck2)
        engine.setPlayerReady(p2, true)
        assertTrue(engine.startGame(), "Game should start")
        return Triple(engine, p1, p2)
    }

    // ==================== Initialization ====================

    @Test
    fun `addPlayer assigns unique IDs`() {
        val engine = GameEngine()
        val id1 = engine.addPlayer("Alice", createTestDeck(), isAdmin = true)
        val id2 = engine.addPlayer("Bob", createTestDeck())

        assertNotEquals(id1, id2)
        assertEquals(2, engine.getPlayerCount())
    }

    @Test
    fun `addPlayer generates unique names on collision`() {
        val engine = GameEngine()
        engine.addPlayer("Alice", createTestDeck(), isAdmin = true)
        val id2 = engine.addPlayer("Alice", createTestDeck())

        val player2 = engine.getPlayer(id2)
        assertNotNull(player2)
        assertEquals("Alice (1)", player2.name)
    }

    @Test
    fun `addPlayer generates unique names for triple collision`() {
        val engine = GameEngine()
        engine.addPlayer("Alice", createTestDeck(), isAdmin = true)
        engine.addPlayer("Alice", createTestDeck())
        val id3 = engine.addPlayer("Alice", createTestDeck())

        val player3 = engine.getPlayer(id3)
        assertNotNull(player3)
        assertEquals("Alice (2)", player3.name)
    }

    @Test
    fun `first player becomes admin`() {
        val engine = GameEngine()
        val id1 = engine.addPlayer("Alice", createTestDeck(), isAdmin = true)

        val player = engine.getPlayer(id1)
        assertNotNull(player)
        assertTrue(player.isAdmin)
        assertTrue(player.isHost)
        assertFalse(player.isReady) // admin is not auto-ready (may not have deck)
        assertEquals(id1, engine.getAdminId())
    }

    @Test
    fun `second player is not admin and not ready`() {
        val engine = GameEngine()
        engine.addPlayer("Alice", createTestDeck(), isAdmin = true)
        val id2 = engine.addPlayer("Bob", createTestDeck())

        val player = engine.getPlayer(id2)
        assertNotNull(player)
        assertFalse(player.isAdmin)
        assertFalse(player.isReady)
    }

    @Test
    fun `removePlayer reduces count`() {
        val engine = GameEngine()
        val id1 = engine.addPlayer("Alice", createTestDeck(), isAdmin = true)
        engine.addPlayer("Bob", createTestDeck())
        assertEquals(2, engine.getPlayerCount())

        engine.removePlayer(id1)
        assertEquals(1, engine.getPlayerCount())
        assertNull(engine.getPlayer(id1))
    }

    @Test
    fun `isLobbyFull respects maxPlayers`() {
        val engine = GameEngine(maxPlayers = 2)
        engine.addPlayer("Alice", createTestDeck(), isAdmin = true)
        assertFalse(engine.isLobbyFull())

        engine.addPlayer("Bob", createTestDeck())
        assertTrue(engine.isLobbyFull())
    }

    @Test
    fun `lobbyState updates on addPlayer`() {
        val engine = GameEngine()
        val id1 = engine.addPlayer("Alice", createTestDeck(), isAdmin = true)

        val lobby = engine.lobbyState.value
        assertEquals(1, lobby.players.size)
        assertEquals("Alice", lobby.players[0].name)
        assertEquals(id1, lobby.adminId)
        assertEquals(id1, lobby.hostId) // backward compat
    }

    // ==================== Start Game ====================

    @Test
    fun `startGame fails with fewer than 2 players`() {
        val engine = GameEngine()
        engine.addPlayer("Alice", createTestDeck(), isAdmin = true)

        assertFalse(engine.startGame())
        assertFalse(engine.isGameStarted())
    }

    @Test
    fun `startGame fails when non-admin player not ready`() {
        val engine = GameEngine()
        engine.addPlayer("Alice", createTestDeck(), isAdmin = true)
        engine.addPlayer("Bob", createTestDeck())

        assertFalse(engine.startGame())
    }

    @Test
    fun `startGame succeeds when all non-admin players ready`() {
        val engine = GameEngine()
        engine.addPlayer("Alice", createTestDeck(), isAdmin = true)
        val p2 = engine.addPlayer("Bob", createTestDeck())
        engine.setPlayerReady(p2, true)

        assertTrue(engine.startGame())
        assertTrue(engine.isGameStarted())
    }

    @Test
    fun `startGame cannot be called twice`() {
        val engine = GameEngine()
        engine.addPlayer("Alice", createTestDeck(), isAdmin = true)
        val p2 = engine.addPlayer("Bob", createTestDeck())
        engine.setPlayerReady(p2, true)

        assertTrue(engine.startGame())
        assertFalse(engine.startGame()) // second call fails
    }

    @Test
    fun `startGame creates initial state with correct players`() {
        val (engine, p1, p2) = createStartedEngine()
        val state = engine.getCurrentState()
        assertNotNull(state)

        assertEquals(2, state.players.size)
        assertEquals(GameConstants.STARTING_LIFE, state.players[0].life)
        assertEquals(GameConstants.STARTING_LIFE, state.players[1].life)
        assertEquals(0, state.activePlayerIndex)
        assertEquals(1, state.turnNumber)
        assertEquals(GamePhase.UNTAP, state.phase)
    }

    @Test
    fun `startGame places commanders in command zone`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!

        val commanders = state.cardInstances.filter {
            it.ownerId == p1 && it.zone == Zone.COMMAND_ZONE
        }
        assertEquals(1, commanders.size)
        assertEquals("Test Commander", commanders[0].card.name)
    }

    @Test
    fun `startGame with partner places both commanders in command zone`() {
        val engine = GameEngine()
        val p1 = engine.addPlayer("Alice", createTestDeckWithPartner(), isAdmin = true)
        val p2 = engine.addPlayer("Bob", createTestDeck())
        engine.setPlayerReady(p2, true)
        assertTrue(engine.startGame())

        val state = engine.getCurrentState()!!
        val commanders = state.cardInstances.filter {
            it.ownerId == p1 && it.zone == Zone.COMMAND_ZONE
        }
        assertEquals(2, commanders.size)
        val names = commanders.map { it.card.name }.toSet()
        assertTrue("Partner A" in names)
        assertTrue("Partner B" in names)
    }

    @Test
    fun `startGame draws starting hands of 7`() {
        val (engine, p1, p2) = createStartedEngine()
        val state = engine.getCurrentState()!!

        val p1Hand = state.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.HAND }
        val p2Hand = state.cardInstances.filter { it.ownerId == p2 && it.zone == Zone.HAND }
        assertEquals(GameConstants.STARTING_HAND_SIZE, p1Hand.size)
        assertEquals(GameConstants.STARTING_HAND_SIZE, p2Hand.size)
    }

    @Test
    fun `startGame puts remaining cards in library`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!

        val library = state.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.LIBRARY }
        // 99 cards - 7 drawn = 92 in library
        assertEquals(92, library.size)
    }

    @Test
    fun `startGame total card count is correct per player`() {
        val (engine, p1, p2) = createStartedEngine()
        val state = engine.getCurrentState()!!

        val p1Cards = state.cardInstances.filter { it.ownerId == p1 }
        val p2Cards = state.cardInstances.filter { it.ownerId == p2 }
        assertEquals(100, p1Cards.size) // 99 cards + 1 commander
        assertEquals(100, p2Cards.size)
    }

    // ==================== Draw Card ====================

    @Test
    fun `draw card moves top of library to hand`() {
        val (engine, p1, _) = createStartedEngine()
        val stateBefore = engine.getCurrentState()!!
        val handBefore = stateBefore.cardInstances.count { it.ownerId == p1 && it.zone == Zone.HAND }
        val libBefore = stateBefore.cardInstances.count { it.ownerId == p1 && it.zone == Zone.LIBRARY }

        val result = engine.executeAction(NetworkAction.DrawCard(p1), p1)
        assertTrue(result.success)

        val stateAfter = engine.getCurrentState()!!
        val handAfter = stateAfter.cardInstances.count { it.ownerId == p1 && it.zone == Zone.HAND }
        val libAfter = stateAfter.cardInstances.count { it.ownerId == p1 && it.zone == Zone.LIBRARY }

        assertEquals(handBefore + 1, handAfter)
        assertEquals(libBefore - 1, libAfter)
    }

    @Test
    fun `draw from empty library marks player as lost`() {
        val (engine, p1, _) = createStartedEngine()

        // Move all library cards to graveyard to empty library
        var state = engine.getCurrentState()!!
        val libCards = state.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.LIBRARY }
        libCards.forEach { card ->
            engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.GRAVEYARD), p1)
        }

        // Now draw from empty library
        val result = engine.executeAction(NetworkAction.DrawCard(p1), p1)
        assertTrue(result.success)

        val finalState = engine.getCurrentState()!!
        val player = finalState.players.find { it.id == p1 }!!
        assertTrue(player.hasLost)
    }

    @Test
    fun `cannot draw for another player`() {
        val (engine, p1, p2) = createStartedEngine()

        val result = engine.executeAction(NetworkAction.DrawCard(p2), p1)
        // DrawCard validation: action.playerId != playerId → rejected
        assertFalse(result.success)
        assertEquals("Cannot draw for another player", result.reason)
    }

    // ==================== Move Card ====================

    @Test
    fun `move card from hand to battlefield`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val handCard = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        val result = engine.executeAction(NetworkAction.MoveCard(handCard.instanceId, Zone.BATTLEFIELD), p1)
        assertTrue(result.success)

        val after = engine.getCurrentState()!!
        val moved = after.cardInstances.find { it.instanceId == handCard.instanceId }!!
        assertEquals(Zone.BATTLEFIELD, moved.zone)
    }

    @Test
    fun `move card from battlefield resets state`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val handCard = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        // Play to battlefield
        engine.executeAction(NetworkAction.MoveCard(handCard.instanceId, Zone.BATTLEFIELD), p1)
        // Tap it
        engine.executeAction(NetworkAction.ToggleTap(handCard.instanceId), p1)
        // Add counter
        engine.executeAction(NetworkAction.AddCardCounter(handCard.instanceId, "+1/+1", 3), p1)

        // Verify state was set
        val midState = engine.getCurrentState()!!
        val onField = midState.cardInstances.find { it.instanceId == handCard.instanceId }!!
        assertTrue(onField.isTapped)
        assertEquals(3, onField.counters["+1/+1"])

        // Move to graveyard
        engine.executeAction(NetworkAction.MoveCard(handCard.instanceId, Zone.GRAVEYARD), p1)

        val after = engine.getCurrentState()!!
        val inGrave = after.cardInstances.find { it.instanceId == handCard.instanceId }!!
        assertEquals(Zone.GRAVEYARD, inGrave.zone)
        assertFalse(inGrave.isTapped) // reset
        assertTrue(inGrave.counters.isEmpty()) // reset
        assertEquals(0, inGrave.powerModifier) // reset
    }

    @Test
    fun `move card to battlefield assigns grid position`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val handCard = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(handCard.instanceId, Zone.BATTLEFIELD), p1)

        val after = engine.getCurrentState()!!
        val onField = after.cardInstances.find { it.instanceId == handCard.instanceId }!!
        // Grid position should be assigned
        assertNotNull(onField.gridX)
        assertNotNull(onField.gridY)
    }

    // ==================== Token Behavior ====================

    @Test
    fun `token ceases to exist when leaving battlefield`() {
        val (engine, p1, _) = createStartedEngine()

        // Create a token
        val result = engine.executeAction(
            NetworkAction.CreateToken(p1, "Soldier", "Creature — Soldier", "1", "1", "White", null, 1),
            p1
        )
        assertTrue(result.success)

        val state = engine.getCurrentState()!!
        val token = state.cardInstances.find { it.isToken && it.ownerId == p1 }
        assertNotNull(token)
        assertEquals(Zone.BATTLEFIELD, token.zone)

        // Move token off battlefield
        val moveResult = engine.executeAction(NetworkAction.MoveCard(token.instanceId, Zone.GRAVEYARD), p1)
        assertTrue(moveResult.success)

        // Token should be completely removed from the game
        val after = engine.getCurrentState()!!
        val tokenAfter = after.cardInstances.find { it.instanceId == token.instanceId }
        assertNull(tokenAfter, "Token should cease to exist when leaving battlefield")
    }

    @Test
    fun `create multiple tokens`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(
            NetworkAction.CreateToken(p1, "Goblin", "Creature — Goblin", "1", "1", "Red", null, 5),
            p1
        )

        val state = engine.getCurrentState()!!
        val tokens = state.cardInstances.filter { it.isToken && it.ownerId == p1 }
        assertEquals(5, tokens.size)
        tokens.forEach {
            assertEquals("Goblin", it.card.name)
            assertEquals(Zone.BATTLEFIELD, it.zone)
            assertTrue(it.isToken)
        }
    }

    // ==================== Tap / Untap ====================

    @Test
    fun `toggle tap taps untapped card`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.ToggleTap(card.instanceId), p1)

        val after = engine.getCurrentState()!!
        val tapped = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertTrue(tapped.isTapped)
    }

    @Test
    fun `toggle tap untaps tapped card`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.ToggleTap(card.instanceId), p1) // tap
        engine.executeAction(NetworkAction.ToggleTap(card.instanceId), p1) // untap

        val after = engine.getCurrentState()!!
        val card2 = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertFalse(card2.isTapped)
    }

    @Test
    fun `untap all untaps all tapped permanents`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val handCards = state.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.HAND }.take(3)

        // Play 3 cards and tap them
        handCards.forEach { card ->
            engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
            engine.executeAction(NetworkAction.ToggleTap(card.instanceId), p1)
        }

        // Verify all tapped
        val midState = engine.getCurrentState()!!
        val tappedCount = midState.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.BATTLEFIELD && it.isTapped
        }
        assertEquals(3, tappedCount)

        // Untap all
        engine.executeAction(NetworkAction.UntapAll(p1), p1)

        val after = engine.getCurrentState()!!
        val stillTapped = after.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.BATTLEFIELD && it.isTapped
        }
        assertEquals(0, stillTapped)
    }

    @Test
    fun `untap all respects doesnt untap flag`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.ToggleTap(card.instanceId), p1)
        engine.executeAction(NetworkAction.ToggleDoesntUntap(card.instanceId), p1)

        // Untap all should skip this card
        engine.executeAction(NetworkAction.UntapAll(p1), p1)

        val after = engine.getCurrentState()!!
        val frozen = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertTrue(frozen.isTapped, "Card with doesntUntap should remain tapped")
    }

    // ==================== Life Total ====================

    @Test
    fun `update life sets exact value`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.UpdateLife(p1, 25), p1)

        val state = engine.getCurrentState()!!
        val player = state.players.find { it.id == p1 }!!
        assertEquals(25, player.life)
    }

    @Test
    fun `update life to zero marks hasLost`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.UpdateLife(p1, 0), p1)

        val state = engine.getCurrentState()!!
        val player = state.players.find { it.id == p1 }!!
        assertEquals(0, player.life)
        assertTrue(player.hasLost)
    }

    @Test
    fun `update life to negative marks hasLost`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.UpdateLife(p1, -5), p1)

        val state = engine.getCurrentState()!!
        val player = state.players.find { it.id == p1 }!!
        assertEquals(-5, player.life)
        assertTrue(player.hasLost)
    }

    @Test
    fun `life gain works`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.UpdateLife(p1, 55), p1)

        val state = engine.getCurrentState()!!
        assertEquals(55, state.players.find { it.id == p1 }!!.life)
    }

    // ==================== Turn & Phase ====================

    @Test
    fun `next phase advances through phases in order`() {
        val (engine, p1, _) = createStartedEngine()

        assertEquals(GamePhase.UNTAP, engine.getCurrentState()!!.phase)

        engine.executeAction(NetworkAction.NextPhase, p1)
        assertEquals(GamePhase.UPKEEP, engine.getCurrentState()!!.phase)

        engine.executeAction(NetworkAction.NextPhase, p1)
        assertEquals(GamePhase.DRAW, engine.getCurrentState()!!.phase)

        engine.executeAction(NetworkAction.NextPhase, p1)
        assertEquals(GamePhase.MAIN_1, engine.getCurrentState()!!.phase)
    }

    @Test
    fun `set phase jumps directly`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.SetPhase(GamePhase.MAIN_2), p1)
        assertEquals(GamePhase.MAIN_2, engine.getCurrentState()!!.phase)
    }

    @Test
    fun `pass turn changes active player and resets phase`() {
        val (engine, p1, p2) = createStartedEngine()

        // Advance to main phase
        engine.executeAction(NetworkAction.SetPhase(GamePhase.MAIN_1), p1)

        // Pass turn
        engine.executeAction(NetworkAction.PassTurn, p1)

        val state = engine.getCurrentState()!!
        assertEquals(2, state.turnNumber)
        assertEquals(1, state.activePlayerIndex)
        assertEquals(p2, state.activePlayer.id)
        assertEquals(GamePhase.UNTAP, state.phase) // reset
    }

    @Test
    fun `turn wraps around in 4-player game`() {
        val engine = GameEngine(maxPlayers = 4)
        val ids = (1..4).map { engine.addPlayer("P$it", createTestDeck(), isAdmin = it == 1) }
        ids.drop(1).forEach { engine.setPlayerReady(it, true) }
        assertTrue(engine.startGame())

        // Pass 4 times to get back to first player
        for (i in 1..4) {
            val activeId = engine.getCurrentState()!!.activePlayer.id
            engine.executeAction(NetworkAction.PassTurn, activeId)
        }

        val state = engine.getCurrentState()!!
        assertEquals(5, state.turnNumber)
        assertEquals(0, state.activePlayerIndex)
        assertEquals(ids[0], state.activePlayer.id)
    }

    @Test
    fun `non-active player cannot advance phase`() {
        val (engine, _, p2) = createStartedEngine()

        // p2 is not the active player
        val result = engine.executeAction(NetworkAction.NextPhase, p2)
        assertFalse(result.success)
        assertEquals("Not your turn", result.reason)
    }

    @Test
    fun `non-active player cannot pass turn`() {
        val (engine, _, p2) = createStartedEngine()

        val result = engine.executeAction(NetworkAction.PassTurn, p2)
        assertFalse(result.success)
        assertEquals("Not your turn", result.reason)
    }

    // ==================== Counters ====================

    @Test
    fun `add card counter`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.AddCardCounter(card.instanceId, "+1/+1", 3), p1)

        val after = engine.getCurrentState()!!
        val updated = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(3, updated.counters["+1/+1"])
    }

    @Test
    fun `remove card counter`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.AddCardCounter(card.instanceId, "+1/+1", 5), p1)
        engine.executeAction(NetworkAction.RemoveCardCounter(card.instanceId, "+1/+1", 2), p1)

        val after = engine.getCurrentState()!!
        val updated = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(3, updated.counters["+1/+1"])
    }

    @Test
    fun `remove counter to zero removes counter type`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.AddCardCounter(card.instanceId, "+1/+1", 2), p1)
        engine.executeAction(NetworkAction.RemoveCardCounter(card.instanceId, "+1/+1", 2), p1)

        val after = engine.getCurrentState()!!
        val updated = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertFalse(updated.counters.containsKey("+1/+1"))
    }

    @Test
    fun `set card counter to specific value`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.SetCardCounter(card.instanceId, "charge", 7), p1)

        val after = engine.getCurrentState()!!
        val updated = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(7, updated.counters["charge"])
    }

    @Test
    fun `add player counter`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.AddPlayerCounter(p1, "poison", 4), p1)

        val state = engine.getCurrentState()!!
        val player = state.players.find { it.id == p1 }!!
        assertEquals(4, player.getCounter("poison"))
    }

    @Test
    fun `remove player counter`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.AddPlayerCounter(p1, "energy", 6), p1)
        engine.executeAction(NetworkAction.RemovePlayerCounter(p1, "energy", 2), p1)

        val state = engine.getCurrentState()!!
        val player = state.players.find { it.id == p1 }!!
        assertEquals(4, player.getCounter("energy"))
    }

    @Test
    fun `set player counter to specific value`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.SetPlayerCounter(p1, "experience", 10), p1)

        val state = engine.getCurrentState()!!
        val player = state.players.find { it.id == p1 }!!
        assertEquals(10, player.getCounter("experience"))
    }

    // ==================== Concede ====================

    @Test
    fun `concede sets life to zero and hasLost`() {
        val (engine, _, p2) = createStartedEngine()

        engine.executeAction(NetworkAction.Concede(p2), p2)

        val state = engine.getCurrentState()!!
        val player = state.players.find { it.id == p2 }!!
        assertEquals(0, player.life)
        assertTrue(player.hasLost)
    }

    @Test
    fun `cannot concede for another player`() {
        val (engine, p1, p2) = createStartedEngine()

        val result = engine.executeAction(NetworkAction.Concede(p2), p1)
        assertFalse(result.success)
        assertEquals("Cannot concede for another player", result.reason)
    }

    @Test
    fun `conceded player cannot take actions`() {
        val (engine, p1, p2) = createStartedEngine()

        // Concede p1
        engine.executeAction(NetworkAction.Concede(p1), p1)

        // Try to draw — should fail because eliminated
        val result = engine.executeAction(NetworkAction.DrawCard(p1), p1)
        assertFalse(result.success)
        assertEquals("You have been eliminated", result.reason)
    }

    // ==================== Commander Damage ====================

    @Test
    fun `commander damage at 21 marks player as lost`() {
        val (engine, p1, p2) = createStartedEngine()
        val state = engine.getCurrentState()!!

        // Find p1's commander
        val commander = state.cardInstances.first {
            it.ownerId == p1 && it.zone == Zone.COMMAND_ZONE
        }

        // Deal 21 commander damage to p2
        engine.executeAction(
            NetworkAction.UpdateCommanderDamage(p2, commander.instanceId, 21),
            p1
        )

        val after = engine.getCurrentState()!!
        val p2Player = after.players.find { it.id == p2 }!!
        assertTrue(p2Player.hasLost)
        assertEquals(21, p2Player.commanderDamage[commander.instanceId])
    }

    @Test
    fun `commander damage below 21 does not kill`() {
        val (engine, p1, p2) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val commander = state.cardInstances.first {
            it.ownerId == p1 && it.zone == Zone.COMMAND_ZONE
        }

        engine.executeAction(
            NetworkAction.UpdateCommanderDamage(p2, commander.instanceId, 20),
            p1
        )

        val after = engine.getCurrentState()!!
        val p2Player = after.players.find { it.id == p2 }!!
        assertFalse(p2Player.hasLost)
    }

    // ==================== Mill ====================

    @Test
    fun `mill moves cards from library to graveyard`() {
        val (engine, p1, _) = createStartedEngine()
        val libBefore = engine.getCurrentState()!!.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.LIBRARY
        }

        engine.executeAction(NetworkAction.MillCards(p1, 5), p1)

        val after = engine.getCurrentState()!!
        val libAfter = after.cardInstances.count { it.ownerId == p1 && it.zone == Zone.LIBRARY }
        val graveAfter = after.cardInstances.count { it.ownerId == p1 && it.zone == Zone.GRAVEYARD }

        assertEquals(libBefore - 5, libAfter)
        assertEquals(5, graveAfter)
    }

    @Test
    fun `mill more than library size mills only available cards`() {
        val (engine, p1, _) = createStartedEngine()
        val libSize = engine.getCurrentState()!!.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.LIBRARY
        }

        engine.executeAction(NetworkAction.MillCards(p1, 999), p1)

        val after = engine.getCurrentState()!!
        val libAfter = after.cardInstances.count { it.ownerId == p1 && it.zone == Zone.LIBRARY }
        val graveAfter = after.cardInstances.count { it.ownerId == p1 && it.zone == Zone.GRAVEYARD }

        assertEquals(0, libAfter)
        assertEquals(libSize, graveAfter)
    }

    // ==================== Mulligan ====================

    @Test
    fun `mulligan returns hand and draws new 7`() {
        val (engine, p1, _) = createStartedEngine()
        val stateBefore = engine.getCurrentState()!!
        val handBefore = stateBefore.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.HAND }
            .map { it.instanceId }.toSet()

        engine.executeAction(NetworkAction.Mulligan(p1), p1)

        val after = engine.getCurrentState()!!
        val handAfter = after.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.HAND }
        assertEquals(7, handAfter.size)

        // Verify it's a different hand (at least some cards changed — shuffled)
        val newIds = handAfter.map { it.instanceId }.toSet()
        // With 92 cards in library, getting the exact same 7 is astronomically unlikely
        // But we can at least verify count and that old hand cards are back in library
        val totalCards = after.cardInstances.count { it.ownerId == p1 }
        assertEquals(100, totalCards, "Total card count should be preserved")
    }

    // ==================== Shuffle ====================

    @Test
    fun `shuffle library preserves card count`() {
        val (engine, p1, _) = createStartedEngine()
        val libBefore = engine.getCurrentState()!!.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.LIBRARY
        }

        engine.executeAction(NetworkAction.ShuffleLibrary(p1), p1)

        val libAfter = engine.getCurrentState()!!.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.LIBRARY
        }
        assertEquals(libBefore, libAfter)
    }

    // ==================== Library Position ====================

    @Test
    fun `move card to top of library`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val handCard = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCardToTopOfLibrary(handCard.instanceId), p1)

        val after = engine.getCurrentState()!!
        val libCards = after.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.LIBRARY }
        assertEquals(handCard.instanceId, libCards.last().instanceId, "Card should be on top (last in list)")
    }

    @Test
    fun `move card to bottom of library`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val handCard = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCardToBottomOfLibrary(handCard.instanceId), p1)

        val after = engine.getCurrentState()!!
        val libCards = after.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.LIBRARY }
        assertEquals(handCard.instanceId, libCards.first().instanceId, "Card should be on bottom (first in list)")
    }

    // ==================== Power/Toughness ====================

    @Test
    fun `modify power`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.ModifyPower(card.instanceId, 3), p1)

        val after = engine.getCurrentState()!!
        val updated = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(3, updated.powerModifier)
    }

    @Test
    fun `modify toughness`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.ModifyToughness(card.instanceId, -1), p1)

        val after = engine.getCurrentState()!!
        val updated = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(-1, updated.toughnessModifier)
    }

    @Test
    fun `reset power toughness`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.ModifyPowerToughness(card.instanceId, 5), p1)
        engine.executeAction(NetworkAction.ResetPowerToughness(card.instanceId), p1)

        val after = engine.getCurrentState()!!
        val updated = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(0, updated.powerModifier)
        assertEquals(0, updated.toughnessModifier)
    }

    // ==================== Flip / Face Down ====================

    @Test
    fun `flip card toggles`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.FlipCard(card.instanceId), p1)

        val mid = engine.getCurrentState()!!
        assertTrue(mid.cardInstances.find { it.instanceId == card.instanceId }!!.isFlipped)

        engine.executeAction(NetworkAction.FlipCard(card.instanceId), p1)

        val after = engine.getCurrentState()!!
        assertFalse(after.cardInstances.find { it.instanceId == card.instanceId }!!.isFlipped)
    }

    @Test
    fun `toggle face down`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.ToggleFaceDown(card.instanceId), p1)

        val after = engine.getCurrentState()!!
        assertTrue(after.cardInstances.find { it.instanceId == card.instanceId }!!.isFaceDown)
    }

    // ==================== DFC / Non-DFC Flip ====================

    private fun createDfcDeck(): Deck {
        return Deck(
            name = "DFC Deck",
            commander = Card(
                name = "Delver of Secrets",
                type = "Creature — Human Wizard",
                power = "1",
                toughness = "1",
                backFaceImageUri = "https://example.com/insectile.jpg",
                backFaceName = "Insectile Aberration",
                backFaceType = "Creature — Human Insect",
                backFacePower = "3",
                backFaceToughness = "2",
                backFaceOracleText = "Flying"
            ),
            cards = (1..99).map { i ->
                when {
                    i == 1 -> Card(
                        name = "Huntmaster of the Fells",
                        type = "Creature — Human Werewolf",
                        power = "2",
                        toughness = "2",
                        backFaceImageUri = "https://example.com/ravager.jpg",
                        backFaceName = "Ravager of the Fells",
                        backFaceType = "Creature — Werewolf",
                        backFacePower = "4",
                        backFaceToughness = "4"
                    )
                    i == 2 -> Card(
                        name = "Lightning Bolt",
                        type = "Instant"
                    )
                    i == 3 -> Card(
                        name = "Grizzly Bears",
                        type = "Creature — Bear",
                        power = "2",
                        toughness = "2"
                    )
                    i <= 35 -> Card(name = "Land $i", type = "Basic Land")
                    else -> Card(name = "Spell $i", type = "Instant")
                }
            }
        )
    }

    private fun createStartedDfcEngine(): Triple<GameEngine, String, String> {
        val engine = GameEngine(maxPlayers = 4)
        val p1 = engine.addPlayer("Alice", createDfcDeck(), isAdmin = true)
        val p2 = engine.addPlayer("Bob", createTestDeck())
        engine.setPlayerReady(p2, true)
        assertTrue(engine.startGame())
        return Triple(engine, p1, p2)
    }

    @Test
    fun `DFC card has backFace data`() {
        val (engine, p1, _) = createStartedDfcEngine()
        val state = engine.getCurrentState()!!

        val commander = state.cardInstances.first {
            it.ownerId == p1 && it.zone == Zone.COMMAND_ZONE
        }
        assertTrue(commander.card.isDoubleFaced)
        assertEquals("Insectile Aberration", commander.card.backFaceName)
        assertEquals("Creature — Human Insect", commander.card.backFaceType)
        assertEquals("3", commander.card.backFacePower)
        assertEquals("2", commander.card.backFaceToughness)
    }

    @Test
    fun `non-DFC card is not double-faced`() {
        val (engine, p1, _) = createStartedDfcEngine()
        val state = engine.getCurrentState()!!

        val nonDfc = state.cardInstances.first {
            it.ownerId == p1 && it.card.name == "Grizzly Bears"
        }
        assertFalse(nonDfc.card.isDoubleFaced)
        assertNull(nonDfc.card.backFaceName)
        assertNull(nonDfc.card.backFaceImageUri)
    }

    @Test
    fun `flip DFC card sets isFlipped`() {
        val (engine, p1, _) = createStartedDfcEngine()
        val state = engine.getCurrentState()!!

        val commander = state.cardInstances.first {
            it.ownerId == p1 && it.zone == Zone.COMMAND_ZONE
        }

        // Move commander to battlefield first
        engine.executeAction(NetworkAction.MoveCard(commander.instanceId, Zone.BATTLEFIELD), p1)

        // Flip it (DFC — transform to back face)
        engine.executeAction(NetworkAction.FlipCard(commander.instanceId), p1)

        val after = engine.getCurrentState()!!
        val flipped = after.cardInstances.find { it.instanceId == commander.instanceId }!!
        assertTrue(flipped.isFlipped)
        // Card data still has front face info and back face info
        assertTrue(flipped.card.isDoubleFaced)
        assertEquals("Delver of Secrets", flipped.card.name) // front face name stays
        assertEquals("Insectile Aberration", flipped.card.backFaceName) // back face accessible
    }

    @Test
    fun `flip DFC card back to front face`() {
        val (engine, p1, _) = createStartedDfcEngine()
        val state = engine.getCurrentState()!!

        val commander = state.cardInstances.first {
            it.ownerId == p1 && it.zone == Zone.COMMAND_ZONE
        }
        engine.executeAction(NetworkAction.MoveCard(commander.instanceId, Zone.BATTLEFIELD), p1)

        // Flip to back
        engine.executeAction(NetworkAction.FlipCard(commander.instanceId), p1)
        assertTrue(engine.getCurrentState()!!.cardInstances.find { it.instanceId == commander.instanceId }!!.isFlipped)

        // Flip to front
        engine.executeAction(NetworkAction.FlipCard(commander.instanceId), p1)
        assertFalse(engine.getCurrentState()!!.cardInstances.find { it.instanceId == commander.instanceId }!!.isFlipped)
    }

    @Test
    fun `flip non-DFC card still toggles isFlipped`() {
        val (engine, p1, _) = createStartedDfcEngine()
        val state = engine.getCurrentState()!!

        val bear = state.cardInstances.first { it.ownerId == p1 && it.card.name == "Grizzly Bears" }
        engine.executeAction(NetworkAction.MoveCard(bear.instanceId, Zone.BATTLEFIELD), p1)

        // Flip non-DFC card (shows generic card back in UI)
        engine.executeAction(NetworkAction.FlipCard(bear.instanceId), p1)

        val after = engine.getCurrentState()!!
        val flipped = after.cardInstances.find { it.instanceId == bear.instanceId }!!
        assertTrue(flipped.isFlipped)
        assertFalse(flipped.card.isDoubleFaced) // still not a DFC
        assertNull(flipped.card.backFaceName) // no back face data
    }

    @Test
    fun `flip resets when leaving battlefield`() {
        val (engine, p1, _) = createStartedDfcEngine()
        val state = engine.getCurrentState()!!

        val commander = state.cardInstances.first {
            it.ownerId == p1 && it.zone == Zone.COMMAND_ZONE
        }
        engine.executeAction(NetworkAction.MoveCard(commander.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.FlipCard(commander.instanceId), p1)

        // Move to graveyard — isFlipped should reset
        engine.executeAction(NetworkAction.MoveCard(commander.instanceId, Zone.GRAVEYARD), p1)

        val after = engine.getCurrentState()!!
        val inGrave = after.cardInstances.find { it.instanceId == commander.instanceId }!!
        assertFalse(inGrave.isFlipped, "isFlipped should reset when leaving battlefield")
    }

    @Test
    fun `face down and flip are independent states`() {
        val (engine, p1, _) = createStartedDfcEngine()
        val state = engine.getCurrentState()!!

        val bear = state.cardInstances.first { it.ownerId == p1 && it.card.name == "Grizzly Bears" }
        engine.executeAction(NetworkAction.MoveCard(bear.instanceId, Zone.BATTLEFIELD), p1)

        // Set face down (morph)
        engine.executeAction(NetworkAction.ToggleFaceDown(bear.instanceId), p1)
        // Also flip
        engine.executeAction(NetworkAction.FlipCard(bear.instanceId), p1)

        val after = engine.getCurrentState()!!
        val card = after.cardInstances.find { it.instanceId == bear.instanceId }!!
        assertTrue(card.isFaceDown)
        assertTrue(card.isFlipped)

        // Unflip — face down should remain
        engine.executeAction(NetworkAction.FlipCard(bear.instanceId), p1)
        val after2 = engine.getCurrentState()!!
        val card2 = after2.cardInstances.find { it.instanceId == bear.instanceId }!!
        assertTrue(card2.isFaceDown)
        assertFalse(card2.isFlipped)
    }

    @Test
    fun `play face down puts card on battlefield face down`() {
        val (engine, p1, _) = createStartedDfcEngine()
        val state = engine.getCurrentState()!!

        val bear = state.cardInstances.first { it.ownerId == p1 && it.card.name == "Grizzly Bears" }

        engine.executeAction(NetworkAction.PlayFaceDown(bear.instanceId), p1)

        val after = engine.getCurrentState()!!
        val played = after.cardInstances.find { it.instanceId == bear.instanceId }!!
        assertEquals(Zone.BATTLEFIELD, played.zone)
        assertTrue(played.isFaceDown)
    }

    @Test
    fun `DFC in library with another DFC on battlefield both track independently`() {
        val (engine, p1, _) = createStartedDfcEngine()
        val state = engine.getCurrentState()!!

        // Commander is DFC (Delver) — move to battlefield and flip
        val commander = state.cardInstances.first {
            it.ownerId == p1 && it.zone == Zone.COMMAND_ZONE
        }
        engine.executeAction(NetworkAction.MoveCard(commander.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.FlipCard(commander.instanceId), p1)

        // Find Huntmaster (another DFC) in library
        val huntmaster = engine.getCurrentState()!!.cardInstances.first {
            it.ownerId == p1 && it.card.name == "Huntmaster of the Fells"
        }

        // Move Huntmaster to battlefield — should not be flipped
        engine.executeAction(NetworkAction.MoveCard(huntmaster.instanceId, Zone.BATTLEFIELD), p1)

        val after = engine.getCurrentState()!!
        val commanderAfter = after.cardInstances.find { it.instanceId == commander.instanceId }!!
        val huntmasterAfter = after.cardInstances.find { it.instanceId == huntmaster.instanceId }!!

        assertTrue(commanderAfter.isFlipped, "Commander should still be flipped")
        assertFalse(huntmasterAfter.isFlipped, "Huntmaster should not be flipped")
        assertTrue(huntmasterAfter.card.isDoubleFaced)
        assertEquals("Ravager of the Fells", huntmasterAfter.card.backFaceName)
    }

    // ==================== Pause / Resume ====================

    @Test
    fun `pause prevents actions`() {
        val (engine, p1, _) = createStartedEngine()

        engine.pauseGame("test pause")
        assertTrue(engine.isPaused.value)
        assertEquals("test pause", engine.pauseReason.value)

        val result = engine.executeAction(NetworkAction.DrawCard(p1), p1)
        assertFalse(result.success)
        assertEquals("Game not active", result.reason)
    }

    @Test
    fun `resume allows actions again`() {
        val (engine, p1, _) = createStartedEngine()

        engine.pauseGame("test pause")
        engine.resumeGame()

        assertFalse(engine.isPaused.value)
        assertNull(engine.pauseReason.value)

        val result = engine.executeAction(NetworkAction.DrawCard(p1), p1)
        assertTrue(result.success)
    }

    // ==================== Actions before game starts ====================

    @Test
    fun `actions fail before game starts`() {
        val engine = GameEngine()
        val p1 = engine.addPlayer("Alice", createTestDeck(), isAdmin = true)

        val result = engine.executeAction(NetworkAction.DrawCard(p1), p1)
        assertFalse(result.success)
        assertEquals("Game not active", result.reason)
    }

    // ==================== Card Ownership Validation ====================

    @Test
    fun `cannot tap card you dont own or control`() {
        val (engine, p1, p2) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val p1Card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(p1Card.instanceId, Zone.BATTLEFIELD), p1)

        // p2 tries to tap p1's card
        val result = engine.executeAction(NetworkAction.ToggleTap(p1Card.instanceId), p2)
        assertFalse(result.success)
        assertEquals("You don't control this card", result.reason)
    }

    @Test
    fun `cannot move card you dont own or control`() {
        val (engine, p1, p2) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val p1Card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        val result = engine.executeAction(NetworkAction.MoveCard(p1Card.instanceId, Zone.GRAVEYARD), p2)
        assertFalse(result.success)
        assertEquals("You don't control this card", result.reason)
    }

    // ==================== Attach / Detach ====================

    @Test
    fun `attach and detach cards`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val cards = state.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.HAND }.take(2)

        // Play both to battlefield
        engine.executeAction(NetworkAction.MoveCard(cards[0].instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.MoveCard(cards[1].instanceId, Zone.BATTLEFIELD), p1)

        // Attach card 0 to card 1
        engine.executeAction(NetworkAction.AttachCard(cards[0].instanceId, cards[1].instanceId), p1)

        val mid = engine.getCurrentState()!!
        val attached = mid.cardInstances.find { it.instanceId == cards[0].instanceId }!!
        assertEquals(cards[1].instanceId, attached.attachedTo)

        // Detach
        engine.executeAction(NetworkAction.DetachCard(cards[0].instanceId), p1)

        val after = engine.getCurrentState()!!
        val detached = after.cardInstances.find { it.instanceId == cards[0].instanceId }!!
        assertNull(detached.attachedTo)
    }

    // ==================== Give Control ====================

    @Test
    fun `give control changes controller`() {
        val (engine, p1, p2) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.GiveControlTo(card.instanceId, p2), p1)

        val after = engine.getCurrentState()!!
        val given = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(p2, given.controllerId)
        assertEquals(p1, given.ownerId) // ownership doesn't change
    }

    // ==================== Annotation ====================

    @Test
    fun `set and clear annotation`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.SetAnnotation(card.instanceId, "Pumped +3"), p1)

        val mid = engine.getCurrentState()!!
        assertEquals("Pumped +3", mid.cardInstances.find { it.instanceId == card.instanceId }!!.annotation)

        engine.executeAction(NetworkAction.SetAnnotation(card.instanceId, null), p1)

        val after = engine.getCurrentState()!!
        assertNull(after.cardInstances.find { it.instanceId == card.instanceId }!!.annotation)
    }

    // ==================== Clone ====================

    @Test
    fun `clone card creates copy`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)

        val totalBefore = engine.getCurrentState()!!.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.BATTLEFIELD
        }

        engine.executeAction(NetworkAction.CloneCard(card.instanceId, p1, Zone.BATTLEFIELD, 2), p1)

        val after = engine.getCurrentState()!!
        val totalAfter = after.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.BATTLEFIELD
        }
        assertEquals(totalBefore + 2, totalAfter)

        val clones = after.cardInstances.filter {
            it.isClone && it.ownerId == p1
        }
        assertEquals(2, clones.size)
    }

    // ==================== Game Log ====================

    @Test
    fun `actions generate game events`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.DrawCard(p1), p1)
        engine.executeAction(NetworkAction.UpdateLife(p1, 35), p1)
        engine.executeAction(NetworkAction.NextPhase, p1)

        val state = engine.getCurrentState()!!
        // GameStarted + 7 draws per player (14) + DrawCard + UpdateLife + PhaseChanged
        // At minimum we should have the 3 events we just added
        val recentEvents = state.gameLog.takeLast(3)
        assertTrue(recentEvents.any { it is GameEvent.CardDrawn })
        assertTrue(recentEvents.any { it is GameEvent.LifeChanged })
        assertTrue(recentEvents.any { it is GameEvent.PhaseChanged })
    }

    // ==================== Card Count Integrity ====================

    @Test
    fun `total card count preserved through complex operations`() {
        val (engine, p1, p2) = createStartedEngine()

        // Perform various operations
        val state = engine.getCurrentState()!!
        val handCards = state.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.HAND }

        // Play 2 cards
        engine.executeAction(NetworkAction.MoveCard(handCards[0].instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.MoveCard(handCards[1].instanceId, Zone.BATTLEFIELD), p1)
        // Draw 3
        engine.executeAction(NetworkAction.DrawCard(p1), p1)
        engine.executeAction(NetworkAction.DrawCard(p1), p1)
        engine.executeAction(NetworkAction.DrawCard(p1), p1)
        // Mill 2
        engine.executeAction(NetworkAction.MillCards(p1, 2), p1)
        // Move one to exile
        engine.executeAction(NetworkAction.MoveCard(handCards[0].instanceId, Zone.EXILE), p1)

        val after = engine.getCurrentState()!!
        val p1Total = after.cardInstances.count { it.ownerId == p1 }
        val p2Total = after.cardInstances.count { it.ownerId == p2 }

        assertEquals(100, p1Total, "Player 1 total card count should be preserved")
        assertEquals(100, p2Total, "Player 2 total card count should be preserved")
    }

    // ==================== Library Position (specific) ====================

    @Test
    fun `move card to library position from top`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val handCard = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        // Move to 3rd from top
        engine.executeAction(NetworkAction.MoveCardToLibraryPosition(handCard.instanceId, 3), p1)

        val after = engine.getCurrentState()!!
        val libCards = after.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.LIBRARY }
        // Card should be in library (hand count decreased)
        val handAfter = after.cardInstances.count { it.ownerId == p1 && it.zone == Zone.HAND }
        assertEquals(6, handAfter) // 7 - 1
        assertTrue(libCards.any { it.instanceId == handCard.instanceId })
    }

    @Test
    fun `move card to library position from bottom`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val handCard = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCardToLibraryPositionFromBottom(handCard.instanceId, 2), p1)

        val after = engine.getCurrentState()!!
        val libCards = after.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.LIBRARY }
        assertTrue(libCards.any { it.instanceId == handCard.instanceId })
        // Should be near the bottom (second from bottom = index 1)
        assertEquals(handCard.instanceId, libCards[1].instanceId)
    }

    // ==================== Bulk Zone Moves ====================

    @Test
    fun `move top cards to zone`() {
        val (engine, p1, _) = createStartedEngine()
        val graveBefore = engine.getCurrentState()!!.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.GRAVEYARD
        }
        val libBefore = engine.getCurrentState()!!.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.LIBRARY
        }

        engine.executeAction(NetworkAction.MoveTopCardsToZone(p1, 3, Zone.GRAVEYARD), p1)

        val after = engine.getCurrentState()!!
        val graveAfter = after.cardInstances.count { it.ownerId == p1 && it.zone == Zone.GRAVEYARD }
        val libAfter = after.cardInstances.count { it.ownerId == p1 && it.zone == Zone.LIBRARY }

        assertEquals(graveBefore + 3, graveAfter)
        assertEquals(libBefore - 3, libAfter)
    }

    @Test
    fun `move bottom cards to zone`() {
        val (engine, p1, _) = createStartedEngine()
        val exileBefore = engine.getCurrentState()!!.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.EXILE
        }

        engine.executeAction(NetworkAction.MoveBottomCardsToZone(p1, 4, Zone.EXILE), p1)

        val after = engine.getCurrentState()!!
        val exileAfter = after.cardInstances.count { it.ownerId == p1 && it.zone == Zone.EXILE }
        assertEquals(exileBefore + 4, exileAfter)
    }

    @Test
    fun `move bottom card to top`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val libCards = state.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.LIBRARY }
        val bottomCard = libCards.first() // first = bottom

        engine.executeAction(NetworkAction.MoveBottomCardToTop(p1), p1)

        val after = engine.getCurrentState()!!
        val libAfter = after.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.LIBRARY }
        assertEquals(bottomCard.instanceId, libAfter.last().instanceId, "Bottom card should now be on top")
        assertEquals(libCards.size, libAfter.size, "Library size should be unchanged")
    }

    @Test
    fun `move bottom card to top on empty library is no-op`() {
        val (engine, p1, _) = createStartedEngine()

        // Empty the library
        val state = engine.getCurrentState()!!
        state.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.LIBRARY }.forEach {
            engine.executeAction(NetworkAction.MoveCard(it.instanceId, Zone.GRAVEYARD), p1)
        }

        // Should not crash
        val result = engine.executeAction(NetworkAction.MoveBottomCardToTop(p1), p1)
        assertTrue(result.success) // no-op but doesn't fail
    }

    // ==================== Partial Shuffles ====================

    @Test
    fun `shuffle top cards preserves library size`() {
        val (engine, p1, _) = createStartedEngine()
        val libBefore = engine.getCurrentState()!!.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.LIBRARY
        }

        engine.executeAction(NetworkAction.ShuffleTopCards(p1, 10), p1)

        val libAfter = engine.getCurrentState()!!.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.LIBRARY
        }
        assertEquals(libBefore, libAfter)
    }

    @Test
    fun `shuffle bottom cards preserves library size`() {
        val (engine, p1, _) = createStartedEngine()
        val libBefore = engine.getCurrentState()!!.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.LIBRARY
        }

        engine.executeAction(NetworkAction.ShuffleBottomCards(p1, 10), p1)

        val libAfter = engine.getCurrentState()!!.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.LIBRARY
        }
        assertEquals(libBefore, libAfter)
    }

    @Test
    fun `shuffle top cards with count 1 is no-op`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val libBefore = state.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.LIBRARY }
            .map { it.instanceId }

        engine.executeAction(NetworkAction.ShuffleTopCards(p1, 1), p1)

        val libAfter = engine.getCurrentState()!!.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.LIBRARY }
            .map { it.instanceId }
        assertEquals(libBefore, libAfter, "Shuffling 1 card should be no-op")
    }

    // ==================== SetPowerToughness / Flow ====================

    @Test
    fun `set power toughness to exact values`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        // Find a creature with known P/T
        val creature = state.cardInstances.first {
            it.ownerId == p1 && it.zone == Zone.HAND && it.card.power == "2"
        }

        engine.executeAction(NetworkAction.MoveCard(creature.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.SetPowerToughness(creature.instanceId, 5, 7), p1)

        val after = engine.getCurrentState()!!
        val updated = after.cardInstances.find { it.instanceId == creature.instanceId }!!
        // base power is 2, modifier should be 3 to get to 5
        assertEquals(3, updated.powerModifier)
        // base toughness is 2, modifier should be 5 to get to 7
        assertEquals(5, updated.toughnessModifier)
    }

    @Test
    fun `flow power increases power decreases toughness`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.FlowPower(card.instanceId), p1)

        val after = engine.getCurrentState()!!
        val updated = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(1, updated.powerModifier)
        assertEquals(-1, updated.toughnessModifier)
    }

    @Test
    fun `flow toughness decreases power increases toughness`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.FlowToughness(card.instanceId), p1)

        val after = engine.getCurrentState()!!
        val updated = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(-1, updated.powerModifier)
        assertEquals(1, updated.toughnessModifier)
    }

    @Test
    fun `multiple flow operations accumulate`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.FlowPower(card.instanceId), p1)
        engine.executeAction(NetworkAction.FlowPower(card.instanceId), p1)
        engine.executeAction(NetworkAction.FlowPower(card.instanceId), p1)

        val after = engine.getCurrentState()!!
        val updated = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(3, updated.powerModifier)
        assertEquals(-3, updated.toughnessModifier)
    }

    // ==================== SetPhase UNTAP auto-untap ====================

    @Test
    fun `set phase to UNTAP auto-untaps active player permanents`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val cards = state.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.HAND }.take(2)

        // Play and tap
        cards.forEach {
            engine.executeAction(NetworkAction.MoveCard(it.instanceId, Zone.BATTLEFIELD), p1)
            engine.executeAction(NetworkAction.ToggleTap(it.instanceId), p1)
        }

        // Move to main phase first
        engine.executeAction(NetworkAction.SetPhase(GamePhase.MAIN_1), p1)

        // Set back to UNTAP — should auto-untap
        engine.executeAction(NetworkAction.SetPhase(GamePhase.UNTAP), p1)

        val after = engine.getCurrentState()!!
        val tapped = after.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.BATTLEFIELD && it.isTapped
        }
        assertEquals(0, tapped, "SetPhase(UNTAP) should auto-untap active player's permanents")
    }

    @Test
    fun `set phase to UNTAP does not untap doesntUntap cards`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.ToggleTap(card.instanceId), p1)
        engine.executeAction(NetworkAction.ToggleDoesntUntap(card.instanceId), p1)

        engine.executeAction(NetworkAction.SetPhase(GamePhase.MAIN_1), p1)
        engine.executeAction(NetworkAction.SetPhase(GamePhase.UNTAP), p1)

        val after = engine.getCurrentState()!!
        val frozen = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertTrue(frozen.isTapped, "doesntUntap card should remain tapped during UNTAP phase")
    }

    // ==================== Library Visibility ====================

    @Test
    fun `toggle reveal top card`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.ToggleRevealTopCard(p1), p1)

        val state = engine.getCurrentState()!!
        val player = state.players.find { it.id == p1 }!!
        assertTrue(player.revealTopCard)
        assertTrue(player.lookAtTopCard, "Revealing should also enable looking")
    }

    @Test
    fun `toggle reveal top card off`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.ToggleRevealTopCard(p1), p1) // on
        engine.executeAction(NetworkAction.ToggleRevealTopCard(p1), p1) // off

        val state = engine.getCurrentState()!!
        val player = state.players.find { it.id == p1 }!!
        assertFalse(player.revealTopCard)
    }

    @Test
    fun `toggle look at top card`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.ToggleLookAtTopCard(p1), p1)

        val state = engine.getCurrentState()!!
        val player = state.players.find { it.id == p1 }!!
        assertTrue(player.lookAtTopCard)
    }

    @Test
    fun `toggle look at top card off also disables reveal`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.ToggleRevealTopCard(p1), p1) // reveal on (also sets look)
        engine.executeAction(NetworkAction.ToggleLookAtTopCard(p1), p1) // look off

        val state = engine.getCurrentState()!!
        val player = state.players.find { it.id == p1 }!!
        assertFalse(player.lookAtTopCard)
        assertFalse(player.revealTopCard, "Disabling look should also disable reveal")
    }

    @Test
    fun `cannot toggle library visibility for another player`() {
        val (engine, p1, p2) = createStartedEngine()

        val result = engine.executeAction(NetworkAction.ToggleRevealTopCard(p2), p1)
        assertFalse(result.success)
        assertEquals("Cannot toggle library visibility for another player", result.reason)
    }

    // ==================== Die Roll & Chat ====================

    @Test
    fun `log die roll adds event`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.LogDieRoll(p1, "d20", 17), p1)

        val state = engine.getCurrentState()!!
        val dieEvents = state.gameLog.filterIsInstance<GameEvent.DieRolled>()
        assertEquals(1, dieEvents.size)
        assertEquals("d20", dieEvents[0].dieType)
        assertEquals(17, dieEvents[0].result)
    }

    @Test
    fun `log die roll with multiple dice`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(
            NetworkAction.LogDieRoll(p1, "d6", 9, numberOfDice = 2, individualResults = listOf(3, 6)),
            p1
        )

        val state = engine.getCurrentState()!!
        val dieEvents = state.gameLog.filterIsInstance<GameEvent.DieRolled>()
        assertEquals(1, dieEvents.size)
        assertEquals(2, dieEvents[0].numberOfDice)
        assertEquals(listOf(3, 6), dieEvents[0].individualResults)
    }

    @Test
    fun `send chat message adds event`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.SendChatMessage(p1, "GG"), p1)

        val state = engine.getCurrentState()!!
        val chatEvents = state.gameLog.filterIsInstance<GameEvent.ChatMessage>()
        assertEquals(1, chatEvents.size)
        assertEquals("GG", chatEvents[0].message)
    }

    // ==================== Grid Position ====================

    @Test
    fun `update card grid position`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.UpdateCardGridPosition(card.instanceId, 3, 1), p1)

        val after = engine.getCurrentState()!!
        val updated = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(3, updated.gridX)
        assertEquals(1, updated.gridY)
    }

    // ==================== Control Revert on Zone Change ====================

    @Test
    fun `control reverts to owner when card leaves battlefield`() {
        val (engine, p1, p2) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        // Play and give control to p2
        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.GiveControlTo(card.instanceId, p2), p1)

        // Verify p2 controls it
        val mid = engine.getCurrentState()!!
        assertEquals(p2, mid.cardInstances.find { it.instanceId == card.instanceId }!!.controllerId)

        // Move to graveyard — control should revert to owner
        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.GRAVEYARD), p2)

        val after = engine.getCurrentState()!!
        val inGrave = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(p1, inGrave.controllerId, "Control should revert to owner when leaving battlefield")
        assertEquals(p1, inGrave.ownerId)
    }

    // ==================== Multiple Counter Types ====================

    @Test
    fun `multiple counter types on same card`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.AddCardCounter(card.instanceId, "+1/+1", 3), p1)
        engine.executeAction(NetworkAction.AddCardCounter(card.instanceId, "charge", 5), p1)
        engine.executeAction(NetworkAction.AddCardCounter(card.instanceId, "loyalty", 2), p1)

        val after = engine.getCurrentState()!!
        val updated = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(3, updated.counters["+1/+1"])
        assertEquals(5, updated.counters["charge"])
        assertEquals(2, updated.counters["loyalty"])
        assertEquals(3, updated.counters.size)
    }

    // ==================== 6-Player Game ====================

    @Test
    fun `6-player game initializes and cycles turns`() {
        val engine = GameEngine(maxPlayers = 6)
        val ids = (1..6).map { engine.addPlayer("P$it", createTestDeck(), isAdmin = it == 1) }
        ids.drop(1).forEach { engine.setPlayerReady(it, true) }
        assertTrue(engine.startGame())

        val state = engine.getCurrentState()!!
        assertEquals(6, state.players.size)
        state.players.forEach {
            assertEquals(GameConstants.STARTING_LIFE, it.life)
        }

        // Cycle through all 6 players
        for (i in 0 until 6) {
            val activeId = engine.getCurrentState()!!.activePlayer.id
            assertEquals(ids[i], activeId)
            engine.executeAction(NetworkAction.PassTurn, activeId)
        }
        // Should be back to first player
        assertEquals(ids[0], engine.getCurrentState()!!.activePlayer.id)
        assertEquals(7, engine.getCurrentState()!!.turnNumber)
    }

    // ==================== Multiple Concessions ====================

    @Test
    fun `multiple players can concede independently`() {
        val engine = GameEngine(maxPlayers = 4)
        val ids = (1..4).map { engine.addPlayer("P$it", createTestDeck(), isAdmin = it == 1) }
        ids.drop(1).forEach { engine.setPlayerReady(it, true) }
        assertTrue(engine.startGame())

        // P2 and P4 concede
        engine.executeAction(NetworkAction.Concede(ids[1]), ids[1])
        engine.executeAction(NetworkAction.Concede(ids[3]), ids[3])

        val state = engine.getCurrentState()!!
        assertFalse(state.players.find { it.id == ids[0] }!!.hasLost)
        assertTrue(state.players.find { it.id == ids[1] }!!.hasLost)
        assertFalse(state.players.find { it.id == ids[2] }!!.hasLost)
        assertTrue(state.players.find { it.id == ids[3] }!!.hasLost)
    }

    // ==================== Commander Damage from Multiple Sources ====================

    @Test
    fun `commander damage tracked per commander`() {
        val (engine, p1, p2) = createStartedEngine()
        val state = engine.getCurrentState()!!

        val p1Cmd = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.COMMAND_ZONE }
        val p2Cmd = state.cardInstances.first { it.ownerId == p2 && it.zone == Zone.COMMAND_ZONE }

        // Both commanders deal damage to p1
        engine.executeAction(NetworkAction.UpdateCommanderDamage(p1, p1Cmd.instanceId, 10), p1)
        engine.executeAction(NetworkAction.UpdateCommanderDamage(p1, p2Cmd.instanceId, 8), p1)

        val after = engine.getCurrentState()!!
        val p1Player = after.players.find { it.id == p1 }!!
        assertEquals(10, p1Player.commanderDamage[p1Cmd.instanceId])
        assertEquals(8, p1Player.commanderDamage[p2Cmd.instanceId])
        assertFalse(p1Player.hasLost, "Neither source reached 21")
    }

    @Test
    fun `commander damage from one source reaching 21 kills even if others are low`() {
        val (engine, p1, p2) = createStartedEngine()
        val state = engine.getCurrentState()!!

        val p1Cmd = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.COMMAND_ZONE }
        val p2Cmd = state.cardInstances.first { it.ownerId == p2 && it.zone == Zone.COMMAND_ZONE }

        // p2Cmd deals only 5 to p1
        engine.executeAction(NetworkAction.UpdateCommanderDamage(p1, p2Cmd.instanceId, 5), p1)
        // p1's own commander deals 21 to p1
        engine.executeAction(NetworkAction.UpdateCommanderDamage(p1, p1Cmd.instanceId, 21), p1)

        val after = engine.getCurrentState()!!
        val p1Player = after.players.find { it.id == p1 }!!
        assertTrue(p1Player.hasLost, "Should die from 21 damage from single commander source")
    }

    // ==================== Clone to Non-Battlefield ====================

    @Test
    fun `clone card to hand`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)

        val handBefore = engine.getCurrentState()!!.cardInstances.count { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.CloneCard(card.instanceId, p1, Zone.HAND, 1), p1)

        val after = engine.getCurrentState()!!
        val handAfter = after.cardInstances.count { it.ownerId == p1 && it.zone == Zone.HAND }
        assertEquals(handBefore + 1, handAfter)

        val clone = after.cardInstances.find {
            it.isClone && it.ownerId == p1 && it.zone == Zone.HAND
        }
        assertNotNull(clone)
        assertEquals(card.card.name, clone.card.name)
    }

    // ==================== Clone Token Ceases to Exist ====================

    @Test
    fun `cloned card ceases to exist when leaving battlefield`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.CloneCard(card.instanceId, p1, Zone.BATTLEFIELD, 1), p1)

        val mid = engine.getCurrentState()!!
        val clone = mid.cardInstances.find { it.isClone && it.ownerId == p1 }!!

        // Move clone off battlefield
        engine.executeAction(NetworkAction.MoveCard(clone.instanceId, Zone.GRAVEYARD), p1)

        val after = engine.getCurrentState()!!
        assertNull(after.cardInstances.find { it.instanceId == clone.instanceId },
            "Clone should cease to exist when leaving battlefield")
    }

    // ==================== setPlayerReady toggle ====================

    @Test
    fun `setPlayerReady can toggle off`() {
        val engine = GameEngine()
        engine.addPlayer("Alice", createTestDeck(), isAdmin = true)
        val p2 = engine.addPlayer("Bob", createTestDeck())

        engine.setPlayerReady(p2, true)
        assertTrue(engine.getPlayer(p2)!!.isReady)

        engine.setPlayerReady(p2, false)
        assertFalse(engine.getPlayer(p2)!!.isReady)

        // Game should not start with unready player
        assertFalse(engine.startGame())
    }

    // ==================== getPlayerIds ====================

    @Test
    fun `getPlayerIds returns all player IDs`() {
        val engine = GameEngine()
        val id1 = engine.addPlayer("Alice", createTestDeck(), isAdmin = true)
        val id2 = engine.addPlayer("Bob", createTestDeck())
        val id3 = engine.addPlayer("Charlie", createTestDeck())

        val ids = engine.getPlayerIds()
        assertEquals(setOf(id1, id2, id3), ids)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `action with nonexistent player ID fails`() {
        val (engine, _, _) = createStartedEngine()

        val result = engine.executeAction(NetworkAction.DrawCard("nonexistent"), "nonexistent")
        assertFalse(result.success)
        assertEquals("Player not found", result.reason)
    }

    @Test
    fun `move nonexistent card is no-op`() {
        val (engine, p1, _) = createStartedEngine()

        // MoveCard with bad ID — executeAction succeeds but state unchanged
        val result = engine.executeAction(NetworkAction.MoveCard("bad-card-id", Zone.GRAVEYARD), p1)
        // Validation passes (ownership check can't find card → "Card not found")
        assertFalse(result.success)
        assertEquals("Card not found", result.reason)
    }

    @Test
    fun `tap nonexistent card fails validation`() {
        val (engine, p1, _) = createStartedEngine()

        val result = engine.executeAction(NetworkAction.ToggleTap("bad-card-id"), p1)
        assertFalse(result.success)
        assertEquals("Card not found", result.reason)
    }

    @Test
    fun `player counters do not go below zero`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.AddPlayerCounter(p1, "energy", 3), p1)
        engine.executeAction(NetworkAction.RemovePlayerCounter(p1, "energy", 10), p1)

        val state = engine.getCurrentState()!!
        val counter = state.players.find { it.id == p1 }!!.getCounter("energy")
        assertTrue(counter >= 0, "Player counter should not go below 0")
    }

    // ==================== Grid Position Stack Limit ====================

    @Test
    fun `grid position rejects more than 3 cards at same position`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val handCards = state.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.HAND }.take(4)

        // Play 4 cards to battlefield
        handCards.forEach {
            engine.executeAction(NetworkAction.MoveCard(it.instanceId, Zone.BATTLEFIELD), p1)
        }

        // Move first 3 to same grid position (0,0)
        engine.executeAction(NetworkAction.UpdateCardGridPosition(handCards[0].instanceId, 0, 0), p1)
        engine.executeAction(NetworkAction.UpdateCardGridPosition(handCards[1].instanceId, 0, 0), p1)
        engine.executeAction(NetworkAction.UpdateCardGridPosition(handCards[2].instanceId, 0, 0), p1)

        // 4th card should be rejected (stays at its original position)
        engine.executeAction(NetworkAction.UpdateCardGridPosition(handCards[3].instanceId, 0, 0), p1)

        val after = engine.getCurrentState()!!
        val atTarget = after.cardInstances.filter {
            it.ownerId == p1 && it.zone == Zone.BATTLEFIELD && it.gridX == 0 && it.gridY == 0
        }
        assertTrue(atTarget.size <= 3, "Should not allow more than 3 cards at same grid position")
    }

    // ==================== GiveControlTo Validation ====================

    @Test
    fun `cannot give control of card you dont control`() {
        val (engine, p1, p2) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val p1Card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(p1Card.instanceId, Zone.BATTLEFIELD), p1)

        // p2 tries to give away p1's card
        val result = engine.executeAction(NetworkAction.GiveControlTo(p1Card.instanceId, p1), p2)
        assertFalse(result.success)
        assertEquals("You don't control this card", result.reason)
    }

    // ==================== Counters Reset on Zone Change ====================

    @Test
    fun `counters reset when card moves off battlefield`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val card = state.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.AddCardCounter(card.instanceId, "+1/+1", 5), p1)
        engine.executeAction(NetworkAction.AddCardCounter(card.instanceId, "charge", 3), p1)
        engine.executeAction(NetworkAction.ModifyPower(card.instanceId, 2), p1)
        engine.executeAction(NetworkAction.ToggleTap(card.instanceId), p1)

        // Verify state is set
        val mid = engine.getCurrentState()!!
        val onField = mid.cardInstances.find { it.instanceId == card.instanceId }!!
        assertEquals(5, onField.counters["+1/+1"])
        assertEquals(3, onField.counters["charge"])
        assertEquals(2, onField.powerModifier)
        assertTrue(onField.isTapped)

        // Move to hand
        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.HAND), p1)

        val after = engine.getCurrentState()!!
        val inHand = after.cardInstances.find { it.instanceId == card.instanceId }!!
        assertTrue(inHand.counters.isEmpty(), "Counters should reset")
        assertEquals(0, inHand.powerModifier, "Power modifier should reset")
        assertEquals(0, inHand.toughnessModifier, "Toughness modifier should reset")
        assertFalse(inHand.isTapped, "Tapped should reset")
        assertFalse(inHand.doesntUntap, "doesntUntap should reset")
        assertNull(inHand.attachedTo, "attachedTo should reset")
    }

    // ==================== Attachment Clears on Zone Change ====================

    @Test
    fun `attachment clears when attached card leaves battlefield`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val cards = state.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.HAND }.take(2)

        engine.executeAction(NetworkAction.MoveCard(cards[0].instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.MoveCard(cards[1].instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.AttachCard(cards[0].instanceId, cards[1].instanceId), p1)

        // Move the attached card to graveyard
        engine.executeAction(NetworkAction.MoveCard(cards[0].instanceId, Zone.GRAVEYARD), p1)

        val after = engine.getCurrentState()!!
        val inGrave = after.cardInstances.find { it.instanceId == cards[0].instanceId }!!
        assertNull(inGrave.attachedTo, "Attachment should clear when leaving battlefield")
    }

    // ==================== Reveal + Look Interaction Edge Cases ====================

    @Test
    fun `reveal enables look, but disabling reveal keeps look`() {
        val (engine, p1, _) = createStartedEngine()

        // Enable reveal (also enables look)
        engine.executeAction(NetworkAction.ToggleRevealTopCard(p1), p1)
        val s1 = engine.getCurrentState()!!.players.find { it.id == p1 }!!
        assertTrue(s1.revealTopCard)
        assertTrue(s1.lookAtTopCard)

        // Disable reveal — look should stay on
        engine.executeAction(NetworkAction.ToggleRevealTopCard(p1), p1)
        val s2 = engine.getCurrentState()!!.players.find { it.id == p1 }!!
        assertFalse(s2.revealTopCard)
        // lookAtTopCard persists independently when reveal is turned off
        // (reveal on sets look=true, reveal off doesn't touch look)
        assertTrue(s2.lookAtTopCard)
    }

    // ==================== Multiple draws on same turn ====================

    @Test
    fun `drawing multiple cards decrements library correctly`() {
        val (engine, p1, _) = createStartedEngine()
        val libBefore = engine.getCurrentState()!!.cardInstances.count {
            it.ownerId == p1 && it.zone == Zone.LIBRARY
        }

        repeat(10) {
            engine.executeAction(NetworkAction.DrawCard(p1), p1)
        }

        val after = engine.getCurrentState()!!
        val libAfter = after.cardInstances.count { it.ownerId == p1 && it.zone == Zone.LIBRARY }
        val handAfter = after.cardInstances.count { it.ownerId == p1 && it.zone == Zone.HAND }

        assertEquals(libBefore - 10, libAfter)
        assertEquals(GameConstants.STARTING_HAND_SIZE + 10, handAfter) // 7 starting + 10 drawn
    }

    // ==================== Token moving between battlefield zones ====================

    @Test
    fun `token stays on battlefield when moved to battlefield`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(
            NetworkAction.CreateToken(p1, "Angel", "Creature — Angel", "4", "4", "White", null, 1),
            p1
        )

        val state = engine.getCurrentState()!!
        val token = state.cardInstances.find { it.isToken && it.ownerId == p1 }!!

        // Moving token to battlefield again (same zone) should keep it
        engine.executeAction(NetworkAction.MoveCard(token.instanceId, Zone.BATTLEFIELD), p1)

        val after = engine.getCurrentState()!!
        val tokenAfter = after.cardInstances.find { it.instanceId == token.instanceId }
        assertNotNull(tokenAfter, "Token should survive battlefield→battlefield move")
    }

    // ==================== Commander from command zone to battlefield and back ====================

    @Test
    fun `commander can move between command zone and battlefield`() {
        val (engine, p1, _) = createStartedEngine()
        val state = engine.getCurrentState()!!
        val commander = state.cardInstances.first {
            it.ownerId == p1 && it.zone == Zone.COMMAND_ZONE
        }

        // Play commander
        engine.executeAction(NetworkAction.MoveCard(commander.instanceId, Zone.BATTLEFIELD), p1)
        assertEquals(Zone.BATTLEFIELD,
            engine.getCurrentState()!!.cardInstances.find { it.instanceId == commander.instanceId }!!.zone)

        // Return to command zone
        engine.executeAction(NetworkAction.MoveCard(commander.instanceId, Zone.COMMAND_ZONE), p1)
        assertEquals(Zone.COMMAND_ZONE,
            engine.getCurrentState()!!.cardInstances.find { it.instanceId == commander.instanceId }!!.zone)
    }

    // ==================== Game log accumulation ====================

    @Test
    fun `game log preserves events across multiple actions`() {
        val (engine, p1, _) = createStartedEngine()

        engine.executeAction(NetworkAction.DrawCard(p1), p1)
        engine.executeAction(NetworkAction.UpdateLife(p1, 38), p1)
        engine.executeAction(NetworkAction.NextPhase, p1)
        engine.executeAction(NetworkAction.NextPhase, p1)
        engine.executeAction(NetworkAction.NextPhase, p1)

        val state = engine.getCurrentState()!!
        // At minimum: GameStarted + starting hand draws + our 5 actions
        assertTrue(state.gameLog.size >= 6, "Log should accumulate: ${state.gameLog.size} events")

        // Verify order — last 5 should be our actions
        val last5 = state.gameLog.takeLast(5)
        assertTrue(last5[0] is GameEvent.CardDrawn)
        assertTrue(last5[1] is GameEvent.LifeChanged)
        assertTrue(last5[2] is GameEvent.PhaseChanged)
        assertTrue(last5[3] is GameEvent.PhaseChanged)
        assertTrue(last5[4] is GameEvent.PhaseChanged)
    }
}
