package com.dustinmcafee.dongadeuce.network

import com.dustinmcafee.dongadeuce.models.GamePhase
import com.dustinmcafee.dongadeuce.models.Zone
import kotlinx.serialization.Serializable

/**
 * All game actions that can be sent over the network.
 * These map directly to GameViewModel methods.
 */
@Serializable
sealed class NetworkAction {

    // ==================== Card Movement ====================

    /**
     * Draw a card from library to hand
     */
    @Serializable
    data class DrawCard(val playerId: String) : NetworkAction()

    /**
     * Move a card to a different zone
     */
    @Serializable
    data class MoveCard(
        val cardId: String,
        val targetZone: Zone
    ) : NetworkAction()

    /**
     * Move card to top of library
     */
    @Serializable
    data class MoveCardToTopOfLibrary(val cardId: String) : NetworkAction()

    /**
     * Move card to bottom of library
     */
    @Serializable
    data class MoveCardToBottomOfLibrary(val cardId: String) : NetworkAction()

    /**
     * Move card to specific position in library from top
     */
    @Serializable
    data class MoveCardToLibraryPosition(
        val cardId: String,
        val positionFromTop: Int
    ) : NetworkAction()

    /**
     * Move card to specific position in library from bottom
     */
    @Serializable
    data class MoveCardToLibraryPositionFromBottom(
        val cardId: String,
        val positionFromBottom: Int
    ) : NetworkAction()

    /**
     * Move top N cards to a zone (e.g., mill to graveyard)
     */
    @Serializable
    data class MoveTopCardsToZone(
        val playerId: String,
        val count: Int,
        val targetZone: Zone
    ) : NetworkAction()

    /**
     * Move bottom N cards to a zone
     */
    @Serializable
    data class MoveBottomCardsToZone(
        val playerId: String,
        val count: Int,
        val targetZone: Zone
    ) : NetworkAction()

    /**
     * Move bottom card to top of library
     */
    @Serializable
    data class MoveBottomCardToTop(
        val playerId: String
    ) : NetworkAction()

    /**
     * Mill cards from library to graveyard
     */
    @Serializable
    data class MillCards(
        val playerId: String,
        val count: Int
    ) : NetworkAction()

    // ==================== Card State ====================

    /**
     * Tap or untap a card
     */
    @Serializable
    data class ToggleTap(val cardId: String) : NetworkAction()

    /**
     * Flip a card (for flip cards, DFCs)
     */
    @Serializable
    data class FlipCard(val cardId: String) : NetworkAction()

    /**
     * Toggle face-down state (morph, manifest)
     */
    @Serializable
    data class ToggleFaceDown(val cardId: String) : NetworkAction()

    /**
     * Play a card face-down to battlefield
     */
    @Serializable
    data class PlayFaceDown(val cardId: String) : NetworkAction()

    /**
     * Toggle "doesn't untap" flag
     */
    @Serializable
    data class ToggleDoesntUntap(val cardId: String) : NetworkAction()

    /**
     * Set annotation/note on a card
     */
    @Serializable
    data class SetAnnotation(
        val cardId: String,
        val annotation: String?
    ) : NetworkAction()

    /**
     * Update card grid position on battlefield
     */
    @Serializable
    data class UpdateCardGridPosition(
        val cardId: String,
        val gridX: Int,
        val gridY: Int
    ) : NetworkAction()

    // ==================== Card Counters ====================

    /**
     * Add counters to a card
     */
    @Serializable
    data class AddCardCounter(
        val cardId: String,
        val counterType: String,
        val amount: Int = 1
    ) : NetworkAction()

    /**
     * Remove counters from a card
     */
    @Serializable
    data class RemoveCardCounter(
        val cardId: String,
        val counterType: String,
        val amount: Int = 1
    ) : NetworkAction()

    /**
     * Set counter to specific value on a card
     */
    @Serializable
    data class SetCardCounter(
        val cardId: String,
        val counterType: String,
        val amount: Int
    ) : NetworkAction()

    // ==================== Power/Toughness ====================

    /**
     * Modify power by amount
     */
    @Serializable
    data class ModifyPower(
        val cardId: String,
        val amount: Int
    ) : NetworkAction()

    /**
     * Modify toughness by amount
     */
    @Serializable
    data class ModifyToughness(
        val cardId: String,
        val amount: Int
    ) : NetworkAction()

    /**
     * Modify both power and toughness by amount
     */
    @Serializable
    data class ModifyPowerToughness(
        val cardId: String,
        val amount: Int
    ) : NetworkAction()

    /**
     * Set exact power and toughness values
     */
    @Serializable
    data class SetPowerToughness(
        val cardId: String,
        val newPower: Int,
        val newToughness: Int
    ) : NetworkAction()

    /**
     * Reset power/toughness to base values
     */
    @Serializable
    data class ResetPowerToughness(val cardId: String) : NetworkAction()

    /**
     * Flow ability: +1 power, -1 toughness
     */
    @Serializable
    data class FlowPower(val cardId: String) : NetworkAction()

    /**
     * Flow ability: -1 power, +1 toughness
     */
    @Serializable
    data class FlowToughness(val cardId: String) : NetworkAction()

    // ==================== Attachments ====================

    /**
     * Attach a card (aura/equipment) to another card
     */
    @Serializable
    data class AttachCard(
        val sourceId: String,
        val targetId: String
    ) : NetworkAction()

    /**
     * Detach a card from what it's attached to
     */
    @Serializable
    data class DetachCard(val cardId: String) : NetworkAction()

    // ==================== Control ====================

    /**
     * Give control of a card to another player
     */
    @Serializable
    data class GiveControlTo(
        val cardId: String,
        val newControllerId: String
    ) : NetworkAction()

    // ==================== Player Life ====================

    /**
     * Update a player's life total
     */
    @Serializable
    data class UpdateLife(
        val playerId: String,
        val newLife: Int
    ) : NetworkAction()

    // ==================== Player Counters ====================

    /**
     * Add counters to a player (poison, energy, experience)
     */
    @Serializable
    data class AddPlayerCounter(
        val playerId: String,
        val counterType: String,
        val amount: Int = 1
    ) : NetworkAction()

    /**
     * Remove counters from a player
     */
    @Serializable
    data class RemovePlayerCounter(
        val playerId: String,
        val counterType: String,
        val amount: Int = 1
    ) : NetworkAction()

    /**
     * Set player counter to specific value
     */
    @Serializable
    data class SetPlayerCounter(
        val playerId: String,
        val counterType: String,
        val amount: Int
    ) : NetworkAction()

    // ==================== Commander Damage ====================

    /**
     * Update commander damage dealt to a player
     */
    @Serializable
    data class UpdateCommanderDamage(
        val playerId: String,
        val commanderId: String,
        val newDamage: Int
    ) : NetworkAction()

    // ==================== Turn/Phase ====================

    /**
     * Advance to next phase
     */
    @Serializable
    data object NextPhase : NetworkAction()

    /**
     * Pass turn to next player
     */
    @Serializable
    data object PassTurn : NetworkAction()

    /**
     * Set phase directly
     */
    @Serializable
    data class SetPhase(val phase: GamePhase) : NetworkAction()

    /**
     * Concede the game
     */
    @Serializable
    data class Concede(val playerId: String) : NetworkAction()

    /**
     * Untap all permanents for a player
     */
    @Serializable
    data class UntapAll(val playerId: String) : NetworkAction()

    // ==================== Library Operations ====================

    /**
     * Shuffle a player's library
     */
    @Serializable
    data class ShuffleLibrary(val playerId: String) : NetworkAction()

    /**
     * Shuffle top N cards of library
     */
    @Serializable
    data class ShuffleTopCards(
        val playerId: String,
        val count: Int
    ) : NetworkAction()

    /**
     * Shuffle bottom N cards of library
     */
    @Serializable
    data class ShuffleBottomCards(
        val playerId: String,
        val count: Int
    ) : NetworkAction()

    /**
     * Mulligan: return hand to library, shuffle, draw new hand
     */
    @Serializable
    data class Mulligan(val playerId: String) : NetworkAction()

    // ==================== Tokens & Copies ====================

    /**
     * Create a token
     */
    @Serializable
    data class CreateToken(
        val playerId: String,
        val tokenName: String,
        val tokenType: String,
        val power: String?,
        val toughness: String?,
        val color: String,
        val imageUri: String?,
        val quantity: Int = 1,
        val oracleText: String? = null
    ) : NetworkAction()

    /**
     * Clone/copy a card
     */
    @Serializable
    data class CloneCard(
        val cardId: String,
        val newOwnerId: String,
        val targetZone: Zone = Zone.BATTLEFIELD,
        val quantity: Int = 1
    ) : NetworkAction()

    // ==================== Logging ====================

    /**
     * Log a die roll result
     */
    @Serializable
    data class LogDieRoll(
        val playerId: String,
        val dieType: String,
        val result: Int,
        val numberOfDice: Int = 1,
        val individualResults: List<Int> = emptyList()
    ) : NetworkAction()

    /**
     * Send a chat message
     */
    @Serializable
    data class SendChatMessage(
        val playerId: String,
        val message: String
    ) : NetworkAction()

    // ==================== Library Visibility ====================

    /**
     * Toggle always reveal top card (visible to all players)
     */
    @Serializable
    data class ToggleRevealTopCard(val playerId: String) : NetworkAction()

    /**
     * Toggle always look at top card (visible only to owner)
     */
    @Serializable
    data class ToggleLookAtTopCard(val playerId: String) : NetworkAction()
}
