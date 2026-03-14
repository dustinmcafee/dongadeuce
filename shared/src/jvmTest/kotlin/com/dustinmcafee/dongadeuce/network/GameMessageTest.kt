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
        assertEquals("Test Player", deserialized.playerName)
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
        assertEquals(2, deserialized.players.size)
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
        assertTrue(deserialized.action is NetworkAction.DrawCard)
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

    // ==================== Cross-Platform Compatibility ====================
    // These tests validate that Android and PC clients can communicate
    // by testing backward-compatible deserialization of messages with
    // missing new fields and extra unknown fields.

    @Test
    fun `LobbyPlayer without isAdmin field deserializes with default`() {
        // Simulates an old client (pre-v6) sending a LobbyPlayer without isAdmin
        val oldFormatJson = """{"id":"p1","name":"Alice","hasDeck":true,"isReady":true,"isHost":true}"""
        val deserialized = json.decodeFromString<LobbyPlayer>(oldFormatJson)

        assertEquals("p1", deserialized.id)
        assertEquals("Alice", deserialized.name)
        assertTrue(deserialized.isHost)
        assertFalse(deserialized.isAdmin, "isAdmin should default to false when missing")
    }

    @Test
    fun `LobbyPlayer with isAdmin field deserializes correctly`() {
        // New client sends isAdmin
        val newFormatJson = """{"id":"p1","name":"Alice","hasDeck":true,"isReady":true,"isHost":true,"isAdmin":true}"""
        val deserialized = json.decodeFromString<LobbyPlayer>(newFormatJson)

        assertTrue(deserialized.isAdmin)
        assertTrue(deserialized.isHost)
    }

    @Test
    fun `LobbyState without adminId and gameCode deserializes with defaults`() {
        // Old client sends LobbyState without new fields
        val oldFormatJson = """{"type":"com.dustinmcafee.dongadeuce.network.GameMessage.LobbyState","players":[],"hostId":"h1","maxPlayers":4}"""
        val deserialized = json.decodeFromString<GameMessage.LobbyState>(oldFormatJson)

        assertEquals("h1", deserialized.hostId)
        assertEquals("", deserialized.adminId, "adminId should default to empty string")
        assertEquals("", deserialized.gameCode, "gameCode should default to empty string")
        assertEquals(4, deserialized.maxPlayers)
    }

    @Test
    fun `LobbyState with adminId and gameCode deserializes correctly`() {
        val newFormatJson = """{"players":[],"hostId":"h1","adminId":"a1","gameCode":"ABC123","maxPlayers":6}"""
        val deserialized = json.decodeFromString<GameMessage.LobbyState>(newFormatJson)

        assertEquals("h1", deserialized.hostId)
        assertEquals("a1", deserialized.adminId)
        assertEquals("ABC123", deserialized.gameCode)
        assertEquals(6, deserialized.maxPlayers)
    }

    @Test
    fun `unknown fields in JSON are ignored (forward compatibility)`() {
        // Future client might add fields the current client doesn't know about
        val futureJson = """{"id":"p1","name":"Alice","hasDeck":true,"isReady":true,"isHost":false,"isAdmin":false,"avatarUrl":"https://example.com/avatar.png","rating":1500}"""
        val deserialized = json.decodeFromString<LobbyPlayer>(futureJson)

        assertEquals("p1", deserialized.id)
        assertEquals("Alice", deserialized.name)
        // Should not crash — unknown fields ignored
    }

    @Test
    fun `new message types CreateGame JoinGame GameCreated serialize correctly`() {
        val createGame = GameMessage.CreateGame("Alice", createTestDeck(), 4)
        val serialized1 = json.encodeToString<GameMessage>(createGame)
        val deserialized1 = json.decodeFromString<GameMessage>(serialized1)
        assertEquals(createGame, deserialized1)

        val joinGame = GameMessage.JoinGame("ABC123", "Bob", createTestDeck())
        val serialized2 = json.encodeToString<GameMessage>(joinGame)
        val deserialized2 = json.decodeFromString<GameMessage>(serialized2)
        assertEquals(joinGame, deserialized2)

        val gameCreated = GameMessage.GameCreated("XYZ789", "player-1")
        val serialized3 = json.encodeToString<GameMessage>(gameCreated)
        val deserialized3 = json.decodeFromString<GameMessage>(serialized3)
        assertEquals(gameCreated, deserialized3)
    }

    @Test
    fun `NetworkAction round-trips through GameMessage GameAction`() {
        // All action types that can be sent between Android and PC
        val actions = listOf(
            NetworkAction.DrawCard("p1"),
            NetworkAction.MoveCard("card1", Zone.BATTLEFIELD),
            NetworkAction.ToggleTap("card1"),
            NetworkAction.FlipCard("card1"),
            NetworkAction.UpdateLife("p1", 35),
            NetworkAction.NextPhase,
            NetworkAction.PassTurn,
            NetworkAction.SetPhase(GamePhase.MAIN_1),
            NetworkAction.Concede("p1"),
            NetworkAction.UntapAll("p1"),
            NetworkAction.AddCardCounter("card1", "+1/+1", 3),
            NetworkAction.RemoveCardCounter("card1", "+1/+1", 1),
            NetworkAction.SetCardCounter("card1", "charge", 5),
            NetworkAction.AddPlayerCounter("p1", "poison", 1),
            NetworkAction.RemovePlayerCounter("p1", "energy", 2),
            NetworkAction.SetPlayerCounter("p1", "experience", 10),
            NetworkAction.CreateToken("p1", "Goblin", "Creature", "1", "1", "Red", null, 3),
            NetworkAction.CloneCard("card1", "p1", Zone.BATTLEFIELD, 1),
            NetworkAction.MillCards("p1", 5),
            NetworkAction.Mulligan("p1"),
            NetworkAction.ShuffleLibrary("p1"),
            NetworkAction.MoveCardToTopOfLibrary("card1"),
            NetworkAction.MoveCardToBottomOfLibrary("card1"),
            NetworkAction.MoveCardToLibraryPosition("card1", 3),
            NetworkAction.MoveCardToLibraryPositionFromBottom("card1", 2),
            NetworkAction.MoveTopCardsToZone("p1", 3, Zone.GRAVEYARD),
            NetworkAction.MoveBottomCardsToZone("p1", 2, Zone.EXILE),
            NetworkAction.MoveBottomCardToTop("p1"),
            NetworkAction.ShuffleTopCards("p1", 10),
            NetworkAction.ShuffleBottomCards("p1", 5),
            NetworkAction.ModifyPower("card1", 2),
            NetworkAction.ModifyToughness("card1", -1),
            NetworkAction.ModifyPowerToughness("card1", 3),
            NetworkAction.SetPowerToughness("card1", 5, 5),
            NetworkAction.ResetPowerToughness("card1"),
            NetworkAction.FlowPower("card1"),
            NetworkAction.FlowToughness("card1"),
            NetworkAction.ToggleDoesntUntap("card1"),
            NetworkAction.SetAnnotation("card1", "test note"),
            NetworkAction.ToggleFaceDown("card1"),
            NetworkAction.PlayFaceDown("card1"),
            NetworkAction.AttachCard("card1", "card2"),
            NetworkAction.DetachCard("card1"),
            NetworkAction.GiveControlTo("card1", "p2"),
            NetworkAction.UpdateCommanderDamage("p1", "cmd1", 7),
            NetworkAction.UpdateCardGridPosition("card1", 2, 1),
            NetworkAction.LogDieRoll("p1", "d20", 17),
            NetworkAction.SendChatMessage("p1", "hello"),
            NetworkAction.ToggleRevealTopCard("p1"),
            NetworkAction.ToggleLookAtTopCard("p1")
        )

        for (action in actions) {
            val message = GameMessage.GameAction(action, "p1", 12345L)
            val serialized = json.encodeToString<GameMessage>(message)
            val deserialized = json.decodeFromString<GameMessage>(serialized)

            assertTrue(deserialized is GameMessage.GameAction,
                "Failed to round-trip ${action::class.simpleName}")
            assertEquals(action, (deserialized as GameMessage.GameAction).action,
                "Action mismatch for ${action::class.simpleName}")
        }
    }

    @Test
    fun `GameState with all zones round-trips correctly`() {
        // Build a realistic game state with cards in every zone
        val card = Card(name = "Sol Ring", type = "Artifact", manaCost = "{1}")
        val dfcCard = Card(
            name = "Delver of Secrets",
            type = "Creature",
            backFaceName = "Insectile Aberration",
            backFaceImageUri = "https://example.com/back.jpg"
        )

        val instances = listOf(
            CardInstance(card = card, ownerId = "p1", zone = Zone.HAND),
            CardInstance(card = card, ownerId = "p1", zone = Zone.LIBRARY),
            CardInstance(card = card, ownerId = "p1", zone = Zone.BATTLEFIELD, isTapped = true,
                counters = mapOf("+1/+1" to 3), gridX = 0, gridY = 1),
            CardInstance(card = card, ownerId = "p1", zone = Zone.GRAVEYARD),
            CardInstance(card = card, ownerId = "p1", zone = Zone.EXILE),
            CardInstance(card = card, ownerId = "p1", zone = Zone.COMMAND_ZONE),
            CardInstance(card = dfcCard, ownerId = "p1", zone = Zone.BATTLEFIELD, isFlipped = true),
            CardInstance(card = card, ownerId = "p1", zone = Zone.BATTLEFIELD, isFaceDown = true),
            CardInstance(card = card, ownerId = "p1", zone = Zone.BATTLEFIELD, isToken = true),
            CardInstance(card = card, ownerId = "p1", zone = Zone.BATTLEFIELD, isClone = true,
                clonedFromId = "original-id")
        )

        val state = GameState(
            gameId = "test",
            players = listOf(
                Player(id = "p1", name = "Alice", life = 35,
                    commanderDamage = mapOf("cmd1" to 14),
                    counters = mapOf("poison" to 7, "energy" to 3)),
                Player(id = "p2", name = "Bob", life = 0, hasLost = true)
            ),
            cardInstances = instances,
            activePlayerIndex = 0,
            turnNumber = 5,
            phase = GamePhase.COMBAT_DAMAGE
        )

        val message = GameMessage.StateUpdate(state, 99L)
        val serialized = json.encodeToString<GameMessage>(message)
        val deserialized = json.decodeFromString<GameMessage>(serialized)

        assertTrue(deserialized is GameMessage.StateUpdate)
        val roundTripped = (deserialized as GameMessage.StateUpdate).gameState

        assertEquals(state.gameId, roundTripped.gameId)
        assertEquals(state.players.size, roundTripped.players.size)
        assertEquals(state.cardInstances.size, roundTripped.cardInstances.size)
        assertEquals(state.turnNumber, roundTripped.turnNumber)
        assertEquals(state.phase, roundTripped.phase)
        assertEquals(35, roundTripped.players[0].life)
        assertEquals(7, roundTripped.players[0].getCounter("poison"))
        assertTrue(roundTripped.players[1].hasLost)

        // Verify card states survived round-trip
        val tappedCard = roundTripped.cardInstances.find { it.isTapped }
        assertNotNull(tappedCard)
        assertEquals(3, tappedCard.counters["+1/+1"])

        val flippedDfc = roundTripped.cardInstances.find { it.isFlipped }
        assertNotNull(flippedDfc)
        assertEquals("Insectile Aberration", flippedDfc.card.backFaceName)

        val faceDown = roundTripped.cardInstances.find { it.isFaceDown }
        assertNotNull(faceDown)

        val token = roundTripped.cardInstances.find { it.isToken }
        assertNotNull(token)

        val clone = roundTripped.cardInstances.find { it.isClone }
        assertNotNull(clone)
        assertEquals("original-id", clone.clonedFromId)
    }
}
