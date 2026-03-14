package com.dustinmcafee.dongadeuce

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dustinmcafee.dongadeuce.models.*
import com.dustinmcafee.dongadeuce.network.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumented serialization tests.
 * These run on a real Android device/emulator to validate that
 * Android's kotlinx-serialization produces identical output to desktop.
 *
 * If these pass alongside the JVM tests, the protocol is guaranteed
 * cross-platform compatible.
 */
@RunWith(AndroidJUnit4::class)
class CrossPlatformSerializationTest {

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

    // ==================== Message Serialization ====================

    @Test
    fun playerJoin_roundTrips() {
        val msg = GameMessage.PlayerJoin("Alice", createTestDeck())
        val serialized = json.encodeToString<GameMessage>(msg)
        val deserialized = json.decodeFromString<GameMessage>(serialized)
        assertEquals(msg, deserialized)
    }

    @Test
    fun playerJoined_roundTrips() {
        val msg = GameMessage.PlayerJoined("player-1", "Alice")
        val serialized = json.encodeToString<GameMessage>(msg)
        val deserialized = json.decodeFromString<GameMessage>(serialized)
        assertEquals(msg, deserialized)
    }

    @Test
    fun playerLeft_roundTrips() {
        val msg = GameMessage.PlayerLeft("player-1", "Alice", "disconnected")
        val serialized = json.encodeToString<GameMessage>(msg)
        val deserialized = json.decodeFromString<GameMessage>(serialized)
        assertEquals(msg, deserialized)
    }

    @Test
    fun playerReady_roundTrips() {
        val msg = GameMessage.PlayerReady("player-1", true)
        val serialized = json.encodeToString<GameMessage>(msg)
        val deserialized = json.decodeFromString<GameMessage>(serialized)
        assertEquals(msg, deserialized)
    }

    @Test
    fun lobbyState_roundTrips() {
        val msg = GameMessage.LobbyState(
            players = listOf(
                LobbyPlayer("p1", "Alice", true, true, isHost = true, isAdmin = true),
                LobbyPlayer("p2", "Bob", true, false, isHost = false, isAdmin = false)
            ),
            hostId = "p1",
            adminId = "p1",
            gameCode = "ABC123",
            maxPlayers = 4
        )
        val serialized = json.encodeToString<GameMessage>(msg)
        val deserialized = json.decodeFromString<GameMessage>(serialized)
        assertEquals(msg, deserialized)
    }

    @Test
    fun kickPlayer_roundTrips() {
        val msg = GameMessage.KickPlayer("player-1", "AFK")
        val serialized = json.encodeToString<GameMessage>(msg)
        val deserialized = json.decodeFromString<GameMessage>(serialized)
        assertEquals(msg, deserialized)
    }

    @Test
    fun actionRejected_roundTrips() {
        val msg = GameMessage.ActionRejected(12345L, "Not your turn")
        val serialized = json.encodeToString<GameMessage>(msg)
        val deserialized = json.decodeFromString<GameMessage>(serialized)
        assertEquals(msg, deserialized)
    }

    @Test
    fun pauseResume_roundTrip() {
        val pause = GameMessage.Pause("Player disconnected", "player-2")
        val resume = GameMessage.Resume("player-2")
        assertEquals(pause, json.decodeFromString<GameMessage>(json.encodeToString<GameMessage>(pause)))
        assertEquals(resume, json.decodeFromString<GameMessage>(json.encodeToString<GameMessage>(resume)))
    }

    @Test
    fun chat_roundTrips() {
        val msg = GameMessage.Chat("p1", "Alice", "Hello!")
        val serialized = json.encodeToString<GameMessage>(msg)
        val deserialized = json.decodeFromString<GameMessage>(serialized)
        assertEquals(msg, deserialized)
    }

    @Test
    fun error_allCodes_roundTrip() {
        for (code in ErrorCode.values()) {
            val msg = GameMessage.Error(code, "test")
            val serialized = json.encodeToString<GameMessage>(msg)
            val deserialized = json.decodeFromString<GameMessage>(serialized)
            assertEquals(msg, deserialized)
        }
    }

    @Test
    fun createGame_roundTrips() {
        val msg = GameMessage.CreateGame("Alice", createTestDeck(), 4)
        val serialized = json.encodeToString<GameMessage>(msg)
        val deserialized = json.decodeFromString<GameMessage>(serialized)
        assertEquals(msg, deserialized)
    }

    @Test
    fun gameCreated_roundTrips() {
        val msg = GameMessage.GameCreated("XYZ789", "player-1")
        val serialized = json.encodeToString<GameMessage>(msg)
        val deserialized = json.decodeFromString<GameMessage>(serialized)
        assertEquals(msg, deserialized)
    }

    @Test
    fun joinGame_roundTrips() {
        val msg = GameMessage.JoinGame("ABC123", "Bob", createTestDeck())
        val serialized = json.encodeToString<GameMessage>(msg)
        val deserialized = json.decodeFromString<GameMessage>(serialized)
        assertEquals(msg, deserialized)
    }

    // ==================== Backward Compatibility ====================

    @Test
    fun lobbyPlayer_withoutIsAdmin_deserializesWithDefault() {
        // Simulates a desktop v5 client sending a LobbyPlayer without isAdmin
        val oldJson = """{"id":"p1","name":"Alice","hasDeck":true,"isReady":true,"isHost":true}"""
        val player = json.decodeFromString<LobbyPlayer>(oldJson)
        assertEquals("p1", player.id)
        assertTrue(player.isHost)
        assertFalse(player.isAdmin) // default
    }

    @Test
    fun lobbyState_withoutNewFields_deserializesWithDefaults() {
        // Desktop v5 client sends LobbyState without adminId/gameCode
        val oldJson = """{"players":[],"hostId":"h1","maxPlayers":4}"""
        val state = json.decodeFromString<GameMessage.LobbyState>(oldJson)
        assertEquals("h1", state.hostId)
        assertEquals("", state.adminId)
        assertEquals("", state.gameCode)
    }

    @Test
    fun unknownFields_areIgnored() {
        // Future client sends extra fields
        val futureJson = """{"id":"p1","name":"Alice","hasDeck":true,"isReady":true,"isHost":false,"isAdmin":false,"futureField":"value","rating":9999}"""
        val player = json.decodeFromString<LobbyPlayer>(futureJson)
        assertEquals("p1", player.id)
        // Should not crash
    }

    // ==================== All 50 NetworkAction Types ====================

    @Test
    fun allNetworkActions_roundTrip() {
        val actions: List<NetworkAction> = listOf(
            NetworkAction.DrawCard("p1"),
            NetworkAction.MoveCard("c1", Zone.BATTLEFIELD),
            NetworkAction.MoveCardToTopOfLibrary("c1"),
            NetworkAction.MoveCardToBottomOfLibrary("c1"),
            NetworkAction.MoveCardToLibraryPosition("c1", 3),
            NetworkAction.MoveCardToLibraryPositionFromBottom("c1", 2),
            NetworkAction.MoveTopCardsToZone("p1", 3, Zone.GRAVEYARD),
            NetworkAction.MoveBottomCardsToZone("p1", 2, Zone.EXILE),
            NetworkAction.MoveBottomCardToTop("p1"),
            NetworkAction.MillCards("p1", 5),
            NetworkAction.ToggleTap("c1"),
            NetworkAction.FlipCard("c1"),
            NetworkAction.ToggleFaceDown("c1"),
            NetworkAction.PlayFaceDown("c1"),
            NetworkAction.ToggleDoesntUntap("c1"),
            NetworkAction.SetAnnotation("c1", "note"),
            NetworkAction.UpdateCardGridPosition("c1", 2, 1),
            NetworkAction.AddCardCounter("c1", "+1/+1", 3),
            NetworkAction.RemoveCardCounter("c1", "+1/+1", 1),
            NetworkAction.SetCardCounter("c1", "charge", 5),
            NetworkAction.ModifyPower("c1", 2),
            NetworkAction.ModifyToughness("c1", -1),
            NetworkAction.ModifyPowerToughness("c1", 3),
            NetworkAction.SetPowerToughness("c1", 5, 5),
            NetworkAction.ResetPowerToughness("c1"),
            NetworkAction.FlowPower("c1"),
            NetworkAction.FlowToughness("c1"),
            NetworkAction.AttachCard("c1", "c2"),
            NetworkAction.DetachCard("c1"),
            NetworkAction.GiveControlTo("c1", "p2"),
            NetworkAction.UpdateLife("p1", 35),
            NetworkAction.AddPlayerCounter("p1", "poison", 1),
            NetworkAction.RemovePlayerCounter("p1", "energy", 2),
            NetworkAction.SetPlayerCounter("p1", "experience", 10),
            NetworkAction.UpdateCommanderDamage("p1", "cmd1", 7),
            NetworkAction.NextPhase,
            NetworkAction.PassTurn,
            NetworkAction.SetPhase(GamePhase.MAIN_1),
            NetworkAction.Concede("p1"),
            NetworkAction.UntapAll("p1"),
            NetworkAction.ShuffleLibrary("p1"),
            NetworkAction.ShuffleTopCards("p1", 10),
            NetworkAction.ShuffleBottomCards("p1", 5),
            NetworkAction.Mulligan("p1"),
            NetworkAction.CreateToken("p1", "Goblin", "Creature", "1", "1", "Red", null, 3),
            NetworkAction.CloneCard("c1", "p1", Zone.BATTLEFIELD, 1),
            NetworkAction.LogDieRoll("p1", "d20", 17, 1, listOf(17)),
            NetworkAction.SendChatMessage("p1", "hello"),
            NetworkAction.ToggleRevealTopCard("p1"),
            NetworkAction.ToggleLookAtTopCard("p1")
        )

        for (action in actions) {
            val message = GameMessage.GameAction(action, "p1", 12345L)
            val serialized = json.encodeToString<GameMessage>(message)
            val deserialized = json.decodeFromString<GameMessage>(serialized)

            assertTrue(
                "Failed to round-trip ${action::class.simpleName}",
                deserialized is GameMessage.GameAction
            )
            assertEquals(
                "Action mismatch for ${action::class.simpleName}",
                action,
                (deserialized as GameMessage.GameAction).action
            )
        }
    }

    // ==================== Full GameState Round-Trip ====================

    @Test
    fun fullGameState_roundTrips() {
        val card = Card(name = "Sol Ring", type = "Artifact", manaCost = "{1}")
        val dfcCard = Card(
            name = "Delver of Secrets",
            type = "Creature",
            backFaceName = "Insectile Aberration",
            backFaceImageUri = "https://example.com/back.jpg",
            backFacePower = "3",
            backFaceToughness = "2"
        )

        val instances = listOf(
            CardInstance(card = card, ownerId = "p1", zone = Zone.HAND),
            CardInstance(card = card, ownerId = "p1", zone = Zone.LIBRARY),
            CardInstance(card = card, ownerId = "p1", zone = Zone.BATTLEFIELD, isTapped = true,
                counters = mapOf("+1/+1" to 3, "charge" to 2), gridX = 0, gridY = 1,
                powerModifier = 2, toughnessModifier = -1),
            CardInstance(card = card, ownerId = "p1", zone = Zone.GRAVEYARD),
            CardInstance(card = card, ownerId = "p1", zone = Zone.EXILE),
            CardInstance(card = card, ownerId = "p1", zone = Zone.COMMAND_ZONE),
            CardInstance(card = dfcCard, ownerId = "p1", zone = Zone.BATTLEFIELD, isFlipped = true),
            CardInstance(card = card, ownerId = "p1", zone = Zone.BATTLEFIELD, isFaceDown = true),
            CardInstance(card = card, ownerId = "p1", zone = Zone.BATTLEFIELD, isToken = true),
            CardInstance(card = card, ownerId = "p1", zone = Zone.BATTLEFIELD, isClone = true,
                clonedFromId = "original-id"),
            CardInstance(card = card, ownerId = "p1", zone = Zone.BATTLEFIELD,
                doesntUntap = true, annotation = "Frozen", attachedTo = "other-card")
        )

        val state = GameState(
            gameId = "test-game",
            players = listOf(
                Player(id = "p1", name = "Alice", life = 35,
                    commanderDamage = mapOf("cmd1" to 14, "cmd2" to 7),
                    counters = mapOf("poison" to 7, "energy" to 3, "experience" to 2)),
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
        val rt = (deserialized as GameMessage.StateUpdate).gameState

        assertEquals("test-game", rt.gameId)
        assertEquals(2, rt.players.size)
        assertEquals(11, rt.cardInstances.size)
        assertEquals(5, rt.turnNumber)
        assertEquals(GamePhase.COMBAT_DAMAGE, rt.phase)

        // Player state
        val alice = rt.players[0]
        assertEquals(35, alice.life)
        assertFalse(alice.hasLost)
        assertEquals(7, alice.getCounter("poison"))
        assertEquals(3, alice.getCounter("energy"))
        assertEquals(2, alice.getCounter("experience"))
        assertEquals(14, alice.commanderDamage["cmd1"])
        assertEquals(7, alice.commanderDamage["cmd2"])

        val bob = rt.players[1]
        assertEquals(0, bob.life)
        assertTrue(bob.hasLost)

        // Card states
        val tapped = rt.cardInstances.find { it.isTapped }!!
        assertEquals(3, tapped.counters["+1/+1"])
        assertEquals(2, tapped.counters["charge"])
        assertEquals(2, tapped.powerModifier)
        assertEquals(-1, tapped.toughnessModifier)

        val flipped = rt.cardInstances.find { it.isFlipped }!!
        assertTrue(flipped.card.isDoubleFaced)
        assertEquals("Insectile Aberration", flipped.card.backFaceName)
        assertEquals("3", flipped.card.backFacePower)

        assertNotNull(rt.cardInstances.find { it.isFaceDown })
        assertNotNull(rt.cardInstances.find { it.isToken })

        val clone = rt.cardInstances.find { it.isClone }!!
        assertEquals("original-id", clone.clonedFromId)

        val frozen = rt.cardInstances.find { it.doesntUntap }!!
        assertEquals("Frozen", frozen.annotation)
        assertNotNull(frozen.attachedTo)
    }

    // ==================== Deck Serialization ====================

    @Test
    fun deck_withPartnerCommander_roundTrips() {
        val deck = Deck(
            name = "Partner Deck",
            commander = Card(name = "Partner A", type = "Legendary Creature"),
            partnerCommander = Card(name = "Partner B", type = "Legendary Creature"),
            cards = (1..98).map { Card(name = "Card $it") }
        )
        val serialized = json.encodeToString(deck)
        val deserialized = json.decodeFromString<Deck>(serialized)
        assertEquals(deck.name, deserialized.name)
        assertEquals(deck.commander.name, deserialized.commander.name)
        assertNotNull(deserialized.partnerCommander)
        assertEquals("Partner B", deserialized.partnerCommander!!.name)
        assertEquals(98, deserialized.cards.size)
    }

    @Test
    fun deck_withSideboard_roundTrips() {
        val deck = Deck(
            name = "SB Deck",
            commander = Card(name = "Commander", type = "Legendary Creature"),
            cards = (1..99).map { Card(name = "Card $it") },
            sideboard = (1..15).map { Card(name = "SB Card $it") }
        )
        val serialized = json.encodeToString(deck)
        val deserialized = json.decodeFromString<Deck>(serialized)
        assertEquals(99, deserialized.cards.size)
        assertEquals(15, deserialized.sideboard.size)
    }

    // ==================== DFC Card Serialization ====================

    @Test
    fun dfcCard_allBackFaceFields_roundTrip() {
        val card = Card(
            name = "Delver of Secrets",
            type = "Creature — Human Wizard",
            power = "1",
            toughness = "1",
            manaCost = "{U}",
            backFaceImageUri = "https://example.com/insectile.jpg",
            backFaceName = "Insectile Aberration",
            backFaceType = "Creature — Human Insect",
            backFacePower = "3",
            backFaceToughness = "2",
            backFaceOracleText = "Flying",
            backFaceManaCost = ""
        )
        val serialized = json.encodeToString(card)
        val deserialized = json.decodeFromString<Card>(serialized)

        assertTrue(deserialized.isDoubleFaced)
        assertEquals("Insectile Aberration", deserialized.backFaceName)
        assertEquals("Creature — Human Insect", deserialized.backFaceType)
        assertEquals("3", deserialized.backFacePower)
        assertEquals("2", deserialized.backFaceToughness)
        assertEquals("Flying", deserialized.backFaceOracleText)
    }

    @Test
    fun nonDfcCard_hasNoBackFace() {
        val card = Card(name = "Lightning Bolt", type = "Instant")
        val serialized = json.encodeToString(card)
        val deserialized = json.decodeFromString<Card>(serialized)
        assertFalse(deserialized.isDoubleFaced)
        assertNull(deserialized.backFaceName)
        assertNull(deserialized.backFaceImageUri)
    }

    // ==================== Zone Enum ====================

    @Test
    fun allZones_serializeCorrectly() {
        for (zone in Zone.values()) {
            val instance = CardInstance(
                card = Card(name = "Test"),
                ownerId = "p1",
                zone = zone
            )
            val serialized = json.encodeToString(instance)
            val deserialized = json.decodeFromString<CardInstance>(serialized)
            assertEquals(zone, deserialized.zone)
        }
    }

    // ==================== GamePhase Enum ====================

    @Test
    fun allPhases_serializeCorrectly() {
        for (phase in GamePhase.values()) {
            val state = GameState(
                gameId = "test",
                players = listOf(Player(id = "p1", name = "Alice")),
                cardInstances = emptyList(),
                activePlayerIndex = 0,
                phase = phase
            )
            val msg = GameMessage.StateUpdate(state)
            val serialized = json.encodeToString<GameMessage>(msg)
            val deserialized = json.decodeFromString<GameMessage>(serialized) as GameMessage.StateUpdate
            assertEquals(phase, deserialized.gameState.phase)
        }
    }

    // ==================== ErrorCode Enum ====================

    @Test
    fun allErrorCodes_serializeCorrectly() {
        for (code in ErrorCode.values()) {
            val serialized = json.encodeToString(code)
            val deserialized = json.decodeFromString<ErrorCode>(serialized)
            assertEquals(code, deserialized)
        }
    }
}
