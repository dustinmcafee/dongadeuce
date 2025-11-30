package com.dustinmcafee.dongadeuce.models

import com.dustinmcafee.dongadeuce.platform.generateUUID
import com.dustinmcafee.dongadeuce.platform.currentTimeMillis
import kotlinx.serialization.Serializable

/**
 * Represents a specific instance of a card in the game
 * Each physical card gets a unique ID to track it across zones
 */
@Serializable
data class CardInstance(
    val instanceId: String = generateUUID(),
    val card: Card,
    val ownerId: String,
    val controllerId: String = ownerId,
    val zone: Zone,
    val isTapped: Boolean = false,
    val isFlipped: Boolean = false,
    val isFaceDown: Boolean = false,
    val counters: Map<String, Int> = emptyMap(), // e.g., "+1/+1" -> 3
    val attachedTo: String? = null, // instanceId of card this is attached to
    val gridX: Int? = null, // Grid X position on battlefield (null = auto-arrange)
    val gridY: Int? = null,  // Grid Y position on battlefield (null = auto-arrange)
    val powerModifier: Int = 0, // Modifier to power (e.g., +2 or -1)
    val toughnessModifier: Int = 0, // Modifier to toughness
    val doesntUntap: Boolean = false, // Card doesn't untap during untap step
    val annotation: String? = null, // Custom text note on card
    val placedTimestamp: Long = currentTimeMillis(), // When card was placed at this position
    val isClone: Boolean = false, // True if this is a copy/clone of another card
    val clonedFromId: String? = null, // instanceId of the original card this was cloned from
    val handPosition: Int? = null, // Position in hand for sorting (null = default order)
    val isToken: Boolean = false // True if this is a token (should be removed when leaving battlefield)
) {
    fun tap() = copy(isTapped = true)
    fun untap() = copy(isTapped = false)
    fun flip() = copy(isFlipped = !isFlipped)
    fun moveToZone(newZone: Zone) = copy(zone = newZone)
    fun addCounter(counterType: String, amount: Int = 1): CardInstance {
        val current = counters[counterType] ?: 0
        return copy(counters = counters + (counterType to current + amount))
    }
    fun setGridPosition(x: Int, y: Int) = copy(gridX = x, gridY = y, placedTimestamp = currentTimeMillis())
    fun changeController(newControllerId: String) = copy(controllerId = newControllerId)

    /**
     * Determines the target battlefield row based on card type.
     * Row 0 (top visually): Creatures, Planeswalkers
     * Row 1 (middle): Artifacts, Enchantments
     * Row 2 (bottom visually): Lands
     * Creature type takes priority (artifact creatures go to top row).
     * Note: Row 0 is at top of screen (y=0), so higher row numbers are lower on screen.
     */
    fun getTargetBattlefieldRow(): Int {
        val typeLine = card.type?.lowercase() ?: ""
        return when {
            // Creature takes priority (artifact creatures go to top row)
            typeLine.contains("creature") || typeLine.contains("planeswalker") -> 0  // Top row (y=0)
            typeLine.contains("artifact") || typeLine.contains("enchantment") -> 1  // Middle row
            typeLine.contains("land") -> 2  // Bottom row (highest y)
            else -> 1  // Default to middle row for other types
        }
    }

    /**
     * Create a clone/copy of this card instance
     * The clone is a new card with a new ID, belonging to the specified player
     * Clone starts untapped, without counters, in the specified zone
     */
    fun createClone(newOwnerId: String, targetZone: Zone = Zone.BATTLEFIELD): CardInstance {
        return CardInstance(
            instanceId = generateUUID(),
            card = this.card,
            ownerId = newOwnerId,
            controllerId = newOwnerId,
            zone = targetZone,
            isTapped = false,
            isFlipped = false,
            isFaceDown = false,
            counters = emptyMap(),
            attachedTo = null,
            gridX = null,
            gridY = null,
            powerModifier = 0,
            toughnessModifier = 0,
            doesntUntap = false,
            annotation = null,
            placedTimestamp = currentTimeMillis(),
            isClone = true,
            clonedFromId = this.instanceId,
            handPosition = null
        )
    }
}
