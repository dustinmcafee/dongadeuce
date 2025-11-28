package com.dustinmcafee.dongadeuce.network

import com.dustinmcafee.dongadeuce.models.GamePhase
import com.dustinmcafee.dongadeuce.models.Zone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.*

class NetworkActionTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ==================== Card Movement Actions ====================

    @Test
    fun `DrawCard serializes correctly`() {
        val original = NetworkAction.DrawCard("player-1")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `MoveCard serializes correctly`() {
        val original = NetworkAction.MoveCard("card-123", Zone.BATTLEFIELD)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `MoveCard with all zones serializes correctly`() {
        for (zone in Zone.values()) {
            val original = NetworkAction.MoveCard("card-123", zone)
            val serialized = json.encodeToString<NetworkAction>(original)
            val deserialized = json.decodeFromString<NetworkAction>(serialized)
            assertEquals(original, deserialized)
        }
    }

    @Test
    fun `MoveCardToTopOfLibrary serializes correctly`() {
        val original = NetworkAction.MoveCardToTopOfLibrary("card-123")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `MoveCardToBottomOfLibrary serializes correctly`() {
        val original = NetworkAction.MoveCardToBottomOfLibrary("card-123")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `MoveCardToLibraryPosition serializes correctly`() {
        val original = NetworkAction.MoveCardToLibraryPosition("card-123", 5)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `MoveCardToLibraryPositionFromBottom serializes correctly`() {
        val original = NetworkAction.MoveCardToLibraryPositionFromBottom("card-123", 3)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `MoveTopCardsToZone serializes correctly`() {
        val original = NetworkAction.MoveTopCardsToZone("player-1", 5, Zone.GRAVEYARD)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `MoveBottomCardsToZone serializes correctly`() {
        val original = NetworkAction.MoveBottomCardsToZone("player-1", 3, Zone.EXILE)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `MoveBottomCardToTop serializes correctly`() {
        val original = NetworkAction.MoveBottomCardToTop("player-1")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `MillCards serializes correctly`() {
        val original = NetworkAction.MillCards("player-1", 10)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    // ==================== Card State Actions ====================

    @Test
    fun `ToggleTap serializes correctly`() {
        val original = NetworkAction.ToggleTap("card-123")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `FlipCard serializes correctly`() {
        val original = NetworkAction.FlipCard("card-123")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `ToggleFaceDown serializes correctly`() {
        val original = NetworkAction.ToggleFaceDown("card-123")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `PlayFaceDown serializes correctly`() {
        val original = NetworkAction.PlayFaceDown("card-123")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `ToggleDoesntUntap serializes correctly`() {
        val original = NetworkAction.ToggleDoesntUntap("card-123")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `SetAnnotation serializes correctly`() {
        val original = NetworkAction.SetAnnotation("card-123", "This is a note")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `SetAnnotation with null serializes correctly`() {
        val original = NetworkAction.SetAnnotation("card-123", null)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    // ==================== Counter Actions ====================

    @Test
    fun `AddCardCounter serializes correctly`() {
        val original = NetworkAction.AddCardCounter("card-123", "+1/+1", 3)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `RemoveCardCounter serializes correctly`() {
        val original = NetworkAction.RemoveCardCounter("card-123", "+1/+1", 2)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `SetCardCounter serializes correctly`() {
        val original = NetworkAction.SetCardCounter("card-123", "charge", 5)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    // ==================== Power/Toughness Actions ====================

    @Test
    fun `ModifyPower serializes correctly`() {
        val original = NetworkAction.ModifyPower("card-123", 3)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `ModifyToughness serializes correctly`() {
        val original = NetworkAction.ModifyToughness("card-123", -2)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `ModifyPowerToughness serializes correctly`() {
        val original = NetworkAction.ModifyPowerToughness("card-123", 2)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `SetPowerToughness serializes correctly`() {
        val original = NetworkAction.SetPowerToughness("card-123", 5, 5)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `ResetPowerToughness serializes correctly`() {
        val original = NetworkAction.ResetPowerToughness("card-123")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `FlowPower serializes correctly`() {
        val original = NetworkAction.FlowPower("card-123")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `FlowToughness serializes correctly`() {
        val original = NetworkAction.FlowToughness("card-123")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    // ==================== Attachment & Control Actions ====================

    @Test
    fun `AttachCard serializes correctly`() {
        val original = NetworkAction.AttachCard("aura-123", "creature-456")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `DetachCard serializes correctly`() {
        val original = NetworkAction.DetachCard("aura-123")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `GiveControlTo serializes correctly`() {
        val original = NetworkAction.GiveControlTo("card-123", "player-2")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    // ==================== Player Actions ====================

    @Test
    fun `UpdateLife serializes correctly`() {
        val original = NetworkAction.UpdateLife("player-1", 30)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `UpdateCommanderDamage serializes correctly`() {
        val original = NetworkAction.UpdateCommanderDamage("player-1", "commander-123", 15)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `AddPlayerCounter serializes correctly`() {
        val original = NetworkAction.AddPlayerCounter("player-1", "poison", 3)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `RemovePlayerCounter serializes correctly`() {
        val original = NetworkAction.RemovePlayerCounter("player-1", "poison", 2)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `SetPlayerCounter serializes correctly`() {
        val original = NetworkAction.SetPlayerCounter("player-1", "energy", 10)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    // ==================== Phase & Turn Actions ====================

    @Test
    fun `NextPhase serializes correctly`() {
        val original = NetworkAction.NextPhase
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `PassTurn serializes correctly`() {
        val original = NetworkAction.PassTurn
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `SetPhase serializes correctly`() {
        val original = NetworkAction.SetPhase(GamePhase.COMBAT_DECLARE_ATTACKERS)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `SetPhase with all phases serializes correctly`() {
        for (phase in GamePhase.values()) {
            val original = NetworkAction.SetPhase(phase)
            val serialized = json.encodeToString<NetworkAction>(original)
            val deserialized = json.decodeFromString<NetworkAction>(serialized)
            assertEquals(original, deserialized)
        }
    }

    @Test
    fun `Concede serializes correctly`() {
        val original = NetworkAction.Concede("player-1")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `UntapAll serializes correctly`() {
        val original = NetworkAction.UntapAll("player-1")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    // ==================== Library Actions ====================

    @Test
    fun `ShuffleLibrary serializes correctly`() {
        val original = NetworkAction.ShuffleLibrary("player-1")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `ShuffleTopCards serializes correctly`() {
        val original = NetworkAction.ShuffleTopCards("player-1", 10)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `ShuffleBottomCards serializes correctly`() {
        val original = NetworkAction.ShuffleBottomCards("player-1", 5)
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `Mulligan serializes correctly`() {
        val original = NetworkAction.Mulligan("player-1")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    // ==================== Token & Clone Actions ====================

    @Test
    fun `CreateToken serializes correctly`() {
        val original = NetworkAction.CreateToken(
            playerId = "player-1",
            tokenName = "Soldier",
            tokenType = "Creature - Soldier",
            power = "1",
            toughness = "1",
            color = "White",
            imageUri = null,
            quantity = 3
        )
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `CloneCard serializes correctly`() {
        val original = NetworkAction.CloneCard("card-123", "player-1")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    // ==================== Reveal Actions ====================

    @Test
    fun `ToggleRevealTopCard serializes correctly`() {
        val original = NetworkAction.ToggleRevealTopCard("player-1")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `ToggleLookAtTopCard serializes correctly`() {
        val original = NetworkAction.ToggleLookAtTopCard("player-1")
        val serialized = json.encodeToString<NetworkAction>(original)
        val deserialized = json.decodeFromString<NetworkAction>(serialized)
        assertEquals(original, deserialized)
    }
}
