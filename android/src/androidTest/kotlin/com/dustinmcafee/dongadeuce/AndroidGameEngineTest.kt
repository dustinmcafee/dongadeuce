package com.dustinmcafee.dongadeuce

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.network.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumented GameEngine tests.
 * Validates that the game engine produces identical results on
 * Android's Dalvik/ART runtime as on desktop JVM.
 */
@RunWith(AndroidJUnit4::class)
class AndroidGameEngineTest {

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
        val p1 = engine.addPlayer("Alice", createTestDeck(), isAdmin = true)
        val p2 = engine.addPlayer("Bob", createTestDeck())
        engine.setPlayerReady(p2, true)
        assertTrue(engine.startGame())
        return Triple(engine, p1, p2)
    }

    @Test
    fun addPlayer_assignsUniqueIds() {
        val engine = GameEngine()
        val id1 = engine.addPlayer("Alice", createTestDeck(), isAdmin = true)
        val id2 = engine.addPlayer("Bob", createTestDeck())
        assertNotEquals(id1, id2)
        assertEquals(2, engine.getPlayerCount())
    }

    @Test
    fun startGame_createsCorrectInitialState() {
        val (engine, p1, p2) = createStartedEngine()
        val state = engine.getCurrentState()!!

        assertEquals(2, state.players.size)
        assertEquals(GameConstants.STARTING_LIFE, state.players[0].life)
        assertEquals(1, state.turnNumber)
        assertEquals(GamePhase.UNTAP, state.phase)

        // Commander in command zone
        val cmds = state.cardInstances.filter { it.ownerId == p1 && it.zone == Zone.COMMAND_ZONE }
        assertEquals(1, cmds.size)

        // 7 cards in hand
        val hand = state.cardInstances.count { it.ownerId == p1 && it.zone == Zone.HAND }
        assertEquals(7, hand)

        // 92 in library
        val lib = state.cardInstances.count { it.ownerId == p1 && it.zone == Zone.LIBRARY }
        assertEquals(92, lib)

        // 100 total
        val total = state.cardInstances.count { it.ownerId == p1 }
        assertEquals(100, total)
    }

    @Test
    fun drawCard_movesFromLibraryToHand() {
        val (engine, p1, _) = createStartedEngine()
        val result = engine.executeAction(NetworkAction.DrawCard(p1), p1)
        assertTrue(result.success)

        val state = engine.getCurrentState()!!
        assertEquals(8, state.cardInstances.count { it.ownerId == p1 && it.zone == Zone.HAND })
        assertEquals(91, state.cardInstances.count { it.ownerId == p1 && it.zone == Zone.LIBRARY })
    }

    @Test
    fun moveCard_handToBattlefield() {
        val (engine, p1, _) = createStartedEngine()
        val card = engine.getCurrentState()!!.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }

        val result = engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        assertTrue(result.success)
        assertEquals(Zone.BATTLEFIELD, engine.getCurrentState()!!.cardInstances.find { it.instanceId == card.instanceId }!!.zone)
    }

    @Test
    fun toggleTap_works() {
        val (engine, p1, _) = createStartedEngine()
        val card = engine.getCurrentState()!!.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }
        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)

        engine.executeAction(NetworkAction.ToggleTap(card.instanceId), p1)
        assertTrue(engine.getCurrentState()!!.cardInstances.find { it.instanceId == card.instanceId }!!.isTapped)

        engine.executeAction(NetworkAction.ToggleTap(card.instanceId), p1)
        assertFalse(engine.getCurrentState()!!.cardInstances.find { it.instanceId == card.instanceId }!!.isTapped)
    }

    @Test
    fun updateLife_setsValue() {
        val (engine, p1, _) = createStartedEngine()
        engine.executeAction(NetworkAction.UpdateLife(p1, 25), p1)
        assertEquals(25, engine.getCurrentState()!!.players.find { it.id == p1 }!!.life)
    }

    @Test
    fun updateLife_toZero_marksHasLost() {
        val (engine, p1, _) = createStartedEngine()
        engine.executeAction(NetworkAction.UpdateLife(p1, 0), p1)
        assertTrue(engine.getCurrentState()!!.players.find { it.id == p1 }!!.hasLost)
    }

    @Test
    fun passTurn_changesActivePlayer() {
        val (engine, p1, p2) = createStartedEngine()
        engine.executeAction(NetworkAction.PassTurn, p1)
        assertEquals(p2, engine.getCurrentState()!!.activePlayer.id)
        assertEquals(2, engine.getCurrentState()!!.turnNumber)
    }

    @Test
    fun nextPhase_advancesPhase() {
        val (engine, p1, _) = createStartedEngine()
        engine.executeAction(NetworkAction.NextPhase, p1)
        assertEquals(GamePhase.UPKEEP, engine.getCurrentState()!!.phase)
    }

    @Test
    fun concede_setsLifeZeroAndHasLost() {
        val (engine, _, p2) = createStartedEngine()
        engine.executeAction(NetworkAction.Concede(p2), p2)
        val player = engine.getCurrentState()!!.players.find { it.id == p2 }!!
        assertEquals(0, player.life)
        assertTrue(player.hasLost)
    }

    @Test
    fun createToken_putsOnBattlefield() {
        val (engine, p1, _) = createStartedEngine()
        engine.executeAction(
            NetworkAction.CreateToken(p1, "Goblin", "Creature", "1", "1", "Red", null, 3), p1
        )
        val tokens = engine.getCurrentState()!!.cardInstances.filter { it.isToken && it.ownerId == p1 }
        assertEquals(3, tokens.size)
        tokens.forEach { assertEquals(Zone.BATTLEFIELD, it.zone) }
    }

    @Test
    fun token_ceasesToExist_whenLeavingBattlefield() {
        val (engine, p1, _) = createStartedEngine()
        engine.executeAction(
            NetworkAction.CreateToken(p1, "Soldier", "Creature", "1", "1", "White", null, 1), p1
        )
        val token = engine.getCurrentState()!!.cardInstances.find { it.isToken }!!
        engine.executeAction(NetworkAction.MoveCard(token.instanceId, Zone.GRAVEYARD), p1)
        assertNull(engine.getCurrentState()!!.cardInstances.find { it.instanceId == token.instanceId })
    }

    @Test
    fun addCardCounter_works() {
        val (engine, p1, _) = createStartedEngine()
        val card = engine.getCurrentState()!!.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }
        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.AddCardCounter(card.instanceId, "+1/+1", 3), p1)
        assertEquals(3, engine.getCurrentState()!!.cardInstances.find { it.instanceId == card.instanceId }!!.counters["+1/+1"])
    }

    @Test
    fun addPlayerCounter_works() {
        val (engine, p1, _) = createStartedEngine()
        engine.executeAction(NetworkAction.AddPlayerCounter(p1, "poison", 4), p1)
        assertEquals(4, engine.getCurrentState()!!.players.find { it.id == p1 }!!.getCounter("poison"))
    }

    @Test
    fun commanderDamage_at21_kills() {
        val (engine, p1, p2) = createStartedEngine()
        val cmd = engine.getCurrentState()!!.cardInstances.first { it.ownerId == p1 && it.zone == Zone.COMMAND_ZONE }
        engine.executeAction(NetworkAction.UpdateCommanderDamage(p2, cmd.instanceId, 21), p1)
        assertTrue(engine.getCurrentState()!!.players.find { it.id == p2 }!!.hasLost)
    }

    @Test
    fun millCards_movesToGraveyard() {
        val (engine, p1, _) = createStartedEngine()
        engine.executeAction(NetworkAction.MillCards(p1, 5), p1)
        assertEquals(5, engine.getCurrentState()!!.cardInstances.count { it.ownerId == p1 && it.zone == Zone.GRAVEYARD })
    }

    @Test
    fun mulligan_returnsHandAndDrawsNew() {
        val (engine, p1, _) = createStartedEngine()
        engine.executeAction(NetworkAction.Mulligan(p1), p1)
        assertEquals(7, engine.getCurrentState()!!.cardInstances.count { it.ownerId == p1 && it.zone == Zone.HAND })
    }

    @Test
    fun flipCard_togglesState() {
        val (engine, p1, _) = createStartedEngine()
        val card = engine.getCurrentState()!!.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }
        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        engine.executeAction(NetworkAction.FlipCard(card.instanceId), p1)
        assertTrue(engine.getCurrentState()!!.cardInstances.find { it.instanceId == card.instanceId }!!.isFlipped)
        engine.executeAction(NetworkAction.FlipCard(card.instanceId), p1)
        assertFalse(engine.getCurrentState()!!.cardInstances.find { it.instanceId == card.instanceId }!!.isFlipped)
    }

    @Test
    fun validation_cannotDrawForOtherPlayer() {
        val (engine, p1, p2) = createStartedEngine()
        val result = engine.executeAction(NetworkAction.DrawCard(p2), p1)
        assertFalse(result.success)
        assertEquals("Cannot draw for another player", result.reason)
    }

    @Test
    fun validation_notYourTurn() {
        val (engine, _, p2) = createStartedEngine()
        val result = engine.executeAction(NetworkAction.PassTurn, p2)
        assertFalse(result.success)
        assertEquals("Not your turn", result.reason)
    }

    @Test
    fun validation_cannotTapOtherPlayersCard() {
        val (engine, p1, p2) = createStartedEngine()
        val card = engine.getCurrentState()!!.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }
        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.BATTLEFIELD), p1)
        val result = engine.executeAction(NetworkAction.ToggleTap(card.instanceId), p2)
        assertFalse(result.success)
    }

    @Test
    fun pauseAndResume_work() {
        val (engine, p1, _) = createStartedEngine()
        engine.pauseGame("test")
        assertTrue(engine.isPaused.value)

        val result = engine.executeAction(NetworkAction.DrawCard(p1), p1)
        assertFalse(result.success)

        engine.resumeGame()
        assertFalse(engine.isPaused.value)

        val result2 = engine.executeAction(NetworkAction.DrawCard(p1), p1)
        assertTrue(result2.success)
    }

    @Test
    fun totalCardCount_preservedThroughOperations() {
        val (engine, p1, p2) = createStartedEngine()

        engine.executeAction(NetworkAction.DrawCard(p1), p1)
        engine.executeAction(NetworkAction.DrawCard(p1), p1)
        engine.executeAction(NetworkAction.MillCards(p1, 3), p1)
        val card = engine.getCurrentState()!!.cardInstances.first { it.ownerId == p1 && it.zone == Zone.HAND }
        engine.executeAction(NetworkAction.MoveCard(card.instanceId, Zone.EXILE), p1)

        val state = engine.getCurrentState()!!
        assertEquals(100, state.cardInstances.count { it.ownerId == p1 })
        assertEquals(100, state.cardInstances.count { it.ownerId == p2 })
    }
}
