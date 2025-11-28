package com.dustinmcafee.dongadeuce.network

import com.dustinmcafee.dongadeuce.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.*

class GameMessageTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun createTestDeck(): Deck {
        return Deck(
            name = "Test Deck",
            commander = Card(name = "Test Commander", type = "Legendary Creature"),
            cards = (1..99).map { Card(name = "Card $it") }
        )
    }

    private fun createTestGameState(): GameState {
        return GameState(
            gameId = "test-game-123",
            players = listOf(
                Player(id = "player-1", name = "Player 1"),
                Player(id = "player-2", name = "Player 2")
            ),
            cardInstances = emptyList(),
            activePlayerIndex = 0,
            turnNumber = 1,
            phase = GamePhase.UNTAP
        )
    }

    // ==================== Connection Messages ====================

    @Test
    fun `PlayerJoin serializes and deserializes correctly`() {
        val original = GameMessage.PlayerJoin(
            playerName = "Test Player",
            deck = createTestDeck()
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
        assertTrue(deserialized is GameMessage.PlayerJoin)
        assertEquals("Test Player", (deserialized as GameMessage.PlayerJoin).playerName)
    }

    @Test
    fun `PlayerJoined serializes and deserializes correctly`() {
        val original = GameMessage.PlayerJoined(
            playerId = "player-123",
            playerName = "Test Player"
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
        assertTrue(deserialized is GameMessage.PlayerJoined)
    }

    @Test
    fun `PlayerLeft serializes and deserializes correctly`() {
        val original = GameMessage.PlayerLeft(
            playerId = "player-123",
            playerName = "Test Player",
            reason = "Disconnected"
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun `PlayerReady serializes and deserializes correctly`() {
        val original = GameMessage.PlayerReady(
            playerId = "player-123",
            isReady = true
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
    }

    // ==================== Lobby Messages ====================

    @Test
    fun `LobbyState serializes and deserializes correctly`() {
        val original = GameMessage.LobbyState(
            players = listOf(
                LobbyPlayer(id = "1", name = "Player 1", hasDeck = true, isReady = true, isHost = true),
                LobbyPlayer(id = "2", name = "Player 2", hasDeck = true, isReady = false, isHost = false)
            ),
            hostId = "1",
            maxPlayers = 4
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
        assertTrue(deserialized is GameMessage.LobbyState)
        assertEquals(2, (deserialized as GameMessage.LobbyState).players.size)
    }

    @Test
    fun `GameStarting serializes and deserializes correctly`() {
        val original = GameMessage.GameStarting(
            gameState = createTestGameState()
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun `KickPlayer serializes and deserializes correctly`() {
        val original = GameMessage.KickPlayer(
            playerId = "player-123",
            reason = "AFK"
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
    }

    // ==================== Game Action Messages ====================

    @Test
    fun `GameAction with DrawCard serializes correctly`() {
        val original = GameMessage.GameAction(
            action = NetworkAction.DrawCard("player-1"),
            playerId = "player-1",
            actionId = 123456789L
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
        assertTrue(deserialized is GameMessage.GameAction)
        assertTrue((deserialized as GameMessage.GameAction).action is NetworkAction.DrawCard)
    }

    @Test
    fun `GameAction with MoveCard serializes correctly`() {
        val original = GameMessage.GameAction(
            action = NetworkAction.MoveCard("card-123", Zone.BATTLEFIELD),
            playerId = "player-1",
            actionId = 123456789L
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun `StateUpdate serializes and deserializes correctly`() {
        val original = GameMessage.StateUpdate(
            gameState = createTestGameState(),
            lastActionId = 123456789L
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun `ActionRejected serializes and deserializes correctly`() {
        val original = GameMessage.ActionRejected(
            actionId = 123456789L,
            reason = "Invalid action"
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
    }

    // ==================== Connection Management ====================

    @Test
    fun `Ping serializes and deserializes correctly`() {
        val original = GameMessage.Ping(timestamp = 123456789L)

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun `Pong serializes and deserializes correctly`() {
        val original = GameMessage.Pong(
            timestamp = 123456789L,
            receivedAt = 123456800L
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun `Pause serializes and deserializes correctly`() {
        val original = GameMessage.Pause(
            reason = "Player disconnected",
            disconnectedPlayerId = "player-2"
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun `Resume serializes and deserializes correctly`() {
        val original = GameMessage.Resume(reconnectedPlayerId = "player-2")

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
    }

    // ==================== Chat Messages ====================

    @Test
    fun `Chat serializes and deserializes correctly`() {
        val original = GameMessage.Chat(
            playerId = "player-1",
            playerName = "Player 1",
            message = "Hello everyone!",
            timestamp = 123456789L
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
    }

    // ==================== Error Messages ====================

    @Test
    fun `Error serializes and deserializes correctly`() {
        val original = GameMessage.Error(
            code = ErrorCode.INVALID_ACTION,
            message = "Cannot perform this action"
        )

        val serialized = json.encodeToString<GameMessage>(original)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun `All ErrorCode values can be serialized`() {
        for (code in ErrorCode.values()) {
            val error = GameMessage.Error(code = code, message = "Test")
            val serialized = json.encodeToString<GameMessage>(error)
            val deserialized = json.decodeFromString<GameMessage>(serialized)
            assertEquals(error, deserialized)
        }
    }

    // ==================== LobbyPlayer Tests ====================

    @Test
    fun `LobbyPlayer serializes correctly`() {
        val original = LobbyPlayer(
            id = "player-123",
            name = "Test Player",
            hasDeck = true,
            isReady = true,
            isHost = true
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<LobbyPlayer>(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun `LobbyPlayer with defaults serializes correctly`() {
        val original = LobbyPlayer(
            id = "player-123",
            name = "Test Player",
            hasDeck = false,
            isReady = false
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<LobbyPlayer>(serialized)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
        assertEquals(false, deserialized.isHost)
    }
}
